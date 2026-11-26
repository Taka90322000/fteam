package jp.machiriku.dao.management;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.machiriku.bean.management.AdminAuditLogItem;
import jp.machiriku.bean.management.AdminDashboardSummary;

/**
 * 管理者権限の確認、会員行ロック、監査ログの記録・照会、および運営ダッシュボード集計を行うDAO。
 */
public class AdminAuditDao extends ManagementBaseDao {
    /** 監査ログの対象キーと変更内容をJSON文字列へ変換するマッパー。 */
    private static final ObjectMapper JSON = new ObjectMapper();

    /** 本番接続を使用するDAOを生成する。 */
    public AdminAuditDao() {
        super();
    }

    /**
     * テスト用の共有接続を使用するDAOを生成する。
     * @param sharedConnection テスト用接続。
     */
    public AdminAuditDao(Connection sharedConnection) {
        super(sharedConnection);
    }

    /**
     * 指定会員が現在有効な管理権限を持つか確認する。
     * @param userId 確認する会員ID。
     * @return 本登録済みで利用可能な管理者の場合はtrue。
     * @throws SQLException DB参照に失敗した場合。
     */
    public boolean isAdmin(int userId) throws SQLException {
        try (Connection con = openConnection()) {
            return isAdmin(con, userId, false);
        }
    }

    /**
     * 既存のトランザクション接続上で、指定会員が有効な管理権限を持つか確認する。
     * @param con 共有するDB接続。
     * @param userId 確認する会員ID。
     * @param lockRow 更新中に権限剥奪や停止と競合させないため行ロックする場合はtrue。
     * @return 本登録済みで利用可能な管理者の場合はtrue。
     * @throws SQLException DB参照に失敗した場合。
     */
    public boolean isAdmin(Connection con, int userId, boolean lockRow) throws SQLException {
        // 公開登録の利用区分とは独立したadmin_membersテーブルで権限を確認する。
        String sql = """
                SELECT u.user_id
                FROM admin_members a
                JOIN users u ON u.user_id = a.user_id
                WHERE u.user_id = ?
                  AND u.account_status = 'ACTIVE'
                  AND u.withdrawn_at IS NULL
                  AND a.is_active
                """ + (lockRow ? " FOR SHARE OF a, u" : "");
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 利用停止対象から除外すべき有効な管理権限レコード（admin_members.is_active）が存在するか確認する。
     * @param con 共有するDB接続。
     * @param userId 確認する会員ID。
     * @return 有効な管理権限レコードが存在する場合はtrue。
     * @throws SQLException DB参照に失敗した場合。
     */
    public boolean hasActiveAdminGrant(Connection con, int userId) throws SQLException {
        String sql = "SELECT user_id FROM admin_members WHERE user_id = ? AND is_active";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * サポート操作を実行できる会員であることを確認し、更新中の状態変更を防ぐため行をロックする。
     * @param con トランザクション中のDB接続。
     * @param userId 対象会員のID。
     * @param activeOnly 本登録済み（ACTIVE）の会員のみ許可する場合はtrue。
     * @return 条件を満たす会員が存在しロックできた場合はtrue。
     * @throws SQLException DB参照に失敗した場合。
     */
    public boolean lockEligibleMember(Connection con, int userId, boolean activeOnly) throws SQLException {
        // 通報などはACTIVEのみ、問い合わせはPENDINGも含めて操作可能とする。
        String sql = """
                SELECT user_id
                FROM users
                WHERE user_id = ?
                  AND withdrawn_at IS NULL
                  AND (account_status = 'ACTIVE' OR (NOT ? AND account_status = 'PENDING'))
                FOR SHARE
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setBoolean(2, activeOnly);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 担当者選択欄に表示する有効な管理者一覧を取得する。
     * @return 会員ID文字列をキー、表示名（ID付き）を値とする選択肢マップ。
     * @throws SQLException DB参照に失敗した場合。
     */
    public Map<String, String> findActiveAdminChoices() throws SQLException {
        String sql = """
                SELECT u.user_id, u.display_name || ' (#' || u.user_id || ')' AS label
                FROM admin_members a
                JOIN users u ON u.user_id = a.user_id
                WHERE a.is_active
                  AND u.account_status = 'ACTIVE'
                  AND u.withdrawn_at IS NULL
                ORDER BY u.display_name, u.user_id
                """;
        // 表示順を保持するためLinkedHashMapに格納する。
        Map<String, String> choices = new LinkedHashMap<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                choices.put(String.valueOf(rs.getInt("user_id")), rs.getString("label"));
            }
        }
        return choices;
    }

    /**
     * 管理者の更新操作と同じトランザクション内で監査ログを1件記録する。
     * @param con トランザクション中のDB接続。
     * @param actorId 操作した管理者の会員ID。
     * @param action 操作コード（例: HIDE, UPDATE, RESOLVE）。
     * @param targetType 対象区分（例: PROPERTY, USER, REPORT）。
     * @param targetKey 対象を特定する主キーのマップ。
     * @param reason 操作理由。不要な操作ではnull。
     * @param changes 変更前後の差分マップ。個人情報や本文は含めないこと。
     * @throws SQLException 監査ログの保存に失敗した場合。
     */
    public void insertLog(Connection con, int actorId, String action, String targetType,
            Map<String, ?> targetKey, String reason, Map<String, ?> changes) throws SQLException {
        // 対象キーと差分をJSONBとして保存し、後から検索・監査できるようにする。
        String sql = """
                INSERT INTO admin_audit_logs (actor_user_id, action, target_type, target_key, reason, changes)
                VALUES (?, ?, ?, ?::jsonb, ?, ?::jsonb)
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, actorId);
            stmt.setString(2, action);
            stmt.setString(3, targetType);
            stmt.setString(4, JSON.valueToTree(targetKey).toString());
            stmt.setString(5, reason);
            stmt.setString(6, JSON.valueToTree(changes).toString());
            stmt.executeUpdate();
        }
    }

    /**
     * 運営ダッシュボードに表示する対応状況と運用状況の各件数を取得する。
     * @return 集計結果を保持するAdminDashboardSummary。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminDashboardSummary findDashboardSummary() throws SQLException {
        String sql = """
                SELECT
                    (SELECT count(*) FROM reports WHERE report_status = 'OPEN') AS open_reports,
                    (SELECT count(*) FROM reports WHERE report_status = 'IN_REVIEW') AS reviewing_reports,
                    (SELECT count(*) FROM inquiries WHERE inquiry_status = 'WAITING_ADMIN') AS waiting_admin_inquiries,
                    (SELECT count(*) FROM inquiries WHERE inquiry_status = 'IN_PROGRESS') AS in_progress_inquiries,
                    (SELECT count(*) FROM users WHERE account_status = 'ACTIVE') AS active_users,
                    (SELECT count(*) FROM properties
                     WHERE listing_status IN ('OPEN', 'CONTRACTED')
                       AND deleted_at IS NULL AND admin_hidden_at IS NULL) AS visible_properties,
                    (SELECT count(*) FROM surveys WHERE survey_status = 'OPEN') AS open_surveys,
                    (SELECT count(*) FROM coupons
                     WHERE coupon_status = 'ACTIVE'
                       AND CURRENT_TIMESTAMP >= distribution_starts_at
                       AND CURRENT_TIMESTAMP < distribution_ends_at) AS active_coupons
                """;
        // 各件数を1回のSELECTで集計し、JavaBeanへ詰め替える。
        AdminDashboardSummary summary = new AdminDashboardSummary();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                summary.setOpenReports(rs.getLong("open_reports"));
                summary.setReviewingReports(rs.getLong("reviewing_reports"));
                summary.setWaitingAdminInquiries(rs.getLong("waiting_admin_inquiries"));
                summary.setInProgressInquiries(rs.getLong("in_progress_inquiries"));
                summary.setActiveUsers(rs.getLong("active_users"));
                summary.setVisibleProperties(rs.getLong("visible_properties"));
                summary.setOpenSurveys(rs.getLong("open_surveys"));
                summary.setActiveCoupons(rs.getLong("active_coupons"));
            }
        }
        return summary;
    }

    /**
     * 管理者の操作履歴を検索語で絞り込み、新しい順に取得する。
     * @param keyword 絞り込みキーワード。空文字の場合は全件対象。
     * @param limit 取得上限件数（次ページ判定のため PAGE_SIZE + 1 を指定）。
     * @param offset 読み飛ばす件数。
     * @return 操作履歴の一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminAuditLogItem> findAuditLogs(String keyword, int limit, int offset) throws SQLException {
        String sql = """
                SELECT l.occurred_at, l.actor_user_id, u.display_name AS actor_name,
                       l.action, l.target_type, l.target_key::text AS target_key,
                       l.reason, NULLIF(l.changes, '{}'::jsonb)::text AS changes
                FROM admin_audit_logs l
                JOIN users u ON u.user_id = l.actor_user_id
                WHERE strpos(lower(concat_ws(' ', l.action, l.target_type, l.target_key, u.display_name, l.reason)), lower(?)) > 0
                ORDER BY l.occurred_at DESC, l.audit_log_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminAuditLogItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, keyword);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminAuditLogItem item = new AdminAuditLogItem();
                    item.setOccurredAt(getOffsetDateTime(rs, "occurred_at"));
                    item.setActorUserId(rs.getInt("actor_user_id"));
                    item.setActorName(rs.getString("actor_name"));
                    item.setAction(rs.getString("action"));
                    item.setTargetType(rs.getString("target_type"));
                    item.setTargetKey(rs.getString("target_key"));
                    item.setReason(rs.getString("reason"));
                    item.setChanges(rs.getString("changes"));
                    list.add(item);
                }
            }
        }
        return list;
    }
}
