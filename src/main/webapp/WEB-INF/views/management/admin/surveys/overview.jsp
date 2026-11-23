<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<c:set var="tab" value="${model.detail()}" />
<c:set var="survey" value="${model.survey()}" />
<c:set var="template" value="${'TEMPLATE' eq model.kind()}" />
<c:if test="${model.editable()}">
    <section class="panel form-panel">
        <h2><c:out value="${template ? 'テンプレートの編集' : '下書きの編集'}" /></h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="edit">
            <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
            <div class="form-fields">
                <mg:input id="survey-title" name="title" label="タイトル" value="${survey['名前']}" maxLength="200" required="true" />
                <c:choose>
                    <c:when test="${template}">
                        <mg:select id="survey-edit-industry" name="industry" label="対象業種" choices="${tab.industries()}" value="${survey['対象業種ID']}" required="true" />
                    </c:when>
                    <c:otherwise>
                        <mg:input id="survey-starts" name="starts" label="受付開始（日本時間）" type="datetime-local" value="${survey['受付開始']}" />
                        <mg:input id="survey-ends" name="ends" label="受付終了（日本時間）" type="datetime-local" value="${survey['受付終了']}" />
                    </c:otherwise>
                </c:choose>
            </div>
            <button type="submit">変更を保存</button>
        </form>
    </section>
</c:if>

<c:if test="${tab.canClose()}">
    <section class="panel form-panel">
        <h2>回答受付を停止</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="close">
            <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
            <div class="form-fields">
                <mg:textarea id="survey-close-reason" name="reason" label="停止理由" maxLength="2000" required="true" />
            </div>
            <button type="submit">受付を停止する</button>
        </form>
    </section>
</c:if>

<c:if test="${tab.canRevise()}">
    <section class="panel form-panel">
        <h2>設問を改訂</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="revise">
            <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
            <button type="submit">改訂版の下書きを作る</button>
        </form>
    </section>
</c:if>
