package jp.machiriku.dao.management;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.machiriku.bean.management.AdminPropertyItem;
import jp.machiriku.bean.management.AdminQualificationItem;
import jp.machiriku.bean.management.AdminReportItem;
import jp.machiriku.bean.management.AdminUserItem;
import jp.machiriku.bean.management.ManagementProblem;

/** 一般公開の掲載物件・必要資格案内、および会員が閲覧できる事業主情報を取得するDAO。 */
public class PublicDirectoryDao extends ManagementBaseDao {
    /** 一般公開できる物件の共通WHERE条件。 */
    private static final String VISIBLE_PROPERTY =
            "p.deleted_at IS NULL AND p.admin_hidden_at IS NULL AND p.listing_status IN ('OPEN', 'CONTRACTED')";

    /** 営業中店舗または閲覧者との取引履歴がある事業主に絞る共通WHERE条件。 */
    private static final String VISIBLE_BUSINESS = """
            u.account_status = 'ACTIVE' AND u.withdrawn_at IS NULL AND u.user_role = 'BUSINESS'
            AND (
                EXISTS (
                    SELECT 1 FROM applications a
                    JOIN tenancies t ON t.application_id = a.application_id
                    JOIN properties p ON p.property_id = a.property_id
                    WHERE a.business_user_id = b.user_id AND t.tenancy_status = 'OPEN' AND
            """ + VISIBLE_PROPERTY + """
                )
                OR EXISTS (
                    SELECT 1 FROM applications a
                    JOIN properties p ON p.property_id = a.property_id
                    WHERE a.business_user_id = b.user_id AND p.owner_user_id = ?
                )
                OR EXISTS (
                    SELECT 1 FROM offers o
                    WHERE o.business_user_id = b.user_id AND o.owner_user_id = ? AND o.offer_status <> 'DRAFT'
                )
            )
            """;

    /** 本番接続を使用するDAOを生成する。 */
    public PublicDirectoryDao() {
        super();
    }

    /**
     * テスト用の共有接続を使用するDAOを生成する。
     * @param sharedConnection テスト用接続。
     */
    public PublicDirectoryDao(Connection sharedConnection) {
        super(sharedConnection);
    }

    /**
     * 公開中の物件を名称・所在地で検索する。
     * @param keyword 検索語。空文字の場合は全件対象。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 公開物件の一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminPropertyItem> findVisibleProperties(String keyword, int limit, int offset) throws SQLException {
        String sql = """
                SELECT p.property_id, p.property_name,
                       concat_ws('', p.prefecture, p.municipality, p.address_line) AS address,
                       p.floor_area_sqm, p.monthly_rent_yen
                FROM properties p
                WHERE
                """ + VISIBLE_PROPERTY + """
                  AND strpos(lower(concat_ws(' ', p.property_name, p.prefecture, p.municipality, p.address_line)), lower(?)) > 0
                ORDER BY p.updated_at DESC, p.property_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminPropertyItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, keyword);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminPropertyItem item = new AdminPropertyItem();
                    item.setPropertyId(rs.getInt("property_id"));
                    item.setPropertyName(rs.getString("property_name"));
                    item.setFullAddress(rs.getString("address"));
                    item.setFloorAreaSqm(rs.getBigDecimal("floor_area_sqm"));
                    item.setMonthlyRentYen(rs.getInt("monthly_rent_yen"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 公開条件を満たす物件の詳細を1件取得する。
     * @param propertyId 物件ID。
     * @return 公開物件詳細。非公開または存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminPropertyItem findVisiblePropertyById(int propertyId) throws SQLException {
        try (Connection con = openConnection()) {
            return findVisiblePropertyById(con, propertyId);
        }
    }

    /**
     * 既存のDB接続上で公開物件詳細を1件取得する。
     * @param con 共有するDB接続。
     * @param propertyId 物件ID。
     * @return 公開物件詳細。非公開または存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminPropertyItem findVisiblePropertyById(Connection con, int propertyId) throws SQLException {
        String sql = """
                SELECT p.property_id, p.property_name, p.owner_user_id,
                       COALESCE(NULLIF(o.company_name, ''), NULLIF(o.representative_name, ''), u.display_name) AS owner_name,
                       concat_ws('', p.prefecture, p.municipality, p.address_line) AS address,
                       p.floor_area_sqm, p.monthly_rent_yen, p.description
                FROM properties p
                JOIN users u ON u.user_id = p.owner_user_id
                LEFT JOIN owner_profiles o ON o.user_id = u.user_id
                WHERE p.property_id = ? AND
                """ + VISIBLE_PROPERTY;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
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
                item.setFullAddress(rs.getString("address"));
                item.setFloorAreaSqm(rs.getBigDecimal("floor_area_sqm"));
                item.setMonthlyRentYen(rs.getInt("monthly_rent_yen"));
                item.setDescription(rs.getString("description"));
                return item;
            }
        }
    }

    /**
     * 閲覧者が参照できる事業主の一覧を検索する。
     * @param viewerId 閲覧者の会員ID。
     * @param keyword 事業者名の検索語。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 閲覧可能な事業主一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.Profile> findVisibleBusinesses(int viewerId, String keyword, int limit, int offset)
            throws SQLException {
        String sql = """
                SELECT b.user_id, b.business_name, b.preferred_area
                FROM business_profiles b
                JOIN users u ON u.user_id = b.user_id
                WHERE
                """ + VISIBLE_BUSINESS + """
                  AND strpos(lower(b.business_name), lower(?)) > 0
                ORDER BY b.business_name, b.user_id
                LIMIT ? OFFSET ?
                """;
        List<AdminUserItem.Profile> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, viewerId);
            stmt.setInt(2, viewerId);
            stmt.setString(3, keyword);
            stmt.setInt(4, limit);
            stmt.setInt(5, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.Profile item = new AdminUserItem.Profile();
                    item.setUserId(rs.getInt("user_id"));
                    item.setBusinessName(rs.getString("business_name"));
                    item.setPreferredArea(rs.getString("preferred_area"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 閲覧者が参照できる事業主の詳細を1件取得する。
     * @param businessId 事業主の会員ID。
     * @param viewerId 閲覧者の会員ID。
     * @return 事業主詳細。閲覧対象外または存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminUserItem.Profile findVisibleBusinessById(int businessId, int viewerId) throws SQLException {
        try (Connection con = openConnection()) {
            return findVisibleBusinessById(con, businessId, viewerId);
        }
    }

    /**
     * 既存のDB接続上で閲覧可能な事業主詳細を1件取得する。
     * @param con 共有するDB接続。
     * @param businessId 事業主の会員ID。
     * @param viewerId 閲覧者の会員ID。
     * @return 事業主詳細。閲覧対象外または存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminUserItem.Profile findVisibleBusinessById(Connection con, int businessId, int viewerId)
            throws SQLException {
        String sql = """
                SELECT b.user_id, b.business_name, b.preferred_area, b.industry_experience
                FROM business_profiles b
                JOIN users u ON u.user_id = b.user_id
                WHERE b.user_id = ? AND
                """ + VISIBLE_BUSINESS;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, businessId);
            stmt.setInt(2, viewerId);
            stmt.setInt(3, viewerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminUserItem.Profile item = new AdminUserItem.Profile();
                item.setUserId(rs.getInt("user_id"));
                item.setBusinessName(rs.getString("business_name"));
                item.setPreferredArea(rs.getString("preferred_area"));
                item.setIndustryExperience(rs.getString("industry_experience"));
                return item;
            }
        }
    }

    /**
     * 通報対象が閲覧可能かつ通報可能な対象であることを検証し、画面表示用の対象情報を返す。
     * @param viewerId 通報しようとしている会員ID。
     * @param kind 通報対象区分（PROPERTY / OWNER / BUSINESS）。
     * @param targetId 通報対象の物件IDまたは会員ID。
     * @param propertyId オーナー通報時の関連物件ID。
     * @return 検証済みの通報対象情報。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminReportItem.Target findReportTarget(int viewerId, String kind, int targetId, Integer propertyId)
            throws SQLException {
        try (Connection con = openConnection()) {
            return findReportTarget(con, viewerId, kind, targetId, propertyId);
        }
    }

    /**
     * 既存のDB接続上で通報対象の妥当性を検証し、対象情報を返す。
     * @param con 共有するDB接続。
     * @param viewerId 通報する会員ID。
     * @param kind 通報対象区分（PROPERTY / OWNER / BUSINESS）。
     * @param targetId 通報対象の物件IDまたは会員ID。
     * @param propertyId オーナー通報時の関連物件ID。
     * @return 検証済みの通報対象情報。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminReportItem.Target findReportTarget(Connection con, int viewerId, String kind,
            int targetId, Integer propertyId) throws SQLException {
        if ("PROPERTY".equals(kind)) {
            AdminPropertyItem prop = findVisiblePropertyById(con, targetId);
            if (prop == null) {
                throw new ManagementProblem(404, "指定された情報が見つかりません。");
            }
            if (prop.getOwnerUserId() == viewerId) {
                throw new ManagementProblem(403, "自身の掲載物件には通報できません。");
            }
            AdminReportItem.Target info = new AdminReportItem.Target();
            info.setKind(kind);
            info.setTargetId(targetId);
            info.setLabel("物件名");
            info.setName(prop.getPropertyName());
            return info;
        }
        // 会員対象（事業主・オーナー）の場合、自分自身の通報を禁止する。
        if (viewerId == targetId) {
            throw new ManagementProblem(403, "自身のアカウントには通報できません。");
        }
        if ("BUSINESS".equals(kind)) {
            AdminUserItem.Profile biz = findVisibleBusinessById(con, targetId, viewerId);
            if (biz == null) {
                throw new ManagementProblem(404, "指定された情報が見つかりません。");
            }
            AdminReportItem.Target info = new AdminReportItem.Target();
            info.setKind(kind);
            info.setTargetId(targetId);
            info.setLabel("事業者名");
            info.setName(biz.getBusinessName());
            return info;
        }
        // オーナーを通報する場合は、公開中の関連物件の掲載者であることを確認する。
        if (propertyId == null) {
            throw new ManagementProblem(400, "通報に関連する物件を選んでください。");
        }
        String ownerSql = """
                SELECT COALESCE(NULLIF(o.company_name, ''), o.representative_name) AS owner_name,
                       p.property_name
                FROM properties p
                JOIN owner_profiles o ON o.user_id = p.owner_user_id
                JOIN users u ON u.user_id = o.user_id
                WHERE p.property_id = ? AND p.owner_user_id = ? AND u.user_role = 'OWNER' AND
                """ + VISIBLE_PROPERTY;
        try (PreparedStatement stmt = con.prepareStatement(ownerSql)) {
            stmt.setInt(1, propertyId);
            stmt.setInt(2, targetId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定された情報が見つかりません。");
                }
                AdminReportItem.Target info = new AdminReportItem.Target();
                info.setKind(kind);
                info.setTargetId(targetId);
                info.setRelatedPropertyId(propertyId);
                info.setLabel("掲載者");
                info.setName(rs.getString("owner_name"));
                info.setRelatedPropertyName(rs.getString("property_name"));
                return info;
            }
        }
    }

    /**
     * 公開案内画面の絞り込みに使う有効な業種の選択肢を取得する。
     * @return 業種ID文字列をキー、「カテゴリ / 業種名」を値とする選択肢マップ。
     * @throws SQLException DB照会に失敗した場合。
     */
    public Map<String, String> findIndustryChoices() throws SQLException {
        String sql = """
                SELECT i.industry_type_id, concat(c.category_name, ' / ', i.industry_name) AS label
                FROM industry_types i
                JOIN industry_categories c USING (category_id)
                WHERE i.is_active AND c.is_active
                ORDER BY c.display_order, c.category_id, i.display_order, i.industry_type_id
                """;
        Map<String, String> choices = new LinkedHashMap<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                choices.put(String.valueOf(rs.getInt("industry_type_id")), rs.getString("label"));
            }
        }
        return choices;
    }

    /**
     * 公開中の業種別必要資格・書類一覧を取得する。
     * @param industryId 絞り込む業種ID。全業種の場合はnull。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 公開要件の一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminQualificationItem> findPublicRequirements(Integer industryId, int limit, int offset)
            throws SQLException {
        String sql = """
                SELECT i.industry_name, q.qualification_name, q.requirement_kind, r.is_required,
                       r.condition_text, q.guidance_text, q.guidance_url
                FROM industry_qualification_requirements r
                JOIN qualifications q USING (qualification_id)
                JOIN industry_types i USING (industry_type_id)
                JOIN industry_categories c USING (category_id)
                WHERE i.is_active AND c.is_active AND q.is_active
                  AND (?::integer IS NULL OR i.industry_type_id = ?)
                ORDER BY c.display_order, c.category_id, i.display_order, i.industry_type_id,
                         r.is_required DESC, q.qualification_id
                LIMIT ? OFFSET ?
                """;
        List<AdminQualificationItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setObject(1, industryId);
            stmt.setObject(2, industryId);
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminQualificationItem item = new AdminQualificationItem();
                    item.setIndustryName(rs.getString("industry_name"));
                    item.setQualificationName(rs.getString("qualification_name"));
                    item.setRequirementKind(rs.getString("requirement_kind"));
                    item.setRequired(rs.getBoolean("is_required"));
                    item.setConditionText(rs.getString("condition_text"));
                    item.setGuidanceText(rs.getString("guidance_text"));
                    item.setGuidanceUrl(rs.getString("guidance_url"));
                    list.add(item);
                }
            }
        }
        return list;
    }
}
