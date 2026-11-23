<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.INQUIRIES)}">問い合わせ一覧へ</a>
</nav>
<div class="sections">
    <mg:details heading="受付内容" root="${root}" record="${model.inquiry()}" />

    <mg:table heading="やり取り" root="${root}" pagination="${model.posts()}" columns="日時|投稿者|種類|内容" />

    <c:if test="${model.canReply()}">
        <section class="panel form-panel">
            <h2>問い合わせに返信</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.INQUIRIES)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="reply">
                <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
                <div class="form-fields">
                    <mg:textarea id="member-reply-body" name="body" label="返信内容" maxLength="8000" required="true" />
                </div>
                <button type="submit">送信する</button>
            </form>
        </section>
    </c:if>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
