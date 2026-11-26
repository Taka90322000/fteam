package jp.machiriku.servlet.vote;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import jp.machiriku.bean.DemandReport;
import jp.machiriku.bean.IndustryMaster;
import jp.machiriku.bean.PropertyVote;
import jp.machiriku.bean.VoteContext;
import jp.machiriku.dao.DemandDao;
import jp.machiriku.dao.IndustryMasterDao;
import jp.machiriku.dao.VoteContextDao;
import jp.machiriku.dao.VoteDao;

/** VoteContext・Industries・VoteCounts の各Actionのテスト(DBは使わない)。 */
public class NewVoteApisActionTest {

    // ---- VoteContext.action ----------------------------------------------------------------------

    @Test
    public void contextRequiresAnActiveResident() throws Exception {
        ApiTestHarness guest = ApiTestHarness.anonymous("GET");
        new VoteContextAction(new FakeContextDao(), new FakeVoteDao()).execute(guest.request, guest.response);
        assertEquals(401, guest.status);
        ApiTestHarness business = ApiTestHarness.as("GET", "BUSINESS", "ACTIVE");
        new VoteContextAction(new FakeContextDao(), new FakeVoteDao()).execute(business.request, business.response);
        assertEquals(403, business.status);
        assertEquals("NOT_RESIDENT", business.json().get("code").asText());
    }

    @Test
    public void contextRejectsBadInput() throws Exception {
        ApiTestHarness post = ApiTestHarness.as("POST", "RESIDENT", "ACTIVE");
        post.parameters.put("propertyId", "1");
        new VoteContextAction(new FakeContextDao(), new FakeVoteDao()).execute(post.request, post.response);
        assertEquals(405, post.status);
        for (String value : new String[] {null, "", "abc", "0", "-1", "1.5"}) {
            ApiTestHarness h = ApiTestHarness.as("GET", "RESIDENT", "ACTIVE");
            if (value != null) h.parameters.put("propertyId", value);
            new VoteContextAction(new FakeContextDao(), new FakeVoteDao()).execute(h.request, h.response);
            assertEquals("propertyId=" + value, 400, h.status);
            assertEquals("INVALID_INPUT", h.json().get("code").asText());
        }
    }

    @Test
    public void contextReturnsPropertyLocationsAndMyVote() throws Exception {
        FakeContextDao dao = new FakeContextDao();
        FakeVoteDao votes = new FakeVoteDao();
        votes.stored = vote(false);
        ApiTestHarness h = ApiTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "7");
        new VoteContextAction(dao, votes).execute(h.request, h.response);
        assertEquals(200, h.status);
        assertEquals(42, dao.userId);
        assertEquals(7, dao.propertyId);
        assertEquals("no-store", h.responseHeaders.get("Cache-Control"));
        JsonNode body = h.json();
        assertEquals("駅前の空き店舗", body.get("property").get("name").asText());
        assertTrue(body.get("votable").asBoolean());
        assertTrue(body.get("reason").isNull());
        assertEquals(2, body.get("voteCount").asInt());
        assertEquals(102, body.get("defaultLocationId").asInt());
        assertEquals(2, body.get("locations").size());
        assertEquals("自宅", body.get("locations").get(0).get("label").asText());
        assertEquals("勤務先", body.get("locations").get(1).get("label").asText());
        assertTrue(body.get("locations").get(1).get("nearest").asBoolean());
        assertEquals(0.46, body.get("locations").get(1).get("distanceKm").asDouble(), 0.0001);
        assertEquals(9, body.get("vote").get("voteId").asInt());
    }

    @Test
    public void contextHidesWithdrawnVoteAndExplainsReason() throws Exception {
        FakeContextDao dao = new FakeContextDao();
        dao.context = new VoteContext(dao.context.property(), false, "PROPERTY_CLOSED", "この物件は現在投票を受け付けていません。", 0, List.of(), null);
        FakeVoteDao votes = new FakeVoteDao();
        votes.stored = vote(true);
        ApiTestHarness h = ApiTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "7");
        new VoteContextAction(dao, votes).execute(h.request, h.response);
        JsonNode body = h.json();
        assertFalse(body.get("votable").asBoolean());
        assertEquals("PROPERTY_CLOSED", body.get("reason").asText());
        assertTrue("取消済みの投票は未投票として返す", body.get("vote").isNull());
        assertTrue(body.get("defaultLocationId").isNull());
    }

    @Test
    public void contextUnknownPropertyIs404AndDbErrorIs500() throws Exception {
        FakeContextDao missing = new FakeContextDao();
        missing.context = null;
        ApiTestHarness h = ApiTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "7");
        new VoteContextAction(missing, new FakeVoteDao()).execute(h.request, h.response);
        assertEquals(404, h.status);
        assertEquals("PROPERTY_NOT_FOUND", h.json().get("code").asText());

        FakeContextDao broken = new FakeContextDao();
        broken.failure = new SQLException("password=secret leaked", "08006");
        ApiTestHarness e = ApiTestHarness.as("GET", "RESIDENT", "ACTIVE");
        e.parameters.put("propertyId", "7");
        new VoteContextAction(broken, new FakeVoteDao()).execute(e.request, e.response);
        assertEquals(500, e.status);
        assertFalse(e.body.toString().contains("secret"));
    }

    // ---- Industries.action -----------------------------------------------------------------------

    @Test
    public void industriesIsPublicAndReturnsCategoriesWithIndustries() throws Exception {
        ApiTestHarness h = ApiTestHarness.anonymous("GET");
        new IndustriesAction(new FakeIndustryDao()).execute(h.request, h.response);
        assertEquals(200, h.status);
        JsonNode body = h.json();
        assertTrue(body.get("success").asBoolean());
        assertEquals("C01", body.get("categories").get(0).get("categoryCode").asText());
        assertEquals("飲食", body.get("categories").get(0).get("categoryName").asText());
        JsonNode industry = body.get("categories").get(0).get("industries").get(0);
        assertEquals(10, industry.get("industryTypeId").asInt());
        assertEquals("I0101", industry.get("industryCode").asText());
        assertEquals("カフェ", industry.get("industryName").asText());
    }

    @Test
    public void industriesRejectsPostAndHidesDbDetails() throws Exception {
        ApiTestHarness post = ApiTestHarness.anonymous("POST");
        new IndustriesAction(new FakeIndustryDao()).execute(post.request, post.response);
        assertEquals(405, post.status);
        FakeIndustryDao broken = new FakeIndustryDao();
        broken.failure = new SQLException("password=secret leaked", "08006");
        ApiTestHarness h = ApiTestHarness.anonymous("GET");
        new IndustriesAction(broken).execute(h.request, h.response);
        assertEquals(500, h.status);
        assertFalse(h.body.toString().contains("secret"));
    }

    // ---- VoteCounts.action -----------------------------------------------------------------------

    @Test
    public void voteCountsParsesIdsAndReturnsSummaries() throws Exception {
        FakeDemandDao dao = new FakeDemandDao();
        ApiTestHarness h = ApiTestHarness.anonymous("GET");
        h.parameters.put("propertyIds", " 3, 1,3 ,2 ");
        new VoteCountsAction(dao).execute(h.request, h.response);
        assertEquals(200, h.status);
        assertEquals("重複は1つにまとめ、順序は保つ", List.of(3, 1, 2), dao.ids);
        JsonNode body = h.json();
        assertEquals(2, body.get("properties").size());
        JsonNode first = body.get("properties").get(0);
        assertEquals(1, first.get("propertyId").asInt());
        assertEquals(2, first.get("voteCount").asInt());
        assertEquals(2.4, first.get("score").asDouble(), 0.0001);
        assertEquals("I0102", first.get("topIndustry").get("industryCode").asText());
        assertTrue(body.get("properties").get(1).get("topIndustry").isNull());
        assertEquals("no-store", h.responseHeaders.get("Cache-Control"));
    }

    @Test
    public void voteCountsRejectsBadIds() throws Exception {
        String tooMany = java.util.stream.IntStream.rangeClosed(1, 51).mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        String[] bad = {null, "", "  ", "abc", "1,,2", "1,x", "0", "-5", "1.5", tooMany};
        for (String value : bad) {
            FakeDemandDao dao = new FakeDemandDao();
            ApiTestHarness h = ApiTestHarness.anonymous("GET");
            if (value != null) h.parameters.put("propertyIds", value);
            new VoteCountsAction(dao).execute(h.request, h.response);
            assertEquals("propertyIds=" + value, 400, h.status);
            assertEquals("INVALID_INPUT", h.json().get("code").asText());
            assertNull("DBは呼ばない", dao.ids);
        }
        String fifty = tooMany.substring(0, tooMany.lastIndexOf(','));
        FakeDemandDao ok = new FakeDemandDao();
        ApiTestHarness h = ApiTestHarness.anonymous("GET");
        h.parameters.put("propertyIds", fifty);
        new VoteCountsAction(ok).execute(h.request, h.response);
        assertEquals("50件までは受け付ける", 200, h.status);
        assertEquals(50, ok.ids.size());
    }

    @Test
    public void voteCountsRejectsPostAndHidesDbDetails() throws Exception {
        ApiTestHarness post = ApiTestHarness.anonymous("POST");
        post.parameters.put("propertyIds", "1");
        new VoteCountsAction(new FakeDemandDao()).execute(post.request, post.response);
        assertEquals(405, post.status);
        FakeDemandDao broken = new FakeDemandDao();
        broken.failure = new SQLException("password=secret leaked", "08006");
        ApiTestHarness h = ApiTestHarness.anonymous("GET");
        h.parameters.put("propertyIds", "1");
        new VoteCountsAction(broken).execute(h.request, h.response);
        assertEquals(500, h.status);
        assertFalse(h.body.toString().contains("secret"));
    }

    // ---- 偽のDAO ---------------------------------------------------------------------------------

    private static PropertyVote vote(boolean withdrawn) {
        PropertyVote vote = new PropertyVote();
        vote.setVoteId(9);
        vote.setPropertyId(7);
        vote.setResidentUserId(42);
        vote.setDistanceKm(new BigDecimal("0.460"));
        vote.setDistanceScaleKm(new BigDecimal("1.500"));
        if (withdrawn) vote.setWithdrawnAt(java.time.OffsetDateTime.now());
        return vote;
    }

    private static final class FakeContextDao extends VoteContextDao {
        VoteContext context = new VoteContext(
                new VoteContext.PropertyInfo(7, "駅前の空き店舗", "栃木県", "宇都宮市", "宇都宮市1-1", "OPEN", true, new BigDecimal("1.500")),
                true, null, null, 2,
                List.of(new VoteContext.LocationOption(101, "HOME", "栃木県", "日光市", "日光1-1", true, new BigDecimal("16.000"), false),
                        new VoteContext.LocationOption(102, "WORK", "栃木県", "宇都宮市", "勤務先1-1", true, new BigDecimal("0.460"), true)),
                102);
        SQLException failure;
        int userId, propertyId;

        @Override
        public Optional<VoteContext> load(int userId, int propertyId) throws SQLException {
            if (failure != null) throw failure;
            this.userId = userId; this.propertyId = propertyId;
            return Optional.ofNullable(context);
        }
    }

    private static final class FakeVoteDao extends VoteDao {
        PropertyVote stored;

        @Override
        public Optional<PropertyVote> findVote(int userId, int propertyId) { return Optional.ofNullable(stored); }
    }

    private static final class FakeIndustryDao extends IndustryMasterDao {
        SQLException failure;

        @Override
        public IndustryMaster findActive() throws SQLException {
            if (failure != null) throw failure;
            return new IndustryMaster(List.of(new IndustryMaster.Category(1, "C01", "飲食",
                    List.of(new IndustryMaster.Industry(10, "I0101", "カフェ"), new IndustryMaster.Industry(11, "I0102", "レストラン")))));
        }
    }

    private static final class FakeDemandDao extends DemandDao {
        SQLException failure;
        List<Integer> ids;

        @Override
        public List<DemandReport.PropertySummary> summaries(List<Integer> propertyIds) throws SQLException {
            if (failure != null) throw failure;
            this.ids = propertyIds;
            return List.of(new DemandReport.PropertySummary(1, 2, new BigDecimal("2.400"), "I0102", "レストラン"),
                    new DemandReport.PropertySummary(2, 0, new BigDecimal("0.000"), null, null));
        }
    }
}
