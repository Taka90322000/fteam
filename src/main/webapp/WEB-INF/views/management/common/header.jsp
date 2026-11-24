<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Actionの表示モードに合わせて、見出しとメニューを出し分ける共通ヘッダー。 --%>
<c:set var="root" value="${pageContext.request.contextPath}" />
<c:set var="title" value="${requestScope.managementTitle}" />
<c:set var="route" value="${requestScope.managementRoute}" />
<c:set var="admin" value="${requestScope.managementAdmin}" />
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
        <a class="brand" href="${fn:escapeXml(root += (admin ? ManagementPaths.ADMIN : '/index.jsp'))}">
            <span class="brand-mark">マチリク</span><span>${admin ? '管理画面' : '宇都宮市'}</span>
        </a>
        <c:choose>
            <c:when test="${admin}">
                <a class="site-link" href="${fn:escapeXml(root)}/index.jsp">利用者サイトへ</a>
            </c:when>
            <c:otherwise>
                <nav class="public-nav" aria-label="サイト内メニュー">
                    <a href="${fn:escapeXml(root += ManagementPaths.PROPERTIES)}"<c:if test="${route eq ManagementPaths.PROPERTIES}"> aria-current="page"</c:if>>掲載物件</a>
                    <a href="${fn:escapeXml(root += ManagementPaths.REQUIREMENTS)}"<c:if test="${route eq ManagementPaths.REQUIREMENTS}"> aria-current="page"</c:if>>必要資格・書類</a>
                    <c:if test="${not empty requestScope.managementCsrf}">
                        <a href="${fn:escapeXml(root += ManagementPaths.BUSINESSES)}"<c:if test="${route eq ManagementPaths.BUSINESSES}"> aria-current="page"</c:if>>事業主一覧</a>
                    </c:if>
                    <a href="${fn:escapeXml(root += ManagementPaths.INQUIRIES)}"<c:if test="${route eq ManagementPaths.INQUIRIES}"> aria-current="page"</c:if>>お問い合わせ</a>
                </nav>
            </c:otherwise>
        </c:choose>
    </header>
    <div class="layout ${admin ? 'layout-admin' : 'layout-public'}">
        <c:if test="${admin}">
            <jsp:include page="/WEB-INF/views/management/admin/common/sidebar.jsp" />
        </c:if>
        <main id="main-content" tabindex="-1">
            <h1><c:out value="${title}" /></h1>
            <c:if test="${param.saved eq '1'}">
                <p class="notice" role="status">保存しました。</p>
            </c:if>
