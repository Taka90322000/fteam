<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" import="jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="type" %>
<%@ attribute name="value" type="java.lang.Object" %>
<%@ attribute name="required" type="java.lang.Boolean" %>
<%@ attribute name="maxLength" type="java.lang.Integer" %>
<%-- 日時の初期値は、ブラウザーの入力形式に合わせてから属性に出力する。 --%>
<c:set var="inputType" value="${type eq null ? 'text' : type}" />
<c:set var="initial" value="${inputType eq 'datetime-local' ? ManagementFormat.dateTimeInput(value) : value}" />
<div class="field">
    <label for="${fn:escapeXml(id)}"><c:out value="${label}" /><c:if test="${required}"> <small>必須</small></c:if></label>
    <input id="${fn:escapeXml(id)}" name="${fn:escapeXml(name)}" type="${fn:escapeXml(inputType)}"
           value="${fn:escapeXml(initial)}"
           <c:if test="${required}"> required</c:if>
           <c:if test="${maxLength ne null}"> maxlength="${maxLength}"</c:if>
           <c:if test="${inputType eq 'datetime-local'}"> step="any"</c:if>>
</div>
