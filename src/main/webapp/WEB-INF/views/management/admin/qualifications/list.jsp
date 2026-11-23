<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}" aria-current="page">資格・書類マスター</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS += '?tab=requirements')}">業種ごとの必要条件</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS += '?new=1')}">資格・書類を追加</a>
</nav>
<div class="sections">
    <section class="panel form-panel">
        <h2>絞り込み</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="qualification-query" name="q" label="名称" type="search" value="${model.query()}" maxLength="100" />
                <mg:select id="qualification-kind" name="kind" label="種類" choices="${model.kinds()}" value="${model.kind()}" placeholder="すべての種類" />
                <mg:select id="qualification-status" name="status" label="有効状態" choices="${ManagementDisplay.options('true', '有効', 'false', '無効')}" value="${model.status()}" placeholder="すべて" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <mg:table heading="資格・書類一覧" root="${root}" pagination="${model.qualifications()}" links="${{'ID': ManagementPaths.ADMIN_QUALIFICATIONS += '?id='}}" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
