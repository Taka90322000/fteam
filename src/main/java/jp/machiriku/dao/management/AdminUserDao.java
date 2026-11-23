package jp.machiriku.dao.management;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 利用区分別の会員情報と活動履歴を照会し、プロフィールや会員状態を保存する。 */
public final class AdminUserDao {
    /**
     * 利用区分に対応する固定のプロフィール表を選ぶ。
     * @param role DB上の利用区分。
     * @return SQLに使用できる表名。
     */
    private static String profileTable(String role) {
        return switch (role) { case "RESIDENT" -> "resident_profiles"; case "BUSINESS" -> "business_profiles"; case "OWNER" -> "owner_profiles";
            default -> throw new IllegalArgumentException("利用区分が正しくありません。"); };
    }

    /** 共通のDAO接続で各データ操作を行うため、このクラスを直接生成しない。 */
    private AdminUserDao() { }

    /**
     * 希望業種や区分別プロフィールを表示する対象の会員を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> member(ManagementDao db, int userId) throws SQLException {
        return db.one("""
                SELECT user_id AS "会員ID",display_name AS "名前",email AS "メール", user_role AS "区分",
                       account_status AS "利用状態",created_at AS "登録日",updated_at AS "更新日",withdrawn_at AS "退会日時"
                FROM users WHERE user_id=?
                """, userId);
    }

    /**
     * 利用区分、状態、登録日、検索語で会員を絞り込む。
     * @param db 管理操作で共用するDAO接続。
     * @param role プロフィールや一覧の対象となる利用区分。
     * @param status 保存または絞り込みに指定する状態コード。絞り込みでは空文字で全件。
     * @param from 対象期間の開始日。未指定の場合はnull。
     * @param to 対象期間の終了日。未指定の場合はnull。
     * @param keyword 絞り込みに用いる検索語。空文字は指定なし。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> members(ManagementDao db,
            String role, String status, LocalDate from, LocalDate to, String keyword, int limit, int offset) throws SQLException {
        return db.rows("""
                SELECT u.user_id AS "ID",u.display_name AS "名前",COALESCE(b.business_name,o.company_name) AS "事業者名・法人名",
                       u.email AS "メール",u.account_status AS "利用状態",u.created_at AS "登録日"
                FROM users u LEFT JOIN business_profiles b ON b.user_id=u.user_id LEFT JOIN owner_profiles o ON o.user_id=u.user_id
                WHERE u.user_role=? AND u.account_status=COALESCE(NULLIF(?,''),u.account_status)
                  AND (u.created_at AT TIME ZONE 'Asia/Tokyo')::date >= COALESCE(?::date,'-infinity'::date)
                  AND (u.created_at AT TIME ZONE 'Asia/Tokyo')::date <= COALESCE(?::date,'infinity'::date)
                  AND strpos(lower(concat_ws(' ',u.user_id,u.display_name,u.email,b.business_name,o.company_name,b.representative_name,o.representative_name)),lower(?))>0
                ORDER BY u.created_at DESC,u.user_id DESC LIMIT ? OFFSET ?
                """, role, status, from, to, keyword, limit, offset);
    }

    /**
     * 会員の利用区分に対応するプロフィールを取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param role プロフィールや一覧の対象となる利用区分。
     * @param userId 照会・更新の対象となる会員のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> profile(ManagementDao db, String role, int userId) throws SQLException {
        return db.rows(switch (role) {
            case "RESIDENT" -> "SELECT age_group AS \"年齢区分\",gender AS \"性別\" FROM resident_profiles WHERE user_id=?";
            case "OWNER" -> """
                    SELECT representative_name AS "代表者",company_name AS "法人名",phone_e164 AS "電話番号",
                           secondary_phone_e164 AS "追加電話番号",icon_image_url AS "アイコン画像"
                    FROM owner_profiles WHERE user_id=?
                    """;
            default -> """
                    SELECT business_name AS "事業者名",representative_name AS "代表者",phone_e164 AS "電話番号",secondary_phone_e164 AS "追加電話番号",
                           preferred_area AS "希望エリア",employee_count AS "従業員数",industry_experience AS "業界経験",
                           management_experience AS "経営経験",icon_image_url AS "アイコン画像"
                    FROM business_profiles WHERE user_id=?
                    """;
        }, userId);
    }

    /**
     * 利用停止の対象から除外する有効な管理権限を確認する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> activeAdminGrant(ManagementDao db, int userId) throws SQLException {
        return db.rows("SELECT user_id FROM admin_members WHERE user_id=? AND is_active", userId);
    }

    /**
     * 会員に対する最新の操作履歴を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> recentAccountActions(ManagementDao db, int userId) throws SQLException {
        return db.rows("""
                SELECT u.display_name AS "管理者",l.action AS "操作",l.reason AS "理由",l.occurred_at AS "日時"
                FROM admin_audit_logs l JOIN users u ON u.user_id=l.actor_user_id
                WHERE l.target_type='USER' AND l.target_key=jsonb_build_object('user_id',?::integer)
                ORDER BY l.occurred_at DESC,l.audit_log_id DESC LIMIT 10
                """, userId);
    }

    /**
     * 選択済みの希望業種と現在の業種マスター状態を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> preferences(ManagementDao db, int userId) throws SQLException {
        return db.rows("""
                SELECT i.industry_type_id AS "ID", c.category_name AS "分類",i.industry_name AS "希望業種",
                       CASE WHEN i.is_active AND c.is_active THEN '有効' ELSE '無効' END AS "マスターの状態"
                FROM user_industry_preferences pr JOIN industry_types i ON pr.industry_type_id=i.industry_type_id
                    JOIN industry_categories c ON i.category_id=c.category_id
                WHERE pr.user_id=? ORDER BY c.display_order,c.category_id,i.display_order,i.industry_type_id
                """, userId);
    }

    /**
     * 有効な業種と対象会員が選択済みの業種を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> preferenceChoices(ManagementDao db, int userId) throws SQLException {
        return db.rows("""
                SELECT i.industry_type_id AS id, c.category_name || ' / ' || i.industry_name ||
                    CASE WHEN i.is_active AND c.is_active THEN '' ELSE '（無効）' END AS label
                FROM industry_types i JOIN industry_categories c ON c.category_id=i.category_id
                WHERE (i.is_active AND c.is_active) OR EXISTS (SELECT 1 FROM user_industry_preferences pr WHERE pr.user_id=? AND pr.industry_type_id=i.industry_type_id)
                ORDER BY c.display_order,c.category_id,i.display_order,i.industry_type_id
                """, userId);
    }

    /**
     * 対象会員の自宅、勤務先、学校の登録情報を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> locations(ManagementDao db, int userId) throws SQLException {
        return db.rows("""
                SELECT location_kind AS "種類",prefecture AS "都道府県",municipality AS "市区町村",address_line AS "番地・建物名",
                       latitude AS "緯度",longitude AS "経度",updated_at AS "更新日時"
                FROM user_locations WHERE user_id=? ORDER BY location_kind
                """, userId);
    }

    /**
     * 事業主の保有資格と有効期限を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> qualifications(ManagementDao db, int userId) throws SQLException {
        return db.rows("""
                SELECT bq.qualification_id AS "ID",q.qualification_name AS "名前",q.requirement_kind AS "種類",
                       bq.credential_number AS "資格番号",bq.obtained_on AS "取得日",bq.expires_on AS "有効期限",bq.is_active AS "有効"
                FROM business_qualifications bq JOIN qualifications q USING (qualification_id)
                WHERE bq.business_user_id=? ORDER BY bq.is_active DESC,q.qualification_name,q.qualification_id
                """, userId);
    }

    /**
     * 資格を登録する事業主プロフィールがあるか確認する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> businessProfile(ManagementDao db, int userId) throws SQLException {
        return db.rows("SELECT user_id FROM business_profiles WHERE user_id=?", userId);
    }

    /**
     * 保有資格に登録できる資格と、登録済みの資格を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> qualificationChoices(ManagementDao db, int userId) throws SQLException {
        return db.rows("""
                SELECT q.qualification_id AS id,q.qualification_name AS label FROM qualifications q
                WHERE (q.is_active AND q.requirement_kind IN ('QUALIFICATION','PERMIT')) OR EXISTS
                    (SELECT 1 FROM business_qualifications bq WHERE bq.business_user_id=? AND bq.qualification_id=q.qualification_id)
                ORDER BY q.qualification_name,q.qualification_id
                """, userId);
    }

    /**
     * 会員の指定された活動履歴をページ単位で取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param tab 取得する履歴の種類。
     * @param userId 照会・更新の対象となる会員のID。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> history(ManagementDao db, String tab, int userId, int limit, int offset) throws SQLException {
        // 種類ごとに定義したSQLから選び、検索条件は値として渡す。
        String query = switch (tab) {
            case "votes" -> """
                    SELECT v.vote_id AS "投票ID", v.property_id AS "物件ID",p.property_name AS "投票先",v.created_at AS "投票日時",
                           v.updated_at AS "更新日時",v.withdrawn_at AS "取下げ日時",
                           (SELECT string_agg(c.preference_rank || '位 ' || i.industry_name, '、' ORDER BY c.preference_rank)
                            FROM vote_choices c JOIN industry_types i USING (industry_type_id) WHERE c.vote_id=v.vote_id) AS "投票した業種"
                    FROM property_votes v JOIN properties p USING (property_id) WHERE v.resident_user_id=? ORDER BY v.updated_at DESC,v.vote_id DESC
                    """;
            case "answers" -> """
                    SELECT r.response_id AS "回答ID",r.survey_id AS "アンケートID",s.survey_title AS "アンケート",t.store_name AS "店舗",
                           r.rating AS "満足度",r.responded_at AS "回答日時",
                           (SELECT string_agg(concat(q.question_text,'　',COALESCE(a.text_answer,o.option_text)), E'\\n' ORDER BY q.question_no)
                            FROM survey_answers a JOIN survey_questions q USING (question_id) LEFT JOIN survey_options o USING (option_id)
                            WHERE a.response_id=r.response_id) AS "回答内容"
                    FROM survey_responses r JOIN surveys s USING (survey_id) LEFT JOIN tenancies t USING (tenancy_id)
                    WHERE r.resident_user_id=? ORDER BY r.responded_at DESC,r.response_id DESC
                    """;
            case "held", "used" -> """
                    SELECT c.coupon_id AS "クーポンID",c.coupon_title AS "クーポン",t.store_name AS "利用店舗",cr.issued_at AS "発行日時",
                           c.valid_until AS "有効期限",cr.used_at AS "使用日時",cr.revoked_at AS "無効化日時",
                           CASE WHEN cr.used_at IS NOT NULL THEN '使用済み' WHEN cr.revoked_at IS NOT NULL THEN '無効'
                                WHEN c.valid_until<=CURRENT_TIMESTAMP THEN '期限切れ' ELSE '未使用' END AS "状態"
                    FROM coupon_recipients cr JOIN coupons c USING (coupon_id) JOIN tenancies t USING (tenancy_id)
                    WHERE cr.resident_user_id=? AND cr.used_at IS
                    """ + ("used".equals(tab) ? " NOT NULL ORDER BY cr.used_at DESC,c.coupon_id DESC" : " NULL ORDER BY cr.issued_at DESC,c.coupon_id DESC");
            case "applications" -> """
                    SELECT a.application_id AS "申込ID",a.property_id AS "物件ID",p.property_name AS "物件",a.application_status AS "状態",a.created_at AS "申込日時"
                    FROM applications a JOIN properties p USING (property_id) WHERE a.business_user_id=? ORDER BY a.created_at DESC,a.application_id DESC
                    """;
            case "tenancies" -> """
                    SELECT t.tenancy_id AS "契約ID",a.application_id AS "申込ID",a.property_id AS "物件ID",p.property_name AS "物件",t.store_name AS "店舗",
                           t.contract_start_on AS "契約開始日",t.contract_end_on AS "契約終了日",t.tenancy_status AS "状態"
                    FROM tenancies t JOIN applications a USING (application_id) JOIN properties p USING (property_id) WHERE a.business_user_id=?
                    ORDER BY t.created_at DESC,t.tenancy_id DESC
                    """;
            case "plans" -> """
                    SELECT op.plan_id AS "計画ID",op.tenancy_id AS "契約ID",a.property_id AS "物件ID",op.title AS "計画",op.planned_open_on AS "開店予定日",
                           op.progress_status AS "進捗",op.publication_status AS "公開状態"
                    FROM opening_plans op JOIN tenancies t USING (tenancy_id) JOIN applications a USING (application_id)
                    WHERE a.business_user_id=? ORDER BY op.updated_at DESC,op.plan_id DESC
                    """;
            case "offers" -> """
                    SELECT o.offer_id AS "オファーID",o.property_id AS "物件ID",p.property_name AS "物件",
                           CASE WHEN o.owner_user_id=viewer.id THEN o.business_user_id ELSE o.owner_user_id END AS "相手会員ID",
                           CASE WHEN o.owner_user_id=viewer.id THEN business.display_name ELSE owner.display_name END AS "相手",
                           CASE WHEN o.owner_user_id=viewer.id THEN '送信' ELSE '受信' END AS "方向",o.offer_status AS "状態",o.sent_at AS "送信日時"
                    FROM offers o JOIN properties p USING (property_id) JOIN users business ON business.user_id=o.business_user_id
                        JOIN users owner ON owner.user_id=o.owner_user_id CROSS JOIN (SELECT ?::integer AS id) viewer
                    WHERE o.owner_user_id=viewer.id OR o.business_user_id=viewer.id ORDER BY o.sent_at DESC,o.offer_id DESC
                    """;
            case "surveys" -> """
                    SELECT s.survey_id AS "アンケートID",s.survey_title AS "アンケート",t.store_name AS "店舗",s.survey_status AS "状態",
                           (SELECT count(*) FROM survey_responses r WHERE r.survey_id=s.survey_id) AS "回答数",s.updated_at AS "更新日時"
                    FROM surveys s JOIN tenancies t USING (tenancy_id) JOIN applications a USING (application_id)
                    WHERE a.business_user_id=? ORDER BY s.updated_at DESC,s.survey_id DESC
                    """;
            case "coupons" -> """
                    SELECT c.coupon_id AS "クーポンID",c.coupon_title AS "クーポン",t.store_name AS "店舗",c.coupon_status AS "状態",c.valid_until AS "有効期限",
                           (SELECT count(*) FROM coupon_recipients cr WHERE cr.coupon_id=c.coupon_id) AS "発行数",
                           (SELECT count(*) FROM coupon_recipients cr WHERE cr.coupon_id=c.coupon_id AND cr.used_at IS NOT NULL) AS "使用数"
                    FROM coupons c JOIN tenancies t USING (tenancy_id) JOIN applications a USING (application_id)
                    WHERE a.business_user_id=? ORDER BY c.updated_at DESC,c.coupon_id DESC
                    """;
            case "properties" -> """
                    SELECT p.property_id AS "物件ID",p.property_name AS "物件名",p.listing_status AS "募集・契約状態",p.admin_hidden_at AS "掲載停止日時",
                           (SELECT count(*) FROM applications a WHERE a.property_id=p.property_id) AS "申込数"
                    FROM properties p WHERE p.owner_user_id=? ORDER BY p.updated_at DESC,p.property_id DESC
                    """;
            case "reports" -> """
                    SELECT report_id AS "通報ID",reason_code AS "理由",report_status AS "対応状態",created_at AS "受付日時"
                    FROM reports WHERE target_user_id=? ORDER BY created_at DESC,report_id DESC
                    """;
            case "inquiries" -> """
                    SELECT inquiry_id AS "問い合わせID",subject AS "件名",inquiry_status AS "対応状態",updated_at AS "更新日時"
                    FROM inquiries WHERE requester_user_id=? ORDER BY updated_at DESC,inquiry_id DESC
                    """;
            default -> throw new SQLException("Unsupported member history.", ManagementDao.NO_DATA);
        };
        return db.rows(query + " LIMIT ? OFFSET ?", userId, limit, offset);
    }

    /**
     * プロフィール保存や利用停止と競合しないよう会員の状態をロックする。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> lockAccount(ManagementDao db, int userId) throws SQLException {
        return db.one("SELECT user_role,account_status,withdrawn_at FROM users WHERE user_id=? FOR UPDATE", userId);
    }

    /**
     * 利用停止または利用再開後の会員状態を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param status 保存または絞り込みに指定する状態コード。絞り込みでは空文字で全件。
     * @param userId 照会・更新の対象となる会員のID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateStatus(ManagementDao db, String status, int userId) throws SQLException {
        db.update("UPDATE users SET account_status=?,updated_at=CURRENT_TIMESTAMP WHERE user_id=?", status, userId);
    }

    /**
     * 利用再開時に復元する停止前の会員状態を履歴から取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> latestSuspension(ManagementDao db, int userId) throws SQLException {
        return db.rows("""
                SELECT changes->>'before' AS state FROM admin_audit_logs
                WHERE target_type='USER' AND action='SUSPEND' AND target_key=jsonb_build_object('user_id',?::integer)
                ORDER BY audit_log_id DESC LIMIT 1
                """, userId);
    }

    /**
     * 利用区分が対応するプロフィールに、対象会員が本登録済みか確認する。
     * @param db 管理操作で共用するDAO接続。
     * @param role プロフィールや一覧の対象となる利用区分。
     * @param userId 照会・更新の対象となる会員のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> profileKeys(ManagementDao db, String role, int userId) throws SQLException {
        return db.rows("SELECT user_id FROM " + profileTable(role) + " WHERE user_id=?", userId);
    }

    /**
     * 会員の表示名と連絡用メールアドレスを保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param displayName 会員情報に保存する表示名。
     * @param email 連絡用のメールアドレス。未登録の場合はnull。
     * @param userId 照会・更新の対象となる会員のID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateContact(ManagementDao db, String displayName, String email, int userId) throws SQLException {
        db.update("UPDATE users SET display_name=?, email=?, updated_at=CURRENT_TIMESTAMP WHERE user_id=?", displayName, email, userId);
    }

    /**
     * 地域住民の年齢区分と性別を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param ageGroup 地域住民の年齢区分。
     * @param gender 地域住民の性別。
     * @param userId 照会・更新の対象となる会員のID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateResident(ManagementDao db, String ageGroup, String gender, int userId) throws SQLException {
        db.update("UPDATE resident_profiles SET age_group=NULLIF(?,''),gender=NULLIF(?,''),updated_at=CURRENT_TIMESTAMP WHERE user_id=?", ageGroup, gender, userId);
    }

    /**
     * オーナーの代表者と連絡先を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param representative 事業主・オーナーの代表者名。
     * @param company オーナーの法人名。未登録の場合はnull。
     * @param phone 連絡先の電話番号。
     * @param secondaryPhone 追加の電話番号。未登録の場合はnull。
     * @param userId 照会・更新の対象となる会員のID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateOwner(ManagementDao db,
            String representative, String company, String phone, String secondaryPhone, int userId) throws SQLException {
        db.update("""
                            UPDATE owner_profiles SET representative_name=?,company_name=?,phone_e164=?,secondary_phone_e164=?,updated_at=CURRENT_TIMESTAMP WHERE user_id=?
                            """, representative, company, phone, secondaryPhone, userId);
    }

    /**
     * 事業主の名称、連絡先、出店条件を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param businessName 事業者の名称。
     * @param representative 事業主・オーナーの代表者名。
     * @param phone 連絡先の電話番号。
     * @param secondaryPhone 追加の電話番号。未登録の場合はnull。
     * @param preferredArea 事業主が出店を希望するエリア名。
     * @param employeeCount 従業員数。
     * @param industryExperience 業界経験の記載内容。
     * @param managementExperience 経営経験の記載内容。
     * @param userId 照会・更新の対象となる会員のID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateBusiness(ManagementDao db,
            String businessName, String representative, String phone, String secondaryPhone, String preferredArea, int employeeCount, String industryExperience, String managementExperience, int userId) throws SQLException {
        db.update("""
                            UPDATE business_profiles SET business_name=?,representative_name=?,phone_e164=?,secondary_phone_e164=?,preferred_area=?,employee_count=?,
                                industry_experience=?,management_experience=?,updated_at=CURRENT_TIMESTAMP WHERE user_id=?
                            """, businessName, representative, phone, secondaryPhone, preferredArea, employeeCount, industryExperience, managementExperience, userId);
    }

    /**
     * 希望業種を差分更新するために、登録済みの業種IDを取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> preferenceKeys(ManagementDao db, int userId) throws SQLException {
        return db.rows("SELECT industry_type_id FROM user_industry_preferences WHERE user_id=? ORDER BY industry_type_id", userId);
    }

    /**
     * 選択できる業種を確認し、希望業種の登録が終わるまで無効化との競合を防ぐ。
     * @param db 管理操作で共用するDAO接続。
     * @param industryId 対象となる業種のID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> lockActiveIndustry(ManagementDao db, int industryId) throws SQLException {
        return db.rows("""
                    SELECT i.industry_type_id FROM industry_types i JOIN industry_categories c USING (category_id)
                    WHERE i.industry_type_id=? AND i.is_active AND c.is_active FOR SHARE OF i,c
                    """, industryId);
    }

    /**
     * 対象会員の希望業種を一件追加する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @param industryId 対象となる業種のID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void addPreference(ManagementDao db, int userId, int industryId) throws SQLException {
        db.update("INSERT INTO user_industry_preferences (user_id,industry_type_id) VALUES (?,?)", userId, industryId);
    }

    /**
     * 対象会員の希望業種を一件解除する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @param industryId 対象となる業種のID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void deletePreference(ManagementDao db, int userId, int industryId) throws SQLException {
        db.update("DELETE FROM user_industry_preferences WHERE user_id=? AND industry_type_id=?", userId, industryId);
    }

    /**
     * 対象住民の生活拠点を種類ごとに登録または更新する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @param kind 対象や保存する項目を区別する種別コード。
     * @param prefecture 都道府県名。
     * @param municipality 市区町村名。
     * @param address 番地や建物名を含む住所。
     * @param latitude 緯度。座標未指定の場合はnull。
     * @param longitude 経度。座標未指定の場合はnull。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void upsertLocation(ManagementDao db,
            int userId, String kind, String prefecture, String municipality, String address, BigDecimal latitude, BigDecimal longitude) throws SQLException {
        db.update("""
                INSERT INTO user_locations (user_id,location_kind,prefecture,municipality,address_line,latitude,longitude) VALUES (?,?,?,?,?,?,?)
                ON CONFLICT (user_id,location_kind) DO UPDATE SET prefecture=EXCLUDED.prefecture,municipality=EXCLUDED.municipality,
                    address_line=EXCLUDED.address_line,latitude=EXCLUDED.latitude,longitude=EXCLUDED.longitude,updated_at=CURRENT_TIMESTAMP
                """, userId, kind, prefecture, municipality, address, latitude, longitude);
    }

    /**
     * 対象会員が資格を保存できる事業主プロフィールを持つことを確認する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static void requireBusinessProfile(ManagementDao db, int userId) throws SQLException {
        db.one("SELECT user_id FROM business_profiles WHERE user_id=?", userId);
    }

    /**
     * 事業主に割り当てられる資格か、すでに割り当てられている資格かを確認する。
     * @param db 管理操作で共用するDAO接続。
     * @param qualificationId 対象となる資格・許認可・書類マスターのID。
     * @param userId 照会・更新の対象となる会員のID。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static void requireAssignableQualification(ManagementDao db, int qualificationId, int userId) throws SQLException {
        db.one("""
                SELECT q.qualification_id FROM qualifications q WHERE qualification_id=? AND ((q.is_active AND q.requirement_kind IN ('QUALIFICATION','PERMIT'))
                    OR EXISTS (SELECT 1 FROM business_qualifications b WHERE b.business_user_id=? AND b.qualification_id=q.qualification_id))
                """, qualificationId, userId);
    }

    /**
     * 保有資格の資格番号、取得日、有効期限、登録状態を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param userId 照会・更新の対象となる会員のID。
     * @param qualificationId 対象となる資格・許認可・書類マスターのID。
     * @param credential 資格・許認可の登録番号。未登録の場合はnull。
     * @param obtained 資格の取得日。未登録の場合はnull。
     * @param expires 資格の有効期限。未登録の場合はnull。
     * @param active 有効な状態として保存する場合はtrue。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void upsertQualification(ManagementDao db,
            int userId, int qualificationId, String credential, LocalDate obtained, LocalDate expires, boolean active) throws SQLException {
        db.update("""
                INSERT INTO business_qualifications (business_user_id,qualification_id,credential_number,obtained_on,expires_on,is_active) VALUES (?,?,?,?,?,?)
                ON CONFLICT (business_user_id,qualification_id) DO UPDATE SET credential_number=EXCLUDED.credential_number,
                    obtained_on=EXCLUDED.obtained_on,expires_on=EXCLUDED.expires_on,is_active=EXCLUDED.is_active,updated_at=CURRENT_TIMESTAMP
                """, userId, qualificationId, credential, obtained, expires, active);
    }
}
