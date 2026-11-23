<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/management/common/view.jspf" %>
<jsp:include page="/WEB-INF/views/management/common/header.jsp" />
<div class="sections">
    <section class="panel form-panel">
        <h2>対象業種</h2>
        <form action="${fn:escapeXml(root += ManagementPaths.REQUIREMENTS)}" method="get" accept-charset="UTF-8">
            <div class="form-fields">
                <mg:select id="public-requirement-industry" name="industry" label="業種" choices="${model.industries()}" value="${model.industryId()}" placeholder="すべての業種" />
            </div>
            <button type="submit">表示する</button>
        </form>
    </section>

    <mg:table heading="業種別の要件" root="${root}" pagination="${model.requirements()}" columns="業種|名称|種別|必要区分|適用条件|案内|案内URL" />
</div>
<jsp:include page="/WEB-INF/views/management/common/footer.jsp" />
