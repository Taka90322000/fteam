package jp.machiriku.servlet.vote;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.PropertyVote;
import jp.machiriku.bean.User;
import jp.machiriku.bean.VoteContext;
import jp.machiriku.dao.VoteContextDao;
import jp.machiriku.dao.VoteDao;
import jp.machiriku.tool.Action;

/**
 * 投票画面(R01)を開くときに1回呼ぶAPI(GETのみ、結果はJSON)。ログイン中の地域住民だけが使える。
 * 物件の概要、今投票できるか(できない理由)、距離の基準にできる生活拠点と物件までの距離、自分の現在の投票を返す。
 * 画面は、ここで返した locations の locationId を、投票時(Vote.action)の locationId に付けて送る。
 */
public class VoteContextAction extends Action {
    private final VoteContextDao contextDao;
    private final VoteDao voteDao;

    public VoteContextAction() {
        this(new VoteContextDao(), new VoteDao());
    }

    public VoteContextAction(VoteContextDao contextDao, VoteDao voteDao) {
        this.contextDao = contextDao;
        this.voteDao = voteDao;
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        User user = VoteSupport.requireResident(req, res);
        if (user == null) { return; }
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            VoteSupport.error(res, 405, "METHOD_NOT_ALLOWED", "GETで呼び出してください。");
            return;
        }
        int propertyId;
        try {
            propertyId = Integer.parseInt(req.getParameter("propertyId").strip());
            if (propertyId < 1) { throw new NumberFormatException(); }
        } catch (RuntimeException e) {
            VoteSupport.error(res, 400, "INVALID_INPUT", "物件IDが正しくありません。");
            return;
        }

        try {
            Optional<VoteContext> found = contextDao.load(user.getUserId(), propertyId);
            if (found.isEmpty()) {
                VoteSupport.error(res, 404, "PROPERTY_NOT_FOUND", "物件が見つかりません。");
                return;
            }
            Optional<PropertyVote> mine = voteDao.findVote(user.getUserId(), propertyId).filter(vote -> !vote.isWithdrawn());
            VoteSupport.write(res, 200, toMap(found.get(), mine.orElse(null)));
        } catch (SQLException e) {
            VoteSupport.logDatabaseError(req, e);
            VoteSupport.error(res, 500, "SERVER_ERROR", "投票画面の情報を取得できませんでした。時間をおいてもう一度お試しください。");
        }
    }

    static Map<String, Object> toMap(VoteContext context, PropertyVote myVote) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        VoteContext.PropertyInfo info = context.property();
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("propertyId", info.propertyId());
        property.put("name", info.name());
        property.put("prefecture", info.prefecture());
        property.put("municipality", info.municipality());
        property.put("addressLine", info.addressLine());
        property.put("listingStatus", info.listingStatus());
        property.put("distanceScaleKm", info.distanceScaleKm());
        body.put("property", property);
        body.put("votable", context.votable());
        body.put("reason", context.reason());
        body.put("reasonMessage", context.reasonMessage());
        body.put("voteCount", context.voteCount());
        List<Map<String, Object>> locations = new ArrayList<>();
        for (VoteContext.LocationOption option : context.locations()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("locationId", option.locationId());
            map.put("kind", option.kind());
            map.put("label", labelOf(option.kind()));
            map.put("prefecture", option.prefecture());
            map.put("municipality", option.municipality());
            map.put("addressLine", option.addressLine());
            map.put("hasCoordinates", option.hasCoordinates());
            map.put("distanceKm", option.distanceKm());
            map.put("nearest", option.nearest());
            locations.add(map);
        }
        body.put("locations", locations);
        body.put("defaultLocationId", context.defaultLocationId());
        body.put("vote", myVote == null ? null : VoteSupport.toMap(myVote));
        return body;
    }

    private static String labelOf(String kind) {
        return switch (kind == null ? "" : kind) {
            case "HOME" -> "自宅";
            case "WORK" -> "勤務先";
            case "SCHOOL" -> "学校";
            default -> kind;
        };
    }
}
