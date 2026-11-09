-- DB所有者または管理者が、app_userにMACHIRIKU_DB_USERの値を指定してpsqlで実行する。
-- 実行例: psql <管理者用の接続オプション> -v app_user=your_application_role -f database/grant-user-access.sql
-- UserDaoに必要な権限だけを付与し、会員レコードやテーブル定義は変更しない。
-- 途中で失敗した場合は処理を止め、権限付与を一つのトランザクションとして扱う。
\set ON_ERROR_STOP on
BEGIN;
-- publicスキーマ内のオブジェクトを利用できるようにする。
GRANT USAGE ON SCHEMA public TO :"app_user";
-- 会員の検索・登録と、本登録前の利用区分の更新に必要な権限を付与する。
GRANT SELECT, INSERT, UPDATE ON TABLE public.users TO :"app_user";
-- 会員IDの自動採番に使うシーケンスを利用できるようにする。
GRANT USAGE ON SEQUENCE public.users_user_id_seq TO :"app_user";
COMMIT;
