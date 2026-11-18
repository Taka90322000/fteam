-- machirikuへDB所有者（postgres）で接続して実行する。
-- オーナー情報を作成し、既存の電話番号を各プロフィールへ移してからusersの列を削除する。
-- 移行先がない電話番号や入力不足がある場合は全変更を取り消す。
BEGIN;

-- 稼働中の登録処理と競合した場合は、長時間待ち続けず処理を中止する。
SET LOCAL lock_timeout = '10s';
LOCK TABLE public.users, public.business_profiles IN ACCESS EXCLUSIVE MODE;

CREATE TABLE IF NOT EXISTS public.owner_profiles (
    user_id integer PRIMARY KEY REFERENCES public.users(user_id) ON DELETE RESTRICT,
    representative_name varchar(100) NOT NULL,
    company_name varchar(150),
    icon_image_url text,
    phone_e164 varchar(16),
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

LOCK TABLE public.owner_profiles IN ACCESS EXCLUSIVE MODE;

-- 以前のオーナー用SQLを適用済みの場合にも、必要な列を追加する。
ALTER TABLE public.owner_profiles
    ADD COLUMN IF NOT EXISTS icon_image_url text,
    ADD COLUMN IF NOT EXISTS phone_e164 varchar(16);

ALTER TABLE public.business_profiles
    ADD COLUMN IF NOT EXISTS icon_image_url text,
    ADD COLUMN IF NOT EXISTS phone_e164 varchar(16);

DO $migration$
BEGIN
    -- 適用済みDBでは元の列がないため、この移行部分を省略する。
    IF EXISTS (
        SELECT 1 FROM pg_attribute
        WHERE attrelid = 'public.users'::regclass AND attname = 'phone_e164' AND NOT attisdropped
    ) THEN
        -- 未完成プロフィールの代表者名や事業エリアを仮の値で作らず、管理者に確認を求める。
        IF EXISTS (
            SELECT 1 FROM public.users u
            WHERE u.phone_e164 IS NOT NULL AND u.user_role = 'BUSINESS'
              AND NOT EXISTS (SELECT 1 FROM public.business_profiles b WHERE b.user_id = u.user_id)
        ) THEN
            RAISE EXCEPTION '電話番号を持つ事業主に未作成のプロフィールがあります。移行前に登録を完了してください。';
        END IF;
        IF EXISTS (
            SELECT 1 FROM public.users u
            WHERE u.phone_e164 IS NOT NULL AND u.user_role = 'OWNER'
              AND NOT EXISTS (SELECT 1 FROM public.owner_profiles o WHERE o.user_id = u.user_id)
        ) THEN
            RAISE EXCEPTION '電話番号を持つオーナーに未作成のプロフィールがあります。代表者名を確認して登録してください。';
        END IF;
        IF EXISTS (
            SELECT 1 FROM public.users
            WHERE phone_e164 IS NOT NULL AND user_role NOT IN ('BUSINESS', 'OWNER')
        ) THEN
            RAISE EXCEPTION '事業主・オーナー以外の会員に電話番号があります。保存先を確認してから移行してください。';
        END IF;

        -- 移行先に別の電話番号が既にある場合、元の番号を失わないよう中止する。
        IF EXISTS (
            SELECT 1 FROM public.users u JOIN public.business_profiles b USING (user_id)
            WHERE u.phone_e164 IS NOT NULL AND b.phone_e164 IS NOT NULL
              AND u.phone_e164 IS DISTINCT FROM b.phone_e164
        ) OR EXISTS (
            SELECT 1 FROM public.users u JOIN public.owner_profiles o USING (user_id)
            WHERE u.phone_e164 IS NOT NULL AND o.phone_e164 IS NOT NULL
              AND u.phone_e164 IS DISTINCT FROM o.phone_e164
        ) THEN
            RAISE EXCEPTION '会員とプロフィールの電話番号が異なります。正しい番号を確認してから移行してください。';
        END IF;

        UPDATE public.business_profiles b
        SET phone_e164 = u.phone_e164
        FROM public.users u
        WHERE b.user_id = u.user_id AND b.phone_e164 IS NULL AND u.phone_e164 IS NOT NULL;

        UPDATE public.owner_profiles o
        SET phone_e164 = u.phone_e164
        FROM public.users u
        WHERE o.user_id = u.user_id AND o.phone_e164 IS NULL AND u.phone_e164 IS NOT NULL;
    END IF;

    IF EXISTS (SELECT 1 FROM public.business_profiles WHERE phone_e164 IS NULL)
       OR EXISTS (SELECT 1 FROM public.owner_profiles WHERE phone_e164 IS NULL) THEN
        RAISE EXCEPTION '電話番号が未登録のプロフィールがあります。実際の電話番号を設定してから移行してください。';
    END IF;
END;
$migration$;

ALTER TABLE public.business_profiles ALTER COLUMN phone_e164 SET NOT NULL;
ALTER TABLE public.owner_profiles ALTER COLUMN phone_e164 SET NOT NULL;

-- 電話番号の形式と、プロフィール内での重複禁止をDBでも保証する。
DO $constraints$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.business_profiles'::regclass AND conname = 'uq_business_profiles_phone_e164') THEN
        ALTER TABLE public.business_profiles ADD CONSTRAINT uq_business_profiles_phone_e164 UNIQUE (phone_e164);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.business_profiles'::regclass AND conname = 'ck_business_profiles_phone_e164') THEN
        ALTER TABLE public.business_profiles ADD CONSTRAINT ck_business_profiles_phone_e164
            CHECK (phone_e164 ~ '^\+[1-9][0-9]{7,14}$');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.owner_profiles'::regclass AND conname = 'uq_owner_profiles_phone_e164') THEN
        ALTER TABLE public.owner_profiles ADD CONSTRAINT uq_owner_profiles_phone_e164 UNIQUE (phone_e164);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.owner_profiles'::regclass AND conname = 'ck_owner_profiles_phone_e164') THEN
        ALTER TABLE public.owner_profiles ADD CONSTRAINT ck_owner_profiles_phone_e164
            CHECK (phone_e164 ~ '^\+[1-9][0-9]{7,14}$');
    END IF;
END;
$constraints$;

-- 同じテーブルにある電話番号の既定値・UNIQUE・電話必須CHECKも列削除に伴って除去される。
-- CASCADEを付けず、外部のビュー等が依存している場合はDB全体を変更せず中止する。
ALTER TABLE public.users DROP COLUMN IF EXISTS phone_e164;

GRANT SELECT, INSERT, UPDATE ON public.owner_profiles, public.business_profiles TO machiriku_app;

COMMIT;
