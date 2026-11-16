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

/** LINE認証の一時情報、ログインセッション、登録画面の共通処理。 */
final class LineLoginSupport {
    // 認証開始時の一時情報を保存するセッションキー。
    static final String ATTEMPT_KEY = "lineLoginAttempt";
    // 登録フォーム用のCSRFトークンを保存するセッションキー。
    static final String REGISTRATION_CSRF_KEY = "registrationCsrfToken";
    // 新規登録画面で選択できる利用区分。
    static final Set<String> REGISTRATION_ROLES = Set.of("resident", "business", "owner");
    // 認証開始からコールバックまでに許容する最大秒数。
    private static final long ATTEMPT_MAX_AGE_SECONDS = 600;
    // 認証用の値やCSRFトークンを作るための乱数生成器。
    private static final SecureRandom RANDOM = new SecureRandom();

    private LineLoginSupport() { }

    /** 本人確認済みで利用可能な会員だけを、セッションから取得する。 */
    static User authenticatedUser(HttpSession session) {
        // userはログイン時にセッションへ保存された会員情報。
        if (session != null && session.getAttribute("user") instanceof User user
                && available(user)) {
            return user;
        }
        return null;
    }

    /** 退会しておらず、本登録前または本登録済みの会員かを判定する。 */
    static boolean available(User user) {
        return user != null && user.getWithdrawnAt() == null
                && ("ACTIVE".equals(user.getAccountStatus()) || "PENDING".equals(user.getAccountStatus()));
    }

    /** ログイン済みの入口アクセスをトップへ返す。登録画面では本登録前の選び直しを受け付ける。 */
    static boolean redirectAuthenticated(HttpServletRequest request, HttpServletResponse response,
            boolean allowPendingRegistration) throws Exception {
        // ログイン状態に応じた応答をキャッシュさせない。
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false); // リクエストに紐づく既存セッション。
        User user = authenticatedUser(session); // ログイン済みで利用可能な会員。

        // デモログインでは会員情報を保存せず、許可された利用区分だけを持つ。
        // signedInAsDemoはデモログイン中ならtrue。roleはセッション上の利用区分。
        boolean signedInAsDemo = session != null && session.getAttribute("user") == null
                && session.getAttribute("userRole") instanceof String role
                && (REGISTRATION_ROLES.contains(role) || "admin".equals(role));

        // 本登録前の会員が登録画面で区分を選び直せるようにする。
        // トップへ遷移させる会員ならtrue。
        boolean redirectMemberToTop = user != null
                && (!allowPendingRegistration || "ACTIVE".equals(user.getAccountStatus()));
        if (signedInAsDemo || redirectMemberToTop) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return true;
        }
        return false;
    }

    /** ブラウザーで区分選択画面へ戻っても使えるよう、登録中のトークンを保持する。 */
    static String registrationCsrfToken(HttpSession session) {
        // 同時に画面を開いてもトークンが二重に作られないようにする。
        synchronized (session) {
            if (!(session.getAttribute(REGISTRATION_CSRF_KEY) instanceof String)) {
                session.setAttribute(REGISTRATION_CSRF_KEY, randomHex());
            }
            return (String) session.getAttribute(REGISTRATION_CSRF_KEY);
        }
    }

    /** 登録フォームから送信されたトークンを、セッションの値と照合する。 */
    static boolean validRegistrationCsrf(HttpSession session, String submittedToken) {
        // expectedTokenはセッションに保存した照合用のCSRFトークン。
        if (session == null || !(session.getAttribute(REGISTRATION_CSRF_KEY) instanceof String expectedToken)) {
            return false;
        }
        return matchesSecret(expectedToken, submittedToken);
    }

    /** 初回表示とエラー再表示で、現在の区分・本人確認状態・送信トークンをそろえる。 */
    static void prepareRegistration(HttpServletRequest request) {
        // 送信された区分を優先し、未指定や無効なら会員の区分か住民を初期値にする。
        User user = authenticatedUser(request.getSession(false)); // 現在ログイン中の会員。
        String role = request.getParameter("regRole"); // 登録画面で選択状態にする利用区分。
        if (role == null || !REGISTRATION_ROLES.contains(role)) {
            role = user == null ? "resident" : user.getUserRole().toLowerCase(Locale.ROOT);
        }

        // 登録画面の表示とフォームの送信に必要な値を渡す。
        request.setAttribute("registrationRole", role);
        request.setAttribute("registrationPending", user != null && "PENDING".equals(user.getAccountStatus()));
        request.setAttribute("csrfToken", registrationCsrfToken(request.getSession()));
    }

    /** セッションを作り直し、会員情報と今回LINEから取得した値を保存する。 */
    static void signIn(HttpServletRequest request, User user, String lineDisplayName, String lineEmail) {
        // 登録用トークンを取り出してから、現在のセッションを破棄する。
        HttpSession previousSession = request.getSession(false); // ログイン確定前のセッション。
        // 本登録前の会員に引き継ぐ登録用CSRFトークン。
        Object registrationToken = previousSession == null ? null : previousSession.getAttribute(REGISTRATION_CSRF_KEY);
        if (previousSession != null) {
            previousSession.invalidate();
        }

        // 新しいセッションに会員情報と本登録の状態を保存する。
        HttpSession session = request.getSession(true); // ログイン確定後のセッション。
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userRole", user.getUserRole().toLowerCase(Locale.ROOT));
        session.setAttribute("userName", user.getDisplayName());
        session.setAttribute("userEmail", user.getEmail() == null ? "" : user.getEmail());
        session.setAttribute("isOnboarded", "ACTIVE".equals(user.getAccountStatus()));

        // 今回LINEから取得した情報は、会員情報とは別に保持する。
        session.setAttribute("lineDisplayName", lineDisplayName);
        session.setAttribute("lineEmail", lineEmail == null ? "" : lineEmail);

        // 本登録前は、ブラウザーで区分選択に戻ったときも同じトークンを使えるようにする。
        // tokenは旧セッションから取り出した文字列のトークン。
        if ("PENDING".equals(user.getAccountStatus()) && registrationToken instanceof String token) {
            session.setAttribute(REGISTRATION_CSRF_KEY, token);
        }
    }

    static String registrationDestination(User user) {
        // 本登録前の会員は区分別の初期設定画面へ、それ以外はトップへ案内する。
        if (!"PENDING".equals(user.getAccountStatus())) {
            return "/index.jsp";
        }
        // 会員の利用区分に対応する初期設定アクション名。
        String action = switch (user.getUserRole()) {
            case "RESIDENT" -> "ResidentSetup";
            case "BUSINESS" -> "BusinessSetup";
            case "OWNER" -> "OwnerSetup";
            default -> throw new IllegalArgumentException("利用区分を選び直してください。");
        };
        return "/jp/machiriku/servlet/auth/" + action + ".action";
    }

    /** 認証ごとにstate・nonce・PKCE検証値を作り、登録区分と開始時刻を記録する。 */
    static Attempt begin(String registrationRole) {
        // 照合に使う乱数を、URLで扱える形式にする。
        // 認可コードの交換時に送るPKCE検証値。
        String codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes());
        String state = randomHex(); // コールバックが今回の認証に対応するかを確認する値。
        String nonce = randomHex(); // IDトークンが今回の認証に対応するかを確認する値。
        return new Attempt(state, nonce, codeVerifier, registrationRole, Instant.now().getEpochSecond());
    }

    /** セッションに対応するstateと10分の有効期限を確認し、成功した認証情報を一度だけ取り出す。 */
    static Attempt consume(HttpSession session, String callbackState) {
        // 認証開始時のセッションがあることを確認する。
        if (session == null) {
            return null;
        }
        // 同じ認証情報を二重に使えないよう、検証と削除を同じロック内で行う。
        synchronized (session) {
            // attemptは認証開始時に保存した一時情報。
            if (!(session.getAttribute(ATTEMPT_KEY) instanceof Attempt attempt)) {
                return null;
            }

            // 有効期間を外れた認証情報を破棄する。
            // 認証開始からの経過秒数。
            long elapsedSeconds = Instant.now().getEpochSecond() - attempt.createdAt();
            if (elapsedSeconds < 0 || elapsedSeconds > ATTEMPT_MAX_AGE_SECONDS) {
                session.removeAttribute(ATTEMPT_KEY);
                return null;
            }
            // 受け取ったstateを照合し、不一致なら現在の認証情報を残す。
            if (!matchesSecret(attempt.state(), callbackState)) {
                return null;
            }

            // 照合に成功した認証情報をセッションから取り除く。
            session.removeAttribute(ATTEMPT_KEY);
            return attempt;
        }
    }

    /** 呼び出し元が用意した案内文を、登録またはログイン画面に表示する。 */
    static void error(HttpServletRequest request, HttpServletResponse response, boolean registration,
            String message) throws Exception {
        // 認証済みセッションには入口と同じ遷移ルールを適用する。
        if (redirectAuthenticated(request, response, registration)) {
            return;
        }

        // 案内文と再表示に必要な値を、ログイン画面または登録画面へ渡す。
        request.setAttribute("errorMessage", message);
        if (registration) {
            prepareRegistration(request);
        }
        // 案内文を表示するJSPのパス。
        String view = registration ? "/WEB-INF/views/auth/register.jsp" : "/WEB-INF/views/auth/login.jsp";
        request.getRequestDispatcher(view).forward(request, response);
    }

    // 比較時間から値を推測されにくい方法で照合する。
    private static boolean matchesSecret(String expected, String received) {
        return received != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8));
    }

    private static String randomHex() {
        return HexFormat.of().formatHex(randomBytes());
    }

    private static byte[] randomBytes() {
        // 乱数を格納する32バイトの配列。
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        return random;
    }

    /**
     * 認証開始からコールバックまで、サーバーセッション内だけで保持する一時情報。
     *
     * @param state コールバックを認証開始時のリクエストと照合する値
     * @param nonce IDトークンを認証開始時のリクエストと照合する値
     * @param codeVerifier 認可コードの交換時に使うPKCE検証値
     * @param registrationRole 登録時に選択した利用区分。通常ログインではnull
     * @param createdAt 認証を開始したUNIX時刻（秒）
     */
    record Attempt(String state, String nonce, String codeVerifier, String registrationRole,
            long createdAt) implements Serializable { }
}
