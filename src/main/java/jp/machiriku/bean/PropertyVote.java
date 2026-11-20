package jp.machiriku.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** property_votes(物件投票)の1行と、その希望明細。取消済みなら withdrawnAt に日時が入る。 */
public class PropertyVote implements Serializable {
    private int voteId;
    private int propertyId;
    private int residentUserId;
    private int locationId;
    private BigDecimal distanceKm;
    private BigDecimal distanceScaleKm;
    private String visitFrequency;
    private String requestComment;
    private OffsetDateTime updatedAt;
    private OffsetDateTime withdrawnAt;
    private List<VoteChoice> choices = new ArrayList<>();

    public int getVoteId() { return voteId; }
    public void setVoteId(int voteId) { this.voteId = voteId; }
    public int getPropertyId() { return propertyId; }
    public void setPropertyId(int propertyId) { this.propertyId = propertyId; }
    public int getResidentUserId() { return residentUserId; }
    public void setResidentUserId(int residentUserId) { this.residentUserId = residentUserId; }
    public int getLocationId() { return locationId; }
    public void setLocationId(int locationId) { this.locationId = locationId; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
    public BigDecimal getDistanceScaleKm() { return distanceScaleKm; }
    public void setDistanceScaleKm(BigDecimal distanceScaleKm) { this.distanceScaleKm = distanceScaleKm; }
    public String getVisitFrequency() { return visitFrequency; }
    public void setVisitFrequency(String visitFrequency) { this.visitFrequency = visitFrequency; }
    public String getRequestComment() { return requestComment; }
    public void setRequestComment(String requestComment) { this.requestComment = requestComment; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getWithdrawnAt() { return withdrawnAt; }
    public void setWithdrawnAt(OffsetDateTime withdrawnAt) { this.withdrawnAt = withdrawnAt; }
    public List<VoteChoice> getChoices() { return choices; }
    public void setChoices(List<VoteChoice> choices) { this.choices = choices; }
    public boolean isWithdrawn() { return withdrawnAt != null; }
}
