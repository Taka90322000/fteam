<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<section class="panel">
    <h2>区分別プロフィール</h2>
    <c:choose>
        <c:when test="${profile eq null}">
            <p class="empty">該当するデータはありません。</p>
        </c:when>
        <c:when test="${'RESIDENT' eq member.userRole}">
            <dl class="details">
                <dt>年齢区分</dt>
                <dd><c:out value="${ManagementFormat.text(profile.ageGroup)}" /></dd>

                <dt>性別</dt>
                <dd><c:out value="${ManagementFormat.text(profile.gender)}" /></dd>
            </dl>
        </c:when>
        <c:when test="${'OWNER' eq member.userRole}">
            <dl class="details">
                <dt>代表者</dt>
                <dd><c:out value="${ManagementFormat.text(profile.representativeName)}" /></dd>

                <dt>法人名</dt>
                <dd><c:out value="${ManagementFormat.text(profile.companyName)}" /></dd>

                <dt>電話番号</dt>
                <dd><c:out value="${ManagementFormat.text(profile.phoneE164)}" /></dd>

                <dt>追加電話番号</dt>
                <dd><c:out value="${ManagementFormat.text(profile.secondaryPhoneE164)}" /></dd>

                <dt>アイコン画像</dt>
                <dd><c:choose><c:when test="${ManagementFormat.isWebUrl(profile.iconImageUrl)}"><a href="${fn:escapeXml(profile.iconImageUrl)}" target="_blank" rel="noopener noreferrer"><c:out value="${profile.iconImageUrl}" /></a></c:when><c:otherwise><c:out value="${ManagementFormat.text(profile.iconImageUrl)}" /></c:otherwise></c:choose></dd>
            </dl>
        </c:when>
        <c:otherwise>
            <dl class="details">
                <dt>事業者名</dt>
                <dd><c:out value="${ManagementFormat.text(profile.businessName)}" /></dd>

                <dt>代表者</dt>
                <dd><c:out value="${ManagementFormat.text(profile.representativeName)}" /></dd>

                <dt>電話番号</dt>
                <dd><c:out value="${ManagementFormat.text(profile.phoneE164)}" /></dd>

                <dt>追加電話番号</dt>
                <dd><c:out value="${ManagementFormat.text(profile.secondaryPhoneE164)}" /></dd>

                <dt>希望エリア</dt>
                <dd><c:out value="${ManagementFormat.text(profile.preferredArea)}" /></dd>

                <dt>従業員数</dt>
                <dd><c:out value="${ManagementFormat.text(profile.employeeCount)}" /></dd>

                <dt>業界経験</dt>
                <dd class="multiline"><c:out value="${ManagementFormat.text(profile.industryExperience)}" /></dd>

                <dt>経営経験</dt>
                <dd class="multiline"><c:out value="${ManagementFormat.text(profile.managementExperience)}" /></dd>

                <dt>アイコン画像</dt>
                <dd><c:choose><c:when test="${ManagementFormat.isWebUrl(profile.iconImageUrl)}"><a href="${fn:escapeXml(profile.iconImageUrl)}" target="_blank" rel="noopener noreferrer"><c:out value="${profile.iconImageUrl}" /></a></c:when><c:otherwise><c:out value="${ManagementFormat.text(profile.iconImageUrl)}" /></c:otherwise></c:choose></dd>
            </dl>
        </c:otherwise>
    </c:choose>
</section>

<c:if test="${editable}">
    <section class="panel form-panel">
        <h2>プロフィールの修正</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="profile">
            <input type="hidden" name="id" value="${fn:escapeXml(member.userId)}">
            <div class="form-fields">
                <mg:input id="profile-name" name="name" label="表示名" value="${member.displayName}" maxLength="100" required="true" />
                <mg:input id="profile-email" name="email" label="メール" type="email" value="${member.email}" maxLength="254" />
                <c:if test="${profile ne null}">
                    <c:choose>
                        <c:when test="${'RESIDENT' eq member.userRole}">
                            <mg:select id="profile-age" name="age" label="年齢区分" choices="${ageChoices}" value="${profile.ageGroup}" placeholder="未設定" />
                            <mg:select id="profile-gender" name="gender" label="性別" choices="${genderChoices}" value="${profile.gender}" placeholder="未設定" />
                        </c:when>
                        <c:otherwise>
                            <mg:input id="profile-representative" name="representative" label="代表者名" value="${profile.representativeName}" maxLength="100" required="true" />
                            <mg:input id="profile-phone" name="phone" label="主な電話番号" type="tel" value="${profile.phoneE164}" maxLength="50" required="true" />
                            <mg:input id="profile-secondary-phone" name="secondaryPhone" label="追加の電話番号" type="tel" value="${profile.secondaryPhoneE164}" maxLength="50" />
                            <c:choose>
                                <c:when test="${'OWNER' eq member.userRole}">
                                    <mg:input id="profile-company" name="company" label="法人名" value="${profile.companyName}" maxLength="150" />
                                </c:when>
                                <c:otherwise>
                                    <mg:input id="profile-business" name="businessName" label="事業者名" value="${profile.businessName}" maxLength="150" required="true" />
                                    <mg:input id="profile-area" name="areaName" label="希望エリア名" value="${profile.preferredArea}" maxLength="150" required="true" />
                                    <mg:input id="profile-employees" name="employees" label="従業員数" type="number" value="${profile.employeeCount}" required="true" />
                                    <mg:select id="profile-industry-experience" name="industryExperience" label="業界経験" choices="${industryExperienceChoices}" value="${profile.industryExperience}" placeholder="未設定" />
                                    <mg:select id="profile-management-experience" name="managementExperience" label="経営経験" choices="${managementExperienceChoices}" value="${profile.managementExperience}" placeholder="未設定" />
                                </c:otherwise>
                            </c:choose>
                        </c:otherwise>
                    </c:choose>
                </c:if>
            </div>
            <button type="submit">変更を保存</button>
        </form>
    </section>

    <c:if test="${canChangeStatus}">
        <section class="panel form-panel">
            <h2><c:out value="${member.suspended ? '利用停止の解除' : '利用停止'}" /></h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="${fn:escapeXml(member.suspended ? 'restore' : 'suspend')}">
                <input type="hidden" name="id" value="${fn:escapeXml(member.userId)}">
                <div class="form-fields">
                    <mg:textarea id="profile-suspension-reason" name="reason" label="操作理由" maxLength="2000" required="true" />
                </div>
                <button type="submit"><c:out value="${member.suspended ? '利用停止を解除する' : '利用を停止する'}" /></button>
            </form>
        </section>
    </c:if>
</c:if>

<section class="panel">
    <h2>操作履歴（最新10件）</h2>
    <c:choose>
        <c:when test="${empty accountActions}">
            <p class="empty">該当するデータはありません。</p>
        </c:when>
        <c:otherwise>
            <div class="table-scroll" role="region" aria-label="操作履歴（最新10件）" tabindex="0">
                <table aria-label="操作履歴（最新10件）">
                    <thead>
                        <tr>
                            <th scope="col">管理者</th>
                            <th scope="col">操作</th>
                            <th scope="col">理由</th>
                            <th scope="col">日時</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="log" items="${accountActions}">
                            <tr>
                                <td><c:out value="${ManagementFormat.text(log.actorName)}" /></td>
                                <td><c:out value="${ManagementFormat.text(log.action)}" /></td>
                                <td class="multiline"><c:out value="${ManagementFormat.text(log.reason)}" /></td>
                                <td><c:out value="${ManagementFormat.text(log.occurredAt)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>
