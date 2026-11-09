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
    /** LINE通信処理を作成する。テストでは実通信しないクライアントへ差し替える。 */
    protected LineLoginClient createClient() {
        return new LineLoginClient(LineLoginConfig.fromEnvironment());
    }

    protected UserDao createUserDao() { return new UserDao(); }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 認証画面をキャッシュさせず、遷移先へ参照元URLを送信しない。
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("Referrer-Policy", "no-referrer");
        if (!"GET".equals(req.getMethod()) && !"POST".equals(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        String role = req.getParameter("regRole");
        if (LineLoginSupport.redirectAuthenticated(req, res, role != null)) { return; }
        // 前回の登録区分が次のログインへ混入しないよう、開始済みの認証情報を破棄する。
        HttpSession session = req.getSession();
        session.removeAttribute(LineLoginSupport.ATTEMPT_KEY);
        // 登録開始はPOSTで受け付け、選択した利用区分をセッションへ保持する。
        if (role != null && !"POST".equals(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        // 公開登録で選べる役割だけを許可し、管理者などの不正な指定を拒否する。
        if (role != null && !LineLoginSupport.REGISTRATION_ROLES.contains(role)) {
            LineLoginSupport.error(req, res, true, "利用区分を選び直してください。");
            return;
        }
        User current = LineLoginSupport.authenticatedUser(session);
        if (current != null && role != null) {
            // LINE認証済みの本登録前会員は、登録画面からの本人の送信で区分を選び直せる。
            // 同じ画面の二重送信は、DB更新からセッション再作成まで直列化する。
            synchronized (session) {
                try {
                    if (!LineLoginSupport.validRegistrationCsrf(session, req.getParameter("csrfToken"))) {
                        res.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    User saved = createUserDao().updatePendingRole(current.getLineSubject(), role.toUpperCase(Locale.ROOT));
                    if (!LineLoginSupport.available(saved)) {
                        res.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    String lineName = OnboardingSupport.lineValue(session, "lineDisplayName", saved.getDisplayName());
                    String lineEmail = OnboardingSupport.lineValue(session, "lineEmail", saved.getEmail());
                    LineLoginSupport.signIn(req, saved, lineName, lineEmail);
                    res.sendRedirect(req.getContextPath() + LineLoginSupport.registrationDestination(saved));
                } catch (SQLException e) {
                    OnboardingSupport.logDatabaseError(req, e);
                    LineLoginSupport.error(req, res, true, "利用者区分を変更できませんでした。時間をおいて再度お試しください。");
                } catch (IllegalStateException e) {
                    // 先行する送信でセッションが切り替わった場合は、後続からDBを更新しない。
                    res.sendError(HttpServletResponse.SC_CONFLICT);
                }
            }
            return;
        }
        try {
            LineLoginClient client = createClient();
            // 電話番号はLINE認証後の各プロフィール画面で入力し、ここでは利用区分を保持する。
            var attempt = LineLoginSupport.begin(role);
            String url = client.authorizationUrl(attempt.state(), attempt.nonce(), attempt.codeVerifier());
            session.setAttribute(LineLoginSupport.ATTEMPT_KEY, attempt);
            res.sendRedirect(url);
        } catch (IllegalStateException | IllegalArgumentException e) {
            LineLoginSupport.error(req, res, role != null,
                    "LINEログインの設定が完了していません。管理者にお問い合わせください。");
        }
    }
}
