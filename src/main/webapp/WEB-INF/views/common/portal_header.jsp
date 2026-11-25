<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="jp.machiriku.bean.User,jp.machiriku.servlet.auth.LogoutAction,jp.machiriku.tool.Html" %>
<%
  User currentUser = session.getAttribute("user") instanceof User ? (User) session.getAttribute("user") : null;
  String rawSessionRole = session.getAttribute("userRole") instanceof String ? (String) session.getAttribute("userRole") : null;
  boolean isDemoSession = currentUser == null && rawSessionRole != null
      && java.util.Set.of("resident", "business", "owner", "admin").contains(rawSessionRole);
  String logoutCsrfToken = LogoutAction.csrfToken(session);

  String displayName = (String) session.getAttribute("userName");
  if (displayName == null && currentUser != null) {
    displayName = currentUser.getDisplayName();
  }
  if (displayName == null || displayName.trim().isEmpty()) {
    displayName = "LINEユーザー";
  }
  String userRole = rawSessionRole;
  if (userRole == null && currentUser != null && currentUser.getUserRole() != null) {
    userRole = currentUser.getUserRole().toLowerCase(java.util.Locale.ROOT);
  }
  if (userRole == null || userRole.trim().isEmpty()) {
    userRole = "resident";
  }

  String pageTitle = request.getParameter("title");
  if (pageTitle == null || pageTitle.trim().isEmpty()) {
    pageTitle = "マチリク | 宇都宮の空き店舗と、欲しいお店";
  }
  String pageDescription = request.getParameter("description");
  if (pageDescription == null || pageDescription.trim().isEmpty()) {
    pageDescription = "宇都宮市の空き店舗を探し、欲しいお店に投票。地域の声を出店につなげるマチリク。";
  }
  String pageScreenId = request.getParameter("pageScreenId");
  if (pageScreenId == null) {
    pageScreenId = "";
  }
  String assetVersion = request.getParameter("assetVersion");
  if (assetVersion == null || assetVersion.trim().isEmpty()) {
    assetVersion = "20261125_matching_v2";
  }
  String profileActionUrl = request.getContextPath() + "/jp/machiriku/servlet/user/Profile.action";
%>
<!DOCTYPE html>
<html lang="ja">

<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="theme-color" content="#1b4332">
  <title><%= Html.escape(pageTitle) %></title>
  <meta name="description" content="<%= Html.escape(pageDescription) %>">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/main.css?v=<%= Html.escape(assetVersion) %>">
</head>

<body data-context-path="${pageContext.request.contextPath}"<%= !pageScreenId.isEmpty() ? " data-screen-page=\"" + Html.escape(pageScreenId) + "\"" : "" %>>
  <div id="page-top-sentinel" class="scroll-sentinel" aria-hidden="true"></div>
  <a class="skip-link" href="#main-app-container">本文へ移動</a>
  <header class="site-header">
    <div class="header-top">
      <a class="brand-area" href="${pageContext.request.contextPath}/index.jsp#s01" aria-label="マチリク ホーム">
        <span class="brand-logo-badge">マチリク</span>
        <span class="brand-titles">
          <span class="brand-name">宇都宮市</span>
        </span>
      </a>
      <div class="header-nav-right">
        <% if (currentUser == null) { %>
          <label class="role-picker" id="header-role-picker-wrap" for="preview-role">デモ表示
            <select id="preview-role" onchange="MachirikuStore.setRole(this.value)">
              <option value="resident" <%= "resident".equalsIgnoreCase(userRole) ? "selected" : "" %>>地域住民</option>
              <option value="business" <%= "business".equalsIgnoreCase(userRole) ? "selected" : "" %>>出店事業主</option>
              <option value="owner" <%= "owner".equalsIgnoreCase(userRole) ? "selected" : "" %>>物件オーナー</option>
              <option value="admin" <%= "admin".equalsIgnoreCase(userRole) ? "selected" : "" %>>運営管理者</option>
            </select>
          </label>
          <span id="header-role-badge" class="header-role-badge" style="display:none;">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <span id="header-role-badge-text">地域住民</span>
          </span>
          <% if (isDemoSession) { %>
            <form action="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Logout.action" method="POST" style="display:inline; margin:0;">
              <input type="hidden" name="csrfToken" value="<%= Html.escape(logoutCsrfToken) %>">
              <input type="hidden" name="next" value="login">
              <button type="submit" class="btn-header-action profile-link" style="border:none; cursor:pointer; font:inherit;">
                <span>ログイン</span>
              </button>
            </form>
            <form action="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Logout.action" method="POST" style="display:inline; margin:0;" onsubmit="return confirm('デモ表示を終了してログアウトしますか？');">
              <input type="hidden" name="csrfToken" value="<%= Html.escape(logoutCsrfToken) %>">
              <button type="submit" class="btn-header-action" style="border:none; cursor:pointer; font:inherit;">
                <span>デモ終了</span>
              </button>
            </form>
          <% } else { %>
            <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Login.action" class="btn-header-action profile-link">
              <span>ログイン</span>
            </a>
          <% } %>
        <% } else { %>
          <label class="role-picker" id="header-role-picker-wrap" for="preview-role" style="display:none;">利用者
            <select id="preview-role" onchange="MachirikuStore.setRole(this.value)">
              <option value="resident" <%= "resident".equalsIgnoreCase(userRole) ? "selected" : "" %>>地域住民</option>
              <option value="business" <%= "business".equalsIgnoreCase(userRole) ? "selected" : "" %>>出店事業主</option>
              <option value="owner" <%= "owner".equalsIgnoreCase(userRole) ? "selected" : "" %>>物件オーナー</option>
              <option value="admin" <%= "admin".equalsIgnoreCase(userRole) ? "selected" : "" %>>運営管理者</option>
            </select>
          </label>
          <span id="header-role-badge" class="header-role-badge">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <span id="header-role-badge-text"><%= "business".equalsIgnoreCase(userRole) ? "出店事業主" : ("owner".equalsIgnoreCase(userRole) ? "物件オーナー" : ("admin".equalsIgnoreCase(userRole) ? "運営管理者" : "地域住民")) %></span>
          </span>
          <a href="${pageContext.request.contextPath}/index.jsp#s06" class="btn-header-action notification-link" aria-label="通知一覧">
            <svg class="m-icon" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2"
              stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
            </svg>
            <span id="header-notif-badge" class="badge-counter" hidden>0</span>
          </a>
          <a href="<%= profileActionUrl %>" class="btn-header-action profile-link" aria-label="アカウント設定"><span id="header-user-name"><%= Html.escape(displayName) %></span></a>
        <% } %>
      </div>
    </div>
  </header>
  <nav class="header-subnav" aria-label="メインメニュー">
    <div class="subnav-links">
      <a href="${pageContext.request.contextPath}/index.jsp#s01" class="subnav-link<%= pageScreenId.isEmpty() ? " is-active" : "" %>"<%= pageScreenId.isEmpty() ? " aria-current=\"page\"" : "" %>>物件を探す</a>
      <a href="${pageContext.request.contextPath}/index.jsp#s04" class="subnav-link">フォロー</a>
      <a href="${pageContext.request.contextPath}/matching/progress.jsp" class="subnav-link<%= "s05".equalsIgnoreCase(pageScreenId) ? " is-active" : "" %>"<%= "s05".equalsIgnoreCase(pageScreenId) ? " aria-current=\"page\"" : "" %>>開店のようす</a>
      <a href="${pageContext.request.contextPath}/index.jsp#s30" class="subnav-link">クーポン</a>
    </div>
  </nav>
  <main class="main-content" id="main-app-container" tabindex="-1">
