<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES += '?kind=type')}"<c:if test="${not isCategory}"> aria-current="page"</c:if>>業種</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES += '?kind=category')}"<c:if test="${isCategory}"> aria-current="page"</c:if>>業種カテゴリ</a>
</nav>
<div class="sections">
    <c:if test="${editId ne null}">
        <section class="panel">
            <h2>登録済みコード</h2>
            <dl class="details">
                <dt>ID</dt>
                <dd><c:out value="${editId}" /></dd>

                <dt>コード</dt>
                <dd><c:out value="${ManagementFormat.text(current.code)}" /></dd>
            </dl>
        </section>
    </c:if>

    <section class="panel form-panel">
        <h2><c:out value="${editId eq null ? '新規登録' : '内容を編集'}" /></h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="save">
            <input type="hidden" name="kind" value="${fn:escapeXml(kind)}">
            <input type="hidden" name="id" value="${fn:escapeXml(editId)}">
            <div class="form-fields">
                <c:if test="${editId eq null}">
                    <c:choose>
                        <c:when test="${isCategory}">
                            <mg:input id="industry-code" name="code" label="カテゴリコード（英数字3文字）" maxLength="3" required="true" />
                        </c:when>
                        <c:otherwise>
                            <mg:input id="industry-code" name="code" label="業種コード（I＋数字4桁）" maxLength="5" required="true" />
                        </c:otherwise>
                    </c:choose>
                </c:if>
                <c:if test="${not isCategory}">
                    <mg:select id="industry-parent" name="categoryId" label="所属カテゴリ" choices="${categories}" value="${current.categoryId}" required="true" />
                </c:if>
                <mg:input id="industry-name" name="name" label="名称" value="${current.name}" maxLength="${isCategory ? 50 : 80}" required="true" />
                <mg:input id="industry-order" name="order" label="表示順" type="number" value="${current.displayOrder}" required="true" />
                <mg:select id="industry-active" name="active" label="有効状態" choices="${activeChoices}" value="${current.active}" required="true" />
            </div>
            <button type="submit">保存する</button>
        </form>
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
