<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
  String displayName = (String) request.getAttribute("displayName");
  if (displayName == null) displayName = "佐藤 健一";
  String email = (String) request.getAttribute("email");
  if (email == null) email = "";
%>
<jsp:include page="/WEB-INF/views/common/header.jsp">
  <jsp:param name="title" value="出店事業主 初期設定 | マチリク" />
  <jsp:param name="isOnboarding" value="true" />
  <jsp:param name="roleLabel" value="出店事業主" />
  <jsp:param name="userName" value="<%= displayName %>" />
</jsp:include>

<section class="screen-view s18-view s18-business-view" data-screen="S18">
  <div class="screen-header-banner" style="background:var(--primary-50); border:1px solid var(--primary-100); border-radius:var(--radius-lg); padding:1rem 1.25rem; margin-bottom:1.5rem; display:flex; align-items:center; gap:1rem;">
    <span class="screen-header-badge" style="background:var(--primary-700); color:var(--text-inverse); font-weight:800; font-size:0.85rem; padding:0.25rem 0.6rem; border-radius:var(--radius-full);">S18</span>
    <div>
      <h1 class="screen-header-title" style="font-size:1.25rem; font-weight:800; color:var(--primary-900); margin:0;">アカウントの初期設定</h1>
      <p class="screen-header-desc" style="font-size:0.85rem; color:var(--text-sub); margin:0.25rem 0 0 0;">LINEアカウントから情報を取得しました。事業者名や出店希望条件を入力してマチリクを始めましょう。</p>
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
          <span>アカウント区分: 出店事業主</span>
        </span>
      </div>
    </div>

    <form id="business-setup-form" action="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/BusinessSetup.action" method="POST" onsubmit="return validateBusinessForm();">
      <!-- 事業者名 / 屋号 -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-biz-name" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          事業者名 / 屋号 <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
        </label>
        <input id="s18-biz-name" name="businessName" type="text" class="form-control" value="" required placeholder="例: 宇都宮カフェ Lab">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※店舗名、屋号、法人名などをご入力ください。</span>
      </div>

      <!-- 代表者名 -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-rep-name" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          代表者名 <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
        </label>
        <input id="s18-rep-name" name="representativeName" type="text" class="form-control" value="<%= displayName %>" required placeholder="例: 佐藤 健一" oninput="const h = document.getElementById('header-user-name'); if(h && this.value.trim()) h.textContent = this.value.trim();">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINEの表示名が初期設定されています。自由に編集できます。</span>
      </div>

      <!-- メールアドレス -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-biz-email" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          メールアドレス <span style="font-size:0.78rem; color:var(--text-sub);">(変更可能)</span>
        </label>
        <input id="s18-biz-email" name="email" type="email" class="form-control" value="<%= email %>" placeholder="example@domain.com">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINE認証で取得したメールアドレスが反映されています。</span>
      </div>

      <!-- 連絡先電話番号 -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-biz-phone" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          連絡先電話番号 <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
        </label>
        <input id="s18-biz-phone" name="phone" type="tel" class="form-control" value="" required placeholder="例: 090-1234-5678">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※出店応募や物件オーナーとの連絡・確認用に使用されます。</span>
      </div>

      <!-- 出店希望業種・ジャンル (複数選択可) -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
          出店希望業種・ジャンル <span style="color:var(--danger-text); font-size:0.8rem;">*必須 (複数選択可)</span>
        </label>
        <div style="display:grid; grid-template-columns:repeat(auto-fill, minmax(130px, 1fr)); gap:0.4rem;">
          <%
            String[] bizCats = {
              "カフェ・喫茶", "居酒屋・バー", "ラーメン・麺類", "レストラン・軽食",
              "テイクアウト・総菜", "小売・物販", "雑貨・書店", "美容室・理容",
              "ネイル・サロン", "フィットネス", "コワーキング", "その他"
            };
            for (int i = 0; i < bizCats.length; i++) {
              String cat = bizCats[i];
          %>
            <label style="display:flex; align-items:center; gap:0.4rem; padding:0.5rem 0.65rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.84rem;">
              <input type="checkbox" name="preferredIndustry" value="<%= cat %>" <%= i == 0 ? "checked" : "" %> style="cursor:pointer; margin:0;">
              <span><%= cat %></span>
            </label>
          <% } %>
        </div>
      </div>

      <!-- 出店希望地域 (エリア - 地図なぞり範囲指定 / 選択) -->
      <div class="form-group" style="margin-bottom:1.5rem; background:var(--bg-subtle); padding:var(--space-4); border-radius:var(--radius-lg); border:1px solid var(--border-color);">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.65rem; flex-wrap:wrap; gap:0.5rem;">
          <label for="s18-biz-area" style="font-weight:800; font-size:0.95rem; color:var(--text-main);">
            出店希望エリア <span style="color:var(--danger-text); font-size:0.8rem;">*必須 (マップ上でなぞって指定可)</span>
          </label>
          <div style="display:flex; align-items:center; gap:0.35rem;">
            <button type="button" id="s18-draw-btn-draw" class="btn-outline btn-sm is-active" onclick="setBizMapMode('draw')" style="font-size:0.8rem; padding:0.3rem 0.65rem; border-radius:var(--radius-full); display:inline-flex; align-items:center; gap:0.3rem; background:var(--primary-700); color:var(--bg-card); font-weight:700; cursor:pointer;">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 19l7-7 3 3-7 7-3-3z"/><path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/><path d="M2 2l7.586 7.586"/></svg>
              <span>なぞる</span>
            </button>
            <button type="button" id="s18-draw-btn-pan" class="btn-outline btn-sm" onclick="setBizMapMode('pan')" style="font-size:0.8rem; padding:0.3rem 0.65rem; border-radius:var(--radius-full); display:inline-flex; align-items:center; gap:0.3rem; background:var(--bg-card); color:var(--text-main); cursor:pointer;">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 14 14"/></svg>
              <span>地図移動</span>
            </button>
            <button type="button" class="btn-outline btn-sm" onclick="clearBizDraw()" style="font-size:0.8rem; padding:0.3rem 0.65rem; border-radius:var(--radius-full); display:inline-flex; align-items:center; gap:0.25rem; background:var(--bg-card); cursor:pointer;">
              <span>やり直す</span>
            </button>
          </div>
        </div>

        <!-- 地図コンテナ -->
        <div id="s18-biz-map-wrap" style="position:relative; width:100%; height:270px; border-radius:var(--radius-md); border:1.5px solid var(--border-color); background-color:var(--bg-subtle); overflow:hidden; touch-action:none;">
          <div id="s18-biz-map-picker" style="width:100%; height:100%;"></div>
          <div id="s18-biz-draw-surface" style="position:absolute; top:0; left:0; width:100%; height:100%; z-index:5; cursor:crosshair; touch-action:none;"></div>
          <canvas id="s18-biz-draw-canvas" style="position:absolute; top:0; left:0; width:100%; height:100%; pointer-events:none; z-index:6;"></canvas>
          <div id="s18-biz-draw-hint" style="position:absolute; top:8px; left:8px; background:rgba(0,0,0,0.65); color:var(--text-inverse); font-size:0.75rem; font-weight:700; padding:0.25rem 0.6rem; border-radius:var(--radius-md); pointer-events:none; z-index:10;">
            なぞるモード: 指やマウスで囲んで指定
          </div>
        </div>

        <div style="margin-top:0.75rem;">
          <label for="s18-biz-area" style="font-size:0.8rem; font-weight:700; color:var(--text-sub); display:block; margin-bottom:0.25rem;">
            出店希望エリア名 (地図をなぞると自動入力・手動修正可):
          </label>
          <input id="s18-biz-area" name="preferredArea" type="text" class="form-control" value="" required placeholder="地図上を指やマウスで囲むと自動入力されます（手入力も可能）" style="width:100%;">
          <input id="s18-biz-area-points" name="preferredAreaPoints" type="hidden" value="">
          <div id="s18-biz-map-status" style="font-size:0.75rem; color:var(--primary-700); margin-top:0.35rem; font-weight:600;">
            ※地図上をなぞるか、直接エリア名を入力してください
          </div>
        </div>
      </div>

      <!-- 従業員数 -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-biz-employees" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          従業員数 (予定含む) <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
        </label>
        <input id="s18-biz-employees" name="employeeCount" type="number" min="0" class="form-control" value="1" required placeholder="例: 1">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※ご自身のみ・初出店の場合は「1」または「0」と入力してください。</span>
      </div>

      <!-- 店舗経営の経験 -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
          店舗経営の経験 <span style="color:var(--text-muted); font-size:0.8rem;">(任意)</span>
        </label>
        <div style="display:flex; flex-wrap:wrap; gap:0.5rem;">
          <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.85rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.875rem;">
            <input type="radio" name="mgmtExperience" value="あり" style="cursor:pointer;">
            <span>あり (現在経営中 / 過去に経営経験あり)</span>
          </label>
          <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.85rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.875rem;">
            <input type="radio" name="mgmtExperience" value="なし" checked style="cursor:pointer;">
            <span>なし (新規開業・初出店)</span>
          </label>
        </div>
      </div>

      <!-- 業界での勤務経験 -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
          業界での勤務経験 <span style="color:var(--text-muted); font-size:0.8rem;">(任意)</span>
        </label>
        <div style="display:flex; flex-wrap:wrap; gap:0.5rem;">
          <%
            String[] exps = {"未経験", "1〜3年未満", "3〜5年未満", "5年以上"};
            for (int i = 0; i < exps.length; i++) {
          %>
            <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.85rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.875rem;">
              <input type="radio" name="industryExperience" value="<%= exps[i] %>" <%= i == 1 ? "checked" : "" %> style="cursor:pointer;">
              <span><%= exps[i] %></span>
            </label>
          <% } %>
        </div>
      </div>

      <!-- 保有資格・許認可 -->
      <div class="form-group" style="margin-bottom:1.5rem;">
        <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
          保有資格・許認可 <span style="color:var(--text-muted); font-size:0.8rem;">(任意・複数選択可)</span>
        </label>
        <div style="display:flex; flex-wrap:wrap; gap:0.5rem;">
          <%
            String[] qList = {"食品衛生責任者", "防火管理者", "調理師免許", "美容師・理容師免許", "宅地建物取引士", "その他資格・許認可"};
            for (String q : qList) {
          %>
            <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.75rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.84rem;">
              <input type="checkbox" name="qualification" value="<%= q %>" style="cursor:pointer; margin:0;">
              <span><%= q %></span>
            </label>
          <% } %>
        </div>
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.35rem;">※出店応募時に必要な資格情報を事前に登録しておくことで、スムーズに応募手続きを行えます。</span>
      </div>

      <!-- 新着店舗・オファー情報のメール受信設定 -->
      <label style="display:flex; align-items:center; gap:0.65rem; cursor:pointer; font-size:0.875rem; color:var(--text-main); font-weight:600; line-height:1.45; margin-top:1.5rem; padding:0.75rem 1rem; background:var(--bg-subtle); border-radius:var(--radius-md); border:1px solid var(--border-color); box-sizing:border-box; min-height:48px;">
        <input type="checkbox" id="s18-biz-email-notify" name="emailNotify" checked style="cursor:pointer; width:18px; height:18px; margin:0; flex-shrink:0;">
        <span>希望条件に合う新着空き店舗や物件オーナーからのオファー通知をメールで受け取る</span>
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
  let s18BizMap = null;
  let s18BizOverlay = null;
  let s18BizPolygon = null;
  let s18BizPoints = [];
  let s18BizMode = 'draw';
  let s18BizIsDrawing = false;

  function validateBusinessForm() {
    const bizName = document.getElementById('s18-biz-name');
    const repName = document.getElementById('s18-rep-name');
    const phone = document.getElementById('s18-biz-phone');
    const areaInput = document.getElementById('s18-biz-area');

    if (!bizName || !bizName.value.trim()) {
      alert('事業者名・屋号を入力してください');
      if (bizName) bizName.focus();
      return false;
    }
    if (!repName || !repName.value.trim()) {
      alert('代表者名を入力してください');
      if (repName) repName.focus();
      return false;
    }
    if (!phone || !phone.value.trim()) {
      alert('連絡先電話番号を入力してください');
      if (phone) phone.focus();
      return false;
    }
    if (!areaInput || !areaInput.value.trim()) {
      alert('出店希望エリアをマップ上でなぞって指定するか、エリア名を入力してください');
      if (areaInput) areaInput.focus();
      return false;
    }

    const industries = document.querySelectorAll('input[name="preferredIndustry"]:checked');
    if (industries.length === 0) {
      alert('出店希望業種・ジャンルを少なくとも1つ選択してください');
      return false;
    }

    return true;
  }

  function reverseGeocodeWithGoogle(lat, lng, callback) {
    if (typeof google !== 'undefined' && google.maps && google.maps.Geocoder) {
      const geocoder = new google.maps.Geocoder();
      geocoder.geocode({ location: { lat, lng } }, (results, status) => {
        if (status === 'OK' && results && results[0]) {
          let resolved = results[0].formatted_address || '栃木県宇都宮市';
          resolved = resolved.replace(/^日本、/, '');
          callback(resolved);
        } else {
          callback('栃木県宇都宮市周辺');
        }
      });
    } else {
      callback('栃木県宇都宮市周辺');
    }
  }

  function initS18BizAreaMap() {
    const container = document.getElementById('s18-biz-map-picker');
    const wrap = document.getElementById('s18-biz-map-wrap');
    if (!container || !wrap) return;

    function mountMap() {
      s18BizPoints = [];
      s18BizIsDrawing = false;
      s18BizMode = 'draw';

      const centerPos = { lat: 36.5610, lng: 139.8830 };
      s18BizMap = new google.maps.Map(container, {
        center: centerPos,
        zoom: 14,
        mapTypeControl: false,
        streetViewControl: false,
        fullscreenControl: false,
        zoomControl: false,
        gestureHandling: 'none',
        draggable: false,
        scrollwheel: false,
        disableDoubleClickZoom: true
      });

      s18BizOverlay = new google.maps.OverlayView();
      s18BizOverlay.draw = function() {};
      s18BizOverlay.setMap(s18BizMap);

      setupBizDrawingListeners(wrap);

      setTimeout(() => {
        if (s18BizMap && typeof google !== 'undefined' && google.maps && google.maps.event) {
          google.maps.event.trigger(s18BizMap, 'resize');
        }
      }, 200);
    }

    if (typeof google !== 'undefined' && google.maps && google.maps.Map) {
      mountMap();
      return;
    }

    let retries = 0;
    const timer = setInterval(() => {
      retries++;
      if (typeof google !== 'undefined' && google.maps && google.maps.Map) {
        clearInterval(timer);
        mountMap();
      }
      if (retries > 60) clearInterval(timer);
    }, 100);
  }

  function setupBizDrawingListeners(wrap) {
    let surface = document.getElementById('s18-biz-draw-surface');
    let canvas = document.getElementById('s18-biz-draw-canvas');
    if (!surface || !canvas) return;

    const rect = wrap.getBoundingClientRect();
    canvas.width = rect.width || wrap.clientWidth || 320;
    canvas.height = rect.height || wrap.clientHeight || 270;
    const ctx = canvas.getContext ? canvas.getContext('2d') : null;

    let pixelPoints = [];

    surface.onpointerdown = (e) => {
      if (s18BizMode !== 'draw') return;
      if (surface.setPointerCapture) {
        try { surface.setPointerCapture(e.pointerId); } catch (_) {}
      }
      s18BizIsDrawing = true;
      pixelPoints = [];
      const bRect = wrap.getBoundingClientRect();
      const x = e.clientX - bRect.left;
      const y = e.clientY - bRect.top;
      pixelPoints.push({ x, y });

      if (ctx) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.beginPath();
        ctx.moveTo(x, y);
        ctx.strokeStyle = 'rgb(45, 106, 79)';
        ctx.lineWidth = 3;
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';
      }
    };

    surface.onpointermove = (e) => {
      if (!s18BizIsDrawing || s18BizMode !== 'draw') return;
      const bRect = wrap.getBoundingClientRect();
      const x = e.clientX - bRect.left;
      const y = e.clientY - bRect.top;
      pixelPoints.push({ x, y });

      if (ctx) {
        ctx.lineTo(x, y);
        ctx.stroke();
      }
    };

    const finishDraw = (e) => {
      if (!s18BizIsDrawing || s18BizMode !== 'draw') return;
      s18BizIsDrawing = false;
      if (surface.releasePointerCapture) {
        try { surface.releasePointerCapture(e.pointerId); } catch (_) {}
      }

      if (ctx) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
      }

      if (pixelPoints.length < 3) return;

      if (!s18BizOverlay || !s18BizOverlay.getProjection) return;
      const proj = s18BizOverlay.getProjection();
      if (!proj) return;

      const sampled = [];
      const step = Math.max(1, Math.floor(pixelPoints.length / 60));
      for (let i = 0; i < pixelPoints.length; i += step) {
        sampled.push(pixelPoints[i]);
      }
      if (sampled[sampled.length - 1] !== pixelPoints[pixelPoints.length - 1]) {
        sampled.push(pixelPoints[pixelPoints.length - 1]);
      }

      const latLngs = [];
      let sumLat = 0;
      let sumLng = 0;
      for (let i = 0; i < sampled.length; i++) {
        const pt = proj.fromContainerPixelToLatLng(new google.maps.Point(sampled[i].x, sampled[i].y));
        if (pt) {
          latLngs.push(pt);
          sumLat += pt.lat();
          sumLng += pt.lng();
        }
      }

      if (latLngs.length < 3) return;

      if (s18BizPolygon) {
        s18BizPolygon.setMap(null);
      }

      s18BizPolygon = new google.maps.Polygon({
        paths: latLngs,
        strokeColor: 'rgb(45, 106, 79)',
        strokeOpacity: 0.9,
        strokeWeight: 3,
        fillColor: 'rgb(216, 243, 220)',
        fillOpacity: 0.35,
        map: s18BizMap
      });

      const centerLat = sumLat / latLngs.length;
      const centerLng = sumLng / latLngs.length;

      const rawPoints = latLngs.map(ll => ({ lat: Number(ll.lat().toFixed(6)), lng: Number(ll.lng().toFixed(6)) }));
      const pointsInput = document.getElementById('s18-biz-area-points');
      if (pointsInput) pointsInput.value = JSON.stringify(rawPoints);

      const areaInput = document.getElementById('s18-biz-area');
      const statusEl = document.getElementById('s18-biz-map-status');

      reverseGeocodeWithGoogle(centerLat, centerLng, (res) => {
        const cleanName = res + ' 周辺 (なぞり範囲指定)';
        if (areaInput) {
          areaInput.value = cleanName;
        }
        if (statusEl) {
          statusEl.textContent = '✓ なぞり範囲指定完了 (' + rawPoints.length + '点) : ' + cleanName;
        }
      });
    };

    surface.onpointerup = finishDraw;
    surface.onpointercancel = finishDraw;
  }

  function setBizMapMode(mode) {
    s18BizMode = mode;
    const btnDraw = document.getElementById('s18-draw-btn-draw');
    const btnPan = document.getElementById('s18-draw-btn-pan');
    const surface = document.getElementById('s18-biz-draw-surface');
    const hint = document.getElementById('s18-biz-draw-hint');

    if (mode === 'draw') {
      if (btnDraw) {
        btnDraw.classList.add('is-active');
        btnDraw.style.background = 'var(--primary-700)';
        btnDraw.style.color = 'var(--bg-card)';
        btnDraw.style.fontWeight = '700';
      }
      if (btnPan) {
        btnPan.classList.remove('is-active');
        btnPan.style.background = 'var(--bg-card)';
        btnPan.style.color = 'var(--text-main)';
        btnPan.style.fontWeight = 'normal';
      }
      if (surface) {
        surface.style.pointerEvents = 'auto';
        surface.style.display = 'block';
      }
      if (hint) {
        hint.textContent = 'なぞるモード: 指やマウスで囲んで指定';
      }
      if (s18BizMap) {
        s18BizMap.setOptions({
          draggable: false,
          gestureHandling: 'none',
          scrollwheel: false
        });
      }
    } else {
      if (btnPan) {
        btnPan.classList.add('is-active');
        btnPan.style.background = 'var(--primary-700)';
        btnPan.style.color = 'var(--bg-card)';
        btnPan.style.fontWeight = '700';
      }
      if (btnDraw) {
        btnDraw.classList.remove('is-active');
        btnDraw.style.background = 'var(--bg-card)';
        btnDraw.style.color = 'var(--text-main)';
        btnDraw.style.fontWeight = 'normal';
      }
      if (surface) {
        surface.style.pointerEvents = 'none';
        surface.style.display = 'none';
      }
      if (hint) {
        hint.textContent = '地図移動モード: ドラッグして移動・拡大';
      }
      if (s18BizMap) {
        s18BizMap.setOptions({
          draggable: true,
          gestureHandling: 'greedy',
          scrollwheel: true
        });
      }
    }
  }

  function clearBizDraw() {
    if (s18BizPolygon) {
      s18BizPolygon.setMap(null);
      s18BizPolygon = null;
    }
    s18BizPoints = [];
    const pointsInput = document.getElementById('s18-biz-area-points');
    if (pointsInput) pointsInput.value = '';
    const statusEl = document.getElementById('s18-biz-map-status');
    if (statusEl) statusEl.textContent = '※範囲指定がリセットされました。地図上をなぞるか、希望エリアを入力してください。';
    const canvas = document.getElementById('s18-biz-draw-canvas');
    if (canvas && canvas.getContext) {
      const ctx = canvas.getContext('2d');
      if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
  }

  function setBizAreaFromSelect(areaName) {
    if (!areaName) return;
    const areaInput = document.getElementById('s18-biz-area');
    if (areaInput) areaInput.value = areaName;
    const statusEl = document.getElementById('s18-biz-map-status');
    if (statusEl) statusEl.textContent = '✓ 選択中: ' + areaName;

    const areaCoords = {
      '東武宇都宮駅・オリオン通り周辺': { lat: 36.5590, lng: 139.8820, delta: 0.0035 },
      'JR宇都宮駅西口・大通り周辺': { lat: 36.5600, lng: 139.8930, delta: 0.0035 },
      '宇都宮駅東口・LRT沿線エリア': { lat: 36.5580, lng: 139.9040, delta: 0.0040 },
      '競輪場通り・八幡台周辺': { lat: 36.5750, lng: 139.8880, delta: 0.0040 },
      '鶴田・西川田エリア': { lat: 36.5450, lng: 139.8550, delta: 0.0050 },
      '雀宮・インターパーク周辺': { lat: 36.5050, lng: 139.9100, delta: 0.0060 }
    };

    const target = areaCoords[areaName] || { lat: 36.5610, lng: 139.8830, delta: 0.0035 };

    if (s18BizMap && typeof google !== 'undefined' && google.maps) {
      s18BizMap.panTo({ lat: target.lat, lng: target.lng });
      s18BizMap.setZoom(15);

      if (s18BizPolygon) {
        s18BizPolygon.setMap(null);
      }
      const polyPts = [];
      const numPts = 8;
      const d = target.delta;
      for (let i = 0; i < numPts; i++) {
        const angle = (i * 2 * Math.PI) / numPts;
        const pLat = target.lat + Math.sin(angle) * d * 0.7;
        const pLng = target.lng + Math.cos(angle) * d;
        polyPts.push(new google.maps.LatLng(pLat, pLng));
      }
      s18BizPolygon = new google.maps.Polygon({
        paths: polyPts,
        strokeColor: 'rgb(45, 106, 79)',
        strokeOpacity: 0.9,
        strokeWeight: 3,
        fillColor: 'rgb(216, 243, 220)',
        fillOpacity: 0.35,
        map: s18BizMap
      });
      const pointsInput = document.getElementById('s18-biz-area-points');
      if (pointsInput) {
        pointsInput.value = JSON.stringify(polyPts.map(p => ({ lat: Number(p.lat().toFixed(6)), lng: Number(p.lng().toFixed(6)) })));
      }
    }
  }

  window.addEventListener('DOMContentLoaded', () => {
    initS18BizAreaMap();
  });
</script>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
