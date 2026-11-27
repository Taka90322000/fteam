package jp.machiriku.servlet.vote;

import java.sql.SQLException;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.DemandReport;
import jp.machiriku.dao.DemandDao;
import jp.machiriku.tool.Action;

/**
 * 住民の投票から集計した需要を表示するAction(読み取りのみ)。ログインなしでも見られる。
 * <p>URL(GET):</p>
 * <ul>
 *   <li>{@code Demand.action?propertyId=N} 物件1件の需要(業種ランキング・大分類・来店頻度・要望コメント)</li>
 *   <li>{@code Demand.action?prefecture=栃木県&municipality=宇都宮市} エリアの需要(掲載中の物件が対象。両方省略すると全体)</li>
 * </ul>
 * {@code /WEB-INF/views/vote/demand.jsp} へforwardする。要望コメントはログイン中の会員にだけ表示する。
 * 任意パラメーター: limit(業種ランキングの件数、既定10、最大50)、commentLimit(物件別、既定10、最大30)、
 * topProperties(エリア別、既定5、最大20)。
 */
public class DemandAction extends Action {
    private static final int DEFAULT_LIMIT = 10, MAX_LIMIT = 50;
    private static final int DEFAULT_COMMENTS = 10, MAX_COMMENTS = 30;
    private static final int DEFAULT_TOP_PROPERTIES = 5, MAX_TOP_PROPERTIES = 20;
    private static final int PREFECTURE_MAX = 10, MUNICIPALITY_MAX = 50;
    static final String VIEW = VoteViews.DEMAND;

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
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
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
            // 入力が正しくないときは、集計なしで画面を出し、理由を表示する。
            req.setAttribute("errorMessage", e.getMessage());
            req.setAttribute("prefecture", "");
            req.setAttribute("municipality", "");
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            req.getRequestDispatcher(VIEW).forward(req, res);
            return;
        }

        boolean member = VoteSupport.currentActiveMember(req) != null;
        try {
            DemandReport report;
            if (propertyId != null) {
                Optional<DemandReport> found = demandDao.propertyDemand(propertyId, limit, member ? commentLimit : 0);
                if (found.isEmpty()) {
                    res.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                report = found.get();
            } else {
                report = demandDao.areaDemand(prefecture, municipality, limit, topProperties);
            }
            req.setAttribute("report", report);
            req.setAttribute("commentsVisible", member);
            req.setAttribute("prefecture", prefecture == null ? "" : prefecture);
            req.setAttribute("municipality", municipality == null ? "" : municipality);
            req.getRequestDispatcher(VIEW).forward(req, res);
        } catch (SQLException e) {
            req.getServletContext().log("Demand failed (SQLState=" + e.getSQLState() + ").");
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
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
