<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="jp.machiriku.bean.User" %>
<%-- 各画面からincludeする共通ヘッダー。画面名・初期設定中の表示・ログイン状態を受け取る。 --%>
<%!
  // 表示名や利用区分などの文字列をHTML向けに置換し、タグとして解釈されることを防ぐ。
  private String escapeHtml(String value) {
    return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
  }
%>
<%
  // include元の指定を優先し、指定がなければセッションの会員情報や既定値を使う。
  boolean isOnboarding = "true".equalsIgnoreCase(request.getParameter("isOnboarding"));
  String roleLabel = request.getParameter("roleLabel") != null ? request.getParameter("roleLabel") : "地域住民";
  String userName = request.getParameter("userName") != null ? request.getParameter("userName") : "LINEユーザー";
  User currentUser = session.getAttribute("user") instanceof User ? (User) session.getAttribute("user") : null;
  if (currentUser != null && (request.getParameter("userName") == null)) {
    userName = currentUser.getDisplayName();
  }
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
        <%-- 初期設定中は利用区分と編集中の名前を、通常時はログイン状態を表示する。 --%>
        <% if (isOnboarding) { %>
          <span id="header-role-badge" class="header-role-badge">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <span id="header-role-badge-text"><%= escapeHtml(roleLabel) %></span>
          </span>
          <span class="btn-header-action profile-link" style="cursor:default;"><span id="header-user-name"><%= escapeHtml(userName) %></span></span>
        <% } else if (currentUser != null) { %>
          <a href="${pageContext.request.contextPath}/index.jsp#s18" class="btn-header-action profile-link">
            <span><%= escapeHtml(currentUser.getDisplayName()) %> でログイン中</span>
          </a>
        <% } else { %>
          <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Login.action" class="btn-header-action profile-link">
            <span>ログイン</span>
          </a>
        <% } %>
      </div>
    </div>
  </header>
  <%-- 初期設定を進めている間は、通常ページ用の下段ナビゲーションを表示しない。 --%>
  <% if (!isOnboarding) { %>
  <nav class="header-subnav" aria-label="メインメニュー">
    <div class="subnav-links">
      <a href="${pageContext.request.contextPath}/index.jsp" class="subnav-link">ホーム</a>
      <% if (currentUser == null) { %>
      <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Login.action" class="subnav-link <%= "login".equals(request.getParameter("activeNav")) ? "is-active" : "" %>">ログイン</a>
      <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Register.action" class="subnav-link <%= "register".equals(request.getParameter("activeNav")) ? "is-active" : "" %>">新規会員登録</a>
      <% } else { %>
      <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Logout.action" onclick="return confirm('ログアウトしますか？');" class="subnav-link">ログアウト</a>
      <% } %>
    </div>
  </nav>
  <% } %>
  <main class="main-content" id="main-app-container" tabindex="-1">
