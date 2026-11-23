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
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
