package jp.machiriku.servlet.vote;

import java.sql.SQLException;
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
 * 投票完了画面を表示するAction。地域住民のみ。
 * <p>URL: {@code /jp/machiriku/servlet/vote/VoteDone.action?propertyId=N}(GET)</p>
 * 保存済みの自分の投票内容を読み直して表示する({@code /WEB-INF/views/vote/vote_done.jsp})。
 * 投票していない物件を指定した場合は、投票画面へ案内する。
 */
public class VoteDoneAction extends Action {
    private final VoteDao voteDao;
    private final VoteContextDao contextDao;

    public VoteDoneAction() {
        this(new VoteDao(), new VoteContextDao());
    }

    public VoteDoneAction(VoteDao voteDao, VoteContextDao contextDao) {
        this.voteDao = voteDao;
        this.contextDao = contextDao;
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        User user = VoteSupport.requireResident(req, res);
        if (user == null) { return; }
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        Integer propertyId = VoteSupport.positiveInt(req.getParameter("propertyId"));
        if (propertyId == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            Optional<PropertyVote> vote = voteDao.findVote(user.getUserId(), propertyId).filter(v -> !v.isWithdrawn());
            Optional<VoteContext> context = contextDao.load(user.getUserId(), propertyId);
            if (vote.isEmpty() || context.isEmpty()) {
                res.sendRedirect(VoteSupport.url(req, "VoteForm") + "?propertyId=" + propertyId);
                return;
            }
            req.setAttribute("vote", vote.get());
            req.setAttribute("voteContext", context.get());
            req.setAttribute("updated", "1".equals(req.getParameter("updated")));
            req.getRequestDispatcher(VoteViews.DONE).forward(req, res);
        } catch (SQLException e) {
            VoteSupport.logDatabaseError(req, e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
