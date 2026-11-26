package jp.machiriku.servlet.management;

/** JSP、画面モデル、変更後の遷移で共用する管理画面のAction URL。 */
public final class ManagementPaths {
    /** 管理Actionの所属パッケージに対応するURLの先頭部分。 */
    private static final String PACKAGE_PATH = "/jp/machiriku/servlet/management/";

    /** 管理者用ダッシュボード。 */
    public static final String ADMIN = PACKAGE_PATH + "Admin.action";

    /** 利用区分別の会員管理。 */
    public static final String ADMIN_USERS = PACKAGE_PATH + "AdminUsers.action";

    /** 業種とカテゴリの管理。 */
    public static final String ADMIN_INDUSTRIES = PACKAGE_PATH + "AdminIndustries.action";

    /** 資格・書類と必要条件の管理。 */
    public static final String ADMIN_QUALIFICATIONS = PACKAGE_PATH + "AdminQualifications.action";

    /** 物件の掲載管理。 */
    public static final String ADMIN_PROPERTIES = PACKAGE_PATH + "AdminProperties.action";

    /** アンケート管理。 */
    public static final String ADMIN_SURVEYS = PACKAGE_PATH + "AdminSurveys.action";

    /** クーポン管理。 */
    public static final String ADMIN_COUPONS = PACKAGE_PATH + "AdminCoupons.action";

    /** 管理者用の通報一覧・詳細。 */
    public static final String ADMIN_REPORTS = PACKAGE_PATH + "AdminReports.action";

    /** 管理者用の問い合わせ一覧・詳細。 */
    public static final String ADMIN_INQUIRIES = PACKAGE_PATH + "AdminInquiries.action";

    /** 出店申し込みの詳細。 */
    public static final String ADMIN_APPLICATIONS = PACKAGE_PATH + "AdminApplications.action";

    /** 契約情報。 */
    public static final String ADMIN_TENANCIES = PACKAGE_PATH + "AdminTenancies.action";

    /** 出店計画の詳細。 */
    public static final String ADMIN_PLANS = PACKAGE_PATH + "AdminPlans.action";

    /** 出店オファーの詳細。 */
    public static final String ADMIN_OFFERS = PACKAGE_PATH + "AdminOffers.action";

    /** 管理者の操作履歴。 */
    public static final String ADMIN_AUDIT = PACKAGE_PATH + "AdminAudit.action";

    /** 画面のURLを定数として利用する。 */
    private ManagementPaths() { }
}
