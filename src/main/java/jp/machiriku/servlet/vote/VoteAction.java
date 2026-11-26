package jp.machiriku.servlet.vote;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.PropertyVote;
import jp.machiriku.bean.User;
import jp.machiriku.dao.VoteDao;
import jp.machiriku.dao.VoteException;
import jp.machiriku.tool.Action;

/**
 * 地域住民の物件投票API。結果はすべてJSONで返す(画面のS03から呼ばれる想定)。
 * <ul>
 *   <li>GET  ?propertyId=N                       自分の投票内容を取得(未投票なら vote が null)</li>
 *   <li>POST propertyId, rank1〜rank3, frequency, comment [, locationId]   投票・再投票(locationId は距離の基準にする生活拠点。省略すると一番近い拠点)</li>
 *   <li>POST propertyId, withdraw=1              投票の取消</li>
 * </ul>
 * POSTは画面のJavaScriptから送り、ヘッダー X-Requested-With: XMLHttpRequest を付ける。
 */
public class VoteAction extends Action {
    private final VoteDao voteDao;

    public VoteAction() {
        this(new VoteDao());
    }

    public VoteAction(VoteDao voteDao) {
        this.voteDao = voteDao;
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        User user = VoteSupport.requireResident(req, res);
        if (user == null) { return; }

        String method = req.getMethod();
        boolean post = "POST".equalsIgnoreCase(method);
        if (!post && !"GET".equalsIgnoreCase(method)) {
            VoteSupport.error(res, 405, "METHOD_NOT_ALLOWED", "GETまたはPOSTで呼び出してください。");
            return;
        }
        if (post && !VoteSupport.hasAjaxHeader(req)) {
            VoteSupport.error(res, 403, "CSRF", "不正なリクエストです。画面から操作してください。");
            return;
        }

        int propertyId;
        try {
            propertyId = Integer.parseInt(req.getParameter("propertyId").strip());
        } catch (RuntimeException e) {
            VoteSupport.error(res, 400, VoteException.INVALID_INPUT, "物件IDが正しくありません。");
            return;
        }

        try {
            if (!post) {
                respondCurrentVote(res, user, propertyId);
            } else if ("1".equals(req.getParameter("withdraw"))) {
                respondWithdraw(res, user, propertyId);
            } else {
                respondSave(req, res, user, propertyId);
            }
        } catch (VoteException e) {
            VoteSupport.error(res, statusOf(e.getCode()), e.getCode(), e.getMessage());
        } catch (SQLException e) {
            VoteSupport.logDatabaseError(req, e);
            VoteSupport.error(res, 500, "SERVER_ERROR", "投票を処理できませんでした。時間をおいてもう一度お試しください。");
        }
    }

    private void respondCurrentVote(HttpServletResponse res, User user, int propertyId) throws Exception {
        Optional<PropertyVote> vote = voteDao.findVote(user.getUserId(), propertyId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        // 取消済みの投票は「投票していない」として返す(画面はフォームを空で表示する)。
        body.put("vote", vote.filter(v -> !v.isWithdrawn()).map(VoteSupport::toMap).orElse(null));
        VoteSupport.write(res, 200, body);
    }

    private void respondWithdraw(HttpServletResponse res, User user, int propertyId) throws Exception {
        boolean withdrawn = voteDao.withdrawVote(user.getUserId(), propertyId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("withdrawn", withdrawn);
        VoteSupport.write(res, 200, body);
    }

    private void respondSave(HttpServletRequest req, HttpServletResponse res, User user, int propertyId) throws Exception {
        boolean updated = voteDao.findVote(user.getUserId(), propertyId).filter(v -> !v.isWithdrawn()).isPresent();
        String frequency = req.getParameter("frequency");
        if (frequency == null) { frequency = req.getParameter("visitFrequency"); }
        Integer locationId = null;
        String rawLocation = req.getParameter("locationId");
        if (rawLocation != null && !rawLocation.isBlank()) {
            try {
                locationId = Integer.valueOf(rawLocation.strip());
                if (locationId < 1) { throw new NumberFormatException(); }
            } catch (NumberFormatException e) {
                throw new VoteException(VoteException.INVALID_INPUT, "生活拠点が正しくありません。");
            }
        }
        PropertyVote saved = voteDao.saveVote(user.getUserId(), propertyId, locationId,
                List.of(nullToEmpty(req.getParameter("rank1")), nullToEmpty(req.getParameter("rank2")), nullToEmpty(req.getParameter("rank3"))),
                frequency, req.getParameter("comment"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("updated", updated);
        body.put("vote", VoteSupport.toMap(saved));
        VoteSupport.write(res, 200, body);
    }

    private static String nullToEmpty(String value) { return value == null ? "" : value; }

    private static int statusOf(String code) {
        return switch (code) {
            case VoteException.INVALID_INPUT, VoteException.BAD_CHOICES, VoteException.BAD_LOCATION -> 400;
            case VoteException.NOT_RESIDENT -> 403;
            case VoteException.PROPERTY_NOT_FOUND -> 404;
            case VoteException.PROPERTY_CLOSED, VoteException.PROPERTY_NO_LOCATION, VoteException.NO_LOCATION -> 409;
            default -> 400;
        };
    }
}
