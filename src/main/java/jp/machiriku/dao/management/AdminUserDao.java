package jp.machiriku.dao.management;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.machiriku.bean.LocationInput;
import jp.machiriku.bean.management.AdminAuditLogItem;
import jp.machiriku.bean.management.AdminUserItem;
import jp.machiriku.bean.management.ManagementProblem;

/**
 * 管理者向けの会員検索・プロフィール・生活拠点・保有資格・活動履歴の照会と、利用停止/解除・情報修正を行うDAO。
 */
public class AdminUserDao extends ManagementBaseDao {
    /** 管理者権限チェックと監査ログ記録に使うDAO。 */
    private final AdminAuditDao auditDao;

    /** 本番接続を使用するDAOを生成する。 */
    public AdminUserDao() {
        super();
        this.auditDao = new AdminAuditDao();
    }

    /**
     * テスト用の共有接続を使用するDAOを生成する。
     * @param sharedConnection テスト用接続。
     */
    public AdminUserDao(Connection sharedConnection) {
        super(sharedConnection);
        this.auditDao = new AdminAuditDao(sharedConnection);
    }

    /**
     * 利用区分・状態・登録日範囲・検索語で会員一覧を検索する。
     * @param role 利用区分（RESIDENT / BUSINESS / OWNER）。
     * @param status 利用状態コード。空文字で全件。
     * @param from 登録日（開始）。未指定時はnull。
     * @param to 登録日（終了）。未指定時はnull。
     * @param keyword 検索語。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 会員一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem> findUsers(String role, String status, LocalDate from, LocalDate to,
            String keyword, int limit, int offset) throws SQLException {
        String sql = """
                SELECT u.user_id, u.display_name, COALESCE(b.business_name, o.company_name) AS org_name,
                       u.email, u.user_role, u.account_status, u.created_at
                FROM users u
                LEFT JOIN business_profiles b ON b.user_id = u.user_id
                LEFT JOIN owner_profiles o ON o.user_id = u.user_id
                WHERE u.user_role = ?
                  AND u.account_status = COALESCE(NULLIF(?, ''), u.account_status)
                  AND (u.created_at AT TIME ZONE 'Asia/Tokyo')::date >= COALESCE(?::date, '-infinity'::date)
                  AND (u.created_at AT TIME ZONE 'Asia/Tokyo')::date <= COALESCE(?::date, 'infinity'::date)
                  AND strpos(lower(concat_ws(' ', u.user_id, u.display_name, u.email,
                      b.business_name, o.company_name, b.representative_name, o.representative_name)), lower(?)) > 0
                ORDER BY u.created_at DESC, u.user_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminUserItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, role);
            stmt.setString(2, status);
            stmt.setObject(3, from, Types.DATE);
            stmt.setObject(4, to, Types.DATE);
            stmt.setString(5, keyword);
            stmt.setInt(6, limit);
            stmt.setInt(7, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem item = new AdminUserItem();
                    item.setUserId(rs.getInt("user_id"));
                    item.setDisplayName(rs.getString("display_name"));
                    item.setOrganizationName(rs.getString("org_name"));
                    item.setEmail(rs.getString("email"));
                    item.setUserRole(rs.getString("user_role"));
                    item.setAccountStatus(rs.getString("account_status"));
                    item.setCreatedAt(getOffsetDateTime(rs, "created_at"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 指定会員IDの基本情報を取得する。
     * @param userId 会員ID。
     * @return 会員基本情報。存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminUserItem findUserById(int userId) throws SQLException {
        String sql = """
                SELECT user_id, display_name, email, user_role, account_status, created_at, updated_at, withdrawn_at
                FROM users
                WHERE user_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminUserItem item = new AdminUserItem();
                item.setUserId(rs.getInt("user_id"));
                item.setDisplayName(rs.getString("display_name"));
                item.setEmail(rs.getString("email"));
                item.setUserRole(rs.getString("user_role"));
                item.setAccountStatus(rs.getString("account_status"));
                item.setCreatedAt(getOffsetDateTime(rs, "created_at"));
                item.setUpdatedAt(getOffsetDateTime(rs, "updated_at"));
                item.setWithdrawnAt(getOffsetDateTime(rs, "withdrawn_at"));
                return item;
            }
        }
    }

    /**
     * 利用区分に対応する詳細プロフィール1件を取得する。
     * @param role 利用区分（RESIDENT / OWNER / BUSINESS）。
     * @param userId 会員ID。
     * @return 詳細プロフィール。本登録前などで未登録の場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminUserItem.Profile findRoleProfile(String role, int userId) throws SQLException {
        if ("RESIDENT".equals(role)) {
            return findResidentProfile(userId);
        }
        if ("OWNER".equals(role)) {
            return findOwnerProfile(userId);
        }
        return findBusinessProfile(userId);
    }

    /**
     * 地域住民のプロフィールを取得する。
     * @param userId 会員ID。
     * @return 地域住民プロフィール。未登録時はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminUserItem.Profile findResidentProfile(int userId) throws SQLException {
        String sql = "SELECT age_group, gender FROM resident_profiles WHERE user_id = ?";
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminUserItem.Profile profile = new AdminUserItem.Profile();
                profile.setAgeGroup(rs.getString("age_group"));
                profile.setGender(rs.getString("gender"));
                return profile;
            }
        }
    }

    /**
     * 物件オーナーのプロフィールを取得する。
     * @param userId 会員ID。
     * @return 物件オーナープロフィール。未登録時はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminUserItem.Profile findOwnerProfile(int userId) throws SQLException {
        String sql = """
                SELECT representative_name, company_name, phone_e164, secondary_phone_e164, icon_image_url
                FROM owner_profiles
                WHERE user_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminUserItem.Profile profile = new AdminUserItem.Profile();
                profile.setRepresentativeName(rs.getString("representative_name"));
                profile.setCompanyName(rs.getString("company_name"));
                profile.setPhoneE164(rs.getString("phone_e164"));
                profile.setSecondaryPhoneE164(rs.getString("secondary_phone_e164"));
                profile.setIconImageUrl(rs.getString("icon_image_url"));
                return profile;
            }
        }
    }

    /**
     * 事業主のプロフィールを取得する。
     * @param userId 会員ID。
     * @return 事業主プロフィール。未登録時はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminUserItem.Profile findBusinessProfile(int userId) throws SQLException {
        String sql = """
                SELECT business_name, representative_name, phone_e164, secondary_phone_e164,
                       preferred_area, employee_count, industry_experience, management_experience, icon_image_url
                FROM business_profiles
                WHERE user_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminUserItem.Profile profile = new AdminUserItem.Profile();
                profile.setBusinessName(rs.getString("business_name"));
                profile.setRepresentativeName(rs.getString("representative_name"));
                profile.setPhoneE164(rs.getString("phone_e164"));
                profile.setSecondaryPhoneE164(rs.getString("secondary_phone_e164"));
                profile.setPreferredArea(rs.getString("preferred_area"));
                profile.setEmployeeCount(getNullableInt(rs, "employee_count"));
                profile.setIndustryExperience(rs.getString("industry_experience"));
                profile.setManagementExperience(rs.getString("management_experience"));
                profile.setIconImageUrl(rs.getString("icon_image_url"));
                return profile;
            }
        }
    }

    /**
     * 指定会員が有効な管理権限レコードを持つか確認する。
     * @param userId 会員ID。
     * @return 管理権限レコードが有効な場合はtrue。
     * @throws SQLException DB照会に失敗した場合。
     */
    public boolean hasActiveAdminGrant(int userId) throws SQLException {
        try (Connection con = openConnection()) {
            return auditDao.hasActiveAdminGrant(con, userId);
        }
    }

    /**
     * 指定会員に対する管理者の最新操作履歴（最大10件）を取得する。
     * @param userId 会員ID。
     * @return 操作履歴一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminAuditLogItem> findRecentAccountActions(int userId) throws SQLException {
        String sql = """
                SELECT u.display_name, l.action, l.reason, l.occurred_at
                FROM admin_audit_logs l
                JOIN users u ON u.user_id = l.actor_user_id
                WHERE l.target_type = 'USER'
                  AND l.target_key = jsonb_build_object('user_id', ?::integer)
                ORDER BY l.occurred_at DESC, l.audit_log_id DESC
                LIMIT 10
                """;
        List<AdminAuditLogItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminAuditLogItem log = new AdminAuditLogItem();
                    log.setActorName(rs.getString("display_name"));
                    log.setAction(rs.getString("action"));
                    log.setReason(rs.getString("reason"));
                    log.setOccurredAt(getOffsetDateTime(rs, "occurred_at"));
                    list.add(log);
                }
            }
        }
        return list;
    }

    /**
     * 会員の希望業種一覧を取得する。
     * @param userId 会員ID。
     * @return 希望業種一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.Preference> findPreferences(int userId) throws SQLException {
        String sql = """
                SELECT i.industry_type_id, c.category_name, i.industry_name,
                       CASE WHEN i.is_active AND c.is_active THEN '有効' ELSE '無効' END AS master_state
                FROM user_industry_preferences pr
                JOIN industry_types i ON pr.industry_type_id = i.industry_type_id
                JOIN industry_categories c ON i.category_id = c.category_id
                WHERE pr.user_id = ?
                ORDER BY c.display_order, c.category_id, i.display_order, i.industry_type_id
                """;
        List<AdminUserItem.Preference> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.Preference item = new AdminUserItem.Preference();
                    item.setIndustryTypeId(rs.getInt("industry_type_id"));
                    item.setCategoryName(rs.getString("category_name"));
                    item.setIndustryName(rs.getString("industry_name"));
                    item.setMasterStateLabel(rs.getString("master_state"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 希望業種のタブ・ピル選択UIに表示するカテゴリ別業種一覧（有効業種＋選択済み業種）を取得する。
     * @param userId 会員ID。
     * @return カテゴリごとに業種IDと表示名をまとめた階層一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<Map<String, Object>> findPreferenceTaxonomy(int userId) throws SQLException {
        // 会員登録画面と同じカテゴリタブ表示のため、表示順に従ってカテゴリと業種を取得する。
        String sql = """
                SELECT c.category_id, c.category_name, c.is_active AS category_active,
                       i.industry_type_id, i.industry_name, i.is_active AS industry_active
                FROM industry_types i
                JOIN industry_categories c ON c.category_id = i.category_id
                WHERE (i.is_active AND c.is_active)
                   OR EXISTS (
                       SELECT 1 FROM user_industry_preferences pr
                       WHERE pr.user_id = ? AND pr.industry_type_id = i.industry_type_id
                   )
                ORDER BY c.display_order, c.category_id, i.display_order, i.industry_type_id
                """;
        // カテゴリID順にグループ化するためのマップ。
        Map<Integer, Map<String, Object>> categories = new LinkedHashMap<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                // 取得した業種を所属カテゴリごとのsubcategories配列へまとめる。
                while (rs.next()) {
                    int categoryId = rs.getInt("category_id");
                    String categoryName = rs.getString("category_name");
                    boolean categoryActive = rs.getBoolean("category_active");
                    boolean industryActive = rs.getBoolean("industry_active");
                    Map<String, Object> category = categories.computeIfAbsent(categoryId, id -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", String.valueOf(id));
                        map.put("name", categoryActive ? categoryName : categoryName + "（無効）");
                        map.put("subcategories", new ArrayList<Map<String, String>>());
                        return map;
                    });
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> subcategories = (List<Map<String, String>>) category.get("subcategories");
                    // 無効なマスターが選択済みとして残っている場合は判別できるよう印を付ける。
                    String industryLabel = (industryActive && categoryActive)
                            ? rs.getString("industry_name")
                            : rs.getString("industry_name") + "（無効）";
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("id", String.valueOf(rs.getInt("industry_type_id")));
                    item.put("name", industryLabel);
                    subcategories.add(item);
                }
            }
        }
        return List.copyOf(categories.values());
    }

    /**
     * 地域住民の生活拠点一覧を取得する。
     * @param userId 会員ID。
     * @return 生活拠点一覧（居住地→勤務先・学校の順）。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.Location> findLocations(int userId) throws SQLException {
        String sql = """
                SELECT location_kind, prefecture, municipality, address_line, latitude, longitude, updated_at
                FROM user_locations
                WHERE user_id = ?
                ORDER BY CASE location_kind WHEN 'HOME' THEN 1 ELSE 2 END, location_kind
                """;
        List<AdminUserItem.Location> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.Location item = new AdminUserItem.Location();
                    item.setLocationKind(rs.getString("location_kind"));
                    item.setPrefecture(rs.getString("prefecture"));
                    item.setMunicipality(rs.getString("municipality"));
                    item.setAddressLine(rs.getString("address_line"));
                    item.setLatitude(rs.getBigDecimal("latitude"));
                    item.setLongitude(rs.getBigDecimal("longitude"));
                    item.setUpdatedAt(getOffsetDateTime(rs, "updated_at"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 事業主の保有資格一覧を取得する。
     * @param userId 事業主の会員ID。
     * @return 保有資格一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.Qualification> findQualifications(int userId) throws SQLException {
        String sql = """
                SELECT bq.qualification_id, q.qualification_name, q.requirement_kind,
                       bq.credential_number, bq.obtained_on, bq.expires_on, bq.is_active
                FROM business_qualifications bq
                JOIN qualifications q USING (qualification_id)
                WHERE bq.business_user_id = ?
                ORDER BY bq.is_active DESC, q.qualification_name, q.qualification_id
                """;
        List<AdminUserItem.Qualification> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.Qualification item = new AdminUserItem.Qualification();
                    item.setQualificationId(rs.getInt("qualification_id"));
                    item.setQualificationName(rs.getString("qualification_name"));
                    item.setRequirementKind(rs.getString("requirement_kind"));
                    item.setCredentialNumber(rs.getString("credential_number"));
                    item.setObtainedOn(getLocalDate(rs, "obtained_on"));
                    item.setExpiresOn(getLocalDate(rs, "expires_on"));
                    item.setActive(rs.getBoolean("is_active"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 事業主へ新規追加できる有効な資格・許認可（未登録分のみ）の選択肢を取得する。
     * @param userId 事業主の会員ID。
     * @return 資格ID文字列をキー、名称を値とする選択肢マップ。
     * @throws SQLException DB照会に失敗した場合。
     */
    public Map<String, String> findAddableQualificationChoices(int userId) throws SQLException {
        // 既存保有資格を新規追加欄から誤って上書きしないよう、未登録の有効項目だけを返す。
        String sql = """
                SELECT q.qualification_id, q.qualification_name
                FROM qualifications q
                WHERE q.is_active
                  AND q.requirement_kind IN ('QUALIFICATION', 'PERMIT')
                  AND NOT EXISTS (
                      SELECT 1 FROM business_qualifications bq
                      WHERE bq.business_user_id = ? AND bq.qualification_id = q.qualification_id
                  )
                ORDER BY q.qualification_name, q.qualification_id
                """;
        Map<String, String> choices = new LinkedHashMap<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    choices.put(String.valueOf(rs.getInt("qualification_id")), rs.getString("qualification_name"));
                }
            }
        }
        return choices;
    }

    /**
     * 地域住民の投票履歴を取得する。
     * @param userId 会員ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 投票履歴一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.History> findVoteHistory(int userId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT v.vote_id, v.property_id, p.property_name, v.created_at, v.updated_at, v.withdrawn_at,
                       (SELECT string_agg(c.preference_rank || '位 ' || i.industry_name, '、' ORDER BY c.preference_rank)
                        FROM vote_choices c JOIN industry_types i USING (industry_type_id)
                        WHERE c.vote_id = v.vote_id) AS voted_industries
                FROM property_votes v
                JOIN properties p USING (property_id)
                WHERE v.resident_user_id = ?
                ORDER BY v.updated_at DESC, v.vote_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminUserItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.History item = new AdminUserItem.History();
                    item.setRecordId(rs.getLong("vote_id"));
                    item.setPropertyId(rs.getInt("property_id"));
                    item.setTitle(rs.getString("property_name"));
                    item.setPrimaryTime(getOffsetDateTime(rs, "created_at"));
                    item.setSecondaryTime(getOffsetDateTime(rs, "updated_at"));
                    item.setTertiaryTime(getOffsetDateTime(rs, "withdrawn_at"));
                    item.setDetailText(rs.getString("voted_industries"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 地域住民のアンケート回答履歴を取得する。
     * @param userId 会員ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 回答履歴一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.History> findAnswerHistory(int userId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT r.response_id, r.survey_id, s.survey_title, t.store_name, r.rating, r.responded_at,
                       (SELECT string_agg(concat(q.question_text, '　', COALESCE(a.text_answer, o.option_text)),
                                          E'\n' ORDER BY q.question_no)
                        FROM survey_answers a
                        JOIN survey_questions q USING (question_id)
                        LEFT JOIN survey_options o USING (option_id)
                        WHERE a.response_id = r.response_id) AS answers_summary
                FROM survey_responses r
                JOIN surveys s USING (survey_id)
                LEFT JOIN tenancies t USING (tenancy_id)
                WHERE r.resident_user_id = ?
                ORDER BY r.responded_at DESC, r.response_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminUserItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.History item = new AdminUserItem.History();
                    item.setRecordId(rs.getLong("response_id"));
                    item.setRelatedId(rs.getInt("survey_id"));
                    item.setTitle(rs.getString("survey_title"));
                    item.setSubtitle(rs.getString("store_name"));
                    item.setRating(rs.getInt("rating"));
                    item.setPrimaryTime(getOffsetDateTime(rs, "responded_at"));
                    item.setDetailText(rs.getString("answers_summary"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 地域住民の保持クーポンまたはクーポン使用履歴を取得する。
     * @param userId 会員ID。
     * @param usedOnly 使用済み履歴を取得する場合はtrue、保持中（未使用）を取得する場合はfalse。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return クーポン履歴一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.History> findResidentCouponHistory(int userId, boolean usedOnly, int limit, int offset)
            throws SQLException {
        String sql = """
                SELECT c.coupon_id, c.coupon_title, t.store_name, cr.issued_at,
                       c.valid_until, cr.used_at, cr.revoked_at,
                       CASE
                           WHEN cr.used_at IS NOT NULL THEN '使用済み'
                           WHEN cr.revoked_at IS NOT NULL THEN '無効'
                           WHEN c.valid_until <= CURRENT_TIMESTAMP THEN '期限切れ'
                           ELSE '未使用'
                       END AS state_label
                FROM coupon_recipients cr
                JOIN coupons c USING (coupon_id)
                JOIN tenancies t USING (tenancy_id)
                WHERE cr.resident_user_id = ?
                  AND
                """ + (usedOnly
                        ? " cr.used_at IS NOT NULL ORDER BY cr.used_at DESC, c.coupon_id DESC"
                        : " cr.used_at IS NULL ORDER BY cr.issued_at DESC, c.coupon_id DESC")
                + " LIMIT ? OFFSET ?";
        List<AdminUserItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.History item = new AdminUserItem.History();
                    item.setRelatedId(rs.getInt("coupon_id"));
                    item.setTitle(rs.getString("coupon_title"));
                    item.setSubtitle(rs.getString("store_name"));
                    item.setPrimaryTime(getOffsetDateTime(rs, "issued_at"));
                    item.setValidUntil(getOffsetDateTime(rs, "valid_until"));
                    item.setSecondaryTime(getOffsetDateTime(rs, "used_at"));
                    item.setTertiaryTime(getOffsetDateTime(rs, "revoked_at"));
                    item.setStatusText(rs.getString("state_label"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 事業主の出店申し込み履歴を取得する。
     * @param userId 事業主の会員ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 申込履歴一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.History> findApplicationHistory(int userId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT a.application_id, a.property_id, p.property_name, a.application_status, a.created_at
                FROM applications a
                JOIN properties p USING (property_id)
                WHERE a.business_user_id = ?
                ORDER BY a.created_at DESC, a.application_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminUserItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.History item = new AdminUserItem.History();
                    item.setApplicationId(rs.getInt("application_id"));
                    item.setPropertyId(rs.getInt("property_id"));
                    item.setTitle(rs.getString("property_name"));
                    item.setStatusText(rs.getString("application_status"));
                    item.setPrimaryTime(getOffsetDateTime(rs, "created_at"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 事業主の契約物件履歴を取得する。
     * @param userId 事業主の会員ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 契約一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.History> findTenancyHistory(int userId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT t.tenancy_id, a.application_id, a.property_id, p.property_name, t.store_name,
                       t.contract_start_on, t.contract_end_on, t.tenancy_status
                FROM tenancies t
                JOIN applications a USING (application_id)
                JOIN properties p USING (property_id)
                WHERE a.business_user_id = ?
                ORDER BY t.created_at DESC, t.tenancy_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminUserItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.History item = new AdminUserItem.History();
                    item.setTenancyId(rs.getInt("tenancy_id"));
                    item.setApplicationId(rs.getInt("application_id"));
                    item.setPropertyId(rs.getInt("property_id"));
                    item.setTitle(rs.getString("property_name"));
                    item.setSubtitle(rs.getString("store_name"));
                    item.setStartDate(getLocalDate(rs, "contract_start_on"));
                    item.setEndDate(getLocalDate(rs, "contract_end_on"));
                    item.setStatusText(rs.getString("tenancy_status"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 事業主の出店計画履歴を取得する。
     * @param userId 事業主の会員ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 出店計画一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.History> findOpeningPlanHistory(int userId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT op.plan_id, op.tenancy_id, a.property_id, op.title,
                       op.planned_open_on, op.progress_status, op.publication_status
                FROM opening_plans op
                JOIN tenancies t USING (tenancy_id)
                JOIN applications a USING (application_id)
                WHERE a.business_user_id = ?
                ORDER BY op.updated_at DESC, op.plan_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminUserItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.History item = new AdminUserItem.History();
                    item.setRecordId(rs.getLong("plan_id"));
                    item.setTenancyId(rs.getInt("tenancy_id"));
                    item.setPropertyId(rs.getInt("property_id"));
                    item.setTitle(rs.getString("title"));
                    item.setStartDate(getLocalDate(rs, "planned_open_on"));
                    item.setStatusText(rs.getString("progress_status"));
                    item.setSecondaryStatusText(rs.getString("publication_status"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 事業主または物件オーナーのオファー送受信履歴を取得する。
     * @param userId 会員ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return オファー履歴一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.History> findOfferHistory(int userId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT o.offer_id, o.property_id, p.property_name,
                       CASE WHEN o.owner_user_id = viewer.id THEN o.business_user_id ELSE o.owner_user_id END AS partner_id,
                       CASE WHEN o.owner_user_id = viewer.id THEN business.display_name ELSE owner.display_name END AS partner_name,
                       CASE WHEN o.owner_user_id = viewer.id THEN '送信' ELSE '受信' END AS direction,
                       o.offer_status, o.sent_at
                FROM offers o
                JOIN properties p USING (property_id)
                JOIN users business ON business.user_id = o.business_user_id
                JOIN users owner ON owner.user_id = o.owner_user_id
                CROSS JOIN (SELECT ?::integer AS id) viewer
                WHERE o.owner_user_id = viewer.id OR o.business_user_id = viewer.id
                ORDER BY o.sent_at DESC, o.offer_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminUserItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.History item = new AdminUserItem.History();
                    item.setRecordId(rs.getLong("offer_id"));
                    item.setPropertyId(rs.getInt("property_id"));
                    item.setTitle(rs.getString("property_name"));
                    item.setPartnerUserId(rs.getInt("partner_id"));
                    item.setSubtitle(rs.getString("partner_name"));
                    item.setDirection(rs.getString("direction"));
                    item.setStatusText(rs.getString("offer_status"));
                    item.setPrimaryTime(getOffsetDateTime(rs, "sent_at"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 事業主が作成した来店アンケート一覧を取得する。
     * @param userId 事業主の会員ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 作成アンケート一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.History> findSurveyHistory(int userId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT s.survey_id, s.survey_title, t.store_name, s.survey_status,
                       (SELECT count(*) FROM survey_responses r WHERE r.survey_id = s.survey_id) AS response_count,
                       s.updated_at
                FROM surveys s
                JOIN tenancies t USING (tenancy_id)
                JOIN applications a USING (application_id)
                WHERE a.business_user_id = ?
                ORDER BY s.updated_at DESC, s.survey_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminUserItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.History item = new AdminUserItem.History();
                    item.setRelatedId(rs.getInt("survey_id"));
                    item.setTitle(rs.getString("survey_title"));
                    item.setSubtitle(rs.getString("store_name"));
                    item.setStatusText(rs.getString("survey_status"));
                    item.setPrimaryCount(rs.getLong("response_count"));
                    item.setSecondaryTime(getOffsetDateTime(rs, "updated_at"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 事業主が作成したクーポン一覧を取得する。
     * @param userId 事業主の会員ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 作成クーポン一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.History> findCreatedCouponHistory(int userId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT c.coupon_id, c.coupon_title, t.store_name, c.coupon_status, c.valid_until,
                       (SELECT count(*) FROM coupon_recipients cr WHERE cr.coupon_id = c.coupon_id) AS issued_count,
                       (SELECT count(*) FROM coupon_recipients cr WHERE cr.coupon_id = c.coupon_id AND cr.used_at IS NOT NULL) AS used_count
                FROM coupons c
                JOIN tenancies t USING (tenancy_id)
                JOIN applications a USING (application_id)
                WHERE a.business_user_id = ?
                ORDER BY c.updated_at DESC, c.coupon_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminUserItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.History item = new AdminUserItem.History();
                    item.setRelatedId(rs.getInt("coupon_id"));
                    item.setTitle(rs.getString("coupon_title"));
                    item.setSubtitle(rs.getString("store_name"));
                    item.setStatusText(rs.getString("coupon_status"));
                    item.setValidUntil(getOffsetDateTime(rs, "valid_until"));
                    item.setPrimaryCount(rs.getLong("issued_count"));
                    item.setSecondaryCount(rs.getLong("used_count"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 物件オーナーの保有物件一覧を取得する。
     * @param userId オーナーの会員ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 保有物件一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.History> findOwnedPropertyHistory(int userId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT p.property_id, p.property_name, p.listing_status, p.admin_hidden_at,
                       (SELECT count(*) FROM applications a WHERE a.property_id = p.property_id) AS application_count
                FROM properties p
                WHERE p.owner_user_id = ?
                ORDER BY p.updated_at DESC, p.property_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminUserItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.History item = new AdminUserItem.History();
                    item.setPropertyId(rs.getInt("property_id"));
                    item.setTitle(rs.getString("property_name"));
                    item.setStatusText(rs.getString("listing_status"));
                    item.setSecondaryTime(getOffsetDateTime(rs, "admin_hidden_at"));
                    item.setPrimaryCount(rs.getLong("application_count"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 対象会員に対して寄せられた通報履歴を取得する。
     * @param userId 会員ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 通報履歴一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.History> findReportHistory(int userId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT report_id, reason_code, report_status, created_at
                FROM reports
                WHERE target_user_id = ?
                ORDER BY created_at DESC, report_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminUserItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.History item = new AdminUserItem.History();
                    item.setRecordId(rs.getLong("report_id"));
                    item.setDetailText(rs.getString("reason_code"));
                    item.setStatusText(rs.getString("report_status"));
                    item.setPrimaryTime(getOffsetDateTime(rs, "created_at"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 会員本人の問い合わせ履歴を取得する。
     * @param userId 会員ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 問い合わせ履歴一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminUserItem.History> findInquiryHistory(int userId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT inquiry_id, subject, inquiry_status, updated_at
                FROM inquiries
                WHERE requester_user_id = ?
                ORDER BY updated_at DESC, inquiry_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminUserItem.History> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminUserItem.History item = new AdminUserItem.History();
                    item.setRecordId(rs.getLong("inquiry_id"));
                    item.setTitle(rs.getString("subject"));
                    item.setStatusText(rs.getString("inquiry_status"));
                    item.setSecondaryTime(getOffsetDateTime(rs, "updated_at"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 会員の利用停止（BAN）または利用停止解除（停止前の状態へ復元）を実行し、監査ログを記録する。
     * @param actorId 操作した管理者の会員ID。
     * @param targetUserId 対象会員ID。
     * @param suspend 利用停止にする場合はtrue、解除する場合はfalse。
     * @param reason 操作理由。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void changeAccountStatus(int actorId, int targetUserId, boolean suspend, String reason)
            throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                // 管理権限のある会員はDB側で権限を剥奪してから停止する運用を守る。
                if (targetUserId == actorId || auditDao.hasActiveAdminGrant(con, targetUserId)) {
                    throw new ManagementProblem(409, "管理権限が有効な会員は、権限を解除してから利用停止にしてください。");
                }
                AccountLock locked = lockUserRow(con, targetUserId);
                if (locked.withdrawn) {
                    throw new ManagementProblem(409, "退会済み会員の状態は変更できません。");
                }
                if (suspend && "BAN".equals(locked.status)) {
                    con.commit();
                    con.setAutoCommit(true);
                    return;
                }
                if (!suspend && !"BAN".equals(locked.status)) {
                    throw new ManagementProblem(409, "この会員は利用停止中ではありません。");
                }

                String nextStatus = suspend ? "BAN" : findStatusBeforeBan(con, targetUserId, locked.role);
                String updateSql = "UPDATE users SET account_status = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
                try (PreparedStatement stmt = con.prepareStatement(updateSql)) {
                    stmt.setString(1, nextStatus);
                    stmt.setInt(2, targetUserId);
                    stmt.executeUpdate();
                }

                auditDao.insertLog(con, actorId, suspend ? "SUSPEND" : "RESTORE", "USER",
                        Map.of("user_id", targetUserId), reason,
                        Map.of("before", locked.status, "after", nextStatus));

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
     * 会員の基本連絡先（表示名・メール）と、登録済みの場合は区分別プロフィールを更新し、監査ログを記録する。
     * @param actorId 操作した管理者の会員ID。
     * @param targetUserId 対象会員ID。
     * @param displayName 表示名。
     * @param email メールアドレス。任意。
     * @param roleProfile 区分別プロフィールの更新値。本登録前でプロフィール未登録の場合はnull。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void updateProfile(int actorId, int targetUserId, String displayName, String email,
            AdminUserItem.Profile roleProfile) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                AccountLock locked = lockUserRow(con, targetUserId);
                if (locked.withdrawn) {
                    throw new ManagementProblem(409, "退会済み会員のプロフィールは変更できません。");
                }

                String contactSql = "UPDATE users SET display_name = ?, email = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
                try (PreparedStatement stmt = con.prepareStatement(contactSql)) {
                    stmt.setString(1, displayName);
                    stmt.setString(2, email);
                    stmt.setInt(3, targetUserId);
                    stmt.executeUpdate();
                }

                // メールアドレスを削除した場合は、本人画面と整合させるためメール通知設定も無効化する。
                if (email == null) {
                    String disableNotifySql = """
                            UPDATE notification_settings
                            SET email_enabled = false, updated_at = CURRENT_TIMESTAMP
                            WHERE user_id = ? AND email_enabled
                            """;
                    try (PreparedStatement stmt = con.prepareStatement(disableNotifySql)) {
                        stmt.setInt(1, targetUserId);
                        stmt.executeUpdate();
                    }
                }

                // 区分別プロフィールが存在する会員のみ区分別テーブルを更新する。
                if (hasRoleProfileInTx(con, locked.role, targetUserId)) {
                    if (roleProfile == null) {
                        throw new ManagementProblem(400, "プロフィールの入力内容を確認してください。");
                    }
                    updateRoleProfileInTx(con, locked.role, targetUserId, roleProfile);
                }

                auditDao.insertLog(con, actorId, "UPDATE_PROFILE", "USER",
                        Map.of("user_id", targetUserId), null, Map.of("role", locked.role));

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
     * 会員の希望業種を差分更新し、継続選択された業種の登録日時を保ったまま監査ログを記録する。
     * @param actorId 操作した管理者の会員ID。
     * @param targetUserId 対象会員ID。
     * @param selectedIndustryIds 更新後の希望業種ID集合。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void updatePreferences(int actorId, int targetUserId, Set<Integer> selectedIndustryIds)
            throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                AccountLock locked = lockUserRow(con, targetUserId);
                if (locked.withdrawn) {
                    throw new ManagementProblem(409, "退会済み会員のプロフィールは変更できません。");
                }
                // 本登録済みの事業主・物件オーナーは本人画面で希望業種が1件以上必須のため、0件への更新を拒否する。
                if (selectedIndustryIds.isEmpty()
                        && ("BUSINESS".equals(locked.role) || "OWNER".equals(locked.role))
                        && hasRoleProfileInTx(con, locked.role, targetUserId)) {
                    throw new ManagementProblem(400, "本登録済みの事業主・物件オーナーの希望業種は1件以上選択してください。");
                }

                List<Integer> before = findPreferenceIdsInTx(con, targetUserId);
                for (Integer industryId : selectedIndustryIds) {
                    if (before.contains(industryId)) {
                        continue;
                    }
                    // 新規追加する業種が有効であることを共有ロックで確認してから登録する。
                    if (!lockActiveIndustryInTx(con, industryId)) {
                        throw new ManagementProblem(400, "有効な業種を選択してください。");
                    }
                    String insertSql = "INSERT INTO user_industry_preferences (user_id, industry_type_id) VALUES (?, ?)";
                    try (PreparedStatement stmt = con.prepareStatement(insertSql)) {
                        stmt.setInt(1, targetUserId);
                        stmt.setInt(2, industryId);
                        stmt.executeUpdate();
                    }
                }
                for (Integer industryId : before) {
                    if (!selectedIndustryIds.contains(industryId)) {
                        String deleteSql = "DELETE FROM user_industry_preferences WHERE user_id = ? AND industry_type_id = ?";
                        try (PreparedStatement stmt = con.prepareStatement(deleteSql)) {
                            stmt.setInt(1, targetUserId);
                            stmt.setInt(2, industryId);
                            stmt.executeUpdate();
                        }
                    }
                }

                auditDao.insertLog(con, actorId, "UPDATE_PREFERENCES", "USER",
                        Map.of("user_id", targetUserId), null,
                        Map.of("before", before, "after", selectedIndustryIds));

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
     * 地域住民の生活拠点（居住地：HOME必須、勤務先・学校：WORK任意）を一括保存し、監査ログを記録する。
     * 勤務先・学校が未入力（null）の場合は既存のWORKレコードを削除する。
     * @param actorId 操作した管理者の会員ID。
     * @param targetUserId 対象住民の会員ID。
     * @param home 居住地（HOME）の入力内容。必須。
     * @param work 勤務先・学校（WORK）の入力内容。解除する場合はnull。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void saveLocations(int actorId, int targetUserId, LocationInput home, LocationInput work)
            throws SQLException {
        if (home == null || !"HOME".equals(home.kind()) || (work != null && !"WORK".equals(work.kind()))) {
            throw new ManagementProblem(400, "生活拠点の入力内容を確認してください。");
        }
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                AccountLock locked = lockUserRow(con, targetUserId);
                if (locked.withdrawn) {
                    throw new ManagementProblem(409, "退会済み会員のプロフィールは変更できません。");
                }
                if (!"RESIDENT".equals(locked.role)) {
                    throw new ManagementProblem(403, "この会員の生活拠点は変更できません。");
                }

                // 居住地（HOME）を登録または更新する。
                upsertLocationInTx(con, targetUserId, home);

                // 勤務先・学校（WORK）は入力があれば保存し、未入力なら登録を解除する。
                if (work != null) {
                    upsertLocationInTx(con, targetUserId, work);
                } else {
                    deleteWorkLocationInTx(con, targetUserId);
                }

                auditDao.insertLog(con, actorId, "UPDATE_LOCATION", "USER",
                        Map.of("user_id", targetUserId), null,
                        Map.of("home", true, "work", work != null));

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
     * トランザクション内で生活拠点1件を登録・更新する。
     * @param con トランザクション中の接続。
     * @param targetUserId 対象住民の会員ID。
     * @param location 保存する生活拠点。
     * @throws SQLException DB更新に失敗した場合。
     */
    private static void upsertLocationInTx(Connection con, int targetUserId, LocationInput location)
            throws SQLException {
        String upsertSql = """
                INSERT INTO user_locations
                    (user_id, location_kind, prefecture, municipality, address_line, latitude, longitude)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, location_kind)
                DO UPDATE SET prefecture = EXCLUDED.prefecture,
                              municipality = EXCLUDED.municipality,
                              address_line = EXCLUDED.address_line,
                              latitude = EXCLUDED.latitude,
                              longitude = EXCLUDED.longitude,
                              updated_at = CURRENT_TIMESTAMP
                """;
        try (PreparedStatement stmt = con.prepareStatement(upsertSql)) {
            stmt.setInt(1, targetUserId);
            stmt.setString(2, location.kind());
            stmt.setString(3, location.prefecture());
            stmt.setString(4, location.municipality());
            stmt.setString(5, location.addressLine());
            stmt.setBigDecimal(6, location.latitude());
            stmt.setBigDecimal(7, location.longitude());
            stmt.executeUpdate();
        }
    }

    /**
     * トランザクション内で勤務先・学校（WORK）の登録を解除する。投票履歴から参照されている場合は409エラーとする。
     * @param con トランザクション中の接続。
     * @param targetUserId 対象住民の会員ID。
     * @throws SQLException DB照会または削除に失敗した場合。
     */
    private static void deleteWorkLocationInTx(Connection con, int targetUserId) throws SQLException {
        // 投票履歴が参照中の拠点は外部キー制約で削除できないため、分かりやすい業務エラーを返す。
        String checkSql = """
                SELECT 1
                FROM user_locations l
                JOIN property_votes v ON v.location_id = l.location_id
                WHERE l.user_id = ? AND l.location_kind = 'WORK'
                LIMIT 1
                """;
        try (PreparedStatement stmt = con.prepareStatement(checkSql)) {
            stmt.setInt(1, targetUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    throw new ManagementProblem(409, "投票履歴から参照されている勤務先・学校は解除できません。");
                }
            }
        }
        String deleteSql = "DELETE FROM user_locations WHERE user_id = ? AND location_kind = 'WORK'";
        try (PreparedStatement stmt = con.prepareStatement(deleteSql)) {
            stmt.setInt(1, targetUserId);
            stmt.executeUpdate();
        }
    }

    /**
     * 事業主の保有資格・許認可を登録・更新し、監査ログを記録する。
     * @param actorId 操作した管理者の会員ID。
     * @param targetUserId 対象事業主の会員ID。
     * @param qualificationId 資格・許認可ID。
     * @param credentialNumber 資格番号。任意。
     * @param obtainedOn 取得日。任意。
     * @param expiresOn 有効期限。任意。
     * @param active 有効状態。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void upsertQualification(int actorId, int targetUserId, int qualificationId,
            String credentialNumber, LocalDate obtainedOn, LocalDate expiresOn, boolean active) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                AccountLock locked = lockUserRow(con, targetUserId);
                if (locked.withdrawn) {
                    throw new ManagementProblem(409, "退会済み会員のプロフィールは変更できません。");
                }
                if (!"BUSINESS".equals(locked.role)) {
                    throw new ManagementProblem(403, "この会員の保有資格は変更できません。");
                }
                if (!hasRoleProfileInTx(con, "BUSINESS", targetUserId)) {
                    throw new ManagementProblem(404, "事業主プロフィールが見つかりません。");
                }
                requireAssignableQualificationInTx(con, qualificationId, targetUserId);

                String upsertSql = """
                        INSERT INTO business_qualifications
                            (business_user_id, qualification_id, credential_number, obtained_on, expires_on, is_active)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (business_user_id, qualification_id)
                        DO UPDATE SET credential_number = EXCLUDED.credential_number,
                                      obtained_on = EXCLUDED.obtained_on,
                                      expires_on = EXCLUDED.expires_on,
                                      is_active = EXCLUDED.is_active,
                                      updated_at = CURRENT_TIMESTAMP
                        """;
                try (PreparedStatement stmt = con.prepareStatement(upsertSql)) {
                    stmt.setInt(1, targetUserId);
                    stmt.setInt(2, qualificationId);
                    stmt.setString(3, credentialNumber);
                    stmt.setObject(4, obtainedOn, Types.DATE);
                    stmt.setObject(5, expiresOn, Types.DATE);
                    stmt.setBoolean(6, active);
                    stmt.executeUpdate();
                }

                auditDao.insertLog(con, actorId, "UPDATE_QUALIFICATION", "USER",
                        Map.of("user_id", targetUserId), null,
                        Map.of("qualification_id", qualificationId, "is_active", active));

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
     * 更新対象の会員行を排他ロックして利用区分・状態・退会済み判定を返す。
     * @param con トランザクション中の接続。
     * @param userId 会員ID。
     * @return ロック結果。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static AccountLock lockUserRow(Connection con, int userId) throws SQLException {
        String sql = "SELECT user_role, account_status, withdrawn_at FROM users WHERE user_id = ? FOR UPDATE";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定された会員が見つかりません。");
                }
                String status = rs.getString("account_status");
                boolean withdrawn = rs.getObject("withdrawn_at") != null || "WITHDRAWN".equals(status);
                return new AccountLock(rs.getString("user_role"), status, withdrawn);
            }
        }
    }

    /**
     * 利用停止解除時の復帰先状態（PENDING / ACTIVE）を最新のSUSPENDログまたはプロフィール有無から決める。
     * @param con トランザクション中の接続。
     * @param userId 会員ID。
     * @param role 利用区分。
     * @return 復帰先の利用状態コード。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static String findStatusBeforeBan(Connection con, int userId, String role) throws SQLException {
        String sql = """
                SELECT changes->>'before' AS state
                FROM admin_audit_logs
                WHERE target_type = 'USER'
                  AND action = 'SUSPEND'
                  AND target_key = jsonb_build_object('user_id', ?::integer)
                ORDER BY audit_log_id DESC
                LIMIT 1
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String previousState = rs.getString("state");
                    if ("PENDING".equals(previousState) || "ACTIVE".equals(previousState)) {
                        return previousState;
                    }
                }
            }
        }
        // 停止履歴が残っていない場合は、区分別プロフィールが存在するかで復帰先を決める。
        return hasRoleProfileInTx(con, role, userId) ? "ACTIVE" : "PENDING";
    }

    /**
     * 会員の利用区分に対応するプロフィール行が存在するか確認する。
     * @param con トランザクション中の接続。
     * @param role 利用区分。
     * @param userId 会員ID。
     * @return プロフィール行が存在する場合はtrue。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static boolean hasRoleProfileInTx(Connection con, String role, int userId) throws SQLException {
        String sql;
        if ("RESIDENT".equals(role)) {
            sql = "SELECT user_id FROM resident_profiles WHERE user_id = ?";
        } else if ("OWNER".equals(role)) {
            sql = "SELECT user_id FROM owner_profiles WHERE user_id = ?";
        } else if ("BUSINESS".equals(role)) {
            sql = "SELECT user_id FROM business_profiles WHERE user_id = ?";
        } else {
            throw new IllegalArgumentException("利用区分が正しくありません。");
        }
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 利用区分に応じたプロフィールテーブルを更新する。
     * @param con トランザクション中の接続。
     * @param role 利用区分。
     * @param userId 会員ID。
     * @param profile 更新するプロフィール値。
     * @throws SQLException DB更新に失敗した場合。
     */
    private static void updateRoleProfileInTx(Connection con, String role, int userId, AdminUserItem.Profile profile)
            throws SQLException {
        if ("RESIDENT".equals(role)) {
            String sql = """
                    UPDATE resident_profiles
                    SET age_group = NULLIF(?, ''), gender = NULLIF(?, ''), updated_at = CURRENT_TIMESTAMP
                    WHERE user_id = ?
                    """;
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, profile.getAgeGroup() == null ? "" : profile.getAgeGroup());
                stmt.setString(2, profile.getGender() == null ? "" : profile.getGender());
                stmt.setInt(3, userId);
                stmt.executeUpdate();
            }
            return;
        }
        if ("OWNER".equals(role)) {
            String sql = """
                    UPDATE owner_profiles
                    SET representative_name = ?, company_name = ?, phone_e164 = ?, secondary_phone_e164 = ?,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE user_id = ?
                    """;
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, profile.getRepresentativeName());
                stmt.setString(2, profile.getCompanyName());
                stmt.setString(3, profile.getPhoneE164());
                stmt.setString(4, profile.getSecondaryPhoneE164());
                stmt.setInt(5, userId);
                stmt.executeUpdate();
            }
            return;
        }
        String sql = """
                UPDATE business_profiles
                SET business_name = ?, representative_name = ?, phone_e164 = ?, secondary_phone_e164 = ?,
                    preferred_area = ?, employee_count = ?, industry_experience = ?, management_experience = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, profile.getBusinessName());
            stmt.setString(2, profile.getRepresentativeName());
            stmt.setString(3, profile.getPhoneE164());
            stmt.setString(4, profile.getSecondaryPhoneE164());
            stmt.setString(5, profile.getPreferredArea());
            stmt.setInt(6, profile.getEmployeeCount());
            stmt.setString(7, profile.getIndustryExperience());
            stmt.setString(8, profile.getManagementExperience());
            stmt.setInt(9, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * 現在登録済みの希望業種ID一覧を昇順で取得する。
     * @param con トランザクション中の接続。
     * @param userId 会員ID。
     * @return 業種ID一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static List<Integer> findPreferenceIdsInTx(Connection con, int userId) throws SQLException {
        String sql = "SELECT industry_type_id FROM user_industry_preferences WHERE user_id = ? ORDER BY industry_type_id";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("industry_type_id"));
                }
            }
        }
        return ids;
    }

    /**
     * 希望業種として新規追加する業種および所属カテゴリが有効であることを共有ロックで確認する。
     * @param con トランザクション中の接続。
     * @param industryId 業種ID。
     * @return 有効な業種であればtrue。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static boolean lockActiveIndustryInTx(Connection con, int industryId) throws SQLException {
        String sql = """
                SELECT i.industry_type_id
                FROM industry_types i
                JOIN industry_categories c USING (category_id)
                WHERE i.industry_type_id = ? AND i.is_active AND c.is_active
                FOR SHARE OF i, c
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, industryId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 事業主に割り当て可能な資格・許認可（有効な資格/許認可または既存保有資格）であることを確認する。
     * @param con トランザクション中の接続。
     * @param qualificationId 資格ID。
     * @param userId 事業主の会員ID。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static void requireAssignableQualificationInTx(Connection con, int qualificationId, int userId)
            throws SQLException {
        String sql = """
                SELECT q.qualification_id
                FROM qualifications q
                WHERE q.qualification_id = ?
                  AND ((q.is_active AND q.requirement_kind IN ('QUALIFICATION', 'PERMIT'))
                       OR EXISTS (
                           SELECT 1 FROM business_qualifications b
                           WHERE b.business_user_id = ? AND b.qualification_id = q.qualification_id
                       ))
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, qualificationId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定された資格・許認可は登録できません。");
                }
            }
        }
    }

    /**
     * ロックした会員行の利用区分・利用状態・退会済み判定を保持する内部クラス。
     */
    private static final class AccountLock {
        /** 利用区分。 */
        private final String role;
        /** 利用状態。 */
        private final String status;
        /** 退会済みフラグ。 */
        private final boolean withdrawn;

        /**
         * ロック結果を保持するインスタンスを生成する。
         * @param role 利用区分。
         * @param status 利用状態。
         * @param withdrawn 退会済みの場合はtrue。
         */
        private AccountLock(String role, String status, boolean withdrawn) {
            this.role = role;
            this.status = status;
            this.withdrawn = withdrawn;
        }
    }
}
