<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_INQUIRIES)}">問い合わせ一覧へ</a>
</nav>
<div class="sections">
    <mg:details heading="受付内容" root="${root}" record="${model.inquiry()}" links="${{'送信者ID': ManagementPaths.ADMIN_USERS += '?id=', '物件ID': ManagementPaths.ADMIN_PROPERTIES += '?id='}}" />

    <mg:table heading="やり取り" root="${root}" pagination="${model.posts()}" columns="日時|投稿者|種類|内容" />

    <c:if test="${model.canReply()}">
        <section class="panel form-panel">
            <h2>担当者・状態</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_INQUIRIES)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="assign">
                <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
                <div class="form-fields">
                    <mg:select id="inquiry-assignee" name="assignee" label="担当者" choices="${model.admins()}" value="${model.inquiry()['担当者ID']}" placeholder="未割当" />
                    <mg:select id="inquiry-state" name="status" label="対応状態" choices="${model.states()}" value="${model.state()}" required="true" />
                </div>
                <button type="submit">設定を保存</button>
            </form>
        </section>

        <section class="panel form-panel">
            <h2>利用者に返信</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_INQUIRIES)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="reply">
                <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
                <div class="form-fields">
                    <mg:textarea id="admin-reply-body" name="body" label="返信内容" maxLength="8000" required="true" />
                    <mg:select id="admin-reply-state" name="status" label="返信後の状態" choices="${ManagementDisplay.options('IN_PROGRESS', '対応中', 'WAITING_USER', '利用者の返信待ち', 'CLOSED', '完了')}" value="WAITING_USER" required="true" />
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
            <input type="hidden" name="id" value="${fn:escapeXml(model.id())}">
            <div class="form-fields">
                <mg:textarea id="admin-inquiry-note" name="body" label="メモ" maxLength="8000" required="true" />
            </div>
            <button type="submit">メモを追加</button>
        </form>
    </section>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
