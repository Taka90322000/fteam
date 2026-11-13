package jp.machiriku.dao;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import jp.machiriku.bean.BusinessProfileInput;
import jp.machiriku.bean.LocationInput;
import jp.machiriku.bean.OwnerProfileInput;
import jp.machiriku.bean.ResidentProfileInput;

/** 実DBの構造を独立した一時テーブルへ複製し、既存会員・実テーブルの採番を変更せず保存と取消を確認する。 */
public class ProfileDaoTest {
    private static final String AREA = "{\"type\":\"Polygon\",\"coordinates\":[[[139.88,36.56],[139.89,36.56],[139.89,36.57],[139.88,36.56]]]}";
    private static final List<String> TABLES = List.of("users", "industry_categories", "industry_types", "qualifications",
            "resident_profiles", "business_profiles", "owner_profiles", "user_locations", "user_industry_preferences", "business_qualifications", "notification_settings");
    private Connection connection;
    private ProfileDao dao;

    @Before
    public void createIsolatedTemporarySchema() throws Exception {
        Assume.assumeTrue("true".equalsIgnoreCase(System.getenv("MACHIRIKU_RUN_DB_TESTS")));
        connection = Dao.getConnection();
        try (Statement sql = connection.createStatement()) {
            for (String table : TABLES) {
                // INCLUDING IDENTITYで実テーブルとは別の一時シーケンスを作り、commit後も一時表を保持する。
                if (table.equals("owner_profiles")) {
                    // 管理者が本番用SQLを実行する前でも、新規テーブルの形を一時表だけで検証できる。
                    sql.execute("CREATE TEMP TABLE owner_profiles (user_id integer PRIMARY KEY,representative_name varchar(100) NOT NULL,"
                            + "company_name varchar(150),phone_e164 varchar(16) NOT NULL UNIQUE CHECK (phone_e164 ~ '^\\+[1-9][0-9]{7,14}$'),"
                            + "icon_image_url text,updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP) ON COMMIT PRESERVE ROWS");
                } else {
                    sql.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL) ON COMMIT PRESERVE ROWS");
                }
                try (ResultSet row = sql.executeQuery("SELECT '" + table + "'::regclass::oid = 'pg_temp." + table + "'::regclass::oid")) {
                    assertTrue(row.next());
                    assertTrue("実テーブルを操作しないこと: " + table, row.getBoolean(1));
                }
            }
            // 管理者による移行の前後どちらの実DBでも、検証対象は新しい列構成にそろえる。
            sql.execute("ALTER TABLE users DROP COLUMN IF EXISTS phone_e164");
            sql.execute("ALTER TABLE business_profiles ADD COLUMN IF NOT EXISTS phone_e164 varchar(16), ADD COLUMN IF NOT EXISTS icon_image_url text");
            sql.execute("ALTER TABLE business_profiles ALTER COLUMN phone_e164 SET NOT NULL, ADD CHECK (phone_e164 ~ '^\\+[1-9][0-9]{7,14}$')");
            sql.execute("CREATE UNIQUE INDEX test_business_phone_unique ON business_profiles(phone_e164)");
            // LIKEで複製されない外部キーは、一時テーブル同士にだけ追加する。
            sql.execute("ALTER TABLE resident_profiles ADD FOREIGN KEY (user_id) REFERENCES pg_temp.users(user_id)");
            sql.execute("ALTER TABLE business_profiles ADD FOREIGN KEY (user_id) REFERENCES pg_temp.users(user_id)");
            sql.execute("ALTER TABLE owner_profiles ADD FOREIGN KEY (user_id) REFERENCES pg_temp.users(user_id)");
            sql.execute("ALTER TABLE user_locations ADD FOREIGN KEY (user_id) REFERENCES pg_temp.users(user_id)");
            sql.execute("ALTER TABLE industry_types ADD FOREIGN KEY (category_id) REFERENCES pg_temp.industry_categories(category_id)");
            sql.execute("ALTER TABLE user_industry_preferences ADD FOREIGN KEY (user_id) REFERENCES pg_temp.users(user_id)");
            sql.execute("ALTER TABLE user_industry_preferences ADD FOREIGN KEY (industry_type_id) REFERENCES pg_temp.industry_types(industry_type_id)");
            sql.execute("ALTER TABLE business_qualifications ADD FOREIGN KEY (business_user_id) REFERENCES pg_temp.business_profiles(user_id)");
            sql.execute("ALTER TABLE business_qualifications ADD FOREIGN KEY (qualification_id) REFERENCES pg_temp.qualifications(qualification_id)");
            sql.execute("ALTER TABLE notification_settings ADD FOREIGN KEY (user_id) REFERENCES pg_temp.users(user_id)");
            sql.execute("INSERT INTO industry_categories (category_id,category_code,category_name) VALUES (1,'001','飲食')");
            sql.execute("INSERT INTO industry_types (industry_type_id,category_id,industry_code,industry_name,is_active) VALUES "
                    + "(10,1,'I0101','カフェ',true),(11,1,'I0201','食堂',true),(12,1,'I9999','無効な業種',false)");
            sql.execute("INSERT INTO qualifications (qualification_id,qualification_name,requirement_kind,is_active) VALUES "
                    + "(100,'テスト資格','QUALIFICATION',true),(101,'テスト許認可','PERMIT',true),(102,'無効な資格','QUALIFICATION',false)");
            sql.execute("INSERT INTO users (user_id,display_name,email,user_role,line_subject,account_status) VALUES "
                    + "(1,'LINE住民','resident-before@example.invalid','RESIDENT','test-resident','PENDING'),"
                    + "(2,'LINE事業主','business-before@example.invalid','BUSINESS','test-business','PENDING'),"
                    + "(3,'別会員','used@example.invalid','RESIDENT','test-other','ACTIVE'),"
                    + "(4,'停止会員',NULL,'RESIDENT','test-banned','BAN'),"
                    + "(5,'退会会員',NULL,'BUSINESS','test-withdrawn','WITHDRAWN'),"
                    + "(6,'LINEオーナー','owner-before@example.invalid','OWNER','test-owner','PENDING'),"
                    + "(7,'別の事業主',NULL,'BUSINESS','test-business-other','PENDING'),"
                    + "(8,'別のオーナー',NULL,'OWNER','test-owner-other','PENDING')");
        }
        Connection borrowed = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("close")) { return null; }
                    try { return method.invoke(connection, arguments); }
                    catch (InvocationTargetException e) { throw e.getCause(); }
                });
        dao = new ProfileDao() { @Override protected Connection openConnection() { return borrowed; } };
    }

    @After
    public void closeTemporarySchema() throws Exception {
        if (connection != null) { connection.close(); }
    }

    @Test
    public void savesResidentIdentityDemographicsLocationsAndNotificationTogether() throws Exception {
        var saved = dao.saveResident(1, resident("登録ニックネーム", "resident-new@example.invalid", home(), work(), true));
        assertEquals("登録ニックネーム", saved.getDisplayName());
        assertEquals("resident-new@example.invalid", saved.getEmail());
        assertEquals("RESIDENT", saved.getUserRole());
        assertEquals("ACTIVE", saved.getAccountStatus());
        assertEquals("20代", text("SELECT age_group FROM resident_profiles WHERE user_id=1"));
        assertEquals("その他", text("SELECT gender FROM resident_profiles WHERE user_id=1"));
        assertEquals(2, count("SELECT count(*) FROM user_locations WHERE user_id=1"));
        assertEquals("宇都宮市", text("SELECT municipality FROM user_locations WHERE user_id=1 AND location_kind='HOME'"));
        assertEquals("36.561000", text("SELECT latitude::text FROM user_locations WHERE user_id=1 AND location_kind='HOME'"));
        assertEquals("true", text("SELECT email_enabled::text FROM notification_settings WHERE user_id=1"));
        assertEquals("test-resident", saved.getLineSubject());
    }

    @Test
    public void residentResubmissionUpdatesRatherThanDuplicatesAndRemovesOmittedWork() throws Exception {
        dao.saveResident(1, resident("最初", "resident-new@example.invalid", home(), work(), true));
        var saved = dao.saveResident(1, resident("更新後", null, home(), null, true));
        assertEquals("更新後", saved.getDisplayName());
        assertNull(saved.getEmail());
        assertEquals(1, count("SELECT count(*) FROM resident_profiles WHERE user_id=1"));
        assertEquals(1, count("SELECT count(*) FROM user_locations WHERE user_id=1"));
        assertEquals(0, count("SELECT count(*) FROM user_locations WHERE user_id=1 AND location_kind='WORK'"));
        assertEquals("false", text("SELECT email_enabled::text FROM notification_settings WHERE user_id=1"));
    }

    @Test
    public void residentWantedIndustriesResolveCodesToActualIdsAndCanBeCleared() throws Exception {
        ResidentProfileInput input = new ResidentProfileInput("住民", null, "20代", "その他", null, home(), null, false,
                List.of("I0201", "I0101", "I0101"));
        dao.saveResident(1, input);
        assertEquals(2, count("SELECT count(*) FROM user_industry_preferences WHERE user_id=1"));
        assertEquals(1, count("SELECT count(*) FROM user_industry_preferences WHERE user_id=1 AND industry_type_id=10"));
        assertEquals(1, count("SELECT count(*) FROM user_industry_preferences WHERE user_id=1 AND industry_type_id=11"));
        dao.saveResident(1, resident("住民", null, home(), null, false));
        assertEquals(0, count("SELECT count(*) FROM user_industry_preferences WHERE user_id=1"));
    }

    @Test
    public void invalidResidentIndustryRollsBackIdentityProfileAndAddress() throws Exception {
        for (String code : List.of("I9999", "I8888")) {
            ResidentProfileInput input = new ResidentProfileInput("保存されない住民", null, "20代", "その他", null,
                    home(), work(), false, List.of(code));
            assertThrows(IllegalArgumentException.class, () -> dao.saveResident(1, input));
            assertEquals("LINE住民", text("SELECT display_name FROM users WHERE user_id=1"));
            assertEquals("PENDING", text("SELECT account_status FROM users WHERE user_id=1"));
            assertEquals(0, count("SELECT count(*) FROM resident_profiles"));
            assertEquals(0, count("SELECT count(*) FROM user_locations"));
            assertEquals(0, count("SELECT count(*) FROM user_industry_preferences"));
        }
    }

    @Test
    public void savesOwnerProfileContactsPreferencesAndNotifications() throws Exception {
        var saved = dao.saveOwner(6, owner("owner-new@example.invalid", "テスト不動産", List.of(10, 11), true));
        assertEquals("LINEオーナー", saved.getDisplayName());
        assertEquals("OWNER", saved.getUserRole());
        assertEquals("ACTIVE", saved.getAccountStatus());
        assertEquals("owner-new@example.invalid", saved.getEmail());
        assertEquals("+819055555555", text("SELECT phone_e164 FROM owner_profiles WHERE user_id=6"));
        assertEquals("入力したオーナー氏名", text("SELECT representative_name FROM owner_profiles WHERE user_id=6"));
        assertEquals("テスト不動産", text("SELECT company_name FROM owner_profiles WHERE user_id=6"));
        assertEquals(2, count("SELECT count(*) FROM user_industry_preferences WHERE user_id=6"));
        assertEquals("true", text("SELECT email_enabled::text FROM notification_settings WHERE user_id=6"));
    }

    @Test
    public void ownerOptionalEmailCompanyAndRepeatedSubmissionAreSavedCorrectly() throws Exception {
        dao.saveOwner(6, owner("owner-new@example.invalid", "最初の法人名", List.of(10), true));
        var saved = dao.saveOwner(6, owner(null, null, List.of(11), true));
        assertNull(saved.getEmail());
        assertNull(text("SELECT company_name FROM owner_profiles WHERE user_id=6"));
        assertEquals(1, count("SELECT count(*) FROM owner_profiles WHERE user_id=6"));
        assertEquals("11", text("SELECT industry_type_id::text FROM user_industry_preferences WHERE user_id=6"));
        assertEquals("false", text("SELECT email_enabled::text FROM notification_settings WHERE user_id=6"));
    }

    @Test
    public void lateOwnerFailureRollsBackUserProfileAndPreferences() throws Exception {
        execute("ALTER TABLE notification_settings ADD CONSTRAINT test_email_disabled CHECK (NOT email_enabled)");
        assertThrows(SQLException.class, () -> dao.saveOwner(6, owner("owner-new@example.invalid", "法人名", List.of(10), true)));
        assertEquals("owner-before@example.invalid", text("SELECT email FROM users WHERE user_id=6"));
        assertEquals("PENDING", text("SELECT account_status FROM users WHERE user_id=6"));
        assertEquals(0, count("SELECT count(*) FROM owner_profiles"));
        assertEquals(0, count("SELECT count(*) FROM user_industry_preferences"));
    }

    @Test
    public void ownerCannotSaveForDifferentRoleAndInactiveIndustryIsRejected() throws Exception {
        SQLException error = assertThrows(SQLException.class, () -> dao.saveOwner(1, owner(null, null, List.of(10), false)));
        assertEquals("28000", error.getSQLState());
        assertThrows(IllegalArgumentException.class, () -> dao.saveOwner(6, owner(null, null, List.of(12), false)));
        assertEquals(0, count("SELECT count(*) FROM owner_profiles"));
    }

    @Test
    public void savesIconReferencesAndReadsContactsFromEachProfile() throws Exception {
        assertEquals(Map.of(), dao.findProfileContact(6, "OWNER"));
        assertEquals(Map.of(), dao.findProfileContact(2, "BUSINESS"));
        dao.saveOwner(6, new OwnerProfileInput("オーナー", "法人名", null, "+819055555555", List.of(10), false,
                "https://example.invalid/owner.png"));
        dao.saveBusiness(2, new BusinessProfileInput("店舗", "代表者", null, "+819022222222", "宇都宮", 1,
                null, null, List.of(10), List.of(), AREA, false, "/assets/business.png"));
        assertEquals(Map.of("phone", "+819055555555", "iconImageUrl", "https://example.invalid/owner.png"),
                dao.findProfileContact(6, "OWNER"));
        assertEquals(Map.of("phone", "+819022222222", "iconImageUrl", "/assets/business.png"),
                dao.findProfileContact(2, "BUSINESS"));
        assertEquals(0, count("SELECT count(*) FROM pg_attribute WHERE attrelid='pg_temp.users'::regclass AND attname='phone_e164' AND NOT attisdropped"));
        dao.saveOwner(6, owner(null, null, List.of(10), false));
        dao.saveBusiness(2, business(null, List.of(10), List.of(), AREA, false));
        assertNull(text("SELECT icon_image_url FROM owner_profiles WHERE user_id=6"));
        assertNull(text("SELECT icon_image_url FROM business_profiles WHERE user_id=2"));
    }

    @Test
    public void profilePhoneConstraintsRejectMissingOrDuplicateNumbersAtomically() throws Exception {
        SQLException ownerMissing = assertThrows(SQLException.class, () -> dao.saveOwner(6,
                new OwnerProfileInput("オーナー", null, null, null, List.of(10), false)));
        assertEquals("23502", ownerMissing.getSQLState());
        SQLException businessMissing = assertThrows(SQLException.class, () -> dao.saveBusiness(2,
                new BusinessProfileInput("店舗", "代表者", null, null, "宇都宮", 1,
                        null, null, List.of(10), List.of(), AREA, false)));
        assertEquals("23502", businessMissing.getSQLState());
        assertEquals("PENDING", text("SELECT account_status FROM users WHERE user_id=6"));
        assertEquals("PENDING", text("SELECT account_status FROM users WHERE user_id=2"));
        dao.saveOwner(6, owner(null, null, List.of(10), false));
        dao.saveBusiness(2, business(null, List.of(10), List.of(), AREA, false));
        SQLException ownerDuplicate = assertThrows(SQLException.class, () -> dao.saveOwner(8, owner(null, null, List.of(10), false)));
        SQLException businessDuplicate = assertThrows(SQLException.class, () -> dao.saveBusiness(7, business(null, List.of(10), List.of(), AREA, false)));
        assertEquals("23505", ownerDuplicate.getSQLState());
        assertEquals("23505", businessDuplicate.getSQLState());
        assertEquals("PENDING", text("SELECT account_status FROM users WHERE user_id=8"));
        assertEquals("PENDING", text("SELECT account_status FROM users WHERE user_id=7"));
        assertEquals(1, count("SELECT count(*) FROM owner_profiles"));
        assertEquals(1, count("SELECT count(*) FROM business_profiles"));
    }

    @Test
    public void savesBusinessPolygonMastersAndOnlyEditableUserFields() throws Exception {
        var saved = dao.saveBusiness(2, business("business-new@example.invalid", List.of(10, 11), List.of(100), AREA, true));
        assertEquals("LINE事業主", saved.getDisplayName());
        assertEquals("BUSINESS", saved.getUserRole());
        assertEquals("ACTIVE", saved.getAccountStatus());
        assertEquals("+819022222222", text("SELECT phone_e164 FROM business_profiles WHERE user_id=2"));
        assertEquals("手入力の代表者", text("SELECT representative_name FROM business_profiles WHERE user_id=2"));
        assertEquals("テストカフェ", text("SELECT business_name FROM business_profiles WHERE user_id=2"));
        assertEquals("5年以上", text("SELECT industry_experience FROM business_profiles WHERE user_id=2"));
        assertEquals("あり", text("SELECT management_experience FROM business_profiles WHERE user_id=2"));
        assertEquals("4326", text("SELECT ST_SRID(area)::text FROM business_profiles WHERE user_id=2"));
        assertEquals("POLYGON", text("SELECT GeometryType(area) FROM business_profiles WHERE user_id=2"));
        assertEquals("true", text("SELECT ST_IsValid(area)::text FROM business_profiles WHERE user_id=2"));
        assertBusinessArea(AREA);
        assertEquals(2, count("SELECT count(*) FROM user_industry_preferences WHERE user_id=2"));
        assertEquals(1, count("SELECT count(*) FROM business_qualifications WHERE business_user_id=2 AND is_active"));
        assertEquals("true", text("SELECT email_enabled::text FROM notification_settings WHERE user_id=2"));
    }

    @Test
    public void businessWithoutEmailCanCompleteAndDoesNotEnableEmailNotifications() throws Exception {
        var saved = dao.saveBusiness(2, business(null, List.of(10), List.of(), AREA, true));
        assertNull(saved.getEmail());
        assertEquals("ACTIVE", saved.getAccountStatus());
        assertEquals("false", text("SELECT email_enabled::text FROM notification_settings WHERE user_id=2"));
    }

    @Test
    public void businessResubmissionReplacesAreaAndIndustrySelectionAndPreservesQualificationDetails() throws Exception {
        dao.saveBusiness(2, business(null, List.of(10), List.of(100, 101), AREA, false));
        execute("UPDATE business_qualifications SET credential_number='TEST-CREDENTIAL',obtained_on=DATE '2020-01-01' WHERE business_user_id=2 AND qualification_id=100");
        String updatedArea = "{\"type\":\"Polygon\",\"coordinates\":[[[139.90,36.58],[139.91,36.58],[139.91,36.59],[139.90,36.58]]]}";
        dao.saveBusiness(2, business(null, List.of(11), List.of(100), updatedArea, false));
        assertEquals(1, count("SELECT count(*) FROM business_profiles WHERE user_id=2"));
        assertBusinessArea(updatedArea);
        assertEquals("11", text("SELECT industry_type_id::text FROM user_industry_preferences WHERE user_id=2"));
        assertEquals("TEST-CREDENTIAL", text("SELECT credential_number FROM business_qualifications WHERE business_user_id=2 AND qualification_id=100"));
        assertEquals("2020-01-01", text("SELECT obtained_on::text FROM business_qualifications WHERE business_user_id=2 AND qualification_id=100"));
        assertEquals("false", text("SELECT is_active::text FROM business_qualifications WHERE business_user_id=2 AND qualification_id=101"));
        dao.saveBusiness(2, business(null, List.of(11), List.of(101), updatedArea, false));
        assertEquals("true", text("SELECT is_active::text FROM business_qualifications WHERE business_user_id=2 AND qualification_id=101"));
    }

    @Test
    public void lateResidentFailureRollsBackUserProfileAndLocations() throws Exception {
        execute("ALTER TABLE notification_settings ADD CONSTRAINT test_email_disabled CHECK (NOT email_enabled)");
        SQLException failure = assertThrows(SQLException.class,
                () -> dao.saveResident(1, resident("保存されない名前", "resident-new@example.invalid", home(), work(), true)));
        assertEquals("23514", failure.getSQLState());
        assertEquals("LINE住民", text("SELECT display_name FROM users WHERE user_id=1"));
        assertEquals("PENDING", text("SELECT account_status FROM users WHERE user_id=1"));
        assertEquals(0, count("SELECT count(*) FROM resident_profiles"));
        assertEquals(0, count("SELECT count(*) FROM user_locations"));
        assertEquals(0, count("SELECT count(*) FROM notification_settings"));
    }

    @Test
    public void lateBusinessFailureRollsBackAllRelatedWrites() throws Exception {
        execute("ALTER TABLE notification_settings ADD CONSTRAINT test_email_disabled CHECK (NOT email_enabled)");
        assertThrows(SQLException.class,
                () -> dao.saveBusiness(2, business("business-new@example.invalid", List.of(10, 11), List.of(100), AREA, true)));
        assertEquals("business-before@example.invalid", text("SELECT email FROM users WHERE user_id=2"));
        assertEquals("PENDING", text("SELECT account_status FROM users WHERE user_id=2"));
        for (String table : List.of("business_profiles", "user_industry_preferences", "business_qualifications", "notification_settings")) {
            assertEquals(table, 0, count("SELECT count(*) FROM " + table));
        }
    }

    @Test
    public void invalidOrSelfCrossingPolygonNeverCompletesRegistration() throws Exception {
        String crossing = "{\"type\":\"Polygon\",\"coordinates\":[[[139,36],[140,37],[140,36],[139,37],[139,36]]]}";
        assertThrows(IllegalArgumentException.class, () -> dao.saveBusiness(2, business(null, List.of(10), List.of(), crossing, false)));
        assertEquals("PENDING", text("SELECT account_status FROM users WHERE user_id=2"));
        assertEquals(0, count("SELECT count(*) FROM business_profiles"));
    }

    @Test
    public void inactiveOrUnknownMastersAreRejectedWithoutWriting() throws Exception {
        for (int id : new int[]{12, 999}) {
            assertThrows(IllegalArgumentException.class, () -> dao.saveBusiness(2, business(null, List.of(id), List.of(), AREA, false)));
        }
        assertThrows(IllegalArgumentException.class, () -> dao.saveBusiness(2, business(null, List.of(10), List.of(102), AREA, false)));
        assertEquals(0, count("SELECT count(*) FROM business_profiles"));
        assertEquals("PENDING", text("SELECT account_status FROM users WHERE user_id=2"));
    }

    @Test
    public void databaseRoleAndAccountStatusAreCheckedAgainInsideTransaction() throws Exception {
        for (int id : new int[]{2, 4, 999}) {
            SQLException error = assertThrows(SQLException.class, () -> dao.saveResident(id, resident("名前", null, home(), null, false)));
            assertEquals("28000", error.getSQLState());
        }
        SQLException error = assertThrows(SQLException.class, () -> dao.saveBusiness(5, business(null, List.of(10), List.of(), AREA, false)));
        assertEquals("28000", error.getSQLState());
        assertEquals(0, count("SELECT count(*) FROM resident_profiles"));
        assertEquals(0, count("SELECT count(*) FROM business_profiles"));
    }

    @Test
    public void choicesUseActiveDatabaseIdsRatherThanDisplayOrder() throws Exception {
        assertEquals(List.of(10, 11), dao.listIndustries().stream().map(choice -> choice.id()).toList());
        assertEquals(List.of(100, 101), dao.listQualifications().stream().map(choice -> choice.id()).toList());
    }

    private ResidentProfileInput resident(String name, String email, LocationInput home, LocationInput work, boolean notify) {
        return new ResidentProfileInput(name, email, "20代", "その他", null, home, work, notify);
    }

    private BusinessProfileInput business(String email, List<Integer> industries, List<Integer> qualifications, String area, boolean notify) {
        return new BusinessProfileInput("テストカフェ", "手入力の代表者", email, "+819022222222", "宇都宮駅周辺", 3,
                "あり", "5年以上", industries, qualifications, area, notify);
    }

    private OwnerProfileInput owner(String email, String company, List<Integer> industries, boolean notify) {
        return new OwnerProfileInput("入力したオーナー氏名", company, email, "+819055555555", industries, notify);
    }

    private LocationInput home() { return location("HOME", "本町1-1"); }
    private LocationInput work() { return location("WORK", "駅前通り2-2"); }
    private LocationInput location(String kind, String address) {
        return new LocationInput(kind, "栃木県", "宇都宮市", address, new BigDecimal("36.561000"), new BigDecimal("139.883000"));
    }

    private void assertBusinessArea(String geoJson) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT ST_Equals(area, ST_SetSRID(ST_GeomFromGeoJSON(?),4326)) FROM business_profiles WHERE user_id=2")) {
            statement.setString(1, geoJson);
            try (ResultSet row = statement.executeQuery()) {
                assertTrue(row.next());
                assertTrue("送信した出店希望範囲が保存されること", row.getBoolean(1));
            }
        }
    }

    private void execute(String query) throws SQLException { try (Statement sql = connection.createStatement()) { sql.execute(query); } }
    private int count(String query) throws SQLException { try (Statement sql = connection.createStatement(); ResultSet row = sql.executeQuery(query)) { row.next(); return row.getInt(1); } }
    private String text(String query) throws SQLException { try (Statement sql = connection.createStatement(); ResultSet row = sql.executeQuery(query)) { assertTrue(row.next()); return row.getString(1); } }
}
