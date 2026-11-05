package jp.machiriku.servlet;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** ServletからJSPへの表示を確認する。起動スクリプトの応答確認にも使う。 */
@WebServlet("/hello")
public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        // リクエスト属性に入れた値は、転送先のJSPから参照できる。
        request.setAttribute("message", "Servlet動作確認5");

        // WEB-INF内のJSPは、ブラウザーから直接開かずServlet経由で表示する。
        request.getRequestDispatcher("/WEB-INF/views/hello.jsp")
               .forward(request, response);
    }
}
