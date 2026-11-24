package jp.machiriku.servlet.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
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
import jp.machiriku.servlet.ProfileIconSupport;

/** プロフィール設定・変更の本人確認・CSRF対策およびフォーム共通処理。 */
final class ProfileSupport {
    static final String CSRF_KEY = "profileCsrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private ProfileSupport() { }

    static User requireActiveUser(HttpServletRequest req, HttpServletResponse res,
            HttpSession session, Supplier<ProfileDao> profiles) throws Exception {
        User user = MemberSession.requireCurrentUser(req, res, session, profiles);
        if (user == null) { return null; }
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

    /**
     * 業種・資格の選択値を保存処理へ渡す。
     * @param req フォームのリクエスト
     * @param name 選択欄の名前
     * @return 選択されたIDの文字列
     */
    static List<String> values(HttpServletRequest req, String name) { return ProfileIconSupport.selectionValues(req, name); }

    static Set<String> selection(HttpServletRequest req, String name) {
        return new LinkedHashSet<>(values(req, name));
    }

    static void saved(HttpServletRequest req, HttpServletResponse res, HttpSession session,
            User saved, String displayName, Consumer<HttpSession> attributes) throws Exception {
        if (MemberSession.update(session, saved, displayName, attributes)) {
            res.sendRedirect(req.getContextPath() + "/index.jsp?profile_saved=1");
        } else {
            MemberSession.redirectToLogin(req, res);
        }
    }

    static boolean prepareView(HttpServletRequest req, HttpServletResponse res, HttpSession session, User user) throws Exception {
        if ("GET".equalsIgnoreCase(req.getMethod())) {
            String displayName = (String) req.getAttribute("displayName");
            if (displayName == null || displayName.isBlank()) { displayName = user.getDisplayName(); }
            if ("RESIDENT".equals(user.getUserRole())) { user.setDisplayName(displayName); }
            if (req.getAttribute("email") instanceof String email) {
                user.setEmail(email.isEmpty() ? null : email);
            }
            if (!MemberSession.update(session, user, displayName, current -> { })) {
                MemberSession.redirectToLogin(req, res);
                return false;
            }
        }
        try {
            req.setAttribute("csrfToken", csrfToken(session));
        } catch (IllegalStateException expired) {
            MemberSession.redirectToLogin(req, res);
            return false;
        }
        return true;
    }

    static void logDatabaseError(HttpServletRequest req, SQLException error) {
        req.getServletContext().log("Profile update failed (SQLState=" + error.getSQLState() + ").");
    }
}
