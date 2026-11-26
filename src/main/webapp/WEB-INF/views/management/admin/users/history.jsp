<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<section class="panel">
    <h2><c:out value="${historyHeading}" /></h2>
    <c:choose>
        <c:when test="${empty historyPage.items}">
            <p class="empty">該当するデータはありません。</p>
        </c:when>
        <c:when test="${selectedTab eq 'votes'}">
            <div class="table-scroll" role="region" aria-label="${fn:escapeXml(historyHeading)}" tabindex="0">
                <table aria-label="${fn:escapeXml(historyHeading)}">
                    <thead>
                        <tr>
                            <th scope="col">投票ID</th>
                            <th scope="col">物件ID</th>
                            <th scope="col">投票先</th>
                            <th scope="col">投票日時</th>
                            <th scope="col">更新日時</th>
                            <th scope="col">取下げ日時</th>
                            <th scope="col">投票した業種</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${historyPage.items}">
                            <tr>
                                <td><c:out value="${row.recordId}" /></td>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES += '?id=' += row.propertyId)}"><c:out value="${row.propertyId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(row.title)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.primaryTime)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.secondaryTime)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.tertiaryTime)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.detailText)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:when test="${selectedTab eq 'answers'}">
            <div class="table-scroll" role="region" aria-label="${fn:escapeXml(historyHeading)}" tabindex="0">
                <table aria-label="${fn:escapeXml(historyHeading)}">
                    <thead>
                        <tr>
                            <th scope="col">回答ID</th>
                            <th scope="col">アンケートID</th>
                            <th scope="col">アンケート</th>
                            <th scope="col">店舗</th>
                            <th scope="col">満足度</th>
                            <th scope="col">回答日時</th>
                            <th scope="col">回答内容</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${historyPage.items}">
                            <tr>
                                <td><c:out value="${row.recordId}" /></td>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS += '?id=' += row.relatedId)}"><c:out value="${row.relatedId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(row.title)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.subtitle)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.rating)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.primaryTime)}" /></td>
                                <td class="multiline"><c:out value="${ManagementFormat.text(row.detailText)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:when test="${selectedTab eq 'held' or selectedTab eq 'used'}">
            <div class="table-scroll" role="region" aria-label="${fn:escapeXml(historyHeading)}" tabindex="0">
                <table aria-label="${fn:escapeXml(historyHeading)}">
                    <thead>
                        <tr>
                            <th scope="col">クーポンID</th>
                            <th scope="col">クーポン</th>
                            <th scope="col">利用店舗</th>
                            <th scope="col">発行日時</th>
                            <th scope="col">有効期限</th>
                            <th scope="col">使用日時</th>
                            <th scope="col">無効化日時</th>
                            <th scope="col">状態</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${historyPage.items}">
                            <tr>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_COUPONS += '?id=' += row.relatedId)}"><c:out value="${row.relatedId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(row.title)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.subtitle)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.primaryTime)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.validUntil)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.secondaryTime)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.tertiaryTime)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.statusText)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:when test="${selectedTab eq 'applications'}">
            <div class="table-scroll" role="region" aria-label="${fn:escapeXml(historyHeading)}" tabindex="0">
                <table aria-label="${fn:escapeXml(historyHeading)}">
                    <thead>
                        <tr>
                            <th scope="col">申込ID</th>
                            <th scope="col">物件ID</th>
                            <th scope="col">物件</th>
                            <th scope="col">状態</th>
                            <th scope="col">申込日時</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${historyPage.items}">
                            <tr>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_APPLICATIONS += '?id=' += row.applicationId)}"><c:out value="${row.applicationId}" /></a></td>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES += '?id=' += row.propertyId)}"><c:out value="${row.propertyId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(row.title)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.statusText)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.primaryTime)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:when test="${selectedTab eq 'tenancies'}">
            <div class="table-scroll" role="region" aria-label="${fn:escapeXml(historyHeading)}" tabindex="0">
                <table aria-label="${fn:escapeXml(historyHeading)}">
                    <thead>
                        <tr>
                            <th scope="col">契約ID</th>
                            <th scope="col">申込ID</th>
                            <th scope="col">物件ID</th>
                            <th scope="col">物件</th>
                            <th scope="col">店舗</th>
                            <th scope="col">契約開始日</th>
                            <th scope="col">契約終了日</th>
                            <th scope="col">状態</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${historyPage.items}">
                            <tr>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_TENANCIES += '?id=' += row.tenancyId)}"><c:out value="${row.tenancyId}" /></a></td>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_APPLICATIONS += '?id=' += row.applicationId)}"><c:out value="${row.applicationId}" /></a></td>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES += '?id=' += row.propertyId)}"><c:out value="${row.propertyId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(row.title)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.subtitle)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.startDate)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.endDate)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.statusText)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:when test="${selectedTab eq 'plans'}">
            <div class="table-scroll" role="region" aria-label="${fn:escapeXml(historyHeading)}" tabindex="0">
                <table aria-label="${fn:escapeXml(historyHeading)}">
                    <thead>
                        <tr>
                            <th scope="col">計画ID</th>
                            <th scope="col">契約ID</th>
                            <th scope="col">物件ID</th>
                            <th scope="col">計画</th>
                            <th scope="col">開店予定日</th>
                            <th scope="col">進捗</th>
                            <th scope="col">公開状態</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${historyPage.items}">
                            <tr>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PLANS += '?id=' += row.recordId)}"><c:out value="${row.recordId}" /></a></td>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_TENANCIES += '?id=' += row.tenancyId)}"><c:out value="${row.tenancyId}" /></a></td>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES += '?id=' += row.propertyId)}"><c:out value="${row.propertyId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(row.title)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.startDate)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.statusText)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.secondaryStatusText)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:when test="${selectedTab eq 'offers'}">
            <div class="table-scroll" role="region" aria-label="${fn:escapeXml(historyHeading)}" tabindex="0">
                <table aria-label="${fn:escapeXml(historyHeading)}">
                    <thead>
                        <tr>
                            <th scope="col">オファーID</th>
                            <th scope="col">物件ID</th>
                            <th scope="col">物件</th>
                            <th scope="col">相手会員ID</th>
                            <th scope="col">相手</th>
                            <th scope="col">方向</th>
                            <th scope="col">状態</th>
                            <th scope="col">送信日時</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${historyPage.items}">
                            <tr>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_OFFERS += '?id=' += row.recordId)}"><c:out value="${row.recordId}" /></a></td>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES += '?id=' += row.propertyId)}"><c:out value="${row.propertyId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(row.title)}" /></td>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += row.partnerUserId)}"><c:out value="${row.partnerUserId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(row.subtitle)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.direction)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.statusText)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.primaryTime)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:when test="${selectedTab eq 'surveys'}">
            <div class="table-scroll" role="region" aria-label="${fn:escapeXml(historyHeading)}" tabindex="0">
                <table aria-label="${fn:escapeXml(historyHeading)}">
                    <thead>
                        <tr>
                            <th scope="col">アンケートID</th>
                            <th scope="col">アンケート</th>
                            <th scope="col">店舗</th>
                            <th scope="col">状態</th>
                            <th scope="col">回答数</th>
                            <th scope="col">更新日時</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${historyPage.items}">
                            <tr>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS += '?id=' += row.relatedId)}"><c:out value="${row.relatedId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(row.title)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.subtitle)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.statusText)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.primaryCount)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.secondaryTime)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:when test="${selectedTab eq 'coupons'}">
            <div class="table-scroll" role="region" aria-label="${fn:escapeXml(historyHeading)}" tabindex="0">
                <table aria-label="${fn:escapeXml(historyHeading)}">
                    <thead>
                        <tr>
                            <th scope="col">クーポンID</th>
                            <th scope="col">クーポン</th>
                            <th scope="col">店舗</th>
                            <th scope="col">状態</th>
                            <th scope="col">有効期限</th>
                            <th scope="col">発行数</th>
                            <th scope="col">使用数</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${historyPage.items}">
                            <tr>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_COUPONS += '?id=' += row.relatedId)}"><c:out value="${row.relatedId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(row.title)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.subtitle)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.statusText)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.validUntil)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.primaryCount)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.secondaryCount)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:when test="${selectedTab eq 'properties'}">
            <div class="table-scroll" role="region" aria-label="${fn:escapeXml(historyHeading)}" tabindex="0">
                <table aria-label="${fn:escapeXml(historyHeading)}">
                    <thead>
                        <tr>
                            <th scope="col">物件ID</th>
                            <th scope="col">物件名</th>
                            <th scope="col">募集・契約状態</th>
                            <th scope="col">掲載停止日時</th>
                            <th scope="col">申込数</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${historyPage.items}">
                            <tr>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES += '?id=' += row.propertyId)}"><c:out value="${row.propertyId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(row.title)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.statusText)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.secondaryTime)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.primaryCount)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:when test="${selectedTab eq 'reports'}">
            <div class="table-scroll" role="region" aria-label="${fn:escapeXml(historyHeading)}" tabindex="0">
                <table aria-label="${fn:escapeXml(historyHeading)}">
                    <thead>
                        <tr>
                            <th scope="col">通報ID</th>
                            <th scope="col">理由</th>
                            <th scope="col">対応状態</th>
                            <th scope="col">受付日時</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${historyPage.items}">
                            <tr>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_REPORTS += '?id=' += row.recordId)}"><c:out value="${row.recordId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(row.detailText)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.statusText)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.primaryTime)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-scroll" role="region" aria-label="${fn:escapeXml(historyHeading)}" tabindex="0">
                <table aria-label="${fn:escapeXml(historyHeading)}">
                    <thead>
                        <tr>
                            <th scope="col">問い合わせID</th>
                            <th scope="col">件名</th>
                            <th scope="col">対応状態</th>
                            <th scope="col">更新日時</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${historyPage.items}">
                            <tr>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INQUIRIES += '?id=' += row.recordId)}"><c:out value="${row.recordId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(row.title)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.statusText)}" /></td>
                                <td><c:out value="${ManagementFormat.text(row.secondaryTime)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
    <mg:pager page="${historyPage}" root="${root}" label="${historyHeading}" />
</section>
