package jp.machiriku.dao.management;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.machiriku.bean.management.AdminPropertyItem;
import jp.machiriku.bean.management.ManagementProblem;

/** 管理者向けの物件検索・詳細閲覧・関連履歴取得および掲載停止・公開・編集を行うDAO。 */
public class AdminPropertyDao extends ManagementBaseDao {
    /** 管理者権限チェックと監査ログ記録に使うDAO。 */
    private final AdminAuditDao auditDao;

    /** 本番接続を使用するDAOを生成する。 */
    public AdminPropertyDao() {
        super();
        this.auditDao = new AdminAuditDao();
    }

    /**
     * テスト用の共有接続を使用するDAOを生成する。
     * @param sharedConnection テスト用接続。
     */
    public AdminPropertyDao(Connection sharedConnection) {
        super(sharedConnection);
        this.auditDao = new AdminAuditDao(sharedConnection);
    }

    /**
     * 募集状態・掲載状態・キーワードで物件一覧を検索する。
     * @param status 募集・契約状態コード。空文字で全件。
     * @param visibility 管理者の掲載状態（HIDDEN / NORMAL）。空文字で全件。
     * @param keyword 検索語。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 物件一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminPropertyItem> findProperties(String status, String visibility, String keyword,
            int limit, int offset) throws SQLException {
        String sql = """
                SELECT p.property_id, p.property_name,
                       concat_ws('', p.prefecture, p.municipality, p.address_line) AS full_address,
                       COALESCE(o.company_name, o.representative_name, u.display_name) AS owner_name,
                       p.listing_status,
                       CASE
                           WHEN p.deleted_at IS NOT NULL THEN '削除済み'
                           WHEN p.admin_hidden_at IS NOT NULL THEN '掲載停止中'
                           ELSE '通常'
                       END AS management_state,
                       (SELECT count(*) FROM applications a WHERE a.property_id = p.property_id) AS application_count
                FROM properties p
                JOIN users u ON u.user_id = p.owner_user_id
                LEFT JOIN owner_profiles o ON o.user_id = u.user_id
                WHERE p.listing_status = COALESCE(NULLIF(?, ''), p.listing_status)
                  AND (CASE WHEN p.admin_hidden_at IS NOT NULL THEN 'HIDDEN' ELSE 'NORMAL' END)
                      = COALESCE(NULLIF(?, ''), CASE WHEN p.admin_hidden_at IS NOT NULL THEN 'HIDDEN' ELSE 'NORMAL' END)
                  AND strpos(lower(concat_ws(' ', p.property_id, p.property_name, p.prefecture, p.municipality,
                      p.address_line, u.display_name, o.company_name, o.representative_name)), lower(?)) > 0
                ORDER BY p.updated_at DESC, p.property_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminPropertyItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, visibility);
            stmt.setString(3, keyword);
            stmt.setInt(4, limit);
            stmt.setInt(5, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminPropertyItem item = new AdminPropertyItem();
                    item.setPropertyId(rs.getInt("property_id"));
                    item.setPropertyName(rs.getString("property_name"));
                    item.setFullAddress(rs.getString("full_address"));
                    item.setOwnerName(rs.getString("owner_name"));
                    item.setListingStatus(rs.getString("listing_status"));
                    item.setManagementStateLabel(rs.getString("management_state"));
                    item.setApplicationCount(rs.getLong("application_count"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 指定IDの物件詳細を取得する。
     * @param propertyId 物件ID。
     * @return 物件詳細。存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminPropertyItem findPropertyById(int propertyId) throws SQLException {
        String sql = """
                SELECT p.property_id, p.property_name, p.owner_user_id,
                       COALESCE(o.company_name, o.representative_name, u.display_name) AS owner_name,
                       p.prefecture, p.municipality, p.address_line, p.latitude, p.longitude,
                       p.floor_area_sqm, p.monthly_rent_yen, p.description, p.listing_url,
                       p.permit_status, p.listing_status, p.admin_hidden_at, p.deleted_at
                FROM properties p
                JOIN users u ON u.user_id = p.owner_user_id
                LEFT JOIN owner_profiles o ON o.user_id = u.user_id
                WHERE p.property_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, propertyId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminPropertyItem item = new AdminPropertyItem();
                item.setPropertyId(rs.getInt("property_id"));
                item.setPropertyName(rs.getString("property_name"));
                item.setOwnerUserId(rs.getInt("owner_user_id"));
                item.setOwnerName(rs.getString("owner_name"));
                item.setPrefecture(rs.getString("prefecture"));
                item.setMunicipality(rs.getString("municipality"));
                item.setAddressLine(rs.getString("address_line"));
                item.setLatitude(rs.getBigDecimal("latitude"));
                item.setLongitude(rs.getBigDecimal("longitude"));
                item.setFloorAreaSqm(rs.getBigDecimal("floor_area_sqm"));
                item.setMonthlyRentYen(rs.getInt("monthly_rent_yen"));
                item.setDescription(rs.getString("description"));
                item.setListingUrl(rs.getString("listing_url"));
                item.setPermitStatus(rs.getString("permit_status"));
                item.setListingStatus(rs.getString("listing_status"));
                item.setAdminHiddenAt(getOffsetDateTime(rs, "admin_hidden_at"));
                item.setDeletedAt(getOffsetDateTime(rs, "deleted_at"));
                return item;
            }
        }
    }

    /**
     * 物件に登録されている写真一覧を表示順に取得する。
     * @param propertyId 物件ID。
     * @return 写真一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminPropertyItem.Image> findPropertyImages(int propertyId) throws SQLException {
        String sql = "SELECT display_order, storage_key, alt_text FROM property_images WHERE property_id = ? ORDER BY display_order, image_id";
        List<AdminPropertyItem.Image> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, propertyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminPropertyItem.Image item = new AdminPropertyItem.Image();
                    item.setDisplayOrder(rs.getInt("display_order"));
                    item.setStorageKey(rs.getString("storage_key"));
                    item.setAltText(rs.getString("alt_text"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 物件に設定された対象業種一覧を取得する。
     * @param propertyId 物件ID。
     * @return 対象業種一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminPropertyItem.Industry> findPropertyIndustries(int propertyId) throws SQLException {
        String sql = """
                SELECT i.industry_type_id, i.industry_name
                FROM property_industries p
                JOIN industry_types i USING (industry_type_id)
                WHERE p.property_id = ?
                ORDER BY i.display_order, i.industry_type_id
                """;
        List<AdminPropertyItem.Industry> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, propertyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminPropertyItem.Industry item = new AdminPropertyItem.Industry();
                    item.setIndustryTypeId(rs.getInt("industry_type_id"));
                    item.setIndustryName(rs.getString("industry_name"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 物件に設定された契約条件一覧を取得する。
     * @param propertyId 物件ID。
     * @return 契約条件一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminPropertyItem.ContractTerm> findPropertyContractTerms(int propertyId) throws SQLException {
        String sql = """
                SELECT contract_kind, min_days, max_days, daily_rent_yen
                FROM property_contract_terms
                WHERE property_id = ?
                ORDER BY contract_kind
                """;
        List<AdminPropertyItem.ContractTerm> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, propertyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminPropertyItem.ContractTerm item = new AdminPropertyItem.ContractTerm();
                    item.setContractKind(rs.getString("contract_kind"));
                    item.setMinDays(getNullableInt(rs, "min_days"));
                    item.setMaxDays(getNullableInt(rs, "max_days"));
                    item.setDailyRentYen(getNullableInt(rs, "daily_rent_yen"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 物件に対する出店申込・契約履歴を取得する。
     * @param propertyId 物件ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 出店申込履歴一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminPropertyItem.History> findApplicationHistory(int propertyId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT a.application_id, a.business_user_id, b.business_name, a.application_status,
                       t.tenancy_id, t.tenancy_status, a.created_at
                FROM applications a
                JOIN business_profiles b ON b.user_id = a.business_user_id
                LEFT JOIN tenancies t USING (application_id)
                WHERE a.property_id = ?
                ORDER BY a.created_at DESC, a.application_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminPropertyItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, propertyId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminPropertyItem.History item = new AdminPropertyItem.History();
                    item.setApplicationId(rs.getInt("application_id"));
                    item.setBusinessUserId(rs.getInt("business_user_id"));
                    item.setBusinessName(rs.getString("business_name"));
                    item.setStatus(rs.getString("application_status"));
                    item.setTenancyId(getNullableInt(rs, "tenancy_id"));
                    item.setTenancyStatus(rs.getString("tenancy_status"));
                    item.setOccurredAt(getOffsetDateTime(rs, "created_at"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 物件に対する出店オファー履歴を取得する。
     * @param propertyId 物件ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return オファー履歴一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminPropertyItem.History> findOfferHistory(int propertyId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT o.offer_id, o.business_user_id, b.business_name, o.offer_status, o.sent_at
                FROM offers o
                JOIN business_profiles b ON b.user_id = o.business_user_id
                WHERE o.property_id = ?
                ORDER BY o.sent_at DESC, o.offer_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminPropertyItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, propertyId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminPropertyItem.History item = new AdminPropertyItem.History();
                    item.setOfferId(rs.getInt("offer_id"));
                    item.setBusinessUserId(rs.getInt("business_user_id"));
                    item.setBusinessName(rs.getString("business_name"));
                    item.setStatus(rs.getString("offer_status"));
                    item.setOccurredAt(getOffsetDateTime(rs, "sent_at"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 物件に対する通報履歴を取得する。
     * @param propertyId 物件ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 通報履歴一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminPropertyItem.History> findReportHistory(int propertyId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT report_id, report_status, reason_code, created_at
                FROM reports
                WHERE target_property_id = ?
                ORDER BY created_at DESC, report_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminPropertyItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, propertyId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminPropertyItem.History item = new AdminPropertyItem.History();
                    item.setReportId(rs.getLong("report_id"));
                    item.setStatus(rs.getString("report_status"));
                    item.setReasonCode(rs.getString("reason_code"));
                    item.setOccurredAt(getOffsetDateTime(rs, "created_at"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 物件の掲載停止または掲載停止解除を行い、監査ログを記録する。
     * @param propertyId 物件ID。
     * @param adminUserId 操作する管理者の会員ID。
     * @param hide 掲載停止にする場合はtrue、解除する場合はfalse。
     * @param reason 操作理由。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void changeVisibility(int propertyId, int adminUserId, boolean hide, String reason) throws SQLException {
        String lockSql = "SELECT admin_hidden_at, deleted_at FROM properties WHERE property_id = ? FOR UPDATE";
        String updateSql = """
                UPDATE properties
                SET admin_hidden_at = CASE WHEN ? THEN CURRENT_TIMESTAMP END, updated_at = CURRENT_TIMESTAMP
                WHERE property_id = ?
                """;
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                if (!auditDao.isAdmin(con, adminUserId, true)) {
                    throw new ManagementProblem(403, "このアカウントには管理権限がありません。");
                }
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
                if (wasHidden != hide) {
                    try (PreparedStatement stmt = con.prepareStatement(updateSql)) {
                        stmt.setBoolean(1, hide);
                        stmt.setInt(2, propertyId);
                        stmt.executeUpdate();
                    }
                    auditDao.insertLog(con, adminUserId, hide ? "HIDE" : "UNHIDE", "PROPERTY",
                            Map.of("property_id", propertyId), reason,
                            Map.of("before", wasHidden, "after", hide));
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
     * 下書きまたは募集停止中の物件を公開（OPEN）へ変更し、監査ログを記録する。
     * @param propertyId 物件ID。
     * @param adminUserId 操作する管理者の会員ID。
     * @param reason 操作理由。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void publishProperty(int propertyId, int adminUserId, String reason) throws SQLException {
        String lockSql = "SELECT listing_status, admin_hidden_at, deleted_at FROM properties WHERE property_id = ? FOR UPDATE";
        String updateSql = "UPDATE properties SET listing_status = 'OPEN', updated_at = CURRENT_TIMESTAMP WHERE property_id = ?";
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                if (!auditDao.isAdmin(con, adminUserId, true)) {
                    throw new ManagementProblem(403, "このアカウントには管理権限がありません。");
                }
                String beforeStatus;
                try (PreparedStatement stmt = con.prepareStatement(lockSql)) {
                    stmt.setInt(1, propertyId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new ManagementProblem(404, "指定された情報が見つかりません。");
                        }
                        if (rs.getObject("deleted_at") != null) {
                            throw new ManagementProblem(409, "削除済みの物件は更新できません。");
                        }
                        beforeStatus = rs.getString("listing_status");
                        boolean hidden = rs.getObject("admin_hidden_at") != null;
                        if (!Set.of("DRAFT", "PAUSED").contains(beforeStatus) || hidden) {
                            throw new ManagementProblem(409, "この物件は募集を公開できる状態ではありません。");
                        }
                    }
                }
                try (PreparedStatement stmt = con.prepareStatement(updateSql)) {
                    stmt.setInt(1, propertyId);
                    stmt.executeUpdate();
                }
                auditDao.insertLog(con, adminUserId, "PUBLISH", "PROPERTY",
                        Map.of("property_id", propertyId), reason,
                        Map.of("before", beforeStatus, "after", "OPEN"));
                con.commit();
                con.setAutoCommit(true);
            } catch (SQLException | RuntimeException | Error error) {
                rollbackQuietly(con, error);
                throw error;
            }
        }
    }

    /**
     * 物件の掲載内容を修正し、監査ログを記録する。
     * @param propertyId 物件ID。
     * @param adminUserId 操作する管理者の会員ID。
     * @param name 物件名。
     * @param prefecture 都道府県。
     * @param municipality 市区町村。
     * @param addressLine 番地。
     * @param latitude 緯度（null可）。
     * @param longitude 経度（null可）。
     * @param floorArea 面積（㎡）。
     * @param monthlyRent 月額賃料（円）。
     * @param description 説明文。
     * @param listingUrl 掲載URL。
     * @param permitStatus 許認可確認状態コード。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void updateProperty(int propertyId, int adminUserId, String name, String prefecture,
            String municipality, String addressLine, BigDecimal latitude, BigDecimal longitude,
            BigDecimal floorArea, int monthlyRent, String description, String listingUrl, String permitStatus)
            throws SQLException {
        String lockSql = "SELECT deleted_at FROM properties WHERE property_id = ? FOR UPDATE";
        String updateSql = """
                UPDATE properties
                SET property_name = ?, prefecture = ?, municipality = ?, address_line = ?,
                    latitude = ?, longitude = ?, floor_area_sqm = ?, monthly_rent_yen = ?,
                    description = ?, listing_url = ?, permit_status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE property_id = ?
                """;
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                if (!auditDao.isAdmin(con, adminUserId, true)) {
                    throw new ManagementProblem(403, "このアカウントには管理権限がありません。");
                }
                try (PreparedStatement stmt = con.prepareStatement(lockSql)) {
                    stmt.setInt(1, propertyId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new ManagementProblem(404, "指定された情報が見つかりません。");
                        }
                        if (rs.getObject("deleted_at") != null) {
                            throw new ManagementProblem(409, "削除済みの物件は更新できません。");
                        }
                    }
                }
                try (PreparedStatement stmt = con.prepareStatement(updateSql)) {
                    stmt.setString(1, name);
                    stmt.setString(2, prefecture);
                    stmt.setString(3, municipality);
                    stmt.setString(4, addressLine);
                    stmt.setBigDecimal(5, latitude);
                    stmt.setBigDecimal(6, longitude);
                    stmt.setBigDecimal(7, floorArea);
                    stmt.setInt(8, monthlyRent);
                    stmt.setString(9, description);
                    stmt.setString(10, listingUrl);
                    stmt.setString(11, permitStatus);
                    stmt.setInt(12, propertyId);
                    stmt.executeUpdate();
                }
                auditDao.insertLog(con, adminUserId, "UPDATE", "PROPERTY",
                        Map.of("property_id", propertyId), null, Map.of("section", "listing"));
                con.commit();
                con.setAutoCommit(true);
            } catch (SQLException | RuntimeException | Error error) {
                rollbackQuietly(con, error);
                throw error;
            }
        }
    }
}
