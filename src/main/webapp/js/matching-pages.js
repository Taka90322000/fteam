/**
 * マチリク (Machiriku) - matching/ 配下5画面 (S13, S14, S15, S05, B02) コントローラー
 * 各JSPのHTML構造をデータハイドレートし、フォーム検証・確認ダイアログ・タブ切替・状態同期を担当する。
 */
window.MachirikuMatchingPages = (function () {
  const store = window.MachirikuStore;
  const data = window.MACHIRIKU_DATA;

  const PLACEHOLDER_IMG = "assets/property-placeholder.svg";

  function getCtx() {
    if (typeof window !== "undefined" && window.__MACHIRIKU_CONTEXT_PATH__ !== undefined) {
      return String(window.__MACHIRIKU_CONTEXT_PATH__ || "").replace(/\/+$/, "");
    }
    return "";
  }

  function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, (c) => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;"
    }[c]));
  }

  function resolveAssetUrl(url) {
    if (!url) return "";
    const s = String(url);
    if (/^(?:https?:|data:|blob:|\/|\.\.\/)/i.test(s)) return s;
    const ctx = getCtx();
    return ctx ? `${ctx}/${s.replace(/^\/+/, "")}` : `../${s.replace(/^\/+/, "")}`;
  }

  function buildUrl(path, params = {}) {
    const clean = path.startsWith("/") ? path : `/${path}`;
    const base = `${getCtx()}${clean}`;
    const keys = Object.keys(params || {}).filter((k) => params[k] !== undefined && params[k] !== "");
    if (!keys.length) return base;
    return `${base}?${keys.map((k) => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`).join("&")}`;
  }

  function propUrl(jspName, params = {}) {
    return buildUrl(`/property/${jspName}`, params);
  }

  function matchingUrl(jspName, params = {}) {
    return buildUrl(`/matching/${jspName}`, params);
  }

  function indexUrl(hash = "#s01") {
    return `${getCtx()}/index.jsp${hash}`;
  }

  function formatMoney(num) {
    return Number(num || 0).toLocaleString("ja-JP");
  }

  function formatRent(num) {
    return (Number(num || 0) / 10000).toLocaleString("ja-JP", { maximumFractionDigits: 4 });
  }

  function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
  }

  function todayIso() {
    return new Date().toISOString().slice(0, 10);
  }

  function addYearsIso(baseDateStr, years) {
    const base = baseDateStr ? new Date(baseDateStr) : new Date();
    if (isNaN(base.getTime())) return todayIso();
    base.setFullYear(base.getFullYear() + years);
    return base.toISOString().slice(0, 10);
  }

  function pad2(n) {
    return String(n).padStart(2, "0");
  }

  function parseDateParts(raw) {
    const str = String(raw || "").trim();
    if (!str) return null;
    const m = str.match(/^(\d{4})[\/-](\d{1,2})[\/-](\d{1,2})(?:[T\s](\d{1,2}):(\d{1,2})(?::(\d{1,2}))?)?/);
    if (m) {
      return {
        y: m[1],
        m: pad2(m[2]),
        d: pad2(m[3]),
        hh: m[4] != null ? pad2(m[4]) : null,
        mm: m[5] != null ? pad2(m[5]) : null
      };
    }
    const d = new Date(str);
    if (isNaN(d.getTime())) return null;
    const hasTime = /\d{1,2}:\d{2}/.test(str);
    return {
      y: String(d.getFullYear()),
      m: pad2(d.getMonth() + 1),
      d: pad2(d.getDate()),
      hh: hasTime ? pad2(d.getHours()) : null,
      mm: hasTime ? pad2(d.getMinutes()) : null
    };
  }

  function formatDateYMD(raw) {
    const p = parseDateParts(raw);
    if (!p) return String(raw || "");
    return `${p.y}/${p.m}/${p.d}`;
  }

  function formatDateTimeNoSec(raw) {
    const p = parseDateParts(raw);
    if (!p) return String(raw || "");
    return p.hh != null ? `${p.y}/${p.m}/${p.d} ${p.hh}:${p.mm}` : `${p.y}/${p.m}/${p.d}`;
  }

  function formatShortDateTimeNoSec(raw) {
    const p = parseDateParts(raw);
    if (!p) return String(raw || "");
    return p.hh != null ? `${p.m}/${p.d} ${p.hh}:${p.mm}` : `${p.m}/${p.d}`;
  }

  function showRoleNotice(noticeElId, requiredRoles, roleLabel) {
    const el = document.getElementById(noticeElId);
    if (!el) return;
    const state = store.getState();
    const ok = requiredRoles.includes(state.currentRole);
    el.hidden = ok;
    if (!ok) {
      const textEl = el.querySelector("[data-role-notice-text]");
      if (textEl) {
        textEl.textContent = `現在【${state.currentUser?.roleName || state.currentRole}】視点で表示中です。この画面の主な対象ロールは【${roleLabel}】です。`;
      }
    }
  }

  function getAppStatusBadgeClass(status) {
    switch (status) {
      case "consulting":
      case "interviewing":
        return "status-app-consulting";
      case "approved":
        return "status-app-approved";
      case "contracted":
        return "status-app-contracted";
      case "rejected":
      case "withdrawn":
        return "status-app-rejected";
      default:
        return "status-reviewing";
    }
  }

  function getOfferBadgeClass(status) {
    switch (status) {
      case "ACCEPTED": return "status-app-contracted";
      case "DECLINED": return "status-app-rejected";
      case "DRAFT": return "status-draft";
      case "SENT":
      default:
        return "status-app-consulting";
    }
  }

  function getPubBadgeClass(pubStatus) {
    switch (pubStatus) {
      case "PUBLISHED": return "status-app-contracted";
      case "ARCHIVED": return "status-paused";
      case "DRAFT":
      default:
        return "status-draft";
    }
  }

  function formatApplicationPeriod(app, includeOpenPrefix = true) {
    if (app.requestedEndOn) {
      const startText = app.requestedStartOn ? formatDateYMD(app.requestedStartOn) : "未定";
      const endText = formatDateYMD(app.requestedEndOn);
      return `${startText} 〜 ${endText}`;
    }
    const rawOpen = app.targetOpenDate || app.requestedStartOn || "";
    const openDate = rawOpen ? formatDateYMD(rawOpen) : "相談";
    return includeOpenPrefix ? `オープン予定: ${openDate}` : openDate;
  }

  // =========================================================================
  // 共通ユーティリティ: Accessible Form Validation / Confirm Dialog / Tabs / Image
  // =========================================================================
  function getLiveAnnouncer(announcerId = "matching-live-announcer") {
    let el = document.getElementById(announcerId);
    if (!el) {
      el = document.createElement("div");
      el.id = announcerId;
      el.className = "sr-only";
      el.setAttribute("aria-live", "polite");
      document.body.appendChild(el);
    }
    return el;
  }

  function syncFieldValidationState(field, touchedMap, forceInvalid = false) {
    if (!field || field.disabled) {
      if (field) {
        field.classList.remove("user-invalid-fallback");
        field.setAttribute("aria-invalid", "false");
      }
      return false;
    }
    const supportsUserInvalid = typeof CSS !== "undefined" && CSS.supports("selector(:user-invalid)");
    const hasCustomErr = Boolean(field.validity && field.validity.customError);
    let invalid = false;
    if (supportsUserInvalid) {
      invalid = field.matches(":user-invalid") || ((forceInvalid || hasCustomErr) && !field.validity.valid);
      field.classList.toggle("user-invalid-fallback", invalid && !field.matches(":user-invalid"));
    } else {
      const touched = Boolean(touchedMap.get(field));
      invalid = (touched || forceInvalid || hasCustomErr) && !field.validity.valid;
      field.classList.toggle("user-invalid-fallback", invalid);
    }
    field.setAttribute("aria-invalid", String(invalid));
    const errId = field.dataset ? field.dataset.error : "";
    if (errId) {
      const existing = (field.getAttribute("aria-describedby") || "")
        .split(/\s+/)
        .filter(Boolean)
        .filter((id) => id !== errId);
      if (invalid) existing.push(errId);
      if (existing.length > 0) field.setAttribute("aria-describedby", existing.join(" "));
      else field.removeAttribute("aria-describedby");
    }
    return invalid;
  }

  function initAccessibleFormValidation(formEl, options = {}) {
    if (!formEl) return null;
    const announcerId = options.announcerId || "matching-live-announcer";
    const touched = new WeakMap();
    const getFields = () => Array.from(formEl.querySelectorAll("select, input, textarea"));
    let announceTimer = 0;

    const scheduleAnnounce = (field) => {
      if (!field || !field.dataset?.error) return;
      clearTimeout(announceTimer);
      const msgEl = document.getElementById(field.dataset.error);
      const msgText = msgEl ? msgEl.textContent.replace(/\s+/g, " ").trim() : "";
      announceTimer = window.setTimeout(() => {
        const isNowInvalid = field.getAttribute("aria-invalid") === "true";
        const announcer = getLiveAnnouncer(announcerId);
        if (isNowInvalid && msgText) {
          announcer.textContent = "";
          window.requestAnimationFrame(() => {
            announcer.textContent = msgText;
          });
        } else {
          announcer.textContent = "";
        }
      }, 500);
    };

    const resetAll = () => {
      getFields().forEach((f) => {
        touched.set(f, false);
        f.classList.remove("user-invalid-fallback");
        syncFieldValidationState(f, touched);
      });
    };

    if (formEl.dataset.ariaValidationInit !== "true") {
      formEl.dataset.ariaValidationInit = "true";
      getLiveAnnouncer(announcerId);

      formEl.addEventListener("invalid", (e) => {
        const target = e.target;
        if (!target) return;
        touched.set(target, true);
        syncFieldValidationState(target, touched, true);
      }, true);

      formEl.addEventListener("input", (e) => {
        const target = e.target;
        if (!target || !target.matches?.("input, textarea")) return;
        if (target.hasAttribute("aria-invalid")) {
          const invalid = syncFieldValidationState(target, touched);
          if (!invalid) getLiveAnnouncer(announcerId).textContent = "";
        }
      });

      formEl.addEventListener("change", (e) => {
        const target = e.target;
        if (!target || !target.matches?.("select, input, textarea")) return;
        touched.set(target, true);
        const invalid = syncFieldValidationState(target, touched);
        if (invalid) scheduleAnnounce(target);
        else getLiveAnnouncer(announcerId).textContent = "";
      });

      formEl.addEventListener("blur", (e) => {
        const target = e.target;
        if (!target || !target.matches?.("select, input, textarea")) return;
        touched.set(target, true);
        const invalid = syncFieldValidationState(target, touched);
        if (invalid) scheduleAnnounce(target);
      }, true);
    }

    resetAll();
    return {
      markTouched: (field, forceInvalid = false) => {
        if (field) touched.set(field, true);
        return syncFieldValidationState(field, touched, forceInvalid);
      },
      resetField: (field) => {
        if (!field) return;
        touched.set(field, false);
        field.classList.remove("user-invalid-fallback");
        syncFieldValidationState(field, touched);
      },
      resetAll,
      announce: (text) => {
        const announcer = getLiveAnnouncer(announcerId);
        announcer.textContent = "";
        window.requestAnimationFrame(() => {
          announcer.textContent = text;
        });
      },
      focusFirstInvalid: () => {
        const firstInvalid = getFields().find((f) => !f.disabled && f.offsetParent !== null && f.validity && !f.validity.valid);
        if (firstInvalid) {
          touched.set(firstInvalid, true);
          syncFieldValidationState(firstInvalid, touched, true);
          firstInvalid.focus();
          return firstInvalid;
        }
        return null;
      }
    };
  }

  function bindDialogLightDismissFallback(dialog) {
    if (!dialog || ("closedBy" in HTMLDialogElement.prototype)) return;
    const isOutside = (e) => {
      const r = dialog.getBoundingClientRect();
      return e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom;
    };
    let startedOutside = false;
    dialog.addEventListener("pointerdown", (e) => {
      startedOutside = isOutside(e);
    });
    dialog.addEventListener("pointerup", (e) => {
      if (startedOutside && isOutside(e)) dialog.close("cancel");
      startedOutside = false;
    });
  }

  function openConfirmDialog({ id = "matching-confirm-dialog", title, subtitle = "", desc, confirmLabel = "確定する", confirmClass = "btn-primary", onConfirm }) {
    const prev = document.getElementById(id);
    if (prev) prev.remove();

    const dialog = document.createElement("dialog");
    dialog.id = id;
    dialog.className = "o01-confirm-dialog";
    dialog.setAttribute("closedby", "any");
    dialog.setAttribute("aria-labelledby", `${id}-title`);
    dialog.innerHTML = `
      <form method="dialog" class="o01-dialog-panel">
        <h2 id="${escapeHtml(id)}-title" class="o01-dialog-title">${escapeHtml(title)}</h2>
        ${subtitle ? `<p class="o01-dialog-prop">${escapeHtml(subtitle)}</p>` : ""}
        <p class="o01-dialog-desc">${escapeHtml(desc)}</p>
        <div class="o01-dialog-actions">
          <button type="submit" value="cancel" class="btn-secondary o01-dialog-cancel" autofocus>キャンセル</button>
          <button type="submit" value="confirm" class="${escapeHtml(confirmClass)} o01-dialog-confirm">${escapeHtml(confirmLabel)}</button>
        </div>
      </form>
    `;

    bindDialogLightDismissFallback(dialog);

    dialog.addEventListener("close", () => {
      const chosen = dialog.returnValue;
      dialog.returnValue = "";
      dialog.remove();
      if (chosen === "confirm" && typeof onConfirm === "function") {
        onConfirm();
      }
    }, { once: true });

    document.body.appendChild(dialog);
    dialog.showModal();
  }

  function openPhotoLightbox(photos, startIndex = 0) {
    if (!Array.isArray(photos) || !photos.length) return;
    const prev = document.getElementById("matching-photo-lightbox");
    if (prev) prev.remove();

    let current = ((Number(startIndex) || 0) % photos.length + photos.length) % photos.length;
    const hasMany = photos.length > 1;
    const dialog = document.createElement("dialog");
    dialog.id = "matching-photo-lightbox";
    dialog.className = "confirm-lightbox-dialog";
    dialog.setAttribute("closedby", "any");
    dialog.setAttribute("aria-label", "写真の拡大表示");
    dialog.innerHTML = `
      <div class="confirm-lightbox-frame">
        <div class="confirm-lightbox-stage">
          <img class="confirm-lightbox-img" alt="">
        </div>
        <div class="confirm-lightbox-meta">
          <span class="confirm-lightbox-caption"></span>
          <span class="confirm-lightbox-counter"></span>
        </div>
        <div class="confirm-lightbox-toolbar">
          <button type="button" class="confirm-lightbox-btn confirm-lightbox-arrow-btn" data-dir="-1" aria-label="前の写真"${hasMany ? "" : " hidden"}>
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 18l-6-6 6-6"/></svg>
          </button>
          <button type="button" class="confirm-lightbox-btn confirm-lightbox-close" autofocus aria-label="拡大表示を閉じる">閉じる</button>
          <button type="button" class="confirm-lightbox-btn confirm-lightbox-arrow-btn" data-dir="1" aria-label="次の写真"${hasMany ? "" : " hidden"}>
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18l6-6-6-6"/></svg>
          </button>
        </div>
      </div>
    `;

    const imgEl = dialog.querySelector(".confirm-lightbox-img");
    const captionEl = dialog.querySelector(".confirm-lightbox-caption");
    const counterEl = dialog.querySelector(".confirm-lightbox-counter");

    const renderPhoto = (idx) => {
      current = ((idx % photos.length) + photos.length) % photos.length;
      const item = photos[current];
      imgEl.src = resolveAssetUrl(item.url);
      imgEl.alt = item.label || `写真 ${current + 1}`;
      captionEl.textContent = item.label || `写真 ${current + 1}`;
      counterEl.textContent = `${current + 1} / ${photos.length}`;
    };

    dialog.addEventListener("click", (e) => {
      const dirBtn = e.target.closest("[data-dir]");
      if (dirBtn) {
        renderPhoto(current + Number(dirBtn.dataset.dir));
        return;
      }
      if (e.target.closest(".confirm-lightbox-close")) {
        dialog.close();
      }
    });
    bindDialogLightDismissFallback(dialog);

    const onKeyDown = (e) => {
      if (!hasMany) return;
      if (e.key === "ArrowLeft") { e.preventDefault(); renderPhoto(current - 1); }
      else if (e.key === "ArrowRight") { e.preventDefault(); renderPhoto(current + 1); }
    };

    dialog.addEventListener("close", () => {
      document.removeEventListener("keydown", onKeyDown);
      dialog.remove();
    }, { once: true });

    renderPhoto(current);
    document.body.appendChild(dialog);
    document.addEventListener("keydown", onKeyDown);
    dialog.showModal();
  }

  function wireTablistKeyboard(tablistEl, onSelectTab) {
    if (!tablistEl || tablistEl.dataset.kbdWired === "true") return;
    tablistEl.dataset.kbdWired = "true";
    tablistEl.addEventListener("keydown", (e) => {
      const tabs = Array.from(tablistEl.querySelectorAll('[role="tab"]'));
      const curIdx = tabs.indexOf(document.activeElement);
      if (curIdx === -1) return;
      let nextIdx = -1;
      if (e.key === "ArrowRight") nextIdx = (curIdx + 1) % tabs.length;
      else if (e.key === "ArrowLeft") nextIdx = (curIdx - 1 + tabs.length) % tabs.length;
      else if (e.key === "Home") nextIdx = 0;
      else if (e.key === "End") nextIdx = tabs.length - 1;
      if (nextIdx !== -1) {
        e.preventDefault();
        const target = tabs[nextIdx];
        target.focus();
        target.scrollIntoView({ block: "nearest", inline: "nearest" });
        if (typeof onSelectTab === "function") onSelectTab(target);
      }
    });
  }

  function readOptimizedImageDataUrl(file) {
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = (ev) => {
        const rawUrl = String(ev.target?.result || "");
        if (!rawUrl || rawUrl.length <= 350000 || !/^data:image\//i.test(rawUrl)) {
          resolve(rawUrl);
          return;
        }
        const img = new Image();
        img.onload = () => {
          try {
            const maxDim = 1200;
            const scale = Math.min(1, maxDim / Math.max(img.width || 1, img.height || 1));
            const w = Math.max(1, Math.round((img.width || 1) * scale));
            const h = Math.max(1, Math.round((img.height || 1) * scale));
            const canvas = document.createElement("canvas");
            canvas.width = w;
            canvas.height = h;
            const ctx = canvas.getContext("2d");
            if (!ctx) { resolve(rawUrl); return; }
            ctx.fillStyle = "#ffffff";
            ctx.fillRect(0, 0, w, h);
            ctx.drawImage(img, 0, 0, w, h);
            const compressed = canvas.toDataURL("image/jpeg", 0.82);
            resolve(compressed && compressed.length < rawUrl.length ? compressed : rawUrl);
          } catch (_) {
            resolve(rawUrl);
          }
        };
        img.onerror = () => resolve(rawUrl);
        img.src = rawUrl;
      };
      reader.onerror = () => resolve("");
      reader.readAsDataURL(file);
    });
  }

  // =========================================================================
  // S13: matching/offer.jsp (事業主への出店オファー / オーナー)
  // =========================================================================
  let s13StatusFilter = "ALL";
  let s13EditingOfferId = "";
  let s13SelectedPropId = "";
  let s13SelectedBizId = "";

  function getOfferCandidates() {
    const base = Array.isArray(data.OFFER_CANDIDATE_BUSINESSES) ? data.OFFER_CANDIDATE_BUSINESSES : [];
    const state = store.getState();
    const activeBiz = state.users?.business;
    if (!activeBiz) return base;
    return base.map((b) => {
      if (b.id === activeBiz.id) {
        return {
          ...b,
          name: activeBiz.name || b.name,
          businessName: activeBiz.businessName || activeBiz.company || b.businessName,
          preferredIndustries: Array.isArray(activeBiz.preferredIndustries) && activeBiz.preferredIndustries.length
            ? activeBiz.preferredIndustries
            : b.preferredIndustries,
          avgSatisfaction: activeBiz.avgSatisfaction || 0,
          reviewCount: activeBiz.reviewCount || 0
        };
      }
      return b;
    });
  }

  function hydrateS13(params = {}) {
    const sectionEl = document.getElementById("s13-section");
    if (!sectionEl) return;

    showRoleNotice("s13-role-notice", ["owner", "admin"], "物件オーナー");
    const roleBtn = document.getElementById("s13-role-switch-btn");
    if (roleBtn) {
      roleBtn.onclick = () => store.setRole("owner");
    }

    const state = store.getState();
    const offers = store.getOffers();
    const candidates = getOfferCandidates();
    const ownerIds = new Set(state.users?.owner?.ownedPropIds || []);
    const ownedProps = state.properties.filter((p) => ownerIds.has(p.id) || p.ownerId === state.users?.owner?.id);
    const selectableProps = ownedProps.length ? ownedProps : state.properties;

    // KPI更新
    setText("s13-kpi-sent", String(offers.filter((o) => o.offerStatus === "SENT").length));
    setText("s13-kpi-accepted", String(offers.filter((o) => o.offerStatus === "ACCEPTED").length));
    setText("s13-kpi-draft", String(offers.filter((o) => o.offerStatus === "DRAFT").length));
    setText("s13-kpi-candidates", String(candidates.length));

    // 初期物件・事業主選択
    const propSelect = document.getElementById("s13-prop-select");
    const bizSelect = document.getElementById("s13-biz-select");
    if (!s13SelectedPropId) {
      s13SelectedPropId = params?.propId || params?.id || selectableProps[0]?.id || "";
    }
    if (!selectableProps.some((p) => p.id === s13SelectedPropId)) {
      s13SelectedPropId = selectableProps[0]?.id || "";
    }
    if (!s13SelectedBizId) {
      s13SelectedBizId = params?.bizId || candidates[0]?.id || "";
    }

    if (propSelect) {
      propSelect.innerHTML = selectableProps.map((p) => {
        const topLabel = p.topDemands?.[0]?.label ? ` / 需要1位: ${p.topDemands[0].label}` : "";
        return `<option value="${escapeHtml(p.id)}" ${p.id === s13SelectedPropId ? "selected" : ""}>${escapeHtml(p.title)}（賃料 ${formatRent(p.rent)}万円${escapeHtml(topLabel)}）</option>`;
      }).join("");
    }

    const getActiveProp = () => store.getPropertyById(propSelect?.value || s13SelectedPropId) || selectableProps[0];

    const renderPropSummaryAndCandidates = () => {
      const prop = getActiveProp();
      s13SelectedPropId = prop?.id || "";
      const summaryEl = document.getElementById("s13-prop-summary");
      const topDemands = prop?.topDemands || [];
      const topIds = new Set(topDemands.slice(0, 3).map((d) => d.subcatId));
      const analyticsLink = document.getElementById("s13-link-analytics");
      if (analyticsLink && prop) {
        analyticsLink.href = propUrl("analytics.jsp", { id: prop.id });
      }

      if (summaryEl && prop) {
        const top1 = topDemands[0];
        summaryEl.innerHTML = `
          <div class="matching-inline-summary-head">
            <strong>${escapeHtml(prop.title)}</strong>
            <a href="${escapeHtml(propUrl("detail.jsp", { id: prop.id }))}">物件詳細を見る →</a>
          </div>
          <div class="matching-inline-summary-meta">
            ${escapeHtml(prop.address)} ／ 賃料 ${formatRent(prop.rent)}万円・${prop.areaTsubo}坪 ／ 住民需要1位: <strong>${escapeHtml(top1 ? `${top1.label}（${top1.score}pt）` : "集計中")}</strong>
          </div>
        `;
      }

      if (bizSelect) {
        bizSelect.innerHTML = candidates.map((b) => {
          const isMatch = (b.preferredIndustries || []).some((id) => topIds.has(id));
          const genreNames = (b.preferredIndustries || []).map((id) => data.SUBCATS?.[id]?.name || id).join("・");
          return `<option value="${escapeHtml(b.id)}" ${b.id === s13SelectedBizId ? "selected" : ""}>${escapeHtml(b.businessName)}（${escapeHtml(b.name)}） - ${escapeHtml(genreNames)}${isMatch ? " ★需要一致" : ""}</option>`;
        }).join("");
      }

      const candListEl = document.getElementById("s13-candidate-list");
      if (candListEl) {
        candListEl.innerHTML = candidates.map((b) => {
          const isSelected = b.id === (bizSelect?.value || s13SelectedBizId);
          const isMatch = (b.preferredIndustries || []).some((id) => topIds.has(id));
          const genres = (b.preferredIndustries || []).map((id) => data.SUBCATS?.[id]?.name || id);
          return `
            <button type="button" class="matching-candidate-card${isSelected ? " is-selected" : ""}" data-s13-biz="${escapeHtml(b.id)}" aria-pressed="${String(isSelected)}">
              <div class="matching-candidate-top">
                <strong>${escapeHtml(b.businessName)}</strong>
                ${isMatch ? '<span class="req-badge-ok">住民需要と一致</span>' : ""}
              </div>
              <div class="matching-candidate-rep">代表: ${escapeHtml(b.name)} ／ ${escapeHtml(b.preferredArea)}</div>
              <div class="matching-candidate-tags">
                ${genres.map((g) => `<span class="qual-badge">${escapeHtml(g)}</span>`).join("")}
              </div>
            </button>
          `;
        }).join("");

        candListEl.onclick = (e) => {
          const btn = e.target.closest("[data-s13-biz]");
          if (!btn) return;
          s13SelectedBizId = btn.getAttribute("data-s13-biz") || "";
          if (bizSelect) bizSelect.value = s13SelectedBizId;
          renderPropSummaryAndCandidates();
          renderBizPreview();
        };
      }
    };

    const renderBizPreview = () => {
      const bizId = bizSelect?.value || s13SelectedBizId;
      s13SelectedBizId = bizId;
      const biz = candidates.find((b) => b.id === bizId) || candidates[0];
      const prop = getActiveProp();
      const previewEl = document.getElementById("s13-biz-preview-body");
      if (!previewEl || !biz) return;

      const genres = (biz.preferredIndustries || []).map((id) => data.SUBCATS?.[id]?.name || id);
      const topIds = new Set((prop?.topDemands || []).slice(0, 3).map((d) => d.subcatId));
      const matchedNames = (biz.preferredIndustries || [])
        .filter((id) => topIds.has(id))
        .map((id) => data.SUBCATS?.[id]?.name || id);
      const reviewText = biz.reviewCount ? `${biz.avgSatisfaction} / 5（${biz.reviewCount}件）` : "集計中（新規出店）";

      previewEl.innerHTML = `
        <div class="matching-biz-head">
          <h3>${escapeHtml(biz.businessName)}</h3>
          <p class="matching-biz-rep">代表者: ${escapeHtml(biz.representativeName || biz.name)}（従業員 ${biz.employeeCount || 1}名）</p>
        </div>
        ${matchedNames.length ? `<div class="matching-match-callout">この物件の上位需要「<strong>${escapeHtml(matchedNames.join("・"))}</strong>」と希望業種が合致しています。</div>` : ""}
        <dl class="matching-dl-compact">
          <div><dt>希望業種</dt><dd>${escapeHtml(genres.join(" / ") || "相談可")}</dd></div>
          <div><dt>出店希望地域</dt><dd>${escapeHtml(biz.preferredArea || "-")}</dd></div>
          <div><dt>業界・経営経験</dt><dd>${escapeHtml(biz.experienceYears || "-")}<br>${escapeHtml(biz.managementExperience || "")}</dd></div>
          <div><dt>来店アンケート評価</dt><dd>${escapeHtml(reviewText)}</dd></div>
        </dl>
        <div class="matching-qual-block">
          <div class="matching-sub-label">保有資格・許可</div>
          <div class="qualifications-grid">
            ${(biz.qualifications || []).map((q) => `<span class="qual-badge">${escapeHtml(q)}</span>`).join("") || '<span class="sample-note">登録なし</span>'}
          </div>
        </div>
        <p class="matching-biz-story">${escapeHtml(biz.story || "")}</p>
      `;
    };

    // 優遇条件チェックチップ生成
    const incentivesEl = document.getElementById("s13-incentives-grid");
    if (incentivesEl && !incentivesEl.children.length) {
      const opts = data.OFFER_INCENTIVE_OPTIONS || [];
      incentivesEl.innerHTML = opts.map((opt) => `
        <div class="check-chip">
          <input id="s13-inc-${escapeHtml(opt.key)}" type="checkbox" name="offerIncentive" value="${escapeHtml(opt.label)}">
          <label for="s13-inc-${escapeHtml(opt.key)}">${escapeHtml(opt.label)}</label>
        </div>
      `).join("");
    }

    if (propSelect) {
      propSelect.onchange = () => {
        s13SelectedPropId = propSelect.value;
        renderPropSummaryAndCandidates();
        renderBizPreview();
      };
    }
    if (bizSelect) {
      bizSelect.onchange = () => {
        s13SelectedBizId = bizSelect.value;
        renderPropSummaryAndCandidates();
        renderBizPreview();
      };
    }

    renderPropSummaryAndCandidates();
    renderBizPreview();

    const form = document.getElementById("s13-offer-form");
    const msgEl = document.getElementById("s13-message");
    const validationCtrl = initAccessibleFormValidation(form, { announcerId: "s13-live-announcer" });

    // テンプレート挿入
    const insertTemplate = (kind) => {
      const prop = getActiveProp();
      const biz = candidates.find((b) => b.id === (bizSelect?.value || s13SelectedBizId)) || candidates[0];
      const top1 = prop?.topDemands?.[0]?.label || "貴店の業態";
      if (!msgEl) return;
      if (kind === "demand") {
        msgEl.value = `${biz?.businessName || ""} ${biz?.name || ""}様\n\n「${prop?.title || ""}」の物件オーナーでございます。当物件の地域住民による需要投票で「${top1}」への要望が多く寄せられており、貴店のコンセプトに大変共感いたしました。賃料や初期条件も含めて前向きにご相談させていただきたく、ぜひ一度ご内覧または面談の機会をいただけますでしょうか。`;
      } else {
        msgEl.value = `${biz?.businessName || ""} ${biz?.name || ""}様\n\n「${prop?.title || ""}」（${prop?.condition || "居抜き・路面店舗"}）のご紹介でご連絡いたしました。既存の設備を活かして初期投資を抑えたご出店が可能です。フリーレントや内装工事期間のご相談にも柔軟に対応いたしますので、ぜひ現地をご覧いただければ幸いです。`;
      }
      validationCtrl?.markTouched(msgEl);
      msgEl.focus();
    };

    const tplDemand = document.getElementById("s13-tpl-demand");
    const tplInuki = document.getElementById("s13-tpl-inuki");
    if (tplDemand) tplDemand.onclick = () => insertTemplate("demand");
    if (tplInuki) tplInuki.onclick = () => insertTemplate("inuki");

    const clearEditMode = () => {
      s13EditingOfferId = "";
      const keepPropId = propSelect?.value || s13SelectedPropId;
      const keepBizId = bizSelect?.value || s13SelectedBizId;
      if (form) form.reset();
      if (propSelect && keepPropId) propSelect.value = keepPropId;
      if (bizSelect && keepBizId) bizSelect.value = keepBizId;
      const idInput = document.getElementById("s13-offer-id");
      if (idInput) idInput.value = "";
      const banner = document.getElementById("s13-edit-banner");
      if (banner) banner.hidden = true;
      if (msgEl) msgEl.value = "";
      form?.querySelectorAll('input[name="offerIncentive"]').forEach((cb) => { cb.checked = false; });
      validationCtrl?.resetAll();
    };

    const cancelEditBtn = document.getElementById("s13-cancel-edit-btn");
    if (cancelEditBtn) cancelEditBtn.onclick = clearEditMode;

    const collectOfferPayload = (offerStatus) => {
      const incentives = Array.from(form?.querySelectorAll('input[name="offerIncentive"]:checked') || []).map((cb) => cb.value);
      const kindChecked = form?.querySelector('input[name="creationKind"]:checked');
      return {
        id: s13EditingOfferId || undefined,
        propId: propSelect?.value || "",
        businessUserId: bizSelect?.value || "",
        creationKind: kindChecked ? kindChecked.value : "MANUAL",
        incentives,
        message: msgEl ? msgEl.value.trim() : "",
        offerStatus
      };
    };

    const draftBtn = document.getElementById("s13-save-draft-btn");
    if (draftBtn) {
      draftBtn.onclick = () => {
        const payload = collectOfferPayload("DRAFT");
        if (!payload.propId || !payload.businessUserId) {
          window.MachirikuApp?.showToast?.("提案物件と対象事業主を選択してください");
          return;
        }
        store.saveOffer(payload);
        clearEditMode();
        window.MachirikuApp?.showToast?.("オファーを下書き保存しました");
      };
    }

    if (form) {
      form.onsubmit = (e) => {
        e.preventDefault();
        if (!form.checkValidity()) {
          const inv = validationCtrl?.focusFirstInvalid();
          if (inv) {
            const m = inv.validationMessage || "入力内容を確認してください";
            validationCtrl?.announce(m);
            window.MachirikuApp?.showToast?.(m);
          }
          return;
        }
        const payload = collectOfferPayload("SENT");
        const prop = store.getPropertyById(payload.propId);
        const biz = candidates.find((b) => b.id === payload.businessUserId);
        openConfirmDialog({
          id: "s13-send-dialog",
          title: "この内容でオファーを送信しますか？",
          subtitle: `${biz?.businessName || ""}（${biz?.name || ""}）様宛 ／ ${prop?.title || ""}`,
          desc: "送信すると対象事業主の「応募状況確認」画面にオファーが届き、承諾または辞退の回答を受け取れるようになります。",
          confirmLabel: "オファーを送信する",
          onConfirm: () => {
            store.saveOffer(payload);
            clearEditMode();
            window.MachirikuApp?.showToast?.("事業主へオファーを送信しました");
          }
        });
      };
    }

    // 送信済み・下書き一覧タブ＆描画
    const tabsEl = document.getElementById("s13-status-tabs");
    const applyStatusTab = (btn) => {
      if (!btn) return;
      s13StatusFilter = btn.getAttribute("data-offer-status") || "ALL";
      tabsEl?.querySelectorAll('[role="tab"]').forEach((t) => {
        t.setAttribute("aria-selected", String(t === btn));
      });
      renderS13History();
    };

    if (tabsEl) {
      tabsEl.querySelectorAll('[role="tab"]').forEach((btn) => {
        const st = btn.getAttribute("data-offer-status");
        btn.setAttribute("aria-selected", String(st === s13StatusFilter));
        btn.onclick = () => applyStatusTab(btn);
      });
      wireTablistKeyboard(tabsEl, applyStatusTab);
    }

    const renderS13History = () => {
      const allOffers = store.getOffers();
      const filtered = allOffers.filter((o) => s13StatusFilter === "ALL" || o.offerStatus === s13StatusFilter);
      setText("s13-history-count", String(filtered.length));
      const emptyEl = document.getElementById("s13-history-empty");
      const listEl = document.getElementById("s13-history-list");
      if (!listEl) return;

      if (!filtered.length) {
        if (emptyEl) emptyEl.hidden = false;
        listEl.innerHTML = "";
        return;
      }
      if (emptyEl) emptyEl.hidden = true;

      listEl.innerHTML = filtered.map((o) => {
        const kindLabel = o.creationKind === "AUTO" ? "自動オファー候補" : "個別オファー";
        const dateLabel = o.sentAt ? `送信日時: ${o.sentAt}` : `作成日時: ${o.createdAt || ""}`;
        return `
          <article class="detail-card apply-item-card">
            <div class="apply-item-head">
              <div class="apply-item-main">
                <div class="apply-item-meta">
                  <span class="badge-status ${escapeHtml(getOfferBadgeClass(o.offerStatus))}">${escapeHtml(o.statusLabel || o.offerStatus)}</span>
                  <span class="qual-badge">${escapeHtml(kindLabel)}</span>
                  <span class="apply-item-date">${escapeHtml(dateLabel)}</span>
                </div>
                <h3 class="apply-item-title">${escapeHtml(o.businessName)} <small>（${escapeHtml(o.businessRepName || "")}）</small></h3>
                <div class="apply-item-prop">
                  対象物件: <a href="${escapeHtml(propUrl("detail.jsp", { id: o.propId }))}">${escapeHtml(o.propTitle)}</a>
                </div>
                ${(o.incentives || []).length ? `
                  <div class="apply-card-summary-row">
                    ${o.incentives.map((inc) => `<span class="apply-summary-pill">${escapeHtml(inc)}</span>`).join("")}
                  </div>
                ` : ""}
                <p class="matching-offer-excerpt">${escapeHtml(o.message || "（メッセージ未入力）")}</p>
                ${o.respondedAt ? `<p class="apply-item-date">事業主回答日時: ${escapeHtml(o.respondedAt)}${o.responseNote ? `（${escapeHtml(o.responseNote)}）` : ""}</p>` : ""}
              </div>
              <div class="matching-card-actions">
                ${o.offerStatus === "DRAFT" ? `
                  <button type="button" class="btn-secondary btn-sm" data-s13-edit="${escapeHtml(o.id)}">編集する</button>
                  <button type="button" class="btn-primary btn-sm" data-s13-send-now="${escapeHtml(o.id)}">今すぐ送信</button>
                ` : `
                  <a class="btn-secondary btn-sm" href="${escapeHtml(propUrl("detail.jsp", { id: o.propId }))}">物件を見る</a>
                `}
              </div>
            </div>
          </article>
        `;
      }).join("");

      listEl.onclick = (e) => {
        const editBtn = e.target.closest("[data-s13-edit]");
        if (editBtn) {
          const target = store.getOfferById(editBtn.getAttribute("data-s13-edit"));
          if (!target) return;
          s13EditingOfferId = target.id;
          s13SelectedPropId = target.propId;
          s13SelectedBizId = target.businessUserId;
          if (propSelect) propSelect.value = target.propId;
          if (bizSelect) bizSelect.value = target.businessUserId;
          const kindInput = form?.querySelector(`input[name="creationKind"][value="${target.creationKind || "MANUAL"}"]`);
          if (kindInput) kindInput.checked = true;
          form?.querySelectorAll('input[name="offerIncentive"]').forEach((cb) => {
            cb.checked = (target.incentives || []).includes(cb.value);
          });
          if (msgEl) msgEl.value = target.message || "";
          const idInput = document.getElementById("s13-offer-id");
          if (idInput) idInput.value = target.id;
          const banner = document.getElementById("s13-edit-banner");
          if (banner) banner.hidden = false;
          setText("s13-edit-banner-text", `下書き（${target.businessName}様宛）を編集中です`);
          renderPropSummaryAndCandidates();
          renderBizPreview();
          document.getElementById("s13-form-heading")?.scrollIntoView({ behavior: "smooth", block: "start" });
          return;
        }
        const sendBtn = e.target.closest("[data-s13-send-now]");
        if (sendBtn) {
          const target = store.getOfferById(sendBtn.getAttribute("data-s13-send-now"));
          if (!target) return;
          if (!target.message || !target.message.trim()) {
            window.MachirikuApp?.showToast?.("送信前に「編集する」からメッセージを入力してください");
            return;
          }
          openConfirmDialog({
            id: "s13-send-draft-dialog",
            title: "下書きオファーを送信しますか？",
            subtitle: `${target.businessName}様宛 ／ ${target.propTitle}`,
            desc: "事業主の「応募状況確認」画面へオファーが送信されます。",
            confirmLabel: "送信する",
            onConfirm: () => {
              store.updateOfferStatus(target.id, "SENT");
              window.MachirikuApp?.showToast?.("下書きオファーを送信しました");
            }
          });
        }
      };
    };

    renderS13History();
  }

  // =========================================================================
  // S14: matching/contract.jsp (契約成立・物件引渡し記録 / オーナー・事業主)
  // =========================================================================
  let s14SelectedAppId = "";
  let s14PrevAppId = "";
  let s14MobileStepsToggled = false;
  let s14MobileStep2Toggled = false;
  let s14SyncingDetails = false;
  let s14LastWorkflow = null;
  let s14MediaListenerBound = false;

  function syncS14Disclosures(workflow) {
    if (!workflow) return;
    const stepsDetails = document.getElementById("s14-steps-details");
    const step2Details = document.getElementById("s14-step2-details");
    const isMobile = window.matchMedia("(max-width: 768px)").matches;
    s14SyncingDetails = true;
    try {
      if (isMobile) {
        if (stepsDetails && !s14MobileStepsToggled) stepsDetails.open = false;
        if (step2Details && !s14MobileStep2Toggled) step2Details.open = !workflow.isContractSaved;
      } else {
        if (stepsDetails) stepsDetails.open = true;
        if (step2Details) step2Details.open = true;
      }
    } finally {
      s14SyncingDetails = false;
    }
  }

  function getS14WorkflowState(app, tenancy) {
    if (!tenancy) {
      return {
        label: "契約情報未記録",
        badgeClass: "status-reviewing",
        stepIndex: 1,
        isContractSaved: false,
        ownerDone: false,
        bizDone: false,
        bothDone: false
      };
    }
    const ownerDone = Boolean(tenancy.ownerHandoverAt);
    const bizDone = Boolean(tenancy.businessHandoverAt);
    const bothDone = ownerDone && bizDone;
    if (tenancy.tenancyStatus === "OPEN") {
      return {
        label: "営業中・公開済み",
        badgeClass: "status-app-contracted",
        stepIndex: 3,
        isContractSaved: true,
        ownerDone: true,
        bizDone: true,
        bothDone: true
      };
    }
    if (tenancy.tenancyStatus === "HANDED_OVER" || bothDone) {
      return {
        label: "契約成立・引渡し完了",
        badgeClass: "status-app-contracted",
        stepIndex: 3,
        isContractSaved: true,
        ownerDone: true,
        bizDone: true,
        bothDone: true
      };
    }
    let waitLabel = "契約記録済み・引渡し確認待ち";
    if (ownerDone && !bizDone) waitLabel = "引渡し確認中（事業主確認待ち）";
    else if (!ownerDone && bizDone) waitLabel = "引渡し確認中（オーナー確認待ち）";
    return {
      label: waitLabel,
      badgeClass: "status-app-approved",
      stepIndex: 2,
      isContractSaved: true,
      ownerDone,
      bizDone,
      bothDone: false
    };
  }

  function hydrateS14(params = {}) {
    const sectionEl = document.getElementById("s14-section");
    if (!sectionEl) return;

    showRoleNotice("s14-role-notice", ["owner", "business", "admin"], "物件オーナー・出店事業主");
    const swOwner = document.getElementById("s14-switch-owner-btn");
    const swBiz = document.getElementById("s14-switch-business-btn");
    if (swOwner) swOwner.onclick = () => store.setRole("owner");
    if (swBiz) swBiz.onclick = () => store.setRole("business");

    const state = store.getState();
    const isBizView = state.currentRole === "business";
    const isOwnerActor = state.currentRole === "owner" || state.currentRole === "admin";
    const isBizActor = state.currentRole === "business" || state.currentRole === "admin";
    const bcList = document.getElementById("s14-bc-list");
    const backBtn = document.getElementById("s14-back-btn");
    if (bcList) {
      bcList.textContent = isBizView ? "応募状況確認" : "出店希望確認";
      bcList.href = isBizView ? matchingUrl("applications.jsp") : propUrl("apply-list.jsp");
    }
    if (backBtn) {
      backBtn.href = isBizView ? matchingUrl("applications.jsp") : propUrl("apply-list.jsp");
    }

    const apps = state.applications || [];
    const tenancies = store.getTenancies();
    const eligibleApps = apps.filter((a) =>
      a.status === "approved" ||
      a.status === "contracted" ||
      tenancies.some((t) => t.applicationId === a.id)
    );

    const emptyBox = document.getElementById("s14-empty-box");
    const contentWrap = document.getElementById("s14-content-wrap");

    if (!eligibleApps.length) {
      if (emptyBox) emptyBox.hidden = false;
      if (contentWrap) contentWrap.hidden = true;
      return;
    }
    if (emptyBox) emptyBox.hidden = true;
    if (contentWrap) contentWrap.hidden = false;

    const requestedId = params?.id || params?.appId || s14SelectedAppId;
    const activeApp = eligibleApps.find((a) => a.id === requestedId) || eligibleApps[0];
    if (s14PrevAppId !== activeApp.id) {
      s14MobileStepsToggled = false;
      s14MobileStep2Toggled = false;
      s14PrevAppId = activeApp.id;
    }
    s14SelectedAppId = activeApp.id;

    const prop = store.getPropertyById(activeApp.propId);
    const tenancy = store.getTenancyByApplicationId(activeApp.id);
    const workflow = getS14WorkflowState(activeApp, tenancy);
    s14LastWorkflow = workflow;

    // 案件セレクタ（物件名を先頭に置き、モバイルでも建物・部屋番号を識別しやすくする）
    const appSelect = document.getElementById("s14-app-select");
    if (appSelect) {
      const supportsBaseSelect = Boolean(window.CSS?.supports?.("appearance", "base-select"));
      const triggerHtml = supportsBaseSelect
        ? '<button type="button" class="s14-select-trigger"><selectedcontent class="s14-select-value"></selectedcontent></button>'
        : "";
      appSelect.innerHTML = triggerHtml + eligibleApps.map((a) => {
        return `<option value="${escapeHtml(a.id)}" ${a.id === activeApp.id ? "selected" : ""}>${escapeHtml(a.propTitle)} ／ ${escapeHtml(a.storeName)}</option>`;
      }).join("");
      appSelect.onchange = () => {
        s14SelectedAppId = appSelect.value;
        window.location.href = matchingUrl("contract.jsp", { id: s14SelectedAppId });
      };
    }

    const quickSummaryEl = document.getElementById("s14-quick-summary");
    if (quickSummaryEl) {
      const propTitle = prop ? prop.title : (activeApp.propTitle || "-");
      const propLinkHtml = prop
        ? `<a href="${escapeHtml(propUrl("detail.jsp", { id: prop.id }))}">${escapeHtml(propTitle)}</a>`
        : `<span>${escapeHtml(propTitle)}</span>`;
      quickSummaryEl.innerHTML = `
        <span class="s14-strip-item">物件: ${propLinkHtml}</span>
        <span class="s14-strip-sep" aria-hidden="true">/</span>
        <span class="s14-strip-item">出店者: <strong>${escapeHtml(activeApp.applicantName || "-")}</strong></span>
      `;
    }

    // ヘッダーステータスバッジ＆進行ステップバー
    const statusBadge = document.getElementById("s14-status-badge");
    if (statusBadge) {
      statusBadge.textContent = workflow.label;
      statusBadge.className = `badge-status ${workflow.badgeClass}`;
    }

    const stages = ["出店合意", "契約情報の記録", "双方の物件引渡し確認", "出店計画の公開"];
    setText("s14-steps-compact-label", `ステップ ${workflow.stepIndex + 1}/${stages.length}：${stages[workflow.stepIndex]}`);

    const step2SummaryBadge = document.getElementById("s14-step2-summary-badge");
    const step2SummaryHint = document.getElementById("s14-step2-summary-hint");
    if (step2SummaryBadge) {
      step2SummaryBadge.textContent = workflow.isContractSaved ? "完了 ✓" : "現在";
      step2SummaryBadge.className = `step-state-badge ${workflow.isContractSaved ? "is-done" : "is-now"}`;
    }
    if (step2SummaryHint) {
      step2SummaryHint.textContent = workflow.isContractSaved ? "編集" : "入力";
    }

    const stepsDetailsEl = document.getElementById("s14-steps-details");
    const step2DetailsEl = document.getElementById("s14-step2-details");
    if (stepsDetailsEl) {
      stepsDetailsEl.ontoggle = () => {
        if (!s14SyncingDetails && window.matchMedia("(max-width: 768px)").matches) {
          s14MobileStepsToggled = true;
        }
      };
    }
    if (step2DetailsEl) {
      step2DetailsEl.ontoggle = () => {
        if (!s14SyncingDetails && window.matchMedia("(max-width: 768px)").matches) {
          s14MobileStep2Toggled = true;
        }
      };
    }
    if (!s14MediaListenerBound && window.matchMedia) {
      s14MediaListenerBound = true;
      window.matchMedia("(max-width: 768px)").addEventListener("change", () => {
        syncS14Disclosures(s14LastWorkflow);
      });
    }
    syncS14Disclosures(workflow);

    const stepsEl = document.getElementById("s14-steps-list");
    if (stepsEl) {
      stepsEl.innerHTML = stages.map((label, idx) => {
        const isComplete = idx < workflow.stepIndex;
        const isCurrent = idx === workflow.stepIndex;
        const cls = isComplete ? "is-complete" : (isCurrent ? "is-current" : "");
        const badgeHtml = isComplete
          ? '<small class="step-state-badge is-done">完了 ✓</small>'
          : (isCurrent
            ? '<small class="step-state-badge is-now">現在</small>'
            : '<small class="step-state-badge is-pending">未着手</small>');
        return `<li class="${cls}"${isCurrent ? ' aria-current="step"' : ""}><span class="step-number">${idx + 1}</span><span>${escapeHtml(label)}${badgeHtml}</span></li>`;
      }).join("");
    }

    // 概要グリッド
    const summaryEl = document.getElementById("s14-summary-grid");
    if (summaryEl) {
      const contractLabel = activeApp.contractKind === "POPUP" ? "ポップアップ短期出店" : "通常賃貸契約（長期）";
      summaryEl.innerHTML = `
        <div class="o10-summary-item">
          <dt>対象物件</dt>
          <dd>${prop ? `<a href="${escapeHtml(propUrl("detail.jsp", { id: prop.id }))}">${escapeHtml(prop.title)}</a>` : escapeHtml(activeApp.propTitle || "-")}</dd>
        </div>
        <div class="o10-summary-item">
          <dt>出店事業者・代表者</dt>
          <dd>${escapeHtml(activeApp.applicantName || "-")}</dd>
        </div>
        <div class="o10-summary-item">
          <dt>希望業態</dt>
          <dd>${escapeHtml(activeApp.genreName || "-")}</dd>
        </div>
        <div class="o10-summary-item">
          <dt>契約区分</dt>
          <dd>${escapeHtml(contractLabel)}</dd>
        </div>
        <div class="o10-summary-item">
          <dt>応募時の希望時期 / 期間</dt>
          <dd>${escapeHtml(formatApplicationPeriod(activeApp, false))}</dd>
        </div>
        <div class="o10-summary-item">
          <dt>現在の出店状態</dt>
          <dd>${escapeHtml(workflow.label)}</dd>
        </div>
      `;
    }

    setText("s14-form-meta", tenancy
      ? `最終更新: ${tenancy.updatedAt || tenancy.confirmedAt || "-"}`
      : "当事者間で合意した契約条件を入力して保存してください。");

    // フォーム初期値セット
    const storeNameEl = document.getElementById("s14-store-name");
    const startEl = document.getElementById("s14-start-on");
    const endEl = document.getElementById("s14-end-on");
    const endErrEl = document.getElementById("s14-end-on-error");
    const rentEl = document.getElementById("s14-agreed-rent");
    const confDateEl = document.getElementById("s14-confirmed-date");
    const termEl = document.getElementById("s14-contract-term");
    const notesEl = document.getElementById("s14-notes");
    const saveBtn = document.getElementById("s14-save-btn");

    const defaultStart = tenancy?.contractStartOn || activeApp.requestedStartOn || activeApp.targetOpenDate || todayIso();
    const defaultEnd = tenancy?.contractEndOn || activeApp.requestedEndOn || addYearsIso(defaultStart, activeApp.contractKind === "POPUP" ? 0 : 2);
    const defaultTerm = tenancy?.contractTerm || (activeApp.contractKind === "POPUP" ? "短期ポップアップ利用契約" : "普通借家契約 2年間（更新可）");

    if (storeNameEl) storeNameEl.value = tenancy?.storeName || activeApp.storeName || "";
    if (startEl) startEl.value = defaultStart;
    if (endEl) endEl.value = defaultEnd;
    if (rentEl) rentEl.value = String(tenancy?.agreedRent ?? activeApp.targetRent ?? prop?.rent ?? "");
    if (confDateEl) confDateEl.value = tenancy?.confirmedDate || todayIso();
    if (termEl) termEl.value = defaultTerm;
    if (notesEl) notesEl.value = tenancy?.handoverNotes || "";
    if (saveBtn) saveBtn.textContent = tenancy ? "契約情報を更新する" : "契約情報を記録する";

    const form = document.getElementById("s14-contract-form");
    const validationCtrl = initAccessibleFormValidation(form, { announcerId: "s14-live-announcer" });

    const validateContractDates = () => {
      if (!startEl || !endEl) return true;
      if (startEl.value && endEl.value && endEl.value < startEl.value) {
        const msg = "契約終了日は契約開始日以降の日付を指定してください。";
        endEl.setCustomValidity(msg);
        if (endErrEl) endErrEl.innerHTML = `<span role="img" aria-label="エラー">⚠</span> ${msg}`;
        return false;
      }
      endEl.setCustomValidity("");
      return true;
    };

    const onDateChange = () => {
      const ok = validateContractDates();
      if (endEl) validationCtrl?.markTouched(endEl, !ok);
    };
    if (startEl) { startEl.onchange = onDateChange; startEl.oninput = onDateChange; }
    if (endEl) { endEl.onchange = onDateChange; endEl.oninput = onDateChange; }
    validateContractDates();

    if (form) {
      form.onsubmit = (e) => {
        e.preventDefault();
        const datesOk = validateContractDates();
        if (!datesOk || !form.checkValidity()) {
          const inv = validationCtrl?.focusFirstInvalid();
          if (inv) {
            const m = inv.validationMessage || "入力内容を確認してください";
            validationCtrl?.announce(m);
            window.MachirikuApp?.showToast?.(m);
          }
          return;
        }
        const res = store.saveTenancy({
          id: tenancy?.id,
          applicationId: activeApp.id,
          propId: activeApp.propId,
          storeName: storeNameEl?.value.trim() || activeApp.storeName,
          contractKind: activeApp.contractKind || "LONG",
          contractStartOn: startEl?.value || "",
          contractEndOn: endEl?.value || "",
          agreedRent: Number(String(rentEl?.value || "").replace(/[^\d]/g, "")) || 0,
          confirmedDate: confDateEl?.value || todayIso(),
          contractTerm: termEl?.value.trim() || "",
          handoverNotes: notesEl?.value.trim() || ""
        });
        if (!res.success) {
          window.MachirikuApp?.showToast?.(res.message || "保存に失敗しました");
          return;
        }
        window.MachirikuApp?.showToast?.(tenancy ? "契約情報を更新しました" : "契約情報を記録しました。右側の双方引渡し確認へ進めます");
      };
    }

    // 右ペイン：ステップ3（現地チェックリスト＋双方引渡し確認）
    const gateEl = document.getElementById("s14-handover-gate");
    const checklistBox = document.getElementById("s14-checklist-box");
    const dualConfirmEl = document.getElementById("s14-dual-confirm");
    if (gateEl) gateEl.hidden = workflow.isContractSaved;
    checklistBox?.classList.toggle("is-locked", !workflow.isContractSaved);
    dualConfirmEl?.classList.toggle("is-locked", !workflow.isContractSaved);

    const checklistInputs = Array.from(sectionEl.querySelectorAll('input[name="handoverCheck"]'));
    checklistInputs.forEach((cb) => {
      cb.checked = Array.isArray(tenancy?.checklist) ? tenancy.checklist.includes(cb.value) : false;
      cb.disabled = !tenancy;
      cb.onchange = () => {
        if (!tenancy) return;
        const checkedValues = checklistInputs.filter((el) => el.checked).map((el) => el.value);
        const res = store.updateTenancyChecklist(tenancy.id, checkedValues);
        if (res?.success) {
          window.MachirikuApp?.showToast?.("現地引渡しチェックリストを更新しました");
        }
      };
    });

    const ownerBadge = document.getElementById("s14-owner-confirm-badge");
    const ownerTime = document.getElementById("s14-owner-confirm-time");
    const ownerStatusEl = document.getElementById("s14-owner-confirm-status");
    const ownerBtn = document.getElementById("s14-owner-confirm-btn");
    const bizBadge = document.getElementById("s14-biz-confirm-badge");
    const bizTime = document.getElementById("s14-biz-confirm-time");
    const bizStatusEl = document.getElementById("s14-biz-confirm-status");
    const bizBtn = document.getElementById("s14-biz-confirm-btn");
    const nextBox = document.getElementById("s14-next-step-box");
    const nextPlanLink = document.getElementById("s14-next-plan-link");
    const nextProgLink = document.getElementById("s14-next-progress-link");

    const ownerDone = workflow.ownerDone;
    const bizDone = workflow.bizDone;
    const bothDone = workflow.bothDone;

    if (ownerBadge) {
      ownerBadge.textContent = ownerDone ? "確認済み ✓" : "未確認";
      ownerBadge.className = ownerDone ? "req-badge-ok" : "req-badge-info";
    }
    if (ownerTime) ownerTime.textContent = `確認日時: ${tenancy?.ownerHandoverAt || "—"}`;
    if (ownerBtn) {
      ownerBtn.hidden = !isOwnerActor;
      ownerBtn.disabled = !tenancy || ownerDone;
      ownerBtn.textContent = ownerDone
        ? "オーナー引渡し確認済み"
        : (!tenancy ? "契約情報の保存後に実行できます" : "オーナーとして引渡し完了を記録");
      ownerBtn.onclick = () => {
        if (!tenancy) return;
        openConfirmDialog({
          id: "s14-owner-handover-dialog",
          title: "オーナー側の引渡し完了を記録しますか？",
          subtitle: `${tenancy.propTitle} ／ ${tenancy.storeName}`,
          desc: "鍵・物件の引渡しが完了したことを記録します。事業主側の受領確認も完了すると、開店準備フェーズへ移行します。",
          confirmLabel: "引渡し完了を記録",
          onConfirm: () => {
            const res = store.confirmTenancyHandover(tenancy.id, "owner");
            if (res.success) {
              window.MachirikuApp?.showToast?.(res.bothConfirmed
                ? "双方の引渡し確認が完了しました！出店計画を公開できます"
                : "オーナー側の引渡し確認を記録しました");
            }
          }
        });
      };
    }
    if (ownerStatusEl) {
      ownerStatusEl.hidden = isOwnerActor;
      if (!isOwnerActor) {
        ownerStatusEl.textContent = ownerDone
          ? "オーナー側の引渡し完了が確認されました。"
          : (!tenancy ? "契約情報の保存後、オーナーが引渡し確認を行います。" : "オーナーによる引渡し完了の確認待ちです。");
      }
    }

    if (bizBadge) {
      bizBadge.textContent = bizDone ? "確認済み ✓" : "未確認";
      bizBadge.className = bizDone ? "req-badge-ok" : "req-badge-info";
    }
    if (bizTime) bizTime.textContent = `確認日時: ${tenancy?.businessHandoverAt || "—"}`;
    if (bizBtn) {
      bizBtn.hidden = !isBizActor;
      bizBtn.disabled = !tenancy || bizDone;
      bizBtn.textContent = bizDone
        ? "事業主受領確認済み"
        : (!tenancy ? "契約情報の保存後に実行できます" : "事業主として受領完了を記録");
      bizBtn.onclick = () => {
        if (!tenancy) return;
        openConfirmDialog({
          id: "s14-biz-handover-dialog",
          title: "事業主側の受領完了を記録しますか？",
          subtitle: `${tenancy.propTitle} ／ ${tenancy.storeName}`,
          desc: "物件・鍵を受領したことを記録します。オーナー側の引渡し確認と揃うと、出店計画の作成・公開へ進めます。",
          confirmLabel: "受領完了を記録",
          onConfirm: () => {
            const res = store.confirmTenancyHandover(tenancy.id, "business");
            if (res.success) {
              window.MachirikuApp?.showToast?.(res.bothConfirmed
                ? "双方の引渡し確認が完了しました！出店計画を公開できます"
                : "事業主側の受領確認を記録しました");
            }
          }
        });
      };
    }
    if (bizStatusEl) {
      bizStatusEl.hidden = isBizActor;
      if (!isBizActor) {
        bizStatusEl.textContent = bizDone
          ? "事業主側の受領完了が確認されました。"
          : (!tenancy ? "契約情報の保存後、事業主が受領確認を行います。" : "事業主による受領完了の確認待ちです。");
      }
    }

    if (nextBox) {
      nextBox.hidden = !bothDone;
      if (bothDone) {
        if (nextPlanLink) nextPlanLink.href = matchingUrl("opening-plan.jsp", { propId: activeApp.propId, appId: activeApp.id });
        if (nextProgLink) nextProgLink.href = matchingUrl("progress.jsp", { id: activeApp.propId });
      }
    }
  }

  // =========================================================================
  // S15: matching/opening-plan.jsp (出店計画の作成・公開 / 出店事業主)
  // =========================================================================
  let s15SelectedPropId = "";
  let s15EditingPlanId = "";
  let s15PubFilter = "ALL";
  let s15Photos = [];
  let s15BoundPropForDraft = "";

  const PROGRESS_LABEL_MAP = {
    PREPARING: "開店準備中",
    CONSTRUCTION: "内装・設備工事中",
    DATE_FIXED: "オープン日決定",
    OPENED: "開店・営業中"
  };

  function hydrateS15(params = {}) {
    const sectionEl = document.getElementById("s15-section");
    if (!sectionEl) return;

    showRoleNotice("s15-role-notice", ["business", "admin"], "出店事業主");
    const roleBtn = document.getElementById("s15-role-switch-btn");
    if (roleBtn) roleBtn.onclick = () => store.setRole("business");

    const state = store.getState();
    const tenancies = store.getTenancies();
    const contractedPropIds = new Set(tenancies.filter((t) => t.tenancyStatus !== "CANCELLED").map((t) => t.propId));
    const contractedProps = state.properties.filter((p) =>
      contractedPropIds.has(p.id) || p.status === "preparing" || p.status === "opened" || Boolean(p.storePlan)
    );
    const propList = contractedProps.length ? contractedProps : state.properties;

    const appFromParam = params?.appId ? store.getApplicationById(params.appId) : null;
    const requestedPropId = params?.propId || params?.id || appFromParam?.propId || s15SelectedPropId;
    const activeProp = propList.find((p) => p.id === requestedPropId) || propList[0];
    if (!activeProp) return;
    s15SelectedPropId = activeProp.id;

    const propSelect = document.getElementById("s15-prop-select");
    if (propSelect) {
      propSelect.innerHTML = propList.map((p) => {
        const tag = contractedPropIds.has(p.id) ? "契約済" : (p.storePlan ? "計画公開中" : "デモ対象");
        return `<option value="${escapeHtml(p.id)}" ${p.id === activeProp.id ? "selected" : ""}>[${tag}] ${escapeHtml(p.title)}（${escapeHtml(p.address)}）</option>`;
      }).join("");
      propSelect.onchange = () => {
        s15SelectedPropId = propSelect.value;
        s15EditingPlanId = "";
        s15Photos = [];
        s15BoundPropForDraft = "";
        window.location.href = matchingUrl("opening-plan.jsp", { propId: s15SelectedPropId });
      };
    }

    const linkProgress = document.getElementById("s15-link-progress");
    const linkContract = document.getElementById("s15-link-contract");
    const tenancy = store.getTenancyByPropertyId(activeProp.id);
    if (linkProgress) linkProgress.href = matchingUrl("progress.jsp", { id: activeProp.id });
    if (linkContract) {
      linkContract.href = tenancy?.applicationId
        ? matchingUrl("contract.jsp", { id: tenancy.applicationId })
        : matchingUrl("contract.jsp");
    }

    const hintEl = document.getElementById("s15-contract-hint");
    if (hintEl) {
      if (!contractedProps.length) {
        hintEl.hidden = false;
        hintEl.innerHTML = `<span><strong>プレビュー案内：</strong>まだ引渡し完了済みの物件がないため、全物件から出店計画の作成・公開をお試しいただけます。本番運用では契約引渡し完了後に投稿可能になります。</span>`;
      } else {
        hintEl.hidden = true;
      }
    }

    const profileForm = document.getElementById("s15-profile-form");
    const noticeForm = document.getElementById("s15-plan-form");
    const storeNameEl = document.getElementById("s15-store-name");
    const openDateEl = document.getElementById("s15-open-date");
    const openDateLabel = document.getElementById("s15-open-date-label");
    const genreEl = document.getElementById("s15-genre-display");
    const profileStatusPill = document.getElementById("s15-profile-status-pill");
    const profileSavedMetaEl = document.getElementById("s15-profile-saved-meta");
    const formHeadingEl = document.getElementById("s15-form-heading");
    const noticeLeadEl = document.getElementById("s15-notice-lead");
    const publishBtnEl = document.getElementById("s15-publish-btn");
    const titleEl = document.getElementById("s15-title");
    const bodyEl = document.getElementById("s15-body");
    const savedProfile = activeProp.storeProfile || null;

    if (s15BoundPropForDraft !== activeProp.id) {
      s15BoundPropForDraft = activeProp.id;
      const appForProp = (state.applications || []).find((a) => a.propId === activeProp.id && a.status !== "rejected");
      if (storeNameEl) {
        storeNameEl.value = savedProfile?.storeName || tenancy?.storeName || activeProp.storePlan?.storeName || appForProp?.storeName || activeProp.contractedStoreName || "";
      }
      if (openDateEl) {
        openDateEl.value = savedProfile
          ? (savedProfile.openDate || "")
          : (activeProp.storePlan?.openDate || appForProp?.targetOpenDate || "");
      }
      if (genreEl) {
        genreEl.value = tenancy?.genreName || appForProp?.genreName || activeProp.topDemands?.[0]?.label || "未設定";
      }
      const initProg = savedProfile?.progressStatus || activeProp.storePlan?.progressStatus || (activeProp.status === "opened" ? "OPENED" : "PREPARING");
      const progRadio = profileForm?.querySelector(`input[name="progressStatus"][value="${initProg}"]`);
      if (progRadio) progRadio.checked = true;
    }

    const profileValidationCtrl = initAccessibleFormValidation(profileForm, { announcerId: "s15-profile-announcer" });
    const validationCtrl = initAccessibleFormValidation(noticeForm, { announcerId: "s15-live-announcer" });

    const getSelectedProgress = () => {
      const checked = profileForm?.querySelector('input[name="progressStatus"]:checked');
      return checked ? checked.value : "PREPARING";
    };

    const syncProfileStatusUi = (isDirty = false) => {
      const prog = getSelectedProgress();
      const progLabel = PROGRESS_LABEL_MAP[prog] || "開店準備中";
      if (profileStatusPill) {
        profileStatusPill.textContent = progLabel;
        profileStatusPill.className = `badge-status ${prog === "OPENED" ? "status-opened" : "status-preparing"}`;
      }
      if (profileSavedMetaEl) {
        if (isDirty) {
          profileSavedMetaEl.textContent = "未保存の変更があります（「基本情報・進捗を保存する」で反映されます）";
          profileSavedMetaEl.classList.add("is-dirty");
        } else if (activeProp.storeProfile?.updatedAt) {
          profileSavedMetaEl.textContent = `最終保存: ${activeProp.storeProfile.updatedAt}`;
          profileSavedMetaEl.classList.remove("is-dirty");
        } else if (activeProp.storePlan?.storeName) {
          profileSavedMetaEl.textContent = "公開中の店舗情報を表示中";
          profileSavedMetaEl.classList.remove("is-dirty");
        } else {
          profileSavedMetaEl.textContent = "基本情報のみでも保存して公開ページへ反映できます";
          profileSavedMetaEl.classList.remove("is-dirty");
        }
      }
    };

    const syncOpenDateRequirement = () => {
      const isOpened = getSelectedProgress() === "OPENED";
      if (openDateEl) {
        openDateEl.required = isOpened;
        if (!isOpened) {
          openDateEl.setCustomValidity("");
          profileValidationCtrl?.resetField(openDateEl);
        }
      }
      if (openDateLabel) {
        openDateLabel.innerHTML = isOpened
          ? `実開店日 <span class="req-mark">*必須</span>`
          : `開店予定日 <span class="opt-mark">（任意・開店時は必須）</span>`;
      }
    };

    const renderLivePreview = () => {
      const previewEl = document.getElementById("s15-live-preview");
      if (!previewEl) return;
      const prog = getSelectedProgress();
      const progLabel = PROGRESS_LABEL_MAP[prog] || "開店準備中";
      const storeName = storeNameEl?.value.trim() || "（店舗名がここに入ります）";
      const openDate = openDateEl?.value.trim();
      const rawTitle = titleEl?.value.trim() || "";
      const rawBody = bodyEl?.value.trim() || "";
      const hasNoticeDraft = Boolean(rawTitle || rawBody || s15Photos.length);

      previewEl.innerHTML = `
        <div class="matching-preview-header">
          <span class="badge-status ${prog === "OPENED" ? "status-opened" : "status-preparing"}">${escapeHtml(progLabel)}</span>
          <span class="apply-summary-pill">${openDate ? (prog === "OPENED" ? `開店日: ${escapeHtml(openDate)}` : `OPEN予定: ${escapeHtml(openDate)}`) : "開店日: 調整中"}</span>
        </div>
        <div class="matching-preview-prop">${escapeHtml(activeProp.title)}（${escapeHtml(activeProp.address)}）</div>
        <h3 class="matching-preview-store">${escapeHtml(storeName)}</h3>
        <div class="matching-preview-post">
          ${hasNoticeDraft ? `
            <h4>${escapeHtml(rawTitle || "（お知らせ見出しがここに入ります）")}</h4>
            <p>${escapeHtml(rawBody || "住民へのメッセージや準備状況がここに表示されます。")}</p>
            ${s15Photos.length ? `
              <div class="matching-preview-photos">
                ${s15Photos.map((ph) => `<img src="${escapeHtml(resolveAssetUrl(ph.url))}" alt="${escapeHtml(ph.label || "準備写真")}">`).join("")}
              </div>
            ` : ""}
          ` : `
            <p class="s15-preview-placeholder">お知らせ見出し・本文を入力すると、タイムライン投稿のプレビューが表示されます（店舗名・進捗・開店予定日のみの保存も可能です）。</p>
          `}
        </div>
      `;
    };

    const renderS15PhotoGrid = () => {
      setText("s15-photo-count", `${s15Photos.length} / 3 枚`);
      const grid = document.getElementById("s15-photo-preview");
      if (!grid) return;
      if (!s15Photos.length) {
        grid.innerHTML = "";
        renderLivePreview();
        return;
      }
      grid.innerHTML = s15Photos.map((ph, idx) => `
        <div class="confirm-photo-item">
          <img src="${escapeHtml(resolveAssetUrl(ph.url))}" alt="${escapeHtml(ph.label || `準備写真 ${idx + 1}`)}">
          <div class="confirm-photo-label">${escapeHtml(ph.label || `準備写真 ${idx + 1}`)}</div>
          <button type="button" class="btn-secondary btn-xs btn-danger-outline" data-s15-remove-photo="${idx}">削除</button>
        </div>
      `).join("");
      grid.onclick = (e) => {
        const rm = e.target.closest("[data-s15-remove-photo]");
        if (!rm) return;
        const idx = Number(rm.getAttribute("data-s15-remove-photo"));
        if (!isNaN(idx)) {
          s15Photos.splice(idx, 1);
          renderS15PhotoGrid();
        }
      };
      renderLivePreview();
    };

    const photoInput = document.getElementById("s15-photo-input");
    if (photoInput) {
      photoInput.onchange = async () => {
        const files = Array.from(photoInput.files || []);
        photoInput.value = "";
        for (const file of files) {
          if (s15Photos.length >= 3) {
            window.MachirikuApp?.showToast?.("写真は最大3枚まで添付できます");
            break;
          }
          const url = await readOptimizedImageDataUrl(file);
          if (url) {
            s15Photos.push({ url, label: file.name.replace(/\.[^.]+$/, "") });
          }
        }
        renderS15PhotoGrid();
      };
    }

    profileForm?.querySelectorAll('input[name="progressStatus"]').forEach((r) => {
      r.onchange = () => {
        syncOpenDateRequirement();
        syncProfileStatusUi(true);
        renderLivePreview();
      };
    });
    [storeNameEl, openDateEl].forEach((el) => {
      if (el) {
        el.oninput = () => {
          syncProfileStatusUi(true);
          renderLivePreview();
        };
        el.onchange = () => {
          syncProfileStatusUi(true);
          renderLivePreview();
        };
      }
    });
    [titleEl, bodyEl].forEach((el) => {
      if (el) {
        el.oninput = renderLivePreview;
        el.onchange = renderLivePreview;
      }
    });

    syncOpenDateRequirement();
    syncProfileStatusUi(false);
    renderS15PhotoGrid();

    if (profileForm) {
      profileForm.onsubmit = (e) => {
        e.preventDefault();
        syncOpenDateRequirement();
        if (!profileForm.checkValidity()) {
          const inv = profileValidationCtrl?.focusFirstInvalid();
          if (inv) {
            const m = inv.validationMessage || "基本情報の入力内容を確認してください";
            profileValidationCtrl?.announce(m);
            window.MachirikuApp?.showToast?.(m);
          }
          return;
        }
        const res = store.saveOpeningProfile({
          propId: activeProp.id,
          storeName: storeNameEl?.value.trim() || "",
          progressStatus: getSelectedProgress(),
          plannedOpenOn: openDateEl?.value.trim() || ""
        });
        if (!res.success) {
          window.MachirikuApp?.showToast?.(res.message || "基本情報の保存に失敗しました");
          return;
        }
        profileValidationCtrl?.resetAll();
        syncProfileStatusUi(false);
        window.MachirikuApp?.showToast?.("基本情報・現在の進捗を保存しました（公開ページへ反映されました）");
      };
    }

    const resetS15Editor = () => {
      s15EditingPlanId = "";
      s15Photos = [];
      if (noticeForm) noticeForm.reset();
      const idEl = document.getElementById("s15-plan-id");
      if (idEl) idEl.value = "";
      const banner = document.getElementById("s15-edit-banner");
      if (banner) banner.hidden = true;
      if (formHeadingEl) formHeadingEl.textContent = "2. 地域住民へのお知らせを新規投稿する";
      if (noticeLeadEl) noticeLeadEl.hidden = false;
      if (publishBtnEl) publishBtnEl.textContent = "住民に向けて公開する";
      if (titleEl) titleEl.value = "";
      if (bodyEl) bodyEl.value = "";
      validationCtrl?.resetAll();
      renderS15PhotoGrid();
    };

    const syncEditNoticeModeUi = (plan) => {
      const banner = document.getElementById("s15-edit-banner");
      if (plan) {
        if (formHeadingEl) formHeadingEl.textContent = "2. 選択中のお知らせを編集する";
        if (noticeLeadEl) noticeLeadEl.hidden = true;
        if (banner) banner.hidden = false;
        setText("s15-edit-banner-text", `「${plan.title}」を編集中です（上の基本情報・進捗は変更されません）`);
        if (publishBtnEl) publishBtnEl.textContent = "変更内容を公開する";
      } else {
        if (formHeadingEl) formHeadingEl.textContent = "2. 地域住民へのお知らせを新規投稿する";
        if (noticeLeadEl) noticeLeadEl.hidden = false;
        if (banner) banner.hidden = true;
        if (publishBtnEl) publishBtnEl.textContent = "住民に向けて公開する";
      }
    };

    if (s15EditingPlanId) {
      const activeEditPlan = store.getOpeningPlanById(s15EditingPlanId);
      if (activeEditPlan && activeEditPlan.propId === activeProp.id) {
        syncEditNoticeModeUi(activeEditPlan);
      } else {
        s15EditingPlanId = "";
      }
    }

    const cancelEditBtn = document.getElementById("s15-cancel-edit-btn");
    if (cancelEditBtn) cancelEditBtn.onclick = resetS15Editor;

    const collectPlanPayload = (publicationStatus) => {
      if (s15EditingPlanId) {
        return {
          id: s15EditingPlanId,
          propId: activeProp.id,
          title: titleEl?.value.trim() || "",
          body: bodyEl?.value.trim() || "",
          photos: [...s15Photos],
          publicationStatus
        };
      }
      const savedProg = activeProp.storeProfile?.progressStatus || activeProp.storePlan?.progressStatus || "";
      const fallbackProg = getSelectedProgress();
      const snapshotOpenDate = activeProp.storeProfile
        ? (activeProp.storeProfile.openDate || "")
        : (activeProp.storePlan?.openDate || openDateEl?.value.trim() || "");
      const snapshotProg = savedProg || ((fallbackProg === "OPENED" && !snapshotOpenDate) ? "PREPARING" : fallbackProg);
      const snapshotStoreName = activeProp.storeProfile?.storeName || storeNameEl?.value.trim() || tenancy?.storeName || activeProp.contractedStoreName || activeProp.title;
      return {
        propId: activeProp.id,
        storeName: snapshotStoreName,
        progressStatus: snapshotProg,
        plannedOpenOn: snapshotOpenDate,
        title: titleEl?.value.trim() || "",
        body: bodyEl?.value.trim() || "",
        photos: [...s15Photos],
        publicationStatus
      };
    };

    const draftBtn = document.getElementById("s15-draft-btn");
    if (draftBtn) {
      draftBtn.onclick = () => {
        if (!titleEl?.value.trim()) {
          validationCtrl?.markTouched(titleEl, true);
          window.MachirikuApp?.showToast?.("下書き保存にはお知らせ見出しを入力してください");
          titleEl?.focus();
          return;
        }
        const res = store.saveOpeningPlan(collectPlanPayload("DRAFT"));
        if (res.success) {
          resetS15Editor();
          window.MachirikuApp?.showToast?.("お知らせを下書き保存しました（住民画面には非公開です）");
        } else {
          window.MachirikuApp?.showToast?.(res.message || "下書き保存に失敗しました");
        }
      };
    }

    if (noticeForm) {
      noticeForm.onsubmit = (e) => {
        e.preventDefault();
        if (!noticeForm.checkValidity()) {
          const inv = validationCtrl?.focusFirstInvalid();
          if (inv) {
            const m = inv.validationMessage || "お知らせの入力内容を確認してください";
            validationCtrl?.announce(m);
            window.MachirikuApp?.showToast?.(m);
          }
          return;
        }
        const payload = collectPlanPayload("PUBLISHED");
        const isEditing = Boolean(payload.id);
        openConfirmDialog({
          id: "s15-publish-dialog",
          title: isEditing ? "このお知らせの変更内容を公開しますか？" : "このお知らせを住民に公開しますか？",
          subtitle: payload.storeName ? `${payload.storeName} ／ ${payload.title}` : payload.title,
          desc: isEditing
            ? "記事の本文・写真などの更新内容を「開店のようす」ページへ反映します（店舗の基本情報・進捗は変更されません）。"
            : "公開すると「開店のようす」ページに反映され、物件をフォローしている地域住民へお知らせが届きます。",
          confirmLabel: isEditing ? "変更を公開する" : "公開する",
          onConfirm: () => {
            const res = store.saveOpeningPlan(payload);
            if (!res.success) {
              window.MachirikuApp?.showToast?.(res.message || "公開に失敗しました");
              return;
            }
            resetS15Editor();
            window.MachirikuApp?.showToast?.(isEditing ? "お知らせの変更を公開しました！" : "お知らせを公開しました！");
          }
        });
      };
    }

    // 下部履歴タブ＆一覧
    const pubTabsEl = document.getElementById("s15-pub-tabs");
    const applyPubTab = (btn) => {
      if (!btn) return;
      s15PubFilter = btn.getAttribute("data-pub-status") || "ALL";
      pubTabsEl?.querySelectorAll('[role="tab"]').forEach((t) => {
        t.setAttribute("aria-selected", String(t === btn));
      });
      renderS15Posts();
    };
    if (pubTabsEl) {
      pubTabsEl.querySelectorAll('[role="tab"]').forEach((btn) => {
        const st = btn.getAttribute("data-pub-status");
        btn.setAttribute("aria-selected", String(st === s15PubFilter));
        btn.onclick = () => applyPubTab(btn);
      });
      wireTablistKeyboard(pubTabsEl, applyPubTab);
    }

    const renderS15Posts = () => {
      const posts = store.getOpeningPlans(activeProp.id);
      const filtered = posts.filter((p) => s15PubFilter === "ALL" || p.publicationStatus === s15PubFilter);
      setText("s15-posts-count", String(filtered.length));
      const emptyEl = document.getElementById("s15-posts-empty");
      const listEl = document.getElementById("s15-posts-list");
      if (!listEl) return;

      if (!filtered.length) {
        if (emptyEl) emptyEl.hidden = false;
        listEl.innerHTML = "";
        return;
      }
      if (emptyEl) emptyEl.hidden = true;

      listEl.innerHTML = filtered.map((p) => `
        <article class="detail-card apply-item-card">
          <div class="apply-item-head">
            <div class="apply-item-main">
              <div class="apply-item-meta">
                <span class="badge-status ${escapeHtml(getPubBadgeClass(p.publicationStatus))}">${escapeHtml(p.publicationLabel || p.publicationStatus)}</span>
                <span class="qual-badge">${escapeHtml(p.progressLabel || p.progressStatus)}</span>
                ${p.plannedOpenOn ? `<span class="apply-summary-pill">開店予定: ${escapeHtml(p.plannedOpenOn)}</span>` : ""}
                <span class="apply-item-date">${escapeHtml(p.publishedAt ? `公開: ${p.publishedAt}` : `更新: ${p.updatedAt || p.createdAt}`)}</span>
              </div>
              <h3 class="apply-item-title">${escapeHtml(p.title)}</h3>
              <p class="matching-offer-excerpt">${escapeHtml(p.body || "")}</p>
            </div>
            <div class="matching-card-actions">
              <button type="button" class="btn-secondary btn-sm" data-s15-edit="${escapeHtml(p.id)}">編集</button>
              ${p.publicationStatus !== "PUBLISHED"
                ? `<button type="button" class="btn-primary btn-sm" data-s15-pub="${escapeHtml(p.id)}" data-next="PUBLISHED">公開する</button>`
                : `<button type="button" class="btn-secondary btn-sm" data-s15-pub="${escapeHtml(p.id)}" data-next="ARCHIVED">アーカイブ</button>`}
            </div>
          </div>
        </article>
      `).join("");

      listEl.onclick = (e) => {
        const editBtn = e.target.closest("[data-s15-edit]");
        if (editBtn) {
          const plan = store.getOpeningPlanById(editBtn.getAttribute("data-s15-edit"));
          if (!plan) return;
          s15EditingPlanId = plan.id;
          const idEl = document.getElementById("s15-plan-id");
          if (idEl) idEl.value = plan.id;
          if (titleEl) titleEl.value = plan.title || "";
          if (bodyEl) bodyEl.value = plan.body || "";
          s15Photos = Array.isArray(plan.photos) ? plan.photos.map((ph) => ({ ...ph })) : [];
          validationCtrl?.resetAll();
          syncEditNoticeModeUi(plan);
          renderS15PhotoGrid();
          document.getElementById("s15-form-heading")?.scrollIntoView({ behavior: "smooth", block: "start" });
          titleEl?.focus({ preventScroll: true });
          return;
        }
        const pubBtn = e.target.closest("[data-s15-pub]");
        if (pubBtn) {
          const planId = pubBtn.getAttribute("data-s15-pub");
          const next = pubBtn.getAttribute("data-next");
          const plan = store.getOpeningPlanById(planId);
          if (!plan) return;
          if (next === "PUBLISHED" && plan.progressStatus === "OPENED" && !plan.plannedOpenOn) {
            window.MachirikuApp?.showToast?.("「開店・営業中」として公開するには編集から実開店日を入力してください");
            return;
          }
          store.updateOpeningPlanPublication(planId, next);
          window.MachirikuApp?.showToast?.(next === "PUBLISHED" ? "お知らせを公開しました" : "お知らせをアーカイブしました");
        }
      };
    };

    renderS15Posts();
  }

  // =========================================================================
  // S05: matching/progress.jsp (開店のようす・出店進捗閲覧 / 全員)
  // =========================================================================
  let s05SelectedPropId = "";

  function hydrateS05(params = {}) {
    const sectionEl = document.getElementById("s05-section");
    if (!sectionEl) return;

    const state = store.getState();
    const allPublishedPlans = store.getOpeningPlans().filter((p) => p.publicationStatus === "PUBLISHED");
    const publishedPropIds = new Set(allPublishedPlans.map((p) => p.propId));
    const activeProps = state.properties.filter((p) =>
      publishedPropIds.has(p.id) ||
      Boolean(p.storePlan) ||
      p.status === "preparing" ||
      p.status === "opened" ||
      p.status === "agreed"
    );

    const requestedProp = params?.id ? store.getPropertyById(params.id) : null;
    const targetProp = requestedProp && (publishedPropIds.has(requestedProp.id) || requestedProp.storePlan || ["preparing", "opened", "agreed"].includes(requestedProp.status))
      ? requestedProp
      : (activeProps.find((p) => p.id === s05SelectedPropId) || activeProps[0] || null);

    const emptyEl = document.getElementById("s05-empty-state");
    const contentWrap = document.getElementById("s05-content-wrap");

    if (!targetProp) {
      if (emptyEl) {
        emptyEl.hidden = false;
        if (requestedProp) {
          setText("s05-empty-title", `「${requestedProp.title}」の開店お知らせはまだありません`);
          setText("s05-empty-desc", "出店者が決まり、出店計画が公開されるとここに工事や開店の進捗が表示されます。");
        }
      }
      if (contentWrap) contentWrap.hidden = true;
      return;
    }

    if (emptyEl) emptyEl.hidden = true;
    if (contentWrap) contentWrap.hidden = false;
    s05SelectedPropId = targetProp.id;

    // プロジェクト切替バー
    const switcherBar = document.getElementById("s05-switcher-bar");
    const propSelect = document.getElementById("s05-prop-select");
    setText("s05-project-count", String(activeProps.length));
    if (switcherBar && propSelect) {
      switcherBar.hidden = activeProps.length <= 1;
      propSelect.innerHTML = activeProps.map((p) => {
        const sName = p.storePlan?.storeName || p.contractedStoreName || p.title;
        return `<option value="${escapeHtml(p.id)}" ${p.id === targetProp.id ? "selected" : ""}>${escapeHtml(sName)}（${escapeHtml(p.title)}）</option>`;
      }).join("");
      propSelect.onchange = () => {
        s05SelectedPropId = propSelect.value;
        window.location.href = matchingUrl("progress.jsp", { id: s05SelectedPropId });
      };
    }

    // 公開中お知らせ（下書き・アーカイブは除外！）
    const publishedUpdates = store.getOpeningPlans(targetProp.id).filter((p) => p.publicationStatus === "PUBLISHED");
    const latestUpdate = publishedUpdates[0] || null;
    const tenancy = store.getTenancyByPropertyId(targetProp.id);
    const app = (state.applications || []).find((a) => a.propId === targetProp.id && a.status !== "rejected" && a.status !== "withdrawn");

    const storeName = targetProp.storeProfile?.storeName || targetProp.storePlan?.storeName || latestUpdate?.storeName || tenancy?.storeName || targetProp.contractedStoreName || app?.storeName || "出店計画の公開をお待ちください";
    const openDate = targetProp.storeProfile
      ? (targetProp.storeProfile.openDate || "")
      : (targetProp.storePlan?.openDate || latestUpdate?.plannedOpenOn || tenancy?.openedOn || app?.targetOpenDate || "");
    const progressCode = targetProp.storeProfile?.progressStatus || targetProp.storePlan?.progressStatus || latestUpdate?.progressStatus || (targetProp.status === "opened" ? "OPENED" : "PREPARING");
    const progressLabel = PROGRESS_LABEL_MAP[progressCode] || targetProp.statusLabel || "開店準備中";
    const genreName = tenancy?.genreName || app?.genreName || targetProp.topDemands?.[0]?.label || "";

    const heroImg = document.getElementById("s05-hero-img");
    if (heroImg) {
      heroImg.src = resolveAssetUrl(targetProp.coverImage || PLACEHOLDER_IMG);
      heroImg.alt = `${storeName}（${targetProp.title}）`;
      heroImg.onerror = function () {
        if (window.MachirikuScreens?.handlePropertyImageError) {
          window.MachirikuScreens.handlePropertyImageError(this);
        }
      };
    }

    const statusPill = document.getElementById("s05-status-pill");
    if (statusPill) {
      statusPill.textContent = progressLabel;
      statusPill.className = `badge-status ${progressCode === "OPENED" ? "status-opened" : "status-preparing"}`;
    }
    const genrePill = document.getElementById("s05-genre-pill");
    if (genrePill) {
      genrePill.hidden = !genreName;
      genrePill.textContent = genreName;
    }
    setText("s05-open-date-pill", openDate
      ? (progressCode === "OPENED" ? `開店日: ${openDate}` : `OPEN予定: ${openDate}`)
      : "オープン予定日: 調整中");
    setText("s05-store-title", storeName);
    setText("s05-prop-meta", `${targetProp.title} ／ ${targetProp.address}（${targetProp.access || ""}）`);

    // 5段階ステップバー
    const stepsEl = document.getElementById("s05-opening-steps");
    if (stepsEl) {
      const stages = ["希望を集める", "出店者を選ぶ", "契約・引き渡し", "開店の準備", "開店"];
      let stageIdx = 3;
      if (progressCode === "OPENED" || targetProp.status === "opened") stageIdx = 4;
      else if (targetProp.status === "agreed" && !latestUpdate) stageIdx = 2;
      else if (targetProp.status === "reviewing") stageIdx = 1;
      else if (targetProp.status === "voting" || targetProp.status === "listed") stageIdx = 0;

      stepsEl.innerHTML = stages.map((label, idx) => {
        const cls = idx < stageIdx ? "is-complete" : (idx === stageIdx ? "is-current" : "");
        return `<li class="${cls}"${idx === stageIdx ? ' aria-current="step"' : ""}><span class="step-number">${idx + 1}</span><span>${escapeHtml(label)}${idx === stageIdx ? "<small>現在</small>" : ""}</span></li>`;
      }).join("");
    }

    // 公開お知らせタイムライン
    const timelineEl = document.getElementById("s05-timeline-list");
    setText("s05-updates-count", `${publishedUpdates.length}件`);
    if (timelineEl) {
      if (publishedUpdates.length) {
        timelineEl.innerHTML = publishedUpdates.map((u, uIdx) => `
          <article class="s05-update-card">
            <div class="s05-update-head">
              <span class="qual-badge">${escapeHtml(u.progressLabel || u.progressStatus)}</span>
              <time class="apply-item-date">${escapeHtml(u.publishedAt || u.updatedAt || "")}</time>
            </div>
            <h3 class="s05-update-title">${escapeHtml(u.title)}</h3>
            <p class="story-content-body">${escapeHtml(u.body || "")}</p>
            ${Array.isArray(u.photos) && u.photos.length ? `
              <div class="s05-update-photos" data-update-idx="${uIdx}">
                ${u.photos.map((ph, pIdx) => `
                  <button type="button" class="confirm-photo-item confirm-photo-btn" data-s05-photo="${pIdx}" aria-label="${escapeHtml(ph.label || "準備写真")}を拡大表示">
                    <img src="${escapeHtml(resolveAssetUrl(ph.url))}" alt="">
                    <span class="confirm-photo-label">${escapeHtml(ph.label || `写真 ${pIdx + 1}`)}</span>
                  </button>
                `).join("")}
              </div>
            ` : ""}
          </article>
        `).join("");

        timelineEl.onclick = (e) => {
          const btn = e.target.closest("[data-s05-photo]");
          if (!btn) return;
          const wrap = btn.closest("[data-update-idx]");
          const uIdx = Number(wrap?.getAttribute("data-update-idx") || 0);
          const pIdx = Number(btn.getAttribute("data-s05-photo") || 0);
          const photos = publishedUpdates[uIdx]?.photos || [];
          openPhotoLightbox(photos, pIdx);
        };
      } else if (targetProp.storePlan && (targetProp.storePlan.title || targetProp.storePlan.message || targetProp.storePlan.concept)) {
        timelineEl.innerHTML = `
          <article class="s05-update-card">
            <div class="s05-update-head">
              <span class="qual-badge">${escapeHtml(progressLabel)}</span>
            </div>
            <h3 class="s05-update-title">${escapeHtml(targetProp.storePlan.title || `${storeName} 開店準備のお知らせ`)}</h3>
            <p class="story-content-body">${escapeHtml(targetProp.storePlan.message || targetProp.storePlan.concept || "")}</p>
          </article>
        `;
      } else if (targetProp.storeProfile || targetProp.storePlan) {
        timelineEl.innerHTML = `<p class="sample-note">店舗の基本情報・現在の進捗は公開されています。店主からの詳しいお知らせ記事の投稿をお待ちください。</p>`;
      } else {
        timelineEl.innerHTML = `<p class="sample-note">出店が決定しました。店舗名や工事進捗の詳しいお知らせは、事業主からの公開をお待ちください。</p>`;
      }
    }

    // 地域の声
    const voicesEl = document.getElementById("s05-voices-list");
    if (voicesEl) {
      const voices = targetProp.residentVoices || [];
      voicesEl.innerHTML = voices.length
        ? voices.map((v) => `<div class="quote-bubble"><p>${escapeHtml(v.text)}</p><p class="quote-meta">${escapeHtml(v.author)}${v.date ? ` ・ ${escapeHtml(v.date)}` : ""}</p></div>`).join("")
        : `<p class="sample-note">まだ地域の声は寄せられていません。</p>`;
    }

    // サイドペイン：フォローボタン＆スペック＆リンク
    const followed = Boolean(state.currentUser?.followedProps?.includes(targetProp.id));
    const followBtn = document.getElementById("s05-follow-btn");
    if (followBtn) {
      followBtn.setAttribute("aria-pressed", String(followed));
      followBtn.textContent = followed ? "フォロー中（解除する）" : "この物件をフォローする";
      followBtn.onclick = () => {
        store.toggleFollowProperty(targetProp.id);
        const nowFollowed = Boolean(store.getState().currentUser?.followedProps?.includes(targetProp.id));
        window.MachirikuApp?.showToast?.(nowFollowed ? "物件をフォローしました。開店進捗をお知らせします" : "フォローを解除しました");
      };
    }

    const sideSpecs = document.getElementById("s05-side-specs");
    if (sideSpecs) {
      sideSpecs.innerHTML = `
        <div><dt>物件名</dt><dd>${escapeHtml(targetProp.title)}</dd></div>
        <div><dt>所在地</dt><dd>${escapeHtml(targetProp.address)}</dd></div>
        <div><dt>アクセス</dt><dd>${escapeHtml(targetProp.access || "—")}</dd></div>
        <div><dt>開店予定</dt><dd>${escapeHtml(openDate || "調整中")}</dd></div>
        <div><dt>投票参加</dt><dd>${Number(targetProp.votesCount || 0)} 名の住民が投票</dd></div>
      `;
    }

    const coupons = (state.coupons || []).filter((c) => c.propId === targetProp.id);
    const couponLink = document.getElementById("s05-coupon-link");
    if (couponLink) couponLink.hidden = !coupons.length;

    const detailLink = document.getElementById("s05-detail-link");
    if (detailLink) detailLink.href = propUrl("detail.jsp", { id: targetProp.id });

    const editPlanLink = document.getElementById("s05-plan-edit-link");
    if (editPlanLink) {
      const canEdit = state.currentRole === "business" || state.currentRole === "admin";
      editPlanLink.hidden = !canEdit;
      editPlanLink.href = matchingUrl("opening-plan.jsp", { propId: targetProp.id });
    }
  }

  // =========================================================================
  // B02: matching/applications.jsp (事業主側の応募状況確認・受信オファー)
  // =========================================================================
  let b02ActiveTab = "apps";
  let b02StatusFilter = "ALL";
  let b02ContractFilter = "ALL";
  let b02Keyword = "";
  let b02FiltersOpen = false;
  const b02ExpandedIds = new Set();
  let b02PendingFocusInputId = null;
  const B02_READ_STORAGE_KEY = "machiriku_b02_read_counts";

  function loadB02ReadMap() {
    try {
      const raw = window.sessionStorage?.getItem(B02_READ_STORAGE_KEY);
      return raw ? JSON.parse(raw) : {};
    } catch (_) {
      return {};
    }
  }

  function markB02AppRead(appId, count) {
    try {
      const map = loadB02ReadMap();
      map[appId] = Number(count) || 0;
      window.sessionStorage?.setItem(B02_READ_STORAGE_KEY, JSON.stringify(map));
    } catch (_) {}
  }

  function getAppChatMessages(app) {
    if (Array.isArray(app?.chatMessages) && app.chatMessages.length) {
      return app.chatMessages;
    }
    if (app?.ownerReply && String(app.ownerReply).trim()) {
      return [{
        id: "msg-legacy",
        senderRole: "owner",
        senderName: "物件オーナー",
        text: String(app.ownerReply).trim(),
        sentAt: app.repliedAt || ""
      }];
    }
    return [];
  }

  function getB02ChatPlaceholder(app, isHandedOver) {
    if (isHandedOver) {
      return "引渡し・開店準備に関する連絡や確認事項を入力...";
    }
    if (app?.status === "approved") {
      return "契約手続きや引渡し日程に関する確認事項を入力...";
    }
    if (app?.status === "rejected" || app?.status === "withdrawn") {
      return "オーナーへの確認事項を入力...";
    }
    return "内覧日程の調整やオーナーへの確認事項を入力...";
  }

  function getB02ChatEmptyHint(app, isHandedOver) {
    if (isHandedOver) {
      return "引渡し後の確認事項や開店準備の連絡を送信できます。";
    }
    if (app?.status === "approved") {
      return "契約手続きや引渡し日程に関する連絡事項を送信できます。";
    }
    return "オーナーからの返信が届くとここに表示されます。内覧希望の補足や質問も送信できます。";
  }

  function scrollB02ThreadToBottom(container) {
    if (!container) return;
    const thread = container.classList?.contains("o10-chat-thread")
      ? container
      : container.querySelector(".o10-chat-thread");
    if (!thread) return;
    thread.scrollTop = thread.scrollHeight;
  }

  function hydrateB02(params = {}) {
    const sectionEl = document.getElementById("b02-section");
    if (!sectionEl) return;

    if (params?.tab === "offers") b02ActiveTab = "offers";

    showRoleNotice("b02-role-notice", ["business", "admin"], "出店事業主");
    const roleBtn = document.getElementById("b02-role-switch-btn");
    if (roleBtn) roleBtn.onclick = () => store.setRole("business");

    const state = store.getState();
    const myBizId = state.users?.business?.id || "usr-biz-01";
    const allApps = state.applications || [];
    const myApps = allApps.filter((a) => !a.applicantId || a.applicantId === myBizId || a.applicantId === state.currentUser?.id);
    const receivedOffers = store.getOffers().filter((o) =>
      o.offerStatus !== "DRAFT" && (o.businessUserId === myBizId || state.currentRole === "admin" || !o.businessUserId)
    );
    const allVisibleOffers = receivedOffers.length
      ? receivedOffers
      : store.getOffers().filter((o) => o.offerStatus !== "DRAFT");

    // KPI（応募タブ内の3枚）
    const consultingCount = myApps.filter((a) => a.status === "consulting" || a.status === "interviewing").length;
    const agreedCount = myApps.filter((a) => a.status === "approved" || a.status === "contracted").length;
    setText("b02-kpi-total", String(myApps.length));
    setText("b02-kpi-consulting", String(consultingCount));
    setText("b02-kpi-agreed", String(agreedCount));
    setText("b02-tab-apps-count", String(myApps.length));
    setText("b02-tab-offers-count", String(allVisibleOffers.length));

    // タブ・KPIクイック絞り込み同期
    const tabsWrap = document.getElementById("b02-view-tabs");
    const panelApps = document.getElementById("b02-panel-apps");
    const panelOffers = document.getElementById("b02-panel-offers");
    const statusSel = document.getElementById("b02-status-filter");
    const contractSel = document.getElementById("b02-contract-filter");
    const kwInput = document.getElementById("b02-keyword-filter");
    const filterToggleBtn = document.getElementById("b02-filter-toggle");
    const filtersCollapse = document.getElementById("b02-filters-collapse");
    const filterActiveBadge = document.getElementById("b02-filter-active-count");
    const kpiBtns = sectionEl.querySelectorAll("[data-b02-kpi]");

    const syncFilterToggleUi = () => {
      const activeCount = (b02StatusFilter !== "ALL" ? 1 : 0)
        + (b02ContractFilter !== "ALL" ? 1 : 0)
        + (b02Keyword.trim() ? 1 : 0);
      if (filterToggleBtn) {
        filterToggleBtn.setAttribute("aria-expanded", String(b02FiltersOpen));
        filterToggleBtn.classList.toggle("is-open", b02FiltersOpen);
        filterToggleBtn.classList.toggle("has-active", activeCount > 0);
      }
      if (filtersCollapse) {
        filtersCollapse.classList.toggle("is-open", b02FiltersOpen);
      }
      if (filterActiveBadge) {
        filterActiveBadge.hidden = activeCount === 0;
        filterActiveBadge.textContent = String(activeCount);
      }
    };

    if (filterToggleBtn) {
      filterToggleBtn.onclick = () => {
        b02FiltersOpen = !b02FiltersOpen;
        syncFilterToggleUi();
      };
    }

    const syncKpiPressed = () => {
      let activeKpi = "";
      if (b02StatusFilter === "ALL") activeKpi = "all";
      else if (b02StatusFilter === "consulting") activeKpi = "consulting";
      else if (b02StatusFilter === "agreed" || b02StatusFilter === "approved" || b02StatusFilter === "contracted") activeKpi = "agreed";

      kpiBtns.forEach((btn) => {
        btn.setAttribute("aria-pressed", String(btn.getAttribute("data-b02-kpi") === activeKpi));
      });
    };

    const activateB02Tab = (tabName) => {
      b02ActiveTab = tabName === "offers" ? "offers" : "apps";
      tabsWrap?.querySelectorAll('[role="tab"]').forEach((t) => {
        const isSel = t.getAttribute("data-b02-tab") === b02ActiveTab;
        t.setAttribute("aria-selected", String(isSel));
      });
      if (panelApps) panelApps.hidden = b02ActiveTab !== "apps";
      if (panelOffers) panelOffers.hidden = b02ActiveTab !== "offers";
      syncKpiPressed();
    };

    if (tabsWrap) {
      tabsWrap.querySelectorAll('[role="tab"]').forEach((t) => {
        t.onclick = () => activateB02Tab(t.getAttribute("data-b02-tab"));
      });
      wireTablistKeyboard(tabsWrap, (t) => activateB02Tab(t.getAttribute("data-b02-tab")));
    }

    // 応募フィルタ
    if (statusSel) {
      statusSel.value = b02StatusFilter;
      statusSel.onchange = () => {
        b02StatusFilter = statusSel.value;
        syncKpiPressed();
        syncFilterToggleUi();
        renderB02Apps();
      };
    }
    if (contractSel) {
      contractSel.value = b02ContractFilter;
      contractSel.onchange = () => {
        b02ContractFilter = contractSel.value;
        syncFilterToggleUi();
        renderB02Apps();
      };
    }
    if (kwInput) {
      if (document.activeElement !== kwInput) kwInput.value = b02Keyword;
      kwInput.oninput = () => {
        b02Keyword = kwInput.value;
        syncFilterToggleUi();
        renderB02Apps();
      };
    }

    kpiBtns.forEach((btn) => {
      btn.onclick = () => {
        const target = btn.getAttribute("data-b02-kpi");
        b02StatusFilter = target === "consulting" ? "consulting" : (target === "agreed" ? "agreed" : "ALL");
        if (statusSel) statusSel.value = b02StatusFilter;
        syncKpiPressed();
        syncFilterToggleUi();
        renderB02Apps();
      };
    });

    syncFilterToggleUi();
    activateB02Tab(b02ActiveTab);

    const renderB02Apps = () => {
      const kw = b02Keyword.trim().toLowerCase();
      const filtered = myApps.filter((a) => {
        if (b02StatusFilter !== "ALL") {
          const st = a.status === "applying" ? "submitted" : (a.status === "interviewing" ? "consulting" : a.status);
          if (b02StatusFilter === "agreed") {
            if (st !== "approved" && st !== "contracted") return false;
          } else if (st !== b02StatusFilter) {
            return false;
          }
        }
        if (b02ContractFilter !== "ALL") {
          const ck = a.contractKind === "POPUP" ? "POPUP" : "LONG";
          if (ck !== b02ContractFilter) return false;
        }
        if (kw) {
          const hay = `${a.propTitle || ""} ${a.storeName || ""} ${a.genreName || ""}`.toLowerCase();
          if (!hay.includes(kw)) return false;
        }
        return true;
      });

      setText("b02-apps-count", String(filtered.length));
      const emptyEl = document.getElementById("b02-apps-empty");
      const listEl = document.getElementById("b02-apps-list");
      if (!listEl) return;

      if (!filtered.length) {
        if (emptyEl) emptyEl.hidden = false;
        listEl.innerHTML = "";
        return;
      }
      if (emptyEl) emptyEl.hidden = true;

      const readMap = loadB02ReadMap();

      listEl.innerHTML = filtered.map((a) => {
        const checks = Array.isArray(a.checks) ? a.checks : [];
        const confirmedCount = checks.filter((c) => c.ownerCheckStatus === "CONFIRMED").length;
        const rejectedCount = checks.filter((c) => c.ownerCheckStatus === "REJECTED").length;
        const contractLabel = a.contractKind === "POPUP" ? "ポップアップ短期" : "通常賃貸（長期）";
        const messages = getAppChatMessages(a);
        const latestMsg = messages[messages.length - 1] || null;
        const tenancy = store.getTenancyByApplicationId(a.id);
        const isHandedOver = a.status === "contracted" || tenancy?.tenancyStatus === "HANDED_OVER" || tenancy?.tenancyStatus === "OPEN";
        const canContract = a.status === "approved" || isHandedOver || Boolean(tenancy);
        const canPlan = isHandedOver;
        const canWithdraw = a.status === "submitted" || a.status === "applying" || a.status === "consulting" || a.status === "interviewing";
        const isOpen = b02ExpandedIds.has(a.id);
        const contractBtnLabel = isHandedOver ? "契約・引渡し内容を確認" : "契約・引渡し手続き";
        const displayStatusLabel = a.status === "contracted"
          ? "契約成立・引渡し完了"
          : (a.statusLabel || a.status);

        let hasUnreadOwnerReply = false;
        let messagePillHtml = `<span class="apply-summary-pill">オーナー返信待ち</span>`;
        if (latestMsg) {
          const isLatestOwner = latestMsg.senderRole !== "business";
          const senderTag = isLatestOwner ? "オーナー" : "自分";
          const shortTime = formatShortDateTimeNoSec(latestMsg.sentAt);
          const timeSuffix = shortTime ? `（${escapeHtml(shortTime)}）` : "";
          const seenCount = Number(readMap[a.id] || 0);
          hasUnreadOwnerReply = isLatestOwner && !isOpen && messages.length > seenCount;
          if (isLatestOwner) {
            const stateHint = hasUnreadOwnerReply ? "未読・返信可" : "確認済";
            const pillCls = hasUnreadOwnerReply ? "is-owner-unread" : "is-replied";
            messagePillHtml = `<button type="button" class="apply-summary-pill b02-msg-jump-btn ${pillCls}" data-b02-open-chat="${escapeHtml(a.id)}">最新: ${senderTag}${timeSuffix}・${stateHint}（全${messages.length}件）</button>`;
          } else {
            messagePillHtml = `<button type="button" class="apply-summary-pill b02-msg-jump-btn is-self-sent" data-b02-open-chat="${escapeHtml(a.id)}">最新: ${senderTag}${timeSuffix}・相手返信待ち（全${messages.length}件）</button>`;
          }
        }

        const submittedYmd = formatDateYMD(a.submittedAt);
        const viewingText = a.viewingRequestedAt ? formatDateTimeNoSec(a.viewingRequestedAt) : "";
        const chatPlaceholder = getB02ChatPlaceholder(a, isHandedOver);
        const chatEmptyHint = getB02ChatEmptyHint(a, isHandedOver);
        const primaryActionHtml = canPlan
          ? `<a class="btn-primary btn-sm b02-primary-cta" href="${escapeHtml(matchingUrl("opening-plan.jsp", { propId: a.propId, appId: a.id }))}">出店計画を作成・公開</a>`
          : (canContract
            ? `<a class="btn-primary btn-sm b02-primary-cta" href="${escapeHtml(matchingUrl("contract.jsp", { id: a.id }))}">${contractBtnLabel}</a>`
            : "");
        const showSecondaryContract = canPlan && canContract;
        const detailToggleLabel = isOpen ? "詳細・やり取りを閉じる" : "詳細・やり取りを見る";
        const unreadDotHtml = hasUnreadOwnerReply
          ? `<span class="b02-btn-unread-badge"><span class="b02-unread-dot" aria-hidden="true"></span>未読</span>`
          : "";

        return `
          <article class="detail-card apply-item-card b02-app-card" data-b02-app-id="${escapeHtml(a.id)}">
            <div class="apply-item-head">
              <div class="apply-item-main">
                <div class="apply-item-meta">
                  <span class="badge-status ${escapeHtml(getAppStatusBadgeClass(a.status))}">${escapeHtml(displayStatusLabel)}</span>
                  <span class="qual-badge">${escapeHtml(a.genreName || "")}</span>
                  <span class="apply-summary-pill">${escapeHtml(contractLabel)}</span>
                  <span class="apply-item-date">応募日: ${escapeHtml(submittedYmd)}</span>
                </div>
                <h3 class="apply-item-title">
                  <a href="${escapeHtml(propUrl("detail.jsp", { id: a.propId }))}">${escapeHtml(a.propTitle || "対象物件")}</a>
                  <small>（出店予定: ${escapeHtml(a.storeName || "")}）</small>
                </h3>
                <div class="apply-item-prop">
                  ${escapeHtml(formatApplicationPeriod(a, true))}
                  ${viewingText ? ` ／ 内覧希望: ${escapeHtml(viewingText)}` : ""}
                </div>
                <div class="apply-card-summary-row">
                  <span class="apply-summary-pill">${checks.length ? `要件確認: ${confirmedCount}/${checks.length} オーナー確認済` : "資格要件なし"}</span>
                  ${rejectedCount > 0 ? `<span class="apply-summary-pill is-alert">確認不備あり ${rejectedCount}件</span>` : ""}
                  ${messagePillHtml}
                  ${tenancy && !isHandedOver ? `<span class="apply-summary-pill is-replied">契約状況: ${escapeHtml(tenancy.statusLabel)}</span>` : ""}
                </div>
              </div>

              <div class="b02-card-action-col">
                ${primaryActionHtml ? `<div class="b02-primary-row">${primaryActionHtml}</div>` : ""}
                <div class="b02-secondary-row${showSecondaryContract ? " has-two" : ""}">
                  ${showSecondaryContract ? `<a class="btn-secondary btn-sm" href="${escapeHtml(matchingUrl("contract.jsp", { id: a.id }))}">${contractBtnLabel}</a>` : ""}
                  <button type="button" class="btn-secondary btn-sm b02-detail-toggle-btn${hasUnreadOwnerReply ? " has-unread" : ""}" data-b02-toggle="${escapeHtml(a.id)}" aria-expanded="${String(isOpen)}">
                    <span data-b02-toggle-label>${detailToggleLabel}</span>${unreadDotHtml}
                  </button>
                </div>
                ${canWithdraw ? `
                  <div class="b02-withdraw-wrap">
                    <button type="button" class="b02-withdraw-link" data-b02-withdraw="${escapeHtml(a.id)}">この応募を取り下げる</button>
                  </div>
                ` : ""}
              </div>
            </div>

            <div class="b02-app-drawer${isOpen ? " is-open" : ""}" data-b02-drawer="${escapeHtml(a.id)}"${isOpen ? "" : ' inert aria-hidden="true"'}>
              <div class="b02-drawer-inner">
                <div class="b02-drawer-grid">
                  <div>
                    <h4 class="matching-sub-label">応募時に送信した想い・コンセプト</h4>
                    <p class="story-content-body">${escapeHtml(a.message || a.concept || "記載なし")}</p>
                    ${checks.length ? `
                      <h4 class="matching-sub-label" style="margin-top:0.85rem;">資格・要件の確認状況</h4>
                      <ul class="b02-check-mini-list">
                        ${checks.map((c) => {
                          const cls = c.ownerCheckStatus === "CONFIRMED" ? "ok" : (c.ownerCheckStatus === "REJECTED" ? "missing" : "info");
                          const lbl = c.ownerCheckStatus === "CONFIRMED" ? "オーナー確認済" : (c.ownerCheckStatus === "REJECTED" ? "不備あり" : "オーナー確認待ち");
                          return `<li><span>${escapeHtml(c.requirementName || c.qualificationId)}</span><span class="req-badge-${cls}">${lbl}</span>${c.checkNote ? `<small class="b02-check-note">メモ: ${escapeHtml(c.checkNote)}</small>` : ""}</li>`;
                        }).join("")}
                      </ul>
                    ` : ""}
                  </div>
                  <div class="b02-chat-col">
                    <h4 class="matching-sub-label">オーナーとのやり取り</h4>
                    <div class="o10-chat-thread" aria-live="polite">
                      ${messages.length ? messages.map((m) => {
                        const isOwner = m.senderRole !== "business";
                        const roleLabel = isOwner ? "オーナー" : "自分";
                        const displayName = m.senderName || (isOwner ? "物件オーナー" : "あなた");
                        const sentText = m.sentAt ? formatDateTimeNoSec(m.sentAt) : "";
                        return `
                          <div class="o10-chat-msg ${isOwner ? "is-owner" : "is-business"}">
                            <div class="o10-chat-msg-meta">
                              <span class="b02-chat-sender">
                                <strong>${escapeHtml(displayName)}</strong>
                                <span class="b02-chat-role-badge ${isOwner ? "is-owner" : "is-self"}">${roleLabel}</span>
                              </span>
                              ${sentText ? `<span>${escapeHtml(sentText)}</span>` : ""}
                            </div>
                            <div class="o10-chat-msg-body">${escapeHtml(m.text)}</div>
                          </div>
                        `;
                      }).join("") : `<p class="o10-chat-empty">${escapeHtml(chatEmptyHint)}</p>`}
                    </div>
                    <label class="o10-field-label" for="b02-chat-input-${escapeHtml(a.id)}">オーナーへメッセージ送信</label>
                    <textarea id="b02-chat-input-${escapeHtml(a.id)}" class="form-control b02-chat-input" rows="2" data-b02-chat-input="${escapeHtml(a.id)}" placeholder="${escapeHtml(chatPlaceholder)}"></textarea>
                    <div class="o10-chat-send-row">
                      <button type="button" class="btn-primary btn-sm" data-b02-chat-send="${escapeHtml(a.id)}">返信を送信</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </article>
        `;
      }).join("");

      const syncOpenThreadsScroll = () => {
        listEl.querySelectorAll(".b02-app-drawer.is-open").forEach((drawer) => {
          scrollB02ThreadToBottom(drawer);
        });
      };
      syncOpenThreadsScroll();
      requestAnimationFrame(syncOpenThreadsScroll);

      if (b02PendingFocusInputId) {
        const focusId = b02PendingFocusInputId;
        b02PendingFocusInputId = null;
        const focusInput = listEl.querySelector(`[data-b02-chat-input="${CSS.escape(focusId)}"]`);
        if (focusInput) {
          focusInput.focus({ preventScroll: true });
          focusInput.closest(".b02-chat-col")?.scrollIntoView({ behavior: "smooth", block: "nearest" });
        }
      }

      const setB02DrawerOpen = (cardEl, id, shouldOpen, { focusChat = false } = {}) => {
        const drawer = cardEl?.querySelector(`[data-b02-drawer="${CSS.escape(id)}"]`);
        if (!drawer) return;

        const toggleBtn = cardEl.querySelector(`[data-b02-toggle="${CSS.escape(id)}"]`);
        const toggleLabel = toggleBtn?.querySelector("[data-b02-toggle-label]");
        const chatCol = drawer.querySelector(".b02-chat-col");
        const input = drawer.querySelector(`[data-b02-chat-input="${CSS.escape(id)}"]`);

        if (shouldOpen) {
          b02ExpandedIds.add(id);
          const targetApp = store.getApplicationById(id);
          if (targetApp) markB02AppRead(id, getAppChatMessages(targetApp).length);

          drawer.removeAttribute("inert");
          drawer.removeAttribute("aria-hidden");
          void drawer.offsetHeight;
          drawer.classList.add("is-open");

          if (toggleBtn) {
            toggleBtn.setAttribute("aria-expanded", "true");
            toggleBtn.classList.remove("has-unread");
            toggleBtn.querySelector(".b02-btn-unread-badge")?.remove();
          }
          if (toggleLabel) toggleLabel.textContent = "詳細・やり取りを閉じる";

          const openChatBtn = cardEl.querySelector(`[data-b02-open-chat="${CSS.escape(id)}"]`);
          if (openChatBtn && openChatBtn.classList.contains("is-owner-unread")) {
            openChatBtn.classList.remove("is-owner-unread");
            openChatBtn.classList.add("is-replied");
            openChatBtn.textContent = openChatBtn.textContent.replace("未読・返信可", "確認済");
          }

          const alignChatView = () => {
            scrollB02ThreadToBottom(drawer);
            if (focusChat && chatCol) {
              chatCol.scrollIntoView({ behavior: "smooth", block: "nearest" });
            }
          };

          scrollB02ThreadToBottom(drawer);
          requestAnimationFrame(() => {
            scrollB02ThreadToBottom(drawer);
            if (focusChat && input) input.focus({ preventScroll: true });
          });

          let settled = false;
          const finishExpand = () => {
            if (settled) return;
            settled = true;
            alignChatView();
            if (focusChat && input) input.focus({ preventScroll: true });
          };

          const onEnd = (ev) => {
            if (ev.target !== drawer || ev.propertyName !== "grid-template-rows") return;
            drawer.removeEventListener("transitionend", onEnd);
            finishExpand();
          };
          drawer.addEventListener("transitionend", onEnd);
          window.setTimeout(() => {
            drawer.removeEventListener("transitionend", onEnd);
            finishExpand();
          }, 320);
        } else {
          b02ExpandedIds.delete(id);
          drawer.classList.remove("is-open");
          drawer.setAttribute("inert", "");
          drawer.setAttribute("aria-hidden", "true");
          if (toggleBtn) toggleBtn.setAttribute("aria-expanded", "false");
          if (toggleLabel) toggleLabel.textContent = "詳細・やり取りを見る";
        }
      };

      listEl.onclick = (e) => {
        const openChatBtn = e.target.closest("[data-b02-open-chat]");
        if (openChatBtn) {
          const id = openChatBtn.getAttribute("data-b02-open-chat");
          setB02DrawerOpen(openChatBtn.closest("[data-b02-app-id]"), id, true, { focusChat: true });
          return;
        }
        const toggleBtn = e.target.closest("[data-b02-toggle]");
        if (toggleBtn) {
          const id = toggleBtn.getAttribute("data-b02-toggle");
          const willOpen = !b02ExpandedIds.has(id);
          setB02DrawerOpen(toggleBtn.closest("[data-b02-app-id]"), id, willOpen, { focusChat: false });
          return;
        }
        const sendBtn = e.target.closest("[data-b02-chat-send]");
        if (sendBtn) {
          const id = sendBtn.getAttribute("data-b02-chat-send");
          const input = listEl.querySelector(`[data-b02-chat-input="${CSS.escape(id)}"]`);
          const text = input ? input.value.trim() : "";
          if (!text) {
            window.MachirikuApp?.showToast?.("送信するメッセージを入力してください");
            input?.focus();
            return;
          }
          b02ExpandedIds.add(id);
          b02PendingFocusInputId = id;
          const msg = store.addApplicationMessage(id, text, "business");
          const targetApp = store.getApplicationById(id);
          if (msg && targetApp) markB02AppRead(id, getAppChatMessages(targetApp).length);
          window.MachirikuApp?.showToast?.("オーナーへメッセージを送信しました");
          return;
        }
        const withdrawBtn = e.target.closest("[data-b02-withdraw]");
        if (withdrawBtn) {
          const id = withdrawBtn.getAttribute("data-b02-withdraw");
          const app = store.getApplicationById(id);
          if (!app) return;
          openConfirmDialog({
            id: "b02-withdraw-dialog",
            title: "この出店応募を取り下げますか？",
            subtitle: `${app.propTitle}（${app.storeName}）`,
            desc: "取り下げるとステータスが「応募辞退」に変更されます。再度希望する場合は物件詳細から新しく応募してください。",
            confirmLabel: "取り下げを確定",
            confirmClass: "btn-secondary btn-danger-outline",
            onConfirm: () => {
              store.updateApplicationStatus(app.id, "withdrawn");
              window.MachirikuApp?.showToast?.("出店応募を取り下げました");
            }
          });
        }
      };

      // IME変換中のEnter誤送信を防止
      listEl.onkeydown = (e) => {
        if (e.isComposing || e.keyCode === 229) return;
        const input = e.target.closest("[data-b02-chat-input]");
        if (input && e.key === "Enter" && (e.ctrlKey || e.metaKey)) {
          e.preventDefault();
          const id = input.getAttribute("data-b02-chat-input");
          listEl.querySelector(`[data-b02-chat-send="${CSS.escape(id)}"]`)?.click();
        }
      };
    };

    const renderB02Offers = () => {
      const emptyEl = document.getElementById("b02-offers-empty");
      const listEl = document.getElementById("b02-offers-list");
      if (!listEl) return;

      if (!allVisibleOffers.length) {
        if (emptyEl) emptyEl.hidden = false;
        listEl.innerHTML = "";
        return;
      }
      if (emptyEl) emptyEl.hidden = true;

      listEl.innerHTML = allVisibleOffers.map((o) => {
        const prop = store.getPropertyById(o.propId);
        const isPending = o.offerStatus === "SENT";
        const receivedDate = formatDateTimeNoSec(o.sentAt || o.createdAt || "");
        return `
          <article class="detail-card apply-item-card">
            <div class="apply-item-head">
              <div class="apply-item-main">
                <div class="apply-item-meta">
                  <span class="badge-status ${escapeHtml(getOfferBadgeClass(o.offerStatus))}">${escapeHtml(o.statusLabel || o.offerStatus)}</span>
                  <span class="qual-badge">${o.creationKind === "AUTO" ? "条件マッチ打診" : "オーナー個別打診"}</span>
                  <span class="apply-item-date">受信日時: ${escapeHtml(receivedDate)}</span>
                </div>
                <h3 class="apply-item-title">
                  <a href="${escapeHtml(propUrl("detail.jsp", { id: o.propId }))}">${escapeHtml(o.propTitle || "提案物件")}</a>
                </h3>
                ${prop ? `<div class="apply-item-prop">${escapeHtml(prop.address)} ／ 賃料 ${formatRent(prop.rent)}万円・${prop.areaTsubo}坪（オーナー: ${escapeHtml(o.ownerName || prop.ownerName || "")}）</div>` : ""}
                ${(o.incentives || []).length ? `
                  <div class="apply-card-summary-row">
                    ${o.incentives.map((inc) => `<span class="apply-summary-pill is-replied">${escapeHtml(inc)}</span>`).join("")}
                  </div>
                ` : ""}
                <p class="matching-offer-excerpt">${escapeHtml(o.message || "")}</p>
              </div>
              <div class="matching-card-actions">
                ${isPending ? `
                  <button type="button" class="btn-primary btn-sm" data-b02-accept-offer="${escapeHtml(o.id)}">承諾して出店応募へ進む</button>
                  <button type="button" class="btn-secondary btn-sm btn-danger-outline" data-b02-decline-offer="${escapeHtml(o.id)}">今回は辞退する</button>
                ` : (o.offerStatus === "ACCEPTED" ? `
                  <a class="btn-primary btn-sm" href="${escapeHtml(propUrl("apply.jsp", { id: o.propId }))}">出店応募フォームを開く</a>
                ` : `
                  <a class="btn-secondary btn-sm" href="${escapeHtml(propUrl("detail.jsp", { id: o.propId }))}">物件詳細を見る</a>
                `)}
              </div>
            </div>
          </article>
        `;
      }).join("");

      listEl.onclick = (e) => {
        const acceptBtn = e.target.closest("[data-b02-accept-offer]");
        if (acceptBtn) {
          const offer = store.getOfferById(acceptBtn.getAttribute("data-b02-accept-offer"));
          if (!offer) return;
          openConfirmDialog({
            id: "b02-accept-offer-dialog",
            title: "このオファーを承諾して出店応募へ進みますか？",
            subtitle: `${offer.propTitle}`,
            desc: "オファーを承諾済みとして記録し、この物件の出店応募フォームへ移動します。",
            confirmLabel: "承諾して応募へ進む",
            onConfirm: () => {
              store.updateOfferStatus(offer.id, "ACCEPTED", "オファーを承諾しました");
              window.MachirikuApp?.showToast?.("オファーを承諾しました。続けて出店応募を入力してください");
              window.location.href = propUrl("apply.jsp", { id: offer.propId });
            }
          });
          return;
        }
        const declineBtn = e.target.closest("[data-b02-decline-offer]");
        if (declineBtn) {
          const offer = store.getOfferById(declineBtn.getAttribute("data-b02-decline-offer"));
          if (!offer) return;
          openConfirmDialog({
            id: "b02-decline-offer-dialog",
            title: "このオファーを辞退しますか？",
            subtitle: `${offer.propTitle}`,
            desc: "辞退の回答がオーナーへ通知されます。",
            confirmLabel: "辞退を確定",
            confirmClass: "btn-secondary btn-danger-outline",
            onConfirm: () => {
              store.updateOfferStatus(offer.id, "DECLINED", "今回は条件が合わず辞退いたします");
              window.MachirikuApp?.showToast?.("オファーを辞退しました");
            }
          });
        }
      };
    };

    renderB02Apps();
    renderB02Offers();
  }

  function hydrate(screenId, params = {}) {
    switch (String(screenId || "").toLowerCase()) {
      case "s13": return hydrateS13(params);
      case "s14": return hydrateS14(params);
      case "s15": return hydrateS15(params);
      case "s05": return hydrateS05(params);
      case "b02": return hydrateB02(params);
      default: return false;
    }
  }

  function supports(screenId) {
    return ["s13", "s14", "s15", "s05", "b02"].includes(String(screenId || "").toLowerCase());
  }

  return {
    hydrate,
    supports
  };
})();
