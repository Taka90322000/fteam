<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<c:url var="detailUrl" value="${ManagementPaths.ADMIN_USERS}">
    <c:param name="id" value="${member.userId}" />
</c:url>
<c:url var="listUrl" value="${ManagementPaths.ADMIN_USERS}">
    <c:param name="role" value="${member.userRole}" />
</c:url>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(listUrl)}">会員一覧へ</a>
    <a href="${fn:escapeXml(detailUrl)}&amp;tab=profile"<c:if test="${selectedTab eq 'profile'}"> aria-current="page"</c:if>>プロフィール</a>
    <a href="${fn:escapeXml(detailUrl)}&amp;tab=preferences"<c:if test="${selectedTab eq 'preferences'}"> aria-current="page"</c:if>>希望業種</a>
    <c:forEach var="entry" items="${roleTabs}">
        <a href="${fn:escapeXml(detailUrl += '&tab=' += entry.key)}"<c:if test="${selectedTab eq entry.key}"> aria-current="page"</c:if>><c:out value="${entry.value}" /></a>
    </c:forEach>
    <a href="${fn:escapeXml(detailUrl)}&amp;tab=inquiries"<c:if test="${selectedTab eq 'inquiries'}"> aria-current="page"</c:if>>問い合わせ履歴</a>
</nav>
<div class="sections">
    <section class="panel">
        <h2>会員情報</h2>
        <dl class="details">
            <dt>会員ID</dt>
            <dd><c:out value="${member.userId}" /></dd>

            <dt>名前</dt>
            <dd><c:out value="${ManagementFormat.text(member.displayName)}" /></dd>

            <dt>メール</dt>
            <dd><c:out value="${ManagementFormat.text(member.email)}" /></dd>

            <dt>区分</dt>
            <dd><c:out value="${ManagementFormat.text(member.userRoleLabel)}" /></dd>

            <dt>利用状態</dt>
            <dd><c:out value="${ManagementFormat.text(member.accountStatusLabel)}" /></dd>

            <dt>登録日</dt>
            <dd><c:out value="${ManagementFormat.text(member.createdAt)}" /></dd>

            <dt>更新日</dt>
            <dd><c:out value="${ManagementFormat.text(member.updatedAt)}" /></dd>

            <dt>退会日時</dt>
            <dd><c:out value="${ManagementFormat.text(member.withdrawnAt)}" /></dd>
        </dl>
    </section>

    <c:choose>
        <c:when test="${selectedTab eq 'profile'}">
            <jsp:include page="/WEB-INF/views/management/admin/users/profile.jsp" />
        </c:when>
        <c:when test="${selectedTab eq 'preferences'}">
            <jsp:include page="/WEB-INF/views/management/admin/users/preferences.jsp" />
        </c:when>
        <c:when test="${selectedTab eq 'locations'}">
            <jsp:include page="/WEB-INF/views/management/admin/users/locations.jsp" />
        </c:when>
        <c:when test="${selectedTab eq 'qualifications'}">
            <jsp:include page="/WEB-INF/views/management/admin/users/qualifications.jsp" />
        </c:when>
        <c:otherwise>
            <jsp:include page="/WEB-INF/views/management/admin/users/history.jsp" />
        </c:otherwise>
    </c:choose>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
