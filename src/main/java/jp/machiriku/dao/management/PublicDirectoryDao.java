package jp.machiriku.dao.management;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** 一般公開の案内と、利用者が閲覧を許可された物件・事業主を取得する。 */
public final class PublicDirectoryDao {
    /** 募集・契約状態に管理者による掲載停止を重ねた公開条件。 */
    private static final String VISIBLE_PROPERTY = "p.deleted_at IS NULL AND p.admin_hidden_at IS NULL AND p.listing_status IN ('OPEN','CONTRACTED')";
    /** 店舗を営業中の事業主か、閲覧者の物件に対する申込・オファーで関係する事業主を表示する。 */
    private static final String VISIBLE_BUSINESS = """
            u.account_status='ACTIVE' AND u.withdrawn_at IS NULL AND u.user_role='BUSINESS'
            AND (EXISTS (SELECT 1 FROM applications a JOIN tenancies t ON t.application_id=a.application_id
                        JOIN properties p ON p.property_id=a.property_id
                        WHERE a.business_user_id=b.user_id AND t.tenancy_status='OPEN' AND
            """ + VISIBLE_PROPERTY + """
                ) OR EXISTS (SELECT 1 FROM applications a JOIN properties p ON p.property_id=a.property_id
                            WHERE a.business_user_id=b.user_id AND p.owner_user_id=?)
                  OR EXISTS (SELECT 1 FROM offers o WHERE o.business_user_id=b.user_id AND o.owner_user_id=? AND o.offer_status<>'DRAFT'))
            """;

    /** 共通のDAO接続で各データ操作を行うため、このクラスを直接生成しない。 */
    private PublicDirectoryDao() { }

    /**
     * 公開中の物件を名称や所在地で絞り込んで取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param keyword 絞り込みに用いる検索語。空文字は指定なし。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> properties(ManagementDao db, String keyword, int limit, int offset) throws SQLException {
        return db.rows("""
                    SELECT p.property_id AS "ID",p.property_name AS "名前",
                           concat_ws('',p.prefecture,p.municipality,p.address_line) AS "所在地",
                           p.floor_area_sqm AS "面積（㎡）",p.monthly_rent_yen AS "月額賃料（円）"
                    FROM properties p WHERE
                    """ + VISIBLE_PROPERTY + """
                    AND strpos(lower(concat_ws(' ',p.property_name,p.prefecture,p.municipality,p.address_line)),lower(?))>0
                    ORDER BY p.updated_at DESC,p.property_id DESC LIMIT ? OFFSET ?
                    """, keyword, limit, offset);
    }

    /**
     * 公開条件を満たす一件の物件と掲載者情報を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param propertyId 対象または関連する物件のID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> property(ManagementDao db, int propertyId) throws SQLException {
        return db.one("""
                SELECT p.property_id AS "ID",p.property_name AS "名前",p.owner_user_id AS "掲載者ID",
                       COALESCE(NULLIF(o.company_name,''),NULLIF(o.representative_name,''),u.display_name) AS "掲載者",
                       concat_ws('',p.prefecture,p.municipality,p.address_line) AS "所在地",
                       p.floor_area_sqm AS "面積（㎡）",p.monthly_rent_yen AS "月額賃料（円）",p.description AS "説明"
                FROM properties p JOIN users u ON u.user_id=p.owner_user_id LEFT JOIN owner_profiles o ON o.user_id=u.user_id
                WHERE p.property_id=? AND
                """ + VISIBLE_PROPERTY, propertyId);
    }

    /**
     * 閲覧者が公開情報または取引の経緯から参照できる事業主を検索する。
     * @param db 管理操作で共用するDAO接続。
     * @param viewerId 公開範囲を判定するための閲覧者の会員ID。
     * @param keyword 絞り込みに用いる検索語。空文字は指定なし。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> businesses(ManagementDao db,
            int viewerId, String keyword, int limit, int offset) throws SQLException {
        return db.rows("""
                    SELECT b.user_id AS "ID", b.business_name AS "名前", b.preferred_area AS "希望エリア"
                    FROM business_profiles b JOIN users u ON u.user_id=b.user_id WHERE
                    """ + VISIBLE_BUSINESS + """
                    AND strpos(lower(b.business_name),lower(?))>0 ORDER BY b.business_name,b.user_id LIMIT ? OFFSET ?
                    """, viewerId, viewerId, keyword, limit, offset);
    }

    /**
     * 対象の事業主を閲覧者が参照できる条件で取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param businessId 事業主の会員ID。
     * @param viewerId 公開範囲を判定するための閲覧者の会員ID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> business(ManagementDao db, int businessId, int viewerId) throws SQLException {
        return db.one("""
                SELECT b.user_id AS "ID",b.business_name AS "名前",b.preferred_area AS "希望エリア",b.industry_experience AS "業界経験"
                FROM business_profiles b JOIN users u ON u.user_id=b.user_id WHERE b.user_id=? AND
                """ + VISIBLE_BUSINESS, businessId, viewerId, viewerId);
    }

    /**
     * 対象会員が指定された公開物件を掲載したオーナーであることを確認する。
     * @param db 管理操作で共用するDAO接続。
     * @param propertyId 対象または関連する物件のID。
     * @param ownerId 物件を掲載したオーナーの会員ID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> listedOwner(ManagementDao db, int propertyId, int ownerId) throws SQLException {
        return db.one("""
                SELECT COALESCE(NULLIF(o.company_name,''),o.representative_name) AS "掲載者",p.property_name AS "関連物件"
                FROM properties p JOIN owner_profiles o ON o.user_id=p.owner_user_id JOIN users u ON u.user_id=o.user_id
                WHERE p.property_id=? AND p.owner_user_id=? AND u.user_role='OWNER' AND
                """ + VISIBLE_PROPERTY, propertyId, ownerId);
    }

    /**
     * 必要資格・書類の公開案内に表示する有効な業種を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> industryChoices(ManagementDao db) throws SQLException {
        return db.rows("""
                SELECT i.industry_type_id AS id,concat(c.category_name,' / ',i.industry_name) AS label
                FROM industry_types i JOIN industry_categories c USING (category_id)
                WHERE i.is_active AND c.is_active ORDER BY c.display_order,c.category_id,i.display_order,i.industry_type_id
                """);
    }

    /**
     * 公開中の業種別の必要資格・書類と案内を表示順に取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param industryId 対象となる業種のID。未指定の場合はすべての業種。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> requirements(ManagementDao db, Integer industryId, int limit, int offset) throws SQLException {
        return db.rows("""
                SELECT i.industry_name AS "業種",q.qualification_name AS "名称",
                       CASE q.requirement_kind WHEN 'DOCUMENT' THEN '書類' WHEN 'PERMIT' THEN '許認可' ELSE '資格' END AS "種別",
                       CASE WHEN r.is_required THEN '必須' ELSE '任意' END AS "必要区分",
                       r.condition_text AS "適用条件",q.guidance_text AS "案内",q.guidance_url AS "案内URL"
                FROM industry_qualification_requirements r JOIN qualifications q USING (qualification_id)
                JOIN industry_types i USING (industry_type_id) JOIN industry_categories c USING (category_id)
                WHERE i.is_active AND c.is_active AND q.is_active AND (?::integer IS NULL OR i.industry_type_id=?)
                ORDER BY c.display_order,c.category_id,i.display_order,i.industry_type_id,r.is_required DESC,q.qualification_id LIMIT ? OFFSET ?
                """, industryId, industryId, limit, offset);
    }
}
