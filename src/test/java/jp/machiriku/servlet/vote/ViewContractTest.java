package jp.machiriku.servlet.vote;

import static org.junit.Assert.*;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import jp.machiriku.bean.DemandReport;
import jp.machiriku.bean.IndustryMaster;
import jp.machiriku.bean.PropertyVote;
import jp.machiriku.bean.VoteContext;
import jp.machiriku.bean.VoteHistoryPage;
import jp.machiriku.dao.DemandDao;
import jp.machiriku.dao.VoteHistoryDao;

/**
 * 「画面との約束」のテスト。フロント担当がJSPに書く ${...}(EL)が、Actionの渡す値を実際に読めることを確かめる。
 * docs/vote-api.md の「8. 画面を作る人へ」に書いた request属性名と ${...} の書き方は、ここで確認している。
 * <p>ELは、beanのgetXxx()(java.beans.Introspector)・Mapのキー・Listの[番号]で値をたどる。
 * このテストの {@link #el} は同じ規則で値をたどるので、ここで読めるものは画面でも読める。</p>
 */
public class ViewContractTest {

    // ---- Actionが渡す値を、ELで読めるか --------------------------------------------------------------

    @Test
    public void voteFormAttributesAreReadableFromEl() throws Exception {
        VoteFormActionTest.FakeVoteDao voteDao = new VoteFormActionTest.FakeVoteDao();
        voteDao.stored = VoteTestData.vote(9, 5, false);
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "5");
        new VoteFormAction(new VoteFormActionTest.FakeContextDao(), new VoteFormActionTest.FakeIndustryDao(), voteDao)
                .execute(h.request, h.response);
        assertEquals(VoteViews.FORM, h.forwardedTo);

        Map<String, Object> page = h.attributes;
        assertEquals("駅前の空き店舗", el(page, "voteContext.property.name"));
        assertEquals("栃木県", el(page, "voteContext.property.prefecture"));
        assertEquals("宇都宮市", el(page, "voteContext.property.municipality"));
        assertEquals("宇都宮市1-1", el(page, "voteContext.property.addressLine"));
        assertEquals(5, el(page, "voteContext.property.propertyId"));
        assertEquals(Boolean.TRUE, el(page, "voteContext.votable"));
        assertEquals(3, el(page, "voteContext.voteCount"));
        assertEquals(7, el(page, "voteContext.defaultLocationId"));
        assertEquals(7, el(page, "voteContext.locations[0].locationId"));
        assertEquals("HOME", el(page, "voteContext.locations[0].kind"));
        assertEquals("0.400", ((BigDecimal) el(page, "voteContext.locations[0].distanceKm")).toPlainString());
        assertEquals(Boolean.TRUE, el(page, "voteContext.locations[0].nearest"));
        assertEquals(Boolean.FALSE, el(page, "voteContext.locations[1].hasCoordinates"));

        assertEquals("飲食", el(page, "industries.categories[0].categoryName"));
        assertEquals("I0102", el(page, "industries.categories[0].industries[1].industryCode"));
        assertEquals("レストラン", el(page, "industries.categories[0].industries[1].industryName"));

        assertEquals("I0101", el(page, "currentVote.choices[0].industryCode"));
        assertEquals("レストラン", el(page, "currentVote.choices[1].industryName"));
        assertEquals("週1回程度", el(page, "currentVote.visitFrequency"));
        assertEquals("駅前に欲しいです", el(page, "currentVote.requestComment"));
        assertEquals(Boolean.FALSE, el(page, "currentVote.withdrawn"));
        assertEquals("1.263", ((BigDecimal) el(page, "currentVote.score")).toPlainString());

        assertEquals("I0101", el(page, "formValues.rank1"));
        assertEquals("I0102", el(page, "formValues.rank2"));
        assertEquals("週1回程度", el(page, "formValues.frequency"));
        assertEquals("駅前に欲しいです", el(page, "formValues.comment"));
        assertEquals("7", el(page, "formValues.locationId"));

        assertTrue(el(page, "csrfToken") instanceof String);
        assertFalse(((String) el(page, "csrfToken")).isBlank());
    }

    @Test
    public void voteDoneAttributesAreReadableFromEl() throws Exception {
        VoteFormActionTest.FakeVoteDao voteDao = new VoteFormActionTest.FakeVoteDao();
        voteDao.stored = VoteTestData.vote(9, 5, false);
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "5");
        h.parameters.put("updated", "1");
        new VoteDoneAction(voteDao, new VoteFormActionTest.FakeContextDao()).execute(h.request, h.response);
        assertEquals(VoteViews.DONE, h.forwardedTo);

        assertEquals("駅前の空き店舗", el(h.attributes, "voteContext.property.name"));
        assertEquals("カフェ・喫茶店", el(h.attributes, "vote.choices[0].industryName"));
        assertEquals("週1回程度", el(h.attributes, "vote.visitFrequency"));
        assertNotNull(el(h.attributes, "vote.score"));
        assertEquals(Boolean.TRUE, el(h.attributes, "updated"));
    }

    @Test
    public void voteHistoryAttributesAreReadableFromEl() throws Exception {
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("status", "withdrawn");
        h.parameters.put("page", "2");
        h.parameters.put("withdrawn", "1");
        new VoteHistoryAction(new VoteHistoryDao() {
            @Override
            public VoteHistoryPage list(int userId, Status status, int limit, int offset) {
                PropertyVote vote = VoteTestData.vote(9, 5, true);
                return new VoteHistoryPage(11, limit, offset, List.of(new VoteHistoryPage.Item(vote, "駅前の空き店舗", "栃木県",
                        "宇都宮市", "宇都宮市1-1", "OPEN", true, OffsetDateTime.parse("2026-11-01T00:00:00Z"))));
            }
        }).execute(h.request, h.response);
        assertEquals(VoteViews.HISTORY, h.forwardedTo);

        assertEquals(11, el(h.attributes, "history.total"));
        assertEquals(1, ((List<?>) el(h.attributes, "history.items")).size());
        assertEquals(5, el(h.attributes, "history.items[0].vote.propertyId"));
        assertEquals(Boolean.TRUE, el(h.attributes, "history.items[0].vote.withdrawn"));
        assertEquals("カフェ・喫茶店", el(h.attributes, "history.items[0].vote.choices[0].industryName"));
        assertEquals("駅前の空き店舗", el(h.attributes, "history.items[0].propertyName"));
        assertEquals("宇都宮市", el(h.attributes, "history.items[0].municipality"));
        assertEquals("OPEN", el(h.attributes, "history.items[0].listingStatus"));
        assertEquals(Boolean.TRUE, el(h.attributes, "history.items[0].propertyDeleted"));
        assertNotNull(el(h.attributes, "history.items[0].createdAt"));
        assertEquals("withdrawn", el(h.attributes, "status"));
        assertEquals(2, el(h.attributes, "pageNumber"));
        assertEquals(2, el(h.attributes, "totalPages"));
        assertEquals(Boolean.TRUE, el(h.attributes, "withdrawnNotice"));
        assertFalse(((String) el(h.attributes, "csrfToken")).isBlank());
    }

    @Test
    public void demandAttributesAreReadableFromEl() throws Exception {
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "7");
        new DemandAction(new DemandDao() {
            @Override
            public Optional<DemandReport> propertyDemand(int propertyId, int industryLimit, int commentLimit) {
                return Optional.of(new DemandReport(DemandReport.SCOPE_PROPERTY, propertyId, "駅前の空き店舗", "栃木県", "宇都宮市",
                        1, 2, 2, new BigDecimal("0.400"), new BigDecimal("1.600"),
                        List.of(new DemandReport.IndustryDemand(1, 1, "I0101", "カフェ・喫茶店", new BigDecimal("1.600"), new BigDecimal("100.0"), 2, 2, 0, 0)),
                        List.of(new DemandReport.CategoryDemand(1, "C01", "飲食", new BigDecimal("1.600"), new BigDecimal("100.0"), 2)),
                        List.of(new DemandReport.FrequencyCount("週1回程度", 2)),
                        List.of(new DemandReport.DemandComment("駅前に欲しいです", "週1回程度", OffsetDateTime.parse("2026-11-10T10:00:00Z"))),
                        List.of(new DemandReport.TopProperty(1, 7, "駅前の空き店舗", "宇都宮市", 2, new BigDecimal("1.600"), "カフェ・喫茶店"))));
            }
        }).execute(h.request, h.response);
        assertEquals(VoteViews.DEMAND, h.forwardedTo);

        Map<String, Object> page = h.attributes;
        assertEquals("PROPERTY", el(page, "report.scope"));
        assertEquals(7, el(page, "report.propertyId"));
        assertEquals("駅前の空き店舗", el(page, "report.propertyName"));
        assertEquals(2, el(page, "report.voteCount"));
        assertEquals(2, el(page, "report.voterCount"));
        assertEquals("1.600", ((BigDecimal) el(page, "report.totalScore")).toPlainString());
        assertEquals(1, el(page, "report.industries[0].rank"));
        assertEquals("カフェ・喫茶店", el(page, "report.industries[0].industryName"));
        assertEquals(2, el(page, "report.industries[0].firstChoiceCount"));
        assertEquals(2, el(page, "report.industries[0].residentCount"));
        assertEquals("飲食", el(page, "report.categories[0].categoryName"));
        assertEquals("週1回程度", el(page, "report.frequencies[0].frequency"));
        assertEquals(2, el(page, "report.frequencies[0].count"));
        assertEquals("駅前に欲しいです", el(page, "report.comments[0].comment"));
        assertNotNull(el(page, "report.comments[0].votedAt"));
        assertEquals("駅前の空き店舗", el(page, "report.topProperties[0].propertyName"));
        assertEquals(Boolean.TRUE, el(page, "commentsVisible"));
    }

    // ---- 今後beanに項目を足したときの取りこぼし防止 -----------------------------------------------

    @Test
    public void everyRecordComponentOfTheViewBeansHasAnElGetter() throws Exception {
        for (Class<?> type : List.of(VoteContext.class, IndustryMaster.class, VoteHistoryPage.class, DemandReport.class)) {
            checkRecordTree(type);
        }
    }

    private static void checkRecordTree(Class<?> type) throws IntrospectionException {
        assertTrue(type.getName() + " は public でないと、ELから読めない", Modifier.isPublic(type.getModifiers()));
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                PropertyDescriptor property = propertyOf(type, component.getName());
                assertNotNull(type.getSimpleName() + "." + component.getName() + " に getXxx() がない(ELで読めない)", property);
                assertNotNull(type.getSimpleName() + "." + component.getName() + " の読み取りメソッドがない", property.getReadMethod());
                assertEquals(component.getType(), property.getReadMethod().getReturnType());
            }
        }
        for (Class<?> nested : type.getDeclaredClasses()) { checkRecordTree(nested); }
    }

    // ---- ELと同じ規則で値をたどる道具 --------------------------------------------------------------------

    /** 例: el(attributes, "report.industries[0].industryName")。Map→キー、List→[番号]、bean→getXxx()でたどる。 */
    static Object el(Map<String, Object> attributes, String path) {
        Object current = attributes;
        for (String step : path.split("\\.")) {
            String name = step;
            Integer index = null;
            int bracket = step.indexOf('[');
            if (bracket >= 0) {
                name = step.substring(0, bracket);
                index = Integer.valueOf(step.substring(bracket + 1, step.indexOf(']')));
            }
            current = property(current, name, path);
            if (index != null) {
                assertTrue(path + ": [" + index + "] は List でないと読めない(実際: " + (current == null ? "null" : current.getClass().getSimpleName()) + ")",
                        current instanceof List);
                current = ((List<?>) current).get(index);
            }
        }
        return current;
    }

    private static Object property(Object base, String name, String path) {
        assertNotNull(path + ": 途中の値が null です(" + name + " の手前)", base);
        if (base instanceof Map<?, ?> map) {
            assertTrue(path + ": キー " + name + " がありません", map.containsKey(name));
            return map.get(name);
        }
        try {
            PropertyDescriptor property = propertyOf(base.getClass(), name);
            assertNotNull(path + ": " + base.getClass().getSimpleName() + " に getXxx() で読めるプロパティ「" + name + "」がありません", property);
            Method getter = property.getReadMethod();
            assertNotNull(path + ": " + name + " の読み取りメソッドがありません", getter);
            assertTrue(path + ": " + name + " を読むクラスが public ではありません", Modifier.isPublic(getter.getDeclaringClass().getModifiers()));
            return getter.invoke(base);
        } catch (ReflectiveOperationException | IntrospectionException e) {
            throw new AssertionError(path + ": " + name + " を読めませんでした: " + e, e);
        }
    }

    private static PropertyDescriptor propertyOf(Class<?> type, String name) throws IntrospectionException {
        for (PropertyDescriptor property : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
            if (property.getName().equals(name)) return property;
        }
        return null;
    }
}
