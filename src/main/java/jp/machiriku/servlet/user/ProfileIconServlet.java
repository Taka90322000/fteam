package jp.machiriku.servlet.user;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.dao.ProfileDao;
import jp.machiriku.profile.ProfileIconStore;
import jp.machiriku.servlet.MemberSession;

/** 保存されたPNGを、認証済みの会員本人のプレビューへ配信する。 */
@WebServlet("/profile-icons/*")
public final class ProfileIconServlet extends HttpServlet {
    /** Servletのシリアライズバージョン。 */
    private static final long serialVersionUID = 1L;

    /** Tomcatが配信用のServletを生成する。 */
    public ProfileIconServlet() { }

    /**
     * ログイン中の会員の保存先からアイコンを返す。
     * @param request 画像URLへのリクエスト
     * @param response PNGの配信に使用するレスポンス
     * @throws IOException 画像の読み込みやレスポンス送信に失敗した場合
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 配信する拡張子とファイル名を限定し、任意のファイルパスとして扱わない。
        String reference = "/profile-icons" + request.getPathInfo();
        if (!ProfileIconStore.manages(reference)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // リクエストから会員IDを受け取らず、DBで最新の利用状態を確認する。
        User user = MemberSession.requireCurrentUser(request, response, request.getSession(false), ProfileDao::new);
        if (user == null) { return; }

        try (InputStream png = ProfileIconStore.configured().open(user.getUserId(), reference)) {
            response.setContentType("image/png");
            response.setHeader("X-Content-Type-Options", "nosniff");
            png.transferTo(response.getOutputStream());
        } catch (NoSuchFileException error) {
            // 別の画像へ差し替えられた場合や、他の会員の画像URLは404で返す。
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
