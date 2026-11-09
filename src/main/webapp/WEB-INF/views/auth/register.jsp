<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="jp.machiriku.tool.Html" %>
<%-- 会員登録の入口画面。利用区分を受け取り、LINE認証へ進む。 --%>
<%
  boolean registrationPending = Boolean.TRUE.equals(request.getAttribute("registrationPending"));
  String registrationRole = String.valueOf(request.getAttribute("registrationRole"));
  if (!java.util.Set.of("resident", "business", "owner").contains(registrationRole)) registrationRole = "resident";
%>
<jsp:include page="/WEB-INF/views/common/header.jsp">
  <jsp:param name="title" value="新規会員登録 | マチリク" />
  <jsp:param name="activeNav" value="register" />
</jsp:include>

<section class="screen-view s17-view" data-screen="S17">
  <div class="screen-header-banner" style="background:var(--primary-50); border:1px solid var(--primary-100); border-radius:var(--radius-lg); padding:1rem 1.25rem; margin-bottom:1.5rem; display:flex; align-items:center; gap:1rem;">
    <span class="screen-header-badge" style="background:var(--primary-700); color:var(--text-inverse); font-weight:800; font-size:0.85rem; padding:0.25rem 0.6rem; border-radius:var(--radius-full);">S17</span>
    <div>
      <h1 class="screen-header-title" style="font-size:1.25rem; font-weight:800; color:var(--primary-900); margin:0;">種別別 会員登録</h1>
      <p class="screen-header-desc" style="font-size:0.85rem; color:var(--text-sub); margin:0.25rem 0 0 0;"><%= registrationPending ? "本登録が終わる前は、利用者区分を選び直せます。" : "利用者区分（地域住民・出店事業主・物件オーナー）を選択し、LINE公式アカウント連携でスムーズに登録・ログインできます。" %></p>
    </div>
  </div>

  <div class="form-stepper-box" style="max-width:540px; margin:0 auto; background:var(--bg-card); padding:2rem; border-radius:var(--radius-lg); border:1px solid var(--border-color); box-shadow:var(--shadow-sm);">
    
    <%-- 認証開始時や登録処理でActionから戻された案内文を表示する。 --%>
    <% if (request.getAttribute("errorMessage") != null) { %>
      <div style="background:#fee2e2; border:1px solid #f87171; color:#991b1b; padding:0.75rem 1rem; border-radius:var(--radius-md); font-size:0.875rem; margin-bottom:1.25rem;">
        <%= Html.escape(request.getAttribute("errorMessage")) %>
      </div>
    <% } %>

    <%-- 登録開始情報はPOSTで送信する。電話番号は認証後のプロフィール画面で受け付ける。 --%>
    <form id="registerForm" action="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/LineLogin.action" method="POST">
      <%-- 本登録前の区分変更を、同じブラウザーの登録フォームからだけ受け付ける。 --%>
      <input type="hidden" name="csrfToken" value="<%= Html.escape(request.getAttribute("csrfToken")) %>">
      <div class="form-group" style="margin-bottom:1.5rem;">
        <label style="font-size:0.9rem; font-weight:800; display:block; margin-bottom:0.5rem; color:var(--text-main);">
          利用区分を選択してください <span style="color:#ef4444; font-size:0.8rem;">*必須</span>
        </label>
        <div style="display:grid; grid-template-columns:repeat(3, 1fr); gap:0.75rem;">
          <label style="border:1px solid var(--border-color); padding:0.85rem 0.5rem; border-radius:var(--radius-md); text-align:center; cursor:pointer; background:var(--bg-card); display:block;">
            <input type="radio" name="regRole" value="resident" <%= "resident".equals(registrationRole) ? "checked" : "" %> style="accent-color:var(--primary-700);">
            <div style="font-weight:700; margin-top:0.35rem; font-size:0.875rem;">地域住民</div>
          </label>
          <label style="border:1px solid var(--border-color); padding:0.85rem 0.5rem; border-radius:var(--radius-md); text-align:center; cursor:pointer; background:var(--bg-card); display:block;">
            <input type="radio" name="regRole" value="business" <%= "business".equals(registrationRole) ? "checked" : "" %> style="accent-color:var(--primary-700);">
            <div style="font-weight:700; margin-top:0.35rem; font-size:0.875rem;">出店希望事業主</div>
          </label>
          <label style="border:1px solid var(--border-color); padding:0.85rem 0.5rem; border-radius:var(--radius-md); text-align:center; cursor:pointer; background:var(--bg-card); display:block;">
            <input type="radio" name="regRole" value="owner" <%= "owner".equals(registrationRole) ? "checked" : "" %> style="accent-color:var(--primary-700);">
            <div style="font-weight:700; margin-top:0.35rem; font-size:0.875rem;">物件オーナー</div>
          </label>
        </div>
      </div>

      <div style="margin-top:1.5rem;">
        <button type="submit" class="btn-line-login" style="display:flex; align-items:center; justify-content:center; gap:0.6rem; background:#06C755; color:#ffffff; font-weight:700; font-size:1rem; padding:0.85rem 1.25rem; border-radius:var(--radius-md); border:none; width:100%; cursor:pointer;">
          <img src="${pageContext.request.contextPath}/assets/line_icon.svg" alt="LINE" style="width:24px; height:24px; flex-shrink:0;">
          <span><%= registrationPending ? "この利用者区分で登録を続ける" : "LINEで登録・ログイン" %></span>
        </button>
        <p style="font-size:0.8125rem; color:var(--text-sub); text-align:center; margin-top:0.75rem; line-height:1.5;">
          本アプリでは、登録時にLINEに登録されているメールアドレスを、取得できる場合に利用します。取得できない場合も登録できます。<br>
          取得したメールアドレスは、本人確認・サポート対応の目的でのみ利用し、第三者への提供は行いません。<br>
          <span style="font-size:0.75rem; color:var(--text-muted);">※利用規約・プライバシーポリシーに同意の上、次へお進みください。</span>
        </p>
      </div>
    </form>

    <div style="text-align:center; margin-top:1.5rem; font-size:0.875rem;">
      既にアカウントをお持ちの方は <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Login.action" style="color:var(--primary-700); font-weight:700; text-decoration:underline;">ログイン</a>
    </div>

  </div>
</section>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
