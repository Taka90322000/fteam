package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

/** 管理者が利用区分別の会員情報を確認し、利用状態やプロフィールを更新する。 */
public final class AdminUsersAction extends Action {
    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminUsersAction() { }

    /**
     * 管理者が利用区分別の会員情報を確認し、利用状態やプロフィールを更新する。
     * @param req FrontControllerから受け取るHTTPリクエスト。
     * @param res 画面表示、エラー応答や更新後の遷移に使用するHTTPレスポンス。
     * @throws Exception 画面処理を完了できず、FrontControllerでエラー処理が必要な場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        ManagementActionSupport.admin(req, res,
                (db, input, viewer) -> AdminUsers.page(db, input, viewer.getUserId()), AdminUsers::change);
    }
}
