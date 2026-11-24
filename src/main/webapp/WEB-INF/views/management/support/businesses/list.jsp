<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel form-panel">
        <h2>事業主を探す</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.BUSINESSES)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="business-query" name="q" label="事業者名" type="search" value="${query}" maxLength="100" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <section class="panel">
        <h2>事業主</h2>
        <c:choose>
            <c:when test="${empty businessPage.items}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="事業主" tabindex="0">
                    <table aria-label="事業主">
                        <thead>
                            <tr>
                                <th scope="col">ID</th>
                                <th scope="col">名前</th>
                                <th scope="col">希望エリア</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${businessPage.items}">
                                <tr>
                                    <td><a href="${fn:escapeXml(root += ManagementPaths.BUSINESSES += '?id=' += item.userId)}"><c:out value="${item.userId}" /></a></td>
                                    <td><a href="${fn:escapeXml(root += ManagementPaths.BUSINESSES += '?id=' += item.userId)}"><c:out value="${ManagementFormat.text(item.businessName)}" /></a></td>
                                    <td><c:out value="${ManagementFormat.text(item.preferredArea)}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
        <mg:pager page="${businessPage}" root="${root}" label="事業主" />
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
