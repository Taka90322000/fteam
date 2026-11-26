<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel form-panel">
        <h2>絞り込み</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_COUPONS)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="coupon-query" name="q" label="クーポン名・店舗名" type="search" value="${query}" maxLength="100" />
                <mg:select id="coupon-state" name="status" label="配布状態" choices="${states}" value="${status}" placeholder="すべての状態" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <section class="panel">
        <h2>クーポン一覧</h2>
        <c:choose>
            <c:when test="${empty couponPage.items}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="クーポン一覧" tabindex="0">
                    <table aria-label="クーポン一覧">
                        <thead>
                            <tr>
                                <th scope="col">ID</th>
                                <th scope="col">名前</th>
                                <th scope="col">店舗</th>
                                <th scope="col">配布状態</th>
                                <th scope="col">発行数</th>
                                <th scope="col">利用数</th>
                                <th scope="col">無効化数</th>
                                <th scope="col">利用期限</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${couponPage.items}">
                                <tr>
                                    <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_COUPONS += '?id=' += item.couponId)}"><c:out value="${item.couponId}" /></a></td>
                                    <td><c:out value="${ManagementFormat.text(item.couponTitle)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.storeName)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.couponStatusLabel)}" /></td>
                                    <td><c:out value="${item.issuedCount}" /></td>
                                    <td><c:out value="${item.usedCount}" /></td>
                                    <td><c:out value="${item.revokedCount}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.validUntil)}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
        <mg:pager page="${couponPage}" root="${root}" label="クーポン一覧" />
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
