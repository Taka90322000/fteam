package jp.machiriku.dao;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.Statement;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import jp.machiriku.bean.VoteContext;

/** 投票画面の初期表示情報を、一時テーブルで確認する。MACHIRIKU_RUN_DB_TESTS=true のときだけ実行する。 */
public class VoteContextDaoTest {
    private Connection connection;
    private VoteContextDao dao;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("true".equalsIgnoreCase(System.getenv("MACHIRIKU_RUN_DB_TESTS")));
        connection = Dao.getConnection();
        try (Statement sql = connection.createStatement()) {
            sql.execute("CREATE TEMP TABLE user_locations (location_id integer PRIMARY KEY, user_id integer NOT NULL, location_kind varchar(8) NOT NULL, "
                    + "prefecture varchar(10) NOT NULL, municipality varchar(50) NOT NULL, address_line varchar(200), latitude numeric(9,6), longitude numeric(9,6)) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE properties (property_id integer PRIMARY KEY, property_name varchar(150) NOT NULL, prefecture varchar(10) NOT NULL, "
                    + "municipality varchar(50) NOT NULL, address_line varchar(200) NOT NULL, listing_status varchar(16) NOT NULL, latitude numeric(9,6), "
                    + "longitude numeric(9,6), distance_scale_km numeric(8,3) NOT NULL DEFAULT 10, deleted_at timestamptz, admin_hidden_at timestamptz) ON COMMIT PRESERVE ROWS");
            sql.execute("CREATE TEMP TABLE property_votes (vote_id integer PRIMARY KEY, property_id integer NOT NULL, resident_user_id integer NOT NULL, withdrawn_at timestamptz) ON COMMIT PRESERVE ROWS");
            sql.execute("INSERT INTO properties VALUES "
                    + "(1,'駅前の空き店舗','栃木県','宇都宮市','宇都宮市1-1','OPEN',36.555100,139.882800,1.500,NULL,NULL),"
                    + "(2,'下書き','栃木県','宇都宮市','宇都宮市2-2','DRAFT',36.555100,139.882800,10,NULL,NULL),"
                    + "(3,'座標なし','栃木県','宇都宮市','宇都宮市3-3','OPEN',NULL,NULL,10,NULL,NULL),"
                    + "(4,'削除済み','栃木県','宇都宮市','宇都宮市4-4','OPEN',36.555100,139.882800,10,now(),NULL),"
                    + "(5,'非表示','栃木県','宇都宮市','宇都宮市5-5','OPEN',36.555100,139.882800,10,NULL,now())");
            // 住民1: 自宅は遠く、勤務先が近い。学校は座標なし。 住民2: 拠点なし。 住民3: 座標なしの自宅だけ。
            sql.execute("INSERT INTO user_locations VALUES "
                    + "(101,1,'HOME','栃木県','日光市','日光1-1',36.700000,139.900000),"
                    + "(103,1,'SCHOOL','栃木県','宇都宮市','学校前',NULL,NULL),"
                    + "(102,1,'WORK','栃木県','宇都宮市','勤務先1-1',36.551000,139.883000),"
                    + "(301,3,'HOME','栃木県','宇都宮市',NULL,NULL,NULL)");
            sql.execute("INSERT INTO property_votes VALUES (1,1,1,NULL),(2,1,2,NULL),(3,1,3,now())");
        }
        Connection borrowed = DemandTestSchema.borrowed(connection);
        dao = new VoteContextDao() { @Override protected Connection openConnection() { return borrowed; } };
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null) connection.close();
    }

    @Test
    public void votableContextListsLocationsWithDistanceAndNearest() throws Exception {
        VoteContext context = dao.load(1, 1).orElseThrow();
        assertEquals("駅前の空き店舗", context.property().name());
        assertEquals("1.500", context.property().distanceScaleKm().toPlainString());
        assertTrue(context.votable());
        assertNull(context.reason());
        assertEquals("取消済みを除いて2票", 2, context.voteCount());
        assertEquals(Integer.valueOf(102), context.defaultLocationId());
        assertEquals("自宅→勤務先→学校の順", java.util.List.of(101, 102, 103),
                context.locations().stream().map(VoteContext.LocationOption::locationId).toList());
        VoteContext.LocationOption home = context.locations().get(0);
        assertTrue(home.hasCoordinates());
        assertTrue(home.distanceKm().doubleValue() > 10);
        assertFalse(home.nearest());
        VoteContext.LocationOption work = context.locations().get(1);
        assertEquals(0.46, work.distanceKm().doubleValue(), 0.02);
        assertTrue(work.nearest());
        VoteContext.LocationOption school = context.locations().get(2);
        assertFalse(school.hasCoordinates());
        assertNull(school.distanceKm());
        assertFalse(school.nearest());
    }

    @Test
    public void explainsWhyVotingIsNotPossible() throws Exception {
        assertEquals(VoteException.PROPERTY_CLOSED, dao.load(1, 2).orElseThrow().reason());
        assertFalse(dao.load(1, 2).orElseThrow().votable());
        VoteContext noCoordinates = dao.load(1, 3).orElseThrow();
        assertEquals(VoteException.PROPERTY_NO_LOCATION, noCoordinates.reason());
        assertNull("物件に座標がなければ距離は出さない", noCoordinates.locations().get(0).distanceKm());
        VoteContext noLocation = dao.load(2, 1).orElseThrow();
        assertEquals(VoteException.NO_LOCATION, noLocation.reason());
        assertTrue(noLocation.locations().isEmpty());
        assertNull(noLocation.defaultLocationId());
        assertNotNull(noLocation.reasonMessage());
        assertEquals("座標のない拠点しかなければ投票できない", VoteException.NO_LOCATION, dao.load(3, 1).orElseThrow().reason());
    }

    @Test
    public void missingDeletedAndHiddenPropertiesAreEmpty() throws Exception {
        assertTrue(dao.load(1, 999).isEmpty());
        assertTrue(dao.load(1, 4).isEmpty());
        assertTrue(dao.load(1, 5).isEmpty());
    }

    @Test
    public void locationsOfOtherResidentsAreNeverReturned() throws Exception {
        assertTrue(dao.load(2, 1).orElseThrow().locations().isEmpty());
        assertEquals(1, dao.load(3, 1).orElseThrow().locations().size());
    }
}
