package jp.machiriku.dao;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import jp.machiriku.bean.PropertyContractTermInput;
import jp.machiriku.bean.PropertyImageInput;
import jp.machiriku.bean.PropertyInput;

/**
 * 実DBの構造を独立した一時テーブルへ複製し、migrate-property-details.sql適用後の
 * PropertyDaoの登録とロールバックを既存データに影響なく検証するテスト。
 */
public class PropertyDaoTest {
    /** 複製対象の既存テーブル一覧。 */
    private static final List<String> BASE_TABLES = List.of(
            "users", "industry_categories", "industry_types",
            "properties", "property_contract_terms", "property_images", "property_industries");

    /** 一時テーブルを保持するテスト用接続。 */
    private Connection connection;

    /** 一時テーブル接続を利用するテスト対象DAO。 */
    private PropertyDao dao;

    /**
     * 実DBの構造をpg_tempへ複製し、物件詳細マイグレーションSQLを適用してテストデータを準備する。
     *
     * @throws Exception DB接続または一時スキーマ構築に失敗した場合
     */
    @Before
    public void createIsolatedTemporarySchema() throws Exception {
        Assume.assumeTrue("true".equalsIgnoreCase(System.getenv("MACHIRIKU_RUN_DB_TESTS")));
        connection = Dao.getConnection();
        try (Statement sql = connection.createStatement()) {
            for (String table : BASE_TABLES) {
                sql.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL) ON COMMIT PRESERVE ROWS");
                try (ResultSet row = sql.executeQuery("SELECT '" + table + "'::regclass::oid = 'pg_temp." + table + "'::regclass::oid")) {
                    assertTrue(row.next());
                    assertTrue("実テーブルを操作しないこと: " + table, row.getBoolean(1));
                }
            }

            // 移行SQLを一時テーブル向けに適用し、さらに再実行可能（冪等）であることも検証する。
            String migrationSql = tempMigrationSql();
            sql.execute(migrationSql);
            sql.execute(migrationSql);

            // LIKEでは複製されない外部キーを一時テーブル同士に付与する。
            sql.execute("ALTER TABLE pg_temp.industry_types ADD FOREIGN KEY (category_id) REFERENCES pg_temp.industry_categories(category_id)");
            sql.execute("ALTER TABLE pg_temp.properties ADD FOREIGN KEY (owner_user_id) REFERENCES pg_temp.users(user_id)");
            sql.execute("ALTER TABLE pg_temp.property_contract_terms ADD FOREIGN KEY (property_id) REFERENCES pg_temp.properties(property_id)");
            sql.execute("ALTER TABLE pg_temp.property_images ADD FOREIGN KEY (property_id) REFERENCES pg_temp.properties(property_id)");
            sql.execute("ALTER TABLE pg_temp.property_industries ADD FOREIGN KEY (property_id) REFERENCES pg_temp.properties(property_id)");
            sql.execute("ALTER TABLE pg_temp.property_industries ADD FOREIGN KEY (industry_type_id) REFERENCES pg_temp.industry_types(industry_type_id)");

            // テスト用の会員・業種マスタを投入する。
            sql.execute("INSERT INTO industry_categories (category_id, category_code, category_name) VALUES (1, 'C01', '飲食')");
            sql.execute("INSERT INTO industry_types (industry_type_id, category_id, industry_code, industry_name, is_active) VALUES "
                    + "(10, 1, 'I0101', 'カフェ・喫茶店', true), (11, 1, 'I0107', 'パン屋', true), (12, 1, 'I9999', '無効業種', false)");
            sql.execute("INSERT INTO users (user_id, display_name, email, user_role, line_subject, account_status) VALUES "
                    + "(1, '有効オーナー', 'owner@example.invalid', 'OWNER', 'sub-owner', 'ACTIVE'),"
                    + "(2, '本登録前オーナー', NULL, 'OWNER', 'sub-pending', 'PENDING'),"
                    + "(3, '住民会員', NULL, 'RESIDENT', 'sub-resident', 'ACTIVE')");
        }

        Connection borrowed = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    try {
                        return method.invoke(connection, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
        dao = new PropertyDao() {
            @Override
            protected Connection openConnection() {
                return borrowed;
            }
        };
    }

    /**
     * テスト終了後に接続を閉じ、一時テーブルを破棄する。
     *
     * @throws Exception 接続クローズに失敗した場合
     */
    @After
    public void closeTemporarySchema() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * 物件本体と4つの子テーブル（契約条件・業種・設備・写真）が1トランザクションで保存されることを検証する。
     *
     * @throws Exception DB操作に失敗した場合
     */
    @Test
    public void createsPropertyWithAllDetailsTermsIndustriesFacilitiesAndImages() throws Exception {
        PropertyInput input = sampleInput(List.of("I0101", "I0107"), List.of("INUKI", "CITY_GAS", "EXHAUST_DUCT"));
        int propertyId = dao.createProperty(1, input);
        assertTrue(propertyId > 0);

        assertEquals("オリオン通りサウスビル 1階/101", text("SELECT property_name FROM properties WHERE property_id=" + propertyId));
        assertEquals("東武宇都宮駅 徒歩3分", text("SELECT transport_access FROM properties WHERE property_id=" + propertyId));
        assertEquals("15000", text("SELECT management_fee_yen::text FROM properties WHERE property_id=" + propertyId));
        assertEquals("MONTHS", text("SELECT deposit_kind FROM properties WHERE property_id=" + propertyId));
        assertEquals("2", text("SELECT deposit_months::text FROM properties WHERE property_id=" + propertyId));
        assertEquals("NONE", text("SELECT key_money_kind FROM properties WHERE property_id=" + propertyId));
        assertEquals("AMOUNT", text("SELECT security_deposit_kind FROM properties WHERE property_id=" + propertyId));
        assertEquals("500000", text("SELECT security_deposit_yen::text FROM properties WHERE property_id=" + propertyId));
        assertEquals("1F", text("SELECT floor_code FROM properties WHERE property_id=" + propertyId));
        assertEquals("4F", text("SELECT total_floors_code FROM properties WHERE property_id=" + propertyId));
        assertEquals("RC", text("SELECT building_structure FROM properties WHERE property_id=" + propertyId));
        assertEquals("2008", text("SELECT built_year::text FROM properties WHERE property_id=" + propertyId));
        assertEquals("3", text("SELECT built_month::text FROM properties WHERE property_id=" + propertyId));
        assertEquals("CONSULT_IMMEDIATE", text("SELECT handover_timing FROM properties WHERE property_id=" + propertyId));
        assertEquals("元カフェ居抜き", text("SELECT condition_memo FROM properties WHERE property_id=" + propertyId));
        assertEquals(2, count("SELECT count(*) FROM property_contract_terms WHERE property_id=" + propertyId));
        assertEquals(2, count("SELECT count(*) FROM property_industries WHERE property_id=" + propertyId));
        assertEquals(3, count("SELECT count(*) FROM property_facilities WHERE property_id=" + propertyId));
        assertEquals(1, count("SELECT count(*) FROM property_images WHERE property_id=" + propertyId));
        assertEquals("EXTERIOR", text("SELECT image_kind FROM property_images WHERE property_id=" + propertyId));
    }

    /**
     * 無効な業種・設備コードや権限外会員による登録は全テーブルがロールバックされることを検証する。
     *
     * @throws Exception DB操作に失敗した場合
     */
    @Test
    public void rejectsUnauthorizedUsersAndInvalidMastersWithoutPartialWrites() throws Exception {
        // 本登録前オーナー・住民会員はSQLState=28000で拒否される。
        for (int userId : new int[] { 2, 3, 999 }) {
            SQLException err = assertThrows(SQLException.class,
                    () -> dao.createProperty(userId, sampleInput(List.of("I0101"), List.of("INUKI"))));
            assertEquals("28000", err.getSQLState());
        }

        // 無効業種・未知設備コードはIllegalArgumentExceptionでロールバックされる。
        assertThrows(IllegalArgumentException.class,
                () -> dao.createProperty(1, sampleInput(List.of("I9999"), List.of("INUKI"))));
        assertThrows(IllegalArgumentException.class,
                () -> dao.createProperty(1, sampleInput(List.of("I0101"), List.of("NO_SUCH_FACILITY"))));

        assertEquals(0, count("SELECT count(*) FROM properties"));
        assertEquals(0, count("SELECT count(*) FROM property_contract_terms"));
        assertEquals(0, count("SELECT count(*) FROM property_industries"));
        assertEquals(0, count("SELECT count(*) FROM property_facilities"));
        assertEquals(0, count("SELECT count(*) FROM property_images"));
    }

    /**
     * テスト用の完全な物件入力データを生成する。
     *
     * @param industryCodes 業種コード一覧
     * @param facilityCodes 設備コード一覧
     * @return テスト用 PropertyInput
     */
    private static PropertyInput sampleInput(List<String> industryCodes, List<String> facilityCodes) {
        return new PropertyInput(
                "オリオン通りサウスビル 1階/101",
                "栃木県",
                "宇都宮市",
                "江野町5-12",
                new BigDecimal("36.559312"),
                new BigDecimal("139.882105"),
                "東武宇都宮駅 徒歩3分",
                new BigDecimal("52.80"),
                198000,
                15000,
                "MONTHS",
                2,
                "NONE",
                null,
                "AMOUNT",
                null,
                500000,
                "1F",
                "4F",
                "RC",
                2008,
                3,
                "CONSULT_IMMEDIATE",
                "元カフェ居抜き",
                "アーケード沿いの路面店舗です。",
                "https://example.invalid/prop/1",
                "OBTAINED",
                "OPEN",
                List.of(
                        new PropertyContractTermInput("LONG", null, null, null),
                        new PropertyContractTermInput("POPUP", 3, 30, 8000)),
                industryCodes,
                facilityCodes,
                List.of(new PropertyImageInput(
                        "/property-images/12345678-1234-1234-1234-123456789abc.jpg",
                        1,
                        "店舗外観",
                        "EXTERIOR")));
    }

    /**
     * migrate-property-details.sqlを読み込み、psqlメタコマンドとGRANTを除いてpg_temp対象へ書き換える。
     *
     * @return pg_temp上で実行可能なDDL文字列
     * @throws Exception SQLファイルの読み込みに失敗した場合
     */
    private static String tempMigrationSql() throws Exception {
        String raw = Files.readString(Path.of("database/migrate-property-details.sql"));
        // psql専用のメタコマンドとロール権限付与行を取り除き、一時テーブルへ適用する。
        StringBuilder cleaned = new StringBuilder();
        boolean skippingGrant = false;
        for (String line : raw.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("\\set")) {
                continue;
            }
            if (trimmed.startsWith("GRANT ")) {
                skippingGrant = !trimmed.endsWith(";");
                continue;
            }
            if (skippingGrant) {
                if (trimmed.endsWith(";")) {
                    skippingGrant = false;
                }
                continue;
            }
            cleaned.append(line).append('\n');
        }
        return cleaned.toString()
                .replace("public.properties", "pg_temp.properties")
                .replace("public.property_images", "pg_temp.property_images")
                .replace("public.facilities", "pg_temp.facilities")
                .replace("public.property_facilities", "pg_temp.property_facilities");
    }

    /**
     * 単一整数を返すクエリを実行する。
     *
     * @param query 実行するSELECT文
     * @return 1列目の整数値
     * @throws SQLException クエリ実行に失敗した場合
     */
    private int count(String query) throws SQLException {
        try (Statement sql = connection.createStatement(); ResultSet row = sql.executeQuery(query)) {
            assertTrue(row.next());
            return row.getInt(1);
        }
    }

    /**
     * 単一文字列を返すクエリを実行する。
     *
     * @param query 実行するSELECT文
     * @return 1列目の文字列
     * @throws SQLException クエリ実行に失敗した場合
     */
    private String text(String query) throws SQLException {
        try (Statement sql = connection.createStatement(); ResultSet row = sql.executeQuery(query)) {
            assertTrue(row.next());
            return row.getString(1);
        }
    }
}
