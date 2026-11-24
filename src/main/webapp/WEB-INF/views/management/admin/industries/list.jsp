<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES += '?kind=type')}"<c:if test="${not isCategory}"> aria-current="page"</c:if>>業種</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES += '?kind=category')}"<c:if test="${isCategory}"> aria-current="page"</c:if>>業種カテゴリ</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES += '?kind=' += kind += '&new=1')}"><c:out value="${isCategory ? 'カテゴリを追加' : '業種を追加'}" /></a>
</nav>
<div class="sections">
    <section class="panel form-panel">
        <h2>絞り込み</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES)}" method="get" accept-charset="UTF-8">
            <input type="hidden" name="kind" value="${fn:escapeXml(kind)}">
            <div class="form-fields">
                <mg:input id="industry-query" name="q" label="名称・コード" type="search" value="${query}" maxLength="100" />
                <mg:select id="industry-status" name="status" label="有効状態" choices="${activeChoices}" value="${status}" placeholder="すべて" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <section class="panel">
        <h2><c:out value="${isCategory ? '業種カテゴリ一覧' : '業種一覧'}" /></h2>
        <c:choose>
            <c:when test="${empty industryPage.items}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="${isCategory ? '業種カテゴリ一覧' : '業種一覧'}" tabindex="0">
                    <table aria-label="${isCategory ? '業種カテゴリ一覧' : '業種一覧'}">
                        <thead>
                            <tr>
                                <th scope="col">ID</th>
                                <th scope="col">コード</th>
                                <th scope="col">名前</th>
                                <c:if test="${not isCategory}">
                                    <th scope="col">カテゴリ</th>
                                </c:if>
                                <th scope="col">表示順</th>
                                <th scope="col">有効</th>
                                <c:if test="${not isCategory}">
                                    <th scope="col">カテゴリ有効</th>
                                </c:if>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${industryPage.items}">
                                <tr>
                                    <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES += '?kind=' += kind += '&id=' += item.id)}"><c:out value="${item.id}" /></a></td>
                                    <td><c:out value="${ManagementFormat.text(item.code)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.name)}" /></td>
                                    <c:if test="${not isCategory}">
                                        <td><c:out value="${ManagementFormat.text(item.categoryName)}" /></td>
                                    </c:if>
                                    <td><c:out value="${item.displayOrder}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.active)}" /></td>
                                    <c:if test="${not isCategory}">
                                        <td><c:out value="${ManagementFormat.text(item.categoryActive)}" /></td>
                                    </c:if>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
        <mg:pager page="${industryPage}" root="${root}" label="${isCategory ? '業種カテゴリ一覧' : '業種一覧'}" />
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
