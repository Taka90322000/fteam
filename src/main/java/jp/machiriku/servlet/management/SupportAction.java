package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

/** サポートの入口から公開物件一覧へ案内する。 */
public final class SupportAction extends Action {
    /** FrontControllerが使用する引数なしコンストラクター。 */
    public SupportAction() { }

    /**
     * サポート画面への通常の遷移を公開物件一覧へ案内する。
     * @param req FrontControllerから受け取るHTTPリクエスト。
     * @param res リダイレクトの送信に使うHTTPレスポンス。
     * @throws Exception 遷移またはエラーの応答を送信できなかった場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        // この入口は画面への遷移だけを受け付ける。
        if (!"GET".equals(req.getMethod()) && !"HEAD".equals(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        res.sendRedirect(req.getContextPath() + ManagementPaths.PROPERTIES);
    }
}
