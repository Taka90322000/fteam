package jp.machiriku.servlet.management;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminQualificationItem;
import jp.machiriku.bean.management.ManagementLabels;
import jp.machiriku.bean.management.PageResult;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.dao.management.AdminCatalogDao;
import jp.machiriku.tool.Action;

/** 管理者が資格・書類のマスターと業種別の必要条件を確認・更新するAction。 */
public final class AdminQualificationsAction extends Action {
    /** 有効状態の選択肢。 */
    private static final Map<String, String> ACTIVE_CHOICES = ManagementFormat.options("true", "有効", "false", "無効");
    /** 必要条件の必須区分選択肢。 */
    private static final Map<String, String> REQUIRED_CHOICES = ManagementFormat.options("true", "必須", "false", "任意");

    /** 管理者権限チェックに使うDAO。 */
    private final AdminAuditDao auditDao;
    /** 資格・書類・必要条件マスターDAO。 */
    private final AdminCatalogDao catalogDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminQualificationsAction() {
        this(new AdminAuditDao(), new AdminCatalogDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param auditDao 管理者権限チェックDAO。
     * @param catalogDao 資格・書類マスターDAO。
     */
    AdminQualificationsAction(AdminAuditDao auditDao, AdminCatalogDao catalogDao) {
        this.auditDao = auditDao;
        this.catalogDao = catalogDao;
    }

    /**
     * GETでは資格・書類マスター一覧、登録・編集画面、または業種別必要条件画面を表示し、POSTでは保存・解除を実行する。
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

        // 2. POSTの場合は資格マスター保存または業種別必要条件の登録・解除を実行する。
        if ("POST".equals(req.getMethod())) {
            handlePost(req, res, admin);
            return;
        }

        // 3. GET/HEADではタブとIDに応じて表示画面を振り分ける。
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }
        try {
            FormReader input = new FormReader(req);
            if ("requirements".equals(input.value("tab"))) {
                showRequirements(req, res, input);
                return;
            }
            Integer id = input.intId("id", false);
            if (id != null || "1".equals(input.value("new"))) {
                showEdit(req, res, id);
            } else {
                showList(req, res, input);
            }
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * 資格・書類マスターの検索・一覧画面を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showList(HttpServletRequest req, HttpServletResponse res, FormReader input) throws Exception {
        String kind = input.value("kind");
        String query = input.search();
        String status = input.value("status");
        List<AdminQualificationItem> rows = catalogDao.findQualifications(
                kind, status, query, FormReader.PAGE_SIZE + 1, input.offset());
        PageResult<AdminQualificationItem> pageResult = input.toPageResult(rows);

        ManagementAuth.preparePage(req, "必要資格・書類管理", true, true);
        req.setAttribute("kind", kind);
        req.setAttribute("query", query);
        req.setAttribute("status", status);
        req.setAttribute("kinds", ManagementLabels.QUALIFICATION_KINDS);
        req.setAttribute("activeChoices", ACTIVE_CHOICES);
        req.setAttribute("qualificationPage", pageResult);
        req.getRequestDispatcher("/WEB-INF/views/management/admin/qualifications/list.jsp").forward(req, res);
    }

    /**
     * 資格・書類マスターの新規登録・編集画面を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param id 編集対象ID。新規登録時はnull。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showEdit(HttpServletRequest req, HttpServletResponse res, Integer id) throws Exception {
        AdminQualificationItem current;
        if (id == null) {
            current = new AdminQualificationItem();
        } else {
            current = catalogDao.findQualificationById(id);
            if (current == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
                return;
            }
        }

        ManagementAuth.preparePage(req, "必要資格・書類管理", true, true);
        req.setAttribute("editId", id);
        req.setAttribute("current", current);
        req.setAttribute("kinds", ManagementLabels.QUALIFICATION_KINDS);
        req.setAttribute("activeChoices", ACTIVE_CHOICES);
        req.getRequestDispatcher("/WEB-INF/views/management/admin/qualifications/edit.jsp").forward(req, res);
    }

    /**
     * 業種ごとの必要資格・書類割り当て画面を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showRequirements(HttpServletRequest req, HttpServletResponse res, FormReader input) throws Exception {
        Integer industryId = input.intId("industry", false);
        Integer qualificationId = input.intId("id", false);
        Map<String, String> industries = catalogDao.findIndustryChoices();

        ManagementAuth.preparePage(req, "必要資格・書類管理", true, true);
        req.setAttribute("industryId", industryId);
        req.setAttribute("industries", industries);
        req.setAttribute("qualificationId", qualificationId);
        req.setAttribute("requiredChoices", REQUIRED_CHOICES);

        if (industryId != null) {
            if (!catalogDao.existsIndustryType(industryId)) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された業種が見つかりません。");
                return;
            }
            List<AdminQualificationItem> rows = catalogDao.findRequirements(
                    industryId, FormReader.PAGE_SIZE + 1, input.offset());
            AdminQualificationItem current;
            if (qualificationId == null) {
                current = new AdminQualificationItem();
            } else {
                current = catalogDao.findRequirement(industryId, qualificationId);
                if (current == null) {
                    res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された必要条件が見つかりません。");
                    return;
                }
            }
            req.setAttribute("requirementPage", input.toPageResult(rows));
            req.setAttribute("current", current);
            req.setAttribute("qualificationChoices", catalogDao.findRequirementChoices(industryId));
        }

        req.getRequestDispatcher("/WEB-INF/views/management/admin/qualifications/requirements.jsp").forward(req, res);
    }

    /**
     * 資格マスター保存および業種別必要条件の登録・解除を処理する。
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
            String operation = input.choice("operation", "操作", "save", "requirement", "removeRequirement");

            if ("save".equals(operation)) {
                Integer id = input.intId("id", false);
                String name = input.text("name", "名称", 150, true);
                String kind = input.choice("kind", "種類", "QUALIFICATION", "PERMIT", "DOCUMENT");
                boolean active = Boolean.parseBoolean(input.choice("active", "有効状態", "true", "false"));
                String guidance = input.text("guidance", "説明", 8000, false);
                String url = input.webUrl("url", "案内・様式URL");
                int savedId = catalogDao.saveQualification(admin.getUserId(), id, name, kind, guidance, url, active);
                ManagementAuth.redirectSaved(req, res, ManagementPaths.ADMIN_QUALIFICATIONS + "?id=" + savedId);
                return;
            }

            int industryId = input.intId("industry", true);
            int qualificationId = input.intId("qualificationId", true);
            if ("removeRequirement".equals(operation)) {
                String reason = input.text("reason", "解除理由", 2000, true);
                catalogDao.removeRequirement(admin.getUserId(), industryId, qualificationId, reason);
            } else {
                boolean required = Boolean.parseBoolean(input.choice("required", "必須区分", "true", "false"));
                String condition = input.text("condition", "適用条件", 300, false);
                catalogDao.saveRequirement(admin.getUserId(), industryId, qualificationId, required, condition);
            }
            ManagementAuth.redirectSaved(req, res,
                    ManagementPaths.ADMIN_QUALIFICATIONS + "?tab=requirements&industry=" + industryId);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }
}
