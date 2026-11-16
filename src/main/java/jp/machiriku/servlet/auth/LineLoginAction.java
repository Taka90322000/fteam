package jp.machiriku.servlet.auth;

import java.sql.SQLException;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.auth.LineLoginClient;
import jp.machiriku.auth.LineLoginConfig;
import jp.machiriku.bean.User;
import jp.machiriku.dao.UserDao;
import jp.machiriku.tool.Action;

/** 入力を検証し、認証に必要な情報をセッションへ保持してLINEの認可画面へ案内する。 */
public class LineLoginAction extends Action {
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 認証画面をキャッシュさせず、遷移先へ参照元URLを送信しない。
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("Referrer-Policy", "no-referrer");

        // 認証の入口ではGETとPOSTを受け付ける。
        String method = req.getMethod(); // リクエストのHTTPメソッド。
        boolean post = "POST".equals(method); // 登録フォームの送信に使うPOSTかどうか。
        if (!"GET".equals(method) && !post) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // 利用区分の指定で、通常ログインと新規登録を判別する。
        String registrationRole = req.getParameter("regRole"); // 指定された利用区分。未指定ならnull。
        boolean registration = registrationRole != null; // 利用区分を指定した登録リクエストかどうか。
        // ログイン済みならトップへ戻す。本登録前の区分選択は続行する。
        if (LineLoginSupport.redirectAuthenticated(req, res, registration)) {
            return;
        }

        // 前回の認証試行をセッションから破棄する。
        HttpSession session = req.getSession(); // 今回の認証情報を保存するHTTPセッション。
        session.removeAttribute(LineLoginSupport.ATTEMPT_KEY);

        // 登録開始はPOSTに限定し、公開登録で選べる利用区分か確認する。
        if (registration && !post) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        if (registration && !LineLoginSupport.REGISTRATION_ROLES.contains(registrationRole)) {
            LineLoginSupport.error(req, res, true, "利用区分を選び直してください。");
            return;
        }

        // 認証済みの本登録前会員は、現在のセッションで利用区分を変更する。
        User member = LineLoginSupport.authenticatedUser(session); // セッションの認証済み会員。該当しなければnull。
        if (registration && member != null) {
            changePendingRole(req, res, session, member, registrationRole);
            return;
        }

        try {
            // LINEの接続設定を読み込み、今回の認証に使う一時情報を作る。
            LineLoginClient client = createClient(); // LINEの接続設定を持つAPIクライアント。
            LineLoginSupport.Attempt attempt = LineLoginSupport.begin(registrationRole); // 今回の照合に使う認証情報。

            // 今回の認証に必要なパラメーターを含むLINEの認可URL。
            String authorizationUrl = client.authorizationUrl(attempt.state(), attempt.nonce(), attempt.codeVerifier());
            // コールバックで使う情報をセッションに保存し、LINEの認可画面へ移動する。
            session.setAttribute(LineLoginSupport.ATTEMPT_KEY, attempt);
            res.sendRedirect(authorizationUrl);
        } catch (IllegalStateException | IllegalArgumentException e) {
            LineLoginSupport.error(req, res, registration,
                    "LINEログインの設定が完了していません。管理者にお問い合わせください。");
        }
    }

    /** LINE認証済みの本登録前会員が、利用区分を選び直す。 */
    private void changePendingRole(HttpServletRequest req, HttpServletResponse res,
            HttpSession session, User member, String registrationRole) throws Exception {
        // 同じ画面の二重送信は、CSRF検証からセッション再作成まで直列化する。
        synchronized (session) {
            try {
                // フォームのCSRFトークンをセッションの値と照合する。
                if (!LineLoginSupport.validRegistrationCsrf(session, req.getParameter("csrfToken"))) {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                // DBの表記に合わせ、本登録前の会員の利用区分を更新する。
                String databaseRole = registrationRole.toUpperCase(Locale.ROOT); // DB用の大文字の利用区分。
                // DB処理後の会員情報。対象の会員が見つからなければnull。
                User saved = createUserDao().updatePendingRole(member.getLineSubject(), databaseRole);
                // 更新後の会員がログイン可能かを確認する。
                if (!LineLoginSupport.available(saved)) {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                // 引き継ぐLINE表示名。セッションに値がなければ会員の表示名。
                String lineName = OnboardingSupport.lineValue(session, "lineDisplayName", saved.getDisplayName());
                // 引き継ぐLINEメールアドレス。セッションに値がなければ会員のメールアドレス。
                String lineEmail = OnboardingSupport.lineValue(session, "lineEmail", saved.getEmail());
                // LINEから取得した値を引き継ぎ、最新の会員情報でセッションを作り直す。
                LineLoginSupport.signIn(req, saved, lineName, lineEmail);
                // 利用区分と本登録の状態に応じた画面へ進む。
                res.sendRedirect(req.getContextPath() + LineLoginSupport.registrationDestination(saved));
            } catch (SQLException e) {
                OnboardingSupport.logDatabaseError(req, e);
                LineLoginSupport.error(req, res, true,
                        "利用者区分を変更できませんでした。時間をおいて再度お試しください。");
            } catch (IllegalStateException e) {
                // 先行する送信でセッションが無効になった場合、後続のCSRF検証はここで終了する。
                res.sendError(HttpServletResponse.SC_CONFLICT);
            }
        }
    }

    /** テストでは実通信しないクライアントへ差し替える。 */
    protected LineLoginClient createClient() {
        return new LineLoginClient(LineLoginConfig.fromEnvironment());
    }

    /** テストではDBを使わないDAOに差し替える。 */
    protected UserDao createUserDao() {
        return new UserDao();
    }
}
