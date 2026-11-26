<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<c:set var="detailPath" value="${ManagementPaths.ADMIN_PROPERTIES += '?id=' += property.propertyId}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES)}">物件一覧へ</a>
    <a href="${fn:escapeXml(root += detailPath += '&tab=info')}"<c:if test="${tab eq 'info'}"> aria-current="page"</c:if>>物件情報</a>
    <a href="${fn:escapeXml(root += detailPath += '&tab=applications')}"<c:if test="${tab eq 'applications'}"> aria-current="page"</c:if>>出店申込・契約</a>
    <a href="${fn:escapeXml(root += detailPath += '&tab=offers')}"<c:if test="${tab eq 'offers'}"> aria-current="page"</c:if>>オファー</a>
    <a href="${fn:escapeXml(root += detailPath += '&tab=reports')}"<c:if test="${tab eq 'reports'}"> aria-current="page"</c:if>>受けた通報</a>
</nav>
<div class="sections">
    <c:choose>
        <c:when test="${tab eq 'info'}">
            <section class="panel">
                <h2>物件情報</h2>
                <dl class="details">
                    <dt>物件ID</dt>
                    <dd><c:out value="${property.propertyId}" /></dd>

                    <dt>名前</dt>
                    <dd><c:out value="${ManagementFormat.text(property.propertyName)}" /></dd>

                    <dt>掲載者ID</dt>
                    <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += property.ownerUserId)}"><c:out value="${property.ownerUserId}" /></a></dd>

                    <dt>掲載者</dt>
                    <dd><c:out value="${ManagementFormat.text(property.ownerName)}" /></dd>

                    <dt>都道府県</dt>
                    <dd><c:out value="${ManagementFormat.text(property.prefecture)}" /></dd>

                    <dt>市区町村</dt>
                    <dd><c:out value="${ManagementFormat.text(property.municipality)}" /></dd>

                    <dt>番地</dt>
                    <dd><c:out value="${ManagementFormat.text(property.addressLine)}" /></dd>

                    <dt>緯度</dt>
                    <dd><c:out value="${ManagementFormat.text(property.latitude)}" /></dd>

                    <dt>経度</dt>
                    <dd><c:out value="${ManagementFormat.text(property.longitude)}" /></dd>

                    <dt>面積（㎡）</dt>
                    <dd><c:out value="${ManagementFormat.text(property.floorAreaSqm)}" /></dd>

                    <dt>月額賃料（円）</dt>
                    <dd><c:out value="${ManagementFormat.text(property.monthlyRentYen)}" /></dd>

                    <dt>説明</dt>
                    <dd class="multiline"><c:out value="${ManagementFormat.text(property.description)}" /></dd>

                    <dt>掲載URL</dt>
                    <dd>
                        <c:choose>
                            <c:when test="${ManagementFormat.isWebUrl(property.listingUrl)}">
                                <a href="${fn:escapeXml(property.listingUrl)}"><c:out value="${property.listingUrl}" /></a>
                            </c:when>
                            <c:otherwise><c:out value="${ManagementFormat.text(property.listingUrl)}" /></c:otherwise>
                        </c:choose>
                    </dd>

                    <dt>許認可</dt>
                    <dd><c:out value="${ManagementFormat.text(property.permitStatus)}" /></dd>

                    <dt>募集・契約状態</dt>
                    <dd><c:out value="${ManagementFormat.text(property.listingStatusLabel)}" /></dd>

                    <dt>掲載停止日時</dt>
                    <dd><c:out value="${ManagementFormat.text(property.adminHiddenAt)}" /></dd>

                    <dt>削除日時</dt>
                    <dd><c:out value="${ManagementFormat.text(property.deletedAt)}" /></dd>
                </dl>
            </section>

            <section class="panel">
                <h2>写真</h2>
                <c:choose>
                    <c:when test="${empty images}">
                        <p class="empty">該当するデータはありません。</p>
                    </c:when>
                    <c:otherwise>
                        <div class="table-scroll" role="region" aria-label="写真" tabindex="0">
                            <table aria-label="写真">
                                <thead>
                                    <tr>
                                        <th scope="col">表示順</th>
                                        <th scope="col">画像</th>
                                        <th scope="col">説明</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="img" items="${images}">
                                        <tr>
                                            <td><c:out value="${img.displayOrder}" /></td>
                                            <td><c:out value="${ManagementFormat.text(img.storageKey)}" /></td>
                                            <td><c:out value="${ManagementFormat.text(img.altText)}" /></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>

            <section class="panel">
                <h2>対象業種</h2>
                <c:choose>
                    <c:when test="${empty industries}">
                        <p class="empty">該当するデータはありません。</p>
                    </c:when>
                    <c:otherwise>
                        <div class="table-scroll" role="region" aria-label="対象業種" tabindex="0">
                            <table aria-label="対象業種">
                                <thead>
                                    <tr>
                                        <th scope="col">業種ID</th>
                                        <th scope="col">業種</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="ind" items="${industries}">
                                        <tr>
                                            <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES += '?kind=type&id=' += ind.industryTypeId)}"><c:out value="${ind.industryTypeId}" /></a></td>
                                            <td><c:out value="${ManagementFormat.text(ind.industryName)}" /></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>

            <section class="panel">
                <h2>契約条件</h2>
                <c:choose>
                    <c:when test="${empty terms}">
                        <p class="empty">該当するデータはありません。</p>
                    </c:when>
                    <c:otherwise>
                        <div class="table-scroll" role="region" aria-label="契約条件" tabindex="0">
                            <table aria-label="契約条件">
                                <thead>
                                    <tr>
                                        <th scope="col">契約区分</th>
                                        <th scope="col">最短日数</th>
                                        <th scope="col">最長日数</th>
                                        <th scope="col">日額賃料（円）</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="term" items="${terms}">
                                        <tr>
                                            <td><c:out value="${ManagementFormat.text(term.contractKind)}" /></td>
                                            <td><c:out value="${ManagementFormat.text(term.minDays)}" /></td>
                                            <td><c:out value="${ManagementFormat.text(term.maxDays)}" /></td>
                                            <td><c:out value="${ManagementFormat.text(term.dailyRentYen)}" /></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>

            <c:if test="${property.editable}">
                <section class="panel form-panel">
                    <h2><c:out value="${property.hidden ? '掲載停止を解除' : '掲載を停止'}" /></h2>
                    <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES)}" method="post" accept-charset="UTF-8">
                        <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                        <input type="hidden" name="operation" value="${fn:escapeXml(property.hidden ? 'unhide' : 'hide')}">
                        <input type="hidden" name="id" value="${fn:escapeXml(property.propertyId)}">
                        <div class="form-fields">
                            <mg:textarea id="property-visibility-reason" name="reason" label="操作理由" maxLength="2000" required="true" />
                        </div>
                        <button type="submit"><c:out value="${property.hidden ? '掲載停止を解除する' : '掲載を停止する'}" /></button>
                    </form>
                </section>

                <c:if test="${property.canPublish}">
                    <section class="panel form-panel">
                        <h2>募集を公開</h2>
                        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES)}" method="post" accept-charset="UTF-8">
                            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                            <input type="hidden" name="operation" value="publish">
                            <input type="hidden" name="id" value="${fn:escapeXml(property.propertyId)}">
                            <div class="form-fields">
                                <mg:textarea id="property-publish-reason" name="reason" label="操作理由" maxLength="2000" required="true" />
                            </div>
                            <button type="submit">募集を公開する</button>
                        </form>
                    </section>
                </c:if>

                <section class="panel form-panel">
                    <h2>掲載内容を修正</h2>
                    <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES)}" method="post" accept-charset="UTF-8">
                        <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                        <input type="hidden" name="operation" value="edit">
                        <input type="hidden" name="id" value="${fn:escapeXml(property.propertyId)}">
                        <div class="form-fields">
                            <mg:input id="property-name" name="name" label="物件名" value="${property.propertyName}" maxLength="150" required="true" />
                            <mg:input id="property-prefecture" name="prefecture" label="都道府県" value="${property.prefecture}" maxLength="10" required="true" />
                            <mg:input id="property-city" name="city" label="市区町村" value="${property.municipality}" maxLength="50" required="true" />
                            <mg:input id="property-street" name="street" label="番地" value="${property.addressLine}" maxLength="200" required="true" />
                            <mg:input id="property-latitude" name="lat" label="緯度" value="${property.latitude}" maxLength="20" />
                            <mg:input id="property-longitude" name="lng" label="経度" value="${property.longitude}" maxLength="20" />
                            <mg:input id="property-floor" name="floor" label="面積（㎡）" value="${property.floorAreaSqm}" maxLength="20" required="true" />
                            <mg:input id="property-rent" name="rent" label="月額賃料（円）" type="number" value="${property.monthlyRentYen}" required="true" />
                            <mg:select id="property-permit" name="permit" label="許認可の状態" choices="${permits}" value="${property.permitStatus}" required="true" />
                            <mg:textarea id="property-description" name="description" label="説明" value="${property.description}" maxLength="8000" />
                            <mg:input id="property-url" name="url" label="掲載URL" type="url" value="${property.listingUrl}" maxLength="1000" />
                        </div>
                        <button type="submit">修正を保存</button>
                    </form>
                </section>
            </c:if>
        </c:when>

        <c:when test="${tab eq 'applications'}">
            <section class="panel">
                <h2>出店申込・契約</h2>
                <c:choose>
                    <c:when test="${empty historyPage.items}">
                        <p class="empty">該当するデータはありません。</p>
                    </c:when>
                    <c:otherwise>
                        <div class="table-scroll" role="region" aria-label="出店申込・契約" tabindex="0">
                            <table aria-label="出店申込・契約">
                                <thead>
                                    <tr>
                                        <th scope="col">申込ID</th>
                                        <th scope="col">事業主ID</th>
                                        <th scope="col">事業者名</th>
                                        <th scope="col">申込状態</th>
                                        <th scope="col">契約ID</th>
                                        <th scope="col">契約状態</th>
                                        <th scope="col">申込日時</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="row" items="${historyPage.items}">
                                        <tr>
                                            <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_APPLICATIONS += '?id=' += row.applicationId)}"><c:out value="${row.applicationId}" /></a></td>
                                            <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += row.businessUserId)}"><c:out value="${row.businessUserId}" /></a></td>
                                            <td><c:out value="${ManagementFormat.text(row.businessName)}" /></td>
                                            <td><c:out value="${ManagementFormat.text(row.status)}" /></td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${row.tenancyId ne null}">
                                                        <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_TENANCIES += '?id=' += row.tenancyId)}"><c:out value="${row.tenancyId}" /></a>
                                                    </c:when>
                                                    <c:otherwise>—</c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td><c:out value="${ManagementFormat.text(row.tenancyStatus)}" /></td>
                                            <td><c:out value="${ManagementFormat.text(row.occurredAt)}" /></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
                <mg:pager page="${historyPage}" root="${root}" label="出店申込・契約" />
            </section>
        </c:when>

        <c:when test="${tab eq 'offers'}">
            <section class="panel">
                <h2>オファー</h2>
                <c:choose>
                    <c:when test="${empty historyPage.items}">
                        <p class="empty">該当するデータはありません。</p>
                    </c:when>
                    <c:otherwise>
                        <div class="table-scroll" role="region" aria-label="オファー" tabindex="0">
                            <table aria-label="オファー">
                                <thead>
                                    <tr>
                                        <th scope="col">オファーID</th>
                                        <th scope="col">事業主ID</th>
                                        <th scope="col">事業者名</th>
                                        <th scope="col">状態</th>
                                        <th scope="col">送信日時</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="row" items="${historyPage.items}">
                                        <tr>
                                            <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_OFFERS += '?id=' += row.offerId)}"><c:out value="${row.offerId}" /></a></td>
                                            <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += row.businessUserId)}"><c:out value="${row.businessUserId}" /></a></td>
                                            <td><c:out value="${ManagementFormat.text(row.businessName)}" /></td>
                                            <td><c:out value="${ManagementFormat.text(row.status)}" /></td>
                                            <td><c:out value="${ManagementFormat.text(row.occurredAt)}" /></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
                <mg:pager page="${historyPage}" root="${root}" label="オファー" />
            </section>
        </c:when>

        <c:otherwise>
            <section class="panel">
                <h2>物件への通報</h2>
                <c:choose>
                    <c:when test="${empty historyPage.items}">
                        <p class="empty">該当するデータはありません。</p>
                    </c:when>
                    <c:otherwise>
                        <div class="table-scroll" role="region" aria-label="物件への通報" tabindex="0">
                            <table aria-label="物件への通報">
                                <thead>
                                    <tr>
                                        <th scope="col">通報ID</th>
                                        <th scope="col">対応状態</th>
                                        <th scope="col">理由</th>
                                        <th scope="col">受付日時</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="row" items="${historyPage.items}">
                                        <tr>
                                            <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_REPORTS += '?id=' += row.reportId)}"><c:out value="${row.reportId}" /></a></td>
                                            <td><c:out value="${ManagementFormat.text(row.status)}" /></td>
                                            <td><c:out value="${ManagementFormat.text(row.reasonCode)}" /></td>
                                            <td><c:out value="${ManagementFormat.text(row.occurredAt)}" /></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
                <mg:pager page="${historyPage}" root="${root}" label="物件への通報" />
            </section>
        </c:otherwise>
    </c:choose>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
