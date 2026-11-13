package jp.machiriku.tool;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** FrontControllerから呼び出される画面処理の共通インターフェースを定義する基底クラス。 */
public abstract class Action {
	/** 入力の取得、サービスの呼び出し、画面表示やリダイレクトを各Actionで実装する。 */
	public abstract void execute(HttpServletRequest req, HttpServletResponse res) throws Exception;
}
