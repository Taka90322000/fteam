<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="jp.machiriku.tool.Html" %>
<%
  String errorMessage = request.getAttribute("errorMessage") instanceof String
      ? (String) request.getAttribute("errorMessage") : null;
  boolean restoreDraft = Boolean.TRUE.equals(request.getAttribute("restoreDraft"));
%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="物件登録 | マチリク" />
  <jsp:param name="description" value="新しい空き店舗の物件情報・設備・契約条件を入力して確認画面へ進みます。" />
  <jsp:param name="pageScreenId" value="o04" />
  <jsp:param name="assetVersion" value="20261125_prop_db_v1" />
</jsp:include>

    <section id="o04-section" class="screen-view s12-view" data-screen="O04"<%= restoreDraft ? " data-restore-draft=\"1\"" : "" %>>
      <div class="page-header-banner" data-screen="O04">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理</a>
          <span aria-hidden="true">/</span>
          <span aria-current="page">物件登録</span>
        </nav>
        <div class="page-title-row">
          <h1 id="o04-form-heading" class="page-title">物件登録</h1>
          <span class="badge-status status-voting">オーナー</span>
        </div>
      </div>

      <div id="o04-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <button type="button" id="o04-switch-role-btn" class="btn-primary btn-sm">物件オーナーに切り替える</button>
      </div>

      <% if (errorMessage != null && !errorMessage.isBlank()) { %>
      <div role="alert" style="background:#fee2e2; color:#991b1b; padding:0.85rem 1rem; border-radius:var(--radius-md); margin-bottom:1.25rem; border:1px solid #fecaca;">
        <%= Html.escape(errorMessage) %>
      </div>
      <% } %>

      <div class="form-stepper-box form-stepper-wide">
        <jsp:include page="/WEB-INF/views/property/_property_form.jsp">
          <jsp:param name="isEdit" value="false" />
          <jsp:param name="headingId" value="o04-form-heading" />
        </jsp:include>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="o04" />
  <jsp:param name="assetVersion" value="20261125_prop_db_v1" />
</jsp:include>
