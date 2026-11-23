<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<c:set var="tab" value="${model.detail()}" />
<c:set var="location" value="${tab.current()}" />
<c:set var="kinds" value="${ManagementDisplay.options('HOME', '居住地', 'WORK', '勤務先', 'SCHOOL', '学校')}" />
<mg:table heading="生活拠点" root="${root}" rows="${tab.locations()}" />

<c:if test="${tab.editable()}">
    <nav class="page-links" aria-label="この画面のメニュー">
        <c:forEach var="kind" items="${kinds}">
            <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += model.id() += '&tab=locations&kind=' += kind.key)}"<c:if test="${kind.key eq tab.kind()}"> aria-current="page"</c:if>><c:out value="${kind.value += 'を編集'}" /></a>
        </c:forEach>
    </nav>

    <section class="panel form-panel">
        <h2><c:out value="${kinds[tab.kind()] += 'を保存'}" /></h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="location">
            <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
            <input type="hidden" name="kind" value="${fn:escapeXml(tab.kind())}">
            <div class="form-fields">
                <mg:input id="location-prefecture" name="prefecture" label="都道府県" value="${location['都道府県']}" maxLength="10" required="true" />
                <mg:input id="location-city" name="city" label="市区町村" value="${location['市区町村']}" maxLength="50" required="true" />
                <mg:input id="location-address" name="street" label="番地・建物名" value="${location['番地・建物名']}" maxLength="200" />
                <mg:input id="location-latitude" name="lat" label="緯度" value="${location['緯度']}" maxLength="20" />
                <mg:input id="location-longitude" name="lng" label="経度" value="${location['経度']}" maxLength="20" />
            </div>
            <button type="submit">住所を保存</button>
        </form>
    </section>
</c:if>
