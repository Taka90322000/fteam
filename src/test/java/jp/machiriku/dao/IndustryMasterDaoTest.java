package jp.machiriku.dao;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.Statement;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import jp.machiriku.bean.IndustryMaster;

/** 業種マスタの取得を、一時テーブルで確認する。MACHIRIKU_RUN_DB_TESTS=true のときだけ実行する。 */
public class IndustryMasterDaoTest {
    private Connection connection;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("true".equalsIgnoreCase(System.getenv("MACHIRIKU_RUN_DB_TESTS")));
        connection = Dao.getConnection();
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null) connection.close();
    }

    private IndustryMasterDao dao() {
        Connection borrowed = DemandTestSchema.borrowed(connection);
        return new IndustryMasterDao() { @Override protected Connection openConnection() { return borrowed; } };
    }

    @Test
    public void returnsOnlyActiveEntriesInDisplayOrder() throws Exception {
        try (Statement sql = connection.createStatement()) {
            sql.execute("CREATE TEMP TABLE industry_categories (category_id integer PRIMARY KEY, category_code varchar(3) NOT NULL, category_name varchar(50) NOT NULL, "
                    + "is_active boolean NOT NULL DEFAULT true, display_order integer NOT NULL DEFAULT 1) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE industry_types (industry_type_id integer PRIMARY KEY, category_id integer NOT NULL, industry_code varchar(5) NOT NULL, "
                    + "industry_name varchar(80) NOT NULL, is_active boolean NOT NULL DEFAULT true, display_order integer NOT NULL DEFAULT 1) ON COMMIT PRESERVE ROWS");
            sql.execute("INSERT INTO industry_categories VALUES (1,'C01','飲食',true,2),(2,'C02','小売',true,1),(3,'C03','停止中の分類',false,3)");
            sql.execute("INSERT INTO industry_types VALUES (10,1,'I0101','カフェ',true,2),(11,1,'I0102','レストラン',true,1),(12,1,'I0103','停止中の業種',false,3),"
                    + "(20,2,'I0201','コンビニ',true,1),(30,3,'I0301','停止中の分類の業種',true,1)");
        }
        IndustryMaster master = dao().findActive();
        assertEquals("停止中の分類は出ない。表示順が小さい分類が先", java.util.List.of("C02", "C01"),
                master.categories().stream().map(IndustryMaster.Category::categoryCode).toList());
        IndustryMaster.Category food = master.categories().get(1);
        assertEquals("飲食", food.categoryName());
        assertEquals("停止中の業種は出ない。表示順どおり", java.util.List.of("I0102", "I0101"),
                food.industries().stream().map(IndustryMaster.Industry::industryCode).toList());
        assertEquals(11, food.industries().get(0).industryTypeId());
        assertEquals("レストラン", food.industries().get(0).industryName());
    }

    @Test
    public void worksWithoutDisplayOrderColumnsAndFallsBackToIdOrder() throws Exception {
        try (Statement sql = connection.createStatement()) {
            sql.execute("CREATE TEMP TABLE industry_categories (category_id integer PRIMARY KEY, category_code varchar(3) NOT NULL, category_name varchar(50) NOT NULL, "
                    + "is_active boolean NOT NULL DEFAULT true) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE industry_types (industry_type_id integer PRIMARY KEY, category_id integer NOT NULL, industry_code varchar(5) NOT NULL, "
                    + "industry_name varchar(80) NOT NULL, is_active boolean NOT NULL DEFAULT true) ON COMMIT PRESERVE ROWS");
            sql.execute("INSERT INTO industry_categories VALUES (2,'C02','小売',true),(1,'C01','飲食',true)");
            sql.execute("INSERT INTO industry_types VALUES (11,1,'I0102','レストラン',true),(10,1,'I0101','カフェ',true),(20,2,'I0201','コンビニ',true)");
        }
        IndustryMaster master = dao().findActive();
        assertEquals(java.util.List.of("C01", "C02"), master.categories().stream().map(IndustryMaster.Category::categoryCode).toList());
        assertEquals(java.util.List.of("I0101", "I0102"),
                master.categories().get(0).industries().stream().map(IndustryMaster.Industry::industryCode).toList());
    }

    @Test
    public void emptyMasterIsEmptyList() throws Exception {
        try (Statement sql = connection.createStatement()) {
            sql.execute("CREATE TEMP TABLE industry_categories (category_id integer PRIMARY KEY, category_code varchar(3) NOT NULL, category_name varchar(50) NOT NULL, is_active boolean NOT NULL DEFAULT true) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE industry_types (industry_type_id integer PRIMARY KEY, category_id integer NOT NULL, industry_code varchar(5) NOT NULL, industry_name varchar(80) NOT NULL, is_active boolean NOT NULL DEFAULT true) ON COMMIT PRESERVE ROWS");
        }
        assertTrue(dao().findActive().categories().isEmpty());
    }
}
