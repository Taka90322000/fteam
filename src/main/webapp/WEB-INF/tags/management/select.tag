<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" import="jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="choices" type="java.util.Map" required="true" %>
<%@ attribute name="value" type="java.lang.Object" %>
<%@ attribute name="placeholder" %>
<%@ attribute name="required" type="java.lang.Boolean" %>
<%@ attribute name="multiple" type="java.lang.Boolean" %>
<div class="field${multiple ? ' field-wide' : ''}">
    <label for="${fn:escapeXml(id)}"><c:out value="${label}" /><c:if test="${required}"> <small>必須</small></c:if></label>
    <select id="${fn:escapeXml(id)}" name="${fn:escapeXml(name)}"<c:if test="${required}"> required</c:if><c:if test="${multiple}"> multiple size="7"</c:if>>
        <c:if test="${placeholder ne null}">
            <option value=""<c:if test="${ManagementFormat.selected(value, '')}"> selected</c:if>><c:out value="${placeholder}" /></option>
        </c:if>
        <%-- 複数選択の場合は、登録済みの各候補を選択済みとする。 --%>
        <c:forEach var="option" items="${choices}">
            <option value="${fn:escapeXml(option.key)}"<c:if test="${ManagementFormat.selected(value, option.key)}"> selected</c:if>>
                <c:out value="${option.value}" />
            </option>
        </c:forEach>
    </select>
</div>
