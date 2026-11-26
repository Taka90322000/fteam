package jp.machiriku.bean.management;

/** 管理画面・サポート画面で入力不正や権限・状態の競合をHTTPステータス付きで伝える例外。 */
public class ManagementProblem extends RuntimeException {
    /** レスポンスに返すHTTPステータスコード。 */
    private final int status;

    /**
     * 利用者へ案内できるエラーを生成する。
     * @param status HTTPステータスコード（400, 403, 404, 409など）。
     * @param message 利用者へ表示する案内メッセージ。
     */
    public ManagementProblem(int status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * HTTPステータスコードを取得する。
     * @return HTTPステータスコード。
     */
    public int getStatus() {
        return status;
    }
}
