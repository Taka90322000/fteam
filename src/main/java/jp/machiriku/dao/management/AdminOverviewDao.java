package jp.machiriku.dao.management;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** 管理画面の集計と監査記録を照会する。 */
public final class AdminOverviewDao {
    /** 共通のDAO接続で各データ操作を行うため、このクラスを直接生成しない。 */
    private AdminOverviewDao() { }

    /**
     * 管理者の対応が必要な通報、問い合わせの状態別件数を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> supportCounts(ManagementDao db) throws SQLException {
        return db.one("""
                SELECT (SELECT count(*) FROM reports WHERE report_status='OPEN') AS "未対応の通報",
                       (SELECT count(*) FROM reports WHERE report_status='IN_REVIEW') AS "確認中の通報",
                       (SELECT count(*) FROM inquiries WHERE inquiry_status='WAITING_ADMIN') AS "返信待ちの問い合わせ",
                       (SELECT count(*) FROM inquiries WHERE inquiry_status='IN_PROGRESS') AS "対応中の問い合わせ"
                """);
    }

    /**
     * 利用中会員、公開物件、受付中アンケート、配布クーポンの件数を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> operationCounts(ManagementDao db) throws SQLException {
        return db.one("""
                SELECT (SELECT count(*) FROM users WHERE account_status='ACTIVE') AS "利用中の会員",
                       (SELECT count(*) FROM properties WHERE listing_status IN ('OPEN','CONTRACTED') AND deleted_at IS NULL AND admin_hidden_at IS NULL) AS "掲載中の物件",
                       (SELECT count(*) FROM surveys WHERE survey_status='OPEN') AS "受付中のアンケート",
                       (SELECT count(*) FROM coupons WHERE coupon_status='ACTIVE'
                            AND CURRENT_TIMESTAMP>=distribution_starts_at AND CURRENT_TIMESTAMP<distribution_ends_at) AS "配布期間中のクーポン"
                """);
    }

    /**
     * 管理者の操作履歴を検索語で絞り込み、新しい順に取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param keyword 絞り込みに用いる検索語。空文字は指定なし。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> auditHistory(ManagementDao db, String keyword, int limit, int offset) throws SQLException {
        return db.rows("""
                SELECT l.occurred_at AS "操作日時",l.actor_user_id AS "操作者ID",u.display_name AS "操作者",l.action AS "操作",
                       l.target_type AS "対象区分",l.target_key::text AS "対象ID",l.reason AS "理由",
                       NULLIF(l.changes,'{}'::jsonb)::text AS "変更内容"
                FROM admin_audit_logs l JOIN users u ON u.user_id=l.actor_user_id
                WHERE strpos(lower(concat_ws(' ',l.action,l.target_type,l.target_key,u.display_name,l.reason)),lower(?))>0
                ORDER BY l.occurred_at DESC,l.audit_log_id DESC LIMIT ? OFFSET ?
                """, keyword, limit, offset);
    }
}
