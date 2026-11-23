package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

/** 管理者が資格・書類のマスターと業種に必要な条件を確認・更新する。 */
public final class AdminQualificationsAction extends Action {
    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminQualificationsAction() { }

    /**
     * 管理者が資格・書類のマスターと業種に必要な条件を確認・更新する。
     * @param req FrontControllerから受け取るHTTPリクエスト。
     * @param res 画面表示、エラー応答や更新後の遷移に使用するHTTPレスポンス。
     * @throws Exception 画面処理を完了できず、FrontControllerでエラー処理が必要な場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        ManagementActionSupport.admin(req, res,
                (db, input, viewer) -> AdminCatalog.qualifications(db, input), AdminCatalog::saveQualification);
    }
}
