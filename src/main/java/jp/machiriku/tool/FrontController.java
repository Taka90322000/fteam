package jp.machiriku.tool;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 1
@WebServlet(urlPatterns = {"*.action"})
public class FrontController extends HttpServlet {
    
    // 2
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // パスを取得
            String path = req.getServletPath().substring(1);
            // ファイル名を所得しクラス名に変換
            String name = path.replace(".a", "A").replace('/', '.');
            
            System.out.println("★ servlet path -> " + req.getServletPath());
            System.out.println("★ class name -> " + name);
            
            // 5
            Action action = (Action) Class.forName(name).getDeclaredConstructor().newInstance();
            // 6
            action.execute(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            // エラーページへリダイレクト
            resp.sendRedirect("");
        }
    }
    // 8
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
