<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<section class="panel">
    <h2>保有資格・許認可</h2>
    <c:choose>
        <c:when test="${empty qualifications}">
            <p class="empty">該当するデータはありません。</p>
        </c:when>
        <c:otherwise>
            <div class="table-scroll" role="region" aria-label="保有資格・許認可" tabindex="0">
                <table aria-label="保有資格・許認可">
                    <thead>
                        <tr>
                            <th scope="col">ID</th>
                            <th scope="col">名前</th>
                            <th scope="col">種類</th>
                            <th scope="col">資格番号</th>
                            <th scope="col">取得日</th>
                            <th scope="col">有効期限</th>
                            <th scope="col">有効</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="qual" items="${qualifications}">
                            <tr>
                                <td><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += member.userId += '&tab=qualifications&industry=' += qual.qualificationId)}"><c:out value="${qual.qualificationId}" /></a></td>
                                <td><c:out value="${ManagementFormat.text(qual.qualificationName)}" /></td>
                                <td><c:out value="${ManagementFormat.text(qual.requirementKindLabel)}" /></td>
                                <td><c:out value="${ManagementFormat.text(qual.credentialNumber)}" /></td>
                                <td><c:out value="${ManagementFormat.text(qual.obtainedOn)}" /></td>
                                <td><c:out value="${ManagementFormat.text(qual.expiresOn)}" /></td>
                                <td><c:out value="${ManagementFormat.text(qual.active)}" /></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<c:if test="${editable}">
    <c:if test="${editingQualification}">
        <nav class="page-links" aria-label="資格編集の切り替え">
            <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += member.userId += '&tab=qualifications')}">新しい資格を追加</a>
        </nav>
    </c:if>
    <section class="panel form-panel">
        <h2><c:out value="${editingQualification ? '資格情報を更新' : '資格情報を追加'}" /></h2>
        <c:choose>
            <c:when test="${not editingQualification and empty qualificationChoices}">
                <p class="empty">追加できる資格・許認可はありません。登録済みの項目を編集するには一覧のIDを選択してください。</p>
            </c:when>
            <c:otherwise>
                <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS)}" method="post" accept-charset="UTF-8">
                    <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                    <input type="hidden" name="operation" value="qualification">
                    <input type="hidden" name="id" value="${fn:escapeXml(member.userId)}">
                    <div class="form-fields">
                        <c:choose>
                            <c:when test="${editingQualification}">
                                <%-- 編集対象を固定し、別資格への誤上書きを防ぐ。 --%>
                                <input type="hidden" name="qualificationId" value="${fn:escapeXml(currentQualification.qualificationId)}">
                                <div class="field">
                                    <label for="user-qualification-readonly">資格・許認可</label>
                                    <input id="user-qualification-readonly" type="text" value="${fn:escapeXml(currentQualification.qualificationName)}" readonly>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <mg:select id="user-qualification" name="qualificationId" label="資格・許認可" choices="${qualificationChoices}" required="true" />
                            </c:otherwise>
                        </c:choose>
                        <mg:input id="user-credential" name="credential" label="資格番号" value="${currentQualification.credentialNumber}" maxLength="100" />
                        <mg:input id="user-obtained" name="obtained" label="取得日" type="date" value="${currentQualification.obtainedOn}" />
                        <mg:input id="user-expires" name="expires" label="有効期限" type="date" value="${currentQualification.expiresOn}" />
                        <mg:select id="user-qualification-active" name="active" label="有効状態" choices="${activeChoices}" value="${currentQualification.active}" required="true" />
                    </div>
                    <button type="submit"><c:out value="${editingQualification ? '資格情報を更新' : '資格情報を追加'}" /></button>
                </form>
            </c:otherwise>
        </c:choose>
    </section>
</c:if>
