<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.Map,jp.machiriku.profile.ProfileIconStore,jp.machiriku.servlet.ProfileIconSupport,jp.machiriku.tool.Html" %>
<%
  // DBから復元された参照先だけをプレビューに使用する。
  Map<?, ?> iconFields = request.getAttribute("formValues") instanceof Map
      ? (Map<?, ?>) request.getAttribute("formValues") : Map.of();
  String iconReference = (String) iconFields.get("iconImageUrl");
  String savedIcon = ProfileIconSupport.previewSource(request.getContextPath(), iconReference);
%>
<div class="form-group profile-icon-field" data-profile-icon>
  <p class="profile-icon-label">アイコン画像 <span>（任意）</span></p>
  <div class="profile-icon-input">
    <div class="profile-icon-thumbnail">
      <span aria-hidden="true">未設定</span>
      <img alt="アイコンのプレビュー" referrerpolicy="no-referrer" data-icon-preview data-saved-icon="<%= Html.escape(savedIcon) %>"
           <%= savedIcon.isEmpty() ? "hidden" : "src=\"" + Html.escape(savedIcon) + "\"" %>>
    </div>
    <div class="profile-icon-options">
      <%-- 元の写真は名前のない入力欄に置き、フォームには確定後のPNGを添付する。 --%>
      <input type="file" id="profile-icon-file" class="profile-icon-file" data-icon-file
             aria-describedby="profile-icon-help"
             accept="image/jpeg,image/png,image/webp,image/gif,image/avif,image/bmp,image/heic,image/heif,image/heic-sequence,image/heif-sequence,.heic,.heif">
      <label for="profile-icon-file" class="profile-icon-button profile-icon-select">画像を選択</label>
      <button class="profile-icon-button" type="button" data-icon-clear hidden>選択を取り消す</button>
      <% if (!savedIcon.isEmpty()) { %>
        <label class="profile-icon-remove"><input type="checkbox" name="removeIcon" value="1" data-icon-remove> 保存時にアイコンを削除</label>
      <% } %>
    </div>
  </div>
  <p id="profile-icon-help" class="profile-icon-help">JPEG・PNG・WebP・HEIC／HEIFなど、30MBまで</p>
  <p class="profile-icon-status" role="status" data-icon-status><%= Boolean.TRUE.equals(request.getAttribute("iconNeedsReselection")) ? "画像を保存するには、もう一度選択してください。" : "" %></p>

  <dialog class="profile-icon-dialog" aria-labelledby="profile-icon-dialog-title" data-icon-dialog>
    <h2 id="profile-icon-dialog-title">アイコンの切り抜き</h2>
    <p class="profile-icon-help">画像をドラッグして、円の中に収まる範囲を調整してください。</p>
    <div class="profile-icon-stage">
      <canvas width="<%= ProfileIconStore.PIXELS %>" height="<%= ProfileIconStore.PIXELS %>" tabindex="0" data-icon-crop
              aria-label="切り抜きのプレビュー。矢印キーで位置を変更できます"></canvas>
    </div>
    <p class="profile-icon-dialog-status" role="status" data-icon-dialog-status></p>
    <div class="profile-icon-adjustments">
      <div class="profile-icon-zoom">
        <label for="profile-icon-zoom">拡大</label>
        <input type="range" id="profile-icon-zoom" min="1" max="4" step="0.01" value="1" data-icon-zoom disabled>
      </div>
      <div class="profile-icon-rotation">
        <button class="profile-icon-button" type="button" data-icon-rotate="-1" disabled>左へ90°</button>
        <button class="profile-icon-button" type="button" data-icon-rotate="1" disabled>右へ90°</button>
      </div>
    </div>
    <div class="profile-icon-dialog-actions">
      <button class="profile-icon-button" type="button" data-icon-cancel>キャンセル</button>
      <button class="profile-icon-button profile-icon-apply" type="button" data-icon-confirm disabled>この画像を使う</button>
    </div>
  </dialog>
</div>
<script type="module" src="${pageContext.request.contextPath}/js/profile-icon.js"></script>
