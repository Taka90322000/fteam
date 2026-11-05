<%-- 住民の初期設定画面。Actionから渡されたLINE表示名と任意のメールアドレスを初期表示に使う。 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
  String displayName = (String) request.getAttribute("displayName");
  if (displayName == null) displayName = "LINEユーザー";
  String email = (String) request.getAttribute("email");
  if (email == null) email = "";
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
      <!-- ニックネーム (表示名) -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-name" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          ニックネーム (表示名) <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
        </label>
        <input id="s18-name" name="name" type="text" class="form-control" value="<%= displayName %>" required placeholder="例: 山田 太郎" oninput="const h = document.getElementById('header-user-name'); if(h && this.value.trim()) h.textContent = this.value.trim();">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINEの表示名が初期設定されています。自由に編集できます。</span>
      </div>

      <!-- メールアドレス -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-email" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          メールアドレス <span style="font-size:0.78rem; color:var(--text-sub);">(変更可能)</span>
        </label>
        <input id="s18-email" name="email" type="email" class="form-control" value="<%= email %>" placeholder="example@domain.com">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINE認証で取得したメールアドレスが反映されています。</span>
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
              <input type="radio" name="ageGroup" value="<%= opt %>" <%= "未設定".equals(opt) ? "checked" : "" %> style="cursor:pointer; margin:0; flex-shrink:0;">
              <span><%= opt %></span>
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
              <input type="radio" name="gender" value="<%= opt %>" <%= "回答しない".equals(opt) ? "checked" : "" %> style="cursor:pointer;">
              <span><%= opt %></span>
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
            <option value="<%= opt %>"><%= opt %></option>
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
          <input id="s18-living-address" name="livingAddress" type="text" class="form-control" value="栃木県宇都宮市本町1-1" required placeholder="地図をクリックすると自動で住所が設定されます">
          <input id="s18-living-lat" name="livingLat" type="hidden" value="36.5610">
          <input id="s18-living-lng" name="livingLng" type="hidden" value="139.8830">
          <div id="s18-map-status" style="font-size:0.75rem; color:var(--primary-700); margin-top:0.35rem; font-weight:600;">
            ✓ 選択中: (36.5610, 139.8830)
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
            <input id="s18-work-address" name="workAddress" type="text" class="form-control" value="" placeholder="地図をクリックすると自動で住所が設定されます">
            <input id="s18-work-lat" name="workLat" type="hidden" value="36.5590">
            <input id="s18-work-lng" name="workLng" type="hidden" value="139.8980">
            <div id="s18-work-map-status" style="font-size:0.75rem; color:var(--primary-700); margin-top:0.35rem; font-weight:600;">
              ※地図をクリックしてピンを配置してください
            </div>
          </div>
        </div>
      </div>

      <!-- 新着店舗・新着クーポン情報のメール受信設定 -->
      <label style="display:flex; align-items:center; gap:0.65rem; cursor:pointer; font-size:0.875rem; color:var(--text-main); font-weight:600; line-height:1.45; margin-top:1.5rem; padding:0.75rem 1rem; background:var(--bg-subtle); border-radius:var(--radius-md); border:1px solid var(--border-color); box-sizing:border-box; min-height:48px;">
        <input type="checkbox" id="s18-email-notify" name="emailNotify" checked style="cursor:pointer; width:18px; height:18px; margin:0; flex-shrink:0;">
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
  // 居住地と職場・学校は別々の地図とピンで管理し、フォームの座標欄と同期する。
  let s18LivingMap = null;
  let s18LivingMarker = null;
  let s18WorkMap = null;
  let s18WorkMarker = null;

  // 送信前にニックネームと居住地の入力を確認し、不足していれば該当欄へ戻す。
  function validateResidentForm() {
    const nameInput = document.getElementById('s18-name');
    const addrInput = document.getElementById('s18-living-address');
    if (!nameInput || !nameInput.value.trim()) {
      alert('ニックネームを入力してください');
      if (nameInput) nameInput.focus();
      return false;
    }
    if (!addrInput || !addrInput.value.trim()) {
      alert('居住地を地図上でクリックして指定してください（必須）');
      if (addrInput) addrInput.focus();
      return false;
    }
    return true;
  }

  // 緯度・経度から住所名を非同期で取得し、取得できない場合は既定の地域名を返す。
  function reverseGeocodeWithGoogle(lat, lng, callback) {
    if (typeof google !== 'undefined' && google.maps && google.maps.Geocoder) {
      const geocoder = new google.maps.Geocoder();
      geocoder.geocode({ location: { lat, lng } }, (results, status) => {
        if (status === 'OK' && results && results[0]) {
          let resolved = results[0].formatted_address || '栃木県宇都宮市';
          // 日本表記の整形
          resolved = resolved.replace(/^日本、/, '');
          callback(resolved);
        } else {
          callback('栃木県宇都宮市');
        }
      });
    } else {
      callback('栃木県宇都宮市');
    }
  }

  // 居住地の現在値を地図へ反映し、地図クリックとピン移動のイベントを登録する。
  function initLivingMapPicker() {
    const container = document.getElementById('s18-map-picker');
    if (!container) return;

    const latInput = document.getElementById('s18-living-lat');
    const lngInput = document.getElementById('s18-living-lng');
    const addrInput = document.getElementById('s18-living-address');
    const statusEl = document.getElementById('s18-map-status');

    let currentLat = latInput ? parseFloat(latInput.value) : 36.5610;
    let currentLng = lngInput ? parseFloat(lngInput.value) : 139.8830;
    if (isNaN(currentLat) || isNaN(currentLng)) {
      currentLat = 36.5610;
      currentLng = 139.8830;
    }

    // 居住地の選択結果を送信用座標・住所欄・選択状況の表示へ反映する。
    function onLocationSelected(lat, lng) {
      if (latInput) latInput.value = lat;
      if (lngInput) lngInput.value = lng;
      if (statusEl) {
        statusEl.textContent = '✓ 選択中: (' + lat.toFixed(4) + ', ' + lng.toFixed(4) + ')';
      }
      reverseGeocodeWithGoogle(lat, lng, (resolved) => {
        if (addrInput) {
          addrInput.value = resolved;
          addrInput.classList.add('is-flash-updated');
          setTimeout(() => addrInput.classList.remove('is-flash-updated'), 1000);
        }
        if (statusEl) {
          statusEl.textContent = '✓ 選択中: (' + lat.toFixed(4) + ', ' + lng.toFixed(4) + ')';
        }
      });
    }

    if (typeof google !== 'undefined' && google.maps && google.maps.Map) {
      const initialPos = { lat: currentLat, lng: currentLng };
      s18LivingMap = new google.maps.Map(container, {
        center: initialPos,
        zoom: 14,
        mapTypeControl: false,
        streetViewControl: false,
        fullscreenControl: false,
        zoomControl: false,
        gestureHandling: 'greedy',
        scrollwheel: true
      });

      s18LivingMarker = new google.maps.Marker({
        position: initialPos,
        map: s18LivingMap,
        draggable: true,
        title: "居住地ピン"
      });

      google.maps.event.addListener(s18LivingMap, 'click', (e) => {
        const lat = e.latLng.lat();
        const lng = e.latLng.lng();
        s18LivingMarker.setPosition(e.latLng);
        onLocationSelected(lat, lng);
      });

      google.maps.event.addListener(s18LivingMarker, 'dragend', (e) => {
        const lat = e.latLng.lat();
        const lng = e.latLng.lng();
        onLocationSelected(lat, lng);
      });
    }
  }

  // 端末の現在地を取得し、利用できない場合は既定の宇都宮の地点を設定する。
  function handleLivingCurrentLocation() {
    if (typeof navigator !== 'undefined' && navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setLivingCoords(pos.coords.latitude, pos.coords.longitude);
        },
        () => {
          setLivingCoords(36.5590, 139.8980, '栃木県宇都宮市駅前通り3丁目');
          alert('位置情報取得不可のため、宇都宮駅前を設定しました');
        },
        { timeout: 7000 }
      );
    } else {
      setLivingCoords(36.5610, 139.8830, '栃木県宇都宮市本町1-1');
    }
  }

  // 居住地の座標・ピン・表示を一緒に更新し、住所の指定がなければ住所名を調べる。
  function setLivingCoords(lat, lng, manualAddr = null) {
    const latInput = document.getElementById('s18-living-lat');
    const lngInput = document.getElementById('s18-living-lng');
    const addrInput = document.getElementById('s18-living-address');
    const statusEl = document.getElementById('s18-map-status');

    if (latInput) latInput.value = lat;
    if (lngInput) lngInput.value = lng;

    if (s18LivingMap && typeof google !== 'undefined' && google.maps) {
      const newPos = new google.maps.LatLng(lat, lng);
      s18LivingMap.panTo(newPos);
      if (s18LivingMarker) s18LivingMarker.setPosition(newPos);
    }

    if (manualAddr && addrInput) {
      addrInput.value = manualAddr;
      if (statusEl) statusEl.textContent = '✓ 選択中: (' + Number(lat).toFixed(4) + ', ' + Number(lng).toFixed(4) + ')';
      return;
    }

    if (statusEl) statusEl.textContent = '✓ 選択中: (' + Number(lat).toFixed(4) + ', ' + Number(lng).toFixed(4) + ')';
    reverseGeocodeWithGoogle(lat, lng, (resolved) => {
      if (addrInput) addrInput.value = resolved;
      if (statusEl) statusEl.textContent = '✓ 選択中: (' + Number(lat).toFixed(4) + ', ' + Number(lng).toFixed(4) + ')';
    });
  }

  // 任意項目の職場・学校の地図を開閉し、表示後の地図サイズと選択位置を調整する。
  function toggleWorkMap(forceOpen) {
    const triggerCard = document.getElementById('s18-work-trigger-card');
    const area = document.getElementById('s18-work-map-area');
    if (!area) return;

    const isHidden = (area.style.display === 'none' || area.style.display === '');
    const shouldOpen = (forceOpen !== undefined) ? !!forceOpen : isHidden;

    if (shouldOpen) {
      if (triggerCard) triggerCard.style.display = 'none';
      area.style.display = 'block';
      initWorkMapPicker();
      if (s18WorkMap && typeof google !== 'undefined' && google.maps && google.maps.event) {
        setTimeout(() => {
          google.maps.event.trigger(s18WorkMap, 'resize');
          const latInput = document.getElementById('s18-work-lat');
          const lngInput = document.getElementById('s18-work-lng');
          const lat = latInput ? parseFloat(latInput.value) : 36.5590;
          const lng = lngInput ? parseFloat(lngInput.value) : 139.8980;
          s18WorkMap.setCenter({ lat: isNaN(lat) ? 36.5590 : lat, lng: isNaN(lng) ? 139.8980 : lng });
        }, 100);
      }
    } else {
      area.style.display = 'none';
      if (triggerCard) {
        triggerCard.style.display = 'flex';
        const workAddrInput = document.getElementById('s18-work-address');
        const triggerLabel = document.getElementById('s18-work-trigger-label');
        const triggerIcon = document.getElementById('s18-work-trigger-icon');
        const val = workAddrInput ? workAddrInput.value.trim() : '';
        if (triggerLabel) {
          triggerLabel.textContent = val ? '職場・学校: ' + val : '職場や学校を登録する';
        }
        if (triggerIcon) {
          triggerIcon.textContent = val ? '変更' : '＋';
        }
      }
    }
  }

  // 職場・学校の地図を初回に作成し、再表示時は既存の地図とピンを再利用する。
  function initWorkMapPicker() {
    const container = document.getElementById('s18-work-map-picker');
    if (!container) return;

    const latInput = document.getElementById('s18-work-lat');
    const lngInput = document.getElementById('s18-work-lng');
    const addrInput = document.getElementById('s18-work-address');
    const statusEl = document.getElementById('s18-work-map-status');

    let currentLat = latInput ? parseFloat(latInput.value) : 36.5590;
    let currentLng = lngInput ? parseFloat(lngInput.value) : 139.8980;
    if (isNaN(currentLat) || isNaN(currentLng)) {
      currentLat = 36.5590;
      currentLng = 139.8980;
    }

    // 職場・学校の選択結果を送信用座標と住所欄へ反映する。
    function onWorkLocationSelected(lat, lng) {
      if (latInput) latInput.value = lat;
      if (lngInput) lngInput.value = lng;
      if (statusEl) {
        statusEl.textContent = '✓ 選択中: (' + lat.toFixed(4) + ', ' + lng.toFixed(4) + ')';
      }
      reverseGeocodeWithGoogle(lat, lng, (resolved) => {
        if (addrInput) {
          addrInput.value = resolved;
          addrInput.classList.add('is-flash-updated');
          setTimeout(() => addrInput.classList.remove('is-flash-updated'), 1000);
        }
        if (statusEl) {
          statusEl.textContent = '✓ 選択中: (' + lat.toFixed(4) + ', ' + lng.toFixed(4) + ')';
        }
      });
    }

    if (typeof google !== 'undefined' && google.maps && google.maps.Map) {
      const initialPos = { lat: currentLat, lng: currentLng };
      if (s18WorkMap && s18WorkMarker) {
        google.maps.event.trigger(s18WorkMap, 'resize');
        s18WorkMap.setCenter(initialPos);
        s18WorkMarker.setPosition(initialPos);
        return;
      }
      s18WorkMap = new google.maps.Map(container, {
        center: initialPos,
        zoom: 14,
        mapTypeControl: false,
        streetViewControl: false,
        fullscreenControl: false,
        zoomControl: false,
        gestureHandling: 'greedy',
        scrollwheel: true
      });

      s18WorkMarker = new google.maps.Marker({
        position: initialPos,
        map: s18WorkMap,
        draggable: true,
        title: "職場・学校ピン"
      });

      google.maps.event.addListener(s18WorkMap, 'click', (e) => {
        const lat = e.latLng.lat();
        const lng = e.latLng.lng();
        s18WorkMarker.setPosition(e.latLng);
        onWorkLocationSelected(lat, lng);
      });

      google.maps.event.addListener(s18WorkMarker, 'dragend', (e) => {
        const lat = e.latLng.lat();
        const lng = e.latLng.lng();
        onWorkLocationSelected(lat, lng);
      });
    }
  }

  // 端末の現在地を職場・学校の候補へ設定し、取得失敗時は既定地点を利用する。
  function handleWorkCurrentLocation() {
    if (typeof navigator !== 'undefined' && navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setWorkCoords(pos.coords.latitude, pos.coords.longitude);
        },
        () => {
          setWorkCoords(36.5590, 139.8980, '栃木県宇都宮市駅前通り3丁目');
          alert('位置情報取得不可のため、宇都宮駅前を設定しました');
        },
        { timeout: 7000 }
      );
    } else {
      setWorkCoords(36.5590, 139.8980, '栃木県宇都宮市駅前通り3丁目');
    }
  }

  // 職場・学校の座標入力・地図・住所表示を同じ位置へ揃える。
  function setWorkCoords(lat, lng, manualAddr = null) {
    const latInput = document.getElementById('s18-work-lat');
    const lngInput = document.getElementById('s18-work-lng');
    const addrInput = document.getElementById('s18-work-address');
    const statusEl = document.getElementById('s18-work-map-status');

    if (latInput) latInput.value = lat;
    if (lngInput) lngInput.value = lng;

    if (s18WorkMap && typeof google !== 'undefined' && google.maps) {
      const newPos = new google.maps.LatLng(lat, lng);
      s18WorkMap.panTo(newPos);
      if (s18WorkMarker) s18WorkMarker.setPosition(newPos);
    }

    if (manualAddr && addrInput) {
      addrInput.value = manualAddr;
      if (statusEl) statusEl.textContent = '✓ 選択中: (' + Number(lat).toFixed(4) + ', ' + Number(lng).toFixed(4) + ')';
      return;
    }

    if (statusEl) statusEl.textContent = '✓ 選択中: (' + Number(lat).toFixed(4) + ', ' + Number(lng).toFixed(4) + ')';
    reverseGeocodeWithGoogle(lat, lng, (resolved) => {
      if (addrInput) addrInput.value = resolved;
      if (statusEl) statusEl.textContent = '✓ 選択中: (' + Number(lat).toFixed(4) + ', ' + Number(lng).toFixed(4) + ')';
    });
  }

  // フォームのDOMが揃ってから居住地マップの初期化を開始する。
  document.addEventListener('DOMContentLoaded', function() {
    initLivingMapPicker();
  });
</script>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
