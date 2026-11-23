package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

/** 会員が閲覧を許されている事業主の一覧・詳細を表示する。 */
public final class BusinessDirectoryAction extends Action {
    /** FrontControllerが使用する引数なしコンストラクター。 */
    public BusinessDirectoryAction() { }

    /**
     * 会員が閲覧を許されている事業主の一覧・詳細を表示する。
     * @param req FrontControllerから受け取るHTTPリクエスト。
     * @param res 画面表示、エラー応答や更新後の遷移に使用するHTTPレスポンス。
     * @throws Exception 画面処理を完了できず、FrontControllerでエラー処理が必要な場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        ManagementActionSupport.support(req, res, ManagementActionSupport.Access.MEMBER,
                PublicDirectory::businesses, null);
    }
}
