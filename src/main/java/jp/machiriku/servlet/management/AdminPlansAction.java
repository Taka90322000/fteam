package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

/** 管理者が出店計画の内容を確認し、公開中の計画を終了する。 */
public final class AdminPlansAction extends Action {
    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminPlansAction() { }

    /**
     * 管理者が出店計画の内容を確認し、公開中の計画を終了する。
     * @param req FrontControllerから受け取るHTTPリクエスト。
     * @param res 画面表示、エラー応答や更新後の遷移に使用するHTTPレスポンス。
     * @throws Exception 画面処理を完了できず、FrontControllerでエラー処理が必要な場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        ManagementActionSupport.admin(req, res,
                (db, input, viewer) -> AdminListings.related(db, input, "plans"), AdminListings::changePlan);
    }
}
