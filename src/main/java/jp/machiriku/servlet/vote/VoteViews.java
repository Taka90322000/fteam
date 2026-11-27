package jp.machiriku.servlet.vote;

/**
 * 投票機能のActionが forward する画面(JSP)の場所を、ここ1か所にまとめる。
 * <p>画面(JSP)の置き場所やファイル名を変えるときは、このファイルの定数だけを書き換える。
 * Action・テストはすべてこの定数を使っているので、ほかのコードは直さなくてよい。</p>
 * <p>画面側が受け取れる値(request属性)と、送るフォームの項目は docs/vote-api.md の「8. 画面を作る人へ」を参照。</p>
 */
final class VoteViews {
    private static final String DIR = "/WEB-INF/views/vote/";

    /** 投票画面(入力フォーム。入力エラー時の再表示にも使う)。 */
    static final String FORM = DIR + "vote_form.jsp";
    /** 投票完了画面。 */
    static final String DONE = DIR + "vote_done.jsp";
    /** 自分の投票履歴。 */
    static final String HISTORY = DIR + "vote_history.jsp";
    /** 需要の集計(物件別・エリア別)。 */
    static final String DEMAND = DIR + "demand.jsp";

    private VoteViews() { }
}
