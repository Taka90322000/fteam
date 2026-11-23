<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<c:set var="selectedTab" value="${model.tab()}" />
<%-- 利用区分ごとのタブを、管理者が探す順番で並べる。 --%>
<c:choose>
    <c:when test="${model.role() eq 'RESIDENT'}">
        <c:set var="roleTabs" value="${ManagementDisplay.options(
            'locations', '生活拠点', 'votes', '投票履歴', 'answers', 'アンケート回答履歴',
            'held', '保持クーポン', 'used', 'クーポン使用履歴')}" />
    </c:when>
    <c:when test="${model.role() eq 'BUSINESS'}">
        <c:set var="roleTabs" value="${ManagementDisplay.options(
            'qualifications', '保有資格', 'applications', '出店申し込み', 'tenancies', '契約物件',
            'plans', '出店計画', 'offers', '出店オファー', 'surveys', '作成アンケート',
            'coupons', '作成クーポン', 'reports', '受けた通報')}" />
    </c:when>
    <c:otherwise>
        <c:set var="roleTabs" value="${ManagementDisplay.options(
            'properties', '保有物件', 'offers', 'オファー履歴', 'reports', '受けた通報')}" />
    </c:otherwise>
</c:choose>
<c:url var="detailUrl" value="${ManagementPaths.ADMIN_USERS}">
    <c:param name="id" value="${model.id()}" />
</c:url>
<c:url var="listUrl" value="${ManagementPaths.ADMIN_USERS}">
    <c:param name="role" value="${model.role()}" />
</c:url>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(listUrl)}">会員一覧へ</a>
    <a href="${fn:escapeXml(detailUrl)}&amp;tab=profile"<c:if test="${selectedTab eq 'profile'}"> aria-current="page"</c:if>>プロフィール</a>
    <a href="${fn:escapeXml(detailUrl)}&amp;tab=preferences"<c:if test="${selectedTab eq 'preferences'}"> aria-current="page"</c:if>>希望業種</a>
    <c:forEach var="tab" items="${roleTabs}">
        <a href="${fn:escapeXml(detailUrl += '&tab=' += tab.key)}"<c:if test="${selectedTab eq tab.key}"> aria-current="page"</c:if>><c:out value="${tab.value}" /></a>
    </c:forEach>
    <a href="${fn:escapeXml(detailUrl)}&amp;tab=inquiries"<c:if test="${selectedTab eq 'inquiries'}"> aria-current="page"</c:if>>問い合わせ履歴</a>
</nav>
<div class="sections">
    <mg:details heading="会員情報" root="${root}" record="${model.member()}" />

    <c:choose>
        <c:when test="${selectedTab eq 'profile'}">
            <jsp:include page="/WEB-INF/views/management/admin/users/profile.jsp" />
        </c:when>
        <c:when test="${selectedTab eq 'preferences'}">
            <jsp:include page="/WEB-INF/views/management/admin/users/preferences.jsp" />
        </c:when>
        <c:when test="${selectedTab eq 'locations'}">
            <jsp:include page="/WEB-INF/views/management/admin/users/locations.jsp" />
        </c:when>
        <c:when test="${selectedTab eq 'qualifications'}">
            <jsp:include page="/WEB-INF/views/management/admin/users/qualifications.jsp" />
        </c:when>
        <c:otherwise>
            <c:set var="history" value="${model.detail()}" />
            <mg:table heading="${selectedTab eq 'inquiries' ? '問い合わせ履歴' : roleTabs[selectedTab]}"
                      root="${root}" pagination="${history.records()}"
                      links="${{
                          '物件ID': ManagementPaths.ADMIN_PROPERTIES += '?id=',
                          '申込ID': ManagementPaths.ADMIN_APPLICATIONS += '?id=',
                          '契約ID': ManagementPaths.ADMIN_TENANCIES += '?id=',
                          '計画ID': ManagementPaths.ADMIN_PLANS += '?id=',
                          'オファーID': ManagementPaths.ADMIN_OFFERS += '?id=',
                          'アンケートID': ManagementPaths.ADMIN_SURVEYS += '?id=',
                          'クーポンID': ManagementPaths.ADMIN_COUPONS += '?id=',
                          '問い合わせID': ManagementPaths.ADMIN_INQUIRIES += '?id=',
                          '通報ID': ManagementPaths.ADMIN_REPORTS += '?id=',
                          '相手会員ID': ManagementPaths.ADMIN_USERS += '?id='
                      }}" />
        </c:otherwise>
    </c:choose>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
