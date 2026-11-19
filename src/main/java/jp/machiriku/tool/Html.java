package jp.machiriku.tool;

/** LINE情報や再入力値を、HTMLの属性・本文に安全に表示する。 */
public final class Html {
    private Html() { }

    public static String escape(Object value) {
        return value == null ? "" : value.toString().replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
