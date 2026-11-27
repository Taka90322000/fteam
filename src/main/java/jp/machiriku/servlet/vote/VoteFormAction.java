package jp.machiriku.servlet.vote;

import java.sql.SQLException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.dao.IndustryMasterDao;
import jp.machiriku.dao.VoteContextDao;
import jp.machiriku.dao.VoteDao;
import jp.machiriku.tool.Action;

/**
 * 投票画面(入力フォーム)を表示するAction。地域住民のみ。
 * <p>URL: {@code /jp/machiriku/servlet/vote/VoteForm.action?propertyId=N}(GET)</p>
 * 物件の情報・投票できるか・生活拠点と距離・業種の選択肢・今の投票内容を集め、
 * {@code /WEB-INF/views/vote/vote_form.jsp} へforwardする。送信先は {@link VoteAction}。
 */
public class VoteFormAction extends Action {
    private final VoteFormPage page;

    public VoteFormAction() {
        this(new VoteContextDao(), new IndustryMasterDao(), new VoteDao());
    }

    public VoteFormAction(VoteContextDao contextDao, IndustryMasterDao industryDao, VoteDao voteDao) {
        this.page = new VoteFormPage(contextDao, industryDao, voteDao);
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
            page.show(req, res, user, propertyId, null, null, HttpServletResponse.SC_OK);
        } catch (SQLException e) {
            VoteSupport.logDatabaseError(req, e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
