<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="jp.machiriku.servlet.management.ManagementPaths,jp.machiriku.servlet.management.ManagementFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="mg" tagdir="/WEB-INF/tags/management" %>
<c:set var="root" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INQUIRIES)}">問い合わせ一覧へ</a>
</nav>
<div class="sections">
    <section class="panel">
        <h2>受付内容</h2>
        <dl class="details">
            <dt>受付番号</dt>
            <dd><c:out value="${inquiry.inquiryId}" /></dd>

            <dt>件名</dt>
            <dd><c:out value="${ManagementFormat.text(inquiry.subject)}" /></dd>

            <dt>分類</dt>
            <dd><c:out value="${ManagementFormat.text(inquiry.categoryLabel)}" /></dd>

            <dt>状態</dt>
            <dd><c:out value="${ManagementFormat.text(inquiry.statusLabel)}" /></dd>

            <dt>受付日時</dt>
            <dd><c:out value="${ManagementFormat.text(inquiry.createdAt)}" /></dd>

            <dt>更新日時</dt>
            <dd><c:out value="${ManagementFormat.text(inquiry.updatedAt)}" /></dd>

            <dt>関連物件</dt>
            <dd><c:out value="${ManagementFormat.text(inquiry.relatedPropertyName)}" /></dd>

            <dt>送信者ID</dt>
            <dd><a href="${fn:escapeXml(root += ManagementPaths.ADMIN_USERS += '?id=' += inquiry.requesterUserId)}"><c:out value="${inquiry.requesterUserId}" /></a></dd>

            <dt>送信者</dt>
            <dd><c:out value="${ManagementFormat.text(inquiry.requesterName)}" /></dd>

            <dt>担当者ID</dt>
            <dd><c:out value="${ManagementFormat.text(inquiry.assignedAdminUserId)}" /></dd>

            <dt>担当者</dt>
            <dd><c:out value="${ManagementFormat.text(inquiry.assignedAdminName)}" /></dd>

            <dt>物件ID</dt>
            <dd>
                <c:choose>
                    <c:when test="${inquiry.relatedPropertyId ne null}">
                        <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_PROPERTIES += '?id=' += inquiry.relatedPropertyId)}"><c:out value="${inquiry.relatedPropertyId}" /></a>
                    </c:when>
                    <c:otherwise>—</c:otherwise>
                </c:choose>
            </dd>
        </dl>
    </section>

    <section class="panel">
        <h2>やり取り</h2>
        <c:choose>
            <c:when test="${empty messagePage.items}">
                <p class="empty">該当するデータはありません。</p>
            </c:when>
            <c:otherwise>
                <div class="table-scroll" role="region" aria-label="やり取り" tabindex="0">
                    <table aria-label="やり取り">
                        <thead>
                            <tr>
                                <th scope="col">日時</th>
                                <th scope="col">投稿者</th>
                                <th scope="col">種類</th>
                                <th scope="col">内容</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="msg" items="${messagePage.items}">
                                <tr>
                                    <td><c:out value="${ManagementFormat.text(msg.createdAt)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(msg.senderLabel)}" /></td>
                                    <td><c:out value="${ManagementFormat.text(msg.kindLabel)}" /></td>
                                    <td class="multiline"><c:out value="${ManagementFormat.text(msg.body)}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
        <mg:pager page="${messagePage}" root="${root}" label="やり取り" />
    </section>

    <c:if test="${inquiry.canReply}">
        <section class="panel form-panel">
            <h2>担当者・状態</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_INQUIRIES)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="assign">
                <input type="hidden" name="id" value="${fn:escapeXml(inquiry.inquiryId)}">
                <div class="form-fields">
                    <mg:select id="inquiry-assignee" name="assignee" label="担当者" choices="${admins}" value="${inquiry.assignedAdminUserId}" placeholder="未割当" />
                    <mg:select id="inquiry-state" name="status" label="対応状態" choices="${states}" value="${inquiry.status}" required="true" />
                </div>
                <button type="submit">設定を保存</button>
            </form>
        </section>

        <section class="panel form-panel">
            <h2>利用者に返信</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_INQUIRIES)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="reply">
                <input type="hidden" name="id" value="${fn:escapeXml(inquiry.inquiryId)}">
                <div class="form-fields">
                    <mg:textarea id="admin-reply-body" name="body" label="返信内容" maxLength="8000" required="true" />
                    <mg:select id="admin-reply-state" name="status" label="返信後の状態" choices="${replyStates}" value="WAITING_USER" required="true" />
                </div>
                <button type="submit">返信する</button>
            </form>
        </section>
    </c:if>

    <section class="panel form-panel">
        <h2>管理者用メモ</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_INQUIRIES)}" method="post" accept-charset="UTF-8">
            <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
            <input type="hidden" name="operation" value="note">
            <input type="hidden" name="id" value="${fn:escapeXml(inquiry.inquiryId)}">
            <div class="form-fields">
                <mg:textarea id="admin-inquiry-note" name="body" label="メモ" maxLength="8000" required="true" />
            </div>
            <button type="submit">メモを追加</button>
        </form>
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
