package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

/** 会員本人の問い合わせ履歴を表示し、新規の問い合わせや返信を保存する。 */
public final class InquiriesAction extends Action {
    /** FrontControllerが使用する引数なしコンストラクター。 */
    public InquiriesAction() { }

    /**
     * 会員本人の問い合わせ履歴を表示し、新規の問い合わせや返信を保存する。
     * @param req FrontControllerから受け取るHTTPリクエスト。
     * @param res 画面表示、エラー応答や更新後の遷移に使用するHTTPレスポンス。
     * @throws Exception 画面処理を完了できず、FrontControllerでエラー処理が必要な場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        ManagementActionSupport.support(req, res, ManagementActionSupport.Access.MEMBER,
                (db, input, viewer) -> HelpDesk.inquiries(db, input, viewer.getUserId(), false), (db, input, actorId) -> HelpDesk.changeInquiry(db, input, actorId, false));
    }
}
