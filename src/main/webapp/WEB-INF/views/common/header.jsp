<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
  boolean isOnboarding = "true".equalsIgnoreCase(request.getParameter("isOnboarding"));
  String roleLabel = request.getParameter("roleLabel") != null ? request.getParameter("roleLabel") : "地域住民";
  String userName = request.getParameter("userName") != null ? request.getParameter("userName") : "LINEユーザー";
%>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="theme-color" content="#1b4332">
  <title><%= request.getParameter("title") != null ? request.getParameter("title") : "マチリク | 宇都宮の空き店舗と、欲しいお店" %></title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/main.css">
</head>
<body>
  <a class="skip-link" href="#main-app-container">本文へ移動</a>
  <header class="site-header">
    <div class="header-top">
      <a class="brand-area" href="<%= isOnboarding ? "javascript:void(0)" : (request.getContextPath() + "/index.jsp") %>" aria-label="マチリク ホーム" <%= isOnboarding ? "style=\"cursor:default;\" onclick=\"return false;\"" : "" %>>
        <span class="brand-logo-badge">マチリク</span>
        <span class="brand-titles">
          <span class="brand-name">宇都宮市</span>
        </span>
      </a>
      <div class="header-nav-right">
        <% if (isOnboarding) { %>
          <span id="header-role-badge" class="header-role-badge">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <span id="header-role-badge-text"><%= roleLabel %></span>
          </span>
          <span class="btn-header-action profile-link" style="cursor:default;"><span id="header-user-name"><%= userName %></span></span>
        <% } else { %>
          <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Login.action" class="btn-header-action profile-link">
            <span>ログイン</span>
          </a>
        <% } %>
      </div>
    </div>
  </header>
  <% if (!isOnboarding) { %>
  <nav class="header-subnav" aria-label="メインメニュー">
    <div class="subnav-links">
      <a href="${pageContext.request.contextPath}/index.jsp" class="subnav-link">ホーム</a>
      <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Login.action" class="subnav-link <%= "login".equals(request.getParameter("activeNav")) ? "is-active" : "" %>">ログイン</a>
      <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Register.action" class="subnav-link <%= "register".equals(request.getParameter("activeNav")) ? "is-active" : "" %>">新規会員登録</a>
    </div>
  </nav>
  <% } %>
  <main class="main-content" id="main-app-container" tabindex="-1">
