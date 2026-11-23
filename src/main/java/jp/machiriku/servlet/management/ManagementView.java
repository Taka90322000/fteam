package jp.machiriku.servlet.management;

/**
 * Actionが選んだJSPと、その画面が読み取るデータを共通処理に渡す。
 * @param jsp 管理画面のビュー用ディレクトリからの相対パス。Action側で決めた固定値。
 * @param title ページの見出しとブラウザのタイトルに使う文字列。
 * @param model 各JSPが受け取る画面データ。
 */
record ManagementView(String jsp, String title, Object model) {
    /**
     * Servletコンテキスト内のビューへのパスを取得する。
     * @return forwardに使用するJSPのパス。
     */
    String path() {
        return "/WEB-INF/views/management/" + jsp;
    }
}
