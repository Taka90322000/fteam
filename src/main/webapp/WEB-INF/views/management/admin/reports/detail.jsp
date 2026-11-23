<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_REPORTS)}">通報一覧へ</a>
</nav>
<div class="sections">
    <mg:details heading="通報内容" root="${root}" record="${model.report()}"
      links="${{
          '通報者ID': ManagementPaths.ADMIN_USERS += '?id=',
          '対象会員ID': ManagementPaths.ADMIN_USERS += '?id=',
          '対象物件ID': ManagementPaths.ADMIN_PROPERTIES += '?id=',
          '関連物件ID': ManagementPaths.ADMIN_PROPERTIES += '?id='
      }}" />

    <c:if test="${model.editable()}">
        <section class="panel form-panel">
            <h2>確認・担当者設定</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_REPORTS)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="review">
                <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
                <div class="form-fields">
                    <mg:select id="report-assignee" name="assignee" label="担当者" choices="${model.admins()}" value="${model.assignee()}" required="true" />
                </div>
                <button type="submit">確認中にする</button>
            </form>
        </section>

        <section class="panel form-panel">
            <h2>対応結果</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_REPORTS)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="resolve">
                <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
                <div class="form-fields">
                    <mg:select id="report-resolution" name="resolution" label="対応結果" choices="${model.resolutions()}" value="NO_ACTION" required="true" />
                    <mg:textarea id="report-resolution-note" name="reason" label="判断理由" maxLength="2000" required="true" />
                </div>
                <button type="submit">対応を完了する</button>
            </form>
        </section>
    </c:if>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
