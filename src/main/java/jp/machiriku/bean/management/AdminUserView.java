package jp.machiriku.bean.management;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 管理者向けの会員一覧・会員詳細に渡す表示データ。 */
public final class AdminUserView {
    /** 画面データの型を機能単位でまとめる。 */
    private AdminUserView() { }

    /** 詳細画面で選択中のタブが受け取るデータ。 */
    public sealed interface Tab permits Profile, Preferences, Locations, Qualifications, History { }

    /**
     * 選択している利用区分の会員一覧。
     * @param role 現在の利用区分。
     * @param query 名前や会員IDなどの検索語。
     * @param status 絞り込む利用状態。
     * @param from 登録日の下限。
     * @param to 登録日の上限。
     * @param roles 利用区分の名称。
     * @param states 利用状態の名称。
     * @param members 現在の会員一覧。
     */
    public record Listing(String role, String query, String status, LocalDate from, LocalDate to, Map<String, String> roles,
            Map<String, String> states, PageRows members) { }

    /**
     * 会員の共通情報と選択しているタブのデータ。
     * @param id 会員ID。
     * @param role 会員の利用区分。
     * @param tab 閲覧中の情報の種類。
     * @param member 会員の共通情報。
     * @param detail 選択したタブに対応するデータ。
     */
    public record Detail(int id, String role, String tab, Map<String, Object> member, Tab detail) { }

    /**
     * 連絡先の現在値と管理者が行える操作。
     * @param profile 登録された区分別プロフィール。未登録の場合は空。
     * @param editable プロフィールを編集できる場合はtrue。
     * @param canChangeStatus 停止・解除を行える場合はtrue。
     * @param suspended 会員が利用停止中の場合はtrue。
     * @param actions 最新の操作履歴。
     */
    public record Profile(Map<String, Object> profile, boolean editable, boolean canChangeStatus, boolean suspended,
            List<Map<String, Object>> actions) implements Tab { }

    /**
     * 会員が希望している業種と編集に使うマスター。
     * @param industries 登録済みの希望業種。
     * @param selectedIds 選択済みの業種ID。
     * @param choices 追加で選択できる業種。
     * @param editable 希望業種を編集できる場合はtrue。
     */
    public record Preferences(List<Map<String, Object>> industries, List<Integer> selectedIds, Map<String, String> choices,
            boolean editable) implements Tab { }

    /**
     * 生活拠点の登録情報と選んだ種別。
     * @param locations 登録済みの生活拠点。
     * @param kind 選択している居住地などの種類。
     * @param current 選択した生活拠点の現在値。未登録の場合は空。
     * @param editable 生活拠点を編集できる場合はtrue。
     */
    public record Locations(List<Map<String, Object>> locations, String kind, Map<String, Object> current, boolean editable) implements Tab { }

    /**
     * 保有資格と編集に使うマスターと現在値。
     * @param qualifications 登録済みの保有資格。
     * @param selectedId 編集対象の資格ID。未選択の場合はnull。
     * @param current 資格の取得日などの登録済みの値。
     * @param choices 登録できる資格・許認可。
     * @param editable 資格を編集できる場合はtrue。
     */
    public record Qualifications(List<Map<String, Object>> qualifications, Integer selectedId, Map<String, Object> current,
            Map<String, String> choices, boolean editable) implements Tab { }

    /**
     * 会員の投票・申込・回答などの履歴。
     * @param records 閲覧中の履歴の現在のページ。
     */
    public record History(PageRows records) implements Tab { }
}
