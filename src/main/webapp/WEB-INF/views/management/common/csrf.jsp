<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Actionがセッションに対して発行した値を、更新フォームの送信に含める。 --%>
<input type="hidden" name="csrfToken" value="${fn:escapeXml(requestScope.managementCsrf)}">
