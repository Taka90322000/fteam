package jp.machiriku.bean.management;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 管理者向けクーポン一覧および詳細画面で使用するクーポンのデータモデル。
 */
public class AdminCouponItem {
    /** クーポンID。 */
    private int couponId;
    /** クーポン名。 */
    private String couponTitle;
    /** 発行店舗名。 */
    private String storeName;
    /** 対象契約ID。 */
    private int tenancyId;
    /** 発行事業主の会員ID。 */
    private int businessUserId;
    /** 配布状態コード（DRAFT / ACTIVE / STOPPED）。 */
    private String couponStatus;
    /** 特典種別コード（PERCENT / AMOUNT / SERVICE）。 */
    private String benefitKind;
    /** 割引率（割合割引の場合）。 */
    private BigDecimal discountPercent;
    /** 割引額・円（定額割引の場合）。 */
    private Integer discountYen;
    /** サービス特典の内容（サービスの場合）。 */
    private String serviceText;
    /** 配布対象コード（ALL_VOTERS / INDUSTRY_VOTERS / SURVEY）。 */
    private String targetKind;
    /** 対象業種名。 */
    private String targetIndustryName;
    /** 対象アンケートID。 */
    private Integer targetSurveyId;
    /** 配布開始日時。 */
    private OffsetDateTime distributionStartsAt;
    /** 配布終了日時。 */
    private OffsetDateTime distributionEndsAt;
    /** 利用期限日時。 */
    private OffsetDateTime validUntil;
    /** 発行枚数上限。 */
    private int usageLimit;
    /** 利用条件テキスト。 */
    private String termsText;
    /** 発行済み件数。 */
    private long issuedCount;
    /** 利用済み件数。 */
    private long usedCount;
    /** 無効化済み件数。 */
    private long revokedCount;

    /** デフォルトコンストラクター。 */
    public AdminCouponItem() { }

    /**
     * クーポンIDを取得する。
     * @return クーポンID。
     */
    public int getCouponId() {
        return couponId;
    }

    /**
     * クーポンIDを設定する。
     * @param couponId クーポンID。
     */
    public void setCouponId(int couponId) {
        this.couponId = couponId;
    }

    /**
     * クーポン名を取得する。
     * @return クーポン名。
     */
    public String getCouponTitle() {
        return couponTitle;
    }

    /**
     * クーポン名を設定する。
     * @param couponTitle クーポン名。
     */
    public void setCouponTitle(String couponTitle) {
        this.couponTitle = couponTitle;
    }

    /**
     * 発行店舗名を取得する。
     * @return 店舗名。
     */
    public String getStoreName() {
        return storeName;
    }

    /**
     * 発行店舗名を設定する。
     * @param storeName 店舗名。
     */
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    /**
     * 対象契約IDを取得する。
     * @return 契約ID。
     */
    public int getTenancyId() {
        return tenancyId;
    }

    /**
     * 対象契約IDを設定する。
     * @param tenancyId 契約ID。
     */
    public void setTenancyId(int tenancyId) {
        this.tenancyId = tenancyId;
    }

    /**
     * 事業主会員IDを取得する。
     * @return 事業主会員ID。
     */
    public int getBusinessUserId() {
        return businessUserId;
    }

    /**
     * 事業主会員IDを設定する。
     * @param businessUserId 事業主会員ID。
     */
    public void setBusinessUserId(int businessUserId) {
        this.businessUserId = businessUserId;
    }

    /**
     * 配布状態コードを取得する。
     * @return 配布状態コード。
     */
    public String getCouponStatus() {
        return couponStatus;
    }

    /**
     * 配布状態コードを設定する。
     * @param couponStatus 配布状態コード。
     */
    public void setCouponStatus(String couponStatus) {
        this.couponStatus = couponStatus;
    }

    /**
     * 配布状態の日本語表示名を取得する。
     * @return 配布状態の表示名。
     */
    public String getCouponStatusLabel() {
        return ManagementLabels.label(ManagementLabels.COUPON_STATES, couponStatus);
    }

    /**
     * 発行前の下書きであり編集可能かを判定する。
     * @return 編集可能な場合はtrue。
     */
    public boolean isEditable() {
        return "DRAFT".equals(couponStatus) && issuedCount == 0;
    }

    /**
     * 現在配布有効状態かを判定する。
     * @return ACTIVEの場合はtrue。
     */
    public boolean isActive() {
        return "ACTIVE".equals(couponStatus);
    }

    /**
     * 特典種別コードを取得する。
     * @return 特典種別コード。
     */
    public String getBenefitKind() {
        return benefitKind;
    }

    /**
     * 特典種別コードを設定する。
     * @param benefitKind 特典種別コード。
     */
    public void setBenefitKind(String benefitKind) {
        this.benefitKind = benefitKind;
    }

    /**
     * 特典種別の日本語表示名を取得する。
     * @return 特典種別の表示名。
     */
    public String getBenefitKindLabel() {
        return ManagementLabels.label(ManagementLabels.COUPON_BENEFITS, benefitKind);
    }

    /**
     * 割引率を取得する。
     * @return 割引率（%）。
     */
    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    /**
     * 割引率を設定する。
     * @param discountPercent 割引率（%）。
     */
    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
    }

    /**
     * 割引額を取得する。
     * @return 割引額（円）。
     */
    public Integer getDiscountYen() {
        return discountYen;
    }

    /**
     * 割引額を設定する。
     * @param discountYen 割引額（円）。
     */
    public void setDiscountYen(Integer discountYen) {
        this.discountYen = discountYen;
    }

    /**
     * サービス特典内容を取得する。
     * @return サービス内容。
     */
    public String getServiceText() {
        return serviceText;
    }

    /**
     * サービス特典内容を設定する。
     * @param serviceText サービス内容。
     */
    public void setServiceText(String serviceText) {
        this.serviceText = serviceText;
    }

    /**
     * 配布対象コードを取得する。
     * @return 配布対象コード。
     */
    public String getTargetKind() {
        return targetKind;
    }

    /**
     * 配布対象コードを設定する。
     * @param targetKind 配布対象コード。
     */
    public void setTargetKind(String targetKind) {
        this.targetKind = targetKind;
    }

    /**
     * 配布対象の日本語表示名を取得する。
     * @return 配布対象の表示名。
     */
    public String getTargetKindLabel() {
        return ManagementLabels.label(ManagementLabels.COUPON_TARGETS, targetKind);
    }

    /**
     * 対象業種名を取得する。
     * @return 対象業種名。
     */
    public String getTargetIndustryName() {
        return targetIndustryName;
    }

    /**
     * 対象業種名を設定する。
     * @param targetIndustryName 対象業種名。
     */
    public void setTargetIndustryName(String targetIndustryName) {
        this.targetIndustryName = targetIndustryName;
    }

    /**
     * 対象アンケートIDを取得する。
     * @return 対象アンケートID。
     */
    public Integer getTargetSurveyId() {
        return targetSurveyId;
    }

    /**
     * 対象アンケートIDを設定する。
     * @param targetSurveyId 対象アンケートID。
     */
    public void setTargetSurveyId(Integer targetSurveyId) {
        this.targetSurveyId = targetSurveyId;
    }

    /**
     * 配布開始日時を取得する。
     * @return 配布開始日時。
     */
    public OffsetDateTime getDistributionStartsAt() {
        return distributionStartsAt;
    }

    /**
     * 配布開始日時を設定する。
     * @param distributionStartsAt 配布開始日時。
     */
    public void setDistributionStartsAt(OffsetDateTime distributionStartsAt) {
        this.distributionStartsAt = distributionStartsAt;
    }

    /**
     * 配布終了日時を取得する。
     * @return 配布終了日時。
     */
    public OffsetDateTime getDistributionEndsAt() {
        return distributionEndsAt;
    }

    /**
     * 配布終了日時を設定する。
     * @param distributionEndsAt 配布終了日時。
     */
    public void setDistributionEndsAt(OffsetDateTime distributionEndsAt) {
        this.distributionEndsAt = distributionEndsAt;
    }

    /**
     * 利用期限日時を取得する。
     * @return 利用期限日時。
     */
    public OffsetDateTime getValidUntil() {
        return validUntil;
    }

    /**
     * 利用期限日時を設定する。
     * @param validUntil 利用期限日時。
     */
    public void setValidUntil(OffsetDateTime validUntil) {
        this.validUntil = validUntil;
    }

    /**
     * 発行枚数上限を取得する。
     * @return 発行枚数上限。
     */
    public int getUsageLimit() {
        return usageLimit;
    }

    /**
     * 発行枚数上限を設定する。
     * @param usageLimit 発行枚数上限。
     */
    public void setUsageLimit(int usageLimit) {
        this.usageLimit = usageLimit;
    }

    /**
     * 利用条件テキストを取得する。
     * @return 利用条件。
     */
    public String getTermsText() {
        return termsText;
    }

    /**
     * 利用条件テキストを設定する。
     * @param termsText 利用条件。
     */
    public void setTermsText(String termsText) {
        this.termsText = termsText;
    }

    /**
     * 発行済み件数を取得する。
     * @return 発行数。
     */
    public long getIssuedCount() {
        return issuedCount;
    }

    /**
     * 発行済み件数を設定する。
     * @param issuedCount 発行数。
     */
    public void setIssuedCount(long issuedCount) {
        this.issuedCount = issuedCount;
    }

    /**
     * 利用済み件数を取得する。
     * @return 利用数。
     */
    public long getUsedCount() {
        return usedCount;
    }

    /**
     * 利用済み件数を設定する。
     * @param usedCount 利用数。
     */
    public void setUsedCount(long usedCount) {
        this.usedCount = usedCount;
    }

    /**
     * 無効化済み件数を取得する。
     * @return 無効化数。
     */
    public long getRevokedCount() {
        return revokedCount;
    }

    /**
     * 無効化済み件数を設定する。
     * @param revokedCount 無効化数。
     */
    public void setRevokedCount(long revokedCount) {
        this.revokedCount = revokedCount;
    }

    /** クーポンの個別発行・利用・無効化履歴1件を表すデータモデル。 */
    public static class Recipient {
        /** 受取人の会員ID。 */
        private int residentUserId;
        /** 受取人の表示名。 */
        private String residentName;
        /** 発行日時。 */
        private OffsetDateTime issuedAt;
        /** 利用日時。未使用時はnull。 */
        private OffsetDateTime usedAt;
        /** 無効化日時。未無効化時はnull。 */
        private OffsetDateTime revokedAt;
        /** 店舗側で利用処理を行った会員ID。 */
        private Integer redeemedByUserId;
        /** 利用状態の日本語表示名（未使用 / 利用済み / 無効化済み / 期限切れ）。 */
        private String usageStateLabel;
        /** 現在有効期限内かどうか。 */
        private boolean withinValidity;

        /** デフォルトコンストラクター。 */
        public Recipient() { }

        /**
         * 受取人の会員IDを取得する。
         * @return 会員ID。
         */
        public int getResidentUserId() {
            return residentUserId;
        }

        /**
         * 受取人の会員IDを設定する。
         * @param residentUserId 会員ID。
         */
        public void setResidentUserId(int residentUserId) {
            this.residentUserId = residentUserId;
        }

        /**
         * 受取人の表示名を取得する。
         * @return 表示名。
         */
        public String getResidentName() {
            return residentName;
        }

        /**
         * 受取人の表示名を設定する。
         * @param residentName 表示名。
         */
        public void setResidentName(String residentName) {
            this.residentName = residentName;
        }

        /**
         * 発行日時を取得する。
         * @return 発行日時。
         */
        public OffsetDateTime getIssuedAt() {
            return issuedAt;
        }

        /**
         * 発行日時を設定する。
         * @param issuedAt 発行日時。
         */
        public void setIssuedAt(OffsetDateTime issuedAt) {
            this.issuedAt = issuedAt;
        }

        /**
         * 利用日時を取得する。
         * @return 利用日時。
         */
        public OffsetDateTime getUsedAt() {
            return usedAt;
        }

        /**
         * 利用日時を設定する。
         * @param usedAt 利用日時。
         */
        public void setUsedAt(OffsetDateTime usedAt) {
            this.usedAt = usedAt;
        }

        /**
         * 無効化日時を取得する。
         * @return 無効化日時。
         */
        public OffsetDateTime getRevokedAt() {
            return revokedAt;
        }

        /**
         * 無効化日時を設定する。
         * @param revokedAt 無効化日時。
         */
        public void setRevokedAt(OffsetDateTime revokedAt) {
            this.revokedAt = revokedAt;
        }

        /**
         * 利用処理を行った会員IDを取得する。
         * @return 利用処理者ID。
         */
        public Integer getRedeemedByUserId() {
            return redeemedByUserId;
        }

        /**
         * 利用処理を行った会員IDを設定する。
         * @param redeemedByUserId 利用処理者ID。
         */
        public void setRedeemedByUserId(Integer redeemedByUserId) {
            this.redeemedByUserId = redeemedByUserId;
        }

        /**
         * 利用状態の表示名を取得する。
         * @return 利用状態。
         */
        public String getUsageStateLabel() {
            return usageStateLabel;
        }

        /**
         * 利用状態の表示名を設定する。
         * @param usageStateLabel 利用状態。
         */
        public void setUsageStateLabel(String usageStateLabel) {
            this.usageStateLabel = usageStateLabel;
        }

        /**
         * 有効期間内かどうかを取得する。
         * @return 有効期間内ならtrue。
         */
        public boolean isWithinValidity() {
            return withinValidity;
        }

        /**
         * 有効期間内かどうかを設定する。
         * @param withinValidity 有効期間内ならtrue。
         */
        public void setWithinValidity(boolean withinValidity) {
            this.withinValidity = withinValidity;
        }

        /**
         * この発行分を管理者が無効化できる状態かを判定する。
         * @return 有効期間内の未使用・未無効化券であればtrue。
         */
        public boolean isCanRevoke() {
            return usedAt == null && revokedAt == null && withinValidity;
        }
    }
}
