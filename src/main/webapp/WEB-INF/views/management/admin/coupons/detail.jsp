<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<c:set var="coupon" value="${model.coupon()}" />
<c:set var="detailPath" value="${ManagementPaths.ADMIN_COUPONS += '?id=' += model.id()}" />
<c:set var="recipients" value="${model.history() ne null}" />
<c:set var="active" value="${'ACTIVE' eq model.state()}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_COUPONS)}">クーポン一覧へ</a>
    <a href="${fn:escapeXml(root += detailPath)}"<c:if test="${not recipients}"> aria-current="page"</c:if>>配布設定</a>
    <a href="${fn:escapeXml(root += detailPath += '&tab=recipients')}"<c:if test="${recipients}"> aria-current="page"</c:if>>発行・利用履歴</a>
</nav>
<div class="sections">
    <mg:details heading="クーポン情報" root="${root}" record="${coupon}" links="${{'契約ID': ManagementPaths.ADMIN_TENANCIES += '?id=', '事業主ID': ManagementPaths.ADMIN_USERS += '?id=', 'アンケートID': ManagementPaths.ADMIN_SURVEYS += '?id='}}" />

    <c:choose>
        <c:when test="${recipients}">
            <c:set var="history" value="${model.history()}" />
            <mg:table heading="発行・利用履歴" root="${root}" pagination="${history.entries()}" links="${{'会員ID': detailPath += '&tab=recipients&recipientId=', '利用処理者ID': ManagementPaths.ADMIN_USERS += '?id='}}" />

            <c:if test="${history.selectedId() ne null}">
                <mg:details heading="選択中の発行記録" root="${root}" record="${history.selected()}" links="${{'会員ID': ManagementPaths.ADMIN_USERS += '?id='}}" />

                <c:if test="${history.canRevoke()}">
                    <section class="panel form-panel">
                        <h2>この発行分を無効化</h2>
                        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_COUPONS)}" method="post" accept-charset="UTF-8">
                            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                            <input type="hidden" name="operation" value="revoke">
                            <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
                            <input type="hidden" name="recipientId" value="${fn:escapeXml(history.selectedId())}">
                            <div class="form-fields">
                                <mg:textarea id="coupon-revoke-reason" name="reason" label="無効化理由" maxLength="2000" required="true" />
                            </div>
                            <button type="submit"><c:out value="${'「' += history.selected()['受取人'] += '」への発行分を無効化'}" /></button>
                        </form>
                    </section>
                </c:if>
            </c:if>
        </c:when>
        <c:otherwise>
            <c:if test="${model.editable()}">
                <section class="panel form-panel">
                    <h2>下書きの編集</h2>
                    <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_COUPONS)}" method="post" accept-charset="UTF-8">
                        <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                        <input type="hidden" name="operation" value="edit">
                        <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
                        <div class="form-fields">
                            <mg:input id="coupon-title" name="title" label="タイトル" value="${coupon['名前']}" maxLength="200" required="true" />
                            <mg:select id="coupon-benefit" name="benefit" label="特典の種類" choices="${model.benefits()}" value="${model.benefit()}" required="true" />
                            <mg:input id="coupon-percent" name="percent" label="割引率（割合割引を選んだ場合）" value="${coupon['割引率（%）']}" maxLength="6" />
                            <mg:input id="coupon-amount" name="amount" label="割引額（定額割引を選んだ場合）" type="number" value="${coupon['割引額（円）']}" />
                            <mg:input id="coupon-service" name="service" label="サービス内容（サービスを選んだ場合）" value="${coupon['サービス内容']}" maxLength="200" />
                            <mg:input id="coupon-starts" name="starts" label="配布開始（日本時間）" type="datetime-local" value="${coupon['配布開始']}" required="true" />
                            <mg:input id="coupon-ends" name="ends" label="配布終了（日本時間）" type="datetime-local" value="${coupon['配布終了']}" required="true" />
                            <mg:input id="coupon-until" name="validUntil" label="利用期限（日本時間）" type="datetime-local" value="${coupon['利用期限']}" required="true" />
                            <mg:input id="coupon-limit" name="limit" label="発行上限" type="number" value="${coupon['発行上限']}" required="true" />
                            <mg:textarea id="coupon-terms" name="terms" label="利用条件" value="${coupon['利用条件']}" maxLength="4000" />
                        </div>
                        <button type="submit">変更を保存</button>
                    </form>
                </section>
            </c:if>

            <c:if test="${model.state() ne 'DRAFT'}">
                <section class="panel form-panel">
                    <h2><c:out value="${active ? '配布を停止' : '配布を再開'}" /></h2>
                    <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_COUPONS)}" method="post" accept-charset="UTF-8">
                        <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                        <input type="hidden" name="operation" value="${fn:escapeXml(active ? 'stop' : 'resume')}">
                        <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
                        <div class="form-fields">
                            <mg:textarea id="coupon-distribution-reason" name="reason" label="理由" maxLength="2000" required="true" />
                        </div>
                        <button type="submit"><c:out value="${active ? '配布を停止する' : '配布を再開する'}" /></button>
                    </form>
                </section>
            </c:if>
        </c:otherwise>
    </c:choose>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
