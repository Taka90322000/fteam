package jp.machiriku.bean.management;

/**
 * 業種管理画面の一覧・編集フォームで使用する業種および業種カテゴリのデータモデル。
 */
public class AdminIndustryItem {
    /** 業種IDまたは業種カテゴリID。 */
    private int id;
    /** 業種コード（I+数字4桁）またはカテゴリコード（英数字3文字）。 */
    private String code;
    /** 業種名またはカテゴリ名。 */
    private String name;
    /** 所属カテゴリID（業種の場合のみ使用）。 */
    private Integer categoryId;
    /** 所属カテゴリ名（業種一覧表示用）。 */
    private String categoryName;
    /** 表示順（昇順）。 */
    private int displayOrder = 1;
    /** 有効状態。 */
    private boolean active = true;
    /** 所属カテゴリの有効状態（業種一覧表示用）。 */
    private Boolean categoryActive;

    /** デフォルトコンストラクター。 */
    public AdminIndustryItem() { }

    /**
     * 業種IDまたはカテゴリIDを取得する。
     * @return 主キーID。
     */
    public int getId() {
        return id;
    }

    /**
     * 業種IDまたはカテゴリIDを設定する。
     * @param id 主キーID。
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * 業種コードまたはカテゴリコードを取得する。
     * @return コード文字列。
     */
    public String getCode() {
        return code;
    }

    /**
     * 業種コードまたはカテゴリコードを設定する。
     * @param code コード文字列。
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 業種名またはカテゴリ名を取得する。
     * @return 名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 業種名またはカテゴリ名を設定する。
     * @param name 名称。
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 所属カテゴリIDを取得する。
     * @return 所属カテゴリID。カテゴリ自身の場合はnull。
     */
    public Integer getCategoryId() {
        return categoryId;
    }

    /**
     * 所属カテゴリIDを設定する。
     * @param categoryId 所属カテゴリID。
     */
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * 所属カテゴリ名を取得する。
     * @return 所属カテゴリ名。
     */
    public String getCategoryName() {
        return categoryName;
    }

    /**
     * 所属カテゴリ名を設定する。
     * @param categoryName 所属カテゴリ名。
     */
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

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

    /**
     * 所属カテゴリの有効状態を取得する。
     * @return 所属カテゴリが有効な場合はtrue。
     */
    public Boolean getCategoryActive() {
        return categoryActive;
    }

    /**
     * 所属カテゴリの有効状態を設定する。
     * @param categoryActive 所属カテゴリの有効状態。
     */
    public void setCategoryActive(Boolean categoryActive) {
        this.categoryActive = categoryActive;
    }
}
