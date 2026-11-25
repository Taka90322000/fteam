package jp.machiriku.servlet.property;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.Test;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.PropertyInput;
import jp.machiriku.bean.User;
import jp.machiriku.dao.ProfileDao;
import jp.machiriku.dao.PropertyDao;
import jp.machiriku.property.PropertyImageStore;

/**
 * PropertyAddActionの認証・ロール制御・CSRF検証・画像保存・ロールバック・PRGリダイレクトを検証するテスト。
 */
public class PropertyAddActionTest {
    /** テスト用の固定CSRFトークン。 */
    private static final String CSRF_TOKEN = "test-property-add-csrf-token";

    /**
     * 未ログイン利用者はログイン画面へリダイレクトされることを検証する。
     *
     * @throws Exception Action実行に失敗した場合
     */
    @Test
    public void loggedOutUserRedirectsToLogin() throws Exception {
        Harness harness = new Harness("GET");
        harness.hasSession = false;
        execute(harness, new FakeProfileDao(null), new FakePropertyDao(), null);
        assertEquals("/machiriku/jp/machiriku/servlet/auth/Login.action", harness.redirect);
        assertNull(harness.forwarded);
    }

    /**
     * オーナー以外のロール（RESIDENT / BUSINESS）は403で拒否されることを検証する。
     *
     * @throws Exception Action実行に失敗した場合
     */
    @Test
    public void nonOwnerRolesReceiveForbidden() throws Exception {
        for (String role : List.of("RESIDENT", "BUSINESS")) {
            Harness harness = authenticated("GET", role, "ACTIVE");
            execute(harness, new FakeProfileDao(member(harness)), new FakePropertyDao(), null);
            assertEquals(HttpServletResponse.SC_FORBIDDEN, harness.status);
            assertNull(harness.forwarded);
        }
    }

    /**
     * 本登録前（PENDING）のオーナーは初期設定画面へ誘導されることを検証する。
     *
     * @throws Exception Action実行に失敗した場合
     */
    @Test
    public void pendingOwnerRedirectsToOwnerSetup() throws Exception {
        Harness harness = authenticated("GET", "OWNER", "PENDING");
        execute(harness, new FakeProfileDao(member(harness)), new FakePropertyDao(), null);
        assertEquals("/machiriku/jp/machiriku/servlet/auth/OwnerSetup.action", harness.redirect);
        assertNull(harness.forwarded);
    }

    /**
     * 本登録済みオーナーのGETリクエストではCSRFトークンを発行しadd.jspへフォワードすることを検証する。
     *
     * @throws Exception Action実行に失敗した場合
     */
    @Test
    public void activeOwnerGetForwardsToAddJspWithCsrfToken() throws Exception {
        Harness harness = authenticated("GET", "OWNER", "ACTIVE");
        execute(harness, new FakeProfileDao(member(harness)), new FakePropertyDao(), null);
        assertEquals("/property/add.jsp", harness.forwarded);
        assertNotNull(harness.attributes.get("csrfToken"));
        assertFalse(harness.attributes.get("csrfToken").toString().isBlank());
    }

    /**
     * CSRFトークン不一致のPOSTは403で拒否され、DAOが呼ばれないことを検証する。
     *
     * @throws Exception Action実行に失敗した場合
     */
    @Test
    public void postWithInvalidCsrfReturnsForbidden() throws Exception {
        Harness harness = authenticated("POST", "OWNER", "ACTIVE");
        harness.sessionValues.put(PropertyAddAction.CSRF_KEY, CSRF_TOKEN);
        harness.parameters.put("csrfToken", "wrong-token");
        FakePropertyDao propertyDao = new FakePropertyDao();

        execute(harness, new FakeProfileDao(member(harness)), propertyDao, null);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, harness.status);
        assertEquals(0, propertyDao.calls);
    }

    /**
     * 有効なPOST送信で画像保存とDB登録が実行され、管理画面へPRGリダイレクトすることを検証する。
     *
     * @throws Exception テスト実行または一時ディレクトリ操作に失敗した場合
     */
    @Test
    public void validOwnerPostSavesImagesAndRedirectsToManage() throws Exception {
        Path tempDir = Files.createTempDirectory("machiriku-action-img-");
        try {
            Harness harness = authenticated("POST", "OWNER", "ACTIVE");
            prepareCsrf(harness);
            populateValidParams(harness);
            harness.parameters.put("galleryJson",
                    "[{\"kind\":\"exterior\",\"label\":\"正面外観\",\"url\":\"" + createPngDataUrl() + "\"}]");

            FakePropertyDao propertyDao = new FakePropertyDao();
            propertyDao.nextId = 77;
            PropertyImageStore imageStore = new PropertyImageStore(tempDir);

            execute(harness, new FakeProfileDao(member(harness)), propertyDao, imageStore);

            assertEquals("/machiriku/property/manage.jsp?property_added=1&property_id=77", harness.redirect);
            assertEquals(1, propertyDao.calls);
            assertEquals(42, propertyDao.lastOwnerId);
            assertNotNull(propertyDao.lastInput);
            assertEquals("オリオン通りサウスビル 1階/101", propertyDao.lastInput.propertyName());
            assertEquals(1, propertyDao.lastInput.images().size());
            assertEquals("EXTERIOR", propertyDao.lastInput.images().get(0).imageKind());
            assertTrue(PropertyImageStore.manages(propertyDao.lastInput.images().get(0).storageKey()));
        } finally {
            deleteRecursively(tempDir);
        }
    }

    /**
     * DB登録でSQLExceptionが発生した際に、先に書き込んだ画像ファイルを削除してadd.jspへ戻すことを検証する。
     *
     * @throws Exception テスト実行または一時ディレクトリ操作に失敗した場合
     */
    @Test
    public void databaseFailureDeletesUploadedImagesAndRestoresDraft() throws Exception {
        Path tempDir = Files.createTempDirectory("machiriku-action-rollback-");
        try {
            Harness harness = authenticated("POST", "OWNER", "ACTIVE");
            prepareCsrf(harness);
            populateValidParams(harness);
            harness.parameters.put("galleryJson",
                    "[{\"kind\":\"interior\",\"label\":\"店内\",\"url\":\"" + createPngDataUrl() + "\"}]");

            FakePropertyDao propertyDao = new FakePropertyDao();
            propertyDao.failure = new SQLException("constraint violation", "23514");
            PropertyImageStore imageStore = new PropertyImageStore(tempDir);

            execute(harness, new FakeProfileDao(member(harness)), propertyDao, imageStore);

            assertNull(harness.redirect);
            assertEquals("/property/add.jsp", harness.forwarded);
            assertEquals(Boolean.TRUE, harness.attributes.get("restoreDraft"));
            assertNotNull(harness.attributes.get("errorMessage"));

            // 保存済みだった画像ファイルがロールバック時に削除されていることを確認する。
            Path ownerDir = tempDir.resolve("42");
            if (Files.exists(ownerDir)) {
                try (var stream = Files.list(ownerDir)) {
                    assertEquals(0L, stream.count());
                }
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    /**
     * テスト用HarnessでPropertyAddActionを実行する。
     *
     * @param harness リクエスト・レスポンス模擬オブジェクト
     * @param profileDao 会員取得DAOの偽装実装
     * @param propertyDao 物件登録DAOの偽装実装
     * @param imageStore 物件画像ストア
     * @throws Exception Action実行に失敗した場合
     */
    private static void execute(
            Harness harness,
            ProfileDao profileDao,
            PropertyDao propertyDao,
            PropertyImageStore imageStore) throws Exception {
        new PropertyAddAction(profileDao, propertyDao, imageStore).execute(harness.request, harness.response);
    }

    /**
     * 認証済みセッションを持つHarnessを生成する。
     *
     * @param method HTTPメソッド
     * @param role 会員種別
     * @param accountStatus 利用状態
     * @return 初期化済みHarness
     */
    private static Harness authenticated(String method, String role, String accountStatus) {
        Harness harness = new Harness(method);
        User user = new User();
        user.setUserId(42);
        user.setUserRole(role);
        user.setAccountStatus(accountStatus);
        user.setDisplayName("テストオーナー");
        harness.sessionValues.put("user", user);
        harness.sessionValues.put("userRole", role.toLowerCase(Locale.ROOT));
        return harness;
    }

    /**
     * セッションとパラメーターに一致するCSRFトークンを設定する。
     *
     * @param harness 設定対象Harness
     */
    private static void prepareCsrf(Harness harness) {
        harness.sessionValues.put(PropertyAddAction.CSRF_KEY, CSRF_TOKEN);
        harness.parameters.put("csrfToken", CSRF_TOKEN);
    }

    /**
     * 有効な物件登録パラメーター一式をHarnessへ設定する。
     *
     * @param harness 設定対象Harness
     */
    private static void populateValidParams(Harness harness) {
        harness.parameters.put("title", "オリオン通りサウスビル 1階/101");
        harness.parameters.put("prefecture", "栃木県");
        harness.parameters.put("municipality", "宇都宮市");
        harness.parameters.put("addressLine", "江野町5-12");
        harness.parameters.put("access", "東武宇都宮駅 徒歩3分");
        harness.parameters.put("rent", "198000");
        harness.parameters.put("areaSqm", "52.8");
        harness.parameters.put("longEnabled", "on");
        harness.parameters.put("listingStatus", "OPEN");
        harness.multiple.put("allowedIndustries", new String[] { "I0101" });
        harness.multiple.put("facilityCodes", new String[] { "hasInuki" });
    }

    /**
     * セッションからテスト会員を取り出す。
     *
     * @param harness 対象Harness
     * @return セッション内の会員オブジェクト
     */
    private static User member(Harness harness) {
        return (User) harness.sessionValues.get("user");
    }

    /**
     * テスト用の小さなPNG画像のData URLを生成する。
     *
     * @return PNGのData URL
     * @throws Exception 画像生成に失敗した場合
     */
    private static String createPngDataUrl() throws Exception {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.GREEN);
            g.fillRect(0, 0, 32, 32);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", out));
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /**
     * 一時ディレクトリを再帰削除する。
     *
     * @param root 削除対象ディレクトリ
     * @throws Exception 削除に失敗した場合
     */
    private static void deleteRecursively(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            for (Path target : stream.sorted((a, b) -> b.compareTo(a)).toList()) {
                Files.deleteIfExists(target);
            }
        }
    }

    /** 会員情報取得をテスト用に差し替える偽装ProfileDao。 */
    private static final class FakeProfileDao extends ProfileDao {
        /** テストで返却する会員情報。 */
        private final User member;

        /**
         * 返却する会員を指定して生成する。
         *
         * @param member 返却する会員情報
         */
        FakeProfileDao(User member) {
            this.member = member;
        }

        @Override
        public User findUserById(int userId) {
            return member;
        }
    }

    /** 物件登録呼び出しを記録・例外送出する偽装PropertyDao。 */
    private static final class FakePropertyDao extends PropertyDao {
        /** 呼び出し回数。 */
        int calls;
        /** 最後に渡されたオーナーID。 */
        int lastOwnerId;
        /** 最後に渡された物件データ。 */
        PropertyInput lastInput;
        /** 成功時に返却する物件ID。 */
        int nextId = 1;
        /** 設定されていれば送出するSQLException。 */
        SQLException failure;

        @Override
        public int createProperty(int ownerUserId, PropertyInput input) throws SQLException {
            calls++;
            lastOwnerId = ownerUserId;
            lastInput = input;
            if (failure != null) {
                throw failure;
            }
            return nextId;
        }
    }

    /** Servlet APIを動的プロキシで模擬するテスト用ハーネス。 */
    private static final class Harness {
        /** 単一値パラメーター。 */
        final Map<String, String> parameters = new HashMap<>();
        /** 複数値パラメーター。 */
        final Map<String, String[]> multiple = new HashMap<>();
        /** リクエスト属性。 */
        final Map<String, Object> attributes = new HashMap<>();
        /** セッション属性。 */
        final Map<String, Object> sessionValues = new HashMap<>();
        /** セッションの有無。 */
        boolean hasSession = true;
        /** リダイレクト先URL。 */
        String redirect;
        /** フォワード先パス。 */
        String forwarded;
        /** エラーステータスコード。 */
        int status;
        /** 模擬セッション。 */
        final HttpSession session;
        /** 模擬リクエスト。 */
        final HttpServletRequest request;
        /** 模擬レスポンス。 */
        final HttpServletResponse response;

        /**
         * 指定HTTPメソッドでテスト用ハーネスを初期化する。
         *
         * @param method HTTPメソッド
         */
        Harness(String method) {
            session = proxy(HttpSession.class, (ignored, call, args) -> {
                String name = call.getName();
                if (!hasSession) {
                    throw new IllegalStateException("Session expired");
                }
                if ("getAttribute".equals(name)) {
                    return sessionValues.get(args[0]);
                }
                if ("setAttribute".equals(name)) {
                    sessionValues.put((String) args[0], args[1]);
                    return null;
                }
                if ("removeAttribute".equals(name)) {
                    sessionValues.remove(args[0]);
                    return null;
                }
                return null;
            });

            ServletContext context = proxy(ServletContext.class, (ignored, call, args) -> null);

            request = proxy(HttpServletRequest.class, (ignored, call, args) -> {
                String name = call.getName();
                if ("getMethod".equals(name)) {
                    return method;
                }
                if ("getContextPath".equals(name)) {
                    return "/machiriku";
                }
                if ("getParameter".equals(name)) {
                    return parameters.get(args[0]);
                }
                if ("getParameterValues".equals(name)) {
                    return multiple.get(args[0]);
                }
                if ("getAttribute".equals(name)) {
                    return attributes.get(args[0]);
                }
                if ("setAttribute".equals(name)) {
                    attributes.put((String) args[0], args[1]);
                    return null;
                }
                if ("getSession".equals(name)) {
                    return hasSession ? session : null;
                }
                if ("setCharacterEncoding".equals(name)) {
                    return null;
                }
                if ("getServletContext".equals(name)) {
                    return context;
                }
                if ("getRequestDispatcher".equals(name)) {
                    String view = (String) args[0];
                    return proxy(RequestDispatcher.class, (d, dc, da) -> {
                        if ("forward".equals(dc.getName())) {
                            forwarded = view;
                        }
                        return null;
                    });
                }
                return null;
            });

            response = proxy(HttpServletResponse.class, (ignored, call, args) -> {
                String name = call.getName();
                if ("sendRedirect".equals(name)) {
                    redirect = (String) args[0];
                    return null;
                }
                if ("sendError".equals(name)) {
                    status = (Integer) args[0];
                    return null;
                }
                if ("setHeader".equals(name)) {
                    return null;
                }
                return null;
            });
        }

        /**
         * 指定インターフェースの動的プロキシを生成する。
         *
         * @param <T> 対象インターフェース型
         * @param type インターフェースクラス
         * @param handler 呼び出しハンドラー
         * @return プロキシインスタンス
         */
        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
        }
    }
}
