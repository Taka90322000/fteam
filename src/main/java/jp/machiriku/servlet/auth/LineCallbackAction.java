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

/** LINEからの認証結果を検証し、会員登録または既存会員のログインを完了する。 */
public class LineCallbackAction extends Action {
    /** 通信処理を差し替え可能にし、テストでLINEへの接続を不要にする。 */
    protected LineLoginClient createClient() {
        return new LineLoginClient(LineLoginConfig.fromEnvironment());
    }

    /** 会員の永続化処理を取得する。テストではDBを使わないDAOに差し替える。 */
    protected UserDao createUserDao() { return new UserDao(); }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("Referrer-Policy", "no-referrer");
        if (!"GET".equals(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        // stateと有効期限を確認して認証情報を一度だけ消費し、不正な応答では外部APIやDBへ進まない。
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
            // nonceとPKCE検証値は開始時のセッションから使い、LINE検証済みの本人情報だけを受け取る。
            var profile = createClient().authenticate(code, attempt.nonce(), attempt.codeVerifier());
            UserDao dao = createUserDao();
            // メールが同じでも別人の可能性があるため、LINEのsubjectで既存会員を識別する。
            User user = dao.findByLineSubject(profile.lineSubject());
            if (user == null) {
                if (!registration) {
                    LineLoginSupport.error(req, res, true, "このLINEアカウントは未登録です。利用区分を選んで新規会員登録してください。");
                    return;
                }
                // 本人情報はLINEの検証結果、登録区分は認証開始時のセッションから保存する。
                // メール未取得時のnullもそのまま渡し、DB用の役割は大文字に統一する。
                User newUser = new User();
                newUser.setDisplayName(profile.displayName());
                newUser.setEmail(profile.email());
                newUser.setUserRole(attempt.registrationRole().toUpperCase(Locale.ROOT));
                newUser.setLineSubject(profile.lineSubject());
                try {
                    user = dao.insert(newUser);
                } catch (SQLException e) {
                    // 同時に届いた別のコールバックが先に登録した場合は、同じsubjectの会員だけを再取得する。
                    // メールの一致を理由に別のLINEアカウントへ紐付けない。
                    if (!"23505".equals(e.getSQLState())) { throw e; }
                    user = dao.findByLineSubject(profile.lineSubject());
                    if (user == null) {
                        LineLoginSupport.error(req, res, true,
                                "このメールアドレスは既に登録されています。既存の登録情報をご確認ください。");
                        return;
                    }
                }
            }
            // 本登録前の区分変更を行い、保存直前にDBの利用状態を再確認する。
            if (registration && LineLoginSupport.available(user) && "PENDING".equals(user.getAccountStatus())) {
                user = dao.updatePendingRole(profile.lineSubject(), attempt.registrationRole().toUpperCase(Locale.ROOT));
            }
            if (!LineLoginSupport.available(user)) {
                LineLoginSupport.error(req, res, false, "このアカウントは利用できません。管理者にお問い合わせください。");
                return;
            }
            LineLoginSupport.signIn(req, user, profile.displayName(), profile.email());
            res.sendRedirect(req.getContextPath() + (registration
                    ? LineLoginSupport.registrationDestination(user) : "/index.jsp"));
        } catch (InterruptedException e) {
            // 通信中断の状態を上位の処理にも伝えられるよう、割り込みフラグを戻す。
            Thread.currentThread().interrupt();
            LineLoginSupport.error(req, res, registration, "LINEとの通信が中断されました。もう一度お試しください。");
        } catch (IOException e) {
            LineLoginSupport.error(req, res, registration,
                    "LINE認証に失敗しました。もう一度お試しください。");
        } catch (SQLException e) {
            // DBの例外詳細にはメールや電話番号が含まれる場合があるため、SQLStateだけを記録する。
            req.getServletContext().log("LINE login database operation failed (SQLState=" + e.getSQLState() + ").");
            LineLoginSupport.error(req, res, registration, "会員情報の保存・取得に失敗しました。時間をおいて再度お試しください。");
        } catch (IllegalStateException | IllegalArgumentException e) {
            LineLoginSupport.error(req, res, registration, "LINEログインの設定または登録情報を確認してください。");
        }
    }
}
