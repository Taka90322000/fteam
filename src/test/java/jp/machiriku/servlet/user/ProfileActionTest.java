package jp.machiriku.servlet.user;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import jp.machiriku.bean.ProfileSettings;
import jp.machiriku.bean.RegistrationChoice;
import jp.machiriku.bean.ResidentProfileInput;
import jp.machiriku.bean.User;
import jp.machiriku.dao.ProfileDao;

/** ProfileActionの認証チェック、CSRF検証、ロール別表示・保存処理をテストする。 */
public class ProfileActionTest {
    private static final String CSRF_TOKEN = "test-profile-csrf-token";
    private static final String RESIDENT_VIEW = "/WEB-INF/views/user/resident_profile.jsp";
    private static final String BUSINESS_VIEW = "/WEB-INF/views/user/business_profile.jsp";
    private static final String OWNER_VIEW = "/WEB-INF/views/user/owner_profile.jsp";

    @Test
    public void loggedOutUserRedirectsToLogin() throws Exception {
        Harness h = new Harness("GET");
        h.hasSession = false;
        new ProfileAction(new FakeDao()).execute(h.request, h.response);
        assertEquals("/machiriku/jp/machiriku/servlet/auth/Login.action", h.redirect);
        assertNull(h.forwarded);
    }

    @Test
    public void pendingResidentRedirectsToSetup() throws Exception {
        Harness h = new Harness("GET");
        User user = new User();
        user.setUserId(10);
        user.setUserRole("RESIDENT");
        user.setAccountStatus("PENDING");
        h.sessionValues.put("user", user);

        new ProfileAction(new FakeDao()).execute(h.request, h.response);
        assertEquals("/machiriku/jp/machiriku/servlet/auth/ResidentSetup.action", h.redirect);
        assertNull(h.forwarded);

        for (String inactiveStatus : List.of("BAN", "WITHDRAWN")) {
            Harness inactive = new Harness("GET");
            User inactiveUser = new User();
            inactiveUser.setUserId(11);
            inactiveUser.setUserRole("RESIDENT");
            inactiveUser.setAccountStatus(inactiveStatus);
            inactive.sessionValues.put("user", inactiveUser);
            new ProfileAction(new FakeDao()).execute(inactive.request, inactive.response);
            assertEquals(HttpServletResponse.SC_FORBIDDEN, inactive.status);
            assertNull(inactive.forwarded);
        }
    }

    @Test
    public void residentGetLoadsFieldsAndForwards() throws Exception {
        Harness h = authenticated("GET", "RESIDENT");
        FakeDao dao = new FakeDao();
        dao.residentFields = Map.of(
            "name", "別端末で更新済みの名前",
            "email", "updated-elsewhere@example.com",
            "ageGroup", "30代",
            "gender", "女性",
            "livingAddress", "栃木県宇都宮市中央1-1"
        );

        new ProfileAction(dao).execute(h.request, h.response);
        assertEquals(RESIDENT_VIEW, h.forwarded);
        assertNull(h.redirect);
        assertEquals("別端末で更新済みの名前", h.attributes.get("displayName"));
        assertEquals("updated-elsewhere@example.com", h.attributes.get("email"));
        assertEquals("別端末で更新済みの名前", h.sessionValues.get("userName"));
        assertEquals("updated-elsewhere@example.com", h.sessionValues.get("userEmail"));
        assertEquals("resident", h.attributes.get("role"));
        assertNotNull(h.attributes.get("csrfToken"));
        assertNotNull(h.attributes.get("residentTaxonomy"));

        @SuppressWarnings("unchecked")
        Map<String, String> values = (Map<String, String>) h.attributes.get("formValues");
        assertEquals("別端末で更新済みの名前", values.get("name"));
        assertEquals("updated-elsewhere@example.com", values.get("email"));
        assertEquals("30代", values.get("ageGroup"));
        assertEquals("女性", values.get("gender"));
        assertEquals("栃木県宇都宮市中央1-1", values.get("livingAddress"));
    }

    @Test
    public void residentPostSavesAndRedirects() throws Exception {
        Harness h = authenticated("POST", "RESIDENT");
        prepareCsrf(h);
        h.parameters.put("name", "更新後住民名");
        h.parameters.put("email", "updated-resident@example.com");
        h.parameters.put("ageGroup", "20代");
        h.parameters.put("gender", "男性");
        h.parameters.put("industry", "カフェ・喫茶店");
        h.parameters.put("preferredIndustries", "[\"I0101\"]");
        h.parameters.put("livingAddress", "栃木県宇都宮市本町2-2");
        h.parameters.put("livingLat", "36.5650");
        h.parameters.put("livingLng", "139.8850");
        h.parameters.put("emailNotify", "on");

        FakeDao dao = new FakeDao();
        new ProfileAction(dao).execute(h.request, h.response);

        assertEquals(1, dao.residentSaves);
        assertEquals(42, dao.savedUserId);
        assertEquals("更新後住民名", dao.residentInput.displayName());
        assertEquals("/machiriku/index.jsp?profile_saved=1", h.redirect);
        assertNull(h.forwarded);
        assertEquals("更新後住民名", h.sessionValues.get("userName"));
    }

    @Test
    public void businessGetLoadsFieldsAndForwards() throws Exception {
        Harness h = authenticated("GET", "BUSINESS");
        FakeDao dao = new FakeDao();
        dao.profileContact = Map.of("phone", "090-1234-5678", "secondaryPhone", "028-633-9988", "iconImageUrl", "/img/biz.png");
        dao.profileFields = Map.of("businessName", "保存済み事業所", "representativeName", "保存済み代表者", "employeeCount", "7",
                "preferredArea", "宇都宮駅周辺", "preferredAreaPoints", "[{\"lat\":36.56,\"lng\":139.88},{\"lat\":36.57,\"lng\":139.89},{\"lat\":36.56,\"lng\":139.90}]",
                "mgmtExperience", "あり", "industryExperience", "5年以上", "emailNotify", "");
        dao.selectedIndustries = Set.of("1");
        dao.selectedQualifications = Set.of("2");

        new ProfileAction(dao).execute(h.request, h.response);
        assertEquals(BUSINESS_VIEW, h.forwarded);
        assertNull(h.redirect);
        assertEquals("business", h.attributes.get("role"));
        assertNotNull(h.attributes.get("industryChoices"));
        assertNotNull(h.attributes.get("qualificationChoices"));
        assertEquals(dao.profileContact.get("phone"), ((Map<?, ?>) h.attributes.get("formValues")).get("phone"));
        assertEquals("028-633-9988", ((Map<?, ?>) h.attributes.get("formValues")).get("secondaryPhone"));
        Map<?, ?> fields = (Map<?, ?>) h.attributes.get("formValues");
        dao.profileFields.forEach((name, value) -> assertEquals(value, fields.get(name)));
        assertEquals(Set.of("1"), h.attributes.get("selectedIndustries"));
        assertEquals(Set.of("2"), h.attributes.get("selectedQualifications"));
        assertEquals("保存済み代表者", h.sessionValues.get("userName"));
    }

    @Test
    public void businessPostSavesAndRedirects() throws Exception {
        Harness h = authenticated("POST", "BUSINESS");
        prepareCsrf(h);
        h.parameters.put("businessName", "更新事業所");
        h.parameters.put("representativeName", "代表花子");
        h.parameters.put("email", "biz@example.com");
        h.parameters.put("phone", "09012345678");
        h.parameters.put("secondaryPhone", "０２８－６３３－９９８８");
        h.parameters.put("preferredArea", "宇都宮市内");
        h.parameters.put("preferredAreaPoints", "[{\"lat\":36.561234,\"lng\":139.883123},{\"lat\":36.562,\"lng\":139.884},{\"lat\":36.560,\"lng\":139.885}]");
        h.parameters.put("employeeCount", "5");
        h.parameters.put("mgmtExperience", "あり");
        h.parameters.put("industryExperience", "5年以上");
        h.multiple.put("preferredIndustry", new String[] { "1" });
        h.multiple.put("qualification", new String[] { "2" });

        FakeDao dao = new FakeDao();
        new ProfileAction(dao).execute(h.request, h.response);

        assertEquals(1, dao.businessSaves);
        assertEquals("+819012345678", dao.businessInput.phoneE164());
        assertEquals("+81286339988", dao.businessInput.secondaryPhoneE164());
        assertEquals("/machiriku/index.jsp?profile_saved=1", h.redirect);
        assertEquals("代表花子", h.sessionValues.get("userName"));
        assertEquals("更新事業所", h.sessionValues.get("businessName"));
    }

    @Test
    public void ownerGetLoadsFieldsAndForwards() throws Exception {
        Harness h = authenticated("GET", "OWNER");
        FakeDao dao = new FakeDao();
        dao.profileContact = Map.of("phone", "028-633-9988", "secondaryPhone", "090-1234-5678", "iconImageUrl", "/img/owner.png");
        dao.profileFields = Map.of("representativeName", "保存済み代表者", "company", "保存済み法人", "emailNotify", "");
        dao.selectedIndustries = Set.of("1");

        new ProfileAction(dao).execute(h.request, h.response);
        assertEquals(OWNER_VIEW, h.forwarded);
        assertNull(h.redirect);
        assertEquals("owner", h.attributes.get("role"));
        assertEquals("028-633-9988", ((Map<?, ?>) h.attributes.get("formValues")).get("phone"));
        assertEquals("090-1234-5678", ((Map<?, ?>) h.attributes.get("formValues")).get("secondaryPhone"));
        assertEquals("保存済み代表者", h.attributes.get("displayName"));
        assertEquals("保存済み代表者", h.sessionValues.get("userName"));
        assertEquals("保存済み法人", ((Map<?, ?>) h.attributes.get("formValues")).get("company"));
        assertEquals("", ((Map<?, ?>) h.attributes.get("formValues")).get("emailNotify"));
        assertEquals(Set.of("1"), h.attributes.get("selectedIndustries"));
        assertNotNull(h.attributes.get("industryChoices"));
    }

    @Test
    public void ownerPostSavesAndRedirects() throws Exception {
        Harness h = authenticated("POST", "OWNER");
        prepareCsrf(h);
        h.parameters.put("representativeName", "オーナー太郎");
        h.parameters.put("company", "宇都宮ビル管理");
        h.parameters.put("email", "owner@example.com");
        h.parameters.put("phone", "0286339988");
        h.parameters.put("secondaryPhone", "+81 90 1234 5678");
        h.multiple.put("preferredIndustry", new String[] { "1" });

        FakeDao dao = new FakeDao();
        new ProfileAction(dao).execute(h.request, h.response);

        assertEquals(1, dao.ownerSaves);
        assertEquals("+81286339988", dao.ownerInput.phoneE164());
        assertEquals("+819012345678", dao.ownerInput.secondaryPhoneE164());
        assertEquals("/machiriku/index.jsp?profile_saved=1", h.redirect);
        assertEquals("オーナー太郎", h.sessionValues.get("userName"));
        assertEquals("宇都宮ビル管理", h.sessionValues.get("company"));
    }

    @Test
    public void postWithoutCsrfTokenReturnsForbidden() throws Exception {
        Harness h = authenticated("POST", "RESIDENT");
        h.parameters.put("csrfToken", "invalid-token");
        new ProfileAction(new FakeDao()).execute(h.request, h.response);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, h.status);
    }

    @Test
    public void invalidSecondaryPhonePreservesBothInputsAndExistingSession() throws Exception {
        Harness h = authenticated("POST", "OWNER");
        prepareCsrf(h);
        h.parameters.put("representativeName", "オーナー太郎");
        h.parameters.put("phone", "028-633-9988");
        h.parameters.put("secondaryPhone", "+81 28 633 9988");
        User previous = (User) h.sessionValues.get("user");
        FakeDao dao = new FakeDao();
        new ProfileAction(dao).execute(h.request, h.response);
        assertEquals(OWNER_VIEW, h.forwarded);
        assertEquals(0, dao.ownerSaves);
        Map<?, ?> fields = (Map<?, ?>) h.attributes.get("formValues");
        assertEquals("028-633-9988", fields.get("phone"));
        assertEquals("+81 28 633 9988", fields.get("secondaryPhone"));
        assertSame(previous, h.sessionValues.get("user"));
        assertTrue(h.attributes.get("errorMessage").toString().contains("異なる番号"));
        assertEquals(0, dao.settingsReads);
    }

    @Test
    public void failedProfileReadDoesNotRenderAnEmptyEditableForm() throws Exception {
        for (String role : List.of("RESIDENT", "BUSINESS", "OWNER")) {
            Harness h = authenticated("GET", role);
            FakeDao dao = new FakeDao();
            dao.fieldsFailure = new SQLException("private database failure", "08006");
            new ProfileAction(dao).execute(h.request, h.response);
            assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, h.status);
            assertNull(h.forwarded);
            assertNull(h.attributes.get("formValues"));
        }
    }

    @Test
    public void missingProfileDoesNotRenderDefaultsThatCouldOverwriteSavedSettings() throws Exception {
        for (String role : List.of("BUSINESS", "OWNER")) {
            Harness h = authenticated("GET", role);
            FakeDao dao = new FakeDao();
            dao.profileMissing = true;
            new ProfileAction(dao).execute(h.request, h.response);
            assertEquals(HttpServletResponse.SC_NOT_FOUND, h.status);
            assertNull(h.forwarded);
        }
    }

    @Test
    public void emailNotificationGetUsesTheSavedValueForEveryRole() throws Exception {
        for (String role : List.of("RESIDENT", "BUSINESS", "OWNER")) {
            for (String notification : List.of("", "on")) {
                Harness h = authenticated("GET", role);
                FakeDao dao = new FakeDao();
                dao.residentFields = Map.of("emailNotify", notification);
                dao.profileFields = Map.of("emailNotify", notification);
                new ProfileAction(dao).execute(h.request, h.response);
                assertEquals(notification, ((Map<?, ?>) h.attributes.get("formValues")).get("emailNotify"));
            }
        }
    }

    private static Harness authenticated(String method, String role) {
        Harness h = new Harness(method);
        User user = new User();
        user.setUserId(42);
        user.setUserRole(role);
        user.setAccountStatus("ACTIVE");
        user.setDisplayName("テストユーザー");
        user.setEmail("test@example.com");
        h.sessionValues.put("user", user);
        h.sessionValues.put("userRole", role.toLowerCase(java.util.Locale.ROOT));
        return h;
    }

    private static void prepareCsrf(Harness h) {
        h.sessionValues.put("profileCsrfToken", CSRF_TOKEN);
        h.parameters.put("csrfToken", CSRF_TOKEN);
    }

    private static final class FakeDao extends ProfileDao {
        int residentSaves;
        int businessSaves;
        int ownerSaves;
        int savedUserId;
        ResidentProfileInput residentInput;
        BusinessProfileInput businessInput;
        OwnerProfileInput ownerInput;
        Map<String, String> residentFields = Map.of();
        Map<String, String> profileContact = Map.of();
        Map<String, String> profileFields = Map.of();
        Set<String> selectedIndustries = Set.of();
        Set<String> selectedQualifications = Set.of();
        SQLException fieldsFailure;
        boolean profileMissing;
        int settingsReads;

        @Override
        public Map<String, String> findResidentFields(int userId) throws SQLException {
            if (fieldsFailure != null) { throw fieldsFailure; }
            return residentFields;
        }

        @Override
        public Map<String, String> findProfileContact(int userId, String role) {
            return profileContact;
        }

        @Override
        public ProfileSettings findProfileSettings(int userId, String role) throws SQLException {
            settingsReads++;
            if (fieldsFailure != null) { throw fieldsFailure; }
            if (profileMissing) { return null; }
            Map<String, String> fields = new LinkedHashMap<>(profileFields);
            fields.putAll(profileContact);
            return new ProfileSettings(fields, selectedIndustries, selectedQualifications);
        }

        @Override
        public List<RegistrationChoice> listIndustries() {
            return List.of(new RegistrationChoice(1, "カフェ・喫茶店"));
        }

        @Override
        public List<RegistrationChoice> listQualifications() {
            return List.of(new RegistrationChoice(2, "飲食店営業許可"));
        }

        @Override
        public List<Map<String, Object>> listResidentTaxonomy() {
            return List.of(Map.of("id", "C01", "name", "飲食", "subcategories", List.of(Map.of("id", "I0101", "name", "カフェ・喫茶店"))));
        }

        @Override
        public User saveResident(int userId, ResidentProfileInput input) {
            residentSaves++;
            savedUserId = userId;
            residentInput = input;
            User u = new User();
            u.setUserId(userId);
            u.setUserRole("RESIDENT");
            u.setAccountStatus("ACTIVE");
            u.setDisplayName(input.displayName());
            u.setEmail(input.email());
            return u;
        }

        @Override
        public User saveBusiness(int userId, BusinessProfileInput input) {
            businessSaves++;
            savedUserId = userId;
            businessInput = input;
            User u = new User();
            u.setUserId(userId);
            u.setUserRole("BUSINESS");
            u.setAccountStatus("ACTIVE");
            u.setDisplayName(input.businessName());
            u.setEmail(input.email());
            return u;
        }

        @Override
        public User saveOwner(int userId, OwnerProfileInput input) {
            ownerSaves++;
            savedUserId = userId;
            ownerInput = input;
            User u = new User();
            u.setUserId(userId);
            u.setUserRole("OWNER");
            u.setAccountStatus("ACTIVE");
            u.setDisplayName(input.representativeName());
            u.setEmail(input.email());
            return u;
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
                String name = call.getName();
                if ("getAttribute".equals(name)) return sessionValues.get(arguments[0]);
                if ("setAttribute".equals(name)) { sessionValues.put((String) arguments[0], arguments[1]); return null; }
                if ("removeAttribute".equals(name)) { sessionValues.remove(arguments[0]); return null; }
                return null;
            });

            ServletContext context = proxy(ServletContext.class, (ignored, call, arguments) -> {
                if ("log".equals(call.getName())) return null;
                return null;
            });

            request = proxy(HttpServletRequest.class, (ignored, call, arguments) -> {
                String name = call.getName();
                if ("getMethod".equals(name)) return method;
                if ("getContextPath".equals(name)) return "/machiriku";
                if ("getParameter".equals(name)) return parameters.get(arguments[0]);
                if ("getParameterValues".equals(name)) return multiple.get(arguments[0]);
                if ("getAttribute".equals(name)) return attributes.get(arguments[0]);
                if ("setAttribute".equals(name)) { attributes.put((String) arguments[0], arguments[1]); return null; }
                if ("getSession".equals(name)) {
                    return hasSession ? session : null;
                }
                if ("setCharacterEncoding".equals(name)) return null;
                if ("getServletContext".equals(name)) return context;
                if ("getRequestDispatcher".equals(name)) {
                    String view = (String) arguments[0];
                    return proxy(RequestDispatcher.class, (disp, dispCall, dispArgs) -> {
                        if ("forward".equals(dispCall.getName())) forwarded = view;
                        return null;
                    });
                }
                return null;
            });

            response = proxy(HttpServletResponse.class, (ignored, call, arguments) -> {
                String name = call.getName();
                if ("sendRedirect".equals(name)) { redirect = (String) arguments[0]; return null; }
                if ("sendError".equals(name)) { status = (Integer) arguments[0]; return null; }
                if ("setHeader".equals(name)) return null;
                return null;
            });
        }

        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
        }
    }
}
