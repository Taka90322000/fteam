package jp.machiriku.servlet.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

/** 地域住民向けプロフィール設定Action。/jp/machiriku/servlet/user/ResidentProfile.action に対応。 */
public class ResidentProfileAction extends Action {
    private final ProfileAction delegate = new ProfileAction();

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        delegate.execute(req, res);
    }
}
