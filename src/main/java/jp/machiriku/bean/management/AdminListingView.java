package jp.machiriku.bean.management;

import java.util.List;
import java.util.Map;

/** 管理者向けの物件情報・申込・契約・出店計画に渡す表示データ。 */
public final class AdminListingView {
    /** 画面データの型を機能単位でまとめる。 */
    private AdminListingView() { }

    /**
     * 掲載物件の検索条件と結果。
     * @param query 物件名や所在地などの検索語。
     * @param status 絞り込む募集・契約状態。
     * @param visibility 絞り込む掲載状態。
     * @param states 募集・契約状態の名称。
     * @param properties 現在の物件一覧。
     */
    public record Listing(String query, String status, String visibility, Map<String, String> states, PageRows properties) { }

    /**
     * 選択された物件と表示する情報。
     * @param id 物件ID。
     * @param tab 閲覧中の情報の種類。
     * @param property 物件の所在地や賃料など。
     * @param info 物件情報を選んだ場合の関連資料と編集可否。
     * @param history 申込や通報などの履歴を選んだ場合の行。
     */
    public record Detail(int id, String tab, Map<String, Object> property, PropertyInfo info, PageRows history) { }

    /**
     * 物件に付随する資料と掲載に関する操作可否。
     * @param images 登録されている写真。
     * @param industries 物件を対象とする業種。
     * @param terms 契約の条件。
     * @param editable 掲載内容を編集できる場合はtrue。
     * @param hidden 掲載停止中の場合はtrue。
     * @param canPublish 募集を公開できる場合はtrue。
     * @param permits 許認可の状態と名称。
     */
    public record PropertyInfo(List<Map<String, Object>> images, List<Map<String, Object>> industries, List<Map<String, Object>> terms,
            boolean editable, boolean hidden, boolean canPublish, Map<String, String> permits) { }

    /**
     * 申込、契約、出店計画やオファーの詳細。
     * @param id 対象のレコードID。
     * @param record 詳細情報。
     * @param children 申込の必要条件または契約の出店計画。
     * @param canArchive 出店計画の公開を終了できる場合はtrue。
     */
    public record Related(int id, Map<String, Object> record, List<Map<String, Object>> children, boolean canArchive) { }
}
