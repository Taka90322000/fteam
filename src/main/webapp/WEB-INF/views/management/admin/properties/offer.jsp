<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel">
        <h2>詳細</h2>
        <dl class="details">
            <dt>オファー番号</dt>
            <dd><c:out value="${offer.offerId}" /></dd>

            <dt>物件ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES += '?id=' += offer.propertyId)}"><c:out value="${offer.propertyId}" /></a></dd>

            <dt>物件</dt>
            <dd><c:out value="${ManagementFormat.text(offer.propertyName)}" /></dd>

            <dt>オーナーID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += offer.ownerUserId)}"><c:out value="${offer.ownerUserId}" /></a></dd>

            <dt>送信者</dt>
            <dd><c:out value="${ManagementFormat.text(offer.ownerName)}" /></dd>

            <dt>事業主ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += offer.businessUserId)}"><c:out value="${offer.businessUserId}" /></a></dd>

            <dt>受信者</dt>
            <dd><c:out value="${ManagementFormat.text(offer.businessName)}" /></dd>

            <dt>作成方法</dt>
            <dd><c:out value="${ManagementFormat.text(offer.creationKind)}" /></dd>

            <dt>内容</dt>
            <dd class="multiline"><c:out value="${ManagementFormat.text(offer.message)}" /></dd>

            <dt>状態</dt>
            <dd><c:out value="${ManagementFormat.text(offer.offerStatus)}" /></dd>

            <dt>送信日時</dt>
            <dd><c:out value="${ManagementFormat.text(offer.sentAt)}" /></dd>

            <dt>返答日時</dt>
            <dd><c:out value="${ManagementFormat.text(offer.respondedAt)}" /></dd>
        </dl>
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
