package jp.machiriku.servlet.management;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.machiriku.bean.User;
import jp.machiriku.bean.management.AdminPropertyItem;
import jp.machiriku.bean.management.InquiryItem;
import jp.machiriku.bean.management.ManagementLabels;
import jp.machiriku.bean.management.PageResult;
import jp.machiriku.dao.management.MemberSupportDao;
import jp.machiriku.dao.management.PublicDirectoryDao;
import jp.machiriku.tool.Action;

/** 一般会員が自身の問い合わせ作成・一覧閲覧・返信を行うAction。 */
public final class InquiriesAction extends Action {
    /** 会員の問い合わせを操作するDAO。 */
    private final MemberSupportDao supportDao;
    /** 関連物件の公開確認を行うDAO。 */
    private final PublicDirectoryDao directoryDao;

    /** FrontControllerが使用する引数なしコンストラクター。 */
    public InquiriesAction() {
        this(new MemberSupportDao(), new PublicDirectoryDao());
    }

    /**
     * テスト用にDAOを差し替えるコンストラクター。
     * @param supportDao 会員サポートDAO。
     * @param directoryDao 公開ディレクトリDAO。
     */
    InquiriesAction(MemberSupportDao supportDao, PublicDirectoryDao directoryDao) {
        this.supportDao = supportDao;
        this.directoryDao = directoryDao;
    }

    /**
     * GETでは問い合わせ一覧または詳細を表示し、POSTでは新規受付または返信を保存する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @throws Exception 画面表示または保存処理を完了できない場合。
     */
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        // 1. POSTの場合は新規問い合わせまたは返信を登録する。
        if ("POST".equals(req.getMethod())) {
            handlePost(req, res);
            return;
        }

        // 2. GET/HEADでは本登録前を含むログイン済み会員を確認して画面を表示する。
        if (!ManagementAuth.allowReadMethod(req, res)) {
            return;
        }
        User member = ManagementAuth.requireMember(req, res, false);
        if (member == null) {
            return;
        }

        try {
            FormReader input = new FormReader(req);
            Long inquiryId = input.longId("id", false);
            if (inquiryId == null) {
                showList(req, res, input, member);
            } else {
                showDetail(req, res, input, member, inquiryId);
            }
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * 会員からの新規問い合わせ送信または既存スレッドへの返信を処理する。
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
            String operation = input.choice("operation", "操作", "open", "reply");
            long inquiryId;
            if ("open".equals(operation)) {
                Integer propertyId = input.intId("propertyId", false);
                String category = input.choice("category", "カテゴリ",
                        ManagementLabels.INQUIRY_CATEGORIES.keySet().toArray(String[]::new));
                String subject = input.text("subject", "件名", 200, true);
                String body = input.text("body", "本文", 8000, true);
                inquiryId = supportDao.createMemberInquiry(member.getUserId(), propertyId, category, subject, body);
            } else {
                inquiryId = input.longId("id", true);
                String body = input.text("body", "返信内容", 8000, true);
                supportDao.replyMemberInquiry(inquiryId, member.getUserId(), body);
            }
            ManagementAuth.redirectSaved(req, res, ManagementPaths.INQUIRIES + "?id=" + inquiryId);
        } catch (Exception error) {
            ManagementAuth.handleError(req, res, error);
        }
    }

    /**
     * 会員本人の問い合わせ一覧と新規問い合わせフォームを表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @param member ログイン中の会員。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showList(HttpServletRequest req, HttpServletResponse res, FormReader input, User member)
            throws Exception {
        Integer propertyId = input.intId("propertyId", false);
        AdminPropertyItem relatedProperty = null;
        if (propertyId != null) {
            relatedProperty = directoryDao.findVisiblePropertyById(propertyId);
            if (relatedProperty == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
                return;
            }
        }
        List<InquiryItem> rows = supportDao.findMemberInquiries(
                member.getUserId(), FormReader.PAGE_SIZE + 1, input.offset());
        PageResult<InquiryItem> pageResult = input.toPageResult(rows);

        ManagementAuth.preparePage(req, "お問い合わせ", false, true);
        req.setAttribute("propertyId", propertyId);
        req.setAttribute("relatedProperty", relatedProperty);
        req.setAttribute("categories", ManagementLabels.INQUIRY_CATEGORIES);
        req.setAttribute("inquiryPage", pageResult);
        req.getRequestDispatcher("/WEB-INF/views/management/support/inquiries/list.jsp").forward(req, res);
    }

    /**
     * 会員本人の問い合わせ詳細と公開メッセージ一覧を表示する。
     * @param req HTTPリクエスト。
     * @param res HTTPレスポンス。
     * @param input リクエスト読み取り器。
     * @param member ログイン中の会員。
     * @param inquiryId 受付番号。
     * @throws Exception 照会または画面描画に失敗した場合。
     */
    private void showDetail(HttpServletRequest req, HttpServletResponse res, FormReader input,
            User member, long inquiryId) throws Exception {
        InquiryItem inquiry = supportDao.findMemberInquiryById(inquiryId, member.getUserId());
        if (inquiry == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "指定された情報が見つかりません。");
            return;
        }
        List<InquiryItem.Message> messages = supportDao.findMemberMessages(
                inquiryId, member.getUserId(), FormReader.PAGE_SIZE + 1, input.offset());
        PageResult<InquiryItem.Message> messagePage = input.toPageResult(messages);

        ManagementAuth.preparePage(req, "問い合わせ #" + inquiryId, false, true);
        req.setAttribute("inquiry", inquiry);
        req.setAttribute("messagePage", messagePage);
        req.getRequestDispatcher("/WEB-INF/views/management/support/inquiries/detail.jsp").forward(req, res);
    }
}
