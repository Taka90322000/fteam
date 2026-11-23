# 管理画面の使い方

## 初回設定

1. 既存の PostgreSQL データベースに DB 所有者として接続し、`database/migrate-admin-management.sql`、`database/enable-admin-management.sql` の順で実行します。各ファイルは一度だけ実行します。
2. 管理を担当する本人が LINE ログインで本登録を済ませます。本人と照合した `users.user_id` を使って、DB 所有者が管理権限を付与します。

```sql
-- NULLをDB所有者が確認した会員IDに置き換える。
INSERT INTO public.admin_members (user_id)
SELECT user_id FROM public.users
WHERE user_id = NULL AND account_status = 'ACTIVE' AND withdrawn_at IS NULL
ON CONFLICT (user_id) DO UPDATE
SET is_active = true, granted_by_user_id = NULL, granted_at = CURRENT_TIMESTAMP;
```

3. Tomcat へ更新した WAR を配備します。付与先の本人がログインすると、トップ画面に「管理画面」が表示されます。コンテキストが `machiriku` の場合は `/machiriku/jp/machiriku/servlet/management/Admin.action` から開けます。

## 運用

- ユーザー管理は地域住民・事業主・物件オーナーで切り替え、名前を開くと区分別の詳細、履歴、編集項目が表示されます。
- 業種・必要資格・書類の設定は、利用者の業種選択または「必要資格・書類の案内」に反映されます。書類は名称、必須区分、条件、案内文、案内URLで管理します。
- 利用者は「掲載物件」の詳細から物件・掲載オーナーを、事業主一覧の詳細から閲覧可能な事業主を通報できます。問い合わせの返信は「お問い合わせ」の履歴で確認できます。
- 通報を完了するときに選んだ掲載停止や利用停止は、対応結果と一緒に保存されます。問い合わせの管理者用メモは管理画面内に表示されます。
- 管理画面の一覧は PostgreSQL を参照します。トップ画面のデモでブラウザーに保存した投票やアンケートなどは、DB に保存された時点で管理対象になります。

## 画面の変更箇所

- フォームの項目や順序、ボタン、見出しは `src/main/webapp/WEB-INF/views/management/` 以下の機能別JSPで変更します。たとえばユーザー詳細のプロフィールは `admin/users/profile.jsp` にあります。
- Actionは認可・入力検証・DB操作を行い、`jp.machiriku.bean.management` の表示用モデルをJSPに渡します。表示に使うデータを増やす場合は、対応するActionのデータ取得処理と表示用モデルを更新します。
- ページ枠やサイドバーは `views/management/common` と `views/management/admin/common` にあります。入力・表などの共通部品は `WEB-INF/tags/management` にあります。
- JSPはEL式で値を参照します。入力や表の共通タグには元の値を渡し、HTMLに出力する箇所で本文には `c:out`、引用符付き属性には `fn:escapeXml` を使います。JSTLのAPIと実装はMavenでWARに含めます。
