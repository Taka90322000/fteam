package jp.machiriku.servlet.management;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminAuditLogItem;
import jp.machiriku.bean.management.PageResult;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.tool.Action;

/** 管理者が行った操作履歴を検索・閲覧するAction。 */
public final class AdminAuditAction extends Action {
    /** 管理者権限チェックと操作履歴照会に使うDAO。 */
    private final AdminAuditDao auditDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminAuditAction() {
        this(new AdminAuditDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param auditDao 管理者権限チェック・操作履歴DAO。
     */
    AdminAuditAction(AdminAuditDao auditDao) {
        this.auditDao = auditDao;
    }

    /**
     * 操作履歴を検索語で絞り込み、1ページ分をJSPへ表示する。
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

        // 2. 検索条件を読み取り、操作履歴を1ページ分取得する。
        try {
            FormReader input = new FormReader(req);
            String query = input.search();
            List<AdminAuditLogItem> rows = auditDao.findAuditLogs(
                    query, FormReader.PAGE_SIZE + 1, input.offset());
            PageResult<AdminAuditLogItem> pageResult = input.toPageResult(rows);

            // 3. JSPへ渡して操作履歴画面を表示する。
            ManagementAuth.preparePage(req, "操作履歴", true, true);
            req.setAttribute("query", query);
            req.setAttribute("auditPage", pageResult);
            req.getRequestDispatcher("/WEB-INF/views/management/admin/audit/list.jsp").forward(req, res);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }
}
