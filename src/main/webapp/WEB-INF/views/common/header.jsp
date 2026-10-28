<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="jp.machiriku.bean.User" %>
<%!
  private String escapeHtml(String value) {
    return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
  }
%>
<% User currentUser = session.getAttribute("user") instanceof User ? (User) session.getAttribute("user") : null; %>
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
      <a class="brand-area" href="${pageContext.request.contextPath}/index.jsp" aria-label="マチリク ホーム">
        <span class="brand-logo-badge">マチリク</span>
        <span class="brand-titles">
          <span class="brand-name">宇都宮市</span>
        </span>
      </a>
      <div class="header-nav-right">
        <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Login.action" class="btn-header-action profile-link">
          <span><%= currentUser == null ? "ログイン" : escapeHtml(currentUser.getDisplayName()) + " でログイン中" %></span>
        </a>
      </div>
    </div>
  </header>
  <nav class="header-subnav" aria-label="メインメニュー">
    <div class="subnav-links">
      <a href="${pageContext.request.contextPath}/index.jsp" class="subnav-link">ホーム</a>
      <% if (currentUser == null) { %>
      <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Login.action" class="subnav-link <%= "login".equals(request.getParameter("activeNav")) ? "is-active" : "" %>">ログイン</a>
      <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Register.action" class="subnav-link <%= "register".equals(request.getParameter("activeNav")) ? "is-active" : "" %>">新規会員登録</a>
      <% } %>
    </div>
  </nav>
  <main class="main-content" id="main-app-container" tabindex="-1">
