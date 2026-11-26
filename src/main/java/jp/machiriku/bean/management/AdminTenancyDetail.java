package jp.machiriku.bean.management;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** 管理者が閲覧する契約物件詳細の画面データ。 */
public class AdminTenancyDetail {
    /** 契約番号。 */
    private int tenancyId;
    /** 申込ID。 */
    private int applicationId;
    /** 物件ID。 */
    private int propertyId;
    /** 物件名。 */
    private String propertyName;
    /** オーナーの会員ID。 */
    private int ownerUserId;
    /** 事業主の会員ID。 */
    private int businessUserId;
    /** 店舗名。 */
    private String storeName;
    /** 契約状態。 */
    private String tenancyStatus;
    /** 契約開始日。 */
    private LocalDate contractStartOn;
    /** 契約終了日。 */
    private LocalDate contractEndOn;
    /** 契約確定日時。 */
    private OffsetDateTime confirmedAt;
    /** オーナーの引渡し日時。 */
    private OffsetDateTime ownerHandoverAt;
    /** 事業主の受け取り日時。 */
    private OffsetDateTime businessHandoverAt;
    /** 開店日。 */
    private LocalDate openedOn;
    /** 契約に紐づく出店計画一覧。 */
    private List<AdminOpeningPlanDetail> openingPlans = List.of();

    /** 空の契約物件詳細を生成する。 */
    public AdminTenancyDetail() { }

    /**
     * 契約番号を取得する。
     * @return 契約番号。
     */
    public int getTenancyId() {
        return tenancyId;
    }

    /**
     * 契約番号を設定する。
     * @param tenancyId 契約番号。
     */
    public void setTenancyId(int tenancyId) {
        this.tenancyId = tenancyId;
    }

    /**
     * 申込IDを取得する。
     * @return 申込ID。
     */
    public int getApplicationId() {
        return applicationId;
    }

    /**
     * 申込IDを設定する。
     * @param applicationId 申込ID。
     */
    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
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
     * 店舗名を取得する。
     * @return 店舗名。
     */
    public String getStoreName() {
        return storeName;
    }

    /**
     * 店舗名を設定する。
     * @param storeName 店舗名。
     */
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    /**
     * 契約状態を取得する。
     * @return 契約状態。
     */
    public String getTenancyStatus() {
        return tenancyStatus;
    }

    /**
     * 契約状態を設定する。
     * @param tenancyStatus 契約状態。
     */
    public void setTenancyStatus(String tenancyStatus) {
        this.tenancyStatus = tenancyStatus;
    }

    /**
     * 契約開始日を取得する。
     * @return 契約開始日。
     */
    public LocalDate getContractStartOn() {
        return contractStartOn;
    }

    /**
     * 契約開始日を設定する。
     * @param contractStartOn 契約開始日。
     */
    public void setContractStartOn(LocalDate contractStartOn) {
        this.contractStartOn = contractStartOn;
    }

    /**
     * 契約終了日を取得する。
     * @return 契約終了日。
     */
    public LocalDate getContractEndOn() {
        return contractEndOn;
    }

    /**
     * 契約終了日を設定する。
     * @param contractEndOn 契約終了日。
     */
    public void setContractEndOn(LocalDate contractEndOn) {
        this.contractEndOn = contractEndOn;
    }

    /**
     * 契約確定日時を取得する。
     * @return 契約確定日時。
     */
    public OffsetDateTime getConfirmedAt() {
        return confirmedAt;
    }

    /**
     * 契約確定日時を設定する。
     * @param confirmedAt 契約確定日時。
     */
    public void setConfirmedAt(OffsetDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    /**
     * オーナーの引渡し日時を取得する。
     * @return オーナーの引渡し日時。
     */
    public OffsetDateTime getOwnerHandoverAt() {
        return ownerHandoverAt;
    }

    /**
     * オーナーの引渡し日時を設定する。
     * @param ownerHandoverAt オーナーの引渡し日時。
     */
    public void setOwnerHandoverAt(OffsetDateTime ownerHandoverAt) {
        this.ownerHandoverAt = ownerHandoverAt;
    }

    /**
     * 事業主の受け取り日時を取得する。
     * @return 事業主の受け取り日時。
     */
    public OffsetDateTime getBusinessHandoverAt() {
        return businessHandoverAt;
    }

    /**
     * 事業主の受け取り日時を設定する。
     * @param businessHandoverAt 事業主の受け取り日時。
     */
    public void setBusinessHandoverAt(OffsetDateTime businessHandoverAt) {
        this.businessHandoverAt = businessHandoverAt;
    }

    /**
     * 開店日を取得する。
     * @return 開店日。
     */
    public LocalDate getOpenedOn() {
        return openedOn;
    }

    /**
     * 開店日を設定する。
     * @param openedOn 開店日。
     */
    public void setOpenedOn(LocalDate openedOn) {
        this.openedOn = openedOn;
    }

    /**
     * 紐づく出店計画一覧を取得する。
     * @return 出店計画一覧。
     */
    public List<AdminOpeningPlanDetail> getOpeningPlans() {
        return openingPlans;
    }

    /**
     * 紐づく出店計画一覧を設定する。
     * @param openingPlans 出店計画一覧。
     */
    public void setOpeningPlans(List<AdminOpeningPlanDetail> openingPlans) {
        this.openingPlans = openingPlans == null ? List.of() : List.copyOf(openingPlans);
    }
}
