package jp.machiriku.servlet.management;

import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSPで日時・真偽値・NULL値・フォーム初期値を読みやすく表示するための整形ユーティリティ。
 */
public final class ManagementFormat {
    /** 画面表示および日時入力に用いる日本標準時。 */
    public static final ZoneId JAPAN = ZoneId.of("Asia/Tokyo");
    /** 一覧・詳細画面で日時を表示する書式。 */
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    /** 一覧・詳細画面で日付を表示する書式。 */
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** ユーティリティクラスのためインスタンス化しない。 */
    private ManagementFormat() { }

    /**
     * 日時・日付・真偽値・NULLを画面表示用の文字列へ変換する。
     * @param value 表示対象の値。null可。
     * @return JSPでエスケープして表示する文字列。
     */
    public static String text(Object value) {
        if (value == null) {
            return "—";
        }
        if (value instanceof OffsetDateTime time) {
            return DISPLAY_TIME.format(time.atZoneSameInstant(JAPAN));
        }
        if (value instanceof Timestamp time) {
            return DISPLAY_TIME.format(time.toInstant().atZone(JAPAN));
        }
        if (value instanceof LocalDate date) {
            return DISPLAY_DATE.format(date);
        }
        if (value instanceof Boolean flag) {
            return flag ? "はい" : "いいえ";
        }
        String str = value.toString();
        return str.isEmpty() ? "—" : str;
    }

    /**
     * 日時をブラウザのdatetime-local入力欄に合わせた文字列へ変換する。
     * @param value OffsetDateTimeまたはTimestamp。その他はそのまま返す。
     * @return 日本時間のdatetime-local形式文字列、または元の値。
     */
    public static Object dateTimeInput(Object value) {
        if (value instanceof OffsetDateTime time) {
            return time.atZoneSameInstant(JAPAN).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS).toString();
        }
        if (value instanceof Timestamp time) {
            return time.toInstant().atZone(JAPAN).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS).toString();
        }
        return value;
    }

    /**
     * 単一選択・複数選択のセレクトボックスで候補が選択済みか判定する。
     * @param value 現在の選択値（単一値またはCollection）。
     * @param option 判定する候補の送信値。
     * @return 選択済みの場合はtrue。
     */
    public static boolean selected(Object value, String option) {
        if (value instanceof Collection<?> values) {
            for (Object item : values) {
                if (option.equals(String.valueOf(item))) {
                    return true;
                }
            }
            return false;
        }
        return option.equals(value == null ? "" : value.toString());
    }

    /**
     * 値と表示名のペアから定義順のマップを作成する。
     * @param pairs キー、表示名の順に並べた文字列。
     * @return 定義順を保持した選択肢マップ。
     */
    public static Map<String, String> options(String... pairs) {
        Map<String, String> choices = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            choices.put(pairs[i], pairs[i + 1]);
        }
        return choices;
    }

    /**
     * 文字列が安全なHTTP/HTTPSのURLかどうかを判定する。
     * @param value 判定対象のURL文字列。
     * @return ホストを持つHTTP(S) URLならtrue。
     */
    public static boolean isWebUrl(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            return uri.getHost() != null
                    && uri.getUserInfo() == null
                    && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
