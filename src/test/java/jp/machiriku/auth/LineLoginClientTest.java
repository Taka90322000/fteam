package jp.machiriku.auth;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

/** 偽のHTTP応答を使い、LINE認証の送信内容と本人情報の検証を確認する。 */
public class LineLoginClientTest {
    private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String NONCE = "nonce-for-this-login";
    private static final ObjectMapper JSON = new ObjectMapper();
    private FakeHttpClient transport;
    private LineLoginClient client;
    private String tokenResponse;
    private String verificationResponse;
    private int tokenStatus;
    private int verificationStatus;
    private Request tokenRequest;
    private Request verificationRequest;

    @Before
    // LINEへ実際に接続せず、認可コード交換と本人情報検証の応答を用意する。
    public void setUpTransport() throws Exception {
        tokenResponse = "{\"id_token\":\"opaque-id-token\",\"scope\":\"openid profile\"}";
        verificationResponse = JSON.writeValueAsString(validClaims());
        tokenStatus = 200;
        verificationStatus = 200;
        transport = new FakeHttpClient();
        String base = "https://line-test.example";
        client = new LineLoginClient(new LineLoginConfig("12345", "channel+secret&value",
                "http://localhost:8080/machiriku/callback?source=a&value=日本語"),
                transport, URI.create(base + "/authorize"), URI.create(base + "/token"),
                URI.create(base + "/verify"));
        prepareResponses();
    }

    @Test
    // メール取得の要求とPKCE値を確認し、秘密鍵や検証値そのものがURLへ出ないことを検証する。
    public void authorizationRequestsEmailAndUsesPkceWithoutExposingSecret() {
        String url = client.authorizationUrl("state&=value", NONCE, VERIFIER);
        Map<String, String> parameters = parse(URI.create(url).getRawQuery());
        assertEquals("openid profile email", parameters.get("scope"));
        assertTrue(url.contains("scope=openid%20profile%20email"));
        assertEquals("code", parameters.get("response_type"));
        assertEquals("12345", parameters.get("client_id"));
        assertEquals("state&=value", parameters.get("state"));
        assertEquals(NONCE, parameters.get("nonce"));
        assertEquals("http://localhost:8080/machiriku/callback?source=a&value=日本語", parameters.get("redirect_uri"));
        assertEquals("S256", parameters.get("code_challenge_method"));
        // RFC 7636の既知の値を使い、SHA-256とBase64URLによる計算結果を確認する。
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", parameters.get("code_challenge"));
        assertFalse(parameters.containsKey("code_verifier"));
        assertFalse(parameters.containsKey("client_secret"));
    }

    @Test
    // 認可コード交換とトークン検証の順番、送信内容、検証後の会員情報を確認する。
    public void exchangesCodeAndVerifiesOpaqueTokenBeforeUsingClaims() throws Exception {
        LineLoginClient.Profile profile = client.authenticate("code+&=日本語", NONCE, VERIFIER);
        assertEquals("表示名", profile.displayName());
        assertEquals("person@example.com", profile.email());
        assertEquals("U123", profile.lineSubject());
        assertEquals("POST", tokenRequest.method());
        assertEquals("application/x-www-form-urlencoded", tokenRequest.contentType());
        assertEquals("authorization_code", tokenRequest.form().get("grant_type"));
        assertEquals("code+&=日本語", tokenRequest.form().get("code"));
        assertEquals("12345", tokenRequest.form().get("client_id"));
        assertEquals("channel+secret&value", tokenRequest.form().get("client_secret"));
        assertEquals(VERIFIER, tokenRequest.form().get("code_verifier"));
        assertEquals("POST", verificationRequest.method());
        assertEquals("application/x-www-form-urlencoded", verificationRequest.contentType());
        assertEquals(Map.of("id_token", "opaque-id-token", "client_id", "12345", "nonce", NONCE), verificationRequest.form());
    }

    @Test
    // メール未取得・null・空白などを未設定として扱い、認証を続けられることを確認する。
    public void acceptsMissingNullBlankAndNonTextEmailAsUnavailable() throws Exception {
        Map<String, Object> claims = validClaims();
        claims.remove("email");
        verificationResponse = JSON.writeValueAsString(claims);
        prepareResponses();
        assertNull(client.authenticate("code", NONCE, VERIFIER).email());
        for (Object unavailable : new Object[] { null, "", " ", "\t\r\n", 123, false, Map.of("value", "not-a-string") }) {
            claims.put("email", unavailable);
            verificationResponse = JSON.writeValueAsString(claims);
            prepareResponses();
            LineLoginClient.Profile profile = client.authenticate("code", NONCE, VERIFIER);
            assertNull(profile.email());
            assertEquals("表示名", profile.displayName());
            assertEquals("U123", profile.lineSubject());
        }
    }

    @Test
    // 取得できたメールアドレスを、アプリ側で勝手に書き換えないことを確認する。
    public void preservesProvidedEmailWithoutChangingCaseOrAddressingDetails() throws Exception {
        Map<String, Object> claims = validClaims();
        claims.put("email", "Member.Name+LINE@Example.COM");
        verificationResponse = JSON.writeValueAsString(claims);
        prepareResponses();
        assertEquals("Member.Name+LINE@Example.COM", client.authenticate("code", NONCE, VERIFIER).email());
    }

    @Test
    // メールがなくても、会員識別に必須のLINE IDと表示名は引き続き検証する。
    public void unavailableEmailDoesNotMakeRequiredIdentityClaimsOptional() throws Exception {
        for (String key : new String[] { "sub", "name" }) {
            for (Object invalid : new Object[] { null, "", " \t", 123 }) {
                Map<String, Object> claims = validClaims();
                claims.remove("email");
                claims.put(key, invalid);
                verificationResponse = JSON.writeValueAsString(claims);
                failure();
            }
        }
    }

    @Test
    // 別の認証要求・チャネル・発行元や、期限切れのトークンを拒否することを確認する。
    public void rejectsWrongNonceAudienceIssuerAndExpiredToken() throws Exception {
        for (Map.Entry<String, Object> invalid : Map.<String, Object>of(
                "nonce", "other-login", "aud", "other-channel", "iss", "https://attacker.example",
                "exp", Instant.now().minusSeconds(1).getEpochSecond()).entrySet()) {
            Map<String, Object> claims = validClaims();
            claims.put(invalid.getKey(), invalid.getValue());
            verificationResponse = JSON.writeValueAsString(claims);
            assertEquals("LINE identity verification failed.", failure().getMessage());
        }
    }

    @Test
    // 本人情報の必須項目欠落や、不正な型を正しく拒否することを確認する。
    public void rejectsAbsentAndWronglyTypedClaims() throws Exception {
        for (String key : Arrays.asList("iss", "aud", "nonce", "exp", "sub", "name")) {
            Map<String, Object> claims = validClaims();
            claims.remove(key);
            verificationResponse = JSON.writeValueAsString(claims);
            failure();
        }
        Map<String, Object> claims = validClaims();
        claims.put("exp", Long.toString(Instant.now().plusSeconds(600).getEpochSecond()));
        verificationResponse = JSON.writeValueAsString(claims);
        failure();
    }

    @Test
    // コード交換が失敗したら検証APIへ進まず、応答中の秘密情報も例外に残さないことを確認する。
    public void rejectsTokenFailureWithoutCallingVerificationOrLeakingResponse() throws Exception {
        tokenStatus = 400;
        tokenResponse = "{\"error\":\"invalid_grant\",\"error_description\":\"sensitive-value\"}";
        IOException error = failure();
        assertFalse(error.toString().contains("sensitive-value"));
        assertNull(error.getCause());
        assertNull(verificationRequest);
    }

    @Test
    // 検証APIの失敗や壊れたJSONで認証を止め、トークンを例外へ含めないことを確認する。
    public void rejectsVerificationFailureAndMalformedResponseWithoutLeakingTokens() throws Exception {
        verificationStatus = 401;
        verificationResponse = "opaque-id-token";
        assertFalse(failure().toString().contains("opaque-id-token"));
        verificationStatus = 200;
        assertNull(failure().getCause());
        verificationResponse = "{\"error\":\"bad-token\"}";
        failure();
    }

    @Test
    // IDトークンが返されない場合は、本人確認を開始できないことを確認する。
    public void rejectsMissingIdTokenWithoutCallingVerification() throws Exception {
        tokenResponse = "{\"access_token\":\"not-an-id-token\"}";
        assertEquals("LINE did not return an ID token.", failure().getMessage());
        assertNull(verificationRequest);
    }

    @Test
    // 不正なコールバックURLやPKCE値を拒否し、設定の文字列表現から秘密値を隠すことを確認する。
    public void rejectsInvalidConfigurationAndRedactsSecret() {
        for (String uri : Arrays.asList("http://example.com/callback", "//example.com/callback",
                "https://example.com/callback#fragment", "https://user@example.com/callback", "not a URL")) {
            assertThrows(IllegalArgumentException.class, () -> new LineLoginConfig("123", "private-secret", uri));
        }
        assertFalse(new LineLoginConfig("123", "private-secret", "https://example.com/callback")
                .toString().contains("private-secret"));
        assertThrows(IllegalArgumentException.class, () -> client.authorizationUrl("state", NONCE, "too-short"));
    }

    private IOException failure() throws Exception {
        prepareResponses();
        return assertThrows(IOException.class, () -> client.authenticate("code", NONCE, VERIFIER));
    }

    private void prepareResponses() {
        transport.replies.clear();
        transport.replies.add(new Reply("/token", tokenStatus, tokenResponse));
        transport.replies.add(new Reply("/verify", verificationStatus, verificationResponse));
    }

    private static Map<String, Object> validClaims() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("iss", "https://access.line.me");
        result.put("aud", "12345");
        result.put("nonce", NONCE);
        result.put("exp", Instant.now().plusSeconds(600).getEpochSecond());
        result.put("sub", "U123");
        result.put("name", "表示名");
        result.put("email", "person@example.com");
        return result;
    }

    private static Request capture(HttpRequest request) {
        // 実際のBodyPublisherから本文を取り出し、本番コードが送るバイト列を検証する。
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompletableFuture<Void> complete = new CompletableFuture<>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            @Override public void onNext(ByteBuffer buffer) {
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }
            @Override public void onError(Throwable error) { complete.completeExceptionally(error); }
            @Override public void onComplete() { complete.complete(null); }
        });
        complete.join();
        return new Request(request.method(), request.headers().firstValue("Content-Type").orElse(null),
                parse(bytes.toString(StandardCharsets.UTF_8)));
    }

    private static Map<String, String> parse(String form) {
        return Arrays.stream(form.split("&")).map(pair -> pair.split("=", 2)).collect(Collectors.toMap(
                pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
    }

    private final class FakeHttpClient extends HttpClient {
        private final Deque<Reply> replies = new ArrayDeque<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
            Reply reply = replies.pollFirst();
            assertNotNull("Unexpected HTTP request", reply);
            assertEquals(reply.path(), request.uri().getPath());
            assertEquals("https", request.uri().getScheme());
            assertEquals("line-test.example", request.uri().getHost());
            assertEquals("application/json", request.headers().firstValue("Accept").orElse(null));
            assertTrue(request.timeout().orElseThrow().compareTo(Duration.ofSeconds(15)) <= 0);
            if ("/token".equals(reply.path())) { tokenRequest = capture(request); }
            else { verificationRequest = capture(request); }
            return new Response<>(reply.status(), (T) reply.body(), request);
        }

        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { throw new UnsupportedOperationException(); }
        @Override public SSLParameters sslParameters() { return new SSLParameters(); }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                HttpResponse.BodyHandler<T> handler) { throw new UnsupportedOperationException(); }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                HttpResponse.BodyHandler<T> handler, HttpResponse.PushPromiseHandler<T> pushHandler) {
            throw new UnsupportedOperationException();
        }
    }

    private record Reply(String path, int status, String body) { }
    private record Response<T>(int statusCode, T body, HttpRequest request) implements HttpResponse<T> {
        @Override public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (name, value) -> true); }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return request.uri(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }

    private record Request(String method, String contentType, Map<String, String> form) { }
}
