<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<c:set var="tab" value="${model.detail()}" />
<mg:table heading="希望業種" root="${root}" rows="${tab.industries()}" links="${{'ID': ManagementPaths.ADMIN_INDUSTRIES += '?kind=type&id='}}" />

<c:if test="${tab.editable()}">
    <section class="panel form-panel">
        <h2>希望業種を変更</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="preferences">
            <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
            <div class="form-fields">
                <mg:select id="preferences-industries" name="industries" label="希望業種（複数選択）" choices="${tab.choices()}" value="${tab.selectedIds()}" multiple="true" />
            </div>
            <button type="submit">業種を保存</button>
        </form>
    </section>
</c:if>
