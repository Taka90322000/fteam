package jp.machiriku.bean.management;

import java.util.List;

/**
 * 一覧画面の1ページ分の表示データと、前後ページへの移動URLを保持する。
 * @param <T> 一覧の1行を表すJavaBeanの型。
 */
public class PageResult<T> {
    /** 現在のページに表示するレコード一覧。 */
    private final List<T> items;
    /** 前のページへのAction URL。先頭ページの場合はnull。 */
    private final String previousUrl;
    /** 次のページへのAction URL。最終ページの場合はnull。 */
    private final String nextUrl;

    /**
     * 1ページ分の表示データと移動先URLを生成する。
     * @param items 現在のページに表示するデータ一覧。
     * @param previousUrl 前のページのURL。存在しない場合はnull。
     * @param nextUrl 次のページのURL。存在しない場合はnull。
     */
    public PageResult(List<T> items, String previousUrl, String nextUrl) {
        // 呼び出し元のリスト変更が画面表示へ影響しないよう不変リストにする。
        this.items = items == null ? List.of() : List.copyOf(items);
        this.previousUrl = previousUrl;
        this.nextUrl = nextUrl;
    }

    /**
     * 現在のページに表示する一覧を取得する。
     * @return レコード一覧。
     */
    public List<T> getItems() {
        return items;
    }

    /**
     * 前のページのURLを取得する。
     * @return 前のページのURL。存在しない場合はnull。
     */
    public String getPreviousUrl() {
        return previousUrl;
    }

    /**
     * 次のページのURLを取得する。
     * @return 次のページのURL。存在しない場合はnull。
     */
    public String getNextUrl() {
        return nextUrl;
    }

    /**
     * 前後のいずれかに移動可能なページがあるかを判定する。
     * @return ページ移動リンクを表示する場合はtrue。
     */
    public boolean isHasPager() {
        return previousUrl != null || nextUrl != null;
    }
}
