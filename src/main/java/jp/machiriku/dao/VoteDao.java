package jp.machiriku.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jp.machiriku.bean.PropertyVote;
import jp.machiriku.bean.VoteChoice;
import jp.machiriku.logic.DemandScoreCalculator;
import jp.machiriku.logic.DistanceCalculator;
import jp.machiriku.logic.DistanceCalculator.Point;

/**
 * 地域住民の物件投票(property_votes / vote_choices)の保存・取得・取消。
 * 1住民1物件につき1件(UNIQUE(property_id, resident_user_id))で、取消した行は再投票時に再利用する。
 */
public class VoteDao extends Dao {
    public static final int COMMENT_MAX_LENGTH = 1000;
    private static final int FREQUENCY_MAX_LENGTH = 30; // property_votes.visit_frequency の VARCHAR(30)

    protected Connection openConnection() throws SQLException { return getConnection(); }

    /**
     * 投票を保存する(新規・再投票とも)。すべて1つのトランザクションで、途中で失敗した場合は全体を取り消す。
     *
     * @param industryKeys 第1〜第3希望の順に並べた業種(industry_code または industry_type_id)。未選択は null か空文字。
     */
    public PropertyVote saveVote(int userId, int propertyId, List<String> industryKeys,
            String visitFrequency, String requestComment) throws SQLException, VoteException {
        return saveVote(userId, propertyId, null, industryKeys, visitFrequency, requestComment);
    }

    /**
     * 距離の基準にする生活拠点を指定して投票を保存する。
     *
     * @param locationId 距離の基準にする生活拠点(user_locations.location_id)。null なら、座標のある拠点のうち物件に最も近いものを使う。
     *        指定する場合は本人の拠点で、座標が登録されている必要がある。
     */
    public PropertyVote saveVote(int userId, int propertyId, Integer locationId, List<String> industryKeys,
            String visitFrequency, String requestComment) throws SQLException, VoteException {
        String frequency = blankToNull(visitFrequency);
        String comment = blankToNull(requestComment);
        if (frequency != null && frequency.length() > FREQUENCY_MAX_LENGTH) {
            throw new VoteException(VoteException.INVALID_INPUT, "利用頻度が長すぎます。");
        }
        if (comment != null && comment.length() > COMMENT_MAX_LENGTH) {
            throw new VoteException(VoteException.INVALID_INPUT, "コメントは" + COMMENT_MAX_LENGTH + "文字以内で入力してください。");
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                requireActiveResident(connection, userId);
                BigDecimal[] property = loadOpenProperty(connection, propertyId); // [緯度, 経度, 距離の基準(km)]
                int[] nearestLocation = new int[1];
                BigDecimal distanceKm = chooseDistance(connection, userId, property, locationId, nearestLocation);
                List<Integer> typeIds = resolveChoices(connection, industryKeys);

                int voteId = upsertVote(connection, propertyId, userId, nearestLocation[0], distanceKm, property[2], frequency, comment);
                replaceChoices(connection, voteId, typeIds);
                connection.commit();
                return findVote(connection, userId, propertyId).orElseThrow(() -> new SQLException("The saved vote could not be read."));
            } catch (SQLException | VoteException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /** 自分の投票を取得する(取消済みも返し、isWithdrawn で判別する)。投票したことがなければ空。 */
    public Optional<PropertyVote> findVote(int userId, int propertyId) throws SQLException {
        try (Connection connection = openConnection()) {
            return findVote(connection, userId, propertyId);
        }
    }

    /** 投票を取り消す(withdrawn_at を入れる)。取り消す投票がなければ false。 */
    public boolean withdrawVote(int userId, int propertyId) throws SQLException {
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE property_votes SET withdrawn_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP "
                + "WHERE property_id=? AND resident_user_id=? AND withdrawn_at IS NULL")) {
            statement.setInt(1, propertyId);
            statement.setInt(2, userId);
            return statement.executeUpdate() > 0;
        }
    }

    // ---- 内部処理 -------------------------------------------------------------------------------

    private static void requireActiveResident(Connection connection, int userId) throws SQLException, VoteException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM users u JOIN resident_profiles r ON r.user_id=u.user_id "
                + "WHERE u.user_id=? AND u.user_role='RESIDENT' AND u.account_status='ACTIVE' AND u.withdrawn_at IS NULL")) {
            statement.setInt(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) { throw new VoteException(VoteException.NOT_RESIDENT, "地域住民として初期設定を完了した会員だけが投票できます。"); }
            }
        }
    }

    /** 投票できる物件(掲載中・削除なし・座標あり)の緯度・経度・距離の基準を返す。 */
    private static BigDecimal[] loadOpenProperty(Connection connection, int propertyId) throws SQLException, VoteException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT listing_status, latitude, longitude, distance_scale_km FROM properties "
                + "WHERE property_id=? AND deleted_at IS NULL AND to_jsonb(properties)->>'admin_hidden_at' IS NULL FOR SHARE")) {
            statement.setInt(1, propertyId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) { throw new VoteException(VoteException.PROPERTY_NOT_FOUND, "物件が見つかりません。"); }
                if (!"OPEN".equals(rows.getString(1))) { throw new VoteException(VoteException.PROPERTY_CLOSED, "この物件は現在投票を受け付けていません。"); }
                BigDecimal latitude = rows.getBigDecimal(2);
                BigDecimal longitude = rows.getBigDecimal(3);
                if (latitude == null || longitude == null) {
                    throw new VoteException(VoteException.PROPERTY_NO_LOCATION, "この物件は位置情報が未登録のため投票できません。");
                }
                return new BigDecimal[] {latitude, longitude, rows.getBigDecimal(4)};
            }
        }
    }

    /**
     * 距離の基準にする拠点を決めて、距離(km、小数第3位)を返す。locationId があればその拠点(本人のもので、座標があること)、
     * なければ座標のある拠点のうち最も近いもの。採用した拠点IDは outLocationId[0]。
     */
    private static BigDecimal chooseDistance(Connection connection, int userId, BigDecimal[] property, Integer locationId, int[] outLocationId)
            throws SQLException, VoteException {
        if (locationId == null) { return nearestDistance(connection, userId, property, outLocationId); }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT latitude, longitude FROM user_locations WHERE location_id=? AND user_id=?")) {
            statement.setInt(1, locationId);
            statement.setInt(2, userId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) { throw new VoteException(VoteException.BAD_LOCATION, "選択した生活拠点が見つかりません。"); }
                BigDecimal latitude = rows.getBigDecimal(1);
                BigDecimal longitude = rows.getBigDecimal(2);
                if (latitude == null || longitude == null) {
                    throw new VoteException(VoteException.NO_LOCATION, "選択した生活拠点は位置情報が未登録のため、基準にできません。");
                }
                outLocationId[0] = locationId;
                return DistanceCalculator.toDbScale(DistanceCalculator.distanceKm(
                        new Point(property[0].doubleValue(), property[1].doubleValue()),
                        new Point(latitude.doubleValue(), longitude.doubleValue())));
            }
        }
    }

    /** 座標のある生活拠点のうち物件に最も近いものを選び、その距離(km、小数第3位)を返す。拠点IDは outLocationId[0]。 */
    private static BigDecimal nearestDistance(Connection connection, int userId, BigDecimal[] property, int[] outLocationId)
            throws SQLException, VoteException {
        Point target = new Point(property[0].doubleValue(), property[1].doubleValue());
        double best = Double.MAX_VALUE;
        int bestId = -1;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT location_id, latitude, longitude FROM user_locations "
                + "WHERE user_id=? AND latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY location_id")) {
            statement.setInt(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    double km = DistanceCalculator.distanceKm(target,
                            new Point(rows.getBigDecimal(2).doubleValue(), rows.getBigDecimal(3).doubleValue()));
                    if (km < best) { best = km; bestId = rows.getInt(1); }
                }
            }
        }
        if (bestId < 0) {
            throw new VoteException(VoteException.NO_LOCATION, "生活拠点の位置情報が未登録のため投票できません。プロフィールで居住地を地図から選択してください。");
        }
        outLocationId[0] = bestId;
        return DistanceCalculator.toDbScale(best);
    }

    /** 画面から来た業種(コードまたはID)を有効な小分類IDへ変換し、順位のルールを検証する。 */
    private static List<Integer> resolveChoices(Connection connection, List<String> industryKeys) throws SQLException, VoteException {
        List<String> keys = new ArrayList<>();
        if (industryKeys != null) {
            for (String key : industryKeys) { keys.add(blankToNull(key)); }
        }
        while (!keys.isEmpty() && keys.get(keys.size() - 1) == null) { keys.remove(keys.size() - 1); } // 末尾の未選択は無視
        if (keys.size() > 3) { throw new VoteException(VoteException.BAD_CHOICES, "希望は第3希望までです。"); }

        List<Integer> ids = new ArrayList<>();
        for (String key : keys) {
            ids.add(key == null ? null : findIndustryTypeId(connection, key));
        }
        try {
            DemandScoreCalculator.validateChoices(ids);
        } catch (IllegalArgumentException e) {
            throw new VoteException(VoteException.BAD_CHOICES, switch (e.getMessage()) {
                case "The first choice is required." -> "第1希望を選択してください。";
                case "Choices must not skip a rank." -> "希望順位は飛ばさずに選択してください。";
                case "The same industry type cannot be chosen twice." -> "同じ業種を複数の順位に選ぶことはできません。";
                default -> "希望の選び方が正しくありません。";
            });
        }
        return ids;
    }

    private static Integer findIndustryTypeId(Connection connection, String key) throws SQLException, VoteException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT t.industry_type_id FROM industry_types t JOIN industry_categories c ON c.category_id=t.category_id "
                + "WHERE t.is_active AND c.is_active AND (t.industry_code=? OR t.industry_type_id::text=?)")) {
            statement.setString(1, key);
            statement.setString(2, key);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) { throw new VoteException(VoteException.BAD_CHOICES, "選択できない業種が含まれています。"); }
                return rows.getInt(1);
            }
        }
    }

    private static int upsertVote(Connection connection, int propertyId, int userId, int locationId, BigDecimal distanceKm,
            BigDecimal distanceScaleKm, String frequency, String comment) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO property_votes (property_id, resident_user_id, location_id, distance_km, distance_scale_km, "
                + "visit_frequency, request_comment) VALUES (?,?,?,?,?,?,?) "
                + "ON CONFLICT (property_id, resident_user_id) DO UPDATE SET location_id=EXCLUDED.location_id, "
                + "distance_km=EXCLUDED.distance_km, distance_scale_km=EXCLUDED.distance_scale_km, "
                + "visit_frequency=EXCLUDED.visit_frequency, request_comment=EXCLUDED.request_comment, "
                + "updated_at=CURRENT_TIMESTAMP, withdrawn_at=NULL RETURNING vote_id")) {
            statement.setInt(1, propertyId);
            statement.setInt(2, userId);
            statement.setInt(3, locationId);
            statement.setBigDecimal(4, distanceKm);
            statement.setBigDecimal(5, distanceScaleKm);
            statement.setString(6, frequency);
            statement.setString(7, comment);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    private static void replaceChoices(Connection connection, int voteId, List<Integer> typeIds) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM vote_choices WHERE vote_id=?")) {
            delete.setInt(1, voteId);
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO vote_choices (vote_id, preference_rank, industry_type_id, rank_weight) VALUES (?,?,?,?)")) {
            for (int i = 0; i < typeIds.size(); i++) {
                insert.setInt(1, voteId);
                insert.setInt(2, i + 1);
                insert.setInt(3, typeIds.get(i));
                insert.setBigDecimal(4, DemandScoreCalculator.rankWeight(i + 1));
                insert.executeUpdate();
            }
        }
    }

    private static Optional<PropertyVote> findVote(Connection connection, int userId, int propertyId) throws SQLException {
        PropertyVote vote;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT vote_id, property_id, resident_user_id, location_id, distance_km, distance_scale_km, visit_frequency, "
                + "request_comment, updated_at, withdrawn_at FROM property_votes WHERE property_id=? AND resident_user_id=?")) {
            statement.setInt(1, propertyId);
            statement.setInt(2, userId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) { return Optional.empty(); }
                vote = new PropertyVote();
                vote.setVoteId(rows.getInt(1));
                vote.setPropertyId(rows.getInt(2));
                vote.setResidentUserId(rows.getInt(3));
                vote.setLocationId(rows.getInt(4));
                vote.setDistanceKm(rows.getBigDecimal(5));
                vote.setDistanceScaleKm(rows.getBigDecimal(6));
                vote.setVisitFrequency(rows.getString(7));
                vote.setRequestComment(rows.getString(8));
                vote.setUpdatedAt(toOffset(rows.getTimestamp(9)));
                vote.setWithdrawnAt(toOffset(rows.getTimestamp(10)));
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT v.preference_rank, v.industry_type_id, t.industry_code, t.industry_name, v.rank_weight "
                + "FROM vote_choices v JOIN industry_types t ON t.industry_type_id=v.industry_type_id "
                + "WHERE v.vote_id=? ORDER BY v.preference_rank")) {
            statement.setInt(1, vote.getVoteId());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    VoteChoice choice = new VoteChoice();
                    choice.setPreferenceRank(rows.getInt(1));
                    choice.setIndustryTypeId(rows.getInt(2));
                    choice.setIndustryCode(rows.getString(3));
                    choice.setIndustryName(rows.getString(4));
                    choice.setRankWeight(rows.getBigDecimal(5));
                    vote.getChoices().add(choice);
                }
            }
        }
        return Optional.of(vote);
    }

    private static OffsetDateTime toOffset(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static String blankToNull(String value) {
        if (value == null) { return null; }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
