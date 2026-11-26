<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<section class="panel">
    <h2>生活拠点</h2>
    <c:choose>
        <c:when test="${empty locations}">
            <p class="empty">該当するデータはありません。</p>
        </c:when>
        <c:otherwise>
            <div class="table-scroll" role="region" aria-label="生活拠点" tabindex="0">
                <table aria-label="生活拠点">
                    <thead>
                        <tr>
                            <th scope="col">種類</th>
                            <th scope="col">都道府県</th>
                            <th scope="col">市区町村</th>
                            <th scope="col">番地・建物名</th>
                            <th scope="col">緯度</th>
                            <th scope="col">経度</th>
                            <th scope="col">更新日時</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="loc" items="${locations}">
                            <tr>
                                <td><c:out value="${ManagementFormat.text(loc.locationKindLabel)}" /></td>
                                <td><c:out value="${ManagementFormat.text(loc.prefecture)}" /></td>
                                <td><c:out value="${ManagementFormat.text(loc.municipality)}" /></td>
                                <td><c:out value="${ManagementFormat.text(loc.addressLine)}" /></td>
                                <td><c:out value="${ManagementFormat.text(loc.latitude)}" /></td>
                                <td><c:out value="${ManagementFormat.text(loc.longitude)}" /></td>
                                <td><c:out value="${ManagementFormat.text(loc.updatedAt)}" /></td>
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
        <h2>生活拠点を変更</h2>
        <form id="admin-location-form" action="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="location">
            <input type="hidden" name="id" value="${fn:escapeXml(member.userId)}">

            <%-- 会員登録画面と同様に、居住地は地図上での選択を必須とする。 --%>
            <div class="location-card">
                <div class="location-card-head">
                    <label for="admin-living-address" class="location-label">
                        居住地の住所 <small>必須（マップ上で選択）</small>
                    </label>
                    <button type="button" id="admin-living-current-btn" class="location-btn">現在地から自動設定</button>
                </div>
                <p class="location-desc">地図上をクリックするかピンをドラッグして、居住地を設定してください。</p>
                <div id="admin-living-map-picker" class="location-map"></div>
                <div class="field location-address-field">
                    <label for="admin-living-address">選択中の居住地住所</label>
                    <input id="admin-living-address" name="livingAddress" type="text"
                           value="${fn:escapeXml(homeLocation ne null ? homeLocation.fullAddress : '')}"
                           required readonly placeholder="地図をクリックすると自動で住所が設定されます">
                    <input id="admin-living-lat" name="livingLat" type="hidden"
                           value="${fn:escapeXml(homeLocation ne null and homeLocation.latitude ne null ? homeLocation.latitude.toPlainString() : '')}">
                    <input id="admin-living-lng" name="livingLng" type="hidden"
                           value="${fn:escapeXml(homeLocation ne null and homeLocation.longitude ne null ? homeLocation.longitude.toPlainString() : '')}">
                    <div id="admin-living-map-status" class="location-status">※地図をクリックして居住地を選択してください</div>
                </div>
            </div>

            <%-- 勤務先・学校は統合した任意入力枠として折りたたみ・解除に対応する。 --%>
            <div class="location-work-wrap">
                <button type="button" id="admin-work-trigger-card" class="location-trigger-card">
                    <span id="admin-work-trigger-label">勤務先・学校を登録する</span>
                    <span id="admin-work-trigger-icon" class="location-trigger-icon">＋</span>
                </button>

                <div id="admin-work-map-area" class="location-card" style="display: none;">
                    <div class="location-card-head">
                        <label for="admin-work-address" class="location-label">
                            勤務先・学校の住所 <span class="location-optional">（任意・マップ上で選択）</span>
                        </label>
                        <div class="location-actions">
                            <button type="button" id="admin-work-current-btn" class="location-btn">現在地から自動設定</button>
                            <button type="button" id="admin-work-remove-btn" class="location-btn location-btn-danger">登録を解除</button>
                            <button type="button" id="admin-work-close-btn" class="location-btn">✕ 閉じる</button>
                        </div>
                    </div>
                    <p class="location-desc">地図上をクリックするかピンをドラッグして、勤務先・学校を設定してください。</p>
                    <div id="admin-work-map-picker" class="location-map"></div>
                    <div class="field location-address-field">
                        <label for="admin-work-address">選択中の勤務先・学校住所</label>
                        <input id="admin-work-address" name="workAddress" type="text"
                               value="${fn:escapeXml(workLocation ne null ? workLocation.fullAddress : '')}"
                               readonly placeholder="地図をクリックすると自動で住所が設定されます">
                        <input id="admin-work-lat" name="workLat" type="hidden"
                               value="${fn:escapeXml(workLocation ne null and workLocation.latitude ne null ? workLocation.latitude.toPlainString() : '')}">
                        <input id="admin-work-lng" name="workLng" type="hidden"
                               value="${fn:escapeXml(workLocation ne null and workLocation.longitude ne null ? workLocation.longitude.toPlainString() : '')}">
                        <div id="admin-work-map-status" class="location-status">※地図をクリックしてピンを配置してください</div>
                    </div>
                </div>
            </div>

            <button type="submit">生活拠点を保存</button>
        </form>
    </section>
    <script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyCNWrNbVnBfZsjuTJ3sHCKDemfkxNsIv7w&amp;libraries=marker&amp;language=ja&amp;region=JP"></script>
    <script src="${fn:escapeXml(root)}/js/management-location-picker.js"></script>
</c:if>
