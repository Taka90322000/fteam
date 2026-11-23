package jp.machiriku.dao.management;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 通報と問い合わせの受付、担当、対応履歴、メッセージを出し入れする。 */
public final class HelpDeskDao {
    /** 通報一覧と詳細で共有する、通報対象の名称と区分を含む参照。 */
    private static final String REPORT_FROM = """
            FROM reports r JOIN users sender ON sender.user_id=r.reporter_user_id
            LEFT JOIN users target ON target.user_id=r.target_user_id
            LEFT JOIN owner_profiles op ON op.user_id=target.user_id
            LEFT JOIN business_profiles bp ON bp.user_id=target.user_id
            LEFT JOIN properties p ON p.property_id=r.target_property_id
            LEFT JOIN users handler ON handler.user_id=r.assigned_admin_user_id
            """;
    /** 通報対象に表示する名称。 */
    private static final String TARGET_NAME = "COALESCE(p.property_name, bp.business_name, op.company_name, target.display_name)";
    /** 管理者が閲覧する問い合わせの送り主と担当者。 */
    private static final String INQUIRY_FROM = """
            FROM inquiries i JOIN users sender ON sender.user_id=i.requester_user_id
            LEFT JOIN users handler ON handler.user_id=i.assigned_admin_user_id
            LEFT JOIN properties p ON p.property_id=i.related_property_id
            """;

    /** 共通のDAO接続で各データ操作を行うため、このクラスを直接生成しない。 */
    private HelpDeskDao() { }

    /**
     * 通報の詳細と通報者・対象・担当者の情報を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param reportId 通報の受付番号。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> report(ManagementDao db, long reportId) throws SQLException {
        return db.one("""
                SELECT r.report_id AS "受付番号", r.reporter_user_id AS "通報者ID", sender.display_name AS "通報者",
                       r.target_user_id AS "対象会員ID", r.target_property_id AS "対象物件ID", r.related_property_id AS "関連物件ID",
                """ + TARGET_NAME + " AS \"対象\", " + """
                       r.reason_code AS "理由", r.description AS "内容", r.report_status AS "状態",
                       r.assigned_admin_user_id AS "担当者ID", handler.display_name AS "担当者", r.created_at AS "受付日時",
                       r.resolution_code AS "対応結果", r.resolution_note AS "対応理由", r.resolved_at AS "完了日時"
                """ + REPORT_FROM + " WHERE r.report_id=?", reportId);
    }

    /**
     * 対象区分や理由、対応状態、受付日と検索語で通報を絞り込む。
     * @param db 管理操作で共用するDAO接続。
     * @param kind 対象や保存する項目を区別する種別コード。
     * @param status 保存または絞り込みに指定する状態コード。絞り込みでは空文字で全件。
     * @param reasonCode 通報理由を表す区分コード。
     * @param from 対象期間の開始日。未指定の場合はnull。
     * @param to 対象期間の終了日。未指定の場合はnull。
     * @param keyword 絞り込みに用いる検索語。空文字は指定なし。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> reports(ManagementDao db,
            String kind, String status, String reasonCode, LocalDate from, LocalDate to, String keyword, int limit, int offset) throws SQLException {
        return db.rows("""
                SELECT r.report_id AS "ID", COALESCE(target.user_role, 'PROPERTY') AS "対象区分",
                """ + TARGET_NAME + " AS \"対象\", " + """
                       r.reason_code AS "理由", r.report_status AS "状態", handler.display_name AS "担当者", r.created_at AS "受付日時",
                       (SELECT count(*) FROM reports pending WHERE pending.report_status='OPEN'
                           AND ((r.target_user_id IS NOT NULL AND pending.target_user_id=r.target_user_id)
                             OR (r.target_property_id IS NOT NULL AND pending.target_property_id=r.target_property_id))) AS "同じ対象の未対応件数"
                """ + REPORT_FROM + """
                WHERE COALESCE(target.user_role,'PROPERTY')=COALESCE(NULLIF(?, ''),COALESCE(target.user_role,'PROPERTY'))
                  AND r.report_status=COALESCE(NULLIF(?, ''),r.report_status)
                  AND r.reason_code=COALESCE(NULLIF(?, ''),r.reason_code)
                  AND (r.created_at AT TIME ZONE 'Asia/Tokyo')::date >= COALESCE(?::date, '-infinity'::date)
                  AND (r.created_at AT TIME ZONE 'Asia/Tokyo')::date <= COALESCE(?::date, 'infinity'::date)
                  AND strpos(lower(concat_ws(' ',r.report_id,
                """ + TARGET_NAME + ")),lower(?))>0 ORDER BY r.created_at DESC,r.report_id DESC LIMIT ? OFFSET ?", kind, status, reasonCode, from, to, keyword, limit, offset);
    }

    /**
     * 通報の担当や対応結果を変更する前に現在の行をロックする。
     * @param db 管理操作で共用するDAO接続。
     * @param reportId 通報の受付番号。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> lockReport(ManagementDao db, long reportId) throws SQLException {
        return db.one("SELECT report_status,target_user_id,target_property_id FROM reports WHERE report_id=? FOR UPDATE", reportId);
    }

    /**
     * 通報の担当者を設定し、確認中の状態を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param assigneeId 担当管理者の会員ID。未割当または絞り込みなしの場合はnull。
     * @param reportId 通報の受付番号。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void assignReport(ManagementDao db, int assigneeId, long reportId) throws SQLException {
        db.update("UPDATE reports SET report_status='IN_REVIEW',assigned_admin_user_id=? WHERE report_id=?", assigneeId, reportId);
    }

    /**
     * 通報の対応結果と完了日時を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param resolution 通報への対応結果を示すコード。
     * @param reason 管理者が対応を判断した理由。
     * @param actorId 利用者、担当者または管理者本人の会員ID。
     * @param reportId 通報の受付番号。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void resolveReport(ManagementDao db, String resolution, String reason, int actorId, long reportId) throws SQLException {
        db.update("""
                    UPDATE reports SET report_status='RESOLVED',resolution_code=?,resolution_note=?,resolved_at=CURRENT_TIMESTAMP,
                                       assigned_admin_user_id=COALESCE(assigned_admin_user_id,?) WHERE report_id=?
                    """, resolution, reason, actorId, reportId);
    }

    /**
     * 問い合わせの詳細を、管理者または送信者本人に許可された条件で取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param inquiryId 問い合わせの受付番号。
     * @param admin 管理者として照会・更新を行う場合はtrue。
     * @param actorId 利用者、担当者または管理者本人の会員ID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> inquiry(ManagementDao db, long inquiryId, boolean admin, int actorId) throws SQLException {
        return db.one("""
                SELECT i.inquiry_id AS "受付番号", i.subject AS "件名", i.category AS "分類", i.inquiry_status AS "状態",
                       i.created_at AS "受付日時", i.updated_at AS "更新日時", p.property_name AS "関連物件"
                """ + (admin ? ", i.requester_user_id AS \"送信者ID\", sender.display_name AS \"送信者\", "
                        + "i.assigned_admin_user_id AS \"担当者ID\",handler.display_name AS \"担当者\", i.related_property_id AS \"物件ID\" " : "")
                + INQUIRY_FROM + " WHERE i.inquiry_id=? AND (? OR i.requester_user_id=?)", inquiryId, admin, actorId);
    }

    /**
     * 問い合わせの本文・返信を取得し、内部メモは管理者に限定する。
     * @param db 管理操作で共用するDAO接続。
     * @param inquiryId 問い合わせの受付番号。
     * @param admin 管理者として照会・更新を行う場合はtrue。
     * @param actorId 利用者、担当者または管理者本人の会員ID。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> messages(ManagementDao db,
            long inquiryId, boolean admin, int actorId, int limit, int offset) throws SQLException {
        return db.rows("""
                SELECT m.created_at AS "日時", CASE WHEN m.sender_user_id=i.requester_user_id THEN '利用者' ELSE '運営' END AS "投稿者",
                       CASE WHEN m.visibility='ADMIN_ONLY' THEN '管理者用メモ' ELSE '返信' END AS "種類", m.body AS "内容"
                FROM inquiry_messages m JOIN inquiries i ON i.inquiry_id=m.inquiry_id
                WHERE i.inquiry_id=? AND (? OR (i.requester_user_id=? AND m.visibility='PARTICIPANTS'))
                ORDER BY m.created_at DESC,m.message_id DESC LIMIT ? OFFSET ?
                """, inquiryId, admin, actorId, limit, offset);
    }

    /**
     * 管理者用の受付一覧または会員本人の問い合わせ履歴を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param admin 管理者として照会・更新を行う場合はtrue。
     * @param actorId 利用者、担当者または管理者本人の会員ID。
     * @param category 問い合わせのカテゴリ。絞り込みでは空文字で全件。
     * @param status 保存または絞り込みに指定する状態コード。絞り込みでは空文字で全件。
     * @param assigneeId 担当管理者の会員ID。未割当または絞り込みなしの場合はnull。
     * @param keyword 絞り込みに用いる検索語。空文字は指定なし。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> inquiries(ManagementDao db,
            boolean admin, int actorId, String category, String status, Integer assigneeId, String keyword, int limit, int offset) throws SQLException {
        return db.rows("""
                SELECT i.inquiry_id AS "ID", i.subject AS "件名", i.category AS "分類", i.inquiry_status AS "状態", i.updated_at AS "更新日時"
                """ + (admin ? ", sender.display_name AS \"送信者\",handler.display_name AS \"担当者\" " : "") + INQUIRY_FROM + """
                WHERE (? OR i.requester_user_id=?) AND i.category=COALESCE(NULLIF(?, ''),i.category)
                  AND i.inquiry_status=COALESCE(NULLIF(?, ''),i.inquiry_status)
                  AND (?::integer IS NULL OR i.assigned_admin_user_id=?)
                  AND strpos(lower(concat_ws(' ',i.inquiry_id,i.subject,sender.display_name)),lower(?))>0
                ORDER BY CASE WHEN ? AND i.inquiry_status='WAITING_ADMIN' THEN 0 ELSE 1 END,i.updated_at DESC,i.inquiry_id DESC
                LIMIT ? OFFSET ?
                """, admin, actorId, category, status, assigneeId, assigneeId, keyword, admin, limit, offset);
    }

    /**
     * 会員の問い合わせを受け付けて受付番号を発行する。
     * @param db 管理操作で共用するDAO接続。
     * @param actorId 利用者、担当者または管理者本人の会員ID。
     * @param propertyId 対象または関連する物件のID。
     * @param category 問い合わせのカテゴリ。絞り込みでは空文字で全件。
     * @param subject 問い合わせの件名。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> insertInquiry(ManagementDao db,
            int actorId, Integer propertyId, String category, String subject) throws SQLException {
        return db.one("""
                    INSERT INTO inquiries (requester_user_id,related_property_id,category,subject) VALUES (?,?,?,?) RETURNING inquiry_id
                    """, actorId, propertyId, category, subject);
    }

    /**
     * 管理者または持ち主に範囲を絞って問い合わせの更新行をロックする。
     * @param db 管理操作で共用するDAO接続。
     * @param inquiryId 問い合わせの受付番号。
     * @param admin 管理者として照会・更新を行う場合はtrue。
     * @param actorId 利用者、担当者または管理者本人の会員ID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> lockInquiry(ManagementDao db, long inquiryId, boolean admin, int actorId) throws SQLException {
        return db.one("""
                SELECT inquiry_status,assigned_admin_user_id FROM inquiries WHERE inquiry_id=? AND (? OR requester_user_id=?) FOR UPDATE
                """, inquiryId, admin, actorId);
    }

    /**
     * 問い合わせの担当と状態を更新して必要に応じ完了日時を設定する。
     * @param db 管理操作で共用するDAO接続。
     * @param status 保存または絞り込みに指定する状態コード。絞り込みでは空文字で全件。
     * @param assigneeId 担当管理者の会員ID。未割当または絞り込みなしの場合はnull。
     * @param inquiryId 問い合わせの受付番号。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateInquiry(ManagementDao db, String status, Integer assigneeId, long inquiryId) throws SQLException {
        db.update("""
                UPDATE inquiries SET inquiry_status=?,assigned_admin_user_id=?,updated_at=CURRENT_TIMESTAMP,
                                     closed_at=CASE WHEN ?='CLOSED' THEN CURRENT_TIMESTAMP END WHERE inquiry_id=?
                """, status, assigneeId, status, inquiryId);
    }

    /**
     * 通報者本人に表示できる受付状況を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param reportId 通報の受付番号。
     * @param actorId 利用者、担当者または管理者本人の会員ID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> reportReceipt(ManagementDao db, long reportId, int actorId) throws SQLException {
        return db.one("""
                    SELECT report_id AS "受付番号", report_status AS "状態", created_at AS "受付日時"
                    FROM reports WHERE report_id=? AND reporter_user_id=?
                    """, reportId, actorId);
    }

    /**
     * 対象と関連物件を区別して通報を受け付ける。
     * @param db 管理操作で共用するDAO接続。
     * @param actorId 利用者、担当者または管理者本人の会員ID。
     * @param targetUserId 通報対象の会員ID。物件を対象とする場合はnull。
     * @param targetPropertyId 通報対象の物件ID。会員を対象とする場合はnull。
     * @param propertyId 対象または関連する物件のID。
     * @param reasonCode 通報理由を表す区分コード。
     * @param description 掲載や通報の説明文。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> insertReport(ManagementDao db,
            int actorId, Integer targetUserId, Integer targetPropertyId, Integer propertyId, String reasonCode, String description) throws SQLException {
        return db.one("""
                INSERT INTO reports (reporter_user_id,target_user_id,target_property_id,related_property_id,reason_code,description)
                VALUES (?,?,?,?,?,?) RETURNING report_id
                """, actorId, targetUserId, targetPropertyId, propertyId, reasonCode, description);
    }

    /**
     * 問い合わせに公開範囲を指定して本文、返信、または内部メモを記録する。
     * @param db 管理操作で共用するDAO接続。
     * @param inquiryId 問い合わせの受付番号。
     * @param actorId 利用者、担当者または管理者本人の会員ID。
     * @param body 問い合わせの本文、返信、または内部メモ。
     * @param visibility 一覧の絞り込み条件または投稿の公開範囲。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void insertMessage(ManagementDao db, long inquiryId, int actorId, String body, String visibility) throws SQLException {
        db.update("INSERT INTO inquiry_messages (inquiry_id,sender_user_id,body,visibility) VALUES (?,?,?,?)", inquiryId, actorId, body, visibility);
    }

    /**
     * 担当者に設定できる本登録済み管理者を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> activeAdmins(ManagementDao db) throws SQLException {
        return db.rows("""
                SELECT u.user_id AS id,u.display_name || ' (#' || u.user_id || ')' AS label
                FROM admin_members a JOIN users u ON u.user_id=a.user_id
                WHERE a.is_active AND u.account_status='ACTIVE' AND u.withdrawn_at IS NULL ORDER BY u.display_name,u.user_id
                """);
    }
}
