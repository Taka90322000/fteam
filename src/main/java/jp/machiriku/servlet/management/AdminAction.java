package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

/** 通報・問い合わせの対応状況とサービスの利用件数を表示する。 */
public final class AdminAction extends Action {
    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminAction() { }

    /**
     * 通報・問い合わせの対応状況とサービスの利用件数を表示する。
     * @param req FrontControllerから受け取るHTTPリクエスト。
     * @param res 画面表示、エラー応答や更新後の遷移に使用するHTTPレスポンス。
     * @throws Exception 画面処理を完了できず、FrontControllerでエラー処理が必要な場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        ManagementActionSupport.admin(req, res,
                (db, input, viewer) -> AdminOverview.dashboard(db), null);
    }
}
