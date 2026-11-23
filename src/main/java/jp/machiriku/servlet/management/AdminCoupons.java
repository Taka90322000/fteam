package jp.machiriku.servlet.management;

import static jp.machiriku.servlet.management.ManagementDisplay.options;

import java.sql.SQLException;
import java.util.Map;

import jp.machiriku.bean.management.AdminCouponView;
import jp.machiriku.dao.management.AdminCouponDao;
import jp.machiriku.dao.management.ManagementDao;

/** クーポンの配布状況と発行履歴を確認し、配布停止や個別無効化を管理する。 */
final class AdminCoupons {
    /** 配布状態の表示名。 */
    private static final Map<String, String> STATES = options("DRAFT", "下書き", "ACTIVE", "配布有効", "STOPPED", "配布停止中");
    /** 特典の種類と入力欄での表示名。 */
    private static final Map<String, String> BENEFITS = options("PERCENT", "割合割引", "AMOUNT", "定額割引", "SERVICE", "サービス");
    /** 配布対象の表示名。 */
    private static final Map<String, String> TARGETS = options("ALL_VOTERS", "物件への投票者", "INDUSTRY_VOTERS", "対象業種への投票者", "SURVEY", "アンケート回答者");

    /** 画面の処理を静的に利用する。 */
    private AdminCoupons() { }

    /**
     * クーポンの一覧または指定したクーポンの配布・発行情報を取得する。
     * @param db クーポンを照会するDAO接続。
     * @param input 検索語、表示対象とタブの指定。
     * @return クーポン管理のJSPと表示データ。
     * @throws SQLException クーポンを取得できなかった場合。
     */
    static ManagementView page(ManagementDao db, ManagementRequest input) throws SQLException {
        // 一覧と、IDで指定されたクーポンの詳細を区別する。
        Integer id = input.id("id", false);
        if (id == null) {
            return list(db, input);
        }
        var coupon = AdminCouponDao.coupon(db, id);
        String state = coupon.get("配布状態").toString();
        String benefit = coupon.get("特典").toString();
        boolean editable = "DRAFT".equals(state) && ManagementDisplay.key(coupon, "発行数") == 0;
        coupon.put("配布状態", STATES.get(state));
        coupon.put("特典", BENEFITS.get(benefit));
        coupon.put("配布対象", TARGETS.get(coupon.get("配布対象")));

        // 発行履歴を選んだ場合にだけ、発行記録を読み込む。
        var history = "recipients".equals(input.value("tab")) ? recipients(db, input, id) : null;
        return new ManagementView("admin/coupons/detail.jsp", coupon.get("名前").toString(),
                new AdminCouponView.Detail(id, state, benefit, BENEFITS, coupon, editable, history));
    }

    /**
     * 入力された条件でクーポンを検索する。
     * @param db クーポンを照会するDAO接続。
     * @param input キーワードと配布状態。
     * @return 一覧のJSPと検索結果。
     * @throws SQLException クーポンを取得できなかった場合。
     */
    private static ManagementView list(ManagementDao db, ManagementRequest input) throws SQLException {
        // 検索結果に対応するフィルターの値を画面に戻す。
        String query = input.search();
        String state = input.value("status");
        var rows = AdminCouponDao.coupons(db, state, query, ManagementRequest.PAGE_SIZE + 1, input.offset());
        rows.forEach(row -> row.put("配布状態", STATES.get(row.get("配布状態"))));
        return new ManagementView("admin/coupons/list.jsp", "クーポン管理",
                new AdminCouponView.Listing(query, state, STATES, input.pageRows(rows)));
    }

    /**
     * 発行履歴と、選んだ発行記録の利用状態を取得する。
     * @param db 発行記録を照会するDAO接続。
     * @param input 選んだ受取会員IDとページ。
     * @param couponId 対象のクーポンID。
     * @return 発行履歴と選択した記録のデータ。
     * @throws SQLException 発行記録を取得できなかった場合。
     */
    private static AdminCouponView.Recipients recipients(ManagementDao db, ManagementRequest input, int couponId)
            throws SQLException {
        // 受取会員を選んだ場合にだけ、個別に無効化できる状態か照会する。
        var entries = input.pageRows(AdminCouponDao.recipients(db, couponId, ManagementRequest.PAGE_SIZE + 1, input.offset()));
        Integer selectedId = input.id("recipientId", false);
        var selected = selectedId == null ? Map.<String, Object>of() : AdminCouponDao.recipient(db, couponId, selectedId);
        return new AdminCouponView.Recipients(entries, selectedId, selected, selectedId != null && unredeemed(selected));
    }

    /**
     * 選択した券が現在無効化できるかを判断する。
     * @param row 発行・利用・期限の取得結果。
     * @return 有効期間内の未使用券であればtrue。
     */
    private static boolean unredeemed(Map<String, Object> row) {
        return row.get("利用日時") == null && row.get("無効化日時") == null && Boolean.TRUE.equals(row.get("有効期間内"));
    }

    /**
     * 配布設定または一件の発行を更新し、管理者の操作として記録する。
     * @param db 書き込みトランザクション中の接続。
     * @param input 更新内容。
     * @param actorId 権限を確認した会員ID。
     * @return 保存後に表示する画面。
     * @throws SQLException クーポンまたは操作履歴を保存できなかった場合。
     */
    static String change(ManagementDao db, ManagementRequest input, int actorId) throws SQLException {
        int id = input.id("id", true);
        String operation = input.choice("operation", "クーポンの操作", "edit", "stop", "resume", "revoke");
        // 同じ券の配布設定が同時に更新される場合は、ロック後の状態で判断する。
        var coupon = AdminCouponDao.lockCoupon(db, id);
        String state = coupon.get("coupon_status").toString();
        String path = ManagementPaths.ADMIN_COUPONS + "?id=" + id;
        if ("edit".equals(operation)) {
            if (!"DRAFT".equals(state) || !AdminCouponDao.issuedCouponKeys(db, id).isEmpty()) {
                throw new ManagementProblem(409, "内容を編集できるのは、発行前の下書きです。");
            }
            saveDraft(db, input, id);
            db.audit(actorId, "EDIT_COUPON", "coupon", Map.of("coupon_id", id), null, Map.of());
            return path;
        }
        String reason = input.text("reason", "操作理由", 2000, true);
        if ("revoke".equals(operation)) {
            int recipientId = input.id("recipientId", true);
            // 条件をUPDATEにも含め、店舗で利用処理された直後の券を無効化しない。
            int changed = AdminCouponDao.revokeRecipient(db, id, recipientId);
            if (changed == 0) { throw new ManagementProblem(409, "この発行分は無効化できません。利用状況を確認してください。"); }
            db.audit(actorId, "REVOKE_COUPON_RECIPIENT", "coupon_recipient", Map.of("coupon_id", id, "resident_user_id", recipientId), reason,
                    Map.of("revoked", Map.of("before", false, "after", true)));
            return path + "&tab=recipients&recipientId=" + recipientId;
        }
        boolean stop = "stop".equals(operation);
        if (stop ? !"ACTIVE".equals(state) : !"STOPPED".equals(state) || !Boolean.TRUE.equals(coupon.get("in_period"))) {
            throw new ManagementProblem(409, "配布状態または配布期限を確認してください。");
        }
        String next = stop ? "STOPPED" : "ACTIVE";
        AdminCouponDao.updateState(db, next, id);
        db.audit(actorId, stop ? "STOP_COUPON" : "RESUME_COUPON", "coupon", Map.of("coupon_id", id), reason,
                Map.of("coupon_status", Map.of("before", state, "after", next)));
        return path;
    }

    /**
     * 下書きの特典と配布期間を検証して保存する。
     * @param db クーポンを更新する接続。
     * @param input 下書きの編集内容。
     * @param id 保存先のクーポンID。
     * @throws SQLException 下書きを保存できなかった場合。
     */
    private static void saveDraft(ManagementDao db, ManagementRequest input, int id) throws SQLException {
        String kind = input.choice("benefit", "特典の種類", "PERCENT", "AMOUNT", "SERVICE");
        // 選択した特典に対応する列だけに値を入れ、種類を変えたときには旧特典の値を消す。
        var percent = "PERCENT".equals(kind) ? input.decimal("percent", "割引率", 0.01, 100, 2, true) : null;
        Integer amount = "AMOUNT".equals(kind) ? input.number("amount", "割引額", 1, Integer.MAX_VALUE) : null;
        String service = "SERVICE".equals(kind) ? input.text("service", "サービス内容", 200, true) : null;
        var starts = input.dateTime("starts", "配布開始", true);
        var ends = input.dateTime("ends", "配布終了", true);
        var validUntil = input.dateTime("validUntil", "利用期限", true);
        if (!starts.isBefore(ends) || ends.isAfter(validUntil)) {
            throw new ManagementProblem(400, "配布終了は開始より後、利用期限以前に設定してください。");
        }
        int limit = input.number("limit", "発行上限", 1, Integer.MAX_VALUE);
        AdminCouponDao.updateDraft(db, input.text("title", "タイトル", 200, true), kind, percent, amount, service, starts, ends, validUntil, limit, input.text("terms", "利用条件", 4000, false), id);
    }
}
