package jp.machiriku.servlet.auth;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.User;
import jp.machiriku.dao.ProfileDao;
import jp.machiriku.registration.RegistrationForms;
import jp.machiriku.servlet.MemberSession;
import jp.machiriku.tool.Action;

/** LINE取得値を初期表示し、住民の入力をプロフィール・住所・通知設定へまとめて保存する。 */
public class ResidentSetupAction extends Action {
    protected ProfileDao createProfileDao() { return new ProfileDao(); }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        User user;
        HttpSession session = req.getSession(false); // すでに認証済みの会員がいるかを確認するセッション。
        boolean authenticated = MemberSession.signedInUser(session) != null;
        // プレビューはGET表示だけに限定する。POSTはクエリに関係なく本物のログイン会員で処理する。
        boolean preview = !authenticated && "GET".equalsIgnoreCase(req.getMethod())
                && ("1".equals(req.getParameter("preview")) || "true".equalsIgnoreCase(req.getParameter("preview")));
        if (preview) {
            user = new User();
            user.setUserId(0);
            user.setDisplayName("LINEユーザー");
            user.setEmail(null);
            user.setUserRole("RESIDENT");
            user.setAccountStatus("PENDING");
        } else {
            user = OnboardingSupport.requireUser(req, res, session, "RESIDENT", this::createProfileDao);
            if (user == null) { return; }
        }
        if (!"GET".equalsIgnoreCase(req.getMethod()) && !"POST".equalsIgnoreCase(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        boolean posting = "POST".equalsIgnoreCase(req.getMethod());
        Map<String, String> fields;
        if (posting) {
            req.setCharacterEncoding("UTF-8");
            if (!OnboardingSupport.validCsrf(req, session)) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
            fields = OnboardingSupport.fields(req, "name", "email", "ageGroup", "gender", "industry", "preferredIndustries", "livingAddress", "livingLat",
                    "livingLng", "workAddress", "workLat", "workLng", "emailNotify");
            try {
                var input = RegistrationForms.resident(fields);
                User saved = createProfileDao().completeResidentSetup(user.getUserId(), input);
                // DBのcommitが成功してから、画面用の会員情報と初期設定完了状態を更新する。
                String industry = fields.get("industry");
                OnboardingSupport.completed(req, res, session, saved, saved.getDisplayName(), current -> {
                    current.setAttribute("userIndustry", industry);
                    current.setAttribute("userPreferredIndustries", input.preferredIndustryCodes());
                });
                return;
            } catch (IllegalArgumentException e) {
                req.setAttribute("errorMessage", e.getMessage());
            } catch (SQLException e) {
                OnboardingSupport.logDatabaseError(req, e);
                if ("28000".equals(e.getSQLState())) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
                req.setAttribute("errorMessage", "23505".equals(e.getSQLState())
                        ? "このメールアドレスは既に登録されています。入力内容を確認してください。"
                        : "登録内容を保存できませんでした。入力内容を保持していますので、もう一度お試しください。");
            }
        } else {
            // クエリの表示名やメールを信用せず、認証済みセッション内のLINE取得値を使う。
            fields = new LinkedHashMap<>();
            fields.put("name", OnboardingSupport.lineValue(session, "lineDisplayName", user.getDisplayName()));
            fields.put("email", OnboardingSupport.lineValue(session, "lineEmail", user.getEmail()));
            fields.put("ageGroup", "未設定");
            fields.put("gender", "回答しない");
            fields.put("industry", "");
            fields.put("preferredIndustries", "[]");
            fields.put("emailNotify", "on");
        }
        java.util.List<Map<String, Object>> residentTaxonomy = java.util.Collections.emptyList();
        if (!preview) {
            try {
                residentTaxonomy = createProfileDao().listResidentTaxonomy();
            } catch (SQLException e) {
                OnboardingSupport.logDatabaseError(req, e);
                if (req.getAttribute("errorMessage") == null) {
                    req.setAttribute("errorMessage", "業種の選択肢を取得できませんでした。時間をおいて再度お試しください。");
                }
            }
        }
        req.setAttribute("formValues", fields);
        req.setAttribute("displayName", fields.get("name"));
        req.setAttribute("email", fields.get("email"));
        req.setAttribute("role", "resident");
        req.setAttribute("roleLabel", "地域住民");
        req.setAttribute("residentTaxonomy", residentTaxonomy);
        if (OnboardingSupport.prepareView(req, res, preview ? req.getSession() : session)) {
            req.getRequestDispatcher("/WEB-INF/views/auth/resident_setup.jsp").forward(req, res);
        }
    }
}
