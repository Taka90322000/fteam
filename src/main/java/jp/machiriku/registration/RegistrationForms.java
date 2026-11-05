package jp.machiriku.registration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jp.machiriku.bean.BusinessProfileInput;
import jp.machiriku.bean.LocationInput;
import jp.machiriku.bean.ResidentProfileInput;

/** フォームの文字列を検証し、DBへ保存できるデータへ変換する。会員ID・利用区分は入力から受け取らない。 */
public final class RegistrationForms {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> AGES = Set.of("10代", "20代", "30代", "40代", "50代", "60代以上");
    private static final Set<String> GENDERS = Set.of("男性", "女性", "その他");
    private static final Set<String> EXPERIENCE = Set.of("未経験", "1〜3年未満", "3〜5年未満", "5年以上");
    private static final Pattern ADDRESS = Pattern.compile("^(東京都|北海道|(?:京都|大阪)府|.{2,3}県)(.+)$");
    private static final Pattern MUNICIPALITY = Pattern.compile("^(.+?市(?:.+?区)?|.+?区|.+?[町村])(.+)?$");

    private RegistrationForms() { }

    public static ResidentProfileInput resident(Map<String, String> fields) {
        String name = required(fields.get("name"), 100, "ニックネーム");
        String email = email(fields.get("email"));
        String age = optional(fields.get("ageGroup"), 16, "年齢");
        if ("未設定".equals(age)) { age = null; }
        if (age != null && !AGES.contains(age)) { throw invalid("年齢を選び直してください。"); }
        String gender = optional(fields.get("gender"), 16, "性別");
        if ("回答しない".equals(gender)) { gender = null; }
        if (gender != null && !GENDERS.contains(gender)) { throw invalid("性別を選び直してください。"); }
        // 業種・ご職業は、今回の対応ではユーザーの指定により保存対象から外す。
        String occupation = null;
        LocationInput home = location("HOME", fields.get("livingAddress"), fields.get("livingLat"), fields.get("livingLng"), true);
        LocationInput work = location("WORK", fields.get("workAddress"), fields.get("workLat"), fields.get("workLng"), false);
        return new ResidentProfileInput(name, email, age, gender, occupation, home, work, checked(fields.get("emailNotify")));
    }

    public static BusinessProfileInput business(Map<String, String> fields, List<String> industryIds, List<String> qualificationIds) {
        String name = required(fields.get("businessName"), 150, "事業者名・屋号");
        String representative = required(fields.get("representativeName"), 100, "代表者名");
        String email = email(fields.get("email"));
        String phone = normalizePhone(fields.get("phone"));
        String areaName = required(fields.get("preferredArea"), 150, "出店希望エリア名");
        int employeeCount;
        try {
            String count = required(fields.get("employeeCount"), 10, "従業員数");
            if (!count.matches("[0-9]+")) { throw new NumberFormatException(); }
            employeeCount = Integer.parseInt(count);
        } catch (NumberFormatException e) { throw invalid("従業員数は0以上の整数で入力してください。"); }
        String management = optional(fields.get("mgmtExperience"), 100, "店舗経営の経験");
        if (management != null && !Set.of("あり", "なし").contains(management)) { throw invalid("店舗経営の経験を選び直してください。"); }
        String experience = optional(fields.get("industryExperience"), 100, "業界での勤務経験");
        if (experience != null && !EXPERIENCE.contains(experience)) { throw invalid("業界での勤務経験を選び直してください。"); }
        List<Integer> industries = ids(industryIds, "希望業種");
        if (industries.isEmpty()) { throw invalid("希望業種を少なくとも1つ選択してください。"); }
        List<Integer> qualifications = ids(qualificationIds, "資格・許認可");
        String area = areaGeoJson(fields.get("preferredAreaPoints"));
        return new BusinessProfileInput(name, representative, email, phone, areaName, employeeCount,
                management, experience, industries, qualifications, area, checked(fields.get("emailNotify")));
    }

    /** 国内形式の電話番号を、users.phone_e164が使う国際形式へ統一する。 */
    public static String normalizePhone(String value) {
        String phone = required(value, 50, "連絡先電話番号").replaceAll("[\\s()（）ー−‐-]", "");
        if (phone.startsWith("0")) { phone = "+81" + phone.substring(1); }
        if (!phone.matches("\\+[1-9][0-9]{7,14}")) { throw invalid("電話番号を正しく入力してください（例：090-1234-5678）。"); }
        return phone;
    }

    /** 日本の住所を都道府県・市区町村・番地へ分ける。座標は入力がある場合のみ対で保存する。 */
    public static LocationInput location(String kind, String address, String latitude, String longitude, boolean required) {
        String text = optional(address, 300, "住所");
        if (text == null) {
            if (required) { throw invalid("居住地の住所を指定してください。"); }
            return null;
        }
        text = text.replaceFirst("^日本[、,]?\\s*", "").replaceFirst("^〒?\\d{3}[-ー]?\\d{4}\\s*", "").trim();
        Matcher pref = ADDRESS.matcher(text);
        if (!pref.matches()) { throw invalid("住所は都道府県から入力してください。"); }
        String prefecture = pref.group(1);
        Matcher city = MUNICIPALITY.matcher(pref.group(2).trim());
        if (!city.matches()) { throw invalid("住所の市区町村を確認してください。"); }
        String municipality = required(city.group(1), 50, "市区町村");
        String street = optional(city.group(2), 200, "番地・建物名");
        BigDecimal lat = coordinate(latitude, 90, "緯度");
        BigDecimal lng = coordinate(longitude, 180, "経度");
        if ((lat == null) != (lng == null)) { throw invalid("住所の緯度と経度をセットで指定してください。"); }
        if (required && lat == null) { throw invalid("地図上で居住地の位置を選択してください。"); }
        return new LocationInput(kind, prefecture, municipality, street, lat, lng);
    }

    /** {lat,lng}点列を検証し、閉じた[経度,緯度]のGeoJSON Polygonへ変換する。 */
    public static String areaGeoJson(String rawPoints) {
        if (rawPoints == null || rawPoints.isBlank()) { throw invalid("地図上をなぞって出店希望範囲を指定してください。"); }
        if (rawPoints.length() > 256000) { throw invalid("指定範囲の点が多すぎます。もう一度範囲を指定してください。"); }
        JsonNode points;
        try { points = JSON.readTree(rawPoints); }
        catch (JsonProcessingException e) { throw invalid("希望範囲の座標を読み取れません。地図で指定し直してください。"); }
        if (points == null || !points.isArray() || points.size() < 3 || points.size() > 1000) {
            throw invalid("希望範囲は3点以上、1000点以下で指定してください。");
        }
        List<List<BigDecimal>> ring = new ArrayList<>();
        for (JsonNode point : points) {
            if (!point.isObject() || !point.path("lat").isNumber() || !point.path("lng").isNumber()) {
                throw invalid("希望範囲の座標が正しくありません。");
            }
            BigDecimal lat = coordinate(point.get("lat").asText(), 90, "緯度");
            BigDecimal lng = coordinate(point.get("lng").asText(), 180, "経度");
            List<BigDecimal> pair = List.of(lng, lat);
            if (ring.isEmpty() || !ring.get(ring.size() - 1).equals(pair)) { ring.add(pair); }
        }
        if (ring.size() > 1 && ring.get(0).equals(ring.get(ring.size() - 1))) { ring.remove(ring.size() - 1); }
        if (new HashSet<>(ring).size() < 3) { throw invalid("希望範囲は異なる3点以上で囲んでください。"); }
        BigDecimal twiceArea = BigDecimal.ZERO;
        for (int i = 0; i < ring.size(); i++) {
            List<BigDecimal> a = ring.get(i), b = ring.get((i + 1) % ring.size());
            twiceArea = twiceArea.add(a.get(0).multiply(b.get(1)).subtract(b.get(0).multiply(a.get(1))));
        }
        if (twiceArea.signum() == 0) { throw invalid("線ではなく、面積のある範囲を囲んでください。"); }
        if (twiceArea.signum() < 0) { java.util.Collections.reverse(ring); }
        ring.add(ring.get(0));
        ObjectNode geometry = JSON.createObjectNode().put("type", "Polygon");
        ArrayNode coordinates = geometry.putArray("coordinates").addArray();
        for (List<BigDecimal> pair : ring) { coordinates.addArray().add(pair.get(0)).add(pair.get(1)); }
        return geometry.toString();
    }

    private static BigDecimal coordinate(String value, int maximum, String label) {
        if (value == null || value.isBlank()) { return null; }
        if (value.length() > 40) { throw invalid(label + "の値が正しくありません。"); }
        try {
            BigDecimal number = new BigDecimal(value.trim());
            if (number.abs().compareTo(BigDecimal.valueOf(maximum)) > 0) { throw new NumberFormatException(); }
            return number.setScale(6, RoundingMode.HALF_UP);
        } catch (NumberFormatException | ArithmeticException e) { throw invalid(label + "の値が正しくありません。"); }
    }

    private static List<Integer> ids(List<String> values, String label) {
        if (values == null) { return List.of(); }
        if (values.size() > 200) { throw invalid(label + "の選択数が多すぎます。"); }
        Set<Integer> result = new LinkedHashSet<>();
        for (String value : values) {
            try {
                if (value == null || !value.matches("[0-9]{1,10}")) { throw new NumberFormatException(); }
                int id = Integer.parseInt(value);
                if (id <= 0) { throw new NumberFormatException(); }
                result.add(id);
            } catch (NumberFormatException e) { throw invalid(label + "を選び直してください。"); }
        }
        return List.copyOf(result);
    }

    private static String email(String value) {
        String email = optional(value, 254, "メールアドレス");
        if (email != null && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) { throw invalid("メールアドレスを確認してください。"); }
        return email;
    }

    private static boolean checked(String value) { return "on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value); }

    private static String required(String value, int maximum, String label) {
        String result = optional(value, maximum, label);
        if (result == null) { throw invalid(label + "を入力してください。"); }
        return result;
    }

    private static String optional(String value, int maximum, String label) {
        if (value == null || value.isBlank()) { return null; }
        String result = value.trim();
        if (result.codePointCount(0, result.length()) > maximum) { throw invalid(label + "は" + maximum + "文字以内で入力してください。"); }
        return result;
    }

    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
}
