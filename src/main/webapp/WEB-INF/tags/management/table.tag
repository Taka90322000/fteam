<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" import="jp.machiriku.servlet.management.ManagementDisplay" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ attribute name="heading" required="true" %>
<%@ attribute name="root" required="true" %>
<%@ attribute name="rows" type="java.util.List" %>
<%@ attribute name="pagination" type="jp.machiriku.bean.management.PageRows" %>
<%@ attribute name="columns" %>
<%@ attribute name="links" type="java.util.Map" %>
<%-- ページ移動のある一覧と、補足の一覧を同じ表の書式で表示する。 --%>
<c:set var="entries" value="${pagination ne null ? pagination.rows() : rows}" />
<section class="panel">
    <h2><c:out value="${heading}" /></h2>
    <c:choose>
        <c:when test="${empty entries}">
            <p class="empty">該当するデータはありません。</p>
        </c:when>
        <c:otherwise>
            <%-- 明示した列はJSPの指定順に、その他の列はDAOの項目順に表示する。 --%>
            <c:set var="headings" value="${columns eq null ? entries[0].keySet() : fn:split(columns, '|')}" />
            <div class="table-scroll" role="region" aria-label="${fn:escapeXml(heading)}" tabindex="0">
                <table aria-label="${fn:escapeXml(heading)}">
                    <thead><tr>
                        <c:forEach var="column" items="${headings}">
                            <th scope="col"><c:out value="${column}" /></th>
                        </c:forEach>
                    </tr></thead>
                    <tbody>
                        <c:forEach var="entry" items="${entries}">
                            <tr>
                                <c:forEach var="column" items="${headings}">
                                    <c:set var="href" value="${ManagementDisplay.target(entry, column, links, root)}" />
                                    <c:set var="text" value="${ManagementDisplay.display(entry[column])}" />
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty href}">
                                                <a href="${fn:escapeXml(href)}"><c:out value="${text}" /></a>
                                            </c:when>
                                            <c:otherwise><c:out value="${text}" /></c:otherwise>
                                        </c:choose>
                                    </td>
                                </c:forEach>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
    <c:if test="${pagination ne null and (pagination.previous() ne null or pagination.next() ne null)}">
        <nav class="pager" aria-label="${fn:escapeXml(heading)}のページ移動">
            <c:if test="${pagination.previous() ne null}">
                <a rel="prev" href="${fn:escapeXml(root += pagination.previous())}">前のページ</a>
            </c:if>
            <c:if test="${pagination.next() ne null}">
                <a rel="next" href="${fn:escapeXml(root += pagination.next())}">次のページ</a>
            </c:if>
        </nav>
    </c:if>
</section>
