package jp.machiriku.tool;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.servlet.ProfileIconSupport;

/** Action URLへのリクエストを受け取り、対応するActionを呼び出す。 */
@WebServlet("*.action")
@MultipartConfig(fileSizeThreshold = 64 * 1024, maxFileSize = ProfileIconSupport.MAX_REQUEST_BYTES,
        maxRequestSize = ProfileIconSupport.MAX_REQUEST_BYTES)
public class FrontController extends HttpServlet {

    /** Servletコンテナが各画面への振り分けに使う共通の入口を生成する。 */
    public FrontController() { }

    /**
     * 画面表示やフォーム送信を、同じリクエストのまま対象のActionに渡す。
     * @param req Action URLへのリクエスト。
     * @param resp Actionからの応答を返すレスポンス。
     * @throws ServletException Servletで処理を続行できなかった場合。
     * @throws IOException レスポンスを送信できなかった場合。
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // コンテキストパスを除いたURLから、先頭のスラッシュを取り除く。
            String path = req.getServletPath().substring(1);
            // 例：jp/machiriku/servlet/auth/Login.action → jp.machiriku.servlet.auth.LoginAction。
            String name = path.replace(".a", "A").replace('/', '.');

            // jp.machiriku.servlet. 配下のActionクラスのみ許可する。
            if (!name.startsWith("jp.machiriku.servlet.")) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Class<?> clazz = Class.forName(name);
            if (!Action.class.isAssignableFrom(clazz)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // URLに対応するActionを引数なしコンストラクターで生成する。
            Action action = (Action) clazz.getDeclaredConstructor().newInstance();
            // 作成したActionへ、同じリクエストとレスポンスを引き渡す。
            action.execute(req, resp);
        } catch (ClassNotFoundException e) {
            if (!resp.isCommitted()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!resp.isCommitted()) {
                resp.sendRedirect(req.getContextPath() + "/");
            }
        }
    }

    /**
     * multipartの容量を確認したうえで、POSTもActionクラスへ振り分ける。
     * @param req 画面から送信されたリクエスト
     * @param resp Actionが使用するレスポンス
     * @throws ServletException Servletで処理を続行できなかった場合
     * @throws IOException レスポンスを送信できなかった場合
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // サイズが分かる場合は、トークンの取得で本文を読み始める前に上限超過を通知する。
        if (ProfileIconSupport.isMultipart(req) && req.getContentLengthLong() > ProfileIconSupport.MAX_REQUEST_BYTES) {
            resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "画像を含む送信内容の容量が上限を超えています。");
            return;
        }
        doGet(req, resp);
    }
}
