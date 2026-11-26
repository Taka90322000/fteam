package jp.machiriku.servlet.management;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminSurveyItem;
import jp.machiriku.bean.management.ManagementLabels;
import jp.machiriku.bean.management.ManagementProblem;
import jp.machiriku.bean.management.PageResult;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.dao.management.AdminSurveyDao;
import jp.machiriku.tool.Action;

/** 管理者がアンケートの確認・集計・受付停止およびテンプレート・改訂版の作成と設問編集を行うAction。 */
public final class AdminSurveysAction extends Action {
    /** 追加設問の設問番号選択肢（設問1は固定の総合満足度）。 */
    private static final Map<String, String> QUESTION_SLOTS = ManagementFormat.options(
            "2", "設問 2", "3", "設問 3", "4", "設問 4",
            "5", "設問 5", "6", "設問 6", "7", "設問 7",
            "8", "設問 8", "9", "設問 9", "10", "設問 10");
    /** 追加設問の回答形式選択肢。 */
    private static final Map<String, String> ANSWER_KINDS = ManagementFormat.options(
            "SINGLE", "単一選択", "TEXT", "自由記述");
    /** 追加設問の必須区分選択肢。 */
    private static final Map<String, String> REQUIRED_CHOICES = ManagementFormat.options(
            "true", "必須", "false", "任意");
    /** 詳細画面のタブ選択肢。 */
    private static final Map<String, String> DETAIL_TABS = ManagementFormat.options(
            "overview", "アンケート情報", "questions", "設問・選択肢", "responses", "回答・集計");

    /** 管理者権限チェックに使うDAO。 */
    private final AdminAuditDao auditDao;
    /** アンケート管理DAO。 */
    private final AdminSurveyDao surveyDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminSurveysAction() {
        this(new AdminAuditDao(), new AdminSurveyDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param auditDao 管理者権限チェックDAO。
     * @param surveyDao アンケート管理DAO。
     */
    AdminSurveysAction(AdminAuditDao auditDao, AdminSurveyDao surveyDao) {
        this.auditDao = auditDao;
        this.surveyDao = surveyDao;
    }

    /**
     * GETではアンケート一覧・テンプレート作成画面・詳細を表示し、POSTでは作成・停止・改訂・設問編集を実行する。
     * @param req FrontControllerから受け取るHTTPリクエスト。
     * @param res 画面表示、エラー応答や更新後の遷移に使用するHTTPレスポンス。
     * @throws Exception 画面処理を完了できない場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 1. 管理者権限を確認する。
        User admin = ManagementAuth.requireAdmin(req, res, auditDao);
        if (admin == null) {
            return;
        }

        // 2. POSTの場合はテンプレート作成・受付停止・改訂・編集を実行する。
        if ("POST".equals(req.getMethod())) {
            handlePost(req, res, admin);
            return;
        }

        // 3. GET/HEADでは一覧・新規テンプレート・詳細へ振り分ける。
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }
        try {
            FormReader input = new FormReader(req);
            Integer surveyId = input.intId("id", false);
            if (surveyId != null) {
                showDetail(req, res, input, surveyId);
            } else if ("1".equals(input.value("new"))) {
                showTemplateForm(req, res);
            } else {
                showList(req, res, input);
            }
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * アンケート一覧画面を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showList(HttpServletRequest req, HttpServletResponse res, FormReader input) throws Exception {
        String kind = input.value("kind");
        String status = input.value("status");
        String query = input.search();
        List<AdminSurveyItem> rows = surveyDao.findSurveys(
                kind, status, query, FormReader.PAGE_SIZE + 1, input.offset());
        PageResult<AdminSurveyItem> pageResult = input.toPageResult(rows);

        ManagementAuth.preparePage(req, "アンケート管理", true, true);
        req.setAttribute("query", query);
        req.setAttribute("kind", kind);
        req.setAttribute("status", status);
        req.setAttribute("kinds", ManagementLabels.SURVEY_KINDS);
        req.setAttribute("states", ManagementLabels.SURVEY_STATES);
        req.setAttribute("surveyPage", pageResult);
        req.getRequestDispatcher("/WEB-INF/views/management/admin/surveys/list.jsp").forward(req, res);
    }

    /**
     * 新規テンプレート作成画面を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showTemplateForm(HttpServletRequest req, HttpServletResponse res) throws Exception {
        ManagementAuth.preparePage(req, "アンケート管理", true, true);
        req.setAttribute("industries", surveyDao.findIndustryChoices(null));
        req.getRequestDispatcher("/WEB-INF/views/management/admin/surveys/template.jsp").forward(req, res);
    }

    /**
     * アンケート詳細画面と選択されたタブ（概要・設問・回答集計）を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @param surveyId アンケートID。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showDetail(HttpServletRequest req, HttpServletResponse res, FormReader input, int surveyId)
            throws Exception {
        AdminSurveyItem survey = surveyDao.findSurveyById(surveyId);
        if (survey == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
            return;
        }
        String tab = input.value("tab").isEmpty()
                ? "overview"
                : input.choice("tab", "表示する情報", "overview", "questions", "responses");

        ManagementAuth.preparePage(req, survey.getSurveyTitle(), true, true);
        req.setAttribute("survey", survey);
        req.setAttribute("tab", tab);
        req.setAttribute("tabs", DETAIL_TABS);

        if ("questions".equals(tab)) {
            List<AdminSurveyItem.Question> questions = surveyDao.findQuestions(surveyId);
            Integer selectedNo = survey.isEditable() ? input.intId("question", false) : null;
            AdminSurveyItem.Question selected = null;
            if (selectedNo != null) {
                for (AdminSurveyItem.Question item : questions) {
                    if (item.getQuestionNo() == selectedNo) {
                        selected = item;
                        break;
                    }
                }
            }
            req.setAttribute("questions", questions);
            req.setAttribute("selectedNo", selectedNo);
            req.setAttribute("selectedQuestion", selected);
            req.setAttribute("questionSlots", QUESTION_SLOTS);
            req.setAttribute("answerKinds", ANSWER_KINDS);
            req.setAttribute("requiredChoices", REQUIRED_CHOICES);
        } else if ("responses".equals(tab)) {
            req.setAttribute("ratings", surveyDao.findRatingDistribution(surveyId));
            req.setAttribute("optionCounts", surveyDao.findOptionCounts(surveyId));
            req.setAttribute("responsePage", input.toPageResult(
                    surveyDao.findResponses(surveyId, FormReader.PAGE_SIZE + 1, input.offset())));
        } else {
            if (survey.isEditable() && survey.isTemplate()) {
                req.setAttribute("industries", surveyDao.findIndustryChoices(survey.getIndustryTypeId()));
            }
        }

        req.getRequestDispatcher("/WEB-INF/views/management/admin/surveys/detail.jsp").forward(req, res);
    }

    /**
     * アンケートの各種更新フォーム送信を処理する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param admin 操作する管理者。
     * @throws Exception 更新処理を完了できない場合。
     */
    private void handlePost(HttpServletRequest req, HttpServletResponse res, User admin) throws Exception {
        if (!ManagementAuth.verifyCsrf(req, res, true)) {
            return;
        }
        try {
            FormReader input = new FormReader(req);
            String operation = input.choice("operation", "操作",
                    "template", "edit", "close", "revise", "question", "removeQuestion");

            if ("template".equals(operation)) {
                int industryId = input.intId("industry", true);
                String title = input.text("title", "テンプレート名", 200, true);
                int createdId = surveyDao.createTemplate(admin.getUserId(), industryId, title);
                ManagementAuth.redirectSaved(req, res,
                        ManagementPaths.ADMIN_SURVEYS + "?id=" + createdId + "&tab=questions");
                return;
            }

            int surveyId = input.intId("id", true);
            if ("close".equals(operation)) {
                String reason = input.text("reason", "停止理由", 2000, true);
                surveyDao.closeSurvey(admin.getUserId(), surveyId, reason);
                ManagementAuth.redirectSaved(req, res, ManagementPaths.ADMIN_SURVEYS + "?id=" + surveyId);
                return;
            }

            if ("revise".equals(operation)) {
                int revisedId = surveyDao.reviseSurvey(admin.getUserId(), surveyId);
                ManagementAuth.redirectSaved(req, res,
                        ManagementPaths.ADMIN_SURVEYS + "?id=" + revisedId + "&tab=questions");
                return;
            }

            if ("edit".equals(operation)) {
                String title = input.text("title", "タイトル", 200, true);
                Integer industryId = input.intId("industry", false);
                OffsetDateTime startsAt = input.dateTime("starts", "受付開始", false);
                OffsetDateTime endsAt = input.dateTime("ends", "受付終了", false);
                surveyDao.updateOverview(admin.getUserId(), surveyId, title, industryId, startsAt, endsAt);
                ManagementAuth.redirectSaved(req, res, ManagementPaths.ADMIN_SURVEYS + "?id=" + surveyId);
                return;
            }

            int questionNo = input.number("question", "設問番号", 2, 10);
            if ("removeQuestion".equals(operation)) {
                surveyDao.removeQuestion(admin.getUserId(), surveyId, questionNo);
            } else {
                String answerKind = input.choice("answerKind", "回答形式", "SINGLE", "TEXT");
                boolean required = Boolean.parseBoolean(input.choice("required", "必須区分", "true", "false"));
                List<String> options = parseOptions(input.value("options"));
                validateQuestionOptions(answerKind, required, options);
                String questionText = input.text("questionText", "設問文", 300, true);
                surveyDao.saveQuestion(admin.getUserId(), surveyId, questionNo, questionText, answerKind, required, options);
            }
            ManagementAuth.redirectSaved(req, res, ManagementPaths.ADMIN_SURVEYS + "?id=" + surveyId + "&tab=questions");
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * 改行区切りの選択肢入力から空行を除いたリストを作成する。
     * @param raw フォームから送信された選択肢テキスト。
     * @return 前後の空白を除いた非空の選択肢リスト。
     */
    private static List<String> parseOptions(String raw) {
        return Arrays.stream(raw.split("\\R"))
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    /**
     * 回答形式ごとの必須区分・選択肢件数・重複・文字数の制約を検証する。
     * @param answerKind 回答形式（SINGLE / TEXT）。
     * @param required 必須区分。
     * @param options 選択肢一覧。
     */
    private static void validateQuestionOptions(String answerKind, boolean required, List<String> options) {
        if ("TEXT".equals(answerKind)) {
            if (required || !options.isEmpty()) {
                throw new ManagementProblem(400, "自由記述は任意に設定し、選択肢を空にしてください。");
            }
            return;
        }
        boolean invalidLengthOrNull = options.stream()
                .anyMatch(value -> value.codePointCount(0, value.length()) > 200 || value.indexOf('\0') >= 0);
        if (options.size() < 2
                || options.size() > 8
                || options.stream().distinct().count() != options.size()
                || invalidLengthOrNull) {
            throw new ManagementProblem(400, "単一選択には、200文字以内の異なる選択肢を2〜8個入力してください。");
        }
    }
}
