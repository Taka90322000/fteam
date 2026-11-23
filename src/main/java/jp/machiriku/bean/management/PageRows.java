package jp.machiriku.bean.management;

import java.util.List;
import java.util.Map;

/**
 * 一覧画面に渡す現在のページの行と移動先。
 * @param rows 現在のページに表示するレコード。
 * @param previous 前のページのAction URL。存在しなければnull。
 * @param next 次のページのAction URL。存在しなければnull。
 */
public record PageRows(List<Map<String, Object>> rows, String previous, String next) {
    /**
     * 表示する一ページ分の行と、存在する隣のページのURLを保持する。
     * @param rows 現在のページに表示するレコード。
     * @param previous 前のページのAction URL。存在しなければnull。
     * @param next 次のページのAction URL。存在しなければnull。
     */
    public PageRows {
        // 呼び出し元が持つ一覧を後から変更しても表示結果に影響させない。
        rows = List.copyOf(rows);
    }
}
