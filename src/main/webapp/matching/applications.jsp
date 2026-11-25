<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="応募状況確認 | マチリク" />
  <jsp:param name="description" value="自分が提出した出店応募の進捗・オーナーからの返信、およびオーナーから届いた出店オファーを確認します。" />
  <jsp:param name="pageScreenId" value="b02" />
  <jsp:param name="assetVersion" value="20261125_matching_v5" />
</jsp:include>

    <section id="b02-section" class="screen-view b02-view" data-screen="B02">
      <div class="page-header-banner" data-screen="B02">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <span aria-current="page">応募状況確認</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">応募状況確認・受信オファー</h1>
          <span class="badge-status status-reviewing">出店事業主</span>
        </div>
      </div>

      <div id="b02-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <button type="button" id="b02-role-switch-btn" class="btn-primary btn-sm">出店事業主に切り替える</button>
      </div>

      <div id="b02-view-tabs" class="matching-tabs-scroller" role="tablist" aria-label="応募状況とオファー切替">
        <button type="button" id="b02-tab-apps" role="tab" class="matching-tab-btn" data-b02-tab="apps" aria-selected="true">
          自分の出店応募（<span id="b02-tab-apps-count">0</span>）
        </button>
        <button type="button" id="b02-tab-offers" role="tab" class="matching-tab-btn" data-b02-tab="offers" aria-selected="false">
          オーナーからのオファー（<span id="b02-tab-offers-count">0</span>）
        </button>
      </div>

      <div id="b02-panel-apps" role="tabpanel" aria-labelledby="b02-tab-apps">
        <div class="metrics-kpi-grid matching-kpi-grid b02-kpi-grid-3" role="group" aria-label="自分の応募のステータス別件数・クイック絞り込み">
          <button type="button" class="metric-card b02-kpi-btn" data-b02-kpi="all" aria-pressed="true">
            <div class="metric-card-top"><span>提出済みの応募</span></div>
            <div class="metric-card-val"><span id="b02-kpi-total">0</span> <span class="kpi-unit">件</span></div>
            <div class="metric-card-footer"><span>全応募を表示</span></div>
          </button>
          <button type="button" class="metric-card b02-kpi-btn" data-b02-kpi="consulting" aria-pressed="false">
            <div class="metric-card-top"><span>面談・相談中</span></div>
            <div class="metric-card-val"><span id="b02-kpi-consulting">0</span> <span class="kpi-unit">件</span></div>
            <div class="metric-card-footer"><span>調整中で絞る</span></div>
          </button>
          <button type="button" class="metric-card b02-kpi-btn" data-b02-kpi="agreed" aria-pressed="false">
            <div class="metric-card-top"><span>合意・引渡し完了</span></div>
            <div class="metric-card-val"><span id="b02-kpi-agreed">0</span> <span class="kpi-unit">件</span></div>
            <div class="metric-card-footer"><span>契約・準備で絞る</span></div>
          </button>
        </div>

        <search class="apply-list-toolbar matching-search-bar" aria-label="自分の応募を絞り込み">
          <button type="button" id="b02-filter-toggle" class="btn-secondary btn-sm b02-filter-toggle-btn" aria-expanded="false" aria-controls="b02-filters-collapse">
            <span>絞り込み</span>
            <span id="b02-filter-active-count" class="b02-filter-active-badge" hidden>0</span>
            <span class="b02-filter-chevron" aria-hidden="true">▾</span>
          </button>
          <div id="b02-filters-collapse" class="b02-filters-collapse">
            <div class="b02-filters-collapse-inner">
              <div class="apply-filters-group">
                <select id="b02-status-filter" class="form-select" aria-label="応募ステータスで絞り込み">
                  <option value="ALL">すべてのステータス</option>
                  <option value="submitted">新規応募受付中</option>
                  <option value="consulting">面談・相談中</option>
                  <option value="agreed">合意・引渡し完了（すべて）</option>
                  <option value="approved">出店合意（契約調整へ）</option>
                  <option value="contracted">契約成立・引渡し完了</option>
                  <option value="rejected">見送り</option>
                  <option value="withdrawn">応募辞退</option>
                </select>
                <select id="b02-contract-filter" class="form-select" aria-label="契約区分で絞り込み">
                  <option value="ALL">すべての契約形態</option>
                  <option value="LONG">通常賃貸（長期）</option>
                  <option value="POPUP">ポップアップ短期</option>
                </select>
                <input id="b02-keyword-filter" type="search" class="form-control matching-search-input" aria-label="物件名・店舗名で検索" placeholder="物件名・店舗名で絞り込み" autocorrect="off" autocapitalize="none" spellcheck="false">
              </div>
            </div>
          </div>
          <div class="apply-count-badge">表示件数: <strong id="b02-apps-count">0</strong> 件</div>
        </search>

        <div id="b02-apps-empty" class="detail-card manage-empty-card" hidden>
          <h2>出店応募はまだありません</h2>
          <p>気になる空き店舗の詳細画面から、希望業種や契約条件を入力してオーナーへ応募できます。</p>
          <div class="manage-empty-actions">
            <a class="btn-primary" href="${pageContext.request.contextPath}/index.jsp#s01">空き店舗を探す</a>
            <a class="btn-secondary" href="${pageContext.request.contextPath}/property/apply.jsp">出店応募フォームを開く</a>
          </div>
        </div>

        <div id="b02-apps-list" class="apply-cards-stack"></div>
      </div>

      <div id="b02-panel-offers" role="tabpanel" aria-labelledby="b02-tab-offers" hidden>
        <div id="b02-offers-empty" class="detail-card manage-empty-card" hidden>
          <h2>届いているオファーはまだありません</h2>
          <p>物件オーナーから貴店への出店打診が届くと、ここに表示されます。プロフィールで希望業種や保有資格を充実させるとオファーが届きやすくなります。</p>
          <div class="manage-empty-actions">
            <a class="btn-primary" href="${pageContext.request.contextPath}/index.jsp#s01">空き店舗を探す</a>
            <a class="btn-secondary" href="${pageContext.request.contextPath}/matching/offer.jsp">オーナー視点でオファーを試す</a>
          </div>
        </div>

        <div id="b02-offers-list" class="apply-cards-stack"></div>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="b02" />
  <jsp:param name="assetVersion" value="20261125_matching_v5" />
</jsp:include>
