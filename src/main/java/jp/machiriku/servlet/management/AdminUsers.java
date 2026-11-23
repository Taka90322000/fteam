package jp.machiriku.servlet.management;

import static jp.machiriku.servlet.management.ManagementDisplay.options;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.machiriku.bean.management.AdminUserView;
import jp.machiriku.registration.RegistrationForms;
import jp.machiriku.tool.PhoneNumbers;

import jp.machiriku.dao.management.AdminUserDao;
import jp.machiriku.dao.management.ManagementDao;

/** 利用区分別の会員検索、関連データの確認、プロフィールと利用状態の管理を行う。 */
final class AdminUsers {
    /** 公開登録に対応する区分の画面表示。 */
    private static final Map<String, String> ROLES = options("RESIDENT", "地域住民", "BUSINESS", "事業主", "OWNER", "物件オーナー");
    /** DBの会員状態と利用者向けの表示。 */
    private static final Map<String, String> STATES = options("PENDING", "本登録前", "ACTIVE", "利用中", "BAN", "利用停止中", "WITHDRAWN", "退会済み");

    /** 画面ごとの処理を静的に利用する。 */
    private AdminUsers() { }

    /**
     * 利用区分別の会員一覧または選択された会員の詳細を取得する。
     * @param db 管理対象の情報を参照するDAO接続。
     * @param input 会員ID、区分、閲覧するタブなどの入力。
     * @param actorId 操作する管理者の会員ID。
     * @return 一覧または詳細のJSPと表示データ。
     * @throws SQLException 会員情報を取得できなかった場合。
     */
    static ManagementView page(ManagementDao db, ManagementRequest input, int actorId) throws SQLException {
        // 対象会員が指定されていないときは一覧を開く。
        Integer id = input.id("id", false);
        if (id == null) {
            return list(db, input);
        }

        // 利用区分と状態を参照し、この会員で参照できるタブか確認する。
        var member = AdminUserDao.member(db, id);
        String role = member.get("区分").toString();
        String status = member.get("利用状態").toString();
        String tab = input.value("tab").isEmpty() ? "profile" : input.value("tab");
        requireTab(role, tab);

        // 共通情報に加えて、利用者が選んだタブの情報だけを取得する。
        AdminUserView.Tab detail = switch (tab) {
            case "profile" -> profile(db, id, role, status, actorId, member.get("退会日時") != null);
            case "preferences" -> preferences(db, id, status);
            case "locations" -> locations(db, input, id, status);
            case "qualifications" -> qualifications(db, input, id, status);
            default -> new AdminUserView.History(input.pageRows(
                    AdminUserDao.history(db, tab, id, ManagementRequest.PAGE_SIZE + 1, input.offset())));
        };
        member.put("区分", ROLES.get(role));
        member.put("利用状態", STATES.get(status));
        var data = new AdminUserView.Detail(id, role, tab, member, detail);
        return new ManagementView("admin/users/detail.jsp", ROLES.get(role) + "　" + member.get("名前"), data);
    }

    /**
     * 選択された区分の会員を入力された条件で検索する。
     * @param db 会員を照会するDAO接続。
     * @param input 区分、状態、登録日範囲、検索語とページ。
     * @return 会員一覧のJSPと検索結果。
     * @throws SQLException 会員一覧を取得できなかった場合。
     */
    private static ManagementView list(ManagementDao db, ManagementRequest input) throws SQLException {
        // 検索値を検証し、DAOと表示用データの両方に同じ値を使う。
        String role = input.value("role").isEmpty() ? "RESIDENT" : input.choice("role", "利用区分", "RESIDENT", "BUSINESS", "OWNER");
        String query = input.search();
        String status = input.value("status");
        var from = input.date("from", "開始日");
        var to = input.date("to", "終了日");
        var rows = AdminUserDao.members(db, role, status, from, to, query, ManagementRequest.PAGE_SIZE + 1, input.offset());
        for (var row : rows) {
            row.computeIfPresent("利用状態", (key, value) -> STATES.get(value.toString()));
        }
        var data = new AdminUserView.Listing(role, query, status, from, to, ROLES, STATES, input.pageRows(rows));
        return new ManagementView("admin/users/list.jsp", "ユーザー管理", data);
    }

    /**
     * 対象会員の利用区分で参照できる情報かを確認する。
     * @param role 対象会員の利用区分。
     * @param tab リクエストで指定された情報の種類。
     * @throws ManagementProblem 指定の情報は対象の利用区分で閲覧できない場合。
     */
    private static void requireTab(String role, String tab) {
        // 全区分に共通している情報を先に受け付ける。
        if (Set.of("profile", "preferences", "inquiries").contains(tab)) {
            return;
        }
        Set<String> available = switch (role) {
            case "RESIDENT" -> Set.of("locations", "votes", "answers", "held", "used");
            case "BUSINESS" -> Set.of("qualifications", "applications", "tenancies", "plans", "offers", "surveys", "coupons", "reports");
            case "OWNER" -> Set.of("properties", "offers", "reports");
            default -> Set.of();
        };
        if (!available.contains(tab)) {
            throw new ManagementProblem(404, "指定の会員情報は表示できません。");
        }
    }

    /**
     * プロフィールの登録内容と、管理者が現在行える操作を取得する。
     * @param db プロフィールと監査記録を照会するDAO接続。
     * @param id 対象の会員ID。
     * @param role 対象会員の利用区分。
     * @param status 対象会員の利用状態。
     * @param actorId 操作する管理者の会員ID。
     * @param withdrawn 会員情報に退会日時がある場合はtrue。
     * @return プロフィールタブのデータ。
     * @throws SQLException プロフィールを取得できなかった場合。
     */
    private static AdminUserView.Profile profile(ManagementDao db, int id, String role, String status,
            int actorId, boolean withdrawn) throws SQLException {
        // 区分別プロフィールが未登録の場合も、会員の基本連絡先を表示できるようにする。
        var profileRows = AdminUserDao.profile(db, role, id);
        Map<String, Object> profile = profileRows.isEmpty() ? Map.of() : profileRows.getFirst();
        boolean editable = !withdrawn && !"WITHDRAWN".equals(status);
        boolean canChangeStatus = editable && id != actorId && AdminUserDao.activeAdminGrant(db, id).isEmpty();
        return new AdminUserView.Profile(profile, editable, canChangeStatus, "BAN".equals(status),
                editable ? AdminUserDao.recentAccountActions(db, id) : List.of());
    }

    /**
     * 登録済みの希望業種と編集に必要な候補を取得する。
     * @param db 業種と選択済みの情報を照会するDAO接続。
     * @param id 対象の会員ID。
     * @param status 対象会員の利用状態。
     * @return 希望業種タブのデータ。
     * @throws SQLException 希望業種を取得できなかった場合。
     */
    private static AdminUserView.Preferences preferences(ManagementDao db, int id, String status) throws SQLException {
        // 退会会員は登録内容を確認できるため、選択用のマスターだけ取得を省く。
        boolean editable = !"WITHDRAWN".equals(status);
        var rows = AdminUserDao.preferences(db, id);
        var selected = rows.stream().map(row -> ((Number) row.get("ID")).intValue()).toList();
        return new AdminUserView.Preferences(rows, selected,
                editable ? ManagementDisplay.choices(AdminUserDao.preferenceChoices(db, id)) : Map.of(), editable);
    }

    /**
     * 住民の生活拠点を取得し、選択中の種別の住所を特定する。
     * @param db 生活拠点を照会するDAO接続。
     * @param input 選択した生活拠点の種別。
     * @param id 対象住民の会員ID。
     * @param status 対象会員の利用状態。
     * @return 生活拠点タブのデータ。
     * @throws SQLException 生活拠点を取得できなかった場合。
     */
    private static AdminUserView.Locations locations(ManagementDao db, ManagementRequest input, int id, String status)
            throws SQLException {
        // 同じ取得結果から、選択した生活拠点の初期値を読み出す。
        String kind = input.value("kind").isEmpty() ? "HOME" : input.choice("kind", "生活拠点", "HOME", "WORK", "SCHOOL");
        var rows = AdminUserDao.locations(db, id);
        var current = rows.stream().filter(row -> kind.equals(row.get("種類"))).findFirst().orElse(Map.of());
        return new AdminUserView.Locations(rows, kind, current, !"WITHDRAWN".equals(status));
    }

    /**
     * 事業主の保有資格と、選択された資格の登録内容を取得する。
     * @param db 資格とマスターを照会するDAO接続。
     * @param input 選択した資格ID。
     * @param id 対象事業主の会員ID。
     * @param status 対象会員の利用状態。
     * @return 保有資格タブのデータ。
     * @throws SQLException 保有資格を取得できなかった場合。
     */
    private static AdminUserView.Qualifications qualifications(ManagementDao db, ManagementRequest input, int id, String status)
            throws SQLException {
        // プロフィールの登録が終わっている事業主に、資格の選択候補を渡す。
        var rows = AdminUserDao.qualifications(db, id);
        boolean editable = !"WITHDRAWN".equals(status) && !AdminUserDao.businessProfile(db, id).isEmpty();
        if (!editable) {
            return new AdminUserView.Qualifications(rows, null, Map.of(), Map.of(), false);
        }
        Integer selected = input.id("industry", false);
        var current = rows.stream().filter(row -> java.util.Objects.equals(row.get("ID"), selected)).findFirst().orElse(Map.of());
        return new AdminUserView.Qualifications(rows, selected, current,
                ManagementDisplay.choices(AdminUserDao.qualificationChoices(db, id)), true);
    }

    /**
     * 会員種別と状態を再確認したうえで、画面の変更を保存する。
     * @param db トランザクション内の接続。
     * @param input 変更内容。
     * @param actorId 管理者の会員ID。
     * @return 更新後の会員詳細URL。
     * @throws SQLException 保存に失敗した場合。
     */
    static String change(ManagementDao db, ManagementRequest input, int actorId) throws SQLException {
        int id = input.id("id", true);
        String operation = input.choice("operation", "操作", "suspend", "restore", "profile", "preferences", "location", "qualification");
        if (Set.of("suspend", "restore").contains(operation)) {
            accountStatus(db, id, actorId, "suspend".equals(operation), input.text("reason", "操作理由", 2000, true));
            return ManagementPaths.ADMIN_USERS + "?id=" + id;
        }
        // 会員行をロックし、本人のプロフィール更新や退会操作との競合を抑える。
        var member = AdminUserDao.lockAccount(db, id);
        if (member.get("withdrawn_at") != null || "WITHDRAWN".equals(member.get("account_status"))) {
            throw new ManagementProblem(409, "退会済み会員のプロフィールは変更できません。");
        }
        String role = member.get("user_role").toString();
        String tab = switch (operation) {
            case "profile" -> { saveProfile(db, input, id, role, actorId); yield "profile"; }
            case "preferences" -> { savePreferences(db, input, id, actorId); yield "preferences"; }
            case "location" -> { if (!"RESIDENT".equals(role)) { throw new ManagementProblem(403, "この会員の生活拠点は変更できません。"); }
                saveLocation(db, input, id, actorId); yield "locations"; }
            case "qualification" -> { if (!"BUSINESS".equals(role)) { throw new ManagementProblem(403, "この会員の保有資格は変更できません。"); }
                saveQualification(db, input, id, actorId); yield "qualifications"; }
            default -> throw new ManagementProblem(400, "操作を選び直してください。");
        };
        return ManagementPaths.ADMIN_USERS + "?id=" + id + "&tab=" + tab;
    }

    /**
     * 会員の利用停止または停止解除を記録する。
     * @param db トランザクション内の接続。
     * @param id 対象会員ID。
     * @param actorId 操作した管理者の会員ID。
     * @param suspend 停止にする場合にtrue。
     * @param reason 操作理由。
     * @throws SQLException 更新に失敗した場合。
     */
    static void accountStatus(ManagementDao db, int id, int actorId, boolean suspend, String reason) throws SQLException {
        // 管理権限のある会員を停止するには先にDB側で管理権限を解除する。
        if (id == actorId || !AdminUserDao.activeAdminGrant(db, id).isEmpty()) {
            throw new ManagementProblem(409, "管理権限が有効な会員は、権限を解除してから利用停止にしてください。");
        }
        var member = AdminUserDao.lockAccount(db, id);
        String previous = member.get("account_status").toString();
        if (member.get("withdrawn_at") != null || "WITHDRAWN".equals(previous)) { throw new ManagementProblem(409, "退会済み会員の状態は変更できません。"); }
        String state = suspend ? "BAN" : statusBeforeBan(db, id, member.get("user_role").toString());
        if (suspend && "BAN".equals(previous)) { return; }
        if (!suspend && !"BAN".equals(previous)) { throw new ManagementProblem(409, "この会員は利用停止中ではありません。"); }
        AdminUserDao.updateStatus(db, state, id);
        db.audit(actorId, suspend ? "SUSPEND" : "RESTORE", "USER", Map.of("user_id", id), reason, Map.of("before", previous, "after", state));
    }

    /**
     * 本登録前の会員を停止した場合も、解除時に以前の状態へ戻せるようにする。
     * @param db DB接続。
     * @param id 停止中の会員ID。
     * @param role 対象の利用区分。
     * @return 解除後の状態。
     * @throws SQLException 操作履歴やプロフィールの参照に失敗した場合。
     */
    private static String statusBeforeBan(ManagementDao db, int id, String role) throws SQLException {
        var logs = AdminUserDao.latestSuspension(db, id);
        if (!logs.isEmpty() && Set.of("PENDING", "ACTIVE").contains(String.valueOf(logs.getFirst().get("state")))) {
            return logs.getFirst().get("state").toString();
        }
        // 既存の停止会員に履歴がない場合は、プロフィールの登録状況から復帰先を決める。
        return AdminUserDao.profileKeys(db, role, id).isEmpty() ? "PENDING" : "ACTIVE";
    }

    /**
     * プロフィールの入力を保存する。認証IDや利用状態、範囲ポリゴンは既存値を使う。
     * @param db トランザクション内の接続。
     * @param input 編集フォーム。
     * @param id 対象会員ID。
     * @param role 対象会員の利用区分。
     * @param actorId 操作した管理者の会員ID。
     * @throws SQLException 保存に失敗した場合。
     */
    private static void saveProfile(ManagementDao db, ManagementRequest input, int id, String role, int actorId) throws SQLException {
        String name = input.text("name", "表示名", 100, true);
        String email = RegistrationForms.email(input.value("email"));
        AdminUserDao.updateContact(db, name, email, id);
        if (!AdminUserDao.profileKeys(db, role, id).isEmpty()) {
            if ("RESIDENT".equals(role)) {
                String age = input.choice("age", "年齢区分", "", "10代", "20代", "30代", "40代", "50代", "60代以上");
                String gender = input.choice("gender", "性別", "", "男性", "女性", "その他");
                AdminUserDao.updateResident(db, age, gender, id);
            } else {
                String phone = RegistrationForms.normalizePhone(input.value("phone"));
                String secondary = PhoneNumbers.toE164(input.value("secondaryPhone"), "追加の電話番号");
                if (phone.equals(secondary)) { throw new ManagementProblem(400, "追加の電話番号には主番号と異なる番号を入力してください。"); }
                String representative = input.text("representative", "代表者", 100, true);
                if ("OWNER".equals(role)) {
                    AdminUserDao.updateOwner(db, representative, input.text("company", "法人名", 150, false), phone, secondary, id);
                } else {
                    AdminUserDao.updateBusiness(db, input.text("businessName", "事業者名", 150, true), representative, phone, secondary, input.text("areaName", "希望エリア", 150, true), input.number("employees", "従業員数", 0, Integer.MAX_VALUE), input.text("industryExperience", "業界経験", 4000, false), input.text("managementExperience", "経営経験", 4000, false), id);
                }
            }
        }
        db.audit(actorId, "UPDATE_PROFILE", "USER", Map.of("user_id", id), null, Map.of("role", role));
    }

    /**
     * 追加・解除した希望業種だけを書き換え、選択を継続した業種の登録日時を保つ。
     * @param db トランザクション内の接続。
     * @param input 選択した希望業種。
     * @param id 対象会員ID。
     * @param actorId 操作した管理者の会員ID。
     * @throws SQLException 保存に失敗した場合。
     */
    private static void savePreferences(ManagementDao db, ManagementRequest input, int id, int actorId) throws SQLException {
        var before = AdminUserDao.preferenceKeys(db, id)
                .stream().map(row -> (Integer) row.get("industry_type_id")).toList();
        var selected = input.ids("industries");
        for (Integer key : selected) {
            if (before.contains(key)) { continue; }
            if (AdminUserDao.lockActiveIndustry(db, key).isEmpty()) { throw new ManagementProblem(400, "有効な業種を選択してください。"); }
            AdminUserDao.addPreference(db, id, key);
        }
        for (Integer key : before) {
            if (!selected.contains(key)) { AdminUserDao.deletePreference(db, id, key); }
        }
        db.audit(actorId, "UPDATE_PREFERENCES", "USER", Map.of("user_id", id), null, Map.of("before", before, "after", selected));
    }

    /**
     * 生活拠点のIDを維持したまま住所を登録・修正する。
     * @param db トランザクション内の接続。
     * @param input 住所と座標の入力。
     * @param id 住民の会員ID。
     * @param actorId 操作した管理者の会員ID。
     * @throws SQLException 保存に失敗した場合。
     */
    private static void saveLocation(ManagementDao db, ManagementRequest input, int id, int actorId) throws SQLException {
        String kind = input.choice("kind", "生活拠点", "HOME", "WORK", "SCHOOL");
        var lat = input.decimal("lat", "緯度", -90, 90, 6, false);
        var lng = input.decimal("lng", "経度", -180, 180, 6, false);
        if ((lat == null) != (lng == null)) { throw new ManagementProblem(400, "緯度と経度を一緒に入力してください。"); }
        AdminUserDao.upsertLocation(db, id, kind, input.text("prefecture", "都道府県", 10, true), input.text("city", "市区町村", 50, true), input.text("street", "番地・建物名", 200, false), lat, lng);
        db.audit(actorId, "UPDATE_LOCATION", "USER", Map.of("user_id", id), null, Map.of("location_kind", kind));
    }

    /**
     * 事業主の保有資格の情報を保存する。
     * @param db トランザクション内の接続。
     * @param input 資格と登録情報。
     * @param id 事業主の会員ID。
     * @param actorId 操作した管理者の会員ID。
     * @throws SQLException 保存に失敗した場合。
     */
    private static void saveQualification(ManagementDao db, ManagementRequest input, int id, int actorId) throws SQLException {
        int qualification = input.id("qualificationId", true);
        AdminUserDao.requireBusinessProfile(db, id);
        AdminUserDao.requireAssignableQualification(db, qualification, id);
        var obtained = input.date("obtained", "取得日");
        var expires = input.date("expires", "有効期限");
        if (obtained != null && expires != null && expires.isBefore(obtained)) { throw new ManagementProblem(400, "有効期限は取得日以降を指定してください。"); }
        boolean active = Boolean.parseBoolean(input.choice("active", "有効状態", "true", "false"));
        AdminUserDao.upsertQualification(db, id, qualification, input.text("credential", "資格番号", 100, false), obtained, expires, active);
        db.audit(actorId, "UPDATE_QUALIFICATION", "USER", Map.of("user_id", id), null, Map.of("qualification_id", qualification, "is_active", active));
    }
}
