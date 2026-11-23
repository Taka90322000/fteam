<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <c:forEach var="role" items="${model.roles()}">
        <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?role=' += role.key)}"<c:if test="${role.key eq model.role()}"> aria-current="page"</c:if>><c:out value="${role.value}" /></a>
    </c:forEach>
</nav>
<div class="sections">
    <section class="panel form-panel">
        <h2>会員を探す</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS)}" method="get" accept-charset="UTF-8">
            <input type="hidden" name="role" value="${fn:escapeXml(model.role())}">
            <div class="form-fields">
                <mg:input id="users-query" name="q" label="名前・会員ID・メール・事業者名" type="search" value="${model.query()}" maxLength="100" />
                <mg:select id="users-status" name="status" label="利用状態" choices="${model.states()}" value="${model.status()}" placeholder="すべての状態" />
                <mg:input id="users-from" name="from" label="登録日（開始）" type="date" value="${model.from()}" />
                <mg:input id="users-to" name="to" label="登録日（終了）" type="date" value="${model.to()}" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <mg:table heading="${model.roles()[model.role()] += '一覧'}" root="${root}" pagination="${model.members()}" columns="ID|名前|事業者名・法人名|メール|利用状態|登録日" links="${{'ID': ManagementPaths.ADMIN_USERS += '?id='}}" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
