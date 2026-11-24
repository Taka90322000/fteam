<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}" aria-current="page">資格・書類マスター</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS += '?tab=requirements')}">業種ごとの必要条件</a>
</nav>
<div class="sections">
    <section class="panel form-panel">
        <h2><c:out value="${editId eq null ? '資格・書類を追加' : '資格・書類を編集'}" /></h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="save">
            <input type="hidden" name="id" value="${fn:escapeXml(editId)}">
            <div class="form-fields">
                <mg:select id="qualification-kind" name="kind" label="種類" choices="${kinds}" value="${current.requirementKind}" required="true" />
                <mg:input id="qualification-name" name="name" label="名称" value="${current.qualificationName}" maxLength="150" required="true" />
                <mg:textarea id="qualification-guidance" name="guidance" label="説明" value="${current.guidanceText}" maxLength="8000" />
                <mg:input id="qualification-url" name="url" label="案内・様式URL" type="url" value="${current.guidanceUrl}" maxLength="1000" />
                <mg:select id="qualification-active" name="active" label="有効状態" choices="${activeChoices}" value="${current.active}" required="true" />
            </div>
            <button type="submit">保存する</button>
        </form>
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
