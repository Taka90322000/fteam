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
        const source = data.PROPERTIES.find(item => item.id === prop.id);
        if (source) {
          // Photo-only catalog updates must reach saved previews without losing interaction data.
          if (source.galleryVersion && prop.galleryVersion !== source.galleryVersion) {
            prop.gallery = JSON.parse(JSON.stringify(source.gallery));
            prop.galleryVersion = source.galleryVersion;
          }
          // If the property is not an owner-created custom listing, synchronize coverImage and access with the catalog master.
          if (!prop.ownerId || !prop.coverImage || prop.coverImage === 'assets/property-placeholder.svg') {
            prop.coverImage = source.coverImage;
            prop.imageCaption = source.imageCaption;
          }
          if (source.access) {
            prop.access = source.access;
          }
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

  // 順位投票実行 (S03)
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

    const distanceKm = state.currentUser.livingDistanceKm || 0.4;
    const distanceScale = 1.5;
    const distanceFactor = 1 / (1 + distanceKm / distanceScale);
    const weights = [1.0, 0.6, 0.3];

    // 同一ユーザーが既に同一物件に投票済みかチェック（二重カウント・スコア無限増殖の防止）
    const curVote = state.currentUser.votedProps?.[propId];
    const isUpdate = Boolean(curVote);

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
          score: 0,
          label: sub ? sub.name : subcatId
        };
        prop.topDemands.push(targetDemand);
      }
      targetDemand.votes += 1;
      targetDemand.score = Math.round((targetDemand.score + scoreAdded) * 10) / 10;
    });

    // 順位の再ソート
    prop.topDemands.sort((a, b) => b.score - a.score);
    prop.demandScore = Math.round((prop.demandScore + addedTotalScore) * 10) / 10;

    // 住民の声を追加（自由コメントがあれば）
    if (surveyData && surveyData.comment && surveyData.comment.trim()) {
      prop.residentVoices.unshift({
        author: `${state.currentUser.name}（${state.currentUser.livingArea}）`,
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
    return { success: true, addedScore: addedTotalScore, newTotalScore: prop.demandScore, isUpdate };
  }

  // 出店応募送信 (S08)
  function submitApplication(appData) {
    const newApp = {
      id: "app-" + Date.now(),
      propId: appData.propId,
      propTitle: appData.propTitle,
      applicantId: state.currentUser.id,
      applicantName: state.currentUser.name + (state.currentUser.company ? `（${state.currentUser.company}）` : ''),
      genreId: appData.genreId,
      genreName: data.SUBCATS[appData.genreId]?.name || "飲食店",
      storeName: appData.storeName,
      targetRent: Number(appData.targetRent) || 0,
      targetOpenDate: appData.targetOpenDate || appData.requestedStartOn || "",
      message: appData.message || appData.concept || "",
      concept: appData.concept || appData.message || "",
      contractKind: appData.contractKind || "REGULAR",
      requestedStartOn: appData.requestedStartOn || appData.targetOpenDate || "",
      requestedEndOn: appData.requestedEndOn || "",
      viewingRequestedAt: appData.viewingRequestedAt || "",
      status: "applying",
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
      title: `【新規出店応募】${appData.storeName}様より応募がありました`,
      body: `${prop?.title}に新しい出店申込が届きました。内容をご確認ください。`,
      date: "たった今",
      linkScreen: "s10",
      isRead: false
    });

    notify();
    return newApp;
  }

  // 応募ステータス更新 (S10)
  function updateApplicationStatus(appId, newStatus) {
    const app = state.applications.find(a => a.id === appId);
    if (!app) return;
    app.status = newStatus;

    if (newStatus === "interviewing") {
      app.statusLabel = "面談調整中";
    } else if (newStatus === "approved") {
      app.statusLabel = "出店合意（契約調整へ）";
    } else if (newStatus === "contracted") {
      app.statusLabel = "契約成立・引渡し完了";
      const prop = getPropertyById(app.propId);
      if (prop) {
        prop.status = "preparing";
        prop.statusLabel = `出店決定・開店準備中（${app.storeName}）`;
      }
    } else if (newStatus === "rejected") {
      app.statusLabel = "見送り（辞退済）";
    }

    notify();
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

  // 物件新規登録 (S12)
  function registerProperty(propData) {
    const newProp = {
      id: "prop-" + Date.now(),
      code: "UT-NEW-" + Math.floor(Math.random() * 90 + 10),
      title: propData.title,
      subtitle: propData.subtitle || "宇都宮中心市街地の新規登録空き店舗",
      address: propData.address,
      access: propData.access,
      rent: Number(propData.rent),
      managementFee: Number(propData.managementFee) || 0,
      deposit: propData.deposit || "敷金2ヶ月 / 礼金1ヶ月",
      securityDeposit: propData.securityDeposit || "30万円",
      areaTsubo: Number(propData.areaTsubo),
      areaSqm: Math.round(Number(propData.areaTsubo) * 3.30578 * 100) / 100,
      floor: propData.floor || "1階路面",
      structure: propData.structure || "鉄骨造",
      builtYear: propData.builtYear || "1995年築",
      handover: propData.handover || "相談",
      condition: propData.condition || "業種相談可",
      facilities: propData.facilities || ["都市ガス", "給排水"],
      status: "voting",
      statusLabel: "掲載開始・投票受付中",
      isPr: false,
      ownerId: state.currentUser.id,
      ownerName: state.currentUser.name,
      mapCoords: { x: 420, y: 280, lat: 36.5610, lng: 139.8830 },
      coverImage: "assets/prop_orion.svg",
      gallery: [{ label: "店舗外観", type: "facade" }],
      votesCount: 0,
      demandScore: 0,
      topDemands: [],
      residentVoices: [],
      description: propData.description || ""
    };

    state.properties.unshift(newProp);
    if (!state.currentUser.ownedPropIds) state.currentUser.ownedPropIds = [];
    state.currentUser.ownedPropIds.push(newProp.id);

    notify();
    return newProp;
  }

  // 物件更新 (S12 物件編集)
  function updateProperty(propId, updateData) {
    const prop = getPropertyById(propId);
    if (!prop) return null;
    Object.assign(prop, updateData);
    if (updateData.areaTsubo) {
      prop.areaSqm = Math.round(Number(updateData.areaTsubo) * 3.30578 * 100) / 100;
    }
    notify();
    return prop;
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

  // 物件削除
  function deleteProperty(propId) {
    const idx = state.properties.findIndex(p => p.id === propId);
    if (idx !== -1) {
      state.properties.splice(idx, 1);
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
    submitApplication,
    updateApplicationStatus,
    publishStorePlan,
    createSurvey,
    submitSurveyResponse,
    createCoupon,
    redeemCoupon,
    applyPrAdvertisement,
    registerProperty,
    updateProperty,
    deleteProperty,
    getSurveyById,
    getCouponById,
    getApplicationById
  };
})();
