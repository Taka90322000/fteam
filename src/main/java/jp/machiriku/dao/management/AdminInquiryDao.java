package jp.machiriku.dao.management;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.machiriku.bean.management.InquiryItem;
import jp.machiriku.bean.management.ManagementProblem;

/** 管理者向けの問い合わせ検索・詳細閲覧・担当設定・返信・内部メモ記録を行うDAO。 */
public class AdminInquiryDao extends ManagementBaseDao {
    /** 管理者権限チェックと監査ログ記録に使うDAO。 */
    private final AdminAuditDao auditDao;

    /** 本番接続を使用するDAOを生成する。 */
    public AdminInquiryDao() {
        super();
        this.auditDao = new AdminAuditDao();
    }

    /**
     * テスト用の共有接続を使用するDAOを生成する。
     * @param sharedConnection テスト用接続。
     */
    public AdminInquiryDao(Connection sharedConnection) {
        super(sharedConnection);
        this.auditDao = new AdminAuditDao(sharedConnection);
    }

    /**
     * 管理者が条件で絞り込んだ問い合わせ一覧を取得する（返信待ちを先頭に並べる）。
     * @param category カテゴリコード。空文字で全件。
     * @param status 対応状態コード。空文字で全件。
     * @param assigneeId 担当管理者ID。nullで全件。
     * @param keyword 検索語。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 問い合わせ一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<InquiryItem> findInquiries(String category, String status, Integer assigneeId,
            String keyword, int limit, int offset) throws SQLException {
        String sql = """
                SELECT i.inquiry_id, i.subject, i.category, i.inquiry_status, i.updated_at,
                       sender.display_name AS requester_name, handler.display_name AS assignee_name
                FROM inquiries i
                JOIN users sender ON sender.user_id = i.requester_user_id
                LEFT JOIN users handler ON handler.user_id = i.assigned_admin_user_id
                WHERE i.category = COALESCE(NULLIF(?, ''), i.category)
                  AND i.inquiry_status = COALESCE(NULLIF(?, ''), i.inquiry_status)
                  AND (?::integer IS NULL OR i.assigned_admin_user_id = ?)
                  AND strpos(lower(concat_ws(' ', i.inquiry_id, i.subject, sender.display_name)), lower(?)) > 0
                ORDER BY CASE WHEN i.inquiry_status = 'WAITING_ADMIN' THEN 0 ELSE 1 END,
                         i.updated_at DESC, i.inquiry_id DESC
                LIMIT ? OFFSET ?
                """;
        List<InquiryItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, category);
            stmt.setString(2, status);
            stmt.setObject(3, assigneeId);
            stmt.setObject(4, assigneeId);
            stmt.setString(5, keyword);
            stmt.setInt(6, limit);
            stmt.setInt(7, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    InquiryItem item = new InquiryItem();
                    item.setInquiryId(rs.getLong("inquiry_id"));
                    item.setSubject(rs.getString("subject"));
                    item.setCategory(rs.getString("category"));
                    item.setStatus(rs.getString("inquiry_status"));
                    item.setUpdatedAt(getOffsetDateTime(rs, "updated_at"));
                    item.setRequesterName(rs.getString("requester_name"));
                    item.setAssignedAdminName(rs.getString("assignee_name"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 管理者向けの問い合わせ詳細を1件取得する。
     * @param inquiryId 受付番号。
     * @return 問い合わせ詳細。存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public InquiryItem findInquiryById(long inquiryId) throws SQLException {
        String sql = """
                SELECT i.inquiry_id, i.subject, i.category, i.inquiry_status,
                       i.created_at, i.updated_at, i.related_property_id, p.property_name,
                       i.requester_user_id, sender.display_name AS requester_name,
                       i.assigned_admin_user_id, handler.display_name AS assignee_name
                FROM inquiries i
                JOIN users sender ON sender.user_id = i.requester_user_id
                LEFT JOIN users handler ON handler.user_id = i.assigned_admin_user_id
                LEFT JOIN properties p ON p.property_id = i.related_property_id
                WHERE i.inquiry_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, inquiryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                InquiryItem item = new InquiryItem();
                item.setInquiryId(rs.getLong("inquiry_id"));
                item.setSubject(rs.getString("subject"));
                item.setCategory(rs.getString("category"));
                item.setStatus(rs.getString("inquiry_status"));
                item.setCreatedAt(getOffsetDateTime(rs, "created_at"));
                item.setUpdatedAt(getOffsetDateTime(rs, "updated_at"));
                item.setRelatedPropertyId(getNullableInt(rs, "related_property_id"));
                item.setRelatedPropertyName(rs.getString("property_name"));
                item.setRequesterUserId(rs.getInt("requester_user_id"));
                item.setRequesterName(rs.getString("requester_name"));
                item.setAssignedAdminUserId(getNullableInt(rs, "assigned_admin_user_id"));
                item.setAssignedAdminName(rs.getString("assignee_name"));
                return item;
            }
        }
    }

    /**
     * 問い合わせに紐づく全メッセージ（管理者用内部メモを含む）を取得する。
     * @param inquiryId 受付番号。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return メッセージ一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<InquiryItem.Message> findMessages(long inquiryId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT m.created_at,
                       CASE WHEN m.sender_user_id = i.requester_user_id THEN '利用者' ELSE '運営' END AS sender_label,
                       CASE WHEN m.visibility = 'ADMIN_ONLY' THEN '管理者用メモ' ELSE '返信' END AS kind_label,
                       m.body
                FROM inquiry_messages m
                JOIN inquiries i ON i.inquiry_id = m.inquiry_id
                WHERE i.inquiry_id = ?
                ORDER BY m.created_at DESC, m.message_id DESC
                LIMIT ? OFFSET ?
                """;
        List<InquiryItem.Message> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, inquiryId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    InquiryItem.Message msg = new InquiryItem.Message();
                    msg.setCreatedAt(getOffsetDateTime(rs, "created_at"));
                    msg.setSenderLabel(rs.getString("sender_label"));
                    msg.setKindLabel(rs.getString("kind_label"));
                    msg.setBody(rs.getString("body"));
                    list.add(msg);
                }
            }
        }
        return list;
    }

    /**
     * 問い合わせの担当者と対応状態を更新し、監査ログを記録する。
     * @param inquiryId 受付番号。
     * @param adminUserId 操作する管理者の会員ID。
     * @param assigneeId 新しい担当管理者の会員ID。未割当はnull。
     * @param nextStatus 新しい対応状態コード。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void assignInquiry(long inquiryId, int adminUserId, Integer assigneeId, String nextStatus)
            throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                if (!auditDao.isAdmin(con, adminUserId, true)) {
                    throw new ManagementProblem(403, "このアカウントには管理権限がありません。");
                }
                LockedInquiry current = lockInquiry(con, inquiryId);
                if ("CLOSED".equals(current.status)) {
                    throw new ManagementProblem(409, "この問い合わせは対応済みです。");
                }
                if (assigneeId != null && !auditDao.isAdmin(con, assigneeId, false)) {
                    throw new ManagementProblem(400, "有効な管理者を選択してください。");
                }
                updateInquiryStatusAndAssignee(con, inquiryId, nextStatus, assigneeId);
                Map<String, Object> changes = new LinkedHashMap<>();
                changes.put("before", current.status);
                changes.put("after", nextStatus);
                changes.put("previous_assignee", current.assigneeId);
                changes.put("assignee", assigneeId);
                auditDao.insertLog(con, adminUserId, "ASSIGN", "INQUIRY",
                        Map.of("inquiry_id", inquiryId), null, changes);
                con.commit();
                con.setAutoCommit(true);
            } catch (SQLException | RuntimeException | Error error) {
                rollbackQuietly(con, error);
                throw error;
            }
        }
    }

    /**
     * 利用者への返信を登録し、対応状態（および未割当時の担当者）を更新して監査ログを記録する。
     * @param inquiryId 受付番号。
     * @param adminUserId 返信する管理者の会員ID。
     * @param nextStatus 返信後の対応状態コード。
     * @param body 返信本文。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void replyInquiry(long inquiryId, int adminUserId, String nextStatus, String body) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                if (!auditDao.isAdmin(con, adminUserId, true)) {
                    throw new ManagementProblem(403, "このアカウントには管理権限がありません。");
                }
                LockedInquiry current = lockInquiry(con, inquiryId);
                if ("CLOSED".equals(current.status)) {
                    throw new ManagementProblem(409, "この問い合わせは対応済みです。");
                }
                // 担当者が未割当であれば、返信した管理者自身を担当者として設定する。
                Integer nextAssignee = current.assigneeId != null ? current.assigneeId : adminUserId;
                insertMessage(con, inquiryId, adminUserId, body, "PARTICIPANTS");
                updateInquiryStatusAndAssignee(con, inquiryId, nextStatus, nextAssignee);

                Map<String, Object> changes = new LinkedHashMap<>();
                changes.put("before", current.status);
                changes.put("after", nextStatus);
                changes.put("previous_assignee", current.assigneeId);
                changes.put("assignee", nextAssignee);
                auditDao.insertLog(con, adminUserId, "REPLY", "INQUIRY",
                        Map.of("inquiry_id", inquiryId), null, changes);
                con.commit();
                con.setAutoCommit(true);
            } catch (SQLException | RuntimeException | Error error) {
                rollbackQuietly(con, error);
                throw error;
            }
        }
    }

    /**
     * 利用者には公開しない管理者用内部メモを追加し、監査ログを記録する。
     * @param inquiryId 受付番号。
     * @param adminUserId メモを記録する管理者の会員ID。
     * @param body メモ本文。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void addInternalNote(long inquiryId, int adminUserId, String body) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                if (!auditDao.isAdmin(con, adminUserId, true)) {
                    throw new ManagementProblem(403, "このアカウントには管理権限がありません。");
                }
                lockInquiry(con, inquiryId);
                insertMessage(con, inquiryId, adminUserId, body, "ADMIN_ONLY");
                auditDao.insertLog(con, adminUserId, "ADD_NOTE", "INQUIRY",
                        Map.of("inquiry_id", inquiryId), null, Map.of());
                con.commit();
                con.setAutoCommit(true);
            } catch (SQLException | RuntimeException | Error error) {
                rollbackQuietly(con, error);
                throw error;
            }
        }
    }

    /**
     * 更新対象の問い合わせ行をロックし、現在の状態と担当者IDを取得する。
     * @param con トランザクション中のDB接続。
     * @param inquiryId 受付番号。
     * @return ロック後の現在状態。
     * @throws SQLException DB参照に失敗した場合。
     */
    private LockedInquiry lockInquiry(Connection con, long inquiryId) throws SQLException {
        String sql = "SELECT inquiry_status, assigned_admin_user_id FROM inquiries WHERE inquiry_id = ? FOR UPDATE";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, inquiryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定された情報が見つかりません。");
                }
                return new LockedInquiry(rs.getString("inquiry_status"),
                        getNullableInt(rs, "assigned_admin_user_id"));
            }
        }
    }

    /**
     * 問い合わせメッセージテーブルへ1件挿入する。
     * @param con トランザクション中のDB接続。
     * @param inquiryId 受付番号。
     * @param senderId 投稿者の会員ID。
     * @param body 本文。
     * @param visibility 公開範囲（PARTICIPANTS / ADMIN_ONLY）。
     * @throws SQLException DB挿入に失敗した場合。
     */
    private void insertMessage(Connection con, long inquiryId, int senderId, String body, String visibility)
            throws SQLException {
        String sql = "INSERT INTO inquiry_messages (inquiry_id, sender_user_id, body, visibility) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, inquiryId);
            stmt.setInt(2, senderId);
            stmt.setString(3, body);
            stmt.setString(4, visibility);
            stmt.executeUpdate();
        }
    }

    /**
     * 問い合わせの状態と担当者を更新し、完了時は完了日時を設定する。
     * @param con トランザクション中のDB接続。
     * @param inquiryId 受付番号。
     * @param status 新しい状態コード。
     * @param assigneeId 新しい担当者ID。
     * @throws SQLException DB更新に失敗した場合。
     */
    private void updateInquiryStatusAndAssignee(Connection con, long inquiryId, String status, Integer assigneeId)
            throws SQLException {
        String sql = """
                UPDATE inquiries
                SET inquiry_status = ?, assigned_admin_user_id = ?, updated_at = CURRENT_TIMESTAMP,
                    closed_at = CASE WHEN ? = 'CLOSED' THEN CURRENT_TIMESTAMP END
                WHERE inquiry_id = ?
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setObject(2, assigneeId);
            stmt.setString(3, status);
            stmt.setLong(4, inquiryId);
            stmt.executeUpdate();
        }
    }

    /** ロックした問い合わせの現在状態と担当者を保持する内部クラス。 */
    private static class LockedInquiry {
        /** 現在の対応状態。 */
        private final String status;
        /** 現在の担当者ID。 */
        private final Integer assigneeId;

        /**
         * ロック結果を保持する。
         * @param status 現在の対応状態。
         * @param assigneeId 現在の担当者ID。
         */
        private LockedInquiry(String status, Integer assigneeId) {
            this.status = status;
            this.assigneeId = assigneeId;
        }
    }
}
