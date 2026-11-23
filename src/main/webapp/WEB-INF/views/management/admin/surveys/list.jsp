<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS += '?new=1')}">テンプレートを追加</a>
</nav>
<div class="sections">
    <section class="panel form-panel">
        <h2>絞り込み</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="survey-query" name="q" label="アンケート名・店舗名" type="search" value="${model.query()}" maxLength="100" />
                <mg:select id="survey-kind" name="kind" label="種類" choices="${model.kinds()}" value="${model.kind()}" placeholder="すべての種類" />
                <mg:select id="survey-state" name="status" label="受付状態" choices="${model.states()}" value="${model.state()}" placeholder="すべての状態" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <mg:table heading="アンケート一覧" root="${root}" pagination="${model.surveys()}" links="${{'ID': ManagementPaths.ADMIN_SURVEYS += '?id='}}" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
