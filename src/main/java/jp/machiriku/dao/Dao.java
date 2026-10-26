package jp.machiriku.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Dao {
    public static Connection getConnection() throws SQLException {
        String url = System.getenv("MACHIRIKU_DB_URL");
        String user = System.getenv("MACHIRIKU_DB_USER");
        String password = System.getenv("MACHIRIKU_DB_PASSWORD");

        if (url == null || user == null || password == null
                || url.isBlank() || user.isBlank() || password.isBlank()) {
            throw new SQLException("DB environment variables are missing.");
        }
        
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "PostgreSQL JDBC Driver not found", e
            );
        }

        return DriverManager.getConnection(
                url, user, password
        );
    }
}
