package jp.machiriku.servlet.auth;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.auth.LineLoginClient;
import jp.machiriku.auth.LineLoginConfig;
import jp.machiriku.bean.User;
import jp.machiriku.dao.UserDao;

/** 実際のLINEやDBを使わず、Servletとセッションの代役で認証Actionを検証する。 */
public class LineAuthActionsTest {
    private static final String LOGIN = "/WEB-INF/views/auth/login.jsp";
    private static final String REGISTER = "/WEB-INF/views/auth/register.jsp";

    @Test
    // 本登録済み会員とデモ利用者は、認証入口からトップへ案内する。
    public void activeAndDemoSessionsRedirectAwayFromLoginRegistrationAndLineStart() throws Exception {
        for (String method : new String[] { "GET", "POST" }) {
            for (boolean demo : new boolean[] { false, true }) {
                Harness login = authenticated(method, "OWNER", "ACTIVE");
                Harness register = authenticated(method, "OWNER", "ACTIVE");
                Harness start = authenticated(method, "OWNER", "ACTIVE");
                if (demo) {
                    for (Harness request : new Harness[] { login, register, start }) {
                        request.session.attributes.remove("user");
                        request.session.attributes.put("userRole", "owner");
                    }
                }
                start.parameters.put("regRole", "resident");
                new LoginAction().execute(login.request, login.response);
                new RegisterAction().execute(register.request, register.response);
                TestStart action = new TestStart(new FakeClient());
                action.execute(start.request, start.response);
                for (Harness request : new Harness[] { login, register, start }) {
                    assertEquals("/machiriku/index.jsp", request.redirect);
                    assertNull(request.forwarded);
                    assertEquals("no-store", request.headers.get("Cache-Control"));
                }
                assertEquals(0, action.clientsCreated);
                assertEquals(0, action.daosCreated);
            }
        }
    }

    @Test
    // 匿名の登録画面にもトークンを付け、認証後にブラウザーで戻る操作に備える。
    public void anonymousEntryScreensRemainAvailableAndRegistrationPreparesCsrf() throws Exception {
        Harness login = new Harness("GET");
        login.session = null;
        new LoginAction().execute(login.request, login.response);
        assertEquals(LOGIN, login.forwarded);
        assertNull(login.redirect);
        Harness register = new Harness("GET");
        register.session = null;
        new RegisterAction().execute(register.request, register.response);
        assertEquals(REGISTER, register.forwarded);
        assertNull(register.redirect);
        assertEquals(Boolean.FALSE, register.attributes.get("registrationPending"));
        assertEquals("resident", register.attributes.get("registrationRole"));
        assertNotNull(register.attributes.get("csrfToken"));
        assertEquals(register.attributes.get("csrfToken"), register.session.attributes.get(LineLoginSupport.REGISTRATION_CSRF_KEY));
        assertEquals("no-store", register.headers.get("Cache-Control"));
    }

    @Test
    // 本登録前は利用区分の選択画面へ戻れ、ログイン入口はトップへ遷移する。
    public void pendingMemberCanReturnToRoleChoiceWhileLoginEntriesRedirectHome() throws Exception {
        Harness register = authenticated("GET", "BUSINESS", "PENDING");
        new RegisterAction().execute(register.request, register.response);
        assertEquals(REGISTER, register.forwarded);
        assertNull(register.redirect);
        assertEquals(Boolean.TRUE, register.attributes.get("registrationPending"));
        assertEquals("business", register.attributes.get("registrationRole"));
        assertNotNull(register.attributes.get("csrfToken"));
        Harness login = authenticated("GET", "BUSINESS", "PENDING");
        new LoginAction().execute(login.request, login.response);
        assertEquals("/machiriku/index.jsp", login.redirect);
        Harness start = authenticated("GET", "BUSINESS", "PENDING");
        TestStart action = new TestStart(new FakeClient());
        action.execute(start.request, start.response);
        assertEquals("/machiriku/index.jsp", start.redirect);
        assertEquals(0, action.clientsCreated);
        assertEquals(0, action.daosCreated);
    }

    @Test
    // 認証済みの本登録前会員は、LINE取得値を維持して新しい区分の初期設定へ進む。
    public void pendingRoleSelectionUsesVerifiedMemberWithoutRepeatingLineAuthentication() throws Exception {
        for (String role : new String[] { "resident", "business", "owner" }) {
            Harness request = authenticated("POST", "RESIDENT", "PENDING");
            Session oldSession = request.session;
            oldSession.attributes.put("lineDisplayName", "Original LINE Name");
            oldSession.attributes.put("lineEmail", "");
            oldSession.attributes.put("businessName", "前の屋号");
            oldSession.attributes.put("company", "前の法人名");
            oldSession.attributes.put("phone", "+819012345678");
            oldSession.attributes.put("userPreferredIndustries", java.util.List.of("I0101"));
            oldSession.attributes.put(OnboardingSupport.CSRF_KEY, "old-setup-token");
            String token = prepareRoleSelection(request, role);
            request.parameters.put("userId", "999");
            request.parameters.put("lineSubject", "forged-subject");
            request.parameters.put("email", "forged@example.com");
            FakeDao dao = new FakeDao();
            dao.existing = (User) oldSession.attributes.get("user");
            TestStart action = new TestStart(new FakeClient(), dao);
            action.execute(request.request, request.response);
            assertEquals(0, action.clientsCreated);
            assertEquals(1, action.daosCreated);
            assertEquals(1, dao.roleUpdates);
            assertEquals("verified-line-subject", dao.updatedSubject);
            assertEquals(role.toUpperCase(java.util.Locale.ROOT), dao.updatedRole);
            assertEquals(registrationRedirect(role), request.redirect);
            assertNull(request.forwarded);
            assertTrue(oldSession.invalidated);
            assertNotSame(oldSession, request.session);
            assertEquals(role, request.session.attributes.get("userRole"));
            assertEquals(42, request.session.attributes.get("userId"));
            assertEquals("Original LINE Name", request.session.attributes.get("lineDisplayName"));
            assertEquals("", request.session.attributes.get("lineEmail"));
            assertEquals(token, request.session.attributes.get(LineLoginSupport.REGISTRATION_CSRF_KEY));
            for (String attribute : new String[] { OnboardingSupport.CSRF_KEY, "businessName", "company", "phone", "userPreferredIndustries" }) {
                assertNull(attribute, request.session.attributes.get(attribute));
            }
        }
    }

    @Test
    // 同じ登録画面の同時送信は、先行する区分と新しいセッションを確定してから後続を拒否する。
    public void concurrentPendingRoleSelectionsCannotUpdateAfterSessionRotation() throws Exception {
        Harness first = authenticated("POST", "RESIDENT", "PENDING");
        Harness second = new Harness("POST");
        second.session = first.session;
        Session original = first.session;
        prepareRoleSelection(first, "owner");
        prepareRoleSelection(second, "business");
        FakeDao firstDao = new FakeDao();
        firstDao.existing = (User) original.attributes.get("user");
        firstDao.roleUpdateEntered = new CountDownLatch(1);
        firstDao.allowRoleUpdate = new CountDownLatch(1);
        FakeDao secondDao = new FakeDao();
        secondDao.existing = firstDao.existing;
        TestStart firstAction = new TestStart(new FakeClient(), firstDao);
        TestStart secondAction = new TestStart(new FakeClient(), secondDao);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread firstThread = new Thread(() -> {
            try { firstAction.execute(first.request, first.response); }
            catch (Throwable error) { failure.compareAndSet(null, error); }
        }, "first-role-selection");
        Thread secondThread = new Thread(() -> {
            try { secondAction.execute(second.request, second.response); }
            catch (Throwable error) { failure.compareAndSet(null, error); }
        }, "second-role-selection");
        try {
            firstThread.start();
            assertTrue(firstDao.roleUpdateEntered.await(5, TimeUnit.SECONDS));
            secondThread.start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (secondThread.isAlive() && secondThread.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertEquals(Thread.State.BLOCKED, secondThread.getState());
            assertEquals(0, secondDao.roleUpdates);
            assertEquals(0, secondAction.daosCreated);
        } finally {
            firstDao.allowRoleUpdate.countDown();
            firstThread.join(5000);
            secondThread.join(5000);
        }
        assertFalse(firstThread.isAlive());
        assertFalse(secondThread.isAlive());
        assertNull(failure.get());
        assertEquals(1, firstDao.roleUpdates + secondDao.roleUpdates);
        assertTrue(original.invalidated);
        assertNotSame(original, first.session);
        assertFalse(first.session.invalidated);
        assertEquals("OWNER", ((User) first.session.attributes.get("user")).getUserRole());
        assertEquals("owner", first.session.attributes.get("userRole"));
        assertEquals(registrationRedirect("owner"), first.redirect);
        assertEquals(409, second.status);
        assertNull(second.redirect);
    }

    @Test
    // 利用区分の変更は、登録画面で発行された本人のトークンを必要とする。
    public void pendingRoleSelectionRejectsMissingAndForgedCsrfBeforeLineOrDatabase() throws Exception {
        for (String supplied : new String[] { "", "forged-token" }) {
            Harness request = authenticated("POST", "RESIDENT", "PENDING");
            prepareRoleSelection(request, "owner");
            if (supplied.isEmpty()) { request.parameters.remove("csrfToken"); }
            else { request.parameters.put("csrfToken", supplied); }
            TestStart action = new TestStart(new FakeClient());
            action.execute(request.request, request.response);
            assertEquals(403, request.status);
            assertNull(request.redirect);
            assertEquals(0, action.clientsCreated);
            assertEquals(0, action.daosCreated);
            assertEquals("RESIDENT", ((User) request.session.attributes.get("user")).getUserRole());
        }
    }

    @Test
    // 別タブで本登録が完了していた場合は、DB上の区分を採用してトップへ進む。
    public void roleSelectionAfterConcurrentCompletionKeepsActiveDatabaseRole() throws Exception {
        Harness request = authenticated("POST", "RESIDENT", "PENDING");
        prepareRoleSelection(request, "business");
        FakeDao dao = new FakeDao();
        dao.updateResult = user("OWNER", "ACTIVE");
        TestStart action = new TestStart(new FakeClient(), dao);
        action.execute(request.request, request.response);
        assertEquals(1, dao.roleUpdates);
        assertEquals(0, action.clientsCreated);
        assertEquals("/machiriku/index.jsp", request.redirect);
        assertSame(dao.updateResult, request.session.attributes.get("user"));
        assertEquals("owner", request.session.attributes.get("userRole"));
        assertNull(request.session.attributes.get(LineLoginSupport.REGISTRATION_CSRF_KEY));
    }

    @Test
    // 区分変更と利用停止・退会・削除が競合しても、初期設定を続けさせない。
    public void unavailableAccountDuringRoleSelectionIsForbidden() throws Exception {
        for (String state : new String[] { "BAN", "WITHDRAWN", "withdrawn-timestamp", "missing" }) {
            Harness request = authenticated("POST", "RESIDENT", "PENDING");
            prepareRoleSelection(request, "owner");
            FakeDao dao = new FakeDao();
            dao.updateResult = user("RESIDENT", state.equals("withdrawn-timestamp") ? "PENDING" : state);
            if (state.equals("withdrawn-timestamp")) { dao.updateResult.setWithdrawnAt(OffsetDateTime.now()); }
            dao.missingOnUpdate = state.equals("missing");
            TestStart action = new TestStart(new FakeClient(), dao);
            action.execute(request.request, request.response);
            assertEquals(403, request.status);
            assertNull(request.redirect);
            assertEquals(0, action.clientsCreated);
        }
    }

    @Test
    // DB保存に失敗しても、選び直した利用区分と本人の認証状態を保持する。
    public void roleSelectionDatabaseFailurePreservesChoiceAndRegistrationToken() throws Exception {
        Harness request = authenticated("POST", "RESIDENT", "PENDING");
        Session oldSession = request.session;
        String token = prepareRoleSelection(request, "owner");
        FakeDao dao = new FakeDao();
        dao.updateFailure = new SQLException("private database details", "40001");
        TestStart action = new TestStart(new FakeClient(), dao);
        action.execute(request.request, request.response);
        assertEquals(REGISTER, request.forwarded);
        assertNull(request.redirect);
        assertSame(oldSession, request.session);
        assertEquals("RESIDENT", ((User) request.session.attributes.get("user")).getUserRole());
        assertEquals(Boolean.TRUE, request.attributes.get("registrationPending"));
        assertEquals("owner", request.attributes.get("registrationRole"));
        assertEquals(token, request.attributes.get("csrfToken"));
        assertFalse(request.attributes.get("errorMessage").toString().contains("private database details"));
        assertEquals(0, action.clientsCreated);
    }

    @Test
    // 登録開始時に利用区分と、認証に必要な乱数をサーバー側へ保持することを確認する。
    public void registrationStartStoresServerSideRoleAndRandomAuthenticationValues() throws Exception {
        Harness request = new Harness("POST");
        request.parameters.put("regRole", "business");
        FakeClient client = new FakeClient();
        TestStart action = new TestStart(client);
        action.execute(request.request, request.response);
        LineLoginSupport.Attempt attempt = (LineLoginSupport.Attempt) request.session.attributes.get(LineLoginSupport.ATTEMPT_KEY);
        assertNotNull(attempt);
        assertEquals("business", attempt.registrationRole());
        assertTrue(attempt.state().matches("[a-f0-9]{64}"));
        assertTrue(attempt.nonce().matches("[a-f0-9]{64}"));
        assertNotEquals(attempt.state(), attempt.nonce());
        assertTrue(attempt.codeVerifier().matches("[A-Za-z0-9_-]{43}"));
        assertEquals(attempt.state(), client.authorizationState);
        assertEquals(attempt.nonce(), client.authorizationNonce);
        assertEquals(attempt.codeVerifier(), client.authorizationVerifier);
        assertEquals("https://access.line.me/authorize-test", request.redirect);
        assertEquals("no-store", request.headers.get("Cache-Control"));
        assertEquals("no-referrer", request.headers.get("Referrer-Policy"));
    }

    @Test
    // 通常ログインへ切り替えたとき、前回の登録内容を引き継がないことを確認する。
    public void normalGetLoginClearsEarlierRegistrationChoice() throws Exception {
        Harness request = new Harness("GET");
        request.session.attributes.put(LineLoginSupport.ATTEMPT_KEY, LineLoginSupport.begin("owner"));
        new TestStart(new FakeClient()).execute(request.request, request.response);
        LineLoginSupport.Attempt attempt = (LineLoginSupport.Attempt) request.session.attributes.get(LineLoginSupport.ATTEMPT_KEY);
        assertNotNull(attempt);
        assertNull(attempt.registrationRole());
    }

    @Test
    // 管理者区分の偽装を、LINEへ接続する前に拒否することを確認する。
    public void rejectsForgedAdministratorRoleBeforeContactingLine() throws Exception {
        for (String role : new String[] { "admin", "ADMIN", "unknown" }) {
            Harness request = new Harness("POST");
            request.parameters.put("regRole", role);
            TestStart action = new TestStart(new FakeClient());
            action.execute(request.request, request.response);
            assertEquals(REGISTER, request.forwarded);
            assertEquals(0, action.clientsCreated);
            assertNull(request.session.attributes.get(LineLoginSupport.ATTEMPT_KEY));
        }
    }

    @Test
    // 事業主・オーナーの電話番号は初期設定時に入力するため、LINE開始時には要求しない。
    public void businessAndOwnerCanStartRegistrationBeforePhoneEntry() throws Exception {
        for (String role : new String[] { "business", "owner" }) {
            Harness request = new Harness("POST");
            request.parameters.put("regRole", role);
            TestStart action = new TestStart(new FakeClient());
            action.execute(request.request, request.response);
            assertEquals("https://access.line.me/authorize-test", request.redirect);
            assertNull(request.forwarded);
            assertEquals(1, action.clientsCreated);
        }
    }

    @Test
    // 認証要求の不一致・欠落・期限切れなどでは、外部APIやDBへ進まないことを確認する。
    public void rejectsMissingForgedExpiredAndFutureStateBeforeClientOrDao() throws Exception {
        long now = Instant.now().getEpochSecond();
        for (String scenario : new String[] { "missing", "forged", "expired", "future", "no-session" }) {
            Harness request = new Harness("GET");
            LineLoginSupport.Attempt attempt = LineLoginSupport.begin("resident");
            if (scenario.equals("expired") || scenario.equals("future")) {
                attempt = new LineLoginSupport.Attempt(attempt.state(), attempt.nonce(), attempt.codeVerifier(),
                        attempt.registrationRole(), scenario.equals("expired") ? now - 601 : now + 30);
            }
            request.session.attributes.put(LineLoginSupport.ATTEMPT_KEY, attempt);
            request.parameters.put("state", scenario.equals("forged") ? "forged-state" : attempt.state());
            request.parameters.put("code", "authorization-code");
            if (scenario.equals("missing")) { request.parameters.remove("state"); }
            if (scenario.equals("no-session")) { request.session = null; }
            TestCallback action = new TestCallback(new FakeClient(), new FakeDao());
            action.execute(request.request, request.response);
            assertEquals(LOGIN, request.forwarded);
            assertEquals(0, action.clientsCreated);
            assertEquals(0, action.daosCreated);
        }
    }

    @Test
    // 検証済みの会員情報とDB用の利用区分を保存し、成功時にセッションを作り直すことを確認する。
    public void registrationSavesVerifiedFieldsUppercaseRoleThenRotatesSession() throws Exception {
        for (String role : new String[] { "resident", "business", "owner" }) {
            Harness request = callback(role);
            Session oldSession = request.session;
            LineLoginSupport.Attempt attempt = (LineLoginSupport.Attempt) oldSession.attributes.get(LineLoginSupport.ATTEMPT_KEY);
            oldSession.attributes.put("userRole", "admin");
            request.parameters.put("email", "attacker@example.com");
            request.parameters.put("displayName", "forged-name");
            request.parameters.put("regRole", "admin");
            request.parameters.put("nonce", "attacker-nonce");
            request.parameters.put("codeVerifier", "attacker-verifier");
            FakeClient client = new FakeClient();
            FakeDao dao = new FakeDao();
            new TestCallback(client, dao).execute(request.request, request.response);
            assertEquals(1, dao.inserts);
            assertEquals("Verified LINE Name", dao.inserted.getDisplayName());
            assertEquals("verified@example.com", dao.inserted.getEmail());
            assertEquals("verified-line-subject", dao.inserted.getLineSubject());
            assertEquals(role.toUpperCase(java.util.Locale.ROOT), dao.inserted.getUserRole());
            assertTrue(oldSession.invalidated);
            assertNotSame(oldSession, request.session);
            assertSame(dao.inserted, request.session.attributes.get("user"));
            assertEquals(42, request.session.attributes.get("userId"));
            assertEquals(role, request.session.attributes.get("userRole"));
            assertEquals("Verified LINE Name", request.session.attributes.get("lineDisplayName"));
            assertEquals("verified@example.com", request.session.attributes.get("lineEmail"));
            assertEquals(registrationRedirect(role), request.redirect);
            assertEquals(1, client.authenticationCalls);
            assertEquals(attempt.nonce(), client.authenticationNonce);
            assertEquals(attempt.codeVerifier(), client.authenticationVerifier);
        }
    }

    @Test
    // 各利用区分でメールなし登録ができ、リクエスト中の未検証メールを採用しないことを確認する。
    public void everyPublicRoleCanRegisterWithoutEmailAndRequestCannotSupplyUnverifiedEmail() throws Exception {
        for (String role : new String[] { "resident", "business", "owner" }) {
            Harness request = callback(role);
            Session oldSession = request.session;
            request.parameters.put("email", "attacker@example.com");
            FakeClient client = new FakeClient();
            client.profile = new LineLoginClient.Profile("Verified LINE Name", null, "verified-line-subject");
            FakeDao dao = new FakeDao();
            new TestCallback(client, dao).execute(request.request, request.response);
            assertEquals(1, dao.inserts);
            assertNull(dao.inserted.getEmail());
            assertEquals("Verified LINE Name", dao.inserted.getDisplayName());
            assertEquals("verified-line-subject", dao.inserted.getLineSubject());
            assertEquals(role.toUpperCase(java.util.Locale.ROOT), dao.inserted.getUserRole());
            assertTrue(oldSession.invalidated);
            assertNotSame(oldSession, request.session);
            assertSame(dao.inserted, request.session.attributes.get("user"));
            assertEquals(42, request.session.attributes.get("userId"));
            assertEquals(role, request.session.attributes.get("userRole"));
            assertEquals("Verified LINE Name", request.session.attributes.get("lineDisplayName"));
            assertEquals("", request.session.attributes.get("lineEmail"));
            assertEquals(registrationRedirect(role), request.redirect);
            assertNull(request.forwarded);
        }
    }

    @Test
    // 同じLINE会員の本登録前データを、再選択した区分で再利用する。
    public void pendingRegistrationCallbackUsesSelectedRoleAndPreservesBackPageToken() throws Exception {
        Harness request = callback("resident");
        Session oldSession = request.session;
        String token = LineLoginSupport.registrationCsrfToken(oldSession.proxy);
        oldSession.attributes.put(OnboardingSupport.CSRF_KEY, "old-setup-token");
        request.parameters.put("regRole", "admin");
        request.parameters.put("lineSubject", "forged-subject");
        FakeDao dao = new FakeDao();
        dao.existing = user("OWNER", "PENDING");
        dao.existing.setEmail("stored@example.com");
        FakeClient client = new FakeClient();
        client.profile = new LineLoginClient.Profile("Current LINE Name", null, "verified-line-subject");
        new TestCallback(client, dao).execute(request.request, request.response);
        assertEquals(0, dao.inserts);
        assertEquals(1, dao.roleUpdates);
        assertEquals("verified-line-subject", dao.updatedSubject);
        assertEquals("RESIDENT", dao.updatedRole);
        assertEquals(registrationRedirect("resident"), request.redirect);
        assertEquals("resident", request.session.attributes.get("userRole"));
        assertEquals("stored@example.com", ((User) request.session.attributes.get("user")).getEmail());
        assertEquals("Current LINE Name", request.session.attributes.get("lineDisplayName"));
        assertEquals("", request.session.attributes.get("lineEmail"));
        assertEquals(token, request.session.attributes.get(LineLoginSupport.REGISTRATION_CSRF_KEY));
        assertNull(request.session.attributes.get(OnboardingSupport.CSRF_KEY));
        assertTrue(oldSession.invalidated);
    }

    @Test
    // 匿名の登録画面で発行したトークンを、LINE認証後の戻る操作でも利用できる。
    public void initialRegistrationCallbackRetainsOriginalRoleChoiceToken() throws Exception {
        Harness request = callback("business");
        String token = LineLoginSupport.registrationCsrfToken(request.session.proxy);
        FakeDao dao = new FakeDao();
        new TestCallback(new FakeClient(), dao).execute(request.request, request.response);
        assertEquals(1, dao.inserts);
        assertEquals(1, dao.roleUpdates);
        assertEquals(token, request.session.attributes.get(LineLoginSupport.REGISTRATION_CSRF_KEY));
        assertEquals(registrationRedirect("business"), request.redirect);
    }

    @Test
    // コールバック処理中に本登録が完了した場合も、確定した区分を維持する。
    public void callbackRoleUpdateAfterConcurrentCompletionUsesActiveMemberAndTopPage() throws Exception {
        Harness request = callback("resident");
        LineLoginSupport.registrationCsrfToken(request.session.proxy);
        FakeDao dao = new FakeDao();
        dao.existing = user("BUSINESS", "PENDING");
        dao.updateResult = user("OWNER", "ACTIVE");
        new TestCallback(new FakeClient(), dao).execute(request.request, request.response);
        assertEquals(1, dao.roleUpdates);
        assertSame(dao.updateResult, request.session.attributes.get("user"));
        assertEquals("owner", request.session.attributes.get("userRole"));
        assertEquals("/machiriku/index.jsp", request.redirect);
        assertNull(request.session.attributes.get(LineLoginSupport.REGISTRATION_CSRF_KEY));
    }

    @Test
    // 本登録済み会員の再認証では、利用区分選び直し用のトークンを引き継がない。
    public void activeRegistrationCallbackDropsOldRegistrationToken() throws Exception {
        Harness request = callback("resident");
        LineLoginSupport.registrationCsrfToken(request.session.proxy);
        FakeDao dao = new FakeDao();
        dao.existing = user("OWNER", "ACTIVE");
        new TestCallback(new FakeClient(), dao).execute(request.request, request.response);
        assertEquals("/machiriku/index.jsp", request.redirect);
        assertEquals(0, dao.roleUpdates);
        assertNull(request.session.attributes.get(LineLoginSupport.REGISTRATION_CSRF_KEY));
    }

    @Test
    // 再ログイン時にメールを取得できなくても、DBにあるメールと利用区分を維持することを確認する。
    public void existingMemberCanLoginWithoutCurrentLineEmailAndKeepsStoredEmailAndRole() throws Exception {
        for (String storedEmail : new String[] { "stored@example.com", null }) {
            Harness request = callback(null);
            request.parameters.put("email", "attacker@example.com");
            FakeClient client = new FakeClient();
            client.profile = new LineLoginClient.Profile("Verified LINE Name", null, "verified-line-subject");
            FakeDao dao = new FakeDao();
            dao.existing = user("OWNER", "ACTIVE");
            dao.existing.setEmail(storedEmail);
            new TestCallback(client, dao).execute(request.request, request.response);
            assertEquals(0, dao.inserts);
            assertEquals(1, dao.finds);
            assertSame(dao.existing, request.session.attributes.get("user"));
            assertEquals(storedEmail, ((User) request.session.attributes.get("user")).getEmail());
            assertEquals("OWNER", dao.existing.getUserRole());
            assertEquals("owner", request.session.attributes.get("userRole"));
            assertEquals("Verified LINE Name", request.session.attributes.get("lineDisplayName"));
            assertEquals("", request.session.attributes.get("lineEmail"));
            assertEquals("/machiriku/index.jsp", request.redirect);
            assertNull(request.forwarded);
        }
    }

    @Test
    // 既存会員の利用区分をフォームの値で変更せず、新しい会員レコードも作らないことを確認する。
    public void existingLoginKeepsDatabaseRoleWithoutInsertEvenWhenRegistrationRequestsDifferentRole() throws Exception {
        Harness request = callback("resident");
        FakeDao dao = new FakeDao();
        dao.existing = user("OWNER", "ACTIVE");
        new TestCallback(new FakeClient(), dao).execute(request.request, request.response);
        assertEquals(0, dao.inserts);
        assertSame(dao.existing, request.session.attributes.get("user"));
        assertEquals("owner", request.session.attributes.get("userRole"));
        assertEquals("/machiriku/index.jsp", request.redirect);
        assertEquals(0, dao.roleUpdates);
    }

    @Test
    // 未登録のLINE IDで通常ログインした場合、勝手に登録せず登録画面へ案内することを確認する。
    public void normalLoginOfUnknownLineSubjectShowsRegistrationWithoutCreatingUser() throws Exception {
        Harness request = callback(null);
        FakeDao dao = new FakeDao();
        new TestCallback(new FakeClient(), dao).execute(request.request, request.response);
        assertEquals(REGISTER, request.forwarded);
        assertEquals(0, dao.inserts);
        assertNull(request.session.attributes.get("user"));
    }

    @Test
    // 利用停止・退会済みの会員がログインできないことを確認する。
    public void bannedWithdrawnAndWithdrawnTimestampAccountsCannotLogin() throws Exception {
        for (String state : new String[] { "BAN", "WITHDRAWN", "withdrawn-timestamp" }) {
            Harness request = callback(null);
            FakeDao dao = new FakeDao();
            dao.existing = user("RESIDENT", state.equals("withdrawn-timestamp") ? "ACTIVE" : state);
            if (state.equals("withdrawn-timestamp")) { dao.existing.setWithdrawnAt(OffsetDateTime.now()); }
            new TestCallback(new FakeClient(), dao).execute(request.request, request.response);
            assertEquals(LOGIN, request.forwarded);
            assertNull(request.redirect);
            assertNull(request.session.attributes.get("user"));
            assertEquals(0, dao.inserts);
        }
    }

    @Test
    // 登録直後のPENDING状態で、ログイン処理を完了できることを確認する。
    public void pendingAccountCanLogin() throws Exception {
        Harness request = callback(null);
        FakeDao dao = new FakeDao();
        dao.existing = user("BUSINESS", "PENDING");
        new TestCallback(new FakeClient(), dao).execute(request.request, request.response);
        assertEquals("business", request.session.attributes.get("userRole"));
        assertEquals("/machiriku/jp/machiriku/servlet/auth/BusinessSetup.action", request.redirect);
    }

    @Test
    // 本人確認の失敗ではDBを使わず、同じ認証応答も再利用できないことを確認する。
    public void identityVerificationFailureCannotReachDatabaseAndConsumesAttempt() throws Exception {
        Harness request = callback("resident");
        FakeClient client = new FakeClient();
        client.failure = new IOException("LINE identity verification failed.");
        TestCallback action = new TestCallback(client, new FakeDao());
        action.execute(request.request, request.response);
        assertEquals(REGISTER, request.forwarded);
        assertEquals(0, action.daosCreated);
        assertNull(request.session.attributes.get(LineLoginSupport.ATTEMPT_KEY));
        action.execute(request.request, request.response);
        assertEquals(LOGIN, request.forwarded);
        assertEquals(1, client.authenticationCalls);
    }

    @Test
    // ログイン成功後の新しいセッションへ、同じ認証応答を再送しても処理されないことを確認する。
    public void successfulCallbackCannotBeReplayedAgainstNewSession() throws Exception {
        Harness request = callback(null);
        FakeClient client = new FakeClient();
        FakeDao dao = new FakeDao();
        dao.existing = user("RESIDENT", "ACTIVE");
        TestCallback action = new TestCallback(client, dao);
        action.execute(request.request, request.response);
        action.execute(request.request, request.response);
        assertEquals(1, client.authenticationCalls);
        assertEquals(1, dao.finds);
        assertNull(request.forwarded);
        assertEquals("/machiriku/index.jsp", request.redirect);
    }

    @Test
    // メール等の重複が起きても、別のLINE会員へ自動的に結び付けないことを確認する。
    public void duplicateEmailDoesNotLinkToAnUnrelatedAccount() throws Exception {
        Harness request = callback("resident");
        FakeDao dao = new FakeDao();
        dao.insertFailure = new SQLException("sensitive conflict details", "23505");
        new TestCallback(new FakeClient(), dao).execute(request.request, request.response);
        assertEquals(REGISTER, request.forwarded);
        assertEquals(2, dao.finds);
        assertEquals(1, dao.inserts);
        assertNull(request.session.attributes.get("user"));
        assertFalse(request.attributes.get("errorMessage").toString().contains("sensitive conflict details"));
    }

    @Test
    // 同じLINE IDの同時登録が競合した場合、先に保存された会員情報を利用することを確認する。
    public void sameSubjectRegistrationRaceUsesExistingDatabaseRole() throws Exception {
        Harness request = callback("resident");
        FakeDao dao = new FakeDao();
        dao.insertFailure = new SQLException("concurrent insert", "23505");
        dao.afterConflict = user("OWNER", "ACTIVE");
        new TestCallback(new FakeClient(), dao).execute(request.request, request.response);
        assertEquals(2, dao.finds);
        assertEquals(1, dao.inserts);
        assertSame(dao.afterConflict, request.session.attributes.get("user"));
        assertEquals("owner", request.session.attributes.get("userRole"));
        assertEquals("/machiriku/index.jsp", request.redirect);
        assertEquals(0, dao.roleUpdates);
    }

    @Test
    // 認証キャンセルやコード欠落ではAPIへ進まず、認証要求を使い切ることを確認する。
    public void cancellationAndMissingCodeConsumeAttemptWithoutContactingLine() throws Exception {
        for (boolean cancelled : new boolean[] { true, false }) {
            Harness request = callback("resident");
            if (cancelled) { request.parameters.put("error", "access_denied"); }
            else { request.parameters.remove("code"); }
            TestCallback action = new TestCallback(new FakeClient(), new FakeDao());
            action.execute(request.request, request.response);
            assertEquals(REGISTER, request.forwarded);
            assertEquals(0, action.clientsCreated);
            assertEquals(0, action.daosCreated);
            assertNull(request.session.attributes.get(LineLoginSupport.ATTEMPT_KEY));
        }
    }

    // 登録経由は住民・事業主・オーナーそれぞれの初期設定へ案内する。
    private static String registrationRedirect(String role) {
        return switch (role) {
            case "resident" -> "/machiriku/jp/machiriku/servlet/auth/ResidentSetup.action";
            case "business" -> "/machiriku/jp/machiriku/servlet/auth/BusinessSetup.action";
            case "owner" -> "/machiriku/jp/machiriku/servlet/auth/OwnerSetup.action";
            default -> "/machiriku/index.jsp";
        };
    }

    private static Harness callback(String role) {
        Harness request = new Harness("GET");
        LineLoginSupport.Attempt attempt = LineLoginSupport.begin(role);
        request.session.attributes.put(LineLoginSupport.ATTEMPT_KEY, attempt);
        request.parameters.put("state", attempt.state());
        request.parameters.put("code", "authorization-code");
        return request;
    }

    private static Harness authenticated(String method, String role, String status) {
        Harness request = new Harness(method);
        request.session.attributes.put("user", user(role, status));
        request.session.attributes.put("userRole", role.toLowerCase(java.util.Locale.ROOT));
        return request;
    }

    private static String prepareRoleSelection(Harness request, String role) {
        request.parameters.put("regRole", role);
        String token = LineLoginSupport.registrationCsrfToken(request.session.proxy);
        request.parameters.put("csrfToken", token);
        return token;
    }

    private static User user(String role, String status) {
        User user = new User();
        user.setUserId(42);
        user.setDisplayName("Database Name");
        user.setEmail("database@example.com");
        user.setLineSubject("verified-line-subject");
        user.setUserRole(role);
        user.setAccountStatus(status);
        return user;
    }

    private static final class TestStart extends LineLoginAction {
        private final FakeClient client;
        private final FakeDao dao;
        int clientsCreated;
        int daosCreated;
        TestStart(FakeClient client) { this(client, new FakeDao()); }
        TestStart(FakeClient client, FakeDao dao) { this.client = client; this.dao = dao; }
        @Override protected LineLoginClient createClient() { clientsCreated++; return client; }
        @Override protected UserDao createUserDao() { daosCreated++; return dao; }
    }

    private static final class TestCallback extends LineCallbackAction {
        private final FakeClient client;
        private final FakeDao dao;
        int clientsCreated;
        int daosCreated;
        TestCallback(FakeClient client, FakeDao dao) { this.client = client; this.dao = dao; }
        @Override protected LineLoginClient createClient() { clientsCreated++; return client; }
        @Override protected UserDao createUserDao() { daosCreated++; return dao; }
    }

    private static final class FakeClient extends LineLoginClient {
        int authenticationCalls;
        String authorizationState;
        String authorizationNonce;
        String authorizationVerifier;
        String authenticationNonce;
        String authenticationVerifier;
        IOException failure;
        Profile profile = new Profile("Verified LINE Name", "verified@example.com", "verified-line-subject");
        FakeClient() {
            super(new LineLoginConfig("12345", "test-secret", "http://localhost/callback"), new NoNetworkHttpClient());
        }
        @Override public String authorizationUrl(String state, String nonce, String verifier) {
            authorizationState = state; authorizationNonce = nonce; authorizationVerifier = verifier;
            return "https://access.line.me/authorize-test";
        }
        @Override public Profile authenticate(String code, String nonce, String verifier) throws IOException {
            authenticationCalls++;
            authenticationNonce = nonce;
            authenticationVerifier = verifier;
            if (failure != null) { throw failure; }
            return profile;
        }
    }

    /** 認証通信はテスト用の処理へ差し替え、ソケットやセレクターを作らない。 */
    private static final class NoNetworkHttpClient extends HttpClient {
        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { return null; }
        @Override public SSLParameters sslParameters() { return new SSLParameters(); }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }
        @Override public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            throw new UnsupportedOperationException("HTTP is not used in action tests.");
        }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            throw new UnsupportedOperationException("HTTP is not used in action tests.");
        }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                HttpResponse.BodyHandler<T> handler, HttpResponse.PushPromiseHandler<T> pushHandler) {
            throw new UnsupportedOperationException("HTTP is not used in action tests.");
        }
    }

    private static final class FakeDao extends UserDao {
        User existing;
        User afterConflict;
        User inserted;
        User updateResult;
        SQLException insertFailure;
        SQLException updateFailure;
        CountDownLatch roleUpdateEntered;
        CountDownLatch allowRoleUpdate;
        boolean missingOnUpdate;
        String updatedSubject;
        String updatedRole;
        int finds;
        int inserts;
        int roleUpdates;
        @Override public User findByLineSubject(String subject) {
            assertEquals("verified-line-subject", subject);
            finds++;
            return finds > 1 && afterConflict != null ? afterConflict : existing;
        }
        @Override public User insert(User value) throws SQLException {
            inserts++;
            inserted = value;
            if (insertFailure != null) { throw insertFailure; }
            value.setUserId(42);
            value.setAccountStatus("PENDING");
            return value;
        }
        @Override public User updatePendingRole(String subject, String role) throws SQLException {
            assertEquals("verified-line-subject", subject);
            roleUpdates++;
            updatedSubject = subject;
            updatedRole = role;
            if (roleUpdateEntered != null) { roleUpdateEntered.countDown(); }
            if (allowRoleUpdate != null) {
                try {
                    if (!allowRoleUpdate.await(10, TimeUnit.SECONDS)) { throw new SQLException("Role update test timed out."); }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Role update test was interrupted.");
                }
            }
            if (updateFailure != null) { throw updateFailure; }
            if (missingOnUpdate) { return null; }
            if (updateResult != null) { return updateResult; }
            User target = existing;
            if (afterConflict != null) { target = afterConflict; }
            if (inserted != null) { target = inserted; }
            if (target != null && "PENDING".equals(target.getAccountStatus()) && target.getWithdrawnAt() == null) {
                target.setUserRole(role);
            }
            return target;
        }
    }

    private static final class Harness {
        final String method;
        final Map<String, String> parameters = new HashMap<>();
        final Map<String, Object> attributes = new HashMap<>();
        final Map<String, String> headers = new HashMap<>();
        Session session = new Session();
        String forwarded;
        String redirect;
        int status;
        final HttpServletRequest request;
        final HttpServletResponse response;

        Harness(String method) {
            this.method = method;
            request = proxy(HttpServletRequest.class, (ignored, call, arguments) -> {
                switch (call.getName()) {
                    case "getMethod" -> {
                        return method;
                    }
                    case "getContextPath" -> {
                        return "/machiriku";
                    }
                    case "getParameter" -> {
                        return parameters.get(arguments[0]);
                    }
                    case "getAttribute" -> {
                        return attributes.get(arguments[0]);
                    }
                    case "setAttribute" -> {
                        attributes.put((String) arguments[0], arguments[1]); return null;
                    }
                    case "getSession" -> {
                        boolean create = arguments == null || arguments.length == 0 || Boolean.TRUE.equals(arguments[0]);
                        if ((session == null || session.invalidated) && create) { session = new Session(); }
                        return session == null || session.invalidated ? null : session.proxy;
                    }
                    case "getRequestDispatcher" -> {
                        String path = (String) arguments[0];
                        return proxy(RequestDispatcher.class, (dispatcher, dispatchCall, dispatchArguments) -> {
                            if (dispatchCall.getName().equals("forward")) { forwarded = path; }
                            return defaultValue(dispatchCall.getReturnType());
                        });
                    }
                    case "getServletContext" -> {
                        return proxy(ServletContext.class,
                                (context, contextCall, contextArguments) -> defaultValue(contextCall.getReturnType()));
                    }
                    default -> {
                        return defaultValue(call.getReturnType());
                    }
                }
            });
            response = proxy(HttpServletResponse.class, (ignored, call, arguments) -> {
                switch (call.getName()) {
                    case "setHeader" -> {
                        headers.put((String) arguments[0], (String) arguments[1]); return null;
                    }
                    case "sendRedirect" -> {
                        redirect = (String) arguments[0]; return null;
                    }
                    case "sendError" -> {
                        status = (Integer) arguments[0]; return null;
                    }
                    default -> {
                        return defaultValue(call.getReturnType());
                    }
                }
            });
        }
    }

    private static final class Session {
        final Map<String, Object> attributes = new HashMap<>();
        volatile boolean invalidated;
        final HttpSession proxy = proxy(HttpSession.class, (ignored, call, arguments) -> {
            switch (call.getName()) {
                case "getAttribute" -> {
                    ensureValid();
                    return attributes.get(arguments[0]);
                }
                case "setAttribute" -> {
                    ensureValid();
                    attributes.put((String) arguments[0], arguments[1]); return null;
                }
                case "removeAttribute" -> {
                    ensureValid();
                    attributes.remove(arguments[0]); return null;
                }
                case "invalidate" -> {
                    ensureValid();
                    invalidated = true; attributes.clear(); return null;
                }
                default -> {
                    return defaultValue(call.getReturnType());
                }
            }
        });

        private void ensureValid() {
            if (invalidated) { throw new IllegalStateException("Session has been invalidated."); }
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
        if (type == short.class) { return (short) 0; }
        if (type == byte.class) { return (byte) 0; }
        if (type == double.class) { return 0d; }
        if (type == float.class) { return 0f; }
        if (type == char.class) { return (char) 0; }
        return null;
    }
}
