<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<c:if test="${survey.editable}">
    <section class="panel form-panel">
        <h2><c:out value="${survey.template ? 'テンプレートの編集' : '下書きの編集'}" /></h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="edit">
            <input type="hidden" name="id" value="${fn:escapeXml(survey.surveyId)}">
            <div class="form-fields">
                <mg:input id="survey-title" name="title" label="タイトル" value="${survey.surveyTitle}" maxLength="200" required="true" />
                <c:choose>
                    <c:when test="${survey.template}">
                        <mg:select id="survey-edit-industry" name="industry" label="対象業種" choices="${industries}" value="${survey.industryTypeId}" required="true" />
                    </c:when>
                    <c:otherwise>
                        <mg:input id="survey-starts" name="starts" label="受付開始（日本時間）" type="datetime-local" value="${survey.startsAt}" />
                        <mg:input id="survey-ends" name="ends" label="受付終了（日本時間）" type="datetime-local" value="${survey.endsAt}" />
                    </c:otherwise>
                </c:choose>
            </div>
            <button type="submit">変更を保存</button>
        </form>
    </section>
</c:if>

<c:if test="${survey.canClose}">
    <section class="panel form-panel">
        <h2>回答受付を停止</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="close">
            <input type="hidden" name="id" value="${fn:escapeXml(survey.surveyId)}">
            <div class="form-fields">
                <mg:textarea id="survey-close-reason" name="reason" label="停止理由" maxLength="2000" required="true" />
            </div>
            <button type="submit">受付を停止する</button>
        </form>
    </section>
</c:if>

<c:if test="${survey.canRevise}">
    <section class="panel form-panel">
        <h2>設問を改訂</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="revise">
            <input type="hidden" name="id" value="${fn:escapeXml(survey.surveyId)}">
            <button type="submit">改訂版の下書きを作る</button>
        </form>
    </section>
</c:if>
