<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS += '?new=1')}">テンプレートを追加</a>
</nav>
<div class="sections">
    <section class="panel form-panel">
        <h2>絞り込み</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:input id="survey-query" name="q" label="アンケート名・店舗名" type="search" value="${query}" maxLength="100" />
                <mg:select id="survey-kind" name="kind" label="種類" choices="${kinds}" value="${kind}" placeholder="すべての種類" />
                <mg:select id="survey-state" name="status" label="受付状態" choices="${states}" value="${status}" placeholder="すべての状態" />
            </div>
            <button type="submit">検索</button>
        </form>
    </section>

    <section class="panel">
        <h2>アンケート一覧</h2>
        <c:choose>
            <c:when test="${empty surveyPage.items}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="アンケート一覧" tabindex="0">
                    <table aria-label="アンケート一覧">
                        <thead>
                            <tr>
                                <th scope="col">ID</th>
                                <th scope="col">名前</th>
                                <th scope="col">種類</th>
                                <th scope="col">店舗</th>
                                <th scope="col">受付状態</th>
                                <th scope="col">回答数</th>
                                <th scope="col">更新日時</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${surveyPage.items}">
                                <tr>
                                    <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_SURVEYS += '?id=' += item.surveyId)}"><c:out value="${item.surveyId}" /></a></td>
                                    <td><c:out value="${ManagementFormat.text(item.surveyTitle)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.surveyKindLabel)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.storeName)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.surveyStatusLabel)}" /></td>
                                    <td><c:out value="${item.responseCount}" /></td>
                                    <td><c:out value="${ManagementFormat.text(item.updatedAt)}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
        <mg:pager page="${surveyPage}" root="${root}" label="アンケート一覧" />
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
