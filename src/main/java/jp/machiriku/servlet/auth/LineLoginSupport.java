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

final class LineLoginSupport {
    static final String ATTEMPT_KEY = "lineLoginAttempt";
    static final Set<String> REGISTRATION_ROLES = Set.of("resident", "business", "owner");
    private static final SecureRandom RANDOM = new SecureRandom();

    private LineLoginSupport() { }

    static Attempt begin(String role, String phone) {
        byte[] verifier = new byte[32];
        RANDOM.nextBytes(verifier);
        return new Attempt(randomHex(), randomHex(), Base64.getUrlEncoder().withoutPadding()
                .encodeToString(verifier), role, phone, Instant.now().getEpochSecond());
    }

    static Attempt consume(HttpSession session, String state) {
        if (session == null) { return null; }
        synchronized (session) {
            Object value = session.getAttribute(ATTEMPT_KEY);
            if (!(value instanceof Attempt attempt)) { return null; }
            long age = Instant.now().getEpochSecond() - attempt.createdAt();
            if (age < 0 || age > 600) {
                session.removeAttribute(ATTEMPT_KEY);
                return null;
            }
            if (state == null || !MessageDigest.isEqual(attempt.state().getBytes(StandardCharsets.UTF_8),
                    state.getBytes(StandardCharsets.UTF_8))) { return null; }
            session.removeAttribute(ATTEMPT_KEY);
            return attempt;
        }
    }

    static void error(HttpServletRequest request, HttpServletResponse response, boolean registration,
            String message) throws Exception {
        request.setAttribute("errorMessage", message);
        request.getRequestDispatcher(registration ? "/WEB-INF/views/auth/register.jsp"
                : "/WEB-INF/views/auth/login.jsp").forward(request, response);
    }

    private static String randomHex() {
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        return HexFormat.of().formatHex(random);
    }

    record Attempt(String state, String nonce, String codeVerifier, String registrationRole,
            String phoneE164, long createdAt) implements Serializable { }
}
