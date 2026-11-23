<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <nav class="page-links" aria-label="この画面のメニュー">
        <a href="${fn:escapeXml(root += ManagementPaths.PROPERTIES)}">掲載物件一覧へ</a>
        <c:if test="${model.canInquire()}">
        <a href="${fn:escapeXml(root += ManagementPaths.INQUIRIES += '?propertyId=' += model.id())}">この物件について問い合わせる</a>
        </c:if>
        <c:if test="${model.canReport()}">
        <a href="${fn:escapeXml(root += ManagementPaths.REPORT += '?kind=PROPERTY&id=' += model.id())}">この物件を通報</a>
        <a href="${fn:escapeXml(root += ManagementPaths.REPORT += '?kind=OWNER&id=' += model.ownerId() += '&propertyId=' += model.id())}">掲載者を通報</a>
        </c:if>
    </nav>

    <mg:details heading="物件情報" root="${root}" record="${model.property()}" columns="ID|名前|掲載者|所在地|面積（㎡）|月額賃料（円）|説明" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
