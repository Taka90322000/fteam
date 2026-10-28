package jp.machiriku.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

public class RegisterAction extends Action {

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 新規会員登録画面JSPへフォワード
        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, res);
    }
}
