<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<c:set var="detailPath" value="${ManagementPaths.ADMIN_SURVEYS += '?id=' += model.id()}" />
<c:set var="tabs" value="${ManagementDisplay.options('overview', 'アンケート情報', 'questions', '設問・選択肢', 'responses', '回答・集計')}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}">アンケート一覧へ</a>
    <c:forEach var="tab" items="${tabs}">
        <a href="${fn:escapeXml(root += detailPath += '&tab=' += tab.key)}"<c:if test="${tab.key eq model.tab()}"> aria-current="page"</c:if>><c:out value="${tab.value}" /></a>
    </c:forEach>
</nav>
<div class="sections">
    <mg:details heading="アンケート情報" root="${root}" record="${model.survey()}"
      links="${{
          '契約ID': ManagementPaths.ADMIN_TENANCIES += '?id=',
          '事業主ID': ManagementPaths.ADMIN_USERS += '?id=',
          'テンプレートID': ManagementPaths.ADMIN_SURVEYS += '?id=',
          '改訂元ID': ManagementPaths.ADMIN_SURVEYS += '?id=',
          '対象業種ID': ManagementPaths.ADMIN_INDUSTRIES += '?kind=type&id='
      }}" />

    <c:choose>
        <c:when test="${'overview' eq model.tab()}">
            <jsp:include page="/WEB-INF/views/management/admin/surveys/overview.jsp" />
        </c:when>
        <c:when test="${'questions' eq model.tab()}">
            <jsp:include page="/WEB-INF/views/management/admin/surveys/questions.jsp" />
        </c:when>
        <c:otherwise>
            <jsp:include page="/WEB-INF/views/management/admin/surveys/responses.jsp" />
        </c:otherwise>
    </c:choose>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
