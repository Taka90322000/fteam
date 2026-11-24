package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminDashboardSummary;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.tool.Action;

/** 運営ダッシュボード画面を表示し、通報・問い合わせ件数とサービス利用状況を確認する。 */
public final class AdminAction extends Action {
    /** 管理者権限チェックとダッシュボード集計に使うDAO。 */
    private final AdminAuditDao auditDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminAction() {
        this(new AdminAuditDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param auditDao 管理者権限チェック・ダッシュボード集計DAO。
     */
    AdminAction(AdminAuditDao auditDao) {
        this.auditDao = auditDao;
    }

    /**
     * 運営ダッシュボードの集計結果を取得し、JSPへフォワードする。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @throws Exception 画面表示処理を完了できない場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 1. 管理者権限を確認する。
        User admin = ManagementAuth.requireAdmin(req, res, auditDao);
        if (admin == null) {
            return;
        }
        // 2. 表示専用画面のためGET/HEADのみ受け付ける。
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }

        // 3. DBから各件数を集計してJSPへ渡す。
        try {
            AdminDashboardSummary summary = auditDao.findDashboardSummary();
            ManagementAuth.preparePage(req, "運営ダッシュボード", true, true);
            req.setAttribute("summary", summary);
            req.getRequestDispatcher("/WEB-INF/views/management/admin/dashboard.jsp").forward(req, res);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }
}
