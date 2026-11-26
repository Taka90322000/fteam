package jp.machiriku.servlet.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.LocationInput;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminAuditLogItem;
import jp.machiriku.bean.management.AdminUserItem;
import jp.machiriku.bean.management.ManagementLabels;
import jp.machiriku.bean.management.ManagementProblem;
import jp.machiriku.bean.management.PageResult;
import jp.machiriku.dao.ProfileDao;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.dao.management.AdminUserDao;

/**
 * DB接続を伴わない管理・サポート共通部品（FormReader・ManagementFormat・ManagementLabels・ManagementAuth）の単体テスト。
 */
public class ManagementUtilitiesTest {

    /**
     * FormReaderの必須チェック・数値検証・URL検証・ページ送りURL組み立てを検証する。
     */
    @Test
    public void formReaderValidatesInputsAndBuildsPageResult() {
        HttpServletRequest req = mockRequest("GET", "/jp/machiriku/servlet/management/AdminUsers.action",
                Map.of(
                        "q", "  カフェ  ",
                        "role", "BUSINESS",
                        "id", "42",
                        "receipt", "9001",
                        "decimal", "12.34",
                        "date", "2026-04-01",
                        "dt", "2026-04-01T09:30",
                        "url", "https://example.invalid/info",
                        "page", "2",
                        "ignored", "secret"),
                Map.of("industries", new String[] { "11", "12", "11" }),
                null, null);
        FormReader reader = new FormReader(req);

        assertEquals("カフェ", reader.search());
        assertEquals("BUSINESS", reader.choice("role", "利用区分", "RESIDENT", "BUSINESS", "OWNER"));
        assertEquals(Integer.valueOf(42), reader.intId("id", true));
        assertEquals(Long.valueOf(9001L), reader.longId("receipt", true));
        assertEquals(new BigDecimal("12.34"), reader.decimal("decimal", "小数", 0, 100, 2, true));
        assertEquals(LocalDate.of(2026, 4, 1), reader.date("date", "日付"));
        assertEquals(OffsetDateTime.parse("2026-04-01T09:30:00+09:00"), reader.dateTime("dt", "日時", true));
        assertEquals("https://example.invalid/info", reader.webUrl("url", "URL"));
        assertEquals(Set.of(11, 12), reader.intIds("industries"));
        assertEquals(2, reader.page());
        assertEquals(FormReader.PAGE_SIZE, reader.offset());

        // PAGE_SIZE + 1 件を渡すと次ページURLが生成され、許可フィルタのみが引き継がれることを確認する。
        List<Integer> extraRows = new ArrayList<>();
        for (int i = 0; i <= FormReader.PAGE_SIZE; i++) {
            extraRows.add(i);
        }
        PageResult<Integer> page = reader.toPageResult(extraRows);
        assertEquals(FormReader.PAGE_SIZE, page.getItems().size());
        assertTrue(page.isHasPager());
        assertTrue(page.getPreviousUrl().contains("page=1"));
        assertTrue(page.getNextUrl().contains("page=3"));
        assertTrue(page.getNextUrl().contains("role=BUSINESS"));
        assertFalse(page.getNextUrl().contains("ignored"));
    }

    /**
     * FormReaderが不正な文字列・NUL文字・危険URL・範囲外数値をHTTP 400のManagementProblemとして拒否することを検証する。
     */
    @Test
    public void formReaderRejectsInvalidValues() {
        HttpServletRequest req = mockRequest("POST", "/test",
                Map.of(
                        "nul", "abc\0def",
                        "badUrl", "javascript:alert(1)",
                        "badChoice", "ADMIN",
                        "badNum", "0"),
                Map.of(), null, null);
        FormReader reader = new FormReader(req);

        assertEquals(400, assertThrows(ManagementProblem.class,
                () -> reader.text("nul", "本文", 100, true)).getStatus());
        assertEquals(400, assertThrows(ManagementProblem.class,
                () -> reader.webUrl("badUrl", "URL")).getStatus());
        assertEquals(400, assertThrows(ManagementProblem.class,
                () -> reader.choice("badChoice", "区分", "RESIDENT", "BUSINESS")).getStatus());
        assertEquals(400, assertThrows(ManagementProblem.class,
                () -> reader.intId("badNum", true)).getStatus());
        assertEquals(400, assertThrows(ManagementProblem.class,
                () -> reader.text("missing", "必須項目", 50, true)).getStatus());
    }

    /**
     * ManagementFormatの画面表示整形・datetime-local変換・URL安全性判定を検証する。
     */
    @Test
    public void managementFormatFormatsDisplayAndChecksUrls() {
        assertEquals("—", ManagementFormat.text(null));
        assertEquals("—", ManagementFormat.text(""));
        assertEquals("はい", ManagementFormat.text(Boolean.TRUE));
        assertEquals("いいえ", ManagementFormat.text(Boolean.FALSE));
        assertEquals("2026/05/01", ManagementFormat.text(LocalDate.of(2026, 5, 1)));
        OffsetDateTime utc = OffsetDateTime.parse("2026-05-01T00:00:00Z");
        assertEquals("2026/05/01 09:00:00", ManagementFormat.text(utc));
        assertEquals("2026-05-01T09:00", ManagementFormat.dateTimeInput(utc));

        assertTrue(ManagementFormat.selected(List.of(10, 20), "20"));
        assertFalse(ManagementFormat.selected(List.of(10, 20), "30"));
        assertTrue(ManagementFormat.selected("ACTIVE", "ACTIVE"));

        assertTrue(ManagementFormat.isWebUrl("https://example.invalid/path"));
        assertTrue(ManagementFormat.isWebUrl("http://example.invalid"));
        assertFalse(ManagementFormat.isWebUrl("javascript:alert(1)"));
        assertFalse(ManagementFormat.isWebUrl("https://user:pass@example.invalid/"));
        assertFalse(ManagementFormat.isWebUrl("/relative/path"));
    }

    /**
     * ManagementLabelsの通報理由・対応結果の区分別フィルタリングを検証する。
     */
    @Test
    public void managementLabelsReturnsRoleAndTargetSpecificChoices() {
        Map<String, String> propertyReasons = ManagementLabels.reportReasonsFor("PROPERTY");
        assertTrue(propertyReasons.containsKey("DUPLICATE"));
        assertFalse(propertyReasons.containsKey("HARASSMENT"));
        assertFalse(propertyReasons.containsKey("IMPERSONATION"));

        Map<String, String> ownerReasons = ManagementLabels.reportReasonsFor("OWNER");
        assertFalse(ownerReasons.containsKey("DUPLICATE"));
        assertTrue(ownerReasons.containsKey("HARASSMENT"));

        assertTrue(ManagementLabels.reportResolutionsFor(true).containsKey("HIDE_PROPERTY"));
        assertFalse(ManagementLabels.reportResolutionsFor(true).containsKey("SUSPEND_USER"));
        assertTrue(ManagementLabels.reportResolutionsFor(false).containsKey("SUSPEND_USER"));
        assertFalse(ManagementLabels.reportResolutionsFor(false).containsKey("HIDE_PROPERTY"));

        assertEquals("地域住民", ManagementLabels.label(ManagementLabels.USER_ROLES, "RESIDENT"));
        assertEquals("UNKNOWN_CODE", ManagementLabels.label(ManagementLabels.USER_ROLES, "UNKNOWN_CODE"));
        assertNull(ManagementLabels.label(ManagementLabels.USER_ROLES, null));
    }

    /**
     * ManagementAuthのCSRF検証・HTTPメソッド検証・PRGリダイレクト・SQLStateエラー変換を検証する。
     * @throws Exception テスト用モック呼び出しに失敗した場合。
     */
    @Test
    public void managementAuthHandlesCsrfRedirectAndErrors() throws Exception {
        Map<String, Object> sessionAttrs = new HashMap<>();
        HttpSession session = mockSession(sessionAttrs);
        String token = ManagementAuth.csrfToken(session);
        assertNotNull(token);
        assertEquals(token, ManagementAuth.csrfToken(session));

        // 正しいトークンではCSRF検証を通過する。
        ResponseRecorder okRes = new ResponseRecorder();
        HttpServletRequest validPost = mockRequest("POST", "/admin", Map.of("csrfToken", token), Map.of(), session, null);
        assertTrue(ManagementAuth.verifyCsrf(validPost, okRes.proxy(), true));
        assertEquals(0, okRes.errorStatus);

        // 不正トークンでは403エラーを返す。
        ResponseRecorder badRes = new ResponseRecorder();
        HttpServletRequest invalidPost = mockRequest("POST", "/admin", Map.of("csrfToken", "wrong"), Map.of(), session, null);
        assertFalse(ManagementAuth.verifyCsrf(invalidPost, badRes.proxy(), true));
        assertEquals(403, badRes.errorStatus);

        // 読み取り専用エンドポイントでPOSTは405を返す。
        ResponseRecorder methodRes = new ResponseRecorder();
        assertFalse(ManagementAuth.allowReadMethod(invalidPost, methodRes.proxy()));
        assertEquals(405, methodRes.errorStatus);

        // PRGリダイレクトは303かつsaved=1を付与する。
        ResponseRecorder redirectRes = new ResponseRecorder();
        ManagementAuth.redirectSaved(validPost, redirectRes.proxy(), "/jp/machiriku/servlet/management/AdminUsers.action?id=10");
        assertEquals(303, redirectRes.status);
        assertEquals("/machiriku/jp/machiriku/servlet/management/AdminUsers.action?id=10&saved=1",
                redirectRes.headers.get("Location"));

        // SQLStateに応じたHTTPステータスへ変換し、SQL文詳細をログや画面に出さないことを確認する。
        List<String> logs = new ArrayList<>();
        ServletContext ctx = mockContext(logs);
        HttpServletRequest reqWithCtx = mockRequest("GET", "/admin", Map.of(), Map.of(), session, ctx);

        ResponseRecorder conflictRes = new ResponseRecorder();
        ManagementAuth.handleError(reqWithCtx, conflictRes.proxy(),
                new SQLException("duplicate key value violates unique constraint secret_email", "23505"));
        assertEquals(409, conflictRes.errorStatus);
        assertFalse(conflictRes.errorMessage.contains("secret_email"));
        assertEquals(1, logs.size());
        assertFalse(logs.get(0).contains("secret_email"));

        ResponseRecorder notFoundRes = new ResponseRecorder();
        ManagementAuth.handleError(reqWithCtx, notFoundRes.proxy(), new SQLException("no data", "02000"));
        assertEquals(404, notFoundRes.errorStatus);

        ResponseRecorder badArgRes = new ResponseRecorder();
        ManagementAuth.handleError(reqWithCtx, badArgRes.proxy(), new IllegalArgumentException("入力エラー"));
        assertEquals(400, badArgRes.errorStatus);
    }

    /**
     * 地域住民の保持クーポン・クーポン使用履歴SQLでAND句の直後に空白が保たれ、構文エラーを起こさないことを検証する。
     * @throws Exception モック実行に失敗した場合。
     */
    @Test
    public void residentCouponHistorySqlSeparatesAndClauseWithWhitespace() throws Exception {
        List<String> executedSqls = new ArrayList<>();
        Connection mockConnection = capturingConnection(executedSqls);
        AdminUserDao userDao = new AdminUserDao(mockConnection);

        assertTrue(userDao.findResidentCouponHistory(102, false, 26, 0).isEmpty());
        assertTrue(userDao.findResidentCouponHistory(102, true, 26, 0).isEmpty());
        assertEquals(2, executedSqls.size());

        // 保持クーポン（未使用）と使用履歴（使用済み）の双方が正しいSQLトークン区切りであることを確認する。
        assertTrue(executedSqls.get(0).matches("(?s).*WHERE cr\\.resident_user_id = \\?\\s+AND\\s+cr\\.used_at IS NULL\\s+ORDER BY.*"));
        assertTrue(executedSqls.get(1).matches("(?s).*WHERE cr\\.resident_user_id = \\?\\s+AND\\s+cr\\.used_at IS NOT NULL\\s+ORDER BY.*"));
    }

    /**
     * 生活拠点モデル（居住地・勤務先・学校）の表示名と住所連結、およびAdminUsersActionの地図入力検証を確認する。
     * @throws Exception Action実行に失敗した場合。
     */
    @Test
    public void adminUsersLocationHandlesHomeAndWorkMapInput() throws Exception {
        AdminUserItem.Location homeLoc = new AdminUserItem.Location();
        homeLoc.setLocationKind("HOME");
        homeLoc.setPrefecture("栃木県");
        homeLoc.setMunicipality("宇都宮市");
        homeLoc.setAddressLine("本町1-1");
        assertEquals("居住地", homeLoc.getLocationKindLabel());
        assertEquals("栃木県宇都宮市本町1-1", homeLoc.getFullAddress());

        AdminUserItem.Location workLoc = new AdminUserItem.Location();
        workLoc.setLocationKind("WORK");
        workLoc.setPrefecture("栃木県");
        workLoc.setMunicipality("宇都宮市");
        assertEquals("勤務先・学校", workLoc.getLocationKindLabel());
        assertEquals("栃木県宇都宮市", workLoc.getFullAddress());

        // 管理者セッションと差し替えDAOを準備し、居住地・勤務先/学校の保存と解除および不整合拒否を検証する。
        User admin = new User();
        admin.setUserId(101);
        admin.setDisplayName("管理者");
        admin.setUserRole("RESIDENT");
        admin.setAccountStatus("ACTIVE");
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("user", admin);
        HttpSession session = mockSession(sessionAttrs);
        String csrf = ManagementAuth.csrfToken(session);

        AtomicReference<LocationInput> savedHome = new AtomicReference<>();
        AtomicReference<LocationInput> savedWork = new AtomicReference<>();
        AdminAuditDao stubAuditDao = new AdminAuditDao() {
            @Override
            public boolean isAdmin(int userId) {
                return userId == 101;
            }
        };
        AdminUserDao stubUserDao = new AdminUserDao() {
            @Override
            public void saveLocations(int actorId, int targetUserId, LocationInput home, LocationInput work) {
                savedHome.set(home);
                savedWork.set(work);
            }
        };
        AdminUsersAction action = new AdminUsersAction(stubAuditDao, stubUserDao);

        // 居住地＋勤務先・学校の両方を地図入力形式で送信すると正常に保存・リダイレクトされる。
        ResponseRecorder bothRes = new ResponseRecorder();
        HttpServletRequest bothReq = mockRequest("POST", ManagementPaths.ADMIN_USERS,
                Map.of(
                        "csrfToken", csrf,
                        "id", "102",
                        "operation", "location",
                        "livingAddress", "〒320-0033 栃木県宇都宮市本町1-1",
                        "livingLat", "36.561000",
                        "livingLng", "139.883000",
                        "workAddress", "栃木県宇都宮市駅前通り2-2",
                        "workLat", "36.559000",
                        "workLng", "139.898000"),
                Map.of(), session, null);
        action.execute(bothReq, bothRes.proxy());
        assertEquals(303, bothRes.status);
        assertEquals("栃木県", savedHome.get().prefecture());
        assertEquals("宇都宮市", savedHome.get().municipality());
        assertEquals("本町1-1", savedHome.get().addressLine());
        assertNotNull(savedWork.get());
        assertEquals("WORK", savedWork.get().kind());

        // 勤務先・学校を空欄で送信するとwork=null（解除）として保存される。
        ResponseRecorder clearRes = new ResponseRecorder();
        HttpServletRequest clearReq = mockRequest("POST", ManagementPaths.ADMIN_USERS,
                Map.of(
                        "csrfToken", csrf,
                        "id", "102",
                        "operation", "location",
                        "livingAddress", "栃木県宇都宮市本町1-1",
                        "livingLat", "36.561000",
                        "livingLng", "139.883000",
                        "workAddress", "",
                        "workLat", "",
                        "workLng", ""),
                Map.of(), session, null);
        action.execute(clearReq, clearRes.proxy());
        assertEquals(303, clearRes.status);
        assertNull(savedWork.get());

        // 勤務先・学校の住所だけあって座標が欠けている場合は400エラーとなる。
        ResponseRecorder missingCoordRes = new ResponseRecorder();
        HttpServletRequest missingCoordReq = mockRequest("POST", ManagementPaths.ADMIN_USERS,
                Map.of(
                        "csrfToken", csrf,
                        "id", "102",
                        "operation", "location",
                        "livingAddress", "栃木県宇都宮市本町1-1",
                        "livingLat", "36.561000",
                        "livingLng", "139.883000",
                        "workAddress", "栃木県宇都宮市駅前通り2-2",
                        "workLat", "",
                        "workLng", ""),
                Map.of(), session, null);
        action.execute(missingCoordReq, missingCoordRes.proxy());
        assertEquals(400, missingCoordRes.errorStatus);
    }

    /**
     * 管理者権限ロックSQLがadmin_membersとusersの両方を共有ロックし、ProfileDaoが書類(DOCUMENT)を資格候補から除外することを検証する。
     * @throws Exception モック実行に失敗した場合。
     */
    @Test
    public void adminAuditLocksBothTablesAndProfileDaoFiltersDocumentQualifications() throws Exception {
        List<String> auditSqls = new ArrayList<>();
        Connection auditConn = capturingConnection(auditSqls);
        AdminAuditDao auditDao = new AdminAuditDao(auditConn);
        assertFalse(auditDao.isAdmin(auditConn, 101, true));
        assertEquals(1, auditSqls.size());
        assertTrue(auditSqls.get(0).contains("FOR SHARE OF a, u"));

        List<String> profileSqls = new ArrayList<>();
        Connection profileConn = capturingConnection(profileSqls);
        ProfileDao profileDao = new ProfileDao() {
            @Override
            protected Connection openConnection() {
                return profileConn;
            }
        };
        assertTrue(profileDao.listQualifications().isEmpty());
        assertEquals(1, profileSqls.size());
        assertTrue(profileSqls.get(0).contains("requirement_kind IN ('QUALIFICATION','PERMIT')"));
    }

    /**
     * 退会済み会員でも過去の管理操作履歴が取得されること、事業主の経験欄が本人画面と同一選択肢で検証されること、
     * および保有資格の編集対象が未登録IDなら404になることを検証する。
     * @throws Exception Action実行に失敗した場合。
     */
    @Test
    public void adminUsersKeepsWithdrawnHistoryAndEnforcesBusinessExperienceAndQualificationEdit() throws Exception {
        User admin = new User();
        admin.setUserId(101);
        admin.setDisplayName("管理者");
        admin.setUserRole("RESIDENT");
        admin.setAccountStatus("ACTIVE");
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("user", admin);
        HttpSession session = mockSession(sessionAttrs);
        String csrf = ManagementAuth.csrfToken(session);

        AdminAuditDao stubAuditDao = new AdminAuditDao() {
            @Override
            public boolean isAdmin(int userId) {
                return userId == 101;
            }
        };

        // 1. 退会済み会員のプロフィール画面でも、editable=falseのまま操作履歴(accountActions)が設定されることを確認する。
        AdminUserItem withdrawnMember = new AdminUserItem();
        withdrawnMember.setUserId(200);
        withdrawnMember.setDisplayName("退会会員");
        withdrawnMember.setUserRole("RESIDENT");
        withdrawnMember.setAccountStatus("WITHDRAWN");
        AdminAuditLogItem pastAction = new AdminAuditLogItem();
        pastAction.setAction("SUSPEND");
        pastAction.setReason("過去の停止理由");
        AdminUserDao withdrawnUserDao = new AdminUserDao() {
            @Override
            public AdminUserItem findUserById(int userId) {
                return withdrawnMember;
            }

            @Override
            public AdminUserItem.Profile findRoleProfile(String role, int userId) {
                return new AdminUserItem.Profile();
            }

            @Override
            public List<AdminAuditLogItem> findRecentAccountActions(int userId) {
                return List.of(pastAction);
            }
        };
        Map<String, Object> withdrawnAttrs = new HashMap<>();
        ResponseRecorder withdrawnRes = new ResponseRecorder();
        HttpServletRequest withdrawnReq = mockRequestWithAttrs("GET", ManagementPaths.ADMIN_USERS,
                Map.of("id", "200", "tab", "profile"), Map.of(), session, null, withdrawnAttrs);
        new AdminUsersAction(stubAuditDao, withdrawnUserDao).execute(withdrawnReq, withdrawnRes.proxy());
        assertEquals(Boolean.FALSE, withdrawnAttrs.get("editable"));
        assertEquals(List.of(pastAction), withdrawnAttrs.get("accountActions"));

        // 2. 事業主の経験欄で本人画面と一致しない自由記述は400で拒否し、許可選択肢は受け付けることを確認する。
        AdminUserItem businessMember = new AdminUserItem();
        businessMember.setUserId(105);
        businessMember.setDisplayName("事業主");
        businessMember.setUserRole("BUSINESS");
        businessMember.setAccountStatus("ACTIVE");
        AdminUserItem.Qualification ownedQual = new AdminUserItem.Qualification();
        ownedQual.setQualificationId(100);
        ownedQual.setQualificationName("食品衛生責任者");
        AtomicReference<AdminUserItem.Profile> savedProfile = new AtomicReference<>();
        AdminUserDao businessUserDao = new AdminUserDao() {
            @Override
            public AdminUserItem findUserById(int userId) {
                return businessMember;
            }

            @Override
            public AdminUserItem.Profile findRoleProfile(String role, int userId) {
                return new AdminUserItem.Profile();
            }

            @Override
            public AdminUserItem.Profile findBusinessProfile(int userId) {
                return new AdminUserItem.Profile();
            }

            @Override
            public List<AdminUserItem.Qualification> findQualifications(int userId) {
                return List.of(ownedQual);
            }

            @Override
            public void updateProfile(int actorId, int targetUserId, String displayName, String email,
                    AdminUserItem.Profile roleProfile) {
                savedProfile.set(roleProfile);
            }
        };
        AdminUsersAction businessAction = new AdminUsersAction(stubAuditDao, businessUserDao);

        ResponseRecorder freeTextRes = new ResponseRecorder();
        HttpServletRequest freeTextReq = mockRequest("POST", ManagementPaths.ADMIN_USERS,
                Map.ofEntries(
                        Map.entry("csrfToken", csrf),
                        Map.entry("id", "105"),
                        Map.entry("operation", "profile"),
                        Map.entry("name", "事業主"),
                        Map.entry("email", "biz@example.invalid"),
                        Map.entry("representative", "代表者"),
                        Map.entry("phone", "028-633-9988"),
                        Map.entry("secondaryPhone", ""),
                        Map.entry("businessName", "テスト店舗"),
                        Map.entry("areaName", "宇都宮"),
                        Map.entry("employees", "2"),
                        Map.entry("industryExperience", "飲食店を3年経営"),
                        Map.entry("managementExperience", "あり")),
                Map.of(), session, null);
        businessAction.execute(freeTextReq, freeTextRes.proxy());
        assertEquals(400, freeTextRes.errorStatus);

        ResponseRecorder validExpRes = new ResponseRecorder();
        HttpServletRequest validExpReq = mockRequest("POST", ManagementPaths.ADMIN_USERS,
                Map.ofEntries(
                        Map.entry("csrfToken", csrf),
                        Map.entry("id", "105"),
                        Map.entry("operation", "profile"),
                        Map.entry("name", "事業主"),
                        Map.entry("email", "biz@example.invalid"),
                        Map.entry("representative", "代表者"),
                        Map.entry("phone", "028-633-9988"),
                        Map.entry("secondaryPhone", ""),
                        Map.entry("businessName", "テスト店舗"),
                        Map.entry("areaName", "宇都宮"),
                        Map.entry("employees", "2"),
                        Map.entry("industryExperience", "3〜5年未満"),
                        Map.entry("managementExperience", "あり")),
                Map.of(), session, null);
        businessAction.execute(validExpReq, validExpRes.proxy());
        assertEquals(303, validExpRes.status);
        assertEquals("3〜5年未満", savedProfile.get().getIndustryExperience());
        assertEquals("あり", savedProfile.get().getManagementExperience());

        // 3. 保有資格編集で未登録の資格IDを指定すると404となり、登録済みIDなら編集モード(固定対象)となる。
        ResponseRecorder missingQualRes = new ResponseRecorder();
        HttpServletRequest missingQualReq = mockRequestWithAttrs("GET", ManagementPaths.ADMIN_USERS,
                Map.of("id", "105", "tab", "qualifications", "industry", "999"), Map.of(), session, null, new HashMap<>());
        businessAction.execute(missingQualReq, missingQualRes.proxy());
        assertEquals(404, missingQualRes.errorStatus);

        Map<String, Object> editQualAttrs = new HashMap<>();
        ResponseRecorder editQualRes = new ResponseRecorder();
        HttpServletRequest editQualReq = mockRequestWithAttrs("GET", ManagementPaths.ADMIN_USERS,
                Map.of("id", "105", "tab", "qualifications", "industry", "100"), Map.of(), session, null, editQualAttrs);
        businessAction.execute(editQualReq, editQualRes.proxy());
        assertEquals(Boolean.TRUE, editQualAttrs.get("editingQualification"));
        assertEquals(ownedQual, editQualAttrs.get("currentQualification"));
    }

    /**
     * 実行されたSQL文字列を記録し空の結果セットを返すテスト用Connectionプロキシを作成する。
     * @param executedSqls 実行されたSQLを追記するリスト。
     * @return モックConnection。
     */
    private static Connection capturingConnection(List<String> executedSqls) {
        ResultSet emptyRs = (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] { ResultSet.class },
                (proxy, m, args) -> "next".equals(m.getName()) ? Boolean.FALSE : null);
        PreparedStatement mockStmt = (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[] { PreparedStatement.class },
                (proxy, m, args) -> "executeQuery".equals(m.getName()) ? emptyRs : null);
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                (proxy, m, args) -> {
                    if ("prepareStatement".equals(m.getName()) && args != null && args.length >= 1) {
                        executedSqls.add((String) args[0]);
                        return mockStmt;
                    }
                    return null;
                });
    }

    /**
     * テスト用のHttpServletRequestプロキシを作成する。
     * @param method HTTPメソッド。
     * @param servletPath サーブレットパス。
     * @param params 単一パラメータマップ。
     * @param multiParams 複数値パラメータマップ。
     * @param session セッション。
     * @param context ServletContext。
     * @return モックリクエスト。
     */
    private static HttpServletRequest mockRequest(String method, String servletPath,
            Map<String, String> params, Map<String, String[]> multiParams,
            HttpSession session, ServletContext context) {
        return mockRequestWithAttrs(method, servletPath, params, multiParams, session, context, new HashMap<>());
    }

    /**
     * リクエスト属性の記録とダミーのforward処理を備えたテスト用HttpServletRequestプロキシを作成する。
     * @param method HTTPメソッド。
     * @param servletPath サーブレットパス。
     * @param params 単一パラメータマップ。
     * @param multiParams 複数値パラメータマップ。
     * @param session セッション。
     * @param context ServletContext。
     * @param attrs setAttributeで設定された属性を記録するマップ。
     * @return モックリクエスト。
     */
    private static HttpServletRequest mockRequestWithAttrs(String method, String servletPath,
            Map<String, String> params, Map<String, String[]> multiParams,
            HttpSession session, ServletContext context, Map<String, Object> attrs) {
        RequestDispatcher noopDispatcher = (RequestDispatcher) Proxy.newProxyInstance(
                RequestDispatcher.class.getClassLoader(),
                new Class<?>[] { RequestDispatcher.class },
                (proxy, m, args) -> null);
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, m, args) -> switch (m.getName()) {
                    case "getMethod" -> method;
                    case "getServletPath" -> servletPath;
                    case "getContextPath" -> "/machiriku";
                    case "getParameter" -> params.get(args[0]);
                    case "getParameterValues" -> multiParams.get(args[0]);
                    case "getSession" -> session;
                    case "getServletContext" -> context;
                    case "getAttribute" -> attrs.get(args[0]);
                    case "setAttribute" -> {
                        attrs.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "getRequestDispatcher" -> noopDispatcher;
                    default -> null;
                });
    }

    /**
     * テスト用のHttpSessionプロキシを作成する。
     * @param attributes セッション属性を保持するマップ。
     * @return モックセッション。
     */
    private static HttpSession mockSession(Map<String, Object> attributes) {
        return (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[] { HttpSession.class },
                (proxy, m, args) -> switch (m.getName()) {
                    case "getAttribute" -> attributes.get(args[0]);
                    case "setAttribute" -> {
                        attributes.put((String) args[0], args[1]);
                        yield null;
                    }
                    default -> null;
                });
    }

    /**
     * テスト用のServletContextプロキシを作成する。
     * @param logs log()呼び出し内容を記録するリスト。
     * @return モックServletContext。
     */
    private static ServletContext mockContext(List<String> logs) {
        return (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(),
                new Class<?>[] { ServletContext.class },
                (proxy, m, args) -> {
                    if ("log".equals(m.getName()) && args != null && args.length >= 1) {
                        logs.add(String.valueOf(args[0]));
                    }
                    return null;
                });
    }

    /**
     * HttpServletResponseへのステータス・エラー・ヘッダー・リダイレクト呼び出しを記録するテスト補助クラス。
     */
    private static final class ResponseRecorder {
        /** setStatusで設定されたステータスコード。 */
        private int status;
        /** sendErrorで設定されたステータスコード。 */
        private int errorStatus;
        /** sendErrorで設定されたエラーメッセージ。 */
        private String errorMessage;
        /** sendRedirectで設定された遷移先URL。 */
        private String redirectLocation;
        /** setHeaderで設定されたヘッダー群。 */
        private final Map<String, String> headers = new HashMap<>();

        /**
         * 記録機能付きのHttpServletResponseプロキシを返す。
         * @return モックレスポンス。
         */
        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] { HttpServletResponse.class },
                    (proxy, m, args) -> switch (m.getName()) {
                        case "setStatus" -> {
                            status = (Integer) args[0];
                            yield null;
                        }
                        case "sendError" -> {
                            errorStatus = (Integer) args[0];
                            errorMessage = args.length > 1 ? (String) args[1] : null;
                            yield null;
                        }
                        case "sendRedirect" -> {
                            redirectLocation = (String) args[0];
                            yield null;
                        }
                        case "setHeader" -> {
                            headers.put((String) args[0], (String) args[1]);
                            yield null;
                        }
                        default -> null;
                    });
        }
    }
}
