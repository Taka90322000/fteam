<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_REPORTS += '?status=OPEN')}">未対応の通報</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INQUIRIES += '?status=WAITING_ADMIN')}">返信が必要な問い合わせ</a>
</nav>
<div class="sections">
    <section class="panel">
        <h2>対応状況</h2>
        <dl class="details">
            <dt>未対応の通報</dt>
            <dd><c:out value="${summary.openReports}" /></dd>
            <dt>確認中の通報</dt>
            <dd><c:out value="${summary.reviewingReports}" /></dd>
            <dt>返信待ちの問い合わせ</dt>
            <dd><c:out value="${summary.waitingAdminInquiries}" /></dd>
            <dt>対応中の問い合わせ</dt>
            <dd><c:out value="${summary.inProgressInquiries}" /></dd>
        </dl>
    </section>

    <section class="panel">
        <h2>運用状況</h2>
        <dl class="details">
            <dt>利用中の会員</dt>
            <dd><c:out value="${summary.activeUsers}" /></dd>
            <dt>掲載中の物件</dt>
            <dd><c:out value="${summary.visibleProperties}" /></dd>
            <dt>受付中のアンケート</dt>
            <dd><c:out value="${summary.openSurveys}" /></dd>
            <dt>配布期間中のクーポン</dt>
            <dd><c:out value="${summary.activeCoupons}" /></dd>
        </dl>
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
