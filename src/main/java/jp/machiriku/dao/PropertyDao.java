package jp.machiriku.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jp.machiriku.bean.PropertyContractTermInput;
import jp.machiriku.bean.PropertyImageInput;
import jp.machiriku.bean.PropertyInput;

/**
 * 物件登録に関するDB操作を単一トランザクションで実行するDAO。
 * properties・property_contract_terms・property_industries・property_facilities・property_images を一括登録する。
 */
public class PropertyDao extends Dao {
    /** 物件基本情報を登録して採番された property_id を返すSQL。 */
    private static final String INSERT_PROPERTY_SQL =
            "INSERT INTO properties ("
            + "owner_user_id, property_name, prefecture, municipality, address_line, "
            + "latitude, longitude, transport_access, floor_area_sqm, monthly_rent_yen, "
            + "management_fee_yen, deposit_kind, deposit_months, key_money_kind, key_money_months, "
            + "security_deposit_kind, security_deposit_months, security_deposit_yen, "
            + "floor_code, total_floors_code, building_structure, built_year, built_month, "
            + "handover_timing, condition_memo, description, listing_url, permit_status, listing_status"
            + ") VALUES ("
            + "?, ?, ?, ?, ?, "
            + "?, ?, ?, ?, ?, "
            + "?, ?, ?, ?, ?, "
            + "?, ?, ?, "
            + "?, ?, ?, ?, ?, "
            + "?, ?, ?, ?, ?, ?"
            + ") RETURNING property_id";

    /**
     * DB接続を取得する。テスト時に一時テーブル接続へ差し替えられるようprotectedで公開する。
     *
     * @return PostgreSQL接続
     * @throws SQLException 接続の取得に失敗した場合
     */
    protected Connection openConnection() throws SQLException {
        return getConnection();
    }

    /**
     * 新規空き店舗と関連明細（契約条件・希望業種・基本インフラ設備・写真）を1トランザクションで登録する。
     *
     * @param ownerUserId 認証済みのオーナー会員ID
     * @param input 検証済みの物件登録データ
     * @return 自動採番された物件ID（property_id）
     * @throws SQLException オーナー権限不整合（SQLState=28000）またはDB操作に失敗した場合
     * @throws IllegalArgumentException 入力不足や無効なマスタコードが指定された場合
     */
    public int createProperty(int ownerUserId, PropertyInput input) throws SQLException {
        if (input == null || input.contractTerms() == null || input.contractTerms().isEmpty()) {
            throw new IllegalArgumentException("募集する契約形態を1つ以上選択してください。");
        }

        // 物件本体と子テーブル群を原子的に登録するため、手動トランザクションを開始する。
        try (Connection connection = openConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                // 1. オーナー会員の最新状態を行ロック付きで検証する。
                verifyActiveOwner(connection, ownerUserId);

                // 2. 選択された業種コード・設備コードをDBの有効マスタIDへ変換する。
                List<Integer> industryTypeIds = resolveIndustryCodes(connection, input.allowedIndustryCodes());
                List<Integer> facilityIds = resolveFacilityCodes(connection, input.facilityCodes());

                // 3. 物件本体（properties）を登録し、採番された物件IDを取得する。
                int propertyId = insertPropertyRow(connection, ownerUserId, input);

                // 4. 契約条件・希望業種・設備・物件画像の各子テーブルを登録する。
                insertContractTerms(connection, propertyId, input.contractTerms());
                insertPropertyIndustries(connection, propertyId, industryTypeIds);
                insertPropertyFacilities(connection, propertyId, facilityIds);
                insertPropertyImages(connection, propertyId, input.images());

                connection.commit();
                return propertyId;
            } catch (SQLException | RuntimeException error) {
                // 途中で失敗した場合はすべてのテーブル書き込みを取り消す。
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
                throw error;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    /**
     * 登録実行者が有効なオーナー会員（OWNER かつ ACTIVE かつ未退会）であることをDB上で再確認する。
     *
     * @param connection トランザクション中のDB接続
     * @param ownerUserId 検証対象の会員ID
     * @throws SQLException 会員が存在しない、オーナーでない、または利用停止・未本登録の場合（SQLState=28000）
     */
    private static void verifyActiveOwner(Connection connection, int ownerUserId) throws SQLException {
        // 退会や利用停止との競合を防ぐため会員行をロックして確認する。
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT user_role, account_status, withdrawn_at FROM users WHERE user_id=? FOR UPDATE")) {
            statement.setInt(1, ownerUserId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new SQLException("Owner account was not found.", "28000");
                }
                String role = rows.getString("user_role");
                String status = rows.getString("account_status");
                Object withdrawnAt = rows.getObject("withdrawn_at");
                if (!"OWNER".equals(role) || !"ACTIVE".equals(status) || withdrawnAt != null) {
                    throw new SQLException("Only active owner accounts can register properties.", "28000");
                }
            }
        }
    }

    /**
     * properties テーブルへ1件登録し、採番された property_id を返す。
     *
     * @param connection トランザクション中のDB接続
     * @param ownerUserId オーナー会員ID
     * @param input 検証済みの物件データ
     * @return 採番された物件ID
     * @throws SQLException INSERTまたは採番ID取得に失敗した場合
     */
    private static int insertPropertyRow(Connection connection, int ownerUserId, PropertyInput input) throws SQLException {
        // propertiesテーブルへ基本情報・賃料条件・建物スペック・公開状態を登録する。
        try (PreparedStatement statement = connection.prepareStatement(INSERT_PROPERTY_SQL)) {
            statement.setInt(1, ownerUserId);
            statement.setString(2, input.propertyName());
            statement.setString(3, input.prefecture());
            statement.setString(4, input.municipality());
            statement.setString(5, input.addressLine());
            statement.setBigDecimal(6, input.latitude());
            statement.setBigDecimal(7, input.longitude());
            statement.setString(8, input.transportAccess());
            statement.setBigDecimal(9, input.floorAreaSqm());
            statement.setInt(10, input.monthlyRentYen());
            setNullableInteger(statement, 11, input.managementFeeYen());
            statement.setString(12, input.depositKind());
            setNullableSmallInt(statement, 13, input.depositMonths());
            statement.setString(14, input.keyMoneyKind());
            setNullableSmallInt(statement, 15, input.keyMoneyMonths());
            statement.setString(16, input.securityDepositKind());
            setNullableSmallInt(statement, 17, input.securityDepositMonths());
            setNullableInteger(statement, 18, input.securityDepositYen());
            statement.setString(19, input.floorCode());
            statement.setString(20, input.totalFloorsCode());
            statement.setString(21, input.buildingStructure());
            setNullableSmallInt(statement, 22, input.builtYear());
            setNullableSmallInt(statement, 23, input.builtMonth());
            statement.setString(24, input.handoverTiming());
            statement.setString(25, input.conditionMemo());
            statement.setString(26, input.description());
            statement.setString(27, input.listingUrl());
            statement.setString(28, input.permitStatus());
            statement.setString(29, input.listingStatus());

            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new SQLException("Failed to obtain generated property_id.");
                }
                return rows.getInt(1);
            }
        }
    }

    /**
     * property_contract_terms テーブルへ契約形態ごとの条件を登録する。
     *
     * @param connection トランザクション中のDB接続
     * @param propertyId 登録対象の物件ID
     * @param terms 契約条件一覧
     * @throws SQLException INSERTに失敗した場合
     */
    private static void insertContractTerms(
            Connection connection,
            int propertyId,
            List<PropertyContractTermInput> terms) throws SQLException {
        // 選択された契約形態（LONG / POPUP）ごとに1行ずつバッチ登録する。
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO property_contract_terms (property_id, contract_kind, min_days, max_days, daily_rent_yen) "
                + "VALUES (?, ?, ?, ?, ?)")) {
            for (PropertyContractTermInput term : terms) {
                statement.setInt(1, propertyId);
                statement.setString(2, term.contractKind());
                setNullableInteger(statement, 3, term.minDays());
                setNullableInteger(statement, 4, term.maxDays());
                setNullableInteger(statement, 5, term.dailyRentYen());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /**
     * property_industries テーブルへ希望・許可業種を登録する。
     *
     * @param connection トランザクション中のDB接続
     * @param propertyId 登録対象の物件ID
     * @param industryTypeIds 解決済みの業種小分類ID一覧
     * @throws SQLException INSERTに失敗した場合
     */
    private static void insertPropertyIndustries(
            Connection connection,
            int propertyId,
            List<Integer> industryTypeIds) throws SQLException {
        if (industryTypeIds.isEmpty()) {
            return;
        }
        // 未選択時は全業種相談可として行を作成せず、選択がある場合のみ関連行を登録する。
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO property_industries (property_id, industry_type_id) VALUES (?, ?)")) {
            for (int industryTypeId : industryTypeIds) {
                statement.setInt(1, propertyId);
                statement.setInt(2, industryTypeId);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /**
     * property_facilities テーブルへ基本インフラ設備を登録する。
     *
     * @param connection トランザクション中のDB接続
     * @param propertyId 登録対象の物件ID
     * @param facilityIds 解決済みの設備ID一覧
     * @throws SQLException INSERTに失敗した場合
     */
    private static void insertPropertyFacilities(
            Connection connection,
            int propertyId,
            List<Integer> facilityIds) throws SQLException {
        if (facilityIds.isEmpty()) {
            return;
        }
        // 物件が備える有効な設備を中間テーブルへ一括登録する。
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO property_facilities (property_id, facility_id) VALUES (?, ?)")) {
            for (int facilityId : facilityIds) {
                statement.setInt(1, propertyId);
                statement.setInt(2, facilityId);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /**
     * property_images テーブルへ物件画像メタ情報を登録する。
     *
     * @param connection トランザクション中のDB接続
     * @param propertyId 登録対象の物件ID
     * @param images 保存済みの物件画像一覧
     * @throws SQLException INSERTに失敗した場合
     */
    private static void insertPropertyImages(
            Connection connection,
            int propertyId,
            List<PropertyImageInput> images) throws SQLException {
        if (images == null || images.isEmpty()) {
            return;
        }
        // 表示順・種別・説明文を添えて画像参照キーを一括登録する。
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO property_images (property_id, storage_key, display_order, alt_text, image_kind) "
                + "VALUES (?, ?, ?, ?, ?)")) {
            for (PropertyImageInput image : images) {
                statement.setInt(1, propertyId);
                statement.setString(2, image.storageKey());
                statement.setInt(3, image.displayOrder());
                statement.setString(4, image.altText());
                statement.setString(5, image.imageKind());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /**
     * 画面の業種小分類コード（I0101等）をDBの有効な industry_type_id へ解決する。
     *
     * @param connection トランザクション中のDB接続
     * @param codes 業種小分類コード一覧
     * @return 重複を除いた業種小分類ID一覧
     * @throws SQLException マスタ参照に失敗した場合
     * @throws IllegalArgumentException 未登録または無効化された業種コードが含まれる場合
     */
    private static List<Integer> resolveIndustryCodes(Connection connection, List<String> codes) throws SQLException {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        Set<Integer> ids = new LinkedHashSet<>();
        // 有効な大分類・小分類に属する業種コードのみを受け付ける。
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT i.industry_type_id FROM industry_types i "
                + "JOIN industry_categories c ON c.category_id = i.category_id "
                + "WHERE i.industry_code = ? AND i.is_active AND c.is_active")) {
            for (String code : codes) {
                statement.setString(1, code);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) {
                        throw new IllegalArgumentException("選択した業種が変更されました。希望・許可業種を選び直してください。");
                    }
                    ids.add(rows.getInt(1));
                }
            }
        }
        return List.copyOf(ids);
    }

    /**
     * 設備コード（INUKI等）をDBの有効な facility_id へ解決する。
     *
     * @param connection トランザクション中のDB接続
     * @param codes 設備コード一覧
     * @return 重複を除いた設備ID一覧
     * @throws SQLException マスタ参照に失敗した場合
     * @throws IllegalArgumentException 未登録または無効化された設備コードが含まれる場合
     */
    private static List<Integer> resolveFacilityCodes(Connection connection, List<String> codes) throws SQLException {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        Set<Integer> ids = new LinkedHashSet<>();
        // 有効な設備マスタに存在するコードのみを関連付ける。
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT facility_id FROM facilities WHERE facility_code = ? AND is_active")) {
            for (String code : codes) {
                statement.setString(1, code);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) {
                        throw new IllegalArgumentException("選択した設備項目が見つかりません。基本インフラ設備を選び直してください。");
                    }
                    ids.add(rows.getInt(1));
                }
            }
        }
        return List.copyOf(ids);
    }

    /**
     * NULL許容のINTEGER列へパラメーターを設定する。
     *
     * @param statement 対象のPreparedStatement
     * @param index パラメーター位置（1始まり）
     * @param value 設定値（null可）
     * @throws SQLException パラメーター設定に失敗した場合
     */
    private static void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    /**
     * NULL許容のSMALLINT列へパラメーターを設定する。
     *
     * @param statement 対象のPreparedStatement
     * @param index パラメーター位置（1始まり）
     * @param value 設定値（null可）
     * @throws SQLException パラメーター設定に失敗した場合
     */
    private static void setNullableSmallInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.SMALLINT);
        } else {
            statement.setShort(index, value.shortValue());
        }
    }
}
