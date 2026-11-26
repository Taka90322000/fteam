package jp.machiriku.dao;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.util.List;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import jp.machiriku.bean.VoteHistoryPage;

/** 投票履歴を、一時テーブルの検証用データで確認する。MACHIRIKU_RUN_DB_TESTS=true のときだけ実行する。 */
public class VoteHistoryDaoTest {
    private Connection connection;
    private VoteHistoryDao dao;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("true".equalsIgnoreCase(System.getenv("MACHIRIKU_RUN_DB_TESTS")));
        connection = Dao.getConnection();
        DemandTestSchema.create(connection);
        Connection borrowed = DemandTestSchema.borrowed(connection);
        dao = new VoteHistoryDao() { @Override protected Connection openConnection() { return borrowed; } };
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null) connection.close();
    }

    private static List<Integer> voteIds(VoteHistoryPage page) {
        return page.items().stream().map(item -> item.vote().getVoteId()).toList();
    }

    @Test
    public void allVotesNewestFirstIncludingWithdrawnAndDeletedProperty() throws Exception {
        // 住民1の投票: 1(11/10)・4(11/09)・8(取消・11/05 11:00)。物件4(削除済み)への投票は住民5のもの。
        VoteHistoryPage page = dao.list(1, VoteHistoryDao.Status.ALL, 20, 0);
        assertEquals(3, page.total());
        assertEquals(List.of(1, 4, 8), voteIds(page));
        VoteHistoryPage.Item first = page.items().get(0);
        assertEquals("駅前の空き店舗", first.propertyName());
        assertEquals("宇都宮市", first.municipality());
        assertEquals("OPEN", first.listingStatus());
        assertFalse(first.propertyDeleted());
        assertFalse(first.vote().isWithdrawn());
        assertEquals("週1回程度", first.vote().getVisitFrequency());
        assertEquals(2, first.vote().getChoices().size());
        assertEquals("カフェ", first.vote().getChoices().get(0).getIndustryName());
        assertEquals(1, first.vote().getChoices().get(0).getPreferenceRank());
        assertNotNull(first.createdAt());
        VoteHistoryPage.Item withdrawn = page.items().get(2);
        assertTrue(withdrawn.vote().isWithdrawn());
        assertNotNull("取消後も希望内容は履歴に残る", withdrawn.vote().getChoices().get(0));
    }

    @Test
    public void statusFilters() throws Exception {
        VoteHistoryPage active = dao.list(1, VoteHistoryDao.Status.ACTIVE, 20, 0);
        assertEquals(2, active.total());
        assertEquals(List.of(1, 4), voteIds(active));
        VoteHistoryPage withdrawn = dao.list(1, VoteHistoryDao.Status.WITHDRAWN, 20, 0);
        assertEquals(1, withdrawn.total());
        assertEquals(List.of(8), voteIds(withdrawn));
    }

    @Test
    public void paginationKeepsTotalAndOrder() throws Exception {
        VoteHistoryPage first = dao.list(1, VoteHistoryDao.Status.ALL, 2, 0);
        assertEquals(3, first.total());
        assertEquals(List.of(1, 4), voteIds(first));
        VoteHistoryPage second = dao.list(1, VoteHistoryDao.Status.ALL, 2, 2);
        assertEquals(3, second.total());
        assertEquals(List.of(8), voteIds(second));
        assertTrue(dao.list(1, VoteHistoryDao.Status.ALL, 2, 10).items().isEmpty());
    }

    @Test
    public void onlyMyOwnVotesAreReturned() throws Exception {
        assertEquals(List.of(2), voteIds(dao.list(2, VoteHistoryDao.Status.ALL, 20, 0)));
        assertEquals(0, dao.list(99, VoteHistoryDao.Status.ALL, 20, 0).total());
        assertTrue(dao.list(99, VoteHistoryDao.Status.ALL, 20, 0).items().isEmpty());
    }

    @Test
    public void deletedPropertyStaysInHistoryWithFlag() throws Exception {
        VoteHistoryPage page = dao.list(5, VoteHistoryDao.Status.ALL, 20, 0);
        assertEquals(1, page.total());
        assertTrue(page.items().get(0).propertyDeleted());
        assertEquals("削除された物件", page.items().get(0).propertyName());
    }
}
