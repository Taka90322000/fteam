<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel form-panel">
        <h2>絞り込み</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_AUDIT)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="audit-query" name="q" label="操作・対象・操作者名・理由" type="search" value="${query}" maxLength="100" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <section class="panel">
        <h2>管理者が行った操作</h2>
        <c:choose>
            <c:when test="${empty auditPage.items}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="管理者が行った操作" tabindex="0">
                    <table aria-label="管理者が行った操作">
                        <thead>
                            <tr>
                                <th scope="col">操作日時</th>
                                <th scope="col">操作者ID</th>
                                <th scope="col">操作者</th>
                                <th scope="col">操作</th>
                                <th scope="col">対象区分</th>
                                <th scope="col">対象ID</th>
                                <th scope="col">理由</th>
                                <th scope="col">変更内容</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="entry" items="${auditPage.items}">
                                <tr>
                                    <td><c:out value="${ManagementFormat.text(entry.occurredAt)}" /></td>
                                    <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += entry.actorUserId)}"><c:out value="${entry.actorUserId}" /></a></td>
                                    <td><c:out value="${ManagementFormat.text(entry.actorName)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(entry.action)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(entry.targetType)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(entry.targetKey)}" /></td>
                                    <td class="multiline"><c:out value="${ManagementFormat.text(entry.reason)}" /></td>
                                    <td class="multiline"><c:out value="${ManagementFormat.text(entry.changes)}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
        <mg:pager page="${auditPage}" root="${root}" label="管理者が行った操作" />
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
