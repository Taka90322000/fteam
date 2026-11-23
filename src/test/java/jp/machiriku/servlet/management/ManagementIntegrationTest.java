package jp.machiriku.servlet.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import jakarta.servlet.http.HttpServletRequest;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminCatalogView;
import jp.machiriku.bean.management.AdminCouponView;
import jp.machiriku.bean.management.AdminListingView;
import jp.machiriku.bean.management.AdminOverviewView;
import jp.machiriku.bean.management.AdminSurveyView;
import jp.machiriku.bean.management.AdminUserView;
import jp.machiriku.bean.management.HelpDeskView;
import jp.machiriku.bean.management.PublicDirectoryView;
import jp.machiriku.dao.Dao;
import jp.machiriku.dao.management.ManagementDao;

/** 接続先の構造から作った一時テーブルだけを使い、管理操作と利用者への公開範囲を検証する。 */
public class ManagementIntegrationTest {
    /** 実テーブルを残したまま隔離する、管理機能が参照する既存テーブル。 */
    private static final List<String> BASE_TABLES = List.of("users", "resident_profiles", "business_profiles", "owner_profiles",
            "user_locations", "user_industry_preferences", "business_qualifications", "industry_categories", "industry_types", "qualifications",
            "industry_qualification_requirements", "properties", "property_contract_terms", "property_industries", "property_images", "property_votes",
            "vote_choices", "applications", "application_checks", "tenancies", "opening_plans", "offers", "surveys", "survey_questions",
            "survey_options", "survey_responses", "survey_answers", "coupons", "coupon_recipients");
    /** テストデータを表す、本登録済みの管理者ID。 */
    private static final int ADMIN = 101;
    /** 通報・問い合わせを送る地域住民のID。 */
    private static final int RESIDENT = 102;
    /** 問い合わせを読み取る権限のない別の地域住民のID。 */
    private static final int OTHER = 103;
    /** テスト物件のオーナーのID。 */
    private static final int OWNER = 104;
    /** テスト物件で開店した事業主のID。 */
    private static final int BUSINESS = 105;
    /** 本登録前の地域住民のID。 */
    private static final int PENDING = 106;
    /** テスト物件のID。 */
    private static final String PROPERTY_ID = "201";
    /** 受付中の来店アンケートのID。 */
    private static final String SURVEY_ID = "501";
    /** 配布中クーポンのID。 */
    private static final String COUPON_ID = "601";
    /** 一時テーブルが存在する間だけ開いておく接続。 */
    private Connection connection;
    /** Actionが画面や更新の処理に使うDB操作。 */
    private ManagementDao db;

    /**
     * 実データを複製せず、テスト用データと移行構造だけを同じ接続に準備する。
     * @throws Exception 接続先の構造を検証できない場合。
     */
    @Before public void prepare() throws Exception {
        Assume.assumeTrue("true".equalsIgnoreCase(System.getenv("MACHIRIKU_RUN_DB_TESTS")));
        connection = Dao.getConnection();
        db = new ManagementDao(connection);
        try (var statement = connection.createStatement()) {
            // 指定した全テーブルを空の一時テーブルで隠し、接続先の行に触れないことを確認する。
            for (String table : BASE_TABLES) {
                statement.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL)");
                assertTrue(table, Boolean.TRUE.equals(scalar("SELECT to_regclass(?)=to_regclass(?)", table, "pg_temp." + table)));
            }
            // 本体の移行前の構成を再現し、既に移行済みの環境からも同じテストを行う。
            statement.execute("""
                    ALTER TABLE pg_temp.industry_categories DROP COLUMN IF EXISTS display_order, ALTER COLUMN category_id DROP IDENTITY IF EXISTS;
                    ALTER TABLE pg_temp.industry_types DROP COLUMN IF EXISTS display_order, ALTER COLUMN industry_type_id DROP IDENTITY IF EXISTS;
                    ALTER TABLE pg_temp.application_checks DROP COLUMN IF EXISTS is_required_snapshot, DROP COLUMN IF EXISTS condition_text_snapshot;
                    ALTER TABLE pg_temp.properties DROP COLUMN IF EXISTS admin_hidden_at;
                    ALTER TABLE pg_temp.surveys DROP COLUMN IF EXISTS previous_survey_id;
                    ALTER TABLE pg_temp.coupon_recipients DROP COLUMN IF EXISTS revoked_at;
                    INSERT INTO pg_temp.industry_categories(category_id,category_code,category_name) VALUES (10,'TST','テストカテゴリ');
                    INSERT INTO pg_temp.industry_types(industry_type_id,category_id,industry_code,industry_name) VALUES (11,10,'I9991','テスト業種');
                    """);
            // DDLの対象を一時テーブルに限定し、同じロールへの権限付与は実行対象から外す。
            for (String file : List.of("migrate-admin-management.sql", "enable-admin-management.sql")) {
                String migration = Files.readString(Path.of("database", file)).replace("public.", "pg_temp.")
                        .replace("CREATE TABLE pg_temp.", "CREATE TEMP TABLE pg_temp.")
                        .replaceAll("(?m)^GRANT [\\s\\S]*?TO machiriku_app;\\R", "");
                statement.execute(migration);
            }
            // 管理・受付機能の追加テーブルも、一時スキーマに解決されるときだけ使用する。
            for (String table : List.of("admin_members", "admin_audit_logs", "reports", "inquiries", "inquiry_messages")) {
                assertTrue(table, Boolean.TRUE.equals(scalar("SELECT to_regclass(?)=to_regclass(?)", table, "pg_temp." + table)));
            }
            statement.execute("""
                    INSERT INTO users(user_id,display_name,user_role,line_subject,account_status) OVERRIDING SYSTEM VALUE VALUES
                    (101,'管理者','RESIDENT','management-admin','ACTIVE'),(102,'住民','RESIDENT','management-resident','ACTIVE'),
                    (103,'別の住民','RESIDENT','management-other','ACTIVE'),(104,'オーナー','OWNER','management-owner','ACTIVE'),
                    (105,'事業主','BUSINESS','management-business','ACTIVE'),(106,'初期設定中','RESIDENT','management-pending','PENDING');
                    INSERT INTO admin_members(user_id) VALUES (101);
                    INSERT INTO resident_profiles(user_id,age_group,gender) VALUES (102,'20代','その他');
                    INSERT INTO owner_profiles(user_id,representative_name,company_name,phone_e164) VALUES (104,'代表者','テスト法人','+81286300000');
                    INSERT INTO business_profiles(user_id,business_name,representative_name,employee_count,preferred_area,area,phone_e164)
                    VALUES (105,'テスト事業','代表者',1,'宇都宮',ST_GeomFromText('POLYGON((139.88 36.56,139.89 36.56,139.89 36.57,139.88 36.56))',4326),'+81286300001');
                    INSERT INTO user_locations(location_id,user_id,location_kind,prefecture,municipality) OVERRIDING SYSTEM VALUE
                    VALUES (401,102,'HOME','栃木県','宇都宮市');
                    INSERT INTO user_industry_preferences(user_id,industry_type_id) VALUES (102,11),(104,11),(105,11);
                    INSERT INTO properties(property_id,owner_user_id,property_name,prefecture,municipality,address_line,floor_area_sqm,monthly_rent_yen,listing_status)
                    OVERRIDING SYSTEM VALUE VALUES (201,104,'テスト物件','栃木県','宇都宮市','テスト町',50,80000,'OPEN');
                    INSERT INTO applications(application_id,property_id,business_user_id,industry_type_id,contract_kind,requested_start_on,requested_end_on,application_status)
                    OVERRIDING SYSTEM VALUE VALUES (301,201,105,11,'LONG',CURRENT_DATE,CURRENT_DATE+365,'CONTRACTED');
                    INSERT INTO tenancies(tenancy_id,application_id,store_name,contract_start_on,contract_end_on,tenancy_status,confirmed_at,opened_on)
                    OVERRIDING SYSTEM VALUE VALUES (302,301,'テスト店舗',CURRENT_DATE,CURRENT_DATE+365,'OPEN',CURRENT_TIMESTAMP,CURRENT_DATE);
                    INSERT INTO opening_plans(plan_id,tenancy_id,title,progress_status,publication_status,published_at) OVERRIDING SYSTEM VALUE
                    VALUES (303,302,'テスト出店計画','PREPARING','PUBLISHED',CURRENT_TIMESTAMP);
                    INSERT INTO offers(offer_id,property_id,owner_user_id,business_user_id,creation_kind,message,offer_status) OVERRIDING SYSTEM VALUE
                    VALUES (304,201,104,105,'MANUAL','テストオファー','SENT');
                    INSERT INTO property_votes(vote_id,property_id,resident_user_id,location_id,distance_km,distance_scale_km) OVERRIDING SYSTEM VALUE
                    VALUES (402,201,102,401,1.2,1.0);
                    INSERT INTO vote_choices(vote_id,preference_rank,industry_type_id,rank_weight) VALUES (402,1,11,1.00);
                    INSERT INTO surveys(survey_id,tenancy_id,survey_title,survey_kind,survey_status,qr_token_hash,starts_at) OVERRIDING SYSTEM VALUE
                    VALUES (501,302,'テスト来店アンケート','POST_VISIT','OPEN',repeat('a',64),CURRENT_TIMESTAMP);
                    INSERT INTO survey_questions(question_id,survey_id,question_no,question_text,answer_kind) OVERRIDING SYSTEM VALUE
                    VALUES (502,501,2,'テスト設問','SINGLE');
                    INSERT INTO survey_options(option_id,question_id,option_text,display_order) OVERRIDING SYSTEM VALUE
                    VALUES (503,502,'テスト回答',1);
                    INSERT INTO survey_responses(response_id,survey_id,resident_user_id,rating) OVERRIDING SYSTEM VALUE VALUES (504,501,102,5);
                    INSERT INTO survey_answers(response_id,question_id,survey_id,option_id) VALUES (504,502,501,503);
                    INSERT INTO coupons(coupon_id,tenancy_id,coupon_title,benefit_kind,discount_yen,target_kind,
                                        distribution_starts_at,distribution_ends_at,valid_until,usage_limit,coupon_status) OVERRIDING SYSTEM VALUE
                    VALUES (601,302,'テストクーポン','AMOUNT',500,'ALL_VOTERS',CURRENT_TIMESTAMP-INTERVAL '1 day',
                            CURRENT_TIMESTAMP+INTERVAL '3 days',CURRENT_TIMESTAMP+INTERVAL '5 days',20,'ACTIVE'),
                           (602,302,'テスト下書きクーポン','AMOUNT',500,'ALL_VOTERS',CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP+INTERVAL '3 days',CURRENT_TIMESTAMP+INTERVAL '5 days',20,'DRAFT');
                    INSERT INTO coupon_recipients(coupon_id,resident_user_id,source_vote_id,redemption_code_hash)
                    VALUES (601,102,402,repeat('b',64));
                    """);
        }
    }

    /**
     * 接続と検証用データを破棄する。
     * @throws SQLException 接続の解放に失敗した場合。
     */
    @After public void close() throws SQLException {
        if (connection != null) { connection.close(); }
    }

    /**
     * 区分ごとのタブ・参照先が現行スキーマから読めることを確認する。
     * @throws SQLException 画面のSQLが実行できなかった場合。
     */
    @Test public void listsAndUserHistoriesUseDatabaseRows() throws SQLException {
        assertTrue(db.isAdmin(ADMIN, false));
        assertFalse(db.isAdmin(RESIDENT, false));
        // 各利用区分に対応する一覧データを、一覧用JSPに渡すことを確認する。
        for (String role : List.of("RESIDENT", "BUSINESS", "OWNER")) {
            // JSPとモデルが組になった描画先。
            var view = AdminUsers.page(db, input("role", role), ADMIN);
            assertEquals("admin/users/list.jsp", view.jsp());
            // 区分を絞り込んだ結果。
            var listing = (AdminUserView.Listing) view.model();
            assertEquals(role, listing.role());
            assertFalse(listing.members().rows().isEmpty());
        }
        // 各区分で利用できる全タブのデータを読み込む。
        for (var profile : Map.of(RESIDENT, List.of("profile", "locations", "preferences", "votes", "answers", "held", "used", "inquiries"),
                OWNER, List.of("profile", "preferences", "properties", "offers", "reports"),
                BUSINESS, List.of("profile", "preferences", "qualifications", "applications", "tenancies", "plans", "offers", "surveys", "coupons", "reports")).entrySet()) {
            // 選択されたタブに、その会員の表示用データを渡すことを確認する。
            for (String tab : profile.getValue()) {
                // 詳細JSPに渡る表示データ。
                var detail = (AdminUserView.Detail) AdminUsers.page(db, input("id", profile.getKey(), "tab", tab), ADMIN).model();
                assertEquals(tab, detail.tab());
                assertEquals(profile.getKey().intValue(), detail.id());
                assertNotNull(detail.detail());
            }
        }
        assertFalse(((AdminListingView.Listing) AdminListings.properties(db, input()).model()).properties().rows().isEmpty());
        // 物件詳細で、タブに応じた取得結果を渡していることを確認する。
        for (String tab : List.of("info", "applications", "offers", "reports")) {
            // 表示中の物件と選択中のタブ。
            var detail = (AdminListingView.Detail) AdminListings.properties(db, input("id", PROPERTY_ID, "tab", tab)).model();
            assertEquals(tab, detail.tab());
            assertNotNull("info".equals(tab) ? detail.info() : detail.history());
        }
        // 一覧から開いた関連レコードの実データを取得する。
        for (var link : Map.of("applications", 301, "tenancies", 302, "plans", 303, "offers", 304).entrySet()) {
            // 関連する画面の表示データ。
            var detail = (AdminListingView.Related) AdminListings.related(db, input("id", link.getValue()), link.getKey()).model();
            assertEquals(link.getValue().intValue(), detail.id());
            assertFalse(detail.record().isEmpty());
        }
        // ダッシュボードの集計元ごとに値を取得する。
        var dashboard = (AdminOverviewView.Dashboard) AdminOverview.dashboard(db).model();
        assertFalse(dashboard.support().isEmpty());
        assertFalse(dashboard.operations().isEmpty());
        AdminOverview.audit(db, input());
    }

    /**
     * 管理権限、受付の所有権、内部メモ、対象の措置と監査の一括保存を検証する。
     * @throws SQLException DB操作が失敗した場合。
     */
    @Test public void moderatesReportsAndKeepsInquiryNotesPrivate() throws SQLException {
        // 本登録前の会員も問い合わせできるが、通報の送信に必要な本登録は別に確認する。
        assertTrue(db.transaction(() -> db.lockEligibleMember(PENDING, false)));
        assertFalse(db.transaction(() -> db.lockEligibleMember(PENDING, true)));
        var resident = new User();
        resident.setUserId(RESIDENT);
        resident.setAccountStatus("ACTIVE");
        assertFalse(((PublicDirectoryView.Property) PublicDirectory.properties(db, input("id", PROPERTY_ID), resident).model()).property().isEmpty());
        assertFalse(((PublicDirectoryView.Listing) PublicDirectory.businesses(db, input(), resident).model()).entries().rows().isEmpty());
        assertFalse(((HelpDeskView.ReportForm) HelpDesk.reportForm(db, input("kind", "OWNER", "id", OWNER, "propertyId", PROPERTY_ID),
                resident).model()).target().isEmpty());
        assertFalse(((HelpDeskView.ReportForm) HelpDesk.reportForm(db, input("kind", "BUSINESS", "id", BUSINESS), resident).model()).target().isEmpty());
        String receipt = id(db.transaction(() -> HelpDesk.submitReport(db, input("operation", "send", "kind", "PROPERTY", "id", PROPERTY_ID,
                "reasonCode", "INACCURATE", "body", "物件情報の確認をお願いします"), RESIDENT)));
        assertFalse(((HelpDeskView.Receipt) HelpDesk.reportForm(db, input("receipt", receipt), resident).model()).receipt().isEmpty());
        assertFalse(((HelpDeskView.ReportList) HelpDesk.reports(db, input(), ADMIN).model()).reports().rows().isEmpty());
        assertFalse(((HelpDeskView.ReportDetail) HelpDesk.reports(db, input("id", receipt), ADMIN).model()).report().isEmpty());
        db.transaction(() -> HelpDesk.changeReport(db, input("id", receipt, "operation", "review", "assignee", ADMIN), ADMIN));
        // 監査の保存に失敗した場合は、対象の掲載停止も通報のクローズも取り消す。
        var resolution = input("id", receipt, "operation", "resolve", "resolution", "HIDE_PROPERTY", "reason", "掲載内容を確認");
        assertThrows(SQLException.class, () -> db.transaction(() -> HelpDesk.changeReport(db, resolution, RESIDENT)));
        assertFalse(((PublicDirectoryView.Listing) PublicDirectory.properties(db, input(), resident).model()).entries().rows().isEmpty());
        db.transaction(() -> HelpDesk.changeReport(db, resolution, ADMIN));
        assertTrue(((PublicDirectoryView.Listing) PublicDirectory.properties(db, input(), resident).model()).entries().rows().isEmpty());

        String inquiry = id(db.transaction(() -> HelpDesk.changeInquiry(db,
                input("operation", "open", "category", "ACCOUNT", "subject", "問い合わせのテスト", "body", "質問本文"), PENDING, false)));
        assertFalse(((HelpDeskView.AdminInquiryList) HelpDesk.inquiries(db, input(), ADMIN, true).model()).inquiries().rows().isEmpty());
        var selected = input("id", inquiry);
        assertEquals(ManagementDao.NO_DATA, assertThrows(SQLException.class, () -> HelpDesk.inquiries(db, selected, OTHER, false)).getSQLState());
        assertEquals(ManagementDao.NO_DATA, assertThrows(SQLException.class, () -> db.transaction(() -> HelpDesk.changeInquiry(db,
                input("id", inquiry, "operation", "reply", "body", "別の会員の返信"), OTHER, false))).getSQLState());
        db.transaction(() -> HelpDesk.changeInquiry(db, input("id", inquiry, "operation", "note", "body", "公開しないメモ"), ADMIN, true));
        db.transaction(() -> HelpDesk.changeInquiry(db, input("id", inquiry, "operation", "reply", "status", "WAITING_USER", "body", "運営からの返信"), ADMIN, true));
        assertEquals(List.of("運営からの返信", "質問本文"), messages(HelpDesk.inquiries(db, selected, PENDING, false)));
        assertEquals(List.of("運営からの返信", "公開しないメモ", "質問本文"), messages(HelpDesk.inquiries(db, selected, ADMIN, true)));
        db.transaction(() -> HelpDesk.changeInquiry(db, input("id", inquiry, "operation", "reply", "body", "利用者からの回答"), PENDING, false));
        assertFalse(((AdminOverviewView.Audit) AdminOverview.audit(db, input("q", "HIDE")).model()).entries().rows().isEmpty());
        // 会員の復帰では、停止前の本登録状態を操作履歴から復元する。
        db.transaction(() -> AdminUsers.change(db, input("id", PENDING, "operation", "suspend", "reason", "停止の確認"), ADMIN));
        db.transaction(() -> AdminUsers.change(db, input("id", PENDING, "operation", "restore", "reason", "解除の確認"), ADMIN));
        assertEquals("PENDING", scalar("SELECT account_status FROM users WHERE user_id=?", PENDING));
        // 付与を取り消した後は、同じ会員IDからも管理権限を取得できないことを確認する。
        try (var statement = connection.prepareStatement("UPDATE pg_temp.admin_members SET is_active=false WHERE user_id=?")) {
            statement.setInt(1, ADMIN);
            statement.executeUpdate();
        }
        assertFalse(db.isAdmin(ADMIN, false));
    }

    /**
     * マスターの設定、アンケートの版、クーポンの停止・無効化を実データ構造で検証する。
     * @throws SQLException 保存または表示が失敗した場合。
     */
    @Test public void editsCatalogAndKeepsSurveyVersionsAndCouponIssues() throws SQLException {
        assertFalse(((AdminCatalogView.IndustryList) AdminCatalog.industries(db, input()).model()).industries().rows().isEmpty());
        String category = id(db.transaction(() -> AdminCatalog.saveIndustry(db,
                input("operation", "save", "kind", "category", "name", "追加カテゴリ", "code", "NEW", "order", 2, "active", true), ADMIN)));
        assertEquals("11", category);
        String industry = id(db.transaction(() -> AdminCatalog.saveIndustry(db,
                input("operation", "save", "kind", "type", "name", "追加業種", "code", "I9992", "order", 1, "active", true, "categoryId", category), ADMIN)));
        assertEquals("12", industry);
        assertFalse(((AdminCatalogView.IndustryEdit) AdminCatalog.industries(db, input("kind", "type", "id", industry)).model()).current().isEmpty());
        String qualification = id(db.transaction(() -> AdminCatalog.saveQualification(db,
                input("operation", "save", "name", "必要書類の案内", "kind", "DOCUMENT", "active", true, "guidance", "持参物の案内"), ADMIN)));
        db.transaction(() -> AdminCatalog.saveQualification(db, input("operation", "requirement", "industry", industry, "qualificationId", qualification,
                "required", true, "condition", "必要な場合に持参"), ADMIN));
        assertFalse(((PublicDirectoryView.Requirements) PublicDirectory.requirements(db, input("industry", industry)).model()).requirements().rows().isEmpty());
        assertFalse(((AdminCatalogView.Requirements) AdminCatalog.qualifications(db, input("tab", "requirements", "industry", industry))
                .model()).requirements().rows().isEmpty());

        assertFalse(((AdminSurveyView.Listing) AdminSurveys.page(db, input()).model()).surveys().rows().isEmpty());
        // 回答済みアンケートの集計と投稿一覧は回答タブに渡される。
        var surveyDetail = (AdminSurveyView.Detail) AdminSurveys.page(db, input("id", SURVEY_ID, "tab", "responses")).model();
        assertFalse(((AdminSurveyView.Responses) surveyDetail.detail()).answers().rows().isEmpty());
        String revision = id(db.transaction(() -> AdminSurveys.change(db, input("operation", "revise", "id", SURVEY_ID), ADMIN)));
        // 改訂で複製した設問は新しいアンケートの設問タブで読める。
        var revisionDetail = (AdminSurveyView.Detail) AdminSurveys.page(db, input("id", revision, "tab", "questions")).model();
        assertFalse(((AdminSurveyView.Questions) revisionDetail.detail()).questions().isEmpty());
        db.transaction(() -> AdminSurveys.change(db, input("operation", "question", "id", revision, "question", 2,
                "questionText", "改訂した設問", "answerKind", "SINGLE", "required", true, "options", "選択肢1\n選択肢2"), ADMIN));
        assertEquals("テスト設問", scalar("SELECT question_text FROM survey_questions WHERE survey_id=501"));
        assertEquals("改訂した設問", scalar("SELECT question_text FROM survey_questions WHERE survey_id=?", Integer.parseInt(revision)));
        db.transaction(() -> AdminSurveys.change(db, input("operation", "close", "id", SURVEY_ID, "reason", "受付終了の確認"), ADMIN));
        assertEquals("CLOSED", scalar("SELECT survey_status FROM surveys WHERE survey_id=501"));

        assertFalse(((AdminCouponView.Listing) AdminCoupons.page(db, input()).model()).coupons().rows().isEmpty());
        // 指定した会員への配布は、一覧と無効化の可否に反映される。
        var coupon = (AdminCouponView.Detail) AdminCoupons.page(db, input("id", COUPON_ID, "tab", "recipients", "recipientId", RESIDENT)).model();
        assertFalse(coupon.history().entries().rows().isEmpty());
        assertTrue(coupon.history().canRevoke());
        db.transaction(() -> AdminCoupons.change(db, input("operation", "edit", "id", 602, "title", "更新した下書き", "benefit", "SERVICE",
                "service", "ドリンク", "starts", "2090-01-01T09:00", "ends", "2090-01-02T09:00", "validUntil", "2090-01-03T09:00", "limit", 20), ADMIN));
        db.transaction(() -> AdminCoupons.change(db, input("operation", "stop", "id", COUPON_ID, "reason", "一時停止の確認"), ADMIN));
        assertFalse(Boolean.TRUE.equals(scalar("SELECT revoked_at IS NOT NULL FROM coupon_recipients WHERE coupon_id=601")));
        db.transaction(() -> AdminCoupons.change(db, input("operation", "resume", "id", COUPON_ID, "reason", "再開の確認"), ADMIN));
        var revoke = input("operation", "revoke", "id", COUPON_ID, "recipientId", RESIDENT, "reason", "個別無効化の確認");
        db.transaction(() -> AdminCoupons.change(db, revoke, ADMIN));
        assertEquals(409, assertThrows(ManagementProblem.class, () -> db.transaction(() -> AdminCoupons.change(db, revoke, ADMIN))).status());
    }

    /**
     * 本番DAOとは独立に、一時テーブルの保存結果を検証する値を取得する。
     * @param sql 最初の行の値を確認するためのテスト用SQL。
     * @param parameters SQLへバインドする値。
     * @return 一件目の最初の列の値。
     * @throws SQLException 結果の照会に失敗した場合。
     */
    private Object scalar(String sql, Object... parameters) throws SQLException {
        // 一時テーブル専用の接続で、検証対象の値を取得する。
        try (var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getObject(1);
            }
        }
    }

    /**
     * 問い合わせの画面データから、利用者に見える投稿を取り出す。
     * @param page 問い合わせの詳細。
     * @return 最新の投稿から順に並べた本文。
     */
    private static List<Object> messages(ManagementView page) {
        // 権限に応じてActionがJSPへ渡した問い合わせのデータ。
        var detail = (HelpDeskView.InquiryDetail) page.model();
        return detail.posts().rows().stream().map(row -> row.get("内容")).toList();
    }

    /**
     * 保存後のURLから、追加した行のIDを取り出す。
     * @param path 保存処理が返したURL。idまたはreceiptの欄に新しいIDを含むこと。
     * @return 次の画面を開く際のID。
     */
    private static String id(String path) { return path.replaceFirst("^.*[?&](?:id|receipt)=([0-9]+).*$", "$1"); }

    /**
     * フォームのJava側の検証を通して、Actionと同じ入力を渡す。
     * @param pairs 名前と値の組。
     * @return テスト対象に渡すリクエスト。
     */
    private static ManagementRequest input(Object... pairs) {
        Map<String, String> values = new HashMap<>();
        for (int index = 0; index < pairs.length; index += 2) { values.put(pairs[index].toString(), pairs[index + 1].toString()); }
        var request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[] {HttpServletRequest.class},
                (proxy, method, arguments) -> "getParameter".equals(method.getName()) ? values.get(arguments[0]) : null);
        return new ManagementRequest(request);
    }
}
