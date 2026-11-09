package jp.machiriku.servlet.auth;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.User;

/** LINE認証の一時情報、stateの検証、エラー画面への遷移を共通化する。 */
final class LineLoginSupport {
    static final String ATTEMPT_KEY = "lineLoginAttempt";
    static final String REGISTRATION_CSRF_KEY = "registrationCsrfToken";
    static final Set<String> REGISTRATION_ROLES = Set.of("resident", "business", "owner");
    private static final SecureRandom RANDOM = new SecureRandom();

    private LineLoginSupport() { }

    /** 本人確認済みで利用可能な会員だけを、セッションから取得する。 */
    static User authenticatedUser(HttpSession session) {
        if (session != null && session.getAttribute("user") instanceof User user
                && available(user)) {
            return user;
        }
        return null;
    }

    static boolean available(User user) {
        return user != null && user.getWithdrawnAt() == null
                && ("ACTIVE".equals(user.getAccountStatus()) || "PENDING".equals(user.getAccountStatus()));
    }

    /** ログイン済みの入口アクセスをトップへ返す。登録画面では本登録前の選び直しを受け付ける。 */
    static boolean redirectAuthenticated(HttpServletRequest request, HttpServletResponse response,
            boolean allowPendingRegistration) throws Exception {
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false);
        User user = authenticatedUser(session);
        boolean demo = session != null && session.getAttribute("user") == null
                && session.getAttribute("userRole") instanceof String role
                && (REGISTRATION_ROLES.contains(role) || "admin".equals(role));
        if (demo || (user != null && (!allowPendingRegistration || "ACTIVE".equals(user.getAccountStatus())))) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return true;
        }
        return false;
    }

    /** ブラウザーで区分選択画面へ戻っても使えるよう、登録中のトークンを保持する。 */
    static String registrationCsrfToken(HttpSession session) {
        synchronized (session) {
            if (!(session.getAttribute(REGISTRATION_CSRF_KEY) instanceof String)) {
                session.setAttribute(REGISTRATION_CSRF_KEY, randomHex());
            }
            return (String) session.getAttribute(REGISTRATION_CSRF_KEY);
        }
    }

    static boolean validRegistrationCsrf(HttpSession session, String actual) {
        if (session == null || !(session.getAttribute(REGISTRATION_CSRF_KEY) instanceof String expected)) { return false; }
        return actual != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    /** 初回表示とエラー再表示で、現在の区分・本人確認状態・送信トークンをそろえる。 */
    static void prepareRegistration(HttpServletRequest request) {
        User user = authenticatedUser(request.getSession(false));
        String role = request.getParameter("regRole");
        if (!REGISTRATION_ROLES.contains(role == null ? "" : role)) {
            role = user == null ? "resident" : user.getUserRole().toLowerCase(Locale.ROOT);
        }
        request.setAttribute("registrationRole", role);
        request.setAttribute("registrationPending", user != null && "PENDING".equals(user.getAccountStatus()));
        request.setAttribute("csrfToken", registrationCsrfToken(request.getSession()));
    }

    /** LINE取得値と登録トークンを引き継ぎ、前の利用区分の入力情報を新しいセッションへ残さない。 */
    static void signIn(HttpServletRequest request, User user, String lineDisplayName, String lineEmail) {
        HttpSession previous = request.getSession(false);
        Object registrationToken = previous == null ? null : previous.getAttribute(REGISTRATION_CSRF_KEY);
        if (previous != null) { previous.invalidate(); }
        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userRole", user.getUserRole().toLowerCase(Locale.ROOT));
        session.setAttribute("userName", user.getDisplayName());
        session.setAttribute("userEmail", user.getEmail() == null ? "" : user.getEmail());
        session.setAttribute("isOnboarded", "ACTIVE".equals(user.getAccountStatus()));
        session.setAttribute("lineDisplayName", lineDisplayName);
        session.setAttribute("lineEmail", lineEmail == null ? "" : lineEmail);
        if ("PENDING".equals(user.getAccountStatus()) && registrationToken instanceof String token) {
            session.setAttribute(REGISTRATION_CSRF_KEY, token);
        }
    }

    static String registrationDestination(User user) {
        if (!"PENDING".equals(user.getAccountStatus())) { return "/index.jsp"; }
        String action = switch (user.getUserRole()) {
            case "RESIDENT" -> "ResidentSetup";
            case "BUSINESS" -> "BusinessSetup";
            case "OWNER" -> "OwnerSetup";
            default -> throw new IllegalArgumentException("利用区分を選び直してください。");
        };
        return "/jp/machiriku/servlet/auth/" + action + ".action";
    }

    /** 認証ごとに推測困難なstate・nonce・PKCE検証値を作り、登録時の入力と一緒に保持する。 */
    static Attempt begin(String role) {
        byte[] verifier = new byte[32];
        RANDOM.nextBytes(verifier);
        return new Attempt(randomHex(), randomHex(), Base64.getUrlEncoder().withoutPadding()
                .encodeToString(verifier), role, Instant.now().getEpochSecond());
    }

    /** セッションに対応するstateと10分の有効期限を確認し、成功した認証情報を一度だけ取り出す。 */
    static Attempt consume(HttpSession session, String state) {
        if (session == null) { return null; }
        // 同時にコールバックが来ても、同じ認証情報を二重に使わせない。
        synchronized (session) {
            Object value = session.getAttribute(ATTEMPT_KEY);
            if (!(value instanceof Attempt attempt)) { return null; }
            long age = Instant.now().getEpochSecond() - attempt.createdAt();
            if (age < 0 || age > 600) {
                session.removeAttribute(ATTEMPT_KEY);
                return null;
            }
            // 通常の文字列比較ではなく、比較時間による情報漏えいを抑えた方法でstateを照合する。
            if (state == null || !MessageDigest.isEqual(attempt.state().getBytes(StandardCharsets.UTF_8),
                    state.getBytes(StandardCharsets.UTF_8))) { return null; }
            session.removeAttribute(ATTEMPT_KEY);
            return attempt;
        }
    }

    /** 呼び出し元が用意した案内文を、登録またはログイン画面に表示する。 */
    static void error(HttpServletRequest request, HttpServletResponse response, boolean registration,
            String message) throws Exception {
        if (redirectAuthenticated(request, response, registration)) { return; }
        request.setAttribute("errorMessage", message);
        if (registration) { prepareRegistration(request); }
        request.getRequestDispatcher(registration ? "/WEB-INF/views/auth/register.jsp"
                : "/WEB-INF/views/auth/login.jsp").forward(request, response);
    }

    // URLでも扱いやすい16進文字列にし、暗号学的乱数で認証要求ごとに別の値を作る。
    private static String randomHex() {
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        return HexFormat.of().formatHex(random);
    }

    /** 認証開始からコールバックまで、サーバーセッション内だけで保持する一時情報。 */
    record Attempt(String state, String nonce, String codeVerifier, String registrationRole,
            long createdAt) implements Serializable { }
}
