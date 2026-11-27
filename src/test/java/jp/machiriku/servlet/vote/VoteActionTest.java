package jp.machiriku.servlet.vote;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import jp.machiriku.bean.PropertyVote;
import jp.machiriku.dao.VoteDao;
import jp.machiriku.dao.VoteException;
import jp.machiriku.servlet.vote.VoteFormActionTest.FakeContextDao;
import jp.machiriku.servlet.vote.VoteFormActionTest.FakeIndustryDao;

/** VoteAction(投票フォームの送信)の本人確認・CSRF対策・入力検証・リダイレクト/再表示のテスト(DBは使わない)。 */
public class VoteActionTest {
    private static final String BASE = "/machiriku/jp/machiriku/servlet/vote/";

    private static PageTestHarness post() {
        PageTestHarness h = PageTestHarness.as("POST", "RESIDENT", "ACTIVE");
        h.sessionValues.put("voteCsrfToken", "tok");
        h.parameters.put("csrfToken", "tok");
        h.parameters.put("propertyId", "5");
        h.parameters.put("rank1", "I0101");
        h.parameters.put("rank2", "I0102");
        h.parameters.put("frequency", "週1回程度");
        h.parameters.put("comment", "駅前に欲しいです");
        return h;
    }

    private static VoteAction action(FakeDao dao) {
        return new VoteAction(dao, new FakeContextDao(), new FakeIndustryDao());
    }

    @Test
    public void loggedOutIsSentToLogin() throws Exception {
        PageTestHarness h = PageTestHarness.anonymous("POST");
        FakeDao dao = new FakeDao();
        action(dao).execute(h.request, h.response);
        assertEquals("/machiriku/jp/machiriku/servlet/auth/Login.action", h.redirect);
        assertEquals(0, dao.saves);
    }

    @Test
    public void pendingBusinessAndOwnerCannotVote() throws Exception {
        PageTestHarness pending = PageTestHarness.as("POST", "RESIDENT", "PENDING");
        action(new FakeDao()).execute(pending.request, pending.response);
        assertEquals("/machiriku/jp/machiriku/servlet/auth/ResidentSetup.action", pending.redirect);
        for (String role : List.of("BUSINESS", "OWNER")) {
            PageTestHarness h = PageTestHarness.as("POST", role, "ACTIVE");
            FakeDao dao = new FakeDao();
            action(dao).execute(h.request, h.response);
            assertEquals(role, Integer.valueOf(403), h.errorCode);
            assertEquals(0, dao.saves);
        }
    }

    @Test
    public void openingTheAddressDirectlyGuidesToTheForm() throws Exception {
        PageTestHarness h = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        h.parameters.put("propertyId", "5");
        action(new FakeDao()).execute(h.request, h.response);
        assertEquals(BASE + "VoteForm.action?propertyId=5", h.redirect);

        PageTestHarness noId = PageTestHarness.as("GET", "RESIDENT", "ACTIVE");
        action(new FakeDao()).execute(noId.request, noId.response);
        assertEquals(Integer.valueOf(405), noId.errorCode);
        PageTestHarness put = PageTestHarness.as("PUT", "RESIDENT", "ACTIVE");
        action(new FakeDao()).execute(put.request, put.response);
        assertEquals(Integer.valueOf(405), put.errorCode);
    }

    @Test
    public void missingOrWrongCsrfTokenIsRejected() throws Exception {
        PageTestHarness wrong = post();
        wrong.parameters.put("csrfToken", "other");
        PageTestHarness missing = post();
        missing.parameters.remove("csrfToken");
        PageTestHarness noSessionToken = post();
        noSessionToken.sessionValues.remove("voteCsrfToken");
        for (PageTestHarness h : List.of(wrong, missing, noSessionToken)) {
            FakeDao dao = new FakeDao();
            action(dao).execute(h.request, h.response);
            assertEquals(Integer.valueOf(403), h.errorCode);
            assertEquals("保存しない", 0, dao.saves);
            assertEquals(0, dao.withdraws);
        }
    }

    @Test
    public void invalidPropertyIdIs400() throws Exception {
        for (String value : new String[] {null, "", "abc", "0", "-2", "1.5"}) {
            PageTestHarness h = post();
            if (value == null) h.parameters.remove("propertyId"); else h.parameters.put("propertyId", value);
            FakeDao dao = new FakeDao();
            action(dao).execute(h.request, h.response);
            assertEquals("propertyId=" + value, Integer.valueOf(400), h.errorCode);
            assertEquals(0, dao.saves);
        }
    }

    @Test
    public void savingRedirectsToTheDonePage() throws Exception {
        PageTestHarness h = post();
        h.parameters.put("locationId", " 7 ");
        FakeDao dao = new FakeDao();
        action(dao).execute(h.request, h.response);

        assertEquals(BASE + "VoteDone.action?propertyId=5", h.redirect);
        assertEquals(1, dao.saves);
        assertEquals(42, dao.userId);
        assertEquals(5, dao.propertyId);
        assertEquals(Integer.valueOf(7), dao.locationId);
        assertEquals(List.of("I0101", "I0102", ""), dao.keys);
        assertEquals("週1回程度", dao.frequency);
        assertEquals("駅前に欲しいです", dao.comment);
        assertFalse("リダイレクトなのでforwardしない", h.rendered());
    }

    @Test
    public void omittedLocationMeansNearest() throws Exception {
        PageTestHarness h = post();
        FakeDao dao = new FakeDao();
        action(dao).execute(h.request, h.response);
        assertNull(dao.locationId);
        assertEquals(1, dao.saves);
    }

    @Test
    public void revotingIsMarkedAsUpdated() throws Exception {
        PageTestHarness h = post();
        FakeDao dao = new FakeDao();
        dao.stored = VoteTestData.vote(9, 5, false);
        action(dao).execute(h.request, h.response);
        assertEquals(BASE + "VoteDone.action?propertyId=5&updated=1", h.redirect);

        PageTestHarness afterWithdraw = post();
        FakeDao withdrawn = new FakeDao();
        withdrawn.stored = VoteTestData.vote(9, 5, true);
        action(withdrawn).execute(afterWithdraw.request, afterWithdraw.response);
        assertEquals("取り下げ後の投票は新規扱い", BASE + "VoteDone.action?propertyId=5", afterWithdraw.redirect);
    }

    @Test
    public void badLocationIdShowsTheFormAgainWithAMessage() throws Exception {
        for (String value : new String[] {"abc", "0", "-1"}) {
            PageTestHarness h = post();
            h.parameters.put("locationId", value);
            FakeDao dao = new FakeDao();
            action(dao).execute(h.request, h.response);
            assertEquals(value, VoteViews.FORM, h.forwardedTo);
            assertEquals(400, h.status);
            assertEquals("生活拠点が正しくありません。", h.attributes.get("errorMessage"));
            assertEquals(0, dao.saves);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void inputErrorShowsTheFormAgainKeepingWhatWasTyped() throws Exception {
        PageTestHarness h = post();
        FakeDao dao = new FakeDao();
        dao.failure = new VoteException(VoteException.BAD_CHOICES, "第1希望を選んでください。");
        action(dao).execute(h.request, h.response);

        assertNull("エラーならリダイレクトしない", h.redirect);
        assertEquals(VoteViews.FORM, h.forwardedTo);
        assertEquals(400, h.status);
        assertEquals("第1希望を選んでください。", h.attributes.get("errorMessage"));
        Map<String, String> values = (Map<String, String>) h.attributes.get("formValues");
        assertEquals("I0101", values.get("rank1"));
        assertEquals("駅前に欲しいです", values.get("comment"));
        assertEquals("画面を再表示するので、トークンも渡す", h.sessionValues.get("voteCsrfToken"), h.attributes.get("csrfToken"));
    }

    @Test
    public void closedPropertyShowsTheFormWith409() throws Exception {
        for (String code : List.of(VoteException.PROPERTY_CLOSED, VoteException.PROPERTY_NO_LOCATION, VoteException.NO_LOCATION)) {
            PageTestHarness h = post();
            FakeDao dao = new FakeDao();
            dao.failure = new VoteException(code, "投票できません。");
            action(dao).execute(h.request, h.response);
            assertEquals(code, 409, h.status);
            assertEquals(VoteViews.FORM, h.forwardedTo);
        }
    }

    @Test
    public void unknownPropertyIs404AndNonResidentIs403() throws Exception {
        PageTestHarness notFound = post();
        FakeDao dao = new FakeDao();
        dao.failure = new VoteException(VoteException.PROPERTY_NOT_FOUND, "物件が見つかりません。");
        action(dao).execute(notFound.request, notFound.response);
        assertEquals(Integer.valueOf(404), notFound.errorCode);

        PageTestHarness notResident = post();
        FakeDao other = new FakeDao();
        other.failure = new VoteException(VoteException.NOT_RESIDENT, "投票できません。");
        action(other).execute(notResident.request, notResident.response);
        assertEquals(Integer.valueOf(403), notResident.errorCode);
    }

    @Test
    public void withdrawingRedirectsToTheHistory() throws Exception {
        PageTestHarness h = post();
        h.parameters.put("withdraw", "1");
        FakeDao dao = new FakeDao();
        action(dao).execute(h.request, h.response);
        assertEquals(BASE + "VoteHistory.action?withdrawn=1", h.redirect);
        assertEquals(1, dao.withdraws);
        assertEquals(0, dao.saves);
        assertEquals(5, dao.propertyId);
    }

    @Test
    public void databaseErrorIs500WithoutDetails() throws Exception {
        PageTestHarness h = post();
        FakeDao dao = new FakeDao();
        dao.sqlFailure = new SQLException("password=secret leaked", "08006");
        action(dao).execute(h.request, h.response);
        assertEquals(Integer.valueOf(500), h.errorCode);
        assertNull(h.redirect);
    }

    /** DBへ接続せず、呼び出された内容を記録するだけのDAO。 */
    static final class FakeDao extends VoteDao {
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
            if (sqlFailure != null) throw sqlFailure;
            if (failure != null) throw failure;
            saves++;
            this.userId = userId; this.propertyId = propertyId; this.locationId = locationId;
            this.keys = keys; this.frequency = frequency; this.comment = comment;
            return VoteTestData.vote(1, propertyId, false);
        }

        @Override
        public Optional<PropertyVote> findVote(int userId, int propertyId) { return Optional.ofNullable(stored); }

        @Override
        public boolean withdrawVote(int userId, int propertyId) {
            withdraws++;
            this.propertyId = propertyId;
            return true;
        }
    }
}
