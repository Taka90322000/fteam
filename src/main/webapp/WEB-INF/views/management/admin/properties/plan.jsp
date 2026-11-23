<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <mg:details heading="詳細" root="${root}" record="${model.record()}"
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

    <c:if test="${model.canArchive()}">
        <section class="panel form-panel">
            <h2>出店計画の公開を停止</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_PLANS)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="archive">
                <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
                <div class="form-fields">
                    <mg:textarea id="plan-archive-reason" name="reason" label="停止理由" maxLength="2000" required="true" />
                </div>
                <button type="submit">公開を停止する</button>
            </form>
        </section>
    </c:if>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
