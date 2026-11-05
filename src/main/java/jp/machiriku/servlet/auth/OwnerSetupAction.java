package jp.machiriku.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.tool.Action;

public class OwnerSetupAction extends Action {

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String method = req.getMethod();

        if ("POST".equalsIgnoreCase(method)) {
            // 初期設定フォーム送信時の処理
            req.setCharacterEncoding("UTF-8");
            String repName = req.getParameter("representativeName");
            String company = req.getParameter("company");
            String email = req.getParameter("email");
            String phone = req.getParameter("phone");
            String[] preferredIndustries = req.getParameterValues("preferredIndustry");
            boolean emailNotify = "on".equalsIgnoreCase(req.getParameter("emailNotify")) || "true".equalsIgnoreCase(req.getParameter("emailNotify"));

            HttpSession session = req.getSession();
            session.setAttribute("userName", (repName != null && !repName.trim().isEmpty()) ? repName.trim() : "佐藤 武雄");
            session.setAttribute("userRole", "owner");
            session.setAttribute("userEmail", email != null ? email.trim() : "");
            session.setAttribute("company", company != null ? company.trim() : "");
            session.setAttribute("phone", phone != null ? phone.trim() : "");
            session.setAttribute("preferredIndustries", preferredIndustries);
            session.setAttribute("emailNotify", emailNotify);
            session.setAttribute("isOnboarded", true);

            // 完了後はトップページ（index.jsp）へリダイレクト
            res.sendRedirect(req.getContextPath() + "/index.jsp?onboarded=1");
            return;
        }

        // GET時の処理: LINE連携から渡された（またはセッションの）初期値を設定
        HttpSession session = req.getSession();
        String displayName = (String) session.getAttribute("lineDisplayName");
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = req.getParameter("displayName");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "佐藤 武雄"; // オーナー初期値
        }

        String email = (String) session.getAttribute("lineEmail");
        if (email == null || email.trim().isEmpty()) {
            email = req.getParameter("email");
        }
        if (email == null || email.trim().isEmpty()) {
            email = "sato.takeo@sato-fudosan.co.jp";
        }

        req.setAttribute("displayName", displayName);
        req.setAttribute("email", email);
        req.setAttribute("role", "owner");
        req.setAttribute("roleLabel", "物件オーナー");

        // JSPへフォワード
        req.getRequestDispatcher("/WEB-INF/views/auth/owner_setup.jsp").forward(req, res);
    }
}
