package jp.machiriku.servlet.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import jp.machiriku.bean.LocationInput;
import jp.machiriku.bean.management.AdminApplicationDetail;
import jp.machiriku.bean.management.AdminCouponItem;
import jp.machiriku.bean.management.AdminDashboardSummary;
import jp.machiriku.bean.management.AdminSurveyItem;
import jp.machiriku.bean.management.AdminUserItem;
import jp.machiriku.bean.management.InquiryItem;
import jp.machiriku.bean.management.ManagementProblem;
import jp.machiriku.dao.Dao;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.dao.management.AdminCatalogDao;
import jp.machiriku.dao.management.AdminCouponDao;
import jp.machiriku.dao.management.AdminDealDao;
import jp.machiriku.dao.management.AdminInquiryDao;
import jp.machiriku.dao.management.AdminPropertyDao;
import jp.machiriku.dao.management.AdminReportDao;
import jp.machiriku.dao.management.AdminSurveyDao;
import jp.machiriku.dao.management.AdminUserDao;
import jp.machiriku.dao.management.MemberSupportDao;
import jp.machiriku.dao.management.PublicDirectoryDao;

/** 接続先の構造から作った一時テーブルだけを使い、管理操作と利用者への公開範囲を検証する。 */
public class ManagementIntegrationTest {
    /** 実テーブルを残したまま隔離する、管理機能が参照する既存テーブル。 */
    private static final List<String> BASE_TABLES = List.of(
            "users", "resident_profiles", "business_profiles", "owner_profiles",
            "user_locations", "user_industry_preferences", "business_qualifications", "notification_settings",
            "industry_categories", "industry_types", "qualifications",
            "industry_qualification_requirements", "properties", "property_contract_terms",
            "property_industries", "property_images", "property_votes", "vote_choices",
            "applications", "application_checks", "tenancies", "opening_plans", "offers",
            "surveys", "survey_questions", "survey_options", "survey_responses", "survey_answers",
            "coupons", "coupon_recipients");
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
    private static final int PROPERTY_ID = 201;
    /** 受付中の来店アンケートのID。 */
    private static final int SURVEY_ID = 501;
    /** 配布中クーポンのID。 */
    private static final int COUPON_ID = 601;

    /** 一時テーブルが存在する間だけ開いておく接続。 */
    private Connection connection;
    /** 管理者権限・ダッシュボード・監査ログDAO。 */
    private AdminAuditDao auditDao;
    /** 会員管理DAO。 */
    private AdminUserDao userDao;
    /** 物件管理DAO。 */
    private AdminPropertyDao propertyDao;
    /** 申込・契約・計画・オファー詳細DAO。 */
    private AdminDealDao dealDao;
    /** 業種・資格マスターDAO。 */
    private AdminCatalogDao catalogDao;
    /** アンケート管理DAO。 */
    private AdminSurveyDao surveyDao;
    /** クーポン管理DAO。 */
    private AdminCouponDao couponDao;
    /** 公開一覧DAO。 */
    private PublicDirectoryDao publicDirectoryDao;
    /** 会員向けサポートDAO。 */
    private MemberSupportDao memberSupportDao;
    /** 管理者向け問い合わせDAO。 */
    private AdminInquiryDao adminInquiryDao;
    /** 管理者向け通報DAO。 */
    private AdminReportDao adminReportDao;

    /**
     * 実データを複製せず、テスト用データと移行構造だけを同じ接続に準備する。
     * @throws Exception 接続先の構造を検証できない場合。
     */
    @Before
    public void prepare() throws Exception {
        Assume.assumeTrue("true".equalsIgnoreCase(System.getenv("MACHIRIKU_RUN_DB_TESTS")));
        connection = Dao.getConnection();
        auditDao = new AdminAuditDao(connection);
        userDao = new AdminUserDao(connection);
        propertyDao = new AdminPropertyDao(connection);
        dealDao = new AdminDealDao(connection);
        catalogDao = new AdminCatalogDao(connection);
        surveyDao = new AdminSurveyDao(connection);
        couponDao = new AdminCouponDao(connection);
        publicDirectoryDao = new PublicDirectoryDao(connection);
        memberSupportDao = new MemberSupportDao(connection);
        adminInquiryDao = new AdminInquiryDao(connection);
        adminReportDao = new AdminReportDao(connection);

        try (var statement = connection.createStatement()) {
            // 指定した全テーブルを空の一時テーブルで隠し、接続先の行に触れないことを確認する。
            for (String table : BASE_TABLES) {
                statement.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL)");
                assertTrue(table, Boolean.TRUE.equals(
                        scalar("SELECT to_regclass(?) = to_regclass(?)", table, "pg_temp." + table)));
            }
            // 本体の移行前の構成を再現し、既に移行済みの環境からも同じテストを行う。
            statement.execute("""
                    ALTER TABLE pg_temp.industry_categories DROP COLUMN IF EXISTS display_order, ALTER COLUMN category_id DROP IDENTITY IF EXISTS;
                    ALTER TABLE pg_temp.industry_types DROP COLUMN IF EXISTS display_order, ALTER COLUMN industry_type_id DROP IDENTITY IF EXISTS;
                    ALTER TABLE pg_temp.application_checks DROP COLUMN IF EXISTS is_required_snapshot, DROP COLUMN IF EXISTS condition_text_snapshot;
                    ALTER TABLE pg_temp.properties DROP COLUMN IF EXISTS admin_hidden_at;
                    ALTER TABLE pg_temp.surveys DROP COLUMN IF EXISTS previous_survey_id;
                    ALTER TABLE pg_temp.coupon_recipients DROP COLUMN IF EXISTS revoked_at;
                    INSERT INTO pg_temp.industry_categories(category_id, category_code, category_name) VALUES (10, 'TST', 'テストカテゴリ');
                    INSERT INTO pg_temp.industry_types(industry_type_id, category_id, industry_code, industry_name) VALUES (11, 10, 'I9991', 'テスト業種');
                    """);
            // DDLの対象を一時テーブルに限定し、同じロールへの権限付与は実行対象から外す。
            for (String file : List.of("migrate-admin-management.sql", "enable-admin-management.sql")) {
                String migration = Files.readString(Path.of("database", file))
                        .replace("public.", "pg_temp.")
                        .replace("CREATE TABLE pg_temp.", "CREATE TEMP TABLE pg_temp.")
                        .replaceAll("(?m)^GRANT [\\s\\S]*?TO machiriku_app;\\R", "");
                statement.execute(migration);
            }
            // 管理・受付機能の追加テーブルも、一時スキーマに解決されるときだけ使用する。
            for (String table : List.of("admin_members", "admin_audit_logs", "reports", "inquiries", "inquiry_messages")) {
                assertTrue(table, Boolean.TRUE.equals(
                        scalar("SELECT to_regclass(?) = to_regclass(?)", table, "pg_temp." + table)));
            }
            statement.execute("""
                    INSERT INTO users(user_id, display_name, user_role, line_subject, account_status) OVERRIDING SYSTEM VALUE VALUES
                    (101, '管理者', 'RESIDENT', 'management-admin', 'ACTIVE'),
                    (102, '住民', 'RESIDENT', 'management-resident', 'ACTIVE'),
                    (103, '別の住民', 'RESIDENT', 'management-other', 'ACTIVE'),
                    (104, 'オーナー', 'OWNER', 'management-owner', 'ACTIVE'),
                    (105, '事業主', 'BUSINESS', 'management-business', 'ACTIVE'),
                    (106, '初期設定中', 'RESIDENT', 'management-pending', 'PENDING');
                    INSERT INTO admin_members(user_id) VALUES (101);
                    INSERT INTO resident_profiles(user_id, age_group, gender) VALUES (102, '20代', 'その他');
                    INSERT INTO owner_profiles(user_id, representative_name, company_name, phone_e164) VALUES (104, '代表者', 'テスト法人', '+81286300000');
                    INSERT INTO business_profiles(user_id, business_name, representative_name, employee_count, preferred_area, area, phone_e164)
                    VALUES (105, 'テスト事業', '代表者', 1, '宇都宮',
                            ST_GeomFromText('POLYGON((139.88 36.56,139.89 36.56,139.89 36.57,139.88 36.56))', 4326), '+81286300001');
                    INSERT INTO user_locations(location_id, user_id, location_kind, prefecture, municipality) OVERRIDING SYSTEM VALUE
                    VALUES (401, 102, 'HOME', '栃木県', '宇都宮市');
                    INSERT INTO user_industry_preferences(user_id, industry_type_id) VALUES (102, 11), (104, 11), (105, 11);
                    INSERT INTO properties(property_id, owner_user_id, property_name, prefecture, municipality, address_line, floor_area_sqm, monthly_rent_yen, listing_status)
                    OVERRIDING SYSTEM VALUE VALUES (201, 104, 'テスト物件', '栃木県', '宇都宮市', 'テスト町', 50, 80000, 'OPEN');
                    INSERT INTO applications(application_id, property_id, business_user_id, industry_type_id, contract_kind, requested_start_on, requested_end_on, application_status)
                    OVERRIDING SYSTEM VALUE VALUES (301, 201, 105, 11, 'LONG', CURRENT_DATE, CURRENT_DATE + 365, 'CONTRACTED');
                    INSERT INTO tenancies(tenancy_id, application_id, store_name, contract_start_on, contract_end_on, tenancy_status, confirmed_at, opened_on)
                    OVERRIDING SYSTEM VALUE VALUES (302, 301, 'テスト店舗', CURRENT_DATE, CURRENT_DATE + 365, 'OPEN', CURRENT_TIMESTAMP, CURRENT_DATE);
                    INSERT INTO opening_plans(plan_id, tenancy_id, title, progress_status, publication_status, published_at) OVERRIDING SYSTEM VALUE
                    VALUES (303, 302, 'テスト出店計画', 'PREPARING', 'PUBLISHED', CURRENT_TIMESTAMP);
                    INSERT INTO offers(offer_id, property_id, owner_user_id, business_user_id, creation_kind, message, offer_status) OVERRIDING SYSTEM VALUE
                    VALUES (304, 201, 104, 105, 'MANUAL', 'テストオファー', 'SENT');
                    INSERT INTO property_votes(vote_id, property_id, resident_user_id, location_id, distance_km, distance_scale_km) OVERRIDING SYSTEM VALUE
                    VALUES (402, 201, 102, 401, 1.2, 1.0);
                    INSERT INTO vote_choices(vote_id, preference_rank, industry_type_id, rank_weight) VALUES (402, 1, 11, 1.00);
                    INSERT INTO surveys(survey_id, tenancy_id, survey_title, survey_kind, survey_status, qr_token_hash, starts_at) OVERRIDING SYSTEM VALUE
                    VALUES (501, 302, 'テスト来店アンケート', 'POST_VISIT', 'OPEN', repeat('a', 64), CURRENT_TIMESTAMP);
                    INSERT INTO survey_questions(question_id, survey_id, question_no, question_text, answer_kind) OVERRIDING SYSTEM VALUE
                    VALUES (502, 501, 2, 'テスト設問', 'SINGLE');
                    INSERT INTO survey_options(option_id, question_id, option_text, display_order) OVERRIDING SYSTEM VALUE
                    VALUES (503, 502, 'テスト回答', 1);
                    INSERT INTO survey_responses(response_id, survey_id, resident_user_id, rating) OVERRIDING SYSTEM VALUE VALUES (504, 501, 102, 5);
                    INSERT INTO survey_answers(response_id, question_id, survey_id, option_id) VALUES (504, 502, 501, 503);
                    INSERT INTO coupons(coupon_id, tenancy_id, coupon_title, benefit_kind, discount_yen, target_kind,
                                        distribution_starts_at, distribution_ends_at, valid_until, usage_limit, coupon_status) OVERRIDING SYSTEM VALUE
                    VALUES (601, 302, 'テストクーポン', 'AMOUNT', 500, 'ALL_VOTERS', CURRENT_TIMESTAMP - INTERVAL '1 day',
                            CURRENT_TIMESTAMP + INTERVAL '3 days', CURRENT_TIMESTAMP + INTERVAL '5 days', 20, 'ACTIVE'),
                           (602, 302, 'テスト下書きクーポン', 'AMOUNT', 500, 'ALL_VOTERS', CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP + INTERVAL '3 days', CURRENT_TIMESTAMP + INTERVAL '5 days', 20, 'DRAFT');
                    INSERT INTO coupon_recipients(coupon_id, resident_user_id, source_vote_id, redemption_code_hash)
                    VALUES (601, 102, 402, repeat('b', 64));
                    """);
        }
    }

    /**
     * 接続と検証用データを破棄する。
     * @throws SQLException 接続の解放に失敗した場合。
     */
    @After
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * 区分ごとの一覧・全タブ履歴・物件詳細・関連レコード・ダッシュボードが現行スキーマから読めることを確認する。
     * @throws SQLException 画面のSQLが実行できなかった場合。
     */
    @Test
    public void listsAndUserHistoriesUseDatabaseRows() throws SQLException {
        assertTrue(auditDao.isAdmin(ADMIN));
        assertFalse(auditDao.isAdmin(RESIDENT));

        // 各利用区分に対応する一覧が取得できることを確認する。
        for (String role : List.of("RESIDENT", "BUSINESS", "OWNER")) {
            assertFalse(role, userDao.findUsers(role, "", null, null, "", 26, 0).isEmpty());
        }

        // 地域住民・物件オーナー・事業主の各詳細データと活動履歴が取得できることを確認する。
        assertNotNull(userDao.findRoleProfile("RESIDENT", RESIDENT));
        assertFalse(userDao.findLocations(RESIDENT).isEmpty());
        assertFalse(userDao.findPreferences(RESIDENT).isEmpty());
        assertFalse(userDao.findPreferenceTaxonomy(RESIDENT).isEmpty());
        assertFalse(userDao.findVoteHistory(RESIDENT, 26, 0).isEmpty());
        assertFalse(userDao.findAnswerHistory(RESIDENT, 26, 0).isEmpty());
        assertFalse(userDao.findResidentCouponHistory(RESIDENT, false, 26, 0).isEmpty());
        assertNotNull(userDao.findResidentCouponHistory(RESIDENT, true, 26, 0));
        assertNotNull(userDao.findInquiryHistory(RESIDENT, 26, 0));

        // 地域住民の生活拠点（居住地＋勤務先・学校）の一括保存と勤務先・学校の解除を検証する。
        LocationInput home = new LocationInput("HOME", "栃木県", "宇都宮市", "本町1-1",
                new BigDecimal("36.561000"), new BigDecimal("139.883000"));
        LocationInput work = new LocationInput("WORK", "栃木県", "宇都宮市", "駅前通り2-2",
                new BigDecimal("36.559000"), new BigDecimal("139.898000"));
        userDao.saveLocations(ADMIN, RESIDENT, home, work);
        assertEquals(2, userDao.findLocations(RESIDENT).size());
        userDao.saveLocations(ADMIN, RESIDENT, home, null);
        assertEquals(1, userDao.findLocations(RESIDENT).size());

        // 事業主・オーナーの本登録済み会員から希望業種を0件へ削除しようとすると400エラーとなる。
        assertEquals(400, assertThrows(ManagementProblem.class,
                () -> userDao.updatePreferences(ADMIN, BUSINESS, Set.of())).getStatus());
        assertEquals(400, assertThrows(ManagementProblem.class,
                () -> userDao.updatePreferences(ADMIN, OWNER, Set.of())).getStatus());

        // 管理者がメールアドレスを削除すると、有効だったメール通知設定も自動で無効化される。
        try (var stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO notification_settings(user_id, email_enabled) VALUES (102, true) "
                    + "ON CONFLICT (user_id) DO UPDATE SET email_enabled = true");
        }
        AdminUserItem.Profile resProfile = userDao.findRoleProfile("RESIDENT", RESIDENT);
        userDao.updateProfile(ADMIN, RESIDENT, "住民", null, resProfile);
        assertEquals(Boolean.FALSE, scalar("SELECT email_enabled FROM notification_settings WHERE user_id = ?", RESIDENT));

        assertNotNull(userDao.findRoleProfile("OWNER", OWNER));
        assertFalse(userDao.findOwnedPropertyHistory(OWNER, 26, 0).isEmpty());
        assertFalse(userDao.findOfferHistory(OWNER, 26, 0).isEmpty());
        assertNotNull(userDao.findReportHistory(OWNER, 26, 0));

        assertNotNull(userDao.findRoleProfile("BUSINESS", BUSINESS));
        assertNotNull(userDao.findQualifications(BUSINESS));
        assertFalse(userDao.findApplicationHistory(BUSINESS, 26, 0).isEmpty());
        assertFalse(userDao.findTenancyHistory(BUSINESS, 26, 0).isEmpty());
        assertFalse(userDao.findOpeningPlanHistory(BUSINESS, 26, 0).isEmpty());
        assertFalse(userDao.findOfferHistory(BUSINESS, 26, 0).isEmpty());
        assertFalse(userDao.findSurveyHistory(BUSINESS, 26, 0).isEmpty());
        assertFalse(userDao.findCreatedCouponHistory(BUSINESS, 26, 0).isEmpty());

        // 物件一覧・詳細・関連履歴および申込・契約・計画・オファーの詳細が取得できることを確認する。
        assertFalse(propertyDao.findProperties("", "", "", 26, 0).isEmpty());
        assertNotNull(propertyDao.findPropertyById(PROPERTY_ID));
        assertFalse(propertyDao.findApplicationHistory(PROPERTY_ID, 26, 0).isEmpty());
        assertFalse(propertyDao.findOfferHistory(PROPERTY_ID, 26, 0).isEmpty());
        assertNotNull(propertyDao.findReportHistory(PROPERTY_ID, 26, 0));
        assertNotNull(dealDao.findApplicationById(301));
        assertNotNull(dealDao.findTenancyById(302));
        assertNotNull(dealDao.findOpeningPlanById(303));
        assertNotNull(dealDao.findOfferById(304));

        // ダッシュボード集計と監査ログ照会が動作することを確認する。
        AdminDashboardSummary summary = auditDao.findDashboardSummary();
        assertTrue(summary.getActiveUsers() > 0);
        assertTrue(summary.getVisibleProperties() > 0);
        assertNotNull(auditDao.findAuditLogs("", 26, 0));
    }

    /**
     * 管理権限、受付の所有権、内部メモの隔離、通報対応と監査ログの一括保存を検証する。
     * @throws SQLException DB操作が失敗した場合。
     */
    @Test
    public void moderatesReportsAndKeepsInquiryNotesPrivate() throws SQLException {
        // 本登録前の会員も問い合わせできるが、通報の送信に必要な本登録は別に確認する。
        assertTrue(auditDao.lockEligibleMember(connection, PENDING, false));
        assertFalse(auditDao.lockEligibleMember(connection, PENDING, true));

        assertNotNull(publicDirectoryDao.findVisiblePropertyById(PROPERTY_ID));
        assertFalse(publicDirectoryDao.findVisibleBusinesses(RESIDENT, "", 26, 0).isEmpty());
        assertNotNull(publicDirectoryDao.findReportTarget(RESIDENT, "OWNER", OWNER, PROPERTY_ID));
        assertNotNull(publicDirectoryDao.findReportTarget(RESIDENT, "BUSINESS", BUSINESS, null));

        // 通報送信・確認中移行・対応完了（物件掲載停止の同時保存）を検証する。
        long reportId = memberSupportDao.submitReport(
                RESIDENT, "PROPERTY", PROPERTY_ID, null, "INACCURATE", "物件情報の確認をお願いします");
        assertNotNull(memberSupportDao.findReportReceipt(reportId, RESIDENT));
        assertFalse(adminReportDao.findReports("", "", "", null, null, "", 26, 0).isEmpty());
        assertNotNull(adminReportDao.findReportById(reportId));

        adminReportDao.assignReport(reportId, ADMIN, ADMIN);

        // 管理権限のない会員による対応完了は拒否され、物件も非公開にならないことを確認する。
        ManagementProblem forbidden = assertThrows(ManagementProblem.class,
                () -> adminReportDao.resolveReport(reportId, RESIDENT, "HIDE_PROPERTY", "掲載内容を確認"));
        assertEquals(403, forbidden.getStatus());
        assertFalse(publicDirectoryDao.findVisibleProperties("", 26, 0).isEmpty());

        adminReportDao.resolveReport(reportId, ADMIN, "HIDE_PROPERTY", "掲載内容を確認");
        assertTrue(publicDirectoryDao.findVisibleProperties("", 26, 0).isEmpty());

        // 問い合わせの所有権チェックと管理者用内部メモの非公開境界を検証する。
        long inquiryId = memberSupportDao.createMemberInquiry(
                PENDING, null, "ACCOUNT", "問い合わせのテスト", "質問本文");
        assertFalse(adminInquiryDao.findInquiries("", "", null, "", 26, 0).isEmpty());
        assertNull(memberSupportDao.findMemberInquiryById(inquiryId, OTHER));
        ManagementProblem otherReply = assertThrows(ManagementProblem.class,
                () -> memberSupportDao.replyMemberInquiry(inquiryId, OTHER, "別の会員の返信"));
        assertEquals(404, otherReply.getStatus());

        adminInquiryDao.addInternalNote(inquiryId, ADMIN, "公開しないメモ");
        adminInquiryDao.replyInquiry(inquiryId, ADMIN, "WAITING_USER", "運営からの返信");
        assertEquals(List.of("運営からの返信", "質問本文"),
                memberSupportDao.findMemberMessages(inquiryId, PENDING, 26, 0)
                        .stream().map(InquiryItem.Message::getBody).toList());
        assertEquals(List.of("運営からの返信", "公開しないメモ", "質問本文"),
                adminInquiryDao.findMessages(inquiryId, 26, 0)
                        .stream().map(InquiryItem.Message::getBody).toList());

        memberSupportDao.replyMemberInquiry(inquiryId, PENDING, "利用者からの回答");
        assertFalse(auditDao.findAuditLogs("HIDE", 26, 0).isEmpty());

        // 会員の復帰では、停止前の本登録状態（PENDING）を監査ログから復元する。
        userDao.changeAccountStatus(ADMIN, PENDING, true, "停止の確認");
        userDao.changeAccountStatus(ADMIN, PENDING, false, "解除の確認");
        assertEquals("PENDING", scalar("SELECT account_status FROM users WHERE user_id = ?", PENDING));

        // 付与を取り消した後は、同じ会員IDからも管理権限を取得できないことを確認する。
        try (var statement = connection.prepareStatement(
                "UPDATE pg_temp.admin_members SET is_active = false WHERE user_id = ?")) {
            statement.setInt(1, ADMIN);
            statement.executeUpdate();
        }
        assertFalse(auditDao.isAdmin(ADMIN));
    }

    /**
     * マスターの設定、アンケートの改訂版、クーポンの停止・再開・個別無効化を実データ構造で検証する。
     * @throws SQLException 保存または照会が失敗した場合。
     */
    @Test
    public void editsCatalogAndKeepsSurveyVersionsAndCouponIssues() throws SQLException {
        assertFalse(catalogDao.findIndustryTypes("", "", 26, 0).isEmpty());
        int categoryId = catalogDao.saveCategory(ADMIN, null, "NEW", "追加カテゴリ", 2, true);
        assertEquals(11, categoryId);
        int industryId = catalogDao.saveIndustryType(ADMIN, null, categoryId, "I9992", "追加業種", 1, true);
        assertEquals(12, industryId);
        assertNotNull(catalogDao.findIndustryTypeById(industryId));

        int qualificationId = catalogDao.saveQualification(
                ADMIN, null, "必要書類の案内", "DOCUMENT", "持参物の案内", null, true);
        catalogDao.saveRequirement(ADMIN, industryId, qualificationId, true, "必要な場合に持参");
        assertFalse(publicDirectoryDao.findPublicRequirements(industryId, 26, 0).isEmpty());
        assertFalse(catalogDao.findRequirements(industryId, 26, 0).isEmpty());

        // 新規申込チェックへ2列未指定で挿入してもトリガーで必須区分・適用条件がスナップショットされ、後の変更に影響されないことを確認する。
        catalogDao.saveRequirement(ADMIN, 11, qualificationId, true, "申込時の条件");
        try (var stmt = connection.prepareStatement(
                "INSERT INTO application_checks(application_id, qualification_id, requirement_name_snapshot) VALUES (301, ?, '必要書類の案内')")) {
            stmt.setInt(1, qualificationId);
            stmt.executeUpdate();
        }
        catalogDao.saveRequirement(ADMIN, 11, qualificationId, false, "後で変更した条件");
        AdminApplicationDetail appDetail = dealDao.findApplicationById(301);
        assertNotNull(appDetail);
        assertEquals(1, appDetail.getChecks().size());
        assertEquals(Boolean.TRUE, appDetail.getChecks().get(0).getRequired());
        assertEquals("申込時の条件", appDetail.getChecks().get(0).getConditionText());

        // アンケートの回答履歴・改訂版作成・元版保護・受付終了を検証する。
        assertFalse(surveyDao.findSurveys("", "", "", 26, 0).isEmpty());
        AdminSurveyItem surveyDetail = surveyDao.findSurveyById(SURVEY_ID);
        assertNotNull(surveyDetail);
        assertFalse(surveyDao.findResponses(SURVEY_ID, 26, 0).isEmpty());

        int revisionId = surveyDao.reviseSurvey(ADMIN, SURVEY_ID);
        assertFalse(surveyDao.findQuestions(revisionId).isEmpty());
        surveyDao.saveQuestion(ADMIN, revisionId, 2, "改訂した設問", "SINGLE", true, List.of("選択肢1", "選択肢2"));
        assertEquals("テスト設問", scalar("SELECT question_text FROM survey_questions WHERE survey_id = 501"));
        assertEquals("改訂した設問", scalar("SELECT question_text FROM survey_questions WHERE survey_id = ?", revisionId));
        surveyDao.closeSurvey(ADMIN, SURVEY_ID, "受付終了の確認");
        assertEquals("CLOSED", scalar("SELECT survey_status FROM surveys WHERE survey_id = 501"));

        // クーポンの下書き編集・配布停止・再開・個別無効化と二重無効化の拒否を検証する。
        assertFalse(couponDao.findCoupons("", "", 26, 0).isEmpty());
        assertFalse(couponDao.findRecipients(COUPON_ID, 26, 0).isEmpty());
        AdminCouponItem.Recipient recipient = couponDao.findRecipient(COUPON_ID, RESIDENT);
        assertNotNull(recipient);
        assertTrue(recipient.isCanRevoke());

        couponDao.updateDraft(
                ADMIN, 602, "更新した下書き", "SERVICE", null, null, "ドリンク",
                OffsetDateTime.parse("2090-01-01T09:00:00+09:00"),
                OffsetDateTime.parse("2090-01-02T09:00:00+09:00"),
                OffsetDateTime.parse("2090-01-03T09:00:00+09:00"),
                20, null);
        // 下書きクーポン編集の監査ログに変更前後の配布条件が記録されていることを確認する。
        String editChanges = String.valueOf(scalar(
                "SELECT changes::text FROM admin_audit_logs WHERE action = 'EDIT_COUPON' ORDER BY audit_log_id DESC LIMIT 1"));
        assertTrue(editChanges.contains("before"));
        assertTrue(editChanges.contains("after"));
        assertTrue(editChanges.contains("更新した下書き"));
        couponDao.changeDistributionStatus(ADMIN, COUPON_ID, true, "一時停止の確認");
        assertFalse(Boolean.TRUE.equals(
                scalar("SELECT revoked_at IS NOT NULL FROM coupon_recipients WHERE coupon_id = 601")));
        couponDao.changeDistributionStatus(ADMIN, COUPON_ID, false, "再開の確認");
        couponDao.revokeRecipient(ADMIN, COUPON_ID, RESIDENT, "個別無効化の確認");
        ManagementProblem duplicateRevoke = assertThrows(ManagementProblem.class,
                () -> couponDao.revokeRecipient(ADMIN, COUPON_ID, RESIDENT, "個別無効化の確認"));
        assertEquals(409, duplicateRevoke.getStatus());
    }

    /**
     * 本番DAOとは独立に、一時テーブルの保存結果を検証する値を取得する。
     * @param sql 最初の行の値を確認するためのテスト用SQL。
     * @param parameters SQLへバインドする値。
     * @return 一件目の最初の列の値。
     * @throws SQLException 結果の照会に失敗した場合。
     */
    private Object scalar(String sql, Object... parameters) throws SQLException {
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
}
