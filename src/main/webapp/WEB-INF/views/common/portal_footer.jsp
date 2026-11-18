<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="jp.machiriku.bean.User,jp.machiriku.tool.Html,com.fasterxml.jackson.databind.ObjectMapper" %>
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
  String userEmail = (String) session.getAttribute("userEmail");
  if (userEmail == null && currentUser != null) {
    userEmail = currentUser.getEmail();
  }
  if (userEmail == null) {
    userEmail = "";
  }
  String sessionBusinessName = (String) session.getAttribute("businessName");
  if (sessionBusinessName == null) {
    sessionBusinessName = "";
  }

  boolean isLoggedOut = "1".equals(request.getParameter("logged_out"))
      || "true".equalsIgnoreCase(request.getParameter("logged_out"))
      || "true".equalsIgnoreCase(request.getParameter("logout"));

  String pageScreenId = request.getParameter("pageScreenId");
  if (pageScreenId == null) {
    pageScreenId = "";
  }
  String assetVersion = request.getParameter("assetVersion");
  if (assetVersion == null || assetVersion.trim().isEmpty()) {
    assetVersion = "20261118_prop_v5";
  }
  boolean enableHashRedirect = "true".equalsIgnoreCase(request.getParameter("enableHashRedirect"));

  String requestedId = request.getParameter("id");
  if (requestedId == null) requestedId = "";
  String requestedPropId = request.getParameter("propId");
  if (requestedPropId == null) requestedPropId = "";
  String requestedAppId = request.getParameter("appId");
  if (requestedAppId == null) requestedAppId = "";

  String profileActionUrl = request.getContextPath() + "/jp/machiriku/servlet/user/Profile.action";

  ObjectMapper mapper = new ObjectMapper();
  String jsonDisplayName = mapper.writeValueAsString(displayName).replace("</", "<\\/");
  String jsonUserRole = mapper.writeValueAsString(userRole).replace("</", "<\\/");
  String jsonUserEmail = mapper.writeValueAsString(userEmail).replace("</", "<\\/");
  String jsonBusinessName = mapper.writeValueAsString(sessionBusinessName).replace("</", "<\\/");
  String jsonScreenId = mapper.writeValueAsString(pageScreenId).replace("</", "<\\/");
  String jsonId = mapper.writeValueAsString(requestedId).replace("</", "<\\/");
  String jsonPropId = mapper.writeValueAsString(requestedPropId).replace("</", "<\\/");
  String jsonAppId = mapper.writeValueAsString(requestedAppId).replace("</", "<\\/");
%>
  </main>
  <footer class="site-footer">
    <div class="footer-content">
      <div><strong>マチリク 宇都宮</strong>
        <p>欲しいお店の声を、街の次の一軒へ。</p>
      </div>
      <nav class="footer-links" aria-label="関連メニュー">
        <a href="${pageContext.request.contextPath}/index.jsp#s01">空き店舗を探す</a>
        <a href="${pageContext.request.contextPath}/index.jsp#s21">開店の相談</a>
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
        <a href="${pageContext.request.contextPath}/index.jsp#s20">全33画面・仕様対応表</a>
        <button type="button" class="btn-secondary"
          onclick="MachirikuStore.resetState(); MachirikuApp.navigate('s01')">初期状態に戻す</button>
        <span id="storage-status" role="status"></span>
      </div>
    </details>
    <small>© 2026 マチリク / Fチーム</small>
  </footer>
  <button type="button" id="mobile-back-to-top" class="mobile-back-to-top btn-print-hide" aria-label="ページトップへ戻る">
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <polyline points="18 15 12 9 6 15"></polyline>
    </svg>
    <span class="mobile-back-to-top-label" aria-hidden="true">TOP</span>
  </button>
  <script async
    src="https://maps.googleapis.com/maps/api/js?key=AIzaSyCNWrNbVnBfZsjuTJ3sHCKDemfkxNsIv7w&libraries=marker"></script>
  <script defer src="${pageContext.request.contextPath}/js/qrcode.js"></script>
  <script defer src="${pageContext.request.contextPath}/js/data.js?v=<%= Html.escape(assetVersion) %>"></script>
  <script defer src="${pageContext.request.contextPath}/js/map.js?v=<%= Html.escape(assetVersion) %>"></script>
  <script defer src="${pageContext.request.contextPath}/js/store.js?v=<%= Html.escape(assetVersion) %>"></script>
  <script defer src="${pageContext.request.contextPath}/js/screens.js?v=<%= Html.escape(assetVersion) %>"></script>
  <script defer src="${pageContext.request.contextPath}/js/property-pages.js?v=<%= Html.escape(assetVersion) %>"></script>
  <script defer src="${pageContext.request.contextPath}/js/app.js?v=<%= Html.escape(assetVersion) %>"></script>
  <script>
    window.__MACHIRIKU_CONTEXT_PATH__ = "${pageContext.request.contextPath}";
    window.__MACHIRIKU_FIXED_SCREEN__ = <%= jsonScreenId %>;
    window.__MACHIRIKU_FIXED_PARAMS__ = { id: <%= jsonId %>, propId: <%= jsonPropId %>, appId: <%= jsonAppId %> };
    document.addEventListener('DOMContentLoaded', function () {
      <% if (isLoggedOut) { %>
        // 明示的なログアウト時のみブラウザ側に残存するキャッシュを破棄し初期状態にリセット
        try {
          if (window.sessionStorage) {
            window.sessionStorage.removeItem('machiriku_user_display_name');
            window.sessionStorage.removeItem('machiriku_onboarding');
          }
          if (window.localStorage) {
            var keysToRemove = [];
            for (var i = 0; i < window.localStorage.length; i++) {
              var k = window.localStorage.key(i);
              if (k && k.startsWith('machiriku')) {
                keysToRemove.push(k);
              }
            }
            keysToRemove.forEach(function (k) { window.localStorage.removeItem(k); });
          }
          if (window.MachirikuStore && typeof window.MachirikuStore.resetState === 'function') {
            window.MachirikuStore.resetState();
          }
        } catch (_) {}
      <% } else if (currentUser != null) { %>
        if (window.MachirikuStore && typeof MachirikuStore.setRole === 'function') {
          MachirikuStore.setRole(<%= jsonUserRole %>);
        }
        if (window.MachirikuStore && typeof MachirikuStore.updateUserProfile === 'function') {
          var profilePatch = {
            name: <%= jsonDisplayName %>,
            role: <%= jsonUserRole %>,
            email: <%= jsonUserEmail %>,
            isLoggedIn: true
          };
          var sessBizName = <%= jsonBusinessName %>;
          if (sessBizName) profilePatch.businessName = sessBizName;
          MachirikuStore.updateUserProfile(profilePatch);
        }
        try {
          if (window.sessionStorage) {
            window.sessionStorage.removeItem('machiriku_onboarding');
            window.sessionStorage.setItem('machiriku_user_display_name', <%= jsonDisplayName %>);
          }
        } catch (_) {}
      <% } %>

      <% if (enableHashRedirect) { %>
      var ctx = "${pageContext.request.contextPath}";
      var PROPERTY_HASH_MAP = {
        '#s02': '/property/detail.jsp',
        '#s03': '/property/vote.jsp',
        '#r01': '/property/vote.jsp',
        '#s08': '/property/apply.jsp',
        '#b01': '/property/apply.jsp',
        '#s11': '/property/manage.jsp',
        '#o01': '/property/manage.jsp',
        '#s07': '/property/analytics.jsp',
        '#o02': '/property/analytics.jsp',
        '#s09': '/property/apply-list.jsp',
        '#o03': '/property/apply-list.jsp',
        '#o04': '/property/add.jsp',
        '#o05': '/property/add-confirm.jsp',
        '#o06': '/property/edit.jsp',
        '#o07': '/property/edit-confirm.jsp',
        '#o08': '/property/delete-confirm.jsp',
        '#o09': '/property/delete-complete.jsp'
      };
      function checkServletRedirect() {
        var rawHash = window.location.hash || '';
        var h = rawHash.toLowerCase();
        var hashBase = h.split('?')[0].split('&')[0];
        var qIdx = rawHash.indexOf('?');
        var qs = qIdx >= 0 ? rawHash.slice(qIdx) : '';

        if (hashBase === '#s12') {
          var hasId = /(?:^\?|&)id=/.test(qs);
          window.location.replace(ctx + (hasId ? '/property/edit.jsp' + qs : '/property/add.jsp'));
          return true;
        }
        if (PROPERTY_HASH_MAP[hashBase]) {
          window.location.replace(ctx + PROPERTY_HASH_MAP[hashBase] + qs);
          return true;
        }
        if (h === '#s16') {
          window.location.href = ctx + "/jp/machiriku/servlet/auth/Login.action";
          return true;
        } else if (h === '#s17') {
          window.location.href = ctx + "/jp/machiriku/servlet/auth/Register.action";
          return true;
        } else if (h === '#s18' || h.startsWith('#s18?') || h.startsWith('#s18&')) {
          <% if (currentUser == null) { %>
            window.location.href = ctx + "/jp/machiriku/servlet/auth/Login.action";
            return true;
          <% } else { %>
            window.location.href = "<%= profileActionUrl %>";
            return true;
          <% } %>
        }
        return false;
      }

      window.addEventListener('hashchange', checkServletRedirect);
      if (!checkServletRedirect()) {
        MachirikuApp.init();
        <% if ("1".equals(request.getParameter("profile_saved"))) { %>
          if (typeof MachirikuApp !== 'undefined') {
            MachirikuApp.showToast('プロフィール設定を保存しました');
          }
        <% } else if ("1".equals(request.getParameter("onboarded"))) { %>
          if (typeof MachirikuApp !== 'undefined') {
            MachirikuApp.showToast('初期設定が完了しました！マチリクへようこそ');
          }
        <% } %>
      }
      <% } else { %>
      MachirikuApp.init();
      <% } %>
    });
  </script>
</body>

</html>
