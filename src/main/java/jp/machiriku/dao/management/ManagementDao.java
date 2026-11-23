package jp.machiriku.dao.management;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.machiriku.dao.Dao;

/** 管理・サポート操作のDB接続、パラメーター付きSQL、操作履歴の保存を担当する。 */
public final class ManagementDao implements AutoCloseable {
    /** 指定した条件に合う行が存在しないことを示すSQLSTATE。 */
    public static final String NO_DATA = "02000";
    /** 操作対象と差分をJSONとして保存するための変換器。 */
    private static final ObjectMapper JSON = new ObjectMapper();
    /** 画面処理中に共有する接続。書き込みは一つのトランザクションにまとめる。 */
    private final Connection connection;

    /**
     * 呼び出し元が用意した接続を使用する。
     * @param connection 管理・サポート用の接続。インスタンスの終了時に閉じる。
     */
    public ManagementDao(Connection connection) { this.connection = connection; }

    /**
     * アプリの共通設定から接続を開く。
     * @return 各管理DAOで共用する接続。呼び出し側で閉じること。
     * @throws SQLException 接続できない場合。
     */
    public static ManagementDao open() throws SQLException { return new ManagementDao(Dao.getConnection()); }

    /**
     * トランザクション内で実行するDB操作。
     * @param <T> コミット後に呼び出し元へ返す結果の型。
     */
    @FunctionalInterface
    public interface Work<T> {
        /**
         * 同時に確定させるDB変更を実行する。
         * @return 処理結果。
         * @throws SQLException 問い合わせや更新に失敗した場合。
         */
        T run() throws SQLException;
    }

    /**
     * 状態変更と履歴など、関連する書き込みを一括保存する。
     * @param <T> 更新したレコードの識別子など、処理結果の型。
     * @param work 同一接続で実行する処理。
     * @return コミット済みの処理結果。
     * @throws SQLException DB操作またはコミットに失敗した場合。
     */
    public <T> T transaction(Work<T> work) throws SQLException {
        connection.setAutoCommit(false);
        try {
            // 結果は履歴の書き込みも含めて成功した場合に返す。
            T result = work.run();
            connection.commit();
            connection.setAutoCommit(true);
            return result;
        } catch (SQLException | RuntimeException | Error error) {
            // 制約違反や状態の競合時は、この操作で行った変更をまとめて取り消す。
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException rollbackError) {
                // 取消に失敗した接続では自動コミットを有効にせず、呼び出し元で接続を閉じる。
                error.addSuppressed(rollbackError);
            }
            throw error;
        }
    }

    /**
     * DB上の会員状態と付与済みの管理権限を確認する。
     * @param userId 照会する会員ID。
     * @param writing 更新中に会員状態を変えられないよう、対象会員行をロックする場合はtrue。
     * @return 本登録済みで利用可能な管理者の場合にtrue。
     * @throws SQLException スキーマが未適用、またはDB参照に失敗した場合。
     */
    public boolean isAdmin(int userId, boolean writing) throws SQLException {
        // 公開登録の利用区分と独立した管理権限を参照する。
        return !rows("""
                SELECT u.user_id FROM admin_members a JOIN users u ON u.user_id=a.user_id
                WHERE u.user_id=? AND u.account_status='ACTIVE' AND u.withdrawn_at IS NULL AND a.is_active
                """ + (writing ? " FOR SHARE OF u" : ""), userId).isEmpty();
    }

    /**
     * 再照会した会員状態が利用可能であることを確認し、更新中の停止と競合させない。
     * @param userId ロックする会員ID。
     * @param activeOnly 本登録済みの会員だけを対象にする場合はtrue。
     * @return 条件を満たす会員が存在し、行をロックできた場合はtrue。
     * @throws SQLException DB照会に失敗した場合。
     */
    public boolean lockEligibleMember(int userId, boolean activeOnly) throws SQLException {
        // 利用中の会員と、許可されている場合は登録手続き中の会員をロックする。
        return !rows("""
                SELECT user_id FROM users WHERE user_id=? AND withdrawn_at IS NULL
                AND (account_status='ACTIVE' OR (NOT ? AND account_status='PENDING')) FOR SHARE
                """, userId, activeOnly).isEmpty();
    }

    /**
     * 画面に必要な項目を、SQLで指定した順に取得する。
     * @param sql 配置時に定義したSELECT文。入力値はプレースホルダーで指定する。
     * @param values プレースホルダーの値。
     * @return 表示列名をキーにした結果の一覧。
     * @throws SQLException 照会に失敗した場合。
     */
    List<Map<String, Object>> rows(String sql, Object... values) throws SQLException {
        // DAOから返す項目を選択列に限定し、セッション情報を画面に持ち込まない。
        List<Map<String, Object>> result = new ArrayList<>();
        try (var statement = prepare(sql, values); var rows = statement.executeQuery()) {
            var columns = rows.getMetaData();
            // 検索結果を表示順で保持し、接続を閉じた後も描画できる状態にする。
            while (rows.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int index = 1; index <= columns.getColumnCount(); index++) {
                    row.put(columns.getColumnLabel(index), rows.getObject(index));
                }
                result.add(row);
            }
        }
        return result;
    }

    /**
     * 存在するレコードを一件取得する。
     * @param sql 一件に絞るSQL文。
     * @param values プレースホルダーの値。
     * @return 取得した一件。
     * @throws SQLException 対象が見つからないか、照会に失敗した場合。
     */
    Map<String, Object> one(String sql, Object... values) throws SQLException {
        // 見つからない場合を、権限不足などのDBエラーと区別する。
        var result = rows(sql, values);
        if (result.isEmpty()) { throw new SQLException("Management record not found.", NO_DATA); }
        return result.getFirst();
    }

    /**
     * 更新SQLをパラメーター付きで実行する。
     * @param sql 配置時に定義した更新SQL。
     * @param values プレースホルダーの値。
     * @return 更新した行数。
     * @throws SQLException 更新に失敗した場合。
     */
    int update(String sql, Object... values) throws SQLException {
        try (var statement = prepare(sql, values)) {
            return statement.executeUpdate();
        }
    }

    /**
     * 更新と同じトランザクションで、操作対象・理由・必要な変更差分を記録する。
     * @param actorId 更新を行った管理者の会員ID。
     * @param action 操作の識別コード。
     * @param targetType 対象の種類。
     * @param targetKey 各主キー列の名前と値。
     * @param reason 利用者向けの本文を含めない操作理由。
     * @param changes 状態など、履歴に必要な項目の変更前後の値。
     * @throws SQLException 操作履歴を保存できなかった場合。
     */
    public void audit(int actorId, String action, String targetType, Map<String, ?> targetKey,
            String reason, Map<String, ?> changes) throws SQLException {
        // 電話番号や問い合わせ本文などの元データは、呼び出し元で履歴の対象から外す。
        update("""
                INSERT INTO admin_audit_logs (actor_user_id, action, target_type, target_key, reason, changes)
                VALUES (?, ?, ?, ?::jsonb, ?, ?::jsonb)
                """, actorId, action, targetType, JSON.valueToTree(targetKey).toString(), reason, JSON.valueToTree(changes).toString());
    }

    /**
     * バインド済みの文を作る。
     * @param sql 配置時に定義したSQL。
     * @param values プレースホルダーの値。
     * @return 実行できる文。呼び出し元で閉じる。
     * @throws SQLException 準備や値の設定に失敗した場合。
     */
    private PreparedStatement prepare(String sql, Object[] values) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            // 文字列や数値を文の一部として解釈させず、値として渡す。
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            return statement;
        } catch (SQLException | RuntimeException error) {
            try { statement.close(); } catch (SQLException closeError) { error.addSuppressed(closeError); }
            throw error;
        }
    }

    /**
     * 管理DAOが共用した接続を解放する。
     * @throws SQLException 接続を閉じられなかった場合。
     */
    @Override public void close() throws SQLException { connection.close(); }
}
