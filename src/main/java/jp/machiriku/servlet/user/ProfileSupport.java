package jp.machiriku.servlet.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.User;

/** プロフィール設定・変更の本人確認・CSRF対策およびフォーム共通処理。 */
final class ProfileSupport {
    static final String CSRF_KEY = "profileCsrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private ProfileSupport() { }

    static User requireActiveUser(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof User user)) {
            res.sendRedirect(req.getContextPath() + "/jp/machiriku/servlet/auth/Login.action");
            return null;
        }
        if (user.getWithdrawnAt() != null) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        // 初期設定がまだ完了していない会員（PENDING）は初期設定画面へ案内する。
        if ("PENDING".equals(user.getAccountStatus())) {
            String targetAction = switch (user.getUserRole()) {
                case "BUSINESS" -> "BusinessSetup";
                case "OWNER" -> "OwnerSetup";
                default -> "ResidentSetup";
            };
            res.sendRedirect(req.getContextPath() + "/jp/machiriku/servlet/auth/" + targetAction + ".action");
            return null;
        }
        return user;
    }

    static String csrfToken(HttpSession session) {
        synchronized (session) {
            if (!(session.getAttribute(CSRF_KEY) instanceof String)) {
                byte[] random = new byte[32];
                RANDOM.nextBytes(random);
                session.setAttribute(CSRF_KEY, HexFormat.of().formatHex(random));
            }
            return (String) session.getAttribute(CSRF_KEY);
        }
    }

    static boolean validCsrf(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute(CSRF_KEY) instanceof String expected)) { return false; }
        String actual = req.getParameter("csrfToken");
        return actual != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    static Map<String, String> fields(HttpServletRequest req, String... names) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String name : names) {
            String value = req.getParameter(name);
            if ("emailNotify".equals(name) && value == null) { continue; }
            fields.put(name, value == null ? "" : value);
        }
        return fields;
    }

    static List<String> values(HttpServletRequest req, String name) {
        String[] values = req.getParameterValues(name);
        return values == null ? List.of() : Arrays.asList(values);
    }

    static Set<String> selection(HttpServletRequest req, String name) {
        return new LinkedHashSet<>(values(req, name));
    }

    static void updateSession(HttpServletRequest req, User saved, String displayName) {
        HttpSession session = req.getSession();
        session.setAttribute("user", saved);
        session.setAttribute("userId", saved.getUserId());
        session.setAttribute("userRole", saved.getUserRole().toLowerCase(java.util.Locale.ROOT));
        session.setAttribute("userName", displayName);
        session.setAttribute("userEmail", saved.getEmail() == null ? "" : saved.getEmail());
    }

    static void logDatabaseError(HttpServletRequest req, SQLException error) {
        req.getServletContext().log("Profile update failed (SQLState=" + error.getSQLState() + ").");
    }
}
