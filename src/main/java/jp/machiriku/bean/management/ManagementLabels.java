package jp.machiriku.bean.management;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理・サポート画面で使用する各種コード値と日本語表示名の対応を定義するクラス。
 */
public final class ManagementLabels {
    /** 利用区分のコードと表示名。 */
    public static final Map<String, String> USER_ROLES = ordered(
            "RESIDENT", "地域住民",
            "BUSINESS", "事業主",
            "OWNER", "物件オーナー");

    /** 会員の利用状態のコードと表示名。 */
    public static final Map<String, String> USER_STATES = ordered(
            "PENDING", "本登録前",
            "ACTIVE", "利用中",
            "BAN", "利用停止中",
            "WITHDRAWN", "退会済み");

    /** 物件の募集・契約状態のコードと表示名。 */
    public static final Map<String, String> PROPERTY_STATES = ordered(
            "DRAFT", "下書き",
            "OPEN", "募集中",
            "PAUSED", "募集停止中",
            "CONTRACTED", "契約済み");

    /** 物件の許認可確認状態のコードと表示名。 */
    public static final Map<String, String> PERMIT_STATES = ordered(
            "UNKNOWN", "未確認",
            "OBTAINED", "取得済み",
            "NOT_OBTAINED", "未取得");

    /** 物件の管理者掲載状態の選択肢。 */
    public static final Map<String, String> PROPERTY_VISIBILITIES = ordered(
            "HIDDEN", "掲載停止中",
            "NORMAL", "通常");

    /** 問い合わせカテゴリのコードと表示名。 */
    public static final Map<String, String> INQUIRY_CATEGORIES = ordered(
            "ACCOUNT", "アカウント・登録",
            "PROPERTY", "物件・出店",
            "SURVEY_COUPON", "アンケート・クーポン",
            "OTHER", "その他");

    /** 問い合わせ対応状態のコードと表示名。 */
    public static final Map<String, String> INQUIRY_STATES = ordered(
            "WAITING_ADMIN", "管理者の対応待ち",
            "IN_PROGRESS", "対応中",
            "WAITING_USER", "利用者の返信待ち",
            "CLOSED", "完了");

    /** 管理者返信時に選択できる問い合わせ状態のコードと表示名。 */
    public static final Map<String, String> ADMIN_REPLY_STATES = ordered(
            "IN_PROGRESS", "対応中",
            "WAITING_USER", "利用者の返信待ち",
            "CLOSED", "完了");

    /** 通報対象区分のコードと表示名。 */
    public static final Map<String, String> REPORT_TARGET_KINDS = ordered(
            "OWNER", "物件オーナー",
            "BUSINESS", "事業主",
            "PROPERTY", "物件");

    /** 通報対応状態のコードと表示名。 */
    public static final Map<String, String> REPORT_STATES = ordered(
            "OPEN", "未対応",
            "IN_REVIEW", "確認中",
            "RESOLVED", "完了");

    /** 通報理由のコードと表示名。 */
    public static final Map<String, String> REPORT_REASONS = ordered(
            "INACCURATE", "情報の誤り・虚偽",
            "DUPLICATE", "重複掲載",
            "IMPERSONATION", "なりすまし",
            "HARASSMENT", "不適切なやり取り",
            "INAPPROPRIATE", "不適切な掲載内容",
            "OTHER", "その他");

    /** 資格・書類マスター種別のコードと表示名。 */
    public static final Map<String, String> QUALIFICATION_KINDS = ordered(
            "QUALIFICATION", "資格",
            "PERMIT", "許認可",
            "DOCUMENT", "書類");

    /** アンケート種別のコードと表示名。 */
    public static final Map<String, String> SURVEY_KINDS = ordered(
            "POST_VISIT", "来店アンケート",
            "TEMPLATE", "テンプレート");

    /** アンケート受付状態のコードと表示名。 */
    public static final Map<String, String> SURVEY_STATES = ordered(
            "DRAFT", "下書き",
            "OPEN", "受付中",
            "CLOSED", "受付終了");

    /** クーポン配布状態のコードと表示名。 */
    public static final Map<String, String> COUPON_STATES = ordered(
            "DRAFT", "下書き",
            "ACTIVE", "配布有効",
            "STOPPED", "配布停止中");

    /** クーポン特典種別のコードと表示名。 */
    public static final Map<String, String> COUPON_BENEFITS = ordered(
            "PERCENT", "割合割引",
            "AMOUNT", "定額割引",
            "SERVICE", "サービス");

    /** クーポン配布対象のコードと表示名。 */
    public static final Map<String, String> COUPON_TARGETS = ordered(
            "ALL_VOTERS", "物件への投票者",
            "INDUSTRY_VOTERS", "対象業種への投票者",
            "SURVEY", "アンケート回答者");

    /** ユーティリティクラスのためインスタンス化しない。 */
    private ManagementLabels() { }

    /**
     * 通報対象の種類（物件・オーナー・事業主）に応じて選択可能な通報理由マップを返す。
     * @param kind 通報対象区分（PROPERTY / OWNER / BUSINESS）。
     * @return 適用可能な通報理由のコードと表示名。
     */
    public static Map<String, String> reportReasonsFor(String kind) {
        Map<String, String> reasons = new LinkedHashMap<>(REPORT_REASONS);
        if ("PROPERTY".equals(kind)) {
            reasons.remove("HARASSMENT");
            reasons.remove("IMPERSONATION");
        } else {
            reasons.remove("DUPLICATE");
        }
        return reasons;
    }

    /**
     * 通報対象が物件か会員かに応じた対応結果の選択肢を返す。
     * @param isProperty 通報対象が物件の場合はtrue。
     * @return 対応結果コードと表示名のマップ。
     */
    public static Map<String, String> reportResolutionsFor(boolean isProperty) {
        if (isProperty) {
            return ordered("NO_ACTION", "対応不要", "CORRECTION", "修正依頼を記録", "HIDE_PROPERTY", "掲載停止");
        }
        return ordered("NO_ACTION", "対応不要", "CORRECTION", "修正依頼を記録", "SUSPEND_USER", "会員の利用停止");
    }

    /**
     * コードに対応する表示名をマップから取得する。未登録コードはそのまま返す。
     * @param map 定義済みコードマップ。
     * @param code DBのコード値。
     * @return 表示名。codeがnullの場合はnull。
     */
    public static String label(Map<String, String> map, String code) {
        if (code == null) {
            return null;
        }
        return map.getOrDefault(code, code);
    }

    /**
     * 挿入順を保持した文字列マップを作成する。
     * @param pairs キー、値の順に並べた文字列。
     * @return 定義順を保持したマップ。
     */
    public static Map<String, String> ordered(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
