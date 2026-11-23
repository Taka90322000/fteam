<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="jp.machiriku.bean.User,jp.machiriku.servlet.auth.LogoutAction,jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.tool.Html" %>
<%
  User footerUser = session.getAttribute("user") instanceof User ? (User) session.getAttribute("user") : null;
  String rawFooterRole = session.getAttribute("userRole") instanceof String ? (String) session.getAttribute("userRole") : null;
  boolean isFooterDemo = footerUser == null && rawFooterRole != null
      && java.util.Set.of("resident", "business", "owner", "admin").contains(rawFooterRole);
  String footerLogoutToken = LogoutAction.csrfToken(session);
%>
  <%-- 共通ヘッダーで開始したmain要素を閉じ、全画面共通のフッターを表示する。 --%>
  </main>
  <footer class="site-footer">
    <div class="footer-content">
      <div><strong>マチリク 宇都宮</strong>
        <p>欲しいお店の声を、街の次の一軒へ。</p>
      </div>
      <nav class="footer-links" aria-label="関連メニュー">
        <a href="${pageContext.request.contextPath}/index.jsp">ホーム</a>
        <a href="<%= Html.escape(request.getContextPath() + ManagementPaths.PROPERTIES) %>">掲載物件</a>
        <a href="<%= Html.escape(request.getContextPath() + ManagementPaths.REQUIREMENTS) %>">必要資格・書類</a>
        <a href="<%= Html.escape(request.getContextPath() + ManagementPaths.INQUIRIES) %>">お問い合わせ</a>
        <% if (Boolean.TRUE.equals(request.getAttribute("showAdminLink"))) { %>
        <a href="<%= Html.escape(request.getContextPath() + ManagementPaths.ADMIN) %>">管理画面</a>
        <% } %>
        <% if (footerUser == null && !isFooterDemo) { %>
        <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Login.action">ログイン</a>
        <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Register.action">新規会員登録</a>
        <% } else if (isFooterDemo) { %>
        <form action="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Logout.action" method="POST" style="display:inline; margin:0;">
          <input type="hidden" name="csrfToken" value="<%= Html.escape(footerLogoutToken) %>">
          <input type="hidden" name="next" value="login">
          <button type="submit" style="background:none; border:none; padding:0; color:inherit; font:inherit; text-decoration:underline; cursor:pointer;">ログイン</button>
        </form>
        <form action="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Logout.action" method="POST" style="display:inline; margin:0;">
          <input type="hidden" name="csrfToken" value="<%= Html.escape(footerLogoutToken) %>">
          <input type="hidden" name="next" value="register">
          <button type="submit" style="background:none; border:none; padding:0; color:inherit; font:inherit; text-decoration:underline; cursor:pointer;">新規会員登録</button>
        </form>
        <form action="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Logout.action" method="POST" style="display:inline; margin:0;" onsubmit="return confirm('デモ表示を終了してログアウトしますか？');">
          <input type="hidden" name="csrfToken" value="<%= Html.escape(footerLogoutToken) %>">
          <button type="submit" style="background:none; border:none; padding:0; color:inherit; font:inherit; text-decoration:underline; cursor:pointer;">デモ終了（ログアウト）</button>
        </form>
        <% } else { %>
        <form action="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Logout.action" method="POST" style="display:inline; margin:0;" onsubmit="return confirm('ログアウトしますか？');">
          <input type="hidden" name="csrfToken" value="<%= Html.escape(footerLogoutToken) %>">
          <button type="submit" style="background:none; border:none; padding:0; color:inherit; font:inherit; text-decoration:underline; cursor:pointer;">ログアウト</button>
        </form>
        <% } %>
      </nav>
    </div>
    <small>© 2026 マチリク / Fチーム</small>
  </footer>
</body>
</html>
