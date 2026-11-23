<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<c:set var="tab" value="${model.detail()}" />
<c:set var="selected" value="${tab.selected()}" />
<c:set var="slots" value="${ManagementDisplay.options(
    '2', '設問 2', '3', '設問 3', '4', '設問 4',
    '5', '設問 5', '6', '設問 6', '7', '設問 7',
    '8', '設問 8', '9', '設問 9', '10', '設問 10')}" />
<mg:table heading="追加設問（設問1は総合満足度）" root="${root}" rows="${tab.questions()}" links="${model.editable() ? {'設問番号': ManagementPaths.ADMIN_SURVEYS += '?id=' += model.id() += '&tab=questions&question='} : null}" />

<c:if test="${model.editable()}">
    <section class="panel form-panel">
        <h2>追加設問を保存</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="question">
            <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
            <div class="form-fields">
                <mg:select id="survey-question-number" name="question" label="設問番号" choices="${slots}" value="${tab.number()}" required="true" />
                <mg:textarea id="survey-question-text" name="questionText" label="設問文" value="${selected['設問']}" maxLength="300" required="true" />
                <mg:select id="survey-answer-kind" name="answerKind" label="回答形式" choices="${ManagementDisplay.options('SINGLE', '単一選択', 'TEXT', '自由記述')}" value="${selected.getOrDefault('回答形式', 'SINGLE')}" required="true" />
                <mg:select id="survey-question-required" name="required" label="必須区分（自由記述は任意）" choices="${ManagementDisplay.options('true', '必須', 'false', '任意')}" value="${selected.getOrDefault('必須', false)}" required="true" />
                <mg:textarea id="survey-options" name="options" label="選択肢（単一選択は1行に1項目）" value="${selected['選択肢']}" maxLength="1700" />
            </div>
            <button type="submit">設問を保存</button>
        </form>
    </section>

    <c:if test="${not empty selected}">
        <section class="panel form-panel">
            <h2>下書きから設問を削除</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="removeQuestion">
                <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
                <input type="hidden" name="question" value="${fn:escapeXml(tab.number())}">
                <button type="submit">選んだ設問を削除</button>
            </form>
        </section>
    </c:if>
</c:if>
