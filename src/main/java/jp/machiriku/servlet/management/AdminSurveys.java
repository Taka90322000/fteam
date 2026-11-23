package jp.machiriku.servlet.management;

import static jp.machiriku.servlet.management.ManagementDisplay.options;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jp.machiriku.bean.management.AdminSurveyView;
import jp.machiriku.dao.management.AdminSurveyDao;
import jp.machiriku.dao.management.ManagementDao;

/** 来店アンケートの確認・集計・受付停止と、テンプレート・改訂版の管理を行う。 */
final class AdminSurveys {
    /** アンケートの種類と利用者向けの名称。 */
    private static final Map<String, String> KINDS = options("POST_VISIT", "来店アンケート", "TEMPLATE", "テンプレート");
    /** 公開・受付の状態。 */
    private static final Map<String, String> STATES = options("DRAFT", "下書き", "OPEN", "受付中", "CLOSED", "受付終了");

    /** 画面の処理を静的に利用する。 */
    private AdminSurveys() { }

    /**
     * アンケートの一覧、作成画面または詳細のデータを取得する。
     * @param db アンケートを照会するDAO接続。
     * @param input 表示するアンケートID、タブ、検索条件。
     * @return アンケートのJSPと表示データ。
     * @throws SQLException アンケートを取得できなかった場合。
     */
    static ManagementView page(ManagementDao db, ManagementRequest input) throws SQLException {
        // アンケートを指定していない場合は、一覧または新規テンプレートを表示する。
        Integer id = input.id("id", false);
        if (id == null) {
            return list(db, input);
        }
        var survey = AdminSurveyDao.survey(db, id);
        String state = survey.get("受付状態").toString();
        String kind = survey.get("種類").toString();
        String tab = input.value("tab").isEmpty() ? "overview" : input.choice("tab", "表示する情報", "overview", "questions", "responses");
        boolean editable = "DRAFT".equals(state) && ManagementDisplay.key(survey, "回答数") == 0;

        // 選択されたタブで参照できる集計や編集対象のデータを読み込む。
        AdminSurveyView.Tab detail = switch (tab) {
            case "questions" -> questions(db, input, id, editable);
            case "responses" -> responses(db, input, id);
            default -> new AdminSurveyView.Overview(
                    editable && "TEMPLATE".equals(kind) ? industries(db, (Integer) survey.get("対象業種ID")) : Map.of(),
                    "OPEN".equals(state), "POST_VISIT".equals(kind) && !"DRAFT".equals(state));
        };
        survey.put("受付状態", STATES.get(state));
        survey.put("種類", KINDS.get(kind));
        return new ManagementView("admin/surveys/detail.jsp", survey.get("名前").toString(),
                new AdminSurveyView.Detail(id, kind, state, tab, survey, editable, detail));
    }

    /**
     * 検索条件に合うアンケート、または新規テンプレートの業種候補を取得する。
     * @param db アンケートと業種を参照するDAO接続。
     * @param input 新規作成の指定、または一覧の検索条件。
     * @return 対応するJSPと取得結果。
     * @throws SQLException アンケートや対象業種を取得できなかった場合。
     */
    private static ManagementView list(ManagementDao db, ManagementRequest input) throws SQLException {
        // テンプレートを新規登録する場合だけ、対象業種の一覧を必要とする。
        if ("1".equals(input.value("new"))) {
            return new ManagementView("admin/surveys/template.jsp", "アンケート管理",
                    new AdminSurveyView.Template(industries(db, null)));
        }
        String kind = input.value("kind");
        String state = input.value("status");
        String query = input.search();
        var rows = AdminSurveyDao.surveys(db, kind, state, query, ManagementRequest.PAGE_SIZE + 1, input.offset());
        for (var row : rows) {
            row.put("種類", KINDS.get(row.get("種類")));
            row.put("受付状態", STATES.get(row.get("受付状態")));
        }
        return new ManagementView("admin/surveys/list.jsp", "アンケート管理",
                new AdminSurveyView.Listing(query, kind, state, KINDS, STATES, input.pageRows(rows)));
    }

    /**
     * 追加設問の一覧と、編集対象の設問の値を取得する。
     * @param db 設問を照会するDAO接続。
     * @param input 選んだ設問の番号。
     * @param id アンケートID。
     * @param editable 下書きを編集できる場合はtrue。
     * @return 設問タブの一覧と初期値。
     * @throws SQLException 設問を取得できなかった場合。
     */
    private static AdminSurveyView.Questions questions(ManagementDao db, ManagementRequest input, int id, boolean editable)
            throws SQLException {
        // 編集ができる場合にだけ、指定された設問を選択する。
        var rows = AdminSurveyDao.questions(db, id);
        Integer number = editable ? input.id("question", false) : null;
        var selected = rows.stream().filter(row -> number != null && number == ((Number) row.get("設問番号")).intValue())
                .findFirst().orElse(Map.of());
        return new AdminSurveyView.Questions(rows, number, selected);
    }

    /**
     * 回答タブに表示する満足度分布、選択肢別集計と回答履歴を取得する。
     * @param db 回答を参照するDAO接続。
     * @param input 回答履歴のページ。
     * @param id アンケートID。
     * @return 回答と集計データ。
     * @throws SQLException 集計や回答を取得できなかった場合。
     */
    private static AdminSurveyView.Responses responses(ManagementDao db, ManagementRequest input, int id) throws SQLException {
        // 集計には全件を使い、回答の明細にはページ単位の行だけを渡す。
        return new AdminSurveyView.Responses(AdminSurveyDao.ratingDistribution(db, id), AdminSurveyDao.optionCounts(db, id),
                input.pageRows(AdminSurveyDao.responses(db, id, ManagementRequest.PAGE_SIZE + 1, input.offset())));
    }

    /**
     * テンプレート・アンケートの状態と詳細を更新する。
     * @param db トランザクション内の接続。
     * @param input 編集フォーム。
     * @param actorId 操作する管理者の会員ID。
     * @return 保存後のアンケートURL。
     * @throws SQLException 保存できなかった場合。
     */
    static String change(ManagementDao db, ManagementRequest input, int actorId) throws SQLException {
        String operation = input.choice("operation", "操作", "template", "edit", "close", "revise", "question", "removeQuestion");
        if ("template".equals(operation)) {
            int industry = input.id("industry", true);
            requireIndustry(db, industry, null);
            var created = AdminSurveyDao.insertTemplate(db, industry, input.text("title", "テンプレート名", 200, true));
            int id = (Integer) created.get("survey_id");
            db.audit(actorId, "CREATE_TEMPLATE", "SURVEY", Map.of("survey_id", id), null, Map.of("industry_type_id", industry));
            return ManagementPaths.ADMIN_SURVEYS + "?id=" + id + "&tab=questions";
        }
        int id = input.id("id", true);
        // 変更はアンケート行で直列化し、受付中または回答済みの設問を上書きしない。
        var before = AdminSurveyDao.lockSurvey(db, id);
        String state = before.get("survey_status").toString();
        if ("close".equals(operation)) {
            if (!"OPEN".equals(state)) { throw new ManagementProblem(409, "現在受付中のアンケートを選んでください。"); }
            AdminSurveyDao.closeSurvey(db, id);
            db.audit(actorId, "CLOSE", "SURVEY", Map.of("survey_id", id), input.text("reason", "停止理由", 2000, true), Map.of("before", "OPEN", "after", "CLOSED"));
            return ManagementPaths.ADMIN_SURVEYS + "?id=" + id;
        }
        if ("revise".equals(operation)) { return revise(db, before, id, actorId); }
        if (!"DRAFT".equals(state) || !AdminSurveyDao.responseKeys(db, id).isEmpty()) {
            throw new ManagementProblem(409, "編集できるのは回答のない下書きです。改訂版を作成して編集してください。");
        }
        if ("edit".equals(operation)) {
            saveOverview(db, input, before, id, actorId);
            return ManagementPaths.ADMIN_SURVEYS + "?id=" + id;
        }
        saveQuestion(db, input, id, actorId, "removeQuestion".equals(operation));
        return ManagementPaths.ADMIN_SURVEYS + "?id=" + id + "&tab=questions";
    }

    /**
     * 元の設問・選択肢を引き継いだ下書きを作る。
     * @param db トランザクション内の接続。
     * @param before ロックした改訂元の行。
     * @param id 改訂元のアンケートID。
     * @param actorId 操作する管理者の会員ID。
     * @return 改訂版の編集先。
     * @throws SQLException 改訂版を保存できなかった場合。
     */
    private static String revise(ManagementDao db, Map<String, Object> before, int id, int actorId) throws SQLException {
        if (!"POST_VISIT".equals(before.get("survey_kind")) || "DRAFT".equals(before.get("survey_status"))) {
            throw new ManagementProblem(409, "受付を始めた来店アンケートから改訂版を作成してください。");
        }
        // 同じ元版を繰り返し送信した場合は、既にある改訂作業中の下書きへ戻す。
        var drafts = AdminSurveyDao.revisionDrafts(db, id);
        if (!drafts.isEmpty()) { return ManagementPaths.ADMIN_SURVEYS + "?id=" + drafts.getFirst().get("survey_id") + "&tab=questions"; }
        int revised = (Integer) AdminSurveyDao.insertRevision(db, (Integer) before.get("tenancy_id"), (String) before.get("survey_title"), (Integer) before.get("source_template_id"), id).get("survey_id");
        AdminSurveyDao.copyQuestions(db, revised, id);
        AdminSurveyDao.copyOptions(db, revised, id);
        db.audit(actorId, "REVISE", "SURVEY", Map.of("survey_id", revised), null, Map.of("previous_survey_id", id));
        return ManagementPaths.ADMIN_SURVEYS + "?id=" + revised + "&tab=questions";
    }

    /**
     * 編集中のアンケートのタイトル・受付予定・対象業種を保存する。
     * @param db トランザクション内の接続。
     * @param input フォーム。
     * @param before 更新前の値。
     * @param id アンケートID。
     * @param actorId 操作した管理者の会員ID。
     * @throws SQLException 変更を保存できなかった場合。
     */
    private static void saveOverview(ManagementDao db, ManagementRequest input, Map<String, Object> before, int id, int actorId) throws SQLException {
        String title = input.text("title", "タイトル", 200, true);
        if ("TEMPLATE".equals(before.get("survey_kind"))) {
            int industry = input.id("industry", true);
            requireIndustry(db, industry, (Integer) before.get("industry_type_id"));
            AdminSurveyDao.updateTemplate(db, title, industry, id);
        } else {
            var starts = input.dateTime("starts", "受付開始", false);
            var ends = input.dateTime("ends", "受付終了", false);
            if (starts != null && ends != null && !ends.isAfter(starts)) { throw new ManagementProblem(400, "受付終了は開始より後にしてください。"); }
            AdminSurveyDao.updateSchedule(db, title, starts, ends, id);
        }
        db.audit(actorId, "UPDATE", "SURVEY", Map.of("survey_id", id), null, Map.of("before_title", before.get("survey_title"), "title", title));
    }

    /**
     * 下書きの設問一件と選択肢を更新する。
     * @param db トランザクション内の接続。
     * @param input 設問番号・内容・回答形式。
     * @param id アンケートID。
     * @param actorId 操作した管理者の会員ID。
     * @param remove 設問を削除する場合にtrue。
     * @throws SQLException 設問を保存できなかった場合。
     */
    private static void saveQuestion(ManagementDao db, ManagementRequest input, int id, int actorId, boolean remove) throws SQLException {
        int number = input.number("question", "設問番号", 2, 10);
        if (remove) {
            var row = AdminSurveyDao.questionKey(db, id, number);
            // 下書きの設問だけを対象とし、選択肢と設問を同時に取り除く。
            AdminSurveyDao.deleteOptions(db, (Integer) row.get("question_id"));
            AdminSurveyDao.deleteQuestion(db, (Integer) row.get("question_id"));
        } else {
            String kind = input.choice("answerKind", "回答形式", "SINGLE", "TEXT");
            boolean required = Boolean.parseBoolean(input.choice("required", "必須区分", "true", "false"));
            List<String> choices = Arrays.stream(input.value("options").split("\\R")).map(String::strip).filter(value -> !value.isEmpty()).toList();
            if ("TEXT".equals(kind)) {
                if (required || !choices.isEmpty()) { throw new ManagementProblem(400, "自由記述は任意に設定し、選択肢を空にしてください。"); }
            } else if (choices.size() < 2 || choices.size() > 8 || choices.stream().distinct().count() != choices.size()
                    || choices.stream().anyMatch(value -> value.codePointCount(0, value.length()) > 200 || value.indexOf('\0') >= 0)) {
                throw new ManagementProblem(400, "単一選択には、200文字以内の異なる選択肢を2〜8個入力してください。");
            }
            int questionId = (Integer) AdminSurveyDao.upsertQuestion(db, id, number, input.text("questionText", "設問文", 300, true), kind, required).get("question_id");
            AdminSurveyDao.deleteOptions(db, questionId);
            for (int index = 0; index < choices.size(); index++) {
                AdminSurveyDao.insertOption(db, questionId, choices.get(index), index + 1);
            }
        }
        AdminSurveyDao.markUpdated(db, id);
        db.audit(actorId, remove ? "REMOVE_QUESTION" : "SAVE_QUESTION", "SURVEY", Map.of("survey_id", id), null, Map.of("question_no", number));
    }

    /**
     * テンプレートに指定できる業種を取得する。
     * @param db DB接続。
     * @param retained 既存テンプレートが使っている業種ID。未選択ならnull。
     * @return 選択肢のIDと業種名。
     * @throws SQLException マスターを取得できなかった場合。
     */
    private static Map<String, String> industries(ManagementDao db, Integer retained) throws SQLException {
        return ManagementDisplay.choices(AdminSurveyDao.industryChoices(db, retained));
    }

    /**
     * 選ばれた業種が有効か、既存の紐付けと同じであることを確認する。
     * @param db DB接続。
     * @param id 選択した業種ID。
     * @param retained 既存の業種ID。新規ならnull。
     * @throws SQLException マスターを確認できなかった場合。
     */
    private static void requireIndustry(ManagementDao db, int id, Integer retained) throws SQLException {
        AdminSurveyDao.requireIndustry(db, id, retained);
    }
}
