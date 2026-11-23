package jp.machiriku.dao.management;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** クーポンの配布設定、発行履歴、個別の無効化を管理する。 */
public final class AdminCouponDao {
    /** 共通のDAO接続で各データ操作を行うため、このクラスを直接生成しない。 */
    private AdminCouponDao() { }

    /**
     * クーポンの配布情報、関連する店舗と発行・使用件数を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param couponId クーポンのID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> coupon(ManagementDao db, int couponId) throws SQLException {
        return db.one("""
                SELECT c.coupon_id AS "クーポン番号",c.coupon_title AS "名前",t.store_name AS "店舗",c.tenancy_id AS "契約ID",
                       a.business_user_id AS "事業主ID",c.coupon_status AS "配布状態",c.benefit_kind AS "特典",
                       c.discount_percent AS "割引率（%）",c.discount_yen AS "割引額（円）",c.service_text AS "サービス内容",
                       c.target_kind AS "配布対象",i.industry_name AS "対象業種",c.target_survey_id AS "アンケートID",
                       c.distribution_starts_at AS "配布開始",c.distribution_ends_at AS "配布終了",c.valid_until AS "利用期限",
                       c.usage_limit AS "発行上限",c.terms_text AS "利用条件",count(r.resident_user_id) AS "発行数",
                       count(r.used_at) AS "利用数",count(r.revoked_at) AS "無効化数"
                FROM coupons c JOIN tenancies t USING (tenancy_id) JOIN applications a USING (application_id)
                LEFT JOIN industry_types i ON i.industry_type_id=c.target_industry_type_id
                LEFT JOIN coupon_recipients r USING (coupon_id)
                WHERE c.coupon_id=? GROUP BY c.coupon_id,t.store_name,a.business_user_id,i.industry_name
                """, couponId);
    }

    /**
     * クーポンを配布状態と検索語で絞り込む。
     * @param db 管理操作で共用するDAO接続。
     * @param status 保存または絞り込みに指定する状態コード。絞り込みでは空文字で全件。
     * @param keyword 絞り込みに用いる検索語。空文字は指定なし。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> coupons(ManagementDao db,
            String status, String keyword, int limit, int offset) throws SQLException {
        return db.rows("""
                SELECT c.coupon_id AS "ID",c.coupon_title AS "名前",t.store_name AS "店舗",c.coupon_status AS "配布状態",
                       count(r.resident_user_id) AS "発行数",count(r.used_at) AS "利用数",count(r.revoked_at) AS "無効化数",
                       c.valid_until AS "利用期限"
                FROM coupons c JOIN tenancies t USING (tenancy_id) LEFT JOIN coupon_recipients r USING (coupon_id)
                WHERE c.coupon_status=COALESCE(NULLIF(?,''),c.coupon_status)
                  AND strpos(lower(concat_ws(' ',c.coupon_id,c.coupon_title,t.store_name)),lower(?))>0
                GROUP BY c.coupon_id,t.store_name ORDER BY c.updated_at DESC,c.coupon_id DESC LIMIT ? OFFSET ?
                """, status, keyword, limit, offset);
    }

    /**
     * クーポンの発行記録を発行日時の新しい順に取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param couponId クーポンのID。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> recipients(ManagementDao db, int couponId, int limit, int offset) throws SQLException {
        return db.rows("""
                SELECT r.resident_user_id AS "会員ID",u.display_name AS "受取人",r.issued_at AS "発行日時",r.used_at AS "利用日時",
                       r.revoked_at AS "無効化日時",r.redeemed_by_user_id AS "利用処理者ID",
                       CASE WHEN r.used_at IS NOT NULL THEN '利用済み' WHEN r.revoked_at IS NOT NULL THEN '無効化済み'
                            WHEN c.valid_until<=CURRENT_TIMESTAMP THEN '期限切れ' ELSE '未使用' END AS "利用状態"
                FROM coupon_recipients r JOIN users u ON u.user_id=r.resident_user_id JOIN coupons c USING (coupon_id)
                WHERE r.coupon_id=? ORDER BY r.issued_at DESC,r.resident_user_id DESC LIMIT ? OFFSET ?
                """, couponId, limit, offset);
    }

    /**
     * 一件の発行記録と現在の利用・有効期限を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param couponId クーポンのID。
     * @param recipientId クーポンを受け取った会員のID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> recipient(ManagementDao db, int couponId, int recipientId) throws SQLException {
        return db.one("""
                SELECT r.resident_user_id AS "会員ID",u.display_name AS "受取人",r.issued_at AS "発行日時",r.used_at AS "利用日時",
                       r.revoked_at AS "無効化日時",c.valid_until>CURRENT_TIMESTAMP AS "有効期間内"
                FROM coupon_recipients r JOIN users u ON u.user_id=r.resident_user_id JOIN coupons c USING (coupon_id)
                WHERE r.coupon_id=? AND r.resident_user_id=?
                """, couponId, recipientId);
    }

    /**
     * 配布設定を変更する前にクーポン行をロックする。
     * @param db 管理操作で共用するDAO接続。
     * @param couponId クーポンのID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> lockCoupon(ManagementDao db, int couponId) throws SQLException {
        return db.one("SELECT coupon_status,distribution_ends_at>CURRENT_TIMESTAMP AS in_period FROM coupons WHERE coupon_id=? FOR UPDATE", couponId);
    }

    /**
     * 配布設定の変更可否を判断するために、すでに発行されているか確認する。
     * @param db 管理操作で共用するDAO接続。
     * @param couponId クーポンのID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> issuedCouponKeys(ManagementDao db, int couponId) throws SQLException {
        return db.rows("SELECT 1 FROM coupon_recipients WHERE coupon_id=? LIMIT 1", couponId);
    }

    /**
     * 有効期間内で未使用のクーポン発行分を無効化する。
     * @param db 管理操作で共用するDAO接続。
     * @param couponId クーポンのID。
     * @param recipientId クーポンを受け取った会員のID。
     * @return 更新した件数。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static int revokeRecipient(ManagementDao db, int couponId, int recipientId) throws SQLException {
        return db.update("""
                    UPDATE coupon_recipients r SET revoked_at=CURRENT_TIMESTAMP FROM coupons c
                    WHERE r.coupon_id=c.coupon_id AND r.coupon_id=? AND r.resident_user_id=?
                      AND r.used_at IS NULL AND r.revoked_at IS NULL AND c.valid_until>CURRENT_TIMESTAMP
                      AND r.issued_at<=CURRENT_TIMESTAMP
                    """, couponId, recipientId);
    }

    /**
     * クーポンの配布停止または再開を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param status 保存または絞り込みに指定する状態コード。絞り込みでは空文字で全件。
     * @param couponId クーポンのID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateState(ManagementDao db, String status, int couponId) throws SQLException {
        db.update("UPDATE coupons SET coupon_status=?,updated_at=CURRENT_TIMESTAMP WHERE coupon_id=?", status, couponId);
    }

    /**
     * 配布開始前のクーポンの特典、期間、上限を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param title クーポンやアンケートの名称。
     * @param kind 対象や保存する項目を区別する種別コード。
     * @param percent 割合割引の場合の割引率。
     * @param amount 定額割引の場合の割引金額。
     * @param service サービス特典の案内。
     * @param starts 受付・配布を開始する日時。
     * @param ends 受付・配布を終了する日時。
     * @param validUntil クーポンを使用できる期限。
     * @param usageLimit 発行枚数の上限。
     * @param terms クーポンの使用条件。省略時はnull。
     * @param couponId クーポンのID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateDraft(ManagementDao db,
            String title, String kind, BigDecimal percent, Integer amount, String service, OffsetDateTime starts, OffsetDateTime ends, OffsetDateTime validUntil, int usageLimit, String terms, int couponId) throws SQLException {
        db.update("""
                UPDATE coupons SET coupon_title=?,benefit_kind=?,discount_percent=?,discount_yen=?,service_text=?,
                       distribution_starts_at=?,distribution_ends_at=?,valid_until=?,usage_limit=?,terms_text=?,updated_at=CURRENT_TIMESTAMP
                WHERE coupon_id=?
                """, title, kind, percent, amount, service, starts, ends, validUntil, usageLimit, terms, couponId);
    }
}
