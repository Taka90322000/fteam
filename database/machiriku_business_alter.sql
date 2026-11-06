-- Windows上の既存PostgreSQL 17にあるmachirikuデータベース向けの変更スクリプト。
-- 先にStack BuilderでPostgreSQL 17 x64用のPostGISをインストールしておく。
-- DB所有者または管理者のpostgresでmachirikuに接続してから、このファイルを実行する。
-- ADD COLUMNは列を末尾へ追加する。末尾のSELECTではareaをupdated_atの前に表示する。

BEGIN;

-- ポリゴン型と空間関数を利用するため、PostGIS拡張を有効にする。
CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE public.business_profiles
    -- 緯度・経度の座標系（SRID 4326）で、希望する事業エリアをポリゴンとして保持する。
    ADD COLUMN area geometry(Polygon, 4326);

-- 既存のプロフィールとエリア名のみの入力に対応するため、NULLを許可する。
ALTER TABLE public.business_profiles
    ADD CONSTRAINT ck_business_profiles_area_valid
    CHECK (area IS NULL OR (NOT ST_IsEmpty(area) AND ST_IsValid(area)));

-- 空間検索でポリゴンを効率よく絞り込むため、GiSTインデックスを作成する。
CREATE INDEX idx_business_profiles_area
    ON public.business_profiles USING GIST (area);

-- アプリ用ロールにプロフィールの参照・登録・更新権限を付与する。
GRANT SELECT, INSERT, UPDATE
    ON TABLE public.business_profiles TO machiriku_app;

COMMIT;

-- 適用後にPostGISの利用可能なバージョンを確認する。
SELECT PostGIS_Full_Version();

-- 確認用の表示順であり、テーブル内の物理的な列順は変更しない。
SELECT user_id,
       business_name,
       representative_name,
       employee_count,
       preferred_area,
       industry_experience,
       management_experience,
       area,
       updated_at
FROM public.business_profiles;
