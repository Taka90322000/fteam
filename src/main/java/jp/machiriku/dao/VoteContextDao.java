package jp.machiriku.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jp.machiriku.bean.VoteContext;
import jp.machiriku.logic.DistanceCalculator;
import jp.machiriku.logic.DistanceCalculator.Point;

/** 投票画面の初期表示に必要な情報(読み取りのみ)。投票できる条件は VoteDao と同じ。 */
public class VoteContextDao extends Dao {
    protected Connection openConnection() throws SQLException { return getConnection(); }

    /** 物件がない・削除済み・運営が非表示のときは空。 */
    public Optional<VoteContext> load(int userId, int propertyId) throws SQLException {
        try (Connection connection = openConnection()) {
            VoteContext.PropertyInfo property;
            BigDecimal latitude;
            BigDecimal longitude;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT property_name, prefecture, municipality, address_line, listing_status, latitude, longitude, distance_scale_km "
                    + "FROM properties WHERE property_id=? AND deleted_at IS NULL AND to_jsonb(properties)->>'admin_hidden_at' IS NULL")) {
                statement.setInt(1, propertyId);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) { return Optional.empty(); }
                    latitude = rows.getBigDecimal(6);
                    longitude = rows.getBigDecimal(7);
                    property = new VoteContext.PropertyInfo(propertyId, rows.getString(1), rows.getString(2), rows.getString(3),
                            rows.getString(4), rows.getString(5), latitude != null && longitude != null, rows.getBigDecimal(8));
                }
            }

            int voteCount;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM property_votes WHERE property_id=? AND withdrawn_at IS NULL")) {
                statement.setInt(1, propertyId);
                try (ResultSet rows = statement.executeQuery()) {
                    rows.next();
                    voteCount = rows.getInt(1);
                }
            }

            List<VoteContext.LocationOption> locations = new ArrayList<>();
            Integer nearestId = null;
            BigDecimal nearestKm = null;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT location_id, location_kind, prefecture, municipality, address_line, latitude, longitude FROM user_locations "
                    + "WHERE user_id=? ORDER BY CASE location_kind WHEN 'HOME' THEN 1 WHEN 'WORK' THEN 2 WHEN 'SCHOOL' THEN 3 ELSE 4 END, location_id")) {
                statement.setInt(1, userId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        BigDecimal lat = rows.getBigDecimal(6);
                        BigDecimal lng = rows.getBigDecimal(7);
                        boolean located = lat != null && lng != null;
                        BigDecimal km = null;
                        if (located && property.hasCoordinates()) {
                            km = DistanceCalculator.toDbScale(DistanceCalculator.distanceKm(
                                    new Point(latitude.doubleValue(), longitude.doubleValue()), new Point(lat.doubleValue(), lng.doubleValue())));
                            if (nearestKm == null || km.compareTo(nearestKm) < 0) { nearestKm = km; nearestId = rows.getInt(1); }
                        }
                        locations.add(new VoteContext.LocationOption(rows.getInt(1), rows.getString(2), rows.getString(3), rows.getString(4),
                                rows.getString(5), located, km, false));
                    }
                }
            }
            // 一番近い拠点に印を付ける(距離が同じなら先に出てきたもの。VoteDao の選び方と同じ)。
            for (int i = 0; i < locations.size(); i++) {
                VoteContext.LocationOption option = locations.get(i);
                if (nearestId != null && option.locationId() == nearestId) {
                    locations.set(i, new VoteContext.LocationOption(option.locationId(), option.kind(), option.prefecture(),
                            option.municipality(), option.addressLine(), option.hasCoordinates(), option.distanceKm(), true));
                }
            }

            String reason = null;
            String message = null;
            if (!"OPEN".equals(property.listingStatus())) {
                reason = VoteException.PROPERTY_CLOSED;
                message = "この物件は現在投票を受け付けていません。";
            } else if (!property.hasCoordinates()) {
                reason = VoteException.PROPERTY_NO_LOCATION;
                message = "この物件は位置情報が未登録のため投票できません。";
            } else if (locations.stream().noneMatch(VoteContext.LocationOption::hasCoordinates)) {
                reason = VoteException.NO_LOCATION;
                message = "生活拠点の位置情報が未登録のため投票できません。プロフィールで居住地を地図から選択してください。";
            }
            return Optional.of(new VoteContext(property, reason == null, reason, message, voteCount, locations, nearestId));
        }
    }
}
