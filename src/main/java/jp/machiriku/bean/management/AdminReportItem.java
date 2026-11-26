package jp.machiriku.bean.management;

import java.time.OffsetDateTime;

/** 管理者用の通報一覧・詳細画面および会員向け通報受付完了画面で共用する通報データ。 */
public class AdminReportItem {
    /** 受付番号。 */
    private long reportId;
    /** 通報者ID。 */
    private Integer reporterUserId;
    /** 通報者名。 */
    private String reporterName;
    /** 対象区分コード（PROPERTY / OWNER / BUSINESS）。 */
    private String targetKind;
    /** 対象会員ID（会員通報時のみ）。 */
    private Integer targetUserId;
    /** 対象物件ID（物件通報時のみ）。 */
    private Integer targetPropertyId;
    /** 関連物件ID（オーナー通報時のみ）。 */
    private Integer relatedPropertyId;
    /** 通報対象の表示名。 */
    private String targetName;
    /** 通報理由コード。 */
    private String reasonCode;
    /** 通報内容の詳細。 */
    private String description;
    /** 対応状態コード。 */
    private String status;
    /** 担当管理者ID。 */
    private Integer assignedAdminUserId;
    /** 担当管理者名。 */
    private String assignedAdminName;
    /** 受付日時。 */
    private OffsetDateTime createdAt;
    /** 対応結果コード。 */
    private String resolutionCode;
    /** 対応理由。 */
    private String resolutionNote;
    /** 完了日時。 */
    private OffsetDateTime resolvedAt;
    /** 同じ対象に対する未対応通報件数（一覧用）。 */
    private long openCountForSameTarget;

    /** 空の通報データを生成する。 */
    public AdminReportItem() { }

    /**
     * 受付番号を取得する。
     * @return 受付番号。
     */
    public long getReportId() {
        return reportId;
    }

    /**
     * 受付番号を設定する。
     * @param reportId 受付番号。
     */
    public void setReportId(long reportId) {
        this.reportId = reportId;
    }

    /**
     * 通報者IDを取得する。
     * @return 通報者ID。
     */
    public Integer getReporterUserId() {
        return reporterUserId;
    }

    /**
     * 通報者IDを設定する。
     * @param reporterUserId 通報者ID。
     */
    public void setReporterUserId(Integer reporterUserId) {
        this.reporterUserId = reporterUserId;
    }

    /**
     * 通報者名を取得する。
     * @return 通報者名。
     */
    public String getReporterName() {
        return reporterName;
    }

    /**
     * 通報者名を設定する。
     * @param reporterName 通報者名。
     */
    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    /**
     * 対象区分コードを取得する。
     * @return 対象区分コード。
     */
    public String getTargetKind() {
        return targetKind;
    }

    /**
     * 対象区分コードを設定する。
     * @param targetKind 対象区分コード。
     */
    public void setTargetKind(String targetKind) {
        this.targetKind = targetKind;
    }

    /**
     * 対象区分の日本語表示名を取得する。
     * @return 対象区分の表示名。
     */
    public String getTargetKindLabel() {
        return ManagementLabels.label(ManagementLabels.REPORT_TARGET_KINDS, targetKind);
    }

    /**
     * 対象会員IDを取得する。
     * @return 対象会員ID。
     */
    public Integer getTargetUserId() {
        return targetUserId;
    }

    /**
     * 対象会員IDを設定する。
     * @param targetUserId 対象会員ID。
     */
    public void setTargetUserId(Integer targetUserId) {
        this.targetUserId = targetUserId;
    }

    /**
     * 対象物件IDを取得する。
     * @return 対象物件ID。
     */
    public Integer getTargetPropertyId() {
        return targetPropertyId;
    }

    /**
     * 対象物件IDを設定する。
     * @param targetPropertyId 対象物件ID。
     */
    public void setTargetPropertyId(Integer targetPropertyId) {
        this.targetPropertyId = targetPropertyId;
    }

    /**
     * 関連物件IDを取得する。
     * @return 関連物件ID。
     */
    public Integer getRelatedPropertyId() {
        return relatedPropertyId;
    }

    /**
     * 関連物件IDを設定する。
     * @param relatedPropertyId 関連物件ID。
     */
    public void setRelatedPropertyId(Integer relatedPropertyId) {
        this.relatedPropertyId = relatedPropertyId;
    }

    /**
     * 通報対象の表示名を取得する。
     * @return 通報対象の表示名。
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * 通報対象の表示名を設定する。
     * @param targetName 通報対象の表示名。
     */
    public void setTargetName(String targetName) {
        this.targetName = targetName;
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
     * 通報理由の日本語表示名を取得する。
     * @return 通報理由の表示名。
     */
    public String getReasonLabel() {
        return ManagementLabels.label(ManagementLabels.REPORT_REASONS, reasonCode);
    }

    /**
     * 通報内容の詳細を取得する。
     * @return 詳細本文。
     */
    public String getDescription() {
        return description;
    }

    /**
     * 通報内容の詳細を設定する。
     * @param description 詳細本文。
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 対応状態コードを取得する。
     * @return 対応状態コード。
     */
    public String getStatus() {
        return status;
    }

    /**
     * 対応状態コードを設定する。
     * @param status 対応状態コード。
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 対応状態の日本語表示名を取得する。
     * @return 対応状態の表示名。
     */
    public String getStatusLabel() {
        return ManagementLabels.label(ManagementLabels.REPORT_STATES, status);
    }

    /**
     * 管理者が担当設定・対応完了を行える状態か判定する。
     * @return 完了（RESOLVED）以外であればtrue。
     */
    public boolean isEditable() {
        return !"RESOLVED".equals(status);
    }

    /**
     * 担当管理者IDを取得する。
     * @return 担当管理者ID。
     */
    public Integer getAssignedAdminUserId() {
        return assignedAdminUserId;
    }

    /**
     * 担当管理者IDを設定する。
     * @param assignedAdminUserId 担当管理者ID。
     */
    public void setAssignedAdminUserId(Integer assignedAdminUserId) {
        this.assignedAdminUserId = assignedAdminUserId;
    }

    /**
     * 担当管理者名を取得する。
     * @return 担当管理者名。
     */
    public String getAssignedAdminName() {
        return assignedAdminName;
    }

    /**
     * 担当管理者名を設定する。
     * @param assignedAdminName 担当管理者名。
     */
    public void setAssignedAdminName(String assignedAdminName) {
        this.assignedAdminName = assignedAdminName;
    }

    /**
     * 受付日時を取得する。
     * @return 受付日時。
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 受付日時を設定する。
     * @param createdAt 受付日時。
     */
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 対応結果コードを取得する。
     * @return 対応結果コード。
     */
    public String getResolutionCode() {
        return resolutionCode;
    }

    /**
     * 対応結果コードを設定する。
     * @param resolutionCode 対応結果コード。
     */
    public void setResolutionCode(String resolutionCode) {
        this.resolutionCode = resolutionCode;
    }

    /**
     * 対応結果の日本語表示名を取得する。
     * @return 対応結果の表示名。
     */
    public String getResolutionLabel() {
        return ManagementLabels.label(ManagementLabels.reportResolutionsFor(targetPropertyId != null), resolutionCode);
    }

    /**
     * 対応理由を取得する。
     * @return 対応理由。
     */
    public String getResolutionNote() {
        return resolutionNote;
    }

    /**
     * 対応理由を設定する。
     * @param resolutionNote 対応理由。
     */
    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    /**
     * 完了日時を取得する。
     * @return 完了日時。
     */
    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    /**
     * 完了日時を設定する。
     * @param resolvedAt 完了日時。
     */
    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    /**
     * 同じ対象への未対応通報件数を取得する。
     * @return 未対応件数。
     */
    public long getOpenCountForSameTarget() {
        return openCountForSameTarget;
    }

    /**
     * 同じ対象への未対応通報件数を設定する。
     * @param openCountForSameTarget 未対応件数。
     */
    public void setOpenCountForSameTarget(long openCountForSameTarget) {
        this.openCountForSameTarget = openCountForSameTarget;
    }

    /** 通報フォーム上部に表示する通報対象の確認情報。 */
    public static class Target {
        /** 通報対象の区分（PROPERTY / OWNER / BUSINESS）。 */
        private String kind;
        /** 通報対象のID（物件IDまたは会員ID）。 */
        private int targetId;
        /** オーナー通報時の関連物件ID。その他はnull。 */
        private Integer relatedPropertyId;
        /** 画面に表示する項目ラベル（物件名 / 事業者名 / 掲載者）。 */
        private String label;
        /** 画面に表示する対象名称。 */
        private String name;
        /** オーナー通報時に表示する関連物件名。その他はnull。 */
        private String relatedPropertyName;

        /** 空の通報対象情報を生成する。 */
        public Target() { }

        /**
         * 通報対象の区分を取得する。
         * @return 通報対象の区分。
         */
        public String getKind() {
            return kind;
        }

        /**
         * 通報対象の区分を設定する。
         * @param kind 通報対象の区分。
         */
        public void setKind(String kind) {
            this.kind = kind;
        }

        /**
         * 通報対象IDを取得する。
         * @return 通報対象ID。
         */
        public int getTargetId() {
            return targetId;
        }

        /**
         * 通報対象IDを設定する。
         * @param targetId 通報対象ID。
         */
        public void setTargetId(int targetId) {
            this.targetId = targetId;
        }

        /**
         * 関連物件IDを取得する。
         * @return 関連物件ID。
         */
        public Integer getRelatedPropertyId() {
            return relatedPropertyId;
        }

        /**
         * 関連物件IDを設定する。
         * @param relatedPropertyId 関連物件ID。
         */
        public void setRelatedPropertyId(Integer relatedPropertyId) {
            this.relatedPropertyId = relatedPropertyId;
        }

        /**
         * 表示項目ラベルを取得する。
         * @return 表示項目ラベル。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 表示項目ラベルを設定する。
         * @param label 表示項目ラベル。
         */
        public void setLabel(String label) {
            this.label = label;
        }

        /**
         * 通報対象の名称を取得する。
         * @return 対象名称。
         */
        public String getName() {
            return name;
        }

        /**
         * 通報対象の名称を設定する。
         * @param name 対象名称。
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * 関連物件名を取得する。
         * @return 関連物件名。
         */
        public String getRelatedPropertyName() {
            return relatedPropertyName;
        }

        /**
         * 関連物件名を設定する。
         * @param relatedPropertyName 関連物件名。
         */
        public void setRelatedPropertyName(String relatedPropertyName) {
            this.relatedPropertyName = relatedPropertyName;
        }
    }
}
