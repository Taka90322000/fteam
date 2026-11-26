<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_REPORTS)}">通報一覧へ</a>
</nav>
<div class="sections">
    <section class="panel">
        <h2>通報内容</h2>
        <dl class="details">
            <dt>受付番号</dt>
            <dd><c:out value="${report.reportId}" /></dd>

            <dt>通報者ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += report.reporterUserId)}"><c:out value="${report.reporterUserId}" /></a></dd>

            <dt>通報者</dt>
            <dd><c:out value="${ManagementFormat.text(report.reporterName)}" /></dd>

            <dt>対象会員ID</dt>
            <dd>
                <c:choose>
                    <c:when test="${report.targetUserId ne null}">
                        <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += report.targetUserId)}"><c:out value="${report.targetUserId}" /></a>
                    </c:when>
                    <c:otherwise>—</c:otherwise>
                </c:choose>
            </dd>

            <dt>対象物件ID</dt>
            <dd>
                <c:choose>
                    <c:when test="${report.targetPropertyId ne null}">
                        <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES += '?id=' += report.targetPropertyId)}"><c:out value="${report.targetPropertyId}" /></a>
                    </c:when>
                    <c:otherwise>—</c:otherwise>
                </c:choose>
            </dd>

            <dt>関連物件ID</dt>
            <dd>
                <c:choose>
                    <c:when test="${report.relatedPropertyId ne null}">
                        <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES += '?id=' += report.relatedPropertyId)}"><c:out value="${report.relatedPropertyId}" /></a>
                    </c:when>
                    <c:otherwise>—</c:otherwise>
                </c:choose>
            </dd>

            <dt>対象</dt>
            <dd><c:out value="${ManagementFormat.text(report.targetName)}" /></dd>

            <dt>理由</dt>
            <dd><c:out value="${ManagementFormat.text(report.reasonLabel)}" /></dd>

            <dt>内容</dt>
            <dd class="multiline"><c:out value="${ManagementFormat.text(report.description)}" /></dd>

            <dt>状態</dt>
            <dd><c:out value="${ManagementFormat.text(report.statusLabel)}" /></dd>

            <dt>担当者ID</dt>
            <dd><c:out value="${ManagementFormat.text(report.assignedAdminUserId)}" /></dd>

            <dt>担当者</dt>
            <dd><c:out value="${ManagementFormat.text(report.assignedAdminName)}" /></dd>

            <dt>受付日時</dt>
            <dd><c:out value="${ManagementFormat.text(report.createdAt)}" /></dd>

            <dt>対応結果</dt>
            <dd><c:out value="${ManagementFormat.text(report.resolutionLabel)}" /></dd>

            <dt>対応理由</dt>
            <dd class="multiline"><c:out value="${ManagementFormat.text(report.resolutionNote)}" /></dd>

            <dt>完了日時</dt>
            <dd><c:out value="${ManagementFormat.text(report.resolvedAt)}" /></dd>
        </dl>
    </section>

    <c:if test="${report.editable}">
        <section class="panel form-panel">
            <h2>確認・担当者設定</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_REPORTS)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="review">
                <input type="hidden" name="id" value="${fn:escapeXml(report.reportId)}">
                <div class="form-fields">
                    <mg:select id="report-assignee" name="assignee" label="担当者" choices="${admins}" value="${defaultAssignee}" required="true" />
                </div>
                <button type="submit">確認中にする</button>
            </form>
        </section>

        <section class="panel form-panel">
            <h2>対応結果</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_REPORTS)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="resolve">
                <input type="hidden" name="id" value="${fn:escapeXml(report.reportId)}">
                <div class="form-fields">
                    <mg:select id="report-resolution" name="resolution" label="対応結果" choices="${resolutions}" value="NO_ACTION" required="true" />
                    <mg:textarea id="report-resolution-note" name="reason" label="判断理由" maxLength="2000" required="true" />
                </div>
                <button type="submit">対応を完了する</button>
            </form>
        </section>
    </c:if>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
