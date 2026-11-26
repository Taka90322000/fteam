package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminApplicationDetail;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.dao.management.AdminDealDao;
import jp.machiriku.tool.Action;

/** 管理者が物件の出店申し込み詳細と必要資格・書類の確認状況を確認するAction。 */
public final class AdminApplicationsAction extends Action {
    /** 管理者権限チェックに使うDAO。 */
    private final AdminAuditDao auditDao;
    /** 出店申し込み詳細を取得するDAO。 */
    private final AdminDealDao dealDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminApplicationsAction() {
        this(new AdminAuditDao(), new AdminDealDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param auditDao 管理者権限チェックDAO。
     * @param dealDao 取引詳細DAO。
     */
    AdminApplicationsAction(AdminAuditDao auditDao, AdminDealDao dealDao) {
        this.auditDao = auditDao;
        this.dealDao = dealDao;
    }

    /**
     * 指定されたIDの出店申し込み詳細を取得して表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @throws Exception 画面表示処理を完了できない場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 1. 管理者権限とHTTPメソッドを確認する。
        User admin = ManagementAuth.requireAdmin(req, res, auditDao);
        if (admin == null) {
            return;
        }
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }

        // 2. パラメータから申込IDを取得し、DBから詳細とチェック項目を読み込む。
        try {
            FormReader input = new FormReader(req);
            int applicationId = input.intId("id", true);
            AdminApplicationDetail application = dealDao.findApplicationById(applicationId);
            if (application == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
                return;
            }

            // 3. JSPへ渡して申込詳細を表示する。
            ManagementAuth.preparePage(req, "出店申し込み", true, true);
            req.setAttribute("application", application);
            req.getRequestDispatcher("/WEB-INF/views/management/admin/properties/application.jsp").forward(req, res);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }
}
