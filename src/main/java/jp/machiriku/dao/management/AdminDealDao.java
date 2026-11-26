package jp.machiriku.dao.management;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jp.machiriku.bean.management.AdminApplicationDetail;
import jp.machiriku.bean.management.AdminOfferDetail;
import jp.machiriku.bean.management.AdminOpeningPlanDetail;
import jp.machiriku.bean.management.AdminTenancyDetail;
import jp.machiriku.bean.management.ManagementProblem;

/** 出店申し込み・契約・出店計画・出店オファーの詳細取得および公開停止を行うDAO。 */
public class AdminDealDao extends ManagementBaseDao {
    /** 管理者権限チェックおよび監査ログ記録に使うDAO。 */
    private final AdminAuditDao auditDao;

    /** 本番接続を使用するDAOを生成する。 */
    public AdminDealDao() {
        super();
        this.auditDao = new AdminAuditDao();
    }

    /**
     * テスト用の共有接続を使用するDAOを生成する。
     * @param sharedConnection テスト用接続。
     */
    public AdminDealDao(Connection sharedConnection) {
        super(sharedConnection);
        this.auditDao = new AdminAuditDao(sharedConnection);
    }

    /**
     * 指定されたオファーIDの出店オファー詳細を取得する。
     * @param offerId オファーID。
     * @return 出店オファー詳細。存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminOfferDetail findOfferById(int offerId) throws SQLException {
        String sql = """
                SELECT o.offer_id, o.property_id, p.property_name,
                       o.owner_user_id, owner.display_name AS owner_name,
                       o.business_user_id, business.business_name,
                       o.creation_kind, o.message, o.offer_status,
                       o.sent_at, o.responded_at
                FROM offers o
                JOIN properties p USING (property_id)
                JOIN users owner ON owner.user_id = o.owner_user_id
                JOIN business_profiles business ON business.user_id = o.business_user_id
                WHERE o.offer_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, offerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminOfferDetail detail = new AdminOfferDetail();
                detail.setOfferId(rs.getInt("offer_id"));
                detail.setPropertyId(rs.getInt("property_id"));
                detail.setPropertyName(rs.getString("property_name"));
                detail.setOwnerUserId(rs.getInt("owner_user_id"));
                detail.setOwnerName(rs.getString("owner_name"));
                detail.setBusinessUserId(rs.getInt("business_user_id"));
                detail.setBusinessName(rs.getString("business_name"));
                detail.setCreationKind(rs.getString("creation_kind"));
                detail.setMessage(rs.getString("message"));
                detail.setOfferStatus(rs.getString("offer_status"));
                detail.setSentAt(getOffsetDateTime(rs, "sent_at"));
                detail.setRespondedAt(getOffsetDateTime(rs, "responded_at"));
                return detail;
            }
        }
    }

    /**
     * 指定された申込IDの出店申し込み詳細と必要資格・書類チェック一覧を取得する。
     * @param applicationId 申込ID。
     * @return 出店申し込み詳細。存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminApplicationDetail findApplicationById(int applicationId) throws SQLException {
        String detailSql = """
                SELECT a.application_id, a.property_id, p.property_name,
                       p.owner_user_id, a.business_user_id, a.application_status,
                       i.industry_name, a.contract_kind, a.requested_start_on,
                       a.requested_end_on, a.viewing_requested_at, a.message,
                       a.owner_reply, a.replied_at, a.created_at, t.tenancy_id
                FROM applications a
                JOIN properties p USING (property_id)
                JOIN industry_types i USING (industry_type_id)
                LEFT JOIN tenancies t USING (application_id)
                WHERE a.application_id = ?
                """;
        String checksSql = """
                SELECT requirement_name_snapshot, is_required_snapshot, condition_text_snapshot,
                       business_confirmed, owner_check_status, check_note
                FROM application_checks
                WHERE application_id = ?
                ORDER BY qualification_id
                """;
        try (Connection con = openConnection()) {
            AdminApplicationDetail detail;
            try (PreparedStatement stmt = con.prepareStatement(detailSql)) {
                stmt.setInt(1, applicationId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    detail = new AdminApplicationDetail();
                    detail.setApplicationId(rs.getInt("application_id"));
                    detail.setPropertyId(rs.getInt("property_id"));
                    detail.setPropertyName(rs.getString("property_name"));
                    detail.setOwnerUserId(rs.getInt("owner_user_id"));
                    detail.setBusinessUserId(rs.getInt("business_user_id"));
                    detail.setApplicationStatus(rs.getString("application_status"));
                    detail.setIndustryName(rs.getString("industry_name"));
                    detail.setContractKind(rs.getString("contract_kind"));
                    detail.setRequestedStartOn(getLocalDate(rs, "requested_start_on"));
                    detail.setRequestedEndOn(getLocalDate(rs, "requested_end_on"));
                    detail.setViewingRequestedAt(getOffsetDateTime(rs, "viewing_requested_at"));
                    detail.setMessage(rs.getString("message"));
                    detail.setOwnerReply(rs.getString("owner_reply"));
                    detail.setRepliedAt(getOffsetDateTime(rs, "replied_at"));
                    detail.setCreatedAt(getOffsetDateTime(rs, "created_at"));
                    detail.setTenancyId(getNullableInt(rs, "tenancy_id"));
                }
            }
            // 申込当時の必要資格・書類チェック一覧を取得して詳細データへまとめる。
            List<AdminApplicationDetail.CheckItem> checks = new ArrayList<>();
            try (PreparedStatement stmt = con.prepareStatement(checksSql)) {
                stmt.setInt(1, applicationId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        AdminApplicationDetail.CheckItem item = new AdminApplicationDetail.CheckItem();
                        item.setRequirementName(rs.getString("requirement_name_snapshot"));
                        item.setRequired(getNullableBoolean(rs, "is_required_snapshot"));
                        item.setConditionText(rs.getString("condition_text_snapshot"));
                        item.setBusinessConfirmed(getNullableBoolean(rs, "business_confirmed"));
                        item.setOwnerCheckStatus(rs.getString("owner_check_status"));
                        item.setCheckNote(rs.getString("check_note"));
                        checks.add(item);
                    }
                }
            }
            detail.setChecks(checks);
            return detail;
        }
    }

    /**
     * 指定された契約IDの契約物件詳細と紐づく出店計画一覧を取得する。
     * @param tenancyId 契約ID。
     * @return 契約物件詳細。存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminTenancyDetail findTenancyById(int tenancyId) throws SQLException {
        String detailSql = """
                SELECT t.tenancy_id, t.application_id, a.property_id, p.property_name,
                       p.owner_user_id, a.business_user_id, t.store_name, t.tenancy_status,
                       t.contract_start_on, t.contract_end_on, t.confirmed_at,
                       t.owner_handover_at, t.business_handover_at, t.opened_on
                FROM tenancies t
                JOIN applications a USING (application_id)
                JOIN properties p USING (property_id)
                WHERE t.tenancy_id = ?
                """;
        String plansSql = """
                SELECT plan_id, title, progress_status
                FROM opening_plans
                WHERE tenancy_id = ?
                ORDER BY updated_at DESC
                LIMIT 25
                """;
        try (Connection con = openConnection()) {
            AdminTenancyDetail detail;
            try (PreparedStatement stmt = con.prepareStatement(detailSql)) {
                stmt.setInt(1, tenancyId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    detail = new AdminTenancyDetail();
                    detail.setTenancyId(rs.getInt("tenancy_id"));
                    detail.setApplicationId(rs.getInt("application_id"));
                    detail.setPropertyId(rs.getInt("property_id"));
                    detail.setPropertyName(rs.getString("property_name"));
                    detail.setOwnerUserId(rs.getInt("owner_user_id"));
                    detail.setBusinessUserId(rs.getInt("business_user_id"));
                    detail.setStoreName(rs.getString("store_name"));
                    detail.setTenancyStatus(rs.getString("tenancy_status"));
                    detail.setContractStartOn(getLocalDate(rs, "contract_start_on"));
                    detail.setContractEndOn(getLocalDate(rs, "contract_end_on"));
                    detail.setConfirmedAt(getOffsetDateTime(rs, "confirmed_at"));
                    detail.setOwnerHandoverAt(getOffsetDateTime(rs, "owner_handover_at"));
                    detail.setBusinessHandoverAt(getOffsetDateTime(rs, "business_handover_at"));
                    detail.setOpenedOn(getLocalDate(rs, "opened_on"));
                }
            }
            // この契約に紐づく出店計画を取得する。
            List<AdminOpeningPlanDetail> plans = new ArrayList<>();
            try (PreparedStatement stmt = con.prepareStatement(plansSql)) {
                stmt.setInt(1, tenancyId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        AdminOpeningPlanDetail plan = new AdminOpeningPlanDetail();
                        plan.setPlanId(rs.getInt("plan_id"));
                        plan.setTitle(rs.getString("title"));
                        plan.setProgressStatus(rs.getString("progress_status"));
                        plans.add(plan);
                    }
                }
            }
            detail.setOpeningPlans(plans);
            return detail;
        }
    }

    /**
     * 指定された計画IDの出店計画詳細を取得する。
     * @param planId 出店計画ID。
     * @return 出店計画詳細。存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminOpeningPlanDetail findOpeningPlanById(int planId) throws SQLException {
        String sql = """
                SELECT op.plan_id, op.tenancy_id, t.application_id, a.property_id,
                       a.business_user_id, op.title, op.body, op.planned_open_on,
                       op.progress_status, op.publication_status, op.published_at, op.updated_at
                FROM opening_plans op
                JOIN tenancies t USING (tenancy_id)
                JOIN applications a USING (application_id)
                WHERE op.plan_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, planId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminOpeningPlanDetail detail = new AdminOpeningPlanDetail();
                detail.setPlanId(rs.getInt("plan_id"));
                detail.setTenancyId(rs.getInt("tenancy_id"));
                detail.setApplicationId(rs.getInt("application_id"));
                detail.setPropertyId(rs.getInt("property_id"));
                detail.setBusinessUserId(rs.getInt("business_user_id"));
                detail.setTitle(rs.getString("title"));
                detail.setBody(rs.getString("body"));
                detail.setPlannedOpenOn(getLocalDate(rs, "planned_open_on"));
                detail.setProgressStatus(rs.getString("progress_status"));
                detail.setPublicationStatus(rs.getString("publication_status"));
                detail.setPublishedAt(getOffsetDateTime(rs, "published_at"));
                detail.setUpdatedAt(getOffsetDateTime(rs, "updated_at"));
                return detail;
            }
        }
    }

    /**
     * 公開中の出店計画を非公開（ARCHIVED）へ変更し、監査ログを同じトランザクションで記録する。
     * @param planId 対象の出店計画ID。
     * @param adminUserId 操作を行う管理者の会員ID。
     * @param reason 公開停止理由。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void archiveOpeningPlan(int planId, int adminUserId, String reason) throws SQLException {
        String sql = """
                UPDATE opening_plans
                SET publication_status = 'ARCHIVED', updated_at = CURRENT_TIMESTAMP
                WHERE plan_id = ? AND publication_status = 'PUBLISHED'
                """;
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                // 更新直前に管理者権限を再確認して行ロックする。
                if (!auditDao.isAdmin(con, adminUserId, true)) {
                    throw new ManagementProblem(403, "このアカウントには管理権限がありません。");
                }
                int updated;
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setInt(1, planId);
                    updated = stmt.executeUpdate();
                }
                if (updated == 0) {
                    throw new ManagementProblem(409, "この出店計画の公開状態は変更されています。");
                }
                auditDao.insertLog(con, adminUserId, "ARCHIVE", "OPENING_PLAN",
                        Map.of("plan_id", planId), reason,
                        Map.of("before", "PUBLISHED", "after", "ARCHIVED"));
                con.commit();
                con.setAutoCommit(true);
            } catch (SQLException | RuntimeException | Error error) {
                rollbackQuietly(con, error);
                throw error;
            }
        }
    }
}
