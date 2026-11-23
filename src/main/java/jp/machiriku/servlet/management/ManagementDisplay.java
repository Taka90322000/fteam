package jp.machiriku.servlet.management;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** DBの値を、画面で使う日時や選択値に整える。 */
public final class ManagementDisplay {
    /** 管理画面に表示する日本時間の書式。 */
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    /** 表示値の変換処理を静的に使用する。 */
    private ManagementDisplay() { }

    /**
     * 値と表示名の組を、指定順に並ぶ選択値にする。
     * @param pairs 値、表示名の順に並べた文字列。
     * @return 候補の値をキーとした表示名。
     */
    public static Map<String, String> options(String... pairs) {
        // 選択欄に定義順で表示できるように挿入順を保つ。
        Map<String, String> choices = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            choices.put(pairs[i], pairs[i + 1]);
        }
        return choices;
    }

    /**
     * DAOが返したマスターのIDと名称を選択値に変換する。
     * @param rows idとlabelを含むマスターの行。
     * @return IDをキーとした表示名。
     */
    static Map<String, String> choices(List<Map<String, Object>> rows) {
        // DAOで並び替えた順序を保って選択欄に渡す。
        Map<String, String> choices = new LinkedHashMap<>();
        for (var row : rows) {
            choices.put(row.get("id").toString(), row.get("label").toString());
        }
        return choices;
    }

    /**
     * 日時や有効フラグを一覧・詳細で読みやすい値に変える。
     * @param value 表示元の値。欠損値はnull。
     * @return JSPがエスケープして表示する値。
     */
    public static Object display(Object value) {
        if (value instanceof Timestamp time) {
            return DISPLAY_TIME.format(time.toInstant().atZone(ManagementRequest.JAPAN));
        }
        if (value instanceof Boolean flag) {
            return flag ? "はい" : "いいえ";
        }
        return value == null ? "—" : value;
    }

    /**
     * 日時の初期値をブラウザの日時入力欄に合わせる。
     * @param value DBの日時、または画面がそのまま利用できる値。
     * @return 日本時間に直した入力値。時刻以外は受け取った値。
     */
    public static Object dateTimeInput(Object value) {
        if (value instanceof Timestamp time) {
            return time.toInstant().atZone(ManagementRequest.JAPAN).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS).toString();
        }
        if (value instanceof OffsetDateTime time) {
            return time.atZoneSameInstant(ManagementRequest.JAPAN).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS).toString();
        }
        return value;
    }

    /**
     * 単一・複数選択の初期値に、表示する候補が含まれるか判定する。
     * @param value 選択済みの値、またはその一覧。未選択はnull。
     * @param option 候補の送信値。
     * @return 選択済みの場合はtrue。
     */
    public static boolean selected(Object value, String option) {
        if (value instanceof Collection<?> values) {
            return values.stream().anyMatch(item -> option.equals(String.valueOf(item)));
        }
        return option.equals(value == null ? "" : value.toString());
    }

    /**
     * 関連データのリンクまたはDBに保存された公開URLを表示できる場合に取得する。
     * @param row 表示するレコード。
     * @param column 値に対応する列名。
     * @param links IDの列をキーとした、各詳細画面へのURLの先頭部分。詳細リンクを指定しない場合はnull。
     * @param contextPath アプリのコンテキストパス。
     * @return 閲覧用のURL。対象外の値はnull。
     */
    public static String target(Map<String, Object> row, String column, Map<String, String> links, String contextPath) {
        // 詳細リンクを指定した画面では、対応する列から内部URLを作る。
        if (links != null) {
            // 名前から移動できる一覧では、その行のIDをリンク先に渡す。
            String key = "名前".equals(column) && links.containsKey("ID") ? "ID" : column;
            // 対象列で詳細リンクが指定されている場合だけURLを組み立てる。
            String prefix = links.get(key);
            if (prefix != null && row.get(key) instanceof Number id) {
                return contextPath + prefix + id;
            }
        }
        // 保存値をリンクにする場合はHTTP(S)のURLだけを使う。
        Object value = row.get(column);
        return column.endsWith("URL") && value instanceof String url && ManagementRequest.isWebUrl(url) ? url : null;
    }

    /**
     * SQLの取得結果から整数の識別子や集計件数を取得する。
     * @param row DAOが取得した行。
     * @param column 数値を含む列名。
     * @return 対象の数値。
     */
    static long key(Map<String, Object> row, String column) {
        return ((Number) row.get(column)).longValue();
    }
}
