package jp.machiriku.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.tool.Action;

/** 事業者の初期設定画面を表示し、送信内容をセッションへ保持する。 */
public class BusinessSetupAction extends Action {

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String method = req.getMethod();

        if ("POST".equalsIgnoreCase(method)) {
            // POSTはフォーム送信。文字コードを指定してから各入力値を取り出す。
            req.setCharacterEncoding("UTF-8");
            String businessName = req.getParameter("businessName");
            String repName = req.getParameter("representativeName");
            String email = req.getParameter("email");
            String phone = req.getParameter("phone");
            // エリア名と、なぞった点列のJSON文字列は別の項目として受け取る。
            String preferredArea = req.getParameter("preferredArea");
            String preferredAreaPoints = req.getParameter("preferredAreaPoints");
            String employeeCount = req.getParameter("employeeCount");
            String[] preferredIndustries = req.getParameterValues("preferredIndustry");
            String mgmtExperience = req.getParameter("mgmtExperience");
            String industryExperience = req.getParameter("industryExperience");
            String[] qualifications = req.getParameterValues("qualification");
            boolean emailNotify = "on".equalsIgnoreCase(req.getParameter("emailNotify")) || "true".equalsIgnoreCase(req.getParameter("emailNotify"));

            // 現時点ではDB保存せず、表示に使う入力値と初期設定完了状態をセッションへ保持する。
            HttpSession session = req.getSession();
            session.setAttribute("userName", (repName != null && !repName.trim().isEmpty()) ? repName.trim() : "LINEユーザー");
            session.setAttribute("userRole", "business");
            session.setAttribute("userEmail", email != null ? email.trim() : "");
            session.setAttribute("businessName", businessName != null ? businessName.trim() : "");
            session.setAttribute("preferredArea", preferredArea != null ? preferredArea.trim() : "");
            session.setAttribute("preferredAreaPoints", preferredAreaPoints != null ? preferredAreaPoints.trim() : "");
            session.setAttribute("isOnboarded", true);

            // 完了後はトップページ（index.jsp）へリダイレクト
            res.sendRedirect(req.getContextPath() + "/index.jsp?onboarded=1");
            return;
        }

        // GETは画面表示。LINE連携時のセッション値を優先し、なければリクエスト値を使う。
        HttpSession session = req.getSession();
        String displayName = (String) session.getAttribute("lineDisplayName");
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = req.getParameter("displayName");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "佐藤 健一"; // 実データがない場合に使う、事業者画面のデモ初期値。
        }

        String email = (String) session.getAttribute("lineEmail");
        if (email == null || email.trim().isEmpty()) {
            email = req.getParameter("email");
        }
        if (email == null || email.trim().isEmpty()) {
            // メール未設定時の画面確認用デモ値。DBから取得したアドレスではない。
            email = "sato.business@example.com";
        }

        req.setAttribute("displayName", displayName);
        req.setAttribute("email", email);
        req.setAttribute("role", "business");
        req.setAttribute("roleLabel", "出店事業主");

        // JSPへフォワード
        req.getRequestDispatcher("/WEB-INF/views/auth/business_setup.jsp").forward(req, res);
    }
}
