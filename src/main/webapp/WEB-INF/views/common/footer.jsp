<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="jp.machiriku.bean.User" %>
<%
  User footerUser = session.getAttribute("user") instanceof User ? (User) session.getAttribute("user") : null;
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
        <% if (footerUser == null) { %>
        <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Login.action">ログイン</a>
        <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Register.action">新規会員登録</a>
        <% } else { %>
        <a href="${pageContext.request.contextPath}/jp/machiriku/servlet/auth/Logout.action" onclick="return confirm('ログアウトしますか？');">ログアウト</a>
        <% } %>
      </nav>
    </div>
    <small>© 2026 マチリク / Fチーム</small>
  </footer>
</body>
</html>
