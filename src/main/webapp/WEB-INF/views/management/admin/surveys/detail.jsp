<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<c:set var="detailPath" value="${ManagementPaths.ADMIN_SURVEYS += '?id=' += survey.surveyId}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}">アンケート一覧へ</a>
    <c:forEach var="entry" items="${tabs}">
        <a href="${fn:escapeXml(root += detailPath += '&tab=' += entry.key)}"<c:if test="${entry.key eq tab}"> aria-current="page"</c:if>><c:out value="${entry.value}" /></a>
    </c:forEach>
</nav>
<div class="sections">
    <section class="panel">
        <h2>アンケート情報</h2>
        <dl class="details">
            <dt>アンケート番号</dt>
            <dd><c:out value="${survey.surveyId}" /></dd>

            <dt>名前</dt>
            <dd><c:out value="${ManagementFormat.text(survey.surveyTitle)}" /></dd>

            <dt>種類</dt>
            <dd><c:out value="${ManagementFormat.text(survey.surveyKindLabel)}" /></dd>

            <dt>受付状態</dt>
            <dd><c:out value="${ManagementFormat.text(survey.surveyStatusLabel)}" /></dd>

            <dt>対象業種ID</dt>
            <dd><c:choose><c:when test="${survey.industryTypeId ne null}"><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES += '?kind=type&id=' += survey.industryTypeId)}"><c:out value="${survey.industryTypeId}" /></a></c:when><c:otherwise>—</c:otherwise></c:choose></dd>

            <dt>対象業種</dt>
            <dd><c:out value="${ManagementFormat.text(survey.industryName)}" /></dd>

            <dt>テンプレートID</dt>
            <dd><c:choose><c:when test="${survey.sourceTemplateId ne null}"><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS += '?id=' += survey.sourceTemplateId)}"><c:out value="${survey.sourceTemplateId}" /></a></c:when><c:otherwise>—</c:otherwise></c:choose></dd>

            <dt>改訂元ID</dt>
            <dd><c:choose><c:when test="${survey.previousSurveyId ne null}"><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS += '?id=' += survey.previousSurveyId)}"><c:out value="${survey.previousSurveyId}" /></a></c:when><c:otherwise>—</c:otherwise></c:choose></dd>

            <dt>契約ID</dt>
            <dd><c:choose><c:when test="${survey.tenancyId ne null}"><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_TENANCIES += '?id=' += survey.tenancyId)}"><c:out value="${survey.tenancyId}" /></a></c:when><c:otherwise>—</c:otherwise></c:choose></dd>

            <dt>店舗</dt>
            <dd><c:out value="${ManagementFormat.text(survey.storeName)}" /></dd>

            <dt>事業主ID</dt>
            <dd><c:choose><c:when test="${survey.businessUserId ne null}"><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += survey.businessUserId)}"><c:out value="${survey.businessUserId}" /></a></c:when><c:otherwise>—</c:otherwise></c:choose></dd>

            <dt>受付開始</dt>
            <dd><c:out value="${ManagementFormat.text(survey.startsAt)}" /></dd>

            <dt>受付終了</dt>
            <dd><c:out value="${ManagementFormat.text(survey.endsAt)}" /></dd>

            <dt>回答数</dt>
            <dd><c:out value="${survey.responseCount}" /></dd>

            <dt>平均満足度</dt>
            <dd><c:out value="${ManagementFormat.text(survey.averageRating)}" /></dd>
        </dl>
    </section>

    <c:choose>
        <c:when test="${'overview' eq tab}">
            <jsp:include page="/WEB-INF/views/management/admin/surveys/overview.jsp" />
        </c:when>
        <c:when test="${'questions' eq tab}">
            <jsp:include page="/WEB-INF/views/management/admin/surveys/questions.jsp" />
        </c:when>
        <c:otherwise>
            <jsp:include page="/WEB-INF/views/management/admin/surveys/responses.jsp" />
        </c:otherwise>
    </c:choose>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
