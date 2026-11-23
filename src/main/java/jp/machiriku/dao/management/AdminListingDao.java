package jp.machiriku.dao.management;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** 管理者が扱う物件・契約の情報を照会し、掲載の状態を更新する。 */
public final class AdminListingDao {
    /** 共通のDAO接続で各データ操作を行うため、このクラスを直接生成しない。 */
    private AdminListingDao() { }

    /**
     * 物件の掲載内容と管理状態を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param propertyId 対象または関連する物件のID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> property(ManagementDao db, int propertyId) throws SQLException {
        return db.one("""
                SELECT p.property_id AS "物件ID",p.property_name AS "名前",p.owner_user_id AS "掲載者ID",
                       COALESCE(o.company_name,o.representative_name,u.display_name) AS "掲載者",p.prefecture AS "都道府県",p.municipality AS "市区町村",
                       p.address_line AS "番地",p.latitude AS "緯度",p.longitude AS "経度",p.floor_area_sqm AS "面積（㎡）",p.monthly_rent_yen AS "月額賃料（円）",
                       p.description AS "説明",p.listing_url AS "掲載URL",p.permit_status AS "許認可",p.listing_status AS "募集・契約状態",
                       p.admin_hidden_at AS "掲載停止日時",p.deleted_at AS "削除日時"
                FROM properties p JOIN users u ON u.user_id=p.owner_user_id LEFT JOIN owner_profiles o ON o.user_id=u.user_id WHERE p.property_id=?
                """, propertyId);
    }

    /**
     * 物件画像の保存先を表示順に取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param propertyId 対象または関連する物件のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> propertyImages(ManagementDao db, int propertyId) throws SQLException {
        return db.rows("SELECT display_order AS \"表示順\",storage_key AS \"画像\",alt_text AS \"説明\" FROM property_images WHERE property_id=? ORDER BY display_order,image_id", propertyId);
    }

    /**
     * 物件に設定された業種を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param propertyId 対象または関連する物件のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> propertyIndustries(ManagementDao db, int propertyId) throws SQLException {
        return db.rows("""
                SELECT i.industry_type_id AS "業種ID",i.industry_name AS "業種"
                FROM property_industries p JOIN industry_types i USING (industry_type_id) WHERE p.property_id=? ORDER BY i.display_order,i.industry_type_id
                """, propertyId);
    }

    /**
     * 物件に設定された契約期間・賃料の条件を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param propertyId 対象または関連する物件のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> propertyContractTerms(ManagementDao db, int propertyId) throws SQLException {
        return db.rows("""
                SELECT contract_kind AS "契約区分",min_days AS "最短日数",max_days AS "最長日数",daily_rent_yen AS "日額賃料（円）"
                FROM property_contract_terms WHERE property_id=? ORDER BY contract_kind
                """, propertyId);
    }

    /**
     * 募集・掲載状態と検索語で物件を絞り込む。
     * @param db 管理操作で共用するDAO接続。
     * @param status 保存または絞り込みに指定する状態コード。絞り込みでは空文字で全件。
     * @param visibility 一覧の絞り込み条件または投稿の公開範囲。
     * @param keyword 絞り込みに用いる検索語。空文字は指定なし。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> properties(ManagementDao db,
            String status, String visibility, String keyword, int limit, int offset) throws SQLException {
        return db.rows("""
                SELECT p.property_id AS "ID",p.property_name AS "名前",concat_ws('',p.prefecture,p.municipality,p.address_line) AS "所在地",
                       COALESCE(o.company_name,o.representative_name,u.display_name) AS "オーナー",p.listing_status AS "募集・契約状態",
                       CASE WHEN p.deleted_at IS NOT NULL THEN '削除済み' WHEN p.admin_hidden_at IS NOT NULL THEN '掲載停止中' ELSE '通常' END AS "管理状態",
                       (SELECT count(*) FROM applications a WHERE a.property_id=p.property_id) AS "申込件数"
                FROM properties p JOIN users u ON u.user_id=p.owner_user_id LEFT JOIN owner_profiles o ON o.user_id=u.user_id
                WHERE p.listing_status=COALESCE(NULLIF(?,''),p.listing_status)
                AND (CASE WHEN p.admin_hidden_at IS NOT NULL THEN 'HIDDEN' ELSE 'NORMAL' END)=COALESCE(NULLIF(?,''),CASE WHEN p.admin_hidden_at IS NOT NULL THEN 'HIDDEN' ELSE 'NORMAL' END)
                AND strpos(lower(concat_ws(' ',p.property_id,p.property_name,p.prefecture,p.municipality,p.address_line,u.display_name,o.company_name,o.representative_name)),lower(?))>0
                ORDER BY p.updated_at DESC,p.property_id DESC LIMIT ? OFFSET ?
                """, status, visibility, keyword, limit, offset);
    }

    /**
     * 物件に対する出店申込、オファー、通報のいずれかを取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param tab 取得する履歴の種類。
     * @param propertyId 対象または関連する物件のID。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> propertyHistory(ManagementDao db,
            String tab, int propertyId, int limit, int offset) throws SQLException {
        // 種類ごとに定義したSQLから選び、検索条件は値として渡す。
        String query = switch (tab) {
            case "applications" -> """
                    SELECT a.application_id AS "申込ID",a.business_user_id AS "事業主ID",b.business_name AS "事業者名",a.application_status AS "申込状態",
                           t.tenancy_id AS "契約ID",t.tenancy_status AS "契約状態",a.created_at AS "申込日時"
                    FROM applications a JOIN business_profiles b ON b.user_id=a.business_user_id LEFT JOIN tenancies t USING (application_id)
                    WHERE a.property_id=? ORDER BY a.created_at DESC,a.application_id DESC
                    """;
            case "offers" -> """
                    SELECT o.offer_id AS "オファーID",o.business_user_id AS "事業主ID",b.business_name AS "事業者名",o.offer_status AS "状態",o.sent_at AS "送信日時"
                    FROM offers o JOIN business_profiles b ON b.user_id=o.business_user_id WHERE o.property_id=? ORDER BY o.sent_at DESC,o.offer_id DESC
                    """;
            default -> """
                    SELECT report_id AS "通報ID",report_status AS "対応状態",reason_code AS "理由",created_at AS "受付日時"
                    FROM reports WHERE target_property_id=? ORDER BY created_at DESC,report_id DESC
                    """;
        };
        return db.rows(query + " LIMIT ? OFFSET ?", propertyId, limit, offset);
    }

    /**
     * 募集の状態や掲載内容を変更するときに物件行をロックする。
     * @param db 管理操作で共用するDAO接続。
     * @param propertyId 対象または関連する物件のID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> lockProperty(ManagementDao db, int propertyId) throws SQLException {
        return db.one("SELECT listing_status,admin_hidden_at,deleted_at FROM properties WHERE property_id=? FOR UPDATE", propertyId);
    }

    /**
     * 対象物件の募集を公開する。
     * @param db 管理操作で共用するDAO接続。
     * @param propertyId 対象または関連する物件のID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void publishProperty(ManagementDao db, int propertyId) throws SQLException {
        db.update("UPDATE properties SET listing_status='OPEN',updated_at=CURRENT_TIMESTAMP WHERE property_id=?", propertyId);
    }

    /**
     * 掲載停止を変更する前に物件行と削除状態を確認する。
     * @param db 管理操作で共用するDAO接続。
     * @param propertyId 対象または関連する物件のID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> lockPropertyVisibility(ManagementDao db, int propertyId) throws SQLException {
        return db.one("SELECT admin_hidden_at,deleted_at FROM properties WHERE property_id=? FOR UPDATE", propertyId);
    }

    /**
     * 物件の掲載停止日時を設定または解除する。
     * @param db 管理操作で共用するDAO接続。
     * @param hidden 運営側で掲載を停止する場合はtrue。
     * @param propertyId 対象または関連する物件のID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateVisibility(ManagementDao db, boolean hidden, int propertyId) throws SQLException {
        db.update("UPDATE properties SET admin_hidden_at=CASE WHEN ? THEN CURRENT_TIMESTAMP END,updated_at=CURRENT_TIMESTAMP WHERE property_id=?", hidden, propertyId);
    }

    /**
     * 物件の住所、座標、賃料などの掲載内容を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param name 保存する名称。
     * @param prefecture 都道府県名。
     * @param municipality 市区町村名。
     * @param address 番地や建物名を含む住所。
     * @param latitude 緯度。座標未指定の場合はnull。
     * @param longitude 経度。座標未指定の場合はnull。
     * @param floorArea 物件の面積。単位は平方メートル。
     * @param rent 物件の月額賃料。単位は円。
     * @param description 掲載や通報の説明文。
     * @param url 案内または公開情報のURL。省略時はnull。
     * @param permit 物件の許認可状態コード。
     * @param propertyId 対象または関連する物件のID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateProperty(ManagementDao db,
            String name, String prefecture, String municipality, String address, BigDecimal latitude, BigDecimal longitude, BigDecimal floorArea, int rent, String description, String url, String permit, int propertyId) throws SQLException {
        db.update("""
                UPDATE properties SET property_name=?,prefecture=?,municipality=?,address_line=?,latitude=?,longitude=?,floor_area_sqm=?,
                    monthly_rent_yen=?,description=?,listing_url=?,permit_status=?,updated_at=CURRENT_TIMESTAMP WHERE property_id=?
                """, name, prefecture, municipality, address, latitude, longitude, floorArea, rent, description, url, permit, propertyId);
    }

    /**
     * 出店申込、契約、出店計画またはオファーの明細を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param recordType 取得する関連記録の種類。
     * @param id 操作対象の主キー。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> relatedRecord(ManagementDao db, String recordType, int id) throws SQLException {
        // 種類ごとに定義したSQLから選び、検索条件は値として渡す。
        String query = switch (recordType) {
            case "applications" -> """
                    SELECT a.application_id AS "申込番号",a.property_id AS "物件ID",p.property_name AS "物件",p.owner_user_id AS "オーナーID",a.business_user_id AS "事業主ID",
                           a.application_status AS "状態",i.industry_name AS "申込業種",a.contract_kind AS "契約区分",a.requested_start_on AS "希望開始日",
                           a.requested_end_on AS "希望終了日",a.viewing_requested_at AS "内覧希望日時",a.message AS "申込内容",a.owner_reply AS "オーナーの返信",
                           a.replied_at AS "返信日時",a.created_at AS "申込日時",t.tenancy_id AS "契約ID"
                    FROM applications a JOIN properties p USING (property_id) JOIN industry_types i USING (industry_type_id) LEFT JOIN tenancies t USING (application_id)
                    WHERE a.application_id=?
                    """;
            case "tenancies" -> """
                    SELECT t.tenancy_id AS "契約番号",t.application_id AS "申込ID",a.property_id AS "物件ID",p.property_name AS "物件",p.owner_user_id AS "オーナーID",
                           a.business_user_id AS "事業主ID",t.store_name AS "店舗",t.tenancy_status AS "契約状態",t.contract_start_on AS "契約開始日",t.contract_end_on AS "契約終了日",
                           t.confirmed_at AS "契約確定日時",t.owner_handover_at AS "オーナーの引渡し",t.business_handover_at AS "事業主の受け取り",t.opened_on AS "開店日"
                    FROM tenancies t JOIN applications a USING (application_id) JOIN properties p USING (property_id) WHERE t.tenancy_id=?
                    """;
            case "plans" -> """
                    SELECT op.plan_id AS "計画番号",op.tenancy_id AS "契約ID",t.application_id AS "申込ID",a.property_id AS "物件ID",a.business_user_id AS "事業主ID",
                           op.title AS "計画名",op.body AS "内容",op.planned_open_on AS "開店予定日",op.progress_status AS "進捗",
                           op.publication_status AS "公開状態",op.published_at AS "公開日時",op.updated_at AS "更新日時"
                    FROM opening_plans op JOIN tenancies t USING (tenancy_id) JOIN applications a USING (application_id) WHERE op.plan_id=?
                    """;
            default -> """
                    SELECT o.offer_id AS "オファー番号",o.property_id AS "物件ID",p.property_name AS "物件",o.owner_user_id AS "オーナーID",owner.display_name AS "送信者",
                           o.business_user_id AS "事業主ID",business.business_name AS "受信者",o.creation_kind AS "作成方法",o.message AS "内容",
                           o.offer_status AS "状態",o.sent_at AS "送信日時",o.responded_at AS "返答日時"
                    FROM offers o JOIN properties p USING (property_id) JOIN users owner ON owner.user_id=o.owner_user_id
                        JOIN business_profiles business ON business.user_id=o.business_user_id WHERE o.offer_id=?
                    """;
        };
        return db.one(query, id);
    }

    /**
     * 申し込み当時の必要資格・書類の名称と条件を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param applicationId 出店申し込みのID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> applicationChecks(ManagementDao db, int applicationId) throws SQLException {
        return db.rows("""
                    SELECT requirement_name_snapshot AS "要件名",is_required_snapshot AS "必須",condition_text_snapshot AS "適用条件",
                           business_confirmed AS "事業主による確認",owner_check_status AS "オーナー確認状態",check_note AS "確認メモ"
                    FROM application_checks WHERE application_id=? ORDER BY qualification_id
                    """, applicationId);
    }

    /**
     * 契約に紐づく出店計画の一覧を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param tenancyId 契約のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> openingPlans(ManagementDao db, int tenancyId) throws SQLException {
        return db.rows("SELECT plan_id AS \"計画ID\",title AS \"計画名\",progress_status AS \"進捗\" FROM opening_plans WHERE tenancy_id=? ORDER BY updated_at DESC LIMIT 25", tenancyId);
    }

    /**
     * 公開中の出店計画を公開終了状態にする。
     * @param db 管理操作で共用するDAO接続。
     * @param planId 出店計画のID。
     * @return 更新した件数。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static int archivePlan(ManagementDao db, int planId) throws SQLException {
        return db.update("UPDATE opening_plans SET publication_status='ARCHIVED',updated_at=CURRENT_TIMESTAMP WHERE plan_id=? AND publication_status='PUBLISHED'", planId);
    }
}
