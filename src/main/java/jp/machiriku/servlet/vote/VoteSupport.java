package jp.machiriku.servlet.vote;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.HexFormat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.User;

/** 投票機能の各Actionに共通する、本人確認・CSRF対策・URLの組み立て。 */
final class VoteSupport {
    static final String CSRF_KEY = "voteCsrfToken";
    private static final String BASE = "/jp/machiriku/servlet/vote/";
    private static final SecureRandom RANDOM = new SecureRandom();

    private VoteSupport() { }

    /**
     * ログイン中の有効な地域住民を返す。条件を満たさなければ、ログイン画面・初期設定画面への案内、
     * またはエラー応答を出して null を返す(呼び出し側はそのまま処理を終える)。
     */
    static User requireResident(HttpServletRequest req, HttpServletResponse res) throws Exception {
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
        // 初期設定がまだ完了していない会員は、初期設定画面へ案内する。
        if ("PENDING".equals(user.getAccountStatus())) {
            String target = switch (String.valueOf(user.getUserRole())) {
                case "BUSINESS" -> "BusinessSetup";
                case "OWNER" -> "OwnerSetup";
                default -> "ResidentSetup";
            };
            res.sendRedirect(req.getContextPath() + "/jp/machiriku/servlet/auth/" + target + ".action");
            return null;
        }
        // 投票できるのは、有効な地域住民の会員だけ。
        if (!"RESIDENT".equals(user.getUserRole()) || !"ACTIVE".equals(user.getAccountStatus())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return user;
    }

    /**
     * ログイン中で、初期設定済み・退会していない会員を返す。条件を満たさなければ null(応答は出さない)。
     * 集計の閲覧のように、ログインなしでも見られる画面で「会員ならここまで見せる」を分けるときに使う。
     */
    static User currentActiveMember(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof User user)) { return null; }
        if (user.getWithdrawnAt() != null || !"ACTIVE".equals(user.getAccountStatus())) { return null; }
        return user;
    }

    /** 画面のフォームに埋め込むCSRFトークン。セッションごとに1つ作って使い回す。 */
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

    /** フォームで送られた csrfToken が、セッションのものと一致するか。 */
    static boolean validCsrf(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute(CSRF_KEY) instanceof String expected)) { return false; }
        String actual = req.getParameter("csrfToken");
        return actual != null
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    /** 投票機能のActionのURL(例: /machiriku/jp/machiriku/servlet/vote/VoteForm.action)。 */
    static String url(HttpServletRequest req, String actionName) {
        return req.getContextPath() + BASE + actionName + ".action";
    }

    static void logDatabaseError(HttpServletRequest req, SQLException error) {
        req.getServletContext().log("Vote failed (SQLState=" + error.getSQLState() + ").");
    }

    /** 物件IDなど、1以上の整数のパラメーターを読む。正しくなければ null。 */
    static Integer positiveInt(String raw) {
        if (raw == null) { return null; }
        try {
            int value = Integer.parseInt(raw.strip());
            return value >= 1 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
