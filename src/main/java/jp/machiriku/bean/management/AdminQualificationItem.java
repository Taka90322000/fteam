package jp.machiriku.bean.management;

/**
 * 資格・許認可・書類マスター、業種別の必要条件、および公開案内一覧で共用するデータモデル。
 */
public class AdminQualificationItem {
    /** 資格・書類ID。 */
    private int qualificationId;
    /** 資格・書類の名称。 */
    private String qualificationName;
    /** 種類コード（QUALIFICATION / PERMIT / DOCUMENT）。 */
    private String requirementKind = "QUALIFICATION";
    /** 利用者向けの案内説明。 */
    private String guidanceText;
    /** 案内・様式ページのURL。 */
    private String guidanceUrl;
    /** マスターの有効状態。 */
    private boolean active = true;
    /** 業種別の必要条件における対象業種名（公開案内用）。 */
    private String industryName;
    /** 業種別の必要条件における必須区分（true: 必須、false: 任意）。 */
    private boolean required = true;
    /** 業種別の必要条件における適用条件の補足文。 */
    private String conditionText;

    /** デフォルトコンストラクター。 */
    public AdminQualificationItem() { }

    /**
     * 資格・書類IDを取得する。
     * @return 資格・書類ID。
     */
    public int getQualificationId() {
        return qualificationId;
    }

    /**
     * 資格・書類IDを設定する。
     * @param qualificationId 資格・書類ID。
     */
    public void setQualificationId(int qualificationId) {
        this.qualificationId = qualificationId;
    }

    /**
     * 資格・書類の名称を取得する。
     * @return 名称。
     */
    public String getQualificationName() {
        return qualificationName;
    }

    /**
     * 資格・書類の名称を設定する。
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
     * 利用者向けの案内説明を取得する。
     * @return 説明文。
     */
    public String getGuidanceText() {
        return guidanceText;
    }

    /**
     * 利用者向けの案内説明を設定する。
     * @param guidanceText 説明文。
     */
    public void setGuidanceText(String guidanceText) {
        this.guidanceText = guidanceText;
    }

    /**
     * 案内・様式ページのURLを取得する。
     * @return URL文字列。
     */
    public String getGuidanceUrl() {
        return guidanceUrl;
    }

    /**
     * 案内・様式ページのURLを設定する。
     * @param guidanceUrl URL文字列。
     */
    public void setGuidanceUrl(String guidanceUrl) {
        this.guidanceUrl = guidanceUrl;
    }

    /**
     * マスターの有効状態を取得する。
     * @return 有効な場合はtrue。
     */
    public boolean isActive() {
        return active;
    }

    /**
     * マスターの有効状態を設定する。
     * @param active 有効な場合はtrue。
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * 対象業種名を取得する。
     * @return 業種名。
     */
    public String getIndustryName() {
        return industryName;
    }

    /**
     * 対象業種名を設定する。
     * @param industryName 業種名。
     */
    public void setIndustryName(String industryName) {
        this.industryName = industryName;
    }

    /**
     * 必須区分を取得する。
     * @return 必須の場合はtrue、任意の場合はfalse。
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
     * 必須区分の日本語表示名（必須 / 任意）を取得する。
     * @return 必要区分の表示名。
     */
    public String getRequiredLabel() {
        return required ? "必須" : "任意";
    }

    /**
     * 適用条件を取得する。
     * @return 適用条件の文章。
     */
    public String getConditionText() {
        return conditionText;
    }

    /**
     * 適用条件を設定する。
     * @param conditionText 適用条件の文章。
     */
    public void setConditionText(String conditionText) {
        this.conditionText = conditionText;
    }
}
