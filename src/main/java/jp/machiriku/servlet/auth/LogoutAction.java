package jp.machiriku.servlet.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.tool.Action;

/**
 * 会員およびデモ利用者のログアウト処理を担当し、POSTとCSRFトークンを検証した上でセッション情報を破棄する。
 */
public class LogoutAction extends Action {
    public static final String CSRF_KEY = "logoutCsrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** セッションごとのログアウト用CSRFトークンを発行・取得する。 */
    public static String csrfToken(HttpSession session) {
        if (session == null) {
            return "";
        }
        synchronized (session) {
            if (!(session.getAttribute(CSRF_KEY) instanceof String token) || token.isEmpty()) {
                byte[] random = new byte[32];
                RANDOM.nextBytes(random);
                String created = HexFormat.of().formatHex(random);
                session.setAttribute(CSRF_KEY, created);
                return created;
            }
            return (String) session.getAttribute(CSRF_KEY);
        }
    }

    /** 送信されたCSRFトークンをセッション内の有効なトークンと定数時間で照合する。 */
    static boolean validCsrf(HttpSession session, String submitted) {
        if (session == null || submitted == null || submitted.isBlank()) {
            return false;
        }
        byte[] actual = submitted.getBytes(StandardCharsets.UTF_8);
        for (String key : new String[] { CSRF_KEY, "profileCsrfToken", OnboardingSupport.CSRF_KEY, LineLoginSupport.REGISTRATION_CSRF_KEY }) {
            if (session.getAttribute(key) instanceof String expected && !expected.isEmpty()
                    && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        HttpSession session = req.getSession(false);
        if (session != null) {
            if (!validCsrf(session, req.getParameter("csrfToken"))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            session.invalidate();
        }
        String next = req.getParameter("next");
        if ("login".equals(next)) {
            res.sendRedirect(req.getContextPath() + "/jp/machiriku/servlet/auth/Login.action");
        } else if ("register".equals(next)) {
            res.sendRedirect(req.getContextPath() + "/jp/machiriku/servlet/auth/Register.action");
        } else {
            res.sendRedirect(req.getContextPath() + "/index.jsp?logged_out=1");
        }
    }
}
