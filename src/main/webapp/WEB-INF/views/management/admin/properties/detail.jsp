<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<c:set var="property" value="${model.property()}" />
<c:set var="detailPath" value="${ManagementPaths.ADMIN_PROPERTIES += '?id=' += model.id()}" />
<c:set var="tabs" value="${ManagementDisplay.options('info', '物件情報', 'applications', '出店申込・契約',
    'offers', 'オファー', 'reports', '受けた通報')}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES)}">物件一覧へ</a>
    <c:forEach var="tab" items="${tabs}">
        <a href="${fn:escapeXml(root += detailPath += '&tab=' += tab.key)}"<c:if test="${tab.key eq model.tab()}"> aria-current="page"</c:if>><c:out value="${tab.value}" /></a>
    </c:forEach>
</nav>
<div class="sections">
    <c:choose>
        <c:when test="${model.info() ne null}">
            <c:set var="info" value="${model.info()}" />
            <mg:details heading="物件情報" root="${root}" record="${property}" links="${{'掲載者ID': ManagementPaths.ADMIN_USERS += '?id='}}" />

            <mg:table heading="写真" root="${root}" rows="${info.images()}" />

            <mg:table heading="対象業種" root="${root}" rows="${info.industries()}" links="${{'業種ID': ManagementPaths.ADMIN_INDUSTRIES += '?kind=type&id='}}" />

            <mg:table heading="契約条件" root="${root}" rows="${info.terms()}" />

            <c:if test="${info.editable()}">
                <section class="panel form-panel">
                    <h2><c:out value="${info.hidden() ? '掲載停止を解除' : '掲載を停止'}" /></h2>
                    <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES)}" method="post" accept-charset="UTF-8">
                        <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                        <input type="hidden" name="operation" value="${fn:escapeXml(info.hidden() ? 'unhide' : 'hide')}">
                        <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
                        <div class="form-fields">
                            <mg:textarea id="property-visibility-reason" name="reason" label="操作理由" maxLength="2000" required="true" />
                        </div>
                        <button type="submit"><c:out value="${info.hidden() ? '掲載停止を解除する' : '掲載を停止する'}" /></button>
                    </form>
                </section>

                <c:if test="${info.canPublish()}">
                    <section class="panel form-panel">
                        <h2>募集を公開</h2>
                        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES)}" method="post" accept-charset="UTF-8">
                            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                            <input type="hidden" name="operation" value="publish">
                            <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
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
                        <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
                        <div class="form-fields">
                            <mg:input id="property-name" name="name" label="物件名" value="${property['名前']}" maxLength="150" required="true" />
                            <mg:input id="property-prefecture" name="prefecture" label="都道府県" value="${property['都道府県']}" maxLength="10" required="true" />
                            <mg:input id="property-city" name="city" label="市区町村" value="${property['市区町村']}" maxLength="50" required="true" />
                            <mg:input id="property-street" name="street" label="番地" value="${property['番地']}" maxLength="200" required="true" />
                            <mg:input id="property-latitude" name="lat" label="緯度" value="${property['緯度']}" maxLength="20" />
                            <mg:input id="property-longitude" name="lng" label="経度" value="${property['経度']}" maxLength="20" />
                            <mg:input id="property-floor" name="floor" label="面積（㎡）" value="${property['面積（㎡）']}" maxLength="20" required="true" />
                            <mg:input id="property-rent" name="rent" label="月額賃料（円）" type="number" value="${property['月額賃料（円）']}" required="true" />
                            <mg:select id="property-permit" name="permit" label="許認可の状態" choices="${info.permits()}" value="${property['許認可']}" required="true" />
                            <mg:textarea id="property-description" name="description" label="説明" value="${property['説明']}" maxLength="8000" />
                            <mg:input id="property-url" name="url" label="掲載URL" type="url" value="${property['掲載URL']}" maxLength="1000" />
                        </div>
                        <button type="submit">修正を保存</button>
                    </form>
                </section>
            </c:if>
        </c:when>
        <c:otherwise>
            <mg:table heading="${'reports' eq model.tab() ? '物件への通報' : tabs[model.tab()]}" root="${root}" pagination="${model.history()}"
              links="${{
                  '物件ID': ManagementPaths.ADMIN_PROPERTIES += '?id=',
                  '申込ID': ManagementPaths.ADMIN_APPLICATIONS += '?id=',
                  '契約ID': ManagementPaths.ADMIN_TENANCIES += '?id=',
                  '計画ID': ManagementPaths.ADMIN_PLANS += '?id=',
                  'オファーID': ManagementPaths.ADMIN_OFFERS += '?id=',
                  '掲載者ID': ManagementPaths.ADMIN_USERS += '?id=',
                  'オーナーID': ManagementPaths.ADMIN_USERS += '?id=',
                  '事業主ID': ManagementPaths.ADMIN_USERS += '?id=',
                  '通報ID': ManagementPaths.ADMIN_REPORTS += '?id='
              }}" />
        </c:otherwise>
    </c:choose>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
