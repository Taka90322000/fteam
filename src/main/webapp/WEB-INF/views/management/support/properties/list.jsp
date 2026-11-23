<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel form-panel">
        <h2>物件を探す</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.PROPERTIES)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="property-query" name="q" label="物件名・所在地" type="search" value="${model.query()}" maxLength="100" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <mg:table heading="物件一覧" root="${root}" pagination="${model.entries()}" columns="ID|名前|所在地|面積（㎡）|月額賃料（円）" links="${{'ID': ManagementPaths.PROPERTIES += '?id='}}" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
