package jp.machiriku.servlet.management;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminUserItem;
import jp.machiriku.bean.management.PageResult;
import jp.machiriku.dao.management.PublicDirectoryDao;
import jp.machiriku.tool.Action;

/** ログイン済み会員が閲覧可能な事業主一覧と詳細を表示するAction。 */
public final class BusinessDirectoryAction extends Action {
    /** 事業主の公開プロフィールを参照するDAO。 */
    private final PublicDirectoryDao directoryDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public BusinessDirectoryAction() {
        this(new PublicDirectoryDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param directoryDao 公開ディレクトリDAO。
     */
    BusinessDirectoryAction(PublicDirectoryDao directoryDao) {
        this.directoryDao = directoryDao;
    }

    /**
     * 事業主IDの指定があれば詳細を、未指定なら閲覧可能な事業主一覧を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @throws Exception 画面表示を完了できない場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 1. ログイン済み会員であることとGET/HEADメソッドを確認する。
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }
        User viewer = ManagementAuth.requireMember(req, res, false);
        if (viewer == null) {
            return;
        }

        try {
            FormReader input = new FormReader(req);
            Integer businessId = input.intId("id", false);
            int viewerId = viewer.getUserId();

            // 2. 事業主IDが未指定の場合は一覧を表示する。
            if (businessId == null) {
                String query = input.search();
                List<AdminUserItem.Profile> rows = directoryDao.findVisibleBusinesses(
                        viewerId, query, FormReader.PAGE_SIZE + 1, input.offset());
                PageResult<AdminUserItem.Profile> pageResult = input.toPageResult(rows);

                ManagementAuth.preparePage(req, "事業主一覧", false, true);
                req.setAttribute("query", query);
                req.setAttribute("businessPage", pageResult);
                req.getRequestDispatcher("/WEB-INF/views/management/support/businesses/list.jsp").forward(req, res);
                return;
            }

            // 3. 事業主IDが指定されている場合は閲覧可能な事業主詳細を表示する。
            AdminUserItem.Profile business = directoryDao.findVisibleBusinessById(businessId, viewerId);
            if (business == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
                return;
            }
            boolean canReport = viewerId != businessId && "ACTIVE".equals(viewer.getAccountStatus());

            ManagementAuth.preparePage(req, business.getBusinessName(), false, true);
            req.setAttribute("business", business);
            req.setAttribute("canReport", canReport);
            req.getRequestDispatcher("/WEB-INF/views/management/support/businesses/detail.jsp").forward(req, res);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }
}
