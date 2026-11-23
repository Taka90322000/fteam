<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel form-panel">
        <h2>絞り込み</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_COUPONS)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="coupon-query" name="q" label="クーポン名・店舗名" type="search" value="${model.query()}" maxLength="100" />
                <mg:select id="coupon-state" name="status" label="配布状態" choices="${model.states()}" value="${model.state()}" placeholder="すべての状態" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <mg:table heading="クーポン一覧" root="${root}" pagination="${model.coupons()}" links="${{'ID': ManagementPaths.ADMIN_COUPONS += '?id='}}" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
