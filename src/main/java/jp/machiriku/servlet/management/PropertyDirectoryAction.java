package jp.machiriku.servlet.management;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.PageResult;
import jp.machiriku.bean.management.AdminPropertyItem;
import jp.machiriku.dao.management.PublicDirectoryDao;
import jp.machiriku.tool.Action;

/** 一般公開の掲載物件一覧と物件詳細を表示するAction。 */
public final class PropertyDirectoryAction extends Action {
    /** 公開物件を参照するDAO。 */
    private final PublicDirectoryDao directoryDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public PropertyDirectoryAction() {
        this(new PublicDirectoryDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param directoryDao 公開ディレクトリDAO。
     */
    PropertyDirectoryAction(PublicDirectoryDao directoryDao) {
        this.directoryDao = directoryDao;
    }

    /**
     * 物件IDの指定があれば公開物件詳細を、指定がなければ検索一覧を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @throws Exception 画面表示を完了できない場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 1. 公開画面としてレスポンスを初期化し、ログイン中の会員がいれば取得する。
        User viewer = ManagementAuth.optionalMember(req, res);
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }

        try {
            FormReader input = new FormReader(req);
            Integer propertyId = input.intId("id", false);

            // 2. 物件IDが未指定の場合は公開物件の検索一覧を表示する。
            if (propertyId == null) {
                String query = input.search();
                List<AdminPropertyItem> rows = directoryDao.findVisibleProperties(
                        query, FormReader.PAGE_SIZE + 1, input.offset());
                PageResult<AdminPropertyItem> pageResult = input.toPageResult(rows);

                ManagementAuth.preparePage(req, "掲載物件", false, viewer != null);
                req.setAttribute("query", query);
                req.setAttribute("propertyPage", pageResult);
                req.getRequestDispatcher("/WEB-INF/views/management/support/properties/list.jsp").forward(req, res);
                return;
            }

            // 3. 物件IDが指定されている場合は公開条件を満たす物件詳細を表示する。
            AdminPropertyItem property = directoryDao.findVisiblePropertyById(propertyId);
            if (property == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
                return;
            }
            boolean canInquire = viewer != null;
            boolean canReport = viewer != null
                    && "ACTIVE".equals(viewer.getAccountStatus())
                    && viewer.getUserId() != property.getOwnerUserId();

            ManagementAuth.preparePage(req, property.getPropertyName(), false, viewer != null);
            req.setAttribute("property", property);
            req.setAttribute("canInquire", canInquire);
            req.setAttribute("canReport", canReport);
            req.getRequestDispatcher("/WEB-INF/views/management/support/properties/detail.jsp").forward(req, res);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }
}
