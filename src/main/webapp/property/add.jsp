<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="物件登録 | マチリク" />
  <jsp:param name="description" value="新しい空き店舗の物件情報・設備・契約条件を入力して確認画面へ進みます。" />
  <jsp:param name="pageScreenId" value="o04" />
  <jsp:param name="assetVersion" value="20261117_prop_pages" />
</jsp:include>

    <section id="o04-section" class="screen-view s12-view" data-screen="O04">
      <div class="page-header-banner" data-screen="O04">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理</a>
          <span class="sep">/</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">物件登録</h1>
          <span class="badge-status status-voting">オーナー</span>
        </div>
        <p class="page-description">新しい空き店舗の情報を入力します。登録確認画面で内容をご確認のうえ公開・保存できます。</p>
      </div>

      <div id="o04-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <button type="button" class="btn-primary btn-sm" onclick="MachirikuStore.setRole('owner')">物件オーナーに切り替える</button>
      </div>

      <div class="form-stepper-box form-stepper-wide">
        <jsp:include page="/WEB-INF/views/property/_property_form.jsp">
          <jsp:param name="isEdit" value="false" />
        </jsp:include>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="o04" />
  <jsp:param name="assetVersion" value="20261117_prop_pages" />
</jsp:include>
