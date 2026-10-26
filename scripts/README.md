# ローカル起動

VS Codeで表示したいファイルを保存して開き、`Ctrl+Shift+B` を押すと、ビルドしてTomcatを起動し、そのファイルに対応するページをブラウザーで開きます。

- `src/main/webapp/index.jsp` → `http://localhost:8080/machiriku/index.jsp`
- `HelloServlet.java`、`WEB-INF/views/hello.jsp` → `http://localhost:8080/machiriku/hello`
- `DbCheckServlet.java` → `http://localhost:8080/machiriku/db-check`

Servletは `@WebServlet` または `WEB-INF/web.xml` のURL指定を参照します。`WEB-INF` 内のJSPは、そのパスを `getRequestDispatcher("/WEB-INF/...")` で指定するServletが1つに絞れる場合に表示できます。複数のURLが指定されたServletは、最初のワイルドカードを含まないURLを使います。

JavaのBean・DAOなど、ページのURLを判定できないファイルの場合は、警告を出して従来の `/machiriku/hello` を開きます。起動確認も引き続き `/machiriku/hello` で行います。

コマンドから実行する場合も、ファイルを指定できます。

```powershell
.\scripts\deploy.ps1 -ActiveFile C:\fteam\machiriku\src\main\java\jp\machiriku\servlet\DbCheckServlet.java
```
