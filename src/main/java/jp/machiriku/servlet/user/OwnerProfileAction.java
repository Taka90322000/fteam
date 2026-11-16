package jp.machiriku.servlet.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

/** 物件オーナー向けプロフィール設定Action。/jp/machiriku/servlet/user/OwnerProfile.action に対応。 */
public class OwnerProfileAction extends Action {
    private final ProfileAction delegate = new ProfileAction();

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        delegate.execute(req, res);
    }
}
