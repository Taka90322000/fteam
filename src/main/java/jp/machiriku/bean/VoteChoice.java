package jp.machiriku.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/** vote_choices(投票希望明細)の1行。第1〜第3希望の業種と順位係数。 */
public class VoteChoice implements Serializable {
    private int preferenceRank;
    private int industryTypeId;
    private String industryCode;
    private String industryName;
    private BigDecimal rankWeight;

    public int getPreferenceRank() { return preferenceRank; }
    public void setPreferenceRank(int preferenceRank) { this.preferenceRank = preferenceRank; }
    public int getIndustryTypeId() { return industryTypeId; }
    public void setIndustryTypeId(int industryTypeId) { this.industryTypeId = industryTypeId; }
    public String getIndustryCode() { return industryCode; }
    public void setIndustryCode(String industryCode) { this.industryCode = industryCode; }
    public String getIndustryName() { return industryName; }
    public void setIndustryName(String industryName) { this.industryName = industryName; }
    public BigDecimal getRankWeight() { return rankWeight; }
    public void setRankWeight(BigDecimal rankWeight) { this.rankWeight = rankWeight; }
}
