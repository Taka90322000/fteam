<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="value" type="java.lang.Object" %>
<%@ attribute name="required" type="java.lang.Boolean" %>
<%@ attribute name="maxLength" type="java.lang.Integer" %>
<div class="field field-wide">
    <label for="${fn:escapeXml(id)}"><c:out value="${label}" /><c:if test="${required}"> <small>必須</small></c:if></label>
    <textarea id="${fn:escapeXml(id)}" name="${fn:escapeXml(name)}" rows="4"
              <c:if test="${required}"> required</c:if>
              <c:if test="${maxLength ne null}"> maxlength="${maxLength}"</c:if>><c:out value="${value}" /></textarea>
</div>
