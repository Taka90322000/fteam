package jp.machiriku.servlet.vote;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.machiriku.bean.User;
import jp.machiriku.dao.IndustryMasterDao;
import jp.machiriku.dao.VoteContextDao;
import jp.machiriku.dao.VoteDao;
import jp.machiriku.dao.VoteException;
import jp.machiriku.tool.Action;

/**
 * 投票フォームの送信を受け取って保存するAction。地域住民のみ、POSTのみ。
 * <p>URL: {@code /jp/machiriku/servlet/vote/Vote.action}(POST)</p>
 * <ul>
 *   <li>投票・再投票: propertyId, rank1〜rank3, frequency, comment, locationId(省略可), csrfToken →
 *       保存して {@code VoteDone.action} へリダイレクト</li>
 *   <li>取消: propertyId, withdraw=1, csrfToken → 取り消して {@code VoteHistory.action} へリダイレクト</li>
 *   <li>入力エラー: 同じ投票画面(vote_form.jsp)を、入力値とエラー文つきで再表示</li>
 * </ul>
 * 保存後はリダイレクトするので、完了画面を更新(リロード)しても二重に投票されない。
 */
public class VoteAction extends Action {
    private final VoteDao voteDao;
    private final VoteFormPage page;

    public VoteAction() {
        this(new VoteDao(), new VoteContextDao(), new IndustryMasterDao());
    }

    public VoteAction(VoteDao voteDao, VoteContextDao contextDao, IndustryMasterDao industryDao) {
        this.voteDao = voteDao;
        this.page = new VoteFormPage(contextDao, industryDao, voteDao);
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        User user = VoteSupport.requireResident(req, res);
        if (user == null) { return; }

        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            // アドレスを直接開いた場合は、投票画面へ案内する。
            Integer id = VoteSupport.positiveInt(req.getParameter("propertyId"));
            if ("GET".equalsIgnoreCase(req.getMethod()) && id != null) {
                res.sendRedirect(VoteSupport.url(req, "VoteForm") + "?propertyId=" + id);
            } else {
                res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
            return;
        }

        req.setCharacterEncoding("UTF-8");
        if (!VoteSupport.validCsrf(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Integer propertyId = VoteSupport.positiveInt(req.getParameter("propertyId"));
        if (propertyId == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            if ("1".equals(req.getParameter("withdraw"))) {
                voteDao.withdrawVote(user.getUserId(), propertyId);
                res.sendRedirect(VoteSupport.url(req, "VoteHistory") + "?withdrawn=1");
            } else {
                save(req, res, user, propertyId);
            }
        } catch (SQLException e) {
            VoteSupport.logDatabaseError(req, e);
            if (!res.isCommitted()) { res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); }
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse res, User user, int propertyId) throws Exception {
        Map<String, String> entered = enteredValues(req);
        try {
            Integer locationId = null;
            String rawLocation = entered.get("locationId");
            if (!rawLocation.isBlank()) {
                locationId = VoteSupport.positiveInt(rawLocation);
                if (locationId == null) { throw new VoteException(VoteException.INVALID_INPUT, "生活拠点が正しくありません。"); }
            }
            boolean updated = voteDao.findVote(user.getUserId(), propertyId).filter(v -> !v.isWithdrawn()).isPresent();
            voteDao.saveVote(user.getUserId(), propertyId, locationId,
                    List.of(entered.get("rank1"), entered.get("rank2"), entered.get("rank3")),
                    entered.get("frequency"), entered.get("comment"));
            res.sendRedirect(VoteSupport.url(req, "VoteDone") + "?propertyId=" + propertyId + (updated ? "&updated=1" : ""));
        } catch (VoteException e) {
            switch (e.getCode()) {
                case VoteException.PROPERTY_NOT_FOUND -> res.sendError(HttpServletResponse.SC_NOT_FOUND);
                case VoteException.NOT_RESIDENT -> res.sendError(HttpServletResponse.SC_FORBIDDEN);
                default -> page.show(req, res, user, propertyId, entered, e.getMessage(), statusOf(e.getCode()));
            }
        }
    }

    /** フォームの入力値。再表示のときに、そのまま入力欄へ戻すために使う。 */
    private static Map<String, String> enteredValues(HttpServletRequest req) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String name : new String[] {"rank1", "rank2", "rank3", "frequency", "comment", "locationId"}) {
            String value = req.getParameter(name);
            values.put(name, value == null ? "" : value);
        }
        return values;
    }

    private static int statusOf(String code) {
        return switch (code) {
            case VoteException.PROPERTY_CLOSED, VoteException.PROPERTY_NO_LOCATION, VoteException.NO_LOCATION -> 409;
            default -> 400;
        };
    }
}
