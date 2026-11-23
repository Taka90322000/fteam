package jp.machiriku.bean.management;

import java.util.Map;

/** クーポンの配布・発行・利用状況の表示データ。 */
public final class AdminCouponView {
    /** 画面データの型を機能単位でまとめる。 */
    private AdminCouponView() { }

    /**
     * クーポンの検索条件と結果。
     * @param query クーポン名や店舗名の検索語。
     * @param state 絞り込む配布状態。
     * @param states 配布状態の名称。
     * @param coupons 現在のクーポン一覧。
     */
    public record Listing(String query, String state, Map<String, String> states, PageRows coupons) { }

    /**
     * クーポンの現在値と、編集中または閲覧中の情報。
     * @param id クーポンID。
     * @param state 配布状態。
     * @param benefit 特典の種類。
     * @param benefits 特典の種類と名称。
     * @param coupon クーポンの詳細。
     * @param editable 下書きを編集できる場合はtrue。
     * @param history 発行履歴を開いた場合の情報。
     */
    public record Detail(int id, String state, String benefit, Map<String, String> benefits, Map<String, Object> coupon,
            boolean editable, Recipients history) { }

    /**
     * 発行・利用履歴と、開いている発行記録。
     * @param entries 発行・利用履歴の現在のページ。
     * @param selectedId 選んだ受取会員のID。未選択の場合はnull。
     * @param selected 選んだ発行記録。未選択の場合は空。
     * @param canRevoke 選択した発行分を無効化できる場合はtrue。
     */
    public record Recipients(PageRows entries, Integer selectedId, Map<String, Object> selected, boolean canRevoke) { }
}
