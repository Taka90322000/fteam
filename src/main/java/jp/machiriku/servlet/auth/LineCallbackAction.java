package jp.machiriku.servlet.auth;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.auth.LineLoginClient;
import jp.machiriku.auth.LineLoginConfig;
import jp.machiriku.bean.User;
import jp.machiriku.dao.UserDao;
import jp.machiriku.tool.Action;

public class LineCallbackAction extends Action {
    protected LineLoginClient createClient() {
        return new LineLoginClient(LineLoginConfig.fromEnvironment());
    }

    protected UserDao createUserDao() { return new UserDao(); }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("Referrer-Policy", "no-referrer");
        if (!"GET".equals(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        var attempt = LineLoginSupport.consume(req.getSession(false), req.getParameter("state"));
        if (attempt == null) {
            LineLoginSupport.error(req, res, false, "認証の有効期限が切れたか、認証情報が一致しません。ログインからやり直してください。");
            return;
        }
        boolean registration = attempt.registrationRole() != null;
        if (req.getParameter("error") != null) {
            LineLoginSupport.error(req, res, registration, "LINEでの認証がキャンセルされました。もう一度お試しください。");
            return;
        }
        String code = req.getParameter("code");
        if (code == null || code.isBlank()) {
            LineLoginSupport.error(req, res, registration, "認証コードを取得できませんでした。もう一度お試しください。");
            return;
        }
        try {
            var profile = createClient().authenticate(code, attempt.nonce(), attempt.codeVerifier());
            UserDao dao = createUserDao();
            User user = dao.findByLineSubject(profile.lineSubject());
            if (user == null) {
                if (!registration) {
                    LineLoginSupport.error(req, res, true, "このLINEアカウントは未登録です。利用区分を選んで新規会員登録してください。");
                    return;
                }
                User newUser = new User();
                newUser.setDisplayName(profile.displayName());
                newUser.setEmail(profile.email());
                newUser.setUserRole(attempt.registrationRole().toUpperCase(Locale.ROOT));
                newUser.setLineSubject(profile.lineSubject());
                newUser.setPhoneE164(attempt.phoneE164());
                try {
                    user = dao.insert(newUser);
                } catch (SQLException e) {
                    // A parallel callback may already have registered this LINE account.
                    // Never link a different LINE subject just because its email matches.
                    if (!"23505".equals(e.getSQLState())) { throw e; }
                    user = dao.findByLineSubject(profile.lineSubject());
                    if (user == null) {
                        LineLoginSupport.error(req, res, true,
                                "このメールアドレスまたは電話番号は既に登録されています。既存の登録情報をご確認ください。");
                        return;
                    }
                }
            }
            if ((!"ACTIVE".equals(user.getAccountStatus()) && !"PENDING".equals(user.getAccountStatus()))
                    || user.getWithdrawnAt() != null) {
                LineLoginSupport.error(req, res, false, "このアカウントは利用できません。管理者にお問い合わせください。");
                return;
            }
            // Replace any demo/previous user's session and rotate its identifier.
            req.getSession().invalidate();
            var session = req.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("userRole", user.getUserRole().toLowerCase(Locale.ROOT));
            res.sendRedirect(req.getContextPath() + "/index.jsp");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LineLoginSupport.error(req, res, registration, "LINEとの通信が中断されました。もう一度お試しください。");
        } catch (IOException e) {
            LineLoginSupport.error(req, res, registration,
                    "LINE認証またはメールアドレスの取得に失敗しました。メールアドレスの登録と共有の許可を確認し、もう一度お試しください。");
        } catch (SQLException e) {
            // Do not log SQLException details: PostgreSQL may include email/phone values.
            req.getServletContext().log("LINE login database operation failed (SQLState=" + e.getSQLState() + ").");
            LineLoginSupport.error(req, res, registration, "会員情報の保存・取得に失敗しました。時間をおいて再度お試しください。");
        } catch (IllegalStateException | IllegalArgumentException e) {
            LineLoginSupport.error(req, res, registration, "LINEログインの設定または登録情報を確認してください。");
        }
    }
}
