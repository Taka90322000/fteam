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
            <dt>契約番号</dt>
            <dd><c:out value="${tenancy.tenancyId}" /></dd>

            <dt>申込ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_APPLICATIONS += '?id=' += tenancy.applicationId)}"><c:out value="${tenancy.applicationId}" /></a></dd>

            <dt>物件ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES += '?id=' += tenancy.propertyId)}"><c:out value="${tenancy.propertyId}" /></a></dd>

            <dt>物件</dt>
            <dd><c:out value="${ManagementFormat.text(tenancy.propertyName)}" /></dd>

            <dt>オーナーID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += tenancy.ownerUserId)}"><c:out value="${tenancy.ownerUserId}" /></a></dd>

            <dt>事業主ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += tenancy.businessUserId)}"><c:out value="${tenancy.businessUserId}" /></a></dd>

            <dt>店舗</dt>
            <dd><c:out value="${ManagementFormat.text(tenancy.storeName)}" /></dd>

            <dt>契約状態</dt>
            <dd><c:out value="${ManagementFormat.text(tenancy.tenancyStatus)}" /></dd>

            <dt>契約開始日</dt>
            <dd><c:out value="${ManagementFormat.text(tenancy.contractStartOn)}" /></dd>

            <dt>契約終了日</dt>
            <dd><c:out value="${ManagementFormat.text(tenancy.contractEndOn)}" /></dd>

            <dt>契約確定日時</dt>
            <dd><c:out value="${ManagementFormat.text(tenancy.confirmedAt)}" /></dd>

            <dt>オーナーの引渡し</dt>
            <dd><c:out value="${ManagementFormat.text(tenancy.ownerHandoverAt)}" /></dd>

            <dt>事業主の受け取り</dt>
            <dd><c:out value="${ManagementFormat.text(tenancy.businessHandoverAt)}" /></dd>

            <dt>開店日</dt>
            <dd><c:out value="${ManagementFormat.text(tenancy.openedOn)}" /></dd>
        </dl>
    </section>

    <section class="panel">
        <h2>出店計画</h2>
        <c:choose>
            <c:when test="${empty tenancy.openingPlans}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="出店計画" tabindex="0">
                    <table aria-label="出店計画">
                        <thead>
                            <tr>
                                <th scope="col">計画ID</th>
                                <th scope="col">計画名</th>
                                <th scope="col">進捗</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="plan" items="${tenancy.openingPlans}">
                                <tr>
                                    <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PLANS += '?id=' += plan.planId)}"><c:out value="${plan.planId}" /></a></td>
                                    <td><c:out value="${ManagementFormat.text(plan.title)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(plan.progressStatus)}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
