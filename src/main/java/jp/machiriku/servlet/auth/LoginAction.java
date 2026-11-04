package jp.machiriku.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;
import jp.machiriku.config.AppConfig;

public class LoginAction extends Action {

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // デモ用ログインのパラメータ判定 (demoRole)
        String demoRole = req.getParameter("demoRole");
        if ("true".equalsIgnoreCase(AppConfig.get("MACHIRIKU_DEMO_LOGIN_ENABLED"))
                && java.util.Set.of("resident", "business", "owner", "admin").contains(demoRole == null ? "" : demoRole)) {
            req.getSession().invalidate();
            req.getSession().setAttribute("userRole", demoRole);
            res.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        // 通常のログイン画面JSPへフォワード
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, res);
    }
}
