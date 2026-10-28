package jp.machiriku.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import jp.machiriku.config.AppConfig;

public class Dao {
    public static Connection getConnection() throws SQLException {
        String url;
        String user;
        String password;
        try {
            url = AppConfig.get("MACHIRIKU_DB_URL");
            user = AppConfig.get("MACHIRIKU_DB_USER");
            password = AppConfig.get("MACHIRIKU_DB_PASSWORD");
        } catch (IllegalStateException e) {
            throw new SQLException("DB configuration could not be read from .env.");
        }

        if (url == null || user == null || password == null
                || url.isBlank() || user.isBlank() || password.isBlank()) {
            throw new SQLException("DB settings are missing. Configure MACHIRIKU_DB_URL, MACHIRIKU_DB_USER and MACHIRIKU_DB_PASSWORD in .env.");
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
