package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

/** 管理者が物件の契約内容と関連する出店計画を確認する。 */
public final class AdminTenanciesAction extends Action {
    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminTenanciesAction() { }

    /**
     * 管理者が物件の契約内容と関連する出店計画を確認する。
     * @param req FrontControllerから受け取るHTTPリクエスト。
     * @param res 画面表示、エラー応答や更新後の遷移に使用するHTTPレスポンス。
     * @throws Exception 画面処理を完了できず、FrontControllerでエラー処理が必要な場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        ManagementActionSupport.admin(req, res,
                (db, input, viewer) -> AdminListings.related(db, input, "tenancies"), null);
    }
}
