<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<c:set var="member" value="${model.member()}" />
<c:set var="tab" value="${model.detail()}" />
<c:set var="profile" value="${tab.profile()}" />
<mg:details heading="区分別プロフィール" root="${root}" record="${profile}" />

<c:if test="${tab.editable()}">
    <section class="panel form-panel">
        <h2>プロフィールの修正</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="profile">
            <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
            <div class="form-fields">
                <mg:input id="profile-name" name="name" label="表示名" value="${member['名前']}" maxLength="100" required="true" />
                <mg:input id="profile-email" name="email" label="メール" type="email" value="${member['メール']}" maxLength="254" />
                <c:if test="${not empty profile}">
                    <c:choose>
                        <c:when test="${'RESIDENT' eq model.role()}">
                            <mg:select id="profile-age" name="age" label="年齢区分" choices="${ManagementDisplay.options('10代', '10代', '20代', '20代', '30代', '30代', '40代', '40代', '50代', '50代', '60代以上', '60代以上')}" value="${profile['年齢区分']}" placeholder="未設定" />
                            <mg:select id="profile-gender" name="gender" label="性別" choices="${ManagementDisplay.options('男性', '男性', '女性', '女性', 'その他', 'その他')}" value="${profile['性別']}" placeholder="未設定" />
                        </c:when>
                        <c:otherwise>
                            <mg:input id="profile-representative" name="representative" label="代表者名" value="${profile['代表者']}" maxLength="100" required="true" />
                            <mg:input id="profile-phone" name="phone" label="主な電話番号" type="tel" value="${profile['電話番号']}" maxLength="50" required="true" />
                            <mg:input id="profile-secondary-phone" name="secondaryPhone" label="追加の電話番号" type="tel" value="${profile['追加電話番号']}" maxLength="50" />
                            <c:choose>
                                <c:when test="${'OWNER' eq model.role()}">
                                    <mg:input id="profile-company" name="company" label="法人名" value="${profile['法人名']}" maxLength="150" />
                                </c:when>
                                <c:otherwise>
                                    <mg:input id="profile-business" name="businessName" label="事業者名" value="${profile['事業者名']}" maxLength="150" required="true" />
                                    <mg:input id="profile-area" name="areaName" label="希望エリア名" value="${profile['希望エリア']}" maxLength="150" required="true" />
                                    <mg:input id="profile-employees" name="employees" label="従業員数" type="number" value="${profile['従業員数']}" required="true" />
                                    <mg:textarea id="profile-industry-experience" name="industryExperience" label="業界経験" value="${profile['業界経験']}" maxLength="4000" />
                                    <mg:textarea id="profile-management-experience" name="managementExperience" label="経営経験" value="${profile['経営経験']}" maxLength="4000" />
                                </c:otherwise>
                            </c:choose>
                        </c:otherwise>
                    </c:choose>
                </c:if>
            </div>
            <button type="submit">変更を保存</button>
        </form>
    </section>

    <c:if test="${tab.canChangeStatus()}">
        <section class="panel form-panel">
            <h2><c:out value="${tab.suspended() ? '利用停止の解除' : '利用停止'}" /></h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="${fn:escapeXml(tab.suspended() ? 'restore' : 'suspend')}">
                <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
                <div class="form-fields">
                    <mg:textarea id="profile-suspension-reason" name="reason" label="操作理由" maxLength="2000" required="true" />
                </div>
                <button type="submit"><c:out value="${tab.suspended() ? '利用停止を解除する' : '利用を停止する'}" /></button>
            </form>
        </section>
    </c:if>

    <mg:table heading="操作履歴（最新10件）" root="${root}" rows="${tab.actions()}" />
</c:if>
