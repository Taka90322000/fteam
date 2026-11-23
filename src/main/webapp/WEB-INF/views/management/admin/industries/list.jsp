<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<c:set var="category" value="${'category' eq model.kind()}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES += '?kind=type')}"<c:if test="${not category}"> aria-current="page"</c:if>>業種</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES += '?kind=category')}"<c:if test="${category}"> aria-current="page"</c:if>>業種カテゴリ</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES += '?kind=' += model.kind() += '&new=1')}"><c:out value="${category ? 'カテゴリを追加' : '業種を追加'}" /></a>
</nav>
<div class="sections">
    <section class="panel form-panel">
        <h2>絞り込み</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES)}" method="get" accept-charset="UTF-8">
            <input type="hidden" name="kind" value="${fn:escapeXml(model.kind())}">
            <div class="form-fields">
                <mg:input id="industry-query" name="q" label="名称・コード" type="search" value="${model.query()}" maxLength="100" />
                <mg:select id="industry-status" name="status" label="有効状態" choices="${ManagementDisplay.options('true', '有効', 'false', '無効')}" value="${model.status()}" placeholder="すべて" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <mg:table heading="${category ? '業種カテゴリ一覧' : '業種一覧'}" root="${root}" pagination="${model.industries()}" links="${{'ID': ManagementPaths.ADMIN_INDUSTRIES += '?kind=' += model.kind() += '&id='}}" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
