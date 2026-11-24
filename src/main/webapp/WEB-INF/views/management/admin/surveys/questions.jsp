<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<section class="panel">
    <h2>追加設問（設問1は総合満足度）</h2>
    <c:choose>
        <c:when test="${empty questions}">
            <p class="empty">該当するデータはありません。</p>
        </c:when>
        <c:otherwise>
            <div class="table-scroll" role="region" aria-label="追加設問" tabindex="0">
                <table aria-label="追加設問">
                    <thead>
                        <tr>
                            <th scope="col">設問番号</th>
                            <th scope="col">設問</th>
                            <th scope="col">回答形式</th>
                            <th scope="col">必須</th>
                            <th scope="col">選択肢</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="q" items="${questions}">
                            <tr>
                                <td>
                                    <c:choose>
                                        <c:when test="${survey.editable}">
                                            <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS += '?id=' += survey.surveyId += '&tab=questions&question=' += q.questionNo)}"><c:out value="${q.questionNo}" /></a>
                                        </c:when>
                                        <c:otherwise>
                                            <c:out value="${q.questionNo}" />
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${ManagementFormat.text(q.questionText)}" /></td>
                                <td><c:out value="${ManagementFormat.text(q.answerKindLabel)}" /></td>
                                <td><c:out value="${ManagementFormat.text(q.required)}" /></td>
                                <td class="multiline"><c:out value="${ManagementFormat.text(q.optionsText)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<c:if test="${survey.editable}">
    <section class="panel form-panel">
        <h2>追加設問を保存</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="question">
            <input type="hidden" name="id" value="${fn:escapeXml(survey.surveyId)}">
            <div class="form-fields">
                <mg:select id="survey-question-number" name="question" label="設問番号" choices="${questionSlots}" value="${selectedNo}" required="true" />
                <mg:textarea id="survey-question-text" name="questionText" label="設問文" value="${selectedQuestion ne null ? selectedQuestion.questionText : ''}" maxLength="300" required="true" />
                <mg:select id="survey-answer-kind" name="answerKind" label="回答形式" choices="${answerKinds}" value="${selectedQuestion ne null ? selectedQuestion.answerKind : 'SINGLE'}" required="true" />
                <mg:select id="survey-question-required" name="required" label="必須区分（自由記述は任意）" choices="${requiredChoices}" value="${selectedQuestion ne null ? selectedQuestion.required : false}" required="true" />
                <mg:textarea id="survey-options" name="options" label="選択肢（単一選択は1行に1項目）" value="${selectedQuestion ne null ? selectedQuestion.optionsText : ''}" maxLength="1700" />
            </div>
            <button type="submit">設問を保存</button>
        </form>
    </section>

    <c:if test="${selectedQuestion ne null}">
        <section class="panel form-panel">
            <h2>下書きから設問を削除</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="removeQuestion">
                <input type="hidden" name="id" value="${fn:escapeXml(survey.surveyId)}">
                <input type="hidden" name="question" value="${fn:escapeXml(selectedNo)}">
                <button type="submit">選んだ設問を削除</button>
            </form>
        </section>
    </c:if>
</c:if>
