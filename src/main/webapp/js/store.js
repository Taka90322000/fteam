/**
 * マチリク (Machiriku) - 状態管理ストア & 業務ロジック
 * 準拠: 企画書、テーブル設計書3、画面設計書33画面
 */
window.MachirikuStore = (function() {
  const data = window.MACHIRIKU_DATA;

  // ローカルストレージキー（再読み込み時にも状態維持、リセット可能）
  // A new source snapshot must not be hidden by the previous mock dataset.
  // Previous keys are left intact; only the active dataset's state is loaded.
  const STORAGE_KEY = `machiriku_app_state_${data.DATASET_VERSION}`;

  // 初期ステート
  function getInitialState() {
    const usersMap = {
      resident: JSON.parse(JSON.stringify(data.USERS.resident)),
      business: JSON.parse(JSON.stringify(data.USERS.business)),
      owner: JSON.parse(JSON.stringify(data.USERS.owner)),
      admin: JSON.parse(JSON.stringify(data.USERS.admin))
    };
    return {
      datasetVersion: data.DATASET_VERSION,
      currentRole: "resident", // resident, business, owner, admin
      users: usersMap,
      currentUser: usersMap.resident,
      properties: JSON.parse(JSON.stringify(data.PROPERTIES)),
      applications: JSON.parse(JSON.stringify(data.INITIAL_APPLICATIONS)),
      surveys: JSON.parse(JSON.stringify(data.INITIAL_SURVEYS)),
      coupons: JSON.parse(JSON.stringify(data.INITIAL_COUPONS)),
      notifications: [],
      adminReports: [],
      filters: {
        keyword: "",
        pref: "栃木",
        cities: ["宇都宮市"],
        category: "all",
        categories: [],
        rentMin: 0,
        rentMax: 500000,
        includeMgmtFee: false,
        noDeposit: false,
        sort: "score_desc"
      }
    };
  }

  let persistenceAvailable = true;

  function loadState() {
    const initial = getInitialState();
    try {
      if (!window.localStorage) { persistenceAvailable = false; return initial; }
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (!raw) return initial;
      const saved = JSON.parse(raw);
      const collections = ['properties', 'applications', 'surveys', 'coupons', 'notifications'];
      if (!saved || saved.datasetVersion !== data.DATASET_VERSION || collections.some(key => !Array.isArray(saved[key]))) return initial;
      if (collections.some(key => saved[key].some(item => !item || typeof item.id !== 'string'))) return initial;
      if (!saved.users || Object.keys(initial.users).some(role => !saved.users[role] || saved.users[role].role !== role)) return initial;
      const currentRole = saved.users[saved.currentRole] ? saved.currentRole : 'resident';
      // Upgrade previous covers or external URLs to local assets without resetting votes or follows.
      for (const prop of saved.properties) {
        if (typeof prop.statusLabel === 'string') {
          const sm = prop.statusLabel.match(/^出店決定・開店準備中[（(](.+)[）)]$/);
          if (sm) {
            if (!prop.contractedStoreName) prop.contractedStoreName = sm[1];
            prop.statusLabel = '出店決定・開店準備中';
          }
        }
        const source = data.PROPERTIES.find(item => item.id === prop.id);
        if (source) {
          // Photo-only catalog updates must reach saved previews without losing interaction data,
          // unless the owner has explicitly customized/cleared photos for this property.
          if (source.galleryVersion && prop.galleryVersion !== source.galleryVersion && !prop.photosEdited) {
            prop.gallery = JSON.parse(JSON.stringify(source.gallery));
            prop.galleryVersion = source.galleryVersion;
          }
          if (Array.isArray(prop.gallery)) {
            prop.gallery = prop.gallery.filter(g => g && g.url && g.url !== 'assets/property-placeholder.svg');
          }
          const ownerClearedPhotos = Boolean(
            (prop.photosEdited || prop.ownerId || prop.coverImage === 'assets/property-placeholder.svg') &&
            Array.isArray(prop.gallery) &&
            prop.gallery.length === 0
          );
          // If the property is not an owner-created custom listing, synchronize coverImage and access with the catalog master.
          if (!ownerClearedPhotos && !prop.photosEdited && (!prop.ownerId || !prop.coverImage || prop.coverImage === 'assets/property-placeholder.svg')) {
            prop.coverImage = source.coverImage;
            prop.imageCaption = source.imageCaption;
          }
          if (source.access) {
            prop.access = source.access;
          }
        } else {
          if (Array.isArray(prop.gallery)) {
            prop.gallery = prop.gallery.filter(g => (
              g && g.url && g.url !== 'assets/property-placeholder.svg' &&
              !(g.url === 'assets/properties/prop-orion-01/01_exterior.jpg' && g.label === '外観写真') &&
              !(g.url === 'assets/properties/prop-orion-01/02_interior.jpg' && g.label === '店内サンプル写真')
            ));
          } else {
            prop.gallery = [];
          }
          const firstExt = prop.gallery.find(g => g.kind === 'exterior') || prop.gallery[0];
          prop.coverImage = (firstExt && firstExt.url) || 'assets/property-placeholder.svg';
        }
      }
      // マスタに追加された新規物件（仮想物件など）を saved.properties にマージ
      for (const masterProp of data.PROPERTIES) {
        if (!saved.properties.some(p => p.id === masterProp.id)) {
          saved.properties.push(JSON.parse(JSON.stringify(masterProp)));
        }
      }
      const loadedFilters = { ...initial.filters, ...saved.filters };
      if (!loadedFilters.cities || loadedFilters.cities.length === 0) {
        if (!loadedFilters.pref || loadedFilters.pref === 'all' || loadedFilters.pref === '栃木') {
          loadedFilters.pref = '栃木';
          loadedFilters.cities = ['宇都宮市'];
        }
      }
      if (saved.users.business && initial.users.business) {
        if (!saved.users.business.businessName) {
          saved.users.business.businessName = initial.users.business.businessName || initial.users.business.company || "";
        }
        if (!Array.isArray(saved.users.business.preferredIndustries) || !saved.users.business.preferredIndustries.length) {
          saved.users.business.preferredIndustries = [...(initial.users.business.preferredIndustries || [])];
        }
      }
      return { ...initial, ...saved, currentRole, currentUser: saved.users[currentRole], filters: loadedFilters };
    } catch (_) {
      // Damaged or unavailable local storage must never prevent previewing a screen.
      persistenceAvailable = false;
      return initial;
    }
  }

  let state = loadState();

  function saveState() {
    try {
      if (!window.localStorage) { persistenceAvailable = false; return; }
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
      persistenceAvailable = true;
    } catch (_) { persistenceAvailable = false; }
  }
  saveState();

  // イベントリスナー
  const listeners = [];

  function subscribe(fn) {
    listeners.push(fn);
    return () => {
      const idx = listeners.indexOf(fn);
      if (idx !== -1) listeners.splice(idx, 1);
    };
  }

  function notify(type = 'data', detail = {}) {
    saveState();
    listeners.forEach(fn => fn(state, { type, ...detail }));
  }

  function resetState() {
    state = getInitialState();
    notify('reset');
  }

  // ロール切り替え（各ロールの作業状態・投票・所持クーポン・登録物件を維持）
  function setRole(role) {
    if (!state.users[role]) return;
    state.currentRole = role;
    state.currentUser = state.users[role];
    notify('role');
  }

  // 物件取得
  function getProperties() {
    return state.properties;
  }

  function getPropertyById(id) {
    return state.properties.find(p => p.id === id);
  }

  // フィルタリング済み物件一覧
  function getFilteredProperties(overrideFilters) {
    let list = [...state.properties];
    const filters = overrideFilters ? { ...state.filters, ...overrideFilters } : state.filters;
    const { keyword, area, pref, cities, category, categories, rentMin, rentMax, includeMgmtFee, noDeposit, sort } = filters;

    if (keyword && keyword.trim()) {
      const kw = keyword.trim().toLowerCase();
      list = list.filter(p => 
        [p.title, p.address, p.subtitle, p.condition, p.access, p.description].some(value => String(value || '').toLowerCase().includes(kw))
      );
    }

    if (pref && pref !== "all") {
      list = list.filter(p => p.address && p.address.includes(pref));
    }

    if (cities && Array.isArray(cities) && cities.length > 0) {
      list = list.filter(p => cities.some(city => p.address && p.address.includes(city)));
    }



    if (categories && Array.isArray(categories) && categories.length > 0) {
      list = list.filter(p => {
        return p.topDemands && p.topDemands.some(d => {
          const sub = data.SUBCATS[d.subcatId];
          return sub && (categories.includes(sub.categoryId) || categories.includes(sub.id));
        });
      });
    } else if (category && category !== "all") {
      list = list.filter(p => {
        return p.topDemands && p.topDemands.some(d => {
          const sub = data.SUBCATS[d.subcatId];
          return sub && (sub.categoryId === category || sub.id === category);
        });
      });
    }

    if (rentMin != null && rentMin > 0) {
      list = list.filter(p => {
        const effRent = includeMgmtFee ? (p.rent + (Number(p.managementFee) || 0)) : p.rent;
        return effRent >= rentMin;
      });
    }

    if (rentMax != null && rentMax > 0 && rentMax < 50000000) {
      list = list.filter(p => {
        const effRent = includeMgmtFee ? (p.rent + (Number(p.managementFee) || 0)) : p.rent;
        return effRent <= rentMax;
      });
    }

    if (noDeposit) {
      list = list.filter(p => {
        const depStr = String(p.deposit || '').toLowerCase();
        const keyStr = String(p.keyMoney || '').toLowerCase();
        const noDep = depStr.includes('なし') || depStr === '0' || !p.deposit || p.deposit === 0;
        const noKey = keyStr.includes('なし') || keyStr === '0' || !p.keyMoney || p.keyMoney === 0;
        return noDep && noKey;
      });
    }

    // ソート処理（PR枠は最優先ピン留め）
    list.sort((a, b) => {
      if (a.isPr && !b.isPr) return -1;
      if (!a.isPr && b.isPr) return 1;

      if (sort === "score_desc") return b.demandScore - a.demandScore;
      if (sort === "votes_desc") return b.votesCount - a.votesCount;
      if (sort === "rent_asc") return a.rent - b.rent;
      if (sort === "rent_desc") return b.rent - a.rent;
      if (sort === "area_desc") return b.areaSqm - a.areaSqm;
      return 0;
    });

    return list;
  }

  function setFilters(newFilters, options = {}) {
    state.filters = { ...state.filters, ...newFilters };
    notify('filters', options);
  }

  // お気に入り/フォロー切り替え
  function toggleFollowProperty(propId) {
    if (!state.currentUser.followedProps) state.currentUser.followedProps = [];
    const idx = state.currentUser.followedProps.indexOf(propId);
    if (idx !== -1) {
      state.currentUser.followedProps.splice(idx, 1);
    } else {
      state.currentUser.followedProps.push(propId);
    }
    notify('follow', { propId });
  }

  // 2地点間の概算距離 (km)
  function calcDistanceKm(lat1, lng1, lat2, lng2) {
    if ([lat1, lng1, lat2, lng2].some(v => typeof v !== 'number' || isNaN(v))) return null;
    const rad = Math.PI / 180;
    const dLat = (lat2 - lat1) * rad;
    const dLng = (lng2 - lng1) * rad;
    const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLng / 2) ** 2;
    return Math.round(6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 100) / 100;
  }

  // 順位投票実行 (S03 / R01)
  // 計算根拠: テーブル設計書 設計補足
  // 明細スコア = rank_weight / (1 + distance_km / distance_scale_km)
  function submitVote(propId, ranks, surveyData) {
    const prop = getPropertyById(propId);
    if (!prop) return { success: false, message: "物件が見つかりません" };

    // 重複業種の除去 & 有効値フィルタ
    const cleanRanks = [];
    const seen = new Set();
    (ranks || []).forEach(r => {
      if (r && !seen.has(r)) {
        seen.add(r);
        cleanRanks.push(r);
      }
    });

    if (cleanRanks.length === 0) {
      return { success: false, message: "第1希望の業種を選択してください" };
    }

    const userLocations = state.currentUser.locations || (data.USERS?.resident?.locations || []);
    const selectedLoc = userLocations.find(l => l.id === surveyData?.locationId) || userLocations[0];
    let distanceKm = state.currentUser.livingDistanceKm || 0.4;
    if (selectedLoc && prop.location) {
      const computed = calcDistanceKm(selectedLoc.lat, selectedLoc.lng, prop.location.lat, prop.location.lng);
      if (computed != null) distanceKm = computed;
    }
    const distanceScale = Number(prop.distanceScaleKm) || 1.5;
    const distanceFactor = 1 / (1 + distanceKm / distanceScale);
    const weights = [1.0, 0.6, 0.3];

    // 同一ユーザーが既に同一物件に投票済みかチェック（二重カウント・スコア無限増殖の防止）
    const curVote = state.currentUser.votedProps?.[propId];
    const isUpdate = Boolean(curVote);

    const rankKeys = ["rank1Votes", "rank2Votes", "rank3Votes"];

    // 既存投票がある場合は過去の加算スコア・票数を減算して再集計
    if (isUpdate && curVote.prevRanks && curVote.prevScoreAdded !== undefined) {
      prop.demandScore = Math.max(0, Math.round((prop.demandScore - curVote.prevScoreAdded) * 10) / 10);
      curVote.prevRanks.forEach((prevSubcat, idx) => {
        const prevWeight = weights[idx] || 0.3;
        const prevFactor = curVote.prevDistanceFactor || distanceFactor;
        const prevSubScore = Math.round(prevWeight * prevFactor * 10) / 10;
        const dem = prop.topDemands.find(d => d.subcatId === prevSubcat);
        if (dem) {
          dem.votes = Math.max(0, dem.votes - 1);
          const rk = rankKeys[idx];
          if (rk) {
            dem[rk] = Math.max(0, (dem[rk] || 0) - 1);
          }
          dem.score = Math.max(0, Math.round((dem.score - prevSubScore) * 10) / 10);
        }
      });
    } else {
      prop.votesCount += 1;
    }

    let addedTotalScore = 0;
    cleanRanks.forEach((subcatId, idx) => {
      const weight = weights[idx] || 0.3;
      const scoreAdded = Math.round(weight * distanceFactor * 10) / 10;
      addedTotalScore += scoreAdded;

      // 物件の需要ランキング集計を更新
      let targetDemand = prop.topDemands.find(d => d.subcatId === subcatId);
      if (!targetDemand) {
        const sub = data.SUBCATS[subcatId];
        targetDemand = {
          subcatId: subcatId,
          votes: 0,
          rank1Votes: 0,
          rank2Votes: 0,
          rank3Votes: 0,
          score: 0,
          label: sub ? sub.name : subcatId
        };
        prop.topDemands.push(targetDemand);
      }
      targetDemand.votes += 1;
      const rk = rankKeys[idx];
      if (rk) {
        targetDemand[rk] = (targetDemand[rk] || 0) + 1;
      }
      targetDemand.score = Math.round((targetDemand.score + scoreAdded) * 10) / 10;
    });

    // 順位の再ソート
    prop.topDemands.sort((a, b) => b.score - a.score);
    prop.demandScore = Math.round((prop.demandScore + addedTotalScore) * 10) / 10;

    // 住民の声を追加（再投票時は前回の同コメントを置換、自由コメントがあれば追加）
    if (isUpdate && curVote.comment && curVote.comment.trim() && Array.isArray(prop.residentVoices)) {
      const oldIdx = prop.residentVoices.findIndex(v => v.text === curVote.comment.trim() && v.date === "本日");
      if (oldIdx !== -1) prop.residentVoices.splice(oldIdx, 1);
    }
    if (surveyData && surveyData.comment && surveyData.comment.trim()) {
      prop.residentVoices.unshift({
        author: `${state.currentUser.name}（${selectedLoc ? selectedLoc.label : state.currentUser.livingArea}）`,
        text: surveyData.comment.trim(),
        date: "本日"
      });
    }

    // ユーザー投票履歴を記録（過去の加算値を保持して安全な再投票に対応）
    if (!state.currentUser.votedProps) state.currentUser.votedProps = {};
    state.currentUser.votedProps[propId] = {
      ranks: [...cleanRanks],
      prevRanks: [...cleanRanks],
      prevScoreAdded: addedTotalScore,
      prevDistanceFactor: distanceFactor,
      locationId: selectedLoc ? selectedLoc.id : "loc-home",
      locationLabel: selectedLoc ? selectedLoc.label : "自宅",
      distanceKm: distanceKm,
      date: new Date().toISOString().slice(0, 10),
      comment: surveyData?.comment || "",
      frequency: surveyData?.frequency || "",
      timing: surveyData?.timing || ""
    };

    // 投票感謝通知とクーポン発行
    state.notifications.unshift({
      id: "notif-" + Date.now(),
      type: "vote",
      title: `【投票完了】${prop.title}への投票が反映されました`,
      body: `第1希望「${data.SUBCATS[cleanRanks[0]]?.name || ''}」で投票しました。進捗通知を受け取れます。`,
      date: "たった今",
      linkScreen: "s02",
      isRead: false
    });

    notify();
    return { success: true, addedScore: addedTotalScore, newTotalScore: prop.demandScore, isUpdate, distanceKm };
  }

  // 投票取り下げ (R01 / property_votes.withdrawn_at 相当)
  function withdrawVote(propId) {
    const prop = getPropertyById(propId);
    const curVote = state.currentUser.votedProps?.[propId];
    if (!prop || !curVote) return { success: false, message: "取り下げる投票が見つかりません" };

    const weights = [1.0, 0.6, 0.3];
    const rankKeys = ["rank1Votes", "rank2Votes", "rank3Votes"];
    if (curVote.prevScoreAdded !== undefined) {
      prop.demandScore = Math.max(0, Math.round((prop.demandScore - curVote.prevScoreAdded) * 10) / 10);
    }
    (curVote.prevRanks || []).forEach((prevSubcat, idx) => {
      const prevWeight = weights[idx] || 0.3;
      const prevFactor = curVote.prevDistanceFactor || 1;
      const prevSubScore = Math.round(prevWeight * prevFactor * 10) / 10;
      const dem = prop.topDemands.find(d => d.subcatId === prevSubcat);
      if (dem) {
        dem.votes = Math.max(0, dem.votes - 1);
        const rk = rankKeys[idx];
        if (rk) dem[rk] = Math.max(0, (dem[rk] || 0) - 1);
        dem.score = Math.max(0, Math.round((dem.score - prevSubScore) * 10) / 10);
      }
    });
    prop.votesCount = Math.max(0, (prop.votesCount || 1) - 1);
    if (curVote.comment && curVote.comment.trim() && Array.isArray(prop.residentVoices)) {
      const voiceIdx = prop.residentVoices.findIndex(v => v.text === curVote.comment.trim() && v.date === "本日");
      if (voiceIdx !== -1) prop.residentVoices.splice(voiceIdx, 1);
    }
    prop.topDemands.sort((a, b) => b.score - a.score);
    delete state.currentUser.votedProps[propId];
    notify();
    return { success: true };
  }

  // 出店応募送信 (S08 / B01)
  function submitApplication(appData) {
    const bizName = appData.businessName || state.currentUser.businessName || state.currentUser.company || "";
    const plannedStoreName = appData.plannedStoreName || "";
    const displayStoreName = plannedStoreName || appData.storeName || bizName || "新規店舗";
    const contractKind = appData.contractKind || "LONG";
    const newApp = {
      id: "app-" + Date.now(),
      propId: appData.propId,
      propTitle: appData.propTitle,
      applicantId: state.currentUser.id,
      applicantName: state.currentUser.name + (bizName ? `（${bizName}）` : ''),
      businessName: bizName,
      plannedStoreName: plannedStoreName,
      genreId: appData.genreId,
      genreName: data.SUBCATS[appData.genreId]?.name || "飲食店",
      storeName: displayStoreName,
      targetRent: Number(appData.targetRent) || 0,
      targetOpenDate: appData.targetOpenDate || appData.requestedStartOn || "",
      message: appData.message || appData.concept || "",
      concept: appData.concept || appData.message || "",
      contractKind: contractKind,
      requestedStartOn: appData.requestedStartOn || appData.targetOpenDate || "",
      requestedEndOn: contractKind === "POPUP" ? (appData.requestedEndOn || "") : "",
      viewingRequestedAt: appData.viewingRequestedAt || "",
      experienceYears: appData.experienceYears || "",
      checks: Array.isArray(appData.checks) ? appData.checks : [],
      ownerReply: "",
      repliedAt: null,
      status: "submitted",
      statusLabel: "新規応募受付中",
      submittedAt: new Date().toLocaleString("ja-JP"),
      hasDocuments: true
    };

    state.applications.unshift(newApp);

    // 物件ステータスを更新
    const prop = getPropertyById(appData.propId);
    if (prop && (prop.status === "voting" || prop.status === "listed")) {
      prop.status = "reviewing";
      prop.statusLabel = "出店応募あり・審査中";
    }

    // オーナー宛通知
    state.notifications.unshift({
      id: "notif-" + Date.now(),
      type: "application",
      title: `【新規出店応募】${displayStoreName}様より応募がありました`,
      body: `${prop?.title}に新しい出店申込が届きました。内容をご確認ください。`,
      date: "たった今",
      linkScreen: "o03",
      isRead: false
    });

    notify();
    return newApp;
  }

  // 応募ステータス・返信更新 (O03)
  function updateApplicationStatus(appId, newStatus, ownerReply) {
    const app = state.applications.find(a => a.id === appId);
    if (!app) return null;
    app.status = newStatus;
    if (ownerReply !== undefined) {
      app.ownerReply = ownerReply;
      app.repliedAt = new Date().toLocaleString("ja-JP");
    }

    if (newStatus === "consulting" || newStatus === "interviewing") {
      app.status = "consulting";
      app.statusLabel = "面談・相談中";
    } else if (newStatus === "approved") {
      app.statusLabel = "出店合意（契約調整へ）";
    } else if (newStatus === "contracted") {
      app.statusLabel = "契約成立・引渡し完了";
      const prop = getPropertyById(app.propId);
      if (prop) {
        prop.status = "preparing";
        prop.listingStatus = "CONTRACTED";
        prop.statusLabel = "出店決定・開店準備中";
        if (app.storeName) prop.contractedStoreName = app.storeName;
      }
    } else if (newStatus === "rejected") {
      app.statusLabel = "見送り";
    } else if (newStatus === "withdrawn") {
      app.statusLabel = "応募辞退";
    }

    notify();
    return app;
  }

  // 応募要件チェック確認更新 (O03 / application_checks 準拠)
  function updateApplicationCheck(appId, qualificationId, ownerCheckStatus, checkNote) {
    const app = state.applications.find(a => a.id === appId);
    if (!app || !Array.isArray(app.checks)) return null;
    const chk = app.checks.find(c => c.qualificationId === qualificationId);
    if (!chk) return null;
    chk.ownerCheckStatus = ownerCheckStatus;
    if (checkNote !== undefined) chk.checkNote = checkNote;
    notify();
    return chk;
  }

  // 出店計画公開 (S15)
  function publishStorePlan(planData) {
    const prop = getPropertyById(planData.propId);
    if (prop) {
      prop.status = planData.isOpenNow ? "opened" : "preparing";
      prop.statusLabel = planData.isOpenNow ? "営業中（アンケート＆クーポン実施中）" : `開店準備中（${planData.openDate} OPEN予定）`;
      prop.storePlan = {
        storeName: planData.storeName,
        operatorName: state.currentUser.name,
        progressStatus: planData.progressStatus || "PREPARING",
        concept: planData.concept,
        openDate: planData.openDate,
        message: planData.message
      };
    }

    // 全体通知
    state.notifications.unshift({
      id: "notif-" + Date.now(),
      type: "progress",
      title: `【開店計画公開】『${planData.storeName}』の出店準備が始まりました！`,
      body: `${planData.openDate}にオープン予定です。応援メッセージを送りましょう。`,
      date: "たった今",
      linkScreen: "s05",
      isRead: false
    });

    notify();
  }

  // 来店アンケート作成 (S23)
  function createSurvey(surveyData) {
    const newSurvey = {
      id: "srv-" + Date.now(),
      propId: surveyData.propId,
      storeName: surveyData.storeName,
      title: surveyData.title,
      description: surveyData.description,
      status: "published",
      statusLabel: "回答受付中",
      ratingAvg: 0,
      responseCount: 0,
      createdAt: new Date().toISOString().slice(0, 10),
      rewardCouponId: surveyData.rewardCouponId || null,
      questions: surveyData.questions,
      responses: []
    };

    state.surveys.unshift(newSurvey);
    const businessUser = state.users.business;
    if (!businessUser.activeSurveys) businessUser.activeSurveys = [];
    businessUser.activeSurveys.push(newSurvey.id);
    notify();
    return newSurvey;
  }

  // 来店アンケート回答送信 (S26)
  function submitSurveyResponse(surveyId, responseData) {
    const srv = state.surveys.find(s => s.id === surveyId);
    if (!srv) return { success: false, message: "アンケートが見つかりません" };

    const rating = Number(responseData.rating) || 5;
    const newResponse = {
      rating: rating,
      q2: responseData.q2 || "",
      q3: Number(responseData.q3) || rating,
      comment: responseData.comment || "",
      age: responseData.age || "30代",
      gender: responseData.gender || "回答なし",
      area: responseData.area || "宇都宮市",
      date: "本日"
    };

    srv.responses.unshift(newResponse);
    srv.responseCount = srv.responses.length;
    
    // 平均満足度を再計算
    const totalRating = srv.responses.reduce((sum, r) => sum + r.rating, 0);
    srv.ratingAvg = Math.round((totalRating / srv.responses.length) * 10) / 10;

    // 事業主プロフィールの平均評価を連動更新 (S32)
    const bizUser = state.users.business;
    bizUser.avgSatisfaction = srv.ratingAvg;
    bizUser.reviewCount = (bizUser.reviewCount || 0) + 1;

    // 回答者（田中）にアンケートクーポンを付与 (S30)
    const reward = state.coupons.find(c => c.id === srv.rewardCouponId);
    if (reward) {
      if (!state.currentUser.receivedCoupons) state.currentUser.receivedCoupons = [];
      if (!state.currentUser.receivedCoupons.includes(srv.rewardCouponId)) {
        state.currentUser.receivedCoupons.push(srv.rewardCouponId);
      }
    }

    if (!state.currentUser.answeredSurveys) state.currentUser.answeredSurveys = [];
    state.currentUser.answeredSurveys.push(surveyId);

    // 完了通知
    state.notifications.unshift({
      id: "notif-" + Date.now(),
      type: reward ? 'coupon' : 'survey',
      title: 'アンケートへのご回答ありがとうございました',
      body: reward ? `『${srv.storeName}』で使える${reward.discountValue}クーポンを獲得しました。` : `『${srv.storeName}』への回答を受け付けました。`,
      date: "たった今",
      linkScreen: reward ? 's30' : 's26',
      isRead: false
    });

    notify();
    return { success: true, rewardCouponId: reward?.id || null, newAvgRating: srv.ratingAvg };
  }

  // クーポン作成 (S29)
  function createCoupon(couponData) {
    const newCoupon = {
      id: "cpn-" + Date.now(),
      propId: couponData.propId,
      storeName: couponData.storeName,
      title: couponData.title,
      type: couponData.type,
      typeLabel: couponData.type === "percent" ? "割引率" : (couponData.type === "amount" ? "割引額" : "無料サービス"),
      discountValue: couponData.discountValue,
      condition: couponData.condition,
      targetType: couponData.targetType,
      targetLabel: couponData.targetLabel,
      maxLimit: Number(couponData.maxLimit) || 100,
      issuedCount: 1,
      usedCount: 0,
      startDate: couponData.startDate || new Date().toISOString().slice(0, 10),
      expireDate: couponData.expireDate || "2026-12-31",
      status: "active",
      code: "CPN-" + Math.random().toString(36).substring(2, 8).toUpperCase(),
      isUsedByCurrentUser: false
    };

    state.coupons.unshift(newCoupon);

    // 住民ユーザーの手持ちクーポンにも即時反映（対象の場合）
    if (state.currentUser.role === "resident") {
      if (!state.currentUser.receivedCoupons) state.currentUser.receivedCoupons = [];
      state.currentUser.receivedCoupons.unshift(newCoupon.id);
    }

    notify();
    return newCoupon;
  }

  // クーポン消込・利用 (S31)
  function redeemCoupon(couponId) {
    const cpn = state.coupons.find(c => c.id === couponId);
    if (!cpn) return { success: false, message: "クーポンが見つかりません" };
    if (cpn.isUsedByCurrentUser) return { success: false, message: "このクーポンは既にご利用済みです" };

    cpn.isUsedByCurrentUser = true;
    cpn.usedCount += 1;
    cpn.redeemedAt = new Date().toLocaleString("ja-JP");

    notify();
    return { success: true, coupon: cpn };
  }

  // 物件PR広告掲載申込 (S33)
  function applyPrAdvertisement(propId, planType) {
    const prop = getPropertyById(propId);
    if (!prop) return { success: false };

    prop.isPr = true;
    prop.prPlan = planType;
    prop.prExpiresAt = "2026-11-30";

    state.notifications.unshift({
      id: "notif-" + Date.now(),
      type: "system",
      title: `【PR掲載開始】${prop.title}`,
      body: `検索結果の最上部PR注目枠に掲載されました（プラン: ${planType}）`,
      date: "たった今",
      linkScreen: "s01",
      isRead: false
    });

    notify();
    return { success: true };
  }

  function listingStatusToUiStatus(listingStatus) {
    switch (listingStatus) {
      case "DRAFT": return { status: "draft", statusLabel: "下書き" };
      case "PAUSED": return { status: "paused", statusLabel: "公開一時停止" };
      case "CONTRACTED": return { status: "preparing", statusLabel: "契約済み" };
      case "OPEN":
      default:
        return { status: "voting", statusLabel: "募集中" };
    }
  }

  function normalizePropertyPayload(propData, existing = {}) {
    const areaSqm = propData.areaSqm != null && Number(propData.areaSqm) > 0
      ? Math.round(Number(propData.areaSqm) * 100) / 100
      : (propData.areaTsubo ? Math.round(Number(propData.areaTsubo) * 3.30578 * 100) / 100 : (existing.areaSqm || 50));
    const areaTsubo = propData.areaTsubo != null && Number(propData.areaTsubo) > 0
      ? Math.round(Number(propData.areaTsubo) * 100) / 100
      : Math.round((areaSqm / 3.30578) * 100) / 100;

    const prefecture = propData.prefecture || existing.prefecture || "栃木県";
    const municipality = propData.municipality || existing.municipality || "宇都宮市";
    const addressLine = propData.addressLine || existing.addressLine || "";
    const address = propData.address || `${prefecture}${municipality}${addressLine}`.trim();

    const lat = propData.lat != null && !isNaN(Number(propData.lat)) ? Number(propData.lat) : (existing.location?.lat || 36.5610);
    const lng = propData.lng != null && !isNaN(Number(propData.lng)) ? Number(propData.lng) : (existing.location?.lng || 139.8830);

    const listingStatus = propData.listingStatus || existing.listingStatus || "OPEN";
    const uiStatus = listingStatusToUiStatus(listingStatus);

    const floorStr = (propData.floorNumber && propData.totalFloors)
      ? `${propData.floorNumber}/${propData.totalFloors}`
      : (propData.floor || existing.floor || "1階");

    const rawGallery = Array.isArray(propData.gallery)
      ? propData.gallery
      : (Array.isArray(existing.gallery) ? existing.gallery : []);
    const galleryInput = rawGallery.filter(g => g && g.url && g.url !== "assets/property-placeholder.svg");

    const firstExt = galleryInput.find(g => g.kind === "exterior") || galleryInput[0];
    const coverImage = propData.coverImage || (firstExt && firstExt.url) || "assets/property-placeholder.svg";

    const infra = propData.infrastructure || existing.infrastructure || {};
    const facLabels = [];
    (data.INFRASTRUCTURE_FACILITIES || []).forEach(item => {
      if (infra[item.key]) facLabels.push(item.label);
    });

    return {
      title: propData.title ?? existing.title ?? "",
      prefecture,
      municipality,
      addressLine,
      address,
      access: propData.access ?? existing.access ?? "",
      subtitle: propData.subtitle || `${floorStr} / ${areaSqm}㎡`,
      rent: propData.rent != null ? Number(propData.rent) : (existing.rent || 0),
      managementFee: propData.managementFee === "" || propData.managementFee == null ? null : Number(propData.managementFee),
      deposit: propData.deposit ?? existing.deposit ?? "掲載なし（要確認）",
      keyMoney: propData.keyMoney ?? existing.keyMoney ?? "掲載なし（要確認）",
      securityDeposit: propData.securityDeposit ?? existing.securityDeposit ?? "掲載なし（要確認）",
      areaSqm,
      areaTsubo,
      floorNumber: propData.floorNumber ?? existing.floorNumber ?? "1階",
      totalFloors: propData.totalFloors ?? existing.totalFloors ?? "",
      floor: floorStr,
      structure: propData.structure ?? existing.structure ?? "",
      builtYear: propData.builtYear ?? existing.builtYear ?? "",
      handover: propData.handover ?? existing.handover ?? "相談",
      permitStatus: propData.permitStatus || existing.permitStatus || "UNKNOWN",
      permitStatusLabel: propData.permitStatus === "OBTAINED" ? "取得済み" : (propData.permitStatus === "NOT_OBTAINED" ? "未取得（取得相談可）" : "要確認"),
      listingStatus,
      status: uiStatus.status,
      statusLabel: uiStatus.statusLabel,
      condition: propData.condition ?? existing.condition ?? "業種相談可",
      description: propData.description ?? existing.description ?? "",
      listingUrl: propData.listingUrl ?? existing.listingUrl ?? "",
      infrastructure: infra,
      facilities: facLabels.length ? facLabels : (propData.facilities || existing.facilities || []),
      allowedIndustries: Array.isArray(propData.allowedIndustries) ? propData.allowedIndustries : (existing.allowedIndustries || []),
      contractTerms: propData.contractTerms || existing.contractTerms || {
        longEnabled: true,
        popupEnabled: false,
        minDays: 3,
        maxDays: 30,
        dailyRentYen: null
      },
      distanceScaleKm: propData.distanceScaleKm != null ? Number(propData.distanceScaleKm) : (existing.distanceScaleKm || 10),
      location: { lat, lng },
      mapCoords: { x: 420, y: 280, lat, lng },
      coverImage,
      gallery: galleryInput.map((g, i) => ({ ...g, index: i })),
      photosEdited: Array.isArray(propData.gallery) ? true : Boolean(existing.photosEdited),
      ...(existing.galleryVersion ? { galleryVersion: existing.galleryVersion } : {})
    };
  }

  // 物件新規登録 (O04 / O05)
  function registerProperty(propData) {
    const normalized = normalizePropertyPayload(propData);
    const newProp = {
      id: "prop-" + Date.now(),
      code: "UT-NEW-" + Math.floor(Math.random() * 900 + 100),
      ...normalized,
      isPr: false,
      ownerId: state.currentUser.id,
      ownerName: state.currentUser.name,
      votesCount: 0,
      demandScore: 0,
      topDemands: [],
      residentVoices: []
    };

    state.properties.unshift(newProp);
    if (!state.users.owner.ownedPropIds) state.users.owner.ownedPropIds = [];
    if (!state.users.owner.ownedPropIds.includes(newProp.id)) {
      state.users.owner.ownedPropIds.push(newProp.id);
    }
    if (state.currentUser && state.currentUser !== state.users.owner) {
      if (!state.currentUser.ownedPropIds) state.currentUser.ownedPropIds = [];
      if (!state.currentUser.ownedPropIds.includes(newProp.id)) {
        state.currentUser.ownedPropIds.push(newProp.id);
      }
    }

    notify();
    return newProp;
  }

  // 物件更新 (O06 / O07)
  function updateProperty(propId, updateData) {
    const prop = getPropertyById(propId);
    if (!prop) return null;
    const normalized = normalizePropertyPayload(updateData, prop);
    Object.assign(prop, normalized);
    notify();
    return prop;
  }

  // 公開ステータス切替 (O01)
  function togglePropertyStatus(propId, nextListingStatus) {
    const prop = getPropertyById(propId);
    if (!prop) return null;
    const uiStatus = listingStatusToUiStatus(nextListingStatus);
    prop.listingStatus = nextListingStatus;
    prop.status = uiStatus.status;
    prop.statusLabel = uiStatus.statusLabel;
    notify();
    return prop;
  }

  // 削除影響件数の集計 (O08)
  function getPropertyDeletionImpact(propId) {
    const prop = getPropertyById(propId);
    if (!prop) return { votesCount: 0, appsCount: 0, activeAppsCount: 0, followsCount: 0 };
    const apps = (state.applications || []).filter(a => a.propId === propId);
    const activeApps = apps.filter(a => a.status !== "rejected" && a.status !== "withdrawn");
    let followsCount = 0;
    Object.values(state.users || {}).forEach(u => {
      if (Array.isArray(u.followedProps) && u.followedProps.includes(propId)) followsCount += 1;
    });
    return {
      votesCount: Number(prop.votesCount) || 0,
      appsCount: apps.length,
      activeAppsCount: activeApps.length,
      followsCount
    };
  }

  // オーナーデモサンプル物件割当（初期状態が空のときの確認補助）
  function assignDemoOwnerProperties() {
    const targetIds = (state.properties || []).slice(0, 2).map(p => p.id);
    if (!state.users.owner.ownedPropIds) state.users.owner.ownedPropIds = [];
    targetIds.forEach(id => {
      if (!state.users.owner.ownedPropIds.includes(id)) {
        state.users.owner.ownedPropIds.push(id);
      }
      const p = getPropertyById(id);
      if (p) p.ownerId = state.users.owner.id;
    });
    if (state.currentRole === "owner") {
      state.currentUser.ownedPropIds = [...state.users.owner.ownedPropIds];
    }
    notify();
    return targetIds;
  }

  // 各エンティティID指定取得ヘルパー
  function getSurveyById(id) {
    return state.surveys.find(s => s.id === id);
  }

  function getCouponById(id) {
    return state.coupons.find(c => c.id === id);
  }

  function getApplicationById(id) {
    return state.applications.find(a => a.id === id);
  }

  // 物件削除 (O08 / O09)
  function deleteProperty(propId) {
    const idx = state.properties.findIndex(p => p.id === propId);
    if (idx !== -1) {
      state.properties.splice(idx, 1);
      Object.values(state.users || {}).forEach(u => {
        if (Array.isArray(u.ownedPropIds)) {
          u.ownedPropIds = u.ownedPropIds.filter(id => id !== propId);
        }
        if (Array.isArray(u.followedProps)) {
          u.followedProps = u.followedProps.filter(id => id !== propId);
        }
      });
      notify();
      return true;
    }
    return false;
  }

  // ユーザープロフィール・基本情報更新 (S18)
  function updateUserProfile(profileData) {
    if (!profileData) return null;
    if (!state.currentUser) {
      state.currentUser = state.users[state.currentRole] || {};
    }
    if (profileData.role || profileData.userRole) {
      const r = String(profileData.role || profileData.userRole).toLowerCase();
      if (state.users[r]) {
        state.currentRole = r;
        state.currentUser = state.users[r];
      }
      profileData.role = r;
    }
    if (profileData.displayName && !profileData.name) {
      profileData.name = profileData.displayName;
    }
    Object.assign(state.currentUser, profileData);
    if (state.users && state.users[state.currentRole]) {
      Object.assign(state.users[state.currentRole], profileData);
    }
    notify('profile', { user: state.currentUser });
    return state.currentUser;
  }

  // サーバーセッション情報（index.jsp / User bean）による認証ユーザーの反映
  function setSessionUser(userData) {
    if (!userData) {
      if (state.currentUser) {
        state.currentUser.isLoggedIn = false;
      }
      notify('profile', { user: state.currentUser });
      return null;
    }
    const r = String(userData.role || userData.userRole || state.currentRole || 'resident').toLowerCase();
    if (state.users[r]) {
      state.currentRole = r;
      state.currentUser = state.users[r];
    }
    if (!state.currentUser) {
      state.currentUser = {};
    }
    state.currentUser.isLoggedIn = true;
    if (userData.userId != null) state.currentUser.userId = userData.userId;
    if (userData.displayName || userData.name) {
      state.currentUser.name = userData.displayName || userData.name;
      state.currentUser.displayName = userData.displayName || userData.name;
    }
    if (userData.email !== undefined) state.currentUser.email = userData.email;
    if (userData.accountStatus) state.currentUser.accountStatus = userData.accountStatus;
    state.currentUser.role = r;
    const roleNames = { resident: '地域住民', business: '出店事業主', owner: '物件オーナー', admin: '運営管理者' };
    state.currentUser.roleName = roleNames[r] || r;
    Object.assign(state.currentUser, userData);
    if (state.users[r]) {
      Object.assign(state.users[r], state.currentUser);
    }
    notify('profile', { user: state.currentUser });
    return state.currentUser;
  }

  return {
    getState: () => state,
    isPersistenceAvailable: () => persistenceAvailable,
    subscribe,
    notify,
    resetState,
    setRole,
    updateUserProfile,
    setSessionUser,
    getProperties,
    getPropertyById,
    getFilteredProperties,
    setFilters,
    toggleFollowProperty,
    submitVote,
    withdrawVote,
    submitApplication,
    updateApplicationStatus,
    updateApplicationCheck,
    publishStorePlan,
    createSurvey,
    submitSurveyResponse,
    createCoupon,
    redeemCoupon,
    applyPrAdvertisement,
    registerProperty,
    updateProperty,
    togglePropertyStatus,
    getPropertyDeletionImpact,
    assignDemoOwnerProperties,
    deleteProperty,
    getSurveyById,
    getCouponById,
    getApplicationById
  };
})();
