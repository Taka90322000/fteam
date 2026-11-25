<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="jp.machiriku.tool.Html" %>
<%
  boolean isEditMode = "true".equalsIgnoreCase(request.getParameter("isEdit"));
  String headingId = request.getParameter("headingId");
  if (headingId == null || headingId.trim().isEmpty()) {
    headingId = isEditMode ? "o06-form-heading" : "o04-form-heading";
  }
  String formAction = request.getContextPath() + (isEditMode ? "/property/edit-confirm.jsp" : "/property/add-confirm.jsp");
%>
<form id="prop-editor-form" action="<%= Html.escape(formAction) %>" method="POST" aria-labelledby="<%= Html.escape(headingId) %>">
  <input id="prop-lat" type="hidden" name="lat" value="">
  <input id="prop-lng" type="hidden" name="lng" value="">

  <fieldset class="form-section-block form-fieldset-reset">
    <legend class="form-block-title">1. 基本情報・所在地</legend>
    <div class="form-group">
      <label for="prop-title">物件名称 <span class="req-mark">*必須</span></label>
      <input id="prop-title" type="text" name="title" class="form-control" maxlength="150" required aria-invalid="false" data-error="prop-title-error" placeholder="例: オリオン通りサウスビル 1階/101">
      <div id="prop-title-error" class="form-error-msg">
        <span role="img" aria-label="エラー">⚠</span> 物件名称を入力してください。
      </div>
    </div>

    <div class="form-grid-2">
      <div class="form-group">
        <label for="prop-pref">都道府県 <span class="req-mark">*必須</span></label>
        <input id="prop-pref" type="text" name="prefecture" class="form-control" maxlength="10" required autocomplete="address-level1" aria-invalid="false" data-error="prop-pref-error" placeholder="例: 栃木県">
        <div id="prop-pref-error" class="form-error-msg">
          <span role="img" aria-label="エラー">⚠</span> 都道府県を入力してください。
        </div>
      </div>
      <div class="form-group">
        <label for="prop-city">市区町村 <span class="req-mark">*必須</span></label>
        <input id="prop-city" type="text" name="municipality" class="form-control" maxlength="50" required autocomplete="address-level2" aria-invalid="false" data-error="prop-city-error" placeholder="例: 宇都宮市">
        <div id="prop-city-error" class="form-error-msg">
          <span role="img" aria-label="エラー">⚠</span> 市区町村を入力してください。
        </div>
      </div>
    </div>

    <div class="form-group">
      <label for="prop-address-line">町名・番地・建物名 <span class="req-mark">*必須</span></label>
      <div class="address-geocode-row">
        <input id="prop-address-line" type="text" name="addressLine" class="form-control" maxlength="200" required autocomplete="street-address" aria-invalid="false" data-error="prop-address-line-error" placeholder="例: 江野町5-12">
        <button type="button" id="prop-geocode-btn" class="btn-secondary">住所から地図ピンを取得</button>
      </div>
      <div id="prop-address-line-error" class="form-error-msg">
        <span role="img" aria-label="エラー">⚠</span> 町名・番地・建物名を入力してください。
      </div>
    </div>

    <div class="form-group">
      <span id="prop-map-label" class="field-label">地図上のピン位置（クリックまたはドラッグで住所を自動入力・スクロールで拡大縮小）</span>
      <div id="prop-map-picker" class="prop-map-picker-canvas" role="region" aria-labelledby="prop-map-label"></div>
      <p id="prop-map-status" class="map-picker-status" role="status" aria-live="polite">地図をクリックしてピンを置くと、町名・番地等が自動入力されます。</p>
    </div>

    <div class="form-group">
      <label for="prop-access">交通・最寄駅アクセス <span class="req-mark">*必須</span></label>
      <input id="prop-access" type="text" name="access" class="form-control" maxlength="150" required aria-invalid="false" data-error="prop-access-error" placeholder="例: 東武宇都宮駅 徒歩3分">
      <div id="prop-access-error" class="form-error-msg">
        <span role="img" aria-label="エラー">⚠</span> 交通・最寄駅アクセスを入力してください。
      </div>
    </div>
  </fieldset>

  <fieldset class="form-section-block form-fieldset-reset">
    <legend class="form-block-title">2. 賃料・面積・建物スペック</legend>
    <div class="form-grid-2">
      <div class="form-group">
        <label for="prop-rent">月額賃料 (円・税込) <span class="req-mark">*必須</span></label>
        <input id="prop-rent" type="number" name="rent" class="form-control" inputmode="numeric" min="0" required aria-invalid="false" data-error="prop-rent-error" placeholder="例: 180000">
        <div id="prop-rent-error" class="form-error-msg">
          <span role="img" aria-label="エラー">⚠</span> 0以上の月額賃料を入力してください。
        </div>
      </div>
      <div class="form-group">
        <label for="prop-mgmt">月額管理費・共益費 (円) <span class="opt-mark">（任意）</span></label>
        <input id="prop-mgmt" type="number" name="managementFee" class="form-control" inputmode="numeric" min="0" placeholder="空欄なら管理費等なし">
      </div>
    </div>

    <div class="form-grid-3">
      <div class="form-group">
        <label for="prop-deposit">敷金 <span class="opt-mark">（任意）</span></label>
        <select id="prop-deposit" name="deposit" class="form-select"></select>
      </div>
      <div class="form-group">
        <label for="prop-key-money">礼金 <span class="opt-mark">（任意）</span></label>
        <select id="prop-key-money" name="keyMoney" class="form-select"></select>
      </div>
      <div class="form-group">
        <label for="prop-security-deposit">保証金 <span class="opt-mark">（任意）</span></label>
        <select id="prop-security-deposit" name="securityDeposit" class="form-select"></select>
      </div>
    </div>

    <div class="form-grid-2">
      <div class="form-group">
        <label for="prop-area-sqm">使用部分面積 (㎡) <span class="req-mark">*必須</span></label>
        <input id="prop-area-sqm" type="number" step="0.01" min="0.1" name="areaSqm" class="form-control" inputmode="decimal" required aria-invalid="false" data-error="prop-area-sqm-error" placeholder="例: 50">
        <div id="prop-area-sqm-error" class="form-error-msg">
          <span role="img" aria-label="エラー">⚠</span> 0より大きい使用部分面積（㎡）を入力してください。
        </div>
      </div>
      <div class="form-group">
        <label for="prop-area-tsubo">使用部分面積 (坪・自動換算)</label>
        <input id="prop-area-tsubo" type="number" step="0.01" min="0.1" name="areaTsubo" class="form-control" inputmode="decimal" placeholder="㎡入力で自動換算">
      </div>
    </div>

    <div class="form-grid-3">
      <div class="form-group">
        <label for="prop-floor-num">所在階 <span class="opt-mark">（任意）</span></label>
        <select id="prop-floor-num" name="floorNumber" class="form-select"></select>
      </div>
      <div class="form-group">
        <label for="prop-total-floors">建物階数 <span class="opt-mark">（任意）</span></label>
        <select id="prop-total-floors" name="totalFloors" class="form-select"></select>
      </div>
      <div class="form-group">
        <label for="prop-structure">建物構造 <span class="opt-mark">（任意）</span></label>
        <select id="prop-structure" name="structure" class="form-select"></select>
      </div>
    </div>

    <div class="form-grid-2">
      <fieldset class="form-group form-fieldset-reset">
        <legend class="field-label">築年月 <span class="opt-mark">（任意）</span></legend>
        <div class="built-ym-row">
          <label for="prop-built-year" class="sr-only">築年</label>
          <select id="prop-built-year" name="builtYearNum" class="form-select"></select>
          <label for="prop-built-month" class="sr-only">築月</label>
          <select id="prop-built-month" name="builtMonthNum" class="form-select"></select>
        </div>
      </fieldset>
      <div class="form-group">
        <label for="prop-handover">引渡し時期・状態 <span class="opt-mark">（任意）</span></label>
        <select id="prop-handover" name="handover" class="form-select"></select>
      </div>
    </div>
  </fieldset>

  <fieldset class="form-section-block form-fieldset-reset">
    <legend class="form-block-title">3. 契約形態・公開ステータス・許可状況</legend>
    <fieldset class="form-group form-fieldset-reset">
      <legend class="field-label">募集する契約形態 <span class="req-mark">*必須</span></legend>
      <div class="radio-inline-group">
        <div class="radio-inline-card">
          <input id="prop-long-enabled" type="checkbox" name="longEnabled">
          <label for="prop-long-enabled"><span>通常賃貸（長期契約）を受け付ける</span></label>
        </div>
        <div class="radio-inline-card">
          <input id="prop-popup-enabled" type="checkbox" name="popupEnabled">
          <label for="prop-popup-enabled"><span>ポップアップ短期出店を受け付ける</span></label>
        </div>
      </div>
      <div id="prop-contract-error" class="form-error-msg" role="alert"></div>
      <div id="prop-popup-fields" class="popup-fields-box" hidden>
        <div class="form-grid-3">
          <div class="form-group">
            <label for="prop-popup-min-days">最低利用日数 (日) <span class="req-mark">*必須</span></label>
            <input id="prop-popup-min-days" type="number" name="popupMinDays" min="1" class="form-control" inputmode="numeric" disabled aria-invalid="false" data-error="prop-popup-min-error" placeholder="例: 3">
            <div id="prop-popup-min-error" class="form-error-msg">
              <span role="img" aria-label="エラー">⚠</span> 1日以上を入力してください。
            </div>
          </div>
          <div class="form-group">
            <label for="prop-popup-max-days">最大利用日数 (日) <span class="req-mark">*必須</span></label>
            <input id="prop-popup-max-days" type="number" name="popupMaxDays" min="1" class="form-control" inputmode="numeric" disabled aria-invalid="false" data-error="prop-popup-max-error" placeholder="例: 30">
            <div id="prop-popup-max-error" class="form-error-msg">
              <span role="img" aria-label="エラー">⚠</span> 最低利用日数以上を入力してください。
            </div>
          </div>
          <div class="form-group">
            <label for="prop-popup-daily-rent">日額賃料目安 (円) <span class="opt-mark">（任意）</span></label>
            <input id="prop-popup-daily-rent" type="number" name="popupDailyRent" min="0" class="form-control" inputmode="numeric" disabled placeholder="例: 8000">
          </div>
        </div>
      </div>
    </fieldset>

    <div class="form-grid-2">
      <div class="form-group">
        <label for="prop-permit-status">飲食店等 営業許可の現状 <span class="opt-mark">（任意）</span></label>
        <select id="prop-permit-status" name="permitStatus" class="form-select">
          <option value="">選択してください</option>
          <option value="UNKNOWN">要確認</option>
          <option value="OBTAINED">取得済み（引き継ぎ・居抜き相談可）</option>
          <option value="NOT_OBTAINED">未取得（借主側での新規取得相談可）</option>
        </select>
      </div>
      <div class="form-group">
        <label for="prop-listing-status">公開ステータス <span class="req-mark">*必須</span></label>
        <select id="prop-listing-status" name="listingStatus" class="form-select" required aria-invalid="false" data-error="prop-listing-status-error">
          <option value="">選択してください</option>
          <option value="OPEN">公開中（検索・投票受付）</option>
          <option value="DRAFT">下書き保存</option>
          <% if (isEditMode) { %>
          <option value="PAUSED">一時停止（新規募集停止）</option>
          <option value="CONTRACTED">契約済み</option>
          <% } %>
        </select>
        <div id="prop-listing-status-error" class="form-error-msg">
          <span role="img" aria-label="エラー">⚠</span> 公開ステータスを選択してください。
        </div>
      </div>
    </div>
  </fieldset>

  <fieldset class="form-section-block form-fieldset-reset">
    <legend class="form-block-title">4. 基本インフラ設備（10項目）＆ 希望・許可業種</legend>
    <div id="prop-infra-checkboxes" class="infra-checkbox-grid"></div>

    <div class="form-group">
      <span class="field-label">希望・許可する出店業種（複数選択可・未選択時は全業種相談可）</span>
      <div id="prop-allowed-industries" class="industry-groups-stack"></div>
    </div>

    <div class="form-group">
      <label for="prop-condition">居抜き・設備・業種条件メモ <span class="opt-mark">（任意）</span></label>
      <input id="prop-condition" type="text" name="condition" class="form-control" maxlength="300" placeholder="例: カフェ・物販相談可（重飲食は排気設備要確認）">
    </div>
  </fieldset>

  <fieldset class="form-section-block form-fieldset-reset">
    <legend class="form-block-title">5. 物件写真（並び順・種別指定）＆ アピール説明</legend>
    <div class="photo-upload-toolbar">
      <label for="prop-photo-file" class="btn-secondary btn-sm file-upload-label">
        <span>＋ 写真ファイルを追加</span>
        <input id="prop-photo-file" type="file" accept="image/*" multiple class="sr-only">
      </label>
    </div>
    <div id="prop-gallery-list" class="gallery-builder-list"></div>

    <div class="form-group">
      <label for="prop-description">物件の特徴・アピール説明文 <span class="opt-mark">（任意）</span></label>
      <textarea id="prop-description" name="description" class="form-control auto-grow-textarea" maxlength="2000" placeholder="立地の特徴、人通り、周辺環境、オーナーとしての想いなどをご記入ください。"></textarea>
    </div>

    <div class="form-group">
      <label for="prop-listing-url">既存の不動産掲載ページURL <span class="opt-mark">（任意）</span></label>
      <input id="prop-listing-url" type="url" name="listingUrl" class="form-control" maxlength="1000" autocomplete="url" autocorrect="off" autocapitalize="none" spellcheck="false" placeholder="https://...">
    </div>
  </fieldset>

  <div class="form-actions-bar form-actions-split">
    <a class="btn-secondary" href="${pageContext.request.contextPath}/property/manage.jsp">キャンセル</a>
    <button type="submit" id="prop-submit-btn" class="btn-primary"><%= isEditMode ? "変更内容を確認する →" : "登録内容を確認する →" %></button>
  </div>
</form>
