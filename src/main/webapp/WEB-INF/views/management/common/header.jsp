<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- 管理画面共通の見出しとサイドバーを描画するヘッダー。 --%>
<c:set var="root" value="${pageContext.request.contextPath}" />
<c:set var="title" value="${requestScope.managementTitle}" />
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#1b4332">
    <title><c:out value="${title}" /> | マチリク</title>
    <link rel="stylesheet" href="${fn:escapeXml(root)}/css/management.css">
</head>
<body>
    <a class="skip-link" href="#main-content">本文へ移動</a>
    <header class="masthead">
        <a class="brand" href="${fn:escapeXml(root += ManagementPaths.ADMIN)}">
            <span class="brand-mark">マチリク</span><span>管理画面</span>
        </a>
        <a class="site-link" href="${fn:escapeXml(root)}/index.jsp">利用者サイトへ</a>
    </header>
    <div class="layout layout-admin">
        <jsp:include page="/WEB-INF/views/management/admin/common/sidebar.jsp" />
        <main id="main-content" tabindex="-1">
            <h1><c:out value="${title}" /></h1>
            <c:if test="${param.saved eq '1'}">
                <p class="notice" role="status">保存しました。</p>
            </c:if>
