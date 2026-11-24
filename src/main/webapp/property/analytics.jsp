<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="物件需要分析 | マチリク" />
  <jsp:param name="description" value="登録物件・中心市街地の住民需要スコアと7大分類別分布を分析します。" />
  <jsp:param name="pageScreenId" value="o02" />
  <jsp:param name="assetVersion" value="20261124_focus_v2" />
</jsp:include>

    <section id="o02-section" class="screen-view s07-view" data-screen="O02">
      <div class="page-header-banner" data-screen="O02">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理</a>
          <span aria-hidden="true">/</span>
          <span aria-current="page">物件需要分析</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">物件需要分析</h1>
          <span class="badge-status status-voting">オーナー・事業主</span>
        </div>
      </div>

      <div id="o02-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <button type="button" id="o02-role-switch-btn" class="btn-primary btn-sm">物件オーナーに切り替える</button>
      </div>

      <div class="analytics-selector-bar">
        <div class="analytics-selector-group">
          <label for="o02-prop-select">分析対象の空き店舗:</label>
          <select id="o02-prop-select" class="form-select analytics-select"></select>
        </div>
        <div class="analytics-quick-links">
          <a id="o02-link-detail" class="btn-secondary btn-sm" href="${pageContext.request.contextPath}/property/detail.jsp" hidden>物件詳細を見る</a>
          <a id="o02-link-apply-list" class="btn-primary btn-sm" href="${pageContext.request.contextPath}/property/apply-list.jsp">出店希望を確認</a>
        </div>
      </div>

      <div class="metrics-kpi-grid">
        <div class="metric-card">
          <div class="metric-card-top">
            <span>投票回答数</span>
            <button
              type="button"
              id="o02-vote-help-btn"
              class="o02-help-tip"
              interestfor="o02-vote-rule-popover"
              popovertarget="o02-vote-rule-popover"
              popovertargetaction="show"
              aria-label="投票回答数とスコアの算出ルール"
              aria-expanded="false">?</button>
            <div id="o02-vote-rule-popover" popover="hint" role="tooltip" class="o02-rule-popover">
              1回の回答で最大3業種まで順位投票可（第1希望 1.0倍・第2希望 0.6倍・第3希望 0.3倍、かつ近隣住民ほど高配点）のため、業種別の延べ票数合計は投票回答数を上回り、順位はスコア（pt）降順となります。
            </div>
          </div>
          <div class="metric-card-val"><span id="o02-kpi-votes">0</span> <span class="kpi-unit">件</span></div>
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
          <ul id="o02-cat-chart" class="cat-chart-list" role="list"></ul>
        </div>

        <div class="detail-card">
          <h2 class="detail-card-title">希望業種ランキング詳細</h2>
          <p class="o02-rule-note">※1回答につき最大3業種まで順位投票可（第1〜3希望・近隣ほど高配点）のため、延べ票数の合計は投票回答数を上回り、順位はスコア（pt）降順となります。</p>
          <ol id="o02-subcat-ranking" class="demand-bar-list" role="list"></ol>
        </div>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="o02" />
  <jsp:param name="assetVersion" value="20261124_focus_v2" />
</jsp:include>
