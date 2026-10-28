<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/header.jsp">
  <jsp:param name="title" value="マチリク | 宇都宮の空き店舗と、欲しいお店" />
</jsp:include>

<section class="screen-view" style="max-width:800px; margin:2rem auto; padding:0 1rem;">
  <div style="background:var(--bg-card); padding:2.5rem; border-radius:var(--radius-lg); border:1px solid var(--border-color); box-shadow:var(--shadow-sm); text-align:center;">
    <h1 style="color:var(--primary-800); font-size:1.75rem; font-weight:800; margin-bottom:0.75rem;">マチリク 開発ポータル</h1>
    <p style="color:var(--text-sub); font-size:1rem; margin-bottom:2rem; line-height:1.6;">
      宇都宮市の空き店舗情報と、地域の住民需要をつなぐプラットフォームです。<br>
      バックエンド（JSP / Servlet）連携の動作確認用ポータルです。
    </p>

    <div style="display:flex; justify-content:center; gap:1rem; flex-wrap:wrap;">
      <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Login.action" class="btn-primary" style="display:inline-block; padding:0.75rem 1.75rem; font-weight:700; border-radius:var(--radius-md); text-decoration:none; background:var(--primary-700); color:#ffffff;">
        ログイン画面へ (S16)
      </a>
      <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Register.action" class="btn-secondary" style="display:inline-block; padding:0.75rem 1.75rem; font-weight:700; border-radius:var(--radius-md); text-decoration:none; border:1px solid var(--border-color); background:transparent; color:var(--text-main);">
        新規会員登録画面へ (S17)
      </a>
      <a href="${pageContext.request.contextPath}/db-check" class="btn-outline" style="display:inline-block; padding:0.75rem 1.75rem; font-weight:700; border-radius:var(--radius-md); text-decoration:none; border:1px solid var(--border-color); background:transparent; color:var(--text-sub);">
        DB接続確認 (PostgreSQL)
      </a>
    </div>

    <div style="margin-top:2.5rem; padding-top:1.5rem; border-top:1px dashed var(--border-color); font-size:0.875rem; color:var(--text-muted);">
      現在、<code>feature/login</code> ブランチにて認証周りのJSPおよびCSS基盤を統合中です。
    </div>
  </div>
</section>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
