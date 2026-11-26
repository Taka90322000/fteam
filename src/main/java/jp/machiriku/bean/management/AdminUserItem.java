package jp.machiriku.bean.management;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 管理者向けユーザー一覧・詳細ヘッダーおよび会員に紐づく従属データ（プロフィール・希望業種・生活拠点・保有資格・活動履歴）のモデル。
 */
public class AdminUserItem {
    /** 会員ID。 */
    private int userId;
    /** 表示名。 */
    private String displayName;
    /** 事業者名または法人名（一覧表示用）。 */
    private String organizationName;
    /** 連絡用メールアドレス。 */
    private String email;
    /** 利用区分コード（RESIDENT / BUSINESS / OWNER）。 */
    private String userRole;
    /** 利用状態コード（PENDING / ACTIVE / BAN / WITHDRAWN）。 */
    private String accountStatus;
    /** 登録日時。 */
    private OffsetDateTime createdAt;
    /** 最終更新日時。 */
    private OffsetDateTime updatedAt;
    /** 退会日時。未退会時はnull。 */
    private OffsetDateTime withdrawnAt;

    /** デフォルトコンストラクター。 */
    public AdminUserItem() { }

    /**
     * 会員IDを取得する。
     * @return 会員ID。
     */
    public int getUserId() {
        return userId;
    }

    /**
     * 会員IDを設定する。
     * @param userId 会員ID。
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * 表示名を取得する。
     * @return 表示名。
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 表示名を設定する。
     * @param displayName 表示名。
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 事業者名または法人名を取得する。
     * @return 事業者名・法人名。
     */
    public String getOrganizationName() {
        return organizationName;
    }

    /**
     * 事業者名または法人名を設定する。
     * @param organizationName 事業者名・法人名。
     */
    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    /**
     * メールアドレスを取得する。
     * @return メールアドレス。
     */
    public String getEmail() {
        return email;
    }

    /**
     * メールアドレスを設定する。
     * @param email メールアドレス。
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 利用区分コードを取得する。
     * @return 利用区分コード。
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * 利用区分コードを設定する。
     * @param userRole 利用区分コード。
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * 利用区分の日本語表示名を取得する。
     * @return 利用区分の表示名。
     */
    public String getUserRoleLabel() {
        return ManagementLabels.label(ManagementLabels.USER_ROLES, userRole);
    }

    /**
     * 利用状態コードを取得する。
     * @return 利用状態コード。
     */
    public String getAccountStatus() {
        return accountStatus;
    }

    /**
     * 利用状態コードを設定する。
     * @param accountStatus 利用状態コード。
     */
    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    /**
     * 利用状態の日本語表示名を取得する。
     * @return 利用状態の表示名。
     */
    public String getAccountStatusLabel() {
        return ManagementLabels.label(ManagementLabels.USER_STATES, accountStatus);
    }

    /**
     * 退会済みかどうかを判定する。
     * @return 退会日時があるかWITHDRAWN状態の場合はtrue。
     */
    public boolean isWithdrawn() {
        return withdrawnAt != null || "WITHDRAWN".equals(accountStatus);
    }

    /**
     * 利用停止中かどうかを判定する。
     * @return BAN状態の場合はtrue。
     */
    public boolean isSuspended() {
        return "BAN".equals(accountStatus);
    }

    /**
     * 登録日時を取得する。
     * @return 登録日時。
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 登録日時を設定する。
     * @param createdAt 登録日時。
     */
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 最終更新日時を取得する。
     * @return 最終更新日時。
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 最終更新日時を設定する。
     * @param updatedAt 最終更新日時。
     */
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 退会日時を取得する。
     * @return 退会日時。
     */
    public OffsetDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    /**
     * 退会日時を設定する。
     * @param withdrawnAt 退会日時。
     */
    public void setWithdrawnAt(OffsetDateTime withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }

    /** 会員の利用区分別プロフィールおよび会員向け事業主公開プロフィールで共用するデータモデル。 */
    public static class Profile {
        /** 会員ID（公開事業主一覧・詳細表示用）。 */
        private int userId;
        /** 年齢区分（地域住民用）。 */
        private String ageGroup;
        /** 性別（地域住民用）。 */
        private String gender;
        /** 事業者名（事業主用）。 */
        private String businessName;
        /** 法人名（オーナー用）。 */
        private String companyName;
        /** 代表者名（オーナー・事業主用）。 */
        private String representativeName;
        /** 主な電話番号・E.164形式（オーナー・事業主用）。 */
        private String phoneE164;
        /** 追加の電話番号・E.164形式（オーナー・事業主用）。 */
        private String secondaryPhoneE164;
        /** 希望エリア名（事業主用）。 */
        private String preferredArea;
        /** 従業員数（事業主用）。 */
        private Integer employeeCount;
        /** 業界経験（事業主用）。 */
        private String industryExperience;
        /** 経営経験（事業主用）。 */
        private String managementExperience;
        /** アイコン画像URL（オーナー・事業主用）。 */
        private String iconImageUrl;

        /** デフォルトコンストラクター。 */
        public Profile() { }

        /**
         * 会員IDを取得する。
         * @return 会員ID。
         */
        public int getUserId() {
            return userId;
        }

        /**
         * 会員IDを設定する。
         * @param userId 会員ID。
         */
        public void setUserId(int userId) {
            this.userId = userId;
        }

        /**
         * 年齢区分を取得する。
         * @return 年齢区分。
         */
        public String getAgeGroup() {
            return ageGroup;
        }

        /**
         * 年齢区分を設定する。
         * @param ageGroup 年齢区分。
         */
        public void setAgeGroup(String ageGroup) {
            this.ageGroup = ageGroup;
        }

        /**
         * 性別を取得する。
         * @return 性別。
         */
        public String getGender() {
            return gender;
        }

        /**
         * 性別を設定する。
         * @param gender 性別。
         */
        public void setGender(String gender) {
            this.gender = gender;
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
         * 法人名を取得する。
         * @return 法人名。
         */
        public String getCompanyName() {
            return companyName;
        }

        /**
         * 法人名を設定する。
         * @param companyName 法人名。
         */
        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        /**
         * 代表者名を取得する。
         * @return 代表者名。
         */
        public String getRepresentativeName() {
            return representativeName;
        }

        /**
         * 代表者名を設定する。
         * @param representativeName 代表者名。
         */
        public void setRepresentativeName(String representativeName) {
            this.representativeName = representativeName;
        }

        /**
         * 主な電話番号を取得する。
         * @return 電話番号。
         */
        public String getPhoneE164() {
            return phoneE164;
        }

        /**
         * 主な電話番号を設定する。
         * @param phoneE164 電話番号。
         */
        public void setPhoneE164(String phoneE164) {
            this.phoneE164 = phoneE164;
        }

        /**
         * 追加の電話番号を取得する。
         * @return 追加の電話番号。
         */
        public String getSecondaryPhoneE164() {
            return secondaryPhoneE164;
        }

        /**
         * 追加の電話番号を設定する。
         * @param secondaryPhoneE164 追加の電話番号。
         */
        public void setSecondaryPhoneE164(String secondaryPhoneE164) {
            this.secondaryPhoneE164 = secondaryPhoneE164;
        }

        /**
         * 希望エリア名を取得する。
         * @return 希望エリア名。
         */
        public String getPreferredArea() {
            return preferredArea;
        }

        /**
         * 希望エリア名を設定する。
         * @param preferredArea 希望エリア名。
         */
        public void setPreferredArea(String preferredArea) {
            this.preferredArea = preferredArea;
        }

        /**
         * 従業員数を取得する。
         * @return 従業員数。
         */
        public Integer getEmployeeCount() {
            return employeeCount;
        }

        /**
         * 従業員数を設定する。
         * @param employeeCount 従業員数。
         */
        public void setEmployeeCount(Integer employeeCount) {
            this.employeeCount = employeeCount;
        }

        /**
         * 業界経験を取得する。
         * @return 業界経験。
         */
        public String getIndustryExperience() {
            return industryExperience;
        }

        /**
         * 業界経験を設定する。
         * @param industryExperience 業界経験。
         */
        public void setIndustryExperience(String industryExperience) {
            this.industryExperience = industryExperience;
        }

        /**
         * 経営経験を取得する。
         * @return 経営経験。
         */
        public String getManagementExperience() {
            return managementExperience;
        }

        /**
         * 経営経験を設定する。
         * @param managementExperience 経営経験。
         */
        public void setManagementExperience(String managementExperience) {
            this.managementExperience = managementExperience;
        }

        /**
         * アイコン画像URLを取得する。
         * @return アイコン画像URL。
         */
        public String getIconImageUrl() {
            return iconImageUrl;
        }

        /**
         * アイコン画像URLを設定する。
         * @param iconImageUrl アイコン画像URL。
         */
        public void setIconImageUrl(String iconImageUrl) {
            this.iconImageUrl = iconImageUrl;
        }
    }

    /** 会員が登録している希望業種1件を表すデータモデル。 */
    public static class Preference {
        /** 業種ID。 */
        private int industryTypeId;
        /** 所属カテゴリ名。 */
        private String categoryName;
        /** 業種名。 */
        private String industryName;
        /** 業種・カテゴリマスターの有効状態表示（有効 / 無効）。 */
        private String masterStateLabel;

        /** デフォルトコンストラクター。 */
        public Preference() { }

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
         * 所属カテゴリ名を取得する。
         * @return カテゴリ名。
         */
        public String getCategoryName() {
            return categoryName;
        }

        /**
         * 所属カテゴリ名を設定する。
         * @param categoryName カテゴリ名。
         */
        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
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

        /**
         * マスターの状態表示を取得する。
         * @return 状態表示。
         */
        public String getMasterStateLabel() {
            return masterStateLabel;
        }

        /**
         * マスターの状態表示を設定する。
         * @param masterStateLabel 状態表示。
         */
        public void setMasterStateLabel(String masterStateLabel) {
            this.masterStateLabel = masterStateLabel;
        }
    }

    /** 地域住民の生活拠点（居住地・勤務先・学校）1件を表すデータモデル。 */
    public static class Location {
        /** 生活拠点種別コード（HOME / WORK）。 */
        private String locationKind;
        /** 都道府県。 */
        private String prefecture;
        /** 市区町村。 */
        private String municipality;
        /** 番地・建物名。 */
        private String addressLine;
        /** 緯度。 */
        private BigDecimal latitude;
        /** 経度。 */
        private BigDecimal longitude;
        /** 最終更新日時。 */
        private OffsetDateTime updatedAt;

        /** デフォルトコンストラクター。 */
        public Location() { }

        /**
         * 生活拠点種別コードを取得する。
         * @return 種別コード。
         */
        public String getLocationKind() {
            return locationKind;
        }

        /**
         * 生活拠点種別コードを設定する。
         * @param locationKind 種別コード。
         */
        public void setLocationKind(String locationKind) {
            this.locationKind = locationKind;
        }

        /**
         * 生活拠点種別の日本語表示名を取得する。
         * @return 種別の表示名。
         */
        public String getLocationKindLabel() {
            if ("HOME".equals(locationKind)) {
                return "居住地";
            }
            if ("WORK".equals(locationKind)) {
                return "勤務先・学校";
            }
            return locationKind;
        }

        /**
         * 都道府県・市区町村・番地を連結した表示・編集用の住所文字列を返す。
         * @return 連結済みの住所文字列。未設定項目は空文字として連結する。
         */
        public String getFullAddress() {
            StringBuilder address = new StringBuilder();
            if (prefecture != null) {
                address.append(prefecture);
            }
            if (municipality != null) {
                address.append(municipality);
            }
            if (addressLine != null) {
                address.append(addressLine);
            }
            return address.toString();
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
         * 番地・建物名を取得する。
         * @return 番地・建物名。
         */
        public String getAddressLine() {
            return addressLine;
        }

        /**
         * 番地・建物名を設定する。
         * @param addressLine 番地・建物名。
         */
        public void setAddressLine(String addressLine) {
            this.addressLine = addressLine;
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
         * 最終更新日時を取得する。
         * @return 最終更新日時。
         */
        public OffsetDateTime getUpdatedAt() {
            return updatedAt;
        }

        /**
         * 最終更新日時を設定する。
         * @param updatedAt 最終更新日時。
         */
        public void setUpdatedAt(OffsetDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    /** 事業主が登録している保有資格・許認可1件を表すデータモデル。 */
    public static class Qualification {
        /** 資格ID。 */
        private int qualificationId;
        /** 資格・許認可の名称。 */
        private String qualificationName;
        /** 種類コード（QUALIFICATION / PERMIT / DOCUMENT）。 */
        private String requirementKind;
        /** 資格・許認可番号。 */
        private String credentialNumber;
        /** 取得日。 */
        private LocalDate obtainedOn;
        /** 有効期限。 */
        private LocalDate expiresOn;
        /** 保有資格の有効状態。 */
        private boolean active = true;

        /** デフォルトコンストラクター。 */
        public Qualification() { }

        /**
         * 資格IDを取得する。
         * @return 資格ID。
         */
        public int getQualificationId() {
            return qualificationId;
        }

        /**
         * 資格IDを設定する。
         * @param qualificationId 資格ID。
         */
        public void setQualificationId(int qualificationId) {
            this.qualificationId = qualificationId;
        }

        /**
         * 資格・許認可の名称を取得する。
         * @return 名称。
         */
        public String getQualificationName() {
            return qualificationName;
        }

        /**
         * 資格・許認可の名称を設定する。
         * @param qualificationName 名称。
         */
        public void setQualificationName(String qualificationName) {
            this.qualificationName = qualificationName;
        }

        /**
         * 種類コードを取得する。
         * @return 種類コード。
         */
        public String getRequirementKind() {
            return requirementKind;
        }

        /**
         * 種類コードを設定する。
         * @param requirementKind 種類コード。
         */
        public void setRequirementKind(String requirementKind) {
            this.requirementKind = requirementKind;
        }

        /**
         * 種類の日本語表示名を取得する。
         * @return 種類の表示名。
         */
        public String getRequirementKindLabel() {
            return ManagementLabels.label(ManagementLabels.QUALIFICATION_KINDS, requirementKind);
        }

        /**
         * 資格番号を取得する。
         * @return 資格番号。
         */
        public String getCredentialNumber() {
            return credentialNumber;
        }

        /**
         * 資格番号を設定する。
         * @param credentialNumber 資格番号。
         */
        public void setCredentialNumber(String credentialNumber) {
            this.credentialNumber = credentialNumber;
        }

        /**
         * 取得日を取得する。
         * @return 取得日。
         */
        public LocalDate getObtainedOn() {
            return obtainedOn;
        }

        /**
         * 取得日を設定する。
         * @param obtainedOn 取得日。
         */
        public void setObtainedOn(LocalDate obtainedOn) {
            this.obtainedOn = obtainedOn;
        }

        /**
         * 有効期限を取得する。
         * @return 有効期限。
         */
        public LocalDate getExpiresOn() {
            return expiresOn;
        }

        /**
         * 有効期限を設定する。
         * @param expiresOn 有効期限。
         */
        public void setExpiresOn(LocalDate expiresOn) {
            this.expiresOn = expiresOn;
        }

        /**
         * 有効状態を取得する。
         * @return 有効な場合はtrue。
         */
        public boolean isActive() {
            return active;
        }

        /**
         * 有効状態を設定する。
         * @param active 有効な場合はtrue。
         */
        public void setActive(boolean active) {
            this.active = active;
        }
    }

    /** 会員詳細画面の各種活動履歴タブで表示する1行分のデータモデル。 */
    public static class History {
        /** 履歴レコード自身の主キーID。 */
        private long recordId;
        /** 関連する物件ID。 */
        private Integer propertyId;
        /** 関連する申込ID。 */
        private Integer applicationId;
        /** 関連する契約ID。 */
        private Integer tenancyId;
        /** 関連するアンケートIDまたはクーポンID。 */
        private Integer relatedId;
        /** オファーの相手会員ID。 */
        private Integer partnerUserId;
        /** 主な名称・タイトル。 */
        private String title;
        /** 補足名称（店舗名、相手表示名など）。 */
        private String subtitle;
        /** 詳細テキスト（投票した業種、回答内容、通報理由コードなど）。 */
        private String detailText;
        /** 方向ラベル（オファーの送信 / 受信）。 */
        private String direction;
        /** 主な状態コードまたは表示名。 */
        private String statusText;
        /** 補助状態コード（出店計画の公開状態など）。 */
        private String secondaryStatusText;
        /** 満足度スコア（アンケート回答時）。 */
        private Integer rating;
        /** 件数1（回答数、発行数、申込数など）。 */
        private Long primaryCount;
        /** 件数2（使用数など）。 */
        private Long secondaryCount;
        /** 開始日または予定日（契約開始日、開店予定日）。 */
        private LocalDate startDate;
        /** 終了日（契約終了日）。 */
        private LocalDate endDate;
        /** 日時1（作成日時、投票日時、発行日時、送信日時、受付日時など）。 */
        private OffsetDateTime primaryTime;
        /** 日時2（更新日時、使用日時、掲載停止日時など）。 */
        private OffsetDateTime secondaryTime;
        /** 日時3（取下げ日時、無効化日時など）。 */
        private OffsetDateTime tertiaryTime;
        /** 有効期限日時（クーポン用）。 */
        private OffsetDateTime validUntil;

        /** デフォルトコンストラクター。 */
        public History() { }

        /**
         * 履歴レコードの主キーIDを取得する。
         * @return 主キーID。
         */
        public long getRecordId() {
            return recordId;
        }

        /**
         * 履歴レコードの主キーIDを設定する。
         * @param recordId 主キーID。
         */
        public void setRecordId(long recordId) {
            this.recordId = recordId;
        }

        /**
         * 関連物件IDを取得する。
         * @return 物件ID。
         */
        public Integer getPropertyId() {
            return propertyId;
        }

        /**
         * 関連物件IDを設定する。
         * @param propertyId 物件ID。
         */
        public void setPropertyId(Integer propertyId) {
            this.propertyId = propertyId;
        }

        /**
         * 関連申込IDを取得する。
         * @return 申込ID。
         */
        public Integer getApplicationId() {
            return applicationId;
        }

        /**
         * 関連申込IDを設定する。
         * @param applicationId 申込ID。
         */
        public void setApplicationId(Integer applicationId) {
            this.applicationId = applicationId;
        }

        /**
         * 関連契約IDを取得する。
         * @return 契約ID。
         */
        public Integer getTenancyId() {
            return tenancyId;
        }

        /**
         * 関連契約IDを設定する。
         * @param tenancyId 契約ID。
         */
        public void setTenancyId(Integer tenancyId) {
            this.tenancyId = tenancyId;
        }

        /**
         * 関連アンケートIDまたはクーポンIDを取得する。
         * @return 関連ID。
         */
        public Integer getRelatedId() {
            return relatedId;
        }

        /**
         * 関連アンケートIDまたはクーポンIDを設定する。
         * @param relatedId 関連ID。
         */
        public void setRelatedId(Integer relatedId) {
            this.relatedId = relatedId;
        }

        /**
         * オファーの相手会員IDを取得する。
         * @return 相手会員ID。
         */
        public Integer getPartnerUserId() {
            return partnerUserId;
        }

        /**
         * オファーの相手会員IDを設定する。
         * @param partnerUserId 相手会員ID。
         */
        public void setPartnerUserId(Integer partnerUserId) {
            this.partnerUserId = partnerUserId;
        }

        /**
         * 主な名称・タイトルを取得する。
         * @return 名称・タイトル。
         */
        public String getTitle() {
            return title;
        }

        /**
         * 主な名称・タイトルを設定する。
         * @param title 名称・タイトル。
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * 補足名称（店舗名・相手表示名など）を取得する。
         * @return 補足名称。
         */
        public String getSubtitle() {
            return subtitle;
        }

        /**
         * 補足名称を設定する。
         * @param subtitle 補足名称。
         */
        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        /**
         * 詳細テキストを取得する。
         * @return 詳細テキスト。
         */
        public String getDetailText() {
            return detailText;
        }

        /**
         * 詳細テキストを設定する。
         * @param detailText 詳細テキスト。
         */
        public void setDetailText(String detailText) {
            this.detailText = detailText;
        }

        /**
         * オファーの方向ラベルを取得する。
         * @return 方向ラベル。
         */
        public String getDirection() {
            return direction;
        }

        /**
         * オファーの方向ラベルを設定する。
         * @param direction 方向ラベル。
         */
        public void setDirection(String direction) {
            this.direction = direction;
        }

        /**
         * 主な状態テキストを取得する。
         * @return 状態テキスト。
         */
        public String getStatusText() {
            return statusText;
        }

        /**
         * 主な状態テキストを設定する。
         * @param statusText 状態テキスト。
         */
        public void setStatusText(String statusText) {
            this.statusText = statusText;
        }

        /**
         * 補助状態テキストを取得する。
         * @return 補助状態テキスト。
         */
        public String getSecondaryStatusText() {
            return secondaryStatusText;
        }

        /**
         * 補助状態テキストを設定する。
         * @param secondaryStatusText 補助状態テキスト。
         */
        public void setSecondaryStatusText(String secondaryStatusText) {
            this.secondaryStatusText = secondaryStatusText;
        }

        /**
         * 満足度スコアを取得する。
         * @return 満足度スコア。
         */
        public Integer getRating() {
            return rating;
        }

        /**
         * 満足度スコアを設定する。
         * @param rating 満足度スコア。
         */
        public void setRating(Integer rating) {
            this.rating = rating;
        }

        /**
         * 件数1を取得する。
         * @return 件数1。
         */
        public Long getPrimaryCount() {
            return primaryCount;
        }

        /**
         * 件数1を設定する。
         * @param primaryCount 件数1。
         */
        public void setPrimaryCount(Long primaryCount) {
            this.primaryCount = primaryCount;
        }

        /**
         * 件数2を取得する。
         * @return 件数2。
         */
        public Long getSecondaryCount() {
            return secondaryCount;
        }

        /**
         * 件数2を設定する。
         * @param secondaryCount 件数2。
         */
        public void setSecondaryCount(Long secondaryCount) {
            this.secondaryCount = secondaryCount;
        }

        /**
         * 開始日または予定日を取得する。
         * @return 開始日。
         */
        public LocalDate getStartDate() {
            return startDate;
        }

        /**
         * 開始日または予定日を設定する。
         * @param startDate 開始日。
         */
        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        /**
         * 終了日を取得する。
         * @return 終了日。
         */
        public LocalDate getEndDate() {
            return endDate;
        }

        /**
         * 終了日を設定する。
         * @param endDate 終了日。
         */
        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        /**
         * 日時1を取得する。
         * @return 日時1。
         */
        public OffsetDateTime getPrimaryTime() {
            return primaryTime;
        }

        /**
         * 日時1を設定する。
         * @param primaryTime 日時1。
         */
        public void setPrimaryTime(OffsetDateTime primaryTime) {
            this.primaryTime = primaryTime;
        }

        /**
         * 日時2を取得する。
         * @return 日時2。
         */
        public OffsetDateTime getSecondaryTime() {
            return secondaryTime;
        }

        /**
         * 日時2を設定する。
         * @param secondaryTime 日時2。
         */
        public void setSecondaryTime(OffsetDateTime secondaryTime) {
            this.secondaryTime = secondaryTime;
        }

        /**
         * 日時3を取得する。
         * @return 日時3。
         */
        public OffsetDateTime getTertiaryTime() {
            return tertiaryTime;
        }

        /**
         * 日時3を設定する。
         * @param tertiaryTime 日時3。
         */
        public void setTertiaryTime(OffsetDateTime tertiaryTime) {
            this.tertiaryTime = tertiaryTime;
        }

        /**
         * 有効期限日時を取得する。
         * @return 有効期限日時。
         */
        public OffsetDateTime getValidUntil() {
            return validUntil;
        }

        /**
         * 有効期限日時を設定する。
         * @param validUntil 有効期限日時。
         */
        public void setValidUntil(OffsetDateTime validUntil) {
            this.validUntil = validUntil;
        }
    }
}
