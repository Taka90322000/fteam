package jp.machiriku.servlet.management;

import java.math.BigDecimal;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminPropertyItem;
import jp.machiriku.bean.management.ManagementLabels;
import jp.machiriku.bean.management.ManagementProblem;
import jp.machiriku.bean.management.PageResult;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.dao.management.AdminPropertyDao;
import jp.machiriku.tool.Action;

/** 管理者が掲載物件の一覧・詳細・関連履歴を閲覧し、掲載停止・公開・内容修正を行うAction。 */
public final class AdminPropertiesAction extends Action {
    /** 管理者権限チェックに使うDAO。 */
    private final AdminAuditDao auditDao;
    /** 管理者用物件DAO。 */
    private final AdminPropertyDao propertyDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminPropertiesAction() {
        this(new AdminAuditDao(), new AdminPropertyDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param auditDao 管理者権限チェックDAO。
     * @param propertyDao 管理者用物件DAO。
     */
    AdminPropertiesAction(AdminAuditDao auditDao, AdminPropertyDao propertyDao) {
        this.auditDao = auditDao;
        this.propertyDao = propertyDao;
    }

    /**
     * GETでは物件一覧または物件詳細を表示し、POSTでは掲載停止・公開・内容修正を実行する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @throws Exception 画面表示または更新処理を完了できない場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 1. 管理者権限を確認する。
        User admin = ManagementAuth.requireAdmin(req, res, auditDao);
        if (admin == null) {
            return;
        }

        // 2. POSTの場合は掲載操作または内容修正を実行する。
        if ("POST".equals(req.getMethod())) {
            handlePost(req, res, admin);
            return;
        }

        // 3. GET/HEADでは物件IDの有無に応じて一覧または詳細を表示する。
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }
        try {
            FormReader input = new FormReader(req);
            Integer propertyId = input.intId("id", false);
            if (propertyId == null) {
                showList(req, res, input);
            } else {
                showDetail(req, res, input, propertyId);
            }
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * 物件の掲載停止・解除・募集公開・掲載内容修正を処理する。
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
            int propertyId = input.intId("id", true);
            String operation = input.choice("operation", "操作", "hide", "unhide", "edit", "publish");

            if ("hide".equals(operation) || "unhide".equals(operation)) {
                String reason = input.text("reason", "操作理由", 2000, true);
                propertyDao.changeVisibility(propertyId, admin.getUserId(), "hide".equals(operation), reason);
            } else if ("publish".equals(operation)) {
                String reason = input.text("reason", "操作理由", 2000, true);
                propertyDao.publishProperty(propertyId, admin.getUserId(), reason);
            } else {
                BigDecimal lat = input.decimal("lat", "緯度", -90, 90, 6, false);
                BigDecimal lng = input.decimal("lng", "経度", -180, 180, 6, false);
                if ((lat == null) != (lng == null)) {
                    throw new ManagementProblem(400, "緯度と経度を一緒に入力してください。");
                }
                propertyDao.updateProperty(
                        propertyId,
                        admin.getUserId(),
                        input.text("name", "物件名", 150, true),
                        input.text("prefecture", "都道府県", 10, true),
                        input.text("city", "市区町村", 50, true),
                        input.text("street", "番地", 200, true),
                        lat,
                        lng,
                        input.decimal("floor", "面積", 0.01, 99999999.99, 2, true),
                        input.number("rent", "賃料", 0, Integer.MAX_VALUE),
                        input.text("description", "説明", 8000, false),
                        input.webUrl("url", "掲載URL"),
                        input.choice("permit", "許認可", "UNKNOWN", "OBTAINED", "NOT_OBTAINED"));
            }
            ManagementAuth.redirectSaved(req, res, ManagementPaths.ADMIN_PROPERTIES + "?id=" + propertyId);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * 管理者用の物件検索・一覧画面を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showList(HttpServletRequest req, HttpServletResponse res, FormReader input) throws Exception {
        String status = input.value("status");
        String visibility = input.value("kind");
        String query = input.search();

        List<AdminPropertyItem> rows = propertyDao.findProperties(
                status, visibility, query, FormReader.PAGE_SIZE + 1, input.offset());
        PageResult<AdminPropertyItem> pageResult = input.toPageResult(rows);

        ManagementAuth.preparePage(req, "物件管理", true, true);
        req.setAttribute("query", query);
        req.setAttribute("status", status);
        req.setAttribute("visibility", visibility);
        req.setAttribute("states", ManagementLabels.PROPERTY_STATES);
        req.setAttribute("visibilities", ManagementLabels.PROPERTY_VISIBILITIES);
        req.setAttribute("propertyPage", pageResult);
        req.getRequestDispatcher("/WEB-INF/views/management/admin/properties/list.jsp").forward(req, res);
    }

    /**
     * 物件詳細と選択されたタブ（物件情報・申込・オファー・通報）を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @param propertyId 物件ID。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showDetail(HttpServletRequest req, HttpServletResponse res, FormReader input, int propertyId)
            throws Exception {
        AdminPropertyItem property = propertyDao.findPropertyById(propertyId);
        if (property == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
            return;
        }
        String tab = input.value("tab").isEmpty()
                ? "info"
                : input.choice("tab", "表示する情報", "info", "applications", "reports", "offers");

        ManagementAuth.preparePage(req, property.getPropertyName(), true, true);
        req.setAttribute("property", property);
        req.setAttribute("tab", tab);

        if ("info".equals(tab)) {
            List<AdminPropertyItem.Image> images = propertyDao.findPropertyImages(propertyId);
            List<AdminPropertyItem.Industry> industries = propertyDao.findPropertyIndustries(propertyId);
            List<AdminPropertyItem.ContractTerm> terms = propertyDao.findPropertyContractTerms(propertyId);
            req.setAttribute("images", images);
            req.setAttribute("industries", industries);
            req.setAttribute("terms", terms);
            req.setAttribute("permits", ManagementLabels.PERMIT_STATES);
        } else {
            List<AdminPropertyItem.History> historyRows;
            if ("applications".equals(tab)) {
                historyRows = propertyDao.findApplicationHistory(propertyId, FormReader.PAGE_SIZE + 1, input.offset());
            } else if ("offers".equals(tab)) {
                historyRows = propertyDao.findOfferHistory(propertyId, FormReader.PAGE_SIZE + 1, input.offset());
            } else {
                historyRows = propertyDao.findReportHistory(propertyId, FormReader.PAGE_SIZE + 1, input.offset());
            }
            req.setAttribute("historyPage", input.toPageResult(historyRows));
        }

        req.getRequestDispatcher("/WEB-INF/views/management/admin/properties/detail.jsp").forward(req, res);
    }
}
