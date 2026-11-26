<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<section class="panel">
    <h2>希望業種</h2>
    <c:choose>
        <c:when test="${empty preferences}">
            <p class="empty">該当するデータはありません。</p>
        </c:when>
        <c:otherwise>
            <div class="table-scroll" role="region" aria-label="希望業種" tabindex="0">
                <table aria-label="希望業種">
                    <thead>
                        <tr>
                            <th scope="col">ID</th>
                            <th scope="col">分類</th>
                            <th scope="col">希望業種</th>
                            <th scope="col">マスターの状態</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="pref" items="${preferences}">
                            <tr>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INDUSTRIES += '?kind=type&id=' += pref.industryTypeId)}"><c:out value="${pref.industryTypeId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(pref.categoryName)}" /></td>
                                <td><c:out value="${ManagementFormat.text(pref.industryName)}" /></td>
                                <td><c:out value="${ManagementFormat.text(pref.masterStateLabel)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<c:if test="${editable}">
    <section class="panel form-panel">
        <h2>希望業種を変更</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="preferences">
            <input type="hidden" name="id" value="${fn:escapeXml(member.userId)}">
            <%-- 会員登録画面と同じカテゴリタブ・ピル選択UIで希望業種を複数選択する。 --%>
            <div class="industry-picker"
                 data-taxonomy="${fn:escapeXml(preferenceTaxonomyJson)}"
                 data-selected="${fn:escapeXml(selectedPreferenceJson)}"
                 data-input-name="industries"
                 data-required="${requirePreference ? 'true' : 'false'}">
                <div class="industry-picker-head">
                    <span class="industry-picker-title">
                        希望業種・ジャンル <span class="industry-picker-note"><c:out value="${requirePreference ? '（1件以上必須・複数選択可）' : '（複数選択可）'}" /></span>
                    </span>
                    <span id="admin-industry-count" class="industry-count">選択中: <c:out value="${fn:length(selectedPreferenceIds)}" />件</span>
                </div>
                <div id="admin-selected-tags" class="selected-tags-box">
                    <span class="industry-picker-hint">※下のタブからジャンルを選んでください（複数選択可）</span>
                </div>
                <div id="admin-cat-tabs" class="cat-tabs-bar" role="tablist" aria-label="業種カテゴリ"></div>
                <div id="admin-subcat-panel" class="subcat-grid"></div>
                <div id="admin-industry-inputs">
                    <c:forEach var="prefId" items="${selectedPreferenceIds}">
                        <input type="hidden" name="industries" value="${fn:escapeXml(prefId)}">
                    </c:forEach>
                </div>
            </div>
            <button type="submit">業種を保存</button>
        </form>
    </section>
    <script src="${fn:escapeXml(root)}/js/management-industry-picker.js"></script>
</c:if>
