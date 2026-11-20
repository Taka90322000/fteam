package jp.machiriku.logic;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 需要スコアの計算(テーブル定義書「設計補足」の式)。
 * 明細スコア = rank_weight / (1 + distance_km / distance_scale_km)
 * DB・画面・他クラスには依存しない。丸めは表示時に行う前提のため、ここでは丸めない。
 */
public final class DemandScoreCalculator {
    private static final BigDecimal WEIGHT_1 = new BigDecimal("1.00");
    private static final BigDecimal WEIGHT_2 = new BigDecimal("0.60");
    private static final BigDecimal WEIGHT_3 = new BigDecimal("0.30");

    private DemandScoreCalculator() { }

    /** 希望順位(1〜3)に対応する順位係数。vote_choices.rank_weight に保存する値。 */
    public static BigDecimal rankWeight(int preferenceRank) {
        return switch (preferenceRank) {
            case 1 -> WEIGHT_1;
            case 2 -> WEIGHT_2;
            case 3 -> WEIGHT_3;
            default -> throw new IllegalArgumentException("preferenceRank must be 1, 2 or 3.");
        };
    }

    /** 1件の投票明細のスコア。distanceKm は0以上、distanceScaleKm は0より大。 */
    public static double score(int preferenceRank, double distanceKm, double distanceScaleKm) {
        if (Double.isNaN(distanceKm) || distanceKm < 0) {
            throw new IllegalArgumentException("distanceKm must be 0 or greater.");
        }
        if (Double.isNaN(distanceScaleKm) || distanceScaleKm <= 0) {
            throw new IllegalArgumentException("distanceScaleKm must be greater than 0.");
        }
        return rankWeight(preferenceRank).doubleValue() / (1 + distanceKm / distanceScaleKm);
    }

    /**
     * 投票の希望順位の検証。第1希望が必須で、順位を飛ばさないこと(1、1-2、1-2-3 のみ可)。
     * 定義書: 「第1希望を必須とし、順位を飛ばさないことは送信時に検証する」
     *
     * @param selectedTypeIds 第1希望から順に並べた小分類ID(最大3件)。null要素・重複は不可。
     */
    public static void validateChoices(List<Integer> selectedTypeIds) {
        if (selectedTypeIds == null || selectedTypeIds.isEmpty()) {
            throw new IllegalArgumentException("The first choice is required.");
        }
        if (selectedTypeIds.size() > 3) {
            throw new IllegalArgumentException("At most 3 choices are allowed.");
        }
        Set<Integer> seen = new HashSet<>();
        for (Integer id : selectedTypeIds) {
            if (id == null) {
                throw new IllegalArgumentException("Choices must not skip a rank.");
            }
            if (!seen.add(id)) {
                throw new IllegalArgumentException("The same industry type cannot be chosen twice.");
            }
        }
    }
}
