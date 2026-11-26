package jp.machiriku.servlet.management;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jp.machiriku.bean.management.ManagementProblem;
import jp.machiriku.bean.management.PageResult;

/**
 * 管理・サポート画面のHTTPリクエストから入力値やページ条件を安全に読み取るクラス。
 */
public final class FormReader {
    /** 1ページあたりの表示件数。 */
    public static final int PAGE_SIZE = 25;
    /** ページ移動時に引き継ぐ検索条件・識別子のパラメータ名。 */
    private static final List<String> PAGE_FILTERS = List.of(
            "id", "role", "tab", "q", "status", "kind", "category", "assignee",
            "industry", "reasonCode", "from", "to", "recipientId", "qualificationId", "propertyId");

    /** 読み取り対象のHTTPリクエスト。 */
    private final HttpServletRequest request;

    /**
     * 指定リクエストの入力読み取り器を生成する。
     * @param request HTTPリクエスト。
     */
    public FormReader(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * 指定パラメータの前後の空白を除いた文字列を返す。未指定時は空文字を返す。
     * @param name パラメータ名。
     * @return トリム済み文字列。
     */
    public String value(String name) {
        String raw = request.getParameter(name);
        return raw == null ? "" : raw.strip();
    }

    /**
     * 必須条件・最大文字数・NUL文字禁止を確認して文字列を取得する。
     * @param name パラメータ名。
     * @param label エラー表示用の項目名。
     * @param max 最大文字数（コードポイント単位）。
     * @param required 必須の場合はtrue。
     * @return 検証済み文字列。任意かつ空欄の場合はnull。
     */
    public String text(String name, String label, int max, boolean required) {
        String val = value(name);
        if (val.codePointCount(0, val.length()) > max || val.indexOf('\0') >= 0 || (required && val.isEmpty())) {
            throw invalid(label);
        }
        return val.isEmpty() ? null : val;
    }

    /**
     * 許可された選択肢に含まれる値だけを取得する。
     * @param name パラメータ名。
     * @param label エラー表示用の項目名。
     * @param allowed 許可する値の一覧。
     * @return 検証済みの選択値。
     */
    public String choice(String name, String label, String... allowed) {
        String val = value(name);
        if (!Arrays.asList(allowed).contains(val)) {
            throw invalid(label);
        }
        return val;
    }

    /**
     * 正の整数ID（int）を取得する。
     * @param name パラメータ名。
     * @param required 必須の場合はtrue。
     * @return 正の整数ID。任意かつ空欄の場合はnull。
     */
    public Integer intId(String name, boolean required) {
        String val = value(name);
        if (!required && val.isEmpty()) {
            return null;
        }
        return parseInt(val, name, 1, Integer.MAX_VALUE);
    }

    /**
     * 通報・問い合わせ用の正の受付番号（long）を取得する。
     * @param name パラメータ名。
     * @param required 必須の場合はtrue。
     * @return 正の受付番号。任意かつ空欄の場合はnull。
     */
    public Long longId(String name, boolean required) {
        String val = value(name);
        if (!required && val.isEmpty()) {
            return null;
        }
        try {
            long id = Long.parseLong(val);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // 数値として解釈できない場合は下の例外を送出する。
        }
        throw invalid("受付番号");
    }

    /**
     * 複数選択された正の整数IDを重複なしで取得する。
     * @param name パラメータ名。
     * @return 送信順を保持したIDの集合。
     */
    public Set<Integer> intIds(String name) {
        String[] values = request.getParameterValues(name);
        Set<Integer> ids = new LinkedHashSet<>();
        if (values != null) {
            if (values.length > 200) {
                throw invalid("選択数");
            }
            for (String val : values) {
                ids.add(parseInt(val == null ? "" : val.strip(), name, 1, Integer.MAX_VALUE));
            }
        }
        return ids;
    }

    /**
     * 指定範囲内の整数を取得する。
     * @param name パラメータ名。
     * @param label 項目名。
     * @param min 最小値。
     * @param max 最大値。
     * @return 検証済みの整数。
     */
    public int number(String name, String label, int min, int max) {
        return parseInt(value(name), label, min, max);
    }

    /**
     * 範囲と小数桁数を確認してBigDecimalを取得する。
     * @param name パラメータ名。
     * @param label 項目名。
     * @param min 最小値。
     * @param max 最大値。
     * @param scale 許容する小数点以下の最大桁数。
     * @param required 必須の場合はtrue。
     * @return 検証済みの小数。任意かつ空欄の場合はnull。
     */
    public BigDecimal decimal(String name, String label, double min, double max, int scale, boolean required) {
        String val = value(name);
        if (!required && val.isEmpty()) {
            return null;
        }
        try {
            if (val.length() > 32) {
                throw invalid(label);
            }
            BigDecimal number = new BigDecimal(val);
            if (number.scale() >= 0
                    && number.scale() <= scale
                    && number.compareTo(BigDecimal.valueOf(min)) >= 0
                    && number.compareTo(BigDecimal.valueOf(max)) <= 0) {
                return number;
            }
        } catch (NumberFormatException ignored) {
            // 数値として解釈できない場合は共通エラーにする。
        }
        throw invalid(label);
    }

    /**
     * 任意の日付パラメータ（yyyy-MM-dd）を解析する。
     * @param name パラメータ名。
     * @param label 項目名。
     * @return 日付。未入力時はnull。
     */
    public LocalDate date(String name, String label) {
        String val = value(name);
        if (val.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(val);
        } catch (DateTimeParseException error) {
            throw invalid(label);
        }
    }

    /**
     * 日本時間のdatetime-local入力値をOffsetDateTimeへ変換する。
     * @param name パラメータ名。
     * @param label 項目名。
     * @param required 必須の場合はtrue。
     * @return オフセット付き日時。任意かつ空欄の場合はnull。
     */
    public OffsetDateTime dateTime(String name, String label, boolean required) {
        String val = value(name);
        if (!required && val.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(val).atZone(ManagementFormat.JAPAN).toOffsetDateTime();
        } catch (DateTimeParseException error) {
            throw invalid(label);
        }
    }

    /**
     * 安全なHTTP/HTTPSのURLを取得する。
     * @param name パラメータ名。
     * @param label 項目名。
     * @return URL文字列。空欄時はnull。
     */
    public String webUrl(String name, String label) {
        String val = text(name, label, 1000, false);
        if (val == null || ManagementFormat.isWebUrl(val)) {
            return val;
        }
        throw invalid(label);
    }

    /**
     * 一覧の検索語（100文字以内）を取得する。
     * @return 検索語。未指定時は空文字。
     */
    public String search() {
        String query = text("q", "検索語", 100, false);
        return query == null ? "" : query;
    }

    /**
     * 現在のページ番号（1始まり）を取得する。
     * @return ページ番号。
     */
    public int page() {
        String val = value("page");
        return val.isEmpty() ? 1 : parseInt(val, "ページ", 1, 100000);
    }

    /**
     * SQLのOFFSET句に渡す開始位置を計算する。
     * @return 読み飛ばす件数。
     */
    public int offset() {
        return (page() - 1) * PAGE_SIZE;
    }

    /**
     * 次ページ判定用に1件多く取得したリストから、型付きのPageResultを組み立てる。
     * @param <T> 一覧の行データの型。
     * @param rows PAGE_SIZE + 1 件を上限に取得した検索結果。
     * @return 表示用1ページ分と前後ページURLを含むPageResult。
     */
    public <T> PageResult<T> toPageResult(List<T> rows) {
        int current = page();
        String previousUrl = current > 1 ? buildPageUrl(current - 1) : null;
        String nextUrl = rows.size() > PAGE_SIZE ? buildPageUrl(current + 1) : null;
        List<T> pageItems = rows.size() > PAGE_SIZE ? rows.subList(0, PAGE_SIZE) : rows;
        return new PageResult<>(pageItems, previousUrl, nextUrl);
    }

    /**
     * 現在の絞り込み条件を引き継いだページ移動用URLを作成する。
     * @param targetPage 移動先のページ番号。
     * @return コンテキストパスを除いたAction URL。
     */
    private String buildPageUrl(int targetPage) {
        String servletPath = request.getServletPath() == null ? "" : request.getServletPath();
        StringBuilder path = new StringBuilder(servletPath);
        path.append("?page=").append(targetPage);
        for (String filter : PAGE_FILTERS) {
            String val = value(filter);
            if (!val.isEmpty()) {
                path.append('&').append(filter).append('=')
                        .append(URLEncoder.encode(val, StandardCharsets.UTF_8));
            }
        }
        return path.toString();
    }

    /**
     * 文字列を指定範囲内の整数へ変換する。
     * @param val 変換対象の文字列。
     * @param label 項目名。
     * @param min 最小値。
     * @param max 最大値。
     * @return 変換後の整数。
     */
    private static int parseInt(String val, String label, int min, int max) {
        try {
            int number = Integer.parseInt(val);
            if (number >= min && number <= max) {
                return number;
            }
        } catch (NumberFormatException ignored) {
            // 数値変換失敗は共通の入力エラーとして扱う。
        }
        throw invalid(label);
    }

    /**
     * 項目名を含む400エラー例外を作成する。
     * @param label 項目名。
     * @return HTTP 400を表すManagementProblem。
     */
    private static ManagementProblem invalid(String label) {
        return new ManagementProblem(400, label + "の入力内容を確認してください。");
    }
}
