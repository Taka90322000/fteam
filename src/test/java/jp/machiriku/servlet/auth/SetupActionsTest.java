package jp.machiriku.servlet.auth;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.BusinessProfileInput;
import jp.machiriku.bean.OwnerProfileInput;
import jp.machiriku.bean.RegistrationChoice;
import jp.machiriku.bean.ResidentProfileInput;
import jp.machiriku.bean.User;
import jp.machiriku.dao.ProfileDao;
import jp.machiriku.tool.Action;

/** 初期設定Actionの認証・CSRF・保存成功時だけのセッション更新を確認する。 */
public class SetupActionsTest {
    private static final String TOKEN = "test-onboarding-csrf-token";
    private static final String RESIDENT_VIEW = "/WEB-INF/views/auth/resident_setup.jsp";
    private static final String BUSINESS_VIEW = "/WEB-INF/views/auth/business_setup.jsp";
    private static final String OWNER_VIEW = "/WEB-INF/views/auth/owner_setup.jsp";
    private static final String TRIANGLE = "[{\"lat\":36.561,\"lng\":139.883},"
            + "{\"lat\":36.562,\"lng\":139.884},{\"lat\":36.560,\"lng\":139.885}]";

    @Test
    public void residentGetPrefillsOnlyVerifiedSessionSnapshotsAndCreatesCsrf() throws Exception {
        Harness request = authenticated("GET", "RESIDENT");
        request.sessionValues.put("lineDisplayName", "検証済みLINE表示名");
        request.sessionValues.put("lineEmail", "verified@example.com");
        request.parameters.put("displayName", "不正なリクエスト名");
        request.parameters.put("email", "attacker@example.com");
        FakeDao dao = new FakeDao();
        new ResidentAction(dao).execute(request.request, request.response);
        assertEquals(RESIDENT_VIEW, request.forwarded);
        assertEquals("検証済みLINE表示名", request.attributes.get("displayName"));
        assertEquals("verified@example.com", request.attributes.get("email"));
        assertEquals("検証済みLINE表示名", values(request).get("name"));
        assertEquals("verified@example.com", values(request).get("email"));
        assertNotNull(request.sessionValues.get("onboardingCsrfToken"));
        assertFalse(request.sessionValues.get("onboardingCsrfToken").toString().isBlank());
        assertEquals(request.sessionValues.get("onboardingCsrfToken"), request.attributes.get("csrfToken"));
        assertEquals(0, dao.industryReads);
        assertEquals(0, dao.qualificationReads);
    }

    @Test
    public void businessGetLoadsMasterOptionsButDoesNotGuessRepresentativeName() throws Exception {
        Harness request = authenticated("GET", "BUSINESS");
        request.sessionValues.put("lineDisplayName", "LINEの事業表示名");
        request.sessionValues.put("lineEmail", "line-business@example.com");
        request.parameters.put("representativeName", "不正な代表者名");
        FakeDao dao = new FakeDao();
        dao.profileContact = Map.of("phone", "+819012345678", "iconImageUrl", "/assets/business.png");
        new BusinessAction(dao).execute(request.request, request.response);
        assertEquals(BUSINESS_VIEW, request.forwarded);
        assertEquals("", request.attributes.get("displayName"));
        assertEquals("line-business@example.com", request.attributes.get("email"));
        assertEquals("", values(request).get("businessName"));
        assertEquals("", values(request).get("representativeName"));
        assertEquals("+819012345678", values(request).get("phone"));
        assertEquals("/assets/business.png", values(request).get("iconImageUrl"));
        assertEquals(42, dao.contactUserId);
        assertEquals("BUSINESS", dao.contactRole);
        assertEquals(1, dao.industryReads);
        assertEquals(1, dao.qualificationReads);
        assertEquals(dao.industries, request.attributes.get("industryChoices"));
        assertEquals(dao.qualifications, request.attributes.get("qualificationChoices"));
        assertTrue(request.attributes.get("selectedIndustries") instanceof Set);
        assertTrue(request.attributes.get("selectedQualifications") instanceof Set);
        assertEquals(request.sessionValues.get("onboardingCsrfToken"), request.attributes.get("csrfToken"));
    }

    @Test
    // オーナーの初期値もURLから受け取らず、今回検証したLINE情報と保存済み電話番号を使う。
    public void ownerGetPrefillsLineSnapshotsAndLoadsAttractionIndustries() throws Exception {
        Harness request = authenticated("GET", "OWNER");
        request.sessionValues.put("lineDisplayName", "検証済みオーナー名");
        request.sessionValues.put("lineEmail", "line-owner@example.invalid");
        request.parameters.put("displayName", "不正な名前");
        request.parameters.put("email", "attacker@example.invalid");
        FakeDao dao = new FakeDao();
        dao.profileContact = Map.of("phone", "+81286339988", "iconImageUrl", "https://example.com/owner.png");
        new OwnerAction(dao).execute(request.request, request.response);
        assertEquals(OWNER_VIEW, request.forwarded);
        assertEquals("検証済みオーナー名", request.attributes.get("displayName"));
        assertEquals("検証済みオーナー名", values(request).get("representativeName"));
        assertEquals("line-owner@example.invalid", values(request).get("email"));
        assertEquals("", values(request).get("company"));
        assertEquals("+81286339988", values(request).get("phone"));
        assertEquals("https://example.com/owner.png", values(request).get("iconImageUrl"));
        assertEquals(42, dao.contactUserId);
        assertEquals("OWNER", dao.contactRole);
        assertEquals(1, dao.industryReads);
        assertEquals(0, dao.qualificationReads);
        assertEquals(dao.industries, request.attributes.get("industryChoices"));
        assertEquals(request.sessionValues.get("onboardingCsrfToken"), request.attributes.get("csrfToken"));
    }

    @Test
    public void ownerExplicitUnavailableLineEmailDoesNotUseOldEmailOrDemoValues() throws Exception {
        Harness request = authenticated("GET", "OWNER");
        request.sessionValues.put("lineDisplayName", "LINEオーナー");
        request.sessionValues.put("lineEmail", "");
        new OwnerAction(new FakeDao()).execute(request.request, request.response);
        assertEquals("LINEオーナー", values(request).get("representativeName"));
        assertEquals("", values(request).get("email"));
        assertEquals("", values(request).get("company"));
        assertEquals("", values(request).get("phone"));
        assertEquals("", values(request).get("iconImageUrl"));
    }

    @Test
    public void profileContactFailurePreservesLinePrefillAndReportsError() throws Exception {
        for (String role : List.of("OWNER", "BUSINESS")) {
            Harness request = authenticated("GET", role);
            request.sessionValues.put("lineEmail", "verified@example.invalid");
            FakeDao dao = new FakeDao();
            dao.contactFailure = new SQLException("private database details", "42501");
            Action action = "OWNER".equals(role) ? new OwnerAction(dao) : new BusinessAction(dao);
            action.execute(request.request, request.response);
            assertEquals("verified@example.invalid", values(request).get("email"));
            assertEquals("", values(request).get("phone"));
            assertEquals("", values(request).get("iconImageUrl"));
            assertNotNull(request.attributes.get("errorMessage"));
            assertFalse(request.attributes.get("errorMessage").toString().contains("private database"));
            assertEquals(0, dao.saves);
            assertNull(request.sessionValues.get("isOnboarded"));
        }
    }

    @Test
    public void oldSessionsFallbackToStoredUserButExplicitUnavailableLineEmailStaysEmpty() throws Exception {
        Harness request = authenticated("GET", "RESIDENT");
        new ResidentAction(new FakeDao()).execute(request.request, request.response);
        assertEquals("保存済み表示名", request.attributes.get("displayName"));
        assertEquals("stored@example.com", request.attributes.get("email"));
        request = authenticated("GET", "RESIDENT");
        request.sessionValues.put("lineDisplayName", "今回のLINE表示名");
        request.sessionValues.put("lineEmail", "");
        new ResidentAction(new FakeDao()).execute(request.request, request.response);
        assertEquals("今回のLINE表示名", request.attributes.get("displayName"));
        assertEquals("", request.attributes.get("email"));
        assertEquals("", values(request).get("email"));
    }

    @Test
    public void loggedOutRequestsRedirectToLoginBeforeCreatingDao() throws Exception {
        for (boolean business : new boolean[] { false, true }) {
            for (boolean hasSession : new boolean[] { false, true }) {
                Harness request = new Harness("GET");
                request.hasSession = hasSession;
                request.sessionValues.put("userRole", business ? "business" : "resident");
                FakeDao dao = new FakeDao();
                Action action = action(business, dao);
                action.execute(request.request, request.response);
                assertEquals("/machiriku/jp/machiriku/servlet/auth/Login.action", request.redirect);
                assertEquals(0, createdDaos(action));
                assertEquals(0, dao.saves);
            }
        }
    }

    @Test
    public void wrongRoleCannotReadOrSaveSetupForAnotherRole() throws Exception {
        for (boolean business : new boolean[] { false, true }) {
            for (String method : new String[] { "GET", "POST" }) {
                Harness request = authenticated(method, business ? "RESIDENT" : "BUSINESS");
                prepareCsrf(request);
                Action action = action(business, new FakeDao());
                action.execute(request.request, request.response);
                assertEquals(403, request.status);
                assertEquals(0, createdDaos(action));
            }
        }
    }

    @Test
    public void missingOrMismatchedCsrfRejectsBeforeDaoCreation() throws Exception {
        for (boolean business : new boolean[] { false, true }) {
            for (String csrf : new String[] { null, "", "forged-token" }) {
                Harness request = authenticated("POST", business ? "BUSINESS" : "RESIDENT");
                request.sessionValues.put("onboardingCsrfToken", TOKEN);
                if (csrf != null) { request.parameters.put("csrfToken", csrf); }
                Action action = action(business, new FakeDao());
                action.execute(request.request, request.response);
                assertEquals(403, request.status);
                assertEquals(0, createdDaos(action));
                assertNull(request.sessionValues.get("isOnboarded"));
            }
            Harness request = authenticated("POST", business ? "BUSINESS" : "RESIDENT");
            request.parameters.put("csrfToken", TOKEN);
            Action action = action(business, new FakeDao());
            action.execute(request.request, request.response);
            assertEquals(403, request.status);
            assertEquals(0, createdDaos(action));
        }
    }

    @Test
    public void ownerRequiresAuthenticatedOwnerAndValidCsrfBeforeAnyDao() throws Exception {
        Harness loggedOut = new Harness("GET");
        OwnerAction noLogin = new OwnerAction(new FakeDao());
        noLogin.execute(loggedOut.request, loggedOut.response);
        assertEquals("/machiriku/jp/machiriku/servlet/auth/Login.action", loggedOut.redirect);
        assertEquals(0, noLogin.created);
        for (String role : new String[] { "RESIDENT", "BUSINESS" }) {
            for (String method : new String[] { "GET", "POST" }) {
                Harness request = authenticated(method, role);
                prepareCsrf(request);
                OwnerAction action = new OwnerAction(new FakeDao());
                action.execute(request.request, request.response);
                assertEquals(403, request.status);
                assertEquals(0, action.created);
            }
        }
        for (String token : new String[] { null, "", "forged-token" }) {
            Harness request = ownerPost();
            if (token == null) { request.parameters.remove("csrfToken"); }
            else { request.parameters.put("csrfToken", token); }
            OwnerAction action = new OwnerAction(new FakeDao());
            action.execute(request.request, request.response);
            assertEquals(403, request.status);
            assertEquals(0, action.created);
            assertNull(request.sessionValues.get("isOnboarded"));
        }
        Harness noSessionToken = ownerPost();
        noSessionToken.sessionValues.remove("onboardingCsrfToken");
        OwnerAction action = new OwnerAction(new FakeDao());
        action.execute(noSessionToken.request, noSessionToken.response);
        assertEquals(403, noSessionToken.status);
        assertEquals(0, action.created);
    }

    @Test
    // プレビュー表示用のクエリがあっても、POSTで架空の会員IDへ保存できないことを確認する。
    public void residentPreviewParameterCannotBypassAuthenticationOnPost() throws Exception {
        Harness request = new Harness("POST");
        request.parameters.put("preview", "1");
        request.parameters.put("userId", "1");
        prepareCsrf(request);
        FakeDao dao = new FakeDao();
        ResidentAction action = new ResidentAction(dao);
        action.execute(request.request, request.response);
        assertEquals("/machiriku/jp/machiriku/servlet/auth/Login.action", request.redirect);
        assertEquals(0, action.created);
        assertEquals(0, dao.saves);

        // ログイン済みのPOSTでも、プレビュー用の架空IDに差し替えず認証した会員へ保存する。
        Harness loggedIn = residentPost();
        loggedIn.parameters.put("preview", "true");
        FakeDao loggedInDao = new FakeDao();
        new ResidentAction(loggedInDao).execute(loggedIn.request, loggedIn.response);
        assertEquals(42, loggedInDao.savedUserId);
        assertEquals(1, loggedInDao.saves);
        assertEquals("/machiriku/index.jsp?onboarded=1", loggedIn.redirect);
    }

    @Test
    public void residentPostPersistsValidatedInputThenUpdatesSession() throws Exception {
        Harness request = residentPost();
        request.parameters.put("email", " ");
        User previous = (User) request.sessionValues.get("user");
        FakeDao dao = new FakeDao();
        new ResidentAction(dao).execute(request.request, request.response);
        assertEquals(1, dao.saves);
        assertEquals(42, dao.savedUserId);
        assertEquals("新しい住民名", dao.residentInput.displayName());
        assertNull(dao.residentInput.email());
        assertNull(dao.residentInput.occupation());
        assertNotNull(dao.residentInput.home());
        assertNull(dao.residentInput.work());
        assertEquals(Boolean.TRUE, request.sessionValues.get("isOnboarded"));
        assertSame(dao.savedUser, request.sessionValues.get("user"));
        assertNotSame(previous, request.sessionValues.get("user"));
        assertEquals("新しい住民名", ((User) request.sessionValues.get("user")).getDisplayName());
        assertNull(((User) request.sessionValues.get("user")).getEmail());
        assertEquals("/machiriku/index.jsp?onboarded=1", request.redirect);
        assertNull(request.forwarded);
    }

    @Test
    public void residentPostPassesDesiredIndustryCodesToDatabaseAndIgnoresLabels() throws Exception {
        Harness request = residentPost();
        request.parameters.put("preferredIndustries", "[\"I0101\",\"I0201\",\"I0101\"]");
        request.parameters.put("industry", "自由な表示名では保存しない");
        request.parameters.put("userId", "1");
        FakeDao dao = new FakeDao();
        new ResidentAction(dao).execute(request.request, request.response);
        assertEquals(42, dao.savedUserId);
        assertEquals(List.of("I0101", "I0201"), dao.residentInput.preferredIndustryCodes());
        assertEquals("/machiriku/index.jsp?onboarded=1", request.redirect);
    }

    @Test
    public void ownerPostSavesValidatedFieldsAndCompletesSessionOnlyAfterSave() throws Exception {
        Harness request = ownerPost();
        request.parameters.put("userId", "1");
        request.parameters.put("userRole", "admin");
        request.parameters.put("email", " ");
        FakeDao dao = new FakeDao();
        User previous = (User) request.sessionValues.get("user");
        new OwnerAction(dao).execute(request.request, request.response);
        assertEquals(1, dao.saves);
        assertEquals(42, dao.savedUserId);
        assertEquals("入力した代表者名", dao.ownerInput.representativeName());
        assertEquals("テスト不動産", dao.ownerInput.company());
        assertNull(dao.ownerInput.email());
        assertEquals("+81286339988", dao.ownerInput.phoneE164());
        assertEquals("https://example.com/owner.png", dao.ownerInput.iconImageUrl());
        assertEquals("https://example.com/owner.png", request.sessionValues.get("iconImageUrl"));
        assertEquals("+81286339988", request.sessionValues.get("phone"));
        assertEquals(List.of(1, 2), dao.ownerInput.industryTypeIds());
        assertEquals("OWNER", dao.savedUser.getUserRole());
        assertEquals("保存済み表示名", dao.savedUser.getDisplayName());
        assertSame(dao.savedUser, request.sessionValues.get("user"));
        assertNotSame(previous, request.sessionValues.get("user"));
        assertEquals("入力した代表者名", request.sessionValues.get("userName"));
        assertEquals(Boolean.TRUE, request.sessionValues.get("isOnboarded"));
        assertNull(request.sessionValues.get("onboardingCsrfToken"));
        assertEquals("/machiriku/index.jsp?onboarded=1", request.redirect);
        assertNull(request.forwarded);
    }

    @Test
    public void businessPostPersistsNormalizedPhoneMasterIdsAndPolygonBeforeSessionUpdate() throws Exception {
        Harness request = businessPost();
        FakeDao dao = new FakeDao();
        new BusinessAction(dao).execute(request.request, request.response);
        assertEquals(1, dao.saves);
        assertEquals(42, dao.savedUserId);
        assertEquals("新しい事業所", dao.businessInput.businessName());
        assertEquals("代表者氏名", dao.businessInput.representativeName());
        assertEquals("+819012345678", dao.businessInput.phoneE164());
        assertEquals("/assets/business.png", dao.businessInput.iconImageUrl());
        assertEquals("/assets/business.png", request.sessionValues.get("iconImageUrl"));
        assertEquals("+819012345678", request.sessionValues.get("phone"));
        assertEquals(List.of(1), dao.businessInput.industryTypeIds());
        assertEquals(List.of(2), dao.businessInput.qualificationIds());
        assertTrue(dao.businessInput.areaGeoJson().contains("Polygon"));
        assertEquals(Boolean.TRUE, request.sessionValues.get("isOnboarded"));
        assertSame(dao.savedUser, request.sessionValues.get("user"));
        assertEquals("/machiriku/index.jsp?onboarded=1", request.redirect);
        assertNull(request.forwarded);
    }

    @Test
    public void invalidResidentInputPreservesPostAndDoesNotMarkSessionComplete() throws Exception {
        Harness request = residentPost();
        request.parameters.put("name", " ");
        request.parameters.put("email", "not-an-email");
        request.parameters.remove("emailNotify");
        User previous = (User) request.sessionValues.get("user");
        FakeDao dao = new FakeDao();
        new ResidentAction(dao).execute(request.request, request.response);
        assertEquals(RESIDENT_VIEW, request.forwarded);
        assertEquals(" ", values(request).get("name"));
        assertEquals("not-an-email", values(request).get("email"));
        assertFalse(values(request).containsKey("emailNotify"));
        assertNotNull(request.attributes.get("errorMessage"));
        assertEquals(0, dao.saves);
        assertSame(previous, request.sessionValues.get("user"));
        assertNull(request.sessionValues.get("isOnboarded"));
        assertNull(request.redirect);
    }

    @Test
    public void invalidResidentDesiredIndustriesRemainVisibleAndAreNotSaved() throws Exception {
        Harness request = residentPost();
        request.parameters.put("preferredIndustries", "[\"not-a-code\"]");
        request.parameters.remove("emailNotify");
        User previous = (User) request.sessionValues.get("user");
        FakeDao dao = new FakeDao();
        new ResidentAction(dao).execute(request.request, request.response);
        assertEquals(RESIDENT_VIEW, request.forwarded);
        assertEquals("[\"not-a-code\"]", values(request).get("preferredIndustries"));
        assertFalse(values(request).containsKey("emailNotify"));
        assertNotNull(request.attributes.get("errorMessage"));
        assertEquals(0, dao.saves);
        assertSame(previous, request.sessionValues.get("user"));
        assertNull(request.sessionValues.get("isOnboarded"));
    }

    @Test
    // DB上で業種が無効になっていた場合も、再選択できるよう入力と既存セッションを保持する。
    public void residentSqlFailurePreservesDesiredIndustryCodesAndPreviousSession() throws Exception {
        Harness request = residentPost();
        String selectedCodes = "[\"I0101\",\"I0201\"]";
        request.parameters.put("preferredIndustries", selectedCodes);
        List<String> previousCodes = List.of("I0301");
        request.sessionValues.put("userPreferredIndustries", previousCodes);
        User previous = (User) request.sessionValues.get("user");
        FakeDao dao = new FakeDao();
        dao.failure = new SQLException("private inactive industry details", "23514");
        new ResidentAction(dao).execute(request.request, request.response);
        assertEquals(RESIDENT_VIEW, request.forwarded);
        assertEquals(selectedCodes, values(request).get("preferredIndustries"));
        assertEquals(List.of("I0101", "I0201"), dao.residentInput.preferredIndustryCodes());
        assertSame(previous, request.sessionValues.get("user"));
        assertSame(previousCodes, request.sessionValues.get("userPreferredIndustries"));
        assertNull(request.sessionValues.get("isOnboarded"));
        assertNotNull(request.attributes.get("errorMessage"));
        assertFalse(request.attributes.get("errorMessage").toString().contains("private inactive"));
        assertNull(request.redirect);
    }

    @Test
    public void ownerValidationErrorPreservesPostValuesAndSelectionsWithoutSave() throws Exception {
        Harness request = ownerPost();
        request.parameters.put("representativeName", " ");
        request.parameters.put("company", "入力中の会社名");
        request.parameters.remove("emailNotify");
        FakeDao dao = new FakeDao();
        User previous = (User) request.sessionValues.get("user");
        new OwnerAction(dao).execute(request.request, request.response);
        assertEquals(OWNER_VIEW, request.forwarded);
        assertEquals(" ", values(request).get("representativeName"));
        assertEquals("入力中の会社名", values(request).get("company"));
        assertEquals("https://example.com/owner.png", values(request).get("iconImageUrl"));
        assertFalse(values(request).containsKey("emailNotify"));
        assertSelected(request.attributes.get("selectedIndustries"), 1);
        assertSelected(request.attributes.get("selectedIndustries"), 2);
        assertEquals(0, dao.saves);
        assertSame(previous, request.sessionValues.get("user"));
        assertNull(request.sessionValues.get("isOnboarded"));
    }

    @Test
    public void ownerSqlErrorPreservesInputsAndDoesNotExposeDatabaseDetails() throws Exception {
        Harness request = ownerPost();
        FakeDao dao = new FakeDao();
        dao.failure = new SQLException("private connection and email details", "23505");
        User previous = (User) request.sessionValues.get("user");
        new OwnerAction(dao).execute(request.request, request.response);
        assertEquals(OWNER_VIEW, request.forwarded);
        assertEquals("入力した代表者名", values(request).get("representativeName"));
        assertEquals("テスト不動産", values(request).get("company"));
        assertEquals("https://example.com/owner.png", values(request).get("iconImageUrl"));
        assertSelected(request.attributes.get("selectedIndustries"), 1);
        assertNotNull(request.attributes.get("errorMessage"));
        assertFalse(request.attributes.get("errorMessage").toString().contains("private connection"));
        assertSame(previous, request.sessionValues.get("user"));
        assertNull(request.sessionValues.get("isOnboarded"));
        assertNull(request.redirect);
    }

    @Test
    public void sqlFailurePreservesBusinessValuesSelectionsAndPreviousSessionWithoutLeakingDetails() throws Exception {
        Harness request = businessPost();
        request.parameters.remove("emailNotify");
        User previous = (User) request.sessionValues.get("user");
        FakeDao dao = new FakeDao();
        dao.failure = new SQLException("private email and database details", "23505");
        new BusinessAction(dao).execute(request.request, request.response);
        assertEquals(BUSINESS_VIEW, request.forwarded);
        assertEquals("新しい事業所", values(request).get("businessName"));
        assertEquals("/assets/business.png", values(request).get("iconImageUrl"));
        assertEquals(TRIANGLE, values(request).get("preferredAreaPoints"));
        assertFalse(values(request).containsKey("emailNotify"));
        assertSelected(request.attributes.get("selectedIndustries"), 1);
        assertSelected(request.attributes.get("selectedQualifications"), 2);
        assertNotNull(request.attributes.get("errorMessage"));
        assertFalse(request.attributes.get("errorMessage").toString().contains("private email"));
        assertSame(previous, request.sessionValues.get("user"));
        assertNull(request.sessionValues.get("isOnboarded"));
        assertNull(request.redirect);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> values(Harness request) {
        assertTrue(request.attributes.get("formValues") instanceof Map);
        return (Map<String, String>) request.attributes.get("formValues");
    }

    private static void assertSelected(Object selected, int id) {
        assertTrue(selected instanceof Set);
        Set<?> set = (Set<?>) selected;
        assertTrue(set.contains(id) || set.contains(Integer.toString(id)));
    }

    private static Harness authenticated(String method, String role) {
        Harness request = new Harness(method);
        User user = new User();
        user.setUserId(42);
        user.setUserRole(role);
        user.setAccountStatus("ACTIVE");
        user.setDisplayName("保存済み表示名");
        user.setEmail("stored@example.com");
        request.sessionValues.put("user", user);
        request.sessionValues.put("userRole", role.toLowerCase(java.util.Locale.ROOT));
        return request;
    }

    private static void prepareCsrf(Harness request) {
        request.sessionValues.put("onboardingCsrfToken", TOKEN);
        request.parameters.put("csrfToken", TOKEN);
    }

    private static Harness residentPost() {
        Harness request = authenticated("POST", "RESIDENT");
        prepareCsrf(request);
        request.parameters.put("name", "新しい住民名");
        request.parameters.put("email", "new-resident@example.com");
        request.parameters.put("ageGroup", "未設定");
        request.parameters.put("gender", "回答しない");
        request.parameters.put("industry", "");
        request.parameters.put("livingAddress", "栃木県宇都宮市本町1-1");
        request.parameters.put("livingLat", "36.561");
        request.parameters.put("livingLng", "139.883");
        request.parameters.put("emailNotify", "on");
        return request;
    }

    private static Harness businessPost() {
        Harness request = authenticated("POST", "BUSINESS");
        prepareCsrf(request);
        request.parameters.put("businessName", "新しい事業所");
        request.parameters.put("representativeName", "代表者氏名");
        request.parameters.put("email", "new-business@example.com");
        request.parameters.put("phone", "090-1234-5678");
        request.parameters.put("iconImageUrl", "/assets/business.png");
        request.parameters.put("preferredArea", "宇都宮市本町周辺");
        request.parameters.put("preferredAreaPoints", TRIANGLE);
        request.parameters.put("employeeCount", "3");
        request.parameters.put("mgmtExperience", "なし");
        request.parameters.put("industryExperience", "未経験");
        request.parameters.put("emailNotify", "on");
        request.multiple.put("preferredIndustry", new String[] { "1" });
        request.multiple.put("qualification", new String[] { "2" });
        return request;
    }

    private static Harness ownerPost() {
        Harness request = authenticated("POST", "OWNER");
        prepareCsrf(request);
        request.parameters.put("representativeName", "入力した代表者名");
        request.parameters.put("company", "テスト不動産");
        request.parameters.put("email", "owner@example.invalid");
        request.parameters.put("phone", "028-633-9988");
        request.parameters.put("iconImageUrl", "https://example.com/owner.png");
        request.parameters.put("emailNotify", "on");
        request.multiple.put("preferredIndustry", new String[] { "1", "2" });
        return request;
    }

    private static Action action(boolean business, FakeDao dao) {
        return business ? new BusinessAction(dao) : new ResidentAction(dao);
    }

    private static int createdDaos(Action action) {
        return action instanceof BusinessAction business ? business.created : ((ResidentAction) action).created;
    }

    private static final class ResidentAction extends ResidentSetupAction {
        final FakeDao dao;
        int created;
        ResidentAction(FakeDao dao) { this.dao = dao; }
        @Override protected ProfileDao createProfileDao() { created++; return dao; }
    }

    private static final class BusinessAction extends BusinessSetupAction {
        final FakeDao dao;
        int created;
        BusinessAction(FakeDao dao) { this.dao = dao; }
        @Override protected ProfileDao createProfileDao() { created++; return dao; }
    }

    private static final class OwnerAction extends OwnerSetupAction {
        final FakeDao dao;
        int created;
        OwnerAction(FakeDao dao) { this.dao = dao; }
        @Override protected ProfileDao createProfileDao() { created++; return dao; }
    }

    private static final class FakeDao extends ProfileDao {
        final List<RegistrationChoice> industries = List.of(new RegistrationChoice(1, "飲食店"));
        final List<RegistrationChoice> qualifications = List.of(new RegistrationChoice(2, "営業許可"));
        int industryReads;
        int qualificationReads;
        int saves;
        int savedUserId;
        User savedUser;
        ResidentProfileInput residentInput;
        BusinessProfileInput businessInput;
        OwnerProfileInput ownerInput;
        SQLException failure;
        SQLException contactFailure;
        Map<String, String> profileContact = Map.of();
        int contactUserId;
        String contactRole;

        @Override public Map<String, String> findProfileContact(int userId, String role) throws SQLException {
            contactUserId = userId; contactRole = role;
            if (contactFailure != null) { throw contactFailure; }
            return profileContact;
        }
        @Override public List<RegistrationChoice> listIndustries() { industryReads++; return industries; }
        @Override public List<RegistrationChoice> listQualifications() { qualificationReads++; return qualifications; }
        @Override public User saveResident(int userId, ResidentProfileInput input) throws SQLException {
            saves++; savedUserId = userId; residentInput = input;
            if (failure != null) { throw failure; }
            return saved(userId, "RESIDENT", input.displayName(), input.email());
        }
        @Override public User saveBusiness(int userId, BusinessProfileInput input) throws SQLException {
            saves++; savedUserId = userId; businessInput = input;
            if (failure != null) { throw failure; }
            return saved(userId, "BUSINESS", input.businessName(), input.email());
        }
        @Override public User saveOwner(int userId, OwnerProfileInput input) throws SQLException {
            saves++; savedUserId = userId; ownerInput = input;
            if (failure != null) { throw failure; }
            return saved(userId, "OWNER", "保存済み表示名", input.email());
        }
        private User saved(int id, String role, String name, String email) {
            savedUser = new User();
            savedUser.setUserId(id);
            savedUser.setUserRole(role);
            savedUser.setAccountStatus("ACTIVE");
            savedUser.setDisplayName(name);
            savedUser.setEmail(email);
            return savedUser;
        }
    }

    private static final class Harness {
        final Map<String, String> parameters = new HashMap<>();
        final Map<String, String[]> multiple = new HashMap<>();
        final Map<String, Object> attributes = new HashMap<>();
        final Map<String, Object> sessionValues = new HashMap<>();
        boolean hasSession = true;
        String redirect;
        String forwarded;
        int status;
        final HttpSession session;
        final HttpServletRequest request;
        final HttpServletResponse response;

        Harness(String method) {
            session = proxy(HttpSession.class, (ignored, call, arguments) -> {
                return switch (call.getName()) {
                    case "getAttribute" -> sessionValues.get(arguments[0]);
                    case "setAttribute" -> { sessionValues.put((String) arguments[0], arguments[1]); yield null; }
                    case "removeAttribute" -> { sessionValues.remove(arguments[0]); yield null; }
                    default -> defaultValue(call.getReturnType());
                };
            });
            request = proxy(HttpServletRequest.class, (ignored, call, arguments) -> {
                return switch (call.getName()) {
                    case "getMethod" -> method;
                    case "getContextPath" -> "/machiriku";
                    case "getParameter" -> parameters.get(arguments[0]);
                    case "getParameterValues" -> multiple.containsKey(arguments[0]) ? multiple.get(arguments[0])
                            : parameters.containsKey(arguments[0]) ? new String[] { parameters.get(arguments[0]) } : null;
                    case "getParameterMap" -> {
                        Map<String, String[]> result = new HashMap<>(multiple);
                        parameters.forEach((key, value) -> result.put(key, new String[] { value }));
                        yield result;
                    }
                    case "getParameterNames" -> Collections.enumeration(parameters.keySet());
                    case "getAttribute" -> attributes.get(arguments[0]);
                    case "setAttribute" -> { attributes.put((String) arguments[0], arguments[1]); yield null; }
                    case "getSession" -> hasSession ? session : null;
                    case "getRequestDispatcher" -> {
                        String path = (String) arguments[0];
                        yield proxy(RequestDispatcher.class, (dispatcher, dispatch, dispatchArguments) -> {
                            if (dispatch.getName().equals("forward")) { forwarded = path; }
                            return defaultValue(dispatch.getReturnType());
                        });
                    }
                    case "getServletContext" -> proxy(ServletContext.class,
                            (context, contextCall, contextArguments) -> defaultValue(contextCall.getReturnType()));
                    default -> defaultValue(call.getReturnType());
                };
            });
            response = proxy(HttpServletResponse.class, (ignored, call, arguments) -> {
                return switch (call.getName()) {
                    case "sendRedirect" -> { redirect = (String) arguments[0]; yield null; }
                    case "sendError", "setStatus" -> { status = (Integer) arguments[0]; yield null; }
                    default -> defaultValue(call.getReturnType());
                };
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) { return false; }
        if (type == int.class) { return 0; }
        if (type == long.class) { return 0L; }
        if (type == double.class) { return 0d; }
        return null;
    }
}
