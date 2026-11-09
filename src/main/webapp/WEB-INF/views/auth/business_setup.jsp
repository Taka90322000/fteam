<%-- 事業主の初期設定画面。初回はLINEのメールだけを反映し、名称や代表者名は本人に入力してもらう。 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map,java.util.Set,java.util.List,jp.machiriku.bean.RegistrationChoice,jp.machiriku.bean.User,jp.machiriku.tool.Html" %>
<%
  // 入力エラー時はテキスト・選択ID・地図の点列をそのまま再表示する。
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
          <span>アカウント区分: 出店事業主</span>
        </span>
      </div>
    </div>

    <%-- 事業者情報と希望条件を送信する。なぞった座標はpreferredAreaPointsのJSON文字列にまとめる。 --%>
    <form id="business-setup-form" action="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/BusinessSetup.action" method="POST" onsubmit="return validateBusinessForm();">
      <input type="hidden" name="csrfToken" value="<%= Html.escape(request.getAttribute("csrfToken")) %>">
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

      <%-- 画像の参照先は任意入力とし、エラー時も入力値を保持する。 --%>
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-biz-icon" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          アイコン画像のURL・パス <span style="font-size:0.78rem; color:var(--text-sub);">(任意)</span>
        </label>
        <input id="s18-biz-icon" name="iconImageUrl" type="text" class="form-control" value="<%= Html.escape(formValues.get("iconImageUrl")) %>" maxlength="2048" placeholder="例: https://example.com/icon.png、/assets/icon.png">
      </div>

      <!-- メールアドレス -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-biz-email" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          メールアドレス <span style="font-size:0.78rem; color:var(--text-sub);">(変更可能)</span>
        </label>
        <input id="s18-biz-email" name="email" type="email" class="form-control" value="<%= Html.escape(email) %>" maxlength="254" placeholder="example@domain.com">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINE認証で取得できた場合に反映しています。未取得の場合は空欄です。</span>
      </div>

      <!-- 連絡先電話番号 -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-biz-phone" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          連絡先電話番号 <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
        </label>
        <input id="s18-biz-phone" name="phone" type="tel" class="form-control" value="<%= Html.escape(formValues.get("phone")) %>" required placeholder="例: 09012345678 (ハイフンなし)">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※出店応募や物件オーナーとの連絡・確認用に使用されます。</span>
      </div>

      <!-- 出店希望業種・ジャンル (複数選択可) -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
          出店希望業種・ジャンル <span style="color:var(--danger-text); font-size:0.8rem;">*必須 (複数選択可)</span>
        </label>
        <div style="display:grid; grid-template-columns:repeat(auto-fill, minmax(130px, 1fr)); gap:0.4rem;">
          <%-- DBのマスターに存在するIDを送信し、初回は業種を自動選択しない。 --%>
          <% for (RegistrationChoice choice : industryChoices) { %>
            <label style="display:flex; align-items:center; gap:0.4rem; padding:0.5rem 0.65rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.84rem;">
              <input type="checkbox" name="preferredIndustry" value="<%= Html.escape(choice.id()) %>" <%= selectedIndustries.contains(String.valueOf(choice.id())) ? "checked" : "" %> style="cursor:pointer; margin:0;">
              <span><%= Html.escape(choice.label()) %></span>
            </label>
          <% } %>
        </div>
      </div>

      <!-- 出店希望地域 (エリア - 地図なぞり範囲指定 / 選択) -->
      <div class="form-group" style="margin-bottom:1.5rem; background:var(--bg-subtle); padding:var(--space-4); border-radius:var(--radius-lg); border:1px solid var(--border-color);">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.65rem; flex-wrap:wrap; gap:0.5rem;">
          <label for="s18-biz-area" style="font-weight:800; font-size:0.95rem; color:var(--text-main);">
            出店希望エリア <span style="color:var(--danger-text); font-size:0.8rem;">*必須 (マップ上で囲んで指定)</span>
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
           <input id="s18-biz-area" name="preferredArea" type="text" class="form-control" value="<%= Html.escape(formValues.get("preferredArea")) %>" maxlength="150" required placeholder="地図上を囲んで指定してください。エリア名は後から修正できます" style="width:100%;">
           <%-- JSONはHTML属性としてエスケープし、JavaScriptではDOMのvalueから読み取る。 --%>
           <input id="s18-biz-area-points" name="preferredAreaPoints" type="hidden" value="<%= Html.escape(formValues.get("preferredAreaPoints")) %>">
          <div id="s18-biz-map-status" style="font-size:0.75rem; color:var(--primary-700); margin-top:0.35rem; font-weight:600;">
            ※地図上をなぞって範囲を指定してください。エリア名だけでは登録できません。
          </div>
        </div>
      </div>

      <!-- 従業員数 -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-biz-employees" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          従業員数 (予定含む) <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
        </label>
        <input id="s18-biz-employees" name="employeeCount" type="number" min="0" class="form-control" value="<%= Html.escape(formValues.get("employeeCount")) %>" required placeholder="例: 1">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※ご自身のみ・初出店の場合は「1」または「0」と入力してください。</span>
      </div>

      <!-- 店舗経営の経験 -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
          店舗経営の経験 <span style="color:var(--text-muted); font-size:0.8rem;">(任意)</span>
        </label>
        <div style="display:flex; flex-wrap:wrap; gap:0.5rem;">
          <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.85rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.875rem;">
            <input type="radio" name="mgmtExperience" value="あり" <%= "あり".equals(formValues.get("mgmtExperience")) ? "checked" : "" %> style="cursor:pointer;">
            <span>あり (現在経営中 / 過去に経営経験あり)</span>
          </label>
          <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.85rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.875rem;">
            <input type="radio" name="mgmtExperience" value="なし" <%= "なし".equals(formValues.get("mgmtExperience")) ? "checked" : "" %> style="cursor:pointer;">
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
          <%--String[] qList = {"食品衛生責任者", "防火管理者", "調理師免許", "美容師・理容師免許", "宅地建物取引士", "その他資格・許認可"};--%>
          <% for (RegistrationChoice choice : qualificationChoices) { %>
            <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.75rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.84rem;">
              <input type="checkbox" name="qualification" value="<%= Html.escape(choice.id()) %>" <%= selectedQualifications.contains(String.valueOf(choice.id())) ? "checked" : "" %> style="cursor:pointer; margin:0;">
              <span><%= Html.escape(choice.label()) %></span>
            </label>
          <% } %>
        </div>
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.35rem;">※出店応募時に必要な資格情報を事前に登録しておくことで、スムーズに応募手続きを行えます。</span>
      </div>

      <!-- 新着店舗・オファー情報のメール受信設定 -->
      <label style="display:flex; align-items:center; gap:0.65rem; cursor:pointer; font-size:0.875rem; color:var(--text-main); font-weight:600; line-height:1.45; margin-top:1.5rem; padding:0.75rem 1rem; background:var(--bg-subtle); border-radius:var(--radius-md); border:1px solid var(--border-color); box-sizing:border-box; min-height:48px;">
        <input type="checkbox" id="s18-biz-email-notify" name="emailNotify" <%= !formSubmitted || formValues.containsKey("emailNotify") ? "checked" : "" %> style="cursor:pointer; width:18px; height:18px; margin:0; flex-shrink:0;">
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
    <div style="margin-top:1.5rem; text-align:center;">
      <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Logout.action" onclick="return confirm('登録を中断してログアウトしますか？');" style="color:var(--text-sub); font-size:0.875rem; text-decoration:underline; cursor:pointer;">
        登録を中断してログアウト
      </a>
    </div>
    <%-- 本登録前は、利用者区分の選択画面へ戻って登録を続けられる。 --%>
    <% User registrationUser = session.getAttribute("user") instanceof User ? (User) session.getAttribute("user") : null;
       if (registrationUser != null && "BUSINESS".equals(registrationUser.getUserRole()) && "PENDING".equals(registrationUser.getAccountStatus())
        && registrationUser.getWithdrawnAt() == null) { %>
      <div style="text-align:center; margin-top:1.25rem; font-size:0.875rem;">
        <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Register.action" style="color:var(--primary-700); font-weight:700; text-decoration:underline;">利用者区分を選び直す</a>
      </div>
    <% } %>
  </div>
</section>

<!-- Google Mapsの地図表示・座標変換に使う外部APIを読み込む。 -->
<script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyCNWrNbVnBfZsjuTJ3sHCKDemfkxNsIv7w&libraries=marker"></script>

<script>
  // 地図・座標変換用のオーバーレイ・表示中のポリゴンと操作状態を、この画面内で管理する。
  let s18BizMap = null;
  let s18BizOverlay = null;
  let s18BizPolygon = null;
  let s18BizPoints = [];
  let s18BizMode = 'draw';
  let s18BizIsDrawing = false;
  let s18BizSelectionRevision = 0;

  // 点列はHTML属性から読み取り、JSONをスクリプトへ直接埋め込まない。
  function readBizAreaPoints() {
    const input = document.getElementById('s18-biz-area-points');
    try {
      const points = JSON.parse(input.value);
      if (!Array.isArray(points) || points.length < 3 || points.some(point => !point
          || typeof point.lat !== 'number' || typeof point.lng !== 'number'
          || !Number.isFinite(point.lat) || !Number.isFinite(point.lng)
          || Math.abs(point.lat) > 90 || Math.abs(point.lng) > 180)) return null;
      return new Set(points.map(point => point.lat + ',' + point.lng)).size >= 3 ? points : null;
    } catch (_) {
      return null;
    }
  }

  // エラー再表示では、送信された範囲と入力中のエリア名を変更せずに復元する。
  function restoreBizArea() {
    const points = readBizAreaPoints();
    if (!points || !s18BizMap) return;
    s18BizPoints = points;
    s18BizPolygon = new google.maps.Polygon({
      paths: points, strokeColor: 'rgb(45, 106, 79)', strokeOpacity: 0.9, strokeWeight: 3,
      fillColor: 'rgb(216, 243, 220)', fillOpacity: 0.35, map: s18BizMap
    });
    const bounds = new google.maps.LatLngBounds();
    points.forEach(point => bounds.extend(point));
    s18BizMap.fitBounds(bounds);
    document.getElementById('s18-biz-map-status').textContent = '✓ 選択済みの範囲を復元しました (' + points.length + '点)';
  }

  // 送信前に事業者名・代表者名・電話番号・希望エリア・業種の必須入力を確認する。
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
    // DBで必須となる実際の範囲を確認し、エリア名だけでの登録を防ぐ。
    if (!areaInput || !areaInput.value.trim()) {
      alert('出店希望エリアを地図上で囲んで指定し、エリア名を入力してください');
      if (areaInput) areaInput.focus();
      return false;
    }
    if (!readBizAreaPoints()) {
      alert('出店希望エリアを地図上で囲んで指定してください（3点以上の範囲が必要です）');
      setBizMapMode('draw');
      document.getElementById('s18-biz-map-wrap').scrollIntoView({ block: 'center' });
      return false;
    }

    const industries = document.querySelectorAll('input[name="preferredIndustry"]:checked');
    if (industries.length === 0) {
      alert('出店希望業種・ジャンルを少なくとも1つ選択してください');
      return false;
    }

    return true;
  }

  // 範囲の代表点から住所名を調べ、取得できなければ既定の周辺名を返す。
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

  // Google Mapsの読み込み完了を待ち、希望エリアを描く地図を初期化する。
  function initS18BizAreaMap() {
    const container = document.getElementById('s18-biz-map-picker');
    const wrap = document.getElementById('s18-biz-map-wrap');
    if (!container || !wrap) return;

    // 最初はなぞる操作を優先し、地図移動を止めた状態で描画用の投影変換を用意する。
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

      // 画面上のピクセル位置を緯度・経度へ変換するため、地図の投影情報を取得できるようにする。
      s18BizOverlay = new google.maps.OverlayView();
      s18BizOverlay.draw = function() {};
      s18BizOverlay.setMap(s18BizMap);

      setupBizDrawingListeners(wrap);
      restoreBizArea();

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

    // 外部APIが後から読み込まれた場合にも初期化できるよう、一定回数だけ再確認する。
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

  // マウスとタッチをPointer Eventsで扱い、描画中の画面座標を集める。
  function setupBizDrawingListeners(wrap) {
    let surface = document.getElementById('s18-biz-draw-surface');
    let canvas = document.getElementById('s18-biz-draw-canvas');
    if (!surface || !canvas) return;

    const rect = wrap.getBoundingClientRect();
    canvas.width = rect.width || wrap.clientWidth || 320;
    canvas.height = rect.height || wrap.clientHeight || 270;
    const ctx = canvas.getContext ? canvas.getContext('2d') : null;

    // ここで保持するx・yは地図コンテナ内の画面座標で、まだ緯度・経度ではない。
    let pixelPoints = [];

    // 描画開始位置を記録し、ポインターを捕捉して枠外へ動いても操作を継続する。
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

    // 移動した画面座標を追加し、キャンバス上へ途中の線を表示する。
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

    // 指やマウスを離した時点で線を確定し、画面座標を地理座標へ変換する。
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

      // 移動点を間引き、送信用の座標数とポリゴン描画の負荷を抑える。
      const sampled = [];
      const step = Math.max(1, Math.floor(pixelPoints.length / 60));
      for (let i = 0; i < pixelPoints.length; i += step) {
        sampled.push(pixelPoints[i]);
      }
      if (sampled[sampled.length - 1] !== pixelPoints[pixelPoints.length - 1]) {
        sampled.push(pixelPoints[pixelPoints.length - 1]);
      }

      // 地図の投影情報を使ってx・yを緯度・経度へ変換し、住所検索用の座標合計も求める。
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

      // 変換した点列を地図上のポリゴンとして表示する。見た目の閉じ方はGoogle Mapsに任せる。
      s18BizPolygon = new google.maps.Polygon({
        paths: latLngs,
        strokeColor: 'rgb(45, 106, 79)',
        strokeOpacity: 0.9,
        strokeWeight: 3,
        fillColor: 'rgb(216, 243, 220)',
        fillOpacity: 0.35,
        map: s18BizMap
      });

      // 各頂点の単純平均を住所検索の代表点として使う。面積から求めた重心ではない。
      const centerLat = sumLat / latLngs.length;
      const centerLng = sumLng / latLngs.length;

      // lat・lngを小数点以下6桁に丸めたオブジェクト配列にする。GeoJSONの経度・緯度配列にはまだ変換しない。
      const rawPoints = latLngs.map(ll => ({ lat: Number(ll.lat().toFixed(6)), lng: Number(ll.lng().toFixed(6)) }));
      const pointsInput = document.getElementById('s18-biz-area-points');
      // 座標配列をJSON文字列にしてhidden項目へ入れ、フォームPOSTでサーバーへ渡す。
      if (pointsInput) pointsInput.value = JSON.stringify(rawPoints);
      s18BizPoints = rawPoints;
      const revision = ++s18BizSelectionRevision;

      const areaInput = document.getElementById('s18-biz-area');
      const statusEl = document.getElementById('s18-biz-map-status');

      // 代表点の住所から希望エリア名を補い、指定した点数とともに選択状況を表示する。
      reverseGeocodeWithGoogle(centerLat, centerLng, (res) => {
        if (revision !== s18BizSelectionRevision) return;
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

  // なぞるモードと地図移動モードを切り替え、描画面への入力と地図操作を排他的に切り替える。
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

  // 表示中の範囲と送信用座標をリセットする。エリア名の入力値は現行仕様どおり残す。
  function clearBizDraw() {
    s18BizSelectionRevision++;
    if (s18BizPolygon) {
      s18BizPolygon.setMap(null);
      s18BizPolygon = null;
    }
    s18BizPoints = [];
    const pointsInput = document.getElementById('s18-biz-area-points');
    if (pointsInput) pointsInput.value = '';
    const statusEl = document.getElementById('s18-biz-map-status');
    if (statusEl) statusEl.textContent = '※範囲指定がリセットされました。地図上を囲んで指定してください。';
    const canvas = document.getElementById('s18-biz-draw-canvas');
    if (canvas && canvas.getContext) {
      const ctx = canvas.getContext('2d');
      if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
  }

  // 画面要素を取得できるようになってから、希望エリアマップの準備を始める。
  window.addEventListener('DOMContentLoaded', () => {
    // 範囲選択後に手修正したエリア名を、遅れて返った住所検索結果で上書きしない。
    document.getElementById('s18-biz-area').addEventListener('input', () => s18BizSelectionRevision++);
    initS18BizAreaMap();
  });
</script>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
