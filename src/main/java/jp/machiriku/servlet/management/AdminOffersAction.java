package jp.machiriku.servlet.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminOfferDetail;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.dao.management.AdminDealDao;
import jp.machiriku.tool.Action;

/** 管理者が物件の出店オファー詳細を確認するAction。 */
public final class AdminOffersAction extends Action {
    /** 管理者権限チェックに使うDAO。 */
    private final AdminAuditDao auditDao;
    /** 出店オファー詳細を取得するDAO。 */
    private final AdminDealDao dealDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminOffersAction() {
        this(new AdminAuditDao(), new AdminDealDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param auditDao 管理者権限チェックDAO。
     * @param dealDao 取引詳細DAO。
     */
    AdminOffersAction(AdminAuditDao auditDao, AdminDealDao dealDao) {
        this.auditDao = auditDao;
        this.dealDao = dealDao;
    }

    /**
     * 指定されたIDの出店オファー詳細を取得して表示する。
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

        // 2. パラメータからオファーIDを取得し、DBから詳細を読み込む。
        try {
            FormReader input = new FormReader(req);
            int offerId = input.intId("id", true);
            AdminOfferDetail offer = dealDao.findOfferById(offerId);
            if (offer == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
                return;
            }

            // 3. JSPへ渡してオファー詳細を表示する。
            ManagementAuth.preparePage(req, "出店オファー", true, true);
            req.setAttribute("offer", offer);
            req.getRequestDispatcher("/WEB-INF/views/management/admin/properties/offer.jsp").forward(req, res);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }
}
