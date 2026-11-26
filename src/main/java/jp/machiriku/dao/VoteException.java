package jp.machiriku.dao;

/** 投票の入力や状態が条件を満たさないときの例外。code は画面側が判断に使う固定の英字コード。 */
public class VoteException extends Exception {
    public static final String INVALID_INPUT = "INVALID_INPUT";
    public static final String NOT_RESIDENT = "NOT_RESIDENT";
    public static final String PROPERTY_NOT_FOUND = "PROPERTY_NOT_FOUND";
    public static final String PROPERTY_CLOSED = "PROPERTY_CLOSED";
    public static final String PROPERTY_NO_LOCATION = "PROPERTY_NO_LOCATION";
    public static final String NO_LOCATION = "NO_LOCATION";
    public static final String BAD_CHOICES = "BAD_CHOICES";
    /** 画面で選んだ生活拠点が、本人のものとして存在しない。 */
    public static final String BAD_LOCATION = "BAD_LOCATION";

    private final String code;

    public VoteException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
