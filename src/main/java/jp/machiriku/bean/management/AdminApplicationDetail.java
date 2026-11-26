package jp.machiriku.bean.management;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** 管理者が閲覧する出店申し込み詳細の画面データ。 */
public class AdminApplicationDetail {
    /** 申込番号。 */
    private int applicationId;
    /** 物件ID。 */
    private int propertyId;
    /** 物件名。 */
    private String propertyName;
    /** オーナーの会員ID。 */
    private int ownerUserId;
    /** 事業主の会員ID。 */
    private int businessUserId;
    /** 申込状態。 */
    private String applicationStatus;
    /** 申込業種名。 */
    private String industryName;
    /** 契約区分。 */
    private String contractKind;
    /** 希望開始日。 */
    private LocalDate requestedStartOn;
    /** 希望終了日。 */
    private LocalDate requestedEndOn;
    /** 内覧希望日時。 */
    private OffsetDateTime viewingRequestedAt;
    /** 申込内容。 */
    private String message;
    /** オーナーの返信。 */
    private String ownerReply;
    /** 返信日時。 */
    private OffsetDateTime repliedAt;
    /** 申込日時。 */
    private OffsetDateTime createdAt;
    /** 成立した契約ID。未成立時はnull。 */
    private Integer tenancyId;
    /** 申込時の必要資格・書類チェック一覧。 */
    private List<CheckItem> checks = List.of();

    /** 空の出店申し込み詳細を生成する。 */
    public AdminApplicationDetail() { }

    /**
     * 申込番号を取得する。
     * @return 申込番号。
     */
    public int getApplicationId() {
        return applicationId;
    }

    /**
     * 申込番号を設定する。
     * @param applicationId 申込番号。
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
     * 申込状態を取得する。
     * @return 申込状態。
     */
    public String getApplicationStatus() {
        return applicationStatus;
    }

    /**
     * 申込状態を設定する。
     * @param applicationStatus 申込状態。
     */
    public void setApplicationStatus(String applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    /**
     * 申込業種名を取得する。
     * @return 申込業種名。
     */
    public String getIndustryName() {
        return industryName;
    }

    /**
     * 申込業種名を設定する。
     * @param industryName 申込業種名。
     */
    public void setIndustryName(String industryName) {
        this.industryName = industryName;
    }

    /**
     * 契約区分を取得する。
     * @return 契約区分。
     */
    public String getContractKind() {
        return contractKind;
    }

    /**
     * 契約区分を設定する。
     * @param contractKind 契約区分。
     */
    public void setContractKind(String contractKind) {
        this.contractKind = contractKind;
    }

    /**
     * 希望開始日を取得する。
     * @return 希望開始日。
     */
    public LocalDate getRequestedStartOn() {
        return requestedStartOn;
    }

    /**
     * 希望開始日を設定する。
     * @param requestedStartOn 希望開始日。
     */
    public void setRequestedStartOn(LocalDate requestedStartOn) {
        this.requestedStartOn = requestedStartOn;
    }

    /**
     * 希望終了日を取得する。
     * @return 希望終了日。
     */
    public LocalDate getRequestedEndOn() {
        return requestedEndOn;
    }

    /**
     * 希望終了日を設定する。
     * @param requestedEndOn 希望終了日。
     */
    public void setRequestedEndOn(LocalDate requestedEndOn) {
        this.requestedEndOn = requestedEndOn;
    }

    /**
     * 内覧希望日時を取得する。
     * @return 内覧希望日時。
     */
    public OffsetDateTime getViewingRequestedAt() {
        return viewingRequestedAt;
    }

    /**
     * 内覧希望日時を設定する。
     * @param viewingRequestedAt 内覧希望日時。
     */
    public void setViewingRequestedAt(OffsetDateTime viewingRequestedAt) {
        this.viewingRequestedAt = viewingRequestedAt;
    }

    /**
     * 申込内容を取得する。
     * @return 申込内容。
     */
    public String getMessage() {
        return message;
    }

    /**
     * 申込内容を設定する。
     * @param message 申込内容。
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * オーナーの返信を取得する。
     * @return オーナーの返信。
     */
    public String getOwnerReply() {
        return ownerReply;
    }

    /**
     * オーナーの返信を設定する。
     * @param ownerReply オーナーの返信。
     */
    public void setOwnerReply(String ownerReply) {
        this.ownerReply = ownerReply;
    }

    /**
     * 返信日時を取得する。
     * @return 返信日時。
     */
    public OffsetDateTime getRepliedAt() {
        return repliedAt;
    }

    /**
     * 返信日時を設定する。
     * @param repliedAt 返信日時。
     */
    public void setRepliedAt(OffsetDateTime repliedAt) {
        this.repliedAt = repliedAt;
    }

    /**
     * 申込日時を取得する。
     * @return 申込日時。
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 申込日時を設定する。
     * @param createdAt 申込日時。
     */
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 契約IDを取得する。
     * @return 契約ID。未契約の場合はnull。
     */
    public Integer getTenancyId() {
        return tenancyId;
    }

    /**
     * 契約IDを設定する。
     * @param tenancyId 契約ID。
     */
    public void setTenancyId(Integer tenancyId) {
        this.tenancyId = tenancyId;
    }

    /**
     * 申込時の必要資格・書類一覧を取得する。
     * @return チェック項目一覧。
     */
    public List<CheckItem> getChecks() {
        return checks;
    }

    /**
     * 申込時の必要資格・書類一覧を設定する。
     * @param checks チェック項目一覧。
     */
    public void setChecks(List<CheckItem> checks) {
        this.checks = checks == null ? List.of() : List.copyOf(checks);
    }

    /** 出店申し込み時の必要資格・書類チェック項目の1行分データ。 */
    public static class CheckItem {
        /** 申込当時の要件名。 */
        private String requirementName;
        /** 必須要件かどうか。 */
        private Boolean required;
        /** 適用条件。 */
        private String conditionText;
        /** 事業主による確認済みフラグ。 */
        private Boolean businessConfirmed;
        /** オーナー側の確認状態。 */
        private String ownerCheckStatus;
        /** 確認メモ。 */
        private String checkNote;

        /** 空のチェック項目を生成する。 */
        public CheckItem() { }

        /**
         * 要件名を取得する。
         * @return 要件名。
         */
        public String getRequirementName() {
            return requirementName;
        }

        /**
         * 要件名を設定する。
         * @param requirementName 要件名。
         */
        public void setRequirementName(String requirementName) {
            this.requirementName = requirementName;
        }

        /**
         * 必須区分を取得する。
         * @return 必須の場合はtrue。
         */
        public Boolean getRequired() {
            return required;
        }

        /**
         * 必須区分を設定する。
         * @param required 必須区分。
         */
        public void setRequired(Boolean required) {
            this.required = required;
        }

        /**
         * 適用条件を取得する。
         * @return 適用条件。
         */
        public String getConditionText() {
            return conditionText;
        }

        /**
         * 適用条件を設定する。
         * @param conditionText 適用条件。
         */
        public void setConditionText(String conditionText) {
            this.conditionText = conditionText;
        }

        /**
         * 事業主による確認状態を取得する。
         * @return 確認済みの場合はtrue。
         */
        public Boolean getBusinessConfirmed() {
            return businessConfirmed;
        }

        /**
         * 事業主による確認状態を設定する。
         * @param businessConfirmed 事業主による確認状態。
         */
        public void setBusinessConfirmed(Boolean businessConfirmed) {
            this.businessConfirmed = businessConfirmed;
        }

        /**
         * オーナーの確認状態を取得する。
         * @return オーナーの確認状態。
         */
        public String getOwnerCheckStatus() {
            return ownerCheckStatus;
        }

        /**
         * オーナーの確認状態を設定する。
         * @param ownerCheckStatus オーナーの確認状態。
         */
        public void setOwnerCheckStatus(String ownerCheckStatus) {
            this.ownerCheckStatus = ownerCheckStatus;
        }

        /**
         * 確認メモを取得する。
         * @return 確認メモ。
         */
        public String getCheckNote() {
            return checkNote;
        }

        /**
         * 確認メモを設定する。
         * @param checkNote 確認メモ。
         */
        public void setCheckNote(String checkNote) {
            this.checkNote = checkNote;
        }
    }
}
