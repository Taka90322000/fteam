package jp.machiriku.dao.management;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jp.machiriku.dao.Dao;

/**
 * 管理・サポート機能の各DAOが共通して利用するDB接続取得とResultSet補助処理。
 */
public abstract class ManagementBaseDao extends Dao {
    /** テスト時に一時テーブルを共有するための外部接続（closeを無効化したプロキシ）。本番実行時はnull。 */
    private final Connection sharedConnection;

    /** 本番用の接続設定から毎回DB接続を開くDAOを生成する。 */
    protected ManagementBaseDao() {
        this(null);
    }

    /**
     * テスト用の一時テーブル接続を共有するDAOを生成する。
     * @param sharedConnection テスト用の接続。nullの場合は通常の接続を使用する。
     */
    protected ManagementBaseDao(Connection sharedConnection) {
        this.sharedConnection = sharedConnection == null ? null : nonClosingProxy(sharedConnection);
    }

    /**
     * DB接続を取得する。テスト時は共有接続を返す。
     * @return 使用するJDBC接続。
     * @throws SQLException 接続の取得に失敗した場合。
     */
    protected Connection openConnection() throws SQLException {
        // テスト用接続が指定されている場合はそのまま利用し、通常時は共通の接続を開く。
        if (sharedConnection != null) {
            return sharedConnection;
        }
        return getConnection();
    }

    /**
     * ResultSetからNULLを許容する整数値を取得する。
     * @param rs 取得元のResultSet。
     * @param column 列名。
     * @return 整数値。DB上の値がNULLの場合はnull。
     * @throws SQLException 列の読み取りに失敗した場合。
     */
    protected static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * ResultSetからNULLを許容する長整数値を取得する。
     * @param rs 取得元のResultSet。
     * @param column 列名。
     * @return 長整数値。DB上の値がNULLの場合はnull。
     * @throws SQLException 列の読み取りに失敗した場合。
     */
    protected static Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * ResultSetからNULLを許容する真偽値を取得する。
     * @param rs 取得元のResultSet。
     * @param column 列名。
     * @return 真偽値。DB上の値がNULLの場合はnull。
     * @throws SQLException 列の読み取りに失敗した場合。
     */
    protected static Boolean getNullableBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * ResultSetからオフセット付き日時を取得する。
     * @param rs 取得元のResultSet。
     * @param column 列名。
     * @return 日時。DB上の値がNULLの場合はnull。
     * @throws SQLException 列の読み取りに失敗した場合。
     */
    protected static OffsetDateTime getOffsetDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class);
    }

    /**
     * ResultSetから日付を取得する。
     * @param rs 取得元のResultSet。
     * @param column 列名。
     * @return 日付。DB上の値がNULLの場合はnull。
     * @throws SQLException 列の読み取りに失敗した場合。
     */
    protected static LocalDate getLocalDate(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }

    /**
     * トランザクション失敗時にロールバックし、自動コミットを元に戻す。
     * @param con ロールバック対象のDB接続。
     * @param primary 発生した元の例外。
     */
    protected static void rollbackQuietly(Connection con, Throwable primary) {
        try {
            con.rollback();
            con.setAutoCommit(true);
        } catch (SQLException rollbackError) {
            // ロールバック失敗で元の例外を隠さないよう抑制例外として記録する。
            primary.addSuppressed(rollbackError);
        }
    }

    /**
     * テスト用の共有接続がtry-with-resourcesで閉じられないようclose()を無効化したプロキシを作成する。
     * @param target 元のDB接続。
     * @return close()を無視するConnectionプロキシ。
     */
    private static Connection nonClosingProxy(Connection target) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    try {
                        return method.invoke(target, args);
                    } catch (InvocationTargetException error) {
                        throw error.getCause();
                    }
                });
    }
}
