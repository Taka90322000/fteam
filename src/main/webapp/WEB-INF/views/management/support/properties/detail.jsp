<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <nav class="page-links" aria-label="この画面のメニュー">
        <a href="${fn:escapeXml(root += ManagementPaths.PROPERTIES)}">掲載物件一覧へ</a>
        <c:if test="${canInquire}">
            <a href="${fn:escapeXml(root += ManagementPaths.INQUIRIES += '?propertyId=' += property.propertyId)}">この物件について問い合わせる</a>
        </c:if>
        <c:if test="${canReport}">
            <a href="${fn:escapeXml(root += ManagementPaths.REPORT += '?kind=PROPERTY&id=' += property.propertyId)}">この物件を通報</a>
            <a href="${fn:escapeXml(root += ManagementPaths.REPORT += '?kind=OWNER&id=' += property.ownerUserId += '&propertyId=' += property.propertyId)}">掲載者を通報</a>
        </c:if>
    </nav>

    <section class="panel">
        <h2>物件情報</h2>
        <dl class="details">
            <dt>ID</dt>
            <dd><c:out value="${property.propertyId}" /></dd>

            <dt>名前</dt>
            <dd><c:out value="${ManagementFormat.text(property.propertyName)}" /></dd>

            <dt>掲載者</dt>
            <dd><c:out value="${ManagementFormat.text(property.ownerName)}" /></dd>

            <dt>所在地</dt>
            <dd><c:out value="${ManagementFormat.text(property.fullAddress)}" /></dd>

            <dt>面積（㎡）</dt>
            <dd><c:out value="${ManagementFormat.text(property.floorAreaSqm)}" /></dd>

            <dt>月額賃料（円）</dt>
            <dd><c:out value="${ManagementFormat.text(property.monthlyRentYen)}" /></dd>

            <dt>説明</dt>
            <dd class="multiline"><c:out value="${ManagementFormat.text(property.description)}" /></dd>
        </dl>
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
