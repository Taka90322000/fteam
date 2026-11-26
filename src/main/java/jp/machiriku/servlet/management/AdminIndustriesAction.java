package jp.machiriku.servlet.management;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminIndustryItem;
import jp.machiriku.bean.management.ManagementProblem;
import jp.machiriku.bean.management.PageResult;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.dao.management.AdminCatalogDao;
import jp.machiriku.tool.Action;

/** 管理者が業種と業種カテゴリを検索し、名称や有効状態を登録・更新するAction。 */
public final class AdminIndustriesAction extends Action {
    /** 有効状態フィルタおよび編集欄で使う選択肢。 */
    private static final Map<String, String> ACTIVE_CHOICES = ManagementFormat.options("true", "有効", "false", "無効");

    /** 管理者権限チェックに使うDAO。 */
    private final AdminAuditDao auditDao;
    /** 業種・カテゴリマスターDAO。 */
    private final AdminCatalogDao catalogDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminIndustriesAction() {
        this(new AdminAuditDao(), new AdminCatalogDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param auditDao 管理者権限チェックDAO。
     * @param catalogDao 業種・カテゴリマスターDAO。
     */
    AdminIndustriesAction(AdminAuditDao auditDao, AdminCatalogDao catalogDao) {
        this.auditDao = auditDao;
        this.catalogDao = catalogDao;
    }

    /**
     * GETでは業種・カテゴリの一覧または登録・編集画面を表示し、POSTでは保存を実行する。
     * @param req FrontControllerから受け取るHTTPリクエスト。
     * @param res 画面表示、エラー応答や更新後の遷移に使用するHTTPレスポンス。
     * @throws Exception 画面処理を完了できない場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 1. 管理者権限を確認する。
        User admin = ManagementAuth.requireAdmin(req, res, auditDao);
        if (admin == null) {
            return;
        }

        // 2. POSTの場合は業種またはカテゴリの保存を実行する。
        if ("POST".equals(req.getMethod())) {
            handlePost(req, res, admin);
            return;
        }

        // 3. GET/HEADでは一覧画面または登録・編集画面を表示する。
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }
        try {
            FormReader input = new FormReader(req);
            String kind = input.value("kind").isEmpty()
                    ? "type"
                    : input.choice("kind", "管理する項目", "category", "type");
            boolean isCategory = "category".equals(kind);
            Integer id = input.intId("id", false);

            if (id != null || "1".equals(input.value("new"))) {
                showEdit(req, res, kind, isCategory, id);
            } else {
                showList(req, res, input, kind, isCategory);
            }
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * 業種またはカテゴリの検索・一覧画面を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @param kind 管理種別（type / category）。
     * @param isCategory カテゴリ一覧の場合はtrue。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showList(HttpServletRequest req, HttpServletResponse res, FormReader input,
            String kind, boolean isCategory) throws Exception {
        String query = input.search();
        String status = input.value("status");
        List<AdminIndustryItem> rows = isCategory
                ? catalogDao.findCategories(query, status, FormReader.PAGE_SIZE + 1, input.offset())
                : catalogDao.findIndustryTypes(query, status, FormReader.PAGE_SIZE + 1, input.offset());
        PageResult<AdminIndustryItem> pageResult = input.toPageResult(rows);

        ManagementAuth.preparePage(req, "業種管理", true, true);
        req.setAttribute("kind", kind);
        req.setAttribute("isCategory", isCategory);
        req.setAttribute("query", query);
        req.setAttribute("status", status);
        req.setAttribute("activeChoices", ACTIVE_CHOICES);
        req.setAttribute("industryPage", pageResult);
        req.getRequestDispatcher("/WEB-INF/views/management/admin/industries/list.jsp").forward(req, res);
    }

    /**
     * 業種またはカテゴリの新規登録・編集画面を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param kind 管理種別（type / category）。
     * @param isCategory カテゴリ編集の場合はtrue。
     * @param id 編集対象ID。新規登録時はnull。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showEdit(HttpServletRequest req, HttpServletResponse res,
            String kind, boolean isCategory, Integer id) throws Exception {
        AdminIndustryItem current;
        if (id == null) {
            current = new AdminIndustryItem();
        } else {
            current = isCategory ? catalogDao.findCategoryById(id) : catalogDao.findIndustryTypeById(id);
            if (current == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
                return;
            }
        }

        ManagementAuth.preparePage(req, "業種管理", true, true);
        req.setAttribute("kind", kind);
        req.setAttribute("isCategory", isCategory);
        req.setAttribute("editId", id);
        req.setAttribute("current", current);
        req.setAttribute("activeChoices", ACTIVE_CHOICES);
        if (!isCategory) {
            req.setAttribute("categories", catalogDao.findCategoryChoices());
        }
        req.getRequestDispatcher("/WEB-INF/views/management/admin/industries/edit.jsp").forward(req, res);
    }

    /**
     * 業種またはカテゴリの登録・更新フォーム送信を処理する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param admin 操作する管理者。
     * @throws Exception 更新処理を完了できない場合。
     */
    private void handlePost(HttpServletRequest req, HttpServletResponse res, User admin) throws Exception {
        if (!ManagementAuth.verifyCsrf(req, res, true)) {
            return;
        }
        try {
            FormReader input = new FormReader(req);
            input.choice("operation", "操作", "save");
            String kind = input.choice("kind", "管理する項目", "category", "type");
            boolean isCategory = "category".equals(kind);
            Integer id = input.intId("id", false);
            String name = input.text("name", "名称", isCategory ? 50 : 80, true);
            int order = input.number("order", "表示順", 1, Integer.MAX_VALUE);
            boolean active = Boolean.parseBoolean(input.choice("active", "有効状態", "true", "false"));

            // 追加時のみコードを受け付けて形式を検証する（既存コードは変更させない）。
            String code = null;
            if (id == null) {
                code = input.text("code", "コード", isCategory ? 3 : 5, true);
                String pattern = isCategory ? "[A-Z0-9]{3}" : "I[0-9]{4}";
                if (!code.matches(pattern)) {
                    throw new ManagementProblem(400, "コードの形式を確認してください。");
                }
            }

            int savedId;
            if (isCategory) {
                savedId = catalogDao.saveCategory(admin.getUserId(), id, code, name, order, active);
            } else {
                int categoryId = input.intId("categoryId", true);
                savedId = catalogDao.saveIndustryType(admin.getUserId(), id, categoryId, code, name, order, active);
            }

            ManagementAuth.redirectSaved(req, res, ManagementPaths.ADMIN_INDUSTRIES + "?kind=" + kind + "&id=" + savedId);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }
}
