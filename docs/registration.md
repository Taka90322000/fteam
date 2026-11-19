# LINE連携後の住民・事業主登録

LINE認証が成功すると、今回取得した表示名とメールを認証済みセッションに保持します。メールを取得できなかった場合は空欄にし、仮のメールや氏名は使用しません。

- 住民画面：LINEの表示名をニックネームへ、LINEのメールをメール欄へ初期表示します。
- 事業主画面：LINEのメールだけを初期表示し、屋号と代表者名は本人が入力します。
- オーナー画面：LINEの表示名・任意のメールを初期表示し、氏名・法人名・電話番号・誘致希望業種を登録します。
- エラーで再表示するときは、LINE取得値ではなく直前の入力値を表示します。

## 保存先

| 入力内容 | 保存先 |
| --- | --- |
| 住民のニックネーム・任意メール | `users.display_name`・`users.email` |
| 住民の年齢区分・性別 | `resident_profiles` |
| 住民の居住地、任意の職場・学校の住所と座標 | `user_locations` の `HOME`・`WORK` |
| 住民の近くに欲しい店舗・施設ジャンル | `user_industry_preferences`（画面の業種コードをマスターIDへ変換） |
| 事業主の任意メール | `users.email` |
| 事業主の必須電話番号・任意のアイコン画像URL／パス | `business_profiles.phone_e164`・`business_profiles.icon_image_url` |
| 事業主の任意の追加電話番号 | `business_profiles.secondary_phone_e164` |
| 事業者名・代表者名・従業員数・経験・希望エリア名 | `business_profiles` |
| 地図上で囲んだ出店希望範囲 | `business_profiles.area`（PostGISのPolygon、SRID 4326） |
| 事業主の希望業種 | `user_industry_preferences` |
| 事業主の保有資格・許認可 | `business_qualifications` |
| メール通知の希望 | `notification_settings.email_enabled` |
| オーナーの氏名・法人名 | `owner_profiles` |
| オーナーの任意メール | `users.email` |
| オーナーの必須電話番号・任意のアイコン画像URL／パス | `owner_profiles.phone_e164`・`owner_profiles.icon_image_url` |
| オーナーの任意の追加電話番号 | `owner_profiles.secondary_phone_e164` |
| オーナーの誘致希望業種 | `user_industry_preferences` |

住民の「業種・ご職業」は、今回の指定により保存対象外です。資格や業種はDBの有効なマスターから表示し、画面の表示順ではなくマスターのIDを保存します。

住民の「欲しい業種」は職業とは別の希望条件として保存します。任意の複数選択に対応し、選択をすべて外して再登録した場合は既存の希望業種を解除します。無効・未登録のコードは保存せず、プロフィールや住所の変更もまとめて取り消します。

## オーナー・事業主プロフィールの準備（DB移行手順）

事業主の出店希望範囲（`business_profiles.area`）は PostGIS の `geometry(Polygon, 4326)` 型を使用します。既存DBを更新する場合は、**事前に PostgreSQL へ PostGIS 拡張をインストールした上で**（Windows 環境では Stack Builder 等から対象の PostgreSQL 用 PostGIS を導入）、DB管理者（`postgres` 等）として `machiriku` に接続し、**以下の順番で** SQL を適用してください。

1. **`database/machiriku_business_alter.sql`（PostGIS拡張と出店希望範囲列の追加）**  
   PostGIS 拡張（`CREATE EXTENSION IF NOT EXISTS postgis`）を有効化し、`business_profiles.area` 列、ポリゴン妥当性制約（`ck_business_profiles_area_valid`）、空間検索用の GiST インデックス（`idx_business_profiles_area`）を作成します。この移行を行わない環境では、事業主の初期登録時の保存と登録済みプロフィールの読み込みが失敗します。
2. **`database/migrate-profile-contacts.sql`（オーナー用テーブル作成と連絡先列の移行）**  
   `owner_profiles` テーブルの作成、両プロフィール（`business_profiles`・`owner_profiles`）へのアイコン参照先（`icon_image_url`）・電話番号（`phone_e164`）の追加、既存電話番号の移行、`users.phone_e164` の削除を1つのトランザクションで処理します。
3. **`database/migrate-profile-phone-numbers.sql`（主番号・追加番号の制約整備）**  
   主番号（`phone_e164`）は必須、追加番号（`secondary_phone_e164`）は任意とし、どちらも E.164 形式で保存します。事業所の共有番号を複数会員で利用できるように一意制約を解除し、同一プロフィール内で主番号と追加番号が重複することを防ぎます。
4. **`database/grant-user-access.sql`（アプリ実行ロールへの権限付与）**  
   アプリ用ロール `machiriku_app` に対し、`users` のほか、`ProfileDao` が使用する全テーブル（`resident_profiles`、`business_profiles`、`owner_profiles`、`user_locations`、`user_industry_preferences`、`business_qualifications`、`notification_settings`、`industry_categories`、`industry_types`、`qualifications`）および自動採番シーケンスへの権限を付与します。

```powershell
psql <管理者用の接続オプション> -d machiriku -f database/machiriku_business_alter.sql
psql <管理者用の接続オプション> -d machiriku -f database/migrate-profile-contacts.sql
psql <管理者用の接続オプション> -d machiriku -f database/migrate-profile-phone-numbers.sql
psql <管理者用の接続オプション> -d machiriku -f database/grant-user-access.sql
```

`database/create-owner-profiles.sql` は、すでに連絡先移行済みのDBへオーナー用テーブルだけを単体で新規作成するときの定義です。

LINE認証では表示名・任意メール・利用区分・LINEの識別子を会員に保存します。事業主とオーナーの電話番号は、認証後の初期設定で必須入力します。保存済みの電話番号とアイコン参照先は、各プロフィールから再表示します。

アイコン画像の参照先は任意です。HTTP(S)のURLまたはWeb用パスを2048文字まで入力でき、空欄はSQLの `NULL` として保存します。例として `https://example.com/icon.png`、`/assets/icons/owner.png` を利用できます。

住民画面の `?preview=1` はGETの表示確認専用です。POSTではプレビュー用IDを利用せず、必ずログイン中の住民のIDで保存します。

## 保存時の扱い

- 会員IDと利用区分は認証済みセッションとDBから確認し、フォームの会員IDでは変更できません。
- プロフィール更新、住所・希望条件、通知設定を1つのトランザクションで保存します。途中で失敗した場合は全体をロールバックし、入力画面へ戻します。
- すべての保存に成功してから会員を `ACTIVE` にし、セッションの会員情報と初期設定完了状態を更新します。
- 空のメールはSQLの `NULL` として保存します。メール未登録の場合、メール通知は有効にしません。
- 主番号・追加番号はJavaのlibphonenumberで日本の番号として検証し、E.164形式で保存します（例：`028-633-9988` → `+81286339988`）。携帯・固定・IP電話、ハイフン付き、全角数字、`+81`形式を受け付け、表示・編集では国内形式に整えます。
- 追加番号の空欄はSQLの `NULL` になります。編集時に空欄で保存すると、登録済みの追加番号を削除します。入力エラー時は両番号の入力値を保持します。
- 住民の居住地は実際の地図選択が必要です。画面確認用の住所・座標を初期選択しません。
- 事業主の希望範囲は必須です。`{lat,lng}` 点列を検証し、`[経度,緯度]` の閉じたGeoJSONへ変換して保存します。空の範囲や不正なポリゴンでは登録を完了しません。
- 更新フォームにはセッションごとのCSRFトークンを付けます。入力値はHTMLへエスケープし、DBエラーのログにはSQLStateのみを残します。

## 確認

```powershell
mvn test
```

DB連携テストも実行する場合は、次のように指定します。実DBの定義を使った一時テーブルで確認し、既存の会員やプロフィールには書き込みません。

```powershell
$env:MACHIRIKU_RUN_DB_TESTS = 'true'
mvn test
```
