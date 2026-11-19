/**
 * マチリク (Machiriku) - property/ 配下11画面 (R01, B01, O01〜O09) コントローラー
 * 各JSPのHTML構造をデータハイドレートし、インタラクション・確認遷移・バリデーションを担当する。
 */
window.MachirikuPropertyPages = (function () {
  const store = window.MachirikuStore;
  const data = window.MACHIRIKU_DATA;

  const DRAFT_ADD_KEY = "machiriku_prop_draft_add";
  const DRAFT_EDIT_KEY = "machiriku_prop_draft_edit";
  const DELETE_RECEIPT_KEY = "machiriku_deleted_prop_receipt";
  const PLACEHOLDER_IMG = "assets/property-placeholder.svg";

  function getCtx() {
    if (typeof window !== "undefined" && window.__MACHIRIKU_CONTEXT_PATH__ !== undefined) {
      return String(window.__MACHIRIKU_CONTEXT_PATH__ || "").replace(/\/+$/, "");
    }
    return "";
  }

  function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, c => ({
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

  function propUrl(jspName, params = {}) {
    const base = `${getCtx()}/property/${jspName}`;
    const keys = Object.keys(params || {}).filter(k => params[k] !== undefined && params[k] !== "");
    if (!keys.length) return base;
    return `${base}?${keys.map(k => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`).join("&")}`;
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

  function buildCategoryOptgroupsHtml(selectedId = "", emptyLabel = "", preferredIds = []) {
    let html = emptyLabel ? `<option value="">${escapeHtml(emptyLabel)}</option>` : "";
    const prefSet = new Set(Array.isArray(preferredIds) ? preferredIds : []);
    if (prefSet.size > 0) {
      let prefOpts = "";
      prefSet.forEach(id => {
        const sub = data.SUBCATS?.[id];
        if (sub) {
          const sel = sub.id === selectedId ? "selected" : "";
          prefOpts += `<option value="${escapeHtml(sub.id)}" ${sel}>${escapeHtml(sub.name)}</option>`;
        }
      });
      if (prefOpts) {
        html += `<optgroup label="プロフィール登録済みの希望業種">${prefOpts}</optgroup>`;
      }
    }
    (data.CATEGORIES || []).forEach(cat => {
      html += `<optgroup label="${escapeHtml(cat.name)}">`;
      (cat.subcategories || []).forEach(sub => {
        const sel = !prefSet.has(sub.id) && sub.id === selectedId ? "selected" : "";
        html += `<option value="${escapeHtml(sub.id)}" ${sel}>${escapeHtml(sub.name)}</option>`;
      });
      html += `</optgroup>`;
    });
    return html;
  }

  function getLiveAnnouncer(announcerId = "r01-live-announcer") {
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
    const supportsUserInvalid = CSS.supports("selector(:user-invalid)");
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
      if (existing.length > 0) {
        field.setAttribute("aria-describedby", existing.join(" "));
      } else {
        field.removeAttribute("aria-describedby");
      }
    }
    return invalid;
  }

  function initAccessibleFormValidation(formEl, options = {}) {
    if (!formEl) return null;
    const announcerId = options.announcerId || "r01-live-announcer";
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
      announce: (text) => {
        const announcer = getLiveAnnouncer(announcerId);
        announcer.textContent = "";
        window.requestAnimationFrame(() => {
          announcer.textContent = text;
        });
      },
      focusFirstInvalid: () => {
        const firstInvalid = getFields().find(f => !f.disabled && f.offsetParent !== null && f.validity && !f.validity.valid);
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

  function calcDistanceKm(lat1, lng1, lat2, lng2) {
    if ([lat1, lng1, lat2, lng2].some(v => typeof v !== "number" || isNaN(v))) return null;
    const rad = Math.PI / 180;
    const dLat = (lat2 - lat1) * rad;
    const dLng = (lng2 - lng1) * rad;
    const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLng / 2) ** 2;
    return Math.round(6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 100) / 100;
  }

  // =========================================================================
  // R01: property/vote.jsp (投票 / 地域住民)
  // =========================================================================
  function hydrateR01(params) {
    const propId = params?.id || store.getProperties()[0]?.id;
    const prop = store.getPropertyById(propId);
    const emptyEl = document.getElementById("r01-empty-state");
    const sectionEl = document.getElementById("r01-section");
    if (!sectionEl) return;

    if (!prop) {
      sectionEl.hidden = true;
      if (emptyEl) emptyEl.hidden = false;
      return;
    }
    if (emptyEl) emptyEl.hidden = true;
    sectionEl.hidden = false;

    const state = store.getState();
    const curVote = state.currentUser.votedProps?.[prop.id] || null;
    const detailHref = propUrl("detail.jsp", { id: prop.id });
    const bcDetail = document.getElementById("r01-bc-detail");
    if (bcDetail) bcDetail.setAttribute("href", detailHref);
    const cancelLink = document.getElementById("r01-cancel-link");
    if (cancelLink) cancelLink.setAttribute("href", detailHref);

    setText("r01-prop-title", `【対象物件】${prop.title}`);
    setText("r01-prop-meta", `${prop.address}（現在 ${prop.votesCount || 0}人の住民が投票中）`);

    // 生活拠点選択（自宅 / 職場・学校）
    const locContainer = document.getElementById("r01-location-list");
    if (locContainer) {
      const locs = state.currentUser.locations || (data.USERS?.resident?.locations || []);
      const selectedLocId = curVote?.locationId || locs[0]?.id || "loc-home";
      if (!locs.length) {
        locContainer.innerHTML = `<p class="sample-note">生活拠点が未登録のため既定距離（${state.currentUser.livingDistanceKm || 0.4}km）で集計されます。</p>`;
      } else {
        locContainer.innerHTML = locs.map(loc => {
          const dist = prop.location ? calcDistanceKm(loc.lat, loc.lng, prop.location.lat, prop.location.lng) : null;
          const distLabel = dist != null ? `物件まで約 ${dist} km` : "距離自動計算";
          const checked = loc.id === selectedLocId ? "checked" : "";
          return `
            <label class="location-radio-card">
              <input type="radio" name="locationId" value="${escapeHtml(loc.id)}" ${checked}>
              <div>
                <div class="loc-title">${escapeHtml(loc.label)} <span class="loc-dist-badge">${escapeHtml(distLabel)}</span></div>
                <div class="loc-addr">${escapeHtml(loc.address || "")}</div>
              </div>
            </label>
          `;
        }).join("");
      }
    }

    // 第1〜第3希望セレクト生成（第1入力で第2、第2入力で第3を段階的に表示）
    const r1 = document.getElementById("r01-rank1");
    const r2 = document.getElementById("r01-rank2");
    const r3 = document.getElementById("r01-rank3");
    const r2Card = document.getElementById("r01-rank2-card");
    const r3Card = document.getElementById("r01-rank3-card");
    if (r1) r1.innerHTML = buildCategoryOptgroupsHtml(curVote?.ranks?.[0] || "", "最も出店してほしい業種を選択してください");
    if (r2) r2.innerHTML = buildCategoryOptgroupsHtml(curVote?.ranks?.[1] || "", "希望なし");
    if (r3) r3.innerHTML = buildCategoryOptgroupsHtml(curVote?.ranks?.[2] || "", "希望なし");

    const freqEl = document.getElementById("r01-frequency");
    const timingEl = document.getElementById("r01-timing");
    const commentEl = document.getElementById("r01-comment");
    if (freqEl && curVote?.frequency) freqEl.value = curVote.frequency;
    if (timingEl && curVote?.timing) timingEl.value = curVote.timing;
    if (commentEl) commentEl.value = curVote?.comment || "";

    const submitBtn = document.getElementById("r01-submit-btn");
    if (submitBtn) submitBtn.textContent = curVote ? "投票内容を更新する" : "投票を確定して送信する";

    const withdrawBtn = document.getElementById("r01-withdraw-btn");
    if (withdrawBtn) {
      withdrawBtn.hidden = !curVote;
      withdrawBtn.onclick = () => {
        if (!window.confirm("この物件への投票を取り下げますか？")) return;
        const res = store.withdrawVote(prop.id);
        if (res.success) {
          window.MachirikuApp?.showToast?.("投票を取り下げました");
          window.location.href = detailHref;
        }
      };
    }

    const form = document.getElementById("r01-vote-form");
    const validationCtrl = initAccessibleFormValidation(form, { announcerId: "r01-live-announcer" });

    const syncRankFlow = () => {
      if (!r1 || !r2 || !r3) return;
      const hasR1 = Boolean(r1.value);
      if (!hasR1) {
        r2.value = "";
        r3.value = "";
        validationCtrl?.resetField(r2);
        validationCtrl?.resetField(r3);
      } else if (!r2.value) {
        r3.value = "";
        validationCtrl?.resetField(r3);
      }
      if (r2Card) r2Card.hidden = !hasR1;
      if (r3Card) r3Card.hidden = !(hasR1 && Boolean(r2.value));

      const vals = [r1.value, r2.value, r3.value];
      [r1, r2, r3].forEach((sel, idx) => {
        Array.from(sel.options).forEach(opt => {
          if (!opt.value) { opt.disabled = false; return; }
          opt.disabled = vals.some((v, vIdx) => vIdx !== idx && v === opt.value);
        });
      });
    };
    if (r1 && r2 && r3) {
      r1.onchange = syncRankFlow;
      r2.onchange = syncRankFlow;
      r3.onchange = syncRankFlow;
      syncRankFlow();
    }

    if (form) {
      form.onsubmit = (e) => {
        e.preventDefault();
        if (r1 && !r1.value) {
          r1.classList.add("user-invalid-fallback");
          validationCtrl?.markTouched(r1);
          r1.focus();
          window.MachirikuApp?.showToast?.("第1希望の業種を1つ選択してください");
          return;
        }
        if (submitBtn) submitBtn.disabled = true;
        const ranks = [r1?.value, r2?.value, r3?.value].filter(Boolean);
        const locChecked = form.querySelector('input[name="locationId"]:checked');
        const res = store.submitVote(prop.id, ranks, {
          locationId: locChecked ? locChecked.value : "loc-home",
          frequency: freqEl?.value || "",
          timing: timingEl?.value || "",
          comment: commentEl?.value || ""
        });
        if (!res.success) {
          if (submitBtn) submitBtn.disabled = false;
          if (r1) {
            r1.classList.add("user-invalid-fallback");
            validationCtrl?.markTouched(r1);
            r1.focus();
          }
          window.MachirikuApp?.showToast?.(res.message || "入力内容を確認してください");
          return;
        }
        window.MachirikuApp?.showToast?.(res.isUpdate ? "投票内容を更新しました" : "投票を送信しました！");
        window.location.href = detailHref;
      };
    }
  }

  // =========================================================================
  // B01: property/apply.jsp (出店応募 / 出店希望者)
  // =========================================================================
  function hydrateB01(params) {
    const propId = params?.id || store.getProperties()[0]?.id;
    const prop = store.getPropertyById(propId);
    const emptyEl = document.getElementById("b01-empty-state");
    const sectionEl = document.getElementById("b01-section");
    if (!sectionEl) return;

    if (!prop) {
      sectionEl.hidden = true;
      if (emptyEl) emptyEl.hidden = false;
      return;
    }
    if (emptyEl) emptyEl.hidden = true;
    sectionEl.hidden = false;

    showRoleNotice("b01-role-notice", ["business", "admin"], "出店事業主");
    const switchRoleBtn = document.getElementById("b01-switch-role-btn");
    if (switchRoleBtn) {
      switchRoleBtn.onclick = () => {
        store.setRole("business");
        hydrateB01(params);
      };
    }

    const state = store.getState();
    const bizUser = state.users?.business || state.currentUser || {};
    const detailHref = propUrl("detail.jsp", { id: prop.id });

    const bcDetail = document.getElementById("b01-bc-detail");
    const backLink = document.getElementById("b01-back-link");
    if (bcDetail) bcDetail.setAttribute("href", detailHref);
    if (backLink) backLink.setAttribute("href", detailHref);

    setText("b01-prop-title", `【応募対象】${prop.title}`);
    setText("b01-prop-meta", `賃料: ${formatRent(prop.rent)}万円/月 / 面積: ${prop.areaTsubo}坪（${prop.areaSqm}㎡） / ${prop.condition || "業種相談可"}`);

    const businessNameEl = document.getElementById("b01-business-name");
    const plannedStoreNameEl = document.getElementById("b01-planned-store-name");
    const genreEl = document.getElementById("b01-genre");
    const longOpenRowEl = document.getElementById("b01-long-open-row");
    const openDateEl = document.getElementById("b01-target-open-date");
    const periodRowEl = document.getElementById("b01-popup-period-row");
    const startEl = document.getElementById("b01-start-on");
    const endEl = document.getElementById("b01-end-on");
    const endErrEl = document.getElementById("b01-end-on-error");
    const messageEl = document.getElementById("b01-message");
    const expEl = document.getElementById("b01-exp-years");

    // 屋号・事業者名はアカウント情報を自動入力（開店予定店舗名は任意の別欄）
    const accountBusinessName = bizUser.businessName || bizUser.company || bizUser.brand || "";
    const bizAutofillBadge = document.getElementById("b01-business-name-autofill");
    if (businessNameEl && !businessNameEl.value) {
      businessNameEl.value = accountBusinessName;
    }
    if (bizAutofillBadge) {
      bizAutofillBadge.hidden = !Boolean(accountBusinessName);
    }

    // 出店予定業種もアカウント登録済み希望業種を自動入力（優先選択）
    const preferredIds = Array.isArray(bizUser.preferredIndustries) ? bizUser.preferredIndustries : [];
    const allowedList = Array.isArray(prop.allowedIndustries) ? prop.allowedIndustries : [];
    const matchedPreferred = preferredIds.find(id => allowedList.includes(id)) || preferredIds[0] || "";
    const defaultGenreId = matchedPreferred
      || allowedList[0]
      || "I0101";
    const genreAutofillBadge = document.getElementById("b01-genre-autofill");
    if (genreEl) {
      const selectedGenre = genreEl.value || defaultGenreId;
      genreEl.innerHTML = buildCategoryOptgroupsHtml(selectedGenre, "", preferredIds);
    }
    if (genreAutofillBadge) {
      genreAutofillBadge.hidden = !Boolean(matchedPreferred);
    }

    if (openDateEl && !openDateEl.value) {
      openDateEl.value = new Date(Date.now() + 30 * 86400000).toISOString().split("T")[0];
    }
    // ストーリー文言・実務経験年数の初期自動入力は行わない（自由入力可）

    const form = document.getElementById("b01-apply-form");
    const validationCtrl = initAccessibleFormValidation(form, { announcerId: "b01-live-announcer" });

    // 契約形態連動（長期出店は終了日なし・非表示、ポップアップ短期のみ期間入力を表示）
    const popupTerms = prop.contractTerms || { longEnabled: true, popupEnabled: true, minDays: 3, maxDays: 30, dailyRentYen: Math.round((prop.rent || 180000) / 25) };
    const popupBox = document.getElementById("b01-popup-terms-box");
    const longRadio = document.getElementById("b01-contract-long");
    const popupRadio = document.getElementById("b01-contract-popup");

    const validatePopupDates = () => {
      if (!endEl) return true;
      const isPopup = Boolean(popupRadio && popupRadio.checked);
      if (!isPopup) {
        endEl.setCustomValidity("");
        return true;
      }
      const sVal = startEl?.value;
      const eVal = endEl.value;
      if (!sVal || !eVal) {
        endEl.setCustomValidity("");
        if (endErrEl) endErrEl.innerHTML = `<span role="img" aria-label="エラー">⚠</span> 利用終了希望日を選択してください。`;
        return false;
      }
      if (eVal < sVal) {
        const msg = "利用終了希望日は利用開始希望日以降の日付を指定してください。";
        endEl.setCustomValidity(msg);
        if (endErrEl) endErrEl.innerHTML = `<span role="img" aria-label="エラー">⚠</span> ${msg}`;
        return false;
      }
      const diffDays = Math.round((new Date(eVal) - new Date(sVal)) / 86400000);
      const minD = popupTerms.minDays || 1;
      const maxD = popupTerms.maxDays || 365;
      if (diffDays < minD || diffDays > maxD) {
        const msg = `ポップアップ希望日数は ${minD}〜${maxD}日 の範囲で指定してください（現在 ${diffDays}日）。`;
        endEl.setCustomValidity(msg);
        if (endErrEl) endErrEl.innerHTML = `<span role="img" aria-label="エラー">⚠</span> ${msg}`;
        return false;
      }
      endEl.setCustomValidity("");
      return true;
    };

    const syncContractTerms = () => {
      const isPopup = Boolean(popupRadio && popupRadio.checked);
      if (popupBox) {
        popupBox.hidden = !isPopup;
        if (isPopup) {
          const minD = popupTerms.minDays || 3;
          const maxD = popupTerms.maxDays || 30;
          const daily = popupTerms.dailyRentYen != null ? `${formatMoney(popupTerms.dailyRentYen)}円/日` : "相談";
          popupBox.innerHTML = `
            <strong>ポップアップ短期出店条件</strong>：利用日数目安 <strong>${minD}日〜${maxD}日</strong> / 日額賃料目安 <strong>${escapeHtml(daily)}</strong>
            ${popupTerms.popupEnabled === false ? `<div class="inline-warn-text">※この物件は通常賃貸中心ですが、オーナーへ短期相談として申請できます。</div>` : ""}
          `;
        }
      }
      if (longOpenRowEl) longOpenRowEl.hidden = isPopup;
      if (openDateEl) {
        openDateEl.disabled = isPopup;
        openDateEl.required = !isPopup;
        if (isPopup) {
          openDateEl.setCustomValidity("");
          validationCtrl?.resetField(openDateEl);
        }
      }
      if (periodRowEl) periodRowEl.hidden = !isPopup;
      if (startEl) {
        startEl.disabled = !isPopup;
        startEl.required = isPopup;
        if (isPopup && !startEl.value) {
          startEl.value = openDateEl?.value || new Date(Date.now() + 30 * 86400000).toISOString().split("T")[0];
        }
        if (!isPopup) {
          startEl.setCustomValidity("");
          validationCtrl?.resetField(startEl);
        }
      }
      if (endEl) {
        endEl.disabled = !isPopup;
        endEl.required = isPopup;
        if (isPopup && startEl?.value && !endEl.value) {
          const minD = popupTerms.minDays || 3;
          const sDate = new Date(startEl.value);
          endEl.value = new Date(sDate.getTime() + minD * 86400000).toISOString().split("T")[0];
        }
        if (!isPopup) {
          endEl.setCustomValidity("");
          validationCtrl?.resetField(endEl);
        } else {
          validatePopupDates();
        }
      }
    };
    if (longRadio) longRadio.onchange = syncContractTerms;
    if (popupRadio) popupRadio.onchange = syncContractTerms;
    const handlePopupDateChange = () => {
      if (!endEl) return;
      const ok = validatePopupDates();
      validationCtrl?.markTouched(endEl, !ok);
    };
    if (startEl) {
      startEl.oninput = handlePopupDateChange;
      startEl.onchange = handlePopupDateChange;
    }
    if (endEl) {
      endEl.oninput = handlePopupDateChange;
      endEl.onchange = handlePopupDateChange;
    }
    syncContractTerms();

    // 業種別 必要資格・書類チェック描画（Modern Web Guidance準拠：送信ボタンは非活性化しない）
    const reqAlertEl = document.getElementById("b01-req-alert");
    const reqListEl = document.getElementById("b01-req-list");
    const submitBtn = document.getElementById("b01-submit-btn");
    const heldQualNames = (bizUser.qualifications || []).map(q => String(q.name || ""));
    let missingRequiredQualCount = 0;

    const hasHeldQualification = (qualObj) => {
      if (!qualObj) return false;
      return heldQualNames.some(held => held.includes(qualObj.name) || qualObj.name.includes(held));
    };

    const renderRequirements = () => {
      if (!reqListEl || !genreEl) return;
      const subcatId = genreEl.value;
      const reqMap = data.INDUSTRY_QUALIFICATION_REQUIREMENTS || {};
      const qualMap = data.QUALIFICATIONS_MAP || {};
      const reqs = reqMap[subcatId] || reqMap.DEFAULT_NON_FOOD || [];

      missingRequiredQualCount = 0;

      reqListEl.innerHTML = reqs.map((req, idx) => {
        const q = qualMap[req.qualificationId] || { id: req.qualificationId, name: req.qualificationId, kind: "DOCUMENT", kindLabel: "確認" };
        const isQualType = q.kind === "QUALIFICATION" || req.qualificationId === "qual-confectionery-permit";
        const isHeld = hasHeldQualification(q);
        const mustDisable = isQualType && req.isRequired && !isHeld;
        if (mustDisable) missingRequiredQualCount += 1;

        const cbId = `b01-req-cb-${idx}`;
        const errId = `b01-req-err-${idx}`;
        const badgeClass = isHeld ? "req-badge-ok" : (mustDisable ? "req-badge-missing" : "req-badge-info");
        const badgeText = isHeld ? "プロフィール登録済 ✓" : (mustDisable ? "未登録（応募前にプロフィール登録が必要）" : (req.isRequired ? "必須確認" : "確認・推奨"));

        return `
          <div class="req-check-card ${mustDisable ? "is-missing" : ""}" data-req-id="${escapeHtml(q.id)}">
            <div class="req-check-main">
              <input id="${cbId}" type="checkbox" class="b01-req-checkbox" value="${escapeHtml(q.id)}"
                data-req-name="${escapeHtml(q.name)}"
                ${(!mustDisable && req.isRequired) ? `data-error="${errId}" required aria-invalid="false"` : ""}
                ${isHeld ? "checked" : ""}
                ${mustDisable ? "disabled" : ""}>
              <label for="${cbId}">
                <span class="req-check-title">${escapeHtml(q.name)}</span>
                <span class="${badgeClass}">${escapeHtml(badgeText)}</span>
              </label>
            </div>
            <p class="req-check-desc">${escapeHtml(req.conditionText || q.guidanceText || "")}</p>
            ${(!isHeld && q.guidanceUrl) ? `
              <div class="req-guidance-row">
                <span>申請・取得案内：</span>
                <a href="${escapeHtml(q.guidanceUrl)}" target="_blank" rel="noopener noreferrer">${escapeHtml(q.name)}の取得・申請方法を確認する ↗</a>
              </div>
            ` : ""}
            ${(!mustDisable && req.isRequired) ? `
              <div id="${errId}" class="form-error-msg">
                <span role="img" aria-label="エラー">⚠</span> 「${escapeHtml(q.name)}」の確認チェックを入れてください。
              </div>
            ` : ""}
          </div>
        `;
      }).join("");

      if (reqAlertEl) {
        if (missingRequiredQualCount > 0) {
          reqAlertEl.hidden = false;
          reqAlertEl.innerHTML = `<strong>必須資格が事業主プロフィールに未登録です（${missingRequiredQualCount}件）</strong><br>この業種での応募には対象資格の登録が必要です。資格取得後にプロフィール画面から登録いただくか、保有資格に合う業種をお選びください。`;
        } else {
          reqAlertEl.hidden = true;
          reqAlertEl.innerHTML = "";
        }
      }
      if (submitBtn) submitBtn.disabled = false;
    };

    if (genreEl) {
      genreEl.onchange = renderRequirements;
    }
    renderRequirements();

    if (form) {
      form.onsubmit = (e) => {
        e.preventDefault();
        if (missingRequiredQualCount > 0) {
          if (reqAlertEl) {
            reqAlertEl.hidden = false;
            reqAlertEl.focus();
          }
          const msg = "必須資格がプロフィールに未登録のため、資格登録または別業種への変更が必要です。";
          validationCtrl?.announce(msg);
          window.MachirikuApp?.showToast?.(msg);
          return;
        }
        const isPopup = Boolean(popupRadio && popupRadio.checked);
        const datesOk = validatePopupDates();
        if (!datesOk || !form.checkValidity()) {
          const invalidEl = validationCtrl?.focusFirstInvalid();
          if (invalidEl) {
            const errMsg = invalidEl.validationMessage || "入力内容を確認してください";
            validationCtrl?.announce(errMsg);
            window.MachirikuApp?.showToast?.(errMsg);
          }
          return;
        }

        if (submitBtn) submitBtn.disabled = true;
        const bizNameVal = businessNameEl?.value.trim() || accountBusinessName || "事業者";
        const plannedStoreVal = plannedStoreNameEl?.value.trim() || "";
        const openDateVal = isPopup ? (startEl?.value || "") : (openDateEl?.value || "");
        const sVal = isPopup ? (startEl?.value || "") : openDateVal;
        const eVal = isPopup ? (endEl?.value || "") : "";

        const checks = Array.from(form.querySelectorAll(".b01-req-checkbox")).map(cb => ({
          qualificationId: cb.value,
          requirementName: cb.getAttribute("data-req-name") || cb.value,
          businessConfirmed: cb.checked,
          ownerCheckStatus: "UNCONFIRMED",
          checkNote: ""
        }));

        const created = store.submitApplication({
          propId: prop.id,
          propTitle: prop.title,
          businessName: bizNameVal,
          plannedStoreName: plannedStoreVal,
          storeName: plannedStoreVal || bizNameVal,
          genreId: genreEl?.value || defaultGenreId,
          targetRent: prop.rent || 0,
          contractKind: isPopup ? "POPUP" : "LONG",
          requestedStartOn: sVal,
          requestedEndOn: eVal,
          viewingRequestedAt: document.getElementById("b01-viewing-at")?.value || "",
          targetOpenDate: openDateVal,
          experienceYears: expEl?.value.trim() || "",
          message: messageEl?.value.trim() || "",
          checks
        });

        window.MachirikuApp?.showToast?.("出店応募を送信しました！");
        const doneBox = document.getElementById("b01-complete-box");
        if (doneBox) {
          form.hidden = true;
          doneBox.hidden = false;
          const labelName = plannedStoreVal ? `${plannedStoreVal}（${bizNameVal}）` : bizNameVal;
          setText("b01-complete-summary", `${prop.title} へ「${labelName}（${created.genreName}）」として応募を受け付けました。`);
          const backDetailBtn = document.getElementById("b01-complete-detail-btn");
          if (backDetailBtn) backDetailBtn.setAttribute("href", detailHref);
          document.title = "応募完了 | 出店応募 | マチリク";
          validationCtrl?.announce("出店応募を送信しました");
          doneBox.focus();
        }
      };
    }
  }

  // =========================================================================
  // O01: property/manage.jsp (登録物件管理 / オーナー)
  // =========================================================================
  let manageStatusFilter = "ALL";

  function hydrateO01() {
    const sectionEl = document.getElementById("o01-section");
    if (!sectionEl) return;

    showRoleNotice("o01-role-notice", ["owner", "admin"], "物件オーナー");
    const state = store.getState();
    const ownerIds = new Set(state.users?.owner?.ownedPropIds || []);
    const myProps = state.properties.filter(p => ownerIds.has(p.id) || p.ownerId === state.users?.owner?.id);

    const totalVotes = myProps.reduce((sum, p) => sum + (Number(p.votesCount) || 0), 0);
    const totalScore = Math.round(myProps.reduce((sum, p) => sum + (Number(p.demandScore) || 0), 0) * 10) / 10;
    const appsForMyProps = (state.applications || []).filter(a => myProps.some(p => p.id === a.propId));
    const prCount = myProps.filter(p => p.isPr).length;

    setText("o01-kpi-count", String(myProps.length));
    setText("o01-kpi-votes", String(totalVotes));
    setText("o01-kpi-score", `計 ${totalScore} pt`);
    setText("o01-kpi-apps", String(appsForMyProps.length));
    setText("o01-kpi-pr", String(prCount));

    const tabsEl = document.getElementById("o01-status-tabs");
    if (tabsEl) {
      tabsEl.querySelectorAll("button[data-status]").forEach(btn => {
        const st = btn.getAttribute("data-status");
        btn.classList.toggle("is-active", st === manageStatusFilter);
        btn.onclick = () => {
          manageStatusFilter = st;
          hydrateO01();
        };
      });
    }

    const demoBtn = document.getElementById("o01-assign-demo-btn");
    if (demoBtn) {
      demoBtn.onclick = () => {
        store.assignDemoOwnerProperties();
        window.MachirikuApp?.showToast?.("デモ用サンプル物件（2件）を所有物件に割り当てました");
      };
    }

    const emptyEl = document.getElementById("o01-empty-box");
    const listEl = document.getElementById("o01-prop-list");
    if (!listEl) return;

    if (myProps.length === 0) {
      if (emptyEl) emptyEl.hidden = false;
      listEl.innerHTML = "";
      return;
    }
    if (emptyEl) emptyEl.hidden = true;

    const filtered = myProps.filter(p => {
      if (manageStatusFilter === "ALL") return true;
      const ls = p.listingStatus || "OPEN";
      return ls === manageStatusFilter;
    });

    if (!filtered.length) {
      listEl.innerHTML = `<div class="detail-card"><p class="sample-note">該当ステータスの物件はありません。</p></div>`;
      return;
    }

    listEl.innerHTML = filtered.map(p => {
      const ls = p.listingStatus || "OPEN";
      const isPaused = ls === "PAUSED" || ls === "DRAFT";
      const nextStatus = ls === "OPEN" ? "PAUSED" : "OPEN";
      const toggleLabel = ls === "OPEN" ? "募集を一時停止" : "公開中にする";
      const pendingApps = (state.applications || []).filter(a => a.propId === p.id && (a.status === "submitted" || a.status === "applying" || a.status === "consulting")).length;
      const imgUrl = resolveAssetUrl(p.coverImage || PLACEHOLDER_IMG);

      return `
        <article class="detail-card manage-prop-card">
          <img class="manage-prop-thumb" src="${escapeHtml(imgUrl)}" alt="${escapeHtml(p.title)}" onerror="MachirikuScreens.handlePropertyImageError(this)">
          <div class="manage-prop-body">
            <div class="manage-prop-badges">
              <span class="badge-status status-${escapeHtml(p.status || 'voting')}">${escapeHtml(p.statusLabel || "募集中")}</span>
              ${p.isPr ? '<span class="badge-pr">PR注目枠掲載中</span>' : ""}
              ${pendingApps > 0 ? `<span class="badge-pending-app">未対応応募 ${pendingApps}件</span>` : ""}
              <span class="manage-prop-code">管理コード: ${escapeHtml(p.code || p.id)}</span>
            </div>
            <h3 class="manage-prop-title">
              <a href="${escapeHtml(propUrl("detail.jsp", { id: p.id }))}">${escapeHtml(p.title)}</a>
            </h3>
            <div class="manage-prop-meta">${escapeHtml(p.address)} / 賃料 ${formatRent(p.rent)}万円 / ${p.areaTsubo}坪（${p.areaSqm}㎡）</div>
            <div class="manage-prop-demand">
              投票総数: ${p.votesCount || 0}票 / 需要スコア: ${p.demandScore || 0}pt（1位: ${escapeHtml(p.topDemands?.[0]?.label || "集計中")}）
            </div>
          </div>
          <div class="manage-prop-actions">
            <a class="btn-primary btn-sm" href="${escapeHtml(propUrl("apply-list.jsp", { propId: p.id }))}">出店希望確認${pendingApps > 0 ? ` (${pendingApps})` : ""}</a>
            <a class="btn-secondary btn-sm" href="${escapeHtml(propUrl("analytics.jsp", { id: p.id }))}">需要分析</a>
            <a class="btn-secondary btn-sm" href="${escapeHtml(propUrl("edit.jsp", { id: p.id }))}">物件編集</a>
            <button type="button" class="btn-secondary btn-sm" onclick="MachirikuPropertyPages.toggleStatus('${escapeHtml(p.id)}', '${nextStatus}')">${toggleLabel}</button>
            <a class="btn-secondary btn-sm btn-danger-outline" href="${escapeHtml(propUrl("delete-confirm.jsp", { id: p.id }))}">削除</a>
          </div>
        </article>
      `;
    }).join("");
  }

  function toggleStatus(propId, nextStatus) {
    store.togglePropertyStatus(propId, nextStatus);
    window.MachirikuApp?.showToast?.(nextStatus === "OPEN" ? "物件を公開中に変更しました" : "物件の募集を一時停止しました");
  }

  // =========================================================================
  // O02: property/analytics.jsp (物件需要分析 / オーナー)
  // =========================================================================
  function hydrateO02(params) {
    const sectionEl = document.getElementById("o02-section");
    if (!sectionEl) return;

    showRoleNotice("o02-role-notice", ["owner", "business", "admin"], "物件オーナー・事業主");
    const state = store.getState();
    const props = state.properties || [];
    const selectEl = document.getElementById("o02-prop-select");
    const selectedPropId = params?.id || "all";
    const selectedProp = selectedPropId !== "all" ? store.getPropertyById(selectedPropId) : null;

    if (selectEl) {
      selectEl.innerHTML = `<option value="all" ${selectedPropId === "all" ? "selected" : ""}>宇都宮市 全物件総合需要（${props.length}件）</option>` +
        props.map(p => `<option value="${escapeHtml(p.id)}" ${selectedPropId === p.id ? "selected" : ""}>${escapeHtml(p.title)}（${p.votesCount || 0}票 / ${p.demandScore || 0}pt）</option>`).join("");
      selectEl.onchange = () => {
        window.location.href = propUrl("analytics.jsp", selectEl.value === "all" ? {} : { id: selectEl.value });
      };
    }

    const linkDetail = document.getElementById("o02-link-detail");
    const linkEdit = document.getElementById("o02-link-edit");
    const linkApplyList = document.getElementById("o02-link-apply-list");
    if (linkDetail) {
      linkDetail.hidden = !selectedProp;
      if (selectedProp) linkDetail.setAttribute("href", propUrl("detail.jsp", { id: selectedProp.id }));
    }
    if (linkEdit) {
      linkEdit.hidden = !selectedProp;
      if (selectedProp) linkEdit.setAttribute("href", propUrl("edit.jsp", { id: selectedProp.id }));
    }
    if (linkApplyList) {
      linkApplyList.setAttribute("href", propUrl("apply-list.jsp", selectedProp ? { propId: selectedProp.id } : {}));
    }

    let targetTitle, votesCount, demandScore, topGenreLabel, entryCount, topDemandsList, voicesList;
    if (selectedProp) {
      targetTitle = selectedProp.title;
      votesCount = selectedProp.votesCount || 0;
      demandScore = selectedProp.demandScore || 0;
      topGenreLabel = selectedProp.topDemands?.[0]?.label || "需要集計中";
      entryCount = (state.applications || []).filter(a => a.propId === selectedProp.id).length;
      topDemandsList = selectedProp.topDemands || [];
      voicesList = selectedProp.residentVoices || [];
    } else {
      targetTitle = "全物件総合需要";
      votesCount = props.reduce((acc, p) => acc + (p.votesCount || 0), 0);
      demandScore = Math.round(props.reduce((acc, p) => acc + (p.demandScore || 0), 0) * 10) / 10;
      const byGenre = new Map();
      props.forEach(p => (p.topDemands || []).forEach(d => {
        const cur = byGenre.get(d.subcatId) || { ...d, votes: 0, rank1Votes: 0, rank2Votes: 0, rank3Votes: 0, score: 0 };
        cur.votes += d.votes || 0;
        cur.rank1Votes += d.rank1Votes || 0;
        cur.rank2Votes += d.rank2Votes || 0;
        cur.rank3Votes += d.rank3Votes || 0;
        cur.score = Math.round((cur.score + (d.score || 0)) * 10) / 10;
        byGenre.set(d.subcatId, cur);
      }));
      topDemandsList = [...byGenre.values()].sort((a, b) => b.score - a.score);
      topGenreLabel = topDemandsList[0]?.label || "未集計";
      entryCount = (state.applications || []).length;
      voicesList = props.flatMap(p => p.residentVoices || []);
    }

    setText("o02-kpi-votes", String(votesCount));
    setText("o02-kpi-score", String(demandScore));
    setText("o02-kpi-top-genre", topGenreLabel);
    setText("o02-kpi-apps", String(entryCount));
    setText("o02-chart-title", `7大分類別 需要スコア分布（${targetTitle}）`);

    const catScores = (data.CATEGORIES || []).map(c => {
      const score = topDemandsList
        .filter(d => data.SUBCATS[d.subcatId]?.categoryId === c.id)
        .reduce((total, d) => total + (d.score || 0), 0);
      return { ...c, score: Math.round(score * 10) / 10 };
    });
    const maxCatScore = Math.max(...catScores.map(c => c.score), 1);

    const catChartEl = document.getElementById("o02-cat-chart");
    if (catChartEl) {
      catChartEl.innerHTML = catScores.map(c => {
        const pct = Math.min(100, Math.round((c.score / maxCatScore) * 100));
        return `
          <div class="cat-score-row">
            <div class="cat-score-head">
              <span>${escapeHtml(c.name)}</span>
              <span>${c.score} pt (${pct}%)</span>
            </div>
            <div class="demand-progress-track">
              <div class="demand-progress-fill" style="width:${pct}%; background-color:${c.color};"></div>
            </div>
          </div>
        `;
      }).join("");
    }

    const rankingEl = document.getElementById("o02-subcat-ranking");
    if (rankingEl) {
      const sorted = [...topDemandsList].filter(d => (d.votes || 0) > 0).sort((a, b) => b.score - a.score).slice(0, 8);
      const maxSubScore = Math.max(...sorted.map(d => d.score || 0), 1);
      rankingEl.innerHTML = sorted.length ? sorted.map((d, idx) => {
        const pct = Math.max(4, Math.round(((d.score || 0) / maxSubScore) * 100));
        return `
          <div class="demand-bar-item">
            <div class="demand-bar-header demand-stacked-header">
              <span>第${idx + 1}位: ${escapeHtml(d.label)}</span>
              <span class="demand-stacked-metrics"><strong>${d.score} pt</strong> <span class="demand-votes-breakdown">/ 計${d.votes}票</span></span>
            </div>
            <div class="demand-progress-track">
              <div class="demand-progress-fill" style="width:${pct}%; background-color:var(--primary-600);"></div>
            </div>
          </div>
        `;
      }).join("") : `<p class="sample-note">まだ需要投票がありません。</p>`;
    }

    const voicesEl = document.getElementById("o02-voices-list");
    if (voicesEl) {
      voicesEl.innerHTML = voicesList.length ? voicesList.map(v => `
        <div class="quote-bubble">
          <p class="quote-text">“${escapeHtml(v.text)}”</p>
          <div class="quote-meta"><span>${escapeHtml(v.author)}</span><span>${escapeHtml(v.date || "")}</span></div>
        </div>
      `).join("") : `<p class="sample-note">まだ住民の声が寄せられていません。</p>`;
    }

    const insightEl = document.getElementById("o02-condition-insight");
    if (insightEl) {
      if (selectedProp && topDemandsList[0]) {
        const topSub = topDemandsList[0];
        const allowed = Array.isArray(selectedProp.allowedIndustries) ? selectedProp.allowedIndustries : [];
        const mismatch = allowed.length > 0 && !allowed.includes(topSub.subcatId);
        insightEl.hidden = false;
        insightEl.innerHTML = `
          <div>
            <strong>条件見直しのヒント：</strong>
            ${mismatch
              ? `1位希望の「${escapeHtml(topSub.label)}」は現在の募集許可業種に含まれていません。`
              : `1位希望の「${escapeHtml(topSub.label)}（${topSub.score} pt）」に合う事業主からの応募獲得に向け、募集条件や設備PRを充実させましょう。`}
          </div>
          <a class="btn-secondary btn-sm" href="${escapeHtml(propUrl("edit.jsp", { id: selectedProp.id }))}">この物件の条件を見直す</a>
        `;
      } else {
        insightEl.hidden = true;
      }
    }
  }

  // =========================================================================
  // O03: property/apply-list.jsp (出店希望確認 / オーナー)
  // =========================================================================
  let expandedAppId = null;

  function hydrateO03(params) {
    const sectionEl = document.getElementById("o03-section");
    if (!sectionEl) return;

    showRoleNotice("o03-role-notice", ["owner", "business", "admin"], "物件オーナー");
    const state = store.getState();
    const apps = state.applications || [];
    const props = state.properties || [];

    const propFilterEl = document.getElementById("o03-prop-filter");
    const statusFilterEl = document.getElementById("o03-status-filter");

    if (propFilterEl && propFilterEl.options.length <= 1) {
      const initPropId = params?.propId || params?.id || "ALL";
      propFilterEl.innerHTML = `<option value="ALL">すべての物件</option>` +
        props.map(p => `<option value="${escapeHtml(p.id)}" ${initPropId === p.id ? "selected" : ""}>${escapeHtml(p.title)}</option>`).join("");
      propFilterEl.onchange = () => hydrateO03({});
    }
    if (statusFilterEl) {
      statusFilterEl.onchange = () => hydrateO03({});
    }

    const filterPropId = propFilterEl ? propFilterEl.value : "ALL";
    const filterStatus = statusFilterEl ? statusFilterEl.value : "ALL";

    const filtered = apps.filter(a => {
      if (filterPropId !== "ALL" && a.propId !== filterPropId) return false;
      if (filterStatus !== "ALL") {
        const st = a.status === "applying" ? "submitted" : (a.status === "interviewing" ? "consulting" : a.status);
        if (st !== filterStatus) return false;
      }
      return true;
    });

    setText("o03-total-count", String(filtered.length));
    const emptyEl = document.getElementById("o03-empty-box");
    const listEl = document.getElementById("o03-app-list");
    if (!listEl) return;

    if (!filtered.length) {
      if (emptyEl) emptyEl.hidden = false;
      listEl.innerHTML = "";
      return;
    }
    if (emptyEl) emptyEl.hidden = true;

    const bizUser = state.users?.business || {};
    const avgScore = bizUser.reviewCount ? `${bizUser.avgSatisfaction} / 5（${bizUser.reviewCount}件）` : "集計中（新規事業主）";

    listEl.innerHTML = filtered.map(a => {
      const isOpen = expandedAppId === a.id || filtered.length === 1;
      const checks = Array.isArray(a.checks) ? a.checks : [];
      const contractLabel = a.contractKind === "POPUP" ? "ポップアップ短期" : "通常賃貸";
      return `
        <article class="detail-card apply-item-card">
          <div class="apply-item-head">
            <div>
              <div class="apply-item-meta">
                <span class="badge-status status-reviewing">${escapeHtml(a.statusLabel || a.status)}</span>
                <span class="qual-badge">${escapeHtml(a.genreName || "")}</span>
                <span class="apply-item-date">${escapeHtml(a.submittedAt || "")}</span>
              </div>
              <h3 class="apply-item-title">${escapeHtml(a.storeName)} <small>（応募者: ${escapeHtml(a.applicantName)}）</small></h3>
              <div class="apply-item-prop">
                対象物件: <a href="${escapeHtml(propUrl("detail.jsp", { id: a.propId }))}">${escapeHtml(a.propTitle)}</a>
                ／ 契約: ${contractLabel}（${a.requestedEndOn ? `${escapeHtml(a.requestedStartOn || "未定")} 〜 ${escapeHtml(a.requestedEndOn)}` : `オープン予定: ${escapeHtml(a.targetOpenDate || a.requestedStartOn || "相談")}`}）
              </div>
            </div>
            <button type="button" class="btn-secondary btn-sm" aria-expanded="${isOpen}" onclick="MachirikuPropertyPages.toggleApplyDetail('${escapeHtml(a.id)}')">
              ${isOpen ? "詳細を閉じる" : "詳細・資格確認・返信"}
            </button>
          </div>

          <div id="o03-detail-${escapeHtml(a.id)}" class="apply-item-detail" ${isOpen ? "" : "hidden"}>
            <div class="apply-detail-grid">
              <div>
                <h4 class="apply-sub-heading">出店への想い・メッセージ</h4>
                <div class="story-content-body">${escapeHtml(a.message || a.concept || "メッセージなし")}</div>
                <div class="apply-extra-row">
                  <span><strong>内覧希望日時:</strong> ${escapeHtml((a.viewingRequestedAt || "指定なし").replace("T", " "))}</span>
                  <span><strong>顧客アンケート評価実績:</strong> ${escapeHtml(avgScore)}</span>
                </div>

                <h4 class="apply-sub-heading" style="margin-top:1rem;">必要資格・要件の確認（application_checks）</h4>
                <div class="apply-checks-list">
                  ${checks.length ? checks.map(c => `
                    <div class="apply-check-row">
                      <div>
                        <strong>${escapeHtml(c.requirementName || c.qualificationId)}</strong>
                        <span class="req-badge-${c.ownerCheckStatus === 'CONFIRMED' ? 'ok' : (c.ownerCheckStatus === 'REJECTED' ? 'missing' : 'info')}">
                          ${c.ownerCheckStatus === "CONFIRMED" ? "オーナー確認済" : (c.ownerCheckStatus === "REJECTED" ? "不備あり" : "未確認")}
                        </span>
                      </div>
                      <div class="apply-check-actions">
                        <button type="button" class="btn-secondary btn-xs" onclick="MachirikuPropertyPages.setCheckStatus('${escapeHtml(a.id)}', '${escapeHtml(c.qualificationId)}', 'CONFIRMED')">確認済</button>
                        <button type="button" class="btn-secondary btn-xs btn-danger-outline" onclick="MachirikuPropertyPages.setCheckStatus('${escapeHtml(a.id)}', '${escapeHtml(c.qualificationId)}', 'REJECTED')">不備あり</button>
                      </div>
                    </div>
                  `).join("") : `<p class="sample-note">要件確認項目は登録されていません。</p>`}
                </div>
              </div>

              <div class="apply-reply-panel">
                <h4 class="apply-sub-heading">オーナーからの返信・判断</h4>
                <textarea id="o03-reply-${escapeHtml(a.id)}" class="form-control" rows="4" placeholder="面談候補日時や質問事項、審査結果のメッセージを入力...">${escapeHtml(a.ownerReply || "")}</textarea>
                <div class="apply-action-stack">
                  <button type="button" class="btn-primary btn-sm" onclick="MachirikuPropertyPages.updateApp('${escapeHtml(a.id)}', 'consulting')">面談・相談中にして返信</button>
                  <button type="button" class="btn-cta-apply btn-sm" onclick="MachirikuPropertyPages.updateApp('${escapeHtml(a.id)}', 'approved')">出店を承認する</button>
                  <button type="button" class="btn-secondary btn-sm" onclick="MachirikuPropertyPages.updateApp('${escapeHtml(a.id)}', 'contracted')">契約成立・引渡し完了にする</button>
                  <button type="button" class="btn-secondary btn-sm btn-danger-outline" onclick="MachirikuPropertyPages.updateApp('${escapeHtml(a.id)}', 'rejected')">今回は見送る</button>
                </div>
              </div>
            </div>
          </div>
        </article>
      `;
    }).join("");
  }

  function toggleApplyDetail(appId) {
    expandedAppId = expandedAppId === appId ? null : appId;
    hydrateO03({});
  }

  function setCheckStatus(appId, qualificationId, status) {
    store.updateApplicationCheck(appId, qualificationId, status);
    window.MachirikuApp?.showToast?.(status === "CONFIRMED" ? "要件を確認済みにしました" : "不備ありとして記録しました");
  }

  function updateApp(appId, newStatus) {
    const replyEl = document.getElementById(`o03-reply-${appId}`);
    const replyText = replyEl ? replyEl.value.trim() : "";
    store.updateApplicationStatus(appId, newStatus, replyText);
    window.MachirikuApp?.showToast?.("応募ステータスと返信を更新しました");
  }

  // =========================================================================
  // O04〜O07: add.jsp / add-confirm.jsp / edit.jsp / edit-confirm.jsp 共通フォームロジック
  // =========================================================================
  let pickerMap = null;
  let pickerMarker = null;

  function populateSelectOptions(selectEl, items, selectedVal = "", emptyLabel = "選択してください") {
    if (!selectEl) return;
    const list = Array.isArray(items) ? [...items] : [];
    const target = selectedVal != null ? String(selectedVal) : "";
    if (target && !list.includes(target)) {
      list.push(target);
    }
    let html = emptyLabel ? `<option value="">${escapeHtml(emptyLabel)}</option>` : "";
    html += list.map(v => `<option value="${escapeHtml(v)}" ${v === target ? "selected" : ""}>${escapeHtml(v)}</option>`).join("");
    selectEl.innerHTML = html;
  }

  function extractJapaneseAddressParts(result) {
    const comps = Array.isArray(result?.address_components) ? result.address_components : [];
    let pref = "";
    let locality = "";
    let ward = "";
    for (const comp of comps) {
      const types = comp.types || [];
      if (!pref && types.includes("administrative_area_level_1")) pref = comp.long_name || "";
      if (!locality && types.includes("locality")) locality = comp.long_name || "";
      if (!ward && types.includes("sublocality_level_1")) ward = comp.long_name || "";
    }
    const city = (locality && ward && locality.endsWith("市") && ward.endsWith("区"))
      ? `${locality}${ward}`
      : (locality || ward || "");
    let formatted = String(result?.formatted_address || "")
      .replace(/^日本、\s*/, "")
      .replace(/^〒\d{3}-?\d{4}\s*/, "")
      .trim();
    if (pref && formatted.startsWith(pref)) formatted = formatted.slice(pref.length).trim();
    if (city && formatted.startsWith(city)) formatted = formatted.slice(city.length).trim();
    else if (locality && formatted.startsWith(locality)) formatted = formatted.slice(locality.length).trim();
    const addressLine = formatted
      .replace(/[０-９]/g, ch => String.fromCharCode(ch.charCodeAt(0) - 0xFEE0))
      .replace(/[−―ー‐]/g, "-")
      .trim();
    return { prefecture: pref, municipality: city, addressLine };
  }

  function initPropertyForm(mode, params) {
    const isEdit = mode === "edit";
    const sectionEl = document.getElementById(isEdit ? "o06-section" : "o04-section");
    if (!sectionEl) return;

    showRoleNotice(isEdit ? "o06-role-notice" : "o04-role-notice", ["owner", "admin"], "物件オーナー");
    const switchRoleBtn = document.getElementById(isEdit ? "o06-switch-role-btn" : "o04-switch-role-btn");
    if (switchRoleBtn) {
      switchRoleBtn.onclick = () => {
        store.setRole("owner");
      };
    }

    let baseProp = null;
    if (isEdit) {
      const propId = params?.id || "";
      baseProp = store.getPropertyById(propId);
      const emptyEl = document.getElementById("o06-empty-state");
      if (!baseProp) {
        sectionEl.hidden = true;
        if (emptyEl) emptyEl.hidden = false;
        return;
      }
      sectionEl.hidden = false;
      if (emptyEl) emptyEl.hidden = true;
    }

    // 確認画面から「修正して戻る」場合は sessionStorage のドラフトを復元
    let draft = null;
    try {
      const raw = window.sessionStorage.getItem(isEdit ? DRAFT_EDIT_KEY : DRAFT_ADD_KEY);
      if (raw && params?.restore === "1") {
        const parsed = JSON.parse(raw);
        if (isEdit && parsed.propId === baseProp.id) draft = parsed.after;
        else if (!isEdit) draft = parsed;
      }
    } catch (_) {}

    const hasInitialRecord = Boolean(isEdit || draft);
    const src = draft || baseProp || {};
    const form = document.getElementById("prop-editor-form");
    if (!form) return;

    const opts = data.PROPERTY_FORM_OPTIONS || {};
    const setVal = (id, val) => {
      const el = document.getElementById(id);
      if (el && val !== undefined && val !== null) el.value = String(val);
    };

    // 基本テキスト＆住所分解（既存データの編集時は address から都道府県・市区町村も補完）
    setVal("prop-title", src.title || "");
    let initPref = src.prefecture || "";
    let initCity = src.municipality || "";
    let rawAddr = src.addressLine || "";
    if ((!initPref || !initCity || !rawAddr) && src.address) {
      const addrMatch = String(src.address).match(/^(東京都|北海道|(?:京都|大阪)府|.{2,3}県)([^市区町村]+?[市区町村])(.*)$/);
      if (addrMatch) {
        if (!initPref) initPref = addrMatch[1];
        if (!initCity) initCity = addrMatch[2];
        if (!rawAddr) rawAddr = addrMatch[3].trim();
      } else if (!rawAddr) {
        rawAddr = src.address.replace(/^栃木県宇都宮市/, "");
      }
    }
    setVal("prop-address-line", rawAddr);
    setVal("prop-access", src.access || "");
    setVal("prop-rent", src.rent != null ? src.rent : "");
    setVal("prop-mgmt", src.managementFee != null ? src.managementFee : "");
    setVal("prop-area-sqm", src.areaSqm != null ? src.areaSqm : "");
    setVal("prop-area-tsubo", src.areaTsubo != null ? src.areaTsubo : "");
    setVal("prop-condition", src.condition || "");
    setVal("prop-description", src.description || "");
    setVal("prop-listing-url", src.listingUrl || "");

    // 都道府県・市区町村テキストボックス（新規時は空・マップピン設置で自動入力）
    const prefEl = document.getElementById("prop-pref");
    const cityEl = document.getElementById("prop-city");
    setVal("prop-pref", initPref);
    setVal("prop-city", initCity);

    // 敷金・礼金・保証金・建物スペックドロップダウン（新規時はすべて未選択）
    const cleanOptionalText = (val) => (!hasInitialRecord || val === "掲載なし（要確認）") ? "" : val;
    populateSelectOptions(document.getElementById("prop-deposit"), opts.deposits || [], cleanOptionalText(src.deposit || ""), "選択してください");
    populateSelectOptions(document.getElementById("prop-key-money"), opts.keyMonies || [], cleanOptionalText(src.keyMoney || ""), "選択してください");
    populateSelectOptions(document.getElementById("prop-security-deposit"), opts.securityDeposits || [], cleanOptionalText(src.securityDeposit || ""), "選択してください");
    populateSelectOptions(document.getElementById("prop-floor-num"), opts.floorNumbers || [], src.floorNumber || src.floor || "", "選択してください");
    populateSelectOptions(document.getElementById("prop-total-floors"), opts.totalFloors || [], src.totalFloors || "", "選択してください");
    populateSelectOptions(document.getElementById("prop-structure"), opts.structures || [], src.structure || "", "選択してください");
    populateSelectOptions(document.getElementById("prop-handover"), opts.handovers || [], src.handover || "", "選択してください");

    // 築年月ドロップダウン（年・月）
    const currentYear = new Date().getFullYear();
    const yearItems = [];
    for (let y = currentYear; y >= 1960; y -= 1) yearItems.push(`${y}年`);
    yearItems.push("不明");
    const monthItems = Array.from({ length: 12 }, (_, i) => `${i + 1}月`);
    let initBuiltY = "";
    let initBuiltM = "";
    if (src.builtYear) {
      if (src.builtYear === "不明") {
        initBuiltY = "不明";
      } else {
        const m = String(src.builtYear).match(/(\d{4}年)(?:(\d{1,2}月))?/);
        if (m) {
          initBuiltY = m[1] || "";
          initBuiltM = m[2] || "";
        }
      }
    }
    populateSelectOptions(document.getElementById("prop-built-year"), yearItems, initBuiltY, "年");
    populateSelectOptions(document.getElementById("prop-built-month"), monthItems, initBuiltM, "月");

    // 営業許可・公開ステータス（新規時は未選択、編集・復元時は既存値を保持）
    const fallbackListing = src.status === "contracted" ? "CONTRACTED" : (src.status === "paused" ? "PAUSED" : "OPEN");
    setVal("prop-permit-status", src.permitStatus || (hasInitialRecord ? "UNKNOWN" : ""));
    setVal("prop-listing-status", src.listingStatus || (hasInitialRecord ? fallbackListing : ""));

    const hasSavedCoords = hasInitialRecord && (src.lat != null || src.location?.lat != null) && (src.lng != null || src.location?.lng != null);
    const latVal = hasSavedCoords ? Number(src.lat ?? src.location?.lat) : "";
    const lngVal = hasSavedCoords ? Number(src.lng ?? src.location?.lng) : "";
    setVal("prop-lat", latVal);
    setVal("prop-lng", lngVal);

    const validationCtrl = initAccessibleFormValidation(form, { announcerId: isEdit ? "o06-live-announcer" : "o04-live-announcer" });

    // ㎡ ↔ 坪 自動換算
    const sqmEl = document.getElementById("prop-area-sqm");
    const tsuboEl = document.getElementById("prop-area-tsubo");
    if (sqmEl && tsuboEl) {
      sqmEl.oninput = () => {
        const v = parseFloat(sqmEl.value);
        tsuboEl.value = (!isNaN(v) && v > 0) ? String(Math.round((v / 3.30578) * 100) / 100) : "";
      };
      tsuboEl.oninput = () => {
        const v = parseFloat(tsuboEl.value);
        sqmEl.value = (!isNaN(v) && v > 0) ? String(Math.round((v * 3.30578) * 100) / 100) : "";
      };
    }

    // 契約形態（通常・ポップアップ：新規時は未チェック）
    const ct = src.contractTerms || (hasInitialRecord ? { longEnabled: true, popupEnabled: false, minDays: 3, maxDays: 30, dailyRentYen: "" } : { longEnabled: false, popupEnabled: false, minDays: "", maxDays: "", dailyRentYen: "" });
    const longCb = document.getElementById("prop-long-enabled");
    const popupCb = document.getElementById("prop-popup-enabled");
    const popupFields = document.getElementById("prop-popup-fields");
    const popupMinEl = document.getElementById("prop-popup-min-days");
    const popupMaxEl = document.getElementById("prop-popup-max-days");
    const popupDailyEl = document.getElementById("prop-popup-daily-rent");
    const contractErrEl = document.getElementById("prop-contract-error");

    if (longCb) longCb.checked = Boolean(ct.longEnabled);
    if (popupCb) popupCb.checked = Boolean(ct.popupEnabled);
    setVal("prop-popup-min-days", ct.minDays != null ? ct.minDays : "");
    setVal("prop-popup-max-days", ct.maxDays != null ? ct.maxDays : "");
    setVal("prop-popup-daily-rent", ct.dailyRentYen != null ? ct.dailyRentYen : "");

    const syncContractPopupFields = () => {
      const popupOn = Boolean(popupCb && popupCb.checked);
      if (popupFields) popupFields.hidden = !popupOn;
      [popupMinEl, popupMaxEl, popupDailyEl].forEach(el => {
        if (!el) return;
        el.disabled = !popupOn;
        if (!popupOn) {
          el.required = false;
          el.setCustomValidity("");
          validationCtrl?.resetField(el);
        }
      });
      if (popupMinEl) popupMinEl.required = popupOn;
      if (popupMaxEl) popupMaxEl.required = popupOn;
      if (contractErrEl && ((longCb && longCb.checked) || popupOn)) {
        contractErrEl.textContent = "";
        contractErrEl.style.display = "none";
      }
    };
    if (longCb) longCb.onchange = syncContractPopupFields;
    if (popupCb) popupCb.onchange = syncContractPopupFields;
    syncContractPopupFields();

    // 基本インフラ10項目
    const infraGrid = document.getElementById("prop-infra-checkboxes");
    if (infraGrid) {
      const infraObj = src.infrastructure || {};
      const joined = `${(src.facilities || []).join(" ")} ${src.condition || ""}`;
      infraGrid.innerHTML = (data.INFRASTRUCTURE_FACILITIES || []).map((item, idx) => {
        const checked = hasInitialRecord && (typeof infraObj[item.key] === "boolean"
          ? infraObj[item.key]
          : joined.includes(item.label.replace("あり", "")));
        const cbId = `prop-infra-cb-${idx}`;
        return `<div class="check-chip"><input id="${cbId}" type="checkbox" name="infra_${escapeHtml(item.key)}" value="${escapeHtml(item.key)}" ${checked ? "checked" : ""}><label for="${cbId}">${escapeHtml(item.label)}</label></div>`;
      }).join("");
    }

    // 許可・歓迎業種（27小分類）
    const indContainer = document.getElementById("prop-allowed-industries");
    if (indContainer) {
      const allowedSet = new Set(src.allowedIndustries || []);
      indContainer.innerHTML = (data.CATEGORIES || []).map((cat, cIdx) => `
        <div class="industry-group-box">
          <div class="industry-group-title">${escapeHtml(cat.name)}</div>
          <div class="industry-group-checks">
            ${(cat.subcategories || []).map((sub, sIdx) => {
              const cbId = `prop-ind-cb-${cIdx}-${sIdx}`;
              return `
                <div class="check-chip-sm">
                  <input id="${cbId}" type="checkbox" name="allowedIndustries" value="${escapeHtml(sub.id)}" ${allowedSet.has(sub.id) ? "checked" : ""}>
                  <label for="${cbId}">${escapeHtml(sub.name)}</label>
                </div>
              `;
            }).join("")}
          </div>
        </div>
      `).join("");
    }

    // 写真ギャラリービルダー（新規時は初期写真なし）
    const isCatalogProp = Boolean(baseProp && Array.isArray(data.PROPERTIES) && data.PROPERTIES.some(p => p.id === baseProp.id));
    const sanitizeGalleryItems = (items) => (Array.isArray(items) ? items : [])
      .filter(g => g && g.url && g.url !== PLACEHOLDER_IMG)
      .filter(g => isCatalogProp || (
        !(g.url === "assets/properties/prop-orion-01/01_exterior.jpg" && g.label === "外観写真") &&
        !(g.url === "assets/properties/prop-orion-01/02_interior.jpg" && g.label === "店内サンプル写真")
      ))
      .map(g => ({ kind: g.kind || "exterior", label: g.label || "物件写真", url: g.url }));
    let galleryState = sanitizeGalleryItems(src.gallery);

    const galleryListEl = document.getElementById("prop-gallery-list");
    const syncGalleryDomIndices = () => {
      if (!galleryListEl) return;
      Array.from(galleryListEl.querySelectorAll(".gallery-builder-row")).forEach((row, idx) => {
        row.dataset.gIdx = String(idx);
        const kindEl = row.querySelector("[data-g-kind]");
        const labelEl = row.querySelector("[data-g-label]");
        const delEl = row.querySelector("[data-g-del]");
        if (kindEl) kindEl.setAttribute("data-g-kind", String(idx));
        if (labelEl) labelEl.setAttribute("data-g-label", String(idx));
        if (delEl) delEl.setAttribute("data-g-del", String(idx));
      });
    };

    const reorderGalleryByIndices = (fromIdx, toIdx) => {
      if (!galleryListEl) return;
      const rows = Array.from(galleryListEl.querySelectorAll(".gallery-builder-row"));
      const movingRow = rows[fromIdx];
      const targetRow = rows[toIdx];
      if (!movingRow || !targetRow || fromIdx === toIdx) return;

      const beforeTops = new Map(rows.map(r => [r, r.getBoundingClientRect().top]));
      const [moved] = galleryState.splice(fromIdx, 1);
      galleryState.splice(toIdx, 0, moved);

      if (fromIdx < toIdx) {
        galleryListEl.insertBefore(movingRow, targetRow.nextSibling);
      } else {
        galleryListEl.insertBefore(movingRow, targetRow);
      }
      syncGalleryDomIndices();

      Array.from(galleryListEl.querySelectorAll(".gallery-builder-row")).forEach((r) => {
        const prevTop = beforeTops.get(r);
        if (prevTop == null) return;
        const delta = prevTop - r.getBoundingClientRect().top;
        if (!delta) return;
        r.style.transition = "none";
        r.style.transform = `translate3d(0, ${delta}px, 0)`;
        void r.offsetHeight;
        r.style.transition = "transform 220ms cubic-bezier(0.22, 1, 0.36, 1)";
        r.style.transform = "";
      });
    };

    const renderGalleryBuilder = () => {
      if (!galleryListEl) return;
      galleryListEl.innerHTML = galleryState.map((item, idx) => `
        <div class="gallery-builder-row" data-g-idx="${idx}">
          <button type="button" class="gallery-drag-handle" aria-label="上下にドラッグして順番を入れ替え" title="掴んで上下に動かすと順番を入れ替えられます">
            <svg viewBox="0 0 16 16" width="16" height="16" fill="currentColor" aria-hidden="true">
              <circle cx="5.5" cy="3.5" r="1.3"/><circle cx="10.5" cy="3.5" r="1.3"/>
              <circle cx="5.5" cy="8" r="1.3"/><circle cx="10.5" cy="8" r="1.3"/>
              <circle cx="5.5" cy="12.5" r="1.3"/><circle cx="10.5" cy="12.5" r="1.3"/>
            </svg>
          </button>
          <img class="gallery-builder-thumb" src="${escapeHtml(resolveAssetUrl(item.url))}" alt="" onerror="this.src='${escapeHtml(resolveAssetUrl(PLACEHOLDER_IMG))}'">
          <div class="gallery-builder-fields">
            <select class="form-select form-select-sm" aria-label="写真種別" data-g-kind="${idx}">
              <option value="exterior" ${item.kind === "exterior" ? "selected" : ""}>外観</option>
              <option value="interior" ${item.kind === "interior" ? "selected" : ""}>室内</option>
              <option value="floorplan" ${item.kind === "floorplan" ? "selected" : ""}>間取り</option>
              <option value="facility" ${item.kind === "facility" ? "selected" : ""}>設備</option>
              <option value="surrounding" ${item.kind === "surrounding" ? "selected" : ""}>周辺環境</option>
            </select>
            <input type="text" class="form-control form-control-sm" aria-label="写真の説明" data-g-label="${idx}" value="${escapeHtml(item.label)}" placeholder="写真の説明（例: 店舗入口）">
          </div>
          <div class="gallery-builder-btns">
            <button type="button" class="btn-secondary btn-xs btn-danger-outline" data-g-del="${idx}">削除</button>
          </div>
        </div>
      `).join("") || `<p class="sample-note">写真はまだ追加されていません。必要に応じて「＋ 写真ファイルを追加」から選択できます。</p>`;

      galleryListEl.querySelectorAll("[data-g-kind]").forEach(el => {
        el.onchange = () => {
          const idx = Number(el.getAttribute("data-g-kind"));
          if (galleryState[idx]) galleryState[idx].kind = el.value;
        };
      });
      galleryListEl.querySelectorAll("[data-g-label]").forEach(el => {
        el.oninput = () => {
          const idx = Number(el.getAttribute("data-g-label"));
          if (galleryState[idx]) galleryState[idx].label = el.value;
        };
      });
      galleryListEl.querySelectorAll("[data-g-del]").forEach(el => {
        el.onclick = () => {
          const i = Number(el.getAttribute("data-g-del"));
          if (!Number.isInteger(i) || i < 0 || i >= galleryState.length) return;
          galleryState.splice(i, 1);
          renderGalleryBuilder();
        };
      });

      galleryListEl.querySelectorAll(".gallery-drag-handle").forEach(handle => {
        handle.addEventListener("keydown", (e) => {
          const row = handle.closest(".gallery-builder-row");
          const rows = Array.from(galleryListEl.querySelectorAll(".gallery-builder-row"));
          const idx = rows.indexOf(row);
          if (idx === -1) return;
          if (e.key === "ArrowUp" && idx > 0) {
            e.preventDefault();
            reorderGalleryByIndices(idx, idx - 1);
            handle.focus();
          } else if (e.key === "ArrowDown" && idx < rows.length - 1) {
            e.preventDefault();
            reorderGalleryByIndices(idx, idx + 1);
            handle.focus();
          }
        });

        handle.addEventListener("pointerdown", (e) => {
          if (e.button !== 0 && e.pointerType === "mouse") return;
          const dragRow = handle.closest(".gallery-builder-row");
          if (!dragRow || galleryState.length < 2) return;
          e.preventDefault();
          try { handle.setPointerCapture(e.pointerId); } catch (_) {}

          const pointerY0 = e.clientY;
          let baseShift = 0;
          let currentTy = 0;
          dragRow.classList.add("is-dragging");
          dragRow.style.transition = "none";

          const swapDuringDrag = (fromIdx, toIdx) => {
            const rows = Array.from(galleryListEl.querySelectorAll(".gallery-builder-row"));
            const targetRow = rows[toIdx];
            if (!targetRow) return;
            const beforeTops = new Map(rows.map(r => [r, r.getBoundingClientRect().top]));
            const oldOffsetTop = dragRow.offsetTop;

            const [moved] = galleryState.splice(fromIdx, 1);
            galleryState.splice(toIdx, 0, moved);
            if (fromIdx < toIdx) {
              galleryListEl.insertBefore(dragRow, targetRow.nextSibling);
            } else {
              galleryListEl.insertBefore(dragRow, targetRow);
            }
            syncGalleryDomIndices();

            baseShift -= (dragRow.offsetTop - oldOffsetTop);
            Array.from(galleryListEl.querySelectorAll(".gallery-builder-row")).forEach((r) => {
              if (r === dragRow) return;
              const prevTop = beforeTops.get(r);
              if (prevTop == null) return;
              const delta = prevTop - r.getBoundingClientRect().top;
              if (!delta) return;
              r.style.transition = "none";
              r.style.transform = `translate3d(0, ${delta}px, 0)`;
              void r.offsetHeight;
              r.style.transition = "transform 220ms cubic-bezier(0.22, 1, 0.36, 1)";
              r.style.transform = "";
            });
          };

          const onPointerMove = (moveEv) => {
            if (moveEv.pointerId !== e.pointerId) return;
            currentTy = (moveEv.clientY - pointerY0) + baseShift;
            dragRow.style.transform = `translate3d(0, ${currentTy}px, 0)`;

            let guard = 0;
            while (guard < galleryState.length) {
              guard += 1;
              const rows = Array.from(galleryListEl.querySelectorAll(".gallery-builder-row"));
              const curIdx = rows.indexOf(dragRow);
              if (curIdx === -1) break;
              const dragCenterY = dragRow.offsetTop + currentTy + dragRow.offsetHeight / 2;

              if (curIdx > 0) {
                const prevRow = rows[curIdx - 1];
                if (dragCenterY < prevRow.offsetTop + prevRow.offsetHeight / 2) {
                  swapDuringDrag(curIdx, curIdx - 1);
                  currentTy = (moveEv.clientY - pointerY0) + baseShift;
                  dragRow.style.transform = `translate3d(0, ${currentTy}px, 0)`;
                  continue;
                }
              }
              if (curIdx < rows.length - 1) {
                const nextRow = rows[curIdx + 1];
                if (dragCenterY > nextRow.offsetTop + nextRow.offsetHeight / 2) {
                  swapDuringDrag(curIdx, curIdx + 1);
                  currentTy = (moveEv.clientY - pointerY0) + baseShift;
                  dragRow.style.transform = `translate3d(0, ${currentTy}px, 0)`;
                  continue;
                }
              }
              break;
            }
          };

          const finishDrag = (endEv) => {
            if (endEv.pointerId !== e.pointerId) return;
            window.removeEventListener("pointermove", onPointerMove);
            window.removeEventListener("pointerup", finishDrag);
            window.removeEventListener("pointercancel", finishDrag);
            try { handle.releasePointerCapture(e.pointerId); } catch (_) {}

            dragRow.style.transition = "transform 180ms cubic-bezier(0.22, 1, 0.36, 1)";
            dragRow.style.transform = "";
            window.setTimeout(() => {
              dragRow.classList.remove("is-dragging");
              dragRow.style.transition = "";
              dragRow.style.transform = "";
            }, 190);
          };

          window.addEventListener("pointermove", onPointerMove);
          window.addEventListener("pointerup", finishDrag);
          window.addEventListener("pointercancel", finishDrag);
        });
      });
    };
    renderGalleryBuilder();

    const readOptimizedImageDataUrl = (file) => new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = (ev) => {
        const rawUrl = String(ev.target?.result || "");
        if (!rawUrl || rawUrl.length <= 350000) {
          resolve(rawUrl);
          return;
        }
        const img = new Image();
        img.onload = () => {
          try {
            const maxDim = 1280;
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

    const fileInput = document.getElementById("prop-photo-file");
    if (fileInput) {
      fileInput.onchange = async () => {
        const files = Array.from(fileInput.files || []);
        fileInput.value = "";
        if (!files.length) return;
        const urls = await Promise.all(files.map(readOptimizedImageDataUrl));
        let added = 0;
        files.forEach((file, idx) => {
          const url = urls[idx];
          if (!url) return;
          galleryState.push({ kind: "interior", label: file.name.replace(/\.[^.]+$/, ""), url });
          added += 1;
        });
        if (added > 0) renderGalleryBuilder();
      };
    }

    // Google Maps 座標ピッカー（スクロール拡大有効＋ピン設置時に逆ジオコーディングで住所自動入力）
    const mapStatusEl = document.getElementById("prop-map-status");
    const addrLineEl = document.getElementById("prop-address-line");
    let geocodeReqSeq = 0;

    const applyResolvedAddress = (resolved) => {
      if (!resolved) return;
      if (resolved.prefecture && prefEl) {
        prefEl.value = resolved.prefecture;
        validationCtrl?.markTouched(prefEl);
      }
      if (resolved.municipality && cityEl) {
        cityEl.value = resolved.municipality;
        validationCtrl?.markTouched(cityEl);
      }
      if (resolved.addressLine && addrLineEl) {
        addrLineEl.value = resolved.addressLine;
        validationCtrl?.markTouched(addrLineEl);
      }
      const fullTxt = `${resolved.prefecture || ""}${resolved.municipality || ""}${resolved.addressLine || ""}`;
      if (mapStatusEl && fullTxt) {
        mapStatusEl.textContent = `✓ ピン位置の住所を反映しました: ${fullTxt}`;
      }
    };

    const reverseGeocodeAndFill = (latLng) => {
      if (typeof google === "undefined" || !google.maps || !google.maps.Geocoder) {
        if (mapStatusEl) mapStatusEl.textContent = "ピンを設置しました（住所欄を手動でご入力ください）。";
        return;
      }
      const seq = ++geocodeReqSeq;
      if (mapStatusEl) mapStatusEl.textContent = "ピン位置の住所を取得しています…";
      new google.maps.Geocoder().geocode({ location: latLng }, (results, status) => {
        if (seq !== geocodeReqSeq) return;
        if (status === "OK" && results && results[0]) {
          const parts = extractJapaneseAddressParts(results[0]);
          if (parts.addressLine || parts.municipality || parts.prefecture) {
            applyResolvedAddress(parts);
            return;
          }
        }
        if (mapStatusEl) {
          mapStatusEl.textContent = "この地点の詳細住所を取得できなかったため、住所欄を手動でご確認ください。";
        }
      });
    };

    const placeOrMovePin = (latLng, shouldReverseGeocode = true) => {
      const lat = typeof latLng.lat === "function" ? latLng.lat() : Number(latLng.lat);
      const lng = typeof latLng.lng === "function" ? latLng.lng() : Number(latLng.lng);
      setVal("prop-lat", Math.round(lat * 1000000) / 1000000);
      setVal("prop-lng", Math.round(lng * 1000000) / 1000000);
      if (pickerMap) {
        if (!pickerMarker && typeof google !== "undefined" && google.maps?.Marker) {
          pickerMarker = new google.maps.Marker({ position: { lat, lng }, map: pickerMap, draggable: true });
          pickerMarker.addListener("dragend", (ev) => placeOrMovePin(ev.latLng, true));
        } else if (pickerMarker) {
          pickerMarker.setPosition({ lat, lng });
        }
      }
      if (shouldReverseGeocode) reverseGeocodeAndFill({ lat, lng });
    };

    const initPickerMap = () => {
      const mapEl = document.getElementById("prop-map-picker");
      if (!mapEl || typeof google === "undefined" || !google.maps || !google.maps.Map) return;
      const centerLat = hasSavedCoords ? Number(latVal) : 36.5610;
      const centerLng = hasSavedCoords ? Number(lngVal) : 139.8830;
      const pos = { lat: centerLat, lng: centerLng };
      pickerMap = new google.maps.Map(mapEl, {
        center: pos,
        zoom: hasSavedCoords ? 16 : 14,
        gestureHandling: "greedy",
        scrollwheel: true,
        mapTypeControl: false,
        streetViewControl: false,
        fullscreenControl: false
      });
      pickerMarker = null;
      if (hasSavedCoords) {
        placeOrMovePin(pos, false);
      }
      pickerMap.addListener("click", (ev) => {
        placeOrMovePin(ev.latLng, true);
      });
    };

    const geocodeBtn = document.getElementById("prop-geocode-btn");
    if (geocodeBtn) {
      geocodeBtn.onclick = () => {
        const addr = `${prefEl?.value || ""}${cityEl?.value || ""}${addrLineEl?.value || ""}`;
        if (!addr.trim()) {
          window.MachirikuApp?.showToast?.("住所を入力してから地図ピンを取得してください");
          return;
        }
        if (typeof google === "undefined" || !google.maps || !google.maps.Geocoder) {
          window.MachirikuApp?.showToast?.("地図APIの読み込み後に再試行してください");
          return;
        }
        new google.maps.Geocoder().geocode({ address: addr }, (results, status) => {
          if (status === "OK" && results && results[0]) {
            const loc = results[0].geometry.location;
            placeOrMovePin(loc, false);
            if (pickerMap) {
              pickerMap.setCenter(loc);
              pickerMap.setZoom(16);
            }
            if (mapStatusEl) mapStatusEl.textContent = `✓ 入力された住所の位置へピンを移動しました: ${addr}`;
            window.MachirikuApp?.showToast?.("住所から地図ピンの位置を更新しました");
          } else {
            window.MachirikuApp?.showToast?.("住所から位置を特定できませんでした。地図上をクリックして指定してください");
          }
        });
      };
    }

    initPropertyMapWait();
    function initPropertyMapWait() {
      if (typeof google !== "undefined" && google.maps && google.maps.Map) {
        initPickerMap();
        return;
      }
      let tries = 0;
      const t = setInterval(() => {
        tries += 1;
        if (typeof google !== "undefined" && google.maps && google.maps.Map) {
          clearInterval(t);
          initPickerMap();
        } else if (tries > 15) {
          clearInterval(t);
        }
      }, 400);
    }

    form.onsubmit = (e) => {
      e.preventDefault();
      const val = (id) => document.getElementById(id)?.value.trim() || "";

      const hasContract = Boolean((longCb && longCb.checked) || (popupCb && popupCb.checked));
      if (!hasContract) {
        const msg = "募集する契約形態を1つ以上選択してください。";
        if (contractErrEl) {
          contractErrEl.innerHTML = `<span role="img" aria-label="エラー">⚠</span> ${msg}`;
          contractErrEl.style.display = "block";
        }
        validationCtrl?.announce(msg);
        window.MachirikuApp?.showToast?.(msg);
        longCb?.focus();
        return;
      }

      if (popupCb && popupCb.checked && popupMinEl && popupMaxEl) {
        const minVal = Number(popupMinEl.value);
        const maxVal = Number(popupMaxEl.value);
        if (!isNaN(minVal) && !isNaN(maxVal) && maxVal < minVal) {
          popupMaxEl.setCustomValidity("最大利用日数は最低利用日数以上の値を入力してください。");
        } else {
          popupMaxEl.setCustomValidity("");
        }
      }

      if (!form.checkValidity()) {
        const invalidEl = validationCtrl?.focusFirstInvalid();
        if (invalidEl) {
          const msg = invalidEl.validationMessage || "入力内容を確認してください";
          validationCtrl?.announce(msg);
          window.MachirikuApp?.showToast?.(msg);
        } else {
          const hiddenInvalid = Array.from(form.elements).find(el => !el.disabled && el.validity && !el.validity.valid);
          const msg = hiddenInvalid?.validationMessage || "入力内容に不備があります。必須項目や数値形式をご確認ください。";
          validationCtrl?.announce(msg);
          window.MachirikuApp?.showToast?.(msg);
          hiddenInvalid?.scrollIntoView?.({ block: "center", behavior: "smooth" });
        }
        return;
      }

      const infra = {};
      (data.INFRASTRUCTURE_FACILITIES || []).forEach(item => {
        const cb = form.querySelector(`input[name="infra_${item.key}"]`);
        infra[item.key] = Boolean(cb && cb.checked);
      });
      const allowedIndustries = Array.from(form.querySelectorAll('input[name="allowedIndustries"]:checked')).map(cb => cb.value);

      const prefecture = val("prop-pref");
      const municipality = val("prop-city");
      const addressLine = val("prop-address-line");
      const bYear = val("prop-built-year");
      const bMonth = val("prop-built-month");
      const builtYear = bYear ? (bYear === "不明" ? "不明" : (bMonth ? `${bYear}${bMonth}` : bYear)) : "";

      const rawLat = parseFloat(val("prop-lat"));
      const rawLng = parseFloat(val("prop-lng"));

      const rawSqm = parseFloat(val("prop-area-sqm")) || 0;
      const rawTsubo = parseFloat(val("prop-area-tsubo"));
      const areaTsubo = (!isNaN(rawTsubo) && rawTsubo > 0)
        ? rawTsubo
        : (rawSqm > 0 ? Math.round((rawSqm / 3.30578) * 100) / 100 : 0);

      const payload = {
        title: val("prop-title"),
        prefecture,
        municipality,
        addressLine,
        address: `${prefecture}${municipality}${addressLine}`,
        access: val("prop-access"),
        rent: Number(val("prop-rent")) || 0,
        managementFee: val("prop-mgmt") === "" ? null : Number(val("prop-mgmt")),
        deposit: val("prop-deposit") || "掲載なし（要確認）",
        keyMoney: val("prop-key-money") || "掲載なし（要確認）",
        securityDeposit: val("prop-security-deposit") || "掲載なし（要確認）",
        areaSqm: rawSqm,
        areaTsubo,
        floorNumber: val("prop-floor-num"),
        totalFloors: val("prop-total-floors"),
        structure: val("prop-structure"),
        builtYear,
        handover: val("prop-handover"),
        permitStatus: val("prop-permit-status") || "UNKNOWN",
        listingStatus: val("prop-listing-status") || "OPEN",
        condition: val("prop-condition") || "業種相談可",
        description: val("prop-description"),
        listingUrl: val("prop-listing-url"),
        lat: !isNaN(rawLat) ? rawLat : 36.5610,
        lng: !isNaN(rawLng) ? rawLng : 139.8830,
        infrastructure: infra,
        allowedIndustries,
        contractTerms: {
          longEnabled: Boolean(longCb && longCb.checked),
          popupEnabled: Boolean(popupCb && popupCb.checked),
          minDays: Number(val("prop-popup-min-days")) || 3,
          maxDays: Number(val("prop-popup-max-days")) || 30,
          dailyRentYen: val("prop-popup-daily-rent") ? Number(val("prop-popup-daily-rent")) : null
        },
        gallery: galleryState
      };

      const submitBtn = document.getElementById("prop-submit-btn");
      const targetKey = isEdit ? DRAFT_EDIT_KEY : DRAFT_ADD_KEY;
      const draftData = isEdit
        ? { propId: baseProp.id, after: payload }
        : payload;

      try {
        window.sessionStorage.removeItem(targetKey);
        window.sessionStorage.setItem(targetKey, JSON.stringify(draftData));
        if (submitBtn) submitBtn.disabled = true;
        window.location.href = isEdit
          ? propUrl("edit-confirm.jsp", { id: baseProp.id })
          : propUrl("add-confirm.jsp");
      } catch (_) {
        if (submitBtn) submitBtn.disabled = false;
        const msg = "画像の容量が大きいため確認画面へ進めませんでした。写真の枚数を減らして再度お試しください。";
        validationCtrl?.announce(msg);
        window.MachirikuApp?.showToast?.(msg);
      }
    };
  }

  const PHOTO_KIND_LABELS = {
    exterior: "外観",
    interior: "室内",
    floorplan: "間取り",
    facility: "設備",
    surrounding: "周辺環境"
  };

  function formatConfirmRows(d) {
    const infraOn = (data.INFRASTRUCTURE_FACILITIES || []).filter(i => d.infrastructure?.[i.key]).map(i => i.label).join("・") || "なし";
    const allowedNames = (d.allowedIndustries || []).map(id => data.SUBCATS[id]?.name || id).join("、") || "制限なし・相談可";
    const statusMap = { OPEN: "公開中", DRAFT: "下書き保存", PAUSED: "一時停止", CONTRACTED: "契約済み" };
    const permitMap = { OBTAINED: "取得済み", NOT_OBTAINED: "未取得（相談可）", UNKNOWN: "要確認" };
    const popupLabel = d.contractTerms?.popupEnabled
      ? `可（${d.contractTerms.minDays || 3}〜${d.contractTerms.maxDays || 30}日 / 日額 ${d.contractTerms.dailyRentYen ? formatMoney(d.contractTerms.dailyRentYen) + "円" : "相談"}）`
      : "不可";

    const photos = (Array.isArray(d.gallery) ? d.gallery : []).filter(g => g && g.url && g.url !== PLACEHOLDER_IMG);
    const photosLabel = photos.length
      ? `${photos.length}枚（${photos.map(g => `${PHOTO_KIND_LABELS[g.kind] || "写真"}: ${g.label || "物件写真"}`).join("、")}）`
      : "なし";
    const photosSig = photos.map(g => `${g.kind || "exterior"}|${g.label || ""}|${g.url}`).join("||");

    return [
      { key: "title", label: "物件名称", value: d.title || "" },
      { key: "address", label: "所在地", value: d.address || "" },
      { key: "access", label: "交通・最寄駅", value: d.access || "" },
      { key: "rent", label: "月額賃料 / 共益費", value: `${formatMoney(d.rent)}円 / 共益費 ${d.managementFee == null ? "なし" : formatMoney(d.managementFee) + "円"}` },
      { key: "deposits", label: "敷金 / 礼金 / 保証金", value: `${d.deposit || "なし"} / ${d.keyMoney || "なし"} / ${d.securityDeposit || "なし"}` },
      { key: "area", label: "使用部分面積", value: `${d.areaSqm}㎡（${d.areaTsubo}坪）` },
      { key: "floor", label: "階数・構造・築年", value: `${d.floorNumber || d.floor || "-"} / ${d.totalFloors || "-"} ・ ${d.structure || "-"} ・ ${d.builtYear || "-"}` },
      { key: "popup", label: "ポップアップ短期出店", value: popupLabel },
      { key: "allowed", label: "希望・許可業種", value: allowedNames },
      { key: "infra", label: "基本インフラ設備", value: infraOn },
      { key: "permit", label: "飲食店営業許可", value: permitMap[d.permitStatus] || "要確認" },
      { key: "status", label: "公開状態", value: statusMap[d.listingStatus] || "公開中" },
      { key: "condition", label: "募集条件・メモ", value: d.condition || "" },
      { key: "photos", label: "物件写真", value: photosLabel, sig: photosSig },
      { key: "description", label: "物件アピール説明", value: d.description || "" }
    ];
  }

  function openConfirmPhotoLightbox(photos, startIndex) {
    if (!photos || !photos.length) return;
    const prev = document.getElementById("o05-photo-lightbox");
    if (prev) prev.remove();

    let current = ((Number(startIndex) || 0) % photos.length + photos.length) % photos.length;
    const hasMany = photos.length > 1;

    const dialog = document.createElement("dialog");
    dialog.id = "o05-photo-lightbox";
    dialog.className = "confirm-lightbox-dialog";
    dialog.setAttribute("closedby", "any");
    dialog.setAttribute("aria-label", "登録写真の拡大表示");
    dialog.innerHTML = `
      <div class="confirm-lightbox-frame">
        <div class="confirm-lightbox-stage">
          <img class="confirm-lightbox-img" alt="">
        </div>
        <div class="confirm-lightbox-meta">
          <span class="confirm-lightbox-tag"></span>
          <span class="confirm-lightbox-caption"></span>
          <span class="confirm-lightbox-counter"></span>
        </div>
        <div class="confirm-lightbox-toolbar">
          <button type="button" class="confirm-lightbox-btn confirm-lightbox-arrow-btn" data-dir="-1" aria-label="前の写真" title="前の写真"${hasMany ? "" : " hidden"}>
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 18l-6-6 6-6"/></svg>
          </button>
          <button type="button" class="confirm-lightbox-btn confirm-lightbox-close" autofocus aria-label="拡大表示を閉じる">閉じる</button>
          <button type="button" class="confirm-lightbox-btn confirm-lightbox-arrow-btn" data-dir="1" aria-label="次の写真" title="次の写真"${hasMany ? "" : " hidden"}>
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18l6-6-6-6"/></svg>
          </button>
        </div>
      </div>
    `;

    const imgEl = dialog.querySelector(".confirm-lightbox-img");
    const tagEl = dialog.querySelector(".confirm-lightbox-tag");
    const captionEl = dialog.querySelector(".confirm-lightbox-caption");
    const counterEl = dialog.querySelector(".confirm-lightbox-counter");

    const renderPhoto = (nextIdx) => {
      current = ((nextIdx % photos.length) + photos.length) % photos.length;
      const item = photos[current];
      const kindLabel = PHOTO_KIND_LABELS[item.kind] || "写真";
      const label = item.label || `物件写真 ${current + 1}`;
      imgEl.src = resolveAssetUrl(item.url);
      imgEl.alt = `${kindLabel}：${label}`;
      tagEl.textContent = kindLabel;
      captionEl.textContent = label;
      counterEl.textContent = `${current + 1} / ${photos.length}`;
    };

    const isPointInsideVisibleImage = (e) => {
      const r = imgEl.getBoundingClientRect();
      const natW = imgEl.naturalWidth;
      const natH = imgEl.naturalHeight;
      if (!natW || !natH || !r.width || !r.height) {
        return e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom;
      }
      const scale = Math.min(r.width / natW, r.height / natH);
      const drawnW = natW * scale;
      const drawnH = natH * scale;
      const left = r.left + (r.width - drawnW) / 2;
      const top = r.top + (r.height - drawnH) / 2;
      return e.clientX >= left && e.clientX <= left + drawnW && e.clientY >= top && e.clientY <= top + drawnH;
    };

    dialog.addEventListener("click", (e) => {
      const dirBtn = e.target.closest("[data-dir]");
      if (dirBtn) {
        renderPhoto(current + Number(dirBtn.dataset.dir));
        return;
      }
      if (e.target.closest(".confirm-lightbox-close")) {
        dialog.close();
        return;
      }
      if (e.detail === 0) return;
      if (e.target === imgEl) {
        if (!isPointInsideVisibleImage(e)) dialog.close();
        return;
      }
      if (!e.target.closest(".confirm-lightbox-btn, .confirm-lightbox-tag, .confirm-lightbox-caption, .confirm-lightbox-counter")) {
        dialog.close();
      }
    });

    if (!("closedBy" in HTMLDialogElement.prototype)) {
      const isOutside = (e) => {
        const r = dialog.getBoundingClientRect();
        return e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom;
      };
      let startedOutside = false;
      dialog.addEventListener("pointerdown", (e) => { startedOutside = isOutside(e); });
      dialog.addEventListener("pointerup", (e) => {
        if (startedOutside && isOutside(e)) dialog.close();
        startedOutside = false;
      });
    }

    const onKeyDown = (e) => {
      if (!hasMany) return;
      if (e.key === "ArrowLeft") {
        e.preventDefault();
        renderPhoto(current - 1);
      } else if (e.key === "ArrowRight") {
        e.preventDefault();
        renderPhoto(current + 1);
      }
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

  function hydrateO05() {
    const sectionEl = document.getElementById("o05-section");
    const emptyEl = document.getElementById("o05-empty-state");
    if (!sectionEl) return;

    let draft = null;
    try {
      const raw = window.sessionStorage.getItem(DRAFT_ADD_KEY);
      if (raw) draft = JSON.parse(raw);
    } catch (_) {}

    if (!draft) {
      sectionEl.hidden = true;
      if (emptyEl) emptyEl.hidden = false;
      return;
    }
    sectionEl.hidden = false;
    if (emptyEl) emptyEl.hidden = true;

    const rows = formatConfirmRows(draft);
    const tbody = document.getElementById("o05-confirm-tbody");
    if (tbody) {
      tbody.innerHTML = rows.map(r => `<tr><th scope="row">${escapeHtml(r.label)}</th><td>${escapeHtml(r.value)}</td></tr>`).join("");
    }
    const galleryPrev = document.getElementById("o05-gallery-preview");
    if (galleryPrev) {
      const photos = (draft.gallery || []).filter(g => g && g.url && g.url !== PLACEHOLDER_IMG);
      galleryPrev.innerHTML = photos.length
        ? photos.map((g, idx) => {
          const kindLabel = PHOTO_KIND_LABELS[g.kind] || "写真";
          const label = g.label || `物件写真 ${idx + 1}`;
          return `
          <button type="button" class="confirm-photo-item confirm-photo-btn" data-photo-idx="${idx}" title="クリックして拡大表示" aria-label="${escapeHtml(kindLabel)}：${escapeHtml(label)}を拡大表示">
            <img src="${escapeHtml(resolveAssetUrl(g.url))}" alt="">
            <span class="confirm-photo-label">${escapeHtml(label)}</span>
            <span class="confirm-photo-tag">${escapeHtml(kindLabel)}</span>
          </button>
        `;
        }).join("")
        : `<p class="sample-note">写真は登録されていません。</p>`;

      galleryPrev.onclick = photos.length
        ? (e) => {
          const btn = e.target.closest(".confirm-photo-btn");
          if (!btn || !galleryPrev.contains(btn)) return;
          openConfirmPhotoLightbox(photos, Number(btn.dataset.photoIdx || 0));
        }
        : null;
    }

    const backBtn = document.getElementById("o05-back-btn");
    if (backBtn) backBtn.setAttribute("href", propUrl("add.jsp", { restore: "1" }));

    const commitBtn = document.getElementById("o05-commit-btn");
    if (commitBtn) {
      commitBtn.onclick = () => {
        store.registerProperty(draft);
        try { window.sessionStorage.removeItem(DRAFT_ADD_KEY); } catch (_) {}
        window.MachirikuApp?.showToast?.("新規空き店舗を登録しました！");
        window.location.href = propUrl("manage.jsp");
      };
    }
  }

  function hydrateO07(params) {
    const sectionEl = document.getElementById("o07-section");
    const emptyEl = document.getElementById("o07-empty-state");
    if (!sectionEl) return;

    let payload = null;
    try {
      const raw = window.sessionStorage.getItem(DRAFT_EDIT_KEY);
      if (raw) payload = JSON.parse(raw);
    } catch (_) {}

    const beforeProp = payload?.before || (payload?.propId ? store.getPropertyById(payload.propId) : null);
    if (!payload || !beforeProp || !payload.after) {
      sectionEl.hidden = true;
      if (emptyEl) emptyEl.hidden = false;
      return;
    }
    sectionEl.hidden = false;
    if (emptyEl) emptyEl.hidden = true;

    const beforeRows = formatConfirmRows(beforeProp);
    const afterRows = formatConfirmRows(payload.after);
    const tbody = document.getElementById("o07-confirm-tbody");
    let diffCount = 0;

    if (tbody) {
      tbody.innerHTML = afterRows.map((ar, idx) => {
        const br = beforeRows[idx] || { value: "" };
        const changed = br.value !== ar.value || (ar.sig !== undefined && br.sig !== ar.sig);
        if (changed) diffCount += 1;
        return `
          <tr class="${changed ? "diff-changed-row" : ""}">
            <th scope="row">${escapeHtml(ar.label)} ${changed ? '<span class="diff-badge">変更あり</span>' : ""}</th>
            <td class="diff-before">${escapeHtml(br.value || "-")}</td>
            <td class="diff-after">${escapeHtml(ar.value || "-")}</td>
          </tr>
        `;
      }).join("");
    }

    setText("o07-diff-summary", diffCount > 0 ? `${diffCount}項目が変更されています（緑色でハイライト表示）` : "各項目の変更はありません（そのまま確定できます）");

    const backBtn = document.getElementById("o07-back-btn");
    if (backBtn) backBtn.setAttribute("href", propUrl("edit.jsp", { id: payload.propId, restore: "1" }));

    const commitBtn = document.getElementById("o07-commit-btn");
    if (commitBtn) {
      commitBtn.onclick = () => {
        store.updateProperty(payload.propId, payload.after);
        try { window.sessionStorage.removeItem(DRAFT_EDIT_KEY); } catch (_) {}
        window.MachirikuApp?.showToast?.("物件情報を更新しました！");
        window.location.href = propUrl("manage.jsp");
      };
    }
  }

  // =========================================================================
  // O08 / O09: delete-confirm.jsp / delete-complete.jsp
  // =========================================================================
  function hydrateO08(params) {
    const propId = params?.id || "";
    const prop = store.getPropertyById(propId);
    const sectionEl = document.getElementById("o08-section");
    const emptyEl = document.getElementById("o08-empty-state");
    if (!sectionEl) return;

    if (!prop) {
      sectionEl.hidden = true;
      if (emptyEl) emptyEl.hidden = false;
      return;
    }
    sectionEl.hidden = false;
    if (emptyEl) emptyEl.hidden = true;

    showRoleNotice("o08-role-notice", ["owner", "admin"], "物件オーナー");
    setText("o08-prop-title", prop.title);
    setText("o08-prop-code", prop.code || prop.id);
    setText("o08-prop-address", prop.address || "");
    setText("o08-prop-rent", `${formatRent(prop.rent)}万円 / 月（${prop.areaTsubo}坪）`);
    setText("o08-prop-status", prop.statusLabel || "募集中");

    const impact = store.getPropertyDeletionImpact(prop.id);
    setText("o08-impact-votes", `${impact.votesCount} 票`);
    setText("o08-impact-apps", `${impact.activeAppsCount} 件（全${impact.appsCount}件）`);
    setText("o08-impact-follows", `${impact.followsCount} 人`);

    const checkEl = document.getElementById("o08-confirm-check");
    const deleteBtn = document.getElementById("o08-delete-btn");
    if (checkEl && deleteBtn) {
      deleteBtn.disabled = !checkEl.checked;
      checkEl.onchange = () => {
        deleteBtn.disabled = !checkEl.checked;
      };
      deleteBtn.onclick = () => {
        if (!checkEl.checked) return;
        const receipt = {
          id: prop.id,
          code: prop.code || prop.id,
          title: prop.title,
          deletedAt: new Date().toLocaleString("ja-JP")
        };
        store.deleteProperty(prop.id);
        try { window.sessionStorage.setItem(DELETE_RECEIPT_KEY, JSON.stringify(receipt)); } catch (_) {}
        window.location.href = propUrl("delete-complete.jsp", { title: prop.title });
      };
    }
  }

  function hydrateO09(params) {
    let receipt = null;
    try {
      const raw = window.sessionStorage.getItem(DELETE_RECEIPT_KEY);
      if (raw) receipt = JSON.parse(raw);
    } catch (_) {}

    const title = receipt?.title || params?.title || "対象物件";
    const code = receipt?.code || "-";
    const deletedAt = receipt?.deletedAt || new Date().toLocaleString("ja-JP");
    setText("o09-deleted-title", title);
    setText("o09-deleted-code", code);
    setText("o09-deleted-at", deletedAt);
  }

  function hydrate(screenId, params = {}) {
    switch (String(screenId || "").toLowerCase()) {
      case "r01": return hydrateR01(params);
      case "b01": return hydrateB01(params);
      case "o01": return hydrateO01(params);
      case "o02": return hydrateO02(params);
      case "o03": return hydrateO03(params);
      case "o04": return initPropertyForm("add", params);
      case "o05": return hydrateO05(params);
      case "o06": return initPropertyForm("edit", params);
      case "o07": return hydrateO07(params);
      case "o08": return hydrateO08(params);
      case "o09": return hydrateO09(params);
      default: break;
    }
  }

  return {
    hydrate,
    toggleStatus,
    toggleApplyDetail,
    setCheckStatus,
    updateApp
  };
})();
