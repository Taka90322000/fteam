package jp.machiriku.servlet.management;

import java.io.IOException;
import java.sql.SQLException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jp.machiriku.bean.User;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.servlet.MemberSession;

/** 通常画面（トップ画面・プロフィール画面）に管理画面へのリンクを表示できるか確認するフィルター。 */
@WebFilter(urlPatterns = {"/index.jsp", "/jp/machiriku/servlet/user/Profile.action"})
public final class AdminNavigationFilter implements Filter {
    /** 通常画面に管理画面リンクを表示するためのリクエスト属性名。 */
    private static final String SHOW_ADMIN_LINK = "showAdminLink";

    /** Servletコンテナが使用する引数なしコンストラクター。 */
    public AdminNavigationFilter() { }

    /**
     * 権限のあるLINE会員にのみ管理リンク表示フラグを設定し、後続処理へ進む。
     * @param request トップ画面またはプロフィール画面へのHTTPリクエスト。
     * @param response 後続処理が使用するHTTPレスポンス。
     * @param chain 通常の画面処理を行うフィルターチェーン。
     * @throws IOException レスポンスの書き出しに失敗した場合。
     * @throws ServletException 後続処理でエラーが発生した場合。
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        User member = MemberSession.signedInUser(req.getSession(false));
        if (member != null) {
            try {
                // 管理権限の有無をDBで確認し、権限がある場合だけリンク表示を許可する。
                AdminAuditDao auditDao = new AdminAuditDao();
                if (auditDao.isAdmin(member.getUserId())) {
                    req.setAttribute(SHOW_ADMIN_LINK, true);
                }
            } catch (SQLException error) {
                // 管理用テーブルが未適用でも既存画面を表示できるよう握りつぶす。
            }
        }
        chain.doFilter(request, response);
    }
}
