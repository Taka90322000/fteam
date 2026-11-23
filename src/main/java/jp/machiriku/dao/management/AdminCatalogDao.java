package jp.machiriku.dao.management;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** 業種・資格マスターと、業種ごとに定める必要条件を照会・保存する。 */
public final class AdminCatalogDao {
    /** 共通のDAO接続で各データ操作を行うため、このクラスを直接生成しない。 */
    private AdminCatalogDao() { }

    /**
     * 業種またはカテゴリを名称と有効状態で絞り込み、表示順に取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param category 業種処理ではカテゴリを対象とするときtrue。
     * @param keyword 絞り込みに用いる検索語。空文字は指定なし。
     * @param status 保存または絞り込みに指定する状態コード。絞り込みでは空文字で全件。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> industries(ManagementDao db,
            boolean category, String keyword, String status, int limit, int offset) throws SQLException {
        // 種類ごとに定義したSQLから選び、検索条件は値として渡す。
        String query = category ? """
                SELECT category_id AS "ID",category_code AS "コード",category_name AS "名前",display_order AS "表示順",is_active AS "有効"
                FROM industry_categories WHERE strpos(lower(concat_ws(' ',category_code,category_name)),lower(?))>0
                AND is_active::text=COALESCE(NULLIF(?,''),is_active::text) ORDER BY display_order,category_id LIMIT ? OFFSET ?
                """ : """
                SELECT i.industry_type_id AS "ID",i.industry_code AS "コード",i.industry_name AS "名前",c.category_name AS "カテゴリ",
                       i.display_order AS "表示順",i.is_active AS "有効",c.is_active AS "カテゴリ有効"
                FROM industry_types i JOIN industry_categories c USING (category_id)
                WHERE strpos(lower(concat_ws(' ',i.industry_code,i.industry_name,c.category_name)),lower(?))>0
                AND i.is_active::text=COALESCE(NULLIF(?,''),i.is_active::text)
                ORDER BY c.display_order,c.category_id,i.display_order,i.industry_type_id LIMIT ? OFFSET ?
                """;
        return db.rows(query, keyword, status, limit, offset);
    }

    /**
     * 業種またはカテゴリを編集するための現在値を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param category 業種処理ではカテゴリを対象とするときtrue。
     * @param id 操作対象の主キー。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> industry(ManagementDao db, boolean category, int id) throws SQLException {
        return db.one(category ? """
                SELECT category_code AS code,category_name AS name,display_order,is_active FROM industry_categories WHERE category_id=?
                """ : """
                SELECT industry_code AS code,industry_name AS name,category_id,display_order,is_active FROM industry_types WHERE industry_type_id=?
                """, id);
    }

    /**
     * 業種の所属先に指定できるカテゴリを表示順に取得する。
     * @param db 管理操作で共用するDAO接続。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> categoryChoices(ManagementDao db) throws SQLException {
        return db.rows("""
                    SELECT category_id AS id,category_name || CASE WHEN is_active THEN '' ELSE '（無効）' END AS label
                    FROM industry_categories ORDER BY display_order,category_id
                    """);
    }

    /**
     * 業種の所属先として指定されたカテゴリの存在を確認する。
     * @param db 管理操作で共用するDAO接続。
     * @param categoryId 対象となる業種カテゴリのID。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static void requireCategory(ManagementDao db, int categoryId) throws SQLException {
        db.one("SELECT category_id FROM industry_categories WHERE category_id=?", categoryId);
    }

    /**
     * 新しい業種カテゴリを登録し、自動採番したIDを取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param code 保存するマスターコード。
     * @param name 保存する名称。
     * @param displayOrder 表示順を表す正の整数。
     * @param active 有効な状態として保存する場合はtrue。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> insertCategory(ManagementDao db,
            String code, String name, int displayOrder, boolean active) throws SQLException {
        return db.one("INSERT INTO industry_categories (category_code,category_name,display_order,is_active) VALUES (?,?,?,?) RETURNING category_id", code, name, displayOrder, active);
    }

    /**
     * 新しい業種を登録し、自動採番したIDを取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param categoryId 対象となる業種カテゴリのID。
     * @param code 保存するマスターコード。
     * @param name 保存する名称。
     * @param displayOrder 表示順を表す正の整数。
     * @param active 有効な状態として保存する場合はtrue。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> insertIndustry(ManagementDao db,
            int categoryId, String code, String name, int displayOrder, boolean active) throws SQLException {
        return db.one("INSERT INTO industry_types (category_id,industry_code,industry_name,display_order,is_active) VALUES (?,?,?,?,?) RETURNING industry_type_id", categoryId, code, name, displayOrder, active);
    }

    /**
     * 編集前の業種またはカテゴリをロックし、変更前の値を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param category 業種処理ではカテゴリを対象とするときtrue。
     * @param id 操作対象の主キー。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> lockIndustryEntry(ManagementDao db, boolean category, int id) throws SQLException {
        return db.one(category ? "SELECT category_name AS name,display_order,is_active FROM industry_categories WHERE category_id=? FOR UPDATE"
                    : "SELECT industry_name AS name,category_id,display_order,is_active FROM industry_types WHERE industry_type_id=? FOR UPDATE", id);
    }

    /**
     * カテゴリの名称、表示順、有効状態を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param name 保存する名称。
     * @param displayOrder 表示順を表す正の整数。
     * @param active 有効な状態として保存する場合はtrue。
     * @param id 操作対象の主キー。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateCategory(ManagementDao db, String name, int displayOrder, boolean active, int id) throws SQLException {
        db.update("UPDATE industry_categories SET category_name=?,display_order=?,is_active=?,updated_at=CURRENT_TIMESTAMP WHERE category_id=?", name, displayOrder, active, id);
    }

    /**
     * 業種の所属先、名称、表示順、有効状態を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param categoryId 対象となる業種カテゴリのID。
     * @param name 保存する名称。
     * @param displayOrder 表示順を表す正の整数。
     * @param active 有効な状態として保存する場合はtrue。
     * @param id 操作対象の主キー。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateIndustry(ManagementDao db,
            int categoryId, String name, int displayOrder, boolean active, int id) throws SQLException {
        db.update("UPDATE industry_types SET category_id=?,industry_name=?,display_order=?,is_active=?,updated_at=CURRENT_TIMESTAMP WHERE industry_type_id=?", categoryId, name, displayOrder, active, id);
    }

    /**
     * 資格・書類マスターの編集前の値を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param qualificationId 対象となる資格・許認可・書類マスターのID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> qualification(ManagementDao db, int qualificationId) throws SQLException {
        return db.one("""
                    SELECT qualification_name AS name,requirement_kind AS kind,guidance_text,guidance_url,is_active
                    FROM qualifications WHERE qualification_id=?
                    """, qualificationId);
    }

    /**
     * 資格・書類マスターを種別、有効状態、検索語で絞り込む。
     * @param db 管理操作で共用するDAO接続。
     * @param kind 対象や保存する項目を区別する種別コード。
     * @param status 保存または絞り込みに指定する状態コード。絞り込みでは空文字で全件。
     * @param keyword 絞り込みに用いる検索語。空文字は指定なし。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> qualifications(ManagementDao db,
            String kind, String status, String keyword, int limit, int offset) throws SQLException {
        return db.rows("""
                SELECT qualification_id AS "ID",qualification_name AS "名前",requirement_kind AS "種類",guidance_text AS "説明",is_active AS "有効"
                FROM qualifications WHERE requirement_kind=COALESCE(NULLIF(?,''),requirement_kind)
                AND is_active::text=COALESCE(NULLIF(?,''),is_active::text) AND strpos(lower(qualification_name),lower(?))>0
                ORDER BY qualification_name,qualification_id LIMIT ? OFFSET ?
                """, kind, status, keyword, limit, offset);
    }

    /**
     * 必要条件を割り当てる業種をカテゴリと合わせて取得する。
     * @param db 管理操作で共用するDAO接続。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> industryChoices(ManagementDao db) throws SQLException {
        return db.rows("""
                SELECT i.industry_type_id AS id,c.category_name || ' / ' || i.industry_name AS label
                FROM industry_types i JOIN industry_categories c USING (category_id) ORDER BY c.display_order,c.category_id,i.display_order,i.industry_type_id
                """);
    }

    /**
     * 必要条件を割り当てる対象業種の存在を確認する。
     * @param db 管理操作で共用するDAO接続。
     * @param industryId 対象となる業種のID。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static void requireIndustry(ManagementDao db, int industryId) throws SQLException {
        db.one("SELECT industry_type_id FROM industry_types WHERE industry_type_id=?", industryId);
    }

    /**
     * 対象業種にすでに割り当てられている必要資格・書類を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param industryId 対象となる業種のID。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> requirements(ManagementDao db, int industryId, int limit, int offset) throws SQLException {
        return db.rows("""
                SELECT q.qualification_id AS "ID",q.qualification_name AS "名前",q.requirement_kind AS "種類",
                       req.is_required AS "必須",req.condition_text AS "適用条件",q.guidance_url AS "案内URL",q.is_active AS "有効"
                FROM industry_qualification_requirements req JOIN qualifications q USING (qualification_id)
                WHERE req.industry_type_id=? ORDER BY req.is_required DESC,q.qualification_name,q.qualification_id LIMIT ? OFFSET ?
                """, industryId, limit, offset);
    }

    /**
     * 指定した業種と資格の組に設定済みの必要条件を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param industryId 対象となる業種のID。
     * @param qualificationId 対象となる資格・許認可・書類マスターのID。
     * @return 設定済みの必須区分と適用条件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> requirement(ManagementDao db, int industryId, int qualificationId) throws SQLException {
        return db.one("""
                SELECT is_required,condition_text FROM industry_qualification_requirements WHERE industry_type_id=? AND qualification_id=?
                """, industryId, qualificationId);
    }

    /**
     * 有効な資格と、対象業種に割り当て済みの資格を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param industryId 対象となる業種のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> requirementChoices(ManagementDao db, int industryId) throws SQLException {
        return db.rows("""
                SELECT q.qualification_id AS id,q.qualification_name || '（' || q.requirement_kind || '）' AS label FROM qualifications q
                WHERE q.is_active OR EXISTS (SELECT 1 FROM industry_qualification_requirements r WHERE r.industry_type_id=? AND r.qualification_id=q.qualification_id)
                ORDER BY q.qualification_name,q.qualification_id
                """, industryId);
    }

    /**
     * 新しい資格・書類マスターを登録し、生成したIDを取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param name 保存する名称。
     * @param kind 対象や保存する項目を区別する種別コード。
     * @param guidance 利用者へ案内する説明文。省略時はnull。
     * @param url 案内または公開情報のURL。省略時はnull。
     * @param active 有効な状態として保存する場合はtrue。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> insertQualification(ManagementDao db,
            String name, String kind, String guidance, String url, boolean active) throws SQLException {
        return db.one("INSERT INTO qualifications (qualification_name,requirement_kind,guidance_text,guidance_url,is_active) VALUES (?,?,?,?,?) RETURNING qualification_id", name, kind, guidance, url, active);
    }

    /**
     * 資格の種別や有効状態を書き換える前に行をロックする。
     * @param db 管理操作で共用するDAO接続。
     * @param qualificationId 対象となる資格・許認可・書類マスターのID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> lockQualification(ManagementDao db, int qualificationId) throws SQLException {
        return db.one("SELECT qualification_name,requirement_kind,is_active FROM qualifications WHERE qualification_id=? FOR UPDATE", qualificationId);
    }

    /**
     * 事業主の保有資格としてすでに参照されているか確認する。
     * @param db 管理操作で共用するDAO接続。
     * @param qualificationId 対象となる資格・許認可・書類マスターのID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> businessQualificationUsage(ManagementDao db, int qualificationId) throws SQLException {
        return db.rows("SELECT qualification_id FROM business_qualifications WHERE qualification_id=? LIMIT 1", qualificationId);
    }

    /**
     * 資格・書類マスターの案内と有効状態を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param name 保存する名称。
     * @param kind 対象や保存する項目を区別する種別コード。
     * @param guidance 利用者へ案内する説明文。省略時はnull。
     * @param url 案内または公開情報のURL。省略時はnull。
     * @param active 有効な状態として保存する場合はtrue。
     * @param qualificationId 対象となる資格・許認可・書類マスターのID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateQualification(ManagementDao db,
            String name, String kind, String guidance, String url, boolean active, int qualificationId) throws SQLException {
        db.update("UPDATE qualifications SET qualification_name=?,requirement_kind=?,guidance_text=?,guidance_url=?,is_active=? WHERE qualification_id=?", name, kind, guidance, url, active, qualificationId);
    }

    /**
     * 同じ業種の必要条件を同時に更新できないように業種行をロックする。
     * @param db 管理操作で共用するDAO接続。
     * @param industryId 対象となる業種のID。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static void lockIndustry(ManagementDao db, int industryId) throws SQLException {
        db.one("SELECT industry_type_id FROM industry_types WHERE industry_type_id=? FOR UPDATE", industryId);
    }

    /**
     * 指定した業種と資格の組の必要条件をロックして取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param industryId 対象となる業種のID。
     * @param qualificationId 対象となる資格・許認可・書類マスターのID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> lockRequirement(ManagementDao db, int industryId, int qualificationId) throws SQLException {
        return db.rows("""
                SELECT is_required,condition_text FROM industry_qualification_requirements WHERE industry_type_id=? AND qualification_id=? FOR UPDATE
                """, industryId, qualificationId);
    }

    /**
     * 指定した業種と資格・書類の関連付けを解除する。
     * @param db 管理操作で共用するDAO接続。
     * @param industryId 対象となる業種のID。
     * @param qualificationId 対象となる資格・許認可・書類マスターのID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void deleteRequirement(ManagementDao db, int industryId, int qualificationId) throws SQLException {
        db.update("DELETE FROM industry_qualification_requirements WHERE industry_type_id=? AND qualification_id=?", industryId, qualificationId);
    }

    /**
     * 新しい必要条件に指定できる有効な資格を確認し、行をロックする。
     * @param db 管理操作で共用するDAO接続。
     * @param qualificationId 対象となる資格・許認可・書類マスターのID。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static void lockActiveQualification(ManagementDao db, int qualificationId) throws SQLException {
        db.one("SELECT qualification_id FROM qualifications WHERE qualification_id=? AND is_active FOR SHARE", qualificationId);
    }

    /**
     * 業種に必要な資格・書類と、必須区分や条件を登録または更新する。
     * @param db 管理操作で共用するDAO接続。
     * @param industryId 対象となる業種のID。
     * @param qualificationId 対象となる資格・許認可・書類マスターのID。
     * @param required 必須として扱う場合はtrue。
     * @param condition 適用される条件。条件を指定しない場合はnull。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void upsertRequirement(ManagementDao db,
            int industryId, int qualificationId, boolean required, String condition) throws SQLException {
        db.update("""
                    INSERT INTO industry_qualification_requirements (industry_type_id,qualification_id,is_required,condition_text) VALUES (?,?,?,?)
                    ON CONFLICT (industry_type_id,qualification_id) DO UPDATE SET is_required=EXCLUDED.is_required,condition_text=EXCLUDED.condition_text
                    """, industryId, qualificationId, required, condition);
    }
}
