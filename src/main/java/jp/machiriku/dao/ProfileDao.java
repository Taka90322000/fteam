package jp.machiriku.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import jp.machiriku.bean.BusinessProfileInput;
import jp.machiriku.bean.LocationInput;
import jp.machiriku.bean.OwnerProfileInput;
import jp.machiriku.bean.RegistrationChoice;
import jp.machiriku.bean.ResidentProfileInput;
import jp.machiriku.bean.User;

/** 初期設定の関連テーブルを1トランザクションで保存する。途中失敗時に一部だけ登録されることを防ぐ。 */
public class ProfileDao extends Dao {
    private static final String USER_COLUMNS = "user_id, display_name, email, user_role, "
            + "line_subject, account_status, created_at, updated_at, withdrawn_at";

    protected Connection openConnection() throws SQLException { return getConnection(); }

    /** プロフィールに移した電話番号とアイコンの保存先を、再表示用に取得する。 */
    public Map<String, String> findProfileContact(int userId, String role) throws SQLException {
        String table = switch (role) {
            case "OWNER" -> "owner_profiles";
            case "BUSINESS" -> "business_profiles";
            default -> throw new IllegalArgumentException("プロフィールの利用区分が正しくありません。");
        };
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT phone_e164,icon_image_url FROM " + table + " WHERE user_id=?")) {
            statement.setInt(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) { return Map.of(); }
                String phone = rows.getString("phone_e164");
                String icon = rows.getString("icon_image_url");
                return Map.of("phone", phone == null ? "" : phone, "iconImageUrl", icon == null ? "" : icon);
            }
        }
    }

    /** 登録済み住民のプロフィール・居住地・職場・通知・希望業種を再表示用に取得する。 */
    public Map<String, String> findResidentFields(int userId) throws SQLException {
        Map<String, String> result = new LinkedHashMap<>();
        try (Connection connection = openConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT age_group, gender FROM resident_profiles WHERE user_id=?")) {
                statement.setInt(1, userId);
                try (ResultSet rows = statement.executeQuery()) {
                    if (rows.next()) {
                        String age = rows.getString("age_group");
                        String gender = rows.getString("gender");
                        if (age != null && !age.isBlank()) result.put("ageGroup", age);
                        if (gender != null && !gender.isBlank()) result.put("gender", gender);
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT location_kind, address_line, latitude, longitude FROM user_locations WHERE user_id=?")) {
                statement.setInt(1, userId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        String kind = rows.getString("location_kind");
                        String addr = rows.getString("address_line");
                        BigDecimal lat = rows.getBigDecimal("latitude");
                        BigDecimal lng = rows.getBigDecimal("longitude");
                        if ("LIVING".equals(kind)) {
                            if (addr != null) result.put("livingAddress", addr);
                            if (lat != null) result.put("livingLat", lat.toPlainString());
                            if (lng != null) result.put("livingLng", lng.toPlainString());
                        } else if ("WORK".equals(kind)) {
                            if (addr != null) result.put("workAddress", addr);
                            if (lat != null) result.put("workLat", lat.toPlainString());
                            if (lng != null) result.put("workLng", lng.toPlainString());
                        }
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT email_enabled FROM notification_settings WHERE user_id=?")) {
                statement.setInt(1, userId);
                try (ResultSet rows = statement.executeQuery()) {
                    if (rows.next()) {
                        result.put("emailNotify", rows.getBoolean("email_enabled") ? "on" : "");
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT i.industry_code FROM user_industry_preferences u JOIN industry_types i ON i.industry_type_id=u.industry_type_id WHERE u.user_id=?")) {
                statement.setInt(1, userId);
                try (ResultSet rows = statement.executeQuery()) {
                    List<String> codes = new ArrayList<>();
                    while (rows.next()) {
                        String code = rows.getString("industry_code");
                        if (code != null) codes.add(code);
                    }
                    if (!codes.isEmpty()) {
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < codes.size(); i++) {
                            if (i > 0) sb.append(",");
                            sb.append("\"").append(codes.get(i)).append("\"");
                        }
                        sb.append("]");
                        result.put("preferredIndustries", sb.toString());
                    }
                }
            }
        }
        return result;
    }

    /** 有効な業種だけを、DBのマスターID付きで画面へ渡す。 */
    public List<RegistrationChoice> listIndustries() throws SQLException {
        String sql = "SELECT i.industry_type_id, c.category_name || ' / ' || i.industry_name AS label "
                + "FROM industry_types i JOIN industry_categories c ON c.category_id=i.category_id "
                + "WHERE i.is_active AND c.is_active ORDER BY c.category_id,i.industry_type_id";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            List<RegistrationChoice> choices = new ArrayList<>();
            while (rows.next()) { choices.add(new RegistrationChoice(rows.getInt(1), rows.getString(2))); }
            return List.copyOf(choices);
        }
    }

    public List<RegistrationChoice> listQualifications() throws SQLException {
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT qualification_id,qualification_name FROM qualifications WHERE is_active ORDER BY qualification_id");
                ResultSet rows = statement.executeQuery()) {
            List<RegistrationChoice> choices = new ArrayList<>();
            while (rows.next()) { choices.add(new RegistrationChoice(rows.getInt(1), rows.getString(2))); }
            return List.copyOf(choices);
        }
    }

    public User saveResident(int userId, ResidentProfileInput input) throws SQLException {
        if (input == null || input.home() == null || !"HOME".equals(input.home().kind())
                || (input.work() != null && !"WORK".equals(input.work().kind()))) {
            throw new IllegalArgumentException("居住地と職場の入力内容を確認してください。");
        }
        return save(userId, "RESIDENT", (connection, user) -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET display_name=?,email=?,updated_at=CURRENT_TIMESTAMP WHERE user_id=?")) {
                statement.setString(1, input.displayName());
                statement.setString(2, nullable(input.email()));
                statement.setInt(3, userId);
                statement.executeUpdate();
            }
            // 職業はユーザー指定で保存対象外。既存のプロフィール列だけを使用する。
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO resident_profiles (user_id,age_group,gender) VALUES (?,?,?) "
                    + "ON CONFLICT (user_id) DO UPDATE SET age_group=EXCLUDED.age_group,gender=EXCLUDED.gender,updated_at=CURRENT_TIMESTAMP")) {
                statement.setInt(1, userId);
                statement.setString(2, input.ageGroup());
                statement.setString(3, input.gender());
                statement.executeUpdate();
            }
            saveLocation(connection, userId, input.home());
            if (input.work() != null) {
                saveLocation(connection, userId, input.work());
            } else {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM user_locations WHERE user_id=? AND location_kind='WORK'")) {
                    statement.setInt(1, userId);
                    statement.executeUpdate();
                }
            }
            // 住民が欲しい業種も、業種コードをDBの有効なIDへ変換して同じトランザクションで保存する。
            saveIndustryPreferences(connection, userId, resolveIndustryCodes(connection, input.preferredIndustryCodes()));
            saveNotifications(connection, userId, input.emailNotify() && nullable(input.email()) != null);
        });
    }

    /** オーナーの連絡先・プロフィール・誘致希望業種・通知設定をまとめて登録する。 */
    public User saveOwner(int userId, OwnerProfileInput input) throws SQLException {
        if (input == null || input.industryTypeIds() == null || input.industryTypeIds().isEmpty()) {
            throw new IllegalArgumentException("誘致希望業種を少なくとも1つ選択してください。");
        }
        return save(userId, "OWNER", (connection, user) -> {
            List<Integer> industries = uniqueIds(input.industryTypeIds());
            validateMasterIds(connection, industries, true);
            // LINE表示名は保持し、編集したオーナー氏名は専用プロフィールへ保存する。
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET email=?,updated_at=CURRENT_TIMESTAMP WHERE user_id=?")) {
                statement.setString(1, nullable(input.email()));
                statement.setInt(2, userId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO owner_profiles (user_id,representative_name,company_name,phone_e164,icon_image_url) VALUES (?,?,?,?,?) "
                    + "ON CONFLICT (user_id) DO UPDATE SET representative_name=EXCLUDED.representative_name,"
                    + "company_name=EXCLUDED.company_name,phone_e164=EXCLUDED.phone_e164,"
                    + "icon_image_url=EXCLUDED.icon_image_url,updated_at=CURRENT_TIMESTAMP")) {
                statement.setInt(1, userId);
                statement.setString(2, input.representativeName());
                statement.setString(3, nullable(input.company()));
                statement.setString(4, input.phoneE164());
                statement.setString(5, nullable(input.iconImageUrl()));
                statement.executeUpdate();
            }
            saveIndustryPreferences(connection, userId, industries);
            saveNotifications(connection, userId, input.emailNotify() && nullable(input.email()) != null);
        });
    }

    public User saveBusiness(int userId, BusinessProfileInput input) throws SQLException {
        if (input == null || input.industryTypeIds() == null || input.industryTypeIds().isEmpty()) {
            throw new IllegalArgumentException("希望業種を指定してください。");
        }
        return save(userId, "BUSINESS", (connection, user) -> {
            List<Integer> industries = uniqueIds(input.industryTypeIds());
            List<Integer> qualifications = uniqueIds(input.qualificationIds());
            validateMasterIds(connection, industries, true);
            validateMasterIds(connection, qualifications, false);
            String areaGeoJson = nullable(input.areaGeoJson());
            if (areaGeoJson != null) {
                // 自己交差など、数値の範囲だけでは検出できない不正なポリゴンもPostGISで確認する。
                validatePolygon(connection, areaGeoJson);
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET email=?,updated_at=CURRENT_TIMESTAMP WHERE user_id=?")) {
                statement.setString(1, nullable(input.email()));
                statement.setInt(2, userId);
                statement.executeUpdate();
            }
            // LINEの表示名は保持し、本人が入力した代表者名や電話番号は事業主プロフィールへ保存する。
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO business_profiles (user_id,business_name,representative_name,employee_count,preferred_area,industry_experience,management_experience,phone_e164,icon_image_url,area) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,ST_SetSRID(ST_GeomFromGeoJSON(?),4326)) "
                    + "ON CONFLICT (user_id) DO UPDATE SET business_name=EXCLUDED.business_name,representative_name=EXCLUDED.representative_name,"
                    + "employee_count=EXCLUDED.employee_count,preferred_area=EXCLUDED.preferred_area,industry_experience=EXCLUDED.industry_experience,"
                    + "management_experience=EXCLUDED.management_experience,phone_e164=EXCLUDED.phone_e164,"
                    + "icon_image_url=EXCLUDED.icon_image_url,area=EXCLUDED.area,updated_at=CURRENT_TIMESTAMP")) {
                statement.setInt(1, userId);
                statement.setString(2, input.businessName());
                statement.setString(3, input.representativeName());
                statement.setInt(4, input.employeeCount());
                statement.setString(5, input.preferredArea());
                statement.setString(6, input.industryExperience());
                statement.setString(7, input.managementExperience());
                statement.setString(8, input.phoneE164());
                statement.setString(9, nullable(input.iconImageUrl()));
                statement.setString(10, areaGeoJson);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM user_industry_preferences WHERE user_id=?")) {
                statement.setInt(1, userId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO user_industry_preferences (user_id,industry_type_id) VALUES (?,?)")) {
                for (int industryId : industries) {
                    statement.setInt(1, userId);
                    statement.setInt(2, industryId);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            // 選択解除した資格は非表示にし、資格番号や取得日など既存の詳細は消さない。
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE business_qualifications SET is_active=false,updated_at=CURRENT_TIMESTAMP WHERE business_user_id=?")) {
                statement.setInt(1, userId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO business_qualifications (business_user_id,qualification_id) VALUES (?,?) "
                    + "ON CONFLICT (business_user_id,qualification_id) DO UPDATE SET is_active=true,updated_at=CURRENT_TIMESTAMP")) {
                for (int qualificationId : qualifications) {
                    statement.setInt(1, userId);
                    statement.setInt(2, qualificationId);
                    statement.addBatch();
                }
                if (!qualifications.isEmpty()) { statement.executeBatch(); }
            }
            saveNotifications(connection, userId, input.emailNotify() && nullable(input.email()) != null);
        });
    }

    /** 同じ会員の同時送信を直列化し、権限をDBでも再確認してから関連情報を保存する。 */
    private User save(int userId, String requiredRole, SaveWork work) throws SQLException {
        try (Connection connection = openConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                User current;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT " + USER_COLUMNS + " FROM users WHERE user_id=? FOR UPDATE")) {
                    statement.setInt(1, userId);
                    try (ResultSet rows = statement.executeQuery()) { current = rows.next() ? UserDao.readUser(rows) : null; }
                }
                if (current == null || !requiredRole.equals(current.getUserRole()) || current.getWithdrawnAt() != null
                        || (!"PENDING".equals(current.getAccountStatus()) && !"ACTIVE".equals(current.getAccountStatus()))) {
                    throw new SQLException("This account cannot complete the requested registration.", "28000");
                }
                work.run(connection, current);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE users SET account_status='ACTIVE',updated_at=CURRENT_TIMESTAMP WHERE user_id=?")) {
                    statement.setInt(1, userId);
                    statement.executeUpdate();
                }
                User saved;
                try (PreparedStatement statement = connection.prepareStatement("SELECT " + USER_COLUMNS + " FROM users WHERE user_id=?")) {
                    statement.setInt(1, userId);
                    try (ResultSet rows = statement.executeQuery()) {
                        if (!rows.next()) { throw new SQLException("The saved user could not be read."); }
                        saved = UserDao.readUser(rows);
                    }
                }
                connection.commit();
                return saved;
            } catch (SQLException | RuntimeException error) {
                try { connection.rollback(); } catch (SQLException rollbackError) { error.addSuppressed(rollbackError); }
                throw error;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    private void saveLocation(Connection connection, int userId, LocationInput location) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO user_locations (user_id,location_kind,prefecture,municipality,address_line,latitude,longitude) VALUES (?,?,?,?,?,?,?) "
                + "ON CONFLICT (user_id,location_kind) DO UPDATE SET prefecture=EXCLUDED.prefecture,municipality=EXCLUDED.municipality,"
                + "address_line=EXCLUDED.address_line,latitude=EXCLUDED.latitude,longitude=EXCLUDED.longitude,updated_at=CURRENT_TIMESTAMP")) {
            statement.setInt(1, userId);
            statement.setString(2, location.kind());
            statement.setString(3, location.prefecture());
            statement.setString(4, location.municipality());
            statement.setString(5, location.addressLine());
            statement.setBigDecimal(6, location.latitude());
            statement.setBigDecimal(7, location.longitude());
            statement.executeUpdate();
        }
    }

    private void saveNotifications(Connection connection, int userId, boolean emailEnabled) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO notification_settings (user_id,email_enabled) VALUES (?,?) "
                + "ON CONFLICT (user_id) DO UPDATE SET email_enabled=EXCLUDED.email_enabled,updated_at=CURRENT_TIMESTAMP")) {
            statement.setInt(1, userId);
            statement.setBoolean(2, emailEnabled);
            statement.executeUpdate();
        }
    }

    /** I0101などの画面用コードを、DBで使う主キーへ対応付ける。未登録・無効なコードは保存しない。 */
    private List<Integer> resolveIndustryCodes(Connection connection, List<String> codes) throws SQLException {
        if (codes == null || codes.isEmpty()) { return List.of(); }
        if (codes.size() > 200) { throw new IllegalArgumentException("欲しい業種の選択数が多すぎます。"); }
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT i.industry_type_id FROM industry_types i JOIN industry_categories c ON c.category_id=i.category_id "
                + "WHERE i.industry_code=? AND i.is_active AND c.is_active")) {
            for (String code : codes) {
                if (code == null || !code.matches("I[0-9]{4}")) { throw new IllegalArgumentException("欲しい業種を選び直してください。"); }
                statement.setString(1, code);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) { throw new IllegalArgumentException("選択した業種が変更されました。欲しい業種を選び直してください。"); }
                    ids.add(rows.getInt(1));
                }
            }
        }
        return List.copyOf(ids);
    }

    private void saveIndustryPreferences(Connection connection, int userId, List<Integer> ids) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM user_industry_preferences WHERE user_id=?")) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
        if (ids.isEmpty()) { return; }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO user_industry_preferences (user_id,industry_type_id) VALUES (?,?)")) {
            for (int id : ids) {
                statement.setInt(1, userId);
                statement.setInt(2, id);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private List<Integer> uniqueIds(List<Integer> ids) {
        if (ids == null) { return List.of(); }
        if (ids.size() > 200 || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("業種・資格の選択内容を確認してください。");
        }
        return List.copyOf(new LinkedHashSet<>(ids));
    }

    private void validateMasterIds(Connection connection, List<Integer> ids, boolean industries) throws SQLException {
        String sql = industries
                ? "SELECT 1 FROM industry_types i JOIN industry_categories c ON c.category_id=i.category_id WHERE i.industry_type_id=? AND i.is_active AND c.is_active"
                : "SELECT 1 FROM qualifications WHERE qualification_id=? AND is_active";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int id : ids) {
                statement.setInt(1, id);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) { throw new IllegalArgumentException("業種・資格の選択肢が変更されました。選び直してください。"); }
                }
            }
        }
    }

    private void validatePolygon(Connection connection, String geoJson) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT ST_IsValid(g) AND NOT ST_IsEmpty(g) AND GeometryType(g)='POLYGON' FROM "
                + "(SELECT ST_SetSRID(ST_GeomFromGeoJSON(?),4326) AS g) AS shape")) {
            statement.setString(1, geoJson);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next() || !rows.getBoolean(1)) {
                    throw new IllegalArgumentException("希望範囲が交差しているか、正しく囲まれていません。地図で指定し直してください。");
                }
            }
        }
    }

    private static String nullable(String value) { return value == null || value.isBlank() ? null : value; }

    @FunctionalInterface
    private interface SaveWork { void run(Connection connection, User current) throws SQLException; }
}
