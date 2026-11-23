package jp.machiriku.bean.management;

import java.time.LocalDate;
import java.util.Map;

/** 通報の受付と、問い合わせのやり取りを表示するためのデータ。 */
public final class HelpDeskView {
    /** 画面データの型を機能単位でまとめる。 */
    private HelpDeskView() { }

    /**
     * 通報一覧と検索条件。
     * @param query 対象名や受付番号の検索語。
     * @param kind 通報対象の区分。
     * @param state 絞り込む対応状態。
     * @param reason 絞り込む通報理由。
     * @param from 受付日の下限。
     * @param to 受付日の上限。
     * @param kinds 対象区分の名称。
     * @param states 対応状態の名称。
     * @param reasons 通報理由の名称。
     * @param reports 現在の通報一覧。
     */
    public record ReportList(String query, String kind, String state, String reason, LocalDate from, LocalDate to,
            Map<String, String> kinds, Map<String, String> states, Map<String, String> reasons, PageRows reports) { }

    /**
     * 通報の詳細と対応に必要な値。
     * @param id 通報の受付番号。
     * @param report 対象・理由・対応などの情報。
     * @param editable 通報に対して対応できる場合はtrue。
     * @param assignee 選択済みの担当者ID。
     * @param admins 担当に指定できる管理者。
     * @param resolutions 対象に適用できる対応結果。
     */
    public record ReportDetail(long id, Map<String, Object> report, boolean editable, Integer assignee, Map<String, String> admins,
            Map<String, String> resolutions) { }

    /**
     * 管理者の問い合わせ受付一覧と検索条件。
     * @param query 受付番号や送信者、件名の検索語。
     * @param category 絞り込む問い合わせの分類。
     * @param state 絞り込む問い合わせの状態。
     * @param assignee 絞り込む担当者ID。
     * @param categories 分類の名称。
     * @param states 対応状態の名称。
     * @param admins 担当に指定できる管理者。
     * @param inquiries 現在の受付一覧。
     */
    public record AdminInquiryList(String query, String category, String state, Integer assignee, Map<String, String> categories,
            Map<String, String> states, Map<String, String> admins, PageRows inquiries) { }

    /**
     * 会員本人の受付履歴と新規問い合わせの初期値。
     * @param propertyId 関連付ける掲載物件ID。未選択ならnull。
     * @param property 関連付ける掲載物件の公開情報。
     * @param categories 送信できる問い合わせの分類。
     * @param inquiries 本人の受付履歴。
     */
    public record MemberInquiryList(Integer propertyId, Map<String, Object> property, Map<String, String> categories, PageRows inquiries) { }

    /**
     * 閲覧を許可された問い合わせとやり取り。
     * @param id 問い合わせの受付番号。
     * @param inquiry 受付の詳細。
     * @param posts 閲覧できる範囲の投稿と返信。
     * @param canReply 返信を受け付ける場合はtrue。
     * @param state 現在の対応状態。
     * @param admins 管理者用画面で選べる担当者。
     * @param states 管理者用画面で選べる対応状態。
     */
    public record InquiryDetail(long id, Map<String, Object> inquiry, PageRows posts, boolean canReply, String state,
            Map<String, String> admins, Map<String, String> states) { }

    /**
     * 通報対象と選択可能な理由。
     * @param kind 対象が物件か会員かを示す区分。
     * @param id 通報する対象のID。
     * @param propertyId 関連する物件ID。
     * @param target 通報者に表示できる対象情報。
     * @param reasons 選択可能な通報理由。
     */
    public record ReportForm(String kind, int id, Integer propertyId, Map<String, Object> target, Map<String, String> reasons) { }

    /**
     * 送信した会員が閲覧できる通報結果。
     * @param receipt 本人の受付番号と状態。
     */
    public record Receipt(Map<String, Object> receipt) { }
}
