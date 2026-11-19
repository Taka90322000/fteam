package jp.machiriku.auth;

import java.net.URI;

import jp.machiriku.config.AppConfig;

/** LINEログインのサーバー用設定。チャネルシークレットはブラウザーへ渡さない。 */
public record LineLoginConfig(String channelId, String channelSecret, String redirectUri) {

    /** 必須項目とコールバックURLを検証し、HTTPはローカル開発時だけ許可する。 */
    public LineLoginConfig {
        requireValue(channelId, "LINE_CHANNEL_ID");
        requireValue(channelSecret, "LINE_CHANNEL_SECRET");
        requireValue(redirectUri, "LINE_REDIRECT_URI");
        URI redirect;
        try {
            redirect = URI.create(redirectUri);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("LINE_REDIRECT_URI must be a valid callback URL.");
        }
        String host = redirect.getHost();
        boolean localhost = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
                || "::1".equals(host) || "[::1]".equals(host);
        boolean secure = "https".equalsIgnoreCase(redirect.getScheme());
        boolean localHttp = localhost && "http".equalsIgnoreCase(redirect.getScheme());
        if (host == null || (!secure && !localHttp) || redirect.getUserInfo() != null
                || redirect.getFragment() != null) {
            throw new IllegalArgumentException("LINE_REDIRECT_URI must use HTTPS (HTTP is allowed only for localhost).");
        }
    }

    /** 共通設定からLINE用の値を読み込む。.envと環境変数の優先順位はAppConfigに従う。 */
    public static LineLoginConfig fromEnvironment() {
        try {
            return new LineLoginConfig(environment("LINE_CHANNEL_ID"),
                    environment("LINE_CHANNEL_SECRET"), environment("LINE_REDIRECT_URI"));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    // 未設定の場合も、秘密の値ではなく設定項目名だけをエラーに含める。
    private static String environment(String key) {
        String value = AppConfig.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing LINE Login setting in .env: " + key);
        }
        return value;
    }

    private static void requireValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be configured.");
        }
    }

    // 設定オブジェクトがログへ出力されても、シークレットを漏らさない。
    @Override
    public String toString() {
        return "LineLoginConfig[channelId=" + channelId + ", channelSecret=[REDACTED], redirectUri=" + redirectUri + "]";
    }
}
