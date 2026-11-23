package jp.machiriku.servlet.management;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.management.PageRows;

/** 管理・サポートフォームの入力検証と、一覧のページ移動に必要な値を扱う。 */
final class ManagementRequest {
    /** 一覧で一度に表示する行数。 */
    static final int PAGE_SIZE = 25;
    /** 管理機能と問い合わせの送信で共有するセッション内のトークン名。 */
    private static final String CSRF_KEY = "managementCsrfToken";
    /** セッション固有のトークンを発行する乱数源。 */
    private static final SecureRandom RANDOM = new SecureRandom();
    /** 営業期間の入力に用いる日本の時刻。 */
    static final ZoneId JAPAN = ZoneId.of("Asia/Tokyo");
    /** ページ移動で引き継ぐ検索条件と詳細画面の識別子。 */
    private static final List<String> PAGE_FILTERS = List.of("id", "role", "tab", "q", "status", "kind", "category", "assignee",
            "industry", "reasonCode", "from", "to", "recipientId", "qualificationId", "propertyId");
    /** 文字列・選択値を受け取るHTTPリクエスト。 */
    private final HttpServletRequest request;

    /**
     * 現在の画面のフォーム入力を扱う。
     * @param request 認可後のリクエスト。文字コードを設定済みであること。
     */
    ManagementRequest(HttpServletRequest request) { this.request = request; }

    /**
     * 指定した欄の文字列を取得する。
     * @param name フォームまたは検索条件の名前。
     * @return 前後の空白を除いた値。空欄なら空文字。
     */
    String value(String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.strip();
    }

    /**
     * 文字数と必須条件を確認して保存値を取得する。
     * @param name フォームの欄名。
     * @param label エラー画面に表示する項目名。
     * @param max DBまたはフォームで許容する最大文字数。
     * @param required 必須項目かどうか。
     * @return 検証済みの文字列。任意欄が空ならnull。
     * @throws ManagementProblem 必須欄が空か、保存できる文字数や文字の条件を満たさない場合。
     */
    String text(String name, String label, int max, boolean required) {
        String value = value(name);
        // Javaが2文字として扱う記号も、DBと同じように一つのコードポイントとして数える。
        if (value.codePointCount(0, value.length()) > max || value.indexOf('\0') >= 0 || (required && value.isEmpty())) {
            throw invalid(label);
        }
        return value.isEmpty() ? null : value;
    }

    /**
     * 選択肢の中にある値だけを受け付ける。
     * @param name フォームの欄名。
     * @param label エラー画面に表示する項目名。
     * @param allowed サーバーが認める値。
     * @return 選択した値。
     * @throws ManagementProblem 許可された選択肢に値が含まれない場合。
     */
    String choice(String name, String label, String... allowed) {
        String value = value(name);
        if (!Arrays.asList(allowed).contains(value)) { throw invalid(label); }
        return value;
    }

    /**
     * 既存のintegerキーを取得する。
     * @param name IDの欄名。
     * @param required 必須の場合はtrue。
     * @return 正のID。任意欄が空ならnull。
     * @throws ManagementProblem 必須IDが未指定、または正の整数として扱えない場合。
     */
    Integer id(String name, boolean required) {
        String value = value(name);
        if (!required && value.isEmpty()) { return null; }
        return integer(value, name, 1, Integer.MAX_VALUE);
    }

    /**
     * 通報・問い合わせで使うbigintキーを取得する。
     * @param name 受付番号の欄名。
     * @param required 必須の場合はtrue。
     * @return 正のID。任意欄が空ならnull。
     */
    Long ticket(String name, boolean required) {
        String value = value(name);
        if (!required && value.isEmpty()) { return null; }
        try {
            long id = Long.parseLong(value);
            if (id > 0) { return id; }
        } catch (NumberFormatException ignored) { /* 受付番号として扱える範囲を超えている。 */ }
        throw invalid("受付番号");
    }

    /**
     * 複数選択された整数IDを、送信順に重複を除いて取得する。
     * @param name 複数選択の欄名。
     * @return 選択されたID。
     */
    Set<Integer> ids(String name) {
        String[] values = request.getParameterValues(name);
        Set<Integer> ids = new LinkedHashSet<>();
        if (values != null) {
            // 一つのフォームが扱える選択数に絞り、選択されていない場合は空集合で返す。
            if (values.length > 200) { throw invalid("選択数"); }
            for (String value : values) { ids.add(integer(value, name, 1, Integer.MAX_VALUE)); }
        }
        return ids;
    }

    /**
     * 指定した範囲内の整数を取得する。
     * @param name フォームの欄名。
     * @param label 項目名。
     * @param min 最小値。
     * @param max 最大値。
     * @return 検証済みの数値。
     */
    int number(String name, String label, int min, int max) { return integer(value(name), label, min, max); }

    /**
     * DBのnumeric列へ収まる小数を取得する。
     * @param name フォームの欄名。
     * @param label 項目名。
     * @param min 最小値。
     * @param max 最大値。
     * @param scale 最大の小数点以下桁数。
     * @param required 必須項目ならtrue。
     * @return 検証済みの小数。空欄ならnull。
     * @throws ManagementProblem 必須欄が空、または指定された桁数や数値の範囲を外れる場合。
     */
    BigDecimal decimal(String name, String label, double min, double max, int scale, boolean required) {
        String value = value(name);
        if (!required && value.isEmpty()) { return null; }
        try {
            // SQLの丸めに依存せず、入力された桁数と範囲を確認する。
            if (value.length() > 32) { throw invalid(label); }
            var number = new BigDecimal(value);
            if (number.scale() >= 0 && number.scale() <= scale && number.compareTo(BigDecimal.valueOf(min)) >= 0
                    && number.compareTo(BigDecimal.valueOf(max)) <= 0) { return number; }
        } catch (NumberFormatException ignored) { /* 数値に変換できない場合は共通の入力エラーを返す。 */ }
        throw invalid(label);
    }

    /**
     * 任意の日付欄を解析する。
     * @param name フォームの欄名。
     * @param label エラー画面に表示する項目名。
     * @return 日付。未指定ならnull。
     */
    LocalDate date(String name, String label) {
        String value = value(name);
        if (value.isEmpty()) { return null; }
        try { return LocalDate.parse(value); } catch (DateTimeParseException error) { throw invalid(label); }
    }

    /**
     * 営業期間などの日本時間をDB用のオフセット付き日時に変換する。
     * @param name フォームの欄名。
     * @param label エラー画面に表示する項目名。
     * @param required 必須の場合はtrue。
     * @return オフセット付き日時。任意欄が空ならnull。
     * @throws ManagementProblem 必須欄が空、または日時として解釈できない場合。
     */
    OffsetDateTime dateTime(String name, String label, boolean required) {
        String value = value(name);
        if (!required && value.isEmpty()) { return null; }
        try { return LocalDateTime.parse(value).atZone(JAPAN).toOffsetDateTime(); }
        catch (DateTimeParseException error) { throw invalid(label); }
    }

    /**
     * 公開する案内リンクに使えるURLだけを取得する。
     * @param name URLの欄名。
     * @param label エラー画面に表示する項目名。
     * @return HTTP(S)のURL。未指定ならnull。
     * @throws ManagementProblem 有効な案内用URLにできない場合。
     */
    String webUrl(String name, String label) {
        String value = text(name, label, 1000, false);
        if (value == null || isWebUrl(value)) { return value; }
        throw invalid(label);
    }

    /**
     * 案内に入力された値をブラウザーのリンクにできるか確認する。
     * @param value 公開するURLの候補。
     * @return ホストを指定したHTTP(S)のURLであればtrue。
     */
    static boolean isWebUrl(String value) {
        try {
            URI uri = URI.create(value);
            return uri.getHost() != null && uri.getUserInfo() == null
                    && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()));
        } catch (IllegalArgumentException ignored) { return false; }
    }

    /**
     * 画面の絞り込みで使う検索語を取得する。
     * @return 100文字以内の検索語。空欄なら空文字。
     */
    String search() { String query = text("q", "検索語", 100, false); return query == null ? "" : query; }

    /**
     * 一覧の開始位置を計算する。
     * @return SQLのOFFSETに使う件数。
     */
    int offset() { return (page() - 1) * PAGE_SIZE; }

    /**
     * 取得した行から表示する一ページと、移動可能なページを用意する。
     * @param rows 次ページの有無を判断する一行を含む検索結果。
     * @return 一覧の行と前後のページのURL。
     */
    PageRows pageRows(List<Map<String, Object>> rows) {
        // 一件余分に取得している場合にだけ次のページを表示する。
        int current = page();
        String previous = current > 1 ? pagePath(current - 1) : null;
        String next = rows.size() > PAGE_SIZE ? pagePath(current + 1) : null;
        return new PageRows(rows.stream().limit(PAGE_SIZE).toList(), previous, next);
    }

    /**
     * 一覧の現在ページを取得する。
     * @return 1から始まるページ番号。
     */
    int page() { String value = value("page"); return value.isEmpty() ? 1 : integer(value, "ページ", 1, 100000); }

    /**
     * 対象と絞り込み条件を保ったページ移動先を作る。
     * @param page 移動するページ番号。
     * @return コンテキストパスに続く画面内URL。
     */
    String pagePath(int page) {
        // 現在のAction URLを使い、ページ番号と既知の絞り込み条件を付ける。
        StringBuilder path = new StringBuilder(request.getServletPath());
        path.append("?page=").append(page);
        for (String filter : PAGE_FILTERS) {
            String value = value(filter);
            if (!value.isEmpty()) { path.append('&').append(filter).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8)); }
        }
        return path.toString();
    }

    /**
     * フォームを同じ会員のセッションから送信できるようにする。
     * @param session 認証済みの会員セッション。
     * @return フォームに付けるトークン。
     */
    static String csrfToken(HttpSession session) {
        synchronized (session) {
            if (session.getAttribute(CSRF_KEY) instanceof String token) { return token; }
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = HexFormat.of().formatHex(bytes);
            session.setAttribute(CSRF_KEY, token);
            return token;
        }
    }

    /**
     * リクエストが現在のセッションから送信されたかを確認する。
     * @param session 会員セッション。
     * @return トークンが一致した場合にtrue。
     */
    boolean validCsrf(HttpSession session) {
        try {
            return session != null && session.getAttribute(CSRF_KEY) instanceof String token
                    && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), value("csrfToken").getBytes(StandardCharsets.UTF_8));
        } catch (IllegalStateException expired) { return false; }
    }

    /**
     * 桁と範囲の確認を行う。
     * @param value 数字の文字列。
     * @param label 項目名。
     * @param min 最小値。
     * @param max 最大値。
     * @return 指定範囲内の整数。
     */
    private static int integer(String value, String label, int min, int max) {
        try {
            int number = Integer.parseInt(value);
            if (number >= min && number <= max) { return number; }
        } catch (NumberFormatException ignored) { /* 整数の範囲外は入力エラーとして扱う。 */ }
        throw invalid(label);
    }

    /**
     * 指定項目の入力を案内するエラーを作る。
     * @param label 画面に表示する項目名。
     * @return HTTP 400として扱うエラー。
     */
    private static ManagementProblem invalid(String label) { return new ManagementProblem(400, label + "の入力内容を確認してください。"); }
}
