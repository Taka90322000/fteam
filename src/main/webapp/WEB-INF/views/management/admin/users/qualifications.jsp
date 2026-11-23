<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<c:set var="tab" value="${model.detail()}" />
<c:set var="current" value="${tab.current()}" />
<mg:table heading="保有資格・許認可" root="${root}" rows="${tab.qualifications()}" links="${{'ID': ManagementPaths.ADMIN_USERS += '?id=' += model.id() += '&tab=qualifications&industry='}}" />

<c:if test="${tab.editable()}">
    <section class="panel form-panel">
        <h2>資格情報を保存</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="qualification">
            <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
            <div class="form-fields">
                <mg:select id="user-qualification" name="qualificationId" label="資格・許認可" choices="${tab.choices()}" value="${tab.selectedId()}" required="true" />
                <mg:input id="user-credential" name="credential" label="資格番号" value="${current['資格番号']}" maxLength="100" />
                <mg:input id="user-obtained" name="obtained" label="取得日" type="date" value="${current['取得日']}" />
                <mg:input id="user-expires" name="expires" label="有効期限" type="date" value="${current['有効期限']}" />
                <mg:select id="user-qualification-active" name="active" label="有効状態" choices="${ManagementDisplay.options('true', '有効', 'false', '無効')}" value="${current.getOrDefault('有効', true)}" required="true" />
            </div>
            <button type="submit">資格情報を保存</button>
        </form>
    </section>
</c:if>
