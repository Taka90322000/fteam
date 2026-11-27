package jp.machiriku.servlet.vote;

import java.sql.SQLException;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.VoteHistoryPage;
import jp.machiriku.dao.VoteHistoryDao;
import jp.machiriku.tool.Action;

/**
 * 自分の投票履歴を表示するAction。地域住民のみ。
 * <p>URL: {@code /jp/machiriku/servlet/vote/VoteHistory.action?status=all&page=1}(GET)</p>
 * status は active(有効)・withdrawn(取消済み)・all(すべて、既定)。page は1から数える(1ページ10件)。
 * {@code /WEB-INF/views/vote/vote_history.jsp} へforwardする。取消ボタンの送信先は {@link VoteAction}。
 */
public class VoteHistoryAction extends Action {
    static final int PAGE_SIZE = 10;

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
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        VoteHistoryDao.Status status = parseStatus(req.getParameter("status"));
        Integer requestedPage = req.getParameter("page") == null || req.getParameter("page").isBlank()
                ? Integer.valueOf(1) : VoteSupport.positiveInt(req.getParameter("page"));
        if (status == null || requestedPage == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            VoteHistoryPage history = historyDao.list(user.getUserId(), status, PAGE_SIZE, (requestedPage - 1) * PAGE_SIZE);
            int totalPages = Math.max(1, (history.total() + PAGE_SIZE - 1) / PAGE_SIZE);
            req.setAttribute("history", history);
            req.setAttribute("status", status.name().toLowerCase(Locale.ROOT));
            req.setAttribute("pageNumber", requestedPage);
            req.setAttribute("totalPages", totalPages);
            req.setAttribute("withdrawnNotice", "1".equals(req.getParameter("withdrawn")));
            req.setAttribute("csrfToken", VoteSupport.csrfToken(req.getSession()));
            req.getRequestDispatcher(VoteViews.HISTORY).forward(req, res);
        } catch (SQLException e) {
            VoteSupport.logDatabaseError(req, e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /** 未指定なら ALL。不正な値なら null。 */
    private static VoteHistoryDao.Status parseStatus(String raw) {
        if (raw == null || raw.isBlank()) { return VoteHistoryDao.Status.ALL; }
        return switch (raw.strip().toLowerCase(Locale.ROOT)) {
            case "active" -> VoteHistoryDao.Status.ACTIVE;
            case "withdrawn" -> VoteHistoryDao.Status.WITHDRAWN;
            case "all" -> VoteHistoryDao.Status.ALL;
            default -> null;
        };
    }
}
