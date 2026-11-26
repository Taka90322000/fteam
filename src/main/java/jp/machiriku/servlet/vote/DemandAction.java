package jp.machiriku.servlet.vote;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.DemandReport;
import jp.machiriku.dao.DemandDao;
import jp.machiriku.tool.Action;

/**
 * 住民の投票から集計した需要を返すAPI(読み取りのみ、結果はJSON)。
 * <ul>
 *   <li>GET ?propertyId=N                         物件1件の需要(業種ランキング・大分類・来店頻度・要望コメント)</li>
 *   <li>GET ?municipality=宇都宮市&amp;prefecture=栃木県   エリアの需要(掲載中の物件が対象。両方省略すると全体)</li>
 * </ul>
 * 共通の任意パラメーター: limit(業種ランキングの件数、既定10、最大50)。
 * 物件別は commentLimit(既定10、最大30)、エリア別は topProperties(既定5、最大20)。
 * 集計だけなので、ログインしていなくても見られる。要望コメントはログイン中の会員にだけ返す。
 */
public class DemandAction extends Action {
    private static final int DEFAULT_LIMIT = 10, MAX_LIMIT = 50;
    private static final int DEFAULT_COMMENTS = 10, MAX_COMMENTS = 30;
    private static final int DEFAULT_TOP_PROPERTIES = 5, MAX_TOP_PROPERTIES = 20;
    private static final int PREFECTURE_MAX = 10, MUNICIPALITY_MAX = 50;

    private final DemandDao demandDao;

    public DemandAction() {
        this(new DemandDao());
    }

    public DemandAction(DemandDao demandDao) {
        this.demandDao = demandDao;
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            VoteSupport.error(res, 405, "METHOD_NOT_ALLOWED", "GETで呼び出してください。");
            return;
        }

        Integer propertyId;
        int limit, commentLimit, topProperties;
        String prefecture, municipality;
        try {
            propertyId = optionalInt(req.getParameter("propertyId"), 1, Integer.MAX_VALUE, "物件ID");
            limit = withDefault(optionalInt(req.getParameter("limit"), 1, MAX_LIMIT, "limit"), DEFAULT_LIMIT);
            commentLimit = withDefault(optionalInt(req.getParameter("commentLimit"), 0, MAX_COMMENTS, "commentLimit"), DEFAULT_COMMENTS);
            topProperties = withDefault(optionalInt(req.getParameter("topProperties"), 0, MAX_TOP_PROPERTIES, "topProperties"), DEFAULT_TOP_PROPERTIES);
            prefecture = optionalText(req.getParameter("prefecture"), PREFECTURE_MAX, "都道府県");
            municipality = optionalText(req.getParameter("municipality"), MUNICIPALITY_MAX, "市区町村");
        } catch (IllegalArgumentException e) {
            VoteSupport.error(res, 400, "INVALID_INPUT", e.getMessage());
            return;
        }

        boolean member = VoteSupport.currentActiveMember(req) != null;
        try {
            DemandReport report;
            if (propertyId != null) {
                Optional<DemandReport> found = demandDao.propertyDemand(propertyId, limit, member ? commentLimit : 0);
                if (found.isEmpty()) {
                    VoteSupport.error(res, 404, "PROPERTY_NOT_FOUND", "物件が見つかりません。");
                    return;
                }
                report = found.get();
            } else {
                report = demandDao.areaDemand(prefecture, municipality, limit, topProperties);
            }
            Map<String, Object> body = toMap(report);
            body.put("commentsVisible", member);
            VoteSupport.write(res, 200, body);
        } catch (SQLException e) {
            req.getServletContext().log("Demand failed (SQLState=" + e.getSQLState() + ").");
            VoteSupport.error(res, 500, "SERVER_ERROR", "需要を集計できませんでした。時間をおいてもう一度お試しください。");
        }
    }

    /** 画面へ返すJSONの形。投稿者を特定できる情報(会員ID・名前)は含めない。 */
    static Map<String, Object> toMap(DemandReport report) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("scope", report.scope());
        if (report.propertyId() != null) {
            Map<String, Object> property = new LinkedHashMap<>();
            property.put("propertyId", report.propertyId());
            property.put("name", report.propertyName());
            property.put("prefecture", report.prefecture());
            property.put("municipality", report.municipality());
            body.put("property", property);
        } else {
            Map<String, Object> area = new LinkedHashMap<>();
            area.put("prefecture", report.prefecture());
            area.put("municipality", report.municipality());
            body.put("area", area);
        }
        body.put("propertyCount", report.propertyCount());
        body.put("voteCount", report.voteCount());
        body.put("voterCount", report.voterCount());
        body.put("averageDistanceKm", report.averageDistanceKm());
        body.put("totalScore", report.totalScore());

        List<Map<String, Object>> industries = new ArrayList<>();
        for (DemandReport.IndustryDemand item : report.industries()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("rank", item.rank());
            map.put("industryTypeId", item.industryTypeId());
            map.put("industryCode", item.industryCode());
            map.put("industryName", item.industryName());
            map.put("score", item.score());
            map.put("sharePercent", item.sharePercent());
            map.put("residentCount", item.residentCount());
            map.put("firstChoiceCount", item.firstChoiceCount());
            map.put("secondChoiceCount", item.secondChoiceCount());
            map.put("thirdChoiceCount", item.thirdChoiceCount());
            industries.add(map);
        }
        body.put("industries", industries);

        List<Map<String, Object>> categories = new ArrayList<>();
        for (DemandReport.CategoryDemand item : report.categories()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("rank", item.rank());
            map.put("categoryCode", item.categoryCode());
            map.put("categoryName", item.categoryName());
            map.put("score", item.score());
            map.put("sharePercent", item.sharePercent());
            map.put("residentCount", item.residentCount());
            categories.add(map);
        }
        body.put("categories", categories);

        List<Map<String, Object>> frequencies = new ArrayList<>();
        for (DemandReport.FrequencyCount item : report.frequencies()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("frequency", item.frequency());
            map.put("count", item.count());
            frequencies.add(map);
        }
        body.put("frequencies", frequencies);

        if (DemandReport.SCOPE_PROPERTY.equals(report.scope())) {
            List<Map<String, Object>> comments = new ArrayList<>();
            for (DemandReport.DemandComment item : report.comments()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("comment", item.comment());
                map.put("frequency", item.frequency());
                map.put("votedAt", item.votedAt() == null ? null : item.votedAt().toString());
                comments.add(map);
            }
            body.put("comments", comments);
        } else {
            List<Map<String, Object>> top = new ArrayList<>();
            for (DemandReport.TopProperty item : report.topProperties()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("rank", item.rank());
                map.put("propertyId", item.propertyId());
                map.put("name", item.propertyName());
                map.put("municipality", item.municipality());
                map.put("voteCount", item.voteCount());
                map.put("score", item.score());
                map.put("topIndustryName", item.topIndustryName());
                top.add(map);
            }
            body.put("topProperties", top);
        }
        body.put("generatedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        return body;
    }

    private static int withDefault(Integer value, int fallback) { return value == null ? fallback : value; }

    /** 未指定・空文字は null。指定があれば範囲内の整数であること。 */
    private static Integer optionalInt(String raw, int min, int max, String label) {
        if (raw == null || raw.isBlank()) { return null; }
        try {
            int value = Integer.parseInt(raw.strip());
            if (value < min || value > max) { throw new NumberFormatException(); }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + "が正しくありません。");
        }
    }

    private static String optionalText(String raw, int maxLength, String label) {
        if (raw == null || raw.isBlank()) { return null; }
        String value = raw.strip();
        if (value.length() > maxLength) { throw new IllegalArgumentException(label + "が長すぎます。"); }
        return value;
    }
}
