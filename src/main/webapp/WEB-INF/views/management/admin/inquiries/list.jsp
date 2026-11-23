<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel form-panel">
        <h2>絞り込み</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_INQUIRIES)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="inquiry-query" name="q" label="受付番号・送信者・件名" type="search" value="${model.query()}" maxLength="100" />
                <mg:select id="inquiry-category" name="category" label="カテゴリ" choices="${model.categories()}" value="${model.category()}" placeholder="すべてのカテゴリ" />
                <mg:select id="inquiry-state" name="status" label="対応状態" choices="${model.states()}" value="${model.state()}" placeholder="すべての状態" />
                <mg:select id="inquiry-assignee" name="assignee" label="担当者" choices="${model.admins()}" value="${model.assignee()}" placeholder="すべての担当者" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <mg:table heading="受付一覧" root="${root}" pagination="${model.inquiries()}" links="${{'ID': ManagementPaths.ADMIN_INQUIRIES += '?id='}}" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
