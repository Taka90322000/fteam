package jp.machiriku.servlet.vote;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.Optional;

import org.junit.Test;

import jp.machiriku.bean.PropertyVote;
import jp.machiriku.bean.VoteContext;
import jp.machiriku.dao.VoteContextDao;
import jp.machiriku.dao.VoteDao;

/** VoteDoneAction(投票完了画面)のテスト(DBは使わない)。 */
public class VoteDoneActionTest {

    @Test
    public void guardsComeFirst() throws Exception {
        PageTestHarness out = PageTestHarness.anonymous("GET");
        new VoteDoneAction(new FakeVoteDao(), new FakeContextDao()).execute(out.request, out.response);
        assertEquals("/machiriku/jp/machiriku/servlet/auth/Login.action", out.redirect);

        PageTestHarness business = PageTestHarness.as("GET", "BUSINESS", "ACTIVE");
        new VoteDoneAction(new FakeVoteDao(), new FakeContextDao()).execute(business.request, business.response);
        assertEquals(Integer.valueOf(403), business.errorCode);

        PageTestHarness post = PageTestHarness.as("POST", "RESIDENT", "ACTIVE");
        new VoteDoneAction(new FakeVoteDao(), new FakeContextDao()).execute(post.request, post.response);
        assertEquals(Integer.valueOf(405), post.errorCode);
    }

    @Test
    public void invalidPropertyIdIs400() throws Exception {
        for (String value : new String[] {null, "abc", "0"}) {
            PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
            if (value != null) h.parameters.put("propertyId", value);
            new VoteDoneAction(new FakeVoteDao(), new FakeContextDao()).execute(h.request, h.response);
            assertEquals(Integer.valueOf(400), h.errorCode);
        }
    }

    @Test
    public void showsTheSavedVote() throws Exception {
        FakeVoteDao voteDao = new FakeVoteDao();
        voteDao.stored = VoteTestData.vote(9, 5, false);
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "5");
        h.parameters.put("updated", "1");
        new VoteDoneAction(voteDao, new FakeContextDao()).execute(h.request, h.response);

        assertEquals(VoteViews.DONE, h.forwardedTo);
        assertSame(voteDao.stored, h.attributes.get("vote"));
        assertTrue(h.attributes.get("voteContext") instanceof VoteContext);
        assertEquals(Boolean.TRUE, h.attributes.get("updated"));
        assertEquals("no-store", h.responseHeaders.get("Cache-Control"));
    }

    @Test
    public void notVotedOrWithdrawnGoesBackToTheForm() throws Exception {
        for (PropertyVote stored : new PropertyVote[] {null, VoteTestData.vote(9, 5, true)}) {
            FakeVoteDao voteDao = new FakeVoteDao();
            voteDao.stored = stored;
            PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
            h.parameters.put("propertyId", "5");
            new VoteDoneAction(voteDao, new FakeContextDao()).execute(h.request, h.response);
            assertEquals("/machiriku/jp/machiriku/servlet/vote/VoteForm.action?propertyId=5", h.redirect);
            assertFalse(h.rendered());
        }
    }

    @Test
    public void databaseErrorIs500() throws Exception {
        FakeVoteDao voteDao = new FakeVoteDao();
        voteDao.failure = new SQLException("boom", "08006");
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "5");
        new VoteDoneAction(voteDao, new FakeContextDao()).execute(h.request, h.response);
        assertEquals(Integer.valueOf(500), h.errorCode);
    }

    private static final class FakeVoteDao extends VoteDao {
        PropertyVote stored;
        SQLException failure;

        @Override
        public Optional<PropertyVote> findVote(int userId, int propertyId) throws SQLException {
            if (failure != null) throw failure;
            return Optional.ofNullable(stored);
        }
    }

    private static final class FakeContextDao extends VoteContextDao {
        @Override
        public Optional<VoteContext> load(int userId, int propertyId) { return Optional.of(VoteTestData.context(propertyId, true)); }
    }
}
