package jp.machiriku.dao.management;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.machiriku.bean.management.AdminSurveyItem;
import jp.machiriku.bean.management.ManagementProblem;

/**
 * アンケートの検索・詳細・集計およびテンプレート作成・受付停止・改訂・設問編集を行うDAO。
 */
public class AdminSurveyDao extends ManagementBaseDao {
    /** 管理者権限チェックと監査ログ記録に使うDAO。 */
    private final AdminAuditDao auditDao;

    /** 本番接続を使用するDAOを生成する。 */
    public AdminSurveyDao() {
        super();
        this.auditDao = new AdminAuditDao();
    }

    /**
     * テスト用の共有接続を使用するDAOを生成する。
     * @param sharedConnection テスト用接続。
     */
    public AdminSurveyDao(Connection sharedConnection) {
        super(sharedConnection);
        this.auditDao = new AdminAuditDao(sharedConnection);
    }

    /**
     * 種別・受付状態・キーワードでアンケート一覧を検索する。
     * @param kind 種別コード（POST_VISIT / TEMPLATE）。空文字で全件。
     * @param status 受付状態コード（DRAFT / OPEN / CLOSED）。空文字で全件。
     * @param keyword 検索語。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return アンケート一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminSurveyItem> findSurveys(String kind, String status, String keyword, int limit, int offset)
            throws SQLException {
        String sql = """
                SELECT s.survey_id, s.survey_title, s.survey_kind, t.store_name, s.survey_status,
                       (SELECT count(*) FROM survey_responses r WHERE r.survey_id = s.survey_id) AS response_count,
                       s.updated_at
                FROM surveys s
                LEFT JOIN tenancies t USING (tenancy_id)
                WHERE s.survey_kind = COALESCE(NULLIF(?, ''), s.survey_kind)
                  AND s.survey_status = COALESCE(NULLIF(?, ''), s.survey_status)
                  AND strpos(lower(concat_ws(' ', s.survey_id, s.survey_title, t.store_name)), lower(?)) > 0
                ORDER BY s.updated_at DESC, s.survey_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminSurveyItem> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, kind);
            stmt.setString(2, status);
            stmt.setString(3, keyword);
            stmt.setInt(4, limit);
            stmt.setInt(5, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminSurveyItem item = new AdminSurveyItem();
                    item.setSurveyId(rs.getInt("survey_id"));
                    item.setSurveyTitle(rs.getString("survey_title"));
                    item.setSurveyKind(rs.getString("survey_kind"));
                    item.setStoreName(rs.getString("store_name"));
                    item.setSurveyStatus(rs.getString("survey_status"));
                    item.setResponseCount(rs.getLong("response_count"));
                    item.setUpdatedAt(getOffsetDateTime(rs, "updated_at"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 指定IDのアンケート詳細と回答件数・平均満足度を取得する。
     * @param surveyId アンケートID。
     * @return アンケート詳細。存在しない場合はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    public AdminSurveyItem findSurveyById(int surveyId) throws SQLException {
        String sql = """
                SELECT s.survey_id, s.survey_title, s.survey_kind, s.survey_status,
                       s.industry_type_id, i.industry_name, s.source_template_id,
                       s.previous_survey_id, s.tenancy_id, t.store_name, a.business_user_id,
                       s.starts_at, s.ends_at,
                       (SELECT count(*) FROM survey_responses r WHERE r.survey_id = s.survey_id) AS response_count,
                       (SELECT round(avg(r.rating), 2) FROM survey_responses r WHERE r.survey_id = s.survey_id) AS average_rating
                FROM surveys s
                LEFT JOIN tenancies t USING (tenancy_id)
                LEFT JOIN applications a USING (application_id)
                LEFT JOIN industry_types i ON i.industry_type_id = s.industry_type_id
                WHERE s.survey_id = ?
                """;
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, surveyId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AdminSurveyItem item = new AdminSurveyItem();
                item.setSurveyId(rs.getInt("survey_id"));
                item.setSurveyTitle(rs.getString("survey_title"));
                item.setSurveyKind(rs.getString("survey_kind"));
                item.setSurveyStatus(rs.getString("survey_status"));
                item.setIndustryTypeId(getNullableInt(rs, "industry_type_id"));
                item.setIndustryName(rs.getString("industry_name"));
                item.setSourceTemplateId(getNullableInt(rs, "source_template_id"));
                item.setPreviousSurveyId(getNullableInt(rs, "previous_survey_id"));
                item.setTenancyId(getNullableInt(rs, "tenancy_id"));
                item.setStoreName(rs.getString("store_name"));
                item.setBusinessUserId(getNullableInt(rs, "business_user_id"));
                item.setStartsAt(getOffsetDateTime(rs, "starts_at"));
                item.setEndsAt(getOffsetDateTime(rs, "ends_at"));
                item.setResponseCount(rs.getLong("response_count"));
                item.setAverageRating(rs.getBigDecimal("average_rating"));
                return item;
            }
        }
    }

    /**
     * アンケートの追加設問と選択肢を番号順に取得する。
     * @param surveyId アンケートID。
     * @return 設問一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminSurveyItem.Question> findQuestions(int surveyId) throws SQLException {
        String sql = """
                SELECT q.question_no, q.question_text, q.answer_kind, q.is_required,
                       (SELECT string_agg(o.option_text, E'\n' ORDER BY o.display_order)
                        FROM survey_options o
                        WHERE o.question_id = q.question_id) AS options_text
                FROM survey_questions q
                WHERE q.survey_id = ?
                ORDER BY q.question_no
                """;
        List<AdminSurveyItem.Question> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, surveyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminSurveyItem.Question item = new AdminSurveyItem.Question();
                    item.setQuestionNo(rs.getInt("question_no"));
                    item.setQuestionText(rs.getString("question_text"));
                    item.setAnswerKind(rs.getString("answer_kind"));
                    item.setRequired(rs.getBoolean("is_required"));
                    item.setOptionsText(rs.getString("options_text"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 満足度（5〜1）ごとの回答数分布を取得する。
     * @param surveyId アンケートID。
     * @return スコア降順の分布リスト。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminSurveyItem.RatingCount> findRatingDistribution(int surveyId) throws SQLException {
        String sql = """
                SELECT score, count(r.response_id) AS response_count
                FROM generate_series(1, 5) AS score
                LEFT JOIN survey_responses r ON r.rating = score AND r.survey_id = ?
                GROUP BY score
                ORDER BY score DESC
                """;
        List<AdminSurveyItem.RatingCount> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, surveyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminSurveyItem.RatingCount item = new AdminSurveyItem.RatingCount();
                    item.setRating(rs.getInt("score"));
                    item.setResponseCount(rs.getLong("response_count"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 単一選択設問の選択肢ごとの回答数を取得する。
     * @param surveyId アンケートID。
     * @return 設問番号・選択肢順の集計リスト。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminSurveyItem.OptionCount> findOptionCounts(int surveyId) throws SQLException {
        String sql = """
                SELECT q.question_no, q.question_text, o.option_text, count(a.response_id) AS response_count
                FROM survey_questions q
                JOIN survey_options o ON o.question_id = q.question_id
                LEFT JOIN survey_answers a ON a.question_id = q.question_id AND a.option_id = o.option_id
                WHERE q.survey_id = ?
                GROUP BY q.question_id, q.question_no, q.question_text, o.option_id, o.option_text, o.display_order
                ORDER BY q.question_no, o.display_order
                """;
        List<AdminSurveyItem.OptionCount> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, surveyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminSurveyItem.OptionCount item = new AdminSurveyItem.OptionCount();
                    item.setQuestionNo(rs.getInt("question_no"));
                    item.setQuestionText(rs.getString("question_text"));
                    item.setOptionText(rs.getString("option_text"));
                    item.setResponseCount(rs.getLong("response_count"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * 個別回答履歴を新しい順に取得する。
     * @param surveyId アンケートID。
     * @param limit 取得上限件数。
     * @param offset 読み飛ばす件数。
     * @return 回答履歴一覧。
     * @throws SQLException DB照会に失敗した場合。
     */
    public List<AdminSurveyItem.Response> findResponses(int surveyId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT r.response_id, r.resident_user_id, u.display_name, r.rating,
                       r.age_group_snapshot, r.municipality_snapshot, r.responded_at,
                       (SELECT string_agg(concat(q.question_text, '　', COALESCE(a.text_answer, o.option_text)),
                                          E'\n' ORDER BY q.question_no)
                        FROM survey_answers a
                        JOIN survey_questions q USING (question_id)
                        LEFT JOIN survey_options o USING (option_id)
                        WHERE a.response_id = r.response_id) AS answers_summary
                FROM survey_responses r
                JOIN users u ON u.user_id = r.resident_user_id
                WHERE r.survey_id = ?
                ORDER BY r.responded_at DESC, r.response_id DESC
                LIMIT ? OFFSET ?
                """;
        List<AdminSurveyItem.Response> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, surveyId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AdminSurveyItem.Response item = new AdminSurveyItem.Response();
                    item.setResponseId(rs.getLong("response_id"));
                    item.setResidentUserId(rs.getInt("resident_user_id"));
                    item.setResidentName(rs.getString("display_name"));
                    item.setRating(rs.getInt("rating"));
                    item.setAgeGroup(rs.getString("age_group_snapshot"));
                    item.setMunicipality(rs.getString("municipality_snapshot"));
                    item.setRespondedAt(getOffsetDateTime(rs, "responded_at"));
                    item.setAnswersSummary(rs.getString("answers_summary"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * テンプレートに指定できる有効業種（および既存設定の業種）の選択肢を取得する。
     * @param retainedIndustryId 既存テンプレートの業種ID。新規時はnull。
     * @return 業種ID文字列をキー、業種名を値とする選択肢マップ。
     * @throws SQLException DB照会に失敗した場合。
     */
    public Map<String, String> findIndustryChoices(Integer retainedIndustryId) throws SQLException {
        String sql = """
                SELECT i.industry_type_id, i.industry_name
                FROM industry_types i
                JOIN industry_categories c USING (category_id)
                WHERE (i.is_active AND c.is_active) OR i.industry_type_id = ?
                ORDER BY c.display_order, c.category_id, i.display_order, i.industry_type_id
                """;
        Map<String, String> choices = new LinkedHashMap<>();
        try (Connection con = openConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setObject(1, retainedIndustryId, Types.INTEGER);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    choices.put(String.valueOf(rs.getInt("industry_type_id")), rs.getString("industry_name"));
                }
            }
        }
        return choices;
    }

    /**
     * 新しい業種別アンケートテンプレートを下書きで作成し、監査ログを記録する。
     * @param actorId 操作した管理者の会員ID。
     * @param industryTypeId 対象業種ID。
     * @param title テンプレート名。
     * @return 作成されたアンケートID。
     * @throws SQLException DB更新に失敗した場合。
     */
    public int createTemplate(int actorId, int industryTypeId, String title) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                requireIndustryInTx(con, industryTypeId, null);

                int createdId;
                String insertSql = """
                        INSERT INTO surveys (industry_type_id, survey_title, survey_kind, survey_status, created_at, updated_at)
                        VALUES (?, ?, 'TEMPLATE', 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        RETURNING survey_id
                        """;
                try (PreparedStatement stmt = con.prepareStatement(insertSql)) {
                    stmt.setInt(1, industryTypeId);
                    stmt.setString(2, title);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        createdId = rs.getInt("survey_id");
                    }
                }

                auditDao.insertLog(con, actorId, "CREATE_TEMPLATE", "SURVEY",
                        Map.of("survey_id", createdId), null, Map.of("industry_type_id", industryTypeId));

                con.commit();
                con.setAutoCommit(true);
                return createdId;
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
     * 受付中のアンケートの回答受付を終了し、監査ログを記録する。
     * @param actorId 操作した管理者の会員ID。
     * @param surveyId アンケートID。
     * @param reason 停止理由。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void closeSurvey(int actorId, int surveyId, String reason) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                SurveyLock locked = lockSurveyRow(con, surveyId);
                if (!"OPEN".equals(locked.status)) {
                    throw new ManagementProblem(409, "現在受付中のアンケートを選んでください。");
                }

                String sql = "UPDATE surveys SET survey_status = 'CLOSED', updated_at = CURRENT_TIMESTAMP WHERE survey_id = ?";
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setInt(1, surveyId);
                    stmt.executeUpdate();
                }

                auditDao.insertLog(con, actorId, "CLOSE", "SURVEY",
                        Map.of("survey_id", surveyId), reason, Map.of("before", "OPEN", "after", "CLOSED"));

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
     * 公開済みまたは終了済みの来店アンケートから、設問・選択肢を複製した改訂版下書きを作成する。
     * 既に改訂版の下書きが存在する場合は重複作成せず既存のIDを返す。
     * @param actorId 操作した管理者の会員ID。
     * @param surveyId 改訂元のアンケートID。
     * @return 改訂版下書きのアンケートID。
     * @throws SQLException DB更新に失敗した場合。
     */
    public int reviseSurvey(int actorId, int surveyId) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                SurveyLock locked = lockSurveyRow(con, surveyId);
                if (!"POST_VISIT".equals(locked.kind) || "DRAFT".equals(locked.status)) {
                    throw new ManagementProblem(409, "受付を始めた来店アンケートから改訂版を作成してください。");
                }

                // 同じ元版からの改訂下書きが既にある場合は重複させずその下書きへ案内する。
                Integer existingDraft = findExistingRevisionDraft(con, surveyId);
                if (existingDraft != null) {
                    con.commit();
                    con.setAutoCommit(true);
                    return existingDraft;
                }

                int revisedId;
                String insertSql = """
                        INSERT INTO surveys (tenancy_id, survey_title, survey_kind, source_template_id,
                                             previous_survey_id, survey_status, created_at, updated_at)
                        VALUES (?, ?, 'POST_VISIT', ?, ?, 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        RETURNING survey_id
                        """;
                try (PreparedStatement stmt = con.prepareStatement(insertSql)) {
                    stmt.setObject(1, locked.tenancyId, Types.INTEGER);
                    stmt.setString(2, locked.title);
                    stmt.setObject(3, locked.sourceTemplateId, Types.INTEGER);
                    stmt.setInt(4, surveyId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        revisedId = rs.getInt("survey_id");
                    }
                }

                // 元版の設問と選択肢を改訂版へそのまま複製する。
                copyQuestionsAndOptions(con, revisedId, surveyId);

                auditDao.insertLog(con, actorId, "REVISE", "SURVEY",
                        Map.of("survey_id", revisedId), null, Map.of("previous_survey_id", surveyId));

                con.commit();
                con.setAutoCommit(true);
                return revisedId;
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
     * 下書きアンケートまたはテンプレートのタイトル・対象業種・受付予定日時を更新する。
     * @param actorId 操作した管理者の会員ID。
     * @param surveyId アンケートID。
     * @param title 新しいタイトル。
     * @param industryTypeId テンプレートの場合の対象業種ID。来店アンケート時はnull。
     * @param startsAt 来店アンケートの場合の受付開始日時。任意。
     * @param endsAt 来店アンケートの場合の受付終了日時。任意。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void updateOverview(int actorId, int surveyId, String title, Integer industryTypeId,
            OffsetDateTime startsAt, OffsetDateTime endsAt) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                SurveyLock locked = lockSurveyRow(con, surveyId);
                requireEditableDraft(con, surveyId, locked.status);

                if ("TEMPLATE".equals(locked.kind)) {
                    if (industryTypeId == null) {
                        throw new ManagementProblem(400, "対象業種の入力内容を確認してください。");
                    }
                    requireIndustryInTx(con, industryTypeId, locked.industryTypeId);
                    String sql = """
                            UPDATE surveys
                            SET survey_title = ?, industry_type_id = ?, updated_at = CURRENT_TIMESTAMP
                            WHERE survey_id = ?
                            """;
                    try (PreparedStatement stmt = con.prepareStatement(sql)) {
                        stmt.setString(1, title);
                        stmt.setInt(2, industryTypeId);
                        stmt.setInt(3, surveyId);
                        stmt.executeUpdate();
                    }
                } else {
                    if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
                        throw new ManagementProblem(400, "受付終了は開始より後にしてください。");
                    }
                    String sql = """
                            UPDATE surveys
                            SET survey_title = ?, starts_at = ?, ends_at = ?, updated_at = CURRENT_TIMESTAMP
                            WHERE survey_id = ?
                            """;
                    try (PreparedStatement stmt = con.prepareStatement(sql)) {
                        stmt.setString(1, title);
                        stmt.setObject(2, startsAt);
                        stmt.setObject(3, endsAt);
                        stmt.setInt(4, surveyId);
                        stmt.executeUpdate();
                    }
                }

                Map<String, Object> changes = new LinkedHashMap<>();
                changes.put("before_title", locked.title);
                changes.put("title", title);
                auditDao.insertLog(con, actorId, "UPDATE", "SURVEY", Map.of("survey_id", surveyId), null, changes);

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
     * 下書きアンケートの追加設問（設問2〜10）と選択肢を登録・更新する。
     * @param actorId 操作した管理者の会員ID。
     * @param surveyId アンケートID。
     * @param questionNo 設問番号（2〜10）。
     * @param questionText 設問文。
     * @param answerKind 回答形式（SINGLE / TEXT）。
     * @param required 必須設問の場合はtrue。
     * @param options 単一選択時の選択肢一覧。自由記述時は空リスト。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void saveQuestion(int actorId, int surveyId, int questionNo, String questionText,
            String answerKind, boolean required, List<String> options) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                SurveyLock locked = lockSurveyRow(con, surveyId);
                requireEditableDraft(con, surveyId, locked.status);

                int questionId;
                String upsertSql = """
                        INSERT INTO survey_questions (survey_id, question_no, question_text, answer_kind, is_required, created_at)
                        VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        ON CONFLICT (survey_id, question_no)
                        DO UPDATE SET question_text = EXCLUDED.question_text,
                                      answer_kind = EXCLUDED.answer_kind,
                                      is_required = EXCLUDED.is_required
                        RETURNING question_id
                        """;
                try (PreparedStatement stmt = con.prepareStatement(upsertSql)) {
                    stmt.setInt(1, surveyId);
                    stmt.setInt(2, questionNo);
                    stmt.setString(3, questionText);
                    stmt.setString(4, answerKind);
                    stmt.setBoolean(5, required);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        questionId = rs.getInt("question_id");
                    }
                }

                // 既存の選択肢を置き換え、表示順付きで登録し直す。
                deleteOptionsByQuestion(con, questionId);
                String optionSql = "INSERT INTO survey_options (question_id, option_text, display_order) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = con.prepareStatement(optionSql)) {
                    for (int index = 0; index < options.size(); index++) {
                        stmt.setInt(1, questionId);
                        stmt.setString(2, options.get(index));
                        stmt.setInt(3, index + 1);
                        stmt.executeUpdate();
                    }
                }

                markSurveyUpdated(con, surveyId);
                auditDao.insertLog(con, actorId, "SAVE_QUESTION", "SURVEY",
                        Map.of("survey_id", surveyId), null, Map.of("question_no", questionNo));

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
     * 下書きアンケートから指定番号の設問と選択肢を削除する。
     * @param actorId 操作した管理者の会員ID。
     * @param surveyId アンケートID。
     * @param questionNo 削除する設問番号（2〜10）。
     * @throws SQLException DB更新に失敗した場合。
     */
    public void removeQuestion(int actorId, int surveyId, int questionNo) throws SQLException {
        try (Connection con = openConnection()) {
            con.setAutoCommit(false);
            try {
                requireAdminInTx(con, actorId);
                SurveyLock locked = lockSurveyRow(con, surveyId);
                requireEditableDraft(con, surveyId, locked.status);

                int questionId = findQuestionId(con, surveyId, questionNo);
                deleteOptionsByQuestion(con, questionId);
                String deleteQuestionSql = "DELETE FROM survey_questions WHERE question_id = ?";
                try (PreparedStatement stmt = con.prepareStatement(deleteQuestionSql)) {
                    stmt.setInt(1, questionId);
                    stmt.executeUpdate();
                }

                markSurveyUpdated(con, surveyId);
                auditDao.insertLog(con, actorId, "REMOVE_QUESTION", "SURVEY",
                        Map.of("survey_id", surveyId), null, Map.of("question_no", questionNo));

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
     * 更新対象のアンケート行を排他ロックして主要項目を取得する。
     * @param con トランザクション中の接続。
     * @param surveyId アンケートID。
     * @return ロックしたアンケート情報。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static SurveyLock lockSurveyRow(Connection con, int surveyId) throws SQLException {
        String sql = """
                SELECT survey_status, survey_kind, survey_title, tenancy_id, industry_type_id, source_template_id
                FROM surveys
                WHERE survey_id = ?
                FOR UPDATE
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, surveyId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定されたアンケートが見つかりません。");
                }
                return new SurveyLock(
                        rs.getString("survey_status"),
                        rs.getString("survey_kind"),
                        rs.getString("survey_title"),
                        getNullableInt(rs, "tenancy_id"),
                        getNullableInt(rs, "industry_type_id"),
                        getNullableInt(rs, "source_template_id"));
            }
        }
    }

    /**
     * 回答の付いていない下書きであることを確認する。条件外の場合は409例外を送出する。
     * @param con トランザクション中の接続。
     * @param surveyId アンケートID。
     * @param status ロック時点の受付状態。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static void requireEditableDraft(Connection con, int surveyId, String status) throws SQLException {
        boolean hasResponse = false;
        String sql = "SELECT response_id FROM survey_responses WHERE survey_id = ? LIMIT 1";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, surveyId);
            try (ResultSet rs = stmt.executeQuery()) {
                hasResponse = rs.next();
            }
        }
        if (!"DRAFT".equals(status) || hasResponse) {
            throw new ManagementProblem(409, "編集できるのは回答のない下書きです。改訂版を作成して編集してください。");
        }
    }

    /**
     * テンプレートに指定された業種が有効または既存設定と同一であることを確認する。
     * @param con トランザクション中の接続。
     * @param industryTypeId 選択された業種ID。
     * @param retainedIndustryId 既存テンプレートの業種ID。新規時はnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static void requireIndustryInTx(Connection con, int industryTypeId, Integer retainedIndustryId)
            throws SQLException {
        String sql = """
                SELECT i.industry_type_id
                FROM industry_types i
                JOIN industry_categories c USING (category_id)
                WHERE i.industry_type_id = ?
                  AND ((i.is_active AND c.is_active) OR i.industry_type_id = ?)
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, industryTypeId);
            stmt.setObject(2, retainedIndustryId, Types.INTEGER);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定された対象業種が見つかりません。");
                }
            }
        }
    }

    /**
     * 同じ元アンケートから作成済みの改訂下書きがあれば最新のIDを返す。
     * @param con トランザクション中の接続。
     * @param previousSurveyId 改訂元のアンケートID。
     * @return 既存改訂下書きのID。なければnull。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static Integer findExistingRevisionDraft(Connection con, int previousSurveyId) throws SQLException {
        String sql = """
                SELECT survey_id
                FROM surveys
                WHERE previous_survey_id = ? AND survey_status = 'DRAFT'
                ORDER BY survey_id DESC
                LIMIT 1
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, previousSurveyId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("survey_id") : null;
            }
        }
    }

    /**
     * 改訂元の設問および選択肢を改訂版アンケートへ複製する。
     * @param con トランザクション中の接続。
     * @param revisedSurveyId 改訂版のアンケートID。
     * @param sourceSurveyId 改訂元のアンケートID。
     * @throws SQLException DB更新に失敗した場合。
     */
    private static void copyQuestionsAndOptions(Connection con, int revisedSurveyId, int sourceSurveyId)
            throws SQLException {
        String copyQuestionsSql = """
                INSERT INTO survey_questions (survey_id, question_no, question_text, answer_kind, is_required, created_at)
                SELECT ?, question_no, question_text, answer_kind, is_required, CURRENT_TIMESTAMP
                FROM survey_questions
                WHERE survey_id = ?
                """;
        try (PreparedStatement stmt = con.prepareStatement(copyQuestionsSql)) {
            stmt.setInt(1, revisedSurveyId);
            stmt.setInt(2, sourceSurveyId);
            stmt.executeUpdate();
        }

        String copyOptionsSql = """
                INSERT INTO survey_options (question_id, option_text, display_order)
                SELECT copied.question_id, o.option_text, o.display_order
                FROM survey_options o
                JOIN survey_questions original ON original.question_id = o.question_id
                JOIN survey_questions copied ON copied.survey_id = ? AND copied.question_no = original.question_no
                WHERE original.survey_id = ?
                """;
        try (PreparedStatement stmt = con.prepareStatement(copyOptionsSql)) {
            stmt.setInt(1, revisedSurveyId);
            stmt.setInt(2, sourceSurveyId);
            stmt.executeUpdate();
        }
    }

    /**
     * アンケートIDと設問番号から設問IDを取得する。存在しない場合は404例外を送出する。
     * @param con トランザクション中の接続。
     * @param surveyId アンケートID。
     * @param questionNo 設問番号。
     * @return 設問ID。
     * @throws SQLException DB照会に失敗した場合。
     */
    private static int findQuestionId(Connection con, int surveyId, int questionNo) throws SQLException {
        String sql = "SELECT question_id FROM survey_questions WHERE survey_id = ? AND question_no = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, surveyId);
            stmt.setInt(2, questionNo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ManagementProblem(404, "指定された設問が見つかりません。");
                }
                return rs.getInt("question_id");
            }
        }
    }

    /**
     * 指定設問に紐づく旧選択肢をすべて削除する。
     * @param con トランザクション中の接続。
     * @param questionId 設問ID。
     * @throws SQLException DB更新に失敗した場合。
     */
    private static void deleteOptionsByQuestion(Connection con, int questionId) throws SQLException {
        String sql = "DELETE FROM survey_options WHERE question_id = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, questionId);
            stmt.executeUpdate();
        }
    }

    /**
     * アンケート本体の更新日時を現在時刻へ更新する。
     * @param con トランザクション中の接続。
     * @param surveyId アンケートID。
     * @throws SQLException DB更新に失敗した場合。
     */
    private static void markSurveyUpdated(Connection con, int surveyId) throws SQLException {
        String sql = "UPDATE surveys SET updated_at = CURRENT_TIMESTAMP WHERE survey_id = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, surveyId);
            stmt.executeUpdate();
        }
    }

    /**
     * ロックしたアンケート行の更新判定用スナップショット。
     */
    private static final class SurveyLock {
        /** 受付状態。 */
        private final String status;
        /** 種別。 */
        private final String kind;
        /** タイトル。 */
        private final String title;
        /** 契約ID。 */
        private final Integer tenancyId;
        /** 対象業種ID。 */
        private final Integer industryTypeId;
        /** 作成元テンプレートID。 */
        private final Integer sourceTemplateId;

        /**
         * ロック結果を保持するインスタンスを生成する。
         * @param status 受付状態。
         * @param kind 種別。
         * @param title タイトル。
         * @param tenancyId 契約ID。
         * @param industryTypeId 対象業種ID。
         * @param sourceTemplateId 作成元テンプレートID。
         */
        private SurveyLock(String status, String kind, String title,
                Integer tenancyId, Integer industryTypeId, Integer sourceTemplateId) {
            this.status = status;
            this.kind = kind;
            this.title = title;
            this.tenancyId = tenancyId;
            this.industryTypeId = industryTypeId;
            this.sourceTemplateId = sourceTemplateId;
        }
    }
}
