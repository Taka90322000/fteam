<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<c:set var="current" value="${model.current()}" />
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<nav class="page-links" aria-label="この画面のメニュー">
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}">資格・書類マスター</a>
    <a href="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS += '?tab=requirements')}" aria-current="page">業種ごとの必要条件</a>
</nav>
<div class="sections">
    <section class="panel form-panel">
        <h2>対象業種</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}" method="get" accept-charset="UTF-8">
            <input type="hidden" name="tab" value="requirements">
            <div class="form-fields">
                <mg:select id="requirement-industry" name="industry" label="業種" choices="${model.industries()}" value="${model.industryId()}" placeholder="業種を選択" required="true" />
            </div>
            <button type="submit">表示する</button>
        </form>
    </section>

    <c:if test="${model.industryId() ne null}">
        <mg:table heading="登録済みの必要条件" root="${root}" pagination="${model.requirements()}" links="${{'ID': ManagementPaths.ADMIN_QUALIFICATIONS += '?tab=requirements&industry=' += model.industryId() += '&id='}}" />

        <section class="panel form-panel">
            <h2>必要条件を設定</h2>
            <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}" method="post" accept-charset="UTF-8">
                <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                <input type="hidden" name="operation" value="requirement">
                <input type="hidden" name="industry" value="${fn:escapeXml(model.industryId())}">
                <div class="form-fields">
                    <mg:select id="requirement-qualification" name="qualificationId" label="資格・許認可・書類" choices="${model.qualifications()}" value="${model.qualificationId()}" placeholder="選択してください" required="true" />
                    <mg:select id="requirement-required" name="required" label="必須区分" choices="${ManagementDisplay.options('true', '必須', 'false', '任意')}" value="${current.getOrDefault('is_required', true)}" required="true" />
                    <mg:textarea id="requirement-condition" name="condition" label="適用条件" value="${current['condition_text']}" maxLength="300" />
                </div>
                <button type="submit">設定を保存</button>
            </form>
        </section>

        <c:if test="${model.qualificationId() ne null}">
            <section class="panel form-panel">
                <h2>関連付けを解除</h2>
                <form action="${fn:escapeXml(root += ManagementPaths.ADMIN_QUALIFICATIONS)}" method="post" accept-charset="UTF-8">
                    <jsp:include page="/WEB-INF/views/management/common/csrf.jsp" />
                    <input type="hidden" name="operation" value="removeRequirement">
                    <input type="hidden" name="industry" value="${fn:escapeXml(model.industryId())}">
                    <input type="hidden" name="qualificationId" value="${fn:escapeXml(model.qualificationId())}">
                    <div class="form-fields">
                        <mg:input id="requirement-remove-reason" name="reason" label="解除理由" maxLength="2000" required="true" />
                    </div>
                    <button type="submit">この要件を解除</button>
                </form>
            </section>
        </c:if>
    </c:if>
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
