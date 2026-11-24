package jp.machiriku.bean.management;

import java.time.OffsetDateTime;

/** 問い合わせ一覧および詳細画面で表示する問い合わせデータ。 */
public class InquiryItem {
    /** 受付番号。 */
    private long inquiryId;
    /** 件名。 */
    private String subject;
    /** カテゴリコード。 */
    private String category;
    /** 対応状態コード。 */
    private String status;
    /** 受付日時。 */
    private OffsetDateTime createdAt;
    /** 更新日時。 */
    private OffsetDateTime updatedAt;
    /** 関連物件ID。未指定時はnull。 */
    private Integer relatedPropertyId;
    /** 関連物件名。 */
    private String relatedPropertyName;
    /** 送信者会員ID（管理者画面用）。 */
    private Integer requesterUserId;
    /** 送信者名（管理者画面用）。 */
    private String requesterName;
    /** 担当管理者ID（管理者画面用）。 */
    private Integer assignedAdminUserId;
    /** 担当者名（管理者画面用）。 */
    private String assignedAdminName;

    /** 空の問い合わせデータを生成する。 */
    public InquiryItem() { }

    /**
     * 受付番号を取得する。
     * @return 受付番号。
     */
    public long getInquiryId() {
        return inquiryId;
    }

    /**
     * 受付番号を設定する。
     * @param inquiryId 受付番号。
     */
    public void setInquiryId(long inquiryId) {
        this.inquiryId = inquiryId;
    }

    /**
     * 件名を取得する。
     * @return 件名。
     */
    public String getSubject() {
        return subject;
    }

    /**
     * 件名を設定する。
     * @param subject 件名。
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * カテゴリコードを取得する。
     * @return カテゴリコード。
     */
    public String getCategory() {
        return category;
    }

    /**
     * カテゴリコードを設定する。
     * @param category カテゴリコード。
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * カテゴリの日本語表示名を取得する。
     * @return カテゴリの表示名。
     */
    public String getCategoryLabel() {
        return ManagementLabels.label(ManagementLabels.INQUIRY_CATEGORIES, category);
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
        return ManagementLabels.label(ManagementLabels.INQUIRY_STATES, status);
    }

    /**
     * 返信・状態更新が可能な状態（完了前）か判定する。
     * @return 完了（CLOSED）以外であればtrue。
     */
    public boolean isCanReply() {
        return !"CLOSED".equals(status);
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

    /**
     * 送信者会員IDを取得する。
     * @return 送信者会員ID。
     */
    public Integer getRequesterUserId() {
        return requesterUserId;
    }

    /**
     * 送信者会員IDを設定する。
     * @param requesterUserId 送信者会員ID。
     */
    public void setRequesterUserId(Integer requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    /**
     * 送信者名を取得する。
     * @return 送信者名。
     */
    public String getRequesterName() {
        return requesterName;
    }

    /**
     * 送信者名を設定する。
     * @param requesterName 送信者名。
     */
    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
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
     * 担当者名を取得する。
     * @return 担当者名。
     */
    public String getAssignedAdminName() {
        return assignedAdminName;
    }

    /**
     * 担当者名を設定する。
     * @param assignedAdminName 担当者名。
     */
    public void setAssignedAdminName(String assignedAdminName) {
        this.assignedAdminName = assignedAdminName;
    }

    /** 問い合わせスレッドの1件の投稿（本文・返信・管理者用メモ）を表すデータ。 */
    public static class Message {
        /** 投稿日時。 */
        private OffsetDateTime createdAt;
        /** 投稿者区分（利用者 / 運営）。 */
        private String senderLabel;
        /** 投稿種類（返信 / 管理者用メモ）。 */
        private String kindLabel;
        /** 投稿本文。 */
        private String body;

        /** 空の投稿データを生成する。 */
        public Message() { }

        /**
         * 投稿日時を取得する。
         * @return 投稿日時。
         */
        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        /**
         * 投稿日時を設定する。
         * @param createdAt 投稿日時。
         */
        public void setCreatedAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
        }

        /**
         * 投稿者区分を取得する。
         * @return 投稿者区分。
         */
        public String getSenderLabel() {
            return senderLabel;
        }

        /**
         * 投稿者区分を設定する。
         * @param senderLabel 投稿者区分。
         */
        public void setSenderLabel(String senderLabel) {
            this.senderLabel = senderLabel;
        }

        /**
         * 投稿種類を取得する。
         * @return 投稿種類。
         */
        public String getKindLabel() {
            return kindLabel;
        }

        /**
         * 投稿種類を設定する。
         * @param kindLabel 投稿種類。
         */
        public void setKindLabel(String kindLabel) {
            this.kindLabel = kindLabel;
        }

        /**
         * 本文を取得する。
         * @return 本文。
         */
        public String getBody() {
            return body;
        }

        /**
         * 本文を設定する。
         * @param body 本文。
         */
        public void setBody(String body) {
            this.body = body;
        }
    }
}
