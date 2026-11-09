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

    const parsed = parseHash();
    currentScreenId = parsed.screenId;
    currentParams = parsed.params;

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
    let hash = `#${screenId}`;
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

    const renderFnName = `render${currentScreenId.toUpperCase()}`;
    const renderFn = screens[renderFnName];

    let html;
    try {
      html = typeof renderFn === 'function' ? renderFn(currentParams) : screens.renderEmptyState('not-found', '画面が見つかりません', 'メニューから目的の画面を選んでください。');
    } catch (error) {
      console.error('Screen render failed:', currentScreenId, error);
      html = screens.renderEmptyState(currentScreenId, '画面を表示できません', 'もう一度開くか、物件一覧に戻ってください。');
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

    if (isOnboarding || (user && user.isLoggedIn !== false)) {
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

  function renderSubnavLinks(role) {
    const container = document.querySelector(".subnav-links");
    if (!container) return;
    if (renderedNavRole === role) return;
    renderedNavRole = role;
    const items = ROLE_NAV_MENUS[role] || ROLE_NAV_MENUS.resident;
    container.innerHTML = items.map(item => {
      const sId = item.hash.replace("#", "").toLowerCase();
      const isActive = currentScreenId === sId ? "is-active" : "";
      return `<a href="${item.hash}" class="subnav-link ${isActive}" ${isActive ? 'aria-current="page"' : ''}>${item.label}</a>`;
    }).join("");
  }

  function updateSubnavActive() {
    const links = document.querySelectorAll(".subnav-link");
    links.forEach(l => {
      const href = l.getAttribute("href") || "";
      if (href.replace("#", "").toLowerCase() === currentScreenId) {
        l.classList.add("is-active");
        l.setAttribute('aria-current', 'page');
      } else {
        l.classList.remove("is-active");
        l.removeAttribute('aria-current');
      }
    });
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

  return {
    init,
    navigate,
    getCurrentScreen: () => currentScreenId,
    showToast
  };
})();
