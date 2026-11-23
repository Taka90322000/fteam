<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.PROPERTIES)}">掲載物件へ</a>
</nav>
<div class="sections">
    <mg:details heading="受付内容" root="${root}" record="${model.receipt()}" columns="受付番号|状態|受付日時" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
