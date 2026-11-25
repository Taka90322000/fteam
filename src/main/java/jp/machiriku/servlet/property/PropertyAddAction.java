package jp.machiriku.servlet.property;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.PropertyImageInput;
import jp.machiriku.bean.User;
import jp.machiriku.dao.ProfileDao;
import jp.machiriku.dao.PropertyDao;
import jp.machiriku.property.PropertyForms;
import jp.machiriku.property.PropertyForms.ParsedProperty;
import jp.machiriku.property.PropertyImageStore;
import jp.machiriku.servlet.MemberSession;
import jp.machiriku.tool.Action;

/**
 * 物件登録画面（property/add.jsp）の表示および確認画面からのDB登録確定を処理するAction。
 * /jp/machiriku/servlet/property/PropertyAdd.action に対応する。
 */
public class PropertyAddAction extends Action {
    /** セッションへ保存する物件登録用CSRFトークンの属性名。 */
    public static final String CSRF_KEY = "propertyAddCsrfToken";

    /** CSRFトークン生成に使用する暗号論的乱数生成器。 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** フォームから読み取る単一値パラメーター名一覧。 */
    private static final List<String> SINGLE_FIELD_NAMES = List.of(
            "title", "prefecture", "municipality", "addressLine", "access",
            "lat", "lng", "rent", "managementFee", "deposit", "keyMoney", "securityDeposit",
            "areaSqm", "floorNumber", "totalFloors", "structure",
            "builtYearNum", "builtMonthNum", "builtYear", "handover",
            "longEnabled", "popupEnabled", "popupMinDays", "popupMaxDays", "popupDailyRent",
            "permitStatus", "listingStatus", "condition", "description", "listingUrl",
            "infra_hasInuki", "infra_hasParking", "infra_hasBarrierFree", "infra_hasPrivateToilet",
            "infra_hasIndividualAc", "infra_hasPower", "infra_hasCityGas", "infra_hasWaterDrainage",
            "infra_hasGreaseTrap", "infra_hasExhaustDuct");

    /** 認証状態の再確認に使用するプロフィールDAO（テスト差し替え用）。 */
    private final ProfileDao profileDao;

    /** 物件登録に使用する物件DAO（テスト差し替え用）。 */
    private final PropertyDao propertyDao;

    /** 物件画像の永続化に使用する画像ストア（テスト差し替え用）。 */
    private final PropertyImageStore imageStore;

    /**
     * FrontControllerから呼び出される既定コンストラクター。
     */
    public PropertyAddAction() {
        this(null, null, null);
    }

    /**
     * テスト用にDAOと画像ストアを差し替えられるコンストラクター。
     *
     * @param profileDao 会員状態確認用DAO（nullなら既定実装を生成）
     * @param propertyDao 物件登録用DAO（nullなら既定実装を生成）
     * @param imageStore 物件画像保存用ストア（nullなら設定既定を使用）
     */
    public PropertyAddAction(ProfileDao profileDao, PropertyDao propertyDao, PropertyImageStore imageStore) {
        this.profileDao = profileDao;
        this.propertyDao = propertyDao;
        this.imageStore = imageStore;
    }

    /**
     * セッション単位の物件登録用CSRFトークンを発行または取得する。
     *
     * @param session 現在のHTTPセッション
     * @return 16進文字列のCSRFトークン（セッションがnullなら空文字）
     */
    public static String csrfToken(HttpSession session) {
        if (session == null) {
            return "";
        }
        // 同時リクエストでも同一トークンを安全に初期化できるようセッション単位で同期する。
        synchronized (session) {
            if (!(session.getAttribute(CSRF_KEY) instanceof String)) {
                byte[] randomBytes = new byte[32];
                RANDOM.nextBytes(randomBytes);
                session.setAttribute(CSRF_KEY, HexFormat.of().formatHex(randomBytes));
            }
            return (String) session.getAttribute(CSRF_KEY);
        }
    }

    /**
     * GETで物件登録画面を表示し、POSTで入力検証・画像保存・DB登録を実行する。
     *
     * @param req HTTPリクエスト
     * @param res HTTPレスポンス
     * @throws Exception 画面転送またはリダイレクトに失敗した場合
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 1. 認証済み会員かつ本登録済みのオーナーであることをDBの最新状態で確認する。
        HttpSession session = req.getSession(false);
        User user = requireActiveOwner(req, res, session);
        if (user == null) {
            return;
        }

        // 2. GETとPOST以外のHTTPメソッドを拒否する。
        String method = req.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // 3. GET時はCSRFトークンを設定して登録画面へフォワードする。
        if ("GET".equalsIgnoreCase(method)) {
            forwardToForm(req, res, session, false);
            return;
        }

        // 4. POST時は文字コードとCSRFトークンを検証する。
        req.setCharacterEncoding("UTF-8");
        if (!validCsrf(req, session)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 5. リクエストパラメーターを集約し、検証・画像保存・DB登録を実行する。
        Map<String, String> fields = collectSingleFields(req);
        List<String> allowedIndustries = parameterList(req, "allowedIndustries");
        List<String> facilityCodes = parameterList(req, "facilityCodes");
        String galleryJson = req.getParameter("galleryJson");

        PropertyImageStore activeStore = resolveImageStore();
        List<PropertyImageInput> savedImages = List.of();
        try {
            ParsedProperty parsed = PropertyForms.parseForCreate(fields, allowedIndustries, facilityCodes, galleryJson);
            // 先に画像をディスクへ保存し、DB登録に失敗した場合は後続のcatchで片付ける。
            savedImages = activeStore.saveAll(user.getUserId(), parsed.photos());
            int propertyId = createPropertyDao().createProperty(
                    user.getUserId(),
                    parsed.property().withImages(savedImages));

            // 登録成功後はPRGパターンで管理画面へリダイレクトし、二重送信を防ぐ。
            res.sendRedirect(req.getContextPath() + "/property/manage.jsp?property_added=1&property_id=" + propertyId);
            return;
        } catch (IllegalArgumentException error) {
            // 入力検証または画像形式検証エラーの場合は保存済み画像を掃除して入力画面へ戻す。
            discardImages(req, activeStore, user.getUserId(), savedImages);
            req.setAttribute("errorMessage", error.getMessage());
        } catch (IOException error) {
            // 画像保存のI/O失敗時は機密なパスを含めずログを残し、利用者向けメッセージを表示する。
            discardImages(req, activeStore, user.getUserId(), savedImages);
            req.getServletContext().log("Could not save property images.");
            req.setAttribute("errorMessage", "物件写真を保存できませんでした。時間をおいて再度お試しください。");
        } catch (SQLException error) {
            // 接続断などでコミット結果が不明な場合を除き、孤立した画像ファイルを片付ける。
            String sqlState = error.getSQLState();
            if (sqlState != null && !sqlState.startsWith("08") && !"40003".equals(sqlState)) {
                discardImages(req, activeStore, user.getUserId(), savedImages);
            }
            req.getServletContext().log("Property registration failed (SQLState=" + sqlState + ").");
            if ("28000".equals(sqlState)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            req.setAttribute("errorMessage", "物件情報を登録できませんでした。入力内容をご確認の上、もう一度お試しください。");
        }

        // エラー時はドラフト復元フラグ付きで入力画面へ戻し、入力内容と写真編集をやり直せるようにする。
        forwardToForm(req, res, session, true);
    }

    /**
     * プロフィールDAOを取得する。
     *
     * @return ProfileDao インスタンス
     */
    protected ProfileDao createProfileDao() {
        return profileDao != null ? profileDao : new ProfileDao();
    }

    /**
     * 物件DAOを取得する。
     *
     * @return PropertyDao インスタンス
     */
    protected PropertyDao createPropertyDao() {
        return propertyDao != null ? propertyDao : new PropertyDao();
    }

    /**
     * 物件画像ストアを取得する。
     *
     * @return PropertyImageStore インスタンス
     */
    protected PropertyImageStore resolveImageStore() {
        return imageStore != null ? imageStore : PropertyImageStore.configured();
    }

    /**
     * 本登録済み（ACTIVE）の物件オーナー（OWNER）であることを確認する。
     *
     * @param req HTTPリクエスト
     * @param res HTTPレスポンス
     * @param session 現在のセッション
     * @return 確認済みのオーナー会員情報（リダイレクト・エラー応答済みならnull）
     * @throws Exception リダイレクトやエラー応答の送信に失敗した場合
     */
    private User requireActiveOwner(HttpServletRequest req, HttpServletResponse res, HttpSession session) throws Exception {
        User user = MemberSession.requireCurrentUser(req, res, session, this::createProfileDao);
        if (user == null) {
            return null;
        }
        // 本登録前（PENDING）の会員は初期設定画面へ誘導する。
        if ("PENDING".equals(user.getAccountStatus())) {
            String setupAction = switch (user.getUserRole()) {
                case "BUSINESS" -> "BusinessSetup";
                case "OWNER" -> "OwnerSetup";
                default -> "ResidentSetup";
            };
            res.sendRedirect(req.getContextPath() + "/jp/machiriku/servlet/auth/" + setupAction + ".action");
            return null;
        }
        // 物件登録はオーナー会員のみに限定する。
        if (!"OWNER".equals(user.getUserRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return user;
    }

    /**
     * リクエストのCSRFトークンをセッション値と定数時間で比較検証する。
     *
     * @param req HTTPリクエスト
     * @param session 現在のHTTPセッション
     * @return 一致する場合はtrue
     */
    private static boolean validCsrf(HttpServletRequest req, HttpSession session) {
        if (session == null) {
            return false;
        }
        try {
            if (!(session.getAttribute(CSRF_KEY) instanceof String expected)) {
                return false;
            }
            String actual = req.getParameter("csrfToken");
            return actual != null && MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    actual.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalStateException expired) {
            return false;
        }
    }

    /**
     * 物件フォームの単一値パラメーターをマップへ集約する。
     *
     * @param req HTTPリクエスト
     * @return 項目名と入力値のマップ
     */
    private static Map<String, String> collectSingleFields(HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String name : SINGLE_FIELD_NAMES) {
            String value = req.getParameter(name);
            if (value != null) {
                fields.put(name, value);
            }
        }
        return fields;
    }

    /**
     * 複数値パラメーターを安全に不変リストとして取得する。
     *
     * @param req HTTPリクエスト
     * @param name パラメーター名
     * @return 取得した値のリスト（未送信時は空リスト）
     */
    private static List<String> parameterList(HttpServletRequest req, String name) {
        String[] values = req.getParameterValues(name);
        if (values == null || values.length == 0) {
            return List.of();
        }
        return Arrays.asList(values);
    }

    /**
     * CSRFトークンを設定して物件登録画面（add.jsp）へフォワードする。
     *
     * @param req HTTPリクエスト
     * @param res HTTPレスポンス
     * @param session 現在のセッション
     * @param restoreDraft ドラフト復元を行う場合はtrue
     * @throws Exception フォワードまたはリダイレクトに失敗した場合
     */
    private static void forwardToForm(
            HttpServletRequest req,
            HttpServletResponse res,
            HttpSession session,
            boolean restoreDraft) throws Exception {
        try {
            req.setAttribute("csrfToken", csrfToken(session));
        } catch (IllegalStateException expired) {
            MemberSession.redirectToLogin(req, res);
            return;
        }
        if (restoreDraft) {
            req.setAttribute("restoreDraft", Boolean.TRUE);
        }
        req.getRequestDispatcher("/property/add.jsp").forward(req, res);
    }

    /**
     * DB登録に失敗した際に、先に保存した物件画像ファイルを安全に削除する。
     *
     * @param req ログ記録用のHTTPリクエスト
     * @param store 物件画像ストア
     * @param ownerUserId オーナー会員ID
     * @param savedImages 削除対象の保存済み画像一覧
     */
    private static void discardImages(
            HttpServletRequest req,
            PropertyImageStore store,
            int ownerUserId,
            List<PropertyImageInput> savedImages) {
        if (savedImages == null || savedImages.isEmpty()) {
            return;
        }
        List<String> keys = savedImages.stream().map(PropertyImageInput::storageKey).toList();
        try {
            store.deleteAll(ownerUserId, keys);
        } catch (IOException cleanupError) {
            // 削除失敗で元のエラー応答を覆い隠さないようログのみ記録する。
            req.getServletContext().log("Could not delete unused property images.");
        }
    }
}
