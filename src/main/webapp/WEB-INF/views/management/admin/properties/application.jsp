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
            <dt>申込番号</dt>
            <dd><c:out value="${application.applicationId}" /></dd>

            <dt>物件ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES += '?id=' += application.propertyId)}"><c:out value="${application.propertyId}" /></a></dd>

            <dt>物件</dt>
            <dd><c:out value="${ManagementFormat.text(application.propertyName)}" /></dd>

            <dt>オーナーID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += application.ownerUserId)}"><c:out value="${application.ownerUserId}" /></a></dd>

            <dt>事業主ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += application.businessUserId)}"><c:out value="${application.businessUserId}" /></a></dd>

            <dt>状態</dt>
            <dd><c:out value="${ManagementFormat.text(application.applicationStatus)}" /></dd>

            <dt>申込業種</dt>
            <dd><c:out value="${ManagementFormat.text(application.industryName)}" /></dd>

            <dt>契約区分</dt>
            <dd><c:out value="${ManagementFormat.text(application.contractKind)}" /></dd>

            <dt>希望開始日</dt>
            <dd><c:out value="${ManagementFormat.text(application.requestedStartOn)}" /></dd>

            <dt>希望終了日</dt>
            <dd><c:out value="${ManagementFormat.text(application.requestedEndOn)}" /></dd>

            <dt>内覧希望日時</dt>
            <dd><c:out value="${ManagementFormat.text(application.viewingRequestedAt)}" /></dd>

            <dt>申込内容</dt>
            <dd class="multiline"><c:out value="${ManagementFormat.text(application.message)}" /></dd>

            <dt>オーナーの返信</dt>
            <dd class="multiline"><c:out value="${ManagementFormat.text(application.ownerReply)}" /></dd>

            <dt>返信日時</dt>
            <dd><c:out value="${ManagementFormat.text(application.repliedAt)}" /></dd>

            <dt>申込日時</dt>
            <dd><c:out value="${ManagementFormat.text(application.createdAt)}" /></dd>

            <dt>契約ID</dt>
            <dd>
                <c:choose>
                    <c:when test="${application.tenancyId ne null}">
                        <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_TENANCIES += '?id=' += application.tenancyId)}"><c:out value="${application.tenancyId}" /></a>
                    </c:when>
                    <c:otherwise>—</c:otherwise>
                </c:choose>
            </dd>
        </dl>
    </section>

    <section class="panel">
        <h2>申込時の必要資格・書類</h2>
        <c:choose>
            <c:when test="${empty application.checks}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="申込時の必要資格・書類" tabindex="0">
                    <table aria-label="申込時の必要資格・書類">
                        <thead>
                            <tr>
                                <th scope="col">要件名</th>
                                <th scope="col">必須</th>
                                <th scope="col">適用条件</th>
                                <th scope="col">事業主による確認</th>
                                <th scope="col">オーナー確認状態</th>
                                <th scope="col">確認メモ</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${application.checks}">
                                <tr>
                                    <td><c:out value="${ManagementFormat.text(item.requirementName)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.required)}" /></td>
                                    <td class="multiline"><c:out value="${ManagementFormat.text(item.conditionText)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.businessConfirmed)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.ownerCheckStatus)}" /></td>
                                    <td class="multiline"><c:out value="${ManagementFormat.text(item.checkNote)}" /></td>
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
