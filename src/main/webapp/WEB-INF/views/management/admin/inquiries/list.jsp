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
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_INQUIRIES)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="inquiry-query" name="q" label="受付番号・送信者・件名" type="search" value="${query}" maxLength="100" />
                <mg:select id="inquiry-category" name="category" label="カテゴリ" choices="${categories}" value="${category}" placeholder="すべてのカテゴリ" />
                <mg:select id="inquiry-state" name="status" label="対応状態" choices="${states}" value="${status}" placeholder="すべての状態" />
                <mg:select id="inquiry-assignee" name="assignee" label="担当者" choices="${admins}" value="${assigneeId}" placeholder="すべての担当者" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <section class="panel">
        <h2>受付一覧</h2>
        <c:choose>
            <c:when test="${empty inquiryPage.items}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="受付一覧" tabindex="0">
                    <table aria-label="受付一覧">
                        <thead>
                            <tr>
                                <th scope="col">ID</th>
                                <th scope="col">件名</th>
                                <th scope="col">分類</th>
                                <th scope="col">状態</th>
                                <th scope="col">更新日時</th>
                                <th scope="col">送信者</th>
                                <th scope="col">担当者</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${inquiryPage.items}">
                                <tr>
                                    <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INQUIRIES += '?id=' += item.inquiryId)}"><c:out value="${item.inquiryId}" /></a></td>
                                    <td><c:out value="${ManagementFormat.text(item.subject)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.categoryLabel)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.statusLabel)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.updatedAt)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.requesterName)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.assignedAdminName)}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
        <mg:pager page="${inquiryPage}" root="${root}" label="受付一覧" />
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
