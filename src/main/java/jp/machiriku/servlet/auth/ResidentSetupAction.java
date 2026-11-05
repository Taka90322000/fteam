package jp.machiriku.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.tool.Action;

/** 地域住民の初期設定画面を表示し、送信内容をセッションへ保持する。 */
public class ResidentSetupAction extends Action {

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String method = req.getMethod();

        if ("POST".equalsIgnoreCase(method)) {
            // POSTはフォーム送信。文字コードを指定してから各入力値を取り出す。
            req.setCharacterEncoding("UTF-8");
            String name = req.getParameter("name");
            String email = req.getParameter("email");
            String ageGroup = req.getParameter("ageGroup");
            String gender = req.getParameter("gender");
            String industry = req.getParameter("industry");
            // 住所の表示文字列と、地図上で選んだ緯度・経度をそれぞれ受け取る。
            String livingAddress = req.getParameter("livingAddress");
            String livingLat = req.getParameter("livingLat");
            String livingLng = req.getParameter("livingLng");
            String workAddress = req.getParameter("workAddress");
            String workLat = req.getParameter("workLat");
            String workLng = req.getParameter("workLng");
            boolean emailNotify = "on".equalsIgnoreCase(req.getParameter("emailNotify")) || "true".equalsIgnoreCase(req.getParameter("emailNotify"));

            // 現時点ではDB保存せず、画面用の会員情報と初期設定完了状態を保持する。
            HttpSession session = req.getSession();
            session.setAttribute("userName", (name != null && !name.trim().isEmpty()) ? name.trim() : "LINEユーザー");
            session.setAttribute("userRole", "resident");
            session.setAttribute("userEmail", email != null ? email.trim() : "");
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
            displayName = "LINEユーザー";
        }

        String email = (String) session.getAttribute("lineEmail");
        if (email == null || email.trim().isEmpty()) {
            email = req.getParameter("email");
        }
        if (email == null || email.trim().isEmpty()) {
            email = "tanaka.line@example.com"; // 実データがない場合に使う、画面確認用のデモ初期値。
        }

        req.setAttribute("displayName", displayName);
        req.setAttribute("email", email);
        req.setAttribute("role", "resident");
        req.setAttribute("roleLabel", "地域住民");

        // JSPへフォワード
        req.getRequestDispatcher("/WEB-INF/views/auth/resident_setup.jsp").forward(req, res);
    }
}
