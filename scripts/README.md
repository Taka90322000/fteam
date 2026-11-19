# ローカル起動

LINEログインとDBの接続設定はプロジェクト直下の `.env` に記入します。既存の `.env` がない場合は `.env.example` をコピーして作成してください。設定項目と読み込みルールは [LINEログインの設定](../docs/line-login.md) を参照してください。

`.env` を編集した後はTomcatを再起動してください。`.env` はGitに含めず、サーバー起動時のカレントディレクトリにかかわらずJava側から読み込みます。

VS Codeで表示したいファイルを保存して開き、`Ctrl+Shift+B` を押すと、ビルドしてTomcatを起動し、そのファイルに対応するページをブラウザーで開きます。

- `src/main/webapp/index.jsp` → `http://localhost:8080/machiriku/index.jsp`
- `LoginAction.java`、`WEB-INF/views/auth/login.jsp` → `http://localhost:8080/machiriku/jp/machiriku/servlet/auth/Login.action`
- `ResidentSetupAction.java`、`WEB-INF/views/auth/resident_setup.jsp` → `http://localhost:8080/machiriku/jp/machiriku/servlet/auth/ResidentSetup.action`
- `ProfileAction.java`、`WEB-INF/views/user/` の各プロフィールJSP → `http://localhost:8080/machiriku/jp/machiriku/servlet/user/Profile.action`（ログイン中の会員区分の画面）

ServletやActionは `@WebServlet`、`WEB-INF/web.xml`、または `FrontController` の `.action` ルーティングを参照します。`WEB-INF` 内のJSPは、そのパスを `getRequestDispatcher("/WEB-INF/...")` で指定するクラスが1つに絞れる場合に表示できます。複数のURLが指定されたServletは、最初のワイルドカードを含まないURLを使います。

JavaのBean・DAOなど、ページのURLを判定できないファイルの場合は、警告を出してトップページ `/machiriku/index.jsp` を開きます。起動確認も `/machiriku/index.jsp` で行います。

`deploy.ps1` は `target/dev-deploy` 内の新しいディレクトリにビルドを展開し、成功後にTomcatの参照先を切り替えます。切替後の起動確認に失敗すると、直前の参照先へ戻します。JSPや静的ファイルの変更も、ファイルを保存してから `Ctrl+Shift+B` で反映します。

コマンドから実行する場合も、ファイルを指定できます。

```powershell
.\scripts\deploy.ps1 -ActiveFile C:\fteam\machiriku\src\main\webapp\index.jsp
```
