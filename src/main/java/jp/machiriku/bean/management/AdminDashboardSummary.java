package jp.machiriku.bean.management;

/** 運営ダッシュボードに表示する対応状況と運用状況の集計件数。 */
public class AdminDashboardSummary {
    /** 未対応の通報件数。 */
    private long openReports;
    /** 確認中の通報件数。 */
    private long reviewingReports;
    /** 管理者の返信待ちの問い合わせ件数。 */
    private long waitingAdminInquiries;
    /** 対応中の問い合わせ件数。 */
    private long inProgressInquiries;
    /** 利用中（ACTIVE）の会員数。 */
    private long activeUsers;
    /** 公開中の物件数。 */
    private long visibleProperties;
    /** 受付中のアンケート数。 */
    private long openSurveys;
    /** 配布期間中の有効なクーポン数。 */
    private long activeCoupons;

    /** 空の集計データを生成する。 */
    public AdminDashboardSummary() { }

    /**
     * 未対応の通報件数を取得する。
     * @return 未対応の通報件数。
     */
    public long getOpenReports() {
        return openReports;
    }

    /**
     * 未対応の通報件数を設定する。
     * @param openReports 未対応の通報件数。
     */
    public void setOpenReports(long openReports) {
        this.openReports = openReports;
    }

    /**
     * 確認中の通報件数を取得する。
     * @return 確認中の通報件数。
     */
    public long getReviewingReports() {
        return reviewingReports;
    }

    /**
     * 確認中の通報件数を設定する。
     * @param reviewingReports 確認中の通報件数。
     */
    public void setReviewingReports(long reviewingReports) {
        this.reviewingReports = reviewingReports;
    }

    /**
     * 返信待ちの問い合わせ件数を取得する。
     * @return 返信待ちの問い合わせ件数。
     */
    public long getWaitingAdminInquiries() {
        return waitingAdminInquiries;
    }

    /**
     * 返信待ちの問い合わせ件数を設定する。
     * @param waitingAdminInquiries 返信待ちの問い合わせ件数。
     */
    public void setWaitingAdminInquiries(long waitingAdminInquiries) {
        this.waitingAdminInquiries = waitingAdminInquiries;
    }

    /**
     * 対応中の問い合わせ件数を取得する。
     * @return 対応中の問い合わせ件数。
     */
    public long getInProgressInquiries() {
        return inProgressInquiries;
    }

    /**
     * 対応中の問い合わせ件数を設定する。
     * @param inProgressInquiries 対応中の問い合わせ件数。
     */
    public void setInProgressInquiries(long inProgressInquiries) {
        this.inProgressInquiries = inProgressInquiries;
    }

    /**
     * 利用中の会員数を取得する。
     * @return 利用中の会員数。
     */
    public long getActiveUsers() {
        return activeUsers;
    }

    /**
     * 利用中の会員数を設定する。
     * @param activeUsers 利用中の会員数。
     */
    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    /**
     * 掲載中の物件数を取得する。
     * @return 掲載中の物件数。
     */
    public long getVisibleProperties() {
        return visibleProperties;
    }

    /**
     * 掲載中の物件数を設定する。
     * @param visibleProperties 掲載中の物件数。
     */
    public void setVisibleProperties(long visibleProperties) {
        this.visibleProperties = visibleProperties;
    }

    /**
     * 受付中のアンケート数を取得する。
     * @return 受付中のアンケート数。
     */
    public long getOpenSurveys() {
        return openSurveys;
    }

    /**
     * 受付中のアンケート数を設定する。
     * @param openSurveys 受付中のアンケート数。
     */
    public void setOpenSurveys(long openSurveys) {
        this.openSurveys = openSurveys;
    }

    /**
     * 配布期間中のクーポン数を取得する。
     * @return 配布期間中のクーポン数。
     */
    public long getActiveCoupons() {
        return activeCoupons;
    }

    /**
     * 配布期間中のクーポン数を設定する。
     * @param activeCoupons 配布期間中のクーポン数。
     */
    public void setActiveCoupons(long activeCoupons) {
        this.activeCoupons = activeCoupons;
    }
}
