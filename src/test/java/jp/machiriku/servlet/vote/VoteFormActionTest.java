package jp.machiriku.servlet.vote;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import jp.machiriku.bean.IndustryMaster;
import jp.machiriku.bean.PropertyVote;
import jp.machiriku.bean.User;
import jp.machiriku.bean.VoteContext;
import jp.machiriku.dao.IndustryMasterDao;
import jp.machiriku.dao.VoteContextDao;
import jp.machiriku.dao.VoteDao;

/** VoteFormAction(投票画面の表示)の本人確認・入力検証・JSPへのforwardのテスト(DBは使わない)。 */
public class VoteFormActionTest {

    @Test
    public void loggedOutIsSentToLogin() throws Exception {
        PageTestHarness h = PageTestHarness.anonymous("GET");
        h.parameters.put("propertyId", "5");
        new VoteFormAction(new FakeContextDao(), new FakeIndustryDao(), new FakeVoteDao()).execute(h.request, h.response);
        assertEquals("/machiriku/jp/machiriku/servlet/auth/Login.action", h.redirect);
        assertFalse(h.rendered());
    }

    @Test
    public void pendingResidentIsSentToSetup() throws Exception {
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "PENDING");
        h.parameters.put("propertyId", "5");
        new VoteFormAction(new FakeContextDao(), new FakeIndustryDao(), new FakeVoteDao()).execute(h.request, h.response);
        assertEquals("/machiriku/jp/machiriku/servlet/auth/ResidentSetup.action", h.redirect);
    }

    @Test
    public void businessAndOwnerCannotVote() throws Exception {
        for (String role : List.of("BUSINESS", "OWNER")) {
            PageTestHarness h = PageTestHarness.as("GET", role, "ACTIVE");
            h.parameters.put("propertyId", "5");
            new VoteFormAction(new FakeContextDao(), new FakeIndustryDao(), new FakeVoteDao()).execute(h.request, h.response);
            assertEquals(role, Integer.valueOf(403), h.errorCode);
            assertFalse(h.rendered());
        }
    }

    @Test
    public void withdrawnMemberIsRejected() throws Exception {
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        ((User) h.sessionValues.get("user")).setWithdrawnAt(java.time.OffsetDateTime.now());
        h.parameters.put("propertyId", "5");
        new VoteFormAction(new FakeContextDao(), new FakeIndustryDao(), new FakeVoteDao()).execute(h.request, h.response);
        assertEquals(Integer.valueOf(403), h.errorCode);
    }

    @Test
    public void postIsNotAllowed() throws Exception {
        PageTestHarness h = PageTestHarness.as("POST", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "5");
        new VoteFormAction(new FakeContextDao(), new FakeIndustryDao(), new FakeVoteDao()).execute(h.request, h.response);
        assertEquals(Integer.valueOf(405), h.errorCode);
    }

    @Test
    public void invalidPropertyIdIs400() throws Exception {
        for (String value : new String[] {null, "", "abc", "0", "-1", "1.5"}) {
            PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
            if (value != null) h.parameters.put("propertyId", value);
            FakeContextDao dao = new FakeContextDao();
            new VoteFormAction(dao, new FakeIndustryDao(), new FakeVoteDao()).execute(h.request, h.response);
            assertEquals("propertyId=" + value, Integer.valueOf(400), h.errorCode);
            assertEquals("DBは呼ばない", 0, dao.calls);
        }
    }

    @Test
    public void unknownPropertyIs404() throws Exception {
        FakeContextDao dao = new FakeContextDao();
        dao.context = null;
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "5");
        new VoteFormAction(dao, new FakeIndustryDao(), new FakeVoteDao()).execute(h.request, h.response);
        assertEquals(Integer.valueOf(404), h.errorCode);
        assertFalse(h.rendered());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void rendersTheFormWithEverythingTheJspNeeds() throws Exception {
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", " 5 ");
        FakeContextDao dao = new FakeContextDao();
        new VoteFormAction(dao, new FakeIndustryDao(), new FakeVoteDao()).execute(h.request, h.response);

        assertEquals(VoteViews.FORM, h.forwardedTo);
        assertEquals(5, dao.propertyId);
        assertEquals(42, dao.userId);
        assertEquals("no-store", h.responseHeaders.get("Cache-Control"));
        assertEquals(200, h.status);
        assertTrue(h.attributes.get("voteContext") instanceof VoteContext);
        assertTrue(h.attributes.get("industries") instanceof IndustryMaster);
        assertNull("未投票", h.attributes.get("currentVote"));
        assertNull(h.attributes.get("errorMessage"));
        String token = (String) h.attributes.get("csrfToken");
        assertEquals("セッションのトークンと同じ", h.sessionValues.get("voteCsrfToken"), token);
        assertEquals(64, token.length());
        Map<String, String> values = (Map<String, String>) h.attributes.get("formValues");
        assertEquals("", values.get("rank1"));
        assertEquals("週1回程度", values.get("frequency"));
        assertEquals("最寄りの拠点が選ばれている", "7", values.get("locationId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void existingVoteFillsTheForm() throws Exception {
        FakeVoteDao voteDao = new FakeVoteDao();
        voteDao.stored = VoteTestData.vote(9, 5, false);
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "5");
        new VoteFormAction(new FakeContextDao(), new FakeIndustryDao(), voteDao).execute(h.request, h.response);

        assertSame(voteDao.stored, h.attributes.get("currentVote"));
        Map<String, String> values = (Map<String, String>) h.attributes.get("formValues");
        assertEquals("I0101", values.get("rank1"));
        assertEquals("I0102", values.get("rank2"));
        assertEquals("", values.get("rank3"));
        assertEquals("駅前に欲しいです", values.get("comment"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void withdrawnVoteIsTreatedAsNotVoted() throws Exception {
        FakeVoteDao voteDao = new FakeVoteDao();
        voteDao.stored = VoteTestData.vote(9, 5, true);
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "5");
        new VoteFormAction(new FakeContextDao(), new FakeIndustryDao(), voteDao).execute(h.request, h.response);
        assertNull(h.attributes.get("currentVote"));
        assertEquals("", ((Map<String, String>) h.attributes.get("formValues")).get("rank1"));
    }

    @Test
    public void databaseErrorIs500WithoutDetails() throws Exception {
        FakeContextDao dao = new FakeContextDao();
        dao.failure = new SQLException("password=secret leaked", "08006");
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "5");
        new VoteFormAction(dao, new FakeIndustryDao(), new FakeVoteDao()).execute(h.request, h.response);
        assertEquals(Integer.valueOf(500), h.errorCode);
        assertFalse(h.rendered());
    }

    static final class FakeContextDao extends VoteContextDao {
        VoteContext context = VoteTestData.context(5, true);
        SQLException failure;
        int calls, userId, propertyId;

        @Override
        public Optional<VoteContext> load(int userId, int propertyId) throws SQLException {
            calls++;
            if (failure != null) throw failure;
            this.userId = userId; this.propertyId = propertyId;
            return Optional.ofNullable(context);
        }
    }

    static final class FakeIndustryDao extends IndustryMasterDao {
        @Override
        public IndustryMaster findActive() { return VoteTestData.industries(); }
    }

    static final class FakeVoteDao extends VoteDao {
        PropertyVote stored;

        @Override
        public Optional<PropertyVote> findVote(int userId, int propertyId) { return Optional.ofNullable(stored); }
    }
}
