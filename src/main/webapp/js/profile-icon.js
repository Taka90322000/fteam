// スマホで撮った高解像度の写真も選べるよう、元ファイルには切り抜き後より大きい上限を設ける。
const MAX_SOURCE_BYTES = 30 * 1024 * 1024;
const field = document.querySelector('[data-profile-icon]');
if (field) initializeIconField(field);

/**
 * ブラウザーで表示できる画像を、撮影時の向きで読み込む。
 * @param {Blob} blob 選択した画像または変換済みの画像
 * @returns {Promise<HTMLImageElement>} キャンバスに描画できる画像
 */
async function readBrowserImage(blob) {
  // 選択した写真をネットワークへ送らず、ブラウザー内のURLで参照する。
  const url = URL.createObjectURL(blob);
  const image = new Image();
  try {
    image.src = url;
    await image.decode();
    return image;
  } finally {
    URL.revokeObjectURL(url);
  }
}

/**
 * ブラウザーの標準機能または同梱デコーダーで画像を読み込む。
 * @param {File} file 選択した元ファイル
 * @returns {Promise<HTMLImageElement|ImageBitmap>} 切り抜きに使う画像
 */
async function decodePhoto(file) {
  try {
    return await readBrowserImage(file);
  } catch (error) {
    // MIMEタイプが空になる環境でも、ファイル名やHEIFのヘッダーから判定する。
    const header = new TextDecoder().decode(await file.slice(4, 32).arrayBuffer());
    const isHeif = /\.hei[cf]$/i.test(file.name) || /^image\/hei[cf](?:-sequence)?$/i.test(file.type)
      || (header.startsWith('ftyp') && /(?:heic|heix|hevc|hevx|heim|heis|mif1|msf1)/.test(header));
    if (!isHeif) throw error;
    const { heicTo } = await import('./vendor/heic-to/heic-to-1.6.7.js');
    if (typeof createImageBitmap === 'function') {
      // フルサイズのPNGへの変換を挟まず、デコード済みの画素を切り抜きに使用する。
      return heicTo({ blob: file, type: 'bitmap' });
    }
    return readBrowserImage(await heicTo({ blob: file, type: 'image/png' }));
  }
}

/**
 * 共通のアイコン欄と切り抜きダイアログを初期化する。
 * @param {HTMLElement} field 事業主またはオーナーのアイコン欄
 */
function initializeIconField(field) {
  // プレビューと編集操作を、それぞれ担当するフォーム要素に結び付ける。
  const form = field.closest('form');
  const picker = field.querySelector('[data-icon-file]');
  const preview = field.querySelector('[data-icon-preview]');
  const remove = field.querySelector('[data-icon-remove]');
  const clear = field.querySelector('[data-icon-clear]');
  const status = field.querySelector('[data-icon-status]');
  const dialog = field.querySelector('[data-icon-dialog]');
  const dialogStatus = field.querySelector('[data-icon-dialog-status]');
  const canvas = field.querySelector('[data-icon-crop]');
  const zoom = field.querySelector('[data-icon-zoom]');
  const rotateButtons = field.querySelectorAll('[data-icon-rotate]');
  const confirm = field.querySelector('[data-icon-confirm]');
  // 表示とアップロードで同じキャンバスの画素を使い、切り抜きの位置を一致させる。
  const context = canvas.getContext('2d');
  const size = canvas.width;
  const center = size / 2;
  // 編集中の写真と、フォーム保存待ちの確定画像を別々に保持する。
  let crop = null;
  let pendingFile = null;
  let pendingUrl = null;
  let selection = 0;
  // 各指の位置をキャンバス座標で保持し、1本指の移動と2本指の拡大を扱う。
  const pointers = new Map();

  /** 確定済み・保存済み・削除予定のいずれかをフォームの画像欄に表示する。 */
  function refreshPreview() {
    const source = pendingUrl || (remove?.checked ? '' : preview.dataset.savedIcon);
    preview.hidden = !source;
    clear.hidden = !pendingFile;
    if (source) preview.src = source;
    else preview.removeAttribute('src');
  }

  /**
   * 再選択や取り消しで使わなくなった画像を解放する。
   * @param {Blob|null} file 保存待ちのPNG。nullなら選択を取り消す
   */
  function setPendingFile(file) {
    if (pendingUrl) URL.revokeObjectURL(pendingUrl);
    pendingFile = file;
    pendingUrl = file ? URL.createObjectURL(file) : null;
    refreshPreview();
  }

  /** 切り抜きを中断し、進行中のデコード結果も破棄できるようにする。 */
  function resetCrop() {
    selection++;
    pointers.clear();
    crop?.image.close?.();
    crop = null;
    context.clearRect(0, 0, size, size);
    zoom.value = '1';
    setCropEnabled(false);
  }

  /**
   * 画像の読み込みや書き出しが終わるまで編集ボタンを無効にする。
   * @param {boolean} enabled 編集操作を受け付けるか
   */
  function setCropEnabled(enabled) {
    zoom.disabled = !enabled;
    confirm.disabled = !enabled;
    rotateButtons.forEach(button => { button.disabled = !enabled; });
  }

  /** 回転後の画像で切り抜き領域が埋まるよう移動を制限し、プレビューを描画する。 */
  function drawCrop() {
    const scale = size / Math.min(crop.width, crop.height) * crop.zoom;
    const width = crop.width * scale;
    const height = crop.height * scale;
    const sideways = crop.turns % 2 !== 0;
    const edgeX = Math.max(0, ((sideways ? height : width) - size) / 2);
    const edgeY = Math.max(0, ((sideways ? width : height) - size) / 2);
    crop.x = Math.max(-edgeX, Math.min(edgeX, crop.x));
    crop.y = Math.max(-edgeY, Math.min(edgeY, crop.y));

    context.clearRect(0, 0, size, size);
    context.save();
    context.translate(center + crop.x, center + crop.y);
    context.rotate(crop.turns * Math.PI / 2);
    context.drawImage(crop.image, -width / 2, -height / 2, width, height);
    context.restore();
  }

  /**
   * プレビュー上の注目点を維持しながら、拡大率と移動量を変更する。
   * @param {number} nextZoom 操作後の拡大率
   * @param {{x: number, y: number}} from 操作前の注目点
   * @param {{x: number, y: number}} to 操作後の注目点
   */
  function adjustCrop(nextZoom, from, to) {
    const next = Math.max(Number(zoom.min), Math.min(Number(zoom.max), nextZoom));
    const ratio = next / crop.zoom;
    crop.x = to.x - center - (from.x - center - crop.x) * ratio;
    crop.y = to.y - center - (from.y - center - crop.y) * ratio;
    crop.zoom = next;
    zoom.value = String(next);
    drawCrop();
  }

  /**
   * ポインターの画面座標をプレビュー内の座標に変換する。
   * @param {PointerEvent} event ドラッグまたはピンチの入力イベント
   * @returns {{x: number, y: number}} キャンバス上の位置
   */
  function cropPoint(event) {
    const bounds = canvas.getBoundingClientRect();
    return { x: (event.clientX - bounds.left) * size / bounds.width,
      y: (event.clientY - bounds.top) * size / bounds.height };
  }

  /** ドラッグの位置、またはピンチの中心と指の間隔を求める。 */
  function gesture() {
    const [first, second] = pointers.values();
    if (!second) return { ...first, distance: 0 };
    return { x: (first.x + second.x) / 2, y: (first.y + second.y) / 2,
      distance: Math.hypot(first.x - second.x, first.y - second.y) };
  }

  /**
   * 選択した元画像をデコードし、準備できた場合だけ操作を受け付ける。
   * @param {File} file 画像選択ダイアログで受け取った元ファイル
   */
  async function selectPhoto(file) {
    if (file.size === 0 || file.size > MAX_SOURCE_BYTES) {
      status.textContent = '30MB以下の画像を選択してください。';
      return;
    }

    resetCrop();
    const selected = selection;
    dialogStatus.textContent = '画像を読み込んでいます…';
    dialog.showModal();
    try {
      const image = await decodePhoto(file);
      if (selection !== selected || !dialog.open) { image.close?.(); return; }
      const width = image.naturalWidth || image.width;
      const height = image.naturalHeight || image.height;
      crop = { image, width, height, x: 0, y: 0, zoom: 1, turns: 0 };
      drawCrop();
      setCropEnabled(true);
      dialogStatus.textContent = '';
    } catch (_) {
      if (selection === selected && dialog.open) {
        dialogStatus.textContent = '画像を開けませんでした。JPEG・PNGなどの画像を選び直してください。';
      }
    }
  }

  // 元ファイルを選び直した場合も、確定するまでは直前のアイコンを保持する。
  picker.addEventListener('change', () => {
    const file = picker.files[0];
    picker.value = '';
    if (file) selectPhoto(file);
  });
  field.querySelector('[data-icon-cancel]').addEventListener('click', () => dialog.close());
  dialog.addEventListener('close', () => { if (!dialog.open) resetCrop(); });
  preview.addEventListener('error', () => { preview.hidden = true; });

  // ピンチ時は指の間隔に応じて拡大し、1本指では移動だけを行う。
  canvas.addEventListener('pointerdown', event => {
    if (!crop || confirm.disabled || pointers.size === 2 || event.button !== 0) return;
    canvas.setPointerCapture(event.pointerId);
    pointers.set(event.pointerId, cropPoint(event));
  });
  canvas.addEventListener('pointermove', event => {
    if (!pointers.has(event.pointerId) || confirm.disabled) return;
    const from = gesture();
    pointers.set(event.pointerId, cropPoint(event));
    const to = gesture();
    const factor = from.distance > 0 && to.distance > 0 ? to.distance / from.distance : 1;
    adjustCrop(crop.zoom * factor, from, to);
  });
  for (const endEvent of ['pointerup', 'pointercancel', 'lostpointercapture']) {
    canvas.addEventListener(endEvent, event => pointers.delete(event.pointerId));
  }
  canvas.addEventListener('keydown', event => {
    if (!crop || confirm.disabled) return;
    const movement = { ArrowUp: [0, -12], ArrowDown: [0, 12], ArrowLeft: [-12, 0], ArrowRight: [12, 0] }[event.key];
    if (!Array.isArray(movement)) return;
    event.preventDefault();
    crop.x += movement[0];
    crop.y += movement[1];
    drawCrop();
  });
  zoom.addEventListener('input', () => {
    const anchor = { x: center, y: center };
    adjustCrop(Number(zoom.value), anchor, anchor);
  });
  rotateButtons.forEach(button => button.addEventListener('click', () => {
    const direction = Number(button.dataset.iconRotate);
    const previousX = crop.x;
    crop.turns = (crop.turns + direction + 4) % 4;
    crop.x = -direction * crop.y;
    crop.y = direction * previousX;
    drawCrop();
  }));

  // 画面の調整結果だけをPNGに書き出し、既存フォームの保存時に送信する。
  confirm.addEventListener('click', () => {
    const confirmed = selection;
    setCropEnabled(false);
    canvas.toBlob(file => {
      if (confirmed !== selection || !dialog.open) return;
      if (!file) {
        dialogStatus.textContent = '切り抜いた画像を作成できませんでした。もう一度お試しください。';
        setCropEnabled(true);
        return;
      }
      if (remove) remove.checked = false;
      setPendingFile(file);
      status.textContent = 'この画像はフォームを保存すると登録されます。';
      dialog.close();
    }, 'image/png');
  });
  clear.addEventListener('click', () => {
    setPendingFile(null);
    status.textContent = '';
  });
  remove?.addEventListener('change', () => {
    setPendingFile(null);
    status.textContent = remove.checked ? 'フォームを保存するとアイコンを削除します。' : '';
  });
  form.addEventListener('formdata', event => {
    const data = event.formData;
    // 各チェックボックスを個別のパートにすると、多数選択したときにTomcatの上限に達する。
    for (const name of ['preferredIndustry', 'qualification']) {
      const values = data.getAll(name);
      if (values.length > 1) data.set(name, values.join(','));
    }
    if (pendingFile) data.set('profileIcon', pendingFile, 'profile-icon.png');
  });
}
