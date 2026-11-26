package jp.machiriku.servlet.vote;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.DemandReport;
import jp.machiriku.dao.DemandDao;
import jp.machiriku.tool.Action;

/**
 * 物件一覧に出す「投票数」と「いま一番求められている業種」を、複数の物件まとめて返すAPI(GETのみ、結果はJSON、ログイン不要)。
 * GET ?propertyIds=1,2,3 (1〜50件)。削除済み・非表示・存在しない物件は結果に含まれない。
 * 物件ごとに Demand.action を呼ぶと一覧の表示が遅くなるので、一覧にはこちらを使う。
 */
public class VoteCountsAction extends Action {
    static final int MAX_IDS = 50;

    private final DemandDao demandDao;

    public VoteCountsAction() {
        this(new DemandDao());
    }

    public VoteCountsAction(DemandDao demandDao) {
        this.demandDao = demandDao;
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            VoteSupport.error(res, 405, "METHOD_NOT_ALLOWED", "GETで呼び出してください。");
            return;
        }
        List<Integer> ids;
        try {
            ids = parseIds(req.getParameter("propertyIds"));
        } catch (IllegalArgumentException e) {
            VoteSupport.error(res, 400, "INVALID_INPUT", e.getMessage());
            return;
        }
        try {
            List<Map<String, Object>> properties = new ArrayList<>();
            for (DemandReport.PropertySummary summary : demandDao.summaries(ids)) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("propertyId", summary.propertyId());
                map.put("voteCount", summary.voteCount());
                map.put("score", summary.score());
                if (summary.topIndustryCode() == null) {
                    map.put("topIndustry", null);
                } else {
                    Map<String, Object> top = new LinkedHashMap<>();
                    top.put("industryCode", summary.topIndustryCode());
                    top.put("industryName", summary.topIndustryName());
                    map.put("topIndustry", top);
                }
                properties.add(map);
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("properties", properties);
            VoteSupport.write(res, 200, body);
        } catch (SQLException e) {
            VoteSupport.logDatabaseError(req, e);
            VoteSupport.error(res, 500, "SERVER_ERROR", "投票数を集計できませんでした。時間をおいてもう一度お試しください。");
        }
    }

    /** カンマ区切りの物件ID。重複は1つにまとめ、1〜50件の正の整数であること。 */
    static List<Integer> parseIds(String raw) {
        if (raw == null || raw.isBlank()) { throw new IllegalArgumentException("propertyIds を指定してください。"); }
        Set<Integer> ids = new LinkedHashSet<>();
        for (String part : raw.split(",", -1)) {
            try {
                int id = Integer.parseInt(part.strip());
                if (id < 1) { throw new NumberFormatException(); }
                ids.add(id);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("propertyIds が正しくありません。");
            }
        }
        if (ids.size() > MAX_IDS) { throw new IllegalArgumentException("一度に指定できる物件は" + MAX_IDS + "件までです。"); }
        return new ArrayList<>(ids);
    }
}
