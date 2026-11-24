<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="事業主への出店オファー | マチリク" />
  <jsp:param name="description" value="住民需要に合う出店事業主へオーナーから直接出店を打診し、オファー履歴を管理します。" />
  <jsp:param name="pageScreenId" value="s13" />
  <jsp:param name="assetVersion" value="20261124_matching_v1" />
</jsp:include>

    <section id="s13-section" class="screen-view s13-view" data-screen="S13">
      <div class="page-header-banner" data-screen="S13">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理</a>
          <span aria-hidden="true">/</span>
          <span aria-current="page">事業主への出店オファー</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">事業主への出店オファー</h1>
          <span class="badge-status status-voting">オーナー</span>
        </div>
        <p class="page-description">物件の住民需要に合致する事業主を選び、優遇条件やメッセージを添えて出店を打診できます。</p>
      </div>

      <div id="s13-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <button type="button" id="s13-role-switch-btn" class="btn-primary btn-sm">物件オーナーに切り替える</button>
      </div>

      <div class="metrics-kpi-grid matching-kpi-grid">
        <div class="metric-card">
          <div class="metric-card-top"><span>送信済みオファー</span></div>
          <div class="metric-card-val"><span id="s13-kpi-sent">0</span> <span class="kpi-unit">件</span></div>
          <div class="metric-card-footer"><span>回答待ち・対応中</span></div>
        </div>
        <div class="metric-card">
          <div class="metric-card-top"><span>承諾されたオファー</span></div>
          <div class="metric-card-val"><span id="s13-kpi-accepted">0</span> <span class="kpi-unit">件</span></div>
          <div class="metric-card-footer"><span class="kpi-accent">応募・面談へ進行</span></div>
        </div>
        <div class="metric-card">
          <div class="metric-card-top"><span>下書き保存</span></div>
          <div class="metric-card-val"><span id="s13-kpi-draft">0</span> <span class="kpi-unit">件</span></div>
          <div class="metric-card-footer"><span>送信前に見直し可能</span></div>
        </div>
        <div class="metric-card">
          <div class="metric-card-top"><span>打診可能な事業主</span></div>
          <div class="metric-card-val"><span id="s13-kpi-candidates">0</span> <span class="kpi-unit">名</span></div>
          <div class="metric-card-footer"><span>希望業種・保有資格公開中</span></div>
        </div>
      </div>

      <div class="o10-layout matching-two-col">
        <div class="o10-main-col">
          <div class="form-stepper-box matching-form-card">
            <div class="stepper-header">
              <h2 id="s13-form-heading" class="prop-target-heading">オファー作成・送信フォーム</h2>
              <div id="s13-edit-banner" class="matching-edit-banner" hidden>
                <span id="s13-edit-banner-text">下書きを編集中です</span>
                <button type="button" id="s13-cancel-edit-btn" class="btn-secondary btn-xs">新規作成に戻す</button>
              </div>
            </div>

            <form id="s13-offer-form" action="${pageContext.request.contextPath}/matching/offer.jsp" method="POST" aria-labelledby="s13-form-heading">
              <input type="hidden" id="s13-offer-id" name="offerId" value="">

              <fieldset class="form-section-block form-fieldset-reset">
                <legend class="form-block-title">1. 提案物件と送信方法</legend>

                <div class="form-group">
                  <label for="s13-prop-select">オファー提案物件 <span class="req-mark">*必須</span></label>
                  <select id="s13-prop-select" name="propId" class="form-select" required aria-invalid="false" data-error="s13-prop-error"></select>
                  <div id="s13-prop-error" class="form-error-msg">
                    <span role="img" aria-label="エラー">⚠</span> オファー提案物件を選択してください。
                  </div>
                </div>

                <div id="s13-prop-summary" class="matching-inline-summary" aria-live="polite"></div>

                <fieldset class="form-group form-fieldset-reset">
                  <legend class="field-label">送信方法 <span class="req-mark">*必須</span></legend>
                  <div class="radio-inline-group">
                    <div class="radio-inline-card">
                      <input id="s13-kind-manual" type="radio" name="creationKind" value="MANUAL" checked>
                      <label for="s13-kind-manual"><span>個別オファー（事業主を指定して打診）</span></label>
                    </div>
                    <div class="radio-inline-card">
                      <input id="s13-kind-auto" type="radio" name="creationKind" value="AUTO">
                      <label for="s13-kind-auto"><span>自動オファー候補として登録</span></label>
                    </div>
                  </div>
                  <span class="form-hint-text">物件の需要1位業種や条件に合う事業主へ、個別または条件一致で打診します。</span>
                </fieldset>
              </fieldset>

              <fieldset class="form-section-block form-fieldset-reset">
                <legend class="form-block-title">2. 打診先事業主の選択</legend>

                <div class="form-group">
                  <label for="s13-biz-select">オファー対象事業主 <span class="req-mark">*必須</span></label>
                  <select id="s13-biz-select" name="businessUserId" class="form-select" required aria-invalid="false" data-error="s13-biz-error"></select>
                  <div id="s13-biz-error" class="form-error-msg">
                    <span role="img" aria-label="エラー">⚠</span> 打診先の事業主を選択してください。
                  </div>
                </div>

                <div id="s13-candidate-list" class="matching-candidate-grid" aria-label="需要に合う事業主候補"></div>
              </fieldset>

              <fieldset class="form-section-block form-fieldset-reset">
                <legend class="form-block-title">3. 優遇条件とオファーメッセージ</legend>

                <div class="form-group">
                  <span class="field-label">オーナーからの優遇・支援提案条件 <span class="opt-mark">（複数選択可）</span></span>
                  <div id="s13-incentives-grid" class="matching-check-grid"></div>
                </div>

                <div class="form-group">
                  <div class="matching-label-row">
                    <label for="s13-message">オファーメッセージ本文 <span class="req-mark">*必須</span></label>
                    <div class="matching-template-btns">
                      <button type="button" id="s13-tpl-demand" class="btn-secondary btn-xs">需要マッチ文面を挿入</button>
                      <button type="button" id="s13-tpl-inuki" class="btn-secondary btn-xs">居抜き・優遇文面を挿入</button>
                    </div>
                  </div>
                  <span id="s13-message-hint" class="form-hint-text">物件の魅力や住民からの要望、面談・内覧の希望などを具体的にお書きください。</span>
                  <textarea id="s13-message" name="message" class="form-control auto-grow-textarea" maxlength="2000" required aria-invalid="false" aria-describedby="s13-message-hint" data-error="s13-message-error" placeholder="例: 当物件の住民需要投票で貴店の業態が上位となっており、ぜひ一度ご内覧・ご相談の機会をいただけますでしょうか。"></textarea>
                  <div id="s13-message-error" class="form-error-msg">
                    <span role="img" aria-label="エラー">⚠</span> オファーメッセージを入力してください。
                  </div>
                </div>
              </fieldset>

              <div class="form-actions-bar form-actions-split">
                <div class="matching-action-left">
                  <a class="btn-secondary" href="${pageContext.request.contextPath}/property/manage.jsp">物件管理へ戻る</a>
                  <button type="button" id="s13-save-draft-btn" class="btn-secondary">下書き保存する</button>
                </div>
                <button type="submit" id="s13-submit-btn" class="btn-primary">確認してオファーを送信する</button>
              </div>
            </form>
          </div>
        </div>

        <aside class="o10-side-col" aria-label="選択中の事業主プロフィール">
          <section class="detail-card o10-card" aria-labelledby="s13-biz-preview-heading">
            <h2 id="s13-biz-preview-heading" class="o10-card-heading">選択中の事業主プロフィール</h2>
            <div id="s13-biz-preview-body"></div>
          </section>

          <section class="detail-card o10-card" aria-labelledby="s13-guide-heading">
            <h2 id="s13-guide-heading" class="o10-card-heading">オファー送信時のポイント</h2>
            <ul class="matching-guide-list">
              <li>住民投票の上位業種と事業主の希望業種が一致しているほど、内覧・応募につながりやすくなります。</li>
              <li>フリーレントや居抜き譲渡などの優遇条件を明示すると、事業主が初期投資を見積もりやすくなります。</li>
              <li>オファー送信後は、事業主の「応募状況確認」画面に届き、承諾されると出店応募へ進みます。</li>
            </ul>
            <div class="matching-side-links">
              <a id="s13-link-analytics" class="btn-secondary btn-sm" href="${pageContext.request.contextPath}/property/analytics.jsp">物件需要分析を確認する</a>
              <a class="btn-secondary btn-sm" href="${pageContext.request.contextPath}/property/apply-list.jsp">届いている出店応募を見る</a>
            </div>
          </section>
        </aside>
      </div>

      <section class="matching-history-section" aria-labelledby="s13-history-heading">
        <div class="matching-section-head">
          <h2 id="s13-history-heading" class="section-heading">送信済み・下書きオファー一覧</h2>
          <span class="apply-count-badge">表示件数: <strong id="s13-history-count">0</strong> 件</span>
        </div>

        <div id="s13-status-tabs" class="matching-tabs-scroller" role="tablist" aria-label="オファーステータス絞り込み">
          <button type="button" role="tab" class="matching-tab-btn" data-offer-status="ALL" aria-selected="true">すべて</button>
          <button type="button" role="tab" class="matching-tab-btn" data-offer-status="SENT" aria-selected="false">送信済み</button>
          <button type="button" role="tab" class="matching-tab-btn" data-offer-status="ACCEPTED" aria-selected="false">承諾済み</button>
          <button type="button" role="tab" class="matching-tab-btn" data-offer-status="DECLINED" aria-selected="false">辞退</button>
          <button type="button" role="tab" class="matching-tab-btn" data-offer-status="DRAFT" aria-selected="false">下書き</button>
        </div>

        <div id="s13-history-empty" class="detail-card manage-empty-card" hidden>
          <h3>オファー履歴はまだありません</h3>
          <p>上のフォームから事業主へ出店オファーを送信または下書き保存すると、ここに一覧表示されます。</p>
        </div>

        <div id="s13-history-list" class="apply-cards-stack"></div>
      </section>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="s13" />
  <jsp:param name="assetVersion" value="20261124_matching_v1" />
</jsp:include>
