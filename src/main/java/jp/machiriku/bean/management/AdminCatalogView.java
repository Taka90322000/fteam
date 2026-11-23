package jp.machiriku.bean.management;

import java.util.Map;

/** 業種・資格マスターと業種別の要件を表示するためのデータ。 */
public final class AdminCatalogView {
    /** 画面データの型を機能単位でまとめる。 */
    private AdminCatalogView() { }

    /**
     * 業種またはカテゴリの一覧と検索条件。
     * @param kind 業種かカテゴリかを示す区分。
     * @param query 名称やコードの検索語。
     * @param status 絞り込む有効状態。
     * @param industries 検索結果。
     */
    public record IndustryList(String kind, String query, String status, PageRows industries) { }

    /**
     * 業種またはカテゴリの登録済みの値。
     * @param kind 業種かカテゴリかを示す区分。
     * @param id 編集対象のID。新規の場合はnull。
     * @param current 既存データ。新規の場合は空。
     * @param categories 所属先として選択できるカテゴリ。
     */
    public record IndustryEdit(String kind, Integer id, Map<String, Object> current, Map<String, String> categories) { }

    /**
     * 資格・書類の一覧と検索条件。
     * @param kind 絞り込む資格・書類の種類。
     * @param query 名称の検索語。
     * @param status 絞り込む有効状態。
     * @param kinds 資格・書類の種類と名称。
     * @param qualifications 検索結果。
     */
    public record QualificationList(String kind, String query, String status, Map<String, String> kinds, PageRows qualifications) { }

    /**
     * 資格・書類の編集対象と現在値。
     * @param id 編集対象の資格・書類ID。新規の場合はnull。
     * @param current 既存データ。新規の場合は空。
     * @param kinds 選択できる資格・書類の種類。
     */
    public record QualificationEdit(Integer id, Map<String, Object> current, Map<String, String> kinds) { }

    /**
     * 業種に必要な資格・書類の紐付けと現在値。
     * @param industryId 選択した業種ID。未選択の場合はnull。
     * @param industries 選択できる業種。
     * @param requirements 対象業種の必要条件。業種未選択の場合はnull。
     * @param qualificationId 選択した資格ID。未選択の場合はnull。
     * @param current 設定済みの必須区分や適用条件。
     * @param qualifications 設定に使える資格・許認可・書類。
     */
    public record Requirements(Integer industryId, Map<String, String> industries, PageRows requirements, Integer qualificationId,
            Map<String, Object> current, Map<String, String> qualifications) { }
}
