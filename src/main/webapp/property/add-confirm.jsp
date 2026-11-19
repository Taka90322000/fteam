<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="物件登録確認 | マチリク" />
  <jsp:param name="description" value="新規登録する空き店舗の入力内容を確認し、登録を確定します。" />
  <jsp:param name="pageScreenId" value="o05" />
  <jsp:param name="assetVersion" value="20261119_o05_v9" />
</jsp:include>

    <section id="o05-empty-state" class="screen-view empty-state" data-screen="O05" hidden>
      <h1 class="page-title">確認する登録内容がありません</h1>
      <p>物件登録画面から情報を入力のうえお進みください。</p>
      <a class="btn-primary" href="${pageContext.request.contextPath}/property/add.jsp">物件登録へ戻る</a>
    </section>

    <section id="o05-section" class="screen-view o05-view" data-screen="O05">
      <div class="page-header-banner" data-screen="O05">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/property/add.jsp?restore=1">物件登録</a>
          <span aria-hidden="true">/</span>
          <span aria-current="page">物件登録確認</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">物件登録確認</h1>
          <span class="badge-status status-reviewing">オーナー</span>
        </div>
        <p class="page-description">以下の内容で空き店舗を登録します。修正が必要な場合は「内容を修正する」で入力画面に戻れます。</p>
      </div>

      <div class="form-stepper-box form-stepper-wide">
        <h2 class="detail-card-title">入力内容の確認</h2>
        <table class="specs-table" aria-label="登録物件の入力内容確認">
          <tbody id="o05-confirm-tbody"></tbody>
        </table>

        <h3 class="detail-sub-heading" style="margin-top:1.5rem;">登録写真プレビュー</h3>
        <div id="o05-gallery-preview" class="confirm-photo-grid"></div>

        <div class="form-actions-bar" style="margin-top:1.75rem;">
          <a id="o05-back-btn" class="btn-secondary" href="${pageContext.request.contextPath}/property/add.jsp?restore=1">← 内容を修正する</a>
          <button type="button" id="o05-commit-btn" class="btn-primary">この内容で登録を確定する</button>
        </div>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="o05" />
  <jsp:param name="assetVersion" value="20261119_o05_v9" />
</jsp:include>
