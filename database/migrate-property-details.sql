-- machirikuへDB所有者（postgres）で接続して実行する。
-- pgAdminのクエリツールおよびpsql（psql -v ON_ERROR_STOP=1 -U postgres -d machiriku -f database/migrate-property-details.sql）の両方で実行可能。
-- property/add.jsp（物件登録・編集）の入力項目に対応するため、
-- properties・property_imagesへの列追加、設備マスタ（facilities）と物件設備（property_facilities）の作成を行う。
BEGIN;

-- 稼働中の更新処理と競合した場合は、長時間待ち続けず処理を中止する。
SET LOCAL lock_timeout = '10s';
LOCK TABLE public.properties, public.property_images IN ACCESS EXCLUSIVE MODE;

-- ============================================================================
-- 1. properties（空き店舗物件）への列追加
--    敷金・礼金・保証金は「未選択＝なし（NONE）」として扱う。
-- ============================================================================
ALTER TABLE public.properties
    ADD COLUMN IF NOT EXISTS transport_access varchar(150),
    ADD COLUMN IF NOT EXISTS management_fee_yen integer,
    ADD COLUMN IF NOT EXISTS deposit_kind varchar(16) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS deposit_months smallint,
    ADD COLUMN IF NOT EXISTS key_money_kind varchar(16) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS key_money_months smallint,
    ADD COLUMN IF NOT EXISTS security_deposit_kind varchar(16) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS security_deposit_months smallint,
    ADD COLUMN IF NOT EXISTS security_deposit_yen integer,
    ADD COLUMN IF NOT EXISTS floor_code varchar(16),
    ADD COLUMN IF NOT EXISTS total_floors_code varchar(16),
    ADD COLUMN IF NOT EXISTS building_structure varchar(16),
    ADD COLUMN IF NOT EXISTS built_year smallint,
    ADD COLUMN IF NOT EXISTS built_month smallint,
    ADD COLUMN IF NOT EXISTS handover_timing varchar(24),
    ADD COLUMN IF NOT EXISTS condition_memo varchar(300);

-- 既存の物件レコードがある環境で不整合が起きないよう、NOT NULL付与前に検証する。
DO $check_properties$
BEGIN
    IF EXISTS (
        SELECT 1 FROM public.properties
        WHERE transport_access IS NULL OR btrim(transport_access) = ''
    ) THEN
        RAISE EXCEPTION 'transport_accessが未設定の物件データが存在するため、NOT NULL制約を付与できません。';
    END IF;
END;
$check_properties$;

ALTER TABLE public.properties
    ALTER COLUMN transport_access SET NOT NULL;

-- 再実行時にも同じ定義で検証できるよう、追加分のCHECK制約を付け直す。
ALTER TABLE public.properties
    DROP CONSTRAINT IF EXISTS ck_properties_transport_access_not_blank,
    DROP CONSTRAINT IF EXISTS ck_properties_management_fee_yen,
    DROP CONSTRAINT IF EXISTS ck_properties_deposit_kind,
    DROP CONSTRAINT IF EXISTS ck_properties_deposit_consistency,
    DROP CONSTRAINT IF EXISTS ck_properties_key_money_kind,
    DROP CONSTRAINT IF EXISTS ck_properties_key_money_consistency,
    DROP CONSTRAINT IF EXISTS ck_properties_security_deposit_kind,
    DROP CONSTRAINT IF EXISTS ck_properties_security_deposit_consistency,
    DROP CONSTRAINT IF EXISTS ck_properties_floor_code,
    DROP CONSTRAINT IF EXISTS ck_properties_total_floors_code,
    DROP CONSTRAINT IF EXISTS ck_properties_building_structure,
    DROP CONSTRAINT IF EXISTS ck_properties_built_year,
    DROP CONSTRAINT IF EXISTS ck_properties_built_month,
    DROP CONSTRAINT IF EXISTS ck_properties_handover_timing;

ALTER TABLE public.properties
    ADD CONSTRAINT ck_properties_transport_access_not_blank
        CHECK (btrim(transport_access) <> ''),
    ADD CONSTRAINT ck_properties_management_fee_yen
        CHECK (management_fee_yen IS NULL OR management_fee_yen >= 0),
    ADD CONSTRAINT ck_properties_deposit_kind
        CHECK (deposit_kind IN ('NONE', 'MONTHS', 'NEGOTIABLE')),
    ADD CONSTRAINT ck_properties_deposit_consistency
        CHECK (
            (deposit_kind IN ('NONE', 'NEGOTIABLE') AND deposit_months IS NULL)
            OR (deposit_kind = 'MONTHS' AND deposit_months IS NOT NULL AND deposit_months >= 1)
        ),
    ADD CONSTRAINT ck_properties_key_money_kind
        CHECK (key_money_kind IN ('NONE', 'MONTHS', 'NEGOTIABLE')),
    ADD CONSTRAINT ck_properties_key_money_consistency
        CHECK (
            (key_money_kind IN ('NONE', 'NEGOTIABLE') AND key_money_months IS NULL)
            OR (key_money_kind = 'MONTHS' AND key_money_months IS NOT NULL AND key_money_months >= 1)
        ),
    ADD CONSTRAINT ck_properties_security_deposit_kind
        CHECK (security_deposit_kind IN ('NONE', 'MONTHS', 'AMOUNT', 'NEGOTIABLE')),
    ADD CONSTRAINT ck_properties_security_deposit_consistency
        CHECK (
            (security_deposit_kind IN ('NONE', 'NEGOTIABLE') AND security_deposit_months IS NULL AND security_deposit_yen IS NULL)
            OR (security_deposit_kind = 'MONTHS' AND security_deposit_months IS NOT NULL AND security_deposit_months >= 1 AND security_deposit_yen IS NULL)
            OR (security_deposit_kind = 'AMOUNT' AND security_deposit_yen IS NOT NULL AND security_deposit_yen >= 1 AND security_deposit_months IS NULL)
        ),
    ADD CONSTRAINT ck_properties_floor_code
        CHECK (
            floor_code IS NULL
            OR floor_code IN (
                'B2', 'B1', '1F', '2F', '3F', '4F', '5F', '6F', '7F', '8F', '9F',
                '10F_PLUS', 'WHOLE_BUILDING', 'OTHER'
            )
        ),
    ADD CONSTRAINT ck_properties_total_floors_code
        CHECK (
            total_floors_code IS NULL
            OR total_floors_code IN (
                '1F', '2F', '3F', '4F', '5F', '6F', '7F', '8F', '9F', '10F', '11F_PLUS'
            )
        ),
    ADD CONSTRAINT ck_properties_building_structure
        CHECK (
            building_structure IS NULL
            OR building_structure IN ('WOOD', 'LIGHT_STEEL', 'STEEL', 'RC', 'SRC', 'ALC', 'OTHER')
        ),
    ADD CONSTRAINT ck_properties_built_year
        CHECK (built_year IS NULL OR built_year BETWEEN 1900 AND 2100),
    ADD CONSTRAINT ck_properties_built_month
        CHECK (built_month IS NULL OR (built_year IS NOT NULL AND built_month BETWEEN 1 AND 12)),
    ADD CONSTRAINT ck_properties_handover_timing
        CHECK (
            handover_timing IS NULL
            OR handover_timing IN (
                'IMMEDIATE', 'CONSULT_IMMEDIATE', 'AFTER_1M', 'AFTER_2M', 'AFTER_3M', 'CONSULT'
            )
        );

COMMENT ON COLUMN public.properties.transport_access IS '交通・最寄駅アクセス。必須。';
COMMENT ON COLUMN public.properties.management_fee_yen IS '月額管理費・共益費。円。NULLは管理費等なし。';
COMMENT ON COLUMN public.properties.deposit_kind IS '敷金区分。NONE / MONTHS / NEGOTIABLE。未選択はNONE。';
COMMENT ON COLUMN public.properties.deposit_months IS '敷金月数。deposit_kind=MONTHSのとき1以上。';
COMMENT ON COLUMN public.properties.key_money_kind IS '礼金区分。NONE / MONTHS / NEGOTIABLE。未選択はNONE。';
COMMENT ON COLUMN public.properties.key_money_months IS '礼金月数。key_money_kind=MONTHSのとき1以上。';
COMMENT ON COLUMN public.properties.security_deposit_kind IS '保証金区分。NONE / MONTHS / AMOUNT / NEGOTIABLE。未選択はNONE。';
COMMENT ON COLUMN public.properties.security_deposit_months IS '保証金月数。security_deposit_kind=MONTHSのとき1以上。';
COMMENT ON COLUMN public.properties.security_deposit_yen IS '保証金額。円。security_deposit_kind=AMOUNTのとき1以上。';
COMMENT ON COLUMN public.properties.floor_code IS '所在階区分。B2/B1/1F〜9F/10F_PLUS/WHOLE_BUILDING/OTHER。';
COMMENT ON COLUMN public.properties.total_floors_code IS '建物階数区分。1F〜10F/11F_PLUS。';
COMMENT ON COLUMN public.properties.building_structure IS '建物構造。WOOD/LIGHT_STEEL/STEEL/RC/SRC/ALC/OTHER。';
COMMENT ON COLUMN public.properties.built_year IS '築年（西暦4桁）。不明・未設定はNULL。';
COMMENT ON COLUMN public.properties.built_month IS '築月（1〜12）。built_year設定時のみ指定可。';
COMMENT ON COLUMN public.properties.handover_timing IS '引渡し時期・状態。IMMEDIATE/CONSULT_IMMEDIATE/AFTER_1M/AFTER_2M/AFTER_3M/CONSULT。';
COMMENT ON COLUMN public.properties.condition_memo IS '居抜き・設備・業種条件メモ。一覧・詳細向けの簡潔な条件補足。';

-- ============================================================================
-- 2. property_images（物件画像）への画像種別列追加
-- ============================================================================
ALTER TABLE public.property_images
    ADD COLUMN IF NOT EXISTS image_kind varchar(16) NOT NULL DEFAULT 'OTHER';

ALTER TABLE public.property_images
    DROP CONSTRAINT IF EXISTS ck_property_images_image_kind;

ALTER TABLE public.property_images
    ADD CONSTRAINT ck_property_images_image_kind
        CHECK (image_kind IN ('EXTERIOR', 'INTERIOR', 'FLOORPLAN', 'FACILITY', 'SURROUNDING', 'OTHER'));

COMMENT ON COLUMN public.property_images.image_kind IS '画像種別。EXTERIOR / INTERIOR / FLOORPLAN / FACILITY / SURROUNDING / OTHER。';

-- ============================================================================
-- 3. facilities（基本インフラ設備マスタ）の作成と初期データ投入
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.facilities (
    facility_id integer GENERATED BY DEFAULT AS IDENTITY,
    facility_code varchar(32) NOT NULL,
    facility_name varchar(80) NOT NULL,
    display_order integer NOT NULL DEFAULT 1,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_facilities PRIMARY KEY (facility_id),
    CONSTRAINT uq_facilities_facility_code UNIQUE (facility_code),
    CONSTRAINT uq_facilities_facility_name UNIQUE (facility_name),
    CONSTRAINT ck_facilities_display_order CHECK (display_order >= 1)
);

COMMENT ON TABLE public.facilities IS '物件の基本インフラ設備マスタ。有効フラグと表示順で管理し、項目の追加に対応。';
COMMENT ON COLUMN public.facilities.facility_id IS '設備ID';
COMMENT ON COLUMN public.facilities.facility_code IS '設備コード。名称変更時も不変の識別子。';
COMMENT ON COLUMN public.facilities.facility_name IS '設備名（画面表示用）';
COMMENT ON COLUMN public.facilities.display_order IS '表示順。小さい値から並べる。';
COMMENT ON COLUMN public.facilities.is_active IS '有効フラグ';
COMMENT ON COLUMN public.facilities.created_at IS '作成日時';
COMMENT ON COLUMN public.facilities.updated_at IS '更新日時。更新時にアクション層で更新。';

-- 画面（data.js: INFRASTRUCTURE_FACILITIES）の10項目を初期登録する。
INSERT INTO public.facilities (facility_code, facility_name, display_order)
VALUES
    ('INUKI', '居抜き', 1),
    ('PARKING', '駐車場あり', 2),
    ('BARRIER_FREE', 'バリアフリー', 3),
    ('PRIVATE_TOILET', '専用トイレ', 4),
    ('INDIVIDUAL_AC', '個別空調', 5),
    ('POWER', '動力あり', 6),
    ('CITY_GAS', '都市ガス', 7),
    ('WATER_DRAINAGE', '給排水', 8),
    ('GREASE_TRAP', 'グリストラップ', 9),
    ('EXHAUST_DUCT', '排気ダクト', 10)
ON CONFLICT (facility_code) DO UPDATE
SET facility_name = EXCLUDED.facility_name,
    display_order = EXCLUDED.display_order,
    is_active = true,
    updated_at = CURRENT_TIMESTAMP;

-- ============================================================================
-- 4. property_facilities（物件設備）中間テーブルの作成
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.property_facilities (
    property_id integer NOT NULL,
    facility_id integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_property_facilities PRIMARY KEY (property_id, facility_id),
    CONSTRAINT fk_property_facilities_property_id
        FOREIGN KEY (property_id) REFERENCES public.properties(property_id) ON DELETE RESTRICT,
    CONSTRAINT fk_property_facilities_facility_id
        FOREIGN KEY (facility_id) REFERENCES public.facilities(facility_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_property_facilities_facility_id
    ON public.property_facilities (facility_id);

COMMENT ON TABLE public.property_facilities IS '物件が備える基本インフラ設備の関連付け。';
COMMENT ON COLUMN public.property_facilities.property_id IS '物件ID。FK: properties(property_id)';
COMMENT ON COLUMN public.property_facilities.facility_id IS '設備ID。FK: facilities(facility_id)';
COMMENT ON COLUMN public.property_facilities.created_at IS '作成日時';

-- ============================================================================
-- 5. アプリ用ロール（machiriku_app）への権限付与
-- ============================================================================
GRANT SELECT, INSERT, UPDATE ON TABLE
    public.properties
TO machiriku_app;

-- 物件編集時に画像・契約条件・許可業種・設備を差し替え保存できるようDELETEも付与する。
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE
    public.property_images,
    public.property_contract_terms,
    public.property_industries,
    public.property_facilities
TO machiriku_app;

GRANT SELECT ON TABLE
    public.facilities
TO machiriku_app;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO machiriku_app;

COMMIT;
