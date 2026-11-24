<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}" aria-current="page">資格・書類マスター</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS += '?tab=requirements')}">業種ごとの必要条件</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS += '?new=1')}">資格・書類を追加</a>
</nav>
<div class="sections">
    <section class="panel form-panel">
        <h2>絞り込み</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="qualification-query" name="q" label="名称" type="search" value="${query}" maxLength="100" />
                <mg:select id="qualification-kind" name="kind" label="種類" choices="${kinds}" value="${kind}" placeholder="すべての種類" />
                <mg:select id="qualification-status" name="status" label="有効状態" choices="${activeChoices}" value="${status}" placeholder="すべて" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <section class="panel">
        <h2>資格・書類一覧</h2>
        <c:choose>
            <c:when test="${empty qualificationPage.items}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="資格・書類一覧" tabindex="0">
                    <table aria-label="資格・書類一覧">
                        <thead>
                            <tr>
                                <th scope="col">ID</th>
                                <th scope="col">名前</th>
                                <th scope="col">種類</th>
                                <th scope="col">説明</th>
                                <th scope="col">有効</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${qualificationPage.items}">
                                <tr>
                                    <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS += '?id=' += item.qualificationId)}"><c:out value="${item.qualificationId}" /></a></td>
                                    <td><c:out value="${ManagementFormat.text(item.qualificationName)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.requirementKindLabel)}" /></td>
                                    <td class="multiline"><c:out value="${ManagementFormat.text(item.guidanceText)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.active)}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
        <mg:pager page="${qualificationPage}" root="${root}" label="資格・書類一覧" />
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
