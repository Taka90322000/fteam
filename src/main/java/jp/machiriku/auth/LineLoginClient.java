package jp.machiriku.auth;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Authorization-code flow with PKCE; identity claims come only from LINE's verification endpoint. */
public class LineLoginClient {
    private static final URI AUTHORIZE = URI.create("https://access.line.me/oauth2/v2.1/authorize");
    private static final URI TOKEN = URI.create("https://api.line.me/oauth2/v2.1/token");
    private static final URI VERIFY = URI.create("https://api.line.me/oauth2/v2.1/verify");
    private static final String ISSUER = "https://access.line.me";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final LineLoginConfig config;
    private final HttpClient client;
    private final URI authorizationEndpoint;
    private final URI tokenEndpoint;
    private final URI verificationEndpoint;

    public LineLoginClient(LineLoginConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build(), AUTHORIZE, TOKEN, VERIFY);
    }

    protected LineLoginClient(LineLoginConfig config, HttpClient client) {
        this(config, client, AUTHORIZE, TOKEN, VERIFY);
    }

    // Endpoint injection is intentionally package-private and used only by local protocol tests.
    LineLoginClient(LineLoginConfig config, HttpClient client, URI authorizationEndpoint,
            URI tokenEndpoint, URI verificationEndpoint) {
        this.config = Objects.requireNonNull(config);
        this.client = Objects.requireNonNull(client);
        this.authorizationEndpoint = Objects.requireNonNull(authorizationEndpoint);
        this.tokenEndpoint = Objects.requireNonNull(tokenEndpoint);
        this.verificationEndpoint = Objects.requireNonNull(verificationEndpoint);
    }

    public String authorizationUrl(String state, String nonce, String codeVerifier) {
        if (state == null || state.isBlank() || nonce == null || nonce.isBlank() || !validVerifier(codeVerifier)) {
            throw new IllegalArgumentException("A state, nonce, and valid PKCE verifier are required.");
        }
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("response_type", "code");
        parameters.put("client_id", config.channelId());
        parameters.put("redirect_uri", config.redirectUri());
        parameters.put("scope", "openid profile email");
        parameters.put("state", state);
        parameters.put("nonce", nonce);
        parameters.put("code_challenge", challenge(codeVerifier));
        parameters.put("code_challenge_method", "S256");
        return authorizationEndpoint + "?" + form(parameters).replace("+", "%20");
    }

    public Profile authenticate(String code, String nonce, String codeVerifier) throws IOException, InterruptedException {
        if (code == null || code.isBlank() || nonce == null || nonce.isBlank() || !validVerifier(codeVerifier)) {
            throw new IOException("LINE login request is incomplete.");
        }
        Map<String, String> exchange = new LinkedHashMap<>();
        exchange.put("grant_type", "authorization_code");
        exchange.put("code", code);
        exchange.put("redirect_uri", config.redirectUri());
        exchange.put("client_id", config.channelId());
        exchange.put("client_secret", config.channelSecret());
        exchange.put("code_verifier", codeVerifier);
        JsonNode token = post(tokenEndpoint, exchange);
        String idToken = requiredText(token, "id_token", "LINE did not return an ID token.");

        JsonNode verified = post(verificationEndpoint,
                Map.of("id_token", idToken, "client_id", config.channelId(), "nonce", nonce));
        if (!ISSUER.equals(text(verified, "iss")) || !config.channelId().equals(text(verified, "aud"))
                || !nonce.equals(text(verified, "nonce")) || !verified.path("exp").isIntegralNumber()
                || !verified.path("exp").canConvertToLong()
                || verified.path("exp").longValue() <= Instant.now().getEpochSecond()) {
            throw new IOException("LINE identity verification failed.");
        }
        String subject = requiredText(verified, "sub", "LINE did not return a user identifier.");
        String name = requiredText(verified, "name", "LINE did not return a display name.");
        String email = text(verified, "email");
        if (email == null || email.isBlank()) { email = null; }
        return new Profile(name, email, subject);
    }

    private JsonNode post(URI endpoint, Map<String, String> parameters) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form(parameters), StandardCharsets.UTF_8)).build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // Do not attach an exception containing provider response bodies or submitted tokens.
            throw new IOException("LINE authentication service could not be reached.");
        }
        if (response.statusCode() != 200) {
            throw new IOException("LINE authentication service rejected the request.");
        }
        try {
            JsonNode data = JSON.readTree(response.body());
            if (data == null || !data.isObject() || data.has("error")) {
                throw new IOException("LINE authentication service returned an invalid response.");
            }
            return data;
        } catch (IOException e) {
            // Jackson parse errors can include fragments of a response containing credentials.
            throw new IOException("LINE authentication service returned an invalid response.");
        }
    }

    private static String text(JsonNode object, String key) {
        JsonNode value = object.path(key);
        return value.isTextual() ? value.textValue() : null;
    }

    private static String requiredText(JsonNode object, String key, String message) throws IOException {
        String value = text(object, key);
        if (value == null || value.isBlank()) {
            throw new IOException(message);
        }
        return value;
    }

    private static boolean validVerifier(String verifier) {
        return verifier != null && verifier.matches("[A-Za-z0-9._~-]{43,128}");
    }

    private static String challenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.");
        }
    }

    private static String form(Map<String, String> parameters) {
        return parameters.entrySet().stream().map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
    }

    public record Profile(String displayName, String email, String lineSubject) { }
}
