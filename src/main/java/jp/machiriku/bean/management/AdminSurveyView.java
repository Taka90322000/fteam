package jp.machiriku.bean.management;

import java.util.List;
import java.util.Map;

/** アンケートの一覧・設問・回答に渡す表示データ。 */
public final class AdminSurveyView {
    /** 画面データの型を機能単位でまとめる。 */
    private AdminSurveyView() { }

    /** 詳細画面で選択中のタブが受け取るデータ。 */
    public sealed interface Tab permits Overview, Questions, Responses { }

    /**
     * アンケートの検索条件と結果。
     * @param query アンケート名や店舗名の検索語。
     * @param kind 対象の種類。
     * @param state 対象の受付状態。
     * @param kinds アンケートの種類と名称。
     * @param states 受付状態の名称。
     * @param surveys 現在のアンケート一覧。
     */
    public record Listing(String query, String kind, String state, Map<String, String> kinds, Map<String, String> states,
            PageRows surveys) { }

    /**
     * 新規テンプレートに指定できる対象業種。
     * @param industries 選択できる業種。
     */
    public record Template(Map<String, String> industries) { }

    /**
     * 選んだアンケートと表示中のタブのデータ。
     * @param id アンケートID。
     * @param kind アンケートの種類。
     * @param state 現在の受付状態。
     * @param tab 閲覧中の情報の種類。
     * @param survey アンケートの名前や受付日時など。
     * @param editable 下書きを編集できる場合はtrue。
     * @param detail 選択したタブに対応するデータ。
     */
    public record Detail(int id, String kind, String state, String tab, Map<String, Object> survey, boolean editable, Tab detail) { }

    /**
     * アンケート情報を編集・改訂できる条件。
     * @param industries テンプレートに指定できる対象業種。
     * @param canClose 受付を停止できる場合はtrue。
     * @param canRevise 改訂下書きを作成できる場合はtrue。
     */
    public record Overview(Map<String, String> industries, boolean canClose, boolean canRevise) implements Tab { }

    /**
     * 設問と選んだ設問の現在値。
     * @param questions 追加設問と選択肢。
     * @param number 編集中の設問番号。未選択の場合はnull。
     * @param selected 編集中の設問の現在値。
     */
    public record Questions(List<Map<String, Object>> questions, Integer number, Map<String, Object> selected) implements Tab { }

    /**
     * 満足度分布・選択肢の集計と回答履歴。
     * @param ratings 満足度別の回答件数。
     * @param counts 設問ごと・選択肢ごとの件数。
     * @param answers 回答履歴の現在のページ。
     */
    public record Responses(List<Map<String, Object>> ratings, List<Map<String, Object>> counts, PageRows answers) implements Tab { }
}
