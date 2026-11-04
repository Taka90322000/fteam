<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/header.jsp">
  <jsp:param name="title" value="ログイン | マチリク" />
  <jsp:param name="activeNav" value="login" />
</jsp:include>

<section class="screen-view s16-view" data-screen="S16">
  <div class="screen-header-banner" style="background:var(--primary-50); border:1px solid var(--primary-100); border-radius:var(--radius-lg); padding:1rem 1.25rem; margin-bottom:1.5rem; display:flex; align-items:center; gap:1rem;">
    <span class="screen-header-badge" style="background:var(--primary-700); color:var(--text-inverse); font-weight:800; font-size:0.85rem; padding:0.25rem 0.6rem; border-radius:var(--radius-full);">S16</span>
    <div>
      <h1 class="screen-header-title" style="font-size:1.25rem; font-weight:800; color:var(--primary-900); margin:0;">ログイン</h1>
      <p class="screen-header-desc" style="font-size:0.85rem; color:var(--text-sub); margin:0.25rem 0 0 0;">登録済みアカウントでログインします。LINE公式アカウント連携でスムーズにログインできます。</p>
    </div>
  </div>

  <div class="form-stepper-box" style="max-width:540px; margin:0 auto; background:var(--bg-card); padding:2rem; border-radius:var(--radius-lg); border:1px solid var(--border-color); box-shadow:var(--shadow-sm);">
    
    <% if (request.getAttribute("errorMessage") != null) { %>
      <div style="background:#fee2e2; border:1px solid #f87171; color:#991b1b; padding:0.75rem 1rem; border-radius:var(--radius-md); font-size:0.875rem; margin-bottom:1.25rem;">
        <%= request.getAttribute("errorMessage") %>
      </div>
    <% } %>

    <div style="margin-top:0.5rem; margin-bottom:1.5rem;">
      <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/LineLogin.action" class="btn-line-login" style="display:flex; align-items:center; justify-content:center; gap:0.6rem; background:#06C755; color:#ffffff; font-weight:700; font-size:1rem; padding:0.85rem 1.25rem; border-radius:var(--radius-md); text-decoration:none; border:none; width:100%; box-sizing:border-box;">
        <img src="${pageContext.request.contextPath}/assets/line_icon.svg" alt="LINE" style="width:24px; height:24px; flex-shrink:0;">
        <span>LINEでログイン</span>
      </a>
      <p style="font-size:0.8125rem; color:var(--text-sub); text-align:center; margin-top:0.75rem; line-height:1.5;">
        LINEアカウントを使って簡単・安全にログインできます。
      </p>
    </div>

    <div style="text-align:center; margin-top:1.5rem; font-size:0.875rem;">
      アカウントをお持ちでない方は <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Register.action" style="color:var(--primary-700); font-weight:700; text-decoration:underline;">新規会員登録</a>
    </div>

    <div style="margin-top:2rem; padding-top:1.5rem; border-top:1px dashed var(--border-color); text-align:center;">
      <div style="font-size:0.8125rem; color:var(--text-sub); margin-bottom:0.75rem; font-weight:700;">
        【デモ・審査確認用】ロール別 1クリックログイン
      </div>
      <form action="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Login.action" method="POST">
        <div style="display:grid; grid-template-columns:repeat(2, 1fr); gap:0.5rem;">
          <button type="submit" name="demoRole" value="resident" class="btn-outline" style="font-size:0.8125rem; padding:0.5rem 0.75rem; border:1px solid var(--border-color); background:transparent; border-radius:var(--radius-md); cursor:pointer;">地域住民でログイン</button>
          <button type="submit" name="demoRole" value="business" class="btn-outline" style="font-size:0.8125rem; padding:0.5rem 0.75rem; border:1px solid var(--border-color); background:transparent; border-radius:var(--radius-md); cursor:pointer;">事業主でログイン</button>
          <button type="submit" name="demoRole" value="owner" class="btn-outline" style="font-size:0.8125rem; padding:0.5rem 0.75rem; border:1px solid var(--border-color); background:transparent; border-radius:var(--radius-md); cursor:pointer;">オーナーでログイン</button>
          <button type="submit" name="demoRole" value="admin" class="btn-outline" style="font-size:0.8125rem; padding:0.5rem 0.75rem; border:1px solid var(--border-color); background:transparent; border-radius:var(--radius-md); cursor:pointer;">管理者でログイン</button>
        </div>
      </form>
    </div>

  </div>
</section>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
