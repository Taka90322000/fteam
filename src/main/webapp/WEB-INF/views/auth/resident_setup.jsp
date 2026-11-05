<%-- 住民の初期設定画面。Actionから渡されたLINE表示名と任意のメールアドレスを初期表示に使う。 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map,jp.machiriku.tool.Html" %>
<%
  String displayName = (String) request.getAttribute("displayName");
  if (displayName == null) displayName = "";
  String email = (String) request.getAttribute("email");
  if (email == null) email = "";
  // 再表示時はPOSTされた値を優先し、初回だけLINEの実際のプロフィールを使用する。
  Map<String, String> formValues = request.getAttribute("formValues") instanceof Map
      ? (Map<String, String>) request.getAttribute("formValues") : java.util.Collections.emptyMap();
  displayName = formValues.getOrDefault("name", displayName);
  email = formValues.getOrDefault("email", email);
  boolean formSubmitted = "POST".equals(request.getMethod());
%>
<jsp:include page="/WEB-INF/views/common/header.jsp">
  <jsp:param name="title" value="アカウントの初期設定 | マチリク" />
  <jsp:param name="isOnboarding" value="true" />
  <jsp:param name="roleLabel" value="地域住民" />
  <jsp:param name="userName" value="<%= displayName %>" />
</jsp:include>

<section class="screen-view s18-view" data-screen="S18">
  <div class="screen-header-banner" style="background:var(--primary-50); border:1px solid var(--primary-100); border-radius:var(--radius-lg); padding:1rem 1.25rem; margin-bottom:1.5rem; display:flex; align-items:center; gap:1rem;">
    <span class="screen-header-badge" style="background:var(--primary-700); color:var(--text-inverse); font-weight:800; font-size:0.85rem; padding:0.25rem 0.6rem; border-radius:var(--radius-full);">S18</span>
    <div>
      <h1 class="screen-header-title" style="font-size:1.25rem; font-weight:800; color:var(--primary-900); margin:0;">アカウントの初期設定</h1>
      <p class="screen-header-desc" style="font-size:0.85rem; color:var(--text-sub); margin:0.25rem 0 0 0;">LINEアカウントから情報を取得しました。お住まいの地域など基本情報を入力してマチリクを始めましょう。</p>
    </div>
  </div>

  <div class="form-stepper-box" style="max-width: 680px; margin:0 auto; background:var(--bg-card); padding:2rem; border-radius:var(--radius-lg); border:1px solid var(--border-color); box-shadow:var(--shadow-sm);">
    <% if (request.getAttribute("errorMessage") != null) { %>
      <div role="alert" style="background:#fee2e2; color:#991b1b; padding:0.75rem 1rem; border-radius:var(--radius-md); margin-bottom:1rem;"><%= Html.escape(request.getAttribute("errorMessage")) %></div>
    <% } %>
    
    <!-- LINEログイン完了カード -->
    <div class="onboarding-welcome-card" style="background: var(--bg-subtle); border: 1.5px solid var(--border-color); border-radius: var(--radius-lg); padding: var(--space-3) var(--space-4); margin-bottom: var(--space-4);">
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:0.4rem;">
        <div style="display:flex; align-items:center; gap:0.6rem;">
          <span style="display:inline-flex; align-items:center; justify-content:center; width:28px; height:28px; background:var(--line-green); color:var(--text-inverse); border-radius:var(--radius-full); flex-shrink:0;">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
          </span>
          <strong style="font-size:1.02rem; color:var(--text-main);">LINEログインが完了しました</strong>
        </div>
        <span style="display:inline-flex; align-items:center; gap:0.35rem; padding:0.25rem 0.65rem; background:var(--line-green); color:var(--text-inverse); font-size:0.8125rem; font-weight:800; border-radius:var(--radius-full); letter-spacing:0.02em;">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          <span>アカウント区分: 地域住民</span>
        </span>
      </div>
    </div>

    <%-- 入力内容と居住地・職場などの座標を、初期設定のActionへ送信する。 --%>
    <form id="resident-setup-form" action="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/ResidentSetup.action" method="POST" onsubmit="return validateResidentForm();">
      <input type="hidden" name="csrfToken" value="<%= Html.escape(request.getAttribute("csrfToken")) %>">
      <!-- ニックネーム (表示名) -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-name" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          ニックネーム (表示名) <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
        </label>
        <input id="s18-name" name="name" type="text" class="form-control" value="<%= Html.escape(displayName) %>" maxlength="100" required placeholder="例: 山田 太郎" oninput="const h = document.getElementById('header-user-name'); if(h) h.textContent = this.value.trim();">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINEの表示名が初期設定されています。自由に編集できます。</span>
      </div>

      <!-- メールアドレス -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-email" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          メールアドレス <span style="font-size:0.78rem; color:var(--text-sub);">(変更可能)</span>
        </label>
        <input id="s18-email" name="email" type="email" class="form-control" value="<%= Html.escape(email) %>" maxlength="254" placeholder="example@domain.com">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINE認証で取得できた場合に反映しています。未取得の場合は空欄です。</span>
      </div>

      <!-- 年齢 (任意・ラジオボタン) -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
          年齢 <span style="color:var(--text-muted); font-size:0.8rem;">(任意)</span>
        </label>
        <div style="display:grid; grid-template-columns:repeat(7, minmax(0, 1fr)); gap:0.35rem; width:100%; box-sizing:border-box;">
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

      <!-- 業種 (任意) -->
      <div class="form-group" style="margin-bottom:1.5rem;">
        <label for="s18-industry" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          業種・ご職業 <span style="color:var(--text-muted); font-size:0.8rem;">(任意)</span>
        </label>
        <select id="s18-industry" name="industry" class="form-control">
          <option value="">選択してください（任意）</option>
          <%
            String[] industryOptions = {
              "飲食・カフェ", "小売・物販", "美容・サロン", "医療・介護・福祉",
              "IT・通信・オフィス", "建設・不動産", "教育・スクール",
              "サービス・エンタメ", "学生", "主婦・主夫", "その他"
            };
            for (String opt : industryOptions) {
          %>
            <option value="<%= Html.escape(opt) %>" <%= opt.equals(formValues.get("industry")) ? "selected" : "" %>><%= Html.escape(opt) %></option>
          <% } %>
        </select>
      </div>

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
          <input id="s18-living-address" name="livingAddress" type="text" class="form-control" value="<%= Html.escape(formValues.get("livingAddress")) %>" required placeholder="地図をクリックすると自動で住所が設定されます">
          <input id="s18-living-lat" name="livingLat" type="hidden" value="<%= Html.escape(formValues.get("livingLat")) %>">
          <input id="s18-living-lng" name="livingLng" type="hidden" value="<%= Html.escape(formValues.get("livingLng")) %>">
          <div id="s18-map-status" style="font-size:0.75rem; color:var(--primary-700); margin-top:0.35rem; font-weight:600;">
            ※地図をクリックして居住地を選択してください
          </div>
        </div>
      </div>

      <!-- 職場や学校などの住所 (任意) -->
      <div class="form-group" style="margin-bottom:1.75rem;">
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
            <input id="s18-work-address" name="workAddress" type="text" class="form-control" value="<%= Html.escape(formValues.get("workAddress")) %>" placeholder="地図をクリックすると自動で住所が設定されます">
            <input id="s18-work-lat" name="workLat" type="hidden" value="<%= Html.escape(formValues.get("workLat")) %>">
            <input id="s18-work-lng" name="workLng" type="hidden" value="<%= Html.escape(formValues.get("workLng")) %>">
            <div id="s18-work-map-status" style="font-size:0.75rem; color:var(--primary-700); margin-top:0.35rem; font-weight:600;">
              ※地図をクリックしてピンを配置してください
            </div>
          </div>
        </div>
      </div>

      <!-- 新着店舗・新着クーポン情報のメール受信設定 -->
      <label style="display:flex; align-items:center; gap:0.65rem; cursor:pointer; font-size:0.875rem; color:var(--text-main); font-weight:600; line-height:1.45; margin-top:1.5rem; padding:0.75rem 1rem; background:var(--bg-subtle); border-radius:var(--radius-md); border:1px solid var(--border-color); box-sizing:border-box; min-height:48px;">
        <input type="checkbox" id="s18-email-notify" name="emailNotify" <%= !formSubmitted || formValues.containsKey("emailNotify") ? "checked" : "" %> style="cursor:pointer; width:18px; height:18px; margin:0; flex-shrink:0;">
        <span>新着店舗や新着クーポン情報をメールで受け取る</span>
      </label>

      <!-- 送信ボタン -->
      <div style="margin-top:1.5rem;">
        <button type="submit" class="btn-primary" style="width:100%; min-height:48px; font-size:1.05rem; font-weight:800; display:flex; align-items:center; justify-content:center; gap:0.5rem; box-shadow:var(--shadow-floating); cursor:pointer;">
          <span>登録を完了してマチリクを始める</span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
      </div>
    </form>
  </div>
</section>

<!-- Google Maps JavaScript API -->
<script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyCNWrNbVnBfZsjuTJ3sHCKDemfkxNsIv7w&libraries=marker"></script>

<script>
  // 初期中心は宇都宮にするが、利用者が選ぶまでは住所・座標・ピンを登録しない。
  const s18Locations = {
    living: { map: null, marker: null, revision: 0, mapId: 's18-map-picker', statusId: 's18-map-status' },
    work: { map: null, marker: null, revision: 0, mapId: 's18-work-map-picker', statusId: 's18-work-map-status' }
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
    if (!addrInput.value.trim() || !readResidentCoords('living')) {
      alert('居住地を地図上で選択してください（必須）');
      addrInput.focus();
      return false;
    }
    const workInput = document.getElementById('s18-work-address');
    if (workInput.value.trim() && !readResidentCoords('work')) {
      alert('職場・学校を登録する場合は、地図上で住所を選択してください');
      toggleWorkMap(true);
      workInput.focus();
      return false;
    }
    // 任意の職場・学校が空欄なら、以前の座標も送らない。
    if (!workInput.value.trim()) clearResidentCoords('work');
    return true;
  }

  function updateResidentStatus(kind, text) {
    document.getElementById(s18Locations[kind].statusId).textContent = text;
  }

  // 住所の手修正後に、修正前の座標が保存されることを防ぐ。
  function clearResidentCoords(kind) {
    const location = s18Locations[kind];
    location.revision++;
    document.getElementById('s18-' + kind + '-lat').value = '';
    document.getElementById('s18-' + kind + '-lng').value = '';
    if (location.marker) location.marker.setMap(null);
    updateResidentStatus(kind, '※地図上で住所を選択してください');
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
    const addressInput = document.getElementById('s18-' + kind + '-address');
    document.getElementById('s18-' + kind + '-lat').value = point.lat;
    document.getElementById('s18-' + kind + '-lng').value = point.lng;
    addressInput.value = '';
    showResidentMarker(kind, point);
    if (location.map) location.map.panTo(point);
    updateResidentStatus(kind, '選択した位置の住所を確認しています…');
    if (typeof google === 'undefined' || !google.maps || !google.maps.Geocoder) {
      updateResidentStatus(kind, '住所を取得できませんでした。地図の読み込み後に選び直してください');
      return;
    }
    new google.maps.Geocoder().geocode({ location: point }, (results, status) => {
      // 地図選択後に入力を修正した場合、古い住所検索結果で上書きしない。
      if (revision !== location.revision) return;
      if (status === 'OK' && results && results[0] && results[0].formatted_address) {
        addressInput.value = results[0].formatted_address.replace(/^日本、/, '');
        updateResidentStatus(kind, '✓ 選択中: (' + point.lat.toFixed(4) + ', ' + point.lng.toFixed(4) + ')');
      } else {
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
    // エラー再表示時のみ、送信済みの位置と住所を変更せず復元する。
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
    navigator.geolocation.getCurrentPosition((position) => {
      selectResidentLocation(kind, { lat: position.coords.latitude, lng: position.coords.longitude });
    }, () => {
      // 取得失敗時に宇都宮駅などの仮の地点を登録しない。
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
      document.getElementById('s18-' + kind + '-address').addEventListener('input', () => clearResidentCoords(kind));
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
