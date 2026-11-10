<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="jp.machiriku.bean.User,jp.machiriku.tool.Html" %>
<%
  User currentUser = session.getAttribute("user") instanceof User ? (User) session.getAttribute("user") : null;
  String displayName = (String) session.getAttribute("userName");
  if (displayName == null && currentUser != null) {
    displayName = currentUser.getDisplayName();
  }
  if (displayName == null || displayName.trim().isEmpty()) {
    displayName = "LINEユーザー";
  }
  String userRole = (String) session.getAttribute("userRole");
  if (userRole == null && currentUser != null && currentUser.getUserRole() != null) {
    userRole = currentUser.getUserRole().toLowerCase(java.util.Locale.ROOT);
  }
  if (userRole == null || userRole.trim().isEmpty()) {
    userRole = "resident";
  }
%>
<!DOCTYPE html>
<html lang="ja">

<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="theme-color" content="#1b4332">
  <title>マチリク | 宇都宮の空き店舗と、欲しいお店</title>
  <meta name="description" content="宇都宮市の空き店舗を探し、欲しいお店に投票。地域の声を出店につなげるマチリク。">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/main.css?v=20261110_db_align_v1">
</head>

<body data-context-path="${pageContext.request.contextPath}">
  <a class="skip-link" href="#main-app-container">本文へ移動</a>
  <header class="site-header">
    <div class="header-top">
      <a class="brand-area" href="#s01" aria-label="マチリク ホーム">
        <span class="brand-logo-badge">マチリク</span>
        <span class="brand-titles">
          <span class="brand-name">宇都宮市</span>
        </span>
      </a>
      <div class="header-nav-right">
        <% if (currentUser == null) { %>
          <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Login.action" class="btn-header-action profile-link">
            <span>ログイン</span>
          </a>
        <% } else { %>
          <label class="role-picker" id="header-role-picker-wrap" for="preview-role">利用者
            <select id="preview-role" onchange="MachirikuStore.setRole(this.value)">
              <option value="resident" <%= "resident".equalsIgnoreCase(userRole) ? "selected" : "" %>>地域住民</option>
              <option value="business" <%= "business".equalsIgnoreCase(userRole) ? "selected" : "" %>>出店事業主</option>
              <option value="owner" <%= "owner".equalsIgnoreCase(userRole) ? "selected" : "" %>>物件オーナー</option>
              <option value="admin" <%= "admin".equalsIgnoreCase(userRole) ? "selected" : "" %>>運営管理者</option>
            </select>
          </label>
          <span id="header-role-badge" class="header-role-badge" style="display:none;">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <span id="header-role-badge-text">地域住民</span>
          </span>
          <a href="#s06" class="btn-header-action notification-link" aria-label="通知一覧">
            <svg class="m-icon" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2"
              stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
            </svg>
            <span id="header-notif-badge" class="badge-counter" hidden>0</span>
          </a>
          <a href="#s18" class="btn-header-action profile-link"><span id="header-user-name"><%= Html.escape(displayName) %></span></a>
        <% } %>
      </div>
    </div>
  </header>
  <nav class="header-subnav" aria-label="メインメニュー">
    <div class="subnav-links">
      <a href="#s01" class="subnav-link is-active" aria-current="page">物件を探す</a>
      <a href="#s04" class="subnav-link">フォロー</a>
      <a href="#s05" class="subnav-link">開店のようす</a>
      <a href="#s30" class="subnav-link">クーポン</a>
    </div>
  </nav>
  <main class="main-content" id="main-app-container" tabindex="-1"></main>
  <footer class="site-footer">
    <div class="footer-content">
      <div><strong>マチリク 宇都宮</strong>
        <p>欲しいお店の声を、街の次の一軒へ。</p>
      </div>
      <nav class="footer-links" aria-label="関連メニュー">
        <a href="#s01">空き店舗を探す</a>
        <a href="#s21">開店の相談</a>
        <% if (currentUser == null) { %>
        <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Login.action">ログイン</a>
        <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Register.action">新規会員登録</a>
        <% } else { %>
        <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Logout.action" onclick="return confirm('ログアウトしますか？');">ログアウト</a>
        <% } %>
      </nav>
    </div>
    <details class="prototype-tools">
      <summary>画面設計の確認</summary>
      <div class="prototype-tools-content">
        <p>物件情報は提供されたLIFULL HOME'S掲載データです。投票・応募・送信・予約の保存は画面設計用の試行です。</p>
        <a href="#s20">全33画面・仕様対応表</a>
        <button type="button" class="btn-secondary"
          onclick="MachirikuStore.resetState(); MachirikuApp.navigate('s01')">初期状態に戻す</button>
        <span id="storage-status" role="status"></span>
      </div>
    </details>
    <small>© 2026 マチリク / Fチーム</small>
  </footer>
  <script
    src="https://maps.googleapis.com/maps/api/js?key=AIzaSyCNWrNbVnBfZsjuTJ3sHCKDemfkxNsIv7w&libraries=marker"></script>
  <script src="${pageContext.request.contextPath}/js/qrcode.js"></script>
  <script src="${pageContext.request.contextPath}/js/data.js?v=20261110_db_align_v1"></script>
  <script src="${pageContext.request.contextPath}/js/map.js?v=20261110_db_align_v1"></script>
  <script src="${pageContext.request.contextPath}/js/store.js?v=20261110_db_align_v1"></script>
  <script src="${pageContext.request.contextPath}/js/screens.js?v=20261110_db_align_v1"></script>
  <script src="${pageContext.request.contextPath}/js/app.js?v=20261110_db_align_v1"></script>
  <script>
    window.__MACHIRIKU_CONTEXT_PATH__ = "${pageContext.request.contextPath}";
    document.addEventListener('DOMContentLoaded', function () {
      <% if (currentUser == null) { %>
        // 未ログイン時はブラウザ側に残存するキャッシュを破棄し初期状態にリセット
        try {
          if (window.sessionStorage) {
            window.sessionStorage.removeItem('machiriku_user_display_name');
            window.sessionStorage.removeItem('machiriku_onboarding');
          }
          if (window.localStorage) {
            const keysToRemove = [];
            for (let i = 0; i < window.localStorage.length; i++) {
              const k = window.localStorage.key(i);
              if (k && k.startsWith('machiriku')) {
                keysToRemove.push(k);
              }
            }
            keysToRemove.forEach(k => window.localStorage.removeItem(k));
          }
          if (window.MachirikuStore && typeof window.MachirikuStore.resetState === 'function') {
            window.MachirikuStore.resetState();
          }
        } catch (_) {}
      <% } else { %>
        <% if (!"resident".equals(userRole)) { %>
          if (window.MachirikuStore && typeof MachirikuStore.setRole === 'function') {
            MachirikuStore.setRole("<%= Html.escape(userRole) %>");
          }
        <% } %>
        <% if (displayName != null && !displayName.trim().isEmpty() && !"LINEユーザー".equals(displayName)) { %>
          if (window.MachirikuStore && typeof MachirikuStore.updateUserProfile === 'function') {
            MachirikuStore.updateUserProfile({ name: "<%= Html.escape(displayName) %>" });
          }
          try {
            if (window.sessionStorage) {
              window.sessionStorage.setItem('machiriku_user_display_name', "<%= Html.escape(displayName) %>");
            }
          } catch (_) {}
        <% } %>
      <% } %>

      // すでにサーブレット版が実装済みの画面へのアクセスは本番サーブレットへリダイレクト
      var ctx = "${pageContext.request.contextPath}";
      function checkServletRedirect() {
        var h = (window.location.hash || '').toLowerCase();
        if (h === '#s16') {
          window.location.href = ctx + "/jp/machiriku/servlet/auth/Login.action";
          return true;
        } else if (h === '#s17') {
          window.location.href = ctx + "/jp/machiriku/servlet/auth/Register.action";
          return true;
        }
        <% if (currentUser == null) { %>
        else if (h === '#s18' || h.startsWith('#s18?') || h.startsWith('#s18&')) {
          window.location.href = ctx + "/jp/machiriku/servlet/auth/Login.action";
          return true;
        }
        <% } %>
        return false;
      }

      window.addEventListener('hashchange', checkServletRedirect);
      if (!checkServletRedirect()) {
        MachirikuApp.init();
      }
    });
  </script>
</body>

</html>
