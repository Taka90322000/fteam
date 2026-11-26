package jp.machiriku.servlet.vote;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.IndustryMaster;
import jp.machiriku.dao.IndustryMasterDao;
import jp.machiriku.tool.Action;

/**
 * 業種マスタ(有効な大分類と小分類)を返すAPI(GETのみ、結果はJSON、ログイン不要)。
 * 投票の希望業種の選択肢に使う。運営が管理画面で業種を追加・停止すると、そのまま反映される。
 * 投票時には、ここで返した industryCode(または industryTypeId)を Vote.action の rank1〜rank3 に送る。
 */
public class IndustriesAction extends Action {
    private final IndustryMasterDao industryDao;

    public IndustriesAction() {
        this(new IndustryMasterDao());
    }

    public IndustriesAction(IndustryMasterDao industryDao) {
        this.industryDao = industryDao;
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            VoteSupport.error(res, 405, "METHOD_NOT_ALLOWED", "GETで呼び出してください。");
            return;
        }
        try {
            VoteSupport.write(res, 200, toMap(industryDao.findActive()));
        } catch (SQLException e) {
            VoteSupport.logDatabaseError(req, e);
            VoteSupport.error(res, 500, "SERVER_ERROR", "業種の一覧を取得できませんでした。時間をおいてもう一度お試しください。");
        }
    }

    static Map<String, Object> toMap(IndustryMaster master) {
        List<Map<String, Object>> categories = new ArrayList<>();
        for (IndustryMaster.Category category : master.categories()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("categoryId", category.categoryId());
            map.put("categoryCode", category.categoryCode());
            map.put("categoryName", category.categoryName());
            List<Map<String, Object>> industries = new ArrayList<>();
            for (IndustryMaster.Industry industry : category.industries()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("industryTypeId", industry.industryTypeId());
                item.put("industryCode", industry.industryCode());
                item.put("industryName", industry.industryName());
                industries.add(item);
            }
            map.put("industries", industries);
            categories.add(map);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("categories", categories);
        return body;
    }
}
