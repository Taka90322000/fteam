package jp.machiriku.servlet.management;

import static jp.machiriku.servlet.management.ManagementDisplay.options;

import java.sql.SQLException;
import java.util.Map;

import jp.machiriku.bean.management.HelpDeskView;
import jp.machiriku.bean.User;

import jp.machiriku.dao.management.HelpDeskDao;
import jp.machiriku.dao.management.ManagementDao;

/** 利用者からの通報・問い合わせと、管理者による確認・対応を扱う。 */
final class HelpDesk {
    /** 一覧やフォームで使う問い合わせの分類。 */
    private static final Map<String, String> CATEGORIES = options("ACCOUNT", "アカウント・登録", "PROPERTY", "物件・出店",
            "SURVEY_COUPON", "アンケート・クーポン", "OTHER", "その他");
    /** 問い合わせ一覧で使う対応状態。 */
    private static final Map<String, String> INQUIRY_STATES = options("WAITING_ADMIN", "管理者の対応待ち", "IN_PROGRESS", "対応中",
            "WAITING_USER", "利用者の返信待ち", "CLOSED", "完了");
    /** 通報一覧で使う対応状態。 */
    private static final Map<String, String> REPORT_STATES = options("OPEN", "未対応", "IN_REVIEW", "確認中", "RESOLVED", "完了");
    /** 通報理由の分類と表示順。対象に応じて送信時に選択肢を絞る。 */
    private static final Map<String, String> REASONS = options("INACCURATE", "情報の誤り・虚偽", "DUPLICATE", "重複掲載",
            "IMPERSONATION", "なりすまし", "HARASSMENT", "不適切なやり取り", "INAPPROPRIATE", "不適切な掲載内容", "OTHER", "その他");
    /** 共通処理をインスタンス化せず利用する。 */
    private HelpDesk() { }

    /**
     * 通報の一覧または選択された受付の詳細と対応可否を取得する。
     * @param db 通報を照会するDAO接続。
     * @param input 受付番号や絞り込み条件。
     * @param actorId 操作する管理者の会員ID。
     * @return 一覧または詳細のJSPと表示データ。
     * @throws SQLException 通報を取得できなかった場合。
     */
    static ManagementView reports(ManagementDao db, ManagementRequest input, int actorId) throws SQLException {
        // 受付番号がある場合にだけ詳細を取得する。
        Long id = input.ticket("id", false);
        if (id == null) {
            return reportList(db, input);
        }
        var report = HelpDeskDao.report(db, id);
        String state = report.get("状態").toString();
        var resolutions = resolutionLabels(report.get("対象物件ID") != null);
        report.put("状態", REPORT_STATES.get(state));
        report.computeIfPresent("理由", (key, value) -> REASONS.getOrDefault(value.toString(), value.toString()));
        report.computeIfPresent("対応結果", (key, value) -> resolutions.getOrDefault(value.toString(), value.toString()));

        // 対応が終わっていない場合に担当者の候補と初期値を渡す。
        boolean editable = !"RESOLVED".equals(state);
        Integer assignee = report.get("担当者ID") == null ? actorId : ((Number) report.get("担当者ID")).intValue();
        var admins = editable ? ManagementDisplay.choices(HelpDeskDao.activeAdmins(db)) : Map.<String, String>of();
        return new ManagementView("admin/reports/detail.jsp", "通報 #" + id,
                new HelpDeskView.ReportDetail(id, report, editable, assignee, admins, resolutions));
    }

    /**
     * 対象、理由、状態、期間などで通報を検索する。
     * @param db 通報の一覧を参照するDAO接続。
     * @param input 絞り込み条件とページ。
     * @return 通報一覧のJSPと検索結果。
     * @throws SQLException 通報を取得できなかった場合。
     */
    private static ManagementView reportList(ManagementDao db, ManagementRequest input) throws SQLException {
        // 一覧の区分表示と絞り込みでは同じ対象名称を使う。
        var kinds = options("OWNER", "物件オーナー", "BUSINESS", "事業主", "PROPERTY", "物件");
        String query = input.search();
        String kind = input.value("kind");
        String state = input.value("status");
        String reason = input.value("reasonCode");
        var from = input.date("from", "開始日");
        var to = input.date("to", "終了日");
        var rows = HelpDeskDao.reports(db, kind, state, reason, from, to, query, ManagementRequest.PAGE_SIZE + 1, input.offset());
        for (var row : rows) {
            row.computeIfPresent("対象区分", (key, value) -> kinds.get(value.toString()));
            row.computeIfPresent("状態", (key, value) -> REPORT_STATES.get(value.toString()));
            row.computeIfPresent("理由", (key, value) -> REASONS.getOrDefault(value.toString(), value.toString()));
        }
        return new ManagementView("admin/reports/list.jsp", "通報管理",
                new HelpDeskView.ReportList(query, kind, state, reason, from, to, kinds, REPORT_STATES, REASONS, input.pageRows(rows)));
    }

    /**
     * 通報の担当設定または対応完了を記録する。
     * @param db トランザクション内の接続。
     * @param input 受付番号・担当・結果の送信内容。
     * @param actorId 実行者の会員ID。
     * @return 保存後に開く画面のURL。
     * @throws SQLException 更新に失敗した場合。
     */
    static String changeReport(ManagementDao db, ManagementRequest input, int actorId) throws SQLException {
        long id = input.ticket("id", true);
        // 通報行をロックし、別の管理者が完了した通報を上書きしない。
        var old = HelpDeskDao.lockReport(db, id);
        if ("RESOLVED".equals(old.get("report_status"))) { throw new ManagementProblem(409, "この通報は既に対応済みです。"); }
        String operation = input.choice("operation", "操作", "review", "resolve");
        if ("review".equals(operation)) {
            int assignee = input.id("assignee", true);
            requireAssignee(db, assignee);
            HelpDeskDao.assignReport(db, assignee, id);
            db.audit(actorId, "REVIEW", "REPORT", Map.of("report_id", id), null,
                    Map.of("before", old.get("report_status"), "after", "IN_REVIEW", "assignee", assignee));
        } else {
            Integer propertyId = (Integer) old.get("target_property_id");
            String resolution = input.choice("resolution", "対応結果", resolutionLabels(propertyId != null).keySet().toArray(String[]::new));
            String reason = input.text("reason", "判断理由", 2000, true);
            // 対象に対する措置も受付の完了と同時に反映し、失敗時に片方だけを保存しない。
            if ("HIDE_PROPERTY".equals(resolution)) {
                AdminListings.hideProperty(db, propertyId, actorId, true, reason);
            } else if ("SUSPEND_USER".equals(resolution)) {
                AdminUsers.accountStatus(db, (Integer) old.get("target_user_id"), actorId, true, reason);
            }
            HelpDeskDao.resolveReport(db, resolution, reason, actorId, id);
            db.audit(actorId, "RESOLVE", "REPORT", Map.of("report_id", id), reason,
                    Map.of("before", old.get("report_status"), "after", "RESOLVED", "resolution", resolution));
        }
        return ManagementPaths.ADMIN_REPORTS + "?id=" + id;
    }

    /**
     * 管理者または会員本人が閲覧できる問い合わせを取得する。
     * @param db 受付と投稿を照会するDAO接続。
     * @param input 受付番号または一覧の検索条件。
     * @param actorId 閲覧者の会員ID。
     * @param admin 管理者用画面から照会する場合はtrue。
     * @return 閲覧者に対応する問い合わせのJSPとデータ。
     * @throws SQLException 閲覧できる問い合わせが見つからないか、取得できなかった場合。
     */
    static ManagementView inquiries(ManagementDao db, ManagementRequest input, int actorId, boolean admin) throws SQLException {
        // 一覧は管理者用の絞り込みと会員用の新規受付で取得する情報が異なる。
        Long id = input.ticket("id", false);
        if (id == null) {
            return admin ? adminInquiryList(db, input, actorId) : memberInquiryList(db, input, actorId);
        }

        // DAOに閲覧者の権限を渡し、会員向けには本人が閲覧できる投稿だけを取得する。
        var inquiry = HelpDeskDao.inquiry(db, id, admin, actorId);
        String state = inquiry.get("状態").toString();
        showInquiryLabels(inquiry);
        var posts = input.pageRows(HelpDeskDao.messages(db, id, admin, actorId, ManagementRequest.PAGE_SIZE + 1, input.offset()));
        boolean canReply = !"CLOSED".equals(state);
        var admins = admin && canReply ? ManagementDisplay.choices(HelpDeskDao.activeAdmins(db)) : Map.<String, String>of();
        String jsp = admin ? "admin/inquiries/detail.jsp" : "support/inquiries/detail.jsp";
        return new ManagementView(jsp, "問い合わせ #" + id,
                new HelpDeskView.InquiryDetail(id, inquiry, posts, canReply, state, admins, admin ? INQUIRY_STATES : Map.of()));
    }

    /**
     * 対応状態や担当者で問い合わせを検索する。
     * @param db 問い合わせを参照するDAO接続。
     * @param input 管理者が指定した検索条件とページ。
     * @param actorId 管理者の会員ID。
     * @return 管理者の受付一覧のJSPと表示データ。
     * @throws SQLException 一覧や担当者を取得できなかった場合。
     */
    private static ManagementView adminInquiryList(ManagementDao db, ManagementRequest input, int actorId) throws SQLException {
        // 検索条件を取得し、画面が条件を再表示できるように戻す。
        String query = input.search();
        String category = input.value("category");
        String state = input.value("status");
        Integer assignee = input.id("assignee", false);
        var rows = HelpDeskDao.inquiries(db, true, actorId, category, state, assignee, query, ManagementRequest.PAGE_SIZE + 1, input.offset());
        rows.forEach(HelpDesk::showInquiryLabels);
        return new ManagementView("admin/inquiries/list.jsp", "問い合わせ管理",
                new HelpDeskView.AdminInquiryList(query, category, state, assignee, CATEGORIES, INQUIRY_STATES,
                        ManagementDisplay.choices(HelpDeskDao.activeAdmins(db)), input.pageRows(rows)));
    }

    /**
     * 会員本人の受付履歴と、問い合わせの関連物件を取得する。
     * @param db 問い合わせと公開物件を参照するDAO接続。
     * @param input 関連物件IDとページ。
     * @param actorId 受付履歴の所有者の会員ID。
     * @return 会員用の問い合わせ一覧のJSPと表示データ。
     * @throws SQLException 受付履歴や公開物件を取得できなかった場合。
     */
    private static ManagementView memberInquiryList(ManagementDao db, ManagementRequest input, int actorId) throws SQLException {
        // 関連物件が指定されている場合だけ、掲載中の物件か確認して公開項目を取得する。
        Integer propertyId = input.id("propertyId", false);
        var property = propertyId == null ? Map.<String, Object>of() : PublicDirectory.property(db, propertyId);
        if (propertyId != null) {
            property.remove("掲載者ID");
        }
        var rows = HelpDeskDao.inquiries(db, false, actorId, "", "", null, "", ManagementRequest.PAGE_SIZE + 1, input.offset());
        rows.forEach(HelpDesk::showInquiryLabels);
        return new ManagementView("support/inquiries/list.jsp", "お問い合わせ",
                new HelpDeskView.MemberInquiryList(propertyId, property, CATEGORIES, input.pageRows(rows)));
    }

    /**
     * 取得した分類と対応状態を画面上の名称に変換する。
     * @param row 問い合わせの検索結果または詳細。
     */
    private static void showInquiryLabels(Map<String, Object> row) {
        row.computeIfPresent("分類", (key, value) -> CATEGORIES.getOrDefault(value.toString(), value.toString()));
        row.computeIfPresent("状態", (key, value) -> INQUIRY_STATES.get(value.toString()));
    }

    /**
     * 問い合わせの登録・返信・担当・完了を、受付と投稿が整合する形で保存する。
     * @param db トランザクション内の接続。
     * @param input 送信内容。
     * @param actorId セッションの会員ID。
     * @param admin 管理画面の操作の場合にtrue。
     * @return 更新後の受付詳細へのURL。
     * @throws SQLException 保存に失敗した場合。
     */
    static String changeInquiry(ManagementDao db, ManagementRequest input, int actorId, boolean admin) throws SQLException {
        String operation = admin ? input.choice("operation", "操作", "assign", "reply", "note")
                : input.choice("operation", "操作", "open", "reply");
        if ("open".equals(operation)) {
            // 受付と最初の本文を同時に登録し、本文のない受付を残さない。
            Integer propertyId = input.id("propertyId", false);
            if (propertyId != null) { PublicDirectory.property(db, propertyId); }
            String category = input.choice("category", "カテゴリ", CATEGORIES.keySet().toArray(String[]::new));
            String subject = input.text("subject", "件名", 200, true);
            String body = input.text("body", "本文", 8000, true);
            long id = ManagementDisplay.key(HelpDeskDao.insertInquiry(db, actorId, propertyId, category, subject), "inquiry_id");
            post(db, id, actorId, body, "PARTICIPANTS");
            return ManagementPaths.INQUIRIES + "?id=" + id;
        }

        long id = input.ticket("id", true);
        // 同時の返信・状態変更は受付行で直列化する。会員側からは本人の受付だけを変更する。
        var old = HelpDeskDao.lockInquiry(db, id, admin, actorId);
        String previous = old.get("inquiry_status").toString();
        if ("note".equals(operation)) {
            post(db, id, actorId, input.text("body", "メモ", 8000, true), "ADMIN_ONLY");
            db.audit(actorId, "ADD_NOTE", "INQUIRY", Map.of("inquiry_id", id), null, Map.of());
            return ManagementPaths.ADMIN_INQUIRIES + "?id=" + id;
        }
        if ("CLOSED".equals(previous)) { throw new ManagementProblem(409, "この問い合わせは対応済みです。"); }
        Integer assignee = (Integer) old.get("assigned_admin_user_id");
        String state;
        if ("assign".equals(operation)) {
            // 割当先は本登録済みで有効な管理者に限定する。
            assignee = input.id("assignee", false);
            if (assignee != null) { requireAssignee(db, assignee); }
            state = input.choice("status", "対応状態", INQUIRY_STATES.keySet().toArray(String[]::new));
        } else {
            state = admin ? input.choice("status", "返信後の状態", "IN_PROGRESS", "WAITING_USER", "CLOSED") : "WAITING_ADMIN";
            post(db, id, actorId, input.text("body", "返信内容", 8000, true), "PARTICIPANTS");
            if (admin && assignee == null) { assignee = actorId; }
        }
        HelpDeskDao.updateInquiry(db, state, assignee, id);
        if (admin) {
            var changes = new java.util.LinkedHashMap<String, Object>();
            changes.put("before", previous);
            changes.put("after", state);
            changes.put("previous_assignee", old.get("assigned_admin_user_id"));
            changes.put("assignee", assignee);
            db.audit(actorId, "reply".equals(operation) ? "REPLY" : "ASSIGN", "INQUIRY", Map.of("inquiry_id", id), null, changes);
        }
        return (admin ? ManagementPaths.ADMIN_INQUIRIES : ManagementPaths.INQUIRIES) + "?id=" + id;
    }

    /**
     * 本人の通報受付結果または通報できる対象を取得する。
     * @param db 受付と対象を参照するDAO接続。
     * @param input 受付番号または対象ID。
     * @param user 認証済みの会員。
     * @return 受付結果または通報フォームのJSPと表示データ。
     * @throws SQLException 対象または受付を取得できなかった場合。
     */
    static ManagementView reportForm(ManagementDao db, ManagementRequest input, User user) throws SQLException {
        // 受付番号を指定した場合は、送信者本人の受付結果に絞って取得する。
        Long receipt = input.ticket("receipt", false);
        if (receipt != null) {
            var data = HelpDeskDao.reportReceipt(db, receipt, user.getUserId());
            data.computeIfPresent("状態", (key, value) -> REPORT_STATES.get(value.toString()));
            return new ManagementView("support/reports/receipt.jsp", "通報を受け付けました", new HelpDeskView.Receipt(data));
        }

        // 通報できる対象か確認し、対象の種類に適用できる理由を渡す。
        String kind = input.choice("kind", "通報対象", "PROPERTY", "OWNER", "BUSINESS");
        int id = input.id("id", true);
        Integer propertyId = input.id("propertyId", false);
        var target = PublicDirectory.reportTarget(db, user.getUserId(), kind, id, propertyId);
        String title = switch (kind) {
            case "OWNER" -> "物件オーナーを通報";
            case "BUSINESS" -> "事業主を通報";
            default -> "物件を通報";
        };
        return new ManagementView("support/reports/form.jsp", title,
                new HelpDeskView.ReportForm(kind, id, propertyId, target, reportReasons(kind)));
    }

    /**
     * 閲覧可能な対象と理由を確認して通報を受け付ける。
     * @param db トランザクション内の接続。
     * @param input 通報フォームの送信内容。
     * @param actorId 通報する会員ID。
     * @return 受付番号を表示するURL。
     * @throws SQLException 保存に失敗した場合。
     */
    static String submitReport(ManagementDao db, ManagementRequest input, int actorId) throws SQLException {
        input.choice("operation", "操作", "send");
        String kind = input.choice("kind", "通報対象", "PROPERTY", "OWNER", "BUSINESS");
        int targetId = input.id("id", true);
        Integer propertyId = input.id("propertyId", false);
        PublicDirectory.reportTarget(db, actorId, kind, targetId, propertyId);
        String reason = input.choice("reasonCode", "通報理由", reportReasons(kind).keySet().toArray(String[]::new));
        boolean property = "PROPERTY".equals(kind);
        long id = ManagementDisplay.key(HelpDeskDao.insertReport(db, actorId, property ? null : targetId, property ? targetId : null, "OWNER".equals(kind) ? propertyId : null, reason, input.text("body", "詳細", 8000, true)), "report_id");
        return ManagementPaths.REPORT + "?receipt=" + id;
    }

    /**
     * スレッドに本文・返信・メモを一件追記する。
     * @param db 保存用の接続。
     * @param id 問い合わせの受付番号。
     * @param sender 投稿者の会員ID。
     * @param body 入力検証済みの本文。
     * @param visibility サーバー側で指定した公開範囲。
     * @throws SQLException 保存に失敗した場合。
     */
    private static void post(ManagementDao db, long id, int sender, String body, String visibility) throws SQLException {
        HelpDeskDao.insertMessage(db, id, sender, body, visibility);
    }



    /**
     * 選択された担当者が現在も管理者であることを確認する。
     * @param db DB接続。
     * @param id 担当候補の会員ID。
     * @throws SQLException DB参照に失敗した場合。
     */
    private static void requireAssignee(ManagementDao db, int id) throws SQLException {
        if (!db.isAdmin(id, false)) { throw new ManagementProblem(400, "有効な管理者を選択してください。"); }
    }

    /**
     * 通報対象に合わせた理由を返す。
     * @param kind 通報対象の種類。
     * @return 対象に適用できる理由。
     */
    private static Map<String, String> reportReasons(String kind) {
        var reasons = new java.util.LinkedHashMap<>(REASONS);
        if ("PROPERTY".equals(kind)) { reasons.remove("HARASSMENT"); reasons.remove("IMPERSONATION"); }
        else { reasons.remove("DUPLICATE"); }
        return reasons;
    }

    /**
     * 対象ごとに実行可能な対応結果を返す。
     * @param property 対象が物件の場合にtrue。
     * @return 対応結果のコードと表示名。
     */
    private static Map<String, String> resolutionLabels(boolean property) {
        return property ? options("NO_ACTION", "対応不要", "CORRECTION", "修正依頼を記録", "HIDE_PROPERTY", "掲載停止")
                : options("NO_ACTION", "対応不要", "CORRECTION", "修正依頼を記録", "SUSPEND_USER", "会員の利用停止");
    }



}
