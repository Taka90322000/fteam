<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="登録物件管理 | マチリク" />
  <jsp:param name="description" value="所有する空き店舗の掲載状態・需要スコア・出店応募状況を一元管理します。" />
  <jsp:param name="pageScreenId" value="o01" />
  <jsp:param name="assetVersion" value="20261117_prop_pages" />
</jsp:include>

    <section id="o01-section" class="screen-view s11-view" data-screen="O01">
      <div class="page-header-banner" data-screen="O01">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <span>登録物件管理</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">登録物件管理</h1>
          <span class="badge-status status-voting">オーナー</span>
        </div>
        <p class="page-description">保有する空き店舗の一覧と募集ステータス、需要投票・出店応募への対応状況を管理します。</p>
      </div>

      <div id="o01-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <button type="button" class="btn-primary btn-sm" onclick="MachirikuStore.setRole('owner')">物件オーナーに切り替える</button>
      </div>

      <div class="metrics-kpi-grid" style="margin-bottom:1.5rem;">
        <div class="metric-card">
          <div class="metric-card-top"><span>所有物件数</span></div>
          <div class="metric-card-val"><span id="o01-kpi-count">0</span> <span class="kpi-unit">件</span></div>
          <div class="metric-card-footer"><span>管理対象空き店舗</span></div>
        </div>
        <div class="metric-card">
          <div class="metric-card-top"><span>獲得需要投票</span></div>
          <div class="metric-card-val"><span id="o01-kpi-votes">0</span> <span class="kpi-unit">票</span></div>
          <div class="metric-card-footer"><span id="o01-kpi-score" class="kpi-accent">計 0 pt</span></div>
        </div>
        <div class="metric-card">
          <div class="metric-card-top"><span>受信出店応募</span></div>
          <div class="metric-card-val"><span id="o01-kpi-apps">0</span> <span class="kpi-unit">件</span></div>
          <div class="metric-card-footer"><a href="${pageContext.request.contextPath}/property/apply-list.jsp">出店希望を確認する →</a></div>
        </div>
        <div class="metric-card">
          <div class="metric-card-top"><span>PR注目枠掲載</span></div>
          <div class="metric-card-val"><span id="o01-kpi-pr">0</span> <span class="kpi-unit">件</span></div>
          <div class="metric-card-footer"><span>検索上位枠</span></div>
        </div>
      </div>

      <div class="manage-toolbar">
        <div id="o01-status-tabs" class="status-filter-tabs" role="tablist" aria-label="公開ステータス絞り込み">
          <button type="button" class="status-tab-btn is-active" data-status="ALL">すべて</button>
          <button type="button" class="status-tab-btn" data-status="OPEN">公開中</button>
          <button type="button" class="status-tab-btn" data-status="DRAFT">下書き</button>
          <button type="button" class="status-tab-btn" data-status="PAUSED">一時停止</button>
          <button type="button" class="status-tab-btn" data-status="CONTRACTED">契約済</button>
        </div>
        <a class="btn-primary" href="${pageContext.request.contextPath}/property/add.jsp">＋ 新規空き店舗を登録する</a>
      </div>

      <div id="o01-empty-box" class="detail-card manage-empty-card" hidden>
        <h2>所有物件がまだ登録されていません</h2>
        <p>新しい空き店舗を登録するか、デモ用のサンプル物件（2件）を所有物件として読み込んで管理操作をお試しください。</p>
        <div class="manage-empty-actions">
          <a class="btn-primary" href="${pageContext.request.contextPath}/property/add.jsp">＋ 新規空き店舗を登録する</a>
          <button type="button" id="o01-assign-demo-btn" class="btn-secondary">デモ用サンプル物件（2件）を表示する</button>
        </div>
      </div>

      <div id="o01-prop-list" class="manage-prop-list"></div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="o01" />
  <jsp:param name="assetVersion" value="20261117_prop_pages" />
</jsp:include>
