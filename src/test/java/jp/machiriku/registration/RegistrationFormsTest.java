package jp.machiriku.registration;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 初期設定フォームの入力検証と、地図データの正規化をDB接続なしで確認する。 */
public class RegistrationFormsTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String TRIANGLE = "[{\"lat\":36.561234,\"lng\":139.883123},"
            + "{\"lat\":36.562,\"lng\":139.884},{\"lat\":36.560,\"lng\":139.885}]";

    @Test
    public void residentAcceptsMissingAndBlankOptionalEmail() {
        for (String email : new String[] { null, "", " \t" }) {
            Map<String, String> fields = residentFields();
            if (email == null) { fields.remove("email"); }
            else { fields.put("email", email); }
            assertNull(RegistrationForms.resident(fields).email());
        }
    }

    @Test
    public void residentRejectsMissingNameOverlongNameAndOverlongEmail() {
        for (String name : new String[] { "", " \t", "名".repeat(101) }) {
            Map<String, String> fields = residentFields();
            fields.put("name", name);
            rejects(() -> RegistrationForms.resident(fields));
        }
        Map<String, String> fields = residentFields();
        fields.put("email", "a".repeat(243) + "@example.com");
        rejects(() -> RegistrationForms.resident(fields));
        fields.put("name", "名".repeat(100));
        fields.put("email", "a".repeat(242) + "@example.com");
        assertNotNull(RegistrationForms.resident(fields));
    }

    @Test
    public void residentRejectsInvalidEmailAndIncompleteLivingCoordinates() {
        Map<String, String> malformedEmail = residentFields();
        malformedEmail.put("email", "not-an-email");
        rejects(() -> RegistrationForms.resident(malformedEmail));
        Map<String, String> missingLatitude = residentFields();
        missingLatitude.remove("livingLat");
        rejects(() -> RegistrationForms.resident(missingLatitude));
        Map<String, String> missingLongitude = residentFields();
        missingLongitude.remove("livingLng");
        rejects(() -> RegistrationForms.resident(missingLongitude));
    }

    @Test
    public void residentLeavesUnansweredDemographicsAndOccupationUnset() {
        var input = RegistrationForms.resident(residentFields());
        assertNull(input.ageGroup());
        assertNull(input.gender());
        assertNull(input.occupation());
        assertNull(input.work());
        assertTrue(input.emailNotify());
        Map<String, String> fields = residentFields();
        // 今回は保存対象外なので、表示上の職業や未知の値が送られても保存・検証に使わない。
        for (String occupation : new String[] { "IT・通信・オフィス", "未知の職業", "業".repeat(200) }) {
            fields.put("industry", occupation);
            assertNull(RegistrationForms.resident(fields).occupation());
        }
        fields.remove("emailNotify");
        assertFalse(RegistrationForms.resident(fields).emailNotify());
    }

    @Test
    // 欲しい業種は表示名と区別し、業種コードの順番を保って重複を除いた配列にする。
    public void residentDesiredIndustriesAreParsedAsUniqueDatabaseCodes() {
        Map<String, String> fields = residentFields();
        fields.put("preferredIndustries", "[\"I0101\",\"I0201\",\"I0101\"]");
        fields.put("industry", "表示用の名称は保存値に使わない");
        var input = RegistrationForms.resident(fields);
        assertEquals(List.of("I0101", "I0201"), input.preferredIndustryCodes());
        assertNull(input.occupation());
    }

    @Test
    public void residentCanLeaveDesiredIndustriesUnselected() {
        for (String value : new String[] { null, "", " \t", "[]" }) {
            Map<String, String> fields = residentFields();
            if (value == null) { fields.remove("preferredIndustries"); }
            else { fields.put("preferredIndustries", value); }
            assertEquals(List.of(), RegistrationForms.resident(fields).preferredIndustryCodes());
        }
    }

    @Test
    public void residentRejectsMalformedDesiredIndustryPayloads() {
        for (String value : new String[] { "not-json", "{}", "null", "\"I0101\"", "[1]", "[null]",
                "[\"I0101\",{}]", "[\"カフェ・喫茶店\"]", "[\"I01\"]", "[\"i0101\"]", "[\"I0101 OR 1=1\"]",
                "[\"I0101\"] []", "[\"I0101\"] trailing" }) {
            Map<String, String> fields = residentFields();
            fields.put("preferredIndustries", value);
            rejects(() -> RegistrationForms.resident(fields));
        }
        Map<String, String> tooMany = residentFields();
        tooMany.put("preferredIndustries", "[" + String.join(",", java.util.Collections.nCopies(201, "\"I0101\"")) + "]");
        rejects(() -> RegistrationForms.resident(tooMany));
    }

    @Test
    // 保存対象追加前の呼び出しでも、欲しい業種が未選択として扱われることを確認する。
    public void legacyResidentInputConstructorDefaultsDesiredIndustryCodesToEmpty() {
        var input = new jp.machiriku.bean.ResidentProfileInput("名前", null, null, null, null, null, null, false);
        assertEquals(List.of(), input.preferredIndustryCodes());
    }

    @Test
    public void ownerAcceptsOptionalEmailCompanyAndNormalizesPhone() {
        for (String email : new String[] { null, "", " \t" }) {
            Map<String, String> fields = ownerFields();
            if (email == null) { fields.remove("email"); }
            else { fields.put("email", email); }
            fields.put("company", " ");
            var input = RegistrationForms.owner(fields, List.of("2", "1", "2"));
            assertEquals("テストオーナー", input.representativeName());
            assertNull(input.company());
            assertNull(input.email());
            assertEquals("+81286339988", input.phoneE164());
            assertEquals(List.of(2, 1), input.industryTypeIds());
            assertTrue(input.emailNotify());
        }
    }

    @Test
    public void ownerRejectsMissingNameOverlongCompanyAndMalformedContact() {
        for (String name : new String[] { "", " \t", "名".repeat(101) }) {
            Map<String, String> fields = ownerFields();
            fields.put("representativeName", name);
            rejects(() -> RegistrationForms.owner(fields, List.of("1")));
        }
        for (Map.Entry<String, String> invalid : Map.of("company", "社".repeat(151), "email", "not-an-email", "phone", "invalid").entrySet()) {
            Map<String, String> fields = ownerFields();
            fields.put(invalid.getKey(), invalid.getValue());
            rejects(() -> RegistrationForms.owner(fields, List.of("1")));
        }
        Map<String, String> fields = ownerFields();
        fields.put("representativeName", "名".repeat(100));
        fields.put("company", "社".repeat(150));
        fields.remove("emailNotify");
        assertFalse(RegistrationForms.owner(fields, List.of("1")).emailNotify());
    }

    @Test
    public void ownerRequiresValidAttractionIndustryIds() {
        for (List<String> identifiers : List.of(List.<String>of(), List.of("0"), List.of("-1"), List.of("飲食"), List.of("1 OR 1=1"))) {
            rejects(() -> RegistrationForms.owner(ownerFields(), identifiers));
        }
    }

    @Test
    public void ownerAndBusinessAcceptHttpUrlsAndWebImagePaths() {
        for (String image : List.of("https://example.com/icons/owner.png", "http://example.com/icon.jpg?size=100",
                "/assets/icon.png", "assets/icon.png", "./assets/画像.png")) {
            Map<String, String> owner = ownerFields();
            owner.put("iconImageUrl", " " + image + " ");
            assertEquals(image, RegistrationForms.owner(owner, List.of("1")).iconImageUrl());
            Map<String, String> business = businessFields();
            business.put("iconImageUrl", image);
            assertEquals(image, RegistrationForms.business(business, List.of("1"), List.of()).iconImageUrl());
        }
    }

    @Test
    public void ownerAndBusinessLeaveMissingOrBlankIconsUnset() {
        for (String image : new String[] { null, "", " " }) {
            Map<String, String> owner = ownerFields();
            owner.put("iconImageUrl", image);
            assertNull(RegistrationForms.owner(owner, List.of("1")).iconImageUrl());
            Map<String, String> business = businessFields();
            business.put("iconImageUrl", image);
            assertNull(RegistrationForms.business(business, List.of("1"), List.of()).iconImageUrl());
        }
    }

    @Test
    public void ownerAndBusinessRejectUnsafeOrOverlongImageReferences() {
        for (String image : List.of("javascript:alert(1)", "data:image/png;base64,AAAA", "file:///C:/icon.png",
                "https:", "http:/icon.png", "//example.com/icon.png", "../icon.png", "/assets/../icon.png",
                "/assets/%2e%2e/icon.png", "C:\\icons\\icon.png", "icons\\icon.png", "/%5c/example.com/icon.png",
                "https://user:password@example.com/icon.png", "icons/icon\n.png", "/icons/%0aimage.png",
                "https://example.com/icon\t.png", "https://example.com/" + "a".repeat(2048))) {
            Map<String, String> owner = ownerFields();
            owner.put("iconImageUrl", image);
            rejects(() -> RegistrationForms.owner(owner, List.of("1")));
            Map<String, String> business = businessFields();
            business.put("iconImageUrl", image);
            rejects(() -> RegistrationForms.business(business, List.of("1"), List.of()));
        }
    }

    @Test
    public void legacyOwnerAndBusinessConstructorsLeaveIconUnset() {
        assertNull(new jp.machiriku.bean.OwnerProfileInput("氏名", null, null, "+819012345678", List.of(1), false).iconImageUrl());
        assertNull(new jp.machiriku.bean.BusinessProfileInput("店舗", "氏名", null, "+819012345678", "エリア", 1,
                null, null, List.of(1), List.of(), "{}", false).iconImageUrl());
    }

    @Test
    public void locationStripsGoogleAddressPrefixAndSplitsJapaneseAdministrativeAreas() {
        for (String[] address : new String[][] {
                { "日本、〒320-0033 栃木県宇都宮市本町1-1", "栃木県", "宇都宮市", "本町1-1" },
                { "東京都新宿区西新宿1-1", "東京都", "新宿区", "西新宿1-1" },
                { "大阪府大阪市北区梅田1-1", "大阪府", "大阪市北区", "梅田1-1" } }) {
            var location = RegistrationForms.location("HOME", address[0], "36.561123", "139.883123", true);
            assertEquals("HOME", location.kind());
            assertEquals(address[1], location.prefecture());
            assertEquals(address[2], location.municipality());
            assertEquals(address[3], location.addressLine());
            assertEquals(0, new java.math.BigDecimal("36.561123").compareTo(location.latitude()));
            assertEquals(0, new java.math.BigDecimal("139.883123").compareTo(location.longitude()));
        }
    }

    @Test
    public void businessAcceptsOptionalEmailAndNormalizesDomesticPhone() {
        for (String email : new String[] { null, "", " \t" }) {
            Map<String, String> fields = businessFields();
            if (email == null) { fields.remove("email"); }
            else { fields.put("email", email); }
            fields.put("phone", "090-1234-5678");
            var input = RegistrationForms.business(fields, List.of("1"), List.of());
            assertNull(input.email());
            assertEquals("+819012345678", input.phoneE164());
            assertEquals(List.of(1), input.industryTypeIds());
            assertEquals(List.of(), input.qualificationIds());
            assertTrue(input.emailNotify());
        }
        assertEquals("+819012345678", RegistrationForms.normalizePhone("090-1234-5678"));
        assertEquals("+819012345678", RegistrationForms.normalizePhone(" +81 90 1234 5678 "));
    }

    @Test
    public void businessRejectsMissingOrInvalidIndustryAndQualificationIdentifiers() {
        for (List<String> identifiers : List.of(List.<String>of(), List.of("0"), List.of("-1"), List.of("NaN"), List.of("1 OR 1=1"))) {
            rejects(() -> RegistrationForms.business(businessFields(), identifiers, List.of()));
        }
        rejects(() -> RegistrationForms.business(businessFields(), List.of("1"), List.of("0")));
        rejects(() -> RegistrationForms.business(businessFields(), List.of("1"), List.of("qualification-name")));
    }

    @Test
    public void repeatedBusinessMasterIdentifiersAreDeduplicated() {
        var input = RegistrationForms.business(businessFields(), List.of("2", "1", "2"), List.of("3", "3"));
        assertEquals(2, input.industryTypeIds().size());
        assertEquals(java.util.Set.of(1, 2), new java.util.HashSet<>(input.industryTypeIds()));
        assertEquals(List.of(3), input.qualificationIds());
    }

    @Test
    public void businessRejectsNegativeFractionalAndUnboundedEmployeeCount() {
        for (String employees : new String[] { "-1", "1.5", "not-a-number", "999999999999999999999999" }) {
            Map<String, String> fields = businessFields();
            fields.put("employeeCount", employees);
            rejects(() -> RegistrationForms.business(fields, List.of("1"), List.of()));
        }
    }

    @Test
    public void polygonUsesLongitudeLatitudeAndClosesWithoutChangingCoordinates() throws Exception {
        JsonNode polygon = JSON.readTree(RegistrationForms.areaGeoJson(TRIANGLE));
        assertEquals("Polygon", polygon.path("type").asText());
        JsonNode ring = polygon.path("coordinates").get(0);
        assertEquals(4, ring.size());
        assertCoordinatePresent(ring, 139.883123, 36.561234);
        assertCoordinatePresent(ring, 139.884, 36.562);
        assertCoordinatePresent(ring, 139.885, 36.560);
        assertEquals(ring.get(0), ring.get(3));
    }

    @Test
    public void alreadyClosedPolygonDoesNotReceiveAnotherClosingPoint() throws Exception {
        String closed = TRIANGLE.substring(0, TRIANGLE.length() - 1)
                + ",{\"lat\":36.561234,\"lng\":139.883123}]";
        JsonNode ring = JSON.readTree(RegistrationForms.areaGeoJson(closed)).path("coordinates").get(0);
        assertEquals(4, ring.size());
        assertEquals(ring.get(0), ring.get(3));
    }

    @Test
    public void rejectsMalformedFeaturesNonFiniteCoordinatesAndInsufficientDistinctPoints() {
        for (String points : new String[] { null, "", "not-json", "[]", "{}", "{\"type\":\"Feature\",\"geometry\":{}}",
                "[{\"lat\":36,\"lng\":139},{\"lat\":36,\"lng\":139},{\"lat\":37,\"lng\":140}]",
                "[{\"lat\":91,\"lng\":139},{\"lat\":36,\"lng\":140},{\"lat\":37,\"lng\":139}]",
                "[{\"lat\":36,\"lng\":181},{\"lat\":36,\"lng\":140},{\"lat\":37,\"lng\":139}]",
                "[{\"lat\":1e999,\"lng\":139},{\"lat\":36,\"lng\":140},{\"lat\":37,\"lng\":139}]",
                "[{\"lat\":\"36\",\"lng\":139},{\"lat\":36,\"lng\":140},{\"lat\":37,\"lng\":139}]" }) {
            rejects(() -> RegistrationForms.areaGeoJson(points));
        }
    }

    @Test
    public void rejectsMoreThanOneThousandPolygonPoints() {
        StringBuilder points = new StringBuilder("[");
        for (int i = 0; i < 1001; i++) {
            if (i > 0) { points.append(','); }
            double angle = 2 * Math.PI * i / 1001;
            points.append("{\"lat\":").append(36 + Math.cos(angle) * 0.01)
                    .append(",\"lng\":").append(139 + Math.sin(angle) * 0.01).append('}');
        }
        points.append(']');
        rejects(() -> RegistrationForms.areaGeoJson(points.toString()));
    }

    @Test
    public void rejectsInvalidOrIncompleteLocationCoordinates() {
        for (String[] pair : new String[][] { { "", "139" }, { "36", "" }, { "91", "139" },
                { "36", "181" }, { "-91", "139" }, { "36", "-181" }, { "NaN", "139" },
                { "Infinity", "139" }, { "36", "not-a-number" } }) {
            rejects(() -> RegistrationForms.location("HOME", "栃木県宇都宮市本町1-1", pair[0], pair[1], true));
        }
    }

    @Test
    public void optionalWorkWithNoAddressAndCoordinatesIsIgnored() {
        assertNull(RegistrationForms.location("WORK", "", "", "", false));
        assertNull(RegistrationForms.location("WORK", null, null, null, false));
        assertNull(RegistrationForms.location("WORK", "", "36.561", "139.883", false));
        var work = RegistrationForms.location("WORK", "栃木県宇都宮市本町1-1", "", "", false);
        assertNotNull(work);
        assertNull(work.latitude());
        assertNull(work.longitude());
    }

    @Test
    public void requiredHomeNeedsBothCoordinatesEvenWhenAddressIsPresent() {
        rejects(() -> RegistrationForms.location("HOME", "栃木県宇都宮市本町1-1", "", "", true));
        Map<String, String> fields = residentFields();
        fields.remove("livingLat");
        fields.remove("livingLng");
        rejects(() -> RegistrationForms.resident(fields));
    }

    private static void rejects(Runnable action) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, action::run);
        assertNotNull(error.getMessage());
        assertTrue("入力エラーは利用者が読める日本語で通知する", error.getMessage().matches("(?s).*[ぁ-んァ-ヶ一-龯].*"));
    }

    private static void assertCoordinatePresent(JsonNode ring, double longitude, double latitude) {
        for (JsonNode point : ring) {
            if (Math.abs(point.get(0).doubleValue() - longitude) < 0.000000000001
                    && Math.abs(point.get(1).doubleValue() - latitude) < 0.000000000001) { return; }
        }
        fail("入力した地図上の頂点がGeoJSONに保持されていません。");
    }

    private static Map<String, String> residentFields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("name", "テスト住民");
        fields.put("email", "resident@example.com");
        fields.put("ageGroup", "未設定");
        fields.put("gender", "回答しない");
        fields.put("industry", "");
        fields.put("livingAddress", "栃木県宇都宮市本町1-1");
        fields.put("livingLat", "36.561");
        fields.put("livingLng", "139.883");
        fields.put("emailNotify", "on");
        return fields;
    }

    private static Map<String, String> businessFields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("businessName", "テスト事業所");
        fields.put("representativeName", "テスト代表者");
        fields.put("email", "business@example.com");
        fields.put("phone", "+819012345678");
        fields.put("preferredArea", "宇都宮市本町周辺");
        fields.put("preferredAreaPoints", TRIANGLE);
        fields.put("employeeCount", "1");
        fields.put("mgmtExperience", "なし");
        fields.put("industryExperience", "未経験");
        fields.put("emailNotify", "true");
        return fields;
    }

    private static Map<String, String> ownerFields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("representativeName", "テストオーナー");
        fields.put("company", "テスト不動産");
        fields.put("email", "owner@example.invalid");
        fields.put("phone", "028-633-9988");
        fields.put("emailNotify", "on");
        return fields;
    }
}
