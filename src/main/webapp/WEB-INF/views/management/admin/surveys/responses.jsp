<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<c:set var="tab" value="${model.detail()}" />
<mg:table heading="満足度の分布" root="${root}" rows="${tab.ratings()}" />

<mg:table heading="選択肢の集計" root="${root}" rows="${tab.counts()}" />

<mg:table heading="回答履歴" root="${root}" pagination="${tab.answers()}" links="${{'回答者ID': ManagementPaths.ADMIN_USERS += '?id='}}" />
