package jp.machiriku.logic;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import jp.machiriku.logic.DistanceCalculator.Point;

/** 距離計算と需要スコア計算(定義書「設計補足」の式)のテスト。 */
public class CalculatorsTest {
    @Test
    public void sameAndKnownDistances() {
        Point utsunomiya = new Point(36.5551, 139.8828);
        assertEquals(0.0, DistanceCalculator.distanceKm(utsunomiya, utsunomiya), 1e-9);
        // 緯度1度の差はおよそ111.2km(地球の平均半径による)。
        assertEquals(111.195, DistanceCalculator.distanceKm(new Point(36, 139), new Point(37, 139)), 0.01);
        // 東京駅〜新宿駅はおよそ6.1km。
        assertEquals(6.1, DistanceCalculator.distanceKm(new Point(35.6812, 139.7671), new Point(35.6896, 139.7006)), 0.2);
    }

    @Test
    public void nearestPicksTheClosest() {
        Point target = new Point(36.55, 139.88);
        double nearest = DistanceCalculator.nearestKm(target, List.of(new Point(36.9, 139.9), new Point(36.551, 139.88)));
        assertEquals(DistanceCalculator.distanceKm(target, new Point(36.551, 139.88)), nearest, 1e-9);
    }

    @Test
    public void invalidPointsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Point(91, 0));
        assertThrows(IllegalArgumentException.class, () -> new Point(0, 181));
        assertThrows(IllegalArgumentException.class, () -> DistanceCalculator.nearestKm(new Point(0, 0), List.of()));
    }

    @Test
    public void dbScaleRoundsToThreeDecimals() {
        assertEquals(new BigDecimal("1.235"), DistanceCalculator.toDbScale(1.2345));
        assertEquals(new BigDecimal("0.000"), DistanceCalculator.toDbScale(0));
    }

    @Test
    public void rankWeightsAndScoreFormula() {
        assertEquals(new BigDecimal("1.00"), DemandScoreCalculator.rankWeight(1));
        assertEquals(new BigDecimal("0.60"), DemandScoreCalculator.rankWeight(2));
        assertEquals(new BigDecimal("0.30"), DemandScoreCalculator.rankWeight(3));
        assertThrows(IllegalArgumentException.class, () -> DemandScoreCalculator.rankWeight(4));
        // 距離0なら係数そのもの、距離=基準なら半分。
        assertEquals(1.0, DemandScoreCalculator.score(1, 0, 10), 1e-9);
        assertEquals(0.3, DemandScoreCalculator.score(2, 10, 10), 1e-9);
        assertEquals(0.6 / 1.5, DemandScoreCalculator.score(2, 5, 10), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> DemandScoreCalculator.score(1, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> DemandScoreCalculator.score(1, 1, 0));
    }

    @Test
    public void choiceRules() {
        DemandScoreCalculator.validateChoices(List.of(1));
        DemandScoreCalculator.validateChoices(List.of(1, 2));
        DemandScoreCalculator.validateChoices(List.of(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> DemandScoreCalculator.validateChoices(List.of()));
        assertThrows(IllegalArgumentException.class, () -> DemandScoreCalculator.validateChoices(null));
        assertThrows(IllegalArgumentException.class, () -> DemandScoreCalculator.validateChoices(Arrays.asList(1, null, 3)));
        assertThrows(IllegalArgumentException.class, () -> DemandScoreCalculator.validateChoices(Arrays.asList(null, 2)));
        assertThrows(IllegalArgumentException.class, () -> DemandScoreCalculator.validateChoices(List.of(1, 1)));
        assertThrows(IllegalArgumentException.class, () -> DemandScoreCalculator.validateChoices(List.of(1, 2, 3, 4)));
    }
}
