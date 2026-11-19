-- DB所有者または管理者が、アプリ用ロールmachiriku_appへの権限付与をpsqlで実行する。
-- 実行例: psql <管理者用の接続オプション> -f database/grant-user-access.sql
-- UserDaoおよびProfileDaoに必要な権限を付与し、会員レコードやテーブル定義は変更しない。
-- 途中で失敗した場合は処理を止め、権限付与を一つのトランザクションとして扱う。
\set ON_ERROR_STOP on
BEGIN;
-- publicスキーマ内のオブジェクトを利用できるようにする。
GRANT USAGE ON SCHEMA public TO machiriku_app;

-- 会員の検索・登録、利用区分の更新、各区分のプロフィール・資格・通知設定の保存に必要な権限を付与する。
GRANT SELECT, INSERT, UPDATE ON TABLE
    public.users,
    public.resident_profiles,
    public.business_profiles,
    public.business_qualifications,
    public.notification_settings
TO machiriku_app;

-- 住民の住所（HOME/WORK）と各区分の希望業種は再保存時に入れ替えるため、DELETE権限も付与する。
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE
    public.user_locations,
    public.user_industry_preferences
TO machiriku_app;

-- 業種カテゴリ・業種・資格マスターの参照権限を付与する。
GRANT SELECT ON TABLE
    public.industry_categories,
    public.industry_types,
    public.qualifications
TO machiriku_app;

-- オーナー用プロフィールが作成済みの環境では、同様に参照・登録・更新権限を付与する。
DO $grant_owner_profiles$
BEGIN
    IF to_regclass('public.owner_profiles') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE ON TABLE public.owner_profiles TO machiriku_app;
    END IF;
END;
$grant_owner_profiles$;

-- 会員IDおよび住所IDなどの自動採番に使うシーケンスを利用できるようにする。
GRANT USAGE, SELECT ON SEQUENCE
    public.users_user_id_seq,
    public.user_locations_location_id_seq
TO machiriku_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO machiriku_app;
COMMIT;
