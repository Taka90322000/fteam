<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="/WEB-INF/views/common/portal_header.jsp">
  <jsp:param name="title" value="物件削除確認 | マチリク" />
  <jsp:param name="description" value="登録済み物件の削除意図と影響件数を再確認します。" />
  <jsp:param name="pageScreenId" value="o08" />
  <jsp:param name="assetVersion" value="20261124_focus_v2" />
</jsp:include>

    <section id="o08-empty-state" class="screen-view empty-state" data-screen="O08" hidden>
      <h1 class="page-title">削除対象の物件が見つかりません</h1>
      <p>既に削除されているか、URLが正しくありません。</p>
      <a class="btn-primary" href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理へ戻る</a>
    </section>

    <section id="o08-section" class="screen-view o08-view" data-screen="O08">
      <div class="page-header-banner" data-screen="O08">
        <nav class="breadcrumb-trail" aria-label="現在の位置">
          <a href="${pageContext.request.contextPath}/index.jsp#s01">ホーム</a>
          <span aria-hidden="true">/</span>
          <a href="${pageContext.request.contextPath}/property/manage.jsp">登録物件管理</a>
          <span aria-hidden="true">/</span>
          <span aria-current="page">物件削除確認</span>
        </nav>
        <div class="page-title-row">
          <h1 class="page-title">物件削除確認画面</h1>
          <span class="badge-status status-paused">削除確認</span>
        </div>
      </div>

      <div id="o08-role-notice" class="role-switch-notice" hidden>
        <span data-role-notice-text></span>
        <button type="button" class="btn-primary btn-sm" onclick="MachirikuStore.setRole('owner')">物件オーナーに切り替える</button>
      </div>

      <div class="form-stepper-box">
        <h2 class="detail-card-title">削除対象物件の情報</h2>
        <table class="specs-table" aria-label="削除対象物件">
          <tbody>
            <tr><th scope="row">物件名称</th><td id="o08-prop-title"></td></tr>
            <tr><th scope="row">管理コード</th><td id="o08-prop-code"></td></tr>
            <tr><th scope="row">所在地</th><td id="o08-prop-address"></td></tr>
            <tr><th scope="row">賃料・面積</th><td id="o08-prop-rent"></td></tr>
            <tr><th scope="row">現在の状態</th><td id="o08-prop-status"></td></tr>
          </tbody>
        </table>

        <div class="delete-impact-warning" role="alert">
          <h3>削除による影響・注意事項</h3>
          <p>物件を削除すると検索一覧・地図から非公開（論理削除）となり、以降の住民投票・新規応募の受付が停止します。</p>
          <ul class="delete-impact-metrics">
            <li>現在の住民投票数：<strong id="o08-impact-votes">0 票</strong></li>
            <li>進行中・受信済みの応募：<strong id="o08-impact-apps">0 件</strong></li>
            <li>フォロー中の住民：<strong id="o08-impact-follows">0 人</strong></li>
          </ul>
          <label class="delete-confirm-checkbox">
            <input type="checkbox" id="o08-confirm-check">
            <span>上記の影響と非公開化に同意のうえ、この物件を削除します</span>
          </label>
        </div>

        <div class="form-actions-bar" style="margin-top:1.5rem;">
          <a class="btn-secondary" href="${pageContext.request.contextPath}/property/manage.jsp">キャンセルして一覧へ戻る</a>
          <button type="button" id="o08-delete-btn" class="btn-danger" disabled>物件を削除する</button>
        </div>
      </div>
    </section>

<jsp:include page="/WEB-INF/views/common/portal_footer.jsp">
  <jsp:param name="pageScreenId" value="o08" />
  <jsp:param name="assetVersion" value="20261124_focus_v2" />
</jsp:include>
