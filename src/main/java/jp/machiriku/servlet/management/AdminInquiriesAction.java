package jp.machiriku.servlet.management;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.InquiryItem;
import jp.machiriku.bean.management.ManagementLabels;
import jp.machiriku.bean.management.PageResult;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.dao.management.AdminInquiryDao;
import jp.machiriku.tool.Action;

/** 管理者が問い合わせの検索・詳細閲覧・担当設定・返信・内部メモ追加を行うAction。 */
public final class AdminInquiriesAction extends Action {
    /** 管理者権限チェックと管理者一覧取得に使うDAO。 */
    private final AdminAuditDao auditDao;
    /** 管理者用問い合わせDAO。 */
    private final AdminInquiryDao inquiryDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminInquiriesAction() {
        this(new AdminAuditDao(), new AdminInquiryDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param auditDao 管理者権限チェックDAO。
     * @param inquiryDao 管理者用問い合わせDAO。
     */
    AdminInquiriesAction(AdminAuditDao auditDao, AdminInquiryDao inquiryDao) {
        this.auditDao = auditDao;
        this.inquiryDao = inquiryDao;
    }

    /**
     * GETでは問い合わせ一覧または詳細を表示し、POSTでは担当設定・返信・内部メモを保存する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @throws Exception 画面表示または保存処理を完了できない場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 1. 管理者権限を確認する。
        User admin = ManagementAuth.requireAdmin(req, res, auditDao);
        if (admin == null) {
            return;
        }

        // 2. POSTの場合は担当設定・返信・内部メモのいずれかを実行する。
        if ("POST".equals(req.getMethod())) {
            handlePost(req, res, admin);
            return;
        }

        // 3. GET/HEADでは受付番号の有無に応じて一覧または詳細を表示する。
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }
        try {
            FormReader input = new FormReader(req);
            Long inquiryId = input.longId("id", false);
            if (inquiryId == null) {
                showList(req, res, input);
            } else {
                showDetail(req, res, input, inquiryId);
            }
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * 問い合わせの担当設定・利用者への返信・管理者メモ追加を処理する。
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
            String operation = input.choice("operation", "操作", "assign", "reply", "note");
            long inquiryId = input.longId("id", true);

            if ("note".equals(operation)) {
                String body = input.text("body", "メモ", 8000, true);
                inquiryDao.addInternalNote(inquiryId, admin.getUserId(), body);
            } else if ("assign".equals(operation)) {
                Integer assigneeId = input.intId("assignee", false);
                String status = input.choice("status", "対応状態",
                        ManagementLabels.INQUIRY_STATES.keySet().toArray(String[]::new));
                inquiryDao.assignInquiry(inquiryId, admin.getUserId(), assigneeId, status);
            } else {
                String status = input.choice("status", "返信後の状態", "IN_PROGRESS", "WAITING_USER", "CLOSED");
                String body = input.text("body", "返信内容", 8000, true);
                inquiryDao.replyInquiry(inquiryId, admin.getUserId(), status, body);
            }
            ManagementAuth.redirectSaved(req, res, ManagementPaths.ADMIN_INQUIRIES + "?id=" + inquiryId);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * 管理者用の問い合わせ検索・一覧画面を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showList(HttpServletRequest req, HttpServletResponse res, FormReader input) throws Exception {
        String query = input.search();
        String category = input.value("category");
        String status = input.value("status");
        Integer assigneeId = input.intId("assignee", false);

        List<InquiryItem> rows = inquiryDao.findInquiries(
                category, status, assigneeId, query, FormReader.PAGE_SIZE + 1, input.offset());
        PageResult<InquiryItem> pageResult = input.toPageResult(rows);
        Map<String, String> admins = auditDao.findActiveAdminChoices();

        ManagementAuth.preparePage(req, "問い合わせ管理", true, true);
        req.setAttribute("query", query);
        req.setAttribute("category", category);
        req.setAttribute("status", status);
        req.setAttribute("assigneeId", assigneeId);
        req.setAttribute("categories", ManagementLabels.INQUIRY_CATEGORIES);
        req.setAttribute("states", ManagementLabels.INQUIRY_STATES);
        req.setAttribute("admins", admins);
        req.setAttribute("inquiryPage", pageResult);
        req.getRequestDispatcher("/WEB-INF/views/management/admin/inquiries/list.jsp").forward(req, res);
    }

    /**
     * 管理者用の問い合わせ詳細・やり取り・対応フォーム画面を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @param inquiryId 受付番号。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showDetail(HttpServletRequest req, HttpServletResponse res, FormReader input, long inquiryId)
            throws Exception {
        InquiryItem inquiry = inquiryDao.findInquiryById(inquiryId);
        if (inquiry == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
            return;
        }
        List<InquiryItem.Message> messages = inquiryDao.findMessages(
                inquiryId, FormReader.PAGE_SIZE + 1, input.offset());
        PageResult<InquiryItem.Message> messagePage = input.toPageResult(messages);
        Map<String, String> admins = inquiry.isCanReply() ? auditDao.findActiveAdminChoices() : Map.of();

        ManagementAuth.preparePage(req, "問い合わせ #" + inquiryId, true, true);
        req.setAttribute("inquiry", inquiry);
        req.setAttribute("messagePage", messagePage);
        req.setAttribute("admins", admins);
        req.setAttribute("states", ManagementLabels.INQUIRY_STATES);
        req.setAttribute("replyStates", ManagementLabels.ADMIN_REPLY_STATES);
        req.getRequestDispatcher("/WEB-INF/views/management/admin/inquiries/detail.jsp").forward(req, res);
    }
}
