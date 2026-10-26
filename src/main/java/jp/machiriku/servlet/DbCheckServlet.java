
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

@WebServlet("/db-check")
public class DbCheckServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String sql = "SELECT current_database(), current_user";

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
            getServletContext().log("DB connection failed", e);
            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Database connection failed."
            );
        }
    }
}
