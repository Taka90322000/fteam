package jp.machiriku.servlet.auth;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.dao.ProfileDao;
import jp.machiriku.registration.RegistrationForms;
import jp.machiriku.tool.Action;

/** LINE取得情報を初期表示し、オーナー情報と誘致希望業種をDBへ保存する。 */
public class OwnerSetupAction extends Action {
    protected ProfileDao createProfileDao() { return new ProfileDao(); }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        User user = OnboardingSupport.requireUser(req, res, "OWNER");
        if (user == null) { return; }
        if (!"GET".equalsIgnoreCase(req.getMethod()) && !"POST".equalsIgnoreCase(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        Map<String, String> fields;
        Set<String> industries = Set.of();
        if ("POST".equalsIgnoreCase(req.getMethod())) {
            req.setCharacterEncoding("UTF-8");
            if (!OnboardingSupport.validCsrf(req)) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
            fields = OnboardingSupport.fields(req, "representativeName", "company", "email", "phone", "iconImageUrl", "emailNotify");
            industries = OnboardingSupport.selection(req, "preferredIndustry");
            try {
                var input = RegistrationForms.owner(fields, OnboardingSupport.values(req, "preferredIndustry"));
                User saved = createProfileDao().saveOwner(user.getUserId(), input);
                boolean isEdit = "edit".equalsIgnoreCase(req.getParameter("mode"));
                // DBのcommit後にだけ、画面用の名前と初期設定完了状態を更新する。
                OnboardingSupport.completed(req, saved, input.representativeName());
                req.getSession().setAttribute("company", input.company() == null ? "" : input.company());
                req.getSession().setAttribute("phone", input.phoneE164());
                req.getSession().setAttribute("iconImageUrl", input.iconImageUrl() == null ? "" : input.iconImageUrl());
                req.getSession().setAttribute("preferredIndustries", input.industryTypeIds());
                req.getSession().setAttribute("emailNotify", input.emailNotify() && saved.getEmail() != null);
                res.sendRedirect(req.getContextPath() + (isEdit ? "/index.jsp?profile_saved=1" : "/index.jsp?onboarded=1"));
                return;
            } catch (IllegalArgumentException e) {
                req.setAttribute("errorMessage", e.getMessage());
            } catch (SQLException e) {
                OnboardingSupport.logDatabaseError(req, e);
                if ("28000".equals(e.getSQLState())) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
                String message;
                if ("23505".equals(e.getSQLState())) {
                    message = "このメールアドレスまたは電話番号は既に登録されています。入力内容を確認してください。";
                } else if ("42P01".equals(e.getSQLState()) || "42501".equals(e.getSQLState())) {
                    message = "オーナー登録の準備が完了していません。管理者に確認してください。";
                } else {
                    message = "登録内容を保存できませんでした。入力内容を保持していますので、もう一度お試しください。";
                }
                req.setAttribute("errorMessage", message);
            }
        } else {
            fields = new LinkedHashMap<>();
            fields.put("representativeName", OnboardingSupport.lineValue(req.getSession(), "lineDisplayName", user.getDisplayName()));
            fields.put("email", OnboardingSupport.lineValue(req.getSession(), "lineEmail", user.getEmail()));
            fields.put("phone", "");
            fields.put("iconImageUrl", "");
            fields.put("company", "");
            fields.put("emailNotify", "on");
            // 再表示する連絡先と画像の参照先は、オーナーのプロフィールから取得する。
            try {
                fields.putAll(createProfileDao().findProfileContact(user.getUserId(), "OWNER"));
            } catch (SQLException e) {
                OnboardingSupport.logDatabaseError(req, e);
                req.setAttribute("errorMessage", "登録済みの電話番号・アイコン画像を取得できませんでした。時間をおいて画面を開き直してください。");
            }
        }
        req.setAttribute("formValues", fields);
        req.setAttribute("displayName", fields.get("representativeName"));
        req.setAttribute("email", fields.get("email"));
        req.setAttribute("role", "owner");
        req.setAttribute("roleLabel", "物件オーナー");
        req.setAttribute("csrfToken", OnboardingSupport.csrfToken(req.getSession()));
        req.setAttribute("selectedIndustries", industries);
        try {
            req.setAttribute("industryChoices", createProfileDao().listIndustries());
        } catch (SQLException e) {
            OnboardingSupport.logDatabaseError(req, e);
            req.setAttribute("industryChoices", List.of());
            if (req.getAttribute("errorMessage") == null) {
                req.setAttribute("errorMessage", "誘致希望業種を取得できませんでした。時間をおいて画面を開き直してください。");
            }
        }
        req.getRequestDispatcher("/WEB-INF/views/auth/owner_setup.jsp").forward(req, res);
    }
}
