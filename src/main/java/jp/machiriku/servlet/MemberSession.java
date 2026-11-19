package jp.machiriku.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.User;
import jp.machiriku.dao.ProfileDao;

/** プロフィール・初期設定で使う会員状態の確認と、既存セッションの更新。 */
public final class MemberSession {
    private MemberSession() { }

    public static User signedInUser(HttpSession session) {
        if (session == null) { return null; }
        try {
            return session.getAttribute("user") instanceof User user ? user : null;
        } catch (IllegalStateException expired) {
            return null;
        }
    }

    /** セッションで本人を特定し、表示と遷移にはDBの現在の状態を使う。 */
    public static User requireCurrentUser(HttpServletRequest req, HttpServletResponse res,
            HttpSession session, Supplier<ProfileDao> profiles) throws IOException {
        res.setHeader("Cache-Control", "no-store");
        User signedIn = signedInUser(session);
        if (signedIn == null) {
            redirectToLogin(req, res);
            return null;
        }

        User current;
        try {
            current = profiles.get().findUserById(signedIn.getUserId());
        } catch (SQLException e) {
            req.getServletContext().log("Member lookup failed (SQLState=" + e.getSQLState() + ").");
            res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        if (!belongsTo(session, signedIn.getUserId())) {
            redirectToLogin(req, res);
            return null;
        }
        if (current == null || current.getWithdrawnAt() != null
                || (!"PENDING".equals(current.getAccountStatus()) && !"ACTIVE".equals(current.getAccountStatus()))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return current;
    }

    /** DB処理中にログアウトや失効が起きても、開始時のセッション以外に会員情報を書き込まない。 */
    public static boolean update(HttpSession session, User saved, String displayName, Consumer<HttpSession> attributes) {
        if (!belongsTo(session, saved.getUserId())) { return false; }
        try {
            session.setAttribute("user", saved);
            session.setAttribute("userId", saved.getUserId());
            session.setAttribute("userRole", saved.getUserRole().toLowerCase(Locale.ROOT));
            session.setAttribute("userName", displayName);
            session.setAttribute("userEmail", saved.getEmail() == null ? "" : saved.getEmail());
            session.setAttribute("isOnboarded", "ACTIVE".equals(saved.getAccountStatus()));
            attributes.accept(session);
            return true;
        } catch (IllegalStateException expired) {
            // 更新の途中で無効になったセッションには、それ以上書き込めない。
            return false;
        }
    }

    public static void redirectToLogin(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.sendRedirect(req.getContextPath() + "/jp/machiriku/servlet/auth/Login.action");
    }

    private static boolean belongsTo(HttpSession session, int userId) {
        User signedIn = signedInUser(session);
        return signedIn != null && signedIn.getUserId() == userId;
    }
}
