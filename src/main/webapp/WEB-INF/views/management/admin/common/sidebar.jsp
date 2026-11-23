<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<%-- メニューの表示順を定め、現在のActionに対応する項目を選択中にする。 --%>
<c:set var="menu" value="${ManagementDisplay.options(
    ManagementPaths.ADMIN, 'ダッシュボード', ManagementPaths.ADMIN_USERS, 'ユーザー管理',
    ManagementPaths.ADMIN_INDUSTRIES, '業種管理', ManagementPaths.ADMIN_QUALIFICATIONS, '必要資格・書類管理',
    ManagementPaths.ADMIN_PROPERTIES, '物件管理', ManagementPaths.ADMIN_SURVEYS, 'アンケート管理',
    ManagementPaths.ADMIN_COUPONS, 'クーポン管理', ManagementPaths.ADMIN_REPORTS, '通報管理',
    ManagementPaths.ADMIN_INQUIRIES, '問い合わせ管理', ManagementPaths.ADMIN_AUDIT, '操作履歴')}" />
<nav class="sidebar" aria-label="管理メニュー">
    <c:forEach var="item" items="${menu}">
        <a href="${fn:escapeXml(root += item.key)}"<c:if test="${item.key eq requestScope.managementRoute}"> aria-current="page"</c:if>><c:out value="${item.value}" /></a>
    </c:forEach>
</nav>
