package jp.machiriku.servlet.vote;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.IndustryMaster;
import jp.machiriku.bean.PropertyVote;
import jp.machiriku.bean.User;
import jp.machiriku.bean.VoteChoice;
import jp.machiriku.bean.VoteContext;
import jp.machiriku.dao.IndustryMasterDao;
import jp.machiriku.dao.VoteContextDao;
import jp.machiriku.dao.VoteDao;

/**
 * 投票画面(vote_form.jsp)の表示に必要なデータを集めて、JSPへforwardする。
 * 初回表示(VoteFormAction)と、入力エラー時の再表示(VoteAction)の両方から使う。
 */
final class VoteFormPage {
    static final String VIEW = VoteViews.FORM;

    private final VoteContextDao contextDao;
    private final IndustryMasterDao industryDao;
    private final VoteDao voteDao;

    VoteFormPage(VoteContextDao contextDao, IndustryMasterDao industryDao, VoteDao voteDao) {
        this.contextDao = contextDao;
        this.industryDao = industryDao;
        this.voteDao = voteDao;
    }

    /**
     * 投票画面を表示する。物件が見つからなければ404を返す。
     *
     * @param entered 入力エラーで再表示するときの、利用者が入力していた値。null なら現在の投票内容(なければ空)で埋める。
     * @param errorMessage 画面の上部に出すエラー文。なければ null。
     * @param status 応答のHTTPステータス(通常は200、入力エラー時は400など)
     */
    void show(HttpServletRequest req, HttpServletResponse res, User user, int propertyId,
            Map<String, String> entered, String errorMessage, int status) throws Exception {
        Optional<VoteContext> context = contextDao.load(user.getUserId(), propertyId);
        if (context.isEmpty()) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        IndustryMaster industries = industryDao.findActive();
        PropertyVote current = voteDao.findVote(user.getUserId(), propertyId).filter(v -> !v.isWithdrawn()).orElse(null);

        req.setAttribute("voteContext", context.get());
        req.setAttribute("industries", industries);
        req.setAttribute("currentVote", current);
        req.setAttribute("formValues", entered != null ? entered : valuesOf(current, context.get()));
        req.setAttribute("errorMessage", errorMessage);
        req.setAttribute("csrfToken", VoteSupport.csrfToken(req.getSession()));
        if (status != HttpServletResponse.SC_OK) { res.setStatus(status); }
        req.getRequestDispatcher(VIEW).forward(req, res);
    }

    /** 現在の投票内容(なければ空)から、フォームの初期値を作る。 */
    private static Map<String, String> valuesOf(PropertyVote vote, VoteContext context) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("rank1", "");
        values.put("rank2", "");
        values.put("rank3", "");
        values.put("frequency", "週1回程度");
        values.put("comment", "");
        values.put("locationId", context.defaultLocationId() == null ? "" : String.valueOf(context.defaultLocationId()));
        if (vote != null) {
            for (VoteChoice choice : vote.getChoices()) {
                values.put("rank" + choice.getPreferenceRank(), choice.getIndustryCode());
            }
            if (vote.getVisitFrequency() != null) { values.put("frequency", vote.getVisitFrequency()); }
            if (vote.getRequestComment() != null) { values.put("comment", vote.getRequestComment()); }
            boolean known = context.locations().stream().anyMatch(l -> l.locationId() == vote.getLocationId());
            if (known) { values.put("locationId", String.valueOf(vote.getLocationId())); }
        }
        return values;
    }
}
