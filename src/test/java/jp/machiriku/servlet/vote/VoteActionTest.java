package jp.machiriku.servlet.vote;

import static org.junit.Assert.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.PropertyVote;
import jp.machiriku.bean.User;
import jp.machiriku.bean.VoteChoice;
import jp.machiriku.dao.VoteDao;
import jp.machiriku.dao.VoteException;

/** VoteActionの本人確認・CSRF対策・入力検証・JSON応答のテスト(DBは使わない)。 */
public class VoteActionTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    public void loggedOutIs401() throws Exception {
        Harness h = new Harness("GET");
        h.hasSession = false;
        new VoteAction(new FakeDao()).execute(h.request, h.response);
        assertEquals(401, h.status);
        assertEquals("UNAUTHENTICATED", h.json().get("code").asText());
        assertFalse(h.json().get("success").asBoolean());
    }

    @Test
    public void pendingUserIsAskedToFinishSetup() throws Exception {
        Harness h = Harness.as("GET", "RESIDENT", "PENDING");
        new VoteAction(new FakeDao()).execute(h.request, h.response);
        assertEquals(403, h.status);
        assertEquals("SETUP_REQUIRED", h.json().get("code").asText());
    }

    @Test
    public void businessAndOwnerCannotVote() throws Exception {
        for (String role : List.of("BUSINESS", "OWNER")) {
            Harness h = Harness.as("GET", role, "ACTIVE");
            h.parameters.put("propertyId", "1");
            new VoteAction(new FakeDao()).execute(h.request, h.response);
            assertEquals(403, h.status);
            assertEquals("NOT_RESIDENT", h.json().get("code").asText());
        }
    }

    @Test
    public void withdrawnMemberIsRejected() throws Exception {
        Harness h = Harness.as("GET", "RESIDENT", "ACTIVE");
        ((User) h.sessionValues.get("user")).setWithdrawnAt(java.time.OffsetDateTime.now());
        new VoteAction(new FakeDao()).execute(h.request, h.response);
        assertEquals(403, h.status);
    }

    @Test
    public void postWithoutAjaxHeaderIsRejectedAsCsrf() throws Exception {
        Harness h = Harness.as("POST", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "1");
        h.parameters.put("rank1", "I0101");
        FakeDao dao = new FakeDao();
        new VoteAction(dao).execute(h.request, h.response);
        assertEquals(403, h.status);
        assertEquals("CSRF", h.json().get("code").asText());
        assertEquals(0, dao.saves);
    }

    @Test
    public void invalidPropertyIdIs400() throws Exception {
        for (String value : new String[] {null, "", "abc", "1.5"}) {
            Harness h = Harness.as("GET", "RESIDENT", "ACTIVE");
            if (value != null) h.parameters.put("propertyId", value);
            new VoteAction(new FakeDao()).execute(h.request, h.response);
            assertEquals("propertyId=" + value, 400, h.status);
            assertEquals("INVALID_INPUT", h.json().get("code").asText());
        }
    }

    @Test
    public void putIsNotAllowed() throws Exception {
        Harness h = Harness.as("PUT", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "1");
        new VoteAction(new FakeDao()).execute(h.request, h.response);
        assertEquals(405, h.status);
    }

    @Test
    public void getReturnsNullWhenNotVotedOrWithdrawn() throws Exception {
        Harness h = Harness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "5");
        new VoteAction(new FakeDao()).execute(h.request, h.response);
        assertEquals(200, h.status);
        assertTrue(h.json().get("success").asBoolean());
        assertTrue(h.json().get("vote").isNull());

        FakeDao dao = new FakeDao();
        dao.stored = vote(5);
        dao.stored.setWithdrawnAt(java.time.OffsetDateTime.now());
        Harness w = Harness.as("GET", "RESIDENT", "ACTIVE");
        w.parameters.put("propertyId", "5");
        new VoteAction(dao).execute(w.request, w.response);
        assertTrue(w.json().get("vote").isNull());
    }

    @Test
    public void getReturnsMyVote() throws Exception {
        FakeDao dao = new FakeDao();
        dao.stored = vote(5);
        Harness h = Harness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "5");
        new VoteAction(dao).execute(h.request, h.response);
        JsonNode body = h.json().get("vote");
        assertEquals(5, body.get("propertyId").asInt());
        assertEquals("I0101", body.get("choices").get(0).get("industryCode").asText());
        assertEquals(1, body.get("choices").get(0).get("rank").asInt());
        assertEquals("週1回程度", body.get("frequency").asText());
        assertFalse(body.get("withdrawn").asBoolean());
        // 距離0.4km・基準1.5km: 1.00/(1+0.4/1.5) + 0.60/(1+0.4/1.5) = 1.6/1.26667 = 1.263
        assertEquals(1.263, body.get("score").asDouble(), 0.0005);
    }

    @Test
    public void postSavesInRankOrderAndReportsUpdate() throws Exception {
        FakeDao dao = new FakeDao();
        Harness h = Harness.as("POST", "RESIDENT", "ACTIVE");
        h.headers.put("X-Requested-With", "XMLHttpRequest");
        h.parameters.put("propertyId", " 7 ");
        h.parameters.put("rank1", "I0101");
        h.parameters.put("rank2", "I0102");
        h.parameters.put("frequency", "週1回程度");
        h.parameters.put("comment", "静かなカフェ希望");
        new VoteAction(dao).execute(h.request, h.response);

        assertEquals(200, h.status);
        assertEquals(1, dao.saves);
        assertEquals(42, dao.userId);
        assertEquals(7, dao.propertyId);
        assertEquals(List.of("I0101", "I0102", ""), dao.keys);
        assertEquals("週1回程度", dao.frequency);
        assertEquals("静かなカフェ希望", dao.comment);
        assertTrue(h.json().get("success").asBoolean());
        assertFalse(h.json().get("updated").asBoolean());

        // 既に投票済みなら updated が true になる。
        dao.stored = vote(7);
        Harness again = Harness.as("POST", "RESIDENT", "ACTIVE");
        again.headers.put("X-Requested-With", "XMLHttpRequest");
        again.parameters.put("propertyId", "7");
        again.parameters.put("rank1", "I0101");
        new VoteAction(dao).execute(again.request, again.response);
        assertTrue(again.json().get("updated").asBoolean());
    }

    @Test
    public void visitFrequencyAliasIsAccepted() throws Exception {
        FakeDao dao = new FakeDao();
        Harness h = Harness.as("POST", "RESIDENT", "ACTIVE");
        h.headers.put("X-Requested-With", "XMLHttpRequest");
        h.parameters.put("propertyId", "7");
        h.parameters.put("rank1", "I0101");
        h.parameters.put("visitFrequency", "月1回以下");
        new VoteAction(dao).execute(h.request, h.response);
        assertEquals("月1回以下", dao.frequency);
    }

    @Test
    public void locationIdIsPassedToTheDaoAndOptional() throws Exception {
        FakeDao dao = new FakeDao();
        Harness h = Harness.as("POST", "RESIDENT", "ACTIVE");
        h.headers.put("X-Requested-With", "XMLHttpRequest");
        h.parameters.put("propertyId", "7");
        h.parameters.put("rank1", "I0101");
        h.parameters.put("locationId", " 103 ");
        new VoteAction(dao).execute(h.request, h.response);
        assertEquals(200, h.status);
        assertEquals(Integer.valueOf(103), dao.locationId);

        FakeDao without = new FakeDao();
        for (String blank : new String[] {null, "", "  "}) {
            Harness none = Harness.as("POST", "RESIDENT", "ACTIVE");
            none.headers.put("X-Requested-With", "XMLHttpRequest");
            none.parameters.put("propertyId", "7");
            none.parameters.put("rank1", "I0101");
            if (blank != null) none.parameters.put("locationId", blank);
            new VoteAction(without).execute(none.request, none.response);
            assertEquals(200, none.status);
            assertNull("未指定なら一番近い拠点", without.locationId);
        }
    }

    @Test
    public void invalidLocationIdIs400WithoutSaving() throws Exception {
        for (String value : new String[] {"abc", "0", "-1", "1.5"}) {
            FakeDao dao = new FakeDao();
            Harness h = Harness.as("POST", "RESIDENT", "ACTIVE");
            h.headers.put("X-Requested-With", "XMLHttpRequest");
            h.parameters.put("propertyId", "7");
            h.parameters.put("rank1", "I0101");
            h.parameters.put("locationId", value);
            new VoteAction(dao).execute(h.request, h.response);
            assertEquals("locationId=" + value, 400, h.status);
            assertEquals("INVALID_INPUT", h.json().get("code").asText());
            assertEquals(0, dao.saves);
        }
    }

    @Test
    public void voteExceptionsMapToStatusCodes() throws Exception {
        Map<String, Integer> expected = Map.of(
                VoteException.BAD_CHOICES, 400, VoteException.INVALID_INPUT, 400, VoteException.PROPERTY_NOT_FOUND, 404,
                VoteException.PROPERTY_CLOSED, 409, VoteException.PROPERTY_NO_LOCATION, 409, VoteException.NO_LOCATION, 409,
                VoteException.NOT_RESIDENT, 403, VoteException.BAD_LOCATION, 400);
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            FakeDao dao = new FakeDao();
            dao.failure = new VoteException(entry.getKey(), "メッセージ");
            Harness h = Harness.as("POST", "RESIDENT", "ACTIVE");
            h.headers.put("X-Requested-With", "XMLHttpRequest");
            h.parameters.put("propertyId", "7");
            h.parameters.put("rank1", "I0101");
            new VoteAction(dao).execute(h.request, h.response);
            assertEquals(entry.getKey(), (int) entry.getValue(), h.status);
            assertEquals(entry.getKey(), h.json().get("code").asText());
            assertEquals("メッセージ", h.json().get("message").asText());
        }
    }

    @Test
    public void databaseErrorIs500WithoutDetails() throws Exception {
        FakeDao dao = new FakeDao();
        dao.sqlFailure = new SQLException("password=secret leaked", "08006");
        Harness h = Harness.as("POST", "RESIDENT", "ACTIVE");
        h.headers.put("X-Requested-With", "XMLHttpRequest");
        h.parameters.put("propertyId", "7");
        h.parameters.put("rank1", "I0101");
        new VoteAction(dao).execute(h.request, h.response);
        assertEquals(500, h.status);
        assertEquals("SERVER_ERROR", h.json().get("code").asText());
        assertFalse(h.body.toString().contains("secret"));
    }

    @Test
    public void withdrawCallsDaoAndReports() throws Exception {
        FakeDao dao = new FakeDao();
        Harness h = Harness.as("POST", "RESIDENT", "ACTIVE");
        h.headers.put("X-Requested-With", "XMLHttpRequest");
        h.parameters.put("propertyId", "7");
        h.parameters.put("withdraw", "1");
        new VoteAction(dao).execute(h.request, h.response);
        assertEquals(1, dao.withdraws);
        assertEquals(0, dao.saves);
        assertTrue(h.json().get("withdrawn").asBoolean());
    }

    private static PropertyVote vote(int propertyId) {
        PropertyVote vote = new PropertyVote();
        vote.setVoteId(9);
        vote.setPropertyId(propertyId);
        vote.setResidentUserId(42);
        vote.setDistanceKm(new BigDecimal("0.400"));
        vote.setDistanceScaleKm(new BigDecimal("1.500"));
        vote.setVisitFrequency("週1回程度");
        for (int rank = 1; rank <= 2; rank++) {
            VoteChoice choice = new VoteChoice();
            choice.setPreferenceRank(rank);
            choice.setIndustryTypeId(rank);
            choice.setIndustryCode(rank == 1 ? "I0101" : "I0102");
            choice.setIndustryName(rank == 1 ? "カフェ・喫茶店" : "レストラン");
            choice.setRankWeight(rank == 1 ? new BigDecimal("1.00") : new BigDecimal("0.60"));
            vote.getChoices().add(choice);
        }
        return vote;
    }

    /** DBへ接続せず、呼び出された内容を記録するだけのDAO。 */
    private static final class FakeDao extends VoteDao {
        PropertyVote stored;
        VoteException failure;
        SQLException sqlFailure;
        int saves, withdraws, userId, propertyId;
        List<String> keys;
        Integer locationId;
        String frequency, comment;

        @Override
        public PropertyVote saveVote(int userId, int propertyId, Integer locationId, List<String> keys, String frequency, String comment)
                throws SQLException, VoteException {
            this.locationId = locationId;
            if (sqlFailure != null) throw sqlFailure;
            if (failure != null) throw failure;
            saves++;
            this.userId = userId; this.propertyId = propertyId; this.keys = keys; this.frequency = frequency; this.comment = comment;
            return vote(propertyId);
        }

        @Override
        public Optional<PropertyVote> findVote(int userId, int propertyId) { return Optional.ofNullable(stored); }

        @Override
        public boolean withdrawVote(int userId, int propertyId) { withdraws++; return true; }
    }

    private static final class Harness {
        final Map<String, String> parameters = new HashMap<>();
        final Map<String, String> headers = new HashMap<>();
        final Map<String, Object> sessionValues = new HashMap<>();
        final StringWriter body = new StringWriter();
        boolean hasSession = true;
        int status = 200;
        final HttpServletRequest request;
        final HttpServletResponse response;

        static Harness as(String method, String role, String accountStatus) {
            Harness h = new Harness(method);
            User user = new User();
            user.setUserId(42);
            user.setUserRole(role);
            user.setAccountStatus(accountStatus);
            h.sessionValues.put("user", user);
            return h;
        }

        Harness(String method) {
            HttpSession session = proxy(HttpSession.class, (ignored, call, args) -> {
                if ("getAttribute".equals(call.getName())) return sessionValues.get(args[0]);
                return null;
            });
            ServletContext context = proxy(ServletContext.class, (ignored, call, args) -> null);
            request = proxy(HttpServletRequest.class, (ignored, call, args) -> {
                switch (call.getName()) {
                    case "getMethod": return method;
                    case "getParameter": return parameters.get(args[0]);
                    case "getHeader": return headers.get(args[0]);
                    case "getSession": return hasSession ? session : null;
                    case "getServletContext": return context;
                    default: return null;
                }
            });
            response = proxy(HttpServletResponse.class, (ignored, call, args) -> {
                switch (call.getName()) {
                    case "setStatus": status = (Integer) args[0]; return null;
                    case "getWriter": return new PrintWriter(body, true);
                    default: return null;
                }
            });
        }

        JsonNode json() throws Exception { return JSON.readTree(body.toString()); }

        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
        }
    }
}
