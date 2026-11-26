package jp.machiriku.servlet.vote;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import jp.machiriku.bean.PropertyVote;
import jp.machiriku.bean.VoteChoice;
import jp.machiriku.bean.VoteHistoryPage;
import jp.machiriku.dao.VoteHistoryDao;

/** VoteHistoryActionの本人確認・入力検証・JSON応答のテスト(DBは使わない)。 */
public class VoteHistoryActionTest {

    @Test
    public void loggedOutIs401() throws Exception {
        ApiTestHarness h = ApiTestHarness.anonymous("GET");
        new VoteHistoryAction(new FakeDao()).execute(h.request, h.response);
        assertEquals(401, h.status);
        assertEquals("UNAUTHENTICATED", h.json().get("code").asText());
    }

    @Test
    public void onlyActiveResidentsCanSeeHistory() throws Exception {
        for (String role : List.of("BUSINESS", "OWNER", "ADMIN")) {
            ApiTestHarness h = ApiTestHarness.as("GET", role, "ACTIVE");
            FakeDao dao = new FakeDao();
            new VoteHistoryAction(dao).execute(h.request, h.response);
            assertEquals(role, 403, h.status);
            assertEquals("NOT_RESIDENT", h.json().get("code").asText());
            assertEquals(0, dao.calls);
        }
        ApiTestHarness pending = ApiTestHarness.as("GET", "RESIDENT", "PENDING");
        new VoteHistoryAction(new FakeDao()).execute(pending.request, pending.response);
        assertEquals("SETUP_REQUIRED", pending.json().get("code").asText());
    }

    @Test
    public void postIsNotAllowed() throws Exception {
        ApiTestHarness h = ApiTestHarness.as("POST", "RESIDENT", "ACTIVE");
        new VoteHistoryAction(new FakeDao()).execute(h.request, h.response);
        assertEquals(405, h.status);
    }

    @Test
    public void invalidParametersAre400() throws Exception {
        String[][] bad = {{"status", "everything"}, {"limit", "0"}, {"limit", "101"}, {"limit", "x"}, {"offset", "-1"}, {"offset", "1.5"}};
        for (String[] pair : bad) {
            ApiTestHarness h = ApiTestHarness.as("GET", "RESIDENT", "ACTIVE");
            h.parameters.put(pair[0], pair[1]);
            FakeDao dao = new FakeDao();
            new VoteHistoryAction(dao).execute(h.request, h.response);
            assertEquals(pair[0] + "=" + pair[1], 400, h.status);
            assertEquals("INVALID_INPUT", h.json().get("code").asText());
            assertEquals(0, dao.calls);
        }
    }

    @Test
    public void defaultsAreAllStatusLimit20Offset0() throws Exception {
        FakeDao dao = new FakeDao();
        ApiTestHarness h = ApiTestHarness.as("GET", "RESIDENT", "ACTIVE");
        new VoteHistoryAction(dao).execute(h.request, h.response);
        assertEquals(200, h.status);
        assertEquals(42, dao.userId);
        assertEquals(VoteHistoryDao.Status.ALL, dao.status);
        assertEquals(20, dao.limit);
        assertEquals(0, dao.offset);
    }

    @Test
    public void passesFiltersAndAcceptsAnyCase() throws Exception {
        FakeDao dao = new FakeDao();
        ApiTestHarness h = ApiTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("status", " Withdrawn ");
        h.parameters.put("limit", "5");
        h.parameters.put("offset", "10");
        new VoteHistoryAction(dao).execute(h.request, h.response);
        assertEquals(VoteHistoryDao.Status.WITHDRAWN, dao.status);
        assertEquals(5, dao.limit);
        assertEquals(10, dao.offset);
    }

    @Test
    public void respondsWithVotesAndPropertySummary() throws Exception {
        FakeDao dao = new FakeDao();
        ApiTestHarness h = ApiTestHarness.as("GET", "RESIDENT", "ACTIVE");
        new VoteHistoryAction(dao).execute(h.request, h.response);
        JsonNode body = h.json();
        assertTrue(body.get("success").asBoolean());
        assertEquals(3, body.get("total").asInt());
        assertTrue("あと1件ある", body.get("hasMore").asBoolean());
        JsonNode first = body.get("votes").get(0);
        assertEquals(9, first.get("voteId").asInt());
        assertEquals(5, first.get("propertyId").asInt());
        assertFalse(first.get("withdrawn").asBoolean());
        assertTrue(first.get("withdrawnAt").isNull());
        assertEquals("I0102", first.get("choices").get(0).get("industryCode").asText());
        assertEquals(1.263, first.get("score").asDouble(), 0.0005);
        assertEquals("駅前の空き店舗", first.get("property").get("name").asText());
        assertEquals("OPEN", first.get("property").get("listingStatus").asText());
        assertFalse(first.get("property").get("deleted").asBoolean());
        JsonNode second = body.get("votes").get(1);
        assertTrue(second.get("withdrawn").asBoolean());
        assertFalse(second.get("withdrawnAt").isNull());
        assertTrue(second.get("property").get("deleted").asBoolean());
        assertEquals("no-store", h.responseHeaders.get("Cache-Control"));
    }

    @Test
    public void databaseErrorIs500WithoutDetails() throws Exception {
        FakeDao dao = new FakeDao();
        dao.failure = new SQLException("password=secret leaked", "08006");
        ApiTestHarness h = ApiTestHarness.as("GET", "RESIDENT", "ACTIVE");
        new VoteHistoryAction(dao).execute(h.request, h.response);
        assertEquals(500, h.status);
        assertEquals("SERVER_ERROR", h.json().get("code").asText());
        assertFalse(h.body.toString().contains("secret"));
    }

    private static PropertyVote vote(int voteId, int propertyId, boolean withdrawn) {
        PropertyVote vote = new PropertyVote();
        vote.setVoteId(voteId);
        vote.setPropertyId(propertyId);
        vote.setResidentUserId(42);
        vote.setDistanceKm(new BigDecimal("0.400"));
        vote.setDistanceScaleKm(new BigDecimal("1.500"));
        vote.setUpdatedAt(OffsetDateTime.parse("2026-11-10T10:00:00Z"));
        if (withdrawn) vote.setWithdrawnAt(OffsetDateTime.parse("2026-11-11T10:00:00Z"));
        for (int rank = 1; rank <= 2; rank++) {
            VoteChoice choice = new VoteChoice();
            choice.setPreferenceRank(rank);
            choice.setIndustryCode(rank == 1 ? "I0102" : "I0104");
            choice.setIndustryName(rank == 1 ? "カフェ" : "食堂");
            choice.setRankWeight(rank == 1 ? new BigDecimal("1.00") : new BigDecimal("0.60"));
            vote.getChoices().add(choice);
        }
        return vote;
    }

    private static final class FakeDao extends VoteHistoryDao {
        SQLException failure;
        int calls, userId, limit, offset;
        Status status;

        @Override
        public VoteHistoryPage list(int userId, Status status, int limit, int offset) throws SQLException {
            calls++;
            if (failure != null) throw failure;
            this.userId = userId; this.status = status; this.limit = limit; this.offset = offset;
            return new VoteHistoryPage(3, limit, offset, List.of(
                    new VoteHistoryPage.Item(vote(9, 5, false), "駅前の空き店舗", "栃木県", "宇都宮市", "宇都宮市1-1", "OPEN", false,
                            OffsetDateTime.parse("2026-11-09T10:00:00Z")),
                    new VoteHistoryPage.Item(vote(8, 6, true), "削除された物件", "栃木県", "宇都宮市", "宇都宮市4-4", "OPEN", true,
                            OffsetDateTime.parse("2026-11-08T10:00:00Z"))));
        }
    }
}
