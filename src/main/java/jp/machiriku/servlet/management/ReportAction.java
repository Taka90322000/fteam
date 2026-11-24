package jp.machiriku.servlet.management;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminReportItem;
import jp.machiriku.bean.management.ManagementLabels;
import jp.machiriku.dao.management.MemberSupportDao;
import jp.machiriku.dao.management.PublicDirectoryDao;
import jp.machiriku.tool.Action;

/** 本登録済み会員が物件・オーナー・事業主を通報し、受付結果を確認するAction。 */
public final class ReportAction extends Action {
    /** 通報登録と受付確認を行うDAO。 */
    private final MemberSupportDao supportDao;
    /** 通報対象の確認を行うDAO。 */
    private final PublicDirectoryDao directoryDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public ReportAction() {
        this(new MemberSupportDao(), new PublicDirectoryDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param supportDao 会員サポートDAO。
     * @param directoryDao 公開ディレクトリDAO。
     */
    ReportAction(MemberSupportDao supportDao, PublicDirectoryDao directoryDao) {
        this.supportDao = supportDao;
        this.directoryDao = directoryDao;
    }

    /**
     * GETでは通報フォームまたは受付完了画面を表示し、POSTでは通報を保存する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @throws Exception 画面表示または保存処理を完了できない場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 1. POSTの場合は通報内容を検証して登録する。
        if ("POST".equals(req.getMethod())) {
            handlePost(req, res);
            return;
        }

        // 2. GET/HEADでは本登録済み（ACTIVE）の会員であることを確認する。
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }
        User member = ManagementAuth.requireMember(req, res, true);
        if (member == null) {
            return;
        }

        try {
            FormReader input = new FormReader(req);
            Long receiptId = input.longId("receipt", false);

            // 3. 受付番号が指定されている場合は通報者本人の受付結果画面を表示する。
            if (receiptId != null) {
                AdminReportItem receipt = supportDao.findReportReceipt(receiptId, member.getUserId());
                if (receipt == null) {
                    res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
                    return;
                }
                ManagementAuth.preparePage(req, "通報を受け付けました", false, true);
                req.setAttribute("receipt", receipt);
                req.getRequestDispatcher("/WEB-INF/views/management/support/reports/receipt.jsp").forward(req, res);
                return;
            }

            // 4. 通報対象を確認し、対象区分に応じた理由選択肢付きで通報フォームを表示する。
            String kind = input.choice("kind", "通報対象", "PROPERTY", "OWNER", "BUSINESS");
            int targetId = input.intId("id", true);
            Integer propertyId = input.intId("propertyId", false);
            AdminReportItem.Target target = directoryDao.findReportTarget(member.getUserId(), kind, targetId, propertyId);
            String title = switch (kind) {
                case "OWNER" -> "物件オーナーを通報";
                case "BUSINESS" -> "事業主を通報";
                default -> "物件を通報";
            };
            Map<String, String> reasons = ManagementLabels.reportReasonsFor(kind);

            ManagementAuth.preparePage(req, title, false, true);
            req.setAttribute("target", target);
            req.setAttribute("reasons", reasons);
            req.getRequestDispatcher("/WEB-INF/views/management/support/reports/form.jsp").forward(req, res);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * 通報フォームの送信内容を検証して保存し、受付完了画面へリダイレクトする。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @throws Exception 保存処理を完了できない場合。
     */
    private void handlePost(HttpServletRequest req, HttpServletResponse res) throws Exception {
        User member = ManagementAuth.requireSignedInForPost(req, res);
        if (member == null) {
            return;
        }
        if (!ManagementAuth.verifyCsrf(req, res, false)) {
            return;
        }
        try {
            FormReader input = new FormReader(req);
            input.choice("operation", "操作", "send");
            String kind = input.choice("kind", "通報対象", "PROPERTY", "OWNER", "BUSINESS");
            int targetId = input.intId("id", true);
            Integer propertyId = input.intId("propertyId", false);
            Map<String, String> allowedReasons = ManagementLabels.reportReasonsFor(kind);
            String reasonCode = input.choice("reasonCode", "通報理由",
                    allowedReasons.keySet().toArray(String[]::new));
            String body = input.text("body", "詳細", 8000, true);

            long reportId = supportDao.submitReport(member.getUserId(), kind, targetId, propertyId, reasonCode, body);
            ManagementAuth.redirectSaved(req, res, ManagementPaths.REPORT + "?receipt=" + reportId);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }
}
