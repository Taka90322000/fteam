package jp.machiriku.servlet.user;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.BusinessProfileInput;
import jp.machiriku.bean.OwnerProfileInput;
import jp.machiriku.bean.ProfileSettings;
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
        HttpSession session = req.getSession(false);
        User user = ProfileSupport.requireActiveUser(req, res, session, this::createProfileDao);
        if (user == null) { return; }

        if (!"GET".equalsIgnoreCase(req.getMethod()) && !"POST".equalsIgnoreCase(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        String role = user.getUserRole();
        if ("BUSINESS".equals(role)) {
            handleBusiness(req, res, session, user);
        } else if ("OWNER".equals(role)) {
            handleOwner(req, res, session, user);
        } else {
            handleResident(req, res, session, user);
        }
    }

    private void handleResident(HttpServletRequest req, HttpServletResponse res, HttpSession session, User user) throws Exception {
        boolean posting = "POST".equalsIgnoreCase(req.getMethod());
        Map<String, String> fields = null;
        if (posting) {
            req.setCharacterEncoding("UTF-8");
            if (!ProfileSupport.validCsrf(req, session)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            fields = ProfileSupport.fields(req, "name", "email", "ageGroup", "gender", "industry",
                    "preferredIndustries", "livingAddress", "livingLat", "livingLng",
                    "workAddress", "workLat", "workLng", "emailNotify");
            try {
                ResidentProfileInput input = RegistrationForms.resident(fields);
                User saved = createProfileDao().saveResident(user.getUserId(), input);
                String industry = fields.get("industry");
                ProfileSupport.saved(req, res, session, saved, saved.getDisplayName(), current -> {
                    current.setAttribute("userIndustry", industry);
                    current.setAttribute("userPreferredIndustries", input.preferredIndustryCodes());
                });
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

        List<Map<String, Object>> residentTaxonomy = Collections.emptyList();
        try {
            residentTaxonomy = createProfileDao().listResidentTaxonomy();
        } catch (SQLException e) {
            ProfileSupport.logDatabaseError(req, e);
            res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        if (fields == null) {
            fields = new LinkedHashMap<>();
            fields.put("name", user.getDisplayName());
            fields.put("email", user.getEmail() == null ? "" : user.getEmail());
            fields.put("ageGroup", "未設定");
            fields.put("gender", "回答しない");
            fields.put("industry", "");
            fields.put("preferredIndustries", "[]");
            fields.put("emailNotify", "");
            try {
                fields.putAll(createProfileDao().findResidentFields(user.getUserId()));
            } catch (SQLException e) {
                ProfileSupport.logDatabaseError(req, e);
                res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
        }

        req.setAttribute("formValues", fields);
        req.setAttribute("displayName", fields.get("name"));
        req.setAttribute("email", fields.get("email"));
        req.setAttribute("role", "resident");
        req.setAttribute("roleLabel", "地域住民");
        req.setAttribute("residentTaxonomy", residentTaxonomy);
        if (ProfileSupport.prepareView(req, res, session, user)) {
            req.getRequestDispatcher("/WEB-INF/views/user/resident_profile.jsp").forward(req, res);
        }
    }

    private void handleBusiness(HttpServletRequest req, HttpServletResponse res, HttpSession session, User user) throws Exception {
        boolean posting = "POST".equalsIgnoreCase(req.getMethod());
        Map<String, String> fields = null;
        Set<String> industries = Collections.emptySet();
        Set<String> qualifications = Collections.emptySet();

        if (posting) {
            req.setCharacterEncoding("UTF-8");
            if (!ProfileSupport.validCsrf(req, session)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            fields = ProfileSupport.fields(req, "businessName", "representativeName", "email", "phone", "secondaryPhone",
                    "preferredArea", "preferredAreaPoints", "employeeCount", "mgmtExperience",
                    "industryExperience", "iconImageUrl", "emailNotify");
            industries = ProfileSupport.selection(req, "preferredIndustry");
            qualifications = ProfileSupport.selection(req, "qualification");
            try {
                BusinessProfileInput input = RegistrationForms.business(fields,
                        ProfileSupport.values(req, "preferredIndustry"),
                        ProfileSupport.values(req, "qualification"));
                User saved = createProfileDao().saveBusiness(user.getUserId(), input);
                String preferredAreaPoints = fields.get("preferredAreaPoints");
                ProfileSupport.saved(req, res, session, saved, input.representativeName(), current -> {
                    current.setAttribute("businessName", input.businessName());
                    current.setAttribute("phone", input.phoneE164());
                    current.setAttribute("iconImageUrl", input.iconImageUrl() == null ? "" : input.iconImageUrl());
                    current.setAttribute("preferredArea", input.preferredArea());
                    current.setAttribute("preferredAreaPoints", preferredAreaPoints);
                });
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

        List<RegistrationChoice> industryChoices = Collections.emptyList();
        List<RegistrationChoice> qualificationChoices = Collections.emptyList();
        try {
            industryChoices = createProfileDao().listIndustries();
            qualificationChoices = createProfileDao().listQualifications();
        } catch (SQLException e) {
            ProfileSupport.logDatabaseError(req, e);
            res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        if (fields == null) {
            try {
                // 保存済みの項目と選択IDをまとめて復元する。
                ProfileSettings settings = createProfileDao().findProfileSettings(user.getUserId(), "BUSINESS");
                if (settings == null) {
                    res.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                fields = new LinkedHashMap<>(settings.fields());
                industries = settings.selectedIndustries();
                qualifications = settings.selectedQualifications();
            } catch (SQLException e) {
                ProfileSupport.logDatabaseError(req, e);
                // 復元に失敗した空のフォームから、保存済み情報を上書きさせない。
                res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
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
        if (ProfileSupport.prepareView(req, res, session, user)) {
            req.getRequestDispatcher("/WEB-INF/views/user/business_profile.jsp").forward(req, res);
        }
    }

    private void handleOwner(HttpServletRequest req, HttpServletResponse res, HttpSession session, User user) throws Exception {
        boolean posting = "POST".equalsIgnoreCase(req.getMethod());
        Map<String, String> fields = null;
        Set<String> industries = Collections.emptySet();

        if (posting) {
            req.setCharacterEncoding("UTF-8");
            if (!ProfileSupport.validCsrf(req, session)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            fields = ProfileSupport.fields(req, "representativeName", "company", "email", "phone", "secondaryPhone", "iconImageUrl", "emailNotify");
            industries = ProfileSupport.selection(req, "preferredIndustry");
            try {
                OwnerProfileInput input = RegistrationForms.owner(fields, ProfileSupport.values(req, "preferredIndustry"));
                User saved = createProfileDao().saveOwner(user.getUserId(), input);
                ProfileSupport.saved(req, res, session, saved, input.representativeName(), current -> {
                    current.setAttribute("company", input.company() == null ? "" : input.company());
                    current.setAttribute("phone", input.phoneE164());
                    current.setAttribute("iconImageUrl", input.iconImageUrl() == null ? "" : input.iconImageUrl());
                    current.setAttribute("preferredIndustries", input.industryTypeIds());
                    current.setAttribute("emailNotify", input.emailNotify() && saved.getEmail() != null);
                });
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

        List<RegistrationChoice> industryChoices = Collections.emptyList();
        try {
            industryChoices = createProfileDao().listIndustries();
        } catch (SQLException e) {
            ProfileSupport.logDatabaseError(req, e);
            res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        if (fields == null) {
            try {
                // LINE表示名とは別に保存した代表者名・法人名も復元する。
                ProfileSettings settings = createProfileDao().findProfileSettings(user.getUserId(), "OWNER");
                if (settings == null) {
                    res.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                fields = new LinkedHashMap<>(settings.fields());
                industries = settings.selectedIndustries();
            } catch (SQLException e) {
                ProfileSupport.logDatabaseError(req, e);
                res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
        }

        req.setAttribute("formValues", fields);
        req.setAttribute("displayName", fields.get("representativeName"));
        req.setAttribute("email", fields.get("email"));
        req.setAttribute("role", "owner");
        req.setAttribute("roleLabel", "物件オーナー");
        req.setAttribute("industryChoices", industryChoices);
        req.setAttribute("selectedIndustries", industries);
        if (ProfileSupport.prepareView(req, res, session, user)) {
            req.getRequestDispatcher("/WEB-INF/views/user/owner_profile.jsp").forward(req, res);
        }
    }
}
