<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel">
        <h2>通報対象</h2>
        <dl class="details">
            <dt><c:out value="${target.label}" /></dt>
            <dd><c:out value="${ManagementFormat.text(target.name)}" /></dd>
            <c:if test="${target.relatedPropertyName ne null}">
                <dt>関連物件</dt>
                <dd><c:out value="${ManagementFormat.text(target.relatedPropertyName)}" /></dd>
            </c:if>
        </dl>
    </section>

    <section class="panel form-panel">
        <h2>通報の内容</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.REPORT)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="send">
            <input type="hidden" name="kind" value="${fn:escapeXml(target.kind)}">
            <input type="hidden" name="id" value="${fn:escapeXml(target.targetId)}">
            <input type="hidden" name="propertyId" value="${fn:escapeXml(target.relatedPropertyId)}">
            <div class="form-fields">
                <mg:select id="member-report-reason" name="reasonCode" label="理由" choices="${reasons}" placeholder="理由を選択" required="true" />
                <mg:textarea id="member-report-body" name="body" label="詳細" maxLength="8000" required="true" />
            </div>
            <button type="submit">通報を送信</button>
        </form>
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
