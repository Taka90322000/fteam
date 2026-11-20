package jp.machiriku.dao;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import jp.machiriku.bean.PropertyVote;

/**
 * 投票テーブルの構造(定義書の制約)を一時テーブルとして作り、実テーブルには一切触れずに保存・再投票・取消を確認する。
 * MACHIRIKU_RUN_DB_TESTS=true のときだけ実行する。
 */
public class VoteDaoTest {
    private Connection connection;
    private VoteDao dao;

    @Before
    public void createTemporarySchema() throws Exception {
        Assume.assumeTrue("true".equalsIgnoreCase(System.getenv("MACHIRIKU_RUN_DB_TESTS")));
        connection = Dao.getConnection();
        try (Statement sql = connection.createStatement()) {
            // 一時テーブルは検索パスの先頭(pg_temp)に入るため、DAOの修飾なしSQLはこちらを参照する。
            sql.execute("CREATE TEMP TABLE users (user_id integer PRIMARY KEY, user_role varchar(20) NOT NULL, account_status varchar(20) NOT NULL, withdrawn_at timestamptz) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE resident_profiles (user_id integer PRIMARY KEY REFERENCES pg_temp.users(user_id)) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE user_locations (location_id integer PRIMARY KEY, user_id integer NOT NULL REFERENCES pg_temp.users(user_id), "
                    + "latitude numeric(9,6), longitude numeric(9,6), UNIQUE (location_id, user_id), "
                    + "CHECK ((latitude IS NULL) = (longitude IS NULL))) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE industry_categories (category_id integer PRIMARY KEY, category_code varchar(10) NOT NULL, category_name varchar(50) NOT NULL, is_active boolean NOT NULL DEFAULT true) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE industry_types (industry_type_id integer PRIMARY KEY, category_id integer NOT NULL REFERENCES pg_temp.industry_categories(category_id), "
                    + "industry_code varchar(10) NOT NULL UNIQUE, industry_name varchar(50) NOT NULL, is_active boolean NOT NULL DEFAULT true) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE properties (property_id integer PRIMARY KEY, listing_status varchar(20) NOT NULL CHECK (listing_status IN ('DRAFT','OPEN','PAUSED','CONTRACTED')), "
                    + "latitude numeric(9,6), longitude numeric(9,6), distance_scale_km numeric(6,3) NOT NULL DEFAULT 10, deleted_at timestamptz) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE property_votes (vote_id serial PRIMARY KEY, property_id integer NOT NULL REFERENCES pg_temp.properties(property_id), "
                    + "resident_user_id integer NOT NULL, location_id integer NOT NULL, distance_km numeric(8,3) NOT NULL CHECK (distance_km >= 0), "
                    + "distance_scale_km numeric(6,3) NOT NULL CHECK (distance_scale_km > 0), visit_frequency varchar(30), request_comment text, "
                    + "created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, withdrawn_at timestamptz, "
                    + "UNIQUE (property_id, resident_user_id), FOREIGN KEY (location_id, resident_user_id) REFERENCES pg_temp.user_locations(location_id, user_id)) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE vote_choices (vote_id integer NOT NULL REFERENCES pg_temp.property_votes(vote_id), "
                    + "preference_rank smallint NOT NULL CHECK (preference_rank BETWEEN 1 AND 3), industry_type_id integer NOT NULL REFERENCES pg_temp.industry_types(industry_type_id), "
                    + "rank_weight numeric(3,2) NOT NULL CHECK (rank_weight IN (1.00, 0.60, 0.30)), PRIMARY KEY (vote_id, preference_rank), UNIQUE (vote_id, industry_type_id)) ON COMMIT PRESERVE ROWS");

            sql.execute("INSERT INTO industry_categories VALUES (1,'001','飲食',true),(2,'002','停止中分類',false)");
            sql.execute("INSERT INTO industry_types VALUES (10,1,'I0101','カフェ',true),(11,1,'I0201','食堂',true),(12,1,'I0301','パン屋',true),(13,1,'I9999','無効な業種',false),(14,2,'I8888','無効な分類の業種',true)");
            sql.execute("INSERT INTO users VALUES (1,'RESIDENT','ACTIVE',NULL),(2,'BUSINESS','ACTIVE',NULL),(3,'RESIDENT','PENDING',NULL),(4,'RESIDENT','BAN',NULL),"
                    + "(5,'RESIDENT','ACTIVE',now()),(6,'RESIDENT','ACTIVE',NULL),(7,'RESIDENT','ACTIVE',NULL)");
            sql.execute("INSERT INTO resident_profiles VALUES (1),(3),(4),(5),(6),(7)");
            // 住民1: 自宅は遠く、勤務先が物件に近い → 近い方が採用される。 住民6: 座標なしの拠点のみ。 住民7: 拠点なし。
            sql.execute("INSERT INTO user_locations VALUES (101,1,36.700000,139.900000),(102,1,36.551000,139.883000),(103,1,NULL,NULL),(106,6,NULL,NULL),(103+100,3,36.55,139.88)");
            sql.execute("INSERT INTO properties VALUES (1,'OPEN',36.555100,139.882800,1.500,NULL),(2,'DRAFT',36.555100,139.882800,10,NULL),(3,'OPEN',NULL,NULL,10,NULL),"
                    + "(4,'OPEN',36.555100,139.882800,10,now()),(5,'OPEN',36.555100,139.882800,10,NULL)");
        }
        Connection borrowed = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("close")) return null;
                    try { return method.invoke(connection, arguments); }
                    catch (InvocationTargetException e) { throw e.getCause(); }
                });
        dao = new VoteDao() { @Override protected Connection openConnection() { return borrowed; } };
    }

    @After
    public void drop() throws Exception {
        if (connection != null) connection.close();
    }

    @Test
    public void savesNewVoteWithNearestLocationAndWeights() throws Exception {
        PropertyVote vote = dao.saveVote(1, 1, List.of("I0101", "I0201", "I0301"), "週1回程度", "  静かなお店  ");
        assertFalse(vote.isWithdrawn());
        assertEquals(102, vote.getLocationId());
        assertEquals("1.500", vote.getDistanceScaleKm().toPlainString());
        // 勤務先(36.551,139.883)から物件(36.5551,139.8828)まで約0.46km。
        assertEquals(0.46, vote.getDistanceKm().doubleValue(), 0.02);
        assertEquals("週1回程度", vote.getVisitFrequency());
        assertEquals("静かなお店", vote.getRequestComment());
        assertEquals(3, vote.getChoices().size());
        assertEquals("I0101", vote.getChoices().get(0).getIndustryCode());
        assertEquals("1.00", vote.getChoices().get(0).getRankWeight().toPlainString());
        assertEquals("0.60", vote.getChoices().get(1).getRankWeight().toPlainString());
        assertEquals("0.30", vote.getChoices().get(2).getRankWeight().toPlainString());
    }

    @Test
    public void acceptsIndustryTypeIdAndIgnoresTrailingBlanks() throws Exception {
        PropertyVote vote = dao.saveVote(1, 1, Arrays.asList("11", "I0101", "", null), null, null);
        assertEquals(2, vote.getChoices().size());
        assertEquals("I0201", vote.getChoices().get(0).getIndustryCode());
        assertNull(vote.getVisitFrequency());
        assertNull(vote.getRequestComment());
    }

    @Test
    public void revoteReusesRowAndReplacesChoices() throws Exception {
        PropertyVote first = dao.saveVote(1, 1, List.of("I0101", "I0201"), "月1回以下", "a");
        PropertyVote second = dao.saveVote(1, 1, List.of("I0301"), "毎日", "b");
        assertEquals(first.getVoteId(), second.getVoteId());
        assertEquals(1, count("SELECT count(*) FROM property_votes"));
        assertEquals(1, count("SELECT count(*) FROM vote_choices"));
        assertEquals("I0301", second.getChoices().get(0).getIndustryCode());
        assertEquals("毎日", second.getVisitFrequency());
    }

    @Test
    public void withdrawThenRevoteReusesRow() throws Exception {
        PropertyVote first = dao.saveVote(1, 1, List.of("I0101"), null, null);
        assertTrue(dao.withdrawVote(1, 1));
        assertFalse("二重取消は何もしない", dao.withdrawVote(1, 1));
        assertTrue(dao.findVote(1, 1).orElseThrow().isWithdrawn());
        PropertyVote again = dao.saveVote(1, 1, List.of("I0201"), null, null);
        assertEquals(first.getVoteId(), again.getVoteId());
        assertFalse(again.isWithdrawn());
        assertFalse(dao.withdrawVote(2, 1));
        assertTrue(dao.findVote(6, 1).isEmpty());
    }

    @Test
    public void badChoicesAreRejectedAndNothingIsSaved() throws Exception {
        assertFails(VoteException.BAD_CHOICES, 1, 1, List.of());
        assertFails(VoteException.BAD_CHOICES, 1, 1, Arrays.asList("", "I0101"));
        assertFails(VoteException.BAD_CHOICES, 1, 1, Arrays.asList("I0101", "", "I0201"));
        assertFails(VoteException.BAD_CHOICES, 1, 1, List.of("I0101", "I0101"));
        assertFails(VoteException.BAD_CHOICES, 1, 1, List.of("I0101", "10")); // コードとIDで同じ業種
        assertFails(VoteException.BAD_CHOICES, 1, 1, List.of("I9999"));
        assertFails(VoteException.BAD_CHOICES, 1, 1, List.of("I8888"));
        assertFails(VoteException.BAD_CHOICES, 1, 1, List.of("ZZZZ"));
        assertFails(VoteException.BAD_CHOICES, 1, 1, List.of("I0101", "I0201", "I0301", "10"));
        assertEquals(0, count("SELECT count(*) FROM property_votes"));
        assertEquals(0, count("SELECT count(*) FROM vote_choices"));
    }

    @Test
    public void failedRevoteKeepsThePreviousVote() throws Exception {
        dao.saveVote(1, 1, List.of("I0101", "I0201"), "毎日", "元");
        assertFails(VoteException.BAD_CHOICES, 1, 1, List.of("I9999"));
        PropertyVote kept = dao.findVote(1, 1).orElseThrow();
        assertEquals("元", kept.getRequestComment());
        assertEquals(2, kept.getChoices().size());
    }

    @Test
    public void propertyProblems() throws Exception {
        assertFails(VoteException.PROPERTY_NOT_FOUND, 1, 99, List.of("I0101"));
        assertFails(VoteException.PROPERTY_NOT_FOUND, 1, 4, List.of("I0101")); // 削除済み
        assertFails(VoteException.PROPERTY_CLOSED, 1, 2, List.of("I0101"));
        assertFails(VoteException.PROPERTY_NO_LOCATION, 1, 3, List.of("I0101"));
    }

    @Test
    public void onlyActiveResidentsCanVote() throws Exception {
        for (int userId : new int[] {2, 3, 4, 5, 99}) {
            assertFails(VoteException.NOT_RESIDENT, userId, 1, List.of("I0101"));
        }
    }

    @Test
    public void residentNeedsALocationWithCoordinates() throws Exception {
        assertFails(VoteException.NO_LOCATION, 6, 1, List.of("I0101")); // 座標なしの拠点のみ
        assertFails(VoteException.NO_LOCATION, 7, 1, List.of("I0101")); // 拠点なし
    }

    @Test
    public void textLengthLimits() throws Exception {
        assertFails(VoteException.INVALID_INPUT, 1, 1, List.of("I0101"), "あ".repeat(31), null);
        assertFails(VoteException.INVALID_INPUT, 1, 1, List.of("I0101"), null, "あ".repeat(VoteDao.COMMENT_MAX_LENGTH + 1));
        dao.saveVote(1, 1, List.of("I0101"), "あ".repeat(30), "あ".repeat(VoteDao.COMMENT_MAX_LENGTH));
    }

    @Test
    public void severalResidentsCanVoteForTheSameProperty() throws Exception {
        dao.saveVote(1, 1, List.of("I0101"), null, null);
        dao.saveVote(1, 5, List.of("I0101"), null, null);
        assertEquals(2, count("SELECT count(*) FROM property_votes"));
    }

    private void assertFails(String code, int userId, int propertyId, List<String> keys) throws Exception {
        assertFails(code, userId, propertyId, keys, null, null);
    }

    private void assertFails(String code, int userId, int propertyId, List<String> keys, String frequency, String comment) throws Exception {
        try {
            dao.saveVote(userId, propertyId, keys, frequency, comment);
            fail("expected " + code + " for " + keys);
        } catch (VoteException e) {
            assertEquals(code, e.getCode());
        }
    }

    private int count(String query) throws Exception {
        try (Statement sql = connection.createStatement(); ResultSet row = sql.executeQuery(query)) {
            row.next();
            return row.getInt(1);
        }
    }
}
