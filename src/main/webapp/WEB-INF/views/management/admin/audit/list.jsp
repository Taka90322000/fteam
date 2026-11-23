<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel form-panel">
        <h2>絞り込み</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_AUDIT)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="audit-query" name="q" label="操作・対象・操作者名・理由" type="search" value="${model.query()}" maxLength="100" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <mg:table heading="管理者が行った操作" root="${root}" pagination="${model.entries()}" links="${{'操作者ID': ManagementPaths.ADMIN_USERS += '?id='}}" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
