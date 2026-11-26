package jp.machiriku.servlet.management;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.HexFormat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.ManagementProblem;
import jp.machiriku.dao.ProfileDao;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.servlet.MemberSession;

/**
 * 管理・サポートActionから直接呼び出す、認可・CSRF検証・エラー応答・画面共通属性の設定部品。
 */
public final class ManagementAuth {
    /** 管理・サポート画面で共用するセッション内のCSRFトークンキー。 */
    private static final String CSRF_KEY = "managementCsrfToken";
    /** トークン生成に使用する暗号学的に安全な乱数生成器。 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** ユーティリティクラスのためインスタンス化しない。 */
    private ManagementAuth() { }

    /**
     * 管理者権限を持つLINE会員であることを確認する。
     * 未ログイン時はログイン画面へ遷移し、権限不足時は403を返してnullを返す。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param authDao 管理者権限を照会するDAO。
     * @return 確認済みの管理者会員。継続できない場合はnull。
     * @throws IOException リダイレクトやエラー応答の送信に失敗した場合。
     */
    public static User requireAdmin(HttpServletRequest req, HttpServletResponse res, AdminAuditDao authDao)
            throws IOException {
        prepareResponse(req, res);
        // LINEで本人確認済みのセッションのみを管理操作の対象とする。
        HttpSession session = req.getSession(false);
        User member = MemberSession.signedInUser(session);
        if (member == null) {
            if (hasDemoRole(session)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "管理画面には権限のあるLINE会員でログインしてください。");
            } else {
                MemberSession.redirectToLogin(req, res);
            }
            return null;
        }
        try {
            // DB上の現在の管理権限と利用状態を確認する。
            if (!authDao.isAdmin(member.getUserId())) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "このアカウントには管理権限がありません。");
                return null;
            }
            return member;
        } catch (SQLException error) {
            handleSqlError(req, res, error);
            return null;
        }
    }

    /**
     * サポート画面（問い合わせ・通報・事業者一覧）を利用できる会員であることを確認する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param activeOnly 本登録済み（ACTIVE）のみ許可する場合はtrue。
     * @return 確認済みの会員。継続できない場合はnull。
     * @throws IOException リダイレクトやエラー応答の送信に失敗した場合。
     */
    public static User requireMember(HttpServletRequest req, HttpServletResponse res, boolean activeOnly)
            throws IOException {
        prepareResponse(req, res);
        HttpSession session = req.getSession(false);
        User member = MemberSession.requireCurrentUser(req, res, session, ProfileDao::new);
        if (member == null) {
            return null;
        }
        if (activeOnly && !"ACTIVE".equals(member.getAccountStatus())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "通報するには本登録を完了してください。");
            return null;
        }
        return member;
    }

    /**
     * サポートフォーム送信時のログイン済み会員をセッションから取得する（DB行ロックは各DAO内で行う）。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @return ログイン済み会員。未ログイン時はログイン画面へ遷移しnull。
     * @throws IOException リダイレクトに失敗した場合。
     */
    public static User requireSignedInForPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        prepareResponse(req, res);
        User member = MemberSession.signedInUser(req.getSession(false));
        if (member == null) {
            MemberSession.redirectToLogin(req, res);
            return null;
        }
        return member;
    }

    /**
     * 公開画面向けにキャッシュ制御・文字コードを設定し、ログイン中であれば会員情報を返す。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @return ログイン済みの会員。ゲストの場合はnull。
     * @throws IOException 文字コード設定に失敗した場合。
     */
    public static User optionalMember(HttpServletRequest req, HttpServletResponse res) throws IOException {
        prepareResponse(req, res);
        return MemberSession.signedInUser(req.getSession(false));
    }

    /**
     * GETまたはHEAD以外のメソッドを405で拒否する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @return 許可されたメソッドであればtrue。
     * @throws IOException エラー送信に失敗した場合。
     */
    public static boolean allowReadMethod(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String method = req.getMethod();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return false;
        }
        return true;
    }

    /**
     * セッション固有のCSRFトークンを取得する。未発行であれば新しく作成する。
     * @param session 現在のHTTPセッション。
     * @return フォームのhidden項目に設定するトークン文字列。
     */
    public static String csrfToken(HttpSession session) {
        synchronized (session) {
            if (session.getAttribute(CSRF_KEY) instanceof String token) {
                return token;
            }
            // 256ビットの乱数から16進文字列のトークンを作成する。
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = HexFormat.of().formatHex(bytes);
            session.setAttribute(CSRF_KEY, token);
            return token;
        }
    }

    /**
     * POSTリクエストのCSRFトークンを検証する。不一致の場合は403を返す。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param isAdmin 管理画面用のエラーメッセージを使う場合はtrue。
     * @return トークンが一致した場合はtrue。
     * @throws IOException エラー応答の送信に失敗した場合。
     */
    public static boolean verifyCsrf(HttpServletRequest req, HttpServletResponse res, boolean isAdmin)
            throws IOException {
        HttpSession session = req.getSession(false);
        String submitted = req.getParameter("csrfToken");
        String trimmed = submitted == null ? "" : submitted.strip();
        boolean valid = false;
        try {
            if (session != null && session.getAttribute(CSRF_KEY) instanceof String expected) {
                valid = MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        trimmed.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IllegalStateException expired) {
            valid = false;
        }
        if (!valid) {
            String message = isAdmin
                    ? "管理画面を開き直してから送信してください。"
                    : "サポート画面を開き直してから送信してください。";
            res.sendError(HttpServletResponse.SC_FORBIDDEN, message);
            return false;
        }
        return true;
    }

    /**
     * 共通ヘッダー・サイドバー・CSRF部品が参照するリクエスト属性をまとめて設定する。
     * @param req HTTPリクエスト。
     * @param title 画面タイトル。
     * @param isAdmin 管理画面レイアウトで表示する場合はtrue。
     * @param signedIn ログイン済み会員向けにCSRFトークンを付与する場合はtrue。
     */
    public static void preparePage(HttpServletRequest req, String title, boolean isAdmin, boolean signedIn) {
        req.setAttribute("managementTitle", title);
        req.setAttribute("managementAdmin", isAdmin);
        req.setAttribute("managementRoute", req.getServletPath());
        HttpSession session = req.getSession(false);
        if (signedIn && session != null) {
            req.setAttribute("managementCsrf", csrfToken(session));
        }
    }

    /**
     * POST更新後に303リダイレクト（PRGパターン）を行い、再読み込みによる二重送信を防ぐ。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param path コンテキストパスを除いた遷移先URL。
     */
    public static void redirectSaved(HttpServletRequest req, HttpServletResponse res, String path) {
        res.setStatus(HttpServletResponse.SC_SEE_OTHER);
        String separator = path.contains("?") ? "&" : "?";
        res.setHeader("Location", req.getContextPath() + path + separator + "saved=1");
    }

    /**
     * 業務例外・入力例外・SQL例外を適切なHTTPエラー応答に変換して返す。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param error 発生した例外。
     * @throws IOException エラー応答の送信に失敗した場合。
     */
    public static void handleError(HttpServletRequest req, HttpServletResponse res, Exception error)
            throws IOException {
        if (error instanceof ManagementProblem problem) {
            res.sendError(problem.getStatus(), problem.getMessage());
            return;
        }
        if (error instanceof IllegalArgumentException invalid) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, invalid.getMessage());
            return;
        }
        if (error instanceof SQLException sqlError) {
            handleSqlError(req, res, sqlError);
            return;
        }
        throw new RuntimeException(error);
    }

    /**
     * SQL詳細や個人情報をログ・画面へ露出させず、SQLStateに応じたHTTPエラーを返す。
     * @param req ログ記録に使うリクエスト。
     * @param res HTTPレスポンス。
     * @param error 発生したSQLException。
     * @throws IOException エラー応答の送信に失敗した場合。
     */
    public static void handleSqlError(HttpServletRequest req, HttpServletResponse res, SQLException error)
            throws IOException {
        String state = error.getSQLState() == null ? "" : error.getSQLState();
        if ("02000".equals(state)) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
            return;
        }
        // SQL文やパラメータ値を出さずSQLStateだけを記録する。
        req.getServletContext().log("Management database operation failed (SQLState=" + state + ").");
        if (state.startsWith("23")) {
            res.sendError(HttpServletResponse.SC_CONFLICT,
                    "同じ情報がすでに登録されているか、他のデータから参照されています。入力と現在の状態を確認してください。");
        } else if (state.startsWith("40") || "55P03".equals(state)) {
            res.sendError(HttpServletResponse.SC_CONFLICT,
                    "別の操作と重なりました。画面を開き直して現在の状態を確認してください。");
        } else if ("42P01".equals(state) || "42703".equals(state) || "42501".equals(state)) {
            res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "管理・サポート機能のデータベース設定を確認してください。");
        } else {
            res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "現在データベースを利用できません。時間をおいて再度お試しください。");
        }
    }

    /**
     * キャッシュ禁止ヘッダーとUTF-8文字コードを設定する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @throws IOException 文字コード設定に失敗した場合。
     */
    private static void prepareResponse(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setHeader("Cache-Control", "no-store");
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
    }

    /**
     * LINE会員を持たないセッションがデモ表示ロールを保持しているか判定する。
     * @param session HTTPセッション。
     * @return デモ表示中の場合はtrue。
     */
    private static boolean hasDemoRole(HttpSession session) {
        try {
            return session != null && session.getAttribute("userRole") instanceof String;
        } catch (IllegalStateException expired) {
            return false;
        }
    }
}
