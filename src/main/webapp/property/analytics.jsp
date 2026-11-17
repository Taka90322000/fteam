<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="物件需要分析 | マチリク" />
  <jsp:param name="description" value="登録物件・中心市街地の住民需要スコアと7大分類別分布を分析します。" />
  <jsp:param name="pageScreenId" value="o02" />
  <jsp:param name="assetVersion" value="20261117_prop_pages" />
</jsp:include>

    <section id="o02-section" class="screen-view s07-view" data-screen="O02">
      <div class="page-header-banner" data-screen="O02">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理</a>
          <span class="sep">/</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">物件需要分析</h1>
          <span class="badge-status status-voting">オーナー・事業主</span>
        </div>
        <p class="page-description">物件別の住民投票スコア・7大分類別比率・生の声を多角的に把握し、募集条件や誘致業種の検討に活用します。</p>
      </div>

      <div id="o02-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <button type="button" class="btn-primary btn-sm" onclick="MachirikuStore.setRole('owner')">物件オーナーに切り替える</button>
      </div>

      <div class="analytics-selector-bar">
        <div class="analytics-selector-group">
          <label for="o02-prop-select">分析対象の空き店舗:</label>
          <select id="o02-prop-select" class="form-select analytics-select"></select>
        </div>
        <div class="analytics-quick-links">
          <a id="o02-link-detail" class="btn-secondary btn-sm" href="${pageContext.request.contextPath}/property/detail.jsp" hidden>物件詳細を見る</a>
          <a id="o02-link-edit" class="btn-secondary btn-sm" href="${pageContext.request.contextPath}/property/edit.jsp" hidden>物件条件を編集</a>
          <a id="o02-link-apply-list" class="btn-primary btn-sm" href="${pageContext.request.contextPath}/property/apply-list.jsp">出店希望を確認</a>
        </div>
      </div>

      <div class="metrics-kpi-grid">
        <div class="metric-card">
          <div class="metric-card-top"><span>総需要投票数</span></div>
          <div class="metric-card-val"><span id="o02-kpi-votes">0</span> <span class="kpi-unit">票</span></div>
          <div class="metric-card-footer"><span class="trend-badge trend-up">住民参加集計中</span></div>
        </div>
        <div class="metric-card">
          <div class="metric-card-top"><span>需要総合スコア</span></div>
          <div class="metric-card-val"><span id="o02-kpi-score">0</span> <span class="kpi-unit">pt</span></div>
          <div class="metric-card-footer"><span class="kpi-accent">順位・距離重み付け済</span></div>
        </div>
        <div class="metric-card">
          <div class="metric-card-top"><span>最多希望ジャンル</span></div>
          <div id="o02-kpi-top-genre" class="metric-card-val metric-val-sm">-</div>
          <div class="metric-card-footer"><span>リクエスト第1位</span></div>
        </div>
        <div class="metric-card">
          <div class="metric-card-top"><span>出店エントリー数</span></div>
          <div class="metric-card-val"><span id="o02-kpi-apps">0</span> <span class="kpi-unit">件</span></div>
          <div class="metric-card-footer"><span>応募受付・相談中</span></div>
        </div>
      </div>

      <div id="o02-condition-insight" class="condition-insight-banner" hidden></div>

      <div class="analytics-two-col">
        <div class="detail-card">
          <h2 id="o02-chart-title" class="detail-card-title">7大分類別 需要スコア分布</h2>
          <div id="o02-cat-chart" class="cat-chart-list"></div>

          <h3 class="detail-sub-heading" style="margin-top:1.5rem;">希望業種ランキング詳細</h3>
          <div id="o02-subcat-ranking" class="demand-bar-list"></div>
        </div>

        <div class="detail-card">
          <h2 class="detail-card-title">住民の生の声・リクエスト要望</h2>
          <div id="o02-voices-list" class="voices-scroll-list"></div>
        </div>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="o02" />
  <jsp:param name="assetVersion" value="20261117_prop_pages" />
</jsp:include>
