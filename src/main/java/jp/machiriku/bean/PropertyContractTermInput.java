package jp.machiriku.bean;

/**
 * 物件の契約形態ごとの条件を保持する不変データクラス。
 * property_contract_terms テーブルの1行に対応する。
 *
 * @param contractKind 契約区分（LONG または POPUP）
 * @param minDays 最短利用日数（POPUP時は1以上、LONG時はnull）
 * @param maxDays 最長利用日数（POPUP時はminDays以上、LONG時はnull）
 * @param dailyRentYen 日額賃料目安（円・0以上、任意のためnull可）
 */
public record PropertyContractTermInput(
        String contractKind,
        Integer minDays,
        Integer maxDays,
        Integer dailyRentYen) {
}
