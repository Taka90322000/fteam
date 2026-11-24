<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="出店応募詳細 | マチリク" />
  <jsp:param name="description" value="事業主から届いた出店応募の詳細、保有資格の確認、面談調整や承認・返信を行います。" />
  <jsp:param name="pageScreenId" value="o10" />
  <jsp:param name="assetVersion" value="20261124_matching_v1" />
</jsp:include>

    <section id="o10-empty-state" class="screen-view empty-state" data-screen="O10" hidden>
      <h1 class="page-title">応募情報が見つかりません</h1>
      <p>既に削除・取り下げされたか、URLが正しくありません。</p>
      <a class="btn-primary" href="${pageContext.request.contextPath}/property/apply-list.jsp">出店希望確認へ戻る</a>
    </section>

    <section id="o10-section" class="screen-view o10-view" data-screen="O10">
      <div class="page-header-banner" data-screen="O10">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理</a>
          <span aria-hidden="true">/</span>
          <a id="o10-breadcrumb-list" href="${pageContext.request.contextPath}/property/apply-list.jsp">出店希望確認</a>
          <span aria-hidden="true">/</span>
          <span aria-current="page">出店応募詳細</span>
        </nav>
        <div class="o10-title-bar">
          <div class="page-title-row">
            <h1 id="o10-title" class="page-title">出店応募詳細</h1>
            <span id="o10-status-badge" class="badge-status status-reviewing">-</span>
          </div>
          <a id="o10-back-link" class="btn-secondary btn-sm o10-back-link" href="${pageContext.request.contextPath}/property/apply-list.jsp">← 出店希望一覧に戻る</a>
        </div>
        <p id="o10-subtitle" class="page-description">応募内容・必要資格の登録状況を確認し、面談連絡や承認・見送りを行います。</p>
      </div>

      <div id="o10-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <button type="button" class="btn-primary btn-sm" onclick="MachirikuStore.setRole('owner')">物件オーナーに切り替える</button>
      </div>

      <div class="o10-layout">
        <div class="o10-main-col">
          <section class="detail-card o10-card" aria-labelledby="o10-summary-heading">
            <h2 id="o10-summary-heading" class="o10-card-heading">応募条件・応募者概要</h2>
            <dl id="o10-summary-grid" class="o10-summary-grid"></dl>
          </section>

          <section class="detail-card o10-card" aria-labelledby="o10-message-heading">
            <h2 id="o10-message-heading" class="o10-card-heading">出店への想い・事業コンセプト</h2>
            <div id="o10-message-body" class="story-content-body"></div>
          </section>

          <section class="detail-card o10-card" aria-labelledby="o10-checks-heading">
            <div class="o10-card-head-row">
              <h2 id="o10-checks-heading" class="o10-card-heading">必要資格・要件の確認</h2>
              <span id="o10-checks-summary" class="o10-checks-summary-badge"></span>
            </div>
            <div id="o10-checks-list" class="apply-checks-list"></div>
          </section>
        </div>

        <aside class="o10-side-col" aria-label="チャット・審査判断">
          <section id="o10-chat-card" class="detail-card o10-card o10-chat-panel" aria-labelledby="o10-chat-heading">
            <h2 id="o10-chat-heading" class="o10-card-heading">チャット</h2>
            <div id="o10-chat-thread" class="o10-chat-thread" aria-live="polite"></div>
            <label for="o10-chat-input" class="o10-field-label">メッセージ入力</label>
            <textarea id="o10-chat-input" class="form-control" rows="3" placeholder="面談候補日時や質問事項、確認事項などを入力..."></textarea>
            <div class="o10-chat-send-row">
              <button type="button" id="o10-chat-send-btn" class="btn-primary btn-sm">送信する</button>
            </div>
          </section>

          <section id="o10-judgement-card" class="detail-card apply-reply-panel o10-judgement-panel" aria-labelledby="o10-action-heading">
            <h2 id="o10-action-heading" class="o10-card-heading">審査・ステータス判断</h2>
            <div id="o10-actions-stack" class="apply-action-stack"></div>
            <div id="o10-handover-box" class="o10-handover-box" hidden>
              <p class="o10-handover-note">出店合意・契約成立済みのため、契約引渡し手続き画面へ進めます。</p>
              <a id="o10-handover-link" class="btn-primary btn-sm" href="${pageContext.request.contextPath}/matching/contract.jsp">契約引渡し手続きへ進む →</a>
            </div>
          </section>
        </aside>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="o10" />
  <jsp:param name="assetVersion" value="20261124_matching_v1" />
</jsp:include>
