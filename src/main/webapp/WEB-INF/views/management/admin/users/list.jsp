<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <c:forEach var="entry" items="${roles}">
        <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?role=' += entry.key)}"<c:if test="${entry.key eq role}"> aria-current="page"</c:if>><c:out value="${entry.value}" /></a>
    </c:forEach>
</nav>
<div class="sections">
    <section class="panel form-panel">
        <h2>会員を探す</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS)}" method="get" accept-charset="UTF-8">
            <input type="hidden" name="role" value="${fn:escapeXml(role)}">
            <div class="form-fields">
                <mg:input id="users-query" name="q" label="名前・会員ID・メール・事業者名" type="search" value="${query}" maxLength="100" />
                <mg:select id="users-status" name="status" label="利用状態" choices="${states}" value="${status}" placeholder="すべての状態" />
                <mg:input id="users-from" name="from" label="登録日（開始）" type="date" value="${from}" />
                <mg:input id="users-to" name="to" label="登録日（終了）" type="date" value="${to}" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <section class="panel">
        <h2><c:out value="${roleLabel += '一覧'}" /></h2>
        <c:choose>
            <c:when test="${empty memberPage.items}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="${fn:escapeXml(roleLabel += '一覧')}" tabindex="0">
                    <table aria-label="${fn:escapeXml(roleLabel += '一覧')}">
                        <thead>
                            <tr>
                                <th scope="col">ID</th>
                                <th scope="col">名前</th>
                                <th scope="col">事業者名・法人名</th>
                                <th scope="col">メール</th>
                                <th scope="col">利用状態</th>
                                <th scope="col">登録日</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${memberPage.items}">
                                <tr>
                                    <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += item.userId)}"><c:out value="${item.userId}" /></a></td>
                                    <td><c:out value="${ManagementFormat.text(item.displayName)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.organizationName)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.email)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.accountStatusLabel)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.createdAt)}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
        <mg:pager page="${memberPage}" root="${root}" label="${roleLabel += '一覧'}" />
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
