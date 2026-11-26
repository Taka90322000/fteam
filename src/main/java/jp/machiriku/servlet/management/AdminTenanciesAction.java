package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminTenancyDetail;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.dao.management.AdminDealDao;
import jp.machiriku.tool.Action;

/** 管理者が契約物件の詳細と紐づく出店計画を確認するAction。 */
public final class AdminTenanciesAction extends Action {
    /** 管理者権限チェックに使うDAO。 */
    private final AdminAuditDao auditDao;
    /** 契約物件詳細を取得するDAO。 */
    private final AdminDealDao dealDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminTenanciesAction() {
        this(new AdminAuditDao(), new AdminDealDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param auditDao 管理者権限チェックDAO。
     * @param dealDao 取引詳細DAO。
     */
    AdminTenanciesAction(AdminAuditDao auditDao, AdminDealDao dealDao) {
        this.auditDao = auditDao;
        this.dealDao = dealDao;
    }

    /**
     * 指定されたIDの契約物件詳細と出店計画一覧を取得して表示する。
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

        // 2. パラメータから契約IDを取得し、DBから詳細と出店計画一覧を読み込む。
        try {
            FormReader input = new FormReader(req);
            int tenancyId = input.intId("id", true);
            AdminTenancyDetail tenancy = dealDao.findTenancyById(tenancyId);
            if (tenancy == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
                return;
            }

            // 3. JSPへ渡して契約物件詳細を表示する。
            ManagementAuth.preparePage(req, "契約物件", true, true);
            req.setAttribute("tenancy", tenancy);
            req.getRequestDispatcher("/WEB-INF/views/management/admin/properties/tenancy.jsp").forward(req, res);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }
}
