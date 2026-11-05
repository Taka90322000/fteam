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
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // コンテキストパスを除いたURLから、先頭のスラッシュを取り除く。
            String path = req.getServletPath().substring(1);
            // 例：jp/machiriku/servlet/auth/Login.action → jp.machiriku.servlet.auth.LoginAction。
            String name = path.replace(".a", "A").replace('/', '.');
            
            System.out.println("★ servlet path -> " + req.getServletPath());
            System.out.println("★ class name -> " + name);
            
            // URLに対応するクラスを読み込み、引数なしコンストラクターで生成する。
            Action action = (Action) Class.forName(name).getDeclaredConstructor().newInstance();
            // 作成したActionへ、同じリクエストとレスポンスを引き渡す。
            action.execute(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            // 例外の詳細を記録した後、空の相対URLへリダイレクトする現在のエラー処理。
            resp.sendRedirect("");
        }
    }
    // POSTも同じ振り分けを通し、GET/POSTの違いは各Action側で判断する。
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
