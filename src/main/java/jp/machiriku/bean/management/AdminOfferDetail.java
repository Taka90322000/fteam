package jp.machiriku.bean.management;

import java.time.OffsetDateTime;

/** 管理者が閲覧する出店オファー詳細の画面データ。 */
public class AdminOfferDetail {
    /** オファーID。 */
    private int offerId;
    /** 対象物件ID。 */
    private int propertyId;
    /** 対象物件名。 */
    private String propertyName;
    /** 送信者（オーナー）の会員ID。 */
    private int ownerUserId;
    /** 送信者（オーナー）の表示名。 */
    private String ownerName;
    /** 受信者（事業主）の会員ID。 */
    private int businessUserId;
    /** 受信者（事業主）の事業者名。 */
    private String businessName;
    /** オファーの作成方法。 */
    private String creationKind;
    /** オファー本文。 */
    private String message;
    /** オファーの状態。 */
    private String offerStatus;
    /** 送信日時。 */
    private OffsetDateTime sentAt;
    /** 返答日時。 */
    private OffsetDateTime respondedAt;

    /** 空の出店オファー詳細を生成する。 */
    public AdminOfferDetail() { }

    /**
     * オファーIDを取得する。
     * @return オファーID。
     */
    public int getOfferId() {
        return offerId;
    }

    /**
     * オファーIDを設定する。
     * @param offerId オファーID。
     */
    public void setOfferId(int offerId) {
        this.offerId = offerId;
    }

    /**
     * 物件IDを取得する。
     * @return 物件ID。
     */
    public int getPropertyId() {
        return propertyId;
    }

    /**
     * 物件IDを設定する。
     * @param propertyId 物件ID。
     */
    public void setPropertyId(int propertyId) {
        this.propertyId = propertyId;
    }

    /**
     * 物件名を取得する。
     * @return 物件名。
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * 物件名を設定する。
     * @param propertyName 物件名。
     */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * オーナーの会員IDを取得する。
     * @return オーナーの会員ID。
     */
    public int getOwnerUserId() {
        return ownerUserId;
    }

    /**
     * オーナーの会員IDを設定する。
     * @param ownerUserId オーナーの会員ID。
     */
    public void setOwnerUserId(int ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    /**
     * 送信者名を取得する。
     * @return 送信者名。
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * 送信者名を設定する。
     * @param ownerName 送信者名。
     */
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    /**
     * 事業主の会員IDを取得する。
     * @return 事業主の会員ID。
     */
    public int getBusinessUserId() {
        return businessUserId;
    }

    /**
     * 事業主の会員IDを設定する。
     * @param businessUserId 事業主の会員ID。
     */
    public void setBusinessUserId(int businessUserId) {
        this.businessUserId = businessUserId;
    }

    /**
     * 受信者（事業者名）を取得する。
     * @return 事業者名。
     */
    public String getBusinessName() {
        return businessName;
    }

    /**
     * 受信者（事業者名）を設定する。
     * @param businessName 事業者名。
     */
    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    /**
     * 作成方法を取得する。
     * @return 作成方法。
     */
    public String getCreationKind() {
        return creationKind;
    }

    /**
     * 作成方法を設定する。
     * @param creationKind 作成方法。
     */
    public void setCreationKind(String creationKind) {
        this.creationKind = creationKind;
    }

    /**
     * メッセージ本文を取得する。
     * @return メッセージ本文。
     */
    public String getMessage() {
        return message;
    }

    /**
     * メッセージ本文を設定する。
     * @param message メッセージ本文。
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * オファー状態を取得する。
     * @return オファー状態。
     */
    public String getOfferStatus() {
        return offerStatus;
    }

    /**
     * オファー状態を設定する。
     * @param offerStatus オファー状態。
     */
    public void setOfferStatus(String offerStatus) {
        this.offerStatus = offerStatus;
    }

    /**
     * 送信日時を取得する。
     * @return 送信日時。
     */
    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    /**
     * 送信日時を設定する。
     * @param sentAt 送信日時。
     */
    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }

    /**
     * 返答日時を取得する。
     * @return 返答日時。未返答の場合はnull。
     */
    public OffsetDateTime getRespondedAt() {
        return respondedAt;
    }

    /**
     * 返答日時を設定する。
     * @param respondedAt 返答日時。
     */
    public void setRespondedAt(OffsetDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
