<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.PROPERTIES)}">物件から問い合わせ先を選ぶ</a>
</nav>
<div class="sections">
    <c:if test="${model.propertyId() ne null}">
        <mg:details heading="関連物件" root="${root}" record="${model.property()}" links="${{'ID': ManagementPaths.PROPERTIES += '?id='}}" />
    </c:if>

    <section class="panel form-panel">
        <h2>新しい問い合わせ</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.INQUIRIES)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="open">
            <input type="hidden" name="propertyId" value="${fn:escapeXml(model.propertyId())}">
            <div class="form-fields">
                <mg:select id="member-inquiry-category" name="category" label="カテゴリ" choices="${model.categories()}" value="${model.propertyId() eq null ? 'ACCOUNT' : 'PROPERTY'}" required="true" />
                <mg:input id="member-inquiry-subject" name="subject" label="件名" maxLength="200" required="true" />
                <mg:textarea id="member-inquiry-body" name="body" label="本文" maxLength="8000" required="true" />
            </div>
            <button type="submit">問い合わせを送信</button>
        </form>
    </section>

    <mg:table heading="問い合わせ履歴" root="${root}" pagination="${model.inquiries()}" links="${{'ID': ManagementPaths.INQUIRIES += '?id='}}" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
