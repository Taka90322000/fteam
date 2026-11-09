package jp.machiriku.servlet.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
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

/** 初期設定の本人確認・CSRF対策と、フォーム表示に必要な共通処理をまとめる。 */
final class OnboardingSupport {
    static final String CSRF_KEY = "onboardingCsrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private OnboardingSupport() { }

    static User requireUser(HttpServletRequest req, HttpServletResponse res, String role) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof User user)) {
            res.sendRedirect(req.getContextPath() + "/jp/machiriku/servlet/auth/Login.action");
            return null;
        }
        if (!role.equals(user.getUserRole()) || user.getWithdrawnAt() != null
                || (!"PENDING".equals(user.getAccountStatus()) && !"ACTIVE".equals(user.getAccountStatus()))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
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

    static Set<String> selection(HttpServletRequest req, String name) { return new LinkedHashSet<>(values(req, name)); }

    static String lineValue(HttpSession session, String key, String fallback) {
        Object value = session.getAttribute(key);
        return value instanceof String text ? text : (fallback == null ? "" : fallback);
    }

    static void completed(HttpServletRequest req, User saved, String displayName) {
        HttpSession session = req.getSession();
        session.setAttribute("user", saved);
        session.setAttribute("userId", saved.getUserId());
        session.setAttribute("userRole", saved.getUserRole().toLowerCase(java.util.Locale.ROOT));
        session.setAttribute("userName", displayName);
        session.setAttribute("userEmail", saved.getEmail() == null ? "" : saved.getEmail());
        session.setAttribute("isOnboarded", true);
        session.removeAttribute(CSRF_KEY);
        session.removeAttribute(LineLoginSupport.REGISTRATION_CSRF_KEY);
    }

    static void logDatabaseError(HttpServletRequest req, java.sql.SQLException error) {
        // 例外の本文には個人情報が含まれる場合があるため、SQLStateだけを記録する。
        req.getServletContext().log("Profile registration failed (SQLState=" + error.getSQLState() + ").");
    }
}
