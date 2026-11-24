<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="出店希望確認 | マチリク" />
  <jsp:param name="description" value="事業主から届いた出店応募の想い・保有資格を確認し、面談や承認・返信を行います。" />
  <jsp:param name="pageScreenId" value="o03" />
  <jsp:param name="assetVersion" value="20261124_focus_v2" />
</jsp:include>

    <section id="o03-section" class="screen-view s09-view" data-screen="O03">
      <div class="page-header-banner" data-screen="O03">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理</a>
          <span aria-hidden="true">/</span>
          <span aria-current="page">出店希望確認</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">出店希望確認・応募管理</h1>
          <span class="badge-status status-reviewing">オーナー</span>
        </div>
        <p class="page-description">事業主から届いた応募内容・必要資格の登録状況を確認し、面談連絡や承認・見送りを行います。</p>
      </div>

      <div id="o03-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <button type="button" class="btn-primary btn-sm" onclick="MachirikuStore.setRole('owner')">物件オーナーに切り替える</button>
      </div>

      <div class="apply-list-toolbar">
        <div class="apply-filters-group">
          <select id="o03-prop-filter" class="form-select" aria-label="対象物件で絞り込み">
            <option value="ALL">すべての物件</option>
          </select>
          <select id="o03-status-filter" class="form-select" aria-label="応募ステータスで絞り込み">
            <option value="ALL">すべてのステータス</option>
            <option value="submitted">新規応募受付中</option>
            <option value="consulting">面談・相談中</option>
            <option value="approved">出店合意</option>
            <option value="contracted">契約成立</option>
            <option value="rejected">見送り</option>
          </select>
        </div>
        <div class="apply-count-badge">表示件数: <strong id="o03-total-count">0</strong> 件</div>
      </div>

      <div id="o03-empty-box" class="detail-card manage-empty-card" hidden>
        <h2>まだ出店応募は届いていません</h2>
        <p>物件詳細画面の「この物件に出店を申し込む」から事業主が応募を送信すると、ここに一覧表示されます。</p>
        <div class="manage-empty-actions">
          <a class="btn-secondary" href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理へ戻る</a>
          <a class="btn-primary" href="${pageContext.request.contextPath}/property/apply.jsp">出店応募画面を試す</a>
        </div>
      </div>

      <div id="o03-app-list" class="apply-cards-stack"></div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="o03" />
  <jsp:param name="assetVersion" value="20261124_focus_v2" />
</jsp:include>
