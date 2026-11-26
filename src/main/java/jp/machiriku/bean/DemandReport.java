package jp.machiriku.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 需要の集計結果(物件1件、またはエリア全体)。有効な(取消されていない)投票だけを集計する。
 * 集計テーブルは持たず、投票時に保存した距離と尺度から、そのつど計算する(定義書の設計補足)。
 *
 * @param scope PROPERTY(物件別)または AREA(エリア別)
 * @param propertyId 物件別のときだけ値が入る
 * @param propertyCount 集計の対象になった物件数(物件別は1)
 * @param voteCount 有効な投票の件数
 * @param voterCount 投票した住民の人数(重複なし)
 * @param averageDistanceKm 採用距離の平均(km)。投票がなければ null
 * @param totalScore 全業種のスコア合計
 */
public record DemandReport(
        String scope, Integer propertyId, String propertyName, String prefecture, String municipality,
        int propertyCount, int voteCount, int voterCount, BigDecimal averageDistanceKm, BigDecimal totalScore,
        List<IndustryDemand> industries, List<CategoryDemand> categories, List<FrequencyCount> frequencies,
        List<DemandComment> comments, List<TopProperty> topProperties) implements Serializable {

    public static final String SCOPE_PROPERTY = "PROPERTY";
    public static final String SCOPE_AREA = "AREA";

    /** 小分類(業種)ごとの需要。rank はスコアの高い順の順位。 */
    public record IndustryDemand(int rank, int industryTypeId, String industryCode, String industryName,
            BigDecimal score, BigDecimal sharePercent, int residentCount,
            int firstChoiceCount, int secondChoiceCount, int thirdChoiceCount) implements Serializable { }

    /** 大分類ごとの需要。希望人数は同じ住民を1人と数える(COUNT(DISTINCT 住民))。 */
    public record CategoryDemand(int rank, String categoryCode, String categoryName,
            BigDecimal score, BigDecimal sharePercent, int residentCount) implements Serializable { }

    /** 想定来店頻度の回答の内訳。 */
    public record FrequencyCount(String frequency, int count) implements Serializable { }

    /** 住民の要望コメント。投稿者が特定できる情報は含めない。 */
    public record DemandComment(String comment, String frequency, OffsetDateTime votedAt) implements Serializable { }

    /** 物件一覧に出す、物件ごとの投票数と需要の要約。 */
    public record PropertySummary(int propertyId, int voteCount, BigDecimal score,
            String topIndustryCode, String topIndustryName) implements Serializable { }

    /** エリア内で需要の高い物件。 */
    public record TopProperty(int rank, int propertyId, String propertyName, String municipality,
            int voteCount, BigDecimal score, String topIndustryName) implements Serializable { }
}
