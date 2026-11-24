<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="物件編集確認 | マチリク" />
  <jsp:param name="description" value="変更前と変更後の差分を確認し、物件情報の更新を確定します。" />
  <jsp:param name="pageScreenId" value="o07" />
  <jsp:param name="assetVersion" value="20261124_focus_v2" />
</jsp:include>

    <section id="o07-empty-state" class="screen-view empty-state" data-screen="O07" hidden>
      <h1 class="page-title">確認する変更内容がありません</h1>
      <p>登録物件管理一覧から物件を選び、編集画面から操作してください。</p>
      <a class="btn-primary" href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理へ戻る</a>
    </section>

    <section id="o07-section" class="screen-view o07-view" data-screen="O07">
      <div class="page-header-banner" data-screen="O07">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理</a>
          <span aria-hidden="true">/</span>
          <span aria-current="page">物件編集確認</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">物件編集確認画面</h1>
          <span class="badge-status status-reviewing">オーナー</span>
        </div>
        <p class="page-description">変更前と変更後の差分をご確認のうえ、更新を確定してください。</p>
      </div>

      <div class="form-stepper-box form-stepper-wide">
        <div id="o07-diff-summary" class="diff-summary-banner"></div>
        <div class="diff-table-wrap">
          <table class="specs-table diff-table" aria-label="変更内容の比較確認">
            <thead>
              <tr>
                <th scope="col">項目</th>
                <th scope="col">変更前</th>
                <th scope="col">変更後</th>
              </tr>
            </thead>
            <tbody id="o07-confirm-tbody"></tbody>
          </table>
        </div>

        <div class="form-actions-bar form-actions-split" style="margin-top:1.75rem;">
          <a id="o07-back-btn" class="btn-secondary" href="${pageContext.request.contextPath}/property/edit.jsp">← 内容を修正する</a>
          <button type="button" id="o07-commit-btn" class="btn-primary">この内容で更新を確定する</button>
        </div>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="o07" />
  <jsp:param name="assetVersion" value="20261124_focus_v2" />
</jsp:include>
