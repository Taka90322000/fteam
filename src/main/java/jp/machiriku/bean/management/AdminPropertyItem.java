package jp.machiriku.bean.management;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 管理者向けおよび一般公開の物件一覧・詳細画面で共用する物件データモデル。 */
public class AdminPropertyItem {
    /** 物件ID。 */
    private int propertyId;
    /** 物件名。 */
    private String propertyName;
    /** 掲載者（オーナー）の会員ID。 */
    private int ownerUserId;
    /** 掲載者名。 */
    private String ownerName;
    /** 都道府県。 */
    private String prefecture;
    /** 市区町村。 */
    private String municipality;
    /** 番地。 */
    private String addressLine;
    /** 結合済み所在地（一覧用）。 */
    private String fullAddress;
    /** 緯度。 */
    private BigDecimal latitude;
    /** 経度。 */
    private BigDecimal longitude;
    /** 面積（㎡）。 */
    private BigDecimal floorAreaSqm;
    /** 月額賃料（円）。 */
    private int monthlyRentYen;
    /** 説明文。 */
    private String description;
    /** 掲載URL。 */
    private String listingUrl;
    /** 許認可確認状態コード。 */
    private String permitStatus;
    /** 募集・契約状態コード。 */
    private String listingStatus;
    /** 管理者による掲載停止日時。 */
    private OffsetDateTime adminHiddenAt;
    /** 削除日時。 */
    private OffsetDateTime deletedAt;
    /** 管理状態表示名（通常 / 掲載停止中 / 削除済み）。 */
    private String managementStateLabel;
    /** 申込件数（一覧用）。 */
    private long applicationCount;

    /** 空の物件データを生成する。 */
    public AdminPropertyItem() { }

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
     * 掲載者IDを取得する。
     * @return 掲載者ID。
     */
    public int getOwnerUserId() {
        return ownerUserId;
    }

    /**
     * 掲載者IDを設定する。
     * @param ownerUserId 掲載者ID。
     */
    public void setOwnerUserId(int ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    /**
     * 掲載者名を取得する。
     * @return 掲載者名。
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * 掲載者名を設定する。
     * @param ownerName 掲載者名。
     */
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    /**
     * 都道府県を取得する。
     * @return 都道府県。
     */
    public String getPrefecture() {
        return prefecture;
    }

    /**
     * 都道府県を設定する。
     * @param prefecture 都道府県。
     */
    public void setPrefecture(String prefecture) {
        this.prefecture = prefecture;
    }

    /**
     * 市区町村を取得する。
     * @return 市区町村。
     */
    public String getMunicipality() {
        return municipality;
    }

    /**
     * 市区町村を設定する。
     * @param municipality 市区町村。
     */
    public void setMunicipality(String municipality) {
        this.municipality = municipality;
    }

    /**
     * 番地を取得する。
     * @return 番地。
     */
    public String getAddressLine() {
        return addressLine;
    }

    /**
     * 番地を設定する。
     * @param addressLine 番地。
     */
    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    /**
     * 結合済み所在地を取得する。
     * @return 結合済み所在地。
     */
    public String getFullAddress() {
        return fullAddress;
    }

    /**
     * 結合済み所在地を設定する。
     * @param fullAddress 結合済み所在地。
     */
    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
    }

    /**
     * 緯度を取得する。
     * @return 緯度。
     */
    public BigDecimal getLatitude() {
        return latitude;
    }

    /**
     * 緯度を設定する。
     * @param latitude 緯度。
     */
    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    /**
     * 経度を取得する。
     * @return 経度。
     */
    public BigDecimal getLongitude() {
        return longitude;
    }

    /**
     * 経度を設定する。
     * @param longitude 経度。
     */
    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    /**
     * 面積（㎡）を取得する。
     * @return 面積。
     */
    public BigDecimal getFloorAreaSqm() {
        return floorAreaSqm;
    }

    /**
     * 面積（㎡）を設定する。
     * @param floorAreaSqm 面積。
     */
    public void setFloorAreaSqm(BigDecimal floorAreaSqm) {
        this.floorAreaSqm = floorAreaSqm;
    }

    /**
     * 月額賃料（円）を取得する。
     * @return 月額賃料。
     */
    public int getMonthlyRentYen() {
        return monthlyRentYen;
    }

    /**
     * 月額賃料（円）を設定する。
     * @param monthlyRentYen 月額賃料。
     */
    public void setMonthlyRentYen(int monthlyRentYen) {
        this.monthlyRentYen = monthlyRentYen;
    }

    /**
     * 説明文を取得する。
     * @return 説明文。
     */
    public String getDescription() {
        return description;
    }

    /**
     * 説明文を設定する。
     * @param description 説明文。
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 掲載URLを取得する。
     * @return 掲載URL。
     */
    public String getListingUrl() {
        return listingUrl;
    }

    /**
     * 掲載URLを設定する。
     * @param listingUrl 掲載URL。
     */
    public void setListingUrl(String listingUrl) {
        this.listingUrl = listingUrl;
    }

    /**
     * 許認可確認状態コードを取得する。
     * @return 許認可状態コード。
     */
    public String getPermitStatus() {
        return permitStatus;
    }

    /**
     * 許認可確認状態コードを設定する。
     * @param permitStatus 許認可状態コード。
     */
    public void setPermitStatus(String permitStatus) {
        this.permitStatus = permitStatus;
    }

    /**
     * 募集・契約状態コードを取得する。
     * @return 募集・契約状態コード。
     */
    public String getListingStatus() {
        return listingStatus;
    }

    /**
     * 募集・契約状態コードを設定する。
     * @param listingStatus 募集・契約状態コード。
     */
    public void setListingStatus(String listingStatus) {
        this.listingStatus = listingStatus;
    }

    /**
     * 募集・契約状態の日本語表示名を取得する。
     * @return 募集・契約状態の表示名。
     */
    public String getListingStatusLabel() {
        return ManagementLabels.label(ManagementLabels.PROPERTY_STATES, listingStatus);
    }

    /**
     * 掲載停止日時を取得する。
     * @return 掲載停止日時。
     */
    public OffsetDateTime getAdminHiddenAt() {
        return adminHiddenAt;
    }

    /**
     * 掲載停止日時を設定する。
     * @param adminHiddenAt 掲載停止日時。
     */
    public void setAdminHiddenAt(OffsetDateTime adminHiddenAt) {
        this.adminHiddenAt = adminHiddenAt;
    }

    /**
     * 削除日時を取得する。
     * @return 削除日時。
     */
    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 削除日時を設定する。
     * @param deletedAt 削除日時。
     */
    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 管理状態表示名を取得する。
     * @return 管理状態表示名。
     */
    public String getManagementStateLabel() {
        return managementStateLabel;
    }

    /**
     * 管理状態表示名を設定する。
     * @param managementStateLabel 管理状態表示名。
     */
    public void setManagementStateLabel(String managementStateLabel) {
        this.managementStateLabel = managementStateLabel;
    }

    /**
     * 申込件数を取得する。
     * @return 申込件数。
     */
    public long getApplicationCount() {
        return applicationCount;
    }

    /**
     * 申込件数を設定する。
     * @param applicationCount 申込件数。
     */
    public void setApplicationCount(long applicationCount) {
        this.applicationCount = applicationCount;
    }

    /**
     * 管理者によって掲載停止中か判定する。
     * @return 掲載停止中の場合はtrue。
     */
    public boolean isHidden() {
        return adminHiddenAt != null;
    }

    /**
     * 未削除で編集・掲載操作が可能か判定する。
     * @return 削除されていない場合はtrue。
     */
    public boolean isEditable() {
        return deletedAt == null;
    }

    /**
     * 管理者が募集を公開（OPEN）できる状態か判定する。
     * @return 未削除・非停止で下書きまたは募集停止中の場合はtrue。
     */
    public boolean isCanPublish() {
        return isEditable() && !isHidden()
                && ("DRAFT".equals(listingStatus) || "PAUSED".equals(listingStatus));
    }

    /** 物件詳細に表示する物件写真の1行分データ。 */
    public static class Image {
        /** 表示順。 */
        private int displayOrder;
        /** 画像の保存先キー。 */
        private String storageKey;
        /** 代替テキスト・説明。 */
        private String altText;

        /** 空の写真データを生成する。 */
        public Image() { }

        /**
         * 表示順を取得する。
         * @return 表示順。
         */
        public int getDisplayOrder() {
            return displayOrder;
        }

        /**
         * 表示順を設定する。
         * @param displayOrder 表示順。
         */
        public void setDisplayOrder(int displayOrder) {
            this.displayOrder = displayOrder;
        }

        /**
         * 保存先キーを取得する。
         * @return 保存先キー。
         */
        public String getStorageKey() {
            return storageKey;
        }

        /**
         * 保存先キーを設定する。
         * @param storageKey 保存先キー。
         */
        public void setStorageKey(String storageKey) {
            this.storageKey = storageKey;
        }

        /**
         * 説明を取得する。
         * @return 説明。
         */
        public String getAltText() {
            return altText;
        }

        /**
         * 説明を設定する。
         * @param altText 説明。
         */
        public void setAltText(String altText) {
            this.altText = altText;
        }
    }

    /** 物件詳細に表示する対象業種の1行分データ。 */
    public static class Industry {
        /** 業種ID。 */
        private int industryTypeId;
        /** 業種名。 */
        private String industryName;

        /** 空の対象業種データを生成する。 */
        public Industry() { }

        /**
         * 業種IDを取得する。
         * @return 業種ID。
         */
        public int getIndustryTypeId() {
            return industryTypeId;
        }

        /**
         * 業種IDを設定する。
         * @param industryTypeId 業種ID。
         */
        public void setIndustryTypeId(int industryTypeId) {
            this.industryTypeId = industryTypeId;
        }

        /**
         * 業種名を取得する。
         * @return 業種名。
         */
        public String getIndustryName() {
            return industryName;
        }

        /**
         * 業種名を設定する。
         * @param industryName 業種名。
         */
        public void setIndustryName(String industryName) {
            this.industryName = industryName;
        }
    }

    /** 物件詳細に表示する契約条件の1行分データ。 */
    public static class ContractTerm {
        /** 契約区分。 */
        private String contractKind;
        /** 最短日数。 */
        private Integer minDays;
        /** 最長日数。 */
        private Integer maxDays;
        /** 日額賃料（円）。 */
        private Integer dailyRentYen;

        /** 空の契約条件データを生成する。 */
        public ContractTerm() { }

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
         * 最短日数を取得する。
         * @return 最短日数。
         */
        public Integer getMinDays() {
            return minDays;
        }

        /**
         * 最短日数を設定する。
         * @param minDays 最短日数。
         */
        public void setMinDays(Integer minDays) {
            this.minDays = minDays;
        }

        /**
         * 最長日数を取得する。
         * @return 最長日数。
         */
        public Integer getMaxDays() {
            return maxDays;
        }

        /**
         * 最長日数を設定する。
         * @param maxDays 最長日数。
         */
        public void setMaxDays(Integer maxDays) {
            this.maxDays = maxDays;
        }

        /**
         * 日額賃料を取得する。
         * @return 日額賃料（円）。
         */
        public Integer getDailyRentYen() {
            return dailyRentYen;
        }

        /**
         * 日額賃料を設定する。
         * @param dailyRentYen 日額賃料（円）。
         */
        public void setDailyRentYen(Integer dailyRentYen) {
            this.dailyRentYen = dailyRentYen;
        }
    }

    /** 物件詳細の履歴タブ（出店申込・オファー・通報）で表示する1行分データ。 */
    public static class History {
        /** 申込ID（applicationsタブ用）。 */
        private Integer applicationId;
        /** オファーID（offersタブ用）。 */
        private Integer offerId;
        /** 通報ID（reportsタブ用）。 */
        private Long reportId;
        /** 事業主ID。 */
        private Integer businessUserId;
        /** 事業者名。 */
        private String businessName;
        /** 状態（申込状態・オファー状態・通報対応状態）。 */
        private String status;
        /** 契約ID（applicationsタブ用）。 */
        private Integer tenancyId;
        /** 契約状態（applicationsタブ用）。 */
        private String tenancyStatus;
        /** 通報理由（reportsタブ用）。 */
        private String reasonCode;
        /** 発生日時（申込日時・送信日時・受付日時）。 */
        private OffsetDateTime occurredAt;

        /** 空の履歴データを生成する。 */
        public History() { }

        /**
         * 申込IDを取得する。
         * @return 申込ID。
         */
        public Integer getApplicationId() {
            return applicationId;
        }

        /**
         * 申込IDを設定する。
         * @param applicationId 申込ID。
         */
        public void setApplicationId(Integer applicationId) {
            this.applicationId = applicationId;
        }

        /**
         * オファーIDを取得する。
         * @return オファーID。
         */
        public Integer getOfferId() {
            return offerId;
        }

        /**
         * オファーIDを設定する。
         * @param offerId オファーID。
         */
        public void setOfferId(Integer offerId) {
            this.offerId = offerId;
        }

        /**
         * 通報IDを取得する。
         * @return 通報ID。
         */
        public Long getReportId() {
            return reportId;
        }

        /**
         * 通報IDを設定する。
         * @param reportId 通報ID。
         */
        public void setReportId(Long reportId) {
            this.reportId = reportId;
        }

        /**
         * 事業主IDを取得する。
         * @return 事業主ID。
         */
        public Integer getBusinessUserId() {
            return businessUserId;
        }

        /**
         * 事業主IDを設定する。
         * @param businessUserId 事業主ID。
         */
        public void setBusinessUserId(Integer businessUserId) {
            this.businessUserId = businessUserId;
        }

        /**
         * 事業者名を取得する。
         * @return 事業者名。
         */
        public String getBusinessName() {
            return businessName;
        }

        /**
         * 事業者名を設定する。
         * @param businessName 事業者名。
         */
        public void setBusinessName(String businessName) {
            this.businessName = businessName;
        }

        /**
         * 状態を取得する。
         * @return 状態。
         */
        public String getStatus() {
            return status;
        }

        /**
         * 状態を設定する。
         * @param status 状態。
         */
        public void setStatus(String status) {
            this.status = status;
        }

        /**
         * 契約IDを取得する。
         * @return 契約ID。
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
         * 通報理由コードを取得する。
         * @return 通報理由コード。
         */
        public String getReasonCode() {
            return reasonCode;
        }

        /**
         * 通報理由コードを設定する。
         * @param reasonCode 通報理由コード。
         */
        public void setReasonCode(String reasonCode) {
            this.reasonCode = reasonCode;
        }

        /**
         * 発生日時を取得する。
         * @return 発生日時。
         */
        public OffsetDateTime getOccurredAt() {
            return occurredAt;
        }

        /**
         * 発生日時を設定する。
         * @param occurredAt 発生日時。
         */
        public void setOccurredAt(OffsetDateTime occurredAt) {
            this.occurredAt = occurredAt;
        }
    }
}
