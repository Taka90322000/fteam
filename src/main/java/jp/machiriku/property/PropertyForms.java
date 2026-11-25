package jp.machiriku.property;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jp.machiriku.bean.PropertyContractTermInput;
import jp.machiriku.bean.PropertyInput;

/**
 * 物件登録フォーム（property/add.jsp・add-confirm.jsp）の入力値を検証し、
 * DB制約に適合する PropertyInput と画像アップロード下書き一覧へ変換する。
 */
public final class PropertyForms {
    /** JSON配列の解析に使用するマッパー。 */
    private static final ObjectMapper JSON = new ObjectMapper();

    /** 1物件あたりに受け付ける画像の最大枚数。 */
    public static final int MAX_PHOTO_COUNT = 10;

    /** ギャラリーJSON全体の最大文字数（過剰なリクエストを事前遮断する）。 */
    private static final int MAX_GALLERY_JSON_LENGTH = 8 * 1024 * 1024;

    /** 使用部分面積（㎡）の最大値（numeric(10,2) の上限に合わせる）。 */
    private static final BigDecimal MAX_AREA_SQM = new BigDecimal("99999999.99");

    /** 築年の下限値（DBのCHECK制約と一致させる）。 */
    private static final int MIN_BUILT_YEAR = 1900;

    /** 築年の上限値（DBのCHECK制約と一致させる）。 */
    private static final int MAX_BUILT_YEAR = 2100;

    /** 月数表記（例：「2ヶ月」「3か月」「1」）を抽出するパターン。 */
    private static final Pattern MONTHS_PATTERN = Pattern.compile("^([0-9]{1,3})(?:ヶ|か|カ|ケ)?月?$");

    /** 万円表記（例：「10万円」）を抽出するパターン。 */
    private static final Pattern MAN_YEN_PATTERN = Pattern.compile("^([0-9]{1,5})万円$");

    /** 円表記（例：「100000円」「100000」）を抽出するパターン。 */
    private static final Pattern YEN_PATTERN = Pattern.compile("^([0-9]{1,9})円?$");

    /** 築年表記（例：「2008年」「2008」）を抽出するパターン。 */
    private static final Pattern BUILT_YEAR_PATTERN = Pattern.compile("^([0-9]{4})年?$");

    /** 築月表記（例：「3月」「3」）を抽出するパターン。 */
    private static final Pattern BUILT_MONTH_PATTERN = Pattern.compile("^([0-9]{1,2})月?$");

    /** 結合された築年月表記（例：「2008年3月」「2008年」）を抽出するパターン。 */
    private static final Pattern COMBINED_BUILT_YM_PATTERN = Pattern.compile("^([0-9]{4})年(?:([0-9]{1,2})月)?$");

    /** 業種小分類コードの形式（I0101など）。 */
    private static final Pattern INDUSTRY_CODE_PATTERN = Pattern.compile("^I[0-9]{4}$");

    /** 営業許可状況の許可コード一覧。 */
    private static final Set<String> PERMIT_STATUSES = Set.of("UNKNOWN", "OBTAINED", "NOT_OBTAINED");

    /** 新規登録時に許可する公開ステータス一覧。 */
    private static final Set<String> NEW_LISTING_STATUSES = Set.of("OPEN", "DRAFT");

    /** 所在階の表示名・コードからDBコードへの対応表。 */
    private static final Map<String, String> FLOOR_CODE_MAP = createFloorCodeMap();

    /** 建物階数の表示名・コードからDBコードへの対応表。 */
    private static final Map<String, String> TOTAL_FLOORS_CODE_MAP = createTotalFloorsCodeMap();

    /** 建物構造の表示名・コードからDBコードへの対応表。 */
    private static final Map<String, String> BUILDING_STRUCTURE_MAP = createBuildingStructureMap();

    /** 引渡し時期の表示名・コードからDBコードへの対応表。 */
    private static final Map<String, String> HANDOVER_TIMING_MAP = createHandoverTimingMap();

    /** 画面の設備キーおよび設備コードからDBの facility_code への対応表。 */
    private static final Map<String, String> FACILITY_CODE_MAP = createFacilityCodeMap();

    /** 画面の写真種別からDBの image_kind への対応表。 */
    private static final Map<String, String> IMAGE_KIND_MAP = createImageKindMap();

    /** ユーティリティクラスのためインスタンス化を禁止する。 */
    private PropertyForms() {
    }

    /**
     * フォーム解析結果を保持するレコード。
     *
     * @param property DB保存用に検証済みの物件データ（imagesは空）
     * @param photos 永続化前のアップロード画像下書き一覧
     */
    public record ParsedProperty(PropertyInput property, List<PropertyPhotoDraft> photos) {
    }

    /**
     * 永続化前の物件写真入力を保持するレコード。
     *
     * @param imageKind 写真種別（EXTERIOR / INTERIOR / FLOORPLAN / FACILITY / SURROUNDING / OTHER）
     * @param altText 写真の説明文（任意、最大200文字）
     * @param dataUrl ブラウザーで圧縮されたData URL（data:image/...;base64,...）
     */
    public record PropertyPhotoDraft(String imageKind, String altText, String dataUrl) {
    }

    /**
     * 敷金・礼金・保証金の区分と数値をまとめた内部表現。
     *
     * @param kind 区分コード（NONE / MONTHS / AMOUNT / NEGOTIABLE）
     * @param months 月数（MONTHS時のみ非null）
     * @param yen 金額（円、AMOUNT時のみ非null）
     */
    private record DepositSpec(String kind, Integer months, Integer yen) {
    }

    /**
     * 築年・築月をまとめた内部表現。
     *
     * @param year 築年（西暦4桁、任意）
     * @param month 築月（1〜12、任意）
     */
    private record BuiltYearMonth(Integer year, Integer month) {
    }

    /**
     * 物件登録リクエストの各種パラメーターを検証し、保存可能なデータへ変換する。
     *
     * @param fields 単一値パラメーターのマップ
     * @param allowedIndustries 選択された業種小分類コード一覧
     * @param facilities 選択された設備キーまたは設備コード一覧
     * @param galleryJson 物件写真配列のJSON文字列（任意）
     * @return 検証済みの物件データと写真下書き一覧
     * @throws IllegalArgumentException 必須項目不足や範囲外・不正値が含まれる場合
     */
    public static ParsedProperty parseForCreate(
            Map<String, String> fields,
            List<String> allowedIndustries,
            List<String> facilities,
            String galleryJson) {
        // nullマップでも安全に読み取れるよう空マップへフォールバックする。
        Map<String, String> source = fields == null ? Map.of() : fields;

        // 1. 基本情報・所在地を検証する。
        String propertyName = requiredText(source.get("title"), 150, "物件名称");
        String prefecture = requiredText(source.get("prefecture"), 10, "都道府県");
        String municipality = requiredText(source.get("municipality"), 50, "市区町村");
        String addressLine = requiredText(source.get("addressLine"), 200, "町名・番地・建物名");
        String transportAccess = requiredText(source.get("access"), 150, "交通・最寄駅アクセス");

        // 緯度・経度はDB制約に合わせて両方設定または両方未設定のみ許可する。
        BigDecimal latitude = parseCoordinate(source.get("lat"), 90, "緯度");
        BigDecimal longitude = parseCoordinate(source.get("lng"), 180, "経度");
        if ((latitude == null) != (longitude == null)) {
            throw invalid("地図ピンの緯度と経度はセットで指定してください。");
        }

        // 2. 賃料・面積・建物スペックを検証する。
        int monthlyRentYen = parseRequiredNonNegativeInt(source.get("rent"), "月額賃料");
        Integer managementFeeYen = parseOptionalNonNegativeInt(source.get("managementFee"), "月額管理費・共益費");
        DepositSpec deposit = parseDepositOrKeyMoney(source.get("deposit"), "敷金");
        DepositSpec keyMoney = parseDepositOrKeyMoney(source.get("keyMoney"), "礼金");
        DepositSpec securityDeposit = parseSecurityDeposit(source.get("securityDeposit"));
        BigDecimal floorAreaSqm = parseAreaSqm(source.get("areaSqm"));
        String floorCode = parseMappedCode(source.get("floorNumber"), FLOOR_CODE_MAP, "所在階");
        String totalFloorsCode = parseMappedCode(source.get("totalFloors"), TOTAL_FLOORS_CODE_MAP, "建物階数");
        String buildingStructure = parseMappedCode(source.get("structure"), BUILDING_STRUCTURE_MAP, "建物構造");
        BuiltYearMonth builtYm = parseBuiltYearMonth(
                source.get("builtYearNum"),
                source.get("builtMonthNum"),
                source.get("builtYear"));
        String handoverTiming = parseMappedCode(source.get("handover"), HANDOVER_TIMING_MAP, "引渡し時期・状態");

        // 3. 契約形態・許可状況・公開ステータスを検証する。
        List<PropertyContractTermInput> contractTerms = parseContractTerms(source);
        String permitStatus = parsePermitStatus(source.get("permitStatus"));
        String listingStatus = parseNewListingStatus(source.get("listingStatus"));

        // 4. 設備・希望業種・条件メモを検証する。
        List<String> normalizedIndustries = parseIndustryCodes(allowedIndustries);
        List<String> normalizedFacilities = parseFacilityCodes(facilities, source);
        String conditionMemo = optionalText(source.get("condition"), 300, "居抜き・設備・業種条件メモ");

        // 5. アピール説明・外部掲載URL・写真下書きを検証する。
        String description = optionalText(source.get("description"), 2000, "物件の特徴・アピール説明文");
        String listingUrl = parseListingUrl(source.get("listingUrl"));
        List<PropertyPhotoDraft> photoDrafts = parseGalleryJson(galleryJson);

        // DB登録用の物件入力レコードを組み立てる。
        PropertyInput property = new PropertyInput(
                propertyName,
                prefecture,
                municipality,
                addressLine,
                latitude,
                longitude,
                transportAccess,
                floorAreaSqm,
                monthlyRentYen,
                managementFeeYen,
                deposit.kind(),
                deposit.months(),
                keyMoney.kind(),
                keyMoney.months(),
                securityDeposit.kind(),
                securityDeposit.months(),
                securityDeposit.yen(),
                floorCode,
                totalFloorsCode,
                buildingStructure,
                builtYm.year(),
                builtYm.month(),
                handoverTiming,
                conditionMemo,
                description,
                listingUrl,
                permitStatus,
                listingStatus,
                contractTerms,
                normalizedIndustries,
                normalizedFacilities,
                List.of());
        return new ParsedProperty(property, photoDrafts);
    }

    /**
     * 募集する契約形態（通常賃貸・ポップアップ）のチェックと日数・日額賃料を検証する。
     *
     * @param source フォーム入力マップ
     * @return 1件以上の契約条件一覧
     * @throws IllegalArgumentException 契約形態が未選択、またはポップアップ日数が不正な場合
     */
    private static List<PropertyContractTermInput> parseContractTerms(Map<String, String> source) {
        // 通常賃貸とポップアップの受付有無を判定する。
        boolean longEnabled = isChecked(source.get("longEnabled"));
        boolean popupEnabled = isChecked(source.get("popupEnabled"));
        if (!longEnabled && !popupEnabled) {
            throw invalid("募集する契約形態を1つ以上選択してください。");
        }

        // 選択された契約形態ごとに行データを構築する。
        List<PropertyContractTermInput> terms = new ArrayList<>();
        if (longEnabled) {
            terms.add(new PropertyContractTermInput("LONG", null, null, null));
        }
        if (popupEnabled) {
            int minDays = parseRequiredPositiveInt(source.get("popupMinDays"), "最低利用日数");
            int maxDays = parseRequiredPositiveInt(source.get("popupMaxDays"), "最大利用日数");
            // DB制約（max_days >= min_days）に合わせて前後関係を検証する。
            if (maxDays < minDays) {
                throw invalid("最大利用日数は最低利用日数以上の値を入力してください。");
            }
            Integer dailyRentYen = parseOptionalNonNegativeInt(source.get("popupDailyRent"), "日額賃料目安");
            terms.add(new PropertyContractTermInput("POPUP", minDays, maxDays, dailyRentYen));
        }
        return List.copyOf(terms);
    }

    /**
     * 営業許可状況を検証する。未選択時はDB既定値と同じ UNKNOWN に正規化する。
     *
     * @param raw 入力値
     * @return 正規化済みの営業許可状況コード
     * @throws IllegalArgumentException 許可されていないコードの場合
     */
    private static String parsePermitStatus(String raw) {
        String trimmed = trimToNull(raw);
        if (trimmed == null) {
            return "UNKNOWN";
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (!PERMIT_STATUSES.contains(upper)) {
            throw invalid("飲食店等 営業許可の現状を選び直してください。");
        }
        return upper;
    }

    /**
     * 新規登録時の公開ステータスを検証する（OPEN または DRAFT のみ許可）。
     *
     * @param raw 入力値
     * @return 正規化済みの公開ステータスコード
     * @throws IllegalArgumentException 未選択または新規登録で許可されない値の場合
     */
    private static String parseNewListingStatus(String raw) {
        String trimmed = trimToNull(raw);
        if (trimmed == null) {
            throw invalid("公開ステータスを選択してください。");
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (!NEW_LISTING_STATUSES.contains(upper)) {
            throw invalid("公開ステータスを選び直してください。");
        }
        return upper;
    }

    /**
     * 敷金または礼金の入力値を区分と月数へ変換する。未選択は NONE（なし）として扱う。
     *
     * @param raw 入力値（日本語表示名またはDBコード）
     * @param label エラーメッセージ用の項目名
     * @return 敷金または礼金の正規化結果
     * @throws IllegalArgumentException 想定外の形式または月数範囲外の場合
     */
    private static DepositSpec parseDepositOrKeyMoney(String raw, String label) {
        String trimmed = trimToNull(raw);
        // 未選択や「なし」系表示はすべて NONE に統一する。
        if (trimmed == null || "なし".equals(trimmed) || "掲載なし（要確認）".equals(trimmed)
                || "NONE".equalsIgnoreCase(trimmed)) {
            return new DepositSpec("NONE", null, null);
        }
        if ("相談".equals(trimmed) || "要相談".equals(trimmed) || "NEGOTIABLE".equalsIgnoreCase(trimmed)) {
            return new DepositSpec("NEGOTIABLE", null, null);
        }
        Matcher matcher = MONTHS_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            int months = Integer.parseInt(matcher.group(1));
            if (months == 0) {
                return new DepositSpec("NONE", null, null);
            }
            if (months < 1 || months > 120) {
                throw invalid(label + "の月数は1〜120ヶ月の範囲で選択してください。");
            }
            return new DepositSpec("MONTHS", months, null);
        }
        throw invalid(label + "を選び直してください。");
    }

    /**
     * 保証金の入力値を区分と月数・金額へ変換する。未選択は NONE（なし）として扱う。
     *
     * @param raw 入力値（例：「なし」「3ヶ月」「50万円」「相談」）
     * @return 保証金の正規化結果
     * @throws IllegalArgumentException 想定外の形式または範囲外の場合
     */
    private static DepositSpec parseSecurityDeposit(String raw) {
        String trimmed = trimToNull(raw);
        // 未選択や「なし」系表示はすべて NONE に統一する。
        if (trimmed == null || "なし".equals(trimmed) || "掲載なし（要確認）".equals(trimmed)
                || "NONE".equalsIgnoreCase(trimmed)) {
            return new DepositSpec("NONE", null, null);
        }
        if ("相談".equals(trimmed) || "要相談".equals(trimmed) || "NEGOTIABLE".equalsIgnoreCase(trimmed)) {
            return new DepositSpec("NEGOTIABLE", null, null);
        }

        // 「Nヶ月」形式を月数として解釈する。
        Matcher monthsMatcher = MONTHS_PATTERN.matcher(trimmed);
        if (monthsMatcher.matches() && (trimmed.contains("月") || trimmed.length() <= 2)) {
            int months = Integer.parseInt(monthsMatcher.group(1));
            if (months == 0) {
                return new DepositSpec("NONE", null, null);
            }
            if (months < 1 || months > 120) {
                throw invalid("保証金の月数は1〜120ヶ月の範囲で選択してください。");
            }
            return new DepositSpec("MONTHS", months, null);
        }

        // 「N万円」形式を円単位の整数へ換算する。
        Matcher manYenMatcher = MAN_YEN_PATTERN.matcher(trimmed);
        if (manYenMatcher.matches()) {
            long yen = Long.parseLong(manYenMatcher.group(1)) * 10000L;
            if (yen <= 0 || yen > Integer.MAX_VALUE) {
                throw invalid("保証金の金額が範囲外です。");
            }
            return new DepositSpec("AMOUNT", null, (int) yen);
        }

        // 「N円」または数値のみの金額指定を解釈する。
        Matcher yenMatcher = YEN_PATTERN.matcher(trimmed);
        if (yenMatcher.matches()) {
            long yen = Long.parseLong(yenMatcher.group(1));
            if (yen == 0) {
                return new DepositSpec("NONE", null, null);
            }
            if (yen < 1 || yen > Integer.MAX_VALUE) {
                throw invalid("保証金の金額が範囲外です。");
            }
            return new DepositSpec("AMOUNT", null, (int) yen);
        }

        throw invalid("保証金を選び直してください。");
    }

    /**
     * 使用部分面積（㎡）を検証し、小数第2位へ正規化する。
     *
     * @param raw 入力値
     * @return 0より大きい面積（㎡）
     * @throws IllegalArgumentException 未入力・非数値・0以下・桁数超過の場合
     */
    private static BigDecimal parseAreaSqm(String raw) {
        String trimmed = trimToNull(raw);
        if (trimmed == null) {
            throw invalid("使用部分面積（㎡）を入力してください。");
        }
        if (trimmed.length() > 20) {
            throw invalid("使用部分面積（㎡）の値が大きすぎます。");
        }
        try {
            // 指数表記や正負の異常値を排除しつつ小数第2位へ丸める。
            BigDecimal value = new BigDecimal(trimmed);
            if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(MAX_AREA_SQM) > 0) {
                throw invalid("使用部分面積（㎡）は0より大きい数値で入力してください。");
            }
            BigDecimal scaled = value.setScale(2, RoundingMode.HALF_UP);
            if (scaled.compareTo(BigDecimal.ZERO) <= 0) {
                throw invalid("使用部分面積（㎡）は0.01以上で入力してください。");
            }
            return scaled;
        } catch (NumberFormatException e) {
            throw invalid("使用部分面積（㎡）は数値で入力してください。");
        }
    }

    /**
     * 築年・築月（または結合された築年月文字列）を検証して整数へ変換する。
     *
     * @param rawYear 築年の入力値（例：「2008年」「不明」）
     * @param rawMonth 築月の入力値（例：「3月」）
     * @param rawCombined 確認画面等から送られる結合文字列（例：「2008年3月」）
     * @return 築年と築月のペア（不明・未入力時はいずれもnull）
     * @throws IllegalArgumentException 範囲外の年月、または築年未設定で築月のみ指定された場合
     */
    private static BuiltYearMonth parseBuiltYearMonth(String rawYear, String rawMonth, String rawCombined) {
        String yearText = trimToNull(rawYear);
        String monthText = trimToNull(rawMonth);

        // 個別欄が空で結合文字列だけ渡された場合は、結合文字列から年月を分解する。
        if (yearText == null && monthText == null) {
            String combined = trimToNull(rawCombined);
            if (combined == null || "不明".equals(combined)) {
                return new BuiltYearMonth(null, null);
            }
            Matcher combinedMatcher = COMBINED_BUILT_YM_PATTERN.matcher(combined);
            if (!combinedMatcher.matches()) {
                throw invalid("築年月の形式が正しくありません。");
            }
            yearText = combinedMatcher.group(1);
            monthText = combinedMatcher.group(2);
        }

        // 「不明」または未入力の場合は築年・築月ともにnullとする。
        if (yearText == null || "不明".equals(yearText)) {
            if (monthText != null) {
                throw invalid("築月を指定する場合は築年も選択してください。");
            }
            return new BuiltYearMonth(null, null);
        }

        Matcher yearMatcher = BUILT_YEAR_PATTERN.matcher(yearText);
        if (!yearMatcher.matches()) {
            throw invalid("築年を選び直してください。");
        }
        int year = Integer.parseInt(yearMatcher.group(1));
        if (year < MIN_BUILT_YEAR || year > MAX_BUILT_YEAR) {
            throw invalid("築年は1900年〜2100年の範囲で選択してください。");
        }

        Integer month = null;
        if (monthText != null) {
            Matcher monthMatcher = BUILT_MONTH_PATTERN.matcher(monthText);
            if (!monthMatcher.matches()) {
                throw invalid("築月を選び直してください。");
            }
            int parsedMonth = Integer.parseInt(monthMatcher.group(1));
            if (parsedMonth < 1 || parsedMonth > 12) {
                throw invalid("築月は1月〜12月の範囲で選択してください。");
            }
            month = parsedMonth;
        }
        return new BuiltYearMonth(year, month);
    }

    /**
     * 選択された希望・許可業種コード一覧を重複除去して検証する。
     *
     * @param rawCodes リクエストから受け取った業種コード一覧
     * @return 重複を除いた有効形式の業種コード一覧
     * @throws IllegalArgumentException 件数超過または形式不正のコードが含まれる場合
     */
    private static List<String> parseIndustryCodes(List<String> rawCodes) {
        if (rawCodes == null || rawCodes.isEmpty()) {
            return List.of();
        }
        if (rawCodes.size() > 200) {
            throw invalid("希望・許可する出店業種の選択数が多すぎます。");
        }
        // 画面の選択順を保ちつつ重複を除去する。
        Set<String> normalized = new LinkedHashSet<>();
        for (String rawCode : rawCodes) {
            String code = trimToNull(rawCode);
            if (code == null) {
                continue;
            }
            if (!INDUSTRY_CODE_PATTERN.matcher(code).matches()) {
                throw invalid("希望・許可する出店業種を選び直してください。");
            }
            normalized.add(code);
        }
        return List.copyOf(normalized);
    }

    /**
     * 基本インフラ設備の選択値（複数パラメーターまたは infra_* 形式）をDBの設備コードへ変換する。
     *
     * @param rawFacilities facilityCodes として送信された設備キー・コード一覧
     * @param source フォームの単一値マップ（infra_hasInuki 等のチェックボックス送信にも対応）
     * @return 重複を除いたDB用設備コード一覧
     * @throws IllegalArgumentException 未知の設備コードが含まれる場合
     */
    private static List<String> parseFacilityCodes(List<String> rawFacilities, Map<String, String> source) {
        Set<String> normalized = new LinkedHashSet<>();

        // facilityCodes パラメーターで渡された設備キー・コードを正規化する。
        if (rawFacilities != null) {
            if (rawFacilities.size() > 100) {
                throw invalid("基本インフラ設備の選択数が多すぎます。");
            }
            for (String raw : rawFacilities) {
                String trimmed = trimToNull(raw);
                if (trimmed == null) {
                    continue;
                }
                String code = FACILITY_CODE_MAP.get(trimmed);
                if (code == null) {
                    code = FACILITY_CODE_MAP.get(trimmed.toUpperCase(Locale.ROOT));
                }
                if (code == null) {
                    throw invalid("基本インフラ設備の選択内容が正しくありません。");
                }
                normalized.add(code);
            }
        }

        // _property_form.jsp のチェックボックス名（infra_hasInuki 等）で送信された場合も取り込む。
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.startsWith("infra_")) {
                continue;
            }
            String value = trimToNull(entry.getValue());
            if (value == null || "false".equalsIgnoreCase(value) || "0".equals(value)) {
                continue;
            }
            String facilityKey = key.substring("infra_".length());
            String code = FACILITY_CODE_MAP.get(facilityKey);
            if (code == null) {
                throw invalid("基本インフラ設備の選択内容が正しくありません。");
            }
            normalized.add(code);
        }

        return List.copyOf(normalized);
    }

    /**
     * 物件写真JSON配列を解析し、種別・説明文・Data URLの形式を検証する。
     *
     * @param galleryJson JSON配列文字列（未添付時はnullまたは空文字）
     * @return 検証済みの写真下書き一覧
     * @throws IllegalArgumentException JSON不正・枚数超過・種別不正・説明文長超過の場合
     */
    private static List<PropertyPhotoDraft> parseGalleryJson(String galleryJson) {
        if (galleryJson == null || galleryJson.isBlank()) {
            return List.of();
        }
        if (galleryJson.length() > MAX_GALLERY_JSON_LENGTH) {
            throw invalid("写真の合計データ量が上限を超えています。枚数を減らして再度お試しください。");
        }
        JsonNode root;
        try {
            // 末尾の余分なトークンを拒否して厳格にJSONを読み取る。
            root = JSON.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(galleryJson);
        } catch (JsonProcessingException e) {
            throw invalid("写真データの形式が正しくありません。写真を選び直してください。");
        }
        if (root == null || !root.isArray()) {
            throw invalid("写真データの形式が正しくありません。写真を選び直してください。");
        }
        if (root.size() > MAX_PHOTO_COUNT) {
            throw invalid("登録できる物件写真は最大" + MAX_PHOTO_COUNT + "枚までです。");
        }

        // 配列の並び順をそのまま表示順として取り込む。
        List<PropertyPhotoDraft> drafts = new ArrayList<>();
        for (JsonNode item : root) {
            if (!item.isObject()) {
                throw invalid("写真データの形式が正しくありません。");
            }
            String rawUrl = trimToNull(item.path("url").asText(null));
            if (rawUrl == null) {
                throw invalid("写真データが欠落しています。写真を選び直してください。");
            }
            if (!rawUrl.startsWith("data:image/")) {
                throw invalid("未対応の形式の写真が含まれています。画像ファイルを選び直してください。");
            }
            String rawKind = trimToNull(item.path("kind").asText(null));
            String imageKind = rawKind == null ? "OTHER" : IMAGE_KIND_MAP.get(rawKind.toLowerCase(Locale.ROOT));
            if (imageKind == null) {
                throw invalid("写真種別を選び直してください。");
            }
            String altText = optionalText(item.path("label").asText(null), 200, "写真の説明");
            drafts.add(new PropertyPhotoDraft(imageKind, altText, rawUrl));
        }
        return List.copyOf(drafts);
    }

    /**
     * 既存の不動産掲載ページURLを検証する（http/httpsのみ許可）。
     *
     * @param raw 入力値
     * @return 検証済みURL（未入力時はnull）
     * @throws IllegalArgumentException 文字数超過または不正なURL形式の場合
     */
    private static String parseListingUrl(String raw) {
        String url = optionalText(raw, 1000, "既存の不動産掲載ページURL");
        if (url == null) {
            return null;
        }
        // 制御文字やバックスラッシュを含む入力はURI構文異常として拒否する。
        if (url.codePoints().anyMatch(Character::isISOControl) || url.indexOf('\\') >= 0) {
            throw invalid("既存の不動産掲載ページURLは正しいhttp(s)のURLで入力してください。");
        }
        try {
            URI uri = new URI(url);
            if (!uri.isAbsolute()
                    || (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw invalid("既存の不動産掲載ページURLは正しいhttp(s)のURLで入力してください。");
            }
            return url;
        } catch (URISyntaxException e) {
            throw invalid("既存の不動産掲載ページURLは正しいhttp(s)のURLで入力してください。");
        }
    }

    /**
     * 緯度または経度の数値を検証し、小数第6位へ丸める。
     *
     * @param raw 入力値
     * @param maxAbs 絶対値の上限（緯度90・経度180）
     * @param label エラーメッセージ用の項目名
     * @return 正規化済みの座標値（未入力時はnull）
     * @throws IllegalArgumentException 数値でないか範囲外の場合
     */
    private static BigDecimal parseCoordinate(String raw, int maxAbs, String label) {
        String trimmed = trimToNull(raw);
        if (trimmed == null) {
            return null;
        }
        if (trimmed.length() > 40) {
            throw invalid(label + "の値が正しくありません。");
        }
        try {
            BigDecimal number = new BigDecimal(trimmed);
            if (number.abs().compareTo(BigDecimal.valueOf(maxAbs)) > 0) {
                throw invalid(label + "の値が範囲外です。");
            }
            return number.setScale(6, RoundingMode.HALF_UP);
        } catch (NumberFormatException | ArithmeticException e) {
            throw invalid(label + "の値が正しくありません。");
        }
    }

    /**
     * 必須の0以上整数を検証する。
     *
     * @param raw 入力値
     * @param label エラーメッセージ用の項目名
     * @return 0以上の整数値
     * @throws IllegalArgumentException 未入力・負数・非整数・上限超過の場合
     */
    private static int parseRequiredNonNegativeInt(String raw, String label) {
        Integer value = parseOptionalNonNegativeInt(raw, label);
        if (value == null) {
            throw invalid(label + "を入力してください。");
        }
        return value;
    }

    /**
     * 任意の0以上整数を検証する。
     *
     * @param raw 入力値
     * @param label エラーメッセージ用の項目名
     * @return 0以上の整数値（未入力時はnull）
     * @throws IllegalArgumentException 負数・非整数・上限超過の場合
     */
    private static Integer parseOptionalNonNegativeInt(String raw, String label) {
        String trimmed = trimToNull(raw);
        if (trimmed == null) {
            return null;
        }
        if (!trimmed.matches("^[0-9]{1,10}$")) {
            throw invalid(label + "は0以上の整数で入力してください。");
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw invalid(label + "の値が大きすぎます。");
        }
    }

    /**
     * 必須の1以上整数を検証する。
     *
     * @param raw 入力値
     * @param label エラーメッセージ用の項目名
     * @return 1以上の整数値
     * @throws IllegalArgumentException 未入力・0以下・非整数の場合
     */
    private static int parseRequiredPositiveInt(String raw, String label) {
        Integer value = parseOptionalNonNegativeInt(raw, label);
        if (value == null || value < 1) {
            throw invalid(label + "は1以上の整数で入力してください。");
        }
        return value;
    }

    /**
     * 対応表を用いて任意の選択肢文字列をDBコードへ変換する。
     *
     * @param raw 入力値
     * @param mapping 表示名・コードからDBコードへの対応表
     * @param label エラーメッセージ用の項目名
     * @return 対応するDBコード（未選択時はnull）
     * @throws IllegalArgumentException 対応表に存在しない値の場合
     */
    private static String parseMappedCode(String raw, Map<String, String> mapping, String label) {
        String trimmed = trimToNull(raw);
        if (trimmed == null) {
            return null;
        }
        String code = mapping.get(trimmed);
        if (code == null) {
            code = mapping.get(trimmed.toUpperCase(Locale.ROOT));
        }
        if (code == null) {
            throw invalid(label + "を選び直してください。");
        }
        return code;
    }

    /**
     * 必須文字列の前後空白を除き、文字数上限を検証する。
     *
     * @param raw 入力値
     * @param maxLength 最大文字数（Unicodeコードポイント数）
     * @param label エラーメッセージ用の項目名
     * @return 検証済み文字列
     * @throws IllegalArgumentException 未入力または文字数超過の場合
     */
    private static String requiredText(String raw, int maxLength, String label) {
        String result = optionalText(raw, maxLength, label);
        if (result == null) {
            throw invalid(label + "を入力してください。");
        }
        return result;
    }

    /**
     * 任意文字列の前後空白を除き、文字数上限を検証する。
     *
     * @param raw 入力値
     * @param maxLength 最大文字数（Unicodeコードポイント数）
     * @param label エラーメッセージ用の項目名
     * @return 検証済み文字列（空欄時はnull）
     * @throws IllegalArgumentException 文字数超過の場合
     */
    private static String optionalText(String raw, int maxLength, String label) {
        String trimmed = trimToNull(raw);
        if (trimmed == null) {
            return null;
        }
        if (trimmed.codePointCount(0, trimmed.length()) > maxLength) {
            throw invalid(label + "は" + maxLength + "文字以内で入力してください。");
        }
        return trimmed;
    }

    /**
     * チェックボックスの送信値が有効かどうかを判定する。
     *
     * @param raw 送信値
     * @return on / true / 1 のいずれかならtrue
     */
    private static boolean isChecked(String raw) {
        if (raw == null) {
            return false;
        }
        String trimmed = raw.trim();
        return "on".equalsIgnoreCase(trimmed) || "true".equalsIgnoreCase(trimmed) || "1".equals(trimmed);
    }

    /**
     * 文字列の前後空白を除き、空であればnullを返す。
     *
     * @param raw 対象文字列
     * @return トリム後文字列、空欄ならnull
     */
    private static String trimToNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    /**
     * 入力検証エラーの例外を生成する。
     *
     * @param message 利用者向けのエラーメッセージ
     * @return IllegalArgumentException インスタンス
     */
    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    /**
     * 所在階の表示名・DBコード対応表を生成する。
     *
     * @return 所在階マッピング
     */
    private static Map<String, String> createFloorCodeMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("地下2階", "B2");
        map.put("B2", "B2");
        map.put("地下1階", "B1");
        map.put("B1", "B1");
        for (int floor = 1; floor <= 9; floor++) {
            String code = floor + "F";
            map.put(floor + "階", code);
            map.put(code, code);
        }
        map.put("10階以上", "10F_PLUS");
        map.put("10F_PLUS", "10F_PLUS");
        map.put("一棟貸し", "WHOLE_BUILDING");
        map.put("WHOLE_BUILDING", "WHOLE_BUILDING");
        map.put("その他", "OTHER");
        map.put("OTHER", "OTHER");
        return Map.copyOf(map);
    }

    /**
     * 建物階数の表示名・DBコード対応表を生成する。
     *
     * @return 建物階数マッピング
     */
    private static Map<String, String> createTotalFloorsCodeMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("1階建（平屋）", "1F");
        map.put("1階建", "1F");
        map.put("1F", "1F");
        for (int floor = 2; floor <= 10; floor++) {
            String code = floor + "F";
            map.put(floor + "階建", code);
            map.put(code, code);
        }
        map.put("11階建以上", "11F_PLUS");
        map.put("11F_PLUS", "11F_PLUS");
        return Map.copyOf(map);
    }

    /**
     * 建物構造の表示名・DBコード対応表を生成する。
     *
     * @return 建物構造マッピング
     */
    private static Map<String, String> createBuildingStructureMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("木造", "WOOD");
        map.put("WOOD", "WOOD");
        map.put("軽量鉄骨造", "LIGHT_STEEL");
        map.put("LIGHT_STEEL", "LIGHT_STEEL");
        map.put("鉄骨造", "STEEL");
        map.put("S造(鉄骨造)", "STEEL");
        map.put("STEEL", "STEEL");
        map.put("RC(鉄筋コンクリート造)", "RC");
        map.put("RC", "RC");
        map.put("SRC(鉄骨鉄筋コンクリート造)", "SRC");
        map.put("SRC", "SRC");
        map.put("ALC", "ALC");
        map.put("その他", "OTHER");
        map.put("OTHER", "OTHER");
        return Map.copyOf(map);
    }

    /**
     * 引渡し時期の表示名・DBコード対応表を生成する。
     *
     * @return 引渡し時期マッピング
     */
    private static Map<String, String> createHandoverTimingMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("即時", "IMMEDIATE");
        map.put("IMMEDIATE", "IMMEDIATE");
        map.put("相談（即時可）", "CONSULT_IMMEDIATE");
        map.put("CONSULT_IMMEDIATE", "CONSULT_IMMEDIATE");
        map.put("1ヶ月後", "AFTER_1M");
        map.put("AFTER_1M", "AFTER_1M");
        map.put("2ヶ月後", "AFTER_2M");
        map.put("AFTER_2M", "AFTER_2M");
        map.put("3ヶ月後", "AFTER_3M");
        map.put("AFTER_3M", "AFTER_3M");
        map.put("相談", "CONSULT");
        map.put("要相談", "CONSULT");
        map.put("CONSULT", "CONSULT");
        return Map.copyOf(map);
    }

    /**
     * 画面の設備キーおよび設備コードからDBの facility_code への対応表を生成する。
     *
     * @return 設備コードマッピング
     */
    private static Map<String, String> createFacilityCodeMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("hasInuki", "INUKI");
        map.put("INUKI", "INUKI");
        map.put("hasParking", "PARKING");
        map.put("PARKING", "PARKING");
        map.put("hasBarrierFree", "BARRIER_FREE");
        map.put("BARRIER_FREE", "BARRIER_FREE");
        map.put("hasPrivateToilet", "PRIVATE_TOILET");
        map.put("PRIVATE_TOILET", "PRIVATE_TOILET");
        map.put("hasIndividualAc", "INDIVIDUAL_AC");
        map.put("INDIVIDUAL_AC", "INDIVIDUAL_AC");
        map.put("hasPower", "POWER");
        map.put("POWER", "POWER");
        map.put("hasCityGas", "CITY_GAS");
        map.put("CITY_GAS", "CITY_GAS");
        map.put("hasWaterDrainage", "WATER_DRAINAGE");
        map.put("WATER_DRAINAGE", "WATER_DRAINAGE");
        map.put("hasGreaseTrap", "GREASE_TRAP");
        map.put("GREASE_TRAP", "GREASE_TRAP");
        map.put("hasExhaustDuct", "EXHAUST_DUCT");
        map.put("EXHAUST_DUCT", "EXHAUST_DUCT");
        return Map.copyOf(map);
    }

    /**
     * 画面の写真種別キーからDBの image_kind への対応表を生成する。
     *
     * @return 写真種別マッピング
     */
    private static Map<String, String> createImageKindMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("exterior", "EXTERIOR");
        map.put("interior", "INTERIOR");
        map.put("floorplan", "FLOORPLAN");
        map.put("facility", "FACILITY");
        map.put("surrounding", "SURROUNDING");
        map.put("other", "OTHER");
        return Map.copyOf(map);
    }
}
