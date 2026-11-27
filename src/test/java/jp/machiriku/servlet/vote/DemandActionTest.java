package jp.machiriku.servlet.vote;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import jp.machiriku.bean.DemandReport;
import jp.machiriku.dao.DemandDao;

/** DemandAction(需要集計の表示)の入力検証・権限・JSPへのforwardのテスト(DBは使わない)。 */
public class DemandActionTest {

    @Test
    public void postIsNotAllowed() throws Exception {
        PageTestHarness h = PageTestHarness.as("POST", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "1");
        new DemandAction(new FakeDao()).execute(h.request, h.response);
        assertEquals(Integer.valueOf(405), h.errorCode);
    }

    @Test
    public void invalidParametersShowThePageWithAMessageAndSkipTheDatabase() throws Exception {
        String[][] bad = {{"propertyId", "abc"}, {"propertyId", "0"}, {"propertyId", "-3"}, {"propertyId", "1.5"},
                {"limit", "0"}, {"limit", "51"}, {"limit", "x"}, {"commentLimit", "-1"}, {"commentLimit", "31"},
                {"topProperties", "21"}, {"municipality", "あ".repeat(51)}, {"prefecture", "あ".repeat(11)}};
        for (String[] pair : bad) {
            PageTestHarness h = PageTestHarness.anonymous("GET");
            h.parameters.put(pair[0], pair[1]);
            FakeDao dao = new FakeDao();
            new DemandAction(dao).execute(h.request, h.response);
            assertEquals(pair[0] + "=" + pair[1], 400, h.status);
            assertEquals(VoteViews.DEMAND, h.forwardedTo);
            assertNotNull(h.attributes.get("errorMessage"));
            assertNull("集計なし", h.attributes.get("report"));
            assertEquals("DBは呼ばない", 0, dao.calls);
        }
    }

    @Test
    public void propertyScopeHidesCommentsFromGuests() throws Exception {
        FakeDao dao = new FakeDao();
        PageTestHarness guest = PageTestHarness.anonymous("GET");
        guest.parameters.put("propertyId", " 7 ");
        new DemandAction(dao).execute(guest.request, guest.response);

        assertEquals(VoteViews.DEMAND, guest.forwardedTo);
        assertEquals(200, guest.status);
        assertEquals(7, dao.propertyId);
        assertEquals("ログインなしはコメント0件", 0, dao.commentLimit);
        assertEquals(10, dao.industryLimit);
        assertEquals(Boolean.FALSE, guest.attributes.get("commentsVisible"));
        assertTrue(guest.attributes.get("report") instanceof DemandReport);
        assertEquals("no-store", guest.responseHeaders.get("Cache-Control"));
    }

    @Test
    public void membersSeeComments() throws Exception {
        FakeDao dao = new FakeDao();
        PageTestHarness member = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        member.parameters.put("propertyId", "7");
        member.parameters.put("commentLimit", "5");
        member.parameters.put("limit", "3");
        new DemandAction(dao).execute(member.request, member.response);
        assertEquals(5, dao.commentLimit);
        assertEquals(3, dao.industryLimit);
        assertEquals(Boolean.TRUE, member.attributes.get("commentsVisible"));

        // 初期設定が済んでいない会員には、コメントを見せない。
        FakeDao pendingDao = new FakeDao();
        PageTestHarness pending = PageTestHarness.as("GET", "RESIDENT", "PENDING");
        pending.parameters.put("propertyId", "7");
        new DemandAction(pendingDao).execute(pending.request, pending.response);
        assertEquals(0, pendingDao.commentLimit);
    }

    @Test
    public void unknownPropertyIs404() throws Exception {
        FakeDao dao = new FakeDao();
        dao.found = false;
        PageTestHarness h = PageTestHarness.anonymous("GET");
        h.parameters.put("propertyId", "99");
        new DemandAction(dao).execute(h.request, h.response);
        assertEquals(Integer.valueOf(404), h.errorCode);
        assertFalse(h.rendered());
    }

    @Test
    public void areaScopeUsesPrefectureAndMunicipality() throws Exception {
        FakeDao dao = new FakeDao();
        PageTestHarness h = PageTestHarness.anonymous("GET");
        h.parameters.put("prefecture", " 栃木県 ");
        h.parameters.put("municipality", "宇都宮市");
        h.parameters.put("topProperties", "8");
        new DemandAction(dao).execute(h.request, h.response);

        assertEquals("栃木県", dao.prefecture);
        assertEquals("宇都宮市", dao.municipality);
        assertEquals(8, dao.topProperties);
        assertEquals("栃木県", h.attributes.get("prefecture"));
        assertEquals("宇都宮市", h.attributes.get("municipality"));
        assertEquals(DemandReport.SCOPE_AREA, ((DemandReport) h.attributes.get("report")).scope());
    }

    @Test
    public void noParametersMeansWholeArea() throws Exception {
        FakeDao dao = new FakeDao();
        PageTestHarness h = PageTestHarness.anonymous("GET");
        new DemandAction(dao).execute(h.request, h.response);
        assertNull(dao.prefecture);
        assertNull(dao.municipality);
        assertEquals(5, dao.topProperties);
        assertEquals(VoteViews.DEMAND, h.forwardedTo);
    }

    @Test
    public void databaseErrorIs500WithoutDetails() throws Exception {
        FakeDao dao = new FakeDao();
        dao.failure = new SQLException("password=secret leaked", "08006");
        PageTestHarness h = PageTestHarness.anonymous("GET");
        new DemandAction(dao).execute(h.request, h.response);
        assertEquals(Integer.valueOf(500), h.errorCode);
        assertFalse(h.rendered());
    }

    private static DemandReport report(String scope, Integer propertyId) {
        return new DemandReport(scope, propertyId, propertyId == null ? null : "駅前の空き店舗", "栃木県", "宇都宮市",
                1, 2, 2, new BigDecimal("0.400"), new BigDecimal("1.600"),
                List.of(new DemandReport.IndustryDemand(1, 1, "I0101", "カフェ・喫茶店", new BigDecimal("1.600"), new BigDecimal("100.0"), 2, 2, 0, 0)),
                List.of(), List.of(), List.of(), List.of());
    }

    private static final class FakeDao extends DemandDao {
        boolean found = true;
        SQLException failure;
        int calls, propertyId, industryLimit, commentLimit, topProperties;
        String prefecture, municipality;

        @Override
        public Optional<DemandReport> propertyDemand(int propertyId, int industryLimit, int commentLimit) throws SQLException {
            calls++;
            if (failure != null) throw failure;
            this.propertyId = propertyId; this.industryLimit = industryLimit; this.commentLimit = commentLimit;
            return found ? Optional.of(report(DemandReport.SCOPE_PROPERTY, propertyId)) : Optional.empty();
        }

        @Override
        public DemandReport areaDemand(String prefecture, String municipality, int industryLimit, int topPropertyLimit) throws SQLException {
            calls++;
            if (failure != null) throw failure;
            this.prefecture = prefecture; this.municipality = municipality; this.industryLimit = industryLimit; this.topProperties = topPropertyLimit;
            return report(DemandReport.SCOPE_AREA, null);
        }
    }
}
