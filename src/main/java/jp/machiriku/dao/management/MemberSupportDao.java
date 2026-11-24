package jp.machiriku.dao.management;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jp.machiriku.bean.management.AdminReportItem;
import jp.machiriku.bean.management.InquiryItem;
import jp.machiriku.bean.management.ManagementProblem;

/** 一般会員向けのお問い合わせ作成・返信および通報送信・受付確認を行うDAO。 */
public class MemberSupportDao extends ManagementBaseDao {
    /** 会員状態ロックに使うDAO。 */
    private final AdminAuditDao auditDao;
    /** 公開物件・通報対象の検証に使うDAO。 */
    private final PublicDirectoryDao directoryDao;

    /** 本番接続を使用するDAOを生成する。 */
    public MemberSupportDao() {
        super();
        this.auditDao = new AdminAuditDao();
        this.directoryDao = new PublicDirectoryDao();
    }

    /**
     * テスト用の共有接続を使用するDAOを生成する。
     * @param sharedConnection テスト用接続。
     */
    public MemberSupportDao(Connection sharedConnection) {
        super(sharedConnection);
        this.auditDao = new AdminAuditDao(sharedConnection);
        this.directoryDao = new PublicDirectoryDao(sharedConnection);
    }

    /**
     * 会員本人の問い合わせ履歴一覧を取得する。
     * @param memberId 閲覧する会員ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 問い合わせ一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<InquiryItem> findMemberInquiries(int memberId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT i.inquiry_id, i.subject, i.category, i.inquiry_status, i.updated_at
                FROM inquiries i
                WHERE i.requester_user_id = ?
                ORDER BY i.updated_at DESC, i.inquiry_id DESC
                LIMIT ? OFFSET ?
                """;
        List<InquiryItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, memberId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    InquiryItem item = new InquiryItem();
                    item.setInquiryId(rs.getLong("inquiry_id"));
                    item.setSubject(rs.getString("subject"));
                    item.setCategory(rs.getString("category"));
                    item.setStatus(rs.getString("inquiry_status"));
                    item.setUpdatedAt(getOffsetDateTime(rs, "updated_at"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 会員本人の問い合わせ詳細を1件取得する。他人の受付番号の場合はnullを返す。
     * @param inquiryId 受付番号。
     * @param memberId 閲覧する会員ID。
     * @return 問い合わせ詳細。存在しないか本人のものでない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public InquiryItem findMemberInquiryById(long inquiryId, int memberId) throws SQLException {
        String sql = """
                SELECT i.inquiry_id, i.subject, i.category, i.inquiry_status,
                       i.created_at, i.updated_at, p.property_name
                FROM inquiries i
                LEFT JOIN properties p ON p.property_id = i.related_property_id
                WHERE i.inquiry_id = ? AND i.requester_user_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, inquiryId);
            stmt.setInt(2, memberId);
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
                item.setRelatedPropertyName(rs.getString("property_name"));
                return item;
            }
        }
    }

    /**
     * 会員本人に公開されている問い合わせメッセージ一覧（管理者メモを除外）を取得する。
     * @param inquiryId 受付番号。
     * @param memberId 閲覧する会員ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 公開メッセージ一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<InquiryItem.Message> findMemberMessages(long inquiryId, int memberId, int limit, int offset)
            throws SQLException {
        // 管理者用の内部メモ（ADMIN_ONLY）を除外し、PARTICIPANTSのみを返す。
        String sql = """
                SELECT m.created_at,
                       CASE WHEN m.sender_user_id = i.requester_user_id THEN '利用者' ELSE '運営' END AS sender_label,
                       '返信' AS kind_label,
                       m.body
                FROM inquiry_messages m
                JOIN inquiries i ON i.inquiry_id = m.inquiry_id
                WHERE i.inquiry_id = ?
                  AND i.requester_user_id = ?
                  AND m.visibility = 'PARTICIPANTS'
                ORDER BY m.created_at DESC, m.message_id DESC
                LIMIT ? OFFSET ?
                """;
        List<InquiryItem.Message> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, inquiryId);
            stmt.setInt(2, memberId);
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
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
     * 新しい問い合わせと最初の本文を同じトランザクションで登録する。
     * @param memberId 送信する会員ID（PENDING会員も利用可能）。
     * @param propertyId 関連物件ID。未指定の場合はnull。
     * @param category 問い合わせカテゴリコード。
     * @param subject 件名。
     * @param body 本文。
     * @return 発行された受付番号。
     * @throws SQLException DB保存に失敗した場合。
     */
    public long createMemberInquiry(int memberId, Integer propertyId, String category, String subject, String body)
            throws SQLException {
        String insertInquirySql = """
                INSERT INTO inquiries (requester_user_id, related_property_id, category, subject)
                VALUES (?, ?, ?, ?)
                RETURNING inquiry_id
                """;
        String insertMessageSql = """
                INSERT INTO inquiry_messages (inquiry_id, sender_user_id, body, visibility)
                VALUES (?, ?, ?, 'PARTICIPANTS')
                """;
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                // 送信時点で会員が利用可能（ACTIVEまたはPENDING）かロックして確認する。
                if (!auditDao.lockEligibleMember(con, memberId, false)) {
                    throw new ManagementProblem(403, "このアカウントでは操作できません。");
                }
                if (propertyId != null && directoryDao.findVisiblePropertyById(con, propertyId) == null) {
                    throw new ManagementProblem(404, "指定された情報が見つかりません。");
                }
                long inquiryId;
                try (PreparedStatement stmt = con.prepareStatement(insertInquirySql)) {
                    stmt.setInt(1, memberId);
                    stmt.setObject(2, propertyId);
                    stmt.setString(3, category);
                    stmt.setString(4, subject);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        inquiryId = rs.getLong("inquiry_id");
                    }
                }
                try (PreparedStatement stmt = con.prepareStatement(insertMessageSql)) {
                    stmt.setLong(1, inquiryId);
                    stmt.setInt(2, memberId);
                    stmt.setString(3, body);
                    stmt.executeUpdate();
                }
                con.commit();
                con.setAutoCommit(true);
                return inquiryId;
            } catch (SQLException | RuntimeException | Error error) {
                rollbackQuietly(con, error);
                throw error;
            }
        }
    }

    /**
     * 会員本人の問い合わせへ返信を追加し、状態を「管理者の対応待ち」へ更新する。
     * @param inquiryId 受付番号。
     * @param memberId 返信する会員ID。
     * @param body 返信本文。
     * @throws SQLException DB保存に失敗した場合。
     */
    public void replyMemberInquiry(long inquiryId, int memberId, String body) throws SQLException {
        String lockSql = """
                SELECT inquiry_status, assigned_admin_user_id
                FROM inquiries
                WHERE inquiry_id = ? AND requester_user_id = ?
                FOR UPDATE
                """;
        String insertMessageSql = """
                INSERT INTO inquiry_messages (inquiry_id, sender_user_id, body, visibility)
                VALUES (?, ?, ?, 'PARTICIPANTS')
                """;
        String updateSql = """
                UPDATE inquiries
                SET inquiry_status = 'WAITING_ADMIN', updated_at = CURRENT_TIMESTAMP
                WHERE inquiry_id = ?
                """;
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                if (!auditDao.lockEligibleMember(con, memberId, false)) {
                    throw new ManagementProblem(403, "このアカウントでは操作できません。");
                }
                String currentStatus;
                try (PreparedStatement stmt = con.prepareStatement(lockSql)) {
                    stmt.setLong(1, inquiryId);
                    stmt.setInt(2, memberId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new ManagementProblem(404, "指定された情報が見つかりません。");
                        }
                        currentStatus = rs.getString("inquiry_status");
                    }
                }
                if ("CLOSED".equals(currentStatus)) {
                    throw new ManagementProblem(409, "この問い合わせは対応済みです。");
                }
                try (PreparedStatement stmt = con.prepareStatement(insertMessageSql)) {
                    stmt.setLong(1, inquiryId);
                    stmt.setInt(2, memberId);
                    stmt.setString(3, body);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = con.prepareStatement(updateSql)) {
                    stmt.setLong(1, inquiryId);
                    stmt.executeUpdate();
                }
                con.commit();
                con.setAutoCommit(true);
            } catch (SQLException | RuntimeException | Error error) {
                rollbackQuietly(con, error);
                throw error;
            }
        }
    }

    /**
     * 通報者本人の通報受付状況を取得する。
     * @param reportId 受付番号。
     * @param reporterId 通報者の会員ID。
     * @return 受付完了データ。存在しないか本人のものでない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminReportItem findReportReceipt(long reportId, int reporterId) throws SQLException {
        String sql = """
                SELECT report_id, report_status, created_at
                FROM reports
                WHERE report_id = ? AND reporter_user_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, reportId);
            stmt.setInt(2, reporterId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminReportItem receipt = new AdminReportItem();
                receipt.setReportId(rs.getLong("report_id"));
                receipt.setStatus(rs.getString("report_status"));
                receipt.setCreatedAt(getOffsetDateTime(rs, "created_at"));
                return receipt;
            }
        }
    }

    /**
     * 本登録済み会員からの通報を検証して登録する。
     * @param reporterId 通報する会員ID（ACTIVEのみ許可）。
     * @param kind 通報対象区分（PROPERTY / OWNER / BUSINESS）。
     * @param targetId 通報対象の物件IDまたは会員ID。
     * @param propertyId オーナー通報時の関連物件ID。
     * @param reasonCode 通報理由コード。
     * @param description 通報内容の詳細。
     * @return 発行された受付番号。
     * @throws SQLException DB保存に失敗した場合。
     */
    public long submitReport(int reporterId, String kind, int targetId, Integer propertyId,
            String reasonCode, String description) throws SQLException {
        String insertSql = """
                INSERT INTO reports (reporter_user_id, target_user_id, target_property_id,
                                     related_property_id, reason_code, description)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING report_id
                """;
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                // 通報は本登録済み（ACTIVE）の会員のみに限定する。
                if (!auditDao.lockEligibleMember(con, reporterId, true)) {
                    throw new ManagementProblem(403, "このアカウントでは操作できません。");
                }
                directoryDao.findReportTarget(con, reporterId, kind, targetId, propertyId);
                boolean isProperty = "PROPERTY".equals(kind);
                Integer targetUserId = isProperty ? null : targetId;
                Integer targetPropertyId = isProperty ? targetId : null;
                Integer relatedPropertyId = "OWNER".equals(kind) ? propertyId : null;

                long reportId;
                try (PreparedStatement stmt = con.prepareStatement(insertSql)) {
                    stmt.setInt(1, reporterId);
                    stmt.setObject(2, targetUserId);
                    stmt.setObject(3, targetPropertyId);
                    stmt.setObject(4, relatedPropertyId);
                    stmt.setString(5, reasonCode);
                    stmt.setString(6, description);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        reportId = rs.getLong("report_id");
                    }
                }
                con.commit();
                con.setAutoCommit(true);
                return reportId;
            } catch (SQLException | RuntimeException | Error error) {
                rollbackQuietly(con, error);
                throw error;
            }
        }
    }
}
