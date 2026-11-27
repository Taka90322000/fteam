package jp.machiriku.servlet.vote;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.VoteHistoryPage;
import jp.machiriku.dao.VoteHistoryDao;
import jp.machiriku.tool.Action;

/**
 * 自分の投票履歴API(読み取りのみ、結果はJSON)。ログイン中の地域住民だけが、自分の分を見られる。
 * GET 任意パラメーター: status=active|withdrawn|all(既定 all)、limit(既定20、最大100)、offset(既定0)。
 * 取消済みの投票も履歴に残る。再投票するには、物件の画面から Vote.action へ POST する。
 */
public class VoteHistoryAction extends Action {
    private static final int DEFAULT_LIMIT = 20, MAX_LIMIT = 100;

    private final VoteHistoryDao historyDao;

    public VoteHistoryAction() {
        this(new VoteHistoryDao());
    }

    public VoteHistoryAction(VoteHistoryDao historyDao) {
        this.historyDao = historyDao;
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        User user = VoteSupport.requireResident(req, res);
        if (user == null) { return; }
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            VoteSupport.error(res, 405, "METHOD_NOT_ALLOWED", "GETで呼び出してください。");
            return;
        }

        VoteHistoryDao.Status status;
        int limit, offset;
        try {
            status = parseStatus(req.getParameter("status"));
            limit = parseInt(req.getParameter("limit"), DEFAULT_LIMIT, 1, MAX_LIMIT, "limit");
            offset = parseInt(req.getParameter("offset"), 0, 0, Integer.MAX_VALUE, "offset");
        } catch (IllegalArgumentException e) {
            VoteSupport.error(res, 400, "INVALID_INPUT", e.getMessage());
            return;
        }

        try {
            VoteHistoryPage page = historyDao.list(user.getUserId(), status, limit, offset);
            VoteSupport.write(res, 200, toMap(page));
        } catch (SQLException e) {
            VoteSupport.logDatabaseError(req, e);
            VoteSupport.error(res, 500, "SERVER_ERROR", "投票履歴を取得できませんでした。時間をおいてもう一度お試しください。");
        }
    }

    static Map<String, Object> toMap(VoteHistoryPage page) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("total", page.total());
        body.put("limit", page.limit());
        body.put("offset", page.offset());
        body.put("hasMore", page.offset() + page.items().size() < page.total());
        List<Map<String, Object>> votes = new ArrayList<>();
        for (VoteHistoryPage.Item item : page.items()) {
            Map<String, Object> map = VoteSupport.toMap(item.vote());
            map.put("createdAt", item.createdAt() == null ? null : item.createdAt().toString());
            map.put("withdrawnAt", item.vote().getWithdrawnAt() == null ? null : item.vote().getWithdrawnAt().toString());
            Map<String, Object> property = new LinkedHashMap<>();
            property.put("propertyId", item.vote().getPropertyId());
            property.put("name", item.propertyName());
            property.put("prefecture", item.prefecture());
            property.put("municipality", item.municipality());
            property.put("addressLine", item.addressLine());
            property.put("listingStatus", item.listingStatus());
            property.put("deleted", item.propertyDeleted());
            map.put("property", property);
            votes.add(map);
        }
        body.put("votes", votes);
        return body;
    }

    private static VoteHistoryDao.Status parseStatus(String raw) {
        if (raw == null || raw.isBlank()) { return VoteHistoryDao.Status.ALL; }
        return switch (raw.strip().toLowerCase(java.util.Locale.ROOT)) {
            case "active" -> VoteHistoryDao.Status.ACTIVE;
            case "withdrawn" -> VoteHistoryDao.Status.WITHDRAWN;
            case "all" -> VoteHistoryDao.Status.ALL;
            default -> throw new IllegalArgumentException("statusは active・withdrawn・all のどれかを指定してください。");
        };
    }

    private static int parseInt(String raw, int fallback, int min, int max, String label) {
        if (raw == null || raw.isBlank()) { return fallback; }
        try {
            int value = Integer.parseInt(raw.strip())  ;
            if (value < min || value > max) { throw new NumberFormatException(); }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + "が正しくありません。");
        }
    }
}
