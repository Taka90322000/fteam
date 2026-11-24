package jp.machiriku.bean.management;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 管理者向けアンケート一覧・詳細画面で使用するアンケートのデータモデル。
 */
public class AdminSurveyItem {
    /** アンケートID。 */
    private int surveyId;
    /** アンケート名・テンプレート名。 */
    private String surveyTitle;
    /** 種別コード（POST_VISIT / TEMPLATE）。 */
    private String surveyKind;
    /** 受付状態コード（DRAFT / OPEN / CLOSED）。 */
    private String surveyStatus;
    /** テンプレート対象業種ID。 */
    private Integer industryTypeId;
    /** テンプレート対象業種名。 */
    private String industryName;
    /** 作成元テンプレートのアンケートID。 */
    private Integer sourceTemplateId;
    /** 改訂元アンケートのID。 */
    private Integer previousSurveyId;
    /** 来店アンケートの対象契約ID。 */
    private Integer tenancyId;
    /** 対象店舗名。 */
    private String storeName;
    /** 対象店舗の事業主会員ID。 */
    private Integer businessUserId;
    /** 受付開始日時。 */
    private OffsetDateTime startsAt;
    /** 受付終了日時。 */
    private OffsetDateTime endsAt;
    /** 回答件数。 */
    private long responseCount;
    /** 平均満足度（1〜5）。回答がない場合はnull。 */
    private BigDecimal averageRating;
    /** 最終更新日時。 */
    private OffsetDateTime updatedAt;

    /** デフォルトコンストラクター。 */
    public AdminSurveyItem() { }

    /**
     * アンケートIDを取得する。
     * @return アンケートID。
     */
    public int getSurveyId() {
        return surveyId;
    }

    /**
     * アンケートIDを設定する。
     * @param surveyId アンケートID。
     */
    public void setSurveyId(int surveyId) {
        this.surveyId = surveyId;
    }

    /**
     * アンケート名を取得する。
     * @return アンケート名。
     */
    public String getSurveyTitle() {
        return surveyTitle;
    }

    /**
     * アンケート名を設定する。
     * @param surveyTitle アンケート名。
     */
    public void setSurveyTitle(String surveyTitle) {
        this.surveyTitle = surveyTitle;
    }

    /**
     * 種別コードを取得する。
     * @return 種別コード。
     */
    public String getSurveyKind() {
        return surveyKind;
    }

    /**
     * 種別コードを設定する。
     * @param surveyKind 種別コード。
     */
    public void setSurveyKind(String surveyKind) {
        this.surveyKind = surveyKind;
    }

    /**
     * 種別の日本語表示名を取得する。
     * @return 種別の表示名。
     */
    public String getSurveyKindLabel() {
        return ManagementLabels.label(ManagementLabels.SURVEY_KINDS, surveyKind);
    }

    /**
     * テンプレートかどうかを判定する。
     * @return テンプレートの場合はtrue。
     */
    public boolean isTemplate() {
        return "TEMPLATE".equals(surveyKind);
    }

    /**
     * 受付状態コードを取得する。
     * @return 受付状態コード。
     */
    public String getSurveyStatus() {
        return surveyStatus;
    }

    /**
     * 受付状態コードを設定する。
     * @param surveyStatus 受付状態コード。
     */
    public void setSurveyStatus(String surveyStatus) {
        this.surveyStatus = surveyStatus;
    }

    /**
     * 受付状態の日本語表示名を取得する。
     * @return 受付状態の表示名。
     */
    public String getSurveyStatusLabel() {
        return ManagementLabels.label(ManagementLabels.SURVEY_STATES, surveyStatus);
    }

    /**
     * 回答の付いていない下書きであり編集可能かを判定する。
     * @return 編集可能な場合はtrue。
     */
    public boolean isEditable() {
        return "DRAFT".equals(surveyStatus) && responseCount == 0;
    }

    /**
     * 回答受付を停止できる状態かを判定する。
     * @return 受付中であればtrue。
     */
    public boolean isCanClose() {
        return "OPEN".equals(surveyStatus);
    }

    /**
     * 改訂版の下書きを作成できるかを判定する。
     * @return 公開済みまたは終了済みの来店アンケートであればtrue。
     */
    public boolean isCanRevise() {
        return "POST_VISIT".equals(surveyKind) && !"DRAFT".equals(surveyStatus);
    }

    /**
     * 対象業種IDを取得する。
     * @return 対象業種ID。
     */
    public Integer getIndustryTypeId() {
        return industryTypeId;
    }

    /**
     * 対象業種IDを設定する。
     * @param industryTypeId 対象業種ID。
     */
    public void setIndustryTypeId(Integer industryTypeId) {
        this.industryTypeId = industryTypeId;
    }

    /**
     * 対象業種名を取得する。
     * @return 対象業種名。
     */
    public String getIndustryName() {
        return industryName;
    }

    /**
     * 対象業種名を設定する。
     * @param industryName 対象業種名。
     */
    public void setIndustryName(String industryName) {
        this.industryName = industryName;
    }

    /**
     * 作成元テンプレートIDを取得する。
     * @return テンプレートID。
     */
    public Integer getSourceTemplateId() {
        return sourceTemplateId;
    }

    /**
     * 作成元テンプレートIDを設定する。
     * @param sourceTemplateId テンプレートID。
     */
    public void setSourceTemplateId(Integer sourceTemplateId) {
        this.sourceTemplateId = sourceTemplateId;
    }

    /**
     * 改訂元アンケートIDを取得する。
     * @return 改訂元アンケートID。
     */
    public Integer getPreviousSurveyId() {
        return previousSurveyId;
    }

    /**
     * 改訂元アンケートIDを設定する。
     * @param previousSurveyId 改訂元アンケートID。
     */
    public void setPreviousSurveyId(Integer previousSurveyId) {
        this.previousSurveyId = previousSurveyId;
    }

    /**
     * 対象契約IDを取得する。
     * @return 契約ID。
     */
    public Integer getTenancyId() {
        return tenancyId;
    }

    /**
     * 対象契約IDを設定する。
     * @param tenancyId 契約ID。
     */
    public void setTenancyId(Integer tenancyId) {
        this.tenancyId = tenancyId;
    }

    /**
     * 対象店舗名を取得する。
     * @return 店舗名。
     */
    public String getStoreName() {
        return storeName;
    }

    /**
     * 対象店舗名を設定する。
     * @param storeName 店舗名。
     */
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    /**
     * 対象事業主の会員IDを取得する。
     * @return 事業主会員ID。
     */
    public Integer getBusinessUserId() {
        return businessUserId;
    }

    /**
     * 対象事業主の会員IDを設定する。
     * @param businessUserId 事業主会員ID。
     */
    public void setBusinessUserId(Integer businessUserId) {
        this.businessUserId = businessUserId;
    }

    /**
     * 受付開始日時を取得する。
     * @return 受付開始日時。
     */
    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    /**
     * 受付開始日時を設定する。
     * @param startsAt 受付開始日時。
     */
    public void setStartsAt(OffsetDateTime startsAt) {
        this.startsAt = startsAt;
    }

    /**
     * 受付終了日時を取得する。
     * @return 受付終了日時。
     */
    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    /**
     * 受付終了日時を設定する。
     * @param endsAt 受付終了日時。
     */
    public void setEndsAt(OffsetDateTime endsAt) {
        this.endsAt = endsAt;
    }

    /**
     * 回答件数を取得する。
     * @return 回答件数。
     */
    public long getResponseCount() {
        return responseCount;
    }

    /**
     * 回答件数を設定する。
     * @param responseCount 回答件数。
     */
    public void setResponseCount(long responseCount) {
        this.responseCount = responseCount;
    }

    /**
     * 平均満足度を取得する。
     * @return 平均満足度。
     */
    public BigDecimal getAverageRating() {
        return averageRating;
    }

    /**
     * 平均満足度を設定する。
     * @param averageRating 平均満足度。
     */
    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    /**
     * 最終更新日時を取得する。
     * @return 更新日時。
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 最終更新日時を設定する。
     * @param updatedAt 更新日時。
     */
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** アンケートの追加設問1件と選択肢テキストを表すデータモデル。 */
    public static class Question {
        /** 設問番号（2〜10）。 */
        private int questionNo;
        /** 設問文。 */
        private String questionText;
        /** 回答形式コード（SINGLE / TEXT）。 */
        private String answerKind = "SINGLE";
        /** 必須設問の場合はtrue。 */
        private boolean required;
        /** 改行区切りの選択肢テキスト。 */
        private String optionsText;

        /** デフォルトコンストラクター。 */
        public Question() { }

        /**
         * 設問番号を取得する。
         * @return 設問番号。
         */
        public int getQuestionNo() {
            return questionNo;
        }

        /**
         * 設問番号を設定する。
         * @param questionNo 設問番号。
         */
        public void setQuestionNo(int questionNo) {
            this.questionNo = questionNo;
        }

        /**
         * 設問文を取得する。
         * @return 設問文。
         */
        public String getQuestionText() {
            return questionText;
        }

        /**
         * 設問文を設定する。
         * @param questionText 設問文。
         */
        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        /**
         * 回答形式コードを取得する。
         * @return 回答形式コード。
         */
        public String getAnswerKind() {
            return answerKind;
        }

        /**
         * 回答形式コードを設定する。
         * @param answerKind 回答形式コード。
         */
        public void setAnswerKind(String answerKind) {
            this.answerKind = answerKind;
        }

        /**
         * 回答形式の日本語表示名を取得する。
         * @return 回答形式の表示名。
         */
        public String getAnswerKindLabel() {
            if ("SINGLE".equals(answerKind)) {
                return "単一選択";
            }
            if ("TEXT".equals(answerKind)) {
                return "自由記述";
            }
            return answerKind;
        }

        /**
         * 必須区分を取得する。
         * @return 必須の場合はtrue。
         */
        public boolean isRequired() {
            return required;
        }

        /**
         * 必須区分を設定する。
         * @param required 必須の場合はtrue。
         */
        public void setRequired(boolean required) {
            this.required = required;
        }

        /**
         * 改行区切りの選択肢テキストを取得する。
         * @return 選択肢テキスト。
         */
        public String getOptionsText() {
            return optionsText;
        }

        /**
         * 改行区切りの選択肢テキストを設定する。
         * @param optionsText 選択肢テキスト。
         */
        public void setOptionsText(String optionsText) {
            this.optionsText = optionsText;
        }
    }

    /** アンケートの満足度スコア（1〜5）ごとの回答件数を表す集計モデル。 */
    public static class RatingCount {
        /** 満足度スコア（1〜5）。 */
        private int rating;
        /** 該当スコアの回答件数。 */
        private long responseCount;

        /** デフォルトコンストラクター。 */
        public RatingCount() { }

        /**
         * 満足度スコアを取得する。
         * @return 満足度スコア。
         */
        public int getRating() {
            return rating;
        }

        /**
         * 満足度スコアを設定する。
         * @param rating 満足度スコア。
         */
        public void setRating(int rating) {
            this.rating = rating;
        }

        /**
         * 回答件数を取得する。
         * @return 回答件数。
         */
        public long getResponseCount() {
            return responseCount;
        }

        /**
         * 回答件数を設定する。
         * @param responseCount 回答件数。
         */
        public void setResponseCount(long responseCount) {
            this.responseCount = responseCount;
        }
    }

    /** アンケートの単一選択設問における選択肢ごとの回答件数を表す集計モデル。 */
    public static class OptionCount {
        /** 設問番号。 */
        private int questionNo;
        /** 設問文。 */
        private String questionText;
        /** 選択肢の文言。 */
        private String optionText;
        /** 選択された回答件数。 */
        private long responseCount;

        /** デフォルトコンストラクター。 */
        public OptionCount() { }

        /**
         * 設問番号を取得する。
         * @return 設問番号。
         */
        public int getQuestionNo() {
            return questionNo;
        }

        /**
         * 設問番号を設定する。
         * @param questionNo 設問番号。
         */
        public void setQuestionNo(int questionNo) {
            this.questionNo = questionNo;
        }

        /**
         * 設問文を取得する。
         * @return 設問文。
         */
        public String getQuestionText() {
            return questionText;
        }

        /**
         * 設問文を設定する。
         * @param questionText 設問文。
         */
        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        /**
         * 選択肢の文言を取得する。
         * @return 選択肢の文言。
         */
        public String getOptionText() {
            return optionText;
        }

        /**
         * 選択肢の文言を設定する。
         * @param optionText 選択肢の文言。
         */
        public void setOptionText(String optionText) {
            this.optionText = optionText;
        }

        /**
         * 選択された回答件数を取得する。
         * @return 回答件数。
         */
        public long getResponseCount() {
            return responseCount;
        }

        /**
         * 選択された回答件数を設定する。
         * @param responseCount 回答件数。
         */
        public void setResponseCount(long responseCount) {
            this.responseCount = responseCount;
        }
    }

    /** アンケートの個別回答履歴1件を表すデータモデル。 */
    public static class Response {
        /** 回答ID。 */
        private long responseId;
        /** 回答した地域住民の会員ID。 */
        private int residentUserId;
        /** 回答者の表示名。 */
        private String residentName;
        /** 総合満足度（1〜5）。 */
        private int rating;
        /** 回答時点の年齢区分スナップショット。 */
        private String ageGroup;
        /** 回答時点の市区町村スナップショット。 */
        private String municipality;
        /** 回答日時。 */
        private OffsetDateTime respondedAt;
        /** 追加設問への回答内容をまとめた文字列。 */
        private String answersSummary;

        /** デフォルトコンストラクター。 */
        public Response() { }

        /**
         * 回答IDを取得する。
         * @return 回答ID。
         */
        public long getResponseId() {
            return responseId;
        }

        /**
         * 回答IDを設定する。
         * @param responseId 回答ID。
         */
        public void setResponseId(long responseId) {
            this.responseId = responseId;
        }

        /**
         * 回答者の会員IDを取得する。
         * @return 回答者会員ID。
         */
        public int getResidentUserId() {
            return residentUserId;
        }

        /**
         * 回答者の会員IDを設定する。
         * @param residentUserId 回答者会員ID。
         */
        public void setResidentUserId(int residentUserId) {
            this.residentUserId = residentUserId;
        }

        /**
         * 回答者の表示名を取得する。
         * @return 回答者表示名。
         */
        public String getResidentName() {
            return residentName;
        }

        /**
         * 回答者の表示名を設定する。
         * @param residentName 回答者表示名。
         */
        public void setResidentName(String residentName) {
            this.residentName = residentName;
        }

        /**
         * 総合満足度を取得する。
         * @return 満足度スコア。
         */
        public int getRating() {
            return rating;
        }

        /**
         * 総合満足度を設定する。
         * @param rating 満足度スコア。
         */
        public void setRating(int rating) {
            this.rating = rating;
        }

        /**
         * 回答時点の年齢区分を取得する。
         * @return 年齢区分。
         */
        public String getAgeGroup() {
            return ageGroup;
        }

        /**
         * 回答時点の年齢区分を設定する。
         * @param ageGroup 年齢区分。
         */
        public void setAgeGroup(String ageGroup) {
            this.ageGroup = ageGroup;
        }

        /**
         * 回答時点の市区町村を取得する。
         * @return 市区町村。
         */
        public String getMunicipality() {
            return municipality;
        }

        /**
         * 回答時点の市区町村を設定する。
         * @param municipality 市区町村。
         */
        public void setMunicipality(String municipality) {
            this.municipality = municipality;
        }

        /**
         * 回答日時を取得する。
         * @return 回答日時。
         */
        public OffsetDateTime getRespondedAt() {
            return respondedAt;
        }

        /**
         * 回答日時を設定する。
         * @param respondedAt 回答日時。
         */
        public void setRespondedAt(OffsetDateTime respondedAt) {
            this.respondedAt = respondedAt;
        }

        /**
         * 追加設問への回答内容まとめを取得する。
         * @return 回答内容の文字列。
         */
        public String getAnswersSummary() {
            return answersSummary;
        }

        /**
         * 追加設問への回答内容まとめを設定する。
         * @param answersSummary 回答内容の文字列。
         */
        public void setAnswersSummary(String answersSummary) {
            this.answersSummary = answersSummary;
        }
    }
}
