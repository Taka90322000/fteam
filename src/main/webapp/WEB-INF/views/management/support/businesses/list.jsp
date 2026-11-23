<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel form-panel">
        <h2>事業主を探す</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.BUSINESSES)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="business-query" name="q" label="事業者名" type="search" value="${model.query()}" maxLength="100" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <mg:table heading="事業主" root="${root}" pagination="${model.entries()}" links="${{'ID': ManagementPaths.BUSINESSES += '?id='}}" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
