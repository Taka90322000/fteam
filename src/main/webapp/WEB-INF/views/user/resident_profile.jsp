<%-- 地域住民のプロフィール・基本情報設定画面。登録済み会員専用。後から項目を追加しやすいセクション構造。 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List,java.util.Map,com.fasterxml.jackson.databind.ObjectMapper,jp.machiriku.bean.User,jp.machiriku.tool.Html" %>
<%
  String displayName = (String) request.getAttribute("displayName");
  if (displayName == null) displayName = "";
  String email = (String) request.getAttribute("email");
  if (email == null) email = "";
  Map<String, String> formValues = request.getAttribute("formValues") instanceof Map
      ? (Map<String, String>) request.getAttribute("formValues") : java.util.Collections.emptyMap();
  displayName = formValues.getOrDefault("name", displayName);
  email = formValues.getOrDefault("email", email);
  Object rawTaxonomy = request.getAttribute("residentTaxonomy");
  String jsonResidentTaxonomy = new ObjectMapper()
      .writeValueAsString(rawTaxonomy instanceof List ? rawTaxonomy : java.util.Collections.emptyList());
%>
<jsp:include page="/WEB-INF/views/common/header.jsp">
  <jsp:param name="title" value="プロフィール・基本情報設定 | マチリク" />
  <jsp:param name="isOnboarding" value="false" />
  <jsp:param name="roleLabel" value="地域住民" />
  <jsp:param name="userName" value="<%= displayName %>" />
</jsp:include>

<section class="screen-view s18-view" data-screen="S18">
  <div class="screen-header-banner" style="background:var(--primary-50); border:1px solid var(--primary-100); border-radius:var(--radius-lg); padding:1rem 1.25rem; margin-bottom:1.5rem; display:flex; align-items:center; gap:1rem;">
    <span class="screen-header-badge" style="background:var(--primary-700); color:var(--text-inverse); font-weight:800; font-size:0.85rem; padding:0.25rem 0.6rem; border-radius:var(--radius-full);">S18</span>
    <div>
      <h1 class="screen-header-title" style="font-size:1.25rem; font-weight:800; color:var(--primary-900); margin:0;">プロフィール・基本情報設定</h1>
      <p class="screen-header-desc" style="font-size:0.85rem; color:var(--text-sub); margin:0.25rem 0 0 0;">登録情報の確認や変更ができます。</p>
    </div>
  </div>

  <div class="form-stepper-box" style="max-width: 680px; margin:0 auto; background:var(--bg-card); padding:2rem; border-radius:var(--radius-lg); border:1px solid var(--border-color); box-shadow:var(--shadow-sm);">
    <% if (request.getAttribute("errorMessage") != null) { %>
      <div role="alert" style="background:#fee2e2; color:#991b1b; padding:0.75rem 1rem; border-radius:var(--radius-md); margin-bottom:1rem;"><%= Html.escape(request.getAttribute("errorMessage")) %></div>
    <% } %>

    <form id="resident-profile-form" action="${pageContext.request.contextPath}/jp/machiriku/servlet/user/Profile.action" method="POST" onsubmit="return validateResidentForm();">
      <input type="hidden" name="csrfToken" value="<%= Html.escape(request.getAttribute("csrfToken")) %>">

      <!-- ========================================================
           【セクション1: 基本情報】ニックネーム・メールアドレス
           ======================================================== -->
      <div class="profile-section-block" style="margin-bottom:1.5rem;">
        <!-- ニックネーム (表示名) -->
        <div class="form-group" style="margin-bottom:1.25rem;">
          <label for="s18-name" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
            ニックネーム (表示名) <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
          </label>
          <input id="s18-name" name="name" type="text" class="form-control" value="<%= Html.escape(displayName) %>" maxlength="100" required placeholder="例: 山田 太郎" oninput="const h = document.getElementById('header-user-name'); if(h) h.textContent = this.value.trim();">
          <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※マチリク内で表示されるお名前です。</span>
        </div>

        <!-- メールアドレス -->
        <div class="form-group" style="margin-bottom:1.25rem;">
          <label for="s18-email" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
            メールアドレス <span style="font-size:0.78rem; color:var(--text-sub);">(任意・通知受信用)</span>
          </label>
          <input id="s18-email" name="email" type="email" class="form-control" value="<%= Html.escape(email) %>" maxlength="254" placeholder="example@domain.com">
          <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※重要なお知らせや新着情報の配信先メールアドレスです。</span>
        </div>
      </div>

      <!-- ========================================================
           【セクション2: 属性情報】年齢・性別
           ======================================================== -->
      <div class="profile-section-block" style="margin-bottom:1.5rem; padding-top:0.5rem; border-top:1px solid var(--border-color);">
        <!-- 年齢 (任意・ラジオボタン) -->
        <div class="form-group" style="margin-bottom:1.25rem;">
          <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
            年齢 <span style="color:var(--text-muted); font-size:0.8rem;">(任意)</span>
          </label>
          <div class="setup-age-grid" style="display:grid; grid-template-columns:repeat(7, minmax(0, 1fr)); gap:0.35rem; width:100%; box-sizing:border-box;">
            <%
              String[] ageOptions = {"未設定", "10代", "20代", "30代", "40代", "50代", "60代以上"};
              for (String opt : ageOptions) {
            %>
              <label style="display:flex; align-items:center; justify-content:center; gap:0.25rem; padding:0.45rem 0.15rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.8125rem; white-space:nowrap; text-align:center; box-sizing:border-box;">
                <input type="radio" name="ageGroup" value="<%= Html.escape(opt) %>" <%= formValues.getOrDefault("ageGroup", "未設定").equals(opt) ? "checked" : "" %> style="cursor:pointer; margin:0; flex-shrink:0;">
                <span><%= Html.escape(opt) %></span>
              </label>
            <% } %>
          </div>
        </div>

        <!-- 性別 (任意・ラジオボタン) -->
        <div class="form-group" style="margin-bottom:1.25rem;">
          <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
            性別 <span style="color:var(--text-muted); font-size:0.8rem;">(任意)</span>
          </label>
          <div style="display:flex; flex-wrap:wrap; gap:0.5rem;">
            <%
              String[] genderOptions = {"回答しない", "男性", "女性", "その他"};
              for (String opt : genderOptions) {
            %>
              <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.85rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.875rem;">
                <input type="radio" name="gender" value="<%= Html.escape(opt) %>" <%= formValues.getOrDefault("gender", "回答しない").equals(opt) ? "checked" : "" %> style="cursor:pointer;">
                <span><%= Html.escape(opt) %></span>
              </label>
            <% } %>
          </div>
        </div>
      </div>

      <!-- ========================================================
           【セクション3: 希望業種・施設ジャンル】ピル選択
           ======================================================== -->
      <div class="profile-section-block" style="margin-bottom:1.5rem; padding-top:0.5rem; border-top:1px solid var(--border-color);">
        <div class="form-group resident-industry-picker" data-taxonomy="<%= Html.escape(jsonResidentTaxonomy) %>">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.45rem; flex-wrap:wrap; gap:0.4rem;">
            <label style="font-weight:800; font-size:0.92rem; color:var(--text-main); display:flex; align-items:center; gap:0.35rem;">
              <span>近くに欲しい店舗・施設ジャンル</span>
              <span style="color:var(--text-muted); font-size:0.78rem; font-weight:normal;">(任意・複数選択可)</span>
            </label>
            <span id="s18-resident-ind-count" style="font-size:0.75rem; font-weight:700; color:var(--primary-700); background:var(--primary-50); border:1px solid var(--primary-100); padding:0.15rem 0.55rem; border-radius:var(--radius-full);">
              選択中: 0件
            </span>
          </div>

          <!-- 選択中のピル一覧（タグエリア） -->
          <div id="s18-resident-selected-tags" class="resident-selected-tags-box">
            <span style="font-size:0.75rem; color:var(--text-muted); padding:0.05rem 0.2rem;">※下のタブからジャンルを選んでください（複数選択可）</span>
          </div>

          <!-- 大分類タブバー -->
          <div style="margin-bottom:0.45rem;">
            <div class="resident-cat-tabs-bar" id="s18-resident-cat-tabs"></div>
          </div>

          <!-- 小分類ピル（チップ）選択エリア -->
          <div id="s18-resident-subcat-panel" class="resident-subcat-grid"></div>

          <!-- フォーム送信用の隠しフィールド -->
          <input type="hidden" id="s18-preferred-industries" name="preferredIndustries" value="<%= Html.escape(formValues.getOrDefault("preferredIndustries", "[]")) %>">
          <input type="hidden" id="s18-industry" name="industry" value="<%= Html.escape(formValues.getOrDefault("industry", "")) %>">
        </div>
      </div>

      <!-- ========================================================
           【セクション4: 拠点・エリア情報】居住地・職場・学校
           ======================================================== -->
      <div class="profile-section-block" style="margin-bottom:1.5rem; padding-top:0.5rem; border-top:1px solid var(--border-color);">
        <!-- 居住地の住所 (マップ上で選択・必須) -->
        <div class="form-group" style="margin-bottom:1.5rem; background:var(--bg-subtle); padding:var(--space-4); border-radius:var(--radius-lg); border:1px solid var(--border-color);">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.35rem; flex-wrap:wrap; gap:0.5rem;">
            <label for="s18-living-address" style="font-weight:800; font-size:0.95rem; color:var(--text-main);">
              居住地の住所 <span style="color:var(--danger-text); font-size:0.8rem;">*必須 (マップ上で選択)</span>
            </label>
            <button type="button" class="btn-outline btn-sm" onclick="handleLivingCurrentLocation()" style="font-size:0.8rem; padding:0.3rem 0.75rem; border-radius:var(--radius-full); display:inline-flex; align-items:center; gap:0.35rem; background:var(--bg-card); cursor:pointer;">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/><line x1="12" y1="2" x2="12" y2="6"/><line x1="12" y1="18" x2="12" y2="22"/><line x1="2" y1="12" x2="6" y2="12"/><line x1="18" y1="12" x2="22" y2="12"/></svg>
              <span>現在地から自動設定</span>
            </button>
          </div>
          <p style="font-size:0.8rem; color:var(--text-sub); margin-bottom:0.6rem; line-height:1.4;">
            地図上をクリックするかピンをドラッグして、お住まいの居住地を設定してください。
          </p>

          <!-- 地図コンテナ -->
          <div id="s18-map-picker" class="s18-map-picker" style="width:100%; height:260px; border-radius:var(--radius-md); border:1.5px solid var(--border-color); background-color:var(--bg-subtle); position:relative; overflow:hidden;"></div>

          <div style="margin-top:0.75rem;">
            <label for="s18-living-address" style="font-size:0.8rem; font-weight:700; color:var(--text-sub); display:block; margin-bottom:0.25rem;">
              選択中の居住地住所:
            </label>
            <input id="s18-living-address" name="livingAddress" type="text" class="form-control" value="<%= Html.escape(formValues.get("livingAddress")) %>" required readonly placeholder="地図をクリックすると自動で住所が設定されます">
            <input id="s18-living-lat" name="livingLat" type="hidden" value="<%= Html.escape(formValues.get("livingLat")) %>">
            <input id="s18-living-lng" name="livingLng" type="hidden" value="<%= Html.escape(formValues.get("livingLng")) %>">
            <div id="s18-map-status" style="font-size:0.75rem; color:var(--primary-700); margin-top:0.35rem; font-weight:600;">
              ※地図をクリックして居住地を選択してください
            </div>
          </div>
        </div>

        <!-- 職場や学校などの住所 (任意) -->
        <div class="form-group" style="margin-bottom:1.5rem;">
          <!-- 登録前・折りたたみ時のブロックスイッチ -->
          <button type="button" id="s18-work-trigger-card" class="work-register-card-trigger" onclick="toggleWorkMap(true)" style="display:flex; width:100%; justify-content:space-between; align-items:center; padding:1.1rem 1.25rem; background:var(--bg-card); border:1.5px solid var(--border-color); border-radius:var(--radius-md); cursor:pointer; text-align:left; box-sizing:border-box;">
            <span id="s18-work-trigger-label" style="font-size:0.95rem; font-weight:700; color:var(--text-main);">職場や学校を登録する</span>
            <span id="s18-work-trigger-icon" style="font-size:1.25rem; color:var(--text-sub); line-height:1; font-weight:700;">＋</span>
          </button>

          <!-- クリック時に展開されるブロック -->
          <div id="s18-work-map-area" style="display:none; background:var(--bg-subtle); padding:var(--space-4); border-radius:var(--radius-lg); border:1px solid var(--border-color);">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.4rem; flex-wrap:wrap; gap:0.5rem;">
              <label for="s18-work-address" style="font-weight:800; font-size:0.95rem; color:var(--text-main); margin:0;">
                職場や学校の住所 <span style="color:var(--text-muted); font-size:0.8rem;">(任意・マップ上で選択)</span>
              </label>
              <div style="display:flex; align-items:center; gap:0.5rem;">
                <button type="button" class="btn-outline btn-sm" onclick="handleWorkCurrentLocation()" style="font-size:0.8rem; padding:0.3rem 0.75rem; border-radius:var(--radius-full); display:inline-flex; align-items:center; gap:0.35rem; background:var(--bg-card); cursor:pointer;">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/><line x1="12" y1="2" x2="12" y2="6"/><line x1="12" y1="18" x2="12" y2="22"/><line x1="2" y1="12" x2="6" y2="12"/><line x1="18" y1="12" x2="22" y2="12"/></svg>
                  <span>現在地から自動設定</span>
                </button>
                <button type="button" class="btn-outline btn-sm" onclick="removeWorkLocation()" style="font-size:0.8rem; padding:0.3rem 0.65rem; border-radius:var(--radius-full); display:inline-flex; align-items:center; gap:0.25rem; background:var(--bg-card); color:var(--danger-text, #991b1b); cursor:pointer;">
                  <span>登録を解除</span>
                </button>
                <button type="button" class="btn-outline btn-sm" onclick="toggleWorkMap(false)" style="font-size:0.8rem; padding:0.3rem 0.65rem; border-radius:var(--radius-full); display:inline-flex; align-items:center; gap:0.25rem; background:var(--bg-card); cursor:pointer;">
                  <span>✕ 閉じる</span>
                </button>
              </div>
            </div>
            <p style="font-size:0.8rem; color:var(--text-sub); margin-bottom:0.6rem; line-height:1.4;">
              地図上をクリックするかピンをドラッグして、勤務先や学校を設定してください。
            </p>

            <!-- 地図コンテナ -->
            <div id="s18-work-map-picker" class="s18-map-picker" style="width:100%; height:260px; border-radius:var(--radius-md); border:1.5px solid var(--border-color); background-color:var(--bg-subtle); position:relative; overflow:hidden;"></div>

            <div style="margin-top:0.75rem;">
              <label for="s18-work-address" style="font-size:0.8rem; font-weight:700; color:var(--text-sub); display:block; margin-bottom:0.25rem;">
                選択中の職場・学校住所:
              </label>
              <input id="s18-work-address" name="workAddress" type="text" class="form-control" value="<%= Html.escape(formValues.get("workAddress")) %>" readonly placeholder="地図をクリックすると自動で住所が設定されます">
              <input id="s18-work-lat" name="workLat" type="hidden" value="<%= Html.escape(formValues.get("workLat")) %>">
              <input id="s18-work-lng" name="workLng" type="hidden" value="<%= Html.escape(formValues.get("workLng")) %>">
              <div id="s18-work-map-status" style="font-size:0.75rem; color:var(--primary-700); margin-top:0.35rem; font-weight:600;">
                ※地図をクリックしてピンを配置してください
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ========================================================
           【セクション5: 通知設定】メール受信設定
           ======================================================== -->
      <div class="profile-section-block" style="margin-bottom:1.5rem; padding-top:0.5rem; border-top:1px solid var(--border-color);">
        <label style="display:flex; align-items:center; gap:0.65rem; cursor:pointer; font-size:0.875rem; color:var(--text-main); font-weight:600; line-height:1.45; padding:0.75rem 1rem; background:var(--bg-subtle); border-radius:var(--radius-md); border:1px solid var(--border-color); box-sizing:border-box; min-height:48px;">
          <input type="checkbox" id="s18-email-notify" name="emailNotify" <%= "on".equals(formValues.get("emailNotify")) ? "checked" : "" %> style="cursor:pointer; width:18px; height:18px; margin:0; flex-shrink:0;">
          <span>新着店舗や新着クーポン情報をメールで受け取る</span>
        </label>
      </div>

      <!-- ========================================================
           【セクション6: 将来の拡張用エリア】
           ※今後プロフィールに追加したい項目（例: 自己紹介、興味のあるエリア、SNS連携など）は
             このブロック内にフォーム要素を追記できます。
           ======================================================== -->
      <!--
      <div class="profile-section-block profile-custom-extensions" style="margin-bottom:1.5rem; padding-top:0.5rem; border-top:1px solid var(--border-color);">
        <div class="form-group" style="margin-bottom:1.25rem;">
          <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">追加項目</label>
          <input type="text" class="form-control" placeholder="追加項目の入力例">
        </div>
      </div>
      -->

      <!-- 送信ボタン -->
      <div style="margin-top:2rem;">
        <button type="submit" class="btn-primary" style="width:100%; min-height:48px; font-size:1.05rem; font-weight:800; display:flex; align-items:center; justify-content:center; gap:0.5rem; box-shadow:var(--shadow-floating); cursor:pointer;">
          <span>設定を保存する</span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
      </div>
    </form>

    <div style="margin-top:1.5rem; text-align:center;">
      <form action="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Logout.action" method="POST" style="display:inline; margin:0;" onsubmit="return confirm('ログアウトしますか？');">
        <input type="hidden" name="csrfToken" value="<%= Html.escape(request.getAttribute("csrfToken")) %>">
        <button type="submit" style="background:none; border:none; padding:0; color:var(--text-sub); font-size:0.875rem; text-decoration:underline; cursor:pointer; font-family:inherit;">
          ログアウト
        </button>
      </form>
    </div>
  </div>
</section>

<script src="${pageContext.request.contextPath}/js/resident-industries.js"></script>

<!-- Google Maps JavaScript API -->
<script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyCNWrNbVnBfZsjuTJ3sHCKDemfkxNsIv7w&libraries=marker&language=ja&region=JP"></script>

<script>
  const s18Locations = {
    living: { map: null, marker: null, revision: 0, positionRequestId: 0, geocodeState: 'idle', mapId: 's18-map-picker', statusId: 's18-map-status' },
    work: { map: null, marker: null, revision: 0, positionRequestId: 0, geocodeState: 'idle', mapId: 's18-work-map-picker', statusId: 's18-work-map-status' }
  };
  const s18MapCenter = { lat: 36.5610, lng: 139.8830 };

  function readResidentCoords(kind) {
    const latValue = document.getElementById('s18-' + kind + '-lat').value.trim();
    const lngValue = document.getElementById('s18-' + kind + '-lng').value.trim();
    if (!latValue || !lngValue) return null;
    const lat = Number(latValue);
    const lng = Number(lngValue);
    return Number.isFinite(lat) && Number.isFinite(lng) && Math.abs(lat) <= 90 && Math.abs(lng) <= 180 ? { lat, lng } : null;
  }

  function validateResidentForm() {
    const nameInput = document.getElementById('s18-name');
    const addrInput = document.getElementById('s18-living-address');
    if (!nameInput.value.trim()) {
      alert('ニックネームを入力してください');
      nameInput.focus();
      return false;
    }
    if (s18Locations.living.geocodeState === 'loading') {
      alert('選択した居住地の住所を確認しています。住所が表示されてから保存してください');
      return false;
    }
    if (!addrInput.value.trim() || !readResidentCoords('living')) {
      alert('居住地を地図上で選択してください（必須）');
      addrInput.focus();
      return false;
    }
    const workInput = document.getElementById('s18-work-address');
    const workCoords = readResidentCoords('work');
    if (s18Locations.work.geocodeState === 'loading') {
      alert('選択した職場・学校の住所を確認しています。住所が表示されてから保存してください');
      toggleWorkMap(true);
      return false;
    }
    if (s18Locations.work.geocodeState === 'error') {
      alert('職場・学校の住所を取得できませんでした。地図上で選び直すか、「登録を解除」を押してください');
      toggleWorkMap(true);
      return false;
    }
    if ((workInput.value.trim() && !workCoords) || (!workInput.value.trim() && workCoords)) {
      alert('職場・学校を登録する場合は地図上で住所を選択してください（削除する場合は「登録を解除」を押してください）');
      toggleWorkMap(true);
      workInput.focus();
      return false;
    }
    return true;
  }

  function updateResidentStatus(kind, text) {
    document.getElementById(s18Locations[kind].statusId).textContent = text;
  }

  function clearResidentCoords(kind) {
    const location = s18Locations[kind];
    location.revision++;
    location.geocodeState = 'idle';
    document.getElementById('s18-' + kind + '-lat').value = '';
    document.getElementById('s18-' + kind + '-lng').value = '';
    if (location.marker) location.marker.setMap(null);
    updateResidentStatus(kind, '※地図上で住所を選択してください');
  }

  function removeWorkLocation() {
    document.getElementById('s18-work-address').value = '';
    clearResidentCoords('work');
    toggleWorkMap(false);
  }

  function showResidentMarker(kind, point) {
    const location = s18Locations[kind];
    if (!location.map) return;
    if (!location.marker) {
      location.marker = new google.maps.Marker({ map: location.map, position: point, draggable: true });
      location.marker.addListener('dragend', (event) => selectResidentLocation(kind, {
        lat: event.latLng.lat(), lng: event.latLng.lng()
      }));
    } else {
      location.marker.setMap(location.map);
      location.marker.setPosition(point);
    }
  }

  function selectResidentLocation(kind, point) {
    const location = s18Locations[kind];
    const revision = ++location.revision;
    location.geocodeState = 'loading';
    const addressInput = document.getElementById('s18-' + kind + '-address');
    document.getElementById('s18-' + kind + '-lat').value = point.lat;
    document.getElementById('s18-' + kind + '-lng').value = point.lng;
    addressInput.value = '';
    showResidentMarker(kind, point);
    if (location.map) location.map.panTo(point);
    updateResidentStatus(kind, '選択した位置の住所を確認しています…');
    if (typeof google === 'undefined' || !google.maps || !google.maps.Geocoder) {
      location.geocodeState = 'error';
      updateResidentStatus(kind, '住所を取得できませんでした。地図の読み込み後に選び直してください');
      return;
    }
    new google.maps.Geocoder().geocode({ location: point, language: 'ja', region: 'JP' }, (results, status) => {
      if (revision !== location.revision) return;
      if (status === 'OK' && results && results[0] && results[0].formatted_address) {
        location.geocodeState = 'resolved';
        addressInput.value = results[0].formatted_address.replace(/^日本、/, '');
        updateResidentStatus(kind, '✓ 選択中: (' + point.lat.toFixed(4) + ', ' + point.lng.toFixed(4) + ')');
      } else {
        location.geocodeState = 'error';
        updateResidentStatus(kind, '住所を取得できませんでした。地図上で選び直してください');
      }
    });
  }

  function initResidentMap(kind) {
    const location = s18Locations[kind];
    if (location.map || typeof google === 'undefined' || !google.maps || !google.maps.Map) return;
    const point = readResidentCoords(kind);
    location.map = new google.maps.Map(document.getElementById(location.mapId), {
      center: point || s18MapCenter, zoom: 14, mapTypeControl: false, streetViewControl: false,
      fullscreenControl: false, zoomControl: false, gestureHandling: 'greedy', scrollwheel: true
    });
    location.map.addListener('click', (event) => selectResidentLocation(kind, {
      lat: event.latLng.lat(), lng: event.latLng.lng()
    }));
    if (point) {
      showResidentMarker(kind, point);
      updateResidentStatus(kind, '✓ 選択中: (' + point.lat.toFixed(4) + ', ' + point.lng.toFixed(4) + ')');
    }
  }

  function requestResidentLocation(kind) {
    if (!navigator.geolocation) {
      alert('現在地を取得できません。地図上で住所を選択してください');
      return;
    }
    const location = s18Locations[kind];
    // 解除・地図の選択・再取得より前の現在地応答を無視する。
    const revision = location.revision;
    const requestId = ++location.positionRequestId;
    const isCurrent = () => revision === location.revision && requestId === location.positionRequestId;
    navigator.geolocation.getCurrentPosition((position) => {
      if (!isCurrent()) return;
      selectResidentLocation(kind, { lat: position.coords.latitude, lng: position.coords.longitude });
    }, () => {
      if (!isCurrent()) return;
      alert('現在地を取得できませんでした。地図上で住所を選択してください');
    }, { timeout: 7000 });
  }

  function handleLivingCurrentLocation() { requestResidentLocation('living'); }
  function handleWorkCurrentLocation() { requestResidentLocation('work'); }

  function toggleWorkMap(forceOpen) {
    const area = document.getElementById('s18-work-map-area');
    const trigger = document.getElementById('s18-work-trigger-card');
    const shouldOpen = forceOpen === undefined ? area.style.display === 'none' : !!forceOpen;
    area.style.display = shouldOpen ? 'block' : 'none';
    trigger.style.display = shouldOpen ? 'none' : 'flex';
    if (shouldOpen) {
      initResidentMap('work');
      const map = s18Locations.work.map;
      if (map) {
        google.maps.event.trigger(map, 'resize');
        map.setCenter(readResidentCoords('work') || s18MapCenter);
      }
    } else {
      const address = document.getElementById('s18-work-address').value.trim();
      document.getElementById('s18-work-trigger-label').textContent = address ? '職場・学校: ' + address : '職場や学校を登録する';
      document.getElementById('s18-work-trigger-icon').textContent = address ? '変更' : '＋';
    }
  }

  document.addEventListener('DOMContentLoaded', () => {
    ['living', 'work'].forEach((kind) => {
      if (document.getElementById('s18-' + kind + '-address').value.trim() && readResidentCoords(kind)) {
        s18Locations[kind].geocodeState = 'resolved';
      }
    });

    const workAddress = document.getElementById('s18-work-address').value.trim();
    if (workAddress) toggleWorkMap(true);
    function mountMaps() {
      initResidentMap('living');
      if (document.getElementById('s18-work-map-area').style.display !== 'none') initResidentMap('work');
    }
    mountMaps();
    if (!s18Locations.living.map) {
      let retries = 0;
      const timer = setInterval(() => {
        mountMaps();
        if (s18Locations.living.map || ++retries > 60) clearInterval(timer);
      }, 100);
    }
  });
</script>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
