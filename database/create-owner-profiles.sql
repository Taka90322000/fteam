-- machirikuへDB所有者（postgres）で接続し、1回だけ実行する。
-- usersのLINE表示名と、オーナーが入力した氏名・法人名は別の情報として保存する。
BEGIN;

CREATE TABLE public.owner_profiles (
    user_id integer PRIMARY KEY REFERENCES public.users(user_id) ON DELETE RESTRICT,
    representative_name varchar(100) NOT NULL,
    company_name varchar(150),
    icon_image_url text,
    phone_e164 varchar(16) NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_owner_profiles_phone_e164 UNIQUE (phone_e164),
    CONSTRAINT ck_owner_profiles_phone_e164 CHECK (phone_e164 ~ '^\+[1-9][0-9]{7,14}$')
);

-- 登録処理が必要とする参照・登録・更新だけをアプリ用ユーザーへ付与する。
GRANT SELECT, INSERT, UPDATE ON public.owner_profiles TO machiriku_app;

COMMIT;
