package jp.machiriku.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.auth.LineLoginClient;
import jp.machiriku.auth.LineLoginConfig;
import jp.machiriku.tool.Action;

public class LineLoginAction extends Action {
    protected LineLoginClient createClient() {
        return new LineLoginClient(LineLoginConfig.fromEnvironment());
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("Referrer-Policy", "no-referrer");
        if (!"GET".equals(req.getMethod()) && !"POST".equals(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        // A new attempt must not reuse registration choices from a previous login.
        HttpSession session = req.getSession();
        session.removeAttribute(LineLoginSupport.ATTEMPT_KEY);
        String role = req.getParameter("regRole");
        if (role != null && !"POST".equals(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        if (role != null && !LineLoginSupport.REGISTRATION_ROLES.contains(role)) {
            LineLoginSupport.error(req, res, true, "利用区分を選び直してください。");
            return;
        }
        String phone = null;
        if (role != null) {
            phone = req.getParameter("phoneE164");
            phone = phone == null || phone.isBlank() ? null : phone.trim();
            if ((phone != null && !phone.matches("\\+[1-9][0-9]{7,14}"))
                    || (!"resident".equals(role) && phone == null)) {
                LineLoginSupport.error(req, res, true,
                        "事業主・オーナーは電話番号を国際形式で入力してください（例：+819012345678）。");
                return;
            }
        }
        try {
            LineLoginClient client = createClient();
            var attempt = LineLoginSupport.begin(role, phone);
            String url = client.authorizationUrl(attempt.state(), attempt.nonce(), attempt.codeVerifier());
            session.setAttribute(LineLoginSupport.ATTEMPT_KEY, attempt);
            res.sendRedirect(url);
        } catch (IllegalStateException | IllegalArgumentException e) {
            LineLoginSupport.error(req, res, role != null,
                    "LINEログインの設定が完了していません。管理者にお問い合わせください。");
        }
    }
}
