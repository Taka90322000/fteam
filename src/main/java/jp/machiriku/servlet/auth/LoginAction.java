package jp.machiriku.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.tool.Action;
import jp.machiriku.config.AppConfig;

/** ログイン画面の表示と、設定で明示的に有効にしたデモログインを担当する。 */
public class LoginAction extends Action {

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // デモ用の利用区分は、設定が有効かつ許可された値の場合にだけ受け付ける。
        String demoRole = req.getParameter("demoRole");
        if ("true".equalsIgnoreCase(AppConfig.get("MACHIRIKU_DEMO_LOGIN_ENABLED"))
                && java.util.Set.of("resident", "business", "owner", "admin").contains(demoRole == null ? "" : demoRole)) {
            // 直前にログインしていた会員情報を新しいデモ利用者へ引き継がない。
            req.getSession().invalidate();
            req.getSession().setAttribute("userRole", demoRole);
            res.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        // 通常のログイン画面JSPへフォワード
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, res);
    }
}
