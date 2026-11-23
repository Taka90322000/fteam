package jp.machiriku.dao.management;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** アンケートの受付状況、回答履歴、下書きと改訂版のDB操作をまとめる。 */
public final class AdminSurveyDao {
    /** 共通のDAO接続で各データ操作を行うため、このクラスを直接生成しない。 */
    private AdminSurveyDao() { }

    /**
     * アンケートの基本情報と、回答済み件数や改訂元を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> survey(ManagementDao db, int surveyId) throws SQLException {
        return db.one("""
                SELECT s.survey_id AS "アンケート番号",s.survey_title AS "名前",s.survey_kind AS "種類",s.survey_status AS "受付状態",
                       s.industry_type_id AS "対象業種ID",i.industry_name AS "対象業種",s.source_template_id AS "テンプレートID",
                       s.previous_survey_id AS "改訂元ID",s.tenancy_id AS "契約ID",t.store_name AS "店舗",a.business_user_id AS "事業主ID",
                       s.starts_at AS "受付開始",s.ends_at AS "受付終了",
                       (SELECT count(*) FROM survey_responses r WHERE r.survey_id=s.survey_id) AS "回答数",
                       (SELECT round(avg(r.rating),2) FROM survey_responses r WHERE r.survey_id=s.survey_id) AS "平均満足度"
                FROM surveys s LEFT JOIN tenancies t USING (tenancy_id) LEFT JOIN applications a USING (application_id)
                    LEFT JOIN industry_types i ON i.industry_type_id=s.industry_type_id WHERE s.survey_id=?
                """, surveyId);
    }

    /**
     * 種別や受付状態、名称でアンケートを絞り込む。
     * @param db 管理操作で共用するDAO接続。
     * @param kind 対象や保存する項目を区別する種別コード。
     * @param status 保存または絞り込みに指定する状態コード。絞り込みでは空文字で全件。
     * @param keyword 絞り込みに用いる検索語。空文字は指定なし。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> surveys(ManagementDao db,
            String kind, String status, String keyword, int limit, int offset) throws SQLException {
        return db.rows("""
                SELECT s.survey_id AS "ID",s.survey_title AS "名前",s.survey_kind AS "種類",t.store_name AS "店舗",s.survey_status AS "受付状態",
                       (SELECT count(*) FROM survey_responses r WHERE r.survey_id=s.survey_id) AS "回答数",s.updated_at AS "更新日時"
                FROM surveys s LEFT JOIN tenancies t USING (tenancy_id)
                WHERE s.survey_kind=COALESCE(NULLIF(?,''),s.survey_kind) AND s.survey_status=COALESCE(NULLIF(?,''),s.survey_status)
                  AND strpos(lower(concat_ws(' ',s.survey_id,s.survey_title,t.store_name)),lower(?))>0
                ORDER BY s.updated_at DESC,s.survey_id DESC LIMIT ? OFFSET ?
                """, kind, status, keyword, limit, offset);
    }

    /**
     * 設問の順番と回答形式、選択肢を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> questions(ManagementDao db, int surveyId) throws SQLException {
        return db.rows("""
                SELECT q.question_no AS "設問番号",q.question_text AS "設問",q.answer_kind AS "回答形式",q.is_required AS "必須",
                       (SELECT string_agg(o.option_text,E'\\n' ORDER BY o.display_order) FROM survey_options o WHERE o.question_id=q.question_id) AS "選択肢"
                FROM survey_questions q WHERE q.survey_id=? ORDER BY q.question_no
                """, surveyId);
    }

    /**
     * アンケートの点数別回答数を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> ratingDistribution(ManagementDao db, int surveyId) throws SQLException {
        return db.rows("""
                SELECT score AS "満足度",count(r.response_id) AS "回答数" FROM generate_series(1,5) AS score
                LEFT JOIN survey_responses r ON r.rating=score AND r.survey_id=? GROUP BY score ORDER BY score DESC
                """, surveyId);
    }

    /**
     * 設問の選択肢ごとの回答件数を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> optionCounts(ManagementDao db, int surveyId) throws SQLException {
        return db.rows("""
                SELECT q.question_no AS "設問番号",q.question_text AS "設問",o.option_text AS "選択肢",count(a.response_id) AS "回答数"
                FROM survey_questions q JOIN survey_options o ON o.question_id=q.question_id
                LEFT JOIN survey_answers a ON a.question_id=q.question_id AND a.option_id=o.option_id
                WHERE q.survey_id=? GROUP BY q.question_id,q.question_no,q.question_text,o.option_id,o.option_text,o.display_order
                ORDER BY q.question_no,o.display_order
                """, surveyId);
    }

    /**
     * アンケートの回答履歴を新しい順に取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @param limit 取得する最大件数。
     * @param offset 取得開始位置までに読み飛ばす件数。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> responses(ManagementDao db, int surveyId, int limit, int offset) throws SQLException {
        return db.rows("""
                SELECT r.response_id AS "回答ID",r.resident_user_id AS "回答者ID",u.display_name AS "回答者",r.rating AS "満足度",
                       r.age_group_snapshot AS "年齢区分",r.municipality_snapshot AS "市区町村",r.responded_at AS "回答日時",
                       (SELECT string_agg(concat(q.question_text,'　',COALESCE(a.text_answer,o.option_text)),E'\\n' ORDER BY q.question_no)
                        FROM survey_answers a JOIN survey_questions q USING (question_id) LEFT JOIN survey_options o USING (option_id)
                        WHERE a.response_id=r.response_id) AS "回答内容"
                FROM survey_responses r JOIN users u ON u.user_id=r.resident_user_id WHERE r.survey_id=?
                ORDER BY r.responded_at DESC,r.response_id DESC LIMIT ? OFFSET ?
                """, surveyId, limit, offset);
    }

    /**
     * 対象業種のアンケートテンプレートを下書きとして保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param industryId 対象となる業種のID。
     * @param title クーポンやアンケートの名称。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> insertTemplate(ManagementDao db, int industryId, String title) throws SQLException {
        return db.one("""
                    INSERT INTO surveys (industry_type_id,survey_title,survey_kind,survey_status,created_at,updated_at)
                    VALUES (?,?,'TEMPLATE','DRAFT',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP) RETURNING survey_id
                    """, industryId, title);
    }

    /**
     * 受付状態や設問を変更するときにアンケート行をロックする。
     * @param db 管理操作で共用するDAO接続。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> lockSurvey(ManagementDao db, int surveyId) throws SQLException {
        return db.one("""
                SELECT survey_id,survey_status,survey_kind,survey_title,tenancy_id,industry_type_id,source_template_id
                FROM surveys WHERE survey_id=? FOR UPDATE
                """, surveyId);
    }

    /**
     * アンケートの受付を終了する。
     * @param db 管理操作で共用するDAO接続。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void closeSurvey(ManagementDao db, int surveyId) throws SQLException {
        db.update("UPDATE surveys SET survey_status='CLOSED',updated_at=CURRENT_TIMESTAMP WHERE survey_id=?", surveyId);
    }

    /**
     * 受付データに既存の回答があるか確認する。
     * @param db 管理操作で共用するDAO接続。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> responseKeys(ManagementDao db, int surveyId) throws SQLException {
        return db.rows("SELECT response_id FROM survey_responses WHERE survey_id=? LIMIT 1", surveyId);
    }

    /**
     * 同じアンケートの改訂下書きがすでに作成されているか確認する。
     * @param db 管理操作で共用するDAO接続。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> revisionDrafts(ManagementDao db, int surveyId) throws SQLException {
        return db.rows("SELECT survey_id FROM surveys WHERE previous_survey_id=? AND survey_status='DRAFT' ORDER BY survey_id DESC LIMIT 1", surveyId);
    }

    /**
     * 元の契約やテンプレートを参照する改訂下書きを作成する。
     * @param db 管理操作で共用するDAO接続。
     * @param tenancyId 契約のID。
     * @param title クーポンやアンケートの名称。
     * @param templateId 参照元のテンプレートID。テンプレートを使わない場合はnull。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> insertRevision(ManagementDao db,
            int tenancyId, String title, Integer templateId, int surveyId) throws SQLException {
        return db.one("""
                INSERT INTO surveys (tenancy_id,survey_title,survey_kind,source_template_id,previous_survey_id,survey_status,created_at,updated_at)
                VALUES (?,?,'POST_VISIT',?,?,'DRAFT',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP) RETURNING survey_id
                """, tenancyId, title, templateId, surveyId);
    }

    /**
     * 回答の履歴を保ったまま改訂版へ設問を複製する。
     * @param db 管理操作で共用するDAO接続。
     * @param revisionId 改訂版のアンケートID。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void copyQuestions(ManagementDao db, int revisionId, int surveyId) throws SQLException {
        db.update("""
                INSERT INTO survey_questions (survey_id,question_no,question_text,answer_kind,is_required,created_at)
                SELECT ?,question_no,question_text,answer_kind,is_required,CURRENT_TIMESTAMP FROM survey_questions WHERE survey_id=?
                """, revisionId, surveyId);
    }

    /**
     * 複製した設問に対応する選択肢を改訂版へ複製する。
     * @param db 管理操作で共用するDAO接続。
     * @param revisionId 改訂版のアンケートID。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void copyOptions(ManagementDao db, int revisionId, int surveyId) throws SQLException {
        db.update("""
                INSERT INTO survey_options (question_id,option_text,display_order)
                SELECT copied.question_id,o.option_text,o.display_order FROM survey_options o JOIN survey_questions original ON original.question_id=o.question_id
                    JOIN survey_questions copied ON copied.survey_id=? AND copied.question_no=original.question_no WHERE original.survey_id=?
                """, revisionId, surveyId);
    }

    /**
     * アンケートテンプレートの名称と対象業種を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param title クーポンやアンケートの名称。
     * @param industryId 対象となる業種のID。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateTemplate(ManagementDao db, String title, int industryId, int surveyId) throws SQLException {
        db.update("UPDATE surveys SET survey_title=?,industry_type_id=?,updated_at=CURRENT_TIMESTAMP WHERE survey_id=?", title, industryId, surveyId);
    }

    /**
     * 来店アンケート下書きの名称と受付予定日時を保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param title クーポンやアンケートの名称。
     * @param starts 受付・配布を開始する日時。
     * @param ends 受付・配布を終了する日時。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void updateSchedule(ManagementDao db,
            String title, OffsetDateTime starts, OffsetDateTime ends, int surveyId) throws SQLException {
        db.update("UPDATE surveys SET survey_title=?,starts_at=?,ends_at=?,updated_at=CURRENT_TIMESTAMP WHERE survey_id=?", title, starts, ends, surveyId);
    }

    /**
     * アンケート内の設問番号に対応する設問IDを取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @param questionNumber アンケート内での設問番号。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> questionKey(ManagementDao db, int surveyId, int questionNumber) throws SQLException {
        return db.one("SELECT question_id FROM survey_questions WHERE survey_id=? AND question_no=?", surveyId, questionNumber);
    }

    /**
     * 設問を修正または削除する際に、旧選択肢を削除する。
     * @param db 管理操作で共用するDAO接続。
     * @param questionId 設問のID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void deleteOptions(ManagementDao db, int questionId) throws SQLException {
        db.update("DELETE FROM survey_options WHERE question_id=?", questionId);
    }

    /**
     * アンケートの下書きから対象設問を削除する。
     * @param db 管理操作で共用するDAO接続。
     * @param questionId 設問のID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void deleteQuestion(ManagementDao db, int questionId) throws SQLException {
        db.update("DELETE FROM survey_questions WHERE question_id=?", questionId);
    }

    /**
     * アンケートの下書きの設問を登録または更新する。
     * @param db 管理操作で共用するDAO接続。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @param questionNumber アンケート内での設問番号。
     * @param questionText 保存する設問文。
     * @param kind 対象や保存する項目を区別する種別コード。
     * @param required 必須として扱う場合はtrue。
     * @return 対象の一件。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static Map<String, Object> upsertQuestion(ManagementDao db,
            int surveyId, int questionNumber, String questionText, String kind, boolean required) throws SQLException {
        return db.one("""
                    INSERT INTO survey_questions (survey_id,question_no,question_text,answer_kind,is_required,created_at)
                    VALUES (?,?,?,?,?,CURRENT_TIMESTAMP) ON CONFLICT (survey_id,question_no) DO UPDATE SET
                        question_text=EXCLUDED.question_text,answer_kind=EXCLUDED.answer_kind,is_required=EXCLUDED.is_required RETURNING question_id
                    """, surveyId, questionNumber, questionText, kind, required);
    }

    /**
     * 設問の新しい選択肢を表示順付きで保存する。
     * @param db 管理操作で共用するDAO接続。
     * @param questionId 設問のID。
     * @param optionText 保存する選択肢の文言。
     * @param displayOrder 表示順を表す正の整数。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void insertOption(ManagementDao db, int questionId, String optionText, int displayOrder) throws SQLException {
        db.update("INSERT INTO survey_options (question_id,option_text,display_order) VALUES (?,?,?)", questionId, optionText, displayOrder);
    }

    /**
     * 設問の変更をアンケート本体の更新日時に反映する。
     * @param db 管理操作で共用するDAO接続。
     * @param surveyId 操作対象または改訂元となるアンケートのID。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static void markUpdated(ManagementDao db, int surveyId) throws SQLException {
        db.update("UPDATE surveys SET updated_at=CURRENT_TIMESTAMP WHERE survey_id=?", surveyId);
    }

    /**
     * テンプレートの対象業種に指定できる業種を取得する。
     * @param db 管理操作で共用するDAO接続。
     * @param retainedIndustryId 既存の設定で選ばれている業種ID。引き継がない場合はnull。
     * @return 条件に合うレコードの一覧。
     * @throws SQLException DB処理に失敗した場合。
     */
    public static List<Map<String, Object>> industryChoices(ManagementDao db, Integer retainedIndustryId) throws SQLException {
        return db.rows("""
                SELECT i.industry_type_id AS id,i.industry_name AS label FROM industry_types i JOIN industry_categories c USING (category_id)
                WHERE (i.is_active AND c.is_active) OR i.industry_type_id=? ORDER BY c.display_order,c.category_id,i.display_order,i.industry_type_id
                """, retainedIndustryId);
    }

    /**
     * 対象業種が有効か、既存設定から引き継がれる対象業種であることを確認する。
     * @param db 管理操作で共用するDAO接続。
     * @param industryId 対象となる業種のID。
     * @param retainedIndustryId 既存の設定で選ばれている業種ID。引き継がない場合はnull。
     * @throws SQLException 対象が存在しないか、DB処理に失敗した場合。
     */
    public static void requireIndustry(ManagementDao db, int industryId, Integer retainedIndustryId) throws SQLException {
        db.one("""
                SELECT i.industry_type_id FROM industry_types i JOIN industry_categories c USING (category_id)
                WHERE i.industry_type_id=? AND ((i.is_active AND c.is_active) OR i.industry_type_id=?)
                """, industryId, retainedIndustryId);
    }
}
