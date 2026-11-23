package jp.machiriku.bean.management;

import java.util.Map;

/** 運営ダッシュボードと操作履歴に渡す表示データ。 */
public final class AdminOverviewView {
    /** 画面データの型を機能単位でまとめる。 */
    private AdminOverviewView() { }

    /**
     * 通報・問い合わせと運用状況の集計値。
     * @param support 通報と問い合わせの件数。
     * @param operations 会員・物件・アンケート・クーポンの件数。
     */
    public record Dashboard(Map<String, Object> support, Map<String, Object> operations) { }

    /**
     * 操作履歴と検索条件。
     * @param query 入力した検索語。
     * @param entries 操作履歴の現在のページ。
     */
    public record Audit(String query, PageRows entries) { }
}
