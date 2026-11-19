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

/** PKCE付きの認可コード方式でLINE認証を行い、検証済みの本人情報だけを返す。 */
public class LineLoginClient {
    // ブラウザーを誘導するLINEの認可画面URL。
    private static final URI AUTHORIZE = URI.create("https://access.line.me/oauth2/v2.1/authorize");
    // 認可コードをIDトークンなどに交換するLINE APIのURL。
    private static final URI TOKEN = URI.create("https://api.line.me/oauth2/v2.1/token");
    // IDトークンを検証するLINE APIのURL。
    private static final URI VERIFY = URI.create("https://api.line.me/oauth2/v2.1/verify");
    private static final String ISSUER = "https://access.line.me"; // IDトークンに含まれる発行元の期待値。
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15); // APIリクエスト1回の制限時間。
    private static final ObjectMapper JSON = new ObjectMapper(); // APIの応答本文を読み取るJSONパーサー。

    private final LineLoginConfig config;       // チャネルID・シークレット・コールバックURLの設定。
    private final HttpClient client;            // LINE APIとのサーバー間通信に使うHTTPクライアント。
    private final URI authorizationEndpoint;    // このクライアントが使う認可画面URL。
    private final URI tokenEndpoint;            // このクライアントが使うトークン交換先。
    private final URI verificationEndpoint;     // このクライアントが使うIDトークン検証先。

    /** LINE APIへの接続に待ち時間を設定し、リダイレクトの自動追従を無効にする。 */
    public LineLoginClient(LineLoginConfig config) {
        this(config, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build(), AUTHORIZE, TOKEN, VERIFY);
    }

    /** テスト時に実通信せず確認できるよう、HTTPクライアントを差し替える。 */
    protected LineLoginClient(LineLoginConfig config, HttpClient client) {
        this(config, client, AUTHORIZE, TOKEN, VERIFY);
    }

    // 接続先の差し替えは同じパッケージのテストに限定し、本番はLINE公式APIを使う。
    LineLoginClient(LineLoginConfig config, HttpClient client, URI authorizationEndpoint,
            URI tokenEndpoint, URI verificationEndpoint) {
        this.config = Objects.requireNonNull(config);
        this.client = Objects.requireNonNull(client);
        this.authorizationEndpoint = Objects.requireNonNull(authorizationEndpoint);
        this.tokenEndpoint = Objects.requireNonNull(tokenEndpoint);
        this.verificationEndpoint = Objects.requireNonNull(verificationEndpoint);
    }

    /**
     * LINEの認可画面へ移動するURLを作る。
     * stateは認証要求との対応確認、nonceはIDトークンの再利用防止に使う。
     * codeVerifier自体は送らず、PKCE用のハッシュ値だけをURLへ含める。
     */
    public String authorizationUrl(String state, String nonce, String codeVerifier) {
        // 認証要求に必要な値がそろい、PKCE検証値の形式が正しいか確認する。
        if (state == null || state.isBlank() || nonce == null || nonce.isBlank() || !isValidCodeVerifier(codeVerifier)) {
            throw new IllegalArgumentException("A state, nonce, and valid PKCE verifier are required.");
        }

        // 認可コードの受け取り先と、LINEに求めるアクセス権限を指定する。
        Map<String, String> parameters = new LinkedHashMap<>(); // 認可URLのクエリに付ける項目。
        parameters.put("response_type", "code");
        parameters.put("client_id", config.channelId());
        parameters.put("redirect_uri", config.redirectUri());
        parameters.put("scope", "openid profile email");

        // この認証要求を識別する値と、PKCEの照合に使うハッシュを加える。
        parameters.put("state", state);
        parameters.put("nonce", nonce);
        parameters.put("code_challenge", codeChallenge(codeVerifier));
        parameters.put("code_challenge_method", "S256");

        // パラメーターをURL用にエンコードし、LINEの認可画面のURLを組み立てる。
        return authorizationEndpoint + "?" + encodeForm(parameters).replace("+", "%20");
    }

    /**
     * 認可コードを交換し、LINEによるIDトークン検証に成功した本人情報を取得する。
     *
     * @param code LINEの認可コード。コールバックURLのクエリに含まれる。
     * @param nonce 認証開始時に生成したランダム文字列。IDトークン検証時に照合する。
     * @param codeVerifier 認証開始時に生成したPKCE検証値。認可コードの交換時に送る。
     * @return LINEの検証済み本人情報。メール未取得時の {@code email} は {@code null}。
     */
    public Profile authenticate(String code, String nonce, String codeVerifier) throws IOException, InterruptedException {
        // LINEとの通信前に、認可コードと認証開始時の値を確認する。
        if (code == null || code.isBlank() || nonce == null || nonce.isBlank() || !isValidCodeVerifier(codeVerifier)) {
            throw new IOException("LINE login request is incomplete.");
        }

        // 認可コードとPKCE検証値を使ってIDトークンを取得する。
        String idToken = exchangeCodeForIdToken(code, codeVerifier); // LINEが発行した本人確認用のトークン。

        // IDトークンが今回の認証で発行されたものか確認する。
        JsonNode claims = verifyIdToken(idToken, nonce); // 検証済みのIDトークンに含まれる項目。

        // 検証済みの情報から、必須のLINEユーザーIDと表示名を取り出す。
        // 会員の照合に使うLINEユーザー識別子（sub）。
        String lineSubject = requiredText(claims, "sub", "LINE did not return a user identifier.");
        // LINEプロフィールの表示名（name）。
        String displayName = requiredText(claims, "name", "LINE did not return a display name.");

        // メールは任意。未取得や空白の場合も認証を継続する。
        String email = text(claims, "email"); // LINEが返したメールアドレス。未取得ならnull。
        if (email != null && email.isBlank()) {
            email = null;
        }
        return new Profile(displayName, email, lineSubject);
    }

    /** 開始時のPKCE検証値を添えて認可コードを交換し、本人確認に使うIDトークンを取り出す。 */
    private String exchangeCodeForIdToken(String code, String codeVerifier) throws IOException, InterruptedException {
        // LINEがコードの発行先とPKCEを照合できるよう、交換リクエストに必要な値を設定する。
        Map<String, String> parameters = new LinkedHashMap<>(); // トークン交換APIへ送るフォーム項目。
        parameters.put("grant_type", "authorization_code");
        parameters.put("code", code);
        parameters.put("redirect_uri", config.redirectUri());
        parameters.put("client_id", config.channelId());
        parameters.put("client_secret", config.channelSecret());
        parameters.put("code_verifier", codeVerifier);

        // トークンAPIへ送信し、応答にIDトークンが含まれていることを確認する。
        JsonNode tokenResponse = postForm(tokenEndpoint, parameters); // IDトークンなどを含むLINEの応答。
        return requiredText(tokenResponse, "id_token", "LINE did not return an ID token.");
    }

    /** LINEの検証APIで署名を確認し、応答のクレームも照合する。 */
    private JsonNode verifyIdToken(String idToken, String nonce) throws IOException, InterruptedException {
        // IDトークンをチャネルIDとnonceとともにLINEの検証APIへ送る。
        // LINEが返した検証結果のJSON。
        JsonNode claims = postForm(verificationEndpoint,
                Map.of("id_token", idToken, "client_id", config.channelId(), "nonce", nonce));

        // 発行元、対象チャネル、nonce、有効期限をアプリ側でも確認する。
        JsonNode expiresAt = claims.path("exp"); // IDトークンの有効期限を表すUNIX秒。
        if (!ISSUER.equals(text(claims, "iss"))
                || !config.channelId().equals(text(claims, "aud"))
                || !nonce.equals(text(claims, "nonce"))
                || !expiresAt.isIntegralNumber()
                || !expiresAt.canConvertToLong()
                || expiresAt.longValue() <= Instant.now().getEpochSecond()) {
            throw new IOException("LINE identity verification failed.");
        }
        return claims;
    }

    /** LINE APIへUTF-8のフォーム形式でPOSTし、正常なJSON応答だけを受け付ける。 */
    private JsonNode postForm(URI endpoint, Map<String, String> parameters) throws IOException, InterruptedException {
        // タイムアウトを設定し、フォームの送信とJSONの受け取りを指定する。
        // URL・ヘッダー・フォーム本文をまとめたPOSTリクエスト。
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(encodeForm(parameters), StandardCharsets.UTF_8))
                .build();

        // リクエストを送信し、応答本文をUTF-8で受け取る。
        HttpResponse<String> response; // LINE APIのHTTPステータスと応答本文を持つ結果。
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // 元の例外にAPI応答や送信トークンが含まれる可能性があるため、原因として保持しない。
            throw new IOException("LINE authentication service could not be reached.");
        }

        // HTTP 200の応答だけを成功として扱う。
        if (response.statusCode() != 200) {
            throw new IOException("LINE authentication service rejected the request.");
        }

        // 本文をJSONとして読み取り、エラー項目のないオブジェクトを受け付ける。
        try {
            JsonNode data = JSON.readTree(response.body()); // 形式を確認する前のJSONデータ。
            if (data == null || !data.isObject() || data.has("error")) {
                throw new IOException("LINE authentication service returned an invalid response.");
            }
            return data;
        } catch (IOException e) {
            // JSON解析エラーに秘密情報を含む応答の一部が載る場合があるため、固定メッセージへ置き換える。
            throw new IOException("LINE authentication service returned an invalid response.");
        }
    }

    // 文字列以外の値は未取得として扱い、不正な型を文字列へ変換しない。
    private static String text(JsonNode object, String key) {
        JsonNode value = object.path(key); // 指定キーに対応するJSONの値。
        return value.isTextual() ? value.textValue() : null;
    }

    /** subjectや表示名など、認証後の処理に必要な文字列の存在を確認する。 */
    private static String requiredText(JsonNode object, String key, String message) throws IOException {
        String value = text(object, key); // 指定キーから取得した必須項目の文字列。
        if (value == null || value.isBlank()) {
            throw new IOException(message);
        }
        return value;
    }

    // PKCEで定められた長さと使用可能文字に合うかを確認する。
    private static boolean isValidCodeVerifier(String verifier) {
        return verifier != null && verifier.matches("[A-Za-z0-9._~-]{43,128}");
    }

    /** 検証値のSHA-256ハッシュを、パディングなしのBase64URL形式にする。 */
    private static String codeChallenge(String verifier) {
        try {
            // Base64URLに変換する前のSHA-256ハッシュのバイト列。
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.");
        }
    }

    // 日本語や記号を含む値も、パラメーターの区切りと混同しないようUTF-8で符号化する。
    private static String encodeForm(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    /**
     * 検証後に利用するLINE本人情報。
     *
     * @param displayName LINEプロフィールの表示名
     * @param email LINEから取得したメールアドレス。未取得ならnull
     * @param lineSubject 会員の照合に使うLINEユーザー識別子
     */
    public record Profile(String displayName, String email, String lineSubject) { }
}
