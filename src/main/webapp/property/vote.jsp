<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="欲しいお店に投票 | マチリク" />
  <jsp:param name="description" value="空き店舗に欲しい業種を順位投票します。" />
  <jsp:param name="pageScreenId" value="r01" />
  <jsp:param name="assetVersion" value="20261124_focus_v2" />
</jsp:include>

    <section id="r01-empty-state" class="screen-view empty-state" data-screen="R01" hidden>
      <h1 class="page-title">物件が見つかりません</h1>
      <p>掲載が終了したか、まだ物件が登録されていません。</p>
      <a class="btn-primary" href="${pageContext.request.contextPath}/index.jsp#s01">物件一覧へ</a>
    </section>

    <section id="r01-section" class="screen-view s03-view" data-screen="R01">
      <div class="page-header-banner" data-screen="R01">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a id="r01-bc-detail" href="${pageContext.request.contextPath}/property/detail.jsp">物件詳細</a>
          <span class="sep">/</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">欲しいお店に投票</h1>
          <span class="badge-status status-voting">地域住民</span>
        </div>
      </div>

      <div class="form-stepper-box">
        <div class="stepper-header">
          <h2 id="r01-prop-title" class="prop-target-heading">【対象物件】読み込み中…</h2>
          <div id="r01-prop-meta" class="prop-target-sub"></div>
        </div>

        <form id="r01-vote-form" action="${pageContext.request.contextPath}/property/vote.jsp" method="POST" aria-labelledby="r01-prop-title">
          <fieldset class="form-section-block form-fieldset-reset">
            <legend class="form-block-title">投票基準にする生活拠点を選択</legend>
            <div id="r01-location-list" class="location-radio-grid"></div>
          </fieldset>

          <div class="rank-picker-card">
            <span class="rank-badge-pill rank-badge-1">第1希望（必須）</span>
            <div class="form-group">
              <select id="r01-rank1" name="rank1" class="form-select" required aria-label="最も出店してほしい業種を選択してください" aria-invalid="false" data-error="r01-rank1-error"></select>
              <div id="r01-rank1-error" class="form-error-msg">第1希望の業種を1つ選択してください。</div>
            </div>
          </div>

          <div id="r01-rank2-card" class="rank-picker-card" hidden>
            <span class="rank-badge-pill rank-badge-2">第2希望（任意）</span>
            <div class="form-group">
              <select id="r01-rank2" name="rank2" class="form-select" aria-label="第2希望の業種を選択（任意）" aria-invalid="false"></select>
            </div>
          </div>

          <div id="r01-rank3-card" class="rank-picker-card" hidden>
            <span class="rank-badge-pill rank-badge-3">第3希望（任意）</span>
            <div class="form-group">
              <select id="r01-rank3" name="rank3" class="form-select" aria-label="第3希望の業種を選択（任意）" aria-invalid="false"></select>
            </div>
          </div>

          <fieldset class="survey-optional-box form-fieldset-reset">
            <legend class="form-block-title">店舗利用についての任意アンケート</legend>
            <div class="form-grid-2">
              <div class="form-group">
                <label for="r01-frequency">出店された場合の利用頻度想定</label>
                <select id="r01-frequency" name="frequency" class="form-select">
                  <option value="ほぼ毎日">ほぼ毎日</option>
                  <option value="週2〜3回">週2〜3回以上</option>
                  <option value="週1回程度" selected>週1回程度</option>
                  <option value="月2〜3回">月2〜3回</option>
                  <option value="月1回以下">月1回以下</option>
                </select>
              </div>
              <div class="form-group">
                <label for="r01-timing">主に利用したい時間帯</label>
                <select id="r01-timing" name="timing" class="form-select">
                  <option value="朝・モーニング（7:00〜9:00）">朝・モーニング</option>
                  <option value="ランチタイム（11:30〜14:00）">昼・ランチタイム</option>
                  <option value="カフェタイム（14:00〜17:00）" selected>午後・カフェタイム</option>
                  <option value="ディナー・夜（18:00〜22:00）">夜・ディナー・バータイム</option>
                </select>
              </div>
            </div>
            <div class="form-group" style="margin-top:0.9rem;">
              <label for="r01-comment">出店する事業主・店舗への要望や応援メッセージ（自由記述）</label>
              <textarea id="r01-comment" name="comment" class="form-control auto-grow-textarea" rows="3" maxlength="500" placeholder="例: 仕事帰りに立ち寄れるテイクアウト専門店や、ゆっくり読書できるカフェがあると嬉しいです。"></textarea>
            </div>
          </fieldset>

          <div class="form-actions-bar form-actions-split">
            <a id="r01-cancel-link" class="btn-secondary" href="${pageContext.request.contextPath}/property/detail.jsp">戻る</a>
            <button type="submit" id="r01-submit-btn" class="btn-primary">投票を確定して送信する</button>
          </div>

          <div class="vote-withdraw-row">
            <button type="button" id="r01-withdraw-btn" class="vote-withdraw-link" hidden>この投票を取り下げる</button>
          </div>
        </form>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="r01" />
  <jsp:param name="assetVersion" value="20261124_focus_v2" />
</jsp:include>
