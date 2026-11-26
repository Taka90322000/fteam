package jp.machiriku.dao;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.util.List;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import jp.machiriku.bean.DemandReport;

/** 需要集計を、一時テーブルの検証用データで確認する。MACHIRIKU_RUN_DB_TESTS=true のときだけ実行する。 */
public class DemandDaoTest {
    private Connection connection;
    private DemandDao dao;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("true".equalsIgnoreCase(System.getenv("MACHIRIKU_RUN_DB_TESTS")));
        connection = Dao.getConnection();
        DemandTestSchema.create(connection);
        Connection borrowed = DemandTestSchema.borrowed(connection);
        dao = new DemandDao() { @Override protected Connection openConnection() { return borrowed; } };
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null) connection.close();
    }

    @Test
    public void propertyDemandSumsScoresOfActiveVotesOnly() throws Exception {
        DemandReport report = dao.propertyDemand(1, 10, 10).orElseThrow();
        assertEquals(DemandReport.SCOPE_PROPERTY, report.scope());
        assertEquals("駅前の空き店舗", report.propertyName());
        assertEquals(1, report.propertyCount());
        assertEquals("取消済みの投票は数えない", 2, report.voteCount());
        assertEquals(2, report.voterCount());
        assertEquals("5.000", report.averageDistanceKm().toPlainString());
        // カフェ: 1.00*1.0 + 0.30*0.5(=0.60*0.5) = 1.30 / 食堂: 0.60*1.0 + 1.00*0.5 = 1.10
        assertEquals("2.400", report.totalScore().toPlainString());
        assertEquals(2, report.industries().size());
        DemandReport.IndustryDemand first = report.industries().get(0);
        assertEquals(1, first.rank());
        assertEquals("I0102", first.industryCode());
        assertEquals("1.300", first.score().toPlainString());
        assertEquals("54.2", first.sharePercent().toPlainString());
        assertEquals(2, first.residentCount());
        assertEquals(1, first.firstChoiceCount());
        assertEquals(1, first.secondChoiceCount());
        assertEquals(0, first.thirdChoiceCount());
        DemandReport.IndustryDemand second = report.industries().get(1);
        assertEquals("I0104", second.industryCode());
        assertEquals("1.100", second.score().toPlainString());
        assertEquals("45.8", second.sharePercent().toPlainString());
    }

    @Test
    public void categoryCountsEachResidentOnce() throws Exception {
        DemandReport report = dao.propertyDemand(1, 10, 10).orElseThrow();
        assertEquals(1, report.categories().size());
        DemandReport.CategoryDemand food = report.categories().get(0);
        assertEquals("C01", food.categoryCode());
        assertEquals("飲食", food.categoryName());
        assertEquals("2.400", food.score().toPlainString());
        assertEquals("100.0", food.sharePercent().toPlainString());
        // 住民1は飲食の2小分類を選んでいるが、大分類の希望人数は1人と数える(住民2と合わせて2人)。
        assertEquals(2, food.residentCount());
    }

    @Test
    public void propertyDemandLimitsIndustriesButKeepsWholeShare() throws Exception {
        DemandReport report = dao.propertyDemand(1, 1, 10).orElseThrow();
        assertEquals(1, report.industries().size());
        assertEquals("54.2", report.industries().get(0).sharePercent().toPlainString());
    }

    @Test
    public void frequenciesAndCommentsExcludeWithdrawnAndBlank() throws Exception {
        DemandReport report = dao.propertyDemand(1, 10, 10).orElseThrow();
        assertEquals(2, report.frequencies().size());
        assertEquals(1, report.frequencies().get(0).count());
        assertEquals(2, report.comments().size());
        assertEquals("静かな食堂希望", report.comments().get(0).comment()); // 新しい順
        assertEquals("駅前に欲しい", report.comments().get(1).comment());
        for (DemandReport.DemandComment comment : report.comments()) {
            assertFalse(comment.comment().contains("取消"));
        }
        assertEquals(1, dao.propertyDemand(1, 10, 1).orElseThrow().comments().size());
        assertTrue("commentLimit=0 ならコメントを返さない", dao.propertyDemand(1, 10, 0).orElseThrow().comments().isEmpty());
        assertTrue("空白だけのコメントは返さない", dao.propertyDemand(3, 10, 10).orElseThrow().comments().isEmpty());
    }

    @Test
    public void missingDeletedAndHiddenPropertiesAreNotFound() throws Exception {
        assertTrue(dao.propertyDemand(999, 10, 10).isEmpty());
        assertTrue("削除済み", dao.propertyDemand(4, 10, 10).isEmpty());
        assertTrue("運営が非表示", dao.propertyDemand(5, 10, 10).isEmpty());
    }

    @Test
    public void propertyWithoutVotesIsEmptyReport() throws Exception {
        DemandReport report = dao.propertyDemand(7, 10, 10).orElseThrow(); // 下書きでも物件があれば返す
        assertEquals(0, report.voteCount());
        assertEquals(0, report.voterCount());
        assertNull(report.averageDistanceKm());
        assertEquals("0.000", report.totalScore().toPlainString());
        assertTrue(report.industries().isEmpty());
        assertTrue(report.categories().isEmpty());
    }

    @Test
    public void areaDemandCoversOpenVisiblePropertiesInTheMunicipality() throws Exception {
        DemandReport report = dao.areaDemand(null, "宇都宮市", 10, 5);
        assertEquals(DemandReport.SCOPE_AREA, report.scope());
        assertEquals("掲載中で削除・非表示でない物件1と2だけ", 2, report.propertyCount());
        assertEquals(3, report.voteCount());
        assertEquals("住民1は2物件に投票しても1人", 2, report.voterCount());
        assertEquals("3.400", report.totalScore().toPlainString());
        assertEquals(3, report.industries().size());
        assertEquals("I0102", report.industries().get(0).industryCode());
        assertEquals("1.300", report.industries().get(0).score().toPlainString());
        assertEquals("I0104", report.industries().get(1).industryCode());
        assertEquals("I0301", report.industries().get(2).industryCode());
        assertEquals("1.000", report.industries().get(2).score().toPlainString());
        assertEquals("29.4", report.industries().get(2).sharePercent().toPlainString());
        assertEquals(2, report.categories().size());
        assertEquals("飲食", report.categories().get(0).categoryName());
        assertEquals(2, report.categories().get(0).residentCount());
        assertEquals("小売", report.categories().get(1).categoryName());
        assertEquals(1, report.categories().get(1).residentCount());
        assertTrue("エリア別はコメントを返さない", report.comments().isEmpty());
    }

    @Test
    public void areaDemandRanksPropertiesWithTopIndustry() throws Exception {
        DemandReport report = dao.areaDemand("栃木県", "宇都宮市", 10, 5);
        assertEquals(2, report.topProperties().size());
        DemandReport.TopProperty top = report.topProperties().get(0);
        assertEquals(1, top.rank());
        assertEquals(1, top.propertyId());
        assertEquals("駅前の空き店舗", top.propertyName());
        assertEquals(2, top.voteCount());
        assertEquals("2.400", top.score().toPlainString());
        assertEquals("カフェ", top.topIndustryName());
        assertEquals(2, report.topProperties().get(1).propertyId());
        assertEquals("雑貨店", report.topProperties().get(1).topIndustryName());
        assertEquals(1, dao.areaDemand(null, "宇都宮市", 10, 1).topProperties().size());
        assertTrue(dao.areaDemand(null, "宇都宮市", 10, 0).topProperties().isEmpty());
    }

    @Test
    public void areaWithoutFilterIncludesEveryOpenProperty() throws Exception {
        DemandReport all = dao.areaDemand(null, null, 10, 5);
        assertEquals(3, all.propertyCount());
        assertEquals(4, all.voteCount());
        assertEquals("I0102", all.industries().get(0).industryCode());
        assertEquals("2.300", all.industries().get(0).score().toPlainString());
        DemandReport ashikaga = dao.areaDemand(null, "足利市", 10, 5);
        assertEquals(1, ashikaga.propertyCount());
        assertEquals(1, ashikaga.voteCount());
        assertEquals(1, ashikaga.voterCount());
    }

    @Test
    public void areaWithMismatchedPrefectureOrNoVotesIsEmpty() throws Exception {
        DemandReport mismatch = dao.areaDemand("群馬県", "宇都宮市", 10, 5);
        assertEquals(0, mismatch.propertyCount());
        assertEquals(0, mismatch.voteCount());
        assertTrue(mismatch.industries().isEmpty());
        assertTrue(mismatch.topProperties().isEmpty());
        assertNull(mismatch.averageDistanceKm());
        assertEquals("0.000", mismatch.totalScore().toPlainString());
    }

    @Test
    public void summariesGiveVoteCountAndTopIndustryPerVisibleProperty() throws Exception {
        List<DemandReport.PropertySummary> list = dao.summaries(List.of(1, 2, 3, 4, 5, 7, 999));
        // 4=削除済み・5=非表示・999=存在しない物件は含めない。7(下書き)は物件として存在するので、投票0件で返す。
        assertEquals(List.of(1, 2, 3, 7), list.stream().map(DemandReport.PropertySummary::propertyId).toList());
        DemandReport.PropertySummary first = list.get(0);
        assertEquals("取消済みを除く有効な投票", 2, first.voteCount());
        assertEquals("2.400", first.score().toPlainString());
        assertEquals("I0102", first.topIndustryCode());
        assertEquals("カフェ", first.topIndustryName());
        assertEquals(1, list.get(1).voteCount());
        assertEquals("雑貨店", list.get(1).topIndustryName());
        DemandReport.PropertySummary draft = list.get(3);
        assertEquals(0, draft.voteCount());
        assertEquals("0.000", draft.score().toPlainString());
        assertNull(draft.topIndustryCode());
        assertNull(draft.topIndustryName());
        assertTrue(dao.summaries(List.of()).isEmpty());
        assertTrue(dao.summaries(null).isEmpty());
    }

    @Test
    public void injectionInAreaNamesIsHarmless() throws Exception {
        DemandReport report = dao.areaDemand(null, "宇都宮市' OR '1'='1", 10, 5);
        assertEquals(0, report.propertyCount());
    }
}
