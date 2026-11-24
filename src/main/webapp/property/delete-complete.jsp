<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="物件削除完了 | マチリク" />
  <jsp:param name="description" value="登録物件の削除処理が完了しました。" />
  <jsp:param name="pageScreenId" value="o09" />
  <jsp:param name="assetVersion" value="20261124_focus_v2" />
</jsp:include>

    <section id="o09-section" class="screen-view" data-screen="O09">
      <div class="page-header-banner" data-screen="O09">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理</a>
          <span class="sep">/</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">物件削除完了画面</h1>
          <span class="badge-status status-voting">完了</span>
        </div>
      </div>

      <div class="form-stepper-box complete-state-card">
        <div class="complete-badge-icon" aria-hidden="true">✓</div>
        <h2>物件の削除が完了しました</h2>
        <p>対象物件は検索一覧・登録物件管理から除外され、公開を終了しました。</p>

        <table class="specs-table" style="margin:1.25rem auto; text-align:left;" aria-label="削除された物件の記録">
          <tbody>
            <tr><th scope="row">削除した物件名</th><td id="o09-deleted-title">-</td></tr>
            <tr><th scope="row">管理コード</th><td id="o09-deleted-code">-</td></tr>
            <tr><th scope="row">削除完了日時</th><td id="o09-deleted-at">-</td></tr>
          </tbody>
        </table>

        <div class="complete-actions">
          <a class="btn-primary" href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理に戻る</a>
          <a class="btn-secondary" href="${pageContext.request.contextPath}/index.jsp#s01">物件検索ホームへ</a>
        </div>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="o09" />
  <jsp:param name="assetVersion" value="20261124_focus_v2" />
</jsp:include>
