<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map,java.util.Set,java.util.List,jp.machiriku.bean.RegistrationChoice,jp.machiriku.bean.User,jp.machiriku.tool.Html" %>
<%-- オーナー情報と誘致希望業種を送信する。初回はLINE取得値、エラー時は入力済みの値を表示する。 --%>
<%
  Map<String, String> formValues = request.getAttribute("formValues") instanceof Map
      ? (Map<String, String>) request.getAttribute("formValues") : java.util.Collections.emptyMap();
  String displayName = (String) request.getAttribute("displayName");
  if (displayName == null) displayName = "";
  displayName = formValues.getOrDefault("representativeName", displayName);
  String email = (String) request.getAttribute("email");
  if (email == null) email = "";
  email = formValues.getOrDefault("email", email);
  Set<String> selectedIndustries = request.getAttribute("selectedIndustries") instanceof Set
      ? (Set<String>) request.getAttribute("selectedIndustries") : java.util.Collections.emptySet();
  List<RegistrationChoice> industryChoices = request.getAttribute("industryChoices") instanceof List
      ? (List<RegistrationChoice>) request.getAttribute("industryChoices") : java.util.Collections.emptyList();
  boolean formSubmitted = "POST".equals(request.getMethod());
  User currentUser = session.getAttribute("user") instanceof User ? (User) session.getAttribute("user") : null;
  boolean isOnboarding = currentUser == null || "PENDING".equals(currentUser.getAccountStatus())
      || "1".equals(request.getParameter("onboarding"));
%>
<jsp:include page="/WEB-INF/views/common/header.jsp">
  <jsp:param name="title" value='<%= isOnboarding ? "物件オーナー 初期設定 | マチリク" : "物件オーナー プロフィール設定 | マチリク" %>' />
  <jsp:param name="isOnboarding" value='<%= String.valueOf(isOnboarding) %>' />
  <jsp:param name="roleLabel" value="物件オーナー" />
  <jsp:param name="userName" value="<%= displayName %>" />
</jsp:include>

<section class="screen-view s18-view s18-owner-view" data-screen="S18">
  <div class="screen-header-banner" style="background:var(--primary-50); border:1px solid var(--primary-100); border-radius:var(--radius-lg); padding:1rem 1.25rem; margin-bottom:1.5rem; display:flex; align-items:center; gap:1rem;">
    <span class="screen-header-badge" style="background:var(--primary-700); color:var(--text-inverse); font-weight:800; font-size:0.85rem; padding:0.25rem 0.6rem; border-radius:var(--radius-full);">S18</span>
    <div>
      <h1 class="screen-header-title" style="font-size:1.25rem; font-weight:800; color:var(--primary-900); margin:0;"><%= isOnboarding ? "アカウントの初期設定" : "プロフィール・オーナー設定" %></h1>
      <p class="screen-header-desc" style="font-size:0.85rem; color:var(--text-sub); margin:0.25rem 0 0 0;"><%= isOnboarding ? "LINEアカウントから情報を取得しました。オーナー情報や誘致したい希望業種を入力してマチリクを始めましょう。" : "登録情報の確認や変更ができます。" %></p>
    </div>
  </div>

  <div class="form-stepper-box" style="max-width: 680px; margin:0 auto; background:var(--bg-card); padding:2rem; border-radius:var(--radius-lg); border:1px solid var(--border-color); box-shadow:var(--shadow-sm);">
    <% if (request.getAttribute("errorMessage") != null) { %>
      <div role="alert" style="background:#fee2e2; color:#991b1b; padding:0.75rem 1rem; border-radius:var(--radius-md); margin-bottom:1rem;"><%= Html.escape(request.getAttribute("errorMessage")) %></div>
    <% } %>
    
    <% if (isOnboarding) { %>
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
          <span>アカウント区分: 物件オーナー</span>
        </span>
      </div>
    </div>
    <% } %>

    <form id="owner-setup-form" action="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/OwnerSetup.action" method="POST" onsubmit="return validateOwnerForm();">
      <input type="hidden" name="csrfToken" value="<%= Html.escape(request.getAttribute("csrfToken")) %>">
      <% if (!isOnboarding) { %>
      <input type="hidden" name="mode" value="edit">
      <% } %>
      <!-- オーナー氏名 / 代表者名 -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-owner-name" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          オーナー氏名 / 代表者名 <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
        </label>
        <input id="s18-owner-name" name="representativeName" type="text" class="form-control" value="<%= Html.escape(displayName) %>" maxlength="100" required placeholder="例: 佐藤 武雄" oninput="const h = document.getElementById('header-user-name'); if(h) h.textContent = this.value.trim();">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINEの表示名が初期設定されています。本名や代表者名にご変更いただけます。</span>
      </div>

      <!-- 屋号・法人名 / 管理名 -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-owner-company" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          屋号・法人名 / 管理名 <span style="font-size:0.78rem; color:var(--text-sub);">(任意)</span>
        </label>
        <input id="s18-owner-company" name="company" type="text" class="form-control" value="<%= Html.escape(formValues.get("company")) %>" maxlength="150" placeholder="例: 佐藤不動産管理、宇都宮商事(株) など">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※屋号や法人名、物件の管理名義などをお持ちの場合はご入力ください（空欄でも可）。</span>
      </div>

      <%-- 画像の参照先は任意入力とし、エラー時も入力値を保持する。 --%>
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-owner-icon" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          アイコン画像のURL・パス <span style="font-size:0.78rem; color:var(--text-sub);">(任意)</span>
        </label>
        <input id="s18-owner-icon" name="iconImageUrl" type="text" class="form-control" value="<%= Html.escape(formValues.get("iconImageUrl")) %>" maxlength="2048" placeholder="例: https://example.com/icon.png、/assets/icon.png">
      </div>

      <!-- 連絡先電話番号 -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-owner-phone" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          連絡先電話番号 <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
        </label>
<input id="s18-owner-phone" name="phone" type="tel" class="form-control" value="<%= Html.escape(formValues.getOrDefault("phone", "")) %>" required placeholder="例: 09012345678 (ハイフンなし)">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※出店希望者との連絡、物件問い合わせや運営確認用に使用されます。</span>
      </div>

      <!-- メールアドレス -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label for="s18-owner-email" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
          メールアドレス <span style="font-size:0.78rem; color:var(--text-sub);">(変更可能)</span>
        </label>
        <input id="s18-owner-email" name="email" type="email" class="form-control" value="<%= Html.escape(email) %>" maxlength="254" placeholder="example@domain.com">
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINE認証で取得できた場合に反映しています。未取得の場合は空欄です。</span>
      </div>

      <!-- 貸したい業種・誘致希望ジャンル (複数選択可) -->
      <div class="form-group" style="margin-bottom:1.25rem;">
        <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
          貸したい業種・誘致希望ジャンル <span style="color:var(--danger-text); font-size:0.8rem;">*必須 (複数選択可)</span>
        </label>
        <div style="display:grid; grid-template-columns:repeat(auto-fill, minmax(130px, 1fr)); gap:0.4rem;">
          <%-- 表示名や配列位置ではなく、DBマスターのIDを保存用に送信する。 --%>
          <% for (RegistrationChoice choice : industryChoices) { %>
            <label style="display:flex; align-items:center; gap:0.4rem; padding:0.5rem 0.65rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.84rem;">
              <input type="checkbox" name="preferredIndustry" value="<%= Html.escape(choice.id()) %>" <%= selectedIndustries.contains(String.valueOf(choice.id())) ? "checked" : "" %> style="cursor:pointer; margin:0;">
              <span><%= Html.escape(choice.label()) %></span>
            </label>
          <% } %>
        </div>
        <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.35rem;">※所有物件や空き区画に誘致したい希望ジャンルを選択してください（需要マッチングやオファー時に活用されます）。</span>
      </div>

      <!-- 出店応募通知のメール受信設定 -->
      <label style="display:flex; align-items:center; gap:0.65rem; cursor:pointer; font-size:0.875rem; color:var(--text-main); font-weight:600; line-height:1.45; margin-top:1.5rem; padding:0.75rem 1rem; background:var(--bg-subtle); border-radius:var(--radius-md); border:1px solid var(--border-color); box-sizing:border-box; min-height:48px;">
        <input type="checkbox" id="s18-owner-email-notify" name="emailNotify" <%= !formSubmitted || formValues.containsKey("emailNotify") ? "checked" : "" %> style="cursor:pointer; width:18px; height:18px; margin:0; flex-shrink:0;">
        <span>所有物件への出店応募や事業主からの問い合わせ通知をメールで受け取る</span>
      </label>

      <!-- 送信ボタン -->
      <div style="margin-top:1.5rem;">
        <button type="submit" class="btn-primary" style="width:100%; min-height:48px; font-size:1.05rem; font-weight:800; display:flex; align-items:center; justify-content:center; gap:0.5rem; box-shadow:var(--shadow-floating); cursor:pointer;">
          <span><%= isOnboarding ? "登録を完了してマチリクを始める" : "設定を保存する" %></span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
      </div>
    </form>
    <div style="margin-top:1.5rem; text-align:center;">
      <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Logout.action" <%= isOnboarding ? "onclick=\"return confirm('登録を中断してログアウトしますか？');\"" : "onclick=\"return confirm('ログアウトしますか？');\"" %> style="color:var(--text-sub); font-size:0.875rem; text-decoration:underline; cursor:pointer;">
        <%= isOnboarding ? "登録を中断してログアウト" : "ログアウト" %>
      </a>
    </div>
    <%-- 本登録前は、利用者区分の選択画面へ戻って登録を続けられる。 --%>
    <% User registrationUser = session.getAttribute("user") instanceof User ? (User) session.getAttribute("user") : null;
       if (registrationUser != null && "OWNER".equals(registrationUser.getUserRole()) && "PENDING".equals(registrationUser.getAccountStatus())
        && registrationUser.getWithdrawnAt() == null) { %>
      <div style="text-align:center; margin-top:1.25rem; font-size:0.875rem;">
        <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Register.action" style="color:var(--primary-700); font-weight:700; text-decoration:underline;">利用者区分を選び直す</a>
      </div>
    <% } %>
  </div>
</section>

<script>
  function validateOwnerForm() {
    const repName = document.getElementById('s18-owner-name');
    const phone = document.getElementById('s18-owner-phone');

    if (!repName || !repName.value.trim()) {
      alert('オーナー氏名・代表者名を入力してください');
      if (repName) repName.focus();
      return false;
    }
    if (!phone || !phone.value.trim()) {
      alert('連絡先電話番号を入力してください');
      if (phone) phone.focus();
      return false;
    }

    const industries = document.querySelectorAll('input[name="preferredIndustry"]:checked');
    if (industries.length === 0) {
      alert('誘致希望業種を少なくとも1つ選択してください');
      return false;
    }

    return true;
  }
</script>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
