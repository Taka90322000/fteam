package jp.machiriku.servlet.auth;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** LINE認証の一時情報、stateの検証、エラー画面への遷移を共通化する。 */
final class LineLoginSupport {
    static final String ATTEMPT_KEY = "lineLoginAttempt";
    static final Set<String> REGISTRATION_ROLES = Set.of("resident", "business", "owner");
    private static final SecureRandom RANDOM = new SecureRandom();

    private LineLoginSupport() { }

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
        request.setAttribute("errorMessage", message);
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
