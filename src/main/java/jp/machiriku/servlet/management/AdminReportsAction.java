package jp.machiriku.servlet.management;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminReportItem;
import jp.machiriku.bean.management.ManagementLabels;
import jp.machiriku.bean.management.PageResult;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.dao.management.AdminReportDao;
import jp.machiriku.tool.Action;

/** 管理者が通報の検索・詳細確認・確認中への移行・対応完了を行うAction。 */
public final class AdminReportsAction extends Action {
    /** 管理者権限チェックと管理者一覧取得に使うDAO。 */
    private final AdminAuditDao auditDao;
    /** 管理者用通報DAO。 */
    private final AdminReportDao reportDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminReportsAction() {
        this(new AdminAuditDao(), new AdminReportDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param auditDao 管理者権限チェックDAO。
     * @param reportDao 管理者用通報DAO。
     */
    AdminReportsAction(AdminAuditDao auditDao, AdminReportDao reportDao) {
        this.auditDao = auditDao;
        this.reportDao = reportDao;
    }

    /**
     * GETでは通報一覧または詳細を表示し、POSTでは担当設定または対応完了を実行する。
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

        // 2. POSTの場合は担当設定または対応完了を実行する。
        if ("POST".equals(req.getMethod())) {
            handlePost(req, res, admin);
            return;
        }

        // 3. GET/HEADでは受付番号の有無に応じて一覧または詳細を表示する。
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }
        try {
            FormReader input = new FormReader(req);
            Long reportId = input.longId("id", false);
            if (reportId == null) {
                showList(req, res, input);
            } else {
                showDetail(req, res, admin, reportId);
            }
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * 通報の確認中移行（担当設定）または対応完了（必要に応じ掲載停止・利用停止）を処理する。
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
            long reportId = input.longId("id", true);
            String operation = input.choice("operation", "操作", "review", "resolve");
            if ("review".equals(operation)) {
                int assigneeId = input.intId("assignee", true);
                reportDao.assignReport(reportId, admin.getUserId(), assigneeId);
            } else {
                String resolution = input.choice("resolution", "対応結果",
                        "NO_ACTION", "CORRECTION", "HIDE_PROPERTY", "SUSPEND_USER");
                String reason = input.text("reason", "判断理由", 2000, true);
                reportDao.resolveReport(reportId, admin.getUserId(), resolution, reason);
            }
            ManagementAuth.redirectSaved(req, res, ManagementPaths.ADMIN_REPORTS + "?id=" + reportId);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * 管理者用の通報検索・一覧画面を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showList(HttpServletRequest req, HttpServletResponse res, FormReader input) throws Exception {
        String query = input.search();
        String kind = input.value("kind");
        String status = input.value("status");
        String reason = input.value("reasonCode");
        LocalDate from = input.date("from", "開始日");
        LocalDate to = input.date("to", "終了日");

        List<AdminReportItem> rows = reportDao.findReports(
                kind, status, reason, from, to, query, FormReader.PAGE_SIZE + 1, input.offset());
        PageResult<AdminReportItem> pageResult = input.toPageResult(rows);

        ManagementAuth.preparePage(req, "通報管理", true, true);
        req.setAttribute("query", query);
        req.setAttribute("kind", kind);
        req.setAttribute("status", status);
        req.setAttribute("reason", reason);
        req.setAttribute("from", from);
        req.setAttribute("to", to);
        req.setAttribute("kinds", ManagementLabels.REPORT_TARGET_KINDS);
        req.setAttribute("states", ManagementLabels.REPORT_STATES);
        req.setAttribute("reasons", ManagementLabels.REPORT_REASONS);
        req.setAttribute("reportPage", pageResult);
        req.getRequestDispatcher("/WEB-INF/views/management/admin/reports/list.jsp").forward(req, res);
    }

    /**
     * 管理者用の通報詳細・対応フォーム画面を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param admin ログイン中の管理者。
     * @param reportId 受付番号。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showDetail(HttpServletRequest req, HttpServletResponse res, User admin, long reportId)
            throws Exception {
        AdminReportItem report = reportDao.findReportById(reportId);
        if (report == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
            return;
        }
        Integer defaultAssignee = report.getAssignedAdminUserId() != null
                ? report.getAssignedAdminUserId()
                : admin.getUserId();
        Map<String, String> admins = report.isEditable() ? auditDao.findActiveAdminChoices() : Map.of();
        Map<String, String> resolutions = ManagementLabels.reportResolutionsFor(report.getTargetPropertyId() != null);

        ManagementAuth.preparePage(req, "通報 #" + reportId, true, true);
        req.setAttribute("report", report);
        req.setAttribute("defaultAssignee", defaultAssignee);
        req.setAttribute("admins", admins);
        req.setAttribute("resolutions", resolutions);
        req.getRequestDispatcher("/WEB-INF/views/management/admin/reports/detail.jsp").forward(req, res);
    }
}
