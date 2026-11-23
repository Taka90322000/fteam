package jp.machiriku.servlet.management;

/** 管理画面や問い合わせ画面で案内できる入力・権限・状態のエラー。 */
final class ManagementProblem extends RuntimeException {
    /** 画面へ返すHTTPステータス。 */
    private final int status;

    /**
     * 利用者へ表示できるエラーを作る。
     * @param status エラーに対応するHTTPステータス。
     * @param message 利用者に表示できる案内文。
     */
    ManagementProblem(int status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * エラーのHTTPステータスを返す。
     * @return 画面のレスポンスに使うステータス。
     */
    int status() { return status; }
}
