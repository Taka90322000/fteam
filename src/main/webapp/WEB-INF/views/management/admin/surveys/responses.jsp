<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<section class="panel">
    <h2>満足度の分布</h2>
    <div class="table-scroll" role="region" aria-label="満足度の分布" tabindex="0">
        <table aria-label="満足度の分布">
            <thead>
                <tr>
                    <th scope="col">満足度</th>
                    <th scope="col">回答数</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="r" items="${ratings}">
                    <tr>
                        <td><c:out value="${r.rating}" /></td>
                        <td><c:out value="${r.responseCount}" /></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</section>

<section class="panel">
    <h2>選択肢の集計</h2>
    <c:choose>
        <c:when test="${empty optionCounts}">
            <p class="empty">該当するデータはありません。</p>
        </c:when>
        <c:otherwise>
            <div class="table-scroll" role="region" aria-label="選択肢の集計" tabindex="0">
                <table aria-label="選択肢の集計">
                    <thead>
                        <tr>
                            <th scope="col">設問番号</th>
                            <th scope="col">設問</th>
                            <th scope="col">選択肢</th>
                            <th scope="col">回答数</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="opt" items="${optionCounts}">
                            <tr>
                                <td><c:out value="${opt.questionNo}" /></td>
                                <td><c:out value="${ManagementFormat.text(opt.questionText)}" /></td>
                                <td><c:out value="${ManagementFormat.text(opt.optionText)}" /></td>
                                <td><c:out value="${opt.responseCount}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<section class="panel">
    <h2>回答履歴</h2>
    <c:choose>
        <c:when test="${empty responsePage.items}">
            <p class="empty">該当するデータはありません。</p>
        </c:when>
        <c:otherwise>
            <div class="table-scroll" role="region" aria-label="回答履歴" tabindex="0">
                <table aria-label="回答履歴">
                    <thead>
                        <tr>
                            <th scope="col">回答ID</th>
                            <th scope="col">回答者ID</th>
                            <th scope="col">回答者</th>
                            <th scope="col">満足度</th>
                            <th scope="col">年齢区分</th>
                            <th scope="col">市区町村</th>
                            <th scope="col">回答日時</th>
                            <th scope="col">回答内容</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="ans" items="${responsePage.items}">
                            <tr>
                                <td><c:out value="${ans.responseId}" /></td>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += ans.residentUserId)}"><c:out value="${ans.residentUserId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(ans.residentName)}" /></td>
                                <td><c:out value="${ans.rating}" /></td>
                                <td><c:out value="${ManagementFormat.text(ans.ageGroup)}" /></td>
                                <td><c:out value="${ManagementFormat.text(ans.municipality)}" /></td>
                                <td><c:out value="${ManagementFormat.text(ans.respondedAt)}" /></td>
                                <td class="multiline"><c:out value="${ManagementFormat.text(ans.answersSummary)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
    <mg:pager page="${responsePage}" root="${root}" label="回答履歴" />
</section>
