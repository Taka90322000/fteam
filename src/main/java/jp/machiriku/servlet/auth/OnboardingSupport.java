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
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.User;
import jp.machiriku.dao.ProfileDao;
import jp.machiriku.servlet.MemberSession;

/** 初期設定の本人確認・CSRF対策と、フォーム表示に必要な共通処理をまとめる。 */
final class OnboardingSupport {
    static final String CSRF_KEY = "onboardingCsrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private OnboardingSupport() { }

    static User requireUser(HttpServletRequest req, HttpServletResponse res, HttpSession session,
            String role, Supplier<ProfileDao> profiles) throws Exception {
        User user = MemberSession.requireCurrentUser(req, res, session, profiles);
        if (user == null) { return null; }
        if (!role.equals(user.getUserRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        // 本登録後の初期設定は表示・保存せず、GETでは現在のプロフィール編集へ案内する。
        if ("ACTIVE".equals(user.getAccountStatus())) {
            if ("GET".equalsIgnoreCase(req.getMethod())) {
                res.sendRedirect(req.getContextPath() + "/jp/machiriku/servlet/user/Profile.action");
            } else {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
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

    static boolean validCsrf(HttpServletRequest req, HttpSession session) {
        try {
            if (!(session.getAttribute(CSRF_KEY) instanceof String expected)) { return false; }
            String actual = req.getParameter("csrfToken");
            return actual != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalStateException expired) {
            return false;
        }
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
        try {
            if (session != null && session.getAttribute(key) instanceof String text) { return text; }
        } catch (IllegalStateException expired) {
            // 読み込み中に失効した場合は画面を転送する前にログインへ戻す。
        }
        return fallback == null ? "" : fallback;
    }

    static void completed(HttpServletRequest req, HttpServletResponse res, HttpSession session,
            User saved, String displayName, Consumer<HttpSession> attributes) throws Exception {
        if (MemberSession.update(session, saved, displayName, current -> {
            current.removeAttribute(CSRF_KEY);
            current.removeAttribute(LineLoginSupport.REGISTRATION_CSRF_KEY);
            attributes.accept(current);
        })) {
            res.sendRedirect(req.getContextPath() + "/index.jsp?onboarded=1");
        } else {
            MemberSession.redirectToLogin(req, res);
        }
    }

    static boolean prepareView(HttpServletRequest req, HttpServletResponse res, HttpSession session) throws Exception {
        try {
            req.setAttribute("csrfToken", csrfToken(session));
        } catch (IllegalStateException expired) {
            MemberSession.redirectToLogin(req, res);
            return false;
        }
        return true;
    }

    static void logDatabaseError(HttpServletRequest req, java.sql.SQLException error) {
        // 例外の本文には個人情報が含まれる場合があるため、SQLStateだけを記録する。
        req.getServletContext().log("Profile registration failed (SQLState=" + error.getSQLState() + ").");
    }
}
