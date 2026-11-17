<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="出店応募 | マチリク" />
  <jsp:param name="description" value="出店計画・契約希望条件・必要資格を確認してオーナーへ応募します。" />
  <jsp:param name="pageScreenId" value="b01" />
  <jsp:param name="assetVersion" value="20261117_prop_pages" />
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
          <span class="sep">/</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">出店応募・エントリーフォーム</h1>
          <span class="badge-status status-reviewing">出店希望者</span>
        </div>
        <p class="page-description">店舗のコンセプト、契約希望期間、必要資格・許可要件を確認のうえオーナーへ出店を申し込みます。</p>
      </div>

      <div id="b01-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <button type="button" class="btn-primary btn-sm" onclick="MachirikuStore.setRole('business')">出店事業主に切り替える</button>
      </div>

      <div class="form-stepper-box">
        <div class="stepper-header">
          <h2 id="b01-prop-title" class="prop-target-heading">【応募対象】読み込み中…</h2>
          <div id="b01-prop-meta" class="prop-target-sub"></div>
        </div>

        <div id="b01-complete-box" class="complete-state-card" hidden>
          <div class="complete-badge-icon" aria-hidden="true">✓</div>
          <h3>出店応募を送信しました</h3>
          <p id="b01-complete-summary"></p>
          <div class="complete-actions">
            <a id="b01-complete-detail-btn" class="btn-secondary" href="${pageContext.request.contextPath}/property/detail.jsp">物件詳細へ戻る</a>
            <a class="btn-primary" href="${pageContext.request.contextPath}/index.jsp#s09">応募状況を確認する</a>
          </div>
        </div>

        <form id="b01-apply-form">
          <div class="form-section-block">
            <h3 class="form-block-title">1. 出店計画・契約希望条件</h3>
            <div class="form-group">
              <label for="b01-store-name">店舗屋号・ブランド名 <span class="req-mark">*必須</span></label>
              <input id="b01-store-name" type="text" name="storeName" class="form-control" required placeholder="例: 日光焙煎珈琲スタンド オリオン通り店">
            </div>

            <div class="form-group">
              <label for="b01-genre">出店予定業種（27小分類） <span class="req-mark">*必須</span></label>
              <select id="b01-genre" name="genreId" class="form-select" required></select>
            </div>

            <div class="form-group">
              <label class="field-label">希望契約形態 <span class="req-mark">*必須</span></label>
              <div class="radio-inline-group">
                <label class="radio-inline-card">
                  <input id="b01-contract-long" type="radio" name="contractKind" value="LONG" checked>
                  <span>通常賃貸契約（長期出店）</span>
                </label>
                <label class="radio-inline-card">
                  <input id="b01-contract-popup" type="radio" name="contractKind" value="POPUP">
                  <span>ポップアップ短期出店</span>
                </label>
              </div>
              <div id="b01-popup-terms-box" class="popup-terms-callout" hidden></div>
            </div>

            <div class="form-grid-2">
              <div class="form-group">
                <label for="b01-target-rent">希望月額賃料 (円)</label>
                <input id="b01-target-rent" type="number" name="targetRent" class="form-control" min="0">
              </div>
              <div class="form-group">
                <label for="b01-target-open-date">希望オープン時期</label>
                <input id="b01-target-open-date" type="text" name="targetOpenDate" class="form-control" value="2026年12月上旬">
              </div>
            </div>

            <div class="form-grid-2">
              <div class="form-group">
                <label for="b01-start-on">利用開始希望日 <span class="req-mark">*必須</span></label>
                <input id="b01-start-on" type="date" name="requestedStartOn" class="form-control" required>
              </div>
              <div class="form-group">
                <label for="b01-end-on">利用終了希望日 <span class="req-mark">*必須</span></label>
                <input id="b01-end-on" type="date" name="requestedEndOn" class="form-control" required>
              </div>
            </div>

            <div class="form-group">
              <label for="b01-viewing-at">現地内覧の希望日時（任意）</label>
              <input id="b01-viewing-at" type="datetime-local" name="viewingRequestedAt" class="form-control">
            </div>
          </div>

          <div class="form-section-block">
            <h3 class="form-block-title">2. 出店への想い・ストーリー（オーナーへのメッセージ）</h3>
            <div class="form-group">
              <label for="b01-message">なぜこの場所で出店したいのか、どんなお店にしたいのか <span class="req-mark">*必須</span></label>
              <textarea id="b01-message" name="message" class="form-control" rows="4" required placeholder="例: 地元の食材やこだわりの珈琲を通じて、地域住民が日常的に集うサードプレイスを作りたいです。"></textarea>
            </div>
          </div>

          <div class="form-section-block">
            <h3 class="form-block-title">3. 業種別 必要資格・許可・提出書類の確認</h3>
            <div id="b01-req-alert" class="req-alert-banner" role="alert" hidden></div>
            <div id="b01-req-list" class="req-check-grid"></div>
            <div class="form-group" style="margin-top:0.9rem;">
              <label for="b01-exp-years">実務経験年数（事業主プロフィールより）</label>
              <input id="b01-exp-years" type="text" class="form-control" readonly>
            </div>
          </div>

          <div class="form-actions-bar">
            <span></span>
            <div class="form-actions-right">
              <a id="b01-back-link" class="btn-secondary" href="${pageContext.request.contextPath}/property/detail.jsp">戻る</a>
              <button type="submit" id="b01-submit-btn" class="btn-primary">この内容で出店申込を送信する</button>
            </div>
          </div>
        </form>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="b01" />
  <jsp:param name="assetVersion" value="20261117_prop_pages" />
</jsp:include>
