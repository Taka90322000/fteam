<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}">資格・書類マスター</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS += '?tab=requirements')}" aria-current="page">業種ごとの必要条件</a>
</nav>
<div class="sections">
    <section class="panel form-panel">
        <h2>対象業種</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}" method="get" accept-charset="UTF-8">
            <input type="hidden" name="tab" value="requirements">
            <div class="form-fields">
                <mg:select id="requirement-industry" name="industry" label="業種" choices="${industries}" value="${industryId}" placeholder="業種を選択" required="true" />
            </div>
            <button type="submit">表示する</button>
        </form>
    </section>

    <c:if test="${industryId ne null}">
        <section class="panel">
            <h2>登録済みの必要条件</h2>
            <c:choose>
                <c:when test="${empty requirementPage.items}">
                    <p class="empty">該当するデータはありません。</p>
                </c:when>
                <c:otherwise>
                    <div class="table-scroll" role="region" aria-label="登録済みの必要条件" tabindex="0">
                        <table aria-label="登録済みの必要条件">
                            <thead>
                                <tr>
                                    <th scope="col">ID</th>
                                    <th scope="col">名前</th>
                                    <th scope="col">種類</th>
                                    <th scope="col">必須</th>
                                    <th scope="col">適用条件</th>
                                    <th scope="col">案内URL</th>
                                    <th scope="col">有効</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="item" items="${requirementPage.items}">
                                    <tr>
                                        <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS += '?tab=requirements&industry=' += industryId += '&id=' += item.qualificationId)}"><c:out value="${item.qualificationId}" /></a></td>
                                        <td><c:out value="${ManagementFormat.text(item.qualificationName)}" /></td>
                                        <td><c:out value="${ManagementFormat.text(item.requirementKindLabel)}" /></td>
                                        <td><c:out value="${ManagementFormat.text(item.required)}" /></td>
                                        <td class="multiline"><c:out value="${ManagementFormat.text(item.conditionText)}" /></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${ManagementFormat.isWebUrl(item.guidanceUrl)}">
                                                    <a href="${fn:escapeXml(item.guidanceUrl)}" target="_blank" rel="noopener noreferrer"><c:out value="${item.guidanceUrl}" /></a>
                                                </c:when>
                                                <c:otherwise>
                                                    <c:out value="${ManagementFormat.text(item.guidanceUrl)}" />
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><c:out value="${ManagementFormat.text(item.active)}" /></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
            <mg:pager page="${requirementPage}" root="${root}" label="登録済みの必要条件" />
        </section>

        <section class="panel form-panel">
            <h2>必要条件を設定</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="requirement">
                <input type="hidden" name="industry" value="${fn:escapeXml(industryId)}">
                <div class="form-fields">
                    <mg:select id="requirement-qualification" name="qualificationId" label="資格・許認可・書類" choices="${qualificationChoices}" value="${qualificationId}" placeholder="選択してください" required="true" />
                    <mg:select id="requirement-required" name="required" label="必須区分" choices="${requiredChoices}" value="${current.required}" required="true" />
                    <mg:textarea id="requirement-condition" name="condition" label="適用条件" value="${current.conditionText}" maxLength="300" />
                </div>
                <button type="submit">設定を保存</button>
            </form>
        </section>

        <c:if test="${qualificationId ne null}">
            <section class="panel form-panel">
                <h2>関連付けを解除</h2>
                <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}" method="post" accept-charset="UTF-8">
                    <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                    <input type="hidden" name="operation" value="removeRequirement">
                    <input type="hidden" name="industry" value="${fn:escapeXml(industryId)}">
                    <input type="hidden" name="qualificationId" value="${fn:escapeXml(qualificationId)}">
                    <div class="form-fields">
                        <mg:input id="requirement-remove-reason" name="reason" label="解除理由" maxLength="2000" required="true" />
                    </div>
                    <button type="submit">この要件を解除</button>
                </form>
            </section>
        </c:if>
    </c:if>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
