package jp.machiriku.bean.management;

import java.util.Map;

/** 公開している物件・事業主・必要資格の表示データ。 */
public final class PublicDirectoryView {
    /** 画面データの型を機能単位でまとめる。 */
    private PublicDirectoryView() { }

    /**
     * 公開物件または事業主の検索結果。
     * @param query 画面に表示する検索語。
     * @param entries 検索結果の現在のページ。
     */
    public record Listing(String query, PageRows entries) { }

    /**
     * 公開物件の詳細と利用できる窓口。
     * @param id 公開物件のID。
     * @param ownerId 掲載者のID。
     * @param property 物件の公開情報。
     * @param canInquire 物件の問い合わせを送れる場合はtrue。
     * @param canReport 物件や掲載者を通報できる場合はtrue。
     */
    public record Property(int id, int ownerId, Map<String, Object> property, boolean canInquire, boolean canReport) { }

    /**
     * 掲載可能な事業主プロフィール。
     * @param id 事業主の会員ID。
     * @param business 公開できるプロフィール。
     * @param canReport 事業主を通報できる場合はtrue。
     */
    public record Business(int id, Map<String, Object> business, boolean canReport) { }

    /**
     * 業種別の必要資格・書類と検索条件。
     * @param industryId 選択した業種ID。未選択ならnull。
     * @param industries 公開している業種の名称。
     * @param requirements 表示する必要資格・書類。
     */
    public record Requirements(Integer industryId, Map<String, String> industries, PageRows requirements) { }
}
