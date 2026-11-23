package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

/** 本登録済みの会員から通報を受け付け、送信者本人に受付状況を表示する。 */
public final class ReportAction extends Action {
    /** FrontControllerが使用する引数なしコンストラクター。 */
    public ReportAction() { }

    /**
     * 本登録済みの会員から通報を受け付け、送信者本人に受付状況を表示する。
     * @param req FrontControllerから受け取るHTTPリクエスト。
     * @param res 画面表示、エラー応答や更新後の遷移に使用するHTTPレスポンス。
     * @throws Exception 画面処理を完了できず、FrontControllerでエラー処理が必要な場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        ManagementActionSupport.support(req, res, ManagementActionSupport.Access.ACTIVE_MEMBER,
                HelpDesk::reportForm, HelpDesk::submitReport);
    }
}
