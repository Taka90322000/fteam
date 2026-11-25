package jp.machiriku.bean;

import java.math.BigDecimal;
import java.util.List;

/**
 * 検証済みの物件登録入力データを保持する不変データクラス。
 * properties・property_contract_terms・property_industries・property_facilities・property_images の保存に使用する。
 *
 * @param propertyName 物件名称（必須、最大150文字）
 * @param prefecture 都道府県（必須、最大10文字）
 * @param municipality 市区町村（必須、最大50文字）
 * @param addressLine 町名・番地・建物名（必須、最大200文字）
 * @param latitude 緯度（任意、経度と同時設定、小数第6位まで）
 * @param longitude 経度（任意、緯度と同時設定、小数第6位まで）
 * @param transportAccess 交通・最寄駅アクセス（必須、最大150文字）
 * @param floorAreaSqm 使用部分面積（㎡、必須、0より大きい値）
 * @param monthlyRentYen 月額賃料（円、必須、0以上）
 * @param managementFeeYen 月額管理費・共益費（円、任意、0以上）
 * @param depositKind 敷金区分（NONE / MONTHS / NEGOTIABLE）
 * @param depositMonths 敷金月数（depositKindがMONTHSのとき1以上）
 * @param keyMoneyKind 礼金区分（NONE / MONTHS / NEGOTIABLE）
 * @param keyMoneyMonths 礼金月数（keyMoneyKindがMONTHSのとき1以上）
 * @param securityDepositKind 保証金区分（NONE / MONTHS / AMOUNT / NEGOTIABLE）
 * @param securityDepositMonths 保証金月数（securityDepositKindがMONTHSのとき1以上）
 * @param securityDepositYen 保証金額（円、securityDepositKindがAMOUNTのとき1以上）
 * @param floorCode 所在階区分（任意、B2〜10F_PLUS / WHOLE_BUILDING / OTHER）
 * @param totalFloorsCode 建物階数区分（任意、1F〜11F_PLUS）
 * @param buildingStructure 建物構造（任意、WOOD / LIGHT_STEEL / STEEL / RC / SRC / ALC / OTHER）
 * @param builtYear 築年（西暦4桁、任意、1900〜2100）
 * @param builtMonth 築月（1〜12、任意、builtYear設定時のみ指定可）
 * @param handoverTiming 引渡し時期・状態（任意）
 * @param conditionMemo 居抜き・設備・業種条件メモ（任意、最大300文字）
 * @param description 物件の特徴・アピール説明文（任意、最大2000文字）
 * @param listingUrl 既存の不動産掲載ページURL（任意、最大1000文字）
 * @param permitStatus 営業許可の現状（UNKNOWN / OBTAINED / NOT_OBTAINED）
 * @param listingStatus 公開ステータス（OPEN / DRAFT）
 * @param contractTerms 契約形態の条件一覧（1件以上必須）
 * @param allowedIndustryCodes 希望・許可する出店業種コード一覧（例：I0101、未選択時は空）
 * @param facilityCodes 基本インフラ設備コード一覧（例：INUKI、未選択時は空）
 * @param images 保存済みの物件画像一覧（未添付時は空）
 */
public record PropertyInput(
        String propertyName,
        String prefecture,
        String municipality,
        String addressLine,
        BigDecimal latitude,
        BigDecimal longitude,
        String transportAccess,
        BigDecimal floorAreaSqm,
        int monthlyRentYen,
        Integer managementFeeYen,
        String depositKind,
        Integer depositMonths,
        String keyMoneyKind,
        Integer keyMoneyMonths,
        String securityDepositKind,
        Integer securityDepositMonths,
        Integer securityDepositYen,
        String floorCode,
        String totalFloorsCode,
        String buildingStructure,
        Integer builtYear,
        Integer builtMonth,
        String handoverTiming,
        String conditionMemo,
        String description,
        String listingUrl,
        String permitStatus,
        String listingStatus,
        List<PropertyContractTermInput> contractTerms,
        List<String> allowedIndustryCodes,
        List<String> facilityCodes,
        List<PropertyImageInput> images) {

    /**
     * 物件画像の保存結果だけを差し替えた新しい PropertyInput を生成する。
     *
     * @param savedImages 永続化後に確定した物件画像一覧
     * @return 画像一覧を差し替えた新しい物件登録データ
     */
    public PropertyInput withImages(List<PropertyImageInput> savedImages) {
        // 元のコレクション参照を共有しないよう不変リストへ複製する。
        List<PropertyImageInput> immutableImages = savedImages == null ? List.of() : List.copyOf(savedImages);
        return new PropertyInput(
                propertyName, prefecture, municipality, addressLine, latitude, longitude,
                transportAccess, floorAreaSqm, monthlyRentYen, managementFeeYen,
                depositKind, depositMonths, keyMoneyKind, keyMoneyMonths,
                securityDepositKind, securityDepositMonths, securityDepositYen,
                floorCode, totalFloorsCode, buildingStructure, builtYear, builtMonth,
                handoverTiming, conditionMemo, description, listingUrl,
                permitStatus, listingStatus, contractTerms, allowedIndustryCodes, facilityCodes,
                immutableImages);
    }
}
