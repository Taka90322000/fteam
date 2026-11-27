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
    // JSPの ${...}(EL)で読むための getXxx()。record の xxx() は ${a.xxx} では読めない。
    public String getScope() { return scope; }
    public Integer getPropertyId() { return propertyId; }
    public String getPropertyName() { return propertyName; }
    public String getPrefecture() { return prefecture; }
    public String getMunicipality() { return municipality; }
    public int getPropertyCount() { return propertyCount; }
    public int getVoteCount() { return voteCount; }
    public int getVoterCount() { return voterCount; }
    public BigDecimal getAverageDistanceKm() { return averageDistanceKm; }
    public BigDecimal getTotalScore() { return totalScore; }
    public List<IndustryDemand> getIndustries() { return industries; }
    public List<CategoryDemand> getCategories() { return categories; }
    public List<FrequencyCount> getFrequencies() { return frequencies; }
    public List<DemandComment> getComments() { return comments; }
    public List<TopProperty> getTopProperties() { return topProperties; }


    public static final String SCOPE_PROPERTY = "PROPERTY";
    public static final String SCOPE_AREA = "AREA";

    /** 小分類(業種)ごとの需要。rank はスコアの高い順の順位。 */
    public record IndustryDemand(int rank, int industryTypeId, String industryCode, String industryName,
            BigDecimal score, BigDecimal sharePercent, int residentCount,
            int firstChoiceCount, int secondChoiceCount, int thirdChoiceCount) implements Serializable {
        // JSPの ${...}(EL)で読むための getXxx()。record の xxx() は ${a.xxx} では読めない。
        public int getRank() { return rank; }
        public int getIndustryTypeId() { return industryTypeId; }
        public String getIndustryCode() { return industryCode; }
        public String getIndustryName() { return industryName; }
        public BigDecimal getScore() { return score; }
        public BigDecimal getSharePercent() { return sharePercent; }
        public int getResidentCount() { return residentCount; }
        public int getFirstChoiceCount() { return firstChoiceCount; }
        public int getSecondChoiceCount() { return secondChoiceCount; }
        public int getThirdChoiceCount() { return thirdChoiceCount; }
    }

    /** 大分類ごとの需要。希望人数は同じ住民を1人と数える(COUNT(DISTINCT 住民))。 */
    public record CategoryDemand(int rank, String categoryCode, String categoryName,
            BigDecimal score, BigDecimal sharePercent, int residentCount) implements Serializable {
        // JSPの ${...}(EL)で読むための getXxx()。record の xxx() は ${a.xxx} では読めない。
        public int getRank() { return rank; }
        public String getCategoryCode() { return categoryCode; }
        public String getCategoryName() { return categoryName; }
        public BigDecimal getScore() { return score; }
        public BigDecimal getSharePercent() { return sharePercent; }
        public int getResidentCount() { return residentCount; }
    }

    /** 想定来店頻度の回答の内訳。 */
    public record FrequencyCount(String frequency, int count) implements Serializable {
        // JSPの ${...}(EL)で読むための getXxx()。record の xxx() は ${a.xxx} では読めない。
        public String getFrequency() { return frequency; }
        public int getCount() { return count; }
    }

    /** 住民の要望コメント。投稿者が特定できる情報は含めない。 */
    public record DemandComment(String comment, String frequency, OffsetDateTime votedAt) implements Serializable {
        // JSPの ${...}(EL)で読むための getXxx()。record の xxx() は ${a.xxx} では読めない。
        public String getComment() { return comment; }
        public String getFrequency() { return frequency; }
        public OffsetDateTime getVotedAt() { return votedAt; }
    }

    /** 物件一覧に出す、物件ごとの投票数と需要の要約。 */
    public record PropertySummary(int propertyId, int voteCount, BigDecimal score,
            String topIndustryCode, String topIndustryName) implements Serializable {
        // JSPの ${...}(EL)で読むための getXxx()。record の xxx() は ${a.xxx} では読めない。
        public int getPropertyId() { return propertyId; }
        public int getVoteCount() { return voteCount; }
        public BigDecimal getScore() { return score; }
        public String getTopIndustryCode() { return topIndustryCode; }
        public String getTopIndustryName() { return topIndustryName; }
    }

    /** エリア内で需要の高い物件。 */
    public record TopProperty(int rank, int propertyId, String propertyName, String municipality,
            int voteCount, BigDecimal score, String topIndustryName) implements Serializable {
        // JSPの ${...}(EL)で読むための getXxx()。record の xxx() は ${a.xxx} では読めない。
        public int getRank() { return rank; }
        public int getPropertyId() { return propertyId; }
        public String getPropertyName() { return propertyName; }
        public String getMunicipality() { return municipality; }
        public int getVoteCount() { return voteCount; }
        public BigDecimal getScore() { return score; }
        public String getTopIndustryName() { return topIndustryName; }
    }
}
