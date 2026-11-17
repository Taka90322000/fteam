<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="物件詳細 | マチリク" />
  <jsp:param name="description" value="宇都宮市の空き店舗詳細・住民需要ランキング・出店応募・投票ページ。" />
  <jsp:param name="pageScreenId" value="s02" />
  <jsp:param name="assetVersion" value="20261117_prop_pages" />
</jsp:include>

    <section id="s02-empty-state" class="screen-view empty-state" data-screen="S02" hidden>
      <h1 class="page-title">物件が見つかりません</h1>
      <p>掲載が終了したか、まだ物件が登録されていません。</p>
      <a class="btn-primary" href="${pageContext.request.contextPath}/index.jsp#s01">物件一覧へ</a>
    </section>

    <section id="s02-detail-section" class="screen-view s02-view" data-screen="S02">
      <div class="page-header-banner" data-screen="S02">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/index.jsp#s01">物件検索</a>
          <span class="sep">/</span>
        </nav>
        <div class="page-title-row">
          <h1 id="s02-prop-title" class="page-title">物件詳細を読み込み中…</h1>
          <span id="s02-status-badge" class="badge-status status-voting" hidden></span>
        </div>
        <p id="s02-prop-subtitle" class="page-description" hidden></p>
      </div>

      <div class="detail-layout-grid">
        <!-- 左メインコンテンツ -->
        <div class="detail-main-pane">
          <!-- 写真ギャラリー カルーセル -->
          <div class="detail-gallery-box" id="s02-gallery-box" data-prop="" role="region" aria-roledescription="carousel" aria-label="物件写真ギャラリー">
            <div class="gallery-main-container">
              <button type="button" id="s02-gallery-prev-btn" class="gallery-nav-btn prev" onclick="MachirikuScreens.navGallery(-1)" aria-label="前の写真" hidden>
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 18l-6-6 6-6"/></svg>
              </button>
              <div class="gallery-main-view" id="gallery-main-view">
                <div class="gallery-slider-track" id="gallery-slider-track"></div>
              </div>
              <button type="button" id="s02-gallery-next-btn" class="gallery-nav-btn next" onclick="MachirikuScreens.navGallery(1)" aria-label="次の写真" hidden>
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18l6-6-6-6"/></svg>
              </button>
              <button type="button" id="s02-gallery-all-btn" class="btn-gallery-all-overlay" title="すべての写真を一覧表示">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="vertical-align:-1px; margin-right:4px;"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>すべての写真 (<span id="s02-gallery-all-count">0</span>枚)
              </button>
              <div class="gallery-counter-badge">
                <span id="gallery-current-index">1</span> / <span id="gallery-total-count">1</span>
              </div>
            </div>
            <p id="s02-gallery-status" class="visually-hidden" role="status" aria-live="polite"></p>

            <div id="s02-thumbs-wrapper" class="gallery-carousel-wrapper" hidden>
              <button type="button" class="carousel-arrow-btn prev" onclick="MachirikuScreens.scrollThumbs(-1)" aria-label="前のサムネイル">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 18l-6-6 6-6"/></svg>
              </button>
              <div class="gallery-thumbs-track" id="gallery-thumbs-track"></div>
              <button type="button" class="carousel-arrow-btn next" onclick="MachirikuScreens.scrollThumbs(1)" aria-label="次のサムネイル">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18l6-6-6-6"/></svg>
              </button>
            </div>
          </div>

          <!-- 物件概要カード -->
          <div class="detail-card">
            <h2 class="detail-card-title">物件募集概要</h2>
            <div id="s02-overview-description" class="property-overview-description" hidden></div>

            <table class="specs-table specs-table-core" aria-label="物件の基本情報">
              <tbody>
                <tr>
                  <th scope="row">賃料 / 管理費等</th>
                  <td id="s02-spec-rent"></td>
                </tr>
                <tr>
                  <th scope="row">居抜き・設備状態</th>
                  <td id="s02-spec-condition"></td>
                </tr>
                <tr>
                  <th scope="row">所在地</th>
                  <td id="s02-spec-address"></td>
                </tr>
                <tr>
                  <th scope="row">交通・最寄駅</th>
                  <td id="s02-spec-access"></td>
                </tr>
                <tr>
                  <th scope="row">使用部分面積</th>
                  <td id="s02-spec-area"></td>
                </tr>
              </tbody>
            </table>

            <div id="s02-specs-extra" class="s02-collapse-panel" hidden>
              <table class="specs-table specs-table-extra" aria-label="詳細な契約・設備条件">
                <tbody>
                  <tr>
                    <th scope="row">敷金 / 礼金 / 保証金</th>
                    <td id="s02-spec-deposits"></td>
                  </tr>
                  <tr>
                    <th scope="row">その他の掲載費用</th>
                    <td id="s02-spec-other-fees">掲載なし</td>
                  </tr>
                  <tr>
                    <th scope="row">階数・構造</th>
                    <td id="s02-spec-floor-structure"></td>
                  </tr>
                  <tr>
                    <th scope="row">築年月</th>
                    <td id="s02-spec-built-year"></td>
                  </tr>
                  <tr>
                    <th scope="row">現況・引渡し</th>
                    <td id="s02-spec-handover"></td>
                  </tr>
                  <tr>
                    <th scope="row">基本インフラ設備</th>
                    <td>
                      <ul id="s02-infra-grid" class="infra-facility-grid" role="list" aria-label="基本インフラ設備"></ul>
                    </td>
                  </tr>
                  <tr id="s02-feature-tags-row" hidden>
                    <th scope="row">立地・空間の特徴</th>
                    <td>
                      <div id="s02-feature-tags" class="feature-tag-list"></div>
                    </td>
                  </tr>
                  <tr>
                    <th scope="row">飲食店営業許可</th>
                    <td><span id="s02-permit-status" class="badge-check-ok"></span></td>
                  </tr>
                  <tr id="s02-source-notes-row" hidden>
                    <th scope="row">掲載情報の備考</th>
                    <td>
                      <ul id="s02-source-notes-list" class="listing-fees"></ul>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="disclosure-footer-action">
              <button type="button" id="s02-specs-toggle-btn" class="detail-specs-toggle" aria-expanded="false" aria-controls="s02-specs-extra" onclick="MachirikuScreens.toggleS02Section('s02-specs-extra', this)">
                <span class="toggle-label-closed">詳細な契約・設備条件を表示する</span>
                <span class="toggle-label-open">詳細な契約・設備条件を閉じる</span>
              </button>
            </div>

            <div id="s02-source-info"></div>
            <div id="s02-agent-fallback" style="margin-top:1.25rem; padding-top:1rem; border-top:1px dashed var(--border-color); display:flex; justify-content:space-between; align-items:center; font-size:0.875rem; color:var(--text-sub);" hidden>
              <div>
                <strong>取扱不動産会社:</strong> <span id="s02-agent-name"></span><br>
                <span id="s02-agent-license" style="font-size:0.8rem; color:var(--text-muted);"></span>
              </div>
              <div style="text-align:right;">
                <span style="font-size:0.75rem; color:var(--text-muted);">物件番号: <span id="s02-prop-code-id"></span></span>
              </div>
            </div>
          </div>

          <!-- 住民需要分析セクション -->
          <div class="demand-civic-box">
            <h2 class="detail-card-title" style="color:var(--primary-700); border-bottom-color:var(--border-color);">
              住民の需要データ・リクエスト状況
            </h2>

            <div class="civic-kpi-bar">
              <div class="civic-kpi-item">
                <div class="civic-kpi-label">需要総合スコア</div>
                <div class="civic-kpi-val"><span id="s02-kpi-score">0</span> <span style="font-size:1rem;">pt</span></div>
                <div class="civic-kpi-sub">順位重み × 近隣距離係数の合算</div>
              </div>
              <div class="civic-kpi-item">
                <div class="civic-kpi-label">住民投票総数</div>
                <div class="civic-kpi-val"><span id="s02-kpi-votes">0</span> <span style="font-size:1rem;">人</span></div>
                <div class="civic-kpi-sub">投票した実人数（1人最大3業種）</div>
              </div>
              <div class="civic-kpi-item">
                <div class="civic-kpi-label">最多リクエスト</div>
                <div id="s02-kpi-top-label" class="civic-kpi-val" style="font-size:1.15rem; margin-top:0.6rem;">未集計</div>
                <div id="s02-kpi-top-sub" class="civic-kpi-sub">集計中</div>
              </div>
            </div>

            <div class="demand-ranking-header-row">
              <h3 style="font-size:0.95rem; font-weight:800; color:var(--primary-700); margin:0;">希望業種ランキング</h3>
              <ul class="demand-legend-list" role="list" aria-label="グラフ凡例">
                <li><span class="demand-legend-swatch rank-1" aria-hidden="true"></span>第1希望</li>
                <li><span class="demand-legend-swatch rank-2" aria-hidden="true"></span>第2希望</li>
                <li><span class="demand-legend-swatch rank-3" aria-hidden="true"></span>第3希望</li>
              </ul>
            </div>
            <div id="s02-demand-top-list" class="demand-bar-list"></div>

            <div id="s02-demand-extra-wrap" hidden>
              <div id="s02-demand-extra" class="s02-collapse-panel" data-until-found="true" hidden="until-found">
                <div id="s02-demand-extra-list" class="demand-bar-list demand-extra-inner"></div>
              </div>
              <div class="disclosure-footer-action">
                <button type="button" id="s02-demand-toggle-btn" class="detail-specs-toggle" aria-expanded="false" aria-controls="s02-demand-extra" onclick="MachirikuScreens.toggleS02Section('s02-demand-extra', this)">
                  <span id="s02-demand-toggle-closed" class="toggle-label-closed">6位以下の希望業種を見る</span>
                  <span id="s02-demand-toggle-open" class="toggle-label-open">6位以下の希望業種を閉じる</span>
                </button>
              </div>
            </div>

            <p class="demand-scoring-note">
              マチリクでは、近隣住民が『この空き店舗に欲しい業種』を第1〜第3希望（各 1.0 / 0.6 / 0.3 × 店舗からの近隣距離係数）で集計し、熱量と商圏の近さをスコア化しています。
            </p>
          </div>
        </div>

        <!-- 右サイドバー: アクションCTA -->
        <div class="detail-side-pane">
          <div class="cta-box-card">
            <div id="s02-cta-price-banner" class="cta-price-banner" hidden>
              <div id="s02-cta-rent-label" style="font-size:0.875rem; color:var(--text-muted);">月額賃料</div>
              <div id="s02-cta-rent-val" style="font-size:1.8rem; font-weight:900; color:var(--accent-price);"></div>
              <div id="s02-cta-mgmt" style="font-size:0.875rem; color:var(--text-sub);"></div>
            </div>

            <div id="s02-cta-demand-banner" class="cta-demand-summary-banner">
              <div style="font-size:0.95rem; font-weight:800; color:var(--primary-800); margin-bottom:0.75rem;">
                人気ランキング Top 3
              </div>
              <div id="s02-top3-demand-list" class="demand-bar-list" style="margin-bottom:0; gap:0.65rem;"></div>
            </div>

            <div class="cta-buttons-stack">
              <a id="s02-primary-cta" class="btn-cta-vote" href="${pageContext.request.contextPath}/index.jsp#s03">欲しいお店に投票する</a>
              <button type="button" id="s02-follow-btn" class="btn-secondary" data-follow-id="" aria-pressed="false" style="width:100%; justify-content:center;">
                開店のようすをフォローする
              </button>
            </div>

            <hr id="s02-side-hr" style="border:none; border-top:1px solid var(--border-color); margin:1.25rem 0;" hidden>

            <nav id="s02-side-secondary-nav" aria-label="物件に関する情報" hidden>
              <a id="s02-nav-demand" class="secondary-action" href="${pageContext.request.contextPath}/index.jsp#s07">住民の需要を見る</a>
              <a id="s02-nav-opening" class="secondary-action" href="${pageContext.request.contextPath}/index.jsp#s05">開店の予定を見る</a>
              <a id="s02-nav-pr" class="secondary-action" href="${pageContext.request.contextPath}/index.jsp#s33" hidden>PR掲載について</a>
            </nav>
          </div>

          <!-- 物件所在地GoogleMapカード -->
          <div class="detail-card side-location-card">
            <h3 class="side-location-title">所在地・周辺情報</h3>
            <div id="s02-side-address" class="side-location-address"></div>
            <div id="s02-location-map" class="side-location-map"></div>
          </div>
        </div>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="s02" />
  <jsp:param name="assetVersion" value="20261117_prop_pages" />
</jsp:include>
