package jp.machiriku.tool;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** FrontControllerから呼び出される画面処理の共通インターフェースを定義する基底クラス。 */
public abstract class Action {
    /**
     * 画面の入力を確認し、処理結果に応じて画面を描画するか遷移先へ案内する。
     * @param req FrontControllerから渡されたHTTPリクエスト。
     * @param res 画面表示・遷移・エラー応答に使用するHTTPレスポンス。
     * @throws Exception 画面処理を完了できず、FrontControllerのエラー処理が必要な場合。
     */
    public abstract void execute(HttpServletRequest req, HttpServletResponse res) throws Exception;
}
