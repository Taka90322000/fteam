package jp.machiriku.servlet.vote;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.Test;

import jp.machiriku.bean.VoteHistoryPage;
import jp.machiriku.dao.VoteHistoryDao;

/** VoteHistoryAction(投票履歴)の権限・入力検証・ページ送りのテスト(DBは使わない)。 */
public class VoteHistoryActionTest {

    @Test
    public void guardsComeFirst() throws Exception {
        FakeDao dao = new FakeDao();
        PageTestHarness out = PageTestHarness.anonymous("GET");
        new VoteHistoryAction(dao).execute(out.request, out.response);
        assertEquals("/machiriku/jp/machiriku/servlet/auth/Login.action", out.redirect);

        for (String role : List.of("BUSINESS", "OWNER")) {
            PageTestHarness h = PageTestHarness.as("GET", role, "ACTIVE");
            new VoteHistoryAction(dao).execute(h.request, h.response);
            assertEquals(Integer.valueOf(403), h.errorCode);
        }
        PageTestHarness post = PageTestHarness.as("POST", "RESIDENT", "ACTIVE");
        new VoteHistoryAction(dao).execute(post.request, post.response);
        assertEquals(Integer.valueOf(405), post.errorCode);
        assertEquals("DBは呼ばない", 0, dao.calls);
    }

    @Test
    public void invalidParametersAre400() throws Exception {
        String[][] bad = {{"status", "done"}, {"status", "ACTIVEX"}, {"page", "0"}, {"page", "-1"}, {"page", "abc"}, {"page", "1.5"}};
        for (String[] pair : bad) {
            PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
            h.parameters.put(pair[0], pair[1]);
            FakeDao dao = new FakeDao();
            new VoteHistoryAction(dao).execute(h.request, h.response);
            assertEquals(pair[0] + "=" + pair[1], Integer.valueOf(400), h.errorCode);
            assertEquals(0, dao.calls);
        }
    }

    @Test
    public void defaultsToAllVotesFirstPage() throws Exception {
        FakeDao dao = new FakeDao();
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        new VoteHistoryAction(dao).execute(h.request, h.response);

        assertEquals(VoteViews.HISTORY, h.forwardedTo);
        assertEquals(42, dao.userId);
        assertEquals(VoteHistoryDao.Status.ALL, dao.status);
        assertEquals(10, dao.limit);
        assertEquals(0, dao.offset);
        assertEquals("all", h.attributes.get("status"));
        assertEquals(1, h.attributes.get("pageNumber"));
        assertEquals("25件 → 3ページ", 3, h.attributes.get("totalPages"));
        assertEquals(Boolean.FALSE, h.attributes.get("withdrawnNotice"));
        assertTrue(h.attributes.get("history") instanceof VoteHistoryPage);
        assertEquals(h.sessionValues.get("voteCsrfToken"), h.attributes.get("csrfToken"));
        assertEquals("no-store", h.responseHeaders.get("Cache-Control"));
    }

    @Test
    public void statusAndPageSelectTheRows() throws Exception {
        FakeDao dao = new FakeDao();
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("status", " Withdrawn ");
        h.parameters.put("page", "3");
        h.parameters.put("withdrawn", "1");
        new VoteHistoryAction(dao).execute(h.request, h.response);
        assertEquals(VoteHistoryDao.Status.WITHDRAWN, dao.status);
        assertEquals("3ページ目は21件目から", 20, dao.offset);
        assertEquals("withdrawn", h.attributes.get("status"));
        assertEquals(Boolean.TRUE, h.attributes.get("withdrawnNotice"));

        PageTestHarness active = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        active.parameters.put("status", "active");
        new VoteHistoryAction(dao).execute(active.request, active.response);
        assertEquals(VoteHistoryDao.Status.ACTIVE, dao.status);
    }

    @Test
    public void emptyHistoryStillHasOnePage() throws Exception {
        FakeDao dao = new FakeDao();
        dao.total = 0;
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        new VoteHistoryAction(dao).execute(h.request, h.response);
        assertEquals(1, h.attributes.get("totalPages"));
    }

    @Test
    public void databaseErrorIs500WithoutDetails() throws Exception {
        FakeDao dao = new FakeDao();
        dao.failure = new SQLException("password=secret leaked", "08006");
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        new VoteHistoryAction(dao).execute(h.request, h.response);
        assertEquals(Integer.valueOf(500), h.errorCode);
        assertFalse(h.rendered());
    }

    private static final class FakeDao extends VoteHistoryDao {
        SQLException failure;
        int calls, userId, limit, offset, total = 25;
        Status status;

        @Override
        public VoteHistoryPage list(int userId, Status status, int limit, int offset) throws SQLException {
            calls++;
            if (failure != null) throw failure;
            this.userId = userId; this.status = status; this.limit = limit; this.offset = offset;
            return new VoteHistoryPage(total, limit, offset, total == 0 ? List.of() : List.of(
                    new VoteHistoryPage.Item(VoteTestData.vote(9, 5, false), "駅前の空き店舗", "栃木県", "宇都宮市", "宇都宮市1-1", "OPEN", false,
                            OffsetDateTime.parse("2026-11-09T10:00:00Z"))));
        }
    }
}
