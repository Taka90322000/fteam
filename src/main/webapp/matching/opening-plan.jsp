<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="出店計画の作成・公開 | マチリク" />
  <jsp:param name="description" value="契約完了した店舗の開店予定日・進捗状況・住民へのメッセージを投稿して公開します。" />
  <jsp:param name="pageScreenId" value="s15" />
  <jsp:param name="assetVersion" value="20261124_matching_v1" />
</jsp:include>

    <section id="s15-section" class="screen-view s15-view" data-screen="S15">
      <div class="page-header-banner" data-screen="S15">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/matching/applications.jsp">応募状況確認</a>
          <span aria-hidden="true">/</span>
          <span aria-current="page">出店計画の作成・公開</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">出店計画の作成・公開</h1>
          <span class="badge-status status-reviewing">出店事業主</span>
        </div>
        <p class="page-description">オープン予定日や内装工事のようすを地域住民へ発信します。公開した内容は「開店のようす」ページに反映され、物件をフォローしている住民へ届きます。</p>
      </div>

      <div id="s15-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <button type="button" id="s15-role-switch-btn" class="btn-primary btn-sm">出店事業主に切り替える</button>
      </div>

      <div class="analytics-selector-bar">
        <div class="analytics-selector-group">
          <label for="s15-prop-select">対象の出店物件:</label>
          <select id="s15-prop-select" class="form-select analytics-select"></select>
        </div>
        <div class="analytics-quick-links">
          <a id="s15-link-progress" class="btn-secondary btn-sm" href="${pageContext.request.contextPath}/matching/progress.jsp">公開中の進捗ページを見る</a>
          <a id="s15-link-contract" class="btn-secondary btn-sm" href="${pageContext.request.contextPath}/matching/contract.jsp">契約・引渡し記録へ</a>
        </div>
      </div>

      <div id="s15-contract-hint" class="condition-insight-banner" hidden></div>

      <div class="o10-layout matching-two-col">
        <div class="o10-main-col">
          <div class="form-stepper-box matching-form-card">
            <div class="stepper-header">
              <h2 id="s15-form-heading" class="prop-target-heading">出店計画・進捗お知らせ投稿</h2>
              <div id="s15-edit-banner" class="matching-edit-banner" hidden>
                <span id="s15-edit-banner-text">既存のお知らせを編集中です</span>
                <button type="button" id="s15-cancel-edit-btn" class="btn-secondary btn-xs">新規投稿に戻す</button>
              </div>
            </div>

            <form id="s15-plan-form" action="${pageContext.request.contextPath}/matching/opening-plan.jsp" method="POST" aria-labelledby="s15-form-heading">
              <input type="hidden" id="s15-plan-id" name="planId" value="">

              <fieldset class="form-section-block form-fieldset-reset">
                <legend class="form-block-title">1. 店舗名と現在の開店進捗</legend>

                <div class="form-group">
                  <label for="s15-store-name">店舗正式名称 <span class="req-mark">*必須</span></label>
                  <input id="s15-store-name" type="text" name="storeName" class="form-control" required maxlength="150" aria-invalid="false" data-error="s15-store-name-error" placeholder="例: 日光焙煎珈琲スタンド オリオン通り店">
                  <div id="s15-store-name-error" class="form-error-msg">
                    <span role="img" aria-label="エラー">⚠</span> 店舗正式名称を入力してください。
                  </div>
                </div>

                <fieldset class="form-group form-fieldset-reset">
                  <legend class="field-label">現在の進捗状況 <span class="req-mark">*必須</span></legend>
                  <div class="matching-radio-grid-2">
                    <div class="radio-inline-card">
                      <input id="s15-prog-preparing" type="radio" name="progressStatus" value="PREPARING" checked>
                      <label for="s15-prog-preparing"><span>開店準備中</span></label>
                    </div>
                    <div class="radio-inline-card">
                      <input id="s15-prog-construction" type="radio" name="progressStatus" value="CONSTRUCTION">
                      <label for="s15-prog-construction"><span>内装・設備工事中</span></label>
                    </div>
                    <div class="radio-inline-card">
                      <input id="s15-prog-datefixed" type="radio" name="progressStatus" value="DATE_FIXED">
                      <label for="s15-prog-datefixed"><span>オープン日決定</span></label>
                    </div>
                    <div class="radio-inline-card">
                      <input id="s15-prog-opened" type="radio" name="progressStatus" value="OPENED">
                      <label for="s15-prog-opened"><span>開店・営業中</span></label>
                    </div>
                  </div>
                </fieldset>

                <div class="form-grid-2">
                  <div class="form-group">
                    <label id="s15-open-date-label" for="s15-open-date">開店予定日 <span class="opt-mark">（任意・開店時は必須）</span></label>
                    <input id="s15-open-date" type="date" name="plannedOpenOn" class="form-control" aria-invalid="false" data-error="s15-open-date-error">
                    <div id="s15-open-date-error" class="form-error-msg">
                      <span role="img" aria-label="エラー">⚠</span> 「開店・営業中」として公開する場合は実開店日を入力してください。
                    </div>
                  </div>
                  <div class="form-group">
                    <label for="s15-genre-display">出店予定業態</label>
                    <input id="s15-genre-display" type="text" class="form-control" readonly>
                  </div>
                </div>
              </fieldset>

              <fieldset class="form-section-block form-fieldset-reset">
                <legend class="form-block-title">2. 地域住民へのお知らせ内容</legend>

                <div class="form-group">
                  <label for="s15-title">お知らせ見出し <span class="req-mark">*必須</span></label>
                  <input id="s15-title" type="text" name="title" class="form-control" required maxlength="200" aria-invalid="false" data-error="s15-title-error" placeholder="例: 内装工事がスタートしました！カウンター席のイメージを公開">
                  <div id="s15-title-error" class="form-error-msg">
                    <span role="img" aria-label="エラー">⚠</span> お知らせ見出しを入力してください。
                  </div>
                </div>

                <div class="form-group">
                  <label for="s15-body">住民へのメッセージ・お店のこだわり <span class="req-mark">*必須</span></label>
                  <span id="s15-body-hint" class="form-hint-text">お店のコンセプト、工事の進捗、住民投票で寄せられた声への返信などを記載できます。</span>
                  <textarea id="s15-body" name="body" class="form-control auto-grow-textarea" maxlength="3000" required aria-invalid="false" aria-describedby="s15-body-hint" data-error="s15-body-error" placeholder="例: たくさんの投票ありがとうございました。朝の通勤前に立ち寄れる焼きたてパンと珈琲のお店を目指して準備を進めています。"></textarea>
                  <div id="s15-body-error" class="form-error-msg">
                    <span role="img" aria-label="エラー">⚠</span> 本文・メッセージを入力してください。
                  </div>
                </div>

                <div class="form-group">
                  <span class="field-label">準備・内装写真 <span class="opt-mark">（任意・最大3枚）</span></span>
                  <div class="photo-upload-toolbar">
                    <label class="btn-secondary btn-sm file-upload-label" for="s15-photo-input">
                      <span>＋ 写真を追加する</span>
                      <input id="s15-photo-input" type="file" class="sr-only" accept="image/*" multiple>
                    </label>
                    <span id="s15-photo-count" class="req-upload-status">0 / 3 枚</span>
                  </div>
                  <div id="s15-photo-preview" class="confirm-photo-grid"></div>
                </div>
              </fieldset>

              <div class="form-actions-bar form-actions-split">
                <div class="matching-action-left">
                  <a class="btn-secondary" href="${pageContext.request.contextPath}/matching/applications.jsp">応募一覧へ</a>
                  <button type="button" id="s15-draft-btn" class="btn-secondary">下書き保存する</button>
                </div>
                <button type="submit" id="s15-publish-btn" class="btn-primary">住民に向けて公開する</button>
              </div>
            </form>
          </div>
        </div>

        <aside class="o10-side-col" aria-label="住民向け公開プレビュー">
          <section class="detail-card o10-card" aria-labelledby="s15-preview-heading">
            <div class="o10-card-head-row">
              <h2 id="s15-preview-heading" class="o10-card-heading">住民画面プレビュー</h2>
              <span class="o10-checks-summary-badge">公開時の見え方</span>
            </div>
            <div id="s15-live-preview" class="matching-plan-preview"></div>
          </section>
        </aside>
      </div>

      <section class="matching-history-section" aria-labelledby="s15-posts-heading">
        <div class="matching-section-head">
          <h2 id="s15-posts-heading" class="section-heading">この物件のお知らせ・進捗履歴</h2>
          <span class="apply-count-badge">投稿数: <strong id="s15-posts-count">0</strong> 件</span>
        </div>

        <div id="s15-pub-tabs" class="matching-tabs-scroller" role="tablist" aria-label="公開状態絞り込み">
          <button type="button" role="tab" class="matching-tab-btn" data-pub-status="ALL" aria-selected="true">すべて</button>
          <button type="button" role="tab" class="matching-tab-btn" data-pub-status="PUBLISHED" aria-selected="false">公開中</button>
          <button type="button" role="tab" class="matching-tab-btn" data-pub-status="DRAFT" aria-selected="false">下書き</button>
          <button type="button" role="tab" class="matching-tab-btn" data-pub-status="ARCHIVED" aria-selected="false">アーカイブ</button>
        </div>

        <div id="s15-posts-empty" class="detail-card manage-empty-card" hidden>
          <h3>まだお知らせは投稿されていません</h3>
          <p>上のフォームから最初のお知らせを下書き保存または公開すると、ここに履歴が表示されます。下書きは住民には公開されません。</p>
        </div>

        <div id="s15-posts-list" class="apply-cards-stack"></div>
      </section>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="s15" />
  <jsp:param name="assetVersion" value="20261124_matching_v1" />
</jsp:include>
