package jp.machiriku.dao.management;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jp.machiriku.bean.management.AdminReportItem;
import jp.machiriku.bean.management.ManagementLabels;
import jp.machiriku.bean.management.ManagementProblem;

/** 管理者向けの通報検索・詳細閲覧・確認中への移行・対応完了を行うDAO。 */
public class AdminReportDao extends ManagementBaseDao {
    /** 通報と通報者・対象・担当者を結合する共通FROM句。 */
    private static final String REPORT_FROM = """
            FROM reports r
            JOIN users sender ON sender.user_id = r.reporter_user_id
            LEFT JOIN users target ON target.user_id = r.target_user_id
            LEFT JOIN owner_profiles op ON op.user_id = target.user_id
            LEFT JOIN business_profiles bp ON bp.user_id = target.user_id
            LEFT JOIN properties p ON p.property_id = r.target_property_id
            LEFT JOIN users handler ON handler.user_id = r.assigned_admin_user_id
            """;

    /** 通報対象の表示名を組み立てるSQL式。 */
    private static final String TARGET_NAME =
            "COALESCE(p.property_name, bp.business_name, op.company_name, target.display_name)";

    /** 管理者権限チェックと監査ログ記録に使うDAO。 */
    private final AdminAuditDao auditDao;

    /** 本番接続を使用するDAOを生成する。 */
    public AdminReportDao() {
        super();
        this.auditDao = new AdminAuditDao();
    }

    /**
     * テスト用の共有接続を使用するDAOを生成する。
     * @param sharedConnection テスト用接続。
     */
    public AdminReportDao(Connection sharedConnection) {
        super(sharedConnection);
        this.auditDao = new AdminAuditDao(sharedConnection);
    }

    /**
     * 対象区分・状態・理由・受付日・検索語で通報一覧を絞り込む。
     * @param kind 対象区分（OWNER / BUSINESS / PROPERTY）。空文字で全件。
     * @param status 対応状態コード。空文字で全件。
     * @param reasonCode 通報理由コード。空文字で全件。
     * @param from 受付日（開始）。nullで指定なし。
     * @param to 受付日（終了）。nullで指定なし。
     * @param keyword 検索語。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 通報一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminReportItem> findReports(String kind, String status, String reasonCode,
            LocalDate from, LocalDate to, String keyword, int limit, int offset) throws SQLException {
        String sql = """
                SELECT r.report_id, COALESCE(target.user_role, 'PROPERTY') AS target_kind,
                """ + TARGET_NAME + " AS target_name, " + """
                       r.reason_code, r.report_status, handler.display_name AS assignee_name, r.created_at,
                       (SELECT count(*) FROM reports pending
                        WHERE pending.report_status = 'OPEN'
                          AND ((r.target_user_id IS NOT NULL AND pending.target_user_id = r.target_user_id)
                            OR (r.target_property_id IS NOT NULL AND pending.target_property_id = r.target_property_id))) AS open_count
                """ + REPORT_FROM + """
                WHERE COALESCE(target.user_role, 'PROPERTY') = COALESCE(NULLIF(?, ''), COALESCE(target.user_role, 'PROPERTY'))
                  AND r.report_status = COALESCE(NULLIF(?, ''), r.report_status)
                  AND r.reason_code = COALESCE(NULLIF(?, ''), r.reason_code)
                  AND (r.created_at AT TIME ZONE 'Asia/Tokyo')::date >= COALESCE(?::date, '-infinity'::date)
                  AND (r.created_at AT TIME ZONE 'Asia/Tokyo')::date <= COALESCE(?::date, 'infinity'::date)
                  AND strpos(lower(concat_ws(' ', r.report_id,
                """ + TARGET_NAME + ")), lower(?)) > 0 " + """
                ORDER BY r.created_at DESC, r.report_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminReportItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, kind);
            stmt.setString(2, status);
            stmt.setString(3, reasonCode);
            stmt.setObject(4, from);
            stmt.setObject(5, to);
            stmt.setString(6, keyword);
            stmt.setInt(7, limit);
            stmt.setInt(8, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminReportItem item = new AdminReportItem();
                    item.setReportId(rs.getLong("report_id"));
                    item.setTargetKind(rs.getString("target_kind"));
                    item.setTargetName(rs.getString("target_name"));
                    item.setReasonCode(rs.getString("reason_code"));
                    item.setStatus(rs.getString("report_status"));
                    item.setAssignedAdminName(rs.getString("assignee_name"));
                    item.setCreatedAt(getOffsetDateTime(rs, "created_at"));
                    item.setOpenCountForSameTarget(rs.getLong("open_count"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 通報の詳細を1件取得する。
     * @param reportId 受付番号。
     * @return 通報詳細。存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminReportItem findReportById(long reportId) throws SQLException {
        String sql = """
                SELECT r.report_id, r.reporter_user_id, sender.display_name AS reporter_name,
                       r.target_user_id, r.target_property_id, r.related_property_id,
                """ + TARGET_NAME + " AS target_name, " + """
                       r.reason_code, r.description, r.report_status,
                       r.assigned_admin_user_id, handler.display_name AS assignee_name,
                       r.created_at, r.resolution_code, r.resolution_note, r.resolved_at
                """ + REPORT_FROM + " WHERE r.report_id = ?";
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, reportId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminReportItem item = new AdminReportItem();
                item.setReportId(rs.getLong("report_id"));
                item.setReporterUserId(rs.getInt("reporter_user_id"));
                item.setReporterName(rs.getString("reporter_name"));
                item.setTargetUserId(getNullableInt(rs, "target_user_id"));
                item.setTargetPropertyId(getNullableInt(rs, "target_property_id"));
                item.setRelatedPropertyId(getNullableInt(rs, "related_property_id"));
                item.setTargetName(rs.getString("target_name"));
                item.setReasonCode(rs.getString("reason_code"));
                item.setDescription(rs.getString("description"));
                item.setStatus(rs.getString("report_status"));
                item.setAssignedAdminUserId(getNullableInt(rs, "assigned_admin_user_id"));
                item.setAssignedAdminName(rs.getString("assignee_name"));
                item.setCreatedAt(getOffsetDateTime(rs, "created_at"));
                item.setResolutionCode(rs.getString("resolution_code"));
                item.setResolutionNote(rs.getString("resolution_note"));
                item.setResolvedAt(getOffsetDateTime(rs, "resolved_at"));
                return item;
            }
        }
    }

    /**
     * 通報の担当者を設定して確認中（IN_REVIEW）にし、監査ログを記録する。
     * @param reportId 受付番号。
     * @param adminUserId 操作する管理者の会員ID。
     * @param assigneeId 担当管理者の会員ID。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void assignReport(long reportId, int adminUserId, int assigneeId) throws SQLException {
        String updateSql = "UPDATE reports SET report_status = 'IN_REVIEW', assigned_admin_user_id = ? WHERE report_id = ?";
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                if (!auditDao.isAdmin(con, adminUserId, true)) {
                    throw new ManagementProblem(403, "このアカウントには管理権限がありません。");
                }
                LockedReport current = lockReport(con, reportId);
                if ("RESOLVED".equals(current.status)) {
                    throw new ManagementProblem(409, "この通報は既に対応済みです。");
                }
                if (!auditDao.isAdmin(con, assigneeId, false)) {
                    throw new ManagementProblem(400, "有効な管理者を選択してください。");
                }
                try (PreparedStatement stmt = con.prepareStatement(updateSql)) {
                    stmt.setInt(1, assigneeId);
                    stmt.setLong(2, reportId);
                    stmt.executeUpdate();
                }
                auditDao.insertLog(con, adminUserId, "REVIEW", "REPORT",
                        Map.of("report_id", reportId), null,
                        Map.of("before", current.status, "after", "IN_REVIEW", "assignee", assigneeId));
                con.commit();
                con.setAutoCommit(true);
            } catch (SQLException | RuntimeException | Error error) {
                rollbackQuietly(con, error);
                throw error;
            }
        }
    }

    /**
     * 通報を対応完了（RESOLVED）にし、必要に応じて物件の掲載停止または会員の利用停止を同一トランザクションで実行する。
     * @param reportId 受付番号。
     * @param adminUserId 操作する管理者の会員ID。
     * @param resolution 対応結果コード（NO_ACTION / CORRECTION / HIDE_PROPERTY / SUSPEND_USER）。
     * @param reason 判断理由。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void resolveReport(long reportId, int adminUserId, String resolution, String reason) throws SQLException {
        String resolveSql = """
                UPDATE reports
                SET report_status = 'RESOLVED', resolution_code = ?, resolution_note = ?,
                    resolved_at = CURRENT_TIMESTAMP,
                    assigned_admin_user_id = COALESCE(assigned_admin_user_id, ?)
                WHERE report_id = ?
                """;
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                if (!auditDao.isAdmin(con, adminUserId, true)) {
                    throw new ManagementProblem(403, "このアカウントには管理権限がありません。");
                }
                LockedReport current = lockReport(con, reportId);
                if ("RESOLVED".equals(current.status)) {
                    throw new ManagementProblem(409, "この通報は既に対応済みです。");
                }
                boolean isProperty = current.targetPropertyId != null;
                if (!ManagementLabels.reportResolutionsFor(isProperty).containsKey(resolution)) {
                    throw new ManagementProblem(400, "対応結果の入力内容を確認してください。");
                }
                // 通報への措置と通報完了・監査ログを1トランザクションでまとめて確定させる。
                if ("HIDE_PROPERTY".equals(resolution)) {
                    hidePropertyInTx(con, current.targetPropertyId, adminUserId, reason);
                } else if ("SUSPEND_USER".equals(resolution)) {
                    suspendUserInTx(con, current.targetUserId, adminUserId, reason);
                }
                try (PreparedStatement stmt = con.prepareStatement(resolveSql)) {
                    stmt.setString(1, resolution);
                    stmt.setString(2, reason);
                    stmt.setInt(3, adminUserId);
                    stmt.setLong(4, reportId);
                    stmt.executeUpdate();
                }
                auditDao.insertLog(con, adminUserId, "RESOLVE", "REPORT",
                        Map.of("report_id", reportId), reason,
                        Map.of("before", current.status, "after", "RESOLVED", "resolution", resolution));
                con.commit();
                con.setAutoCommit(true);
            } catch (SQLException | RuntimeException | Error error) {
                rollbackQuietly(con, error);
                throw error;
            }
        }
    }

    /**
     * 更新対象の通報行をロックして現在状態と対象IDを取得する。
     * @param con トランザクション中のDB接続。
     * @param reportId 受付番号。
     * @return ロック後の通報情報。
     * @throws SQLException DB照会に失敗した場合。
     */
    private LockedReport lockReport(Connection con, long reportId) throws SQLException {
        String sql = "SELECT report_status, target_user_id, target_property_id FROM reports WHERE report_id = ? FOR UPDATE";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, reportId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定された情報が見つかりません。");
                }
                return new LockedReport(
                        rs.getString("report_status"),
                        getNullableInt(rs, "target_user_id"),
                        getNullableInt(rs, "target_property_id"));
            }
        }
    }

    /**
     * 通報対応の一環として対象物件を掲載停止にし、監査ログを記録する。
     * @param con トランザクション中のDB接続。
     * @param propertyId 対象物件ID。
     * @param adminUserId 操作する管理者の会員ID。
     * @param reason 操作理由。
     * @throws SQLException DB更新に失敗した場合。
     */
    private void hidePropertyInTx(Connection con, int propertyId, int adminUserId, String reason) throws SQLException {
        String lockSql = "SELECT admin_hidden_at, deleted_at FROM properties WHERE property_id = ? FOR UPDATE";
        boolean wasHidden;
        try (PreparedStatement stmt = con.prepareStatement(lockSql)) {
            stmt.setInt(1, propertyId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定された情報が見つかりません。");
                }
                if (rs.getObject("deleted_at") != null) {
                    throw new ManagementProblem(409, "削除済みの物件は掲載状態を変更できません。");
                }
                wasHidden = rs.getObject("admin_hidden_at") != null;
            }
        }
        if (wasHidden) {
            return;
        }
        try (PreparedStatement stmt = con.prepareStatement(
                "UPDATE properties SET admin_hidden_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE property_id = ?")) {
            stmt.setInt(1, propertyId);
            stmt.executeUpdate();
        }
        auditDao.insertLog(con, adminUserId, "HIDE", "PROPERTY",
                Map.of("property_id", propertyId), reason, Map.of("before", false, "after", true));
    }

    /**
     * 通報対応の一環として対象会員を利用停止にし、監査ログを記録する。
     * @param con トランザクション中のDB接続。
     * @param targetUserId 対象会員ID。
     * @param adminUserId 操作する管理者の会員ID。
     * @param reason 操作理由。
     * @throws SQLException DB更新に失敗した場合。
     */
    private void suspendUserInTx(Connection con, int targetUserId, int adminUserId, String reason) throws SQLException {
        if (targetUserId == adminUserId || auditDao.hasActiveAdminGrant(con, targetUserId)) {
            throw new ManagementProblem(409, "管理権限が有効な会員は、権限を解除してから利用停止にしてください。");
        }
        String lockSql = "SELECT account_status, withdrawn_at FROM users WHERE user_id = ? FOR UPDATE";
        String previousStatus;
        try (PreparedStatement stmt = con.prepareStatement(lockSql)) {
            stmt.setInt(1, targetUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定された情報が見つかりません。");
                }
                previousStatus = rs.getString("account_status");
                if (rs.getObject("withdrawn_at") != null || "WITHDRAWN".equals(previousStatus)) {
                    throw new ManagementProblem(409, "退会済み会員の状態は変更できません。");
                }
            }
        }
        if ("BAN".equals(previousStatus)) {
            return;
        }
        try (PreparedStatement stmt = con.prepareStatement(
                "UPDATE users SET account_status = 'BAN', updated_at = CURRENT_TIMESTAMP WHERE user_id = ?")) {
            stmt.setInt(1, targetUserId);
            stmt.executeUpdate();
        }
        auditDao.insertLog(con, adminUserId, "SUSPEND", "USER",
                Map.of("user_id", targetUserId), reason, Map.of("before", previousStatus, "after", "BAN"));
    }

    /** ロックした通報の現在状態と対象IDを保持する内部クラス。 */
    private static class LockedReport {
        /** 現在の対応状態。 */
        private final String status;
        /** 通報対象の会員ID。 */
        private final Integer targetUserId;
        /** 通報対象の物件ID。 */
        private final Integer targetPropertyId;

        /**
         * ロック結果を保持する。
         * @param status 現在の対応状態。
         * @param targetUserId 対象会員ID。
         * @param targetPropertyId 対象物件ID。
         */
        private LockedReport(String status, Integer targetUserId, Integer targetPropertyId) {
            this.status = status;
            this.targetUserId = targetUserId;
            this.targetPropertyId = targetPropertyId;
        }
    }
}
