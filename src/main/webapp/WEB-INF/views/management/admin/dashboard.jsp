<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_REPORTS += '?status=OPEN')}">未対応の通報</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INQUIRIES += '?status=WAITING_ADMIN')}">返信が必要な問い合わせ</a>
</nav>
<div class="sections">
    <mg:details heading="対応状況" root="${root}" record="${model.support()}" columns="未対応の通報|確認中の通報|返信待ちの問い合わせ|対応中の問い合わせ" />

    <mg:details heading="運用状況" root="${root}" record="${model.operations()}" columns="利用中の会員|掲載中の物件|受付中のアンケート|配布期間中のクーポン" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
