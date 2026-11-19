<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="物件編集 | マチリク" />
  <jsp:param name="description" value="登録済み物件の賃料・設備・募集条件を変更します。" />
  <jsp:param name="pageScreenId" value="o06" />
  <jsp:param name="assetVersion" value="20261119_prop_v14" />
</jsp:include>

    <section id="o06-empty-state" class="screen-view empty-state" data-screen="O06" hidden>
      <h1 class="page-title">編集対象の物件が見つかりません</h1>
      <p>登録物件管理一覧から対象物件を選び直してください。</p>
      <a class="btn-primary" href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理へ戻る</a>
    </section>

    <section id="o06-section" class="screen-view s12-view" data-screen="O06">
      <div class="page-header-banner" data-screen="O06">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理</a>
          <span aria-hidden="true">/</span>
          <span aria-current="page">物件編集</span>
        </nav>
        <div class="page-title-row">
          <h1 id="o06-form-heading" class="page-title">物件編集画面</h1>
          <span class="badge-status status-voting">オーナー</span>
        </div>
        <p class="page-description">登録済み物件の賃料・設備・契約条件・公開ステータスを更新し、確認画面へ進みます。</p>
      </div>

      <div id="o06-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <button type="button" id="o06-switch-role-btn" class="btn-primary btn-sm">物件オーナーに切り替える</button>
      </div>

      <div class="form-stepper-box form-stepper-wide">
        <jsp:include page="/WEB-INF/views/property/_property_form.jsp">
          <jsp:param name="isEdit" value="true" />
          <jsp:param name="headingId" value="o06-form-heading" />
        </jsp:include>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="o06" />
  <jsp:param name="assetVersion" value="20261119_prop_v14" />
</jsp:include>
