-- 既存のmachirikuデータベースへDB所有者として接続し、ファイル全体を1回実行する。
-- 作成・列追加・初期値の設定・権限付与をまとめ、途中で失敗した場合は全体を取り消す。
BEGIN;

-- 稼働中の処理がテーブルを使用している場合、長時間のロック待ちを避ける。
SET LOCAL lock_timeout = '10s';

-- LINE会員のIDに管理権限を結び付ける。初回はDB所有者が会員を確認して登録する。
CREATE TABLE public.admin_members (
    user_id integer PRIMARY KEY REFERENCES public.users(user_id) ON DELETE RESTRICT,
    is_active boolean NOT NULL DEFAULT true,
    granted_by_user_id integer REFERENCES public.admin_members(user_id) ON DELETE RESTRICT,
    granted_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 対象の主キーと変更差分を記録し、利用停止や掲載停止を後から追跡できるようにする。
-- target_keyはクーポン発行記録のような複合キーも持てるようにJSONオブジェクトとする。
CREATE TABLE public.admin_audit_logs (
    audit_log_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_user_id integer NOT NULL REFERENCES public.admin_members(user_id) ON DELETE RESTRICT,
    action varchar(40) NOT NULL,
    target_type varchar(40) NOT NULL,
    target_key jsonb NOT NULL,
    reason text,
    changes jsonb NOT NULL DEFAULT '{}'::jsonb,
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_admin_audit_logs_target_key_object
        CHECK (jsonb_typeof(target_key) = 'object' AND target_key <> '{}'::jsonb),
    CONSTRAINT ck_admin_audit_logs_changes_object CHECK (jsonb_typeof(changes) = 'object')
);

-- 利用者と物件の通報を一つの受付一覧で扱う。人への通報は受付処理で利用区分を確認する。
-- 物件詳細から掲載者を通報した場合は、掲載者を対象にし、経緯を示す物件をrelated_property_idに保存する。
CREATE TABLE public.reports (
    report_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reporter_user_id integer NOT NULL REFERENCES public.users(user_id) ON DELETE RESTRICT,
    target_user_id integer REFERENCES public.users(user_id) ON DELETE RESTRICT,
    target_property_id integer REFERENCES public.properties(property_id) ON DELETE RESTRICT,
    related_property_id integer REFERENCES public.properties(property_id) ON DELETE RESTRICT,
    reason_code varchar(32) NOT NULL,
    description text NOT NULL,
    report_status varchar(16) NOT NULL DEFAULT 'OPEN',
    assigned_admin_user_id integer REFERENCES public.admin_members(user_id) ON DELETE RESTRICT,
    resolution_code varchar(32),
    resolution_note text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at timestamptz,
    CONSTRAINT ck_reports_single_target CHECK ((target_user_id IS NOT NULL) <> (target_property_id IS NOT NULL)),
    CONSTRAINT ck_reports_content CHECK (btrim(reason_code) <> '' AND btrim(description) <> ''),
    CONSTRAINT ck_reports_status CHECK (report_status IN ('OPEN', 'IN_REVIEW', 'RESOLVED')),
    -- 対応完了時に結果と理由を記録し、進行中は完了情報を空欄にする。
    CONSTRAINT ck_reports_resolution CHECK (
        (report_status = 'RESOLVED' AND resolved_at IS NOT NULL
            AND resolution_code IS NOT NULL AND btrim(resolution_code) <> ''
            AND resolution_note IS NOT NULL AND btrim(resolution_note) <> '')
        OR
        (report_status IN ('OPEN', 'IN_REVIEW') AND resolved_at IS NULL
            AND resolution_code IS NULL AND resolution_note IS NULL)
    )
);

-- 問い合わせを会員IDで管理し、返信が届いた際は受付の状態と更新日時も一緒に更新する。
CREATE TABLE public.inquiries (
    inquiry_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    requester_user_id integer NOT NULL REFERENCES public.users(user_id) ON DELETE RESTRICT,
    related_property_id integer REFERENCES public.properties(property_id) ON DELETE RESTRICT,
    category varchar(32) NOT NULL,
    subject varchar(200) NOT NULL,
    inquiry_status varchar(16) NOT NULL DEFAULT 'WAITING_ADMIN',
    assigned_admin_user_id integer REFERENCES public.admin_members(user_id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at timestamptz,
    CONSTRAINT ck_inquiries_subject CHECK (btrim(category) <> '' AND btrim(subject) <> ''),
    CONSTRAINT ck_inquiries_status CHECK (inquiry_status IN ('WAITING_ADMIN', 'IN_PROGRESS', 'WAITING_USER', 'CLOSED')),
    CONSTRAINT ck_inquiries_closed_at CHECK ((inquiry_status = 'CLOSED') = (closed_at IS NOT NULL))
);

-- 最初の本文・返信・管理者用メモを時系列で保存する。登録時に公開範囲を必ず指定する。
CREATE TABLE public.inquiry_messages (
    message_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inquiry_id bigint NOT NULL REFERENCES public.inquiries(inquiry_id) ON DELETE RESTRICT,
    sender_user_id integer NOT NULL REFERENCES public.users(user_id) ON DELETE RESTRICT,
    body text NOT NULL,
    visibility varchar(16) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_inquiry_messages_body CHECK (btrim(body) <> ''),
    CONSTRAINT ck_inquiry_messages_visibility CHECK (visibility IN ('PARTICIPANTS', 'ADMIN_ONLY'))
);

-- 現行のカテゴリID順と、カテゴリ内の業種ID順を表示順の初期値として引き継ぐ。
ALTER TABLE public.industry_categories
    ADD COLUMN display_order integer NOT NULL DEFAULT 1,
    ADD CONSTRAINT ck_industry_categories_display_order CHECK (display_order >= 1);

WITH ordered_categories AS (
    SELECT category_id, row_number() OVER (ORDER BY category_id)::integer AS display_order
    FROM public.industry_categories
)
UPDATE public.industry_categories AS category
SET display_order = ordered.display_order
FROM ordered_categories AS ordered
WHERE category.category_id = ordered.category_id;

ALTER TABLE public.industry_types
    ADD COLUMN display_order integer NOT NULL DEFAULT 1,
    ADD CONSTRAINT ck_industry_types_display_order CHECK (display_order >= 1);

WITH ordered_industries AS (
    SELECT industry_type_id,
           row_number() OVER (PARTITION BY category_id ORDER BY industry_type_id)::integer AS display_order
    FROM public.industry_types
)
UPDATE public.industry_types AS industry
SET display_order = ordered.display_order
FROM ordered_industries AS ordered
WHERE industry.industry_type_id = ordered.industry_type_id;

-- 新規申込では必須区分と適用条件を要件名と一緒に保存する。既存行の未確認の条件はNULLで表す。
ALTER TABLE public.application_checks
    ADD COLUMN is_required_snapshot boolean,
    ADD COLUMN condition_text_snapshot varchar(300);

-- 管理者による掲載停止の日時を持つ。NULLは停止の解除を表し、募集・契約の進捗にはlisting_statusを使う。
ALTER TABLE public.properties
    ADD COLUMN admin_hidden_at timestamptz;

-- 来店アンケートを改訂した際に、同じ契約物件で使った以前の版を参照できるようにする。
ALTER TABLE public.surveys
    ADD COLUMN previous_survey_id integer,
    ADD CONSTRAINT fk_surveys_previous_survey_id_tenancy_id
        FOREIGN KEY (previous_survey_id, tenancy_id) REFERENCES public.surveys(survey_id, tenancy_id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_surveys_previous_survey CHECK (
        previous_survey_id IS NULL OR (survey_kind = 'POST_VISIT' AND previous_survey_id <> survey_id)
    );

-- 配布を停止する状態はクーポン本体で管理し、発行済みの券は受取記録ごとに無効化する。
ALTER TABLE public.coupon_recipients
    ADD COLUMN revoked_at timestamptz,
    ADD CONSTRAINT ck_coupon_recipients_revoked_at CHECK (
        revoked_at IS NULL OR (used_at IS NULL AND revoked_at >= issued_at)
    );

-- 管理画面の受付一覧・対象ごとの履歴と、会員の問い合わせ一覧を安定した順序で取得する。
CREATE INDEX idx_admin_audit_logs_target ON public.admin_audit_logs (target_type, target_key, occurred_at DESC, audit_log_id DESC);
CREATE INDEX idx_reports_status_created_at ON public.reports (report_status, created_at DESC, report_id DESC);
CREATE INDEX idx_reports_target_user ON public.reports (target_user_id, created_at DESC, report_id DESC)
    WHERE target_user_id IS NOT NULL;
CREATE INDEX idx_reports_target_property ON public.reports (target_property_id, created_at DESC, report_id DESC)
    WHERE target_property_id IS NOT NULL;
CREATE INDEX idx_inquiries_status_updated_at ON public.inquiries (inquiry_status, updated_at DESC, inquiry_id DESC);
CREATE INDEX idx_inquiries_requester_updated_at ON public.inquiries (requester_user_id, updated_at DESC, inquiry_id DESC);
CREATE INDEX idx_inquiry_messages_thread ON public.inquiry_messages (inquiry_id, created_at, message_id);
CREATE INDEX idx_surveys_previous_survey_id_tenancy_id ON public.surveys (previous_survey_id, tenancy_id)
    WHERE previous_survey_id IS NOT NULL;

-- pgAdminのテーブル・列のプロパティで、各項目の用途と状態値の意味を確認できるようにする。
COMMENT ON TABLE public.admin_members IS 'LINE会員に付与した運営管理権限。';
COMMENT ON COLUMN public.admin_members.user_id IS '管理権限の付与対象となる会員のID。';
COMMENT ON COLUMN public.admin_members.is_active IS '管理権限の有効状態。解除時はfalseにする。';
COMMENT ON COLUMN public.admin_members.granted_by_user_id IS '管理権限を付与した管理者の会員ID。DB所有者による直接付与はNULL。';
COMMENT ON COLUMN public.admin_members.granted_at IS '管理権限を付与した日時。';

COMMENT ON TABLE public.admin_audit_logs IS '管理者による状態変更や設定変更の操作履歴。';
COMMENT ON COLUMN public.admin_audit_logs.audit_log_id IS '管理者操作履歴の識別子。';
COMMENT ON COLUMN public.admin_audit_logs.actor_user_id IS '操作を実行した管理者の会員ID。';
COMMENT ON COLUMN public.admin_audit_logs.action IS '利用停止や掲載停止など、実行した操作を表すコード。';
COMMENT ON COLUMN public.admin_audit_logs.target_type IS '会員や物件など、操作対象の種類を表すコード。';
COMMENT ON COLUMN public.admin_audit_logs.target_key IS '操作対象の主キー項目を格納するJSONオブジェクト。複合キーにも対応する。';
COMMENT ON COLUMN public.admin_audit_logs.reason IS '管理者が操作を行った理由。';
COMMENT ON COLUMN public.admin_audit_logs.changes IS '操作で変化した項目の変更前後の値を記録するJSONオブジェクト。';
COMMENT ON COLUMN public.admin_audit_logs.occurred_at IS '管理者が操作を行った日時。';

COMMENT ON TABLE public.reports IS '物件オーナー・事業主・物件に対する通報の受付と対応結果。';
COMMENT ON COLUMN public.reports.report_id IS '通報の受付番号。';
COMMENT ON COLUMN public.reports.reporter_user_id IS '通報を送信した会員のID。';
COMMENT ON COLUMN public.reports.target_user_id IS '通報対象となる物件オーナーまたは事業主の会員ID。物件への通報ではNULL。';
COMMENT ON COLUMN public.reports.target_property_id IS '掲載内容が通報された物件のID。会員への通報ではNULL。';
COMMENT ON COLUMN public.reports.related_property_id IS '掲載者を物件詳細から通報した場合など、経緯の確認に使う関連物件のID。';
COMMENT ON COLUMN public.reports.reason_code IS '利用者が選択した通報理由の分類コード。';
COMMENT ON COLUMN public.reports.description IS '利用者が入力した通報の具体的な内容。';
COMMENT ON COLUMN public.reports.report_status IS '対応状態。OPENは未対応、IN_REVIEWは確認中、RESOLVEDは完了。';
COMMENT ON COLUMN public.reports.assigned_admin_user_id IS '通報を担当する管理者の会員ID。未割当の場合はNULL。';
COMMENT ON COLUMN public.reports.resolution_code IS '掲載停止や対応不要など、対応完了時の結果を表すコード。';
COMMENT ON COLUMN public.reports.resolution_note IS '管理者が対応結果を決定した理由。';
COMMENT ON COLUMN public.reports.created_at IS '通報を受け付けた日時。';
COMMENT ON COLUMN public.reports.resolved_at IS '通報の対応を完了した日時。対応完了前はNULL。';

COMMENT ON TABLE public.inquiries IS '利用者からの問い合わせの受付、担当者および対応状況。';
COMMENT ON COLUMN public.inquiries.inquiry_id IS '問い合わせの受付番号。';
COMMENT ON COLUMN public.inquiries.requester_user_id IS '問い合わせを送信した会員のID。';
COMMENT ON COLUMN public.inquiries.related_property_id IS '問い合わせに関連する物件のID。物件を指定していない場合はNULL。';
COMMENT ON COLUMN public.inquiries.category IS 'アカウントや物件など、問い合わせの分類コード。';
COMMENT ON COLUMN public.inquiries.subject IS '問い合わせの一覧や詳細に表示する件名。';
COMMENT ON COLUMN public.inquiries.inquiry_status IS '対応状態。WAITING_ADMINは管理者の対応待ち、IN_PROGRESSは対応中、WAITING_USERは利用者の返信待ち、CLOSEDは完了。';
COMMENT ON COLUMN public.inquiries.assigned_admin_user_id IS '問い合わせを担当する管理者の会員ID。未割当の場合はNULL。';
COMMENT ON COLUMN public.inquiries.created_at IS '問い合わせを受け付けた日時。';
COMMENT ON COLUMN public.inquiries.updated_at IS '返信の追加や状態変更などで問い合わせを最後に更新した日時。';
COMMENT ON COLUMN public.inquiries.closed_at IS '問い合わせの対応を完了した日時。対応完了前はNULL。';

COMMENT ON TABLE public.inquiry_messages IS '問い合わせの最初の本文、返信、管理者用メモの記録。';
COMMENT ON COLUMN public.inquiry_messages.message_id IS '問い合わせに投稿されたメッセージの識別子。';
COMMENT ON COLUMN public.inquiry_messages.inquiry_id IS 'この投稿が属する問い合わせの受付番号。';
COMMENT ON COLUMN public.inquiry_messages.sender_user_id IS '投稿者の会員ID。管理者の返信やメモでは管理者のIDを使う。';
COMMENT ON COLUMN public.inquiry_messages.body IS '問い合わせの本文、返信または管理者用メモの内容。';
COMMENT ON COLUMN public.inquiry_messages.visibility IS '公開範囲。PARTICIPANTSは問い合わせを送信した本人と管理者向け、ADMIN_ONLYは管理者向け。';
COMMENT ON COLUMN public.inquiry_messages.created_at IS '本文、返信またはメモを投稿した日時。';

COMMENT ON COLUMN public.industry_categories.display_order IS '業種カテゴリの表示順。小さい値から並べる。';
COMMENT ON COLUMN public.industry_types.display_order IS 'カテゴリ内での業種の表示順。小さい値から並べる。';
COMMENT ON COLUMN public.application_checks.is_required_snapshot IS '申込時点の必須区分。trueは必須、falseは任意、NULLは当時の区分が未確認。';
COMMENT ON COLUMN public.application_checks.condition_text_snapshot IS '申込時点の適用条件。条件の記載がない場合や当時の条件が未確認の場合はNULL。';
COMMENT ON COLUMN public.properties.admin_hidden_at IS '管理者が掲載を停止した日時。NULLの場合は募集・契約状態に基づく通常の掲載判定を使う。';
COMMENT ON COLUMN public.surveys.previous_survey_id IS '同じ契約物件で使用した改訂元の来店アンケートID。初版はNULL。';
COMMENT ON COLUMN public.coupon_recipients.revoked_at IS '発行済みの未使用クーポンを無効化した日時。NULLの場合は使用日時と有効期限で状態を判定する。';

-- 管理権限はDB所有者が管理し、アプリは参照する。操作履歴とメッセージは追記で管理する。
GRANT SELECT ON TABLE public.admin_members TO machiriku_app;
GRANT SELECT, INSERT ON TABLE public.admin_audit_logs, public.inquiry_messages TO machiriku_app;
GRANT SELECT, INSERT, UPDATE ON TABLE public.reports, public.inquiries TO machiriku_app;
GRANT USAGE ON SEQUENCE
    public.admin_audit_logs_audit_log_id_seq,
    public.reports_report_id_seq,
    public.inquiries_inquiry_id_seq,
    public.inquiry_messages_message_id_seq
TO machiriku_app;

COMMIT;
