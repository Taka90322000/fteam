<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="開店のようす・出店進捗 | マチリク" />
  <jsp:param name="description" value="出店が決まった空き店舗の開店準備・工事進捗・オープン予定日を地域住民向けに公開します。" />
  <jsp:param name="pageScreenId" value="s05" />
  <jsp:param name="assetVersion" value="20261124_matching_v1" />
</jsp:include>

    <section id="s05-section" class="screen-view s05-view" data-screen="S05">
      <div class="page-header-banner" data-screen="S05">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <span aria-current="page">開店のようす</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">開店のようす・出店進捗</h1>
          <span id="s05-header-badge" class="badge-status status-voting">地域公開</span>
        </div>
        <p class="page-description">住民投票から出店が決まった店舗の準備状況やオープン予定日を確認できます。気になる物件をフォローすると最新のお知らせが届きます。</p>
      </div>

      <div id="s05-empty-state" class="detail-card manage-empty-card" hidden>
        <h2 id="s05-empty-title">公開中の開店お知らせはまだありません</h2>
        <p id="s05-empty-desc">空き店舗に欲しいお店を投票したり、気になる物件をフォローしておくと、出店決定後の開店準備のようすを確認できます。</p>
        <div class="manage-empty-actions">
          <a class="btn-primary" href="${pageContext.request.contextPath}/index.jsp#s01">空き店舗を探して投票・フォローする</a>
          <a class="btn-secondary" href="${pageContext.request.contextPath}/matching/opening-plan.jsp">事業主として出店計画を作成する</a>
        </div>
      </div>

      <div id="s05-content-wrap">
        <div id="s05-switcher-bar" class="analytics-selector-bar" hidden>
          <div class="analytics-selector-group">
            <label for="s05-prop-select">表示する店舗・物件:</label>
            <select id="s05-prop-select" class="form-select analytics-select"></select>
          </div>
          <div class="analytics-quick-links">
            <span class="apply-count-badge">公開中の出店プロジェクト: <strong id="s05-project-count">0</strong> 件</span>
          </div>
        </div>

        <div class="detail-layout-grid">
          <div class="detail-main-pane">
            <article class="detail-card s05-hero-card">
              <div class="s05-hero-media">
                <img id="s05-hero-img" class="progress-image" src="${pageContext.request.contextPath}/assets/property-placeholder.svg" alt="">
              </div>
              <div class="s05-hero-body">
                <div class="manage-prop-badges">
                  <span id="s05-status-pill" class="badge-status status-preparing">開店準備中</span>
                  <span id="s05-genre-pill" class="qual-badge" hidden></span>
                  <span id="s05-open-date-pill" class="apply-summary-pill"></span>
                </div>
                <h2 id="s05-store-title" class="s05-store-heading">店舗名</h2>
                <p id="s05-prop-meta" class="manage-prop-meta"></p>

                <ol id="s05-opening-steps" class="opening-steps" aria-label="開店までの進み具合"></ol>
              </div>
            </article>

            <section class="detail-card" aria-labelledby="s05-timeline-heading">
              <div class="o10-card-head-row">
                <h2 id="s05-timeline-heading" class="detail-card-title">事業主からのお知らせ・進捗レポート</h2>
                <span id="s05-updates-count" class="o10-checks-summary-badge">0件</span>
              </div>
              <div id="s05-timeline-list" class="s05-timeline-stack"></div>
            </section>

            <section class="detail-card" aria-labelledby="s05-voices-heading">
              <h2 id="s05-voices-heading" class="detail-card-title">この物件に寄せられた地域の声</h2>
              <div id="s05-voices-list"></div>
            </section>
          </div>

          <aside class="detail-side-pane" aria-label="フォローと関連操作">
            <div class="cta-box-card">
              <h2 class="detail-card-title">開店のお知らせを受け取る</h2>
              <p class="sample-note">この物件をフォローしておくと、工事の進捗やオープン日決定、クーポン配布の案内を確認できます。</p>
              <button type="button" id="s05-follow-btn" class="btn-primary" aria-pressed="false">この物件をフォローする</button>

              <dl id="s05-side-specs" class="s05-side-specs"></dl>

              <div class="s05-side-actions">
                <a id="s05-coupon-link" class="secondary-action" href="${pageContext.request.contextPath}/index.jsp#s30" hidden>受け取ったクーポンを見る</a>
                <a id="s05-detail-link" class="secondary-action" href="${pageContext.request.contextPath}/property/detail.jsp">物件の詳細を見る</a>
                <a id="s05-plan-edit-link" class="secondary-action" href="${pageContext.request.contextPath}/matching/opening-plan.jsp" hidden>出店計画・進捗を更新する（事業主）</a>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="s05" />
  <jsp:param name="assetVersion" value="20261124_matching_v1" />
</jsp:include>
