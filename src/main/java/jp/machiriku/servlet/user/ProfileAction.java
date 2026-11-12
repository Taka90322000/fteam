package jp.machiriku.servlet.user;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.BusinessProfileInput;
import jp.machiriku.bean.OwnerProfileInput;
import jp.machiriku.bean.RegistrationChoice;
import jp.machiriku.bean.ResidentProfileInput;
import jp.machiriku.bean.User;
import jp.machiriku.dao.ProfileDao;
import jp.machiriku.registration.RegistrationForms;
import jp.machiriku.tool.Action;

/** 登録済み会員のプロフィール・基本情報設定画面の表示および更新を行う専用Action。 */
public class ProfileAction extends Action {
    private final ProfileDao profileDao;

    public ProfileAction() {
        this(null);
    }

    public ProfileAction(ProfileDao profileDao) {
        this.profileDao = profileDao;
    }

    protected ProfileDao createProfileDao() {
        return profileDao != null ? profileDao : new ProfileDao();
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        User user = ProfileSupport.requireActiveUser(req, res);
        if (user == null) { return; }

        if (!"GET".equalsIgnoreCase(req.getMethod()) && !"POST".equalsIgnoreCase(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        String role = user.getUserRole();
        if ("BUSINESS".equals(role)) {
            handleBusiness(req, res, user);
        } else if ("OWNER".equals(role)) {
            handleOwner(req, res, user);
        } else {
            handleResident(req, res, user);
        }
    }

    private void handleResident(HttpServletRequest req, HttpServletResponse res, User user) throws Exception {
        boolean posting = "POST".equalsIgnoreCase(req.getMethod());
        Map<String, String> fields = null;
        if (posting) {
            req.setCharacterEncoding("UTF-8");
            if (!ProfileSupport.validCsrf(req)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            fields = ProfileSupport.fields(req, "name", "email", "ageGroup", "gender", "industry",
                    "preferredIndustries", "livingAddress", "livingLat", "livingLng",
                    "workAddress", "workLat", "workLng", "emailNotify");
            try {
                ResidentProfileInput input = RegistrationForms.resident(fields);
                User saved = createProfileDao().saveResident(user.getUserId(), input);
                ProfileSupport.updateSession(req, saved, saved.getDisplayName());
                req.getSession().setAttribute("userIndustry", fields.get("industry"));
                req.getSession().setAttribute("userPreferredIndustries", input.preferredIndustryCodes());
                res.sendRedirect(req.getContextPath() + "/index.jsp?profile_saved=1");
                return;
            } catch (IllegalArgumentException e) {
                req.setAttribute("errorMessage", e.getMessage());
            } catch (SQLException e) {
                ProfileSupport.logDatabaseError(req, e);
                if ("28000".equals(e.getSQLState())) {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                req.setAttribute("errorMessage", "23505".equals(e.getSQLState())
                        ? "このメールアドレスは既に登録されています。入力内容を確認してください。"
                        : "設定内容を保存できませんでした。入力内容をご確認の上、もう一度お試しください。");
            }
        }

        if (fields == null) {
            fields = new LinkedHashMap<>();
            fields.put("name", user.getDisplayName());
            fields.put("email", user.getEmail() == null ? "" : user.getEmail());
            fields.put("ageGroup", "未設定");
            fields.put("gender", "回答しない");
            fields.put("industry", "");
            fields.put("preferredIndustries", "[]");
            fields.put("emailNotify", "on");
            try {
                fields.putAll(createProfileDao().findResidentFields(user.getUserId()));
            } catch (SQLException e) {
                ProfileSupport.logDatabaseError(req, e);
            }
        }

        req.setAttribute("formValues", fields);
        req.setAttribute("displayName", fields.get("name"));
        req.setAttribute("email", fields.get("email"));
        req.setAttribute("role", "resident");
        req.setAttribute("roleLabel", "地域住民");
        req.setAttribute("csrfToken", ProfileSupport.csrfToken(req.getSession()));
        req.getRequestDispatcher("/WEB-INF/views/user/resident_profile.jsp").forward(req, res);
    }

    private void handleBusiness(HttpServletRequest req, HttpServletResponse res, User user) throws Exception {
        boolean posting = "POST".equalsIgnoreCase(req.getMethod());
        Map<String, String> fields = null;
        Set<String> industries = Collections.emptySet();
        Set<String> qualifications = Collections.emptySet();

        if (posting) {
            req.setCharacterEncoding("UTF-8");
            if (!ProfileSupport.validCsrf(req)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            fields = ProfileSupport.fields(req, "businessName", "representativeName", "email", "phone",
                    "preferredArea", "preferredAreaPoints", "employeeCount", "mgmtExperience",
                    "industryExperience", "iconImageUrl", "emailNotify");
            industries = ProfileSupport.selection(req, "preferredIndustry");
            qualifications = ProfileSupport.selection(req, "qualification");
            try {
                BusinessProfileInput input = RegistrationForms.business(fields,
                        ProfileSupport.values(req, "preferredIndustry"),
                        ProfileSupport.values(req, "qualification"));
                User saved = createProfileDao().saveBusiness(user.getUserId(), input);
                ProfileSupport.updateSession(req, saved, input.representativeName());
                req.getSession().setAttribute("businessName", input.businessName());
                req.getSession().setAttribute("phone", input.phoneE164());
                req.getSession().setAttribute("iconImageUrl", input.iconImageUrl() == null ? "" : input.iconImageUrl());
                req.getSession().setAttribute("preferredArea", input.preferredArea());
                req.getSession().setAttribute("preferredAreaPoints", fields.get("preferredAreaPoints"));
                res.sendRedirect(req.getContextPath() + "/index.jsp?profile_saved=1");
                return;
            } catch (IllegalArgumentException e) {
                req.setAttribute("errorMessage", e.getMessage());
            } catch (SQLException e) {
                ProfileSupport.logDatabaseError(req, e);
                if ("28000".equals(e.getSQLState())) {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                req.setAttribute("errorMessage", "23505".equals(e.getSQLState())
                        ? "このメールアドレスまたは電話番号は既に登録されています。入力内容を確認してください。"
                        : "設定内容を保存できませんでした。入力内容をご確認の上、もう一度お試しください。");
            }
        }

        List<RegistrationChoice> industryChoices = Collections.emptyList();
        List<RegistrationChoice> qualificationChoices = Collections.emptyList();
        try {
            industryChoices = createProfileDao().listIndustries();
            qualificationChoices = createProfileDao().listQualifications();
        } catch (SQLException e) {
            ProfileSupport.logDatabaseError(req, e);
        }

        if (fields == null) {
            fields = new LinkedHashMap<>();
            fields.put("email", user.getEmail() == null ? "" : user.getEmail());
            fields.put("businessName", "");
            fields.put("representativeName", user.getDisplayName());
            fields.put("phone", "");
            fields.put("iconImageUrl", "");
            fields.put("employeeCount", "1");
            fields.put("mgmtExperience", "");
            fields.put("industryExperience", "");
            fields.put("emailNotify", "on");
            try {
                fields.putAll(createProfileDao().findProfileContact(user.getUserId(), "BUSINESS"));
            } catch (SQLException e) {
                ProfileSupport.logDatabaseError(req, e);
            }
        }

        req.setAttribute("formValues", fields);
        req.setAttribute("displayName", fields.get("representativeName"));
        req.setAttribute("email", fields.get("email"));
        req.setAttribute("role", "business");
        req.setAttribute("roleLabel", "出店事業主");
        req.setAttribute("industryChoices", industryChoices);
        req.setAttribute("qualificationChoices", qualificationChoices);
        req.setAttribute("selectedIndustries", industries);
        req.setAttribute("selectedQualifications", qualifications);
        req.setAttribute("csrfToken", ProfileSupport.csrfToken(req.getSession()));
        req.getRequestDispatcher("/WEB-INF/views/user/business_profile.jsp").forward(req, res);
    }

    private void handleOwner(HttpServletRequest req, HttpServletResponse res, User user) throws Exception {
        boolean posting = "POST".equalsIgnoreCase(req.getMethod());
        Map<String, String> fields = null;
        Set<String> industries = Collections.emptySet();

        if (posting) {
            req.setCharacterEncoding("UTF-8");
            if (!ProfileSupport.validCsrf(req)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            fields = ProfileSupport.fields(req, "representativeName", "company", "email", "phone", "iconImageUrl", "emailNotify");
            industries = ProfileSupport.selection(req, "preferredIndustry");
            try {
                OwnerProfileInput input = RegistrationForms.owner(fields, ProfileSupport.values(req, "preferredIndustry"));
                User saved = createProfileDao().saveOwner(user.getUserId(), input);
                ProfileSupport.updateSession(req, saved, input.representativeName());
                req.getSession().setAttribute("company", input.company() == null ? "" : input.company());
                req.getSession().setAttribute("phone", input.phoneE164());
                req.getSession().setAttribute("iconImageUrl", input.iconImageUrl() == null ? "" : input.iconImageUrl());
                req.getSession().setAttribute("preferredIndustries", input.industryTypeIds());
                req.getSession().setAttribute("emailNotify", input.emailNotify() && saved.getEmail() != null);
                res.sendRedirect(req.getContextPath() + "/index.jsp?profile_saved=1");
                return;
            } catch (IllegalArgumentException e) {
                req.setAttribute("errorMessage", e.getMessage());
            } catch (SQLException e) {
                ProfileSupport.logDatabaseError(req, e);
                if ("28000".equals(e.getSQLState())) {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                req.setAttribute("errorMessage", "23505".equals(e.getSQLState())
                        ? "このメールアドレスまたは電話番号は既に登録されています。入力内容を確認してください。"
                        : "設定内容を保存できませんでした。入力内容をご確認の上、もう一度お試しください。");
            }
        }

        List<RegistrationChoice> industryChoices = Collections.emptyList();
        try {
            industryChoices = createProfileDao().listIndustries();
        } catch (SQLException e) {
            ProfileSupport.logDatabaseError(req, e);
        }

        if (fields == null) {
            fields = new LinkedHashMap<>();
            fields.put("representativeName", user.getDisplayName());
            fields.put("company", "");
            fields.put("email", user.getEmail() == null ? "" : user.getEmail());
            fields.put("phone", "");
            fields.put("iconImageUrl", "");
            fields.put("emailNotify", "on");
            try {
                fields.putAll(createProfileDao().findProfileContact(user.getUserId(), "OWNER"));
            } catch (SQLException e) {
                ProfileSupport.logDatabaseError(req, e);
            }
        }

        req.setAttribute("formValues", fields);
        req.setAttribute("displayName", fields.get("representativeName"));
        req.setAttribute("email", fields.get("email"));
        req.setAttribute("role", "owner");
        req.setAttribute("roleLabel", "物件オーナー");
        req.setAttribute("industryChoices", industryChoices);
        req.setAttribute("selectedIndustries", industries);
        req.setAttribute("csrfToken", ProfileSupport.csrfToken(req.getSession()));
        req.getRequestDispatcher("/WEB-INF/views/user/owner_profile.jsp").forward(req, res);
    }
}
