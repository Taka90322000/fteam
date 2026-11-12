<%-- 出店事業主のプロフィール設定画面。登録済み会員専用。後から項目を追加しやすいセクション構造。 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map,java.util.Set,java.util.List,jp.machiriku.bean.RegistrationChoice,jp.machiriku.bean.User,jp.machiriku.tool.Html" %>
<%
  Map<String, String> formValues = request.getAttribute("formValues") instanceof Map
      ? (Map<String, String>) request.getAttribute("formValues") : java.util.Collections.emptyMap();
  String displayName = formValues.getOrDefault("representativeName", "");
  String email = (String) request.getAttribute("email");
  if (email == null) email = "";
  email = formValues.getOrDefault("email", email);
  Set<String> selectedIndustries = request.getAttribute("selectedIndustries") instanceof Set
      ? (Set<String>) request.getAttribute("selectedIndustries") : java.util.Collections.emptySet();
  Set<String> selectedQualifications = request.getAttribute("selectedQualifications") instanceof Set
      ? (Set<String>) request.getAttribute("selectedQualifications") : java.util.Collections.emptySet();
  List<RegistrationChoice> industryChoices = request.getAttribute("industryChoices") instanceof List
      ? (List<RegistrationChoice>) request.getAttribute("industryChoices") : java.util.Collections.emptyList();
  List<RegistrationChoice> qualificationChoices = request.getAttribute("qualificationChoices") instanceof List
      ? (List<RegistrationChoice>) request.getAttribute("qualificationChoices") : java.util.Collections.emptyList();
  boolean formSubmitted = "POST".equals(request.getMethod());
%>
<jsp:include page="/WEB-INF/views/common/header.jsp">
  <jsp:param name="title" value="出店事業主 プロフィール設定 | マチリク" />
  <jsp:param name="isOnboarding" value="false" />
  <jsp:param name="roleLabel" value="出店事業主" />
  <jsp:param name="userName" value="<%= displayName %>" />
</jsp:include>

<section class="screen-view s18-view s18-business-view" data-screen="S18">
  <div class="screen-header-banner" style="background:var(--primary-50); border:1px solid var(--primary-100); border-radius:var(--radius-lg); padding:1rem 1.25rem; margin-bottom:1.5rem; display:flex; align-items:center; gap:1rem;">
    <span class="screen-header-badge" style="background:var(--primary-700); color:var(--text-inverse); font-weight:800; font-size:0.85rem; padding:0.25rem 0.6rem; border-radius:var(--radius-full);">S18</span>
    <div>
      <h1 class="screen-header-title" style="font-size:1.25rem; font-weight:800; color:var(--primary-900); margin:0;">プロフィール・出店設定</h1>
      <p class="screen-header-desc" style="font-size:0.85rem; color:var(--text-sub); margin:0.25rem 0 0 0;">登録情報の確認や変更ができます。</p>
    </div>
  </div>

  <div class="form-stepper-box" style="max-width: 680px; margin:0 auto; background:var(--bg-card); padding:2rem; border-radius:var(--radius-lg); border:1px solid var(--border-color); box-shadow:var(--shadow-sm);">
    <% if (request.getAttribute("errorMessage") != null) { %>
      <div role="alert" style="background:#fee2e2; color:#991b1b; padding:0.75rem 1rem; border-radius:var(--radius-md); margin-bottom:1rem;"><%= Html.escape(request.getAttribute("errorMessage")) %></div>
    <% } %>

    <form id="business-profile-form" action="${pageContext.request.contextPath}/jp/machiriku/servlet/user/Profile.action" method="POST" onsubmit="return validateBusinessForm();">
      <input type="hidden" name="csrfToken" value="<%= Html.escape(request.getAttribute("csrfToken")) %>">

      <!-- ========================================================
           【セクション1: 事業者・連絡先基本情報】
           ======================================================== -->
      <div class="profile-section-block" style="margin-bottom:1.5rem;">
        <!-- 事業者名 / 屋号 -->
        <div class="form-group" style="margin-bottom:1.25rem;">
          <label for="s18-biz-name" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
            事業者名 / 屋号 <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
          </label>
          <input id="s18-biz-name" name="businessName" type="text" class="form-control" value="<%= Html.escape(formValues.get("businessName")) %>" maxlength="150" required placeholder="例: 宇都宮カフェ Lab">
          <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※店舗名、屋号、法人名などをご入力ください。</span>
        </div>

        <!-- 代表者名 -->
        <div class="form-group" style="margin-bottom:1.25rem;">
          <label for="s18-rep-name" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
            代表者名 <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
          </label>
          <input id="s18-rep-name" name="representativeName" type="text" class="form-control" value="<%= Html.escape(displayName) %>" maxlength="100" required placeholder="例: 佐藤 健一" oninput="const h = document.getElementById('header-user-name'); if(h) h.textContent = this.value.trim();">
          <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※事業を代表する方のお名前をご入力ください。</span>
        </div>

        <!-- アイコン画像のURL -->
        <div class="form-group" style="margin-bottom:1.25rem;">
          <label for="s18-biz-icon" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
            アイコン画像のURL・パス <span style="font-size:0.78rem; color:var(--text-sub);">(任意)</span>
          </label>
          <input id="s18-biz-icon" name="iconImageUrl" type="text" class="form-control" value="<%= Html.escape(formValues.get("iconImageUrl")) %>" maxlength="2048" placeholder="例: https://example.com/icon.png、/assets/icon.png">
        </div>

        <!-- メールアドレス -->
        <div class="form-group" style="margin-bottom:1.25rem;">
          <label for="s18-biz-email" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
            メールアドレス <span style="font-size:0.78rem; color:var(--text-sub);">(任意・通知受信用)</span>
          </label>
          <input id="s18-biz-email" name="email" type="email" class="form-control" value="<%= Html.escape(email) %>" maxlength="254" placeholder="example@domain.com">
          <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※重要なお知らせやオファー情報の配信先メールアドレスです。</span>
        </div>

        <!-- 連絡先電話番号 -->
        <div class="form-group" style="margin-bottom:1.25rem;">
          <label for="s18-biz-phone" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
            連絡先電話番号 <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
          </label>
          <input id="s18-biz-phone" name="phone" type="tel" class="form-control" value="<%= Html.escape(formValues.get("phone")) %>" required placeholder="例: 09012345678 (ハイフンなし)">
          <div id="s18-biz-phone-error" style="display:none; color:var(--danger-text); font-size:0.8rem; margin-top:0.35rem; font-weight:600;"></div>
          <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※物件オーナーや事務局からの連絡用に使用されます。</span>
        </div>
      </div>

      <!-- ========================================================
           【セクション2: 出店希望条件】エリア・希望業種
           ======================================================== -->
      <div class="profile-section-block" style="margin-bottom:1.5rem; padding-top:0.5rem; border-top:1px solid var(--border-color);">
        <!-- 出店希望エリア (地図なぞり描き) -->
        <div class="form-group" style="margin-bottom:1.5rem; background:var(--bg-subtle); padding:var(--space-4); border-radius:var(--radius-lg); border:1px solid var(--border-color);">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.35rem; flex-wrap:wrap; gap:0.5rem;">
            <label style="font-weight:800; font-size:0.95rem; color:var(--text-main);">
              出店希望エリア <span style="color:var(--danger-text); font-size:0.8rem;">*必須 (地図上を囲んで指定)</span>
            </label>
            <button type="button" class="btn-outline btn-sm" onclick="clearBizAreaPolygon()" style="font-size:0.8rem; padding:0.3rem 0.75rem; border-radius:var(--radius-full); display:inline-flex; align-items:center; gap:0.35rem; background:var(--bg-card); cursor:pointer;">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
              <span>指定をクリア</span>
            </button>
          </div>
          <p style="font-size:0.8rem; color:var(--text-sub); margin-bottom:0.6rem; line-height:1.4;">
            地図上をクリックして3点以上の多角形で希望する出店範囲を囲んでください。
          </p>

          <div id="s18-biz-area-map" class="s18-biz-area-map" style="width:100%; height:280px; border-radius:var(--radius-md); border:1.5px solid var(--border-color); background-color:var(--bg-subtle); position:relative; overflow:hidden;"></div>

          <div style="margin-top:0.75rem;">
            <label for="s18-biz-area-name" style="font-size:0.8rem; font-weight:700; color:var(--text-sub); display:block; margin-bottom:0.25rem;">
              希望エリアの目安名称:
            </label>
            <input id="s18-biz-area-name" name="preferredArea" type="text" class="form-control" value="<%= Html.escape(formValues.get("preferredArea")) %>" placeholder="例: 宇都宮駅西口・大通り周辺">
            <input id="s18-biz-area-points" name="preferredAreaPoints" type="hidden" value="<%= Html.escape(formValues.get("preferredAreaPoints")) %>">
            <div id="s18-biz-area-status" style="font-size:0.75rem; color:var(--primary-700); margin-top:0.35rem; font-weight:600;">
              ※地図上をクリックして出店希望範囲を囲んでください（3点以上）
            </div>
          </div>
        </div>

        <!-- 出店したい業種・業態 -->
        <div class="form-group" style="margin-bottom:1.25rem;">
          <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
            出店したい業種・業態 <span style="color:var(--danger-text); font-size:0.8rem;">*必須 (複数選択可)</span>
          </label>
          <div style="display:grid; grid-template-columns:repeat(auto-fill, minmax(130px, 1fr)); gap:0.4rem;">
            <% for (RegistrationChoice choice : industryChoices) { %>
              <label style="display:flex; align-items:center; gap:0.4rem; padding:0.5rem 0.65rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.84rem;">
                <input type="checkbox" name="preferredIndustry" value="<%= Html.escape(choice.id()) %>" <%= selectedIndustries.contains(String.valueOf(choice.id())) ? "checked" : "" %> style="cursor:pointer; margin:0;">
                <span><%= Html.escape(choice.label()) %></span>
              </label>
            <% } %>
          </div>
        </div>
      </div>

      <!-- ========================================================
           【セクション3: 事業規模・経験・資格】
           ======================================================== -->
      <div class="profile-section-block" style="margin-bottom:1.5rem; padding-top:0.5rem; border-top:1px solid var(--border-color);">
        <!-- 従業員規模 -->
        <div class="form-group" style="margin-bottom:1.25rem;">
          <label for="s18-biz-emp" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
            従業員規模 <span style="color:var(--text-muted); font-size:0.8rem;">(任意)</span>
          </label>
          <input id="s18-biz-emp" name="employeeCount" type="number" min="1" max="9999" class="form-control" value="<%= Html.escape(formValues.getOrDefault("employeeCount", "1")) %>" placeholder="例: 3">
        </div>

        <!-- 店舗・事業の経営経験 -->
        <div class="form-group" style="margin-bottom:1.25rem;">
          <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
            店舗・事業の経営経験 <span style="color:var(--text-muted); font-size:0.8rem;">(任意)</span>
          </label>
          <div style="display:flex; flex-wrap:wrap; gap:0.5rem;">
            <%
              String[] mgmts = {"なし", "1〜3年未満", "3〜5年未満", "5年以上"};
              for (int i = 0; i < mgmts.length; i++) {
            %>
              <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.85rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.875rem;">
                <input type="radio" name="mgmtExperience" value="<%= Html.escape(mgmts[i]) %>" <%= mgmts[i].equals(formValues.get("mgmtExperience")) ? "checked" : "" %> style="cursor:pointer;">
                <span><%= Html.escape(mgmts[i]) %></span>
              </label>
            <% } %>
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
                <input type="radio" name="industryExperience" value="<%= Html.escape(exps[i]) %>" <%= exps[i].equals(formValues.get("industryExperience")) ? "checked" : "" %> style="cursor:pointer;">
                <span><%= Html.escape(exps[i]) %></span>
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
            <% for (RegistrationChoice choice : qualificationChoices) { %>
              <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.75rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.84rem;">
                <input type="checkbox" name="qualification" value="<%= Html.escape(choice.id()) %>" <%= selectedQualifications.contains(String.valueOf(choice.id())) ? "checked" : "" %> style="cursor:pointer; margin:0;">
                <span><%= Html.escape(choice.label()) %></span>
              </label>
            <% } %>
          </div>
        </div>
      </div>

      <!-- ========================================================
           【セクション4: 通知設定】メール受信設定
           ======================================================== -->
      <div class="profile-section-block" style="margin-bottom:1.5rem; padding-top:0.5rem; border-top:1px solid var(--border-color);">
        <label style="display:flex; align-items:center; gap:0.65rem; cursor:pointer; font-size:0.875rem; color:var(--text-main); font-weight:600; line-height:1.45; padding:0.75rem 1rem; background:var(--bg-subtle); border-radius:var(--radius-md); border:1px solid var(--border-color); box-sizing:border-box; min-height:48px;">
          <input type="checkbox" id="s18-biz-email-notify" name="emailNotify" <%= !formSubmitted || formValues.containsKey("emailNotify") ? "checked" : "" %> style="cursor:pointer; width:18px; height:18px; margin:0; flex-shrink:0;">
          <span>希望条件に合う新着空き店舗や物件オーナーからのオファー通知をメールで受け取る</span>
        </label>
      </div>

      <!-- ========================================================
           【セクション5: 将来の拡張用エリア】
           ※今後プロフィールに追加したい項目はここへ追記できます。
           ======================================================== -->

      <!-- 送信ボタン -->
      <div style="margin-top:2rem;">
        <button type="submit" class="btn-primary" style="width:100%; min-height:48px; font-size:1.05rem; font-weight:800; display:flex; align-items:center; justify-content:center; gap:0.5rem; box-shadow:var(--shadow-floating); cursor:pointer;">
          <span>設定を保存する</span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
      </div>
    </form>

    <div style="margin-top:1.5rem; text-align:center;">
      <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Logout.action" onclick="return confirm('ログアウトしますか？');" style="color:var(--text-sub); font-size:0.875rem; text-decoration:underline; cursor:pointer;">
        ログアウト
      </a>
    </div>
  </div>
</section>

<!-- Google Maps JavaScript API -->
<script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyCNWrNbVnBfZsjuTJ3sHCKDemfkxNsIv7w&libraries=geometry"></script>

<script>
  let bizMap = null;
  let bizPolygon = null;
  let bizPoints = [];
  const bizCenter = { lat: 36.5610, lng: 139.8830 };

  function initBizAreaMap() {
    if (bizMap || typeof google === 'undefined' || !google.maps || !google.maps.Map) return;
    const container = document.getElementById('s18-biz-area-map');
    if (!container) return;

    bizMap = new google.maps.Map(container, {
      center: bizCenter, zoom: 14, mapTypeControl: false, streetViewControl: false,
      fullscreenControl: false, zoomControl: false, gestureHandling: 'greedy', scrollwheel: true
    });

    bizPolygon = new google.maps.Polygon({
      paths: [],
      strokeColor: '#2d6a4f',
      strokeOpacity: 0.85,
      strokeWeight: 2,
      fillColor: '#52b788',
      fillOpacity: 0.35,
      map: bizMap
    });

    bizMap.addListener('click', (e) => {
      bizPoints.push({ lat: e.latLng.lat(), lng: e.latLng.lng() });
      updateBizPolygon();
    });

    const savedPoints = document.getElementById('s18-biz-area-points').value;
    if (savedPoints) {
      try {
        const parsed = JSON.parse(savedPoints);
        if (Array.isArray(parsed) && parsed.length >= 3) {
          bizPoints = parsed;
          updateBizPolygon();
          const bounds = new google.maps.LatLngBounds();
          bizPoints.forEach(p => bounds.extend(p));
          bizMap.fitBounds(bounds);
        }
      } catch (_) {}
    }
  }

  function updateBizPolygon() {
    if (!bizPolygon) return;
    bizPolygon.setPath(bizPoints);
    const hidden = document.getElementById('s18-biz-area-points');
    const status = document.getElementById('s18-biz-area-status');
    if (hidden) hidden.value = JSON.stringify(bizPoints);
    if (status) {
      if (bizPoints.length < 3) {
        status.textContent = '※出店希望範囲を囲んでください（あと ' + (3 - bizPoints.length) + ' 点必要です）';
      } else {
        status.textContent = '✓ 出店希望エリアが設定されています (' + bizPoints.length + ' 頂点の範囲)';
      }
    }
  }

  function clearBizAreaPolygon() {
    bizPoints = [];
    updateBizPolygon();
    const areaName = document.getElementById('s18-biz-area-name');
    if (areaName) areaName.value = '';
    const status = document.getElementById('s18-biz-area-status');
    if (status) status.textContent = '※地図上をクリックして出店希望範囲を囲んでください（3点以上）';
  }

  function showBizPhoneError(msg) {
    const errorEl = document.getElementById('s18-biz-phone-error');
    const phoneInput = document.getElementById('s18-biz-phone');
    if (errorEl) {
      errorEl.textContent = msg;
      errorEl.style.display = 'block';
    }
    if (phoneInput) {
      phoneInput.style.borderColor = 'var(--danger-border, #ef4444)';
    }
  }

  function clearBizPhoneError() {
    const errorEl = document.getElementById('s18-biz-phone-error');
    const phoneInput = document.getElementById('s18-biz-phone');
    if (errorEl) {
      errorEl.textContent = '';
      errorEl.style.display = 'none';
    }
    if (phoneInput) {
      phoneInput.style.borderColor = '';
    }
  }

  function validateBusinessForm() {
    const bizName = document.getElementById('s18-biz-name');
    const repName = document.getElementById('s18-rep-name');
    const phone = document.getElementById('s18-biz-phone');

    if (!bizName || !bizName.value.trim()) {
      alert('事業者名 / 屋号を入力してください');
      if (bizName) bizName.focus();
      return false;
    }
    if (!repName || !repName.value.trim()) {
      alert('代表者名を入力してください');
      if (repName) repName.focus();
      return false;
    }
    if (!phone || !phone.value.trim()) {
      showBizPhoneError('連絡先電話番号を入力してください');
      alert('連絡先電話番号を入力してください');
      if (phone) phone.focus();
      return false;
    }
    const rawBizPhone = phone.value.trim();
    const bizDigitsOnly = rawBizPhone.replace(/[\s()（）ー−‐-]/g, '');
    if (!/^\d+$/.test(bizDigitsOnly)) {
      showBizPhoneError('電話番号は半角数字で入力してください（英字や記号は使用できません）');
      alert('電話番号は半角数字で入力してください（英字や記号は使用できません）');
      if (phone) phone.focus();
      return false;
    }
    if (bizDigitsOnly.length < 9 || bizDigitsOnly.length > 11) {
      showBizPhoneError('電話番号の桁数を確認してください（例: 09012345678）');
      alert('電話番号の桁数を確認してください（例: 09012345678）');
      if (phone) phone.focus();
      return false;
    }
    clearBizPhoneError();

    if (bizPoints.length < 3) {
      alert('出店希望エリアを地図上で3点以上囲んで指定してください');
      return false;
    }
    const industries = document.querySelectorAll('input[name="preferredIndustry"]:checked');
    if (industries.length === 0) {
      alert('出店したい業種を少なくとも1つ選択してください');
      return false;
    }
    return true;
  }

  document.addEventListener('DOMContentLoaded', () => {
    const bizPhoneInput = document.getElementById('s18-biz-phone');
    if (bizPhoneInput) {
      bizPhoneInput.addEventListener('input', function() {
        const val = this.value.trim();
        if (!val) { clearBizPhoneError(); return; }
        const digits = val.replace(/[\s()（）ー−‐-]/g, '');
        if (!/^\d*$/.test(digits)) {
          showBizPhoneError('※電話番号は半角数字で入力してください（英字や記号は使用できません）');
        } else {
          clearBizPhoneError();
        }
      });
    }
    initBizAreaMap();
    if (!bizMap) {
      let retries = 0;
      const timer = setInterval(() => {
        initBizAreaMap();
        if (bizMap || ++retries > 60) clearInterval(timer);
      }, 100);
    }
  });
</script>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
