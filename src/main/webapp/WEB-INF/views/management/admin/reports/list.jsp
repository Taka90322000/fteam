<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel form-panel">
        <h2>絞り込み</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_REPORTS)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="report-query" name="q" label="対象名・受付番号" type="search" value="${model.query()}" maxLength="100" />
                <mg:select id="report-kind" name="kind" label="対象区分" choices="${model.kinds()}" value="${model.kind()}" placeholder="すべての対象" />
                <mg:select id="report-status" name="status" label="対応状態" choices="${model.states()}" value="${model.state()}" placeholder="すべての状態" />
                <mg:select id="report-reason" name="reasonCode" label="理由" choices="${model.reasons()}" value="${model.reason()}" placeholder="すべての理由" />
                <mg:input id="report-from" name="from" label="受付日（開始）" type="date" value="${model.from()}" />
                <mg:input id="report-to" name="to" label="受付日（終了）" type="date" value="${model.to()}" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <mg:table heading="通報一覧" root="${root}" pagination="${model.reports()}" links="${{'ID': ManagementPaths.ADMIN_REPORTS += '?id='}}" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
