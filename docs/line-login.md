# LINEログインの設定

プロジェクト直下の `.env` にLINEとDBの設定をまとめます。`.env` はGitの対象外です。共有用の `.env.example` には実際のパスワードやシークレットを記入しないでください。

今回、既存のDB用環境変数から `.env` へ値を移しています。別の環境では、まず `.env.example` を `.env` にコピーしてください（既存の `.env` は上書きしないでください）。

```powershell
Copy-Item .env.example .env
```

`.env` はUTF-8（BOMなし）で保存し、次の項目を設定します。

| 設定名 | 設定内容 |
| --- | --- |
| `MACHIRIKU_DB_URL` | PostgreSQLのJDBC URL（例：`jdbc:postgresql://localhost:5432/machiriku`） |
| `MACHIRIKU_DB_USER` | DB接続ユーザー |
| `MACHIRIKU_DB_PASSWORD` | DB接続パスワード |
| `LINE_CHANNEL_ID` | LINE LoginチャネルのチャネルID |
| `LINE_CHANNEL_SECRET` | 同じチャネルのチャネルシークレット |
| `LINE_REDIRECT_URI` | LINE Developersに登録するコールバックURLと完全に同じ値 |

ローカル開発用のコールバックURL：

```text
http://localhost:8080/machiriku/jp/machiriku/servlet/auth/LineCallback.action
```

公開環境ではHTTPSを使います。`.env` を変更したらTomcatを再起動し、VS Codeで `Ctrl+Shift+B` を実行してください。VS Codeの再起動は不要です。

`.env` にある項目はOSの環境変数より優先します。項目がなければ従来のOS環境変数を使用します。`KEY=` のような空欄は空の設定として扱うため、古いOSの値では補いません。

パスワードに `#` や空白を含む場合は、値をダブルクォートで囲んでください。値はPowerShellとして実行されず、変数展開も行いません。

```dotenv
MACHIRIKU_DB_PASSWORD="password with # and spaces"
```

## 読み込み場所

現行の開発構成では、Javaクラスの配置先からプロジェクトの `.env` を自動で見つけます。Tomcatを別の作業フォルダーから起動した場合も利用でき、起動スクリプトの変更は不要です。`.env` を `src/main/webapp` や `src/main/resources` に置かないでください。プロジェクト直下の `.env` はWARに含まれません。

WARを別のサーバーへ配置する場合は、サーバー内の `.env` の絶対パスを `MACHIRIKU_ENV_FILE` またはJavaの `-Dmachiriku.env.file=...` で指定できます。Javaのシステムプロパティが優先します。明示したファイルが存在しない場合は設定エラーになり、別ファイルへ自動で切り替えません。

LINE Developers Consoleで以下を設定します。

1. LINE Loginチャネルの「LINEログイン設定」にコールバックURLを登録する。
2. 「チャネル基本設定」のOpenID Connectでメールアドレス取得権限を申請する。申請用スクリーンショットには、取得する情報と利用目的を説明した会員登録画面を使用する。
3. 開発中のチャネルでは、テストするLINEアカウントをチャネルのメンバーに追加する。

設定の詳細は[LINE公式のWebログイン実装手順](https://developers.line.biz/ja/docs/line-login/integrate-line-login/)を参照してください。

## DBの利用権限

確認時点の `MACHIRIKU_DB_USER` には `public.users` の `SELECT`・`INSERT` 権限がありませんでした。この状態では実際のログイン・登録はDBで拒否されます。DB所有者または管理者が、アプリ用ロールを指定して同梱のSQLを実行してください。

```powershell
psql <管理者用の接続オプション> -v app_user=<MACHIRIKU_DB_USERの値> -f database/grant-user-access.sql
```

SQLは `public` スキーマの利用権限と、`users` の読取・登録、自動採番シーケンスの利用だけを付与します。既存の会員レコードやテーブル定義は変更しません。管理者権限をアプリの環境変数に設定する必要はありません。

## 登録とログイン

- 登録画面で地域住民・事業主・オーナーを選び、LINEへ進む。
- `openid profile email` を要求し、認可コードをサーバーで交換する。LINEのIDトークン検証APIで署名・対象チャネル・nonce・有効期限を検証してから表示名、メールアドレス、LINEのsubjectを使用する。
- メールアドレスを取得できない場合は登録せず、再試行の案内を表示する。LINE側のメール登録と、利用者のメール共有への同意が必要。
- 未登録の場合のみ `UserDao extends Dao` で `display_name`、`email`、`user_role`、`line_subject` を保存する。事業主・オーナーは既存DB制約により `phone_e164` も必須。電話番号は登録フォームからPOSTで受け付け、認証中はサーバーセッションに保持する。
- DBの利用区分は `RESIDENT`・`BUSINESS`・`OWNER`。既存画面との互換のため、セッションの `userRole` は小文字、`user` はDBから読み出した `User`、`userId` は会員IDとする。
- `user_id`、`account_status=PENDING`、作成・更新日時は既存DBの自動採番・デフォルトを使用する。`PENDING` と `ACTIVE` はログインでき、`BAN` と `WITHDRAWN` は拒否する。既存利用者の役割やステータスを登録フォームの値で変更しない。
- ログイン画面からの未登録アカウントは登録画面へ案内する。メールアドレスが同じでも、別のLINEアカウントを自動で既存会員に紐付けない。
- `state` はセッションに紐付けて10分間・1回限りで使用する。nonceとPKCE S256も検証し、ログイン成功時には新しいセッションを作成する。

デモ用のロール選択ログインは標準で無効です。審査用の隔離されたデモ環境で必要な場合のみ `.env` に `MACHIRIKU_DEMO_LOGIN_ENABLED=true` を設定します。実利用環境では有効にしないでください。

## 検証

```powershell
mvn test
```

実DBの接続を含むDAOテストは、`.env` の `MACHIRIKU_DB_URL`、`MACHIRIKU_DB_USER`、`MACHIRIKU_DB_PASSWORD` を使用します。テストの有効化は誤実行を避けるためOSの `MACHIRIKU_RUN_DB_TESTS` で指定します。DBテストは `users` の定義を複製した一時テーブルだけを使用し、ロールバックします。

```powershell
$env:MACHIRIKU_RUN_DB_TESTS = 'true'
mvn test
```
