package jp.machiriku.dao.management;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.machiriku.bean.management.AdminIndustryItem;
import jp.machiriku.bean.management.AdminQualificationItem;
import jp.machiriku.bean.management.ManagementLabels;
import jp.machiriku.bean.management.ManagementProblem;

/**
 * 業種カテゴリ・業種・資格マスターおよび業種別の必要条件を照会・保存するDAO。
 */
public class AdminCatalogDao extends ManagementBaseDao {
    /** 管理者権限チェックと監査ログ記録に使うDAO。 */
    private final AdminAuditDao auditDao;

    /** 本番接続を使用するDAOを生成する。 */
    public AdminCatalogDao() {
        super();
        this.auditDao = new AdminAuditDao();
    }

    /**
     * テスト用の共有接続を使用するDAOを生成する。
     * @param sharedConnection テスト用接続。
     */
    public AdminCatalogDao(Connection sharedConnection) {
        super(sharedConnection);
        this.auditDao = new AdminAuditDao(sharedConnection);
    }

    /**
     * 業種カテゴリをキーワードと有効状態で絞り込んで取得する。
     * @param keyword コードまたは名称の部分一致語。空文字で全件。
     * @param status 有効状態（true / false）。空文字で全件。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 業種カテゴリ一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminIndustryItem> findCategories(String keyword, String status, int limit, int offset)
            throws SQLException {
        String sql = """
                SELECT category_id, category_code, category_name, display_order, is_active
                FROM industry_categories
                WHERE strpos(lower(concat_ws(' ', category_code, category_name)), lower(?)) > 0
                  AND is_active::text = COALESCE(NULLIF(?, ''), is_active::text)
                ORDER BY display_order, category_id
                LIMIT ? OFFSET ?
                """;
        List<AdminIndustryItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, keyword);
            stmt.setString(2, status);
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminIndustryItem item = new AdminIndustryItem();
                    item.setId(rs.getInt("category_id"));
                    item.setCode(rs.getString("category_code"));
                    item.setName(rs.getString("category_name"));
                    item.setDisplayOrder(rs.getInt("display_order"));
                    item.setActive(rs.getBoolean("is_active"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 業種をキーワードと有効状態で絞り込んで取得する。
     * @param keyword コード・業種名・カテゴリ名の部分一致語。空文字で全件。
     * @param status 有効状態（true / false）。空文字で全件。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 業種一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminIndustryItem> findIndustryTypes(String keyword, String status, int limit, int offset)
            throws SQLException {
        String sql = """
                SELECT i.industry_type_id, i.industry_code, i.industry_name,
                       i.category_id, c.category_name, i.display_order,
                       i.is_active, c.is_active AS category_active
                FROM industry_types i
                JOIN industry_categories c USING (category_id)
                WHERE strpos(lower(concat_ws(' ', i.industry_code, i.industry_name, c.category_name)), lower(?)) > 0
                  AND i.is_active::text = COALESCE(NULLIF(?, ''), i.is_active::text)
                ORDER BY c.display_order, c.category_id, i.display_order, i.industry_type_id
                LIMIT ? OFFSET ?
                """;
        List<AdminIndustryItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, keyword);
            stmt.setString(2, status);
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminIndustryItem item = new AdminIndustryItem();
                    item.setId(rs.getInt("industry_type_id"));
                    item.setCode(rs.getString("industry_code"));
                    item.setName(rs.getString("industry_name"));
                    item.setCategoryId(rs.getInt("category_id"));
                    item.setCategoryName(rs.getString("category_name"));
                    item.setDisplayOrder(rs.getInt("display_order"));
                    item.setActive(rs.getBoolean("is_active"));
                    item.setCategoryActive(rs.getBoolean("category_active"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 編集対象となる業種カテゴリ1件を取得する。
     * @param categoryId カテゴリID。
     * @return カテゴリ情報。存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminIndustryItem findCategoryById(int categoryId) throws SQLException {
        String sql = """
                SELECT category_id, category_code, category_name, display_order, is_active
                FROM industry_categories
                WHERE category_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminIndustryItem item = new AdminIndustryItem();
                item.setId(rs.getInt("category_id"));
                item.setCode(rs.getString("category_code"));
                item.setName(rs.getString("category_name"));
                item.setDisplayOrder(rs.getInt("display_order"));
                item.setActive(rs.getBoolean("is_active"));
                return item;
            }
        }
    }

    /**
     * 編集対象となる業種1件を取得する。
     * @param industryTypeId 業種ID。
     * @return 業種情報。存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminIndustryItem findIndustryTypeById(int industryTypeId) throws SQLException {
        String sql = """
                SELECT industry_type_id, industry_code, industry_name, category_id, display_order, is_active
                FROM industry_types
                WHERE industry_type_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, industryTypeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminIndustryItem item = new AdminIndustryItem();
                item.setId(rs.getInt("industry_type_id"));
                item.setCode(rs.getString("industry_code"));
                item.setName(rs.getString("industry_name"));
                item.setCategoryId(rs.getInt("category_id"));
                item.setDisplayOrder(rs.getInt("display_order"));
                item.setActive(rs.getBoolean("is_active"));
                return item;
            }
        }
    }

    /**
     * 業種編集フォームの所属カテゴリ選択肢を取得する。
     * @return カテゴリID文字列をキー、表示名を値とする選択肢マップ。
     * @throws SQLException DB照会に失敗した場合。
     */
    public Map<String, String> findCategoryChoices() throws SQLException {
        String sql = """
                SELECT category_id,
                       category_name || CASE WHEN is_active THEN '' ELSE '（無効）' END AS label
                FROM industry_categories
                ORDER BY display_order, category_id
                """;
        Map<String, String> choices = new LinkedHashMap<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                choices.put(String.valueOf(rs.getInt("category_id")), rs.getString("label"));
            }
        }
        return choices;
    }

    /**
     * 業種カテゴリを新規登録または更新し、監査ログを記録する。
     * @param actorId 操作した管理者の会員ID。
     * @param categoryId 更新時のカテゴリID。新規登録時はnull。
     * @param code 新規登録時のカテゴリコード。更新時は使用しない。
     * @param name カテゴリ名称。
     * @param displayOrder 表示順。
     * @param active 有効状態。
     * @return 保存されたカテゴリID。
     * @throws SQLException DB更新に失敗した場合。
     */
    public int saveCategory(int actorId, Integer categoryId, String code, String name,
            int displayOrder, boolean active) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                Map<String, Object> after = new LinkedHashMap<>();
                after.put("name", name);
                after.put("display_order", displayOrder);
                after.put("is_active", active);

                int savedId;
                if (categoryId == null) {
                    // 新規登録時はコードを含めて登録し、採番されたIDを取得する。
                    String insertSql = """
                            INSERT INTO industry_categories (category_code, category_name, display_order, is_active)
                            VALUES (?, ?, ?, ?)
                            RETURNING category_id
                            """;
                    try (PreparedStatement stmt = con.prepareStatement(insertSql)) {
                        stmt.setString(1, code);
                        stmt.setString(2, name);
                        stmt.setInt(3, displayOrder);
                        stmt.setBoolean(4, active);
                        try (ResultSet rs = stmt.executeQuery()) {
                            rs.next();
                            savedId = rs.getInt("category_id");
                        }
                    }
                    after.put("code", code);
                    auditDao.insertLog(con, actorId, "CREATE", "INDUSTRY_CATEGORY",
                            Map.of("category_id", savedId), null, Map.of("after", after));
                } else {
                    // 更新時は対象行をロックし、変更前の値を監査ログ用に控えながら更新する。
                    Map<String, Object> before = lockCategory(con, categoryId);
                    String updateSql = """
                            UPDATE industry_categories
                            SET category_name = ?, display_order = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP
                            WHERE category_id = ?
                            """;
                    try (PreparedStatement stmt = con.prepareStatement(updateSql)) {
                        stmt.setString(1, name);
                        stmt.setInt(2, displayOrder);
                        stmt.setBoolean(3, active);
                        stmt.setInt(4, categoryId);
                        stmt.executeUpdate();
                    }
                    savedId = categoryId;
                    auditDao.insertLog(con, actorId, "UPDATE", "INDUSTRY_CATEGORY",
                            Map.of("category_id", savedId), null, Map.of("before", before, "after", after));
                }

                con.commit();
                con.setAutoCommit(true);
                return savedId;
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
     * 業種を新規登録または更新し、監査ログを記録する。
     * @param actorId 操作した管理者の会員ID。
     * @param industryTypeId 更新時の業種ID。新規登録時はnull。
     * @param categoryId 所属カテゴリID。
     * @param code 新規登録時の業種コード。更新時は使用しない。
     * @param name 業種名称。
     * @param displayOrder 表示順。
     * @param active 有効状態。
     * @return 保存された業種ID。
     * @throws SQLException DB更新に失敗した場合。
     */
    public int saveIndustryType(int actorId, Integer industryTypeId, int categoryId, String code,
            String name, int displayOrder, boolean active) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                // 所属先カテゴリが存在しない場合は404を返す。
                requireCategoryInTx(con, categoryId);

                Map<String, Object> after = new LinkedHashMap<>();
                after.put("name", name);
                after.put("display_order", displayOrder);
                after.put("is_active", active);
                after.put("category_id", categoryId);

                int savedId;
                if (industryTypeId == null) {
                    String insertSql = """
                            INSERT INTO industry_types (category_id, industry_code, industry_name, display_order, is_active)
                            VALUES (?, ?, ?, ?, ?)
                            RETURNING industry_type_id
                            """;
                    try (PreparedStatement stmt = con.prepareStatement(insertSql)) {
                        stmt.setInt(1, categoryId);
                        stmt.setString(2, code);
                        stmt.setString(3, name);
                        stmt.setInt(4, displayOrder);
                        stmt.setBoolean(5, active);
                        try (ResultSet rs = stmt.executeQuery()) {
                            rs.next();
                            savedId = rs.getInt("industry_type_id");
                        }
                    }
                    after.put("code", code);
                    auditDao.insertLog(con, actorId, "CREATE", "INDUSTRY_TYPE",
                            Map.of("industry_type_id", savedId), null, Map.of("after", after));
                } else {
                    Map<String, Object> before = lockIndustryTypeEntry(con, industryTypeId);
                    String updateSql = """
                            UPDATE industry_types
                            SET category_id = ?, industry_name = ?, display_order = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP
                            WHERE industry_type_id = ?
                            """;
                    try (PreparedStatement stmt = con.prepareStatement(updateSql)) {
                        stmt.setInt(1, categoryId);
                        stmt.setString(2, name);
                        stmt.setInt(3, displayOrder);
                        stmt.setBoolean(4, active);
                        stmt.setInt(5, industryTypeId);
                        stmt.executeUpdate();
                    }
                    savedId = industryTypeId;
                    auditDao.insertLog(con, actorId, "UPDATE", "INDUSTRY_TYPE",
                            Map.of("industry_type_id", savedId), null, Map.of("before", before, "after", after));
                }

                con.commit();
                con.setAutoCommit(true);
                return savedId;
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
     * 資格・書類マスターを種別・有効状態・名称で絞り込んで取得する。
     * @param kind 種類コード（QUALIFICATION / PERMIT / DOCUMENT）。空文字で全件。
     * @param status 有効状態（true / false）。空文字で全件。
     * @param keyword 名称の部分一致語。空文字で全件。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 資格・書類マスター一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminQualificationItem> findQualifications(String kind, String status, String keyword,
            int limit, int offset) throws SQLException {
        String sql = """
                SELECT qualification_id, qualification_name, requirement_kind, guidance_text, guidance_url, is_active
                FROM qualifications
                WHERE requirement_kind = COALESCE(NULLIF(?, ''), requirement_kind)
                  AND is_active::text = COALESCE(NULLIF(?, ''), is_active::text)
                  AND strpos(lower(qualification_name), lower(?)) > 0
                ORDER BY qualification_name, qualification_id
                LIMIT ? OFFSET ?
                """;
        List<AdminQualificationItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, kind);
            stmt.setString(2, status);
            stmt.setString(3, keyword);
            stmt.setInt(4, limit);
            stmt.setInt(5, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(readQualification(rs));
                }
            }
        }
        return list;
    }

    /**
     * 編集対象となる資格・書類マスター1件を取得する。
     * @param qualificationId 資格・書類ID。
     * @return 資格・書類情報。存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminQualificationItem findQualificationById(int qualificationId) throws SQLException {
        String sql = """
                SELECT qualification_id, qualification_name, requirement_kind, guidance_text, guidance_url, is_active
                FROM qualifications
                WHERE qualification_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, qualificationId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? readQualification(rs) : null;
            }
        }
    }

    /**
     * 業種別必要条件画面の業種選択肢を取得する。
     * @return 業種ID文字列をキー、カテゴリ名/業種名を値とする選択肢マップ。
     * @throws SQLException DB照会に失敗した場合。
     */
    public Map<String, String> findIndustryChoices() throws SQLException {
        String sql = """
                SELECT i.industry_type_id, c.category_name || ' / ' || i.industry_name AS label
                FROM industry_types i
                JOIN industry_categories c USING (category_id)
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
     * 指定した業種IDが存在するか確認する。
     * @param industryTypeId 業種ID。
     * @return 存在する場合はtrue。
     * @throws SQLException DB照会に失敗した場合。
     */
    public boolean existsIndustryType(int industryTypeId) throws SQLException {
        String sql = "SELECT industry_type_id FROM industry_types WHERE industry_type_id = ?";
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, industryTypeId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 指定業種に割り当て済みの必要資格・書類一覧を取得する。
     * @param industryTypeId 業種ID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 必要条件一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminQualificationItem> findRequirements(int industryTypeId, int limit, int offset)
            throws SQLException {
        String sql = """
                SELECT q.qualification_id, q.qualification_name, q.requirement_kind,
                       req.is_required, req.condition_text, q.guidance_url, q.is_active
                FROM industry_qualification_requirements req
                JOIN qualifications q USING (qualification_id)
                WHERE req.industry_type_id = ?
                ORDER BY req.is_required DESC, q.qualification_name, q.qualification_id
                LIMIT ? OFFSET ?
                """;
        List<AdminQualificationItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, industryTypeId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminQualificationItem item = new AdminQualificationItem();
                    item.setQualificationId(rs.getInt("qualification_id"));
                    item.setQualificationName(rs.getString("qualification_name"));
                    item.setRequirementKind(rs.getString("requirement_kind"));
                    item.setRequired(rs.getBoolean("is_required"));
                    item.setConditionText(rs.getString("condition_text"));
                    item.setGuidanceUrl(rs.getString("guidance_url"));
                    item.setActive(rs.getBoolean("is_active"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 指定した業種と資格の組み合わせに設定済みの必要条件1件を取得する。
     * @param industryTypeId 業種ID。
     * @param qualificationId 資格・書類ID。
     * @return 必要条件。未割り当ての場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminQualificationItem findRequirement(int industryTypeId, int qualificationId) throws SQLException {
        String sql = """
                SELECT qualification_id, is_required, condition_text
                FROM industry_qualification_requirements
                WHERE industry_type_id = ? AND qualification_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, industryTypeId);
            stmt.setInt(2, qualificationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminQualificationItem item = new AdminQualificationItem();
                item.setQualificationId(rs.getInt("qualification_id"));
                item.setRequired(rs.getBoolean("is_required"));
                item.setConditionText(rs.getString("condition_text"));
                return item;
            }
        }
    }

    /**
     * 指定業種の必要条件フォームに表示する資格・書類の選択肢（有効または割当済み）を取得する。
     * @param industryTypeId 業種ID。
     * @return 資格ID文字列をキー、名称と種別表示名を値とする選択肢マップ。
     * @throws SQLException DB照会に失敗した場合。
     */
    public Map<String, String> findRequirementChoices(int industryTypeId) throws SQLException {
        String sql = """
                SELECT q.qualification_id, q.qualification_name, q.requirement_kind
                FROM qualifications q
                WHERE q.is_active
                   OR EXISTS (
                       SELECT 1 FROM industry_qualification_requirements r
                       WHERE r.industry_type_id = ? AND r.qualification_id = q.qualification_id
                   )
                ORDER BY q.qualification_name, q.qualification_id
                """;
        Map<String, String> choices = new LinkedHashMap<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, industryTypeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String kindLabel = ManagementLabels.label(
                            ManagementLabels.QUALIFICATION_KINDS, rs.getString("requirement_kind"));
                    choices.put(
                            String.valueOf(rs.getInt("qualification_id")),
                            rs.getString("qualification_name") + "（" + kindLabel + "）");
                }
            }
        }
        return choices;
    }

    /**
     * 資格・書類マスターを新規登録または更新し、監査ログを記録する。
     * @param actorId 操作した管理者の会員ID。
     * @param qualificationId 更新時の資格ID。新規登録時はnull。
     * @param name 名称。
     * @param kind 種類コード（QUALIFICATION / PERMIT / DOCUMENT）。
     * @param guidance 利用者向けの案内説明。任意。
     * @param url 案内・様式URL。任意。
     * @param active 有効状態。
     * @return 保存された資格ID。
     * @throws SQLException DB更新に失敗した場合。
     */
    public int saveQualification(int actorId, Integer qualificationId, String name, String kind,
            String guidance, String url, boolean active) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                int savedId;
                if (qualificationId == null) {
                    String insertSql = """
                            INSERT INTO qualifications (qualification_name, requirement_kind, guidance_text, guidance_url, is_active)
                            VALUES (?, ?, ?, ?, ?)
                            RETURNING qualification_id
                            """;
                    try (PreparedStatement stmt = con.prepareStatement(insertSql)) {
                        stmt.setString(1, name);
                        stmt.setString(2, kind);
                        stmt.setString(3, guidance);
                        stmt.setString(4, url);
                        stmt.setBoolean(5, active);
                        try (ResultSet rs = stmt.executeQuery()) {
                            rs.next();
                            savedId = rs.getInt("qualification_id");
                        }
                    }
                    auditDao.insertLog(con, actorId, "CREATE", "QUALIFICATION",
                            Map.of("qualification_id", savedId), null,
                            Map.of("name", name, "kind", kind, "is_active", active));
                } else {
                    // 更新対象をロックし、事業主の保有資格で使われている項目の種別変更を禁止する。
                    Map<String, Object> before = lockQualification(con, qualificationId);
                    if (!kind.equals(before.get("requirement_kind")) && isUsedByBusinessQualification(con, qualificationId)) {
                        throw new ManagementProblem(409, "保有資格として登録されている項目の種類は変更できません。");
                    }
                    String updateSql = """
                            UPDATE qualifications
                            SET qualification_name = ?, requirement_kind = ?, guidance_text = ?, guidance_url = ?, is_active = ?
                            WHERE qualification_id = ?
                            """;
                    try (PreparedStatement stmt = con.prepareStatement(updateSql)) {
                        stmt.setString(1, name);
                        stmt.setString(2, kind);
                        stmt.setString(3, guidance);
                        stmt.setString(4, url);
                        stmt.setBoolean(5, active);
                        stmt.setInt(6, qualificationId);
                        stmt.executeUpdate();
                    }
                    savedId = qualificationId;
                    auditDao.insertLog(con, actorId, "UPDATE", "QUALIFICATION",
                            Map.of("qualification_id", savedId), null,
                            Map.of("before", before,
                                    "after", Map.of("qualification_name", name, "requirement_kind", kind, "is_active", active)));
                }

                con.commit();
                con.setAutoCommit(true);
                return savedId;
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
     * 業種に必要な資格・書類の割り当てを登録または更新し、監査ログを記録する。
     * @param actorId 操作した管理者の会員ID。
     * @param industryTypeId 業種ID。
     * @param qualificationId 資格・書類ID。
     * @param required 必須の場合はtrue。
     * @param condition 適用条件の補足文。任意。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void saveRequirement(int actorId, int industryTypeId, int qualificationId,
            boolean required, String condition) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                // 同一業種の必要条件編集を直列化するため業種行をロックする。
                lockIndustryRow(con, industryTypeId);
                Map<String, Object> before = lockRequirementRow(con, industryTypeId, qualificationId);
                // 新規割り当ての場合のみ、有効な資格マスターであることを共有ロックで確認する。
                if (before == null) {
                    lockActiveQualification(con, qualificationId);
                }

                String upsertSql = """
                        INSERT INTO industry_qualification_requirements
                            (industry_type_id, qualification_id, is_required, condition_text)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (industry_type_id, qualification_id)
                        DO UPDATE SET is_required = EXCLUDED.is_required, condition_text = EXCLUDED.condition_text
                        """;
                try (PreparedStatement stmt = con.prepareStatement(upsertSql)) {
                    stmt.setInt(1, industryTypeId);
                    stmt.setInt(2, qualificationId);
                    stmt.setBoolean(3, required);
                    stmt.setString(4, condition);
                    stmt.executeUpdate();
                }

                Map<String, Object> after = new LinkedHashMap<>();
                after.put("is_required", required);
                after.put("condition_text", condition);
                auditDao.insertLog(con, actorId, "ASSIGN", "INDUSTRY_REQUIREMENT",
                        Map.of("industry_type_id", industryTypeId, "qualification_id", qualificationId),
                        null,
                        Map.of("before", before == null ? Map.of() : before, "after", after));

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
     * 業種と資格・書類の関連付けを解除し、解除理由とともに監査ログを記録する。
     * @param actorId 操作した管理者の会員ID。
     * @param industryTypeId 業種ID。
     * @param qualificationId 資格・書類ID。
     * @param reason 解除理由。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void removeRequirement(int actorId, int industryTypeId, int qualificationId, String reason)
            throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                lockIndustryRow(con, industryTypeId);
                Map<String, Object> before = lockRequirementRow(con, industryTypeId, qualificationId);
                if (before == null) {
                    throw new ManagementProblem(404, "この必要条件は既に解除されています。");
                }

                String deleteSql = """
                        DELETE FROM industry_qualification_requirements
                        WHERE industry_type_id = ? AND qualification_id = ?
                        """;
                try (PreparedStatement stmt = con.prepareStatement(deleteSql)) {
                    stmt.setInt(1, industryTypeId);
                    stmt.setInt(2, qualificationId);
                    stmt.executeUpdate();
                }

                auditDao.insertLog(con, actorId, "REMOVE", "INDUSTRY_REQUIREMENT",
                        Map.of("industry_type_id", industryTypeId, "qualification_id", qualificationId),
                        reason, Map.of("before", before));

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
     * ResultSetの現在行をAdminQualificationItemへ変換する。
     * @param rs 読み取り対象のResultSet。
     * @return 変換した資格・書類モデル。
     * @throws SQLException 列の読み取りに失敗した場合。
     */
    private static AdminQualificationItem readQualification(ResultSet rs) throws SQLException {
        AdminQualificationItem item = new AdminQualificationItem();
        item.setQualificationId(rs.getInt("qualification_id"));
        item.setQualificationName(rs.getString("qualification_name"));
        item.setRequirementKind(rs.getString("requirement_kind"));
        item.setGuidanceText(rs.getString("guidance_text"));
        item.setGuidanceUrl(rs.getString("guidance_url"));
        item.setActive(rs.getBoolean("is_active"));
        return item;
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
     * 所属先カテゴリの存在を確認する。
     * @param con トランザクション中の接続。
     * @param categoryId カテゴリID。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static void requireCategoryInTx(Connection con, int categoryId) throws SQLException {
        String sql = "SELECT category_id FROM industry_categories WHERE category_id = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定されたカテゴリが見つかりません。");
                }
            }
        }
    }

    /**
     * 更新対象のカテゴリ行をロックし、変更前の値を返す。
     * @param con トランザクション中の接続。
     * @param categoryId カテゴリID。
     * @return 変更前属性のマップ。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static Map<String, Object> lockCategory(Connection con, int categoryId) throws SQLException {
        String sql = """
                SELECT category_name, display_order, is_active
                FROM industry_categories
                WHERE category_id = ?
                FOR UPDATE
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定されたカテゴリが見つかりません。");
                }
                Map<String, Object> before = new LinkedHashMap<>();
                before.put("name", rs.getString("category_name"));
                before.put("display_order", rs.getInt("display_order"));
                before.put("is_active", rs.getBoolean("is_active"));
                return before;
            }
        }
    }

    /**
     * 更新対象の業種行をロックし、変更前の値を返す。
     * @param con トランザクション中の接続。
     * @param industryTypeId 業種ID。
     * @return 変更前属性のマップ。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static Map<String, Object> lockIndustryTypeEntry(Connection con, int industryTypeId) throws SQLException {
        String sql = """
                SELECT industry_name, category_id, display_order, is_active
                FROM industry_types
                WHERE industry_type_id = ?
                FOR UPDATE
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, industryTypeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定された業種が見つかりません。");
                }
                Map<String, Object> before = new LinkedHashMap<>();
                before.put("name", rs.getString("industry_name"));
                before.put("category_id", rs.getInt("category_id"));
                before.put("display_order", rs.getInt("display_order"));
                before.put("is_active", rs.getBoolean("is_active"));
                return before;
            }
        }
    }

    /**
     * 更新対象の資格・書類マスター行をロックし、変更前の値を返す。
     * @param con トランザクション中の接続。
     * @param qualificationId 資格・書類ID。
     * @return 変更前属性のマップ。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static Map<String, Object> lockQualification(Connection con, int qualificationId) throws SQLException {
        String sql = """
                SELECT qualification_name, requirement_kind, is_active
                FROM qualifications
                WHERE qualification_id = ?
                FOR UPDATE
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, qualificationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定された資格・書類が見つかりません。");
                }
                Map<String, Object> before = new LinkedHashMap<>();
                before.put("qualification_name", rs.getString("qualification_name"));
                before.put("requirement_kind", rs.getString("requirement_kind"));
                before.put("is_active", rs.getBoolean("is_active"));
                return before;
            }
        }
    }

    /**
     * 指定資格が事業主の保有資格テーブルから参照されているか確認する。
     * @param con トランザクション中の接続。
     * @param qualificationId 資格・書類ID。
     * @return 1件以上参照されている場合はtrue。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static boolean isUsedByBusinessQualification(Connection con, int qualificationId) throws SQLException {
        String sql = "SELECT qualification_id FROM business_qualifications WHERE qualification_id = ? LIMIT 1";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, qualificationId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 必要条件の更新競合を防ぐため対象業種行を排他ロックする。
     * @param con トランザクション中の接続。
     * @param industryTypeId 業種ID。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static void lockIndustryRow(Connection con, int industryTypeId) throws SQLException {
        String sql = "SELECT industry_type_id FROM industry_types WHERE industry_type_id = ? FOR UPDATE";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, industryTypeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定された業種が見つかりません。");
                }
            }
        }
    }

    /**
     * 指定した業種と資格の既存要件行をロックして取得する。
     * @param con トランザクション中の接続。
     * @param industryTypeId 業種ID。
     * @param qualificationId 資格・書類ID。
     * @return 変更前属性のマップ。未登録の場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static Map<String, Object> lockRequirementRow(Connection con, int industryTypeId, int qualificationId)
            throws SQLException {
        String sql = """
                SELECT is_required, condition_text
                FROM industry_qualification_requirements
                WHERE industry_type_id = ? AND qualification_id = ?
                FOR UPDATE
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, industryTypeId);
            stmt.setInt(2, qualificationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Map<String, Object> before = new LinkedHashMap<>();
                before.put("is_required", rs.getBoolean("is_required"));
                before.put("condition_text", rs.getString("condition_text"));
                return before;
            }
        }
    }

    /**
     * 新規割り当て対象の資格マスターが有効であることを確認して共有ロックする。
     * @param con トランザクション中の接続。
     * @param qualificationId 資格・書類ID。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static void lockActiveQualification(Connection con, int qualificationId) throws SQLException {
        String sql = "SELECT qualification_id FROM qualifications WHERE qualification_id = ? AND is_active FOR SHARE";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, qualificationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定された有効な資格・書類が見つかりません。");
                }
            }
        }
    }
}
