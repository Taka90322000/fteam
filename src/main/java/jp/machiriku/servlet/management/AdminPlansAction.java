package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminOpeningPlanDetail;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.dao.management.AdminDealDao;
import jp.machiriku.tool.Action;

/** 管理者が公開中の出店計画を確認し、必要に応じて公開停止を行うAction。 */
public final class AdminPlansAction extends Action {
    /** 管理者権限チェックに使うDAO。 */
    private final AdminAuditDao auditDao;
    /** 出店計画の照会・公開停止を行うDAO。 */
    private final AdminDealDao dealDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminPlansAction() {
        this(new AdminAuditDao(), new AdminDealDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param auditDao 管理者権限チェックDAO。
     * @param dealDao 取引詳細DAO。
     */
    AdminPlansAction(AdminAuditDao auditDao, AdminDealDao dealDao) {
        this.auditDao = auditDao;
        this.dealDao = dealDao;
    }

    /**
     * GETでは出店計画詳細を表示し、POSTでは公開停止を実行する。
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

        // 2. POSTの場合は公開停止処理を実行する。
        if ("POST".equals(req.getMethod())) {
            if (!ManagementAuth.verifyCsrf(req, res, true)) {
                return;
            }
            try {
                FormReader input = new FormReader(req);
                int planId = input.intId("id", true);
                input.choice("operation", "操作", "archive");
                String reason = input.text("reason", "停止理由", 2000, true);
                dealDao.archiveOpeningPlan(planId, admin.getUserId(), reason);
                ManagementAuth.redirectSaved(req, res, ManagementPaths.ADMIN_PLANS + "?id=" + planId);
            } catch (Exception error) {
                ManagementAuth.handleError(req, res, error);
            }
            return;
        }

        // 3. GET/HEAD以外を拒否し、出店計画の詳細を表示する。
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }
        try {
            FormReader input = new FormReader(req);
            int planId = input.intId("id", true);
            AdminOpeningPlanDetail plan = dealDao.findOpeningPlanById(planId);
            if (plan == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
                return;
            }

            ManagementAuth.preparePage(req, "出店計画", true, true);
            req.setAttribute("plan", plan);
            req.getRequestDispatcher("/WEB-INF/views/management/admin/properties/plan.jsp").forward(req, res);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }
}
