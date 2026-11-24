<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.PROPERTIES)}">掲載物件へ</a>
</nav>
<div class="sections">
    <section class="panel">
        <h2>受付内容</h2>
        <dl class="details">
            <dt>受付番号</dt>
            <dd><c:out value="${receipt.reportId}" /></dd>

            <dt>状態</dt>
            <dd><c:out value="${ManagementFormat.text(receipt.statusLabel)}" /></dd>

            <dt>受付日時</dt>
            <dd><c:out value="${ManagementFormat.text(receipt.createdAt)}" /></dd>
        </dl>
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
