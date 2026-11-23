package jp.machiriku.servlet.management;

import static jp.machiriku.servlet.management.ManagementDisplay.options;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import jp.machiriku.bean.management.AdminListingView;
import jp.machiriku.dao.management.AdminListingDao;
import jp.machiriku.dao.management.ManagementDao;

/** 物件の掲載・内容の管理と、申込・契約・計画・オファーの詳細表示を担当する。 */
final class AdminListings {
    /** 募集・契約の状態を選択するための表示名。 */
    private static final Map<String, String> LISTING_STATES = options("DRAFT", "下書き", "OPEN", "募集中", "PAUSED", "募集停止中", "CONTRACTED", "契約済み");
    /** 許認可の確認状態。 */
    private static final Map<String, String> PERMITS = options("UNKNOWN", "未確認", "OBTAINED", "取得済み", "NOT_OBTAINED", "未取得");

    /** 同じ物件の管理処理を静的に利用する。 */
    private AdminListings() { }

    /**
     * 掲載物件一覧や選んだ物件の詳細・関連履歴を照会する。
     * @param db 物件を参照するDAO接続。
     * @param input 物件ID、表示するタブ、一覧の検索条件。
     * @return 物件管理のJSPと表示データ。
     * @throws SQLException 物件情報を取得できなかった場合。
     */
    static ManagementView properties(ManagementDao db, ManagementRequest input) throws SQLException {
        // 物件を指定していない場合は検索一覧を表示する。
        Integer id = input.id("id", false);
        if (id == null) {
            return propertyList(db, input);
        }
        var property = AdminListingDao.property(db, id);
        String tab = input.value("tab").isEmpty() ? "info" : input.choice("tab", "表示する情報", "info", "applications", "reports", "offers");
        String name = property.get("名前").toString();
        if (!"info".equals(tab)) {
            // 履歴のタブでは、選択されている関連履歴だけを取得する。
            var history = input.pageRows(AdminListingDao.propertyHistory(db, tab, id, ManagementRequest.PAGE_SIZE + 1, input.offset()));
            return new ManagementView("admin/properties/detail.jsp", name,
                    new AdminListingView.Detail(id, tab, property, null, history));
        }

        // 募集状態の表示名と、現在の状態で実行できる掲載操作を渡す。
        String state = property.get("募集・契約状態").toString();
        boolean hidden = property.get("掲載停止日時") != null;
        boolean editable = property.get("削除日時") == null;
        boolean canPublish = editable && !hidden && ("DRAFT".equals(state) || "PAUSED".equals(state));
        property.put("募集・契約状態", LISTING_STATES.get(state));
        var info = new AdminListingView.PropertyInfo(AdminListingDao.propertyImages(db, id), AdminListingDao.propertyIndustries(db, id),
                AdminListingDao.propertyContractTerms(db, id), editable, hidden, canPublish, PERMITS);
        return new ManagementView("admin/properties/detail.jsp", name, new AdminListingView.Detail(id, tab, property, info, null));
    }

    /**
     * 掲載物件を募集状態と掲載状態から絞り込む。
     * @param db 物件を参照するDAO接続。
     * @param input 物件の検索語、状態とページ。
     * @return 物件一覧のJSPと検索結果。
     * @throws SQLException 物件一覧を取得できなかった場合。
     */
    private static ManagementView propertyList(ManagementDao db, ManagementRequest input) throws SQLException {
        // フィルターに使用した値をそのまま表示用データにも渡す。
        String status = input.value("status");
        String visibility = input.value("kind");
        String query = input.search();
        var rows = AdminListingDao.properties(db, status, visibility, query, ManagementRequest.PAGE_SIZE + 1, input.offset());
        for (var row : rows) {
            row.computeIfPresent("募集・契約状態", (key, value) -> LISTING_STATES.get(value.toString()));
        }
        return new ManagementView("admin/properties/list.jsp", "物件管理",
                new AdminListingView.Listing(query, status, visibility, LISTING_STATES, input.pageRows(rows)));
    }

    /**
     * 物件の掲載停止・公開・修正を保存する。
     * @param db トランザクション内の接続。
     * @param input 更新内容。
     * @param actorId 操作する管理者の会員ID。
     * @return 更新後の物件詳細。
     * @throws SQLException 更新できなかった場合。
     */
    static String changeProperty(ManagementDao db, ManagementRequest input, int actorId) throws SQLException {
        int id = input.id("id", true);
        String operation = input.choice("operation", "操作", "hide", "unhide", "edit", "publish");
        if ("hide".equals(operation) || "unhide".equals(operation)) {
            hideProperty(db, id, actorId, "hide".equals(operation), input.text("reason", "操作理由", 2000, true));
        } else {
            var before = AdminListingDao.lockProperty(db, id);
            if (before.get("deleted_at") != null) { throw new ManagementProblem(409, "削除済みの物件は更新できません。"); }
            if ("publish".equals(operation)) {
                if (!java.util.Set.of("DRAFT", "PAUSED").contains(before.get("listing_status")) || before.get("admin_hidden_at") != null) {
                    throw new ManagementProblem(409, "この物件は募集を公開できる状態ではありません。");
                }
                AdminListingDao.publishProperty(db, id);
                db.audit(actorId, "PUBLISH", "PROPERTY", Map.of("property_id", id), input.text("reason", "操作理由", 2000, true),
                        Map.of("before", before.get("listing_status"), "after", "OPEN"));
            } else {
                saveProperty(db, input, actorId, id);
            }
        }
        return ManagementPaths.ADMIN_PROPERTIES + "?id=" + id;
    }

    /**
     * 公開判定に使う管理者の停止状態を変更する。
     * @param db 物件と履歴を一括保存する接続。
     * @param id 対象物件ID。
     * @param actorId 管理者の会員ID。
     * @param hide 停止する場合にtrue。
     * @param reason 管理者の操作理由。
     * @throws SQLException 更新に失敗した場合。
     */
    static void hideProperty(ManagementDao db, int id, int actorId, boolean hide, String reason) throws SQLException {
        var old = AdminListingDao.lockPropertyVisibility(db, id);
        boolean wasHidden = old.get("admin_hidden_at") != null;
        if (old.get("deleted_at") != null) { throw new ManagementProblem(409, "削除済みの物件は掲載状態を変更できません。"); }
        if (wasHidden == hide) { return; }
        AdminListingDao.updateVisibility(db, hide, id);
        db.audit(actorId, hide ? "HIDE" : "UNHIDE", "PROPERTY", Map.of("property_id", id), reason, Map.of("before", wasHidden, "after", hide));
    }

    /**
     * 物件に掲載する内容と座標を更新する。
     * @param db トランザクション内の接続。
     * @param input 掲載内容の入力。
     * @param actorId 編集する管理者の会員ID。
     * @param id 物件ID。
     * @throws SQLException 内容を保存できなかった場合。
     */
    private static void saveProperty(ManagementDao db, ManagementRequest input, int actorId, int id) throws SQLException {
        var lat = input.decimal("lat", "緯度", -90, 90, 6, false);
        var lng = input.decimal("lng", "経度", -180, 180, 6, false);
        if ((lat == null) != (lng == null)) { throw new ManagementProblem(400, "緯度と経度を一緒に入力してください。"); }
        AdminListingDao.updateProperty(db, input.text("name", "物件名", 150, true), input.text("prefecture", "都道府県", 10, true), input.text("city", "市区町村", 50, true), input.text("street", "番地", 200, true), lat, lng, input.decimal("floor", "面積", .01, 99999999.99, 2, true), input.number("rent", "賃料", 0, Integer.MAX_VALUE), input.text("description", "説明", 8000, false), input.webUrl("url", "掲載URL"), input.choice("permit", "許認可", "UNKNOWN", "OBTAINED", "NOT_OBTAINED"), id);
        db.audit(actorId, "UPDATE", "PROPERTY", Map.of("property_id", id), null, Map.of("section", "listing"));
    }

    /**
     * 出店申し込み、契約、出店計画またはオファーの詳細を取得する。
     * @param db 関連情報を照会するDAO接続。
     * @param input 各レコードを特定するID。
     * @param recordType 対応するActionが指定した記録の種類。
     * @return 記録の種類に対応するJSPと詳細データ。
     * @throws SQLException 関連情報を取得できなかった場合。
     */
    static ManagementView related(ManagementDao db, ManagementRequest input, String recordType) throws SQLException {
        // 対象の種類とIDで照会し、各画面に必要な添付情報を読み込む。
        int id = input.id("id", true);
        var detail = AdminListingDao.relatedRecord(db, recordType, id);
        return switch (recordType) {
            case "applications" -> new ManagementView("admin/properties/application.jsp", "出店申し込み",
                    new AdminListingView.Related(id, detail, AdminListingDao.applicationChecks(db, id), false));
            case "tenancies" -> new ManagementView("admin/properties/tenancy.jsp", "契約物件",
                    new AdminListingView.Related(id, detail, AdminListingDao.openingPlans(db, id), false));
            case "plans" -> new ManagementView("admin/properties/plan.jsp", "出店計画",
                    new AdminListingView.Related(id, detail, List.of(), "PUBLISHED".equals(detail.get("公開状態"))));
            default -> new ManagementView("admin/properties/offer.jsp", "出店オファー",
                    new AdminListingView.Related(id, detail, List.of(), false));
        };
    }

    /**
     * 公開された出店計画を運営対応として非公開にする。
     * @param db トランザクション内の接続。
     * @param input 計画と停止理由。
     * @param actorId 操作する管理者の会員ID。
     * @return 操作後の詳細URL。
     * @throws SQLException 更新できなかった場合。
     */
    static String changePlan(ManagementDao db, ManagementRequest input, int actorId) throws SQLException {
        int id = input.id("id", true);
        input.choice("operation", "操作", "archive");
        String reason = input.text("reason", "停止理由", 2000, true);
        if (AdminListingDao.archivePlan(db, id) == 0) {
            throw new ManagementProblem(409, "この出店計画の公開状態は変更されています。");
        }
        db.audit(actorId, "ARCHIVE", "OPENING_PLAN", Map.of("plan_id", id), reason, Map.of("before", "PUBLISHED", "after", "ARCHIVED"));
        return ManagementPaths.ADMIN_PLANS + "?id=" + id;
    }
}
