<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- 管理画面の左メニュー。現在のAction URLと一致する項目を選択中として表示する。 --%>
<c:set var="root" value="${pageContext.request.contextPath}" />
<c:set var="route" value="${requestScope.managementRoute}" />
<nav class="sidebar" aria-label="管理メニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN)}"<c:if test="${route eq ManagementPaths.ADMIN}"> aria-current="page"</c:if>>ダッシュボード</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS)}"<c:if test="${route eq ManagementPaths.ADMIN_USERS}"> aria-current="page"</c:if>>ユーザー管理</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES)}"<c:if test="${route eq ManagementPaths.ADMIN_INDUSTRIES}"> aria-current="page"</c:if>>業種管理</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}"<c:if test="${route eq ManagementPaths.ADMIN_QUALIFICATIONS}"> aria-current="page"</c:if>>必要資格・書類管理</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES)}"<c:if test="${route eq ManagementPaths.ADMIN_PROPERTIES}"> aria-current="page"</c:if>>物件管理</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}"<c:if test="${route eq ManagementPaths.ADMIN_SURVEYS}"> aria-current="page"</c:if>>アンケート管理</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_COUPONS)}"<c:if test="${route eq ManagementPaths.ADMIN_COUPONS}"> aria-current="page"</c:if>>クーポン管理</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_REPORTS)}"<c:if test="${route eq ManagementPaths.ADMIN_REPORTS}"> aria-current="page"</c:if>>通報管理</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INQUIRIES)}"<c:if test="${route eq ManagementPaths.ADMIN_INQUIRIES}"> aria-current="page"</c:if>>問い合わせ管理</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_AUDIT)}"<c:if test="${route eq ManagementPaths.ADMIN_AUDIT}"> aria-current="page"</c:if>>操作履歴</a>
</nav>
