package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

/** 管理者が問い合わせの担当と状態を管理し、返信や内部メモを保存する。 */
public final class AdminInquiriesAction extends Action {
    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminInquiriesAction() { }

    /**
     * 管理者が問い合わせの担当と状態を管理し、返信や内部メモを保存する。
     * @param req FrontControllerから受け取るHTTPリクエスト。
     * @param res 画面表示、エラー応答や更新後の遷移に使用するHTTPレスポンス。
     * @throws Exception 画面処理を完了できず、FrontControllerでエラー処理が必要な場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        ManagementActionSupport.admin(req, res,
                (db, input, viewer) -> HelpDesk.inquiries(db, input, viewer.getUserId(), true), (db, input, actorId) -> HelpDesk.changeInquiry(db, input, actorId, true));
    }
}
