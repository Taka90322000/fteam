package jp.machiriku.servlet.vote;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import jp.machiriku.bean.DemandReport;
import jp.machiriku.dao.DemandDao;

/** DemandActionの入力検証・権限・JSON応答のテスト(DBは使わない)。 */
public class DemandActionTest {

    @Test
    public void postIsNotAllowed() throws Exception {
        ApiTestHarness h = ApiTestHarness.as("POST", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "1");
        new DemandAction(new FakeDao()).execute(h.request, h.response);
        assertEquals(405, h.status);
        assertEquals("METHOD_NOT_ALLOWED", h.json().get("code").asText());
    }

    @Test
    public void invalidParametersAre400() throws Exception {
        String[][] bad = {{"propertyId", "abc"}, {"propertyId", "0"}, {"propertyId", "-3"}, {"propertyId", "1.5"},
                {"limit", "0"}, {"limit", "51"}, {"limit", "x"}, {"commentLimit", "-1"}, {"commentLimit", "31"},
                {"topProperties", "21"}, {"municipality", "あ".repeat(51)}, {"prefecture", "あ".repeat(11)}};
        for (String[] pair : bad) {
            ApiTestHarness h = ApiTestHarness.anonymous("GET");
            h.parameters.put(pair[0], pair[1]);
            FakeDao dao = new FakeDao();
            new DemandAction(dao).execute(h.request, h.response);
            assertEquals(pair[0] + "=" + pair[1], 400, h.status);
            assertEquals("INVALID_INPUT", h.json().get("code").asText());
            assertEquals("DBは呼ばない", 0, dao.calls);
        }
    }

    @Test
    public void propertyScopeReturnsRankingAndHidesCommentsFromGuests() throws Exception {
        FakeDao dao = new FakeDao();
        ApiTestHarness guest = ApiTestHarness.anonymous("GET");
        guest.parameters.put("propertyId", " 7 ");
        new DemandAction(dao).execute(guest.request, guest.response);
        assertEquals(200, guest.status);
        assertEquals(7, dao.propertyId);
        assertEquals("ログインなしはコメント0件", 0, dao.commentLimit);
        assertEquals(10, dao.industryLimit);
        JsonNode body = guest.json();
        assertTrue(body.get("success").asBoolean());
        assertEquals("PROPERTY", body.get("scope").asText());
        assertFalse(body.get("commentsVisible").asBoolean());
        assertEquals("no-store", guest.responseHeaders.get("Cache-Control"));
        assertEquals("駅前の空き店舗", body.get("property").get("name").asText());
        assertEquals(2, body.get("voteCount").asInt());
        assertEquals(1.3, body.get("industries").get(0).get("score").asDouble(), 0.0001);
        assertEquals(54.2, body.get("industries").get(0).get("sharePercent").asDouble(), 0.0001);
        assertEquals("I0102", body.get("industries").get(0).get("industryCode").asText());
        assertEquals("飲食", body.get("categories").get(0).get("categoryName").asText());
        assertEquals(2, body.get("categories").get(0).get("residentCount").asInt());
        assertFalse("物件別にtopPropertiesは付けない", body.has("topProperties"));

        ApiTestHarness member = ApiTestHarness.as("GET", "BUSINESS", "ACTIVE");
        member.parameters.put("propertyId", "7");
        member.parameters.put("commentLimit", "3");
        member.parameters.put("limit", "5");
        new DemandAction(dao).execute(member.request, member.response);
        assertEquals(3, dao.commentLimit);
        assertEquals(5, dao.industryLimit);
        assertTrue(member.json().get("commentsVisible").asBoolean());
        assertEquals("静かなお店", member.json().get("comments").get(0).get("comment").asText());
        assertFalse("投稿者を特定する情報は含めない", member.body.toString().contains("userId"));
        assertFalse(member.body.toString().contains("residentUserId"));
    }

    @Test
    public void pendingOrWithdrawnMembersAreTreatedAsGuests() throws Exception {
        FakeDao dao = new FakeDao();
        ApiTestHarness pending = ApiTestHarness.as("GET", "RESIDENT", "PENDING");
        pending.parameters.put("propertyId", "7");
        new DemandAction(dao).execute(pending.request, pending.response);
        assertEquals(200, pending.status);
        assertFalse(pending.json().get("commentsVisible").asBoolean());
        assertEquals(0, dao.commentLimit);
    }

    @Test
    public void unknownPropertyIs404() throws Exception {
        FakeDao dao = new FakeDao();
        dao.report = null;
        ApiTestHarness h = ApiTestHarness.anonymous("GET");
        h.parameters.put("propertyId", "999");
        new DemandAction(dao).execute(h.request, h.response);
        assertEquals(404, h.status);
        assertEquals("PROPERTY_NOT_FOUND", h.json().get("code").asText());
    }

    @Test
    public void areaScopeUsesFiltersAndReturnsTopProperties() throws Exception {
        FakeDao dao = new FakeDao();
        ApiTestHarness h = ApiTestHarness.anonymous("GET");
        h.parameters.put("municipality", " 宇都宮市 ");
        h.parameters.put("prefecture", "栃木県");
        h.parameters.put("topProperties", "3");
        new DemandAction(dao).execute(h.request, h.response);
        assertEquals(200, h.status);
        assertEquals("宇都宮市", dao.municipality);
        assertEquals("栃木県", dao.prefecture);
        assertEquals(3, dao.topProperties);
        JsonNode body = h.json();
        assertEquals("AREA", body.get("scope").asText());
        assertEquals("宇都宮市", body.get("area").get("municipality").asText());
        assertEquals(1, body.get("topProperties").get(0).get("propertyId").asInt());
        assertEquals("カフェ", body.get("topProperties").get(0).get("topIndustryName").asText());
        assertFalse(body.has("comments"));
        assertFalse(body.has("property"));
    }

    @Test
    public void noFilterMeansWholeCityAndBlankFiltersAreIgnored() throws Exception {
        FakeDao dao = new FakeDao();
        ApiTestHarness h = ApiTestHarness.anonymous("GET");
        h.parameters.put("municipality", "  ");
        h.parameters.put("propertyId", "");
        new DemandAction(dao).execute(h.request, h.response);
        assertEquals(200, h.status);
        assertNull(dao.municipality);
        assertNull(dao.prefecture);
        assertEquals(5, dao.topProperties);
        assertEquals("AREA", h.json().get("scope").asText());
    }

    @Test
    public void emptyReportKeepsNullAverage() throws Exception {
        FakeDao dao = new FakeDao();
        dao.area = new DemandReport(DemandReport.SCOPE_AREA, null, null, null, null, 0, 0, 0, null, new BigDecimal("0.000"),
                List.of(), List.of(), List.of(), List.of(), List.of());
        ApiTestHarness h = ApiTestHarness.anonymous("GET");
        new DemandAction(dao).execute(h.request, h.response);
        JsonNode body = h.json();
        assertEquals(0, body.get("voteCount").asInt());
        assertTrue(body.get("averageDistanceKm").isNull());
        assertEquals(0, body.get("industries").size());
    }

    @Test
    public void databaseErrorIs500WithoutDetails() throws Exception {
        FakeDao dao = new FakeDao();
        dao.failure = new SQLException("password=secret leaked", "08006");
        ApiTestHarness h = ApiTestHarness.anonymous("GET");
        new DemandAction(dao).execute(h.request, h.response);
        assertEquals(500, h.status);
        assertEquals("SERVER_ERROR", h.json().get("code").asText());
        assertFalse(h.body.toString().contains("secret"));
    }

    private static DemandReport propertyReport() {
        return new DemandReport(DemandReport.SCOPE_PROPERTY, 7, "駅前の空き店舗", "栃木県", "宇都宮市", 1, 2, 2,
                new BigDecimal("5.000"), new BigDecimal("2.400"),
                List.of(new DemandReport.IndustryDemand(1, 10, "I0102", "カフェ", new BigDecimal("1.300"), new BigDecimal("54.2"), 2, 1, 1, 0)),
                List.of(new DemandReport.CategoryDemand(1, "C01", "飲食", new BigDecimal("2.400"), new BigDecimal("100.0"), 2)),
                List.of(new DemandReport.FrequencyCount("週1回程度", 1)),
                List.of(new DemandReport.DemandComment("静かなお店", "週1回程度", OffsetDateTime.parse("2026-11-10T10:00:00Z"))), List.of());
    }

    private static DemandReport areaReport() {
        return new DemandReport(DemandReport.SCOPE_AREA, null, null, "栃木県", "宇都宮市", 2, 3, 2,
                new BigDecimal("3.000"), new BigDecimal("3.400"),
                List.of(new DemandReport.IndustryDemand(1, 10, "I0102", "カフェ", new BigDecimal("1.300"), new BigDecimal("38.2"), 2, 1, 1, 0)),
                List.of(), List.of(), List.of(),
                List.of(new DemandReport.TopProperty(1, 1, "駅前の空き店舗", "宇都宮市", 2, new BigDecimal("2.400"), "カフェ")));
    }

    private static final class FakeDao extends DemandDao {
        DemandReport report = propertyReport();
        DemandReport area = areaReport();
        SQLException failure;
        int calls, propertyId, industryLimit, commentLimit, topProperties;
        String prefecture, municipality;

        @Override
        public Optional<DemandReport> propertyDemand(int propertyId, int industryLimit, int commentLimit) throws SQLException {
            calls++;
            if (failure != null) throw failure;
            this.propertyId = propertyId; this.industryLimit = industryLimit; this.commentLimit = commentLimit;
            return Optional.ofNullable(report);
        }

        @Override
        public DemandReport areaDemand(String prefecture, String municipality, int industryLimit, int topPropertyLimit) throws SQLException {
            calls++;
            if (failure != null) throw failure;
            this.prefecture = prefecture; this.municipality = municipality; this.industryLimit = industryLimit; this.topProperties = topPropertyLimit;
            return area;
        }
    }
}
