<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ attribute name="page" type="jp.machiriku.bean.management.PageResult" required="true" %>
<%@ attribute name="root" required="true" %>
<%@ attribute name="label" required="true" %>
<%-- 一覧の前後ページへの移動リンクを表示する共通部品。 --%>
<c:if test="${page.hasPager}">
    <nav class="pager" aria-label="${fn:escapeXml(label)}のページ移動">
        <c:if test="${page.previousUrl ne null}">
            <a rel="prev" href="${fn:escapeXml(root += page.previousUrl)}">前のページ</a>
        </c:if>
        <c:if test="${page.nextUrl ne null}">
            <a rel="next" href="${fn:escapeXml(root += page.nextUrl)}">次のページ</a>
        </c:if>
    </nav>
</c:if>
