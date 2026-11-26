package jp.machiriku.servlet.management;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminCouponItem;
import jp.machiriku.bean.management.ManagementLabels;
import jp.machiriku.bean.management.ManagementProblem;
import jp.machiriku.bean.management.PageResult;
import jp.machiriku.dao.management.AdminAuditDao;
import jp.machiriku.dao.management.AdminCouponDao;
import jp.machiriku.tool.Action;

/** 管理者がクーポンの配布状況・発行履歴を確認し、下書き編集・配布停止/再開・個別無効化を行うAction。 */
public final class AdminCouponsAction extends Action {
    /** 管理者権限チェックに使うDAO。 */
    private final AdminAuditDao auditDao;
    /** クーポン管理DAO。 */
    private final AdminCouponDao couponDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public AdminCouponsAction() {
        this(new AdminAuditDao(), new AdminCouponDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param auditDao 管理者権限チェックDAO。
     * @param couponDao クーポン管理DAO。
     */
    AdminCouponsAction(AdminAuditDao auditDao, AdminCouponDao couponDao) {
        this.auditDao = auditDao;
        this.couponDao = couponDao;
    }

    /**
     * GETではクーポン一覧または詳細（配布設定・発行履歴）を表示し、POSTでは編集・停止/再開・無効化を実行する。
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

        // 2. POSTの場合は編集・配布停止/再開・個別無効化を実行する。
        if ("POST".equals(req.getMethod())) {
            handlePost(req, res, admin);
            return;
        }

        // 3. GET/HEADではIDの有無に応じて一覧または詳細を表示する。
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }
        try {
            FormReader input = new FormReader(req);
            Integer couponId = input.intId("id", false);
            if (couponId == null) {
                showList(req, res, input);
            } else {
                showDetail(req, res, input, couponId);
            }
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * クーポン検索・一覧画面を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showList(HttpServletRequest req, HttpServletResponse res, FormReader input) throws Exception {
        String query = input.search();
        String status = input.value("status");
        List<AdminCouponItem> rows = couponDao.findCoupons(status, query, FormReader.PAGE_SIZE + 1, input.offset());
        PageResult<AdminCouponItem> pageResult = input.toPageResult(rows);

        ManagementAuth.preparePage(req, "クーポン管理", true, true);
        req.setAttribute("query", query);
        req.setAttribute("status", status);
        req.setAttribute("states", ManagementLabels.COUPON_STATES);
        req.setAttribute("couponPage", pageResult);
        req.getRequestDispatcher("/WEB-INF/views/management/admin/coupons/list.jsp").forward(req, res);
    }

    /**
     * クーポン詳細画面（配布設定タブまたは発行・利用履歴タブ）を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @param couponId クーポンID。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showDetail(HttpServletRequest req, HttpServletResponse res, FormReader input, int couponId)
            throws Exception {
        AdminCouponItem coupon = couponDao.findCouponById(couponId);
        if (coupon == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
            return;
        }
        boolean showRecipients = "recipients".equals(input.value("tab"));

        ManagementAuth.preparePage(req, coupon.getCouponTitle(), true, true);
        req.setAttribute("coupon", coupon);
        req.setAttribute("showRecipients", showRecipients);
        req.setAttribute("benefits", ManagementLabels.COUPON_BENEFITS);

        if (showRecipients) {
            List<AdminCouponItem.Recipient> entries = couponDao.findRecipients(
                    couponId, FormReader.PAGE_SIZE + 1, input.offset());
            req.setAttribute("recipientPage", input.toPageResult(entries));

            Integer recipientId = input.intId("recipientId", false);
            if (recipientId != null) {
                AdminCouponItem.Recipient selected = couponDao.findRecipient(couponId, recipientId);
                if (selected == null) {
                    res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された発行記録が見つかりません。");
                    return;
                }
                req.setAttribute("selectedRecipient", selected);
            }
        }

        req.getRequestDispatcher("/WEB-INF/views/management/admin/coupons/detail.jsp").forward(req, res);
    }

    /**
     * 下書き編集・配布停止/再開・個別発行無効化のフォーム送信を処理する。
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
            int couponId = input.intId("id", true);
            String operation = input.choice("operation", "クーポンの操作", "edit", "stop", "resume", "revoke");
            String basePath = ManagementPaths.ADMIN_COUPONS + "?id=" + couponId;

            if ("edit".equals(operation)) {
                String benefitKind = input.choice("benefit", "特典の種類", "PERCENT", "AMOUNT", "SERVICE");
                // 選んだ特典種別に対応する項目だけを読み取り、他の特典項目はnullでクリアする。
                BigDecimal percent = "PERCENT".equals(benefitKind)
                        ? input.decimal("percent", "割引率", 0.01, 100, 2, true) : null;
                Integer amount = "AMOUNT".equals(benefitKind)
                        ? input.number("amount", "割引額", 1, Integer.MAX_VALUE) : null;
                String service = "SERVICE".equals(benefitKind)
                        ? input.text("service", "サービス内容", 200, true) : null;
                OffsetDateTime startsAt = input.dateTime("starts", "配布開始", true);
                OffsetDateTime endsAt = input.dateTime("ends", "配布終了", true);
                OffsetDateTime validUntil = input.dateTime("validUntil", "利用期限", true);
                if (!startsAt.isBefore(endsAt) || endsAt.isAfter(validUntil)) {
                    throw new ManagementProblem(400, "配布終了は開始より後、利用期限以前に設定してください。");
                }
                int limit = input.number("limit", "発行上限", 1, Integer.MAX_VALUE);
                String title = input.text("title", "タイトル", 200, true);
                String terms = input.text("terms", "利用条件", 4000, false);

                couponDao.updateDraft(admin.getUserId(), couponId, title, benefitKind,
                        percent, amount, service, startsAt, endsAt, validUntil, limit, terms);
                ManagementAuth.redirectSaved(req, res, basePath);
                return;
            }

            String reason = input.text("reason", "操作理由", 2000, true);
            if ("revoke".equals(operation)) {
                int recipientId = input.intId("recipientId", true);
                couponDao.revokeRecipient(admin.getUserId(), couponId, recipientId, reason);
                ManagementAuth.redirectSaved(req, res, basePath + "&tab=recipients&recipientId=" + recipientId);
                return;
            }

            couponDao.changeDistributionStatus(admin.getUserId(), couponId, "stop".equals(operation), reason);
            ManagementAuth.redirectSaved(req, res, basePath);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }
}
