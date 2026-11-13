package jp.machiriku.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import jp.machiriku.config.AppConfig;

/** DAO共通のDB接続処理。.envと環境変数の読み込みはAppConfigへ集約する。 */
public class Dao {
    /** 新しいPostgreSQL接続を返す。呼び出し元でtry-with-resourcesなどを使って閉じる。 */
    public static Connection getConnection() throws SQLException {
        String url;
        String user;
        String password;
        try {
            url = AppConfig.get("MACHIRIKU_DB_URL");
            user = AppConfig.get("MACHIRIKU_DB_USER");
            password = AppConfig.get("MACHIRIKU_DB_PASSWORD");
        } catch (IllegalStateException e) {
            // 設定の読み込み失敗を、接続情報を含めずにDB処理の例外として通知する。
            throw new SQLException("DB configuration could not be read from .env.");
        }

        if (url == null || user == null || password == null
                || url.isBlank() || user.isBlank() || password.isBlank()) {
            throw new SQLException("DB settings are missing. Configure MACHIRIKU_DB_URL, MACHIRIKU_DB_USER and MACHIRIKU_DB_PASSWORD in .env.");
        }
        
        // Tomcat上でもJDBCドライバーが利用できることを明示的に確認する。
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
