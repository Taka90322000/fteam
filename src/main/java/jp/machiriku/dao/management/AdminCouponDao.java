package jp.machiriku.dao.management;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.machiriku.bean.management.AdminCouponItem;
import jp.machiriku.bean.management.ManagementProblem;

/**
 * クーポンの検索・詳細・発行履歴取得および下書き編集・配布停止/再開・個別無効化を行うDAO。
 */
public class AdminCouponDao extends ManagementBaseDao {
    /** 管理者権限チェックと監査ログ記録に使うDAO。 */
    private final AdminAuditDao auditDao;

    /** 本番接続を使用するDAOを生成する。 */
    public AdminCouponDao() {
        super();
        this.auditDao = new AdminAuditDao();
    }

    /**
     * テスト用の共有接続を使用するDAOを生成する。
     * @param sharedConnection テスト用接続。
     */
    public AdminCouponDao(Connection sharedConnection) {
        super(sharedConnection);
        this.auditDao = new AdminAuditDao(sharedConnection);
    }

    /**
     * 配布状態とキーワードでクーポン一覧を検索する。
     * @param status 配布状態コード（DRAFT / ACTIVE / STOPPED）。空文字で全件。
     * @param keyword 検索語。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return クーポン一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminCouponItem> findCoupons(String status, String keyword, int limit, int offset)
            throws SQLException {
        String sql = """
                SELECT c.coupon_id, c.coupon_title, t.store_name, c.coupon_status,
                       count(r.resident_user_id) AS issued_count,
                       count(r.used_at) AS used_count,
                       count(r.revoked_at) AS revoked_count,
                       c.valid_until
                FROM coupons c
                JOIN tenancies t USING (tenancy_id)
                LEFT JOIN coupon_recipients r USING (coupon_id)
                WHERE c.coupon_status = COALESCE(NULLIF(?, ''), c.coupon_status)
                  AND strpos(lower(concat_ws(' ', c.coupon_id, c.coupon_title, t.store_name)), lower(?)) > 0
                GROUP BY c.coupon_id, t.store_name
                ORDER BY c.updated_at DESC, c.coupon_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminCouponItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, keyword);
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminCouponItem item = new AdminCouponItem();
                    item.setCouponId(rs.getInt("coupon_id"));
                    item.setCouponTitle(rs.getString("coupon_title"));
                    item.setStoreName(rs.getString("store_name"));
                    item.setCouponStatus(rs.getString("coupon_status"));
                    item.setIssuedCount(rs.getLong("issued_count"));
                    item.setUsedCount(rs.getLong("used_count"));
                    item.setRevokedCount(rs.getLong("revoked_count"));
                    item.setValidUntil(getOffsetDateTime(rs, "valid_until"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 指定IDのクーポン詳細と発行・利用・無効化件数を取得する。
     * @param couponId クーポンID。
     * @return クーポン詳細。存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminCouponItem findCouponById(int couponId) throws SQLException {
        String sql = """
                SELECT c.coupon_id, c.coupon_title, t.store_name, c.tenancy_id, a.business_user_id,
                       c.coupon_status, c.benefit_kind, c.discount_percent, c.discount_yen, c.service_text,
                       c.target_kind, i.industry_name, c.target_survey_id,
                       c.distribution_starts_at, c.distribution_ends_at, c.valid_until,
                       c.usage_limit, c.terms_text,
                       count(r.resident_user_id) AS issued_count,
                       count(r.used_at) AS used_count,
                       count(r.revoked_at) AS revoked_count
                FROM coupons c
                JOIN tenancies t USING (tenancy_id)
                JOIN applications a USING (application_id)
                LEFT JOIN industry_types i ON i.industry_type_id = c.target_industry_type_id
                LEFT JOIN coupon_recipients r USING (coupon_id)
                WHERE c.coupon_id = ?
                GROUP BY c.coupon_id, t.store_name, a.business_user_id, i.industry_name
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, couponId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminCouponItem item = new AdminCouponItem();
                item.setCouponId(rs.getInt("coupon_id"));
                item.setCouponTitle(rs.getString("coupon_title"));
                item.setStoreName(rs.getString("store_name"));
                item.setTenancyId(rs.getInt("tenancy_id"));
                item.setBusinessUserId(rs.getInt("business_user_id"));
                item.setCouponStatus(rs.getString("coupon_status"));
                item.setBenefitKind(rs.getString("benefit_kind"));
                item.setDiscountPercent(rs.getBigDecimal("discount_percent"));
                item.setDiscountYen(getNullableInt(rs, "discount_yen"));
                item.setServiceText(rs.getString("service_text"));
                item.setTargetKind(rs.getString("target_kind"));
                item.setTargetIndustryName(rs.getString("industry_name"));
                item.setTargetSurveyId(getNullableInt(rs, "target_survey_id"));
                item.setDistributionStartsAt(getOffsetDateTime(rs, "distribution_starts_at"));
                item.setDistributionEndsAt(getOffsetDateTime(rs, "distribution_ends_at"));
                item.setValidUntil(getOffsetDateTime(rs, "valid_until"));
                item.setUsageLimit(rs.getInt("usage_limit"));
                item.setTermsText(rs.getString("terms_text"));
                item.setIssuedCount(rs.getLong("issued_count"));
                item.setUsedCount(rs.getLong("used_count"));
                item.setRevokedCount(rs.getLong("revoked_count"));
                return item;
            }
        }
    }

    /**
     * クーポンの個別発行履歴を発行日時の新しい順に取得する。
     * @param couponId クーポンID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 発行履歴一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminCouponItem.Recipient> findRecipients(int couponId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT r.resident_user_id, u.display_name, r.issued_at, r.used_at,
                       r.revoked_at, r.redeemed_by_user_id,
                       CASE
                           WHEN r.used_at IS NOT NULL THEN '利用済み'
                           WHEN r.revoked_at IS NOT NULL THEN '無効化済み'
                           WHEN c.valid_until <= CURRENT_TIMESTAMP THEN '期限切れ'
                           ELSE '未使用'
                       END AS usage_state
                FROM coupon_recipients r
                JOIN users u ON u.user_id = r.resident_user_id
                JOIN coupons c USING (coupon_id)
                WHERE r.coupon_id = ?
                ORDER BY r.issued_at DESC, r.resident_user_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminCouponItem.Recipient> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, couponId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminCouponItem.Recipient item = new AdminCouponItem.Recipient();
                    item.setResidentUserId(rs.getInt("resident_user_id"));
                    item.setResidentName(rs.getString("display_name"));
                    item.setIssuedAt(getOffsetDateTime(rs, "issued_at"));
                    item.setUsedAt(getOffsetDateTime(rs, "used_at"));
                    item.setRevokedAt(getOffsetDateTime(rs, "revoked_at"));
                    item.setRedeemedByUserId(getNullableInt(rs, "redeemed_by_user_id"));
                    item.setUsageStateLabel(rs.getString("usage_state"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 特定の受取会員に対する発行記録1件と有効期限内フラグを取得する。
     * @param couponId クーポンID。
     * @param recipientUserId 受取人の会員ID。
     * @return 発行記録。存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminCouponItem.Recipient findRecipient(int couponId, int recipientUserId) throws SQLException {
        String sql = """
                SELECT r.resident_user_id, u.display_name, r.issued_at, r.used_at,
                       r.revoked_at, c.valid_until > CURRENT_TIMESTAMP AS within_validity
                FROM coupon_recipients r
                JOIN users u ON u.user_id = r.resident_user_id
                JOIN coupons c USING (coupon_id)
                WHERE r.coupon_id = ? AND r.resident_user_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, couponId);
            stmt.setInt(2, recipientUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminCouponItem.Recipient item = new AdminCouponItem.Recipient();
                item.setResidentUserId(rs.getInt("resident_user_id"));
                item.setResidentName(rs.getString("display_name"));
                item.setIssuedAt(getOffsetDateTime(rs, "issued_at"));
                item.setUsedAt(getOffsetDateTime(rs, "used_at"));
                item.setRevokedAt(getOffsetDateTime(rs, "revoked_at"));
                item.setWithinValidity(rs.getBoolean("within_validity"));
                return item;
            }
        }
    }

    /**
     * 発行前の下書きクーポンの特典・期間・発行上限・利用条件を更新し、監査ログを記録する。
     * @param actorId 操作した管理者の会員ID。
     * @param couponId クーポンID。
     * @param title クーポン名。
     * @param benefitKind 特典種別（PERCENT / AMOUNT / SERVICE）。
     * @param discountPercent 割合割引時の割引率。
     * @param discountYen 定額割引時の割引額（円）。
     * @param serviceText サービス特典時の説明文。
     * @param startsAt 配布開始日時。
     * @param endsAt 配布終了日時。
     * @param validUntil 利用期限日時。
     * @param usageLimit 発行枚数上限。
     * @param termsText 利用条件。任意。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void updateDraft(int actorId, int couponId, String title, String benefitKind,
            BigDecimal discountPercent, Integer discountYen, String serviceText,
            OffsetDateTime startsAt, OffsetDateTime endsAt, OffsetDateTime validUntil,
            int usageLimit, String termsText) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                CouponLock locked = lockCouponRow(con, couponId);
                if (!"DRAFT".equals(locked.status) || hasAnyRecipient(con, couponId)) {
                    throw new ManagementProblem(409, "内容を編集できるのは、発行前の下書きです。");
                }
                // 監査ログへ変更前後の配布条件を残すため、更新前のスナップショットを取得する。
                Map<String, Object> before = readDraftSnapshot(con, couponId);

                String updateSql = """
                        UPDATE coupons
                        SET coupon_title = ?, benefit_kind = ?, discount_percent = ?, discount_yen = ?,
                            service_text = ?, distribution_starts_at = ?, distribution_ends_at = ?,
                            valid_until = ?, usage_limit = ?, terms_text = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE coupon_id = ?
                        """;
                try (PreparedStatement stmt = con.prepareStatement(updateSql)) {
                    stmt.setString(1, title);
                    stmt.setString(2, benefitKind);
                    stmt.setBigDecimal(3, discountPercent);
                    stmt.setObject(4, discountYen, Types.INTEGER);
                    stmt.setString(5, serviceText);
                    stmt.setObject(6, startsAt);
                    stmt.setObject(7, endsAt);
                    stmt.setObject(8, validUntil);
                    stmt.setInt(9, usageLimit);
                    stmt.setString(10, termsText);
                    stmt.setInt(11, couponId);
                    stmt.executeUpdate();
                }

                Map<String, Object> after = buildDraftSnapshot(
                        title, benefitKind, discountPercent, discountYen, serviceText,
                        startsAt, endsAt, validUntil, usageLimit, termsText);
                auditDao.insertLog(con, actorId, "EDIT_COUPON", "coupon",
                        Map.of("coupon_id", couponId), null,
                        Map.of("before", before, "after", after));

                con.commit();
                con.setAutoCommit(true);
            } catch (Throwable error) {
                rollbackQuietly(con, error);
                if (error instanceof SQLException sqlError) {
                    throw sqlError;
                }
                if (error instanceof RuntimeException runtimeError) {
                    throw runtimeError;
                }
                throw new RuntimeException(error);
            }
        }
    }

    /**
     * クーポンの配布停止（ACTIVE -> STOPPED）または再開（STOPPED -> ACTIVE）を実行し、監査ログを記録する。
     * @param actorId 操作した管理者の会員ID。
     * @param couponId クーポンID。
     * @param stop 停止する場合はtrue、再開する場合はfalse。
     * @param reason 操作理由。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void changeDistributionStatus(int actorId, int couponId, boolean stop, String reason) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                CouponLock locked = lockCouponRow(con, couponId);
                boolean allowed = stop
                        ? "ACTIVE".equals(locked.status)
                        : "STOPPED".equals(locked.status) && locked.inDistributionPeriod;
                if (!allowed) {
                    throw new ManagementProblem(409, "配布状態または配布期限を確認してください。");
                }

                String nextStatus = stop ? "STOPPED" : "ACTIVE";
                String sql = "UPDATE coupons SET coupon_status = ?, updated_at = CURRENT_TIMESTAMP WHERE coupon_id = ?";
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setString(1, nextStatus);
                    stmt.setInt(2, couponId);
                    stmt.executeUpdate();
                }

                auditDao.insertLog(con, actorId, stop ? "STOP_COUPON" : "RESUME_COUPON", "coupon",
                        Map.of("coupon_id", couponId), reason,
                        Map.of("coupon_status", Map.of("before", locked.status, "after", nextStatus)));

                con.commit();
                con.setAutoCommit(true);
            } catch (Throwable error) {
                rollbackQuietly(con, error);
                if (error instanceof SQLException sqlError) {
                    throw sqlError;
                }
                if (error instanceof RuntimeException runtimeError) {
                    throw runtimeError;
                }
                throw new RuntimeException(error);
            }
        }
    }

    /**
     * 有効期間内で未使用の個別発行クーポンを無効化し、監査ログを記録する。
     * @param actorId 操作した管理者の会員ID。
     * @param couponId クーポンID。
     * @param recipientUserId 受取人の会員ID。
     * @param reason 無効化理由。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void revokeRecipient(int actorId, int couponId, int recipientUserId, String reason) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                lockCouponRow(con, couponId);

                // 利用処理と競合しても利用済み券を無効化しないようWHERE句で状態を厳密に確認する。
                String sql = """
                        UPDATE coupon_recipients r
                        SET revoked_at = CURRENT_TIMESTAMP
                        FROM coupons c
                        WHERE r.coupon_id = c.coupon_id
                          AND r.coupon_id = ?
                          AND r.resident_user_id = ?
                          AND r.used_at IS NULL
                          AND r.revoked_at IS NULL
                          AND c.valid_until > CURRENT_TIMESTAMP
                          AND r.issued_at <= CURRENT_TIMESTAMP
                        """;
                int changed;
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setInt(1, couponId);
                    stmt.setInt(2, recipientUserId);
                    changed = stmt.executeUpdate();
                }
                if (changed == 0) {
                    throw new ManagementProblem(409, "この発行分は無効化できません。利用状況を確認してください。");
                }

                auditDao.insertLog(con, actorId, "REVOKE_COUPON_RECIPIENT", "coupon_recipient",
                        Map.of("coupon_id", couponId, "resident_user_id", recipientUserId),
                        reason, Map.of("revoked", Map.of("before", false, "after", true)));

                con.commit();
                con.setAutoCommit(true);
            } catch (Throwable error) {
                rollbackQuietly(con, error);
                if (error instanceof SQLException sqlError) {
                    throw sqlError;
                }
                if (error instanceof RuntimeException runtimeError) {
                    throw runtimeError;
                }
                throw new RuntimeException(error);
            }
        }
    }

    /**
     * トランザクション内で操作者が有効な管理者であることを確認し行ロックする。
     * @param con トランザクション中の接続。
     * @param actorId 管理者会員ID。
     * @throws SQLException DB照会に失敗した場合。
     */
    private void requireAdminInTx(Connection con, int actorId) throws SQLException {
        if (!auditDao.isAdmin(con, actorId, true)) {
            throw new ManagementProblem(403, "このアカウントには管理権限がありません。");
        }
    }

    /**
     * クーポン行を排他ロックし、現在の配布状態と配布期間内フラグを返す。
     * @param con トランザクション中の接続。
     * @param couponId クーポンID。
     * @return ロック結果。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static CouponLock lockCouponRow(Connection con, int couponId) throws SQLException {
        String sql = """
                SELECT coupon_status, distribution_ends_at > CURRENT_TIMESTAMP AS in_period
                FROM coupons
                WHERE coupon_id = ?
                FOR UPDATE
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, couponId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定されたクーポンが見つかりません。");
                }
                return new CouponLock(rs.getString("coupon_status"), rs.getBoolean("in_period"));
            }
        }
    }

    /**
     * 監査ログ用に更新前の下書きクーポン配布条件を読み取る。
     * @param con トランザクション中の接続。
     * @param couponId クーポンID。
     * @return 更新前条件のマップ。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static Map<String, Object> readDraftSnapshot(Connection con, int couponId) throws SQLException {
        String sql = """
                SELECT coupon_title, benefit_kind, discount_percent, discount_yen, service_text,
                       distribution_starts_at, distribution_ends_at, valid_until, usage_limit, terms_text
                FROM coupons
                WHERE coupon_id = ?
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, couponId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定されたクーポンが見つかりません。");
                }
                return buildDraftSnapshot(
                        rs.getString("coupon_title"),
                        rs.getString("benefit_kind"),
                        rs.getBigDecimal("discount_percent"),
                        getNullableInt(rs, "discount_yen"),
                        rs.getString("service_text"),
                        getOffsetDateTime(rs, "distribution_starts_at"),
                        getOffsetDateTime(rs, "distribution_ends_at"),
                        getOffsetDateTime(rs, "valid_until"),
                        rs.getInt("usage_limit"),
                        rs.getString("terms_text"));
            }
        }
    }

    /**
     * 下書きクーポンの各配布条件を監査ログJSON記録用のマップへまとめる。
     * @param title クーポン名。
     * @param benefitKind 特典種別。
     * @param discountPercent 割引率。
     * @param discountYen 割引額。
     * @param serviceText サービス特典文。
     * @param startsAt 配布開始日時。
     * @param endsAt 配布終了日時。
     * @param validUntil 利用期限日時。
     * @param usageLimit 発行枚数上限。
     * @param termsText 利用条件。
     * @return JSON変換可能な項目マップ。
     */
    private static Map<String, Object> buildDraftSnapshot(String title, String benefitKind,
            BigDecimal discountPercent, Integer discountYen, String serviceText,
            OffsetDateTime startsAt, OffsetDateTime endsAt, OffsetDateTime validUntil,
            int usageLimit, String termsText) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("coupon_title", title);
        snapshot.put("benefit_kind", benefitKind);
        snapshot.put("discount_percent", discountPercent);
        snapshot.put("discount_yen", discountYen);
        snapshot.put("service_text", serviceText);
        snapshot.put("distribution_starts_at", startsAt == null ? null : startsAt.toString());
        snapshot.put("distribution_ends_at", endsAt == null ? null : endsAt.toString());
        snapshot.put("valid_until", validUntil == null ? null : validUntil.toString());
        snapshot.put("usage_limit", usageLimit);
        snapshot.put("terms_text", termsText);
        return snapshot;
    }

    /**
     * 指定クーポンに1件でも発行履歴が存在するか確認する。
     * @param con トランザクション中の接続。
     * @param couponId クーポンID。
     * @return 発行済みであればtrue。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static boolean hasAnyRecipient(Connection con, int couponId) throws SQLException {
        String sql = "SELECT 1 FROM coupon_recipients WHERE coupon_id = ? LIMIT 1";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, couponId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * ロックしたクーポン行の更新判定用スナップショット。
     */
    private static final class CouponLock {
        /** 現在の配布状態。 */
        private final String status;
        /** 現在時刻が配布終了日時より前かどうか。 */
        private final boolean inDistributionPeriod;

        /**
         * ロック結果を保持するインスタンスを生成する。
         * @param status 配布状態。
         * @param inDistributionPeriod 配布期間内ならtrue。
         */
        private CouponLock(String status, boolean inDistributionPeriod) {
            this.status = status;
            this.inDistributionPeriod = inDistributionPeriod;
        }
    }
}
