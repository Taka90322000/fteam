<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="出店応募 | マチリク" />
  <jsp:param name="description" value="出店計画・契約希望条件・必要資格を確認してオーナーへ応募します。" />
  <jsp:param name="pageScreenId" value="b01" />
  <jsp:param name="assetVersion" value="20261118_prop_v5" />
</jsp:include>

    <section id="b01-empty-state" class="screen-view empty-state" data-screen="B01" hidden>
      <h1 class="page-title">物件が見つかりません</h1>
      <p>掲載が終了したか、まだ物件が登録されていません。</p>
      <a class="btn-primary" href="${pageContext.request.contextPath}/index.jsp#s01">物件一覧へ</a>
    </section>

    <section id="b01-section" class="screen-view s08-view" data-screen="B01">
      <div class="page-header-banner" data-screen="B01">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a id="b01-bc-detail" href="${pageContext.request.contextPath}/property/detail.jsp">物件詳細</a>
          <span aria-hidden="true">/</span>
          <span aria-current="page">出店応募</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">出店応募・エントリーフォーム</h1>
          <span class="badge-status status-reviewing">出店希望者</span>
        </div>
      </div>

      <div id="b01-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <button type="button" id="b01-switch-role-btn" class="btn-primary btn-sm">出店事業主に切り替える</button>
      </div>

      <div class="form-stepper-box">
        <div class="stepper-header">
          <h2 id="b01-prop-title" class="prop-target-heading">【応募対象】読み込み中…</h2>
          <div id="b01-prop-meta" class="prop-target-sub"></div>
        </div>

        <div id="b01-complete-box" class="complete-state-card" tabindex="-1" hidden>
          <div class="complete-badge-icon" aria-hidden="true">✓</div>
          <h2>出店応募を送信しました</h2>
          <p id="b01-complete-summary"></p>
          <div class="complete-actions">
            <a id="b01-complete-detail-btn" class="btn-secondary" href="${pageContext.request.contextPath}/property/detail.jsp">物件詳細へ戻る</a>
            <a class="btn-primary" href="${pageContext.request.contextPath}/index.jsp#s09">応募状況を確認する</a>
          </div>
        </div>

        <form id="b01-apply-form" action="${pageContext.request.contextPath}/property/apply.jsp" method="POST" aria-labelledby="b01-prop-title">
          <fieldset class="form-section-block form-fieldset-reset">
            <legend class="form-block-title">1. 出店計画・契約希望条件</legend>

            <div class="form-group">
              <label for="b01-business-name">事業者名・屋号 <span class="req-mark">*必須</span> <span id="b01-business-name-autofill" class="req-badge-ok autofill-badge">自動入力済</span></label>
              <input id="b01-business-name" type="text" name="businessName" class="form-control" required maxlength="150" autocomplete="organization" aria-invalid="false" data-error="b01-business-name-error" placeholder="例: 合同会社日光珈琲舎">
              <div id="b01-business-name-error" class="form-error-msg">
                <span role="img" aria-label="エラー">⚠</span> 事業者名・屋号を入力してください。
              </div>
            </div>

            <div class="form-group">
              <label for="b01-planned-store-name">開店予定の店舗名 <span class="opt-mark">（任意）</span></label>
              <input id="b01-planned-store-name" type="text" name="plannedStoreName" class="form-control" maxlength="150" placeholder="例: 日光焙煎珈琲スタンド オリオン通り店">
            </div>

            <div class="form-group">
              <label for="b01-genre">出店予定業種 <span class="req-mark">*必須</span> <span id="b01-genre-autofill" class="req-badge-ok autofill-badge">自動入力済</span></label>
              <select id="b01-genre" name="genreId" class="form-select" required aria-invalid="false" data-error="b01-genre-error"></select>
              <div id="b01-genre-error" class="form-error-msg">
                <span role="img" aria-label="エラー">⚠</span> 出店予定業種を選択してください。
              </div>
            </div>

            <fieldset class="form-group form-fieldset-reset">
              <legend class="field-label">希望契約形態 <span class="req-mark">*必須</span></legend>
              <div class="radio-inline-group">
                <div class="radio-inline-card">
                  <input id="b01-contract-long" type="radio" name="contractKind" value="LONG" checked>
                  <label for="b01-contract-long"><span>通常賃貸契約（長期出店）</span></label>
                </div>
                <div class="radio-inline-card">
                  <input id="b01-contract-popup" type="radio" name="contractKind" value="POPUP">
                  <label for="b01-contract-popup"><span>ポップアップ短期出店</span></label>
                </div>
              </div>
              <div id="b01-popup-terms-box" class="popup-terms-callout" hidden></div>
            </fieldset>

            <div id="b01-popup-period-row" class="form-grid-2" hidden>
              <div class="form-group">
                <label for="b01-start-on">利用開始希望日 <span class="req-mark">*必須</span></label>
                <input id="b01-start-on" type="date" name="requestedStartOn" class="form-control" disabled aria-invalid="false" data-error="b01-start-on-error">
                <div id="b01-start-on-error" class="form-error-msg">
                  <span role="img" aria-label="エラー">⚠</span> 利用開始希望日を選択してください。
                </div>
              </div>
              <div class="form-group">
                <label for="b01-end-on">利用終了希望日 <span class="req-mark">*必須</span></label>
                <input id="b01-end-on" type="date" name="requestedEndOn" class="form-control" disabled aria-invalid="false" data-error="b01-end-on-error">
                <div id="b01-end-on-error" class="form-error-msg">
                  <span role="img" aria-label="エラー">⚠</span> 利用終了希望日を正しく選択してください。
                </div>
              </div>
            </div>

            <div class="form-grid-2 b01-schedule-grid">
              <div id="b01-long-open-row" class="form-group">
                <label for="b01-target-open-date">店舗オープン予定時期 <span class="req-mark">*必須</span></label>
                <input id="b01-target-open-date" type="date" name="targetOpenDate" class="form-control" required aria-invalid="false" data-error="b01-target-open-date-error">
                <div id="b01-target-open-date-error" class="form-error-msg">
                  <span role="img" aria-label="エラー">⚠</span> 店舗オープン予定時期を選択してください。
                </div>
              </div>

              <div id="b01-viewing-row" class="form-group">
                <label for="b01-viewing-at">現地内覧の希望日時 <span class="opt-mark">（任意）</span></label>
                <input id="b01-viewing-at" type="datetime-local" name="viewingRequestedAt" class="form-control">
              </div>
            </div>
          </fieldset>

          <fieldset class="form-section-block form-fieldset-reset">
            <legend class="form-block-title">2. 出店への想い・ストーリー（オーナーへのメッセージ）</legend>
            <div class="form-group">
              <label for="b01-message">なぜこの場所で出店したいのか、どんなお店にしたいのか <span class="req-mark">*必須</span></label>
              <textarea id="b01-message" name="message" class="form-control auto-grow-textarea" maxlength="2000" required aria-invalid="false" data-error="b01-message-error" placeholder="例: 地元の食材やこだわりの珈琲を通じて、地域住民が日常的に集うサードプレイスを作りたいです。"></textarea>
              <div id="b01-message-error" class="form-error-msg">
                <span role="img" aria-label="エラー">⚠</span> 出店への想い・メッセージを入力してください。
              </div>
            </div>
          </fieldset>

          <fieldset class="form-section-block form-fieldset-reset">
            <legend class="form-block-title">3. 業種別 必要資格・許可・提出書類の確認</legend>
            <div id="b01-req-alert" class="req-alert-banner" role="alert" tabindex="-1" hidden></div>
            <div id="b01-req-list" class="req-check-grid"></div>
            <div class="form-group">
              <label for="b01-exp-years">実務経験年数 <span class="opt-mark">（任意）</span></label>
              <input id="b01-exp-years" type="text" name="experienceYears" class="form-control" maxlength="200" placeholder="例: 飲食業界歴12年（うち店舗統括マネージャー5年）">
            </div>
          </fieldset>

          <div class="form-actions-bar form-actions-split">
            <a id="b01-back-link" class="btn-secondary" href="${pageContext.request.contextPath}/property/detail.jsp">戻る</a>
            <button type="submit" id="b01-submit-btn" class="btn-primary">この内容で出店申込を送信する</button>
          </div>
        </form>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="b01" />
  <jsp:param name="assetVersion" value="20261118_prop_v5" />
</jsp:include>
