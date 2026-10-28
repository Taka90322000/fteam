package jp.machiriku.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;

public class LoginAction extends Action {

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // デモ用ログインのパラメータ判定 (demoRole)
        String demoRole = req.getParameter("demoRole");
        if (demoRole != null && !demoRole.trim().isEmpty()) {
            req.getSession().setAttribute("userRole", demoRole);
            res.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        // 通常のログイン画面JSPへフォワード
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, res);
    }
}
