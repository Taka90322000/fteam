<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<c:set var="detailPath" value="${ManagementPaths.ADMIN_COUPONS += '?id=' += coupon.couponId}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_COUPONS)}">クーポン一覧へ</a>
    <a href="${fn:escapeXml(root += detailPath)}"<c:if test="${not showRecipients}"> aria-current="page"</c:if>>配布設定</a>
    <a href="${fn:escapeXml(root += detailPath += '&tab=recipients')}"<c:if test="${showRecipients}"> aria-current="page"</c:if>>発行・利用履歴</a>
</nav>
<div class="sections">
    <section class="panel">
        <h2>クーポン情報</h2>
        <dl class="details">
            <dt>クーポン番号</dt>
            <dd><c:out value="${coupon.couponId}" /></dd>

            <dt>名前</dt>
            <dd><c:out value="${ManagementFormat.text(coupon.couponTitle)}" /></dd>

            <dt>店舗</dt>
            <dd><c:out value="${ManagementFormat.text(coupon.storeName)}" /></dd>

            <dt>契約ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_TENANCIES += '?id=' += coupon.tenancyId)}"><c:out value="${coupon.tenancyId}" /></a></dd>

            <dt>事業主ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += coupon.businessUserId)}"><c:out value="${coupon.businessUserId}" /></a></dd>

            <dt>配布状態</dt>
            <dd><c:out value="${ManagementFormat.text(coupon.couponStatusLabel)}" /></dd>

            <dt>特典</dt>
            <dd><c:out value="${ManagementFormat.text(coupon.benefitKindLabel)}" /></dd>

            <dt>割引率（%）</dt>
            <dd><c:out value="${ManagementFormat.text(coupon.discountPercent)}" /></dd>

            <dt>割引額（円）</dt>
            <dd><c:out value="${ManagementFormat.text(coupon.discountYen)}" /></dd>

            <dt>サービス内容</dt>
            <dd><c:out value="${ManagementFormat.text(coupon.serviceText)}" /></dd>

            <dt>配布対象</dt>
            <dd><c:out value="${ManagementFormat.text(coupon.targetKindLabel)}" /></dd>

            <dt>対象業種</dt>
            <dd><c:out value="${ManagementFormat.text(coupon.targetIndustryName)}" /></dd>

            <dt>アンケートID</dt>
            <dd><c:choose><c:when test="${coupon.targetSurveyId ne null}"><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS += '?id=' += coupon.targetSurveyId)}"><c:out value="${coupon.targetSurveyId}" /></a></c:when><c:otherwise>—</c:otherwise></c:choose></dd>

            <dt>配布開始</dt>
            <dd><c:out value="${ManagementFormat.text(coupon.distributionStartsAt)}" /></dd>

            <dt>配布終了</dt>
            <dd><c:out value="${ManagementFormat.text(coupon.distributionEndsAt)}" /></dd>

            <dt>利用期限</dt>
            <dd><c:out value="${ManagementFormat.text(coupon.validUntil)}" /></dd>

            <dt>発行上限</dt>
            <dd><c:out value="${coupon.usageLimit}" /></dd>

            <dt>利用条件</dt>
            <dd class="multiline"><c:out value="${ManagementFormat.text(coupon.termsText)}" /></dd>

            <dt>発行数</dt>
            <dd><c:out value="${coupon.issuedCount}" /></dd>

            <dt>利用数</dt>
            <dd><c:out value="${coupon.usedCount}" /></dd>

            <dt>無効化数</dt>
            <dd><c:out value="${coupon.revokedCount}" /></dd>
        </dl>
    </section>

    <c:choose>
        <c:when test="${showRecipients}">
            <section class="panel">
                <h2>発行・利用履歴</h2>
                <c:choose>
                    <c:when test="${empty recipientPage.items}">
                        <p class="empty">該当するデータはありません。</p>
                    </c:when>
                    <c:otherwise>
                        <div class="table-scroll" role="region" aria-label="発行・利用履歴" tabindex="0">
                            <table aria-label="発行・利用履歴">
                                <thead>
                                    <tr>
                                        <th scope="col">会員ID</th>
                                        <th scope="col">受取人</th>
                                        <th scope="col">発行日時</th>
                                        <th scope="col">利用日時</th>
                                        <th scope="col">無効化日時</th>
                                        <th scope="col">利用処理者ID</th>
                                        <th scope="col">利用状態</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="rec" items="${recipientPage.items}">
                                        <tr>
                                            <td><a href="${fn:escapeXml(root += detailPath += '&tab=recipients&recipientId=' += rec.residentUserId)}"><c:out value="${rec.residentUserId}" /></a></td>
                                            <td><c:out value="${ManagementFormat.text(rec.residentName)}" /></td>
                                            <td><c:out value="${ManagementFormat.text(rec.issuedAt)}" /></td>
                                            <td><c:out value="${ManagementFormat.text(rec.usedAt)}" /></td>
                                            <td><c:out value="${ManagementFormat.text(rec.revokedAt)}" /></td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${rec.redeemedByUserId ne null}">
                                                        <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += rec.redeemedByUserId)}"><c:out value="${rec.redeemedByUserId}" /></a>
                                                    </c:when>
                                                    <c:otherwise>—</c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td><c:out value="${ManagementFormat.text(rec.usageStateLabel)}" /></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
                <mg:pager page="${recipientPage}" root="${root}" label="発行・利用履歴" />
            </section>

            <c:if test="${selectedRecipient ne null}">
                <section class="panel">
                    <h2>選択中の発行記録</h2>
                    <dl class="details">
                        <dt>会員ID</dt>
                        <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += selectedRecipient.residentUserId)}"><c:out value="${selectedRecipient.residentUserId}" /></a></dd>

                        <dt>受取人</dt>
                        <dd><c:out value="${ManagementFormat.text(selectedRecipient.residentName)}" /></dd>

                        <dt>発行日時</dt>
                        <dd><c:out value="${ManagementFormat.text(selectedRecipient.issuedAt)}" /></dd>

                        <dt>利用日時</dt>
                        <dd><c:out value="${ManagementFormat.text(selectedRecipient.usedAt)}" /></dd>

                        <dt>無効化日時</dt>
                        <dd><c:out value="${ManagementFormat.text(selectedRecipient.revokedAt)}" /></dd>

                        <dt>有効期間内</dt>
                        <dd><c:out value="${ManagementFormat.text(selectedRecipient.withinValidity)}" /></dd>
                    </dl>
                </section>

                <c:if test="${selectedRecipient.canRevoke}">
                    <section class="panel form-panel">
                        <h2>この発行分を無効化</h2>
                        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_COUPONS)}" method="post" accept-charset="UTF-8">
                            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                            <input type="hidden" name="operation" value="revoke">
                            <input type="hidden" name="id" value="${fn:escapeXml(coupon.couponId)}">
                            <input type="hidden" name="recipientId" value="${fn:escapeXml(selectedRecipient.residentUserId)}">
                            <div class="form-fields">
                                <mg:textarea id="coupon-revoke-reason" name="reason" label="無効化理由" maxLength="2000" required="true" />
                            </div>
                            <button type="submit"><c:out value="${'「' += selectedRecipient.residentName += '」への発行分を無効化'}" /></button>
                        </form>
                    </section>
                </c:if>
            </c:if>
        </c:when>
        <c:otherwise>
            <c:if test="${coupon.editable}">
                <section class="panel form-panel">
                    <h2>下書きの編集</h2>
                    <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_COUPONS)}" method="post" accept-charset="UTF-8">
                        <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                        <input type="hidden" name="operation" value="edit">
                        <input type="hidden" name="id" value="${fn:escapeXml(coupon.couponId)}">
                        <div class="form-fields">
                            <mg:input id="coupon-title" name="title" label="タイトル" value="${coupon.couponTitle}" maxLength="200" required="true" />
                            <mg:select id="coupon-benefit" name="benefit" label="特典の種類" choices="${benefits}" value="${coupon.benefitKind}" required="true" />
                            <mg:input id="coupon-percent" name="percent" label="割引率（割合割引を選んだ場合）" value="${coupon.discountPercent}" maxLength="6" />
                            <mg:input id="coupon-amount" name="amount" label="割引額（定額割引を選んだ場合）" type="number" value="${coupon.discountYen}" />
                            <mg:input id="coupon-service" name="service" label="サービス内容（サービスを選んだ場合）" value="${coupon.serviceText}" maxLength="200" />
                            <mg:input id="coupon-starts" name="starts" label="配布開始（日本時間）" type="datetime-local" value="${coupon.distributionStartsAt}" required="true" />
                            <mg:input id="coupon-ends" name="ends" label="配布終了（日本時間）" type="datetime-local" value="${coupon.distributionEndsAt}" required="true" />
                            <mg:input id="coupon-until" name="validUntil" label="利用期限（日本時間）" type="datetime-local" value="${coupon.validUntil}" required="true" />
                            <mg:input id="coupon-limit" name="limit" label="発行上限" type="number" value="${coupon.usageLimit}" required="true" />
                            <mg:textarea id="coupon-terms" name="terms" label="利用条件" value="${coupon.termsText}" maxLength="4000" />
                        </div>
                        <button type="submit">変更を保存</button>
                    </form>
                </section>
            </c:if>

            <c:if test="${coupon.couponStatus ne 'DRAFT'}">
                <section class="panel form-panel">
                    <h2><c:out value="${coupon.active ? '配布を停止' : '配布を再開'}" /></h2>
                    <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_COUPONS)}" method="post" accept-charset="UTF-8">
                        <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                        <input type="hidden" name="operation" value="${fn:escapeXml(coupon.active ? 'stop' : 'resume')}">
                        <input type="hidden" name="id" value="${fn:escapeXml(coupon.couponId)}">
                        <div class="form-fields">
                            <mg:textarea id="coupon-distribution-reason" name="reason" label="理由" maxLength="2000" required="true" />
                        </div>
                        <button type="submit"><c:out value="${coupon.active ? '配布を停止する' : '配布を再開する'}" /></button>
                    </form>
                </section>
            </c:if>
        </c:otherwise>
    </c:choose>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
