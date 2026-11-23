<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" import="jp.machiriku.servlet.management.ManagementDisplay" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ attribute name="heading" required="true" %>
<%@ attribute name="root" required="true" %>
<%@ attribute name="record" type="java.util.Map" required="true" %>
<%@ attribute name="columns" %>
<%@ attribute name="links" type="java.util.Map" %>
<section class="panel">
    <h2><c:out value="${heading}" /></h2>
    <c:choose>
        <c:when test="${empty record}">
            <p class="empty">該当するデータはありません。</p>
        </c:when>
        <c:otherwise>
            <%-- JSPで属性を指定した場合は、その順番で詳細を表示する。 --%>
            <c:set var="labels" value="${columns eq null ? record.keySet() : fn:split(columns, '|')}" />
            <dl class="details">
                <c:forEach var="label" items="${labels}">
                    <c:set var="href" value="${ManagementDisplay.target(record, label, links, root)}" />
                    <c:set var="text" value="${ManagementDisplay.display(record[label])}" />
                    <dt><c:out value="${label}" /></dt>
                    <dd>
                        <c:choose>
                            <c:when test="${not empty href}">
                                <a href="${fn:escapeXml(href)}"><c:out value="${text}" /></a>
                            </c:when>
                            <c:otherwise><c:out value="${text}" /></c:otherwise>
                        </c:choose>
                    </dd>
                </c:forEach>
            </dl>
        </c:otherwise>
    </c:choose>
</section>
