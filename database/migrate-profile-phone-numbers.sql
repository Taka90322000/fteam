-- machirikuへDB所有者で接続し、migrate-profile-contacts.sqlの後に実行する。
-- 主電話番号は必須、追加番号は任意とし、事業所の共有番号を複数会員で使えるようにする。
BEGIN;

-- 登録・更新処理との競合時に長時間待ち続けることを防ぐ。
SET LOCAL lock_timeout = '10s';
LOCK TABLE public.business_profiles, public.owner_profiles IN ACCESS EXCLUSIVE MODE;

-- 追加の連絡先を国際形式で保存する。未登録はNULLで表す。
ALTER TABLE public.business_profiles
    ADD COLUMN IF NOT EXISTS secondary_phone_e164 varchar(16);

ALTER TABLE public.owner_profiles
    ADD COLUMN IF NOT EXISTS secondary_phone_e164 varchar(16);

-- 同じ事業所の代表番号を複数会員が登録できるよう、一意制約を解除する。
ALTER TABLE public.business_profiles
    DROP CONSTRAINT IF EXISTS uq_business_profiles_phone_e164;

ALTER TABLE public.owner_profiles
    DROP CONSTRAINT IF EXISTS uq_owner_profiles_phone_e164;

-- 再実行時にも同じ定義で検証できるよう、電話番号のCHECK制約を付け直す。
ALTER TABLE public.business_profiles
    DROP CONSTRAINT IF EXISTS ck_business_profiles_phone_e164,
    DROP CONSTRAINT IF EXISTS ck_business_profiles_secondary_phone_e164,
    DROP CONSTRAINT IF EXISTS ck_business_profiles_phone_numbers_distinct;

ALTER TABLE public.owner_profiles
    DROP CONSTRAINT IF EXISTS ck_owner_profiles_phone_e164,
    DROP CONSTRAINT IF EXISTS ck_owner_profiles_secondary_phone_e164,
    DROP CONSTRAINT IF EXISTS ck_owner_profiles_phone_numbers_distinct;

-- 事業主の主番号を必須にし、両番号の形式と同一プロフィール内の重複を検証する。
-- 携帯・固定・IP電話を同じE.164形式で扱い、追加番号の空欄はNULLで保存する。
ALTER TABLE public.business_profiles
    ALTER COLUMN phone_e164 SET NOT NULL,
    ADD CONSTRAINT ck_business_profiles_phone_e164
        CHECK (phone_e164 ~ '^[+][1-9][0-9]{7,14}$'),
    ADD CONSTRAINT ck_business_profiles_secondary_phone_e164
        CHECK (secondary_phone_e164 IS NULL
            OR secondary_phone_e164 ~ '^[+][1-9][0-9]{7,14}$'),
    ADD CONSTRAINT ck_business_profiles_phone_numbers_distinct
        CHECK (secondary_phone_e164 IS NULL
            OR secondary_phone_e164 <> phone_e164);

-- オーナーの連絡先にも同じ入力ルールを適用する。
ALTER TABLE public.owner_profiles
    ALTER COLUMN phone_e164 SET NOT NULL,
    ADD CONSTRAINT ck_owner_profiles_phone_e164
        CHECK (phone_e164 ~ '^[+][1-9][0-9]{7,14}$'),
    ADD CONSTRAINT ck_owner_profiles_secondary_phone_e164
        CHECK (secondary_phone_e164 IS NULL
            OR secondary_phone_e164 ~ '^[+][1-9][0-9]{7,14}$'),
    ADD CONSTRAINT ck_owner_profiles_phone_numbers_distinct
        CHECK (secondary_phone_e164 IS NULL
            OR secondary_phone_e164 <> phone_e164);

-- 管理ツールからも主番号と追加番号の用途を確認できるようにする。
COMMENT ON COLUMN public.business_profiles.phone_e164
    IS '主な連絡先電話番号（携帯・固定・IP電話）。必須、E.164形式。';
COMMENT ON COLUMN public.business_profiles.secondary_phone_e164
    IS '追加の連絡先電話番号（携帯・固定・IP電話）。任意、E.164形式。';
COMMENT ON COLUMN public.owner_profiles.phone_e164
    IS '主な連絡先電話番号（携帯・固定・IP電話）。必須、E.164形式。';
COMMENT ON COLUMN public.owner_profiles.secondary_phone_e164
    IS '追加の連絡先電話番号（携帯・固定・IP電話）。任意、E.164形式。';

COMMIT;
