<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel form-panel">
        <h2>対象業種</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.REQUIREMENTS)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:select id="public-requirement-industry" name="industry" label="業種" choices="${industries}" value="${industryId}" placeholder="すべての業種" />
            </div>
            <button type="submit">表示する</button>
        </form>
    </section>

    <section class="panel">
        <h2>業種別の要件</h2>
        <c:choose>
            <c:when test="${empty requirementPage.items}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="業種別の要件" tabindex="0">
                    <table aria-label="業種別の要件">
                        <thead>
                            <tr>
                                <th scope="col">業種</th>
                                <th scope="col">名称</th>
                                <th scope="col">種別</th>
                                <th scope="col">必要区分</th>
                                <th scope="col">適用条件</th>
                                <th scope="col">案内</th>
                                <th scope="col">案内URL</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${requirementPage.items}">
                                <tr>
                                    <td><c:out value="${ManagementFormat.text(item.industryName)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.qualificationName)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.requirementKindLabel)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.requiredLabel)}" /></td>
                                    <td class="multiline"><c:out value="${ManagementFormat.text(item.conditionText)}" /></td>
                                    <td class="multiline"><c:out value="${ManagementFormat.text(item.guidanceText)}" /></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${ManagementFormat.isWebUrl(item.guidanceUrl)}">
                                                <a href="${fn:escapeXml(item.guidanceUrl)}"><c:out value="${item.guidanceUrl}" /></a>
                                            </c:when>
                                            <c:otherwise><c:out value="${ManagementFormat.text(item.guidanceUrl)}" /></c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
        <mg:pager page="${requirementPage}" root="${root}" label="業種別の要件" />
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
