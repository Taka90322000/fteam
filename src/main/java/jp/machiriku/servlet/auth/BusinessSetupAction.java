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

/** 事業主はメールだけをLINEから初期設定し、事業者情報と希望条件をDBへ保存する。 */
public class BusinessSetupAction extends Action {
    protected ProfileDao createProfileDao() { return new ProfileDao(); }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        User user = OnboardingSupport.requireUser(req, res, "BUSINESS");
        if (user == null) { return; }
        if (!"GET".equalsIgnoreCase(req.getMethod()) && !"POST".equalsIgnoreCase(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        boolean posting = "POST".equalsIgnoreCase(req.getMethod());
        Map<String, String> fields;
        Set<String> industries = Set.of(), qualifications = Set.of();
        if (posting) {
            req.setCharacterEncoding("UTF-8");
            if (!OnboardingSupport.validCsrf(req)) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
            fields = OnboardingSupport.fields(req, "businessName", "representativeName", "email", "phone", "preferredArea", "preferredAreaPoints",
                    "employeeCount", "mgmtExperience", "industryExperience", "iconImageUrl", "emailNotify");
            industries = OnboardingSupport.selection(req, "preferredIndustry");
            qualifications = OnboardingSupport.selection(req, "qualification");
            try {
                var input = RegistrationForms.business(fields, OnboardingSupport.values(req, "preferredIndustry"),
                        OnboardingSupport.values(req, "qualification"));
                User saved = createProfileDao().saveBusiness(user.getUserId(), input);
                OnboardingSupport.completed(req, saved, input.representativeName());
                req.getSession().setAttribute("businessName", input.businessName());
                req.getSession().setAttribute("phone", input.phoneE164());
                req.getSession().setAttribute("iconImageUrl", input.iconImageUrl() == null ? "" : input.iconImageUrl());
                req.getSession().setAttribute("preferredArea", input.preferredArea());
                req.getSession().setAttribute("preferredAreaPoints", fields.get("preferredAreaPoints"));
                res.sendRedirect(req.getContextPath() + "/index.jsp?onboarded=1");
                return;
            } catch (IllegalArgumentException e) {
                req.setAttribute("errorMessage", e.getMessage());
            } catch (SQLException e) {
                OnboardingSupport.logDatabaseError(req, e);
                if ("28000".equals(e.getSQLState())) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
                req.setAttribute("errorMessage", "23505".equals(e.getSQLState())
                        ? "このメールアドレスまたは電話番号は既に登録されています。入力内容を確認してください。"
                        : "登録内容を保存できませんでした。入力内容と地図の範囲を確認して、もう一度お試しください。");
            }
        } else {
            fields = new LinkedHashMap<>();
            fields.put("email", OnboardingSupport.lineValue(req.getSession(), "lineEmail", user.getEmail()));
            fields.put("businessName", "");
            fields.put("representativeName", "");
            fields.put("phone", "");
            fields.put("iconImageUrl", "");
            fields.put("employeeCount", "1");
            fields.put("mgmtExperience", "");
            fields.put("industryExperience", "");
            fields.put("emailNotify", "on");
            // 再表示する連絡先と画像の参照先は、事業主のプロフィールから取得する。
            try {
                fields.putAll(createProfileDao().findProfileContact(user.getUserId(), "BUSINESS"));
            } catch (SQLException e) {
                OnboardingSupport.logDatabaseError(req, e);
                req.setAttribute("errorMessage", "登録済みの電話番号・アイコン画像を取得できませんでした。時間をおいて画面を開き直してください。");
            }
        }
        req.setAttribute("formValues", fields);
        req.setAttribute("displayName", fields.get("representativeName"));
        req.setAttribute("email", fields.get("email"));
        req.setAttribute("role", "business");
        req.setAttribute("roleLabel", "出店事業主");
        req.setAttribute("selectedIndustries", industries);
        req.setAttribute("selectedQualifications", qualifications);
        req.setAttribute("csrfToken", OnboardingSupport.csrfToken(req.getSession()));
        // 選択肢のIDを固定配列で推測せず、実際の業種・資格マスターから取得する。
        try {
            ProfileDao dao = createProfileDao();
            req.setAttribute("industryChoices", dao.listIndustries());
            req.setAttribute("qualificationChoices", dao.listQualifications());
        } catch (SQLException e) {
            OnboardingSupport.logDatabaseError(req, e);
            req.setAttribute("industryChoices", List.of());
            req.setAttribute("qualificationChoices", List.of());
            if (req.getAttribute("errorMessage") == null) {
                req.setAttribute("errorMessage", "業種・資格の選択肢を取得できませんでした。時間をおいて画面を開き直してください。");
            }
        }
        req.getRequestDispatcher("/WEB-INF/views/auth/business_setup.jsp").forward(req, res);
    }
}
