package jp.machiriku.servlet.management;


import java.sql.SQLException;
import java.util.Map;

import jp.machiriku.bean.management.PublicDirectoryView;
import jp.machiriku.bean.User;

import jp.machiriku.dao.management.PublicDirectoryDao;
import jp.machiriku.dao.management.ManagementDao;

/** DBに登録された物件と、会員が閲覧できる通報対象を表示する。 */
final class PublicDirectory {
    /** 共通の対象照会を静的に利用する。 */
    private PublicDirectory() { }

    /**
     * 公開物件の一覧または詳細を取得する。
     * @param db 公開物件を参照するDAO接続。
     * @param input 物件ID、検索語とページ。
     * @param viewer 閲覧する会員。ゲストはnull。
     * @return 公開物件のJSPと表示データ。
     * @throws SQLException 物件を取得できなかった場合。
     */
    static ManagementView properties(ManagementDao db, ManagementRequest input, User viewer) throws SQLException {
        // IDを選んでいない場合は公開物件を一覧で表示する。
        Integer id = input.id("id", false);
        if (id == null) {
            String query = input.search();
            var entries = input.pageRows(PublicDirectoryDao.properties(db, query, ManagementRequest.PAGE_SIZE + 1, input.offset()));
            return new ManagementView("support/properties/list.jsp", "掲載物件", new PublicDirectoryView.Listing(query, entries));
        }

        // 掲載者IDは表示項目と分け、閲覧者が利用できる窓口の判定に使う。
        var data = property(db, id);
        int ownerId = ((Number) data.remove("掲載者ID")).intValue();
        boolean canReport = viewer != null && "ACTIVE".equals(viewer.getAccountStatus()) && viewer.getUserId() != ownerId;
        return new ManagementView("support/properties/detail.jsp", data.get("名前").toString(),
                new PublicDirectoryView.Property(id, ownerId, data, viewer != null, canReport));
    }

    /**
     * 現在公開されている物件をIDで取得する。
     * @param db DB接続。
     * @param id 物件ID。
     * @return 物件の公開項目と掲載者ID。
     * @throws SQLException DB参照に失敗した場合。
     */
    static Map<String, Object> property(ManagementDao db, int id) throws SQLException {
        return PublicDirectoryDao.property(db, id);
    }

    /**
     * 閲覧者が参照できる事業主の一覧と公開プロフィールを取得する。
     * @param db 事業主の情報を参照するDAO接続。
     * @param input 対象会員ID、検索語とページ。
     * @param viewer ログイン済みの閲覧者。
     * @return 事業主を表示するJSPとデータ。
     * @throws SQLException 参照できる事業主が見つからないか、取得に失敗した場合。
     */
    static ManagementView businesses(ManagementDao db, ManagementRequest input, User viewer) throws SQLException {
        // 閲覧者の会員IDで参照できる事業主に絞り込む。
        Integer id = input.id("id", false);
        int viewerId = viewer.getUserId();
        if (id == null) {
            String query = input.search();
            var entries = input.pageRows(PublicDirectoryDao.businesses(db, viewerId, query, ManagementRequest.PAGE_SIZE + 1, input.offset()));
            return new ManagementView("support/businesses/list.jsp", "事業主一覧", new PublicDirectoryView.Listing(query, entries));
        }
        var data = business(db, viewerId, id);
        boolean canReport = viewerId != id && "ACTIVE".equals(viewer.getAccountStatus());
        return new ManagementView("support/businesses/detail.jsp", data.get("名前").toString(),
                new PublicDirectoryView.Business(id, data, canReport));
    }

    /**
     * 閲覧者との関係を確認して事業主プロフィールの公開項目を取得する。
     * @param db DB接続。
     * @param viewerId 閲覧者の会員ID。
     * @param businessId 対象の事業主ID。
     * @return 閲覧できる事業主の情報。
     * @throws SQLException DB参照に失敗した場合。
     */
    private static Map<String, Object> business(ManagementDao db, int viewerId, int businessId) throws SQLException {
        return PublicDirectoryDao.business(db, businessId, viewerId);
    }

    /**
     * 送信フォームとDB保存の両方で、通報できる対象かを確認する。
     * @param db DB接続。
     * @param viewerId セッションの会員ID。
     * @param kind 物件・オーナー・事業主のいずれか。
     * @param id 対象の会員IDまたは物件ID。
     * @param propertyId オーナーの確認に使う物件ID。
     * @return 利用者に表示できる対象名。
     * @throws SQLException DB参照に失敗した場合。
     */
    static Map<String, Object> reportTarget(ManagementDao db, int viewerId, String kind, int id, Integer propertyId) throws SQLException {
        if ("PROPERTY".equals(kind)) {
            var target = property(db, id);
            if (((Number) target.get("掲載者ID")).intValue() == viewerId) { throw new ManagementProblem(403, "自身の掲載物件には通報できません。"); }
            return Map.of("物件名", target.get("名前"));
        }
        if (viewerId == id) { throw new ManagementProblem(403, "自身のアカウントには通報できません。"); }
        if ("BUSINESS".equals(kind)) { return Map.of("事業者名", business(db, viewerId, id).get("名前")); }
        // 掲載者を通報するときは、指定された物件と実際のオーナーが一致することを確認する。
        if (propertyId == null) { throw new ManagementProblem(400, "通報に関連する物件を選んでください。"); }
        return PublicDirectoryDao.listedOwner(db, propertyId, id);
    }

    /**
     * 公開中の必要資格と書類を選択した業種で絞り込む。
     * @param db 公開要件を参照するDAO接続。
     * @param input 対象の業種IDとページ。
     * @return 必要資格・書類案内のJSPと一覧データ。
     * @throws SQLException 要件や業種を取得できなかった場合。
     */
    static ManagementView requirements(ManagementDao db, ManagementRequest input) throws SQLException {
        // 参照する業種と検索結果のページをセットで渡す。
        Integer industry = input.id("industry", false);
        var industries = ManagementDisplay.choices(PublicDirectoryDao.industryChoices(db));
        var rows = input.pageRows(PublicDirectoryDao.requirements(db, industry, ManagementRequest.PAGE_SIZE + 1, input.offset()));
        return new ManagementView("support/requirements/list.jsp", "必要資格・書類の案内",
                new PublicDirectoryView.Requirements(industry, industries, rows));
    }

}
