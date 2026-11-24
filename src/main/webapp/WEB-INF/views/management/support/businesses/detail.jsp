<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <nav class="page-links" aria-label="この画面のメニュー">
        <a href="${fn:escapeXml(root += ManagementPaths.BUSINESSES)}">事業主一覧へ</a>
        <c:if test="${canReport}">
            <a href="${fn:escapeXml(root += ManagementPaths.REPORT += '?kind=BUSINESS&id=' += business.userId)}">事業主を通報</a>
        </c:if>
    </nav>

    <section class="panel">
        <h2>事業主プロフィール</h2>
        <dl class="details">
            <dt>ID</dt>
            <dd><c:out value="${business.userId}" /></dd>

            <dt>名前</dt>
            <dd><c:out value="${ManagementFormat.text(business.businessName)}" /></dd>

            <dt>希望エリア</dt>
            <dd><c:out value="${ManagementFormat.text(business.preferredArea)}" /></dd>

            <dt>業界経験</dt>
            <dd class="multiline"><c:out value="${ManagementFormat.text(business.industryExperience)}" /></dd>
        </dl>
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
