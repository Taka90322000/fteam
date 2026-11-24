package jp.machiriku.servlet.auth;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.User;
import jp.machiriku.dao.ProfileDao;
import jp.machiriku.registration.RegistrationForms;
import jp.machiriku.servlet.ProfileIconSupport;
import jp.machiriku.tool.Action;

/** 事業主はメールだけをLINEから初期設定し、事業者情報と希望条件をDBへ保存する。 */
public class BusinessSetupAction extends Action {
    protected ProfileDao createProfileDao() { return new ProfileDao(); }

    /**
     * 初期設定フォームを表示し、送信時は画像とプロフィールを保存する。
     * @param req 認証済みの会員から受け取ったリクエスト
     * @param res 画面表示または保存結果のレスポンス
     * @throws Exception セッションの確認や画面転送を続行できなかった場合
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        HttpSession session = req.getSession(false);
        User user = OnboardingSupport.requireUser(req, res, session, "BUSINESS", this::createProfileDao);
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
            if (!OnboardingSupport.validCsrf(req, session)) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
            fields = OnboardingSupport.fields(req, "businessName", "representativeName", "email", "phone", "secondaryPhone", "preferredArea", "preferredAreaPoints",
                    "employeeCount", "mgmtExperience", "industryExperience", "emailNotify");
            industries = OnboardingSupport.selection(req, "preferredIndustry");
            qualifications = OnboardingSupport.selection(req, "qualification");
            try {
                var input = RegistrationForms.business(fields, OnboardingSupport.values(req, "preferredIndustry"),
                        OnboardingSupport.values(req, "qualification"));
                // 画像を検証して発行した参照先を、本登録のトランザクションに渡す。
                ProfileDao.SavedProfile savedProfile = ProfileIconSupport.save(req, user,
                        icon -> createProfileDao().saveBusiness(user.getUserId(), input, true, icon));
                String preferredAreaPoints = fields.get("preferredAreaPoints");
                OnboardingSupport.completed(req, res, session, savedProfile.user(), input.representativeName(), current -> {
                    current.setAttribute("businessName", input.businessName());
                    current.setAttribute("phone", input.phoneE164());
                    current.setAttribute("iconImageUrl", savedProfile.iconImageUrl() == null ? "" : savedProfile.iconImageUrl());
                    current.setAttribute("preferredArea", input.preferredArea());
                    current.setAttribute("preferredAreaPoints", preferredAreaPoints);
                });
                return;
            } catch (IllegalArgumentException e) {
                req.setAttribute("errorMessage", e.getMessage());
            } catch (IOException e) {
                // 画像を読み書きできない場合、以前のアイコンを表示したフォームへ戻す。
                req.getServletContext().log("Could not save a business profile icon.");
                req.setAttribute("errorMessage", "アイコン画像を保存できませんでした。時間をおいて再度お試しください。");
            } catch (SQLException e) {
                OnboardingSupport.logDatabaseError(req, e);
                if ("28000".equals(e.getSQLState())) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
                req.setAttribute("errorMessage", "23505".equals(e.getSQLState())
                        ? "このメールアドレスは既に登録されています。入力内容を確認してください。"
                        : "登録内容を保存できませんでした。入力内容と地図の範囲を確認して、もう一度お試しください。");
            }
            ProfileIconSupport.restorePreview(req, createProfileDao(), user, fields);
        } else {
            fields = new LinkedHashMap<>();
            fields.put("email", OnboardingSupport.lineValue(session, "lineEmail", user.getEmail()));
            fields.put("businessName", "");
            fields.put("representativeName", "");
            fields.put("phone", "");
            fields.put("secondaryPhone", "");
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
        if (OnboardingSupport.prepareView(req, res, session)) {
            req.getRequestDispatcher("/WEB-INF/views/auth/business_setup.jsp").forward(req, res);
        }
    }
}
