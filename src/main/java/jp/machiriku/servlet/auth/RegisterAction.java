package jp.machiriku.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

/** 利用区分の選択とLINE連携の開始を行う、新規会員登録画面を表示する。 */
public class RegisterAction extends Action {

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        if (LineLoginSupport.redirectAuthenticated(req, res, true)) { return; }
        LineLoginSupport.prepareRegistration(req);
        // 新規会員登録画面JSPへフォワード
        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, res);
    }
}
