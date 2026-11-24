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
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_REPORTS)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="report-query" name="q" label="対象名・受付番号" type="search" value="${query}" maxLength="100" />
                <mg:select id="report-kind" name="kind" label="対象区分" choices="${kinds}" value="${kind}" placeholder="すべての対象" />
                <mg:select id="report-status" name="status" label="対応状態" choices="${states}" value="${status}" placeholder="すべての状態" />
                <mg:select id="report-reason" name="reasonCode" label="理由" choices="${reasons}" value="${reason}" placeholder="すべての理由" />
                <mg:input id="report-from" name="from" label="受付日（開始）" type="date" value="${from}" />
                <mg:input id="report-to" name="to" label="受付日（終了）" type="date" value="${to}" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <section class="panel">
        <h2>通報一覧</h2>
        <c:choose>
            <c:when test="${empty reportPage.items}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="通報一覧" tabindex="0">
                    <table aria-label="通報一覧">
                        <thead>
                            <tr>
                                <th scope="col">ID</th>
                                <th scope="col">対象区分</th>
                                <th scope="col">対象</th>
                                <th scope="col">理由</th>
                                <th scope="col">状態</th>
                                <th scope="col">担当者</th>
                                <th scope="col">受付日時</th>
                                <th scope="col">同じ対象の未対応件数</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${reportPage.items}">
                                <tr>
                                    <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_REPORTS += '?id=' += item.reportId)}"><c:out value="${item.reportId}" /></a></td>
                                    <td><c:out value="${ManagementFormat.text(item.targetKindLabel)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.targetName)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.reasonLabel)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.statusLabel)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.assignedAdminName)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.createdAt)}" /></td>
                                    <td><c:out value="${item.openCountForSameTarget}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
        <mg:pager page="${reportPage}" root="${root}" label="通報一覧" />
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
