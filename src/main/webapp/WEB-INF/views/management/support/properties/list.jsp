<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel form-panel">
        <h2>物件を探す</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.PROPERTIES)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="property-query" name="q" label="物件名・所在地" type="search" value="${query}" maxLength="100" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <section class="panel">
        <h2>物件一覧</h2>
        <c:choose>
            <c:when test="${empty propertyPage.items}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="物件一覧" tabindex="0">
                    <table aria-label="物件一覧">
                        <thead>
                            <tr>
                                <th scope="col">ID</th>
                                <th scope="col">名前</th>
                                <th scope="col">所在地</th>
                                <th scope="col">面積（㎡）</th>
                                <th scope="col">月額賃料（円）</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${propertyPage.items}">
                                <tr>
                                    <td><a href="${fn:escapeXml(root += ManagementPaths.PROPERTIES += '?id=' += item.propertyId)}"><c:out value="${item.propertyId}" /></a></td>
                                    <td><a href="${fn:escapeXml(root += ManagementPaths.PROPERTIES += '?id=' += item.propertyId)}"><c:out value="${ManagementFormat.text(item.propertyName)}" /></a></td>
                                    <td><c:out value="${ManagementFormat.text(item.fullAddress)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.floorAreaSqm)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.monthlyRentYen)}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
        <mg:pager page="${propertyPage}" root="${root}" label="物件一覧" />
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
