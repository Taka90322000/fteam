<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel form-panel">
        <h2>物件を探す</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="admin-property-query" name="q" label="物件名・所在地・オーナー" type="search" value="${model.query()}" maxLength="100" />
                <mg:select id="admin-property-state" name="status" label="募集・契約状態" choices="${model.states()}" value="${model.status()}" placeholder="すべての状態" />
                <mg:select id="admin-property-visibility" name="kind" label="管理者の掲載状態" choices="${ManagementDisplay.options('HIDDEN', '掲載停止中', 'NORMAL', '通常')}" value="${model.visibility()}" placeholder="すべて" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <mg:table heading="物件一覧" root="${root}" pagination="${model.properties()}" links="${{'ID': ManagementPaths.ADMIN_PROPERTIES += '?id='}}" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
