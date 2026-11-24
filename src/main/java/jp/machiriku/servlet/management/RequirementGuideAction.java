package jp.machiriku.servlet.management;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.PageResult;
import jp.machiriku.bean.management.AdminQualificationItem;
import jp.machiriku.dao.management.PublicDirectoryDao;
import jp.machiriku.tool.Action;

/** 業種別の必要資格・許認可・書類の公開案内画面を表示するAction。 */
public final class RequirementGuideAction extends Action {
    /** 公開要件を参照するDAO。 */
    private final PublicDirectoryDao directoryDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public RequirementGuideAction() {
        this(new PublicDirectoryDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param directoryDao 公開ディレクトリDAO。
     */
    RequirementGuideAction(PublicDirectoryDao directoryDao) {
        this.directoryDao = directoryDao;
    }

    /**
     * 指定業種（または全業種）の公開中の必要資格・書類一覧を取得して表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @throws Exception 画面表示を完了できない場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 1. ゲストにも公開する画面としてレスポンスを初期化する。
        User viewer = ManagementAuth.optionalMember(req, res);
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }

        // 2. 業種フィルターと該当する要件一覧を取得する。
        try {
            FormReader input = new FormReader(req);
            Integer industryId = input.intId("industry", false);
            Map<String, String> industries = directoryDao.findIndustryChoices();
            List<AdminQualificationItem> rows = directoryDao.findPublicRequirements(
                    industryId, FormReader.PAGE_SIZE + 1, input.offset());
            PageResult<AdminQualificationItem> pageResult = input.toPageResult(rows);

            // 3. 公開案内JSPへ渡して描画する。
            ManagementAuth.preparePage(req, "必要資格・書類の案内", false, viewer != null);
            req.setAttribute("industryId", industryId);
            req.setAttribute("industries", industries);
            req.setAttribute("requirementPage", pageResult);
            req.getRequestDispatcher("/WEB-INF/views/management/support/requirements/list.jsp").forward(req, res);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }
}
