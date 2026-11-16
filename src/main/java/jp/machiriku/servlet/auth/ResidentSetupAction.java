package jp.machiriku.servlet.auth;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.dao.ProfileDao;
import jp.machiriku.registration.RegistrationForms;
import jp.machiriku.tool.Action;

/** LINE取得値を初期表示し、住民の入力をプロフィール・住所・通知設定へまとめて保存する。 */
public class ResidentSetupAction extends Action {
    protected ProfileDao createProfileDao() { return new ProfileDao(); }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        User user;
        // プレビューはGET表示だけに限定する。POSTはクエリに関係なく本物のログイン会員で処理する。
        boolean preview = "GET".equalsIgnoreCase(req.getMethod())
                && ("1".equals(req.getParameter("preview")) || "true".equalsIgnoreCase(req.getParameter("preview")));
        if (preview) {
            user = new User();
            user.setUserId(0);
            user.setDisplayName("LINEユーザー");
            user.setEmail(null);
            user.setUserRole("RESIDENT");
            user.setAccountStatus("PENDING");
        } else {
            user = OnboardingSupport.requireUser(req, res, "RESIDENT");
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
            if (!OnboardingSupport.validCsrf(req)) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
            fields = OnboardingSupport.fields(req, "name", "email", "ageGroup", "gender", "industry", "preferredIndustries", "livingAddress", "livingLat",
                    "livingLng", "workAddress", "workLat", "workLng", "emailNotify");
            try {
                var input = RegistrationForms.resident(fields);
                User saved = createProfileDao().saveResident(user.getUserId(), input);
                // 選択した欲しい業種・施設ジャンルをセッションにも保存
                req.getSession().setAttribute("userIndustry", fields.get("industry"));
                req.getSession().setAttribute("userPreferredIndustries", input.preferredIndustryCodes());
                // DBのcommitが成功してから、画面用の会員情報と初期設定完了状態を更新する。
                OnboardingSupport.completed(req, saved, saved.getDisplayName());
                res.sendRedirect(req.getContextPath() + "/index.jsp?onboarded=1");
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
            fields.put("name", OnboardingSupport.lineValue(req.getSession(), "lineDisplayName", user.getDisplayName()));
            fields.put("email", OnboardingSupport.lineValue(req.getSession(), "lineEmail", user.getEmail()));
            fields.put("ageGroup", "未設定");
            fields.put("gender", "回答しない");
            fields.put("industry", "");
            fields.put("preferredIndustries", "[]");
            fields.put("emailNotify", "on");
            // すでに登録済み会員の場合はDBから保存済みのプロフィール情報を読み込んで復元
            if ("ACTIVE".equals(user.getAccountStatus())) {
                try {
                    fields.putAll(createProfileDao().findResidentFields(user.getUserId()));
                } catch (SQLException e) {
                    OnboardingSupport.logDatabaseError(req, e);
                }
            }
        }
        req.setAttribute("formValues", fields);
        req.setAttribute("displayName", fields.get("name"));
        req.setAttribute("email", fields.get("email"));
        req.setAttribute("role", "resident");
        req.setAttribute("roleLabel", "地域住民");
        req.setAttribute("csrfToken", OnboardingSupport.csrfToken(req.getSession()));
        req.getRequestDispatcher("/WEB-INF/views/auth/resident_setup.jsp").forward(req, res);
    }
}
