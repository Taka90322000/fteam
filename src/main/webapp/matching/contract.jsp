<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="契約成立・物件引渡し記録 | マチリク" />
  <jsp:param name="description" value="当事者間で成立した賃貸借契約の記録と、オーナー・事業主双方による物件引渡し確認を行います。" />
  <jsp:param name="pageScreenId" value="s14" />
  <jsp:param name="assetVersion" value="20261124_matching_v1" />
</jsp:include>

    <section id="s14-section" class="screen-view s14-view" data-screen="S14">
      <div class="page-header-banner" data-screen="S14">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a id="s14-bc-list" href="${pageContext.request.contextPath}/property/apply-list.jsp">出店希望確認</a>
          <span aria-hidden="true">/</span>
          <span aria-current="page">契約成立・物件引渡し記録</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">契約成立・物件引渡し記録</h1>
          <span id="s14-status-badge" class="badge-status status-reviewing">オーナー・事業主</span>
        </div>
        <p class="page-description">当事者間で成立した契約条件と物件引渡しの確認を記録します。双方の引渡し確認が完了すると、出店計画の公開へ進めます。</p>
      </div>

      <div id="s14-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <div class="matching-role-btns">
          <button type="button" id="s14-switch-owner-btn" class="btn-primary btn-sm">物件オーナーに切り替える</button>
          <button type="button" id="s14-switch-business-btn" class="btn-secondary btn-sm">出店事業主に切り替える</button>
        </div>
      </div>

      <div id="s14-empty-box" class="detail-card manage-empty-card" hidden>
        <h2>契約・引渡しの対象案件がまだありません</h2>
        <p>出店応募が「出店合意」または「契約成立」に進むと、ここで契約条件と鍵・物件の引渡し確認を記録できます。</p>
        <div class="manage-empty-actions">
          <a class="btn-primary" href="${pageContext.request.contextPath}/property/apply-list.jsp">オーナー側：出店希望確認へ</a>
          <a class="btn-secondary" href="${pageContext.request.contextPath}/matching/applications.jsp">事業主側：応募状況確認へ</a>
          <a class="btn-secondary" href="${pageContext.request.contextPath}/property/apply.jsp">出店応募画面を試す</a>
        </div>
      </div>

      <div id="s14-content-wrap">
        <div class="analytics-selector-bar">
          <div class="analytics-selector-group">
            <label for="s14-app-select">対象の契約・合意案件:</label>
            <select id="s14-app-select" class="form-select analytics-select"></select>
          </div>
          <div class="analytics-quick-links">
            <a id="s14-link-prop" class="btn-secondary btn-sm" href="${pageContext.request.contextPath}/property/detail.jsp">物件詳細を見る</a>
            <a id="s14-link-app" class="btn-secondary btn-sm" href="${pageContext.request.contextPath}/property/apply-detail.jsp">応募詳細に戻る</a>
          </div>
        </div>

        <div class="detail-card matching-steps-card">
          <h2 class="o10-card-heading">契約・引渡しの進行状況</h2>
          <ol id="s14-steps-list" class="opening-steps" aria-label="契約と引渡しの進み具合"></ol>
        </div>

        <div class="matching-legal-note" role="note">
          <strong>ご確認：</strong>本画面は当事者間（または宅建業者を介した手続き）で締結された賃貸借契約・短期利用契約の内容と引渡し完了をシステム上に記録するためのものです。マチリク上で契約締結の仲介自体は行いません。
        </div>

        <div class="o10-layout matching-two-col">
          <div class="o10-main-col">
            <section class="detail-card o10-card" aria-labelledby="s14-summary-heading">
              <h2 id="s14-summary-heading" class="o10-card-heading">対象物件・出店者サマリー</h2>
              <dl id="s14-summary-grid" class="o10-summary-grid"></dl>
            </section>

            <div class="form-stepper-box matching-form-card">
              <div class="stepper-header">
                <h2 id="s14-form-heading" class="prop-target-heading">契約条件・引渡し内容の記録</h2>
                <div id="s14-form-meta" class="prop-target-sub"></div>
              </div>

              <form id="s14-contract-form" action="${pageContext.request.contextPath}/matching/contract.jsp" method="POST" aria-labelledby="s14-form-heading">
                <fieldset class="form-section-block form-fieldset-reset">
                  <legend class="form-block-title">1. 契約の基本情報</legend>

                  <div class="form-group">
                    <label for="s14-store-name">出店店舗名（屋号） <span class="req-mark">*必須</span></label>
                    <input id="s14-store-name" type="text" name="storeName" class="form-control" required maxlength="150" aria-invalid="false" data-error="s14-store-name-error" placeholder="例: 日光焙煎珈琲スタンド オリオン通り店">
                    <div id="s14-store-name-error" class="form-error-msg">
                      <span role="img" aria-label="エラー">⚠</span> 出店店舗名を入力してください。
                    </div>
                  </div>

                  <div class="form-grid-2">
                    <div class="form-group">
                      <label for="s14-start-on">契約開始日 <span class="req-mark">*必須</span></label>
                      <input id="s14-start-on" type="date" name="contractStartOn" class="form-control" required aria-invalid="false" data-error="s14-start-on-error">
                      <div id="s14-start-on-error" class="form-error-msg">
                        <span role="img" aria-label="エラー">⚠</span> 契約開始日を選択してください。
                      </div>
                    </div>
                    <div class="form-group">
                      <label for="s14-end-on">契約終了日 <span class="req-mark">*必須</span></label>
                      <input id="s14-end-on" type="date" name="contractEndOn" class="form-control" required aria-invalid="false" data-error="s14-end-on-error">
                      <div id="s14-end-on-error" class="form-error-msg">
                        <span role="img" aria-label="エラー">⚠</span> 契約終了日は契約開始日以降の日付を指定してください。
                      </div>
                    </div>
                  </div>

                  <div class="form-grid-2">
                    <div class="form-group">
                      <label for="s14-agreed-rent">合意賃料（円・税込） <span class="req-mark">*必須</span></label>
                      <input id="s14-agreed-rent" type="text" inputmode="numeric" pattern="[0-9]+" name="agreedRent" class="form-control" required aria-invalid="false" data-error="s14-rent-error" placeholder="198000">
                      <div id="s14-rent-error" class="form-error-msg">
                        <span role="img" aria-label="エラー">⚠</span> 合意賃料を半角数字で入力してください。
                      </div>
                    </div>
                    <div class="form-group">
                      <label for="s14-confirmed-date">契約成立確認日 <span class="req-mark">*必須</span></label>
                      <input id="s14-confirmed-date" type="date" name="confirmedDate" class="form-control" required aria-invalid="false" data-error="s14-confirmed-date-error">
                      <div id="s14-confirmed-date-error" class="form-error-msg">
                        <span role="img" aria-label="エラー">⚠</span> 契約成立確認日を選択してください。
                      </div>
                    </div>
                  </div>

                  <div class="form-group">
                    <label for="s14-contract-term">契約種別・更新条件 <span class="opt-mark">（任意）</span></label>
                    <input id="s14-contract-term" type="text" name="contractTerm" class="form-control" maxlength="150" placeholder="例: 普通借家契約 2年間（更新可） / 短期ポップアップ利用">
                  </div>
                </fieldset>

                <fieldset class="form-section-block form-fieldset-reset">
                  <legend class="form-block-title">2. 引渡し時チェックリスト・特記事項</legend>

                  <div class="form-group">
                    <span class="field-label">現地引渡し確認項目 <span class="opt-mark">（確認できたものにチェック）</span></span>
                    <div class="matching-check-grid">
                      <div class="check-chip">
                        <input id="s14-chk-key" type="checkbox" name="handoverCheck" value="keys">
                        <label for="s14-chk-key">店舗鍵・セキュリティカードの受渡し</label>
                      </div>
                      <div class="check-chip">
                        <input id="s14-chk-equip" type="checkbox" name="handoverCheck" value="equipment">
                        <label for="s14-chk-equip">居抜き設備・残置物の状態確認</label>
                      </div>
                      <div class="check-chip">
                        <input id="s14-chk-meter" type="checkbox" name="handoverCheck" value="meters">
                        <label for="s14-chk-meter">電気・ガス・水道メーター確認</label>
                      </div>
                      <div class="check-chip">
                        <input id="s14-chk-sign" type="checkbox" name="handoverCheck" value="signage">
                        <label for="s14-chk-sign">改装範囲・看板設置条件の合意</label>
                      </div>
                    </div>
                  </div>

                  <div class="form-group">
                    <label for="s14-notes">合意特記事項・引渡しメモ <span class="opt-mark">（任意）</span></label>
                    <textarea id="s14-notes" name="handoverNotes" class="form-control auto-grow-textarea" maxlength="2000" placeholder="例: フリーレント1ヶ月適用。内装工事期間は○月○日まで。原状回復範囲は別途覚書のとおり。"></textarea>
                  </div>
                </fieldset>

                <div class="form-actions-bar form-actions-split">
                  <a id="s14-back-btn" class="btn-secondary" href="${pageContext.request.contextPath}/property/apply-list.jsp">一覧へ戻る</a>
                  <button type="submit" id="s14-save-btn" class="btn-primary">契約情報を保存する</button>
                </div>
              </form>
            </div>
          </div>

          <aside class="o10-side-col" aria-label="双方引渡し確認パネル">
            <section class="detail-card o10-card" aria-labelledby="s14-handover-heading">
              <h2 id="s14-handover-heading" class="o10-card-heading">双方の物件引渡し確認</h2>
              <p class="matching-panel-lead">契約情報を保存したあと、オーナーによる「引渡し確認」と事業主による「受領確認」の両方が完了すると、開店準備フェーズへ移行します。</p>

              <div class="matching-dual-confirm">
                <div class="matching-confirm-box" id="s14-owner-confirm-box">
                  <div class="matching-confirm-head">
                    <strong>オーナー側：引渡し確認</strong>
                    <span id="s14-owner-confirm-badge" class="req-badge-info">未確認</span>
                  </div>
                  <p id="s14-owner-confirm-time" class="matching-confirm-time">確認日時: —</p>
                  <button type="button" id="s14-owner-confirm-btn" class="btn-primary btn-sm">オーナーとして引渡し完了を記録</button>
                </div>

                <div class="matching-confirm-box" id="s14-biz-confirm-box">
                  <div class="matching-confirm-head">
                    <strong>事業主側：受領確認</strong>
                    <span id="s14-biz-confirm-badge" class="req-badge-info">未確認</span>
                  </div>
                  <p id="s14-biz-confirm-time" class="matching-confirm-time">確認日時: —</p>
                  <button type="button" id="s14-biz-confirm-btn" class="btn-primary btn-sm">事業主として受領完了を記録</button>
                </div>
              </div>

              <div id="s14-next-step-box" class="o10-handover-box" hidden>
                <p class="o10-handover-note"><strong>引渡し完了しました。</strong>住民に向けて開店予定日やお店づくりの進捗を公開できます。</p>
                <a id="s14-next-plan-link" class="btn-primary btn-sm" href="${pageContext.request.contextPath}/matching/opening-plan.jsp">出店計画の作成・公開へ進む →</a>
                <a id="s14-next-progress-link" class="btn-secondary btn-sm" href="${pageContext.request.contextPath}/matching/progress.jsp">開店のようすページを確認する</a>
              </div>
            </section>
          </aside>
        </div>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="s14" />
  <jsp:param name="assetVersion" value="20261124_matching_v1" />
</jsp:include>
