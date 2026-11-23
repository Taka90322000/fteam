package jp.machiriku.servlet.management;

import java.sql.SQLException;
import jp.machiriku.bean.management.AdminOverviewView;
import jp.machiriku.dao.management.AdminOverviewDao;
import jp.machiriku.dao.management.ManagementDao;

/** 運営ダッシュボードと操作履歴を画面モデルにまとめる。 */
final class AdminOverview {
    /** 一覧と集計を静的に組み立てる。 */
    private AdminOverview() { }

    /**
     * ダッシュボードに表示する対応状況と運用状況を集計する。
     * @param db 管理対象を集計するDAO接続。
     * @return ダッシュボードのJSPと件数データ。
     * @throws SQLException 集計に失敗した場合。
     */
    static ManagementView dashboard(ManagementDao db) throws SQLException {
        // 通報・問い合わせとサービス運用の件数を別の集計として取得する。
        var data = new AdminOverviewView.Dashboard(AdminOverviewDao.supportCounts(db), AdminOverviewDao.operationCounts(db));
        return new ManagementView("admin/dashboard.jsp", "運営ダッシュボード", data);
    }

    /**
     * 操作者・対象・理由で操作履歴を絞り込む。
     * @param db 操作履歴を照会するDAO接続。
     * @param input 検索語とページ。
     * @return 履歴のJSPと表示する一ページ分のデータ。
     * @throws SQLException 操作履歴を取得できなかった場合。
     */
    static ManagementView audit(ManagementDao db, ManagementRequest input) throws SQLException {
        // 検索欄の初期値と現在のページに表示する操作履歴を渡す。
        String query = input.search();
        var entries = input.pageRows(AdminOverviewDao.auditHistory(db, query, ManagementRequest.PAGE_SIZE + 1, input.offset()));
        return new ManagementView("admin/audit/list.jsp", "操作履歴", new AdminOverviewView.Audit(query, entries));
    }

}
