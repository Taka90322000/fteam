package jp.machiriku.bean.management;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 管理者が閲覧・公開停止する出店計画詳細の画面データ。 */
public class AdminOpeningPlanDetail {
    /** 計画番号。 */
    private int planId;
    /** 契約ID。 */
    private int tenancyId;
    /** 申込ID。 */
    private int applicationId;
    /** 物件ID。 */
    private int propertyId;
    /** 事業主の会員ID。 */
    private int businessUserId;
    /** 計画名。 */
    private String title;
    /** 計画内容。 */
    private String body;
    /** 開店予定日。 */
    private LocalDate plannedOpenOn;
    /** 進捗状態。 */
    private String progressStatus;
    /** 公開状態。 */
    private String publicationStatus;
    /** 公開日時。 */
    private OffsetDateTime publishedAt;
    /** 更新日時。 */
    private OffsetDateTime updatedAt;

    /** 空の出店計画詳細を生成する。 */
    public AdminOpeningPlanDetail() { }

    /**
     * 計画番号を取得する。
     * @return 計画番号。
     */
    public int getPlanId() {
        return planId;
    }

    /**
     * 計画番号を設定する。
     * @param planId 計画番号。
     */
    public void setPlanId(int planId) {
        this.planId = planId;
    }

    /**
     * 契約IDを取得する。
     * @return 契約ID。
     */
    public int getTenancyId() {
        return tenancyId;
    }

    /**
     * 契約IDを設定する。
     * @param tenancyId 契約ID。
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
     * 計画名を取得する。
     * @return 計画名。
     */
    public String getTitle() {
        return title;
    }

    /**
     * 計画名を設定する。
     * @param title 計画名。
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 計画内容を取得する。
     * @return 計画内容。
     */
    public String getBody() {
        return body;
    }

    /**
     * 計画内容を設定する。
     * @param body 計画内容。
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * 開店予定日を取得する。
     * @return 開店予定日。
     */
    public LocalDate getPlannedOpenOn() {
        return plannedOpenOn;
    }

    /**
     * 開店予定日を設定する。
     * @param plannedOpenOn 開店予定日。
     */
    public void setPlannedOpenOn(LocalDate plannedOpenOn) {
        this.plannedOpenOn = plannedOpenOn;
    }

    /**
     * 進捗状態を取得する。
     * @return 進捗状態。
     */
    public String getProgressStatus() {
        return progressStatus;
    }

    /**
     * 進捗状態を設定する。
     * @param progressStatus 進捗状態。
     */
    public void setProgressStatus(String progressStatus) {
        this.progressStatus = progressStatus;
    }

    /**
     * 公開状態を取得する。
     * @return 公開状態。
     */
    public String getPublicationStatus() {
        return publicationStatus;
    }

    /**
     * 公開状態を設定する。
     * @param publicationStatus 公開状態。
     */
    public void setPublicationStatus(String publicationStatus) {
        this.publicationStatus = publicationStatus;
    }

    /**
     * 公開日時を取得する。
     * @return 公開日時。
     */
    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    /**
     * 公開日時を設定する。
     * @param publishedAt 公開日時。
     */
    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    /**
     * 更新日時を取得する。
     * @return 更新日時。
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 更新日時を設定する。
     * @param updatedAt 更新日時。
     */
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 管理者が公開停止操作を行える状態か判定する。
     * @return 現在公開中（PUBLISHED）の場合はtrue。
     */
    public boolean isCanArchive() {
        return "PUBLISHED".equals(publicationStatus);
    }
}
