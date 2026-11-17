/**
 * マチリク (Machiriku) - メインアプリケーション & ルーター
 */
window.MachirikuApp = (function() {
  const store = window.MachirikuStore;
  const screens = window.MachirikuScreens;
  const mapEngine = window.MachirikuMap;

  let currentScreenId = "s01";
  let currentParams = {};
  let renderedScreenId = null;
  let renderedNavRole = null;
  const screenScrollPositions = {};

  function init() {
    // スクロール位置記録 (S01の一覧等)
    if (typeof window !== 'undefined' && window.addEventListener) {
      window.addEventListener("scroll", () => {
        if (currentScreenId === 's01') {
          screenScrollPositions['s01'] = window.scrollY || window.pageYOffset || 0;
        }
      }, { passive: true });
    }

    // ハッシュ変更監視
    window.addEventListener("hashchange", handleHashChange);

    // 画面幅変更時のヘッダー名前ティッカー再計算
    window.addEventListener("resize", () => {
      clearTimeout(window._headerTickerTimer);
      window._headerTickerTimer = setTimeout(updateHeaderTicker, 150);
    });

    // ストア変更監視
    store.subscribe((state, change = {}) => {
      updateHeader();
      if (change.type === 'follow' && currentScreenId === 's02') {
        document.querySelectorAll('[data-follow-id]').forEach(button => {
          const followed = state.currentUser.followedProps?.includes(button.getAttribute('data-follow-id'));
          button.setAttribute('aria-pressed', String(Boolean(followed)));
          button.textContent = followed ? 'フォロー中・解除する' : '開店のようすをフォローする';
        });
        return;
      }
      renderCurrentScreen(change.type !== 'role' && change.type !== 'reset');
      if (change.resetForm) {
        const keyword = document.getElementById('search-keyword');
        const area = document.getElementById('search-area');
        const rent = document.getElementById('search-rent');
        if (keyword) keyword.value = state.filters.keyword;
        if (area) area.value = state.filters.area;
        if (rent) rent.value = String(state.filters.rentMax);
      }
    });

    // 初期ルート読み込み
    handleHashChange();
    updateHeader();
    initBackToTopButton();
  }

  function initBackToTopButton() {
    if (typeof document === 'undefined') return;
    const btn = document.getElementById('mobile-back-to-top');
    if (!btn) return;

    btn.addEventListener('click', () => {
      const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
      window.scrollTo({ top: 0, behavior: reduceMotion ? 'auto' : 'smooth' });
    });

    const sentinel = document.getElementById('page-top-sentinel');
    if (sentinel && typeof IntersectionObserver !== 'undefined') {
      const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          btn.classList.toggle('is-visible', !entry.isIntersecting);
        });
      }, { threshold: 0 });
      observer.observe(sentinel);
    } else if (typeof window !== 'undefined') {
      const onScroll = () => {
        const y = window.scrollY || window.pageYOffset || 0;
        btn.classList.toggle('is-visible', y > 220);
      };
      window.addEventListener('scroll', onScroll, { passive: true });
      onScroll();
    }
  }

  function parseHash() {
    const hash = window.location.hash.slice(1) || "s01";
    const [screenId, queryString] = hash.split("?");
    const params = {};
    if (queryString) {
      queryString.split("&").forEach(part => {
        const [k, v] = part.split("=");
        if (k) params[decodeURIComponent(k)] = decodeURIComponent(v || "");
      });
    }
    return {
      screenId: (screenId || "s01").toLowerCase(),
      params: params
    };
  }

  function getContextPath() {
    if (typeof window !== 'undefined' && window.__MACHIRIKU_CONTEXT_PATH__ !== undefined) {
      return window.__MACHIRIKU_CONTEXT_PATH__;
    }
    const fromBody = document.body?.getAttribute?.('data-context-path');
    if (fromBody != null) return fromBody;
    if (typeof window !== 'undefined' && window.location && window.location.pathname.startsWith('/machiriku')) {
      return '/machiriku';
    }
    return '';
  }

  function isJspApp() {
    return typeof window !== 'undefined' && window.__MACHIRIKU_CONTEXT_PATH__ !== undefined;
  }

  function getFixedScreen() {
    return (typeof window !== 'undefined' && window.__MACHIRIKU_FIXED_SCREEN__)
      ? String(window.__MACHIRIKU_FIXED_SCREEN__).toLowerCase()
      : '';
  }

  function getQueryParams() {
    const params = {};
    if (typeof window === 'undefined' || !window.location || !window.location.search) return params;
    try {
      const sp = new URLSearchParams(window.location.search);
      sp.forEach((v, k) => {
        params[k] = v;
      });
    } catch (_) {}
    return params;
  }

  const JSP_ROUTE_MAP = {
    s02: '/property/detail.jsp',
    r01: '/property/vote.jsp',
    s03: '/property/vote.jsp',
    b01: '/property/apply.jsp',
    s08: '/property/apply.jsp',
    o01: '/property/manage.jsp',
    s11: '/property/manage.jsp',
    o02: '/property/analytics.jsp',
    s07: '/property/analytics.jsp',
    o03: '/property/apply-list.jsp',
    s09: '/property/apply-list.jsp',
    o04: '/property/add.jsp',
    o05: '/property/add-confirm.jsp',
    o06: '/property/edit.jsp',
    o07: '/property/edit-confirm.jsp',
    o08: '/property/delete-confirm.jsp',
    o09: '/property/delete-complete.jsp'
  };

  function resolveJspRoutePath(screenId, params = {}) {
    const id = String(screenId || '').toLowerCase();
    if (id === 's12') {
      return (params && params.id) ? '/property/edit.jsp' : '/property/add.jsp';
    }
    return JSP_ROUTE_MAP[id] || null;
  }

  function buildScreenUrl(screenId = 's01', params = {}) {
    const id = String(screenId || 's01').toLowerCase();
    const base = String(getContextPath() || '').replace(/\/+$/, '');
    if (isJspApp()) {
      if (id === 's16' || id === 's17') {
        const action = id === 's16' ? 'Login' : 'Register';
        return `${base}/jp/machiriku/servlet/auth/${action}.action`;
      }
      const jspPath = resolveJspRoutePath(id, params);
      if (jspPath) {
        const keys = Object.keys(params || {}).filter(k => params[k] !== undefined && params[k] !== '');
        const qs = keys.length ? `?${keys.map(k => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`).join('&')}` : '';
        return `${base}${jspPath}${qs}`;
      }
    }
    return buildIndexJspUrl(id, params);
  }

  function buildDetailJspUrl(params = {}) {
    return buildScreenUrl('s02', params);
  }

  function buildIndexJspUrl(screenId = 's01', params = {}) {
    const base = String(getContextPath() || '').replace(/\/+$/, '');
    let hash = `#${String(screenId || 's01').toLowerCase()}`;
    const keys = Object.keys(params || {}).filter(k => params[k] !== undefined && params[k] !== '');
    if (keys.length > 0) {
      hash += `?${keys.map(k => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`).join('&')}`;
    }
    return `${base}/index.jsp${hash}`;
  }

  // 画面ごとのアクセス可能ロール定義（未定義の画面は全ユーザー公開）
  const SCREEN_PERMISSIONS = {
    // 出店事業主（および管理者）向け機能
    s08: ['business', 'admin'],
    s15: ['business', 'admin'],
    s21: ['business', 'admin'],
    s22: ['business', 'admin'],
    s23: ['business', 'admin'],
    s24: ['business', 'admin'],
    s25: ['business', 'admin'],
    s27: ['business', 'admin'],
    s28: ['business', 'admin'],
    s29: ['business', 'admin'],
    // 物件オーナー（および管理者）向け機能
    s11: ['owner', 'admin'],
    s12: ['owner', 'admin'],
    s13: ['owner', 'admin'],
    s33: ['owner', 'admin'],
    // オーナー・事業主双方のやり取り画面（契約・申込）
    s09: ['owner', 'business', 'admin'],
    s10: ['owner', 'business', 'admin'],
    s14: ['owner', 'business', 'admin'],
    // 運営管理者専用
    s20: ['admin'],
    // 会員限定設定画面（全会員ロール対象・ログイン必須）
    s18: ['resident', 'business', 'owner', 'admin'],
    s19: ['resident', 'business', 'owner', 'admin']
  };

  const ROLE_NAMES = {
    resident: '地域住民',
    business: '出店事業主',
    owner: '物件オーナー',
    admin: '運営管理者'
  };

  function isUserAuthenticated() {
    const state = store.getState();
    const user = state.currentUser;
    if (user && typeof user.isLoggedIn === 'boolean') {
      return user.isLoggedIn;
    }
    if (typeof document !== 'undefined') {
      // 未ログイン状態のindex.jspにはログインリンク（.header-nav-right a[href*="Login.action"]）が存在する
      const loginLink = document.querySelector('.header-nav-right a[href*="Login.action"]');
      if (loginLink) return false;
      // ログインリンクが存在しない場合（Tomcatログイン済み、またはプロトタイプ・デモ環境）
      const userNameEl = document.getElementById('header-user-name');
      if (userNameEl && userNameEl.textContent && userNameEl.textContent.trim()) {
        const t = userNameEl.textContent.trim();
        if (t !== 'ログイン') return true;
      }
      if (document.getElementById('preview-role')) return true;
    }
    // 初期設定中セッションがあれば認証済みとみなす
    if (isOnboardingActive()) return true;
    return false;
  }

  function isDemoPreviewMode() {
    if (typeof document === 'undefined') return false;
    const wrap = document.getElementById('header-role-picker-wrap');
    const select = document.getElementById('preview-role');
    return Boolean(wrap && select && wrap.style.display !== 'none' && !isUserAuthenticated());
  }

  function checkRouteAuthorization(screenId) {
    const requiredRoles = SCREEN_PERMISSIONS[screenId];
    if (!requiredRoles) {
      // 認可制限のない公開画面 (s01, s02, s03, s04, s05, s06, s07, s16, s17, s26, s30, s31, s32など)
      return { authorized: true };
    }

    // 初期設定中オンボーディング時の S18 は特例許可
    if (screenId === 's18' && isOnboardingActive()) {
      return { authorized: true };
    }

    // デモ表示（未ログイン時のロール切替プルダウン有効時）は、会員専用プロフィール(S18)以外をロール別プレビュー許可
    const allowDemoPreview = screenId !== 's18' && isDemoPreviewMode();

    // 1. 認証チェック（ログイン確認）
    if (!isUserAuthenticated() && !allowDemoPreview) {
      return {
        authorized: false,
        reason: 'unauthenticated',
        message: 'この画面を表示するにはログインが必要です。'
      };
    }

    // 2. 認可チェック（ロール確認）
    const state = store.getState();
    const currentRole = (state.currentRole || state.currentUser?.role || 'resident').toLowerCase();
    if (!requiredRoles.includes(currentRole)) {
      const allowedNames = requiredRoles.map(r => ROLE_NAMES[r] || r).join('・');
      const curName = ROLE_NAMES[currentRole] || currentRole;
      return {
        authorized: false,
        reason: 'unauthorized',
        currentRole: curName,
        allowedRoles: allowedNames,
        message: `この画面は【${allowedNames}】専用です。現在のロール（${curName}）ではアクセスできません。`
      };
    }

    return { authorized: true };
  }

  function isOnboardingActive() {
    if (currentScreenId === 's18') {
      if (currentParams && (currentParams.onboarding === '1' || currentParams.onboarding === 'true' || currentParams.onboarding === true)) {
        return true;
      }
      try {
        if (typeof window !== 'undefined' && window.sessionStorage) {
          const raw = window.sessionStorage.getItem('machiriku_onboarding');
          if (raw) return true;
        }
      } catch (_) {}
    }
    return false;
  }

  function handleHashChange() {
    const previousScreenId = currentScreenId;
    if (previousScreenId === 's01' && typeof window !== 'undefined') {
      screenScrollPositions['s01'] = window.scrollY || window.pageYOffset || 0;
    }

    const fixedScreen = getFixedScreen();
    const rawHash = (typeof window !== 'undefined' && window.location && window.location.hash) ? window.location.hash.slice(1) : '';
    const parsed = parseHash();
    const contextPath = getContextPath();

    if (fixedScreen) {
      const fixedJspPath = resolveJspRoutePath(fixedScreen, getQueryParams());
      if (rawHash) {
        const targetJspPath = resolveJspRoutePath(parsed.screenId, parsed.params);
        if (parsed.screenId !== fixedScreen && (!targetJspPath || targetJspPath !== fixedJspPath)) {
          window.location.replace(buildScreenUrl(parsed.screenId, parsed.params));
          return;
        }
      }
      currentScreenId = fixedScreen;
      const fixedParams = (typeof window !== 'undefined' && window.__MACHIRIKU_FIXED_PARAMS__) ? window.__MACHIRIKU_FIXED_PARAMS__ : {};
      currentParams = Object.assign({}, fixedParams, getQueryParams(), rawHash ? parsed.params : {});
    } else {
      currentScreenId = parsed.screenId;
      currentParams = parsed.params;

      if (isJspApp()) {
        const jspTargetPath = resolveJspRoutePath(currentScreenId, currentParams);
        if (jspTargetPath) {
          window.location.replace(buildScreenUrl(currentScreenId, currentParams));
          return;
        }
      }

      // トップ画面内の認証リンクも、サーバー側のログイン状態と登録状態で振り分ける。
      if (contextPath && (currentScreenId === 's16' || currentScreenId === 's17')) {
        const action = currentScreenId === 's16' ? 'Login' : 'Register';
        window.location.replace(`${contextPath}/jp/machiriku/servlet/auth/${action}.action`);
        return;
      }
    }

    // ルーターガード: 未認証ユーザーが認証必須画面にアクセスした場合は安全にリダイレクト
    const authCheck = checkRouteAuthorization(currentScreenId);
    if (!authCheck.authorized) {
      if (authCheck.reason === 'unauthenticated') {
        if (typeof showToast === 'function') {
          showToast('ログインが必要です。ログイン画面へ移動します');
        }
        if (contextPath) {
          window.location.replace(`${contextPath}/jp/machiriku/servlet/auth/Login.action`);
          return;
        }
      } else if (authCheck.reason === 'unauthorized') {
        if (typeof showToast === 'function') {
          showToast(authCheck.message);
        }
      }
    }

    // 初期設定中ガード: オンボーディングセッション未完了時に他画面への離脱を防止
    if (currentScreenId !== 's18') {
      try {
        if (typeof window !== 'undefined' && window.sessionStorage) {
          const raw = window.sessionStorage.getItem('machiriku_onboarding');
          if (raw) {
            const data = JSON.parse(raw);
            if (data && data.name) {
              window.location.hash = `#s18?onboarding=1&displayName=${encodeURIComponent(data.name)}&role=${encodeURIComponent(data.role || 'resident')}`;
              if (typeof showToast === 'function') {
                showToast('初期設定を完了するまで他の画面へ移動できません');
              }
              return;
            }
          }
        }
      } catch (_) {}
    }

    updateHeader();
    renderCurrentScreen();

    // S01へ戻った場合は直前のスクロール位置へ復帰
    if (currentScreenId === 's01' && screenScrollPositions['s01'] != null && previousScreenId !== 's01') {
      const savedY = screenScrollPositions['s01'];
      if (typeof window !== 'undefined' && typeof window.requestAnimationFrame === 'function') {
        window.requestAnimationFrame(() => {
          window.scrollTo({ top: savedY, behavior: "auto" });
        });
      } else if (typeof window !== 'undefined' && window.scrollTo) {
        window.scrollTo({ top: savedY, behavior: "auto" });
      }
    } else {
      if (typeof window !== 'undefined' && window.scrollTo) {
        window.scrollTo({ top: 0, behavior: "auto" });
      }
    }
    document.getElementById('main-app-container')?.focus?.({ preventScroll: true });
  }

  function navigate(screenId, params = {}) {
    const target = String(screenId || 's01').toLowerCase();
    if (isJspApp()) {
      const targetJspPath = resolveJspRoutePath(target, params);
      if (targetJspPath || target === 's16' || target === 's17' || getFixedScreen()) {
        window.location.href = buildScreenUrl(target, params);
        return;
      }
    }
    let hash = `#${target}`;
    const keys = Object.keys(params);
    if (keys.length > 0) {
      const qs = keys.map(k => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`).join("&");
      hash += `?${qs}`;
    }
    window.location.hash = hash;
  }

  // Reconcile existing nodes so input drafts, focus, open details and the map survive.
  // No framework is needed for this small, static screen prototype.
  function nodeKey(node) {
    if (node.nodeType !== 1) return null;
    return node.id || node.getAttribute('data-id') || null;
  }

  function patchNode(current, next) {
    if (current.nodeType !== next.nodeType || current.nodeName !== next.nodeName) {
      current.replaceWith(next.cloneNode(true));
      return;
    }
    if (current.nodeType !== 1) {
      if (current.nodeValue !== next.nodeValue) current.nodeValue = next.nodeValue;
      return;
    }
    if (current.id === 'portal-map-pane') return;
    // The active control already contains the user's pending input.
    if (current === document.activeElement && /^(INPUT|SELECT|TEXTAREA)$/.test(current.nodeName)) return;
    for (const attribute of Array.from(current.attributes)) {
      if (attribute.name === 'open' && current.nodeName === 'DETAILS') continue;
      if (!next.hasAttribute(attribute.name)) current.removeAttribute(attribute.name);
    }
    for (const attribute of Array.from(next.attributes)) {
      if (current.getAttribute(attribute.name) !== attribute.value) current.setAttribute(attribute.name, attribute.value);
    }
    patchChildren(current, next);
  }

  function patchChildren(current, next) {
    const keyed = new Map(Array.from(current.childNodes).map(node => [nodeKey(node), node]).filter(([key]) => key));
    Array.from(next.childNodes).forEach((nextChild, index) => {
      let existing = current.childNodes[index];
      const key = nodeKey(nextChild);
      const candidate = key && keyed.get(key);
      if (candidate && candidate !== existing) {
        current.insertBefore(candidate, existing || null);
        existing = candidate;
      }
      if (!existing) current.appendChild(nextChild.cloneNode(true));
      else if (key && nodeKey(existing) !== key) current.insertBefore(nextChild.cloneNode(true), existing);
      else patchNode(existing, nextChild);
    });
    while (current.childNodes.length > next.childNodes.length) current.lastChild.remove();
  }

  function renderCurrentScreen(preserve = false) {
    const mainEl = document.getElementById("main-app-container");
    if (!mainEl) return;

    // 画面認可 / ルーターガードの検証
    const fixedScreen = getFixedScreen();
    if (fixedScreen === 's02' && typeof document !== 'undefined' && document.getElementById('s02-detail-section') && typeof screens.hydrateS02 === 'function') {
      screens.hydrateS02(currentParams);
      renderedScreenId = 's02';
      if (typeof screens.initS02MiniMap === 'function') {
        screens.initS02MiniMap();
      }
      if (typeof screens.initGallerySlider === 'function') {
        screens.initGallerySlider();
      }
      updateSubnavActive();
      updateDocumentTitle();
      return;
    }

    if (fixedScreen && window.MachirikuPropertyPages && typeof window.MachirikuPropertyPages.hydrate === 'function') {
      window.MachirikuPropertyPages.hydrate(fixedScreen, currentParams);
      renderedScreenId = fixedScreen;
      updateSubnavActive();
      updateDocumentTitle();
      return;
    }

    const authCheck = checkRouteAuthorization(currentScreenId);

    let html;
    if (!authCheck.authorized) {
      if (authCheck.reason === 'unauthenticated') {
        html = screens.renderEmptyState(
          currentScreenId,
          'ログインが必要です',
          'この画面を表示するにはログインが必要です。ログインまたは新規登録を行ってください。',
          '#s01',
          'トップページへ戻る'
        );
      } else {
        html = screens.renderEmptyState(
          currentScreenId,
          'アクセス権限がありません',
          authCheck.message,
          '#s01',
          'トップページへ戻る'
        );
      }
    } else {
      const renderFnName = `render${currentScreenId.toUpperCase()}`;
      const renderFn = screens[renderFnName];

      try {
        html = typeof renderFn === 'function' ? renderFn(currentParams) : screens.renderEmptyState('not-found', '画面が見つかりません', 'メニューから目的の画面を選んでください。');
      } catch (error) {
        console.error('Screen render failed:', currentScreenId, error);
        html = screens.renderEmptyState(currentScreenId, '画面を表示できません', 'もう一度開くか、物件一覧に戻ってください。');
      }
    }
    let patched = false;
    if (preserve && renderedScreenId === currentScreenId) {
      const template = document.createElement('template');
      // The guard also supports the project's lightweight non-DOM test environment.
      if (template.content && mainEl.childNodes) {
        template.innerHTML = html;
        patchChildren(mainEl, template.content);
        patched = true;
      }
    }
    if (!patched) mainEl.innerHTML = html;
    renderedScreenId = currentScreenId;

    // 画面固有の後処理
    if (currentScreenId === "s01") {
      if (patched) mapEngine.updateProperties(store.getFilteredProperties());
      else initS01Map();
    } else if (currentScreenId === "s02") {
      if (typeof screens.initS02MiniMap === 'function') {
        screens.initS02MiniMap();
      }
      if (typeof screens.initGallerySlider === 'function') {
        screens.initGallerySlider();
      }
    } else if (currentScreenId === "s18") {
      if (typeof screens.initS18MapPicker === 'function') {
        screens.initS18MapPicker();
      }
      if (typeof screens.initS18BizAreaMap === 'function') {
        screens.initS18BizAreaMap();
      }
    } else if (currentScreenId === "s25") {
      initS25Qr();
    }

    updateSubnavActive();
    updateDocumentTitle();
  }

  const FIXED_PAGE_TITLES = {
    r01: '投票 | マチリク',
    b01: '出店応募 | マチリク',
    o01: '登録物件管理 | マチリク',
    o02: '物件需要分析 | マチリク',
    o03: '出店希望確認 | マチリク',
    o04: '物件登録 | マチリク',
    o05: '物件登録確認 | マチリク',
    o06: '物件編集 | マチリク',
    o07: '物件編集確認 | マチリク',
    o08: '物件削除確認 | マチリク',
    o09: '物件削除完了 | マチリク'
  };

  function updateDocumentTitle() {
    if (typeof document === 'undefined') return;
    if (currentScreenId === 's01') {
      document.title = 'マチリク | 宇都宮の空き店舗と、欲しいお店';
      return;
    }
    if (currentScreenId === 's02' || currentScreenId === 'r01' || currentScreenId === 'b01' || currentScreenId === 'o06' || currentScreenId === 'o07' || currentScreenId === 'o08') {
      const propId = currentParams?.id || store.getProperties()[0]?.id;
      const prop = propId ? store.getPropertyById(propId) : null;
      if (prop && prop.title) {
        if (currentScreenId === 's02') {
          document.title = `${prop.title} | マチリク`;
          return;
        }
        const suffix = FIXED_PAGE_TITLES[currentScreenId];
        if (suffix) {
          document.title = `${prop.title} - ${suffix}`;
          return;
        }
      }
    }
    if (FIXED_PAGE_TITLES[currentScreenId]) {
      document.title = FIXED_PAGE_TITLES[currentScreenId];
      return;
    }
    const spec = (window.MACHIRIKU_DATA?.SCREENS_SPEC || []).find(s => s.id === currentScreenId);
    document.title = spec ? `${spec.name} | マチリク` : 'マチリク | 宇都宮の空き店舗と、欲しいお店';
  }

  function initS01Map() {
    const mapPane = document.getElementById("portal-map-pane");
    if (!mapPane) return;

    const props = store.getFilteredProperties();
    mapEngine.init("#portal-map-pane", props, (clickedProp) => {
      // ピンがクリックされた時: デスクトップでは該当カードへスムーズスクロール
      // スマホ（768px以下またはタッチ環境）では強制スクロールを避け、地図上のプレビュー/InfoWindowを優先
      const isMobile = (typeof window !== 'undefined' && (window.innerWidth <= 768 || ('ontouchstart' in window && window.innerWidth <= 1024)));
      if (!isMobile) {
        const card = document.querySelector(`.property-card[data-id="${clickedProp.id}"]`);
        if (card) {
          card.scrollIntoView({ behavior: window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ? "auto" : "smooth", block: "center" });
          card.classList.add("is-highlighted");
          setTimeout(() => card.classList.remove("is-highlighted"), 1500);
        }
      }
    });
  }

  function initS25Qr() {
    const surveyId = currentParams.id || "srv-001";
    // 宇都宮マチリク回答用URL（ローカル実証およびモック用）
    let targetUrl;
    if (window.location && window.location.origin && window.location.origin !== "null") {
      targetUrl = `${window.location.origin}${window.location.pathname}#s26?id=${surveyId}`;
    } else {
      targetUrl = `https://machiriku-utsunomiya.jp/#s26?id=${surveyId}`;
    }
    screens.renderQrCode("survey-qr-container", targetUrl);
  }

  // 利用者ロール別 サブナビゲーションメニュー定義
  const ROLE_NAV_MENUS = {
    resident: [
      { hash: '#s01', label: '物件を探す' },
      { hash: '#s04', label: 'フォロー' },
      { hash: '#s05', label: '開店のようす' },
      { hash: '#s30', label: 'クーポン' }
    ],
    business: [
      { hash: "#s01", label: "物件検索" },
      { hash: "#s07", label: "需要詳細分析" },
      { hash: "#s08", label: "出店応募" },
      { hash: "#s09", label: "応募管理" },
      { hash: "#s15", label: "出店計画公開" },
      { hash: "#s22", label: "来店アンケート" },
      { hash: "#s28", label: "クーポン管理" },
      { hash: "#s32", label: "実績評価" },
      { hash: "#s21", label: "開店支援" }
    ],
    owner: [
      { hash: "#s11", label: "所有物件管理" },
      { hash: "#s12", label: "新規物件登録" },
      { hash: "#s07", label: "住民需要分析" },
      { hash: "#s09", label: "出店申込一覧" },
      { hash: "#s13", label: "事業主オファー" },
      { hash: "#s14", label: "契約引渡し" },
      { hash: "#s33", label: "PR広告" },
      { hash: "#s01", label: "全体マップ" }
    ],
    admin: [
      { hash: "#s20", label: "画面一覧" },
      { hash: "#s01", label: "物件検索・地図" },
      { hash: "#s07", label: "エリア需要分析" },
      { hash: "#s09", label: "出店応募一覧" },
      { hash: "#s11", label: "登録物件管理" },
      { hash: "#s22", label: "アンケート一覧" }
    ]
  };

  function updateHeader() {
    const state = store.getState();
    const user = state.currentUser || {};
    const isOnboarding = isOnboardingActive();

    // 1. サブナビゲーションバーの表示制御（初期設定中は非表示にして入力に集中）
    const subnav = document.querySelector('.header-subnav');
    if (subnav) {
      subnav.style.display = isOnboarding ? 'none' : '';
    }

    // 2. 利用者ロール表示（案A: ログイン後または初期設定中は固定バッジ表示、未ログイン時のみプルダウン）
    const rolePickerWrap = document.getElementById('header-role-picker-wrap');
    const roleBadge = document.getElementById('header-role-badge');
    const roleBadgeText = document.getElementById('header-role-badge-text');

    const roleLabels = {
      resident: '地域住民',
      business: '出店事業主',
      owner: '物件オーナー',
      admin: '運営管理者'
    };
    const activeRole = state.currentRole || 'resident';
    const roleText = roleLabels[activeRole] || '地域住民';

    if (isOnboarding || isUserAuthenticated()) {
      if (rolePickerWrap) rolePickerWrap.style.display = 'none';
      if (roleBadge) {
        roleBadge.style.display = 'inline-flex';
        if (roleBadgeText) roleBadgeText.textContent = roleText;
      }
    } else {
      if (rolePickerWrap) rolePickerWrap.style.display = '';
      if (roleBadge) roleBadge.style.display = 'none';
    }

    const roleSelect = document.getElementById('preview-role');
    if (roleSelect) roleSelect.value = activeRole;

    // 3. 初期設定中は通知リンクを非表示にし、ロゴクリックも無効化
    const notifLink = document.querySelector('.notification-link');
    if (notifLink) {
      notifLink.style.display = isOnboarding ? 'none' : 'inline-flex';
    }

    const brandArea = document.querySelector('.brand-area');
    if (brandArea) {
      if (isOnboarding) {
        brandArea.style.pointerEvents = 'none';
        brandArea.style.cursor = 'default';
        brandArea.setAttribute('tabindex', '-1');
      } else {
        brandArea.style.pointerEvents = '';
        brandArea.style.cursor = 'pointer';
        brandArea.removeAttribute('tabindex');
      }
    }

    const storageStatus = document.getElementById('storage-status');
    if (storageStatus) storageStatus.textContent = store.isPersistenceAvailable() ? '操作した内容はこのブラウザーに保存されます。' : 'この環境では、再読み込みすると初期状態に戻ります。';

    // ロールボタンのアクティブ表示
    const roleBtns = document.querySelectorAll(".role-btn");
    roleBtns.forEach(btn => {
      const r = btn.getAttribute("data-role");
      if (r === state.currentRole) {
        btn.classList.add("is-active");
      } else {
        btn.classList.remove("is-active");
      }
    });

    // ユーザー情報表示
    const userNameEl = document.getElementById("header-user-name");
    if (userNameEl) {
      let displayName = null;
      if (typeof window !== 'undefined' && window.sessionStorage) {
        try {
          const savedName = window.sessionStorage.getItem('machiriku_user_display_name');
          if (savedName) displayName = savedName;
          if (!displayName) {
            const raw = window.sessionStorage.getItem('machiriku_onboarding');
            if (raw) {
              const data = JSON.parse(raw);
              if (data && data.name) displayName = data.name;
            }
          }
        } catch (_) {}
      }
      if (!displayName && currentParams && currentParams.displayName) {
        displayName = currentParams.displayName;
      }
      if (!displayName && user && user.name && user.name !== '田中 一郎') {
        displayName = user.name;
      }
      // JSP/サーバー側で既にDOMに出力されている名前がある場合はそれを最優先で尊重
      if (!displayName && userNameEl.textContent && userNameEl.textContent.trim()) {
        const textInDom = userNameEl.textContent.trim();
        if (textInDom !== '田中 一郎') {
          displayName = textInDom;
        }
      }
      if (!displayName) {
        displayName = (user && user.name && user.name !== '田中 一郎') ? user.name : (isOnboarding ? 'LINEユーザー' : (user.name || 'LINEユーザー'));
      }
      userNameEl.textContent = displayName;
      setTimeout(updateHeaderTicker, 50);
    }

    // 未読通知バッジ
    const unreadCount = (state.notifications || []).filter(n => !n.isRead).length;
    const badgeEl = document.getElementById("header-notif-badge");
    if (badgeEl) {
      if (unreadCount > 0 && !isOnboarding) {
        badgeEl.hidden = false;
        badgeEl.textContent = unreadCount;
        badgeEl.style.display = "inline-block";
      } else {
        badgeEl.hidden = true;
        badgeEl.style.display = "none";
      }
    }

    // ロール別サブナビゲーションリンクの動的更新（初期設定中以外）
    if (!isOnboarding) {
      renderSubnavLinks(state.currentRole);
    }
  }

  function isNavTabActive(tabHashScreenId, activeScreen) {
    if (!tabHashScreenId) return false;
    if (tabHashScreenId === activeScreen) return true;
    const aliases = {
      s02: 's02',
      r01: 's03',
      b01: 's08',
      o01: 's11',
      o02: 's07',
      o03: 's09',
      o04: 's12'
    };
    return aliases[activeScreen] === tabHashScreenId;
  }

  function renderSubnavLinks(role) {
    const container = document.querySelector(".subnav-links");
    if (!container) return;
    if (renderedNavRole === role) return;
    renderedNavRole = role;
    const fixedScreen = getFixedScreen();
    const items = ROLE_NAV_MENUS[role] || ROLE_NAV_MENUS.resident;
    container.innerHTML = items.map(item => {
      const sId = item.hash.replace("#", "").toLowerCase();
      const href = isJspApp() ? buildScreenUrl(sId) : item.hash;
      const isActive = isNavTabActive(sId, fixedScreen || currentScreenId) ? "is-active" : "";
      return `<a href="${href}" data-nav-screen="${sId}" class="subnav-link ${isActive}" ${isActive ? 'aria-current="page"' : ''}>${item.label}</a>`;
    }).join("");
  }

  function updateSubnavActive() {
    const activeScreen = getFixedScreen() || currentScreenId;
    const links = document.querySelectorAll(".subnav-link");
    links.forEach(l => {
      const sId = (l.getAttribute("data-nav-screen") || (l.getAttribute("href") || "").replace("#", "")).toLowerCase();
      if (isNavTabActive(sId, activeScreen)) {
        l.classList.add("is-active");
        l.setAttribute('aria-current', 'page');
      } else {
        l.classList.remove("is-active");
        l.removeAttribute('aria-current');
      }
    });
  }

  // ヘッダー名前の自動横スクロール（文字数が表示幅を超えた場合のティッカープレビュー）
  function updateHeaderTicker() {
    const nameEl = document.getElementById("header-user-name");
    if (!nameEl) return;
    const link = nameEl.closest(".profile-link");
    if (!link) return;

    link.classList.remove("has-overflow");
    nameEl.style.removeProperty("--header-name-scroll-dist");
    nameEl.style.removeProperty("--header-name-scroll-duration");

    // レイアウト確定後のサイズを取得
    const textWidth = nameEl.scrollWidth;
    const containerWidth = link.clientWidth;
    const overflow = textWidth - containerWidth;

    if (overflow > 3) {
      const scrollDist = overflow + 8;
      const duration = Math.max(3.5, Math.min(10, Math.round(scrollDist / 18 * 10) / 10));
      nameEl.style.setProperty("--header-name-scroll-dist", `-${scrollDist}px`);
      nameEl.style.setProperty("--header-name-scroll-duration", `${duration}s`);
      link.classList.add("has-overflow");
    }
  }

  // トースト通知
  function showToast(message) {
    let container = document.getElementById("global-toast-container");
    if (!container) {
      container = document.createElement("div");
      container.id = "global-toast-container";
      container.className = "toast-container";
      container.setAttribute('role', 'status');
      container.setAttribute('aria-live', 'polite');
      document.body.appendChild(container);
    }

    const toast = document.createElement("div");
    toast.className = "toast-msg";
    toast.textContent = message;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = "0";
      toast.style.transition = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ? 'none' : 'opacity 120ms ease';
      setTimeout(() => toast.remove(), 300);
    }, 2800);
  }

  window.MachirikuHeaderTicker = { update: updateHeaderTicker };

  return {
    init,
    navigate,
    getCurrentScreen: () => currentScreenId,
    showToast,
    updateHeaderTicker,
    logout: (isOnboarding) => MachirikuScreens.handleLogout(isOnboarding)
  };
})();
