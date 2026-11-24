package jp.machiriku.servlet.management;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.LocationInput;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminUserItem;
import jp.machiriku.bean.management.ManagementLabels;
import jp.machiriku.bean.management.ManagementProblem;
import jp.machiriku.bean.management.PageResult;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.dao.management.AdminUserDao;
import jp.machiriku.registration.RegistrationForms;
import jp.machiriku.tool.Action;
import jp.machiriku.tool.PhoneNumbers;

/** 管理者が利用区分別の会員検索・詳細閲覧およびプロフィール・希望業種・生活拠点・保有資格・利用状態の更新を行うAction。 */
public final class AdminUsersAction extends Action {
    /** 希望業種選択UIへカテゴリ・選択済みIDを渡すためのJSONシリアライザー。 */
    private static final ObjectMapper JSON = new ObjectMapper();
    /** 地域住民の詳細画面タブ一覧。 */
    private static final Map<String, String> RESIDENT_TABS = ManagementFormat.options(
            "locations", "生活拠点",
            "votes", "投票履歴",
            "answers", "アンケート回答履歴",
            "held", "保持クーポン",
            "used", "クーポン使用履歴");
    /** 事業主の詳細画面タブ一覧。 */
    private static final Map<String, String> BUSINESS_TABS = ManagementFormat.options(
            "qualifications", "保有資格",
            "applications", "出店申し込み",
            "tenancies", "契約物件",
            "plans", "出店計画",
            "offers", "出店オファー",
            "surveys", "作成アンケート",
            "coupons", "作成クーポン",
            "reports", "受けた通報");
    /** 物件オーナーの詳細画面タブ一覧。 */
    private static final Map<String, String> OWNER_TABS = ManagementFormat.options(
            "properties", "保有物件",
            "offers", "オファー履歴",
            "reports", "受けた通報");
    /** 地域住民プロフィール編集の年齢区分選択肢。 */
    private static final Map<String, String> AGE_CHOICES = ManagementFormat.options(
            "10代", "10代", "20代", "20代", "30代", "30代",
            "40代", "40代", "50代", "50代", "60代以上", "60代以上");
    /** 地域住民プロフィール編集の性別選択肢。 */
    private static final Map<String, String> GENDER_CHOICES = ManagementFormat.options(
            "男性", "男性", "女性", "女性", "その他", "その他");
    /** 保有資格の有効状態選択肢。 */
    private static final Map<String, String> ACTIVE_CHOICES = ManagementFormat.options(
            "true", "有効", "false", "無効");

    /** 管理者権限チェックに使うDAO。 */
    private final AdminAuditDao auditDao;
    /** 会員管理DAO。 */
    private final AdminUserDao userDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminUsersAction() {
        this(new AdminAuditDao(), new AdminUserDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param auditDao 管理者権限チェックDAO。
     * @param userDao 会員管理DAO。
     */
    AdminUsersAction(AdminAuditDao auditDao, AdminUserDao userDao) {
        this.auditDao = auditDao;
        this.userDao = userDao;
    }

    /**
     * GETでは会員一覧または会員詳細を表示し、POSTでは利用状態・プロフィール・希望業種・生活拠点・保有資格を更新する。
     * @param req FrontControllerから受け取るHTTPリクエスト。
     * @param res 画面表示、エラー応答や更新後の遷移に使用するHTTPレスポンス。
     * @throws Exception 画面処理を完了できない場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 1. 管理者権限を確認する。
        User admin = ManagementAuth.requireAdmin(req, res, auditDao);
        if (admin == null) {
            return;
        }

        // 2. POSTの場合は各種会員更新処理を実行する。
        if ("POST".equals(req.getMethod())) {
            handlePost(req, res, admin);
            return;
        }

        // 3. GET/HEADでは会員IDの有無に応じて一覧または詳細を表示する。
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }
        try {
            FormReader input = new FormReader(req);
            Integer userId = input.intId("id", false);
            if (userId == null) {
                showList(req, res, input);
            } else {
                showDetail(req, res, input, admin.getUserId(), userId);
            }
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * 利用区分別の会員検索・一覧画面を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showList(HttpServletRequest req, HttpServletResponse res, FormReader input) throws Exception {
        String role = input.value("role").isEmpty()
                ? "RESIDENT"
                : input.choice("role", "利用区分", "RESIDENT", "BUSINESS", "OWNER");
        String query = input.search();
        String status = input.value("status");
        LocalDate from = input.date("from", "開始日");
        LocalDate to = input.date("to", "終了日");

        List<AdminUserItem> rows = userDao.findUsers(
                role, status, from, to, query, FormReader.PAGE_SIZE + 1, input.offset());
        PageResult<AdminUserItem> pageResult = input.toPageResult(rows);

        ManagementAuth.preparePage(req, "ユーザー管理", true, true);
        req.setAttribute("role", role);
        req.setAttribute("roleLabel", ManagementLabels.label(ManagementLabels.USER_ROLES, role));
        req.setAttribute("query", query);
        req.setAttribute("status", status);
        req.setAttribute("from", from);
        req.setAttribute("to", to);
        req.setAttribute("roles", ManagementLabels.USER_ROLES);
        req.setAttribute("states", ManagementLabels.USER_STATES);
        req.setAttribute("memberPage", pageResult);
        req.getRequestDispatcher("/WEB-INF/views/management/admin/users/list.jsp").forward(req, res);
    }

    /**
     * 会員詳細画面と選択されたタブの内容を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @param actorId 閲覧している管理者の会員ID。
     * @param userId 表示対象の会員ID。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showDetail(HttpServletRequest req, HttpServletResponse res, FormReader input,
            int actorId, int userId) throws Exception {
        AdminUserItem member = userDao.findUserById(userId);
        if (member == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
            return;
        }
        String role = member.getUserRole();
        Map<String, String> roleTabs = tabsForRole(role);
        String tab = input.value("tab").isEmpty() ? "profile" : input.value("tab");
        if (!isAllowedTab(roleTabs, tab)) {
            throw new ManagementProblem(404, "指定の会員情報は表示できません。");
        }

        ManagementAuth.preparePage(req, member.getUserRoleLabel() + "　" + member.getDisplayName(), true, true);
        req.setAttribute("member", member);
        req.setAttribute("selectedTab", tab);
        req.setAttribute("roleTabs", roleTabs);

        boolean editable = !member.isWithdrawn();
        if ("profile".equals(tab)) {
            AdminUserItem.Profile profile = userDao.findRoleProfile(role, userId);
            boolean canChangeStatus = editable && userId != actorId && !userDao.hasActiveAdminGrant(userId);
            req.setAttribute("profile", profile);
            req.setAttribute("editable", editable);
            req.setAttribute("canChangeStatus", canChangeStatus);
            req.setAttribute("ageChoices", AGE_CHOICES);
            req.setAttribute("genderChoices", GENDER_CHOICES);
            if (editable) {
                req.setAttribute("accountActions", userDao.findRecentAccountActions(userId));
            }
        } else if ("preferences".equals(tab)) {
            List<AdminUserItem.Preference> preferences = userDao.findPreferences(userId);
            // 選択UI初期表示用とフォールバック送信用の選択済み業種ID一覧を組み立てる。
            List<Integer> selectedIds = new ArrayList<>();
            List<String> selectedIdTexts = new ArrayList<>();
            for (AdminUserItem.Preference item : preferences) {
                selectedIds.add(item.getIndustryTypeId());
                selectedIdTexts.add(String.valueOf(item.getIndustryTypeId()));
            }
            req.setAttribute("preferences", preferences);
            req.setAttribute("selectedPreferenceIds", selectedIds);
            req.setAttribute("editable", editable);
            if (editable) {
                req.setAttribute("preferenceTaxonomyJson",
                        JSON.writeValueAsString(userDao.findPreferenceTaxonomy(userId)));
                req.setAttribute("selectedPreferenceJson", JSON.writeValueAsString(selectedIdTexts));
            }
        } else if ("locations".equals(tab)) {
            List<AdminUserItem.Location> locations = userDao.findLocations(userId);
            // 会員登録画面と同じ「居住地（HOME）」と「勤務先・学校（WORK）」の2枠へ既存データを割り当てる。
            AdminUserItem.Location homeLocation = null;
            AdminUserItem.Location workLocation = null;
            AdminUserItem.Location legacySchoolLocation = null;
            for (AdminUserItem.Location item : locations) {
                if ("HOME".equals(item.getLocationKind())) {
                    homeLocation = item;
                } else if ("WORK".equals(item.getLocationKind())) {
                    workLocation = item;
                } else if ("SCHOOL".equals(item.getLocationKind())) {
                    legacySchoolLocation = item;
                }
            }
            // WORKが未登録で旧SCHOOLのみ存在する場合は、勤務先・学校の編集欄へ引き継ぐ。
            if (workLocation == null && legacySchoolLocation != null) {
                workLocation = legacySchoolLocation;
            }
            req.setAttribute("locations", locations);
            req.setAttribute("homeLocation", homeLocation);
            req.setAttribute("workLocation", workLocation);
            req.setAttribute("editable", editable);
        } else if ("qualifications".equals(tab)) {
            List<AdminUserItem.Qualification> qualifications = userDao.findQualifications(userId);
            boolean canEditQualification = editable && userDao.findBusinessProfile(userId) != null;
            req.setAttribute("qualifications", qualifications);
            req.setAttribute("editable", canEditQualification);
            if (canEditQualification) {
                Integer selectedId = input.intId("industry", false);
                AdminUserItem.Qualification current = new AdminUserItem.Qualification();
                if (selectedId != null) {
                    for (AdminUserItem.Qualification item : qualifications) {
                        if (item.getQualificationId() == selectedId) {
                            current = item;
                            break;
                        }
                    }
                }
                req.setAttribute("selectedQualificationId", selectedId);
                req.setAttribute("currentQualification", current);
                req.setAttribute("qualificationChoices", userDao.findQualificationChoices(userId));
                req.setAttribute("activeChoices", ACTIVE_CHOICES);
            }
        } else {
            List<AdminUserItem.History> historyRows = loadHistory(tab, userId, FormReader.PAGE_SIZE + 1, input.offset());
            String heading = "inquiries".equals(tab) ? "問い合わせ履歴" : roleTabs.get(tab);
            req.setAttribute("historyHeading", heading);
            req.setAttribute("historyPage", input.toPageResult(historyRows));
        }

        req.getRequestDispatcher("/WEB-INF/views/management/admin/users/detail.jsp").forward(req, res);
    }

    /**
     * 会員状態の変更およびプロフィール・希望業種・生活拠点・保有資格の更新を処理する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param admin 操作する管理者。
     * @throws Exception 更新処理を完了できない場合。
     */
    private void handlePost(HttpServletRequest req, HttpServletResponse res, User admin) throws Exception {
        if (!ManagementAuth.verifyCsrf(req, res, true)) {
            return;
        }
        try {
            FormReader input = new FormReader(req);
            int userId = input.intId("id", true);
            String operation = input.choice("operation", "操作",
                    "suspend", "restore", "profile", "preferences", "location", "qualification");

            if ("suspend".equals(operation) || "restore".equals(operation)) {
                String reason = input.text("reason", "操作理由", 2000, true);
                userDao.changeAccountStatus(admin.getUserId(), userId, "suspend".equals(operation), reason);
                ManagementAuth.redirectSaved(req, res, ManagementPaths.ADMIN_USERS + "?id=" + userId);
                return;
            }

            if ("profile".equals(operation)) {
                saveProfile(input, admin.getUserId(), userId);
                ManagementAuth.redirectSaved(req, res, ManagementPaths.ADMIN_USERS + "?id=" + userId + "&tab=profile");
                return;
            }

            if ("preferences".equals(operation)) {
                Set<Integer> selectedIds = input.intIds("industries");
                userDao.updatePreferences(admin.getUserId(), userId, selectedIds);
                ManagementAuth.redirectSaved(req, res,
                        ManagementPaths.ADMIN_USERS + "?id=" + userId + "&tab=preferences");
                return;
            }

            if ("location".equals(operation)) {
                // 会員登録画面と共通の住所分割・座標検証ロジックで居住地と勤務先・学校を検証する。
                LocationInput home = RegistrationForms.location(
                        "HOME",
                        input.value("livingAddress"),
                        input.value("livingLat"),
                        input.value("livingLng"),
                        true);
                String workAddress = input.value("workAddress");
                String workLat = input.value("workLat");
                String workLng = input.value("workLng");
                LocationInput work = RegistrationForms.location("WORK", workAddress, workLat, workLng, false);
                // 勤務先・学校を入力した場合は会員登録画面と同様に地図上の座標も必須とする。
                if (work != null && (work.latitude() == null || work.longitude() == null)) {
                    throw new ManagementProblem(400, "勤務先・学校を登録する場合は地図上で位置を選択してください。");
                }
                // 住所が空欄なのに座標だけが残っている不整合入力を拒否する。
                if (work == null && (!workLat.isEmpty() || !workLng.isEmpty())) {
                    throw new ManagementProblem(400, "勤務先・学校を登録する場合は地図上で住所を選択してください。");
                }
                userDao.saveLocations(admin.getUserId(), userId, home, work);
                ManagementAuth.redirectSaved(req, res,
                        ManagementPaths.ADMIN_USERS + "?id=" + userId + "&tab=locations");
                return;
            }

            int qualificationId = input.intId("qualificationId", true);
            LocalDate obtained = input.date("obtained", "取得日");
            LocalDate expires = input.date("expires", "有効期限");
            if (obtained != null && expires != null && expires.isBefore(obtained)) {
                throw new ManagementProblem(400, "有効期限は取得日以降を指定してください。");
            }
            boolean active = Boolean.parseBoolean(input.choice("active", "有効状態", "true", "false"));
            userDao.upsertQualification(
                    admin.getUserId(),
                    userId,
                    qualificationId,
                    input.text("credential", "資格番号", 100, false),
                    obtained,
                    expires,
                    active);
            ManagementAuth.redirectSaved(req, res,
                    ManagementPaths.ADMIN_USERS + "?id=" + userId + "&tab=qualifications");
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * 会員の利用区分と本登録状況に応じてプロフィールフォームの入力を読み取り保存する。
     * @param input リクエスト読み取り器。
     * @param actorId 操作する管理者の会員ID。
     * @param userId 更新対象の会員ID。
     * @throws Exception DB照会・更新に失敗した場合。
     */
    private void saveProfile(FormReader input, int actorId, int userId) throws Exception {
        AdminUserItem member = userDao.findUserById(userId);
        if (member == null) {
            throw new ManagementProblem(404, "指定された会員が見つかりません。");
        }
        if (member.isWithdrawn()) {
            throw new ManagementProblem(409, "退会済み会員のプロフィールは変更できません。");
        }
        String displayName = input.text("name", "表示名", 100, true);
        String email = RegistrationForms.email(input.value("email"));

        // 区分別プロフィールが存在する会員のみ区分固有の入力項目を読み取り検証する。
        AdminUserItem.Profile updatedProfile = null;
        if (userDao.findRoleProfile(member.getUserRole(), userId) != null) {
            updatedProfile = new AdminUserItem.Profile();
            if ("RESIDENT".equals(member.getUserRole())) {
                updatedProfile.setAgeGroup(
                        input.choice("age", "年齢区分", "", "10代", "20代", "30代", "40代", "50代", "60代以上"));
                updatedProfile.setGender(
                        input.choice("gender", "性別", "", "男性", "女性", "その他"));
            } else {
                String phone = RegistrationForms.normalizePhone(input.value("phone"));
                String secondaryPhone = PhoneNumbers.toE164(input.value("secondaryPhone"), "追加の電話番号");
                if (phone.equals(secondaryPhone)) {
                    throw new ManagementProblem(400, "追加の電話番号には主番号と異なる番号を入力してください。");
                }
                updatedProfile.setRepresentativeName(input.text("representative", "代表者", 100, true));
                updatedProfile.setPhoneE164(phone);
                updatedProfile.setSecondaryPhoneE164(secondaryPhone);
                if ("OWNER".equals(member.getUserRole())) {
                    updatedProfile.setCompanyName(input.text("company", "法人名", 150, false));
                } else {
                    updatedProfile.setBusinessName(input.text("businessName", "事業者名", 150, true));
                    updatedProfile.setPreferredArea(input.text("areaName", "希望エリア", 150, true));
                    updatedProfile.setEmployeeCount(input.number("employees", "従業員数", 0, Integer.MAX_VALUE));
                    updatedProfile.setIndustryExperience(input.text("industryExperience", "業界経験", 4000, false));
                    updatedProfile.setManagementExperience(input.text("managementExperience", "経営経験", 4000, false));
                }
            }
        }

        userDao.updateProfile(actorId, userId, displayName, email, updatedProfile);
    }

    /**
     * 選択された活動履歴タブに対応するDAOメソッドを呼び出して一覧を取得する。
     * @param tab タブ識別子。
     * @param userId 会員ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 活動履歴一覧。
     * @throws Exception DB照会に失敗した場合。
     */
    private List<AdminUserItem.History> loadHistory(String tab, int userId, int limit, int offset) throws Exception {
        return switch (tab) {
            case "votes" -> userDao.findVoteHistory(userId, limit, offset);
            case "answers" -> userDao.findAnswerHistory(userId, limit, offset);
            case "held" -> userDao.findResidentCouponHistory(userId, false, limit, offset);
            case "used" -> userDao.findResidentCouponHistory(userId, true, limit, offset);
            case "applications" -> userDao.findApplicationHistory(userId, limit, offset);
            case "tenancies" -> userDao.findTenancyHistory(userId, limit, offset);
            case "plans" -> userDao.findOpeningPlanHistory(userId, limit, offset);
            case "offers" -> userDao.findOfferHistory(userId, limit, offset);
            case "surveys" -> userDao.findSurveyHistory(userId, limit, offset);
            case "coupons" -> userDao.findCreatedCouponHistory(userId, limit, offset);
            case "properties" -> userDao.findOwnedPropertyHistory(userId, limit, offset);
            case "reports" -> userDao.findReportHistory(userId, limit, offset);
            case "inquiries" -> userDao.findInquiryHistory(userId, limit, offset);
            default -> throw new ManagementProblem(404, "指定の会員情報は表示できません。");
        };
    }

    /**
     * 利用区分ごとに固有の詳細タブ一覧を返す。
     * @param role 利用区分コード。
     * @return タブ識別子と表示名のマップ。
     */
    private static Map<String, String> tabsForRole(String role) {
        if ("RESIDENT".equals(role)) {
            return RESIDENT_TABS;
        }
        if ("BUSINESS".equals(role)) {
            return BUSINESS_TABS;
        }
        return OWNER_TABS;
    }

    /**
     * 指定タブがその会員の利用区分で閲覧可能か判定する。
     * @param roleTabs 利用区分固有のタブ一覧。
     * @param tab 確認対象のタブ識別子。
     * @return 閲覧可能ならtrue。
     */
    private static boolean isAllowedTab(Map<String, String> roleTabs, String tab) {
        return "profile".equals(tab)
                || "preferences".equals(tab)
                || "inquiries".equals(tab)
                || roleTabs.containsKey(tab);
    }
}
