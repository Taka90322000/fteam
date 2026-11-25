package jp.machiriku.property;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import jp.machiriku.bean.PropertyInput;
import jp.machiriku.property.PropertyForms.ParsedProperty;

/**
 * PropertyFormsの入力検証・コード変換・異常系拒否を検証するユニットテスト。
 */
public class PropertyFormsTest {

    /**
     * 全項目を入力した場合にDB制約へ適合する型・コードへ正しく変換されることを検証する。
     */
    @Test
    public void parsesCompleteValidPropertyForm() {
        Map<String, String> fields = baseValidFields();
        fields.put("lat", "36.5593124");
        fields.put("lng", "139.8821056");
        fields.put("managementFee", "15000");
        fields.put("deposit", "2ヶ月");
        fields.put("keyMoney", "1ヶ月");
        fields.put("securityDeposit", "50万円");
        fields.put("floorNumber", "1階");
        fields.put("totalFloors", "4階建");
        fields.put("structure", "RC(鉄筋コンクリート造)");
        fields.put("builtYearNum", "2008年");
        fields.put("builtMonthNum", "3月");
        fields.put("handover", "相談（即時可）");
        fields.put("popupEnabled", "on");
        fields.put("popupMinDays", "3");
        fields.put("popupMaxDays", "30");
        fields.put("popupDailyRent", "8000");
        fields.put("permitStatus", "OBTAINED");
        fields.put("condition", "元カフェ居抜き");
        fields.put("description", "オリオン通り沿いの好立地物件です。");
        fields.put("listingUrl", "https://example.com/properties/1");

        String galleryJson = "[{\"kind\":\"exterior\",\"label\":\"外観写真\",\"url\":\"data:image/jpeg;base64,AAAA\"}]";

        ParsedProperty parsed = PropertyForms.parseForCreate(
                fields,
                List.of("I0101", "I0107", "I0101"),
                List.of("hasInuki", "hasCityGas", "EXHAUST_DUCT"),
                galleryJson);

        PropertyInput prop = parsed.property();
        assertEquals("オリオン通りサウスビル 1階/101", prop.propertyName());
        assertEquals("栃木県", prop.prefecture());
        assertEquals("宇都宮市", prop.municipality());
        assertEquals("江野町5-12", prop.addressLine());
        assertEquals("東武宇都宮駅 徒歩3分", prop.transportAccess());
        assertEquals(new BigDecimal("36.559312"), prop.latitude());
        assertEquals(new BigDecimal("139.882106"), prop.longitude());
        assertEquals(new BigDecimal("52.80"), prop.floorAreaSqm());
        assertEquals(198000, prop.monthlyRentYen());
        assertEquals(Integer.valueOf(15000), prop.managementFeeYen());
        assertEquals("MONTHS", prop.depositKind());
        assertEquals(Integer.valueOf(2), prop.depositMonths());
        assertEquals("MONTHS", prop.keyMoneyKind());
        assertEquals(Integer.valueOf(1), prop.keyMoneyMonths());
        assertEquals("AMOUNT", prop.securityDepositKind());
        assertNull(prop.securityDepositMonths());
        assertEquals(Integer.valueOf(500000), prop.securityDepositYen());
        assertEquals("1F", prop.floorCode());
        assertEquals("4F", prop.totalFloorsCode());
        assertEquals("RC", prop.buildingStructure());
        assertEquals(Integer.valueOf(2008), prop.builtYear());
        assertEquals(Integer.valueOf(3), prop.builtMonth());
        assertEquals("CONSULT_IMMEDIATE", prop.handoverTiming());
        assertEquals("元カフェ居抜き", prop.conditionMemo());
        assertEquals("オリオン通り沿いの好立地物件です。", prop.description());
        assertEquals("https://example.com/properties/1", prop.listingUrl());
        assertEquals("OBTAINED", prop.permitStatus());
        assertEquals("OPEN", prop.listingStatus());
        assertEquals(2, prop.contractTerms().size());
        assertEquals("LONG", prop.contractTerms().get(0).contractKind());
        assertEquals("POPUP", prop.contractTerms().get(1).contractKind());
        assertEquals(Integer.valueOf(3), prop.contractTerms().get(1).minDays());
        assertEquals(Integer.valueOf(30), prop.contractTerms().get(1).maxDays());
        assertEquals(Integer.valueOf(8000), prop.contractTerms().get(1).dailyRentYen());
        assertEquals(List.of("I0101", "I0107"), prop.allowedIndustryCodes());
        assertEquals(List.of("INUKI", "CITY_GAS", "EXHAUST_DUCT"), prop.facilityCodes());
        assertEquals(1, parsed.photos().size());
        assertEquals("EXTERIOR", parsed.photos().get(0).imageKind());
        assertEquals("外観写真", parsed.photos().get(0).altText());
    }

    /**
     * 敷金・礼金・保証金や営業許可が未選択の場合に、仕様通り NONE / UNKNOWN へ正規化されることを検証する。
     */
    @Test
    public void normalizesOmittedOptionalFieldsToDefaults() {
        Map<String, String> fields = baseValidFields();
        fields.put("infra_hasParking", "hasParking");
        fields.put("structure", "S造(鉄骨造)");
        fields.put("handover", "要相談");
        fields.put("builtYear", "不明");

        ParsedProperty parsed = PropertyForms.parseForCreate(fields, List.of(), List.of(), null);
        PropertyInput prop = parsed.property();

        assertNull(prop.latitude());
        assertNull(prop.longitude());
        assertNull(prop.managementFeeYen());
        assertEquals("NONE", prop.depositKind());
        assertNull(prop.depositMonths());
        assertEquals("NONE", prop.keyMoneyKind());
        assertNull(prop.keyMoneyMonths());
        assertEquals("NONE", prop.securityDepositKind());
        assertNull(prop.securityDepositMonths());
        assertNull(prop.securityDepositYen());
        assertEquals("STEEL", prop.buildingStructure());
        assertEquals("CONSULT", prop.handoverTiming());
        assertNull(prop.builtYear());
        assertNull(prop.builtMonth());
        assertEquals("UNKNOWN", prop.permitStatus());
        assertEquals(List.of("PARKING"), prop.facilityCodes());
        assertTrue(parsed.photos().isEmpty());
    }

    /**
     * 不正な契約条件・築年月・座標・公開状態・URLが確実に拒否されることを検証する。
     */
    @Test
    public void rejectsInvalidCombinationsAndOutOfRangeValues() {
        // 契約形態が両方未選択
        Map<String, String> noContract = baseValidFields();
        noContract.remove("longEnabled");
        assertThrows(IllegalArgumentException.class,
                () -> PropertyForms.parseForCreate(noContract, List.of(), List.of(), null));

        // ポップアップの最大日数が最低日数未満
        Map<String, String> badPopup = baseValidFields();
        badPopup.put("popupEnabled", "on");
        badPopup.put("popupMinDays", "10");
        badPopup.put("popupMaxDays", "5");
        assertThrows(IllegalArgumentException.class,
                () -> PropertyForms.parseForCreate(badPopup, List.of(), List.of(), null));

        // 緯度のみ指定（経度なし）
        Map<String, String> halfCoords = baseValidFields();
        halfCoords.put("lat", "36.5610");
        assertThrows(IllegalArgumentException.class,
                () -> PropertyForms.parseForCreate(halfCoords, List.of(), List.of(), null));

        // 築年なしで築月のみ指定
        Map<String, String> monthOnly = baseValidFields();
        monthOnly.put("builtMonthNum", "5月");
        assertThrows(IllegalArgumentException.class,
                () -> PropertyForms.parseForCreate(monthOnly, List.of(), List.of(), null));

        // 新規登録で許可されない公開ステータス
        Map<String, String> badStatus = baseValidFields();
        badStatus.put("listingStatus", "CONTRACTED");
        assertThrows(IllegalArgumentException.class,
                () -> PropertyForms.parseForCreate(badStatus, List.of(), List.of(), null));

        // 危険なスキームのURL
        Map<String, String> badUrl = baseValidFields();
        badUrl.put("listingUrl", "javascript:alert(1)");
        assertThrows(IllegalArgumentException.class,
                () -> PropertyForms.parseForCreate(badUrl, List.of(), List.of(), null));

        // 未知の設備コード
        assertThrows(IllegalArgumentException.class,
                () -> PropertyForms.parseForCreate(baseValidFields(), List.of(), List.of("UNKNOWN_FAC"), null));
    }

    /**
     * 最小限の必須項目を備えた有効なフォームマップを生成する。
     *
     * @return 必須項目のみの有効マップ
     */
    private static Map<String, String> baseValidFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", "オリオン通りサウスビル 1階/101");
        fields.put("prefecture", "栃木県");
        fields.put("municipality", "宇都宮市");
        fields.put("addressLine", "江野町5-12");
        fields.put("access", "東武宇都宮駅 徒歩3分");
        fields.put("rent", "198000");
        fields.put("areaSqm", "52.8");
        fields.put("longEnabled", "on");
        fields.put("listingStatus", "OPEN");
        return fields;
    }
}
