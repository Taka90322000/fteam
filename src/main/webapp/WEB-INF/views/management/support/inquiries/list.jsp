<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.PROPERTIES)}">物件から問い合わせ先を選ぶ</a>
</nav>
<div class="sections">
    <c:if test="${relatedProperty ne null}">
        <section class="panel">
            <h2>関連物件</h2>
            <dl class="details">
                <dt>ID</dt>
                <dd><a href="${fn:escapeXml(root += ManagementPaths.PROPERTIES += '?id=' += relatedProperty.propertyId)}"><c:out value="${relatedProperty.propertyId}" /></a></dd>

                <dt>名前</dt>
                <dd><c:out value="${ManagementFormat.text(relatedProperty.propertyName)}" /></dd>

                <dt>掲載者</dt>
                <dd><c:out value="${ManagementFormat.text(relatedProperty.ownerName)}" /></dd>

                <dt>所在地</dt>
                <dd><c:out value="${ManagementFormat.text(relatedProperty.address)}" /></dd>

                <dt>面積（㎡）</dt>
                <dd><c:out value="${ManagementFormat.text(relatedProperty.floorAreaSqm)}" /></dd>

                <dt>月額賃料（円）</dt>
                <dd><c:out value="${ManagementFormat.text(relatedProperty.monthlyRentYen)}" /></dd>

                <dt>説明</dt>
                <dd><c:out value="${ManagementFormat.text(relatedProperty.description)}" /></dd>
            </dl>
        </section>
    </c:if>

    <section class="panel form-panel">
        <h2>新しい問い合わせ</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.INQUIRIES)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="open">
            <input type="hidden" name="propertyId" value="${fn:escapeXml(propertyId)}">
            <div class="form-fields">
                <mg:select id="member-inquiry-category" name="category" label="カテゴリ" choices="${categories}" value="${propertyId eq null ? 'ACCOUNT' : 'PROPERTY'}" required="true" />
                <mg:input id="member-inquiry-subject" name="subject" label="件名" maxLength="200" required="true" />
                <mg:textarea id="member-inquiry-body" name="body" label="本文" maxLength="8000" required="true" />
            </div>
            <button type="submit">問い合わせを送信</button>
        </form>
    </section>

    <section class="panel">
        <h2>問い合わせ履歴</h2>
        <c:choose>
            <c:when test="${empty inquiryPage.items}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="問い合わせ履歴" tabindex="0">
                    <table aria-label="問い合わせ履歴">
                        <thead>
                            <tr>
                                <th scope="col">ID</th>
                                <th scope="col">件名</th>
                                <th scope="col">分類</th>
                                <th scope="col">状態</th>
                                <th scope="col">更新日時</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${inquiryPage.items}">
                                <tr>
                                    <td><a href="${fn:escapeXml(root += ManagementPaths.INQUIRIES += '?id=' += item.inquiryId)}"><c:out value="${item.inquiryId}" /></a></td>
                                    <td><c:out value="${ManagementFormat.text(item.subject)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.categoryLabel)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.statusLabel)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.updatedAt)}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
        <mg:pager page="${inquiryPage}" root="${root}" label="問い合わせ履歴" />
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
