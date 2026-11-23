<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <nav class="page-links" aria-label="この画面のメニュー">
        <a href="${fn:escapeXml(root += ManagementPaths.BUSINESSES)}">事業主一覧へ</a>
        <c:if test="${model.canReport()}">
        <a href="${fn:escapeXml(root += ManagementPaths.REPORT += '?kind=BUSINESS&id=' += model.id())}">事業主を通報</a>
        </c:if>
    </nav>

    <mg:details heading="事業主プロフィール" root="${root}" record="${model.business()}" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
