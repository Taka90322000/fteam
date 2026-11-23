<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<c:set var="category" value="${'category' eq model.kind()}" />
<c:set var="current" value="${model.current()}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES += '?kind=type')}"<c:if test="${not category}"> aria-current="page"</c:if>>業種</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES += '?kind=category')}"<c:if test="${category}"> aria-current="page"</c:if>>業種カテゴリ</a>
</nav>
<div class="sections">
    <c:if test="${model.id() ne null}">
        <mg:details heading="登録済みコード" root="${root}" record="${{'ID': model.id(), 'コード': current['code']}}" columns="ID|コード" />
    </c:if>

    <section class="panel form-panel">
        <h2><c:out value="${model.id() eq null ? '新規登録' : '内容を編集'}" /></h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="save">
            <input type="hidden" name="kind" value="${fn:escapeXml(model.kind())}">
            <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
            <div class="form-fields">
                <c:if test="${model.id() eq null}">
                    <c:choose>
                        <c:when test="${category}">
                            <mg:input id="industry-code" name="code" label="カテゴリコード（英数字3文字）" maxLength="3" required="true" />
                        </c:when>
                        <c:otherwise>
                            <mg:input id="industry-code" name="code" label="業種コード（I＋数字4桁）" maxLength="5" required="true" />
                        </c:otherwise>
                    </c:choose>
                </c:if>
                <c:if test="${not category}">
                    <mg:select id="industry-parent" name="categoryId" label="所属カテゴリ" choices="${model.categories()}" value="${current['category_id']}" required="true" />
                </c:if>
                <mg:input id="industry-name" name="name" label="名称" value="${current['name']}" maxLength="${category ? 50 : 80}" required="true" />
                <mg:input id="industry-order" name="order" label="表示順" type="number" value="${current.getOrDefault('display_order', 1)}" required="true" />
                <mg:select id="industry-active" name="active" label="有効状態" choices="${ManagementDisplay.options('true', '有効', 'false', '無効')}" value="${current.getOrDefault('is_active', true)}" required="true" />
            </div>
            <button type="submit">保存する</button>
        </form>
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
