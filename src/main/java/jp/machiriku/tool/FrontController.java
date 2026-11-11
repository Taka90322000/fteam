package jp.machiriku.tool;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// *.actionへのリクエストを受け取り、URLから対応するActionクラスを呼び出す。
@WebServlet(urlPatterns = {"*.action"})
public class FrontController extends HttpServlet {

    // 画面表示とフォーム送信の共通振り分け処理。
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

    // POSTも同じ振り分けを通し、GET/POSTの違いは各Action側で判断する。
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
