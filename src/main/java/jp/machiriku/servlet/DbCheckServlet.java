
package jp.machiriku.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.dao.Dao;

/** アプリと同じ接続設定で、PostgreSQLへ接続できるかを確認する開発用Servlet。 */
@WebServlet("/db-check")
public class DbCheckServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        // 会員データを参照せず、接続先DB名と接続ユーザーだけを確認する。
        String sql = "SELECT current_database(), current_user";

        // try-with-resourcesにより、接続・SQL・結果を処理終了時に解放する。
        try (Connection conn = Dao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                response.setContentType("text/plain; charset=UTF-8");

                response.getWriter().println("DB connection successful!");
                response.getWriter().println(
                    "Database: " + rs.getString(1)
                );
                response.getWriter().println(
                    "User: " + rs.getString(2)
                );
            }

        } catch (SQLException e) {
            // 詳細はサーバーログに記録し、ブラウザーには接続失敗だけを返す。
            getServletContext().log("DB connection failed", e);
            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Database connection failed."
            );
        }
    }
}
