<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
  boolean isEditMode = "true".equalsIgnoreCase(request.getParameter("isEdit"));
%>
<form id="prop-editor-form">
  <div class="form-section-block">
    <h2 class="form-block-title">1. 基本情報・所在地（Google Map 座標連携）</h2>
    <div class="form-group">
      <label for="prop-title">物件名称 <span class="req-mark">*必須</span></label>
      <input id="prop-title" type="text" name="title" class="form-control" maxlength="150" required placeholder="例: オリオン通りサウスビル 1階/101">
    </div>

    <div class="form-grid-2">
      <div class="form-group">
        <label for="prop-pref">都道府県 <span class="req-mark">*必須</span></label>
        <input id="prop-pref" type="text" name="prefecture" class="form-control" maxlength="10" required value="栃木県">
      </div>
      <div class="form-group">
        <label for="prop-city">市区町村 <span class="req-mark">*必須</span></label>
        <input id="prop-city" type="text" name="municipality" class="form-control" maxlength="50" required value="宇都宮市">
      </div>
    </div>

    <div class="form-group">
      <label for="prop-address-line">町名・番地・建物名 <span class="req-mark">*必須</span></label>
      <div class="address-geocode-row">
        <input id="prop-address-line" type="text" name="addressLine" class="form-control" maxlength="200" required placeholder="例: 江野町5-12">
        <button type="button" id="prop-geocode-btn" class="btn-secondary">住所から地図ピンを取得</button>
      </div>
    </div>

    <div class="form-group">
      <label>地図上のピン位置（クリックまたはドラッグで微調整できます）</label>
      <div id="prop-map-picker" class="prop-map-picker-canvas"></div>
      <div class="form-grid-2" style="margin-top:0.5rem;">
        <div class="form-group">
          <label for="prop-lat">緯度（latitude）</label>
          <input id="prop-lat" type="number" step="0.000001" name="lat" class="form-control" value="36.5610">
        </div>
        <div class="form-group">
          <label for="prop-lng">経度（longitude）</label>
          <input id="prop-lng" type="number" step="0.000001" name="lng" class="form-control" value="139.8830">
        </div>
      </div>
    </div>

    <div class="form-group">
      <label for="prop-access">交通・最寄駅アクセス <span class="req-mark">*必須</span></label>
      <input id="prop-access" type="text" name="access" class="form-control" required placeholder="例: 東武宇都宮駅 徒歩3分">
    </div>
  </div>

  <div class="form-section-block">
    <h2 class="form-block-title">2. 賃料・面積・建物スペック</h2>
    <div class="form-grid-2">
      <div class="form-group">
        <label for="prop-rent">月額賃料 (円・税込) <span class="req-mark">*必須</span></label>
        <input id="prop-rent" type="number" name="rent" class="form-control" min="0" required value="180000">
      </div>
      <div class="form-group">
        <label for="prop-mgmt">月額管理費・共益費 (円)</label>
        <input id="prop-mgmt" type="number" name="managementFee" class="form-control" min="0" placeholder="空欄なら管理費等なし">
      </div>
    </div>

    <div class="form-grid-3">
      <div class="form-group">
        <label for="prop-deposit">敷金</label>
        <input id="prop-deposit" type="text" name="deposit" class="form-control" placeholder="例: 2ヶ月 / なし">
      </div>
      <div class="form-group">
        <label for="prop-key-money">礼金</label>
        <input id="prop-key-money" type="text" name="keyMoney" class="form-control" placeholder="例: 1ヶ月 / なし">
      </div>
      <div class="form-group">
        <label for="prop-security-deposit">保証金</label>
        <input id="prop-security-deposit" type="text" name="securityDeposit" class="form-control" placeholder="例: 30万円 / なし">
      </div>
    </div>

    <div class="form-grid-2">
      <div class="form-group">
        <label for="prop-area-sqm">使用部分面積 (㎡) <span class="req-mark">*必須</span></label>
        <input id="prop-area-sqm" type="number" step="0.01" min="0.1" name="areaSqm" class="form-control" required value="50">
      </div>
      <div class="form-group">
        <label for="prop-area-tsubo">使用部分面積 (坪・自動換算)</label>
        <input id="prop-area-tsubo" type="number" step="0.01" min="0.1" name="areaTsubo" class="form-control" value="15.13">
      </div>
    </div>

    <div class="form-grid-3">
      <div class="form-group">
        <label for="prop-floor-num">所在階</label>
        <input id="prop-floor-num" type="text" name="floorNumber" class="form-control" placeholder="例: 1階">
      </div>
      <div class="form-group">
        <label for="prop-total-floors">建物階数</label>
        <input id="prop-total-floors" type="text" name="totalFloors" class="form-control" placeholder="例: 4階建">
      </div>
      <div class="form-group">
        <label for="prop-structure">建物構造</label>
        <input id="prop-structure" type="text" name="structure" class="form-control" placeholder="例: 鉄骨造">
      </div>
    </div>

    <div class="form-grid-2">
      <div class="form-group">
        <label for="prop-built-year">築年月</label>
        <input id="prop-built-year" type="text" name="builtYear" class="form-control" placeholder="例: 1995年3月">
      </div>
      <div class="form-group">
        <label for="prop-handover">引渡し時期・状態</label>
        <input id="prop-handover" type="text" name="handover" class="form-control" value="相談">
      </div>
    </div>
  </div>

  <div class="form-section-block">
    <h2 class="form-block-title">3. 契約形態（通常・ポップアップ）・公開ステータス・許可状況</h2>
    <div class="form-group">
      <label class="field-label">募集する契約形態</label>
      <div class="radio-inline-group">
        <label class="radio-inline-card">
          <input id="prop-long-enabled" type="checkbox" checked>
          <span>通常賃貸（長期契約）を受け付ける</span>
        </label>
        <label class="radio-inline-card">
          <input id="prop-popup-enabled" type="checkbox">
          <span>ポップアップ短期出店を受け付ける</span>
        </label>
      </div>
      <div id="prop-popup-fields" class="popup-fields-box" hidden>
        <div class="form-grid-3">
          <div class="form-group">
            <label for="prop-popup-min-days">最低利用日数 (日)</label>
            <input id="prop-popup-min-days" type="number" min="1" class="form-control" value="3">
          </div>
          <div class="form-group">
            <label for="prop-popup-max-days">最大利用日数 (日)</label>
            <input id="prop-popup-max-days" type="number" min="1" class="form-control" value="30">
          </div>
          <div class="form-group">
            <label for="prop-popup-daily-rent">日額賃料目安 (円)</label>
            <input id="prop-popup-daily-rent" type="number" min="0" class="form-control" placeholder="例: 8000">
          </div>
        </div>
      </div>
    </div>

    <div class="form-grid-2">
      <div class="form-group">
        <label for="prop-permit-status">飲食店等 営業許可の現状</label>
        <select id="prop-permit-status" name="permitStatus" class="form-select">
          <option value="UNKNOWN">要確認</option>
          <option value="OBTAINED">取得済み（引き継ぎ・居抜き相談可）</option>
          <option value="NOT_OBTAINED">未取得（借主側での新規取得相談可）</option>
        </select>
      </div>
      <div class="form-group">
        <label for="prop-listing-status">公開ステータス</label>
        <select id="prop-listing-status" name="listingStatus" class="form-select">
          <option value="OPEN">公開中（検索・投票受付）</option>
          <option value="DRAFT">下書き保存</option>
          <% if (isEditMode) { %>
          <option value="PAUSED">一時停止（新規募集停止）</option>
          <option value="CONTRACTED">契約済み</option>
          <% } %>
        </select>
      </div>
    </div>
  </div>

  <div class="form-section-block">
    <h2 class="form-block-title">4. 基本インフラ設備（10項目）＆ 希望・許可業種</h2>
    <div id="prop-infra-checkboxes" class="infra-checkbox-grid"></div>

    <div class="form-group" style="margin-top:1.1rem;">
      <label class="field-label">希望・許可する出店業種（複数選択可・未選択時は全業種相談可）</label>
      <div id="prop-allowed-industries" class="industry-groups-stack"></div>
    </div>

    <div class="form-group" style="margin-top:1rem;">
      <label for="prop-condition">居抜き・設備・業種条件メモ</label>
      <input id="prop-condition" type="text" name="condition" class="form-control" placeholder="例: カフェ・物販相談可（重飲食は排気設備要確認）">
    </div>
  </div>

  <div class="form-section-block">
    <h2 class="form-block-title">5. 物件写真（並び順・種別指定）＆ アピール説明</h2>
    <div class="photo-upload-toolbar">
      <label class="btn-secondary btn-sm file-upload-label">
        <span>＋ 写真ファイルを追加</span>
        <input id="prop-photo-file" type="file" accept="image/*" multiple hidden>
      </label>
      <button type="button" id="prop-add-sample-photo" class="btn-secondary btn-sm">サンプル写真を追加</button>
    </div>
    <div id="prop-gallery-list" class="gallery-builder-list"></div>

    <div class="form-group" style="margin-top:1rem;">
      <label for="prop-description">物件の特徴・アピール説明文</label>
      <textarea id="prop-description" name="description" class="form-control" rows="4" placeholder="立地の特徴、人通り、周辺環境、オーナーとしての想いなどをご記入ください。"></textarea>
    </div>

    <div class="form-group">
      <label for="prop-listing-url">既存の不動産掲載ページURL（任意）</label>
      <input id="prop-listing-url" type="url" name="listingUrl" class="form-control" placeholder="https://...">
    </div>
  </div>

  <div class="form-actions-bar">
    <span></span>
    <div class="form-actions-right">
      <a class="btn-secondary" href="${pageContext.request.contextPath}/property/manage.jsp">キャンセル</a>
      <button type="submit" class="btn-primary"><%= isEditMode ? "変更内容を確認する →" : "登録内容を確認する →" %></button>
    </div>
  </div>
</form>
