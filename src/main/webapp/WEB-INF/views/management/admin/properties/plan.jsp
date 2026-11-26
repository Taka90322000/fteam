<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel">
        <h2>詳細</h2>
        <dl class="details">
            <dt>計画番号</dt>
            <dd><c:out value="${plan.planId}" /></dd>

            <dt>契約ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_TENANCIES += '?id=' += plan.tenancyId)}"><c:out value="${plan.tenancyId}" /></a></dd>

            <dt>申込ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_APPLICATIONS += '?id=' += plan.applicationId)}"><c:out value="${plan.applicationId}" /></a></dd>

            <dt>物件ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES += '?id=' += plan.propertyId)}"><c:out value="${plan.propertyId}" /></a></dd>

            <dt>事業主ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += plan.businessUserId)}"><c:out value="${plan.businessUserId}" /></a></dd>

            <dt>計画名</dt>
            <dd><c:out value="${ManagementFormat.text(plan.title)}" /></dd>

            <dt>内容</dt>
            <dd class="multiline"><c:out value="${ManagementFormat.text(plan.body)}" /></dd>

            <dt>開店予定日</dt>
            <dd><c:out value="${ManagementFormat.text(plan.plannedOpenOn)}" /></dd>

            <dt>進捗</dt>
            <dd><c:out value="${ManagementFormat.text(plan.progressStatus)}" /></dd>

            <dt>公開状態</dt>
            <dd><c:out value="${ManagementFormat.text(plan.publicationStatus)}" /></dd>

            <dt>公開日時</dt>
            <dd><c:out value="${ManagementFormat.text(plan.publishedAt)}" /></dd>

            <dt>更新日時</dt>
            <dd><c:out value="${ManagementFormat.text(plan.updatedAt)}" /></dd>
        </dl>
    </section>

    <c:if test="${plan.canArchive}">
        <section class="panel form-panel">
            <h2>出店計画の公開を停止</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_PLANS)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="archive">
                <input type="hidden" name="id" value="${fn:escapeXml(plan.planId)}">
                <div class="form-fields">
                    <mg:textarea id="plan-archive-reason" name="reason" label="停止理由" maxLength="2000" required="true" />
                </div>
                <button type="submit">公開を停止する</button>
            </form>
        </section>
    </c:if>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
