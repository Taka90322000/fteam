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
import jp.machiriku.dao.management.ManagementDao;
import jp.machiriku.servlet.MemberSession;

/** 通常画面に管理画面へのリンクを表示できるか確認する。 */
@WebFilter(urlPatterns = {"/index.jsp", "/jp/machiriku/servlet/user/Profile.action"})
public final class AdminNavigationFilter implements Filter {
    /** トップ画面とプロフィールに管理画面のリンクを表示するための属性。 */
    private static final String SHOW_ADMIN_LINK = "showAdminLink";

    /** Servletコンテナが通常画面のリンク表示に使用するフィルター。 */
    public AdminNavigationFilter() { }

    /**
     * 権限のあるLINE会員に管理リンクを表示し、既存の画面処理に進む。
     * @param request トップ画面またはプロフィールへのHTTPリクエスト。
     * @param response 後続処理が使用するHTTPレスポンス。
     * @param chain 通常の画面処理を行うフィルターチェーン。
     * @throws IOException レスポンスの書き出しに失敗した場合。
     * @throws ServletException 後続処理でエラーが発生した場合。
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // LINE会員のセッションにだけ、管理リンクを追加できるか調べる。
        var req = (HttpServletRequest) request;
        var member = MemberSession.signedInUser(req.getSession(false));
        if (member != null) {
            try (var db = ManagementDao.open()) {
                if (db.isAdmin(member.getUserId(), false)) {
                    req.setAttribute(SHOW_ADMIN_LINK, true);
                }
            } catch (SQLException error) {
                // 管理機能が未設定でも既存のトップ画面やプロフィールを利用できるようにする。
            }
        }
        chain.doFilter(request, response);
    }
}
