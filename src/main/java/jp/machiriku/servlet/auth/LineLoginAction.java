package jp.machiriku.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.tool.Action;

public class LineLoginAction extends Action {

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String regRole = req.getParameter("regRole");
        if (regRole == null || regRole.trim().isEmpty()) {
            regRole = "resident";
        }

        // LINE認証成功時のモック情報（本番はOAuthコールバックから取得）
        HttpSession session = req.getSession();
        session.setAttribute("lineDisplayName", "LINEユーザー");
        session.setAttribute("lineEmail", "tanaka.line@example.com");
        session.setAttribute("userRole", regRole);

        if ("resident".equals(regRole)) {
            res.sendRedirect(req.getContextPath() + "/jp/machiriku/servlet/auth/ResidentSetup.action");
        } else if ("business".equals(regRole)) {
            // 出店事業主用（今後作成）
            res.sendRedirect(req.getContextPath() + "/jp/machiriku/servlet/auth/ResidentSetup.action?role=business");
        } else {
            // 物件オーナー用（今後作成）
            res.sendRedirect(req.getContextPath() + "/jp/machiriku/servlet/auth/ResidentSetup.action?role=owner");
        }
    }
}
