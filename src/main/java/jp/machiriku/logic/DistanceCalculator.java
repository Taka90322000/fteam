package jp.machiriku.logic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * 緯度経度から2点間の距離(km)を求める。DB・画面・他クラスには依存しない。
 * 球面上の最短距離(ハーバーサイン式)。直線距離であり、道のりではない。
 */
public final class DistanceCalculator {
    /** 地球の平均半径(km)。 */
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private DistanceCalculator() { }

    /** 緯度経度の1点。範囲外の値は作成時に拒否する(properties / user_locations のCHECKと同じ範囲)。 */
    public record Point(double latitude, double longitude) {
        public Point {
            if (Double.isNaN(latitude) || latitude < -90 || latitude > 90) {
                throw new IllegalArgumentException("latitude must be between -90 and 90.");
            }
            if (Double.isNaN(longitude) || longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("longitude must be between -180 and 180.");
            }
        }
    }

    /** 2点間の距離(km)。 */
    public static double distanceKm(Point a, Point b) {
        if (a == null || b == null) { throw new IllegalArgumentException("Both points are required."); }
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        // 丸め誤差で h が1をわずかに超えても NaN にならないようにする。
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }

    /** target から candidates のうち最も近い点までの距離(km)。投票の「最短拠点からの距離」に使う。 */
    public static double nearestKm(Point target, Collection<Point> candidates) {
        if (target == null || candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("A target and at least one candidate are required.");
        }
        double nearest = Double.MAX_VALUE;
        for (Point candidate : candidates) {
            nearest = Math.min(nearest, distanceKm(target, candidate));
        }
        return nearest;
    }

    /** DBの NUMERIC(10,3) に保存するための丸め(小数第3位、四捨五入)。 */
    public static BigDecimal toDbScale(double km) {
        return BigDecimal.valueOf(km).setScale(3, RoundingMode.HALF_UP);
    }
}
