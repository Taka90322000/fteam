<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <mg:details heading="通報対象" root="${root}" record="${model.target()}" />

    <section class="panel form-panel">
        <h2>通報の内容</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.REPORT)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="send">
            <input type="hidden" name="kind" value="${fn:escapeXml(model.kind())}">
            <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
            <input type="hidden" name="propertyId" value="${fn:escapeXml(model.propertyId())}">
            <div class="form-fields">
                <mg:select id="member-report-reason" name="reasonCode" label="理由" choices="${model.reasons()}" placeholder="理由を選択" required="true" />
                <mg:textarea id="member-report-body" name="body" label="詳細" maxLength="8000" required="true" />
            </div>
            <button type="submit">通報を送信</button>
        </form>
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
