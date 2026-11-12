/**
 * マチリク (Machiriku) - 33画面レンダラー & 画面コントローラー
 * 企画書、画面設計書(33画面)、テーブル設計書に完全準拠
 */
window.MachirikuScreens = (function() {
  const store = window.MachirikuStore;
  const data = window.MACHIRIKU_DATA;

  const PAGE_COPY = {
    S01: ["空き店舗を探す", ""],
    S03: ["欲しいお店に投票", "第1希望は必須です。第2・第3希望も選べます。"],
    S04: ["フォローしている物件", ""], S05: ["開店のようす", "投票した物件の、その後を確認できます。"],
    S06: ["お知らせ", ""], S07: ["地域の需要", "物件ごとの投票と、希望業種を確認できます。"],
    S08: ["出店を申し込む", "出店したいお店の内容をオーナーに伝えます。"],
    S09: ["出店の申し込み", ""], S10: ["申し込みの詳細", ""],
    S11: ["管理している物件", ""], S12: [null, "賃料や設備、入居できる業種を入力してください。"],
    S13: ["事業主に声をかける", ""], S14: ["契約・引き渡し", ""],
    S15: ["開店予定を知らせる", "店名や開店予定日を、地域の人に公開します。"],
    S16: ["ログイン", ""], S17: ["会員登録", ""], S18: [null, null], S19: ["通知の設定", ""],
    S20: ["画面一覧・仕様対応表", "画面設計を確認するための一覧です。"],
    S21: ["開店の相談", "内装、設備、資金について相談できます。"],
    S22: ["来店アンケート", ""], S23: ["アンケートをつくる", ""],
    S24: ["公開前の確認", "回答画面の内容を確認してください。"],
    S25: ["店内に掲示するQRコード", "印刷して、テーブルやレジ横に置いてください。"],
    S26: ["来店アンケート", "今日のご感想をお聞かせください。"],
    S27: ["アンケートの結果", ""], S28: ["クーポンの管理", ""], S29: ["クーポンをつくる", ""],
    S30: ["受け取ったクーポン", "お会計のときに、店員に提示してください。"],
    S31: ["クーポンを使う", "利用を確定する前に、店員に確認してもらってください。"],
    S32: ["事業主のプロフィール", "経験や資格、これまでのお店を紹介します。"],
    S33: ["物件の広告掲載", "検索結果にPR枠で掲載します。"]
  };

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, c => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;' }[c]));
  }

  function renderEmptyState(screen, title, description, hash = '#s01', label = '物件を探す') {
    return `<section class="screen-view empty-state" data-screen="${escapeHtml(screen)}">
      <h1 class="page-title">${escapeHtml(title)}</h1><p>${escapeHtml(description)}</p>
      <a class="btn-primary" href="${escapeHtml(hash)}">${escapeHtml(label)}</a></section>`;
  }

  const IMAGE_PLACEHOLDER = 'assets/property-placeholder.svg';
  const failedImages = new Set();

  const KIND_LABELS = {
    floorplan: '間取り',
    exterior: '外観',
    interior: '室内',
    facility: '共用・設備',
    surrounding: '周辺環境'
  };

  function getKindLabel(kind) {
    return KIND_LABELS[kind] || '写真';
  }

  let currentGallery = {
    propId: null,
    index: 0,
    items: []
  };

  function renderPropertyImage(prop, attributes = '') {
    const rawSource = prop.coverImage || (prop.gallery && prop.gallery[0] && (prop.gallery[0].url || prop.gallery[0].localPath)) || IMAGE_PLACEHOLDER;
    const source = rawSource;
    const unavailable = source === IMAGE_PLACEHOLDER || failedImages.has(source);
    return `<img src="${escapeHtml(unavailable ? IMAGE_PLACEHOLDER : source)}" alt="${unavailable ? '写真未登録' : escapeHtml(prop.title) + 'の外観'}" loading="lazy" decoding="async" referrerpolicy="no-referrer" onerror="MachirikuScreens.handlePropertyImageError(this)" ${attributes}>`;
  }

  function handlePropertyImageError(image) {
    const source = image.getAttribute('src');
    image.onerror = null;
    image.removeAttribute('onerror');
    if (source === IMAGE_PLACEHOLDER) return;

    // もし assets/prop-xxx.jpg で失敗した場合は assets/properties/prop-xxx/01_exterior.jpg を自動試行
    if (source.includes('assets/prop-') && source.endsWith('.jpg')) {
      const fallback = source.replace('assets/prop-', 'assets/properties/prop-').replace('.jpg', '/01_exterior.jpg');
      if (!failedImages.has(fallback)) {
        image.src = fallback;
        return;
      }
    }
    // もし assets/properties/prop-xxx/01_exterior.jpg で失敗した場合は assets/prop-xxx.jpg を自動試行
    if (source.includes('/01_exterior.jpg')) {
      const fallback = source.replace('/01_exterior.jpg', '.jpg').replace('assets/properties/prop-', 'assets/prop-');
      if (!failedImages.has(fallback)) {
        image.src = fallback;
        return;
      }
    }

    failedImages.add(source);
    image.alt = '写真未登録';
    image.src = IMAGE_PLACEHOLDER;
  }

  function businessReviews(user) {
    const surveys = store.getState().surveys.filter(s => user?.activeSurveys?.includes(s.id));
    const count = surveys.reduce((total, s) => total + (Number(s.responseCount) || 0), 0);
    const sum = surveys.reduce((total, s) => total + (Number(s.ratingAvg) || 0) * (Number(s.responseCount) || 0), 0);
    return { count, average: count ? (sum / count).toFixed(1) : '—' };
  }

  // ユーティリティ: 金額表記
  function formatMoney(num) {
    return Number(num).toLocaleString("ja-JP");
  }

  function formatRent(num) {
    return (Number(num) / 10000).toLocaleString('ja-JP', { maximumFractionDigits: 4 });
  }

  function renderSourceInformation(prop) {
    if (!prop.source) return '';
    return `<div class="listing-source">
      <p>掲載元: ${escapeHtml(prop.source.site)} / 情報更新日: ${escapeHtml(prop.source.listingUpdatedAt)} / 取得日: ${escapeHtml(prop.source.retrievedAt.slice(0, 10))}</p>
      <p>掲載条件は取得時点の情報です。現在の募集状況・利用可能な業種は掲載元でご確認ください。</p>
      ${prop.sourceNotes?.length ? `<details><summary>掲載情報の補足</summary><ul>${prop.sourceNotes.map(note => `<li>${escapeHtml(note)}</li>`).join('')}</ul></details>` : ''}
    </div>`;
  }

  // ユーティリティ: 共通ページヘッダー生成
  function renderHeaderBanner(screenNum, title, tag, desc, breadcrumbs = []) {
    const copy = PAGE_COPY[screenNum];
    title = copy?.[0] || title;
    desc = copy ? copy[1] : desc;
    const bcHtml = breadcrumbs.map((b, i) => {
      if (b.hash) {
        return `<a href="${b.hash}">${b.label}</a> <span class="sep">/</span>`;
      }
      return `<span>${b.label}</span>`;
    }).join(" ");

    return `
      <div class="page-header-banner" data-screen="${screenNum}">
        ${breadcrumbs.length ? `<nav class="breadcrumb-trail" aria-label="現在の位置"><a href="#s01">ホーム</a><span aria-hidden="true">/</span>${bcHtml}</nav>` : ''}
        <div class="page-title-row">
          <h1 class="page-title">
            ${escapeHtml(title)}
          </h1>
          ${screenNum === 'S02' && tag ? `<span class="badge-status status-voting">${escapeHtml(tag)}</span>` : ''}
        </div>
        ${desc ? `<p class="page-description">${escapeHtml(desc)}</p>` : ''}
      </div>
    `;
  }

  // =========================================================================
  // S01: ホーム (物件検索・地図・需要)
  // =========================================================================
  // =========================================================================
  // S01: ホーム (物件検索・地図・需要)
  // =========================================================================
  // 賃料分布ヒストグラム用：賃料以外の現在の検索条件に一致する物件（母集団）を取得
  function getRentHistogramBaseProperties() {
    return store.getFilteredProperties({
      rentMin: 0,
      rentMax: 50000000,
      includeMgmtFee: Boolean(rentPopoverState.includeMgmtFee),
      noDeposit: Boolean(rentPopoverState.noDeposit)
    });
  }

  // 実データから賃料ヒストグラムのビン（25分割：2万円刻み、0〜50万円以上）を集計
  function computeRentHistogram(baseProps, includeMgmtFee) {
    const BIN_COUNT = 25;
    const BIN_STEP = 2; // 2万円刻み
    const bins = [];
    for (let i = 0; i < BIN_COUNT; i++) {
      const bMin = i * BIN_STEP;
      const bMax = (i + 1) * BIN_STEP;
      bins.push({
        index: i,
        binMin: bMin,
        binMax: bMax,
        label: i === BIN_COUNT - 1 ? `${bMin}万円以上` : `${bMin}〜${bMax}万円`,
        count: 0
      });
    }

    (baseProps || []).forEach(p => {
      const effRent = includeMgmtFee ? (p.rent + (Number(p.managementFee) || 0)) : p.rent;
      const rentMan = effRent / 10000;
      let binIdx = Math.floor(rentMan / BIN_STEP);
      if (binIdx < 0) binIdx = 0;
      if (binIdx >= BIN_COUNT) binIdx = BIN_COUNT - 1;
      bins[binIdx].count++;
    });

    const maxCount = Math.max(0, ...bins.map(b => b.count));
    return {
      bins,
      maxCount,
      totalProperties: baseProps ? baseProps.length : 0
    };
  }

  // 賃料ヒストグラムのHTMLレンダリング（実データ比例・0件は高さ0・架空の山型グラフ完全撤廃）
  function renderRentHistogramHtml(bins, maxCount, rentMinMan, rentMaxMan) {
    if (maxCount === 0) {
      return `<div class="hist-empty-state" aria-live="polite">条件に一致する物件がありません（0件）</div>`;
    }

    return bins.map(b => {
      const inRange = b.binMax > rentMinMan && b.binMin < rentMaxMan;
      const heightPct = b.count > 0 ? Math.max(12, Math.round((b.count / maxCount) * 100)) : 0;
      const hasData = b.count > 0;
      return `
        <div class="hist-bar ${inRange ? 'in-range' : ''} ${hasData ? 'has-data' : 'is-empty'}"
             data-bin="${b.index}"
             data-min="${b.binMin}"
             data-max="${b.binMax}"
             data-count="${b.count}"
             style="height:${heightPct}%;"
             title="${b.label}: ${b.count}件"
             aria-label="${b.label}: ${b.count}件"
             onmouseenter="MachirikuScreens.handleHistBarHover(this, '${b.label}', ${b.count})"
             onmouseleave="MachirikuScreens.handleHistBarLeave()"
             onclick="MachirikuScreens.handleHistBarClick(${b.binMin}, ${b.binMax})">
        </div>
      `;
    }).join('');
  }

  // 全国8地方・47都道府県マスター (添付画像1完全準拠)
  const PREFECTURES_BY_REGION = [
    { region: "北海道・東北", prefs: ["北海道", "青森", "岩手", "宮城", "秋田", "山形", "福島"] },
    { region: "関東", prefs: ["東京", "神奈川", "埼玉", "千葉", "茨城", "栃木", "群馬"] },
    { region: "北陸・甲信越", prefs: ["新潟", "富山", "石川", "福井", "山梨", "長野"] },
    { region: "東海", prefs: ["愛知", "岐阜", "静岡", "三重"] },
    { region: "近畿", prefs: ["大阪", "兵庫", "京都", "滋賀", "奈良", "和歌山"] },
    { region: "中国・四国", prefs: ["鳥取", "島根", "岡山", "広島", "山口", "徳島", "香川", "愛媛", "高知"] },
    { region: "九州・沖縄", prefs: ["福岡", "佐賀", "長崎", "熊本", "大分", "宮崎", "鹿児島", "沖縄"] }
  ];

  // 全国47都道府県 市一覧マスター (市のみ・町は除外)
  const PREFECTURE_MUNICIPALITIES = {
    // 北海道・東北
    "北海道": { "市部": ["札幌市", "函館市", "小樽市", "旭川市", "室蘭市", "釧路市", "帯広市", "北見市", "夕張市", "岩見沢市", "網走市", "留萌市", "苫小牧市", "稚内市", "美唄市", "芦別市", "江別市", "赤平市", "紋別市", "士別市", "名寄市", "三笠市", "根室市", "千歳市", "滝川市", "砂川市", "歌志内市", "深川市", "富良野市", "登別市", "恵庭市", "伊達市", "北広島市", "石狩市", "北斗市"] },
    "青森": { "市部": ["青森市", "弘前市", "八戸市", "黒石市", "五所川原市", "十和田市", "三沢市", "むつ市", "つがる市", "平川市"] },
    "岩手": { "市部": ["盛岡市", "宮古市", "大船渡市", "花巻市", "北上市", "久慈市", "遠野市", "一関市", "陸前高田市", "釜石市", "二戸市", "八幡平市", "奥州市", "滝沢市"] },
    "宮城": { "市部": ["仙台市", "石巻市", "塩竈市", "気仙沼市", "白石市", "名取市", "角田市", "多賀城市", "岩沼市", "登米市", "栗原市", "東松島市", "大崎市", "富谷市"] },
    "秋田": { "市部": ["秋田市", "能代市", "横手市", "大館市", "男鹿市", "湯沢市", "鹿角市", "由利本荘市", "潟上市", "大仙市", "北秋田市", "にかほ市", "仙北市"] },
    "山形": { "市部": ["山形市", "米沢市", "鶴岡市", "酒田市", "新庄市", "寒河江市", "上山市", "村山市", "長井市", "天童市", "東根市", "尾花沢市", "南陽市"] },
    "福島": { "市部": ["福島市", "会津若松市", "郡山市", "いわき市", "白河市", "須賀川市", "喜多方市", "相馬市", "二本松市", "田村市", "南相馬市", "伊達市", "本宮市"] },

    // 関東
    "茨城": { "市部": ["水戸市", "日立市", "土浦市", "古河市", "石岡市", "結城市", "龍ケ崎市", "下妻市", "常総市", "常陸太田市", "高萩市", "北茨城市", "笠間市", "取手市", "牛久市", "つくば市", "ひたちなか市", "鹿嶋市", "潮来市", "守谷市", "常陸大宮市", "那珂市", "筑西市", "坂東市", "稲敷市", "かすみがうら市", "桜川市", "神栖市", "行方市", "鉾田市", "つくばみらい市", "小美玉市"] },
    "栃木": { "市部": ["宇都宮市", "足利市", "栃木市", "佐野市", "鹿沼市", "日光市", "小山市", "真岡市", "大田原市", "矢板市", "那須塩原市", "さくら市", "那須烏山市", "下野市"] },
    "群馬": { "市部": ["前橋市", "高崎市", "桐生市", "伊勢崎市", "太田市", "沼田市", "館林市", "渋川市", "藤岡市", "富岡市", "安中市", "みどり市"] },
    "埼玉": { "市部": ["さいたま市", "川越市", "熊谷市", "川口市", "行田市", "秩父市", "所沢市", "飯能市", "加須市", "本庄市", "東松山市", "春日部市", "狭山市", "羽生市", "鴻巣市", "深谷市", "上尾市", "草加市", "越谷市", "蕨市", "戸田市", "入間市", "朝霞市", "志木市", "和光市", "新座市", "桶川市", "久喜市", "北本市", "八潮市", "富士見市", "三郷市", "蓮田市", "坂戸市", "幸手市", "鶴ヶ島市", "日高市", "吉川市", "ふじみ野市", "白岡市"] },
    "千葉": { "市部": ["千葉市", "銚子市", "市川市", "船橋市", "館山市", "木更津市", "松戸市", "野田市", "茂原市", "成田市", "佐倉市", "東金市", "旭市", "習志野市", "柏市", "勝浦市", "市原市", "流山市", "八千代市", "我孫子市", "鴨川市", "鎌ケ谷市", "君津市", "富津市", "浦安市", "四街道市", "袖ケ浦市", "八街市", "印西市", "白井市", "富里市", "南房総市", "匝瑳市", "香取市", "山武市", "いすみ市", "大網白里市"] },
    "東京": {
      "特別区(23区)": ["千代田区", "中央区", "港区", "新宿区", "文京区", "台東区", "墨田区", "江東区", "品川区", "目黒区", "大田区", "世田谷区", "渋谷区", "中野区", "杉並区", "豊島区", "北区", "荒川区", "板橋区", "練馬区", "足立区", "葛飾区", "江戸川区"],
      "市部": ["八王子市", "立川市", "武蔵野市", "三鷹市", "青梅市", "府中市", "昭島市", "調布市", "町田市", "小金井市", "小平市", "日野市", "東村山市", "国分寺市", "国立市", "福生市", "狛江市", "東大和市", "清瀬市", "東久留米市", "武蔵村山市", "多摩市", "稲城市", "羽村市", "あきる野市", "西東京市"]
    },
    "神奈川": { "市部": ["横浜市", "川崎市", "相模原市", "横須賀市", "平塚市", "鎌倉市", "藤沢市", "小田原市", "茅ヶ崎市", "逗子市", "三浦市", "秦野市", "厚木市", "大和市", "伊勢原市", "海老名市", "座間市", "南足柄市", "綾瀬市"] },

    // 北陸・甲信越
    "新潟": { "市部": ["新潟市", "長岡市", "三条市", "柏崎市", "新発田市", "小千谷市", "加茂市", "十日町市", "見附市", "村上市", "燕市", "糸魚川市", "妙高市", "五泉市", "上越市", "阿賀野市", "佐渡市", "魚沼市", "南魚沼市", "胎内市"] },
    "富山": { "市部": ["富山市", "高岡市", "魚津市", "氷見市", "滑川市", "黒部市", "砺波市", "小矢部市", "南砺市", "射水市"] },
    "石川": { "市部": ["金沢市", "七尾市", "小松市", "輪島市", "珠洲市", "加賀市", "羽咋市", "かほく市", "白山市", "能美市", "野々市市"] },
    "福井": { "市部": ["福井市", "敦賀市", "小浜市", "大野市", "勝山市", "鯖江市", "あわら市", "越前市", "坂井市"] },
    "山梨": { "市部": ["甲府市", "富士吉田市", "都留市", "山梨市", "大月市", "韮崎市", "南アルプス市", "北杜市", "甲斐市", "笛吹市", "上野原市", "甲州市", "中央市"] },
    "長野": { "市部": ["長野市", "松本市", "上田市", "岡谷市", "飯田市", "諏訪市", "須坂市", "小諸市", "伊那市", "駒ヶ根市", "中野市", "大町市", "飯山市", "茅野市", "塩尻市", "佐久市", "千曲市", "東御市", "安曇野市"] },

    // 東海
    "岐阜": { "市部": ["岐阜市", "大垣市", "高山市", "多治見市", "関市", "中津川市", "美濃市", "瑞浪市", "羽島市", "恵那市", "美濃加茂市", "土岐市", "各務原市", "可児市", "山県市", "瑞穂市", "飛騨市", "本巣市", "郡上市", "下呂市", "海津市"] },
    "静岡": { "市部": ["静岡市", "浜松市", "沼津市", "熱海市", "三島市", "富士宮市", "伊東市", "島田市", "富士市", "磐田市", "焼津市", "掛川市", "藤枝市", "御殿場市", "袋井市", "下田市", "裾野市", "湖西市", "伊豆市", "御前崎市", "菊川市", "伊豆の国市", "牧之原市"] },
    "愛知": { "市部": ["名古屋市", "豊橋市", "岡崎市", "一宮市", "瀬戸市", "半田市", "春日井市", "豊川市", "津島市", "碧南市", "刈谷市", "豊田市", "安城市", "西尾市", "蒲郡市", "犬山市", "常滑市", "江南市", "小牧市", "稲沢市", "新城市", "東海市", "大府市", "知多市", "知立市", "尾張旭市", "高浜市", "岩倉市", "豊明市", "日進市", "田原市", "愛西市", "清須市", "北名古屋市", "弥富市", "みよし市", "あま市", "長久手市"] },
    "三重": { "市部": ["津市", "四日市市", "伊勢市", "松阪市", "桑名市", "鈴鹿市", "名張市", "尾鷲市", "亀山市", "鳥羽市", "熊野市", "いなべ市", "志摩市", "伊賀市"] },

    // 近畿
    "滋賀": { "市部": ["大津市", "彦根市", "長浜市", "近江八幡市", "草津市", "守山市", "栗東市", "甲賀市", "野洲市", "湖南市", "高島市", "東近江市", "米原市"] },
    "京都": { "市部": ["京都市", "福知山市", "舞鶴市", "綾部市", "宇治市", "宮津市", "亀岡市", "城陽市", "向日市", "長岡京市", "八幡市", "京田辺市", "京丹後市", "南丹市", "木津川市"] },
    "大阪": { "市部": ["大阪市", "堺市", "岸和田市", "豊中市", "池田市", "吹田市", "泉大津市", "高槻市", "貝塚市", "守口市", "枚方市", "茨木市", "八尾市", "泉佐野市", "富田林市", "寝屋川市", "河内長野市", "松原市", "大東市", "和泉市", "箕面市", "柏原市", "羽曳野市", "門真市", "摂津市", "高石市", "藤井寺市", "東大阪市", "泉南市", "四條畷市", "交野市", "大阪狭山市", "阪南市"] },
    "兵庫": { "市部": ["神戸市", "姫路市", "尼崎市", "明石市", "西宮市", "洲本市", "芦屋市", "伊丹市", "相生市", "豊岡市", "加古川市", "赤穂市", "西脇市", "宝塚市", "三木市", "高砂市", "川西市", "小野市", "三田市", "加西市", "篠山市", "養父市", "丹波市", "南あわじ市", "朝来市", "淡路市", "宍粟市", "たつの市"] },
    "奈良": { "市部": ["奈良市", "大和高田市", "大和郡山市", "天理市", "橿原市", "桜井市", "五條市", "御所市", "生駒市", "香芝市", "葛城市", "宇陀市"] },
    "和歌山": { "市部": ["和歌山市", "海南市", "橋本市", "有田市", "御坊市", "田辺市", "新宮市", "紀の川市", "岩出市"] },

    // 中国・四国
    "鳥取": { "市部": ["鳥取市", "米子市", "倉吉市", "境港市"] },
    "島根": { "市部": ["松江市", "浜田市", "出雲市", "益田市", "大田市", "安来市", "江津市", "雲南市"] },
    "岡山": { "市部": ["岡山市", "倉敷市", "津山市", "玉野市", "笠岡市", "井原市", "総社市", "高梁市", "新見市", "備前市", "瀬戸内市", "赤磐市", "真庭市", "美作市", "浅口市"] },
    "広島": { "市部": ["広島市", "呉市", "竹原市", "三原市", "尾道市", "福山市", "府中市", "三次市", "庄原市", "大竹市", "東広島市", "廿日市市", "安芸高田市", "江田島市"] },
    "山口": { "市部": ["下関市", "宇部市", "山口市", "萩市", "防府市", "下松市", "岩国市", "光市", "長門市", "柳井市", "美祢市", "周南市", "山陽小野田市"] },
    "徳島": { "市部": ["徳島市", "鳴門市", "小松島市", "阿南市", "吉野川市", "阿波市", "美馬市", "三好市"] },
    "香川": { "市部": ["高松市", "丸亀市", "坂出市", "善通寺市", "観音寺市", "さぬき市", "東かがわ市", "三豊市"] },
    "愛媛": { "市部": ["松山市", "今治市", "宇和島市", "八幡浜市", "新居浜市", "西条市", "大洲市", "伊予市", "四国中央市", "西予市", "東温市"] },
    "高知": { "市部": ["高知市", "室戸市", "安芸市", "南国市", "土佐市", "須崎市", "宿毛市", "土佐清水市", "四万十市", "香南市", "香美市"] },

    // 九州・沖縄
    "福岡": { "市部": ["北九州市", "福岡市", "大牟田市", "久留米市", "直方市", "飯塚市", "田川市", "柳川市", "八女市", "筑後市", "大川市", "行橋市", "豊前市", "中間市", "小郡市", "筑紫野市", "春日市", "大野城市", "宗像市", "太宰府市", "古賀市", "福津市", "うきは市", "宮若市", "嘉麻市", "朝倉市", "みやま市", "糸島市", "那珂川市"] },
    "佐賀": { "市部": ["佐賀市", "唐津市", "鳥栖市", "多久市", "伊万里市", "武雄市", "鹿島市", "小城市", "嬉野市", "神埼市"] },
    "長崎": { "市部": ["長崎市", "佐世保市", "島原市", "諫早市", "大村市", "平戸市", "松浦市", "対馬市", "壱岐市", "五島市", "西海市", "雲仙市", "南島原市"] },
    "熊本": { "市部": ["熊本市", "八代市", "人吉市", "荒尾市", "水俣市", "玉名市", "山鹿市", "菊池市", "宇土市", "上天草市", "宇城市", "阿蘇市", "天草市", "合志市"] },
    "大分": { "市部": ["大分市", "別府市", "中津市", "日田市", "佐伯市", "臼杵市", "津久見市", "竹田市", "豊後高田市", "杵築市", "宇佐市", "豊後大野市", "由布市", "国東市"] },
    "宮崎": { "市部": ["宮崎市", "都城市", "延岡市", "日南市", "小林市", "日向市", "串間市", "西都市", "えびの市"] },
    "鹿児島": { "市部": ["鹿児島市", "鹿屋市", "枕崎市", "阿久根市", "出水市", "指宿市", "西之表市", "垂水市", "薩摩川内市", "日置市", "曽於市", "霧島市", "いちき串木野市", "南さつま市", "志布志市", "奄美市", "南九州市", "伊佐市", "姶良市"] },
    "沖縄": { "市部": ["那覇市", "宜野湾市", "石垣市", "浦添市", "名護市", "糸満市", "沖縄市", "豊見城市", "うるま市", "宮古島市", "南城市"] }
  };

  function getMunicipalitiesForPref(prefName) {
    if (PREFECTURE_MUNICIPALITIES[prefName]) {
      return PREFECTURE_MUNICIPALITIES[prefName];
    }
    return {
      "市部": [prefName + "市"]
    };
  }

  function getMunicipalityPropertyCount(cityName, prospectiveFilters) {
    const currentProps = store.getFilteredProperties(prospectiveFilters);
    return currentProps.filter(p => p.address && p.address.includes(cityName)).length;
  }

  // 住民需要スコア集計
  function getCategoryDemandScores() {
    const scores = {};
    data.CATEGORIES.forEach(c => { scores[c.id] = 0; });
    store.getProperties().forEach(p => {
      p.topDemands?.forEach(d => {
        const sub = data.SUBCATS[d.subcatId];
        if (sub && sub.categoryId && scores[sub.categoryId] !== undefined) {
          scores[sub.categoryId] += Math.round(Number(d.score) || 0);
        }
      });
    });
    return scores;
  }

  // UI状態管理
  let areaModalState = {
    isOpen: false,
    step: 'city', // 'pref' or 'city'
    selectedPref: '栃木',
    pendingCities: []
  };

  let rentPopoverOpen = false;
  let rentPopoverState = {
    pendingMin: 0,
    pendingMax: 500000,
    includeMgmtFee: false,
    noDeposit: false
  };

  let catPopoverOpen = false;
  let catPopoverState = {
    pendingCategories: []
  };

  // エリアモーダル レンダラー (画面中央展開・添付画像1&2完全準拠)
  function renderAreaModalHtml(state) {
    const pref = areaModalState.selectedPref || '栃木';
    const pending = areaModalState.pendingCities || [];
    const prospectiveCount = store.getFilteredProperties({ pref: pref, cities: pending }).length;

    let bodyContent = '';

    if (areaModalState.step === 'pref') {
      // Step 1: 都道府県選択 (添付画像1)
      const regionsHtml = PREFECTURES_BY_REGION.map(group => {
        const linksHtml = group.prefs.map(pName => `
          <a class="pref-link ${pref === pName ? 'is-selected' : ''}" href="javascript:void(0)" onclick="MachirikuScreens.setAreaModalStep('city', '${pName}')">${pName}</a>
        `).join('');

        return `
          <div class="area-row" style="margin-bottom: var(--space-4);">
            <div class="area-region-name" style="width: 120px; font-weight: 700; color: var(--text-main); font-size: 0.9375rem;">${group.region}</div>
            <div class="pref-line" style="display: flex; flex-wrap: wrap; gap: 0.9rem; align-items: center;">
              ${linksHtml}
            </div>
          </div>
        `;
      }).join('');

      bodyContent = `
        <div class="area-modal-body" style="padding-top: var(--space-3);">
          <p style="font-weight: 700; color: var(--text-main); font-size: 0.9375rem; margin-bottom: var(--space-3);">都道府県を選択してください</p>
          <div style="display: flex; flex-direction: column; gap: var(--space-2);">
            ${regionsHtml}
          </div>
        </div>
      `;
    } else {
      // Step 2: 市区町村選択 (添付画像2)
      const muniData = getMunicipalitiesForPref(pref);
      const groupsHtml = Object.entries(muniData).map(([groupTitle, cityList]) => {
        const itemsHtml = cityList.map(cityName => {
          const isChecked = pending.includes(cityName);
          const count = getMunicipalityPropertyCount(cityName);

          return `
            <div class="municipality-item">
              <input type="checkbox" id="muni-chk-${escapeHtml(cityName)}" value="${escapeHtml(cityName)}" ${isChecked ? 'checked' : ''} onchange="MachirikuScreens.handleAreaCityCheck('${escapeHtml(cityName)}', this.checked)">
              <button type="button" class="municipality-link" onclick="MachirikuScreens.handleAreaCitySoloSelect('${escapeHtml(cityName)}')">${escapeHtml(cityName)}</button>
              <span class="municipality-count">(${count})</span>
            </div>
          `;
        }).join('');

        return `
          <div class="municipality-group-header">${escapeHtml(groupTitle)}</div>
          <div class="municipality-grid">
            ${itemsHtml}
          </div>
        `;
      }).join('');

      const subbarHtml = pending.length > 0
        ? `
          <div style="display: flex; justify-content: flex-end; align-items: center; padding: 0.4rem var(--space-5); border-bottom: 1px solid var(--border-color); background-color: var(--bg-card);">
            <button type="button" class="btn-back-to-prefs" onclick="MachirikuScreens.setAreaModalStep('pref')">← 都道府県を選び直す (${escapeHtml(pref)})</button>
          </div>
        `
        : `
          <div class="area-modal-subbar">
            <h3 class="modal-instruction-title">${pref === '東京' ? '市区' : '市'}を選択してください。<small>（複数選択できます）</small></h3>
            <button type="button" class="btn-back-to-prefs" onclick="MachirikuScreens.setAreaModalStep('pref')">← 都道府県を選び直す (${escapeHtml(pref)})</button>
          </div>
        `;

      bodyContent = `
        ${subbarHtml}
        <div class="area-modal-body">
          ${groupsHtml}
        </div>
      `;
    }

    return `
      <div class="area-modal-backdrop" id="area-modal-backdrop" onclick="if(event.target === this) MachirikuScreens.closeAreaModal()">
        <div class="area-modal-dialog" role="dialog" aria-modal="true" aria-labelledby="area-modal-heading">
          <div class="area-modal-header">
            <div class="area-modal-title-wrap">
              <span class="area-accent-bar" aria-hidden="true"></span>
              <h2 class="area-modal-title" id="area-modal-heading">エリアを選択</h2>
            </div>
            <button type="button" class="btn-location-action" onclick="MachirikuScreens.handleCurrentLocationSearch()">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M12 2a8 8 0 0 0-8 8c0 5.25 8 12 8 12s8-6.75 8-12a8 8 0 0 0-8-8z"/><circle cx="12" cy="10" r="3"/></svg>
              現在地から探す
            </button>
          </div>

          ${bodyContent}

          ${areaModalState.step === 'city' ? `
            <div class="area-modal-footer">
              <button type="button" class="btn-text-action" onclick="MachirikuScreens.clearAreaSelection()">条件をクリア</button>
              <button type="button" class="btn-primary" style="min-height: 44px; padding: 0.45rem 1.4rem; font-size: 0.9375rem;" onclick="MachirikuScreens.applyAreaSelection()">このエリアで決定する (${prospectiveCount}件)</button>
            </div>
          ` : ''}

          <div class="area-modal-bottom-close-bar">
            <button type="button" class="btn-modal-pill-close" onclick="MachirikuScreens.closeAreaModal()">✕ 閉じる</button>
          </div>
        </div>
      </div>
    `;
  }

  // 賃料ポップオーバー レンダラー (ボタン直下コンパクト吹き出し)
  function renderRentPopoverHtml(state) {
    const minVal = rentPopoverState.pendingMin || 0;
    const maxVal = rentPopoverState.pendingMax != null ? rentPopoverState.pendingMax : 500000;
    const rentMinMan = Math.round(minVal / 10000);
    const rentMaxMan = maxVal < 50000000 ? Math.round(maxVal / 10000) : 50;
    const sliderLeftPct = Math.min(100, Math.max(0, (rentMinMan / 50) * 100));
    const sliderWidthPct = Math.min(100 - sliderLeftPct, Math.max(0, ((rentMaxMan - rentMinMan) / 50) * 100));

    // 実データから賃料ヒストグラムを計算（母集団：エリア・業種等の非賃料フィルター適用済）
    const baseProps = getRentHistogramBaseProperties();
    const { bins, maxCount, totalProperties } = computeRentHistogram(baseProps, rentPopoverState.includeMgmtFee);

    // 現在の条件に該当する物件数（ライブプレビュー用）
    const prospectiveCount = store.getFilteredProperties({
      rentMin: minVal,
      rentMax: maxVal,
      includeMgmtFee: rentPopoverState.includeMgmtFee,
      noDeposit: rentPopoverState.noDeposit
    }).length;

    const minOptions = [
      [0, '下限なし'],
      [50000, '5万円'],
      [100000, '10万円'],
      [120000, '12万円'],
      [140000, '14万円'],
      [150000, '15万円'],
      [160000, '16万円'],
      [180000, '18万円'],
      [200000, '20万円'],
      [250000, '25万円'],
      [300000, '30万円'],
      [400000, '40万円'],
      [500000, '50万円']
    ];

    const maxOptions = [
      [50000000, '上限なし'],
      [100000, '10万円'],
      [120000, '12万円'],
      [140000, '14万円'],
      [150000, '15万円'],
      [160000, '16万円'],
      [180000, '18万円'],
      [200000, '20万円'],
      [250000, '25万円'],
      [300000, '30万円'],
      [400000, '40万円'],
      [500000, '50万円']
    ];

    let minHasSelected = false;
    let minOptionsHtml = minOptions.map(([val, label]) => {
      const isSel = minVal === val;
      if (isSel) minHasSelected = true;
      return `<option value="${val}" ${isSel ? 'selected' : ''}>${label}</option>`;
    }).join('');
    if (!minHasSelected && minVal > 0) {
      minOptionsHtml = `<option value="${minVal}" selected>${rentMinMan}万円</option>` + minOptionsHtml;
    }

    let maxHasSelected = false;
    let maxOptionsHtml = maxOptions.map(([val, label]) => {
      const isSel = maxVal === val;
      if (isSel) maxHasSelected = true;
      return `<option value="${val}" ${isSel ? 'selected' : ''}>${label}</option>`;
    }).join('');
    if (!maxHasSelected && maxVal > 0 && maxVal < 50000000) {
      maxOptionsHtml = `<option value="${maxVal}" selected>${rentMaxMan}万円</option>` + maxOptionsHtml;
    }

    return `
      <div class="popover-modal-backdrop" onclick="if(event.target === this) MachirikuScreens.closeRentPopover()">
        <div class="filter-popover-card rent-popover-card" onclick="event.stopPropagation()">
          <div class="popover-header">
            <h3 class="popover-title">賃料</h3>
            <button type="button" class="popover-close-btn" onclick="MachirikuScreens.closeRentPopover()" aria-label="閉じる">✕</button>
          </div>
          <div class="rent-popover-content">
          <div class="rent-selects-row">
            <div class="rent-select-wrap">
              <label for="rent-sel-min">下限</label>
              <select id="rent-sel-min" onchange="MachirikuScreens.handleRentSelectChange('min', this.value)">
                ${minOptionsHtml}
              </select>
            </div>
            <span class="rent-tilde">〜</span>
            <div class="rent-select-wrap">
              <label for="rent-sel-max">上限</label>
              <select id="rent-sel-max" onchange="MachirikuScreens.handleRentSelectChange('max', this.value)">
                ${maxOptionsHtml}
              </select>
            </div>
          </div>

          <div class="rent-slider-container">
            <div class="rent-hist-indicator-bar">
              <span class="rent-hist-summary-text" id="rent-hist-summary-text">
                賃料分布 (母集団 <strong>${totalProperties}件</strong>)
              </span>
              <span class="rent-hist-hover-tip" id="rent-hist-hover-tip" style="display: none;"></span>
            </div>
            <div class="rent-histogram-bars" id="rent-histogram-bars" role="region" aria-label="賃料分布ヒストグラム">
              ${renderRentHistogramHtml(bins, maxCount, rentMinMan, rentMaxMan)}
            </div>
            <div class="dual-range-track-wrap">
              <div class="range-slider-rail"></div>
              <div class="range-slider-highlight" id="range-slider-highlight" style="left: ${sliderLeftPct}%; width: ${sliderWidthPct}%;"></div>
              <input type="range" id="rent-slider-min" min="0" max="50" step="1" value="${rentMinMan}" oninput="MachirikuScreens.handleRentSliderInput('min', this.value)" aria-label="賃料下限スライダー">
              <input type="range" id="rent-slider-max" min="0" max="50" step="1" value="${rentMaxMan}" oninput="MachirikuScreens.handleRentSliderInput('max', this.value)" aria-label="賃料上限スライダー">
            </div>
            <div class="rent-slider-ticks">
              <span>0</span>
              <span>10万</span>
              <span>20万</span>
              <span>30万</span>
              <span>40万</span>
              <span>50万+</span>
            </div>
          </div>

          <div class="rent-live-preview ${prospectiveCount === 0 ? 'is-zero' : ''}" id="rent-live-preview" aria-live="polite">
            ${prospectiveCount > 0 
              ? `この条件に該当する物件: <strong class="preview-count">${prospectiveCount}件</strong>`
              : `<span class="preview-zero-text">該当する物件: <strong>0件</strong> <small class="zero-hint">（賃料の範囲を広げてください）</small></span>`}
          </div>

          <div class="rent-options-stack">
            <label class="rent-option-check">
              <input type="checkbox" id="check-include-mgmt" ${rentPopoverState.includeMgmtFee ? 'checked' : ''} onchange="MachirikuScreens.handleRentOptionChange('mgmt', this.checked)">
              <span>管理費・共益費を含む</span>
            </label>
            <label class="rent-option-check">
              <input type="checkbox" id="check-no-deposit" ${rentPopoverState.noDeposit ? 'checked' : ''} onchange="MachirikuScreens.handleRentOptionChange('deposit', this.checked)">
              <span>敷金・礼金なし</span>
            </label>
          </div>
        </div>
        <div class="popover-footer">
          <button type="button" class="btn-text-action" onclick="MachirikuScreens.resetRentFilters()">リセット</button>
          <button type="button" class="btn-primary btn-apply-rent" id="btn-rent-apply" onclick="MachirikuScreens.applyRentFilters()">適用する (${prospectiveCount}件)</button>
        </div>
      </div>
    </div>
    `;
  }

  // 業種ポップオーバー レンダラー (ボタン直下2列ポップオーバー)
  function renderCatPopoverHtml(state) {
    const pending = catPopoverState.pendingCategories || [];
    const prospectiveCount = store.getFilteredProperties({ categories: pending }).length;
    const demandScores = getCategoryDemandScores();

    const itemsHtml = data.CATEGORIES.map(c => {
      const isChecked = pending.includes(c.id);
      const score = demandScores[c.id] || 0;

      return `
        <label class="cat-popover-item">
          <span class="cat-item-left">
            <input type="checkbox" value="${c.id}" ${isChecked ? 'checked' : ''} onchange="MachirikuScreens.handleCatCheckboxChange('${c.id}', this.checked)">
            <span class="cat-item-name">${escapeHtml(c.name)}</span>
          </span>
          <span class="cat-demand-score">需要 ${score}pt</span>
        </label>
      `;
    }).join('');

    return `
      <div class="popover-modal-backdrop" onclick="if(event.target === this) MachirikuScreens.closeCatPopover()">
        <div class="filter-popover-card cat-popover-card" onclick="event.stopPropagation()">
          <div class="popover-header">
            <h3 class="popover-title">希望業種（カテゴリ）</h3>
            <button type="button" class="popover-close-btn" onclick="MachirikuScreens.closeCatPopover()" aria-label="閉じる">✕</button>
          </div>
        <div class="cat-popover-grid">
          ${itemsHtml}
        </div>
        <div class="popover-footer">
          <button type="button" class="btn-text-action" onclick="MachirikuScreens.toggleAllCategories()">全選択 / 解除</button>
          <button type="button" class="btn-primary" style="min-height: 38px; padding: 0.35rem 1.15rem; font-size: 0.875rem;" onclick="MachirikuScreens.applyCatFilters()">業種を絞り込む (${prospectiveCount}件)</button>
        </div>
      </div>
    </div>
    `;
  }

  function renderS01() {
    const state = store.getState();
    const props = store.getFilteredProperties();
    const { keyword, area, pref = 'all', cities = [], category = 'all', categories = [], rentMin = 0, rentMax = 500000, includeMgmtFee = false, noDeposit = false, sort } = state.filters;

    const activeCities = (cities && cities.length > 0) ? cities : (pref === '栃木' ? ['宇都宮市'] : []);

    // 非アクティブ時はPending状態をストア同期
    if (!areaModalState.isOpen) {
      areaModalState.selectedPref = (pref !== 'all' ? pref : '栃木');
      areaModalState.pendingCities = [...activeCities];
    }
    if (!rentPopoverOpen) {
      rentPopoverState.pendingMin = rentMin;
      rentPopoverState.pendingMax = rentMax;
      rentPopoverState.includeMgmtFee = Boolean(includeMgmtFee);
      rentPopoverState.noDeposit = Boolean(noDeposit);
    }
    if (!catPopoverOpen) {
      catPopoverState.pendingCategories = (categories && categories.length) ? [...categories] : (category !== 'all' ? [category] : []);
    }

    // 各トリガーボタンのアクティブバッジ表示判定（画像表示箇所のバッジには市名を表示）
    const isAreaActive = (activeCities.length > 0) || (pref !== 'all');
    let areaBadgeText = '';
    if (activeCities.length > 0) {
      areaBadgeText = activeCities.length === 1 ? activeCities[0] : `${activeCities[0]} 他${activeCities.length - 1}`;
    }
    const areaBadgeHtml = areaBadgeText ? `<span class="filter-active-pill">${escapeHtml(areaBadgeText)}</span>` : '';

    const isRentActive = (rentMin > 0) || (rentMax < 500000) || includeMgmtFee || noDeposit;
    let rentBadgeText = '';
    if (rentMin > 0 && rentMax < 500000) {
      rentBadgeText = `${formatRent(rentMin)}〜${formatRent(rentMax)}万`;
    } else if (rentMin > 0) {
      rentBadgeText = `${formatRent(rentMin)}万〜`;
    } else if (rentMax < 500000) {
      rentBadgeText = `〜${formatRent(rentMax)}万`;
    } else if (isRentActive) {
      rentBadgeText = '条件あり';
    }
    const rentBadgeHtml = rentBadgeText ? `<span class="filter-active-pill">${escapeHtml(rentBadgeText)}</span>` : '';

    const activeCatCount = (categories && categories.length) || (category !== 'all' ? 1 : 0);
    const isCatActive = activeCatCount > 0;
    const catBadgeHtml = isCatActive ? `<span class="filter-active-pill">${activeCatCount}業種</span>` : '';

    let areaDisplay = '宇都宮市';
    if (activeCities.length > 0) areaDisplay = activeCities.join('・');
    else if (pref !== 'all') areaDisplay = pref;

    const cardsHtml = props.map(p => {
      const d1 = p.topDemands?.[0] || { label: 'カフェ・喫茶店', votes: 148 };
      const d2 = p.topDemands?.[1] || { label: 'パン・ベーカリー', votes: 96 };
      const d3 = p.topDemands?.[2] || null;

      const totalVotes = p.votesCount || (d1.votes + (d2 ? d2.votes : 0) + (d3 ? d3.votes : 0)) || 100;
      const pct1 = Math.min(100, Math.max(10, Math.round((d1.votes / totalVotes) * 100)));
      const pct2 = d2 ? Math.min(100 - pct1, Math.max(8, Math.round((d2.votes / totalVotes) * 100))) : 0;
      const pct3 = d3 ? Math.min(100 - pct1 - pct2, Math.max(5, Math.round((d3.votes / totalVotes) * 100))) : 0;

      const rentMan = formatRent(p.rent);
      const mgmt = p.managementFee == null ? '管理費等：記載なし' : `管理費等 ${formatMoney(p.managementFee)}円`;
      const isPrClass = p.isPr ? 'is-pr' : '';

      return `
        <article class="property-card ${isPrClass}" data-id="${p.id}" onmouseenter="window.MachirikuMap && MachirikuMap.highlightProperty('${p.id}')" onmouseleave="window.MachirikuMap && MachirikuMap.clearHighlight()" onfocusin="window.MachirikuMap && MachirikuMap.highlightProperty('${p.id}')" onfocusout="window.MachirikuMap && MachirikuMap.clearHighlight()">
          <div class="card-top-badges">
            ${p.isPr ? '<span class="badge-pr">PR注目</span>' : ''}
            <span class="badge-status status-${p.status}">${p.statusLabel}</span>
          </div>
          <div class="card-body-layout">
            <div class="card-thumb-wrap">
              ${renderPropertyImage(p, 'class="card-thumb-svg"')}
            </div>
            <div class="card-info-pane">
              <div>
                <h2 class="card-prop-title"><a class="property-title-link" href="#s02?id=${encodeURIComponent(p.id)}">${escapeHtml(p.title)}</a></h2>
                <div class="card-prop-address">${escapeHtml(p.address)}（${escapeHtml(p.access)}）</div>
                <div class="card-rent-row">
                  <div class="rent-main">${rentMan}万円<small>/月</small></div>
                  <div class="rent-sub">${mgmt}</div>
                </div>
                <div class="card-specs-row">
                  <span><strong>${p.areaTsubo}</strong> 坪 (${p.areaSqm}㎡)</span>
                  <span><strong>${escapeHtml(p.floor)}</strong></span>
                  <span>${escapeHtml(p.condition)}</span>
                </div>
              </div>

              <!-- 住民需要スコア & 需要バー (1位表示 + 2・3位トグル展開) -->
              <div class="demand-preview-box">
                <div class="demand-box-header">
                  <div class="demand-box-score">
                    住民需要スコア <span class="demand-votes-count">(総計 ${p.votesCount}票)</span>
                  </div>
                </div>
                <div class="demand-stacked-bar" role="progressbar" aria-label="需要ランキング内訳">
                  <span class="demand-seg seg-1" style="width: ${pct1}%;" title="1位: ${escapeHtml(d1.label)} (${d1.votes}票 / ${pct1}%)"></span>
                  ${d2 ? `<span class="demand-seg seg-2" style="width: ${pct2}%;" title="2位: ${escapeHtml(d2.label)} (${d2.votes}票 / ${pct2}%)"></span>` : ''}
                  ${d3 ? `<span class="demand-seg seg-3" style="width: ${pct3}%;" title="3位: ${escapeHtml(d3.label)} (${d3.votes}票 / ${pct3}%)"></span>` : ''}
                </div>
                <div class="demand-box-body">
                  <div class="demand-rank-primary-row">
                    <span class="demand-chip"><span class="chip-color c-1"></span><strong>1位:</strong> ${escapeHtml(d1.label)} (${d1.votes}票)</span>
                    ${d2 ? `
                    <button type="button" class="btn-demand-toggle" onclick="event.preventDefault(); event.stopPropagation(); MachirikuScreens.toggleDemandAccordion('${p.id}', this);" aria-expanded="false" title="2位・3位の需要を表示">
                      <svg class="chevron-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="6 9 12 15 18 9"/></svg>
                    </button>` : ''}
                  </div>
                  ${d2 ? `
                  <div id="demand-subranks-${p.id}" class="demand-subranks" style="display: none;">
                    <span class="demand-chip"><span class="chip-color c-2"></span><strong>2位:</strong> ${escapeHtml(d2.label)} (${d2.votes}票)</span>
                    ${d3 ? `<span class="demand-chip"><span class="chip-color c-3"></span><strong>3位:</strong> ${escapeHtml(d3.label)} (${d3.votes}票)</span>` : ''}
                  </div>` : ''}
                  <div class="demand-vote-action-row">
                    <a href="#s03?id=${p.id}" class="demand-vote-btn" title="この物件に欲しいお店を投票">+ この物件に欲しいお店を投票</a>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </article>
      `;
    }).join("");

    return `
      <section class="screen-view s01-view" data-screen="S01">
        <!-- 検索バー (アットホーム風 絞り込み条件) -->
        ${renderHeaderBanner("S01", "空き店舗を探す", "", "")}
        <div class="portal-search-bar">
          <form id="s01-search-form" onsubmit="event.preventDefault(); MachirikuScreens.handleS01Search(this);">
            <div class="search-filter-grid">
              <div class="form-group" style="margin-bottom: 0;">
                <label for="search-keyword">物件名・通り名</label>
                <div class="search-input-wrap">
                  <input id="search-keyword" type="search" name="keyword" class="form-control search-keyword-input" placeholder="建物名・部屋番号・備考等" value="${escapeHtml(keyword)}">
                  <button type="submit" class="btn-search-inside" aria-label="検索する">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                    <span>検索</span>
                  </button>
                </div>
              </div>
            </div>
            
            <!-- 3連ドロップダウントリガー (エリア ▾ / 賃料 ▾ / 業種 ▾) ＋ 右側に条件クリアボタン -->
            <div class="search-filter-triggers-row" role="toolbar" aria-label="絞り込みメニュー">
              <!-- エリア ▾ (モーダル展開) -->
              <button type="button" class="btn-filter-trigger ${isAreaActive ? 'is-active' : ''}" onclick="MachirikuScreens.openAreaModal()">
                <span>エリア</span>
                ${areaBadgeHtml}
                <span class="trigger-caret">▾</span>
              </button>

              <!-- 賃料 ▾ (ボタン直下コンパクトポップオーバー) -->
              <div class="filter-popover-anchor" id="rent-popover-anchor">
                <button type="button" class="btn-filter-trigger ${rentPopoverOpen ? 'is-open' : ''} ${isRentActive ? 'is-active' : ''}" onclick="event.stopPropagation(); MachirikuScreens.toggleRentPopover()">
                  <span>賃料</span>
                  ${rentBadgeHtml}
                  <span class="trigger-caret">▾</span>
                </button>
                ${rentPopoverOpen ? renderRentPopoverHtml(state) : ''}
              </div>

              <!-- 業種 ▾ (ボタン直下2列ポップオーバー) -->
              <div class="filter-popover-anchor" id="cat-popover-anchor">
                <button type="button" class="btn-filter-trigger ${catPopoverOpen ? 'is-open' : ''} ${isCatActive ? 'is-active' : ''}" onclick="event.stopPropagation(); MachirikuScreens.toggleCatPopover()">
                  <span>業種</span>
                  ${catBadgeHtml}
                  <span class="trigger-caret">▾</span>
                </button>
                ${catPopoverOpen ? renderCatPopoverHtml(state) : ''}
              </div>

              <!-- 画像の部分の右側に配置する条件クリアボタン -->
              <button type="button" class="btn-filter-clear" onclick="MachirikuScreens.resetSearch()" title="すべての絞り込み条件をクリア">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                <span>条件をクリア</span>
              </button>
            </div>
          </form>
        </div>

        <!-- 2カラム分割: 物件リスト × Mapbox風ベクター地図 -->
        <div class="portal-split-layout">
          <!-- 左カラム: 物件リスト -->
          <div class="property-cards-column">
            <div class="results-meta-bar">
              <div class="results-count">${escapeHtml(areaDisplay)}の店舗・事務所: <strong>${props.length}</strong> 件表示中</div>
              <div class="results-sort-wrap">
                <label class="results-sort-label" for="field-1">並び順:</label>
                <select id="field-1" class="form-select results-sort-select" onchange="MachirikuScreens.handleSortChange(this.value)">
                  <option value="score_desc" ${sort === 'score_desc' ? 'selected' : ''}>住民需要スコア順</option>
                  <option value="votes_desc" ${sort === 'votes_desc' ? 'selected' : ''}>投票総数順</option>
                  <option value="rent_asc" ${sort === 'rent_asc' ? 'selected' : ''}>賃料が安い順</option>
                  <option value="rent_desc" ${sort === 'rent_desc' ? 'selected' : ''}>賃料が高い順</option>
                  <option value="area_desc" ${sort === 'area_desc' ? 'selected' : ''}>面積が広い順</option>
                </select>
              </div>
            </div>

            <div class="property-cards-feed" id="property-cards-feed">
              ${cardsHtml || `<div class="empty-state"><h2>条件に合う物件が見つかりません</h2><p>キーワードや賃料の条件を変えて、もう一度お試しください。</p><button type="button" class="btn-secondary" onclick="MachirikuScreens.resetSearch()">検索条件を解除する</button></div>`}
            </div>
          </div>

          <!-- 右カラム: Google Maps API カスタムマップ -->
          <div class="portal-map-pane" id="portal-map-pane">
            <div id="google-map-canvas" class="google-map-canvas" style="width:100%; height:100%;"></div>
          </div>
        </div>

        <!-- エリア選択 モーダルウィンドウ (中央展開) -->
        ${areaModalState.isOpen ? renderAreaModalHtml(state) : ''}
      </section>
    `;
  }

  // =========================================================================
  // S02: 物件詳細 (アットホーム × Decidim 仕様)
  // =========================================================================
  function renderS02(params) {
    const propId = params?.id || store.getProperties()[0]?.id;
    const prop = store.getPropertyById(propId);
    if (!prop) return renderEmptyState('S02', "物件が見つかりません", "掲載が終了したか、まだ物件が登録されていません。", "#s01", "物件一覧へ");
    const state = store.getState();
    const isFollowed = state.currentUser.followedProps?.includes(prop.id);
    const hasVoted = Boolean(state.currentUser.votedProps?.[prop.id]);
    const rentMan = formatRent(prop.rent);
    const mgmt = prop.managementFee == null ? '掲載なし（要確認）' : `${formatMoney(prop.managementFee)}円`;

    const top3DemandsHtml = prop.topDemands.slice(0, 3).map((d, idx) => {
      const sub = data.SUBCATS[d.subcatId] || { name: d.label, color: "var(--primary-600)", icon: "" };
      const percent = Math.min(Math.round((d.score / (prop.demandScore || 1)) * 100), 100);
      return `
        <div class="demand-bar-item">
          <div class="demand-bar-header" style="font-size:0.85rem;">
            <span>第${idx + 1}位: ${sub.icon} ${sub.name}</span>
            <span style="color:var(--primary-700); font-weight:700;">${d.votes}票 (${percent}%)</span>
          </div>
          <div class="demand-progress-track" style="height:8px; background:rgba(0,0,0,0.06);">
            <div class="demand-progress-fill" style="width: ${percent}%; background-color: ${sub.color};"></div>
          </div>
        </div>
      `;
    }).join("");

    const topDemandsHtml = prop.topDemands.map((d, idx) => {
      const sub = data.SUBCATS[d.subcatId] || { name: d.label, color: "var(--primary-600)", icon: "" };
      const percent = Math.min(Math.round((d.score / (prop.demandScore || 1)) * 100), 100);
      return `
        <div class="demand-bar-item">
          <div class="demand-bar-header">
            <span>第${idx + 1}位: ${sub.icon} ${sub.name}</span>
            <span style="color:var(--primary-700);">${d.votes}票 / <strong>${d.score} pt</strong> (${percent}%)</span>
          </div>
          <div class="demand-progress-track">
            <div class="demand-progress-fill" style="width: ${percent}%; background-color: ${sub.color};"></div>
          </div>
        </div>
      `;
    }).join("");

    const residentVoicesHtml = (prop.residentVoices || []).map(v => `
      <div class="quote-bubble">
        <p class="quote-text">“${escapeHtml(v.text)}”</p>
        <div class="quote-meta">
          <span>${escapeHtml(v.author)}</span>
          <span>${escapeHtml(v.date)}</span>
        </div>
      </div>
    `).join("");

    const defaultExterior = {
      index: 0,
      kind: 'exterior',
      label: prop.imageCaption || `${prop.title}の外観`,
      url: prop.coverImage || IMAGE_PLACEHOLDER,
      sourceUrl: prop.coverImage
    };

    const galleryItems = (prop.gallery && prop.gallery.length)
      ? prop.gallery.map((it, idx) => {
          const unavailable = !it.url || it.url === IMAGE_PLACEHOLDER || failedImages.has(it.url);
          return {
            ...it,
            index: idx,
            url: unavailable ? IMAGE_PLACEHOLDER : it.url,
            isUnavailable: unavailable
          };
        })
      : [defaultExterior];

    let initialIdx = 0;
    if (currentGallery.propId === prop.id && galleryItems[currentGallery.index]) {
      initialIdx = currentGallery.index;
    } else {
      const extIdx = galleryItems.findIndex(it => it.kind === 'exterior');
      initialIdx = extIdx >= 0 ? extIdx : 0;
    }

    currentGallery = {
      propId: prop.id,
      index: initialIdx,
      items: galleryItems
    };

    setTimeout(() => {
      initGallerySlider();
    }, 0);

    const activeItem = galleryItems[initialIdx] || galleryItems[0];
    const kindLabel = getKindLabel(activeItem.kind);

    return `
      <section class="screen-view s02-view" data-screen="S02">
        ${renderHeaderBanner("S02", prop.title, prop.statusLabel, prop.subtitle, [{ label: "物件検索", hash: "#s01" }])}

        <div class="detail-layout-grid">
          <!-- 左メインコンテンツ -->
          <div class="detail-main-pane">
            <!-- 写真ギャラリー (アットホーム × HOME'S仕様 カルーセル) -->
            <div class="detail-gallery-box" id="s02-gallery-box" data-prop="${prop.id}">
              <!-- メイン写真プレビュー -->
              <div class="gallery-main-container">
                ${galleryItems.length > 1 ? `
                  <button type="button" class="gallery-nav-btn prev" onclick="MachirikuScreens.navGallery(-1)" aria-label="前の写真">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
                  </button>
                ` : ''}
                <div class="gallery-main-view" id="gallery-main-view">
                  <div class="gallery-slider-track" id="gallery-slider-track" style="transform: translateX(-${initialIdx * 100}%);">
                    ${galleryItems.map((item, idx) => `
                      <div class="gallery-slide-item" data-index="${idx}">
                        <img src="${escapeHtml(item.url)}" alt="${escapeHtml(prop.title)} - ${escapeHtml(item.label || '')}" loading="${idx === initialIdx ? 'eager' : 'lazy'}" decoding="async" referrerpolicy="no-referrer" onerror="MachirikuScreens.handlePropertyImageError(this)">
                      </div>
                    `).join('')}
                  </div>
                </div>
                ${galleryItems.length > 1 ? `
                  <button type="button" class="gallery-nav-btn next" onclick="MachirikuScreens.navGallery(1)" aria-label="次の写真">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
                  </button>
                ` : ''}
                <!-- 右上: すべての写真ボタン -->
                <button type="button" class="btn-gallery-all-overlay" onclick="MachirikuScreens.openAllPhotosModal('${prop.id}')" title="すべての写真を一覧表示">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-1px; margin-right:4px;"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>すべての写真 (${galleryItems.length}枚)
                </button>
                <!-- 右下: 枚数カウンター -->
                <div class="gallery-counter-badge">
                  <span id="gallery-current-index">${initialIdx + 1}</span> / <span id="gallery-total-count">${galleryItems.length}</span>
                </div>
              </div>

              <!-- サムネイルカルーセル（添付画像完全準拠） -->
              ${galleryItems.length > 1 ? `
                <div class="gallery-carousel-wrapper">
                  <button type="button" class="carousel-arrow-btn prev" onclick="MachirikuScreens.scrollThumbs(-1)" aria-label="前のサムネイル">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
                  </button>
                  <div class="gallery-thumbs-track" id="gallery-thumbs-track">
                    ${galleryItems.map((img, i) => `
                      <button type="button" class="gallery-thumb-btn ${i === initialIdx ? 'is-selected' : ''}" data-index="${i}" onclick="MachirikuScreens.selectGalleryImage(${i})" title="${escapeHtml(img.label)}">
                        <img src="${escapeHtml(img.url)}" alt="${escapeHtml(img.label)}" loading="lazy" decoding="async" referrerpolicy="no-referrer" onerror="MachirikuScreens.handlePropertyImageError(this)">
                      </button>
                    `).join('')}
                  </div>
                  <button type="button" class="carousel-arrow-btn next" onclick="MachirikuScreens.scrollThumbs(1)" aria-label="次のサムネイル">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
                  </button>
                </div>
              ` : ''}
            </div>

            <!-- 物件概要カード (アットホーム仕様) -->
            <div class="detail-card">
              <h2 class="detail-card-title">物件募集概要</h2>
              <table class="specs-table">
                <tbody>
                  <tr>
                    <th>賃料 / 管理費等</th>
                    <td><strong style="font-size:1.3rem; color:var(--accent-price);">${rentMan}万円</strong> /月 （管理費等: ${mgmt}）${prop.source && prop.rentTaxStatus === 'unknown' ? '<br><small>税区分は掲載元に確認</small>' : ''}</td>
                  </tr>
                  <tr>
                    <th>敷金 / 礼金 / 保証金</th>
                    <td>敷金: ${escapeHtml(prop.deposit ?? '掲載なし（要確認）')} / 礼金: ${escapeHtml(prop.keyMoney ?? '掲載なし（要確認）')} / 保証金: ${escapeHtml(prop.securityDeposit ?? '掲載なし（要確認）')}</td>
                  </tr>
                  <tr>
                    <th>所在地</th>
                    <td>${escapeHtml(prop.address)}</td>
                  </tr>
                  ${prop.otherFees?.length ? `<tr><th>その他の掲載費用</th><td><ul class="listing-fees">${prop.otherFees.map(f => `<li>${escapeHtml(f.name)}: ${escapeHtml(f.amountRaw)}</li>`).join('')}</ul></td></tr>` : ''}
                  <tr>
                    <th>交通・最寄駅</th>
                    <td>${escapeHtml(prop.access)}</td>
                  </tr>
                  <tr>
                    <th>使用部分面積</th>
                    <td><strong>${prop.areaTsubo}坪</strong> （${prop.areaSqm}㎡）</td>
                  </tr>
                  <tr>
                    <th>階数・構造</th>
                    <td>${escapeHtml(prop.floor)} / ${escapeHtml(prop.structure)}（${escapeHtml(prop.builtYear)}）</td>
                  </tr>
                  <tr>
                    <th>現況 / 引渡し</th>
                    <td>${escapeHtml(prop.listingStatusRaw || '掲載なし')} / ${escapeHtml(prop.handover)}</td>
                  </tr>
                  <tr>
                    <th>業種条件</th>
                    <td>${escapeHtml(prop.condition)}</td>
                  </tr>
                  <tr>
                    <th>主要設備</th>
                    <td>
                      <div style="display:flex; flex-wrap:wrap; gap:0.4rem;">
                        ${(prop.facilities || ['電気','上水道','下水道']).map(fac => `<span class="badge-feature">${escapeHtml(fac)}</span>`).join('')}
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <th>飲食店営業許可</th>
                    <td><span class="badge-check-ok">✔ ${escapeHtml(prop.permitStatus || "取得相談可")}</span></td>
                  </tr>
                  ${prop.sourceNotes?.length ? `<tr><th>掲載情報の備考</th><td><ul class="listing-fees">${prop.sourceNotes.map(n => `<li>${escapeHtml(n)}</li>`).join('')}</ul></td></tr>` : ''}
                </tbody>
              </table>

              ${prop.source ? renderSourceInformation(prop) : `
                <div style="margin-top:1.25rem; padding-top:1rem; border-top:1px dashed var(--border-color); display:flex; justify-content:space-between; align-items:center; font-size:0.875rem; color:var(--text-sub);">
                  <div>
                    <strong>取扱不動産会社:</strong> ${escapeHtml(prop.agent ? prop.agent.name : (prop.broker ? prop.broker.name : "株式会社宇都宮不動産 本店"))}<br>
                    <span style="font-size:0.8rem; color:var(--text-muted);">${escapeHtml(prop.agent ? prop.agent.license : "栃木県知事 (4) 第12345号")} / 免許番号・取引態様: 専任媒介</span>
                  </div>
                  <div style="text-align:right;">
                    <span style="font-size:0.75rem; color:var(--text-muted);">物件番号: ${encodeURIComponent(prop.id)}</span>
                  </div>
                </div>
              `}

              <div style="margin-top:1.25rem; font-size:0.9rem; line-height:1.7; color:var(--text-sub);">
                ${escapeHtml(prop.description).replace(/\n/g, '<br>')}
              </div>
            </div>

            <!-- Decidim風 住民需要分析セクション -->
            <div class="demand-civic-box">
              <h2 class="detail-card-title" style="color:var(--primary-700); border-bottom-color:var(--border-color);">
                住民の需要データ・リクエスト状況
              </h2>
              <p style="font-size:0.875rem; color:var(--primary-800); margin-bottom:1.25rem;">
                マチリクでは、近隣住民が『この空き店舗に欲しい業種』を1位〜3位の順位投票で集計しています。
              </p>
              
              <div class="civic-kpi-bar">
                <div class="civic-kpi-item">
                  <div class="civic-kpi-label">需要総合スコア</div>
                  <div class="civic-kpi-val">${prop.demandScore} <span style="font-size:1rem;">pt</span></div>
                </div>
                <div class="civic-kpi-item">
                  <div class="civic-kpi-label">住民投票総数</div>
                  <div class="civic-kpi-val">${prop.votesCount} <span style="font-size:1rem;">人</span></div>
                </div>
                <div class="civic-kpi-item">
                  <div class="civic-kpi-label">最多リクエスト</div>
                  <div class="civic-kpi-val" style="font-size:1.15rem; margin-top:0.6rem;">
                    ${prop.topDemands[0]?.label || '未集計'}
                  </div>
                </div>
              </div>

              <h3 style="font-size:0.95rem; font-weight:800; color:var(--primary-700); margin-bottom:0.75rem;">希望業種ランキング Top 4</h3>
              <div class="demand-bar-list">
                ${topDemandsHtml || '<p class="sample-note">まだ投票がありません。</p>'}
              </div>

            </div>
          </div>

          <!-- 右サイドバー: アクションCTA -->
          <div class="detail-side-pane">
            <div class="cta-box-card">
              ${(state.currentRole === 'business' || state.currentRole === 'owner') ? `
                <div class="cta-price-banner">
                  <div style="font-size:0.875rem; color:var(--text-muted);">月額賃料${prop.rentTaxStatus === 'inclusive' ? '（税込）' : prop.rentTaxStatus === 'exclusive' ? '（税別）' : ''}</div>
                  <div style="font-size:1.8rem; font-weight:900; color:var(--accent-price);">${rentMan}万円</div>
                  <div style="font-size:0.875rem; color:var(--text-sub);">管理費等: ${mgmt}</div>
                </div>
              ` : `
                <div class="cta-demand-summary-banner">
                  <div style="font-size:0.95rem; font-weight:800; color:var(--primary-800); margin-bottom:0.75rem;">
                    人気ランキング Top 3
                  </div>
                  <div class="demand-bar-list" style="margin-bottom:0; gap:0.65rem;">
                    ${top3DemandsHtml || '<p class="sample-note">まだ投票がありません。</p>'}
                  </div>
                </div>
              `}

              <div class="cta-buttons-stack">
                ${state.currentRole === 'resident' ? `
                  <a class="btn-cta-vote" href="#s03?id=${prop.id}">${hasVoted ? '投票内容を変更する' : '欲しいお店に投票する'}</a>
                ` : state.currentRole === 'business' ? `
                  <a class="btn-cta-apply" href="#s08?id=${prop.id}">この物件に出店を申し込む</a>
                ` : state.currentRole === 'owner' && prop.ownerId === state.currentUser.id ? `
                  <a class="btn-cta-apply" href="#s12?id=${prop.id}">物件の掲載内容を編集する</a>
                ` : `
                  <a class="btn-cta-apply" href="#s07?id=${prop.id}">この物件の需要を確認する</a>
                `}
                <button type="button" class="btn-secondary" data-follow-id="${prop.id}" aria-pressed="${Boolean(isFollowed)}" style="width:100%; justify-content:center;" onclick="MachirikuScreens.handleToggleFollow('${prop.id}')">
                  ${isFollowed ? 'フォロー中・解除する' : '開店のようすをフォローする'}
                </button>
              </div>

              <hr style="border:none; border-top:1px solid var(--border-color); margin:1.25rem 0;">

              ${state.currentRole !== 'resident' ? `<nav aria-label="物件に関する情報">
                <a class="secondary-action" href="#s07?id=${prop.id}">住民の需要を見る</a>
                <a class="secondary-action" href="#s05?id=${prop.id}">開店の予定を見る</a>
                ${state.currentRole === 'owner' ? `<a class="secondary-action" href="#s33?id=${prop.id}">PR掲載について</a>` : ''}
              </nav>` : ''}
            </div>

            <!-- 物件所在地GoogleMapカード（独立カード：横幅・左端位置を上位CTAカード内要素と完全同期） -->
            <div class="detail-card" style="padding:1rem var(--space-6); display:block;">
              <div style="font-weight:700; font-size:0.875rem; margin-bottom:0.5rem; color:var(--text-main);">所在地・周辺情報</div>
              <div style="font-size:0.875rem; color:var(--text-sub); margin-bottom:0.75rem;">
                ${escapeHtml(prop.address)}
              </div>
              <div id="s02-location-map" data-lat="${prop.location?.lat || 36.559312}" data-lng="${prop.location?.lng || 139.882105}" data-title="${escapeHtml(prop.title)}" style="width:100%; height:200px; border-radius:var(--radius-sm); border:1px solid var(--border-color); overflow:hidden; background:var(--bg-subtle);"></div>
            </div>
          </div>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S03: 順位投票・任意アンケート (Decidim Japan × formrun 仕様)
  // =========================================================================
  function renderS03(params) {
    const propId = params?.id || store.getProperties()[0]?.id;
    const prop = store.getPropertyById(propId);
    if (!prop) return renderEmptyState('S03', "物件が見つかりません", "掲載が終了したか、まだ物件が登録されていません。", "#s01", "物件一覧へ");
    const state = store.getState();
    const curVote = state.currentUser.votedProps?.[prop.id];

    return `
      <section class="screen-view s03-view" data-screen="S03">
        ${renderHeaderBanner("S03", "欲しい出店ジャンルの順位投票・アンケート", "住民参加", "空き店舗に誘致したい業種を第1〜第3希望まで順位投票します。生活拠点からの距離に応じた重み付けで正確な需要を算出します。", [{ label: "物件詳細", hash: `#s02?id=${prop.id}` }])}

        <div class="form-stepper-box">
          <div class="stepper-header">
            <h2 style="font-size:1.3rem; font-weight:800; color:var(--text-main); margin-bottom:0.35rem;">
              【対象物件】${escapeHtml(prop.title)}
            </h2>
            <div style="font-size:0.875rem; color:var(--text-muted);">
              ${escapeHtml(prop.address)}（現在 ${prop.votesCount}人の住民が投票中）
            </div>
          </div>

          <!-- 距離係数の説明表示カード (テーブル設計書・企画書準拠) -->
          <details class="distance-weight-notice"><summary>投票の集計方法</summary><p>第1希望を最も重く集計します。登録した生活拠点からの距離に応じて、近隣の方の声を重く反映します。</p></details>
          <form id="s03-vote-form" onsubmit="event.preventDefault(); MachirikuScreens.handleVoteSubmit('${prop.id}', this);">
            <!-- 第1希望 (必須 1.0pt) -->
            <div class="rank-picker-card">
              <span class="rank-badge-pill rank-badge-1">第1希望（必須）</span>
              <div class="form-group">
                <label for="field-2">最も出店してほしい業種を選択してください</label>
                <select id="field-2" name="rank1" class="form-select" required onchange="MachirikuScreens.validateRanks(this.form)">
                  <option value="">選択してください</option>
                  ${data.CATEGORIES.map(c => `
                    <optgroup label="${c.name}">
                      ${c.subcategories.map(s => `
                        <option value="${s.id}" ${curVote?.ranks?.[0] === s.id ? 'selected' : ''}>${s.icon} ${s.name}</option>
                      `).join("")}
                    </optgroup>
                  `).join("")}
                </select>
              </div>
            </div>

            <!-- 第2希望 (任意 0.6pt) -->
            <div class="rank-picker-card">
              <span class="rank-badge-pill rank-badge-2">第2希望（任意）</span>
              <div class="form-group">
                <label for="field-3">2番目に出店してほしい業種（任意）</label>
                <select id="field-3" name="rank2" class="form-select" onchange="MachirikuScreens.validateRanks(this.form)">
                  <option value="">希望なし</option>
                  ${data.CATEGORIES.map(c => `
                    <optgroup label="${c.name}">
                      ${c.subcategories.map(s => `
                        <option value="${s.id}" ${curVote?.ranks?.[1] === s.id ? 'selected' : ''}>${s.icon} ${s.name}</option>
                      `).join("")}
                    </optgroup>
                  `).join("")}
                </select>
              </div>
            </div>

            <!-- 第3希望 (任意 0.3pt) -->
            <div class="rank-picker-card">
              <span class="rank-badge-pill rank-badge-3">第3希望（任意）</span>
              <div class="form-group">
                <label for="field-4">3番目に出店してほしい業種（任意）</label>
                <select id="field-4" name="rank3" class="form-select" onchange="MachirikuScreens.validateRanks(this.form)">
                  <option value="">希望なし</option>
                  ${data.CATEGORIES.map(c => `
                    <optgroup label="${c.name}">
                      ${c.subcategories.map(s => `
                        <option value="${s.id}" ${curVote?.ranks?.[2] === s.id ? 'selected' : ''}>${s.icon} ${s.name}</option>
                      `).join("")}
                    </optgroup>
                  `).join("")}
                </select>
              </div>
            </div>

            <!-- 任意需要アンケート -->
            <div style="background-color:var(--bg-card);   padding:1.25rem; margin-top:1.5rem;">
              <h3 style="font-size:1rem; font-weight:800; margin-bottom:1rem; color:var(--text-main);">
                店舗利用についての任意アンケート
              </h3>
              <div class="form-group" style="margin-bottom:1rem;">
                <label for="field-5">出店された場合の利用頻度想定</label>
                <select id="field-5" name="frequency" class="form-select">
                  <option value="週2〜3回">週2〜3回以上</option>
                  <option value="週1回程度" selected>週1回程度</option>
                  <option value="月2〜3回">月2〜3回</option>
                  <option value="月1回以下">月1回以下</option>
                </select>
              </div>
              <div class="form-group" style="margin-bottom:1rem;">
                <label for="field-6">主に利用したい時間帯</label>
                <select id="field-6" name="timing" class="form-select">
                  <option value="ランチタイム（11:30〜14:00）">昼・ランチタイム</option>
                  <option value="カフェタイム（14:00〜17:00）" selected>午後・カフェタイム</option>
                  <option value="ディナー・夜（18:00〜22:00）">夜・ディナー・バータイム</option>
                  <option value="朝・通勤前（7:00〜9:00）">朝・モーニング</option>
                </select>
              </div>
              <div class="form-group">
                <label for="field-7">出店する事業主・店舗への要望や応援メッセージ（自由記述）</label>
                <textarea id="field-7" name="comment" class="form-control" rows="3" placeholder="例: オリオン通りでゆっくり本が読める静かなカフェがあると嬉しいです。テイクアウトもできると助かります。">${escapeHtml(curVote?.comment || '')}</textarea>
              </div>
            </div>

            <div style="margin-top:2rem; display:flex; gap:1rem; justify-content:flex-end;">
              <button type="button" class="btn-secondary" onclick="MachirikuApp.navigate('s02', { id: '${prop.id}' })">戻る</button>
              <button type="submit" class="btn-primary" style="padding:0.75rem 2rem; font-size:1rem;">投票を確定して送信する</button>
            </div>
          </form>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S04: フォロー物件一覧 (地域住民)
  // =========================================================================
  function renderS04() {
    const state = store.getState();
    const followedIds = state.currentUser.followedProps || [];
    const followedProps = state.properties.filter(p => followedIds.includes(p.id));

    return `
      <section class="screen-view s04-view" data-screen="S04">
        ${renderHeaderBanner("S04", "フォローした空き店舗", "地域住民", "あなたが関心を持ってフォローした物件の一覧です。住民投票の進捗や出店事業主決定の通知が届きます。")}

        ${followedProps.length === 0 ? `
          <div class="detail-card" style="text-align:center; padding:3rem 1.5rem;">
            
            <h3 style="font-size:1.15rem; font-weight:800; margin-bottom:0.5rem;">フォローしている物件はありません</h3>
            <p style="font-size:0.88rem; color:var(--text-muted); margin-bottom:1.5rem;">
              物件詳細画面の「 物件をフォローする」ボタンを押すと、この一覧に追加され開店までの進捗通知を受け取れます。
            </p>
            <a href="#s01" class="btn-primary">物件をさがす</a>
          </div>
        ` : `
          <div style="display:grid; grid-template-columns:repeat(auto-fill, minmax(340px, 1fr)); gap:1.25rem;">
            ${followedProps.map(p => `
              <div class="property-card" onclick="MachirikuApp.navigate('s02', { id: '${p.id}' })">
                <div class="card-top-badges">
                  <span class="badge-status status-${p.status}">${p.statusLabel}</span>
                </div>
                <div style="height:160px; overflow:hidden;">
                  ${renderPropertyImage(p, 'style="width:100%; height:100%; object-fit:cover;"')}
                </div>
                <div style="padding:1rem;">
                  <h3 style="font-size:1.05rem; font-weight:800; margin-bottom:0.35rem;">${escapeHtml(p.title)}</h3>
                  <div style="font-size:0.875rem; color:var(--text-muted); margin-bottom:0.75rem;">${escapeHtml(p.address)}</div>
                  <div style="display:flex; justify-content:space-between; align-items:center;">
                    <span style="font-size:0.875rem; font-weight:700; color:var(--primary-700);"> 需要スコア: ${p.demandScore}pt</span>
                    <span class="btn-header-action" style="font-size:0.875rem;">詳細へ </span>
                  </div>
                </div>
              </div>
            `).join("")}
          </div>
        `}
      </section>
    `;
  }

  // =========================================================================
  // S05: 出店計画・進捗閲覧 (Decidim Japan × まちのコイン 仕様)
  // =========================================================================
  function renderS05(params = {}) {
    const state = store.getState();
    const preparingProp = params.id ? store.getPropertyById(params.id) : state.properties.find(p => p.storePlan || p.status === 'preparing' || p.status === 'opened');
    if (!preparingProp) return renderEmptyState('S05', '開店のお知らせはまだありません', '気になる物件をフォローして、開店までの動きを確認しましょう。');
    const application = state.applications.find(a => a.propId === preparingProp.id && a.status !== 'rejected');
    if (preparingProp.source && !preparingProp.storePlan && !application) return renderEmptyState('S05', '開店計画はまだありません', `${escapeHtml(preparingProp.title)}の出店・開店情報はまだ登録されていません。`, `#s02?id=${encodeURIComponent(preparingProp.id)}`, '物件の詳細を見る');
    const plan = preparingProp.storePlan || {};
    const stages = ['希望を集める', '出店者を選ぶ', '契約・引き渡し', '開店の準備', '開店'];
    const stage = Math.max(0, ['voting','reviewing','agreed','preparing','opened'].indexOf(preparingProp.status));
    const followed = state.currentUser.followedProps?.includes(preparingProp.id);
    const coupons = state.coupons.filter(c => c.propId === preparingProp.id);
    return `
      <section class="screen-view s05-view" data-screen="S05">
        ${renderHeaderBanner('S05', '', '', '')}
        <div class="detail-layout-grid">
          <div class="detail-main-pane">
            <div class="detail-gallery-box">${renderPropertyImage(preparingProp, 'class="progress-image"')}</div>
            <div class="detail-card">
              <h2 class="detail-card-title">${escapeHtml(preparingProp.title)}</h2>
              <ol class="opening-steps" aria-label="開店までの進み具合">
                ${stages.map((label, index) => `<li class="${index < stage ? 'is-complete' : index === stage ? 'is-current' : ''}" ${index === stage ? 'aria-current="step"' : ''}><span class="step-number">${index + 1}</span><span>${label}${index === stage ? '<small>現在</small>' : ''}</span></li>`).join('')}
              </ol>
            </div>
            <div class="story-card-box">
              <h2 class="detail-card-title">${escapeHtml(plan.storeName || application?.storeName || '出店計画の公開をお待ちください')}</h2>
              <p class="sample-note">開店予定: ${escapeHtml(plan.openDate || application?.targetOpenDate || '未定')}</p>
              <p class="story-content-body">${escapeHtml(plan.message || application?.concept || '店名や開店日が決まると、ここにお知らせが届きます。')}</p>
            </div>
            <div class="detail-card">
              <h2 class="detail-card-title">地域の声</h2>
              ${(preparingProp.residentVoices || []).map(v => `<div class="quote-bubble"><p>${escapeHtml(v.text)}</p><p class="quote-meta">${escapeHtml(v.author)}</p></div>`).join('') || '<p>まだ声が寄せられていません。</p>'}
            </div>
          </div>
          <div class="detail-side-pane">
            <div class="cta-box-card">
              <h2 class="detail-card-title">開店のお知らせ</h2>
              <p class="sample-note">この物件の動きを、お知らせで確認できます。</p>
              <button type="button" class="btn-primary" data-follow-id="${preparingProp.id}" aria-pressed="${Boolean(followed)}" onclick="MachirikuScreens.handleToggleFollow('${preparingProp.id}')">${followed ? 'フォロー中・解除する' : 'この物件をフォローする'}</button>
              ${coupons.length ? '<a class="secondary-action" href="#s30">受け取ったクーポンを見る</a>' : ''}
              <a class="secondary-action" href="#s02?id=${preparingProp.id}">物件の詳細を見る</a>
            </div>
          </div>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S06: 通知一覧 (全員)
  // =========================================================================
  function renderS06() {
    const state = store.getState();
    const notifs = state.notifications || [];
    if (!notifs.length) return renderEmptyState('S06', 'お知らせはまだありません', '物件のフォローや投票をすると、ここで動きを確認できます。');

    return `
      <section class="screen-view s06-view" data-screen="S06">
        ${renderHeaderBanner("S06", "通知一覧", "共通", "投票した物件の進捗、配布されたクーポン、申込のステータス更新をお知らせします。")}

        <div class="data-table-wrapper" style="max-width:850px; margin:0 auto;">
          <div style="padding:1rem 1.25rem; border-bottom:1px solid var(--border-color); display:flex; justify-content:space-between; align-items:center;">
            <span style="font-weight:700; font-size:0.95rem;">すべてのおしらせ（${notifs.length}件）</span>
            <button type="button" class="btn-secondary" style="font-size:0.875rem; padding:0.25rem 0.6rem;" onclick="MachirikuScreens.markAllNotifsRead()">すべて既読にする</button>
          </div>
          <div style="display:flex; flex-direction:column;">
            ${notifs.map(n => `
              <div style="padding:1.15rem 1.25rem; border-bottom:1px solid var(--border-color); display:flex; gap:1rem; align-items:flex-start; ${!n.isRead ? 'background-color:var(--primary-50);' : ''}">
                <div style="font-size:1.4rem;">
                  ${n.type === 'coupon' ? '' : (n.type === 'vote' ? '' : (n.type === 'progress' ? '' : ''))}
                </div>
                <div style="flex:1;">
                  <div style="display:flex; justify-content:space-between; align-items:baseline; margin-bottom:0.25rem;">
                    <h4 style="font-size:0.95rem; font-weight:800; color:var(--text-main);">${escapeHtml(n.title)}</h4>
                    <span style="font-size:0.875rem; color:var(--text-muted);">${escapeHtml(n.date)}</span>
                  </div>
                  <p style="font-size:0.875rem; color:var(--text-sub); line-height:1.5; margin-bottom:0.4rem;">${escapeHtml(n.body)}</p>
                  ${n.linkScreen ? `
                    <button type="button" class="btn-header-action" style="font-size:0.875rem; padding:0.2rem 0.5rem;" onclick="MachirikuApp.navigate('${n.linkScreen}')">
                      該当画面へ移動 
                    </button>
                  ` : ''}
                </div>
              </div>
            `).join("")}
          </div>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S07: 物件・地域の需要分析 (shadcn/ui Blocks 仕様)
  // =========================================================================
  function renderS07(params) {
    const state = store.getState();
    const props = state.properties;
    const selectedPropId = params?.id || "all";
    const selectedProp = selectedPropId !== "all" ? store.getPropertyById(selectedPropId) : null;

    let targetTitle, votesCount, demandScore, topGenreLabel, entryCount, topDemandsList, voicesList;

    if (selectedProp) {
      targetTitle = selectedProp.title;
      votesCount = selectedProp.votesCount;
      demandScore = selectedProp.demandScore;
      topGenreLabel = selectedProp.topDemands[0]?.label || "需要集計中";
      entryCount = state.applications.filter(a => a.propId === selectedProp.id).length;
      topDemandsList = selectedProp.topDemands || [];
      voicesList = selectedProp.residentVoices || [];
    } else {
      targetTitle = "宇都宮中心街 全物件総合需要";
      votesCount = props.reduce((acc, p) => acc + p.votesCount, 0);
      demandScore = Math.round(props.reduce((acc, p) => acc + p.demandScore, 0) * 10) / 10;
      const byGenre = new Map();
      props.forEach(p => (p.topDemands || []).forEach(d => {
        const total = byGenre.get(d.subcatId) || { ...d, votes: 0, score: 0 };
        total.votes += d.votes;
        total.score = Math.round((total.score + d.score) * 10) / 10;
        byGenre.set(d.subcatId, total);
      }));
      topDemandsList = [...byGenre.values()].sort((a,b) => b.score - a.score);
      topGenreLabel = topDemandsList[0]?.label || '未集計';
      entryCount = state.applications.length;
      voicesList = props.flatMap(p => p.residentVoices || []);
    }

    // 7大分類別比率の算出
    const catScores = data.CATEGORIES.map(c => {
      let score = 0;
      if (selectedProp && selectedProp.topDemands) {
        selectedProp.topDemands.forEach(d => {
          const sub = data.SUBCATS[d.subcatId];
          if (sub && sub.categoryId === c.id) score += d.score;
        });
      } else {
        score = topDemandsList.filter(d => data.SUBCATS[d.subcatId]?.categoryId === c.id).reduce((total,d) => total + d.score, 0);
      }
      return { ...c, score: Math.round(score * 10) / 10 };
    });
    const maxScore = Math.max(...catScores.map(c => c.score), 1);

    return `
      <section class="screen-view s07-view" data-screen="S07">
        ${renderHeaderBanner("S07", "物件・地域の需要詳細分析", "オーナー・事業主", "shadcn/ui Blocks風のメトリクスカードと業種分類別クロス集計で、街の需要を多角的に把握します。", [{ label: "物件検索", hash: "#s01" }])}

        <!-- 物件切替セレクターバー -->
        <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:1rem; margin-bottom:1.5rem; background:var(--bg-card); padding:1.1rem 1.25rem;   ">
          <div style="display:flex; align-items:center; gap:0.75rem; flex-wrap:wrap;">
            <label style="font-weight:700; font-size:0.9rem;" for="field-8">分析対象の空き店舗・エリア:</label>
            <select id="field-8" class="form-select" style="min-width:320px;" onchange="MachirikuApp.navigate('s07', { id: this.value })">
              <option value="all" ${selectedPropId === 'all' ? 'selected' : ''}>宇都宮市中心街 全5物件総合需要</option>
              ${props.map(p => `
                <option value="${encodeURIComponent(p.id)}" ${selectedPropId === p.id ? 'selected' : ''}>
                  ${escapeHtml(p.title)} （${p.votesCount}票 / ${p.demandScore}pt）
                </option>
              `).join("")}
            </select>
          </div>
          ${selectedProp ? `
            <div style="display:flex; gap:0.5rem;">
              <button type="button" class="btn-primary" style="font-size:0.875rem; padding:0.4rem 0.85rem;" onclick="MachirikuApp.navigate('s08', { id: '${selectedProp.id}' })">この物件に出店応募</button>
              <button type="button" class="btn-secondary" style="font-size:0.875rem; padding:0.4rem 0.85rem;" onclick="MachirikuApp.navigate('s03', { id: '${selectedProp.id}' })">住民投票</button>
              <button type="button" class="btn-secondary" style="font-size:0.875rem; padding:0.4rem 0.85rem;" onclick="MachirikuApp.navigate('s02', { id: '${selectedProp.id}' })">物件詳細</button>
            </div>
          ` : ''}
        </div>

        <div class="shadcn-dashboard">
          <!-- 4カラム KPIカード (shadcn/ui Blocks) -->
          <div class="metrics-kpi-grid">
            <div class="metric-card">
              <div class="metric-card-top">
                <span>総需要投票数</span>
                <span></span>
              </div>
              <div class="metric-card-val">${votesCount} <span style="font-size:1rem; font-weight:600;">票</span></div>
              <div class="metric-card-footer">
                <span class="trend-badge trend-up">↑ 住民参加継続中</span>
                <span>中心市街地</span>
              </div>
            </div>

            <div class="metric-card">
              <div class="metric-card-top">
                <span>需要総合スコア</span>
                <span></span>
              </div>
              <div class="metric-card-val">${demandScore} <span style="font-size:1rem; font-weight:600;">pt</span></div>
              <div class="metric-card-footer">
                <span style="color:var(--primary-700); font-weight:700;"> 距離減衰重み付け済</span>
              </div>
            </div>

            <div class="metric-card">
              <div class="metric-card-top">
                <span>最多希望ジャンル</span>
                <span></span>
              </div>
              <div class="metric-card-val" style="font-size:1.25rem; margin-top:0.75rem;">${topGenreLabel}</div>
              <div class="metric-card-footer">
                <span>リクエスト第1位</span>
              </div>
            </div>

            <div class="metric-card">
              <div class="metric-card-top">
                <span>出店エントリー数</span>
                <span></span>
              </div>
              <div class="metric-card-val">${entryCount} <span style="font-size:1rem; font-weight:600;">件</span></div>
              <div class="metric-card-footer">
                <span style="color:var(--info-text); font-weight:700;">審査中・面談進行中</span>
              </div>
            </div>
          </div>

          <div style="display:grid; grid-template-columns:1fr 1fr; gap:1.5rem; margin-top:1.5rem;">
            <!-- 7大分類別 需要比率チャート (SVG Bar Chart) -->
            <div class="detail-card">
              <h3 class="detail-card-title">7大分類別 需要スコア分布（${targetTitle}）</h3>
              <div style="display:flex; flex-direction:column; gap:0.85rem; margin-top:1rem;">
                ${catScores.map(c => {
                  const percent = Math.min(100, Math.round((c.score / maxScore) * 100));
                  return `
                    <div>
                      <div style="display:flex; justify-content:space-between; font-size:0.875rem; font-weight:700; margin-bottom:0.25rem;">
                        <span>${c.name}</span>
                        <span>${c.score} pt (${percent}%)</span>
                      </div>
                      <div class="demand-progress-track">
                        <div class="demand-progress-fill" style="width:${percent}%; background-color:${c.color};"></div>
                      </div>
                    </div>
                  `;
                }).join("")}
              </div>
            </div>

            <!-- 希望業種Topランキング & 住民の生の声 -->
            <div class="detail-card">
              <h3 class="detail-card-title">住民の生の声・リクエスト要望</h3>
              <div style="display:flex; flex-direction:column; gap:0.75rem; max-height:360px; overflow-y:auto; padding-right:0.5rem;">
                ${voicesList.length === 0 ? `
                  <div style="color:var(--text-muted); font-size:0.875rem; padding:1rem 0;">まだ住民の声が寄せられていません。物件の詳細から投票・コメントを投稿できます。</div>
                ` : voicesList.map(v => `
                  <div class="quote-bubble" style="margin-bottom:0.25rem;">
                    <p class="quote-text">“${escapeHtml(v.text)}”</p>
                    <div class="quote-meta">
                      <span>${escapeHtml(v.author)}</span>
                      <span>${escapeHtml(v.date)}</span>
                    </div>
                  </div>
                `).join("")}
              </div>
            </div>
          </div>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S08: 出店申込・資格確認 (さかさま不動産 × formrun 仕様)
  // =========================================================================
  function renderS08(params) {
    const propId = params?.id || store.getProperties()[0]?.id;
    const prop = store.getPropertyById(propId);
    if (!prop) return renderEmptyState('S08', "物件が見つかりません", "掲載が終了したか、まだ物件が登録されていません。", "#s01", "物件一覧へ");
    const state = store.getState();

    return `
      <section class="screen-view s08-view" data-screen="S08">
        ${renderHeaderBanner("S08", "出店申込・エントリーフォーム", "出店事業主", "さかさま不動産のように、あなたの想いやコンセプト、経験、保有資格をオーナーに直接伝える出店エントリー画面です。", [{ label: "物件詳細", hash: `#s02?id=${prop.id}` }])}

        <div class="form-stepper-box">
          <div class="stepper-header">
            <h2 style="font-size:1.25rem; font-weight:800;">【申込対象】${escapeHtml(prop.title)}</h2>
            <div style="font-size:0.875rem; color:var(--text-muted);">賃料: ${formatRent(prop.rent)}万円 / 面積: ${prop.areaTsubo}坪</div>
          </div>

          <form id="s08-apply-form" onsubmit="event.preventDefault(); MachirikuScreens.handleApplySubmit('${prop.id}', this);">
            <!-- Step 1: 屋号・業種 -->
            <div style="margin-bottom:1.5rem;">
              <h3 style="font-size:1rem; font-weight:800; border-bottom:2px solid var(--primary-100); padding-bottom:0.5rem; margin-bottom:1rem;">
                1. 出店計画・基本情報
              </h3>
              <div class="form-group" style="margin-bottom:1rem;">
                <label for="field-9">店舗屋号・ブランド名 *必須</label>
                <input id="field-9" type="text" name="storeName" class="form-control" required placeholder="例: 日光焙煎珈琲スタンド オリオン通り店" value="${escapeHtml(state.currentUser.brand || '')}">
              </div>
              <div class="form-group" style="margin-bottom:1rem;">
                <label for="field-10">出店予定業種（27小分類） *必須</label>
                <select id="field-10" name="genreId" class="form-select" required>
                  ${data.CATEGORIES.map(c => `
                    <optgroup label="${c.name}">
                      ${c.subcategories.map(s => `<option value="${s.id}">${s.icon} ${s.name}</option>`).join("")}
                    </optgroup>
                  `).join("")}
                </select>
              </div>
              <div style="display:grid; grid-template-columns:1fr 1fr; gap:1rem; margin-bottom:1rem;">
                <div class="form-group">
                  <label for="field-11">希望賃料帯 (月額/円)</label>
                  <input id="field-11" type="number" name="targetRent" class="form-control" value="${prop.rent}">
                </div>
                <div class="form-group">
                  <label for="field-12">希望オープン時期</label>
                  <input id="field-12" type="text" name="targetOpenDate" class="form-control" value="2026年12月上旬">
                </div>
              </div>
              <!-- 契約種別 (DB: contract_kind) -->
              <div class="form-group" style="margin-bottom:1rem;">
                <label style="font-weight:700; display:block; margin-bottom:0.4rem;">希望契約形態 *必須</label>
                <div style="display:flex; gap:1.25rem;">
                  <label style="display:inline-flex; align-items:center; gap:0.4rem; cursor:pointer;">
                    <input type="radio" name="contractKind" value="MONTHLY" checked style="accent-color:var(--primary-700);">
                    <span>通常賃貸契約（月額契約）</span>
                  </label>
                  <label style="display:inline-flex; align-items:center; gap:0.4rem; cursor:pointer;">
                    <input type="radio" name="contractKind" value="POPUP" style="accent-color:var(--primary-700);">
                    <span>ポップアップ短期出店</span>
                  </label>
                </div>
              </div>
              <!-- 希望利用期間 (DB: requested_start_on / requested_end_on) -->
              <div style="display:grid; grid-template-columns:1fr 1fr; gap:1rem; margin-bottom:1rem;">
                <div class="form-group">
                  <label for="field-start-on">利用開始希望日 *必須</label>
                  <input id="field-start-on" type="date" name="requestedStartOn" class="form-control" required value="${new Date(Date.now() + 30 * 86400000).toISOString().split('T')[0]}">
                </div>
                <div class="form-group">
                  <label for="field-end-on">利用終了希望日 *必須</label>
                  <input id="field-end-on" type="date" name="requestedEndOn" class="form-control" required value="${new Date(Date.now() + 395 * 86400000).toISOString().split('T')[0]}">
                </div>
              </div>
              <!-- 内覧希望日時 (DB: viewing_requested_at) -->
              <div class="form-group">
                <label for="field-viewing">現地内覧の希望日時 (任意)</label>
                <input id="field-viewing" type="datetime-local" name="viewingRequestedAt" class="form-control">
              </div>
            </div>

            <!-- Step 2: 想い・ストーリー (DB: message) -->
            <div style="margin-bottom:1.5rem;">
              <h3 style="font-size:1rem; font-weight:800; border-bottom:2px solid var(--primary-100); padding-bottom:0.5rem; margin-bottom:1rem;">
                2. 出店への想い・ストーリー（オーナーへのメッセージ）
              </h3>
              <div class="form-group">
                <label for="field-13">なぜこの場所で出店したいのか、どんなお店にしたいのかをお書きください *必須</label>
                <textarea id="field-13" name="message" class="form-control" rows="4" required placeholder="例: 地元日光の厳選コーヒー豆と宇都宮の食材を使ったカフェを開きたいです。若者とシニアが交差する居心地の良い空間をつくります。">${state.currentUser.story || ''}</textarea>
              </div>
            </div>

            <!-- Step 3: 保有資格・経験年数確認 -->
            <div style="margin-bottom:1.5rem;">
              <h3 style="font-size:1rem; font-weight:800; border-bottom:2px solid var(--primary-100); padding-bottom:0.5rem; margin-bottom:1rem;">
                3. 保有資格・営業経験確認
              </h3>
              <div style="background-color:var(--bg-subtle); padding:1rem;  font-size:0.875rem; margin-bottom:1rem;">
                <div style="font-weight:700; margin-bottom:0.5rem;">現在のプロフィール登録資格状況:</div>
                ${(state.currentUser.qualifications || []).map(q => `
                  <div style="color:var(--primary-700); margin-bottom:0.25rem;"> ${q.name}（${q.number}）- 認証済</div>
                `).join("")}
              </div>
              <div class="form-group">
                <label for="field-14">実務経験年数</label>
                <input id="field-14" type="text" class="form-control" value="${state.currentUser.experienceYears || '飲食業12年'}" readonly>
              </div>
            </div>

            <div style="display:flex; justify-content:flex-end; gap:1rem;">
              <button type="button" class="btn-secondary" onclick="MachirikuApp.navigate('s02', { id: '${prop.id}' })">戻る</button>
              <button type="submit" class="btn-primary" style="padding:0.75rem 2rem;">この内容で出店申込を送信する</button>
            </div>
          </form>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S09: 出店申込一覧 (オーナー・事業主 / shadcn/ui Data Table)
  // =========================================================================
  function renderS09() {
    const state = store.getState();
    const apps = state.applications || [];
    if (!apps.length) return renderEmptyState('S09', '申し込みはまだありません', '出店したい物件を探して、詳細から申し込めます。');

    return `
      <section class="screen-view s09-view" data-screen="S09">
        ${renderHeaderBanner("S09", "出店申込一覧・管理", "オーナー・事業主", "物件に届いた出店エントリーの一覧です。応募者の想い・資格を確認し、面談や承認を進めます。")}

        <div class="data-table-wrapper">
          <div class="table-toolbar">
            <span style="font-weight:700;">出店エントリー件数: <strong>${apps.length}</strong> 件</span>
            <input type="text" class="form-control table-search-input" placeholder="屋号・物件名で絞り込み...">
          </div>

          <table class="shadcn-table">
            <thead>
              <tr>
                <th>申込日時</th>
                <th>対象物件</th>
                <th>店舗屋号 / 応募者</th>
                <th>希望業種</th>
                <th>ステータス</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              ${apps.map(a => `
                <tr>
                  <td style="color:var(--text-muted); font-size:0.875rem;">${a.submittedAt}</td>
                  <td><strong>${a.propTitle}</strong></td>
                  <td>
                    <div style="font-weight:800;">${a.storeName}</div>
                    <div style="font-size:0.875rem; color:var(--text-muted);">${a.applicantName}</div>
                  </td>
                  <td><span class="qual-badge">${a.genreName}</span></td>
                  <td><span class="badge-status status-reviewing">${a.statusLabel}</span></td>
                  <td>
                    <button type="button" class="btn-header-action" onclick="MachirikuApp.navigate('s10', { id: '${a.id}' })">
                      詳細・可否判断 
                    </button>
                  </td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S10: 申込詳細・可否判断 (さかさま不動産 × shadcn/ui 仕様)
  // =========================================================================
  function renderS10(params) {
    const appId = params?.id || "app-01";
    const state = store.getState();
    const app = state.applications.find(a => a.id === appId) || state.applications[0];
    if (!app) return renderEmptyState('S10', "申し込みが見つかりません", "申し込み一覧から確認してください。", "#s09", "申し込み一覧へ");
    const prop = store.getPropertyById(app.propId) || store.getProperties()[0];
    if (!prop) return renderEmptyState('S10', "物件が見つかりません", "掲載が終了したか、まだ物件が登録されていません。", "#s01", "物件一覧へ");
    const applicant = Object.values(state.users).find(u => u.id === app.applicantId);
    const reviews = businessReviews(applicant);

    return `
      <section class="screen-view s10-view" data-screen="S10">
        ${renderHeaderBanner("S10", "出店申込詳細・可否判断", "オーナー・事業主", "応募事業主の想い・ストーリー・資格を確認し、面談の調整や承認・契約締結へ進めます。", [{ label: "申込一覧", hash: "#s09" }])}

        <div style="display:grid; grid-template-columns:2fr 1fr; gap:1.75rem;">
          <div>
            <!-- 想い・ストーリーカード (さかさま不動産風) -->
            <div class="story-card-box">
              <div class="story-header">
                
                <div>
                  <h2 style="font-size:1.3rem; font-weight:800; color:var(--text-main);">${escapeHtml(app.storeName)}</h2>
                  <div style="font-size:0.875rem; color:var(--text-muted); margin-bottom:0.75rem;">応募者: ${escapeHtml(app.applicantName)} / 業種: ${escapeHtml(app.genreName)}</div>
                </div>
              </div>

              <!-- 申込条件概要 (DB: applications準拠) -->
              <table class="specs-table" style="margin-bottom:1.25rem;">
                <tbody>
                  <tr>
                    <th style="width:140px;">希望契約形態</th>
                    <td><strong>${app.contractKind === 'POPUP' ? 'ポップアップ短期出店' : '通常賃貸契約（月額）'}</strong></td>
                  </tr>
                  ${app.requestedStartOn ? `
                    <tr>
                      <th>利用希望期間</th>
                      <td>${escapeHtml(app.requestedStartOn)} 〜 ${escapeHtml(app.requestedEndOn || '相談')}</td>
                    </tr>
                  ` : ''}
                  ${app.viewingRequestedAt ? `
                    <tr>
                      <th>現地内覧希望日時</th>
                      <td>${escapeHtml(app.viewingRequestedAt.replace('T', ' '))}</td>
                    </tr>
                  ` : ''}
                </tbody>
              </table>
              
              <div style="font-weight:700; margin-bottom:0.5rem; color:var(--text-main);">■ 出店への想い・メッセージ</div>
              <div class="story-content-body" style="margin-bottom:1.25rem;">
                ${escapeHtml(app.message || app.concept || '')}
              </div>

              <div style="font-weight:700; margin-bottom:0.5rem; color:var(--text-main);">■ 資格・許認可情報</div>
              <div class="qualifications-grid">
                <span class="qual-badge"> 調理師免許取得（第12458号）</span>
                <span class="qual-badge"> 食品衛生責任者</span>
                <span class="qual-badge"> 宇都宮市商工会議所 推薦</span>
              </div>
            </div>

            <!-- 事業主の実績・顧客評価へのリンク -->
            <div class="detail-card" style="display:flex; justify-content:space-between; align-items:center;">
              <div>
                <h4 style="font-size:0.95rem; font-weight:800;">この事業主の過去実績・顧客満足度</h4>
                <div style="font-size:0.875rem; color:var(--text-sub);">来店アンケート: <strong>${reviews.average} / 5（${reviews.count}件）</strong></div>
              </div>
              <button type="button" class="btn-secondary" onclick="MachirikuApp.navigate('s32')">実績・評価を見る </button>
            </div>
          </div>

          <!-- 右サイド: オーナーアクションパネル -->
          <div class="detail-side-pane">
            <div class="cta-box-card">
              <h3 style="font-size:1.05rem; font-weight:800; margin-bottom:0.75rem;">オーナーの判断・操作</h3>
              <div style="font-size:0.875rem; color:var(--text-muted); margin-bottom:1.25rem;">現在のステータス: <strong>${app.statusLabel}</strong></div>

              <div class="cta-buttons-stack">
                <button type="button" class="btn-primary" style="background-color:var(--primary-700);" onclick="MachirikuScreens.handleAppAction('${app.id}', 'interviewing')">
                  面談を希望する（メッセージ送信）
                </button>
                <button type="button" class="btn-cta-apply" onclick="MachirikuScreens.handleAppAction('${app.id}', 'approved')">
                  出店を承認する（外部契約調整へ）
                </button>
                <button type="button" class="btn-secondary" onclick="MachirikuApp.navigate('s14', { id: '${app.id}' })">
                  契約成立・引渡し記録へ進む 
                </button>
                <button type="button" class="btn-secondary" style="color:var(--accent-price);" onclick="MachirikuScreens.handleAppAction('${app.id}', 'rejected')">
                  × 今回は見送る
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S11: 登録物件管理 (物件オーナー)
  // =========================================================================
  function renderS11() {
    const state = store.getState();
    const isOwner = state.currentRole === "owner";
    const myProps = state.properties.filter(p => state.currentUser.ownedPropIds?.includes(p.id)) || [];
    const totalVotes = myProps.reduce((sum, p) => sum + p.votesCount, 0);
    const totalScore = Math.round(myProps.reduce((sum, p) => sum + p.demandScore, 0) * 10) / 10;
    const appsForMyProps = state.applications.filter(a => myProps.some(p => p.id === a.propId));
    const prCount = myProps.filter(p => p.isPr).length;

    return `
      <section class="screen-view s11-view" data-screen="S11">
        ${renderHeaderBanner("S11", "登録物件管理ダッシュボード", "物件オーナー", "所有する登録空き店舗の一覧です。需要スコアの確認、出店エントリーの対応、PR広告掲載を管理します。")}

        ${!isOwner ? `
          <div style="background-color:var(--info-bg); border:1px solid var(--info-border);  padding:1rem 1.25rem; margin-bottom:1.5rem; display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:0.75rem;">
            <div style="font-size:0.88rem; color:var(--info-text);">
              ℹ️ 現在 <strong>【${state.currentUser.roleName}】</strong> の視点で閲覧中です。物件オーナー（佐藤 武雄）の所有物件をフル操作するにはオーナー視点に切り替えます。
            </div>
            <button type="button" class="btn-primary" style="font-size:0.875rem; padding:0.4rem 0.9rem; background-color:var(--warning-text);" onclick="MachirikuStore.setRole('owner')">
              物件オーナーに切り替える
            </button>
          </div>
        ` : ''}

        <!-- shadcn/ui KPI メトリクスカード -->
        <div class="metrics-kpi-grid" style="margin-bottom:1.5rem;">
          <div class="metric-card">
            <div class="metric-card-top"><span>所有物件数</span><span></span></div>
            <div class="metric-card-val">${myProps.length} <span style="font-size:1rem; font-weight:600;">件</span></div>
            <div class="metric-card-footer"><span>中心街管理区画</span></div>
          </div>
          <div class="metric-card">
            <div class="metric-card-top"><span>獲得需要投票</span><span></span></div>
            <div class="metric-card-val">${totalVotes} <span style="font-size:1rem; font-weight:600;">票</span></div>
            <div class="metric-card-footer"><span style="color:var(--primary-700); font-weight:700;">計 ${totalScore} pt</span></div>
          </div>
          <div class="metric-card">
            <div class="metric-card-top"><span>受信出店エントリー</span><span></span></div>
            <div class="metric-card-val">${appsForMyProps.length} <span style="font-size:1rem; font-weight:600;">件</span></div>
            <div class="metric-card-footer"><span style="color:var(--info-text); font-weight:700;">審査・面談対象</span></div>
          </div>
          <div class="metric-card">
            <div class="metric-card-top"><span>PR注目枠掲載</span><span></span></div>
            <div class="metric-card-val">${prCount} <span style="font-size:1rem; font-weight:600;">件</span></div>
            <div class="metric-card-footer"><span>検索最上位ピン留め</span></div>
          </div>
        </div>

        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:1.5rem;">
          <div style="font-size:0.95rem; font-weight:700;">所有物件一覧: <strong>${myProps.length}</strong> 件</div>
          <button type="button" class="btn-primary" onclick="MachirikuApp.navigate('s12')">＋ 新規空き店舗を登録する</button>
        </div>

        <div style="display:flex; flex-direction:column; gap:1.25rem;">
          ${myProps.length === 0 ? `
            <div class="detail-card" style="text-align:center; padding:3rem 1.5rem;">
              <p style="font-size:0.9rem; color:var(--text-muted); margin-bottom:1rem;">所有物件が登録されていません。新規登録から空き店舗を掲載してください。</p>
              <button type="button" class="btn-primary" onclick="MachirikuApp.navigate('s12')">＋ 空き店舗を新規登録する</button>
            </div>
          ` : myProps.map(p => `
            <div class="detail-card" style="display:grid; grid-template-columns:180px 1fr auto; gap:1.25rem; align-items:center;">
              ${renderPropertyImage(p, 'style="width:100%; height:110px; object-fit:cover; border-radius:var(--radius-md);"')}
              <div>
                <div style="display:flex; gap:0.5rem; margin-bottom:0.25rem; align-items:center;">
                  <span class="badge-status status-${p.status}">${p.statusLabel}</span>
                  ${p.isPr ? '<span class="badge-pr">PR注目枠掲載中</span>' : ''}
                  <span style="font-size:0.875rem; color:var(--text-muted);">管理コード: ${escapeHtml(p.code)}</span>
                </div>
                <h3 style="font-size:1.1rem; font-weight:800; cursor:pointer;" onclick="MachirikuApp.navigate('s02', { id: '${encodeURIComponent(p.id)}' })">${escapeHtml(p.title)}</h3>
                <div style="font-size:0.875rem; color:var(--text-muted); margin-bottom:0.4rem;">${escapeHtml(p.address)} / 賃料 ${formatRent(p.rent)}万円 / ${p.areaTsubo}坪</div>
                <div style="font-size:0.875rem; color:var(--primary-700); font-weight:700;">
                  投票総数: ${p.votesCount}票 / 需要スコア: ${p.demandScore}pt （1位: ${escapeHtml(p.topDemands[0]?.label || '集計中')}）
                </div>
              </div>
              <div style="display:flex; flex-direction:column; gap:0.45rem;">
                <button type="button" class="btn-primary" style="font-size:0.875rem; padding:0.35rem 0.75rem;" onclick="MachirikuApp.navigate('s09', { propId: '${p.id}' })">申込確認</button>
                <button type="button" class="btn-secondary" style="font-size:0.875rem; padding:0.35rem 0.75rem;" onclick="MachirikuApp.navigate('s07', { id: '${p.id}' })">需要分析</button>
                <button type="button" class="btn-secondary" style="font-size:0.875rem; padding:0.35rem 0.75rem;" onclick="MachirikuApp.navigate('s12', { id: '${p.id}' })">物件編集</button>
                <button type="button" class="btn-secondary" style="font-size:0.875rem; padding:0.35rem 0.75rem;" onclick="MachirikuApp.navigate('s33', { id: '${p.id}' })">PR広告</button>
                <button type="button" class="btn-secondary" style="font-size:0.875rem; padding:0.35rem 0.75rem; color:var(--accent-price);" onclick="MachirikuScreens.handleDeleteProp('${p.id}')">削除</button>
              </div>
            </div>
          `).join("")}
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S12: 物件登録・編集 (物件オーナー)
  // =========================================================================
  function renderS12(params) {
    const editId = params?.id;
    const editProp = editId ? store.getPropertyById(editId) : null;
    const isEdit = Boolean(editProp);
    const title = isEdit ? "物件情報の編集" : "新規物件の登録・掲載申請";
    const facs = editProp ? (editProp.facilities || []) : ["都市ガス", "給排水設備"];

    return `
      <section class="screen-view s12-view" data-screen="S12">
        ${renderHeaderBanner("S12", title, "物件オーナー", isEdit ? "登録済み物件の条件・賃料・設備情報を更新します。" : "所有する空き店舗の情報を入力して掲載申請を行います。審査承認後に住民投票がスタートします。", [{ label: "物件管理", hash: "#s11" }])}

        <div class="form-stepper-box">
          <form id="s12-prop-form" onsubmit="event.preventDefault(); MachirikuScreens.handlePropRegisterSubmit(this, '${isEdit ? editProp.id : ""}');">
            <div class="form-group" style="margin-bottom:1rem;">
              <label for="field-15">物件名称 *必須</label>
              <input id="field-15" type="text" name="title" class="form-control" required placeholder="例: オリオン通り中央角地空きビル 1F" value="${editProp ? editProp.title : ""}">
            </div>
            <div class="form-group" style="margin-bottom:1rem;">
              <label for="field-16">所在地（住所） *必須</label>
              <input id="field-16" type="text" name="address" class="form-control" required placeholder="例: 栃木県宇都宮市江野町5-12" value="${editProp ? editProp.address : ""}">
            </div>
            <div class="form-group" style="margin-bottom:1rem;">
              <label for="field-17">交通・最寄駅アクセス *必須</label>
              <input id="field-17" type="text" name="access" class="form-control" required placeholder="例: 東武宇都宮駅 徒歩3分" value="${editProp ? editProp.access : ""}">
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:1rem; margin-bottom:1rem;">
              <div class="form-group">
                <label for="field-18">月額賃料 (円・税込) *必須</label>
                <input id="field-18" type="number" name="rent" class="form-control" required placeholder="180000" value="${editProp ? editProp.rent : "180000"}">
              </div>
              <div class="form-group">
                <label for="field-19">月額共益費 (円)</label>
                <input id="field-19" type="number" name="managementFee" class="form-control" placeholder="10000" value="${editProp ? (editProp.managementFee || 0) : "10000"}">
              </div>
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:1rem; margin-bottom:1rem;">
              <div class="form-group">
                <label for="field-20">敷金・礼金条件</label>
                <input id="field-20" type="text" name="deposit" class="form-control" value="${editProp ? (editProp.deposit || "敷金2ヶ月 / 礼金1ヶ月") : "敷金2ヶ月 / 礼金1ヶ月"}">
              </div>
              <div class="form-group">
                <label for="field-21">保証金</label>
                <input id="field-21" type="text" name="securityDeposit" class="form-control" value="${editProp ? (editProp.securityDeposit || "30万円") : "30万円"}">
              </div>
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:1rem; margin-bottom:1rem;">
              <div class="form-group">
                <label for="field-22">使用部分面積 (坪) *必須</label>
                <input id="field-22" type="number" step="0.1" name="areaTsubo" class="form-control" required placeholder="20.5" value="${editProp ? editProp.areaTsubo : "20.5"}">
              </div>
              <div class="form-group">
                <label for="field-23">所在階数</label>
                <input id="field-23" type="text" name="floor" class="form-control" placeholder="1階路面店舗" value="${editProp ? editProp.floor : "地上1階部分"}">
              </div>
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:1rem; margin-bottom:1rem;">
              <div class="form-group">
                <label for="field-24">建物構造</label>
                <input id="field-24" type="text" name="structure" class="form-control" value="${editProp ? (editProp.structure || "鉄骨造") : "鉄骨鉄筋コンクリート造"}">
              </div>
              <div class="form-group">
                <label for="field-25">築年月</label>
                <input id="field-25" type="text" name="builtYear" class="form-control" value="${editProp ? (editProp.builtYear || "1995年築") : "1992年築"}">
              </div>
            </div>

            <div class="form-group" style="margin-bottom:1rem;">
              <label>主要設備（該当するものにチェック）</label>
              <div style="display:grid; grid-template-columns:repeat(4, 1fr); gap:0.5rem; margin-top:0.4rem; font-size:0.875rem;">
                <label><input type="checkbox" name="facGas" ${facs.includes("都市ガス") ? "checked" : ""}> 都市ガス</label>
                <label><input type="checkbox" name="facWater" ${facs.includes("給排水設備") || facs.includes("給排水") ? "checked" : ""}> 給排水設備</label>
                <label><input type="checkbox" name="facGrease" ${facs.includes("グリストラップ") ? "checked" : ""}> グリストラップ</label>
                <label><input type="checkbox" name="facDuct" ${facs.includes("換気ダクト") ? "checked" : ""}> 換気ダクト</label>
                <label><input type="checkbox" name="facPower" ${facs.includes("動力電力") ? "checked" : ""}> 動力電力</label>
                <label><input type="checkbox" name="facSign" ${facs.includes("看板スペース") ? "checked" : ""}> 看板スペース</label>
                <label><input type="checkbox" name="facPark" ${facs.includes("駐車場") ? "checked" : ""}> 駐車場あり</label>
                <label><input type="checkbox" name="facToilet" checked> 専用トイレ</label>
              </div>
            </div>

            <div class="form-group" style="margin-bottom:1rem;">
              <label for="field-26">業種条件・希望</label>
              <input id="field-26" type="text" name="condition" class="form-control" placeholder="飲食相談可（重飲食応相談）・カフェ・物販歓迎" value="${editProp ? editProp.condition : "カフェ・物販・飲食相談可"}">
            </div>

            <div class="form-group" style="margin-bottom:1.5rem;">
              <label for="field-27">物件の特徴・アピール説明</label>
              <textarea id="field-27" name="description" class="form-control" rows="4" placeholder="アーケード街に面し、人通りが絶えない好立地です。">${editProp ? editProp.description : "宇都宮中心街の好立地。住民の需要投票結果を踏まえた出店者を募集します。"}</textarea>
            </div>

            <div style="display:flex; justify-content:flex-end; gap:1rem;">
              <button type="button" class="btn-secondary" onclick="MachirikuApp.navigate('s11')">キャンセル</button>
              <button type="submit" class="btn-primary" style="padding:0.75rem 2rem;">
                ${isEdit ? "更新内容を保存する" : "確認して掲載申請する"}
              </button>
            </div>
          </form>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S13: 事業主へのオファー (物件オーナー)
  // =========================================================================
  function renderS13() {
    const state = store.getState();
    const myProps = state.properties.filter(p => state.currentUser.ownedPropIds?.includes(p.id)) || state.properties;
    const bizUsers = [
      { id: "usr-biz-01", name: "鈴木 健太（合同会社日光珈琲舎）", genre: "カフェ・喫茶店 (4.4)" },
      { id: "usr-biz-02", name: "小林 誠（麺処 こばやし）", genre: "ラーメン・麺類 (新進気鋭)" },
      { id: "usr-biz-03", name: "渡辺 美咲（宇都宮ベーカリーラボ）", genre: "パン屋・ベーカリー" }
    ];

    return `
      <section class="screen-view s13-view" data-screen="S13">
        ${renderHeaderBanner("S13", "事業主への個別オファー送信", "物件オーナー", "需要ランキングに合致する実績のある事業主に対し、オーナー側から出店を打診できる機能です。", [{ label: "物件管理", hash: "#s11" }])}

        <div class="form-stepper-box">
          <form onsubmit="event.preventDefault(); MachirikuScreens.handleSendOffer();">
            <div class="form-group" style="margin-bottom:1rem;">
              <label for="field-28">オファー提案物件</label>
              <select id="field-28" name="propSelect" class="form-select">
                ${myProps.map(p => `
                  <option value="${encodeURIComponent(p.id)}">${escapeHtml(p.title)} （賃料 ${formatRent(p.rent)}万円 / ${p.areaTsubo}坪）</option>
                `).join("")}
              </select>
            </div>

            <div class="form-group" style="margin-bottom:1rem;">
              <label for="field-29">オファー対象事業主</label>
              <select id="field-29" name="bizSelect" class="form-select">
                ${bizUsers.map(b => `
                  <option value="${encodeURIComponent(b.id)}">${escapeHtml(b.name)} - ${escapeHtml(b.genre)}</option>
                `).join("")}
              </select>
            </div>

            <div class="form-group" style="margin-bottom:1rem;">
              <label>オーナーからの優遇・支援提案条件</label>
              <div style="display:flex; flex-wrap:wrap; gap:0.8rem; margin-top:0.4rem; font-size:0.875rem;">
                <label><input type="checkbox" checked> フリーレント1ヶ月付与</label>
                <label><input type="checkbox" checked> 内装改修工事の補助相談可</label>
                <label><input type="checkbox"> 敷金・保証金の分納相談可</label>
                <label><input type="checkbox"> 厨房機器リース支援連携</label>
              </div>
            </div>

            <div class="form-group" style="margin-bottom:1.5rem;">
              <label for="field-30">オファーメッセージ本文</label>
              <textarea id="field-30" class="form-control" rows="4">当物件の住民需要投票で貴殿の業態が上位となっており、コンセプトに大変共感いたしました。商店街の活性化に向け、賃料や初期条件も含めて前向きに協力させていただきます。ぜひ一度、内覧またはオンライン面談をご相談させてください。</textarea>
            </div>

            <div style="display:flex; justify-content:flex-end; gap:1rem;">
              <button type="button" class="btn-secondary" onclick="MachirikuApp.navigate('s11')">戻る</button>
              <button type="submit" class="btn-primary" style="padding:0.75rem 2rem;">オファーメッセージを送信する</button>
            </div>
          </form>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S14: 契約成立・物件引渡し (オーナー・事業主)
  // =========================================================================
  function renderS14(params) {
    const appId = params?.id || "app-01";
    const state = store.getState();
    const app = state.applications.find(a => a.id === appId) || state.applications[0];
    if (!app) return renderEmptyState('S14', "申し込みが見つかりません", "申し込み一覧から確認してください。", "#s09", "申し込み一覧へ");
    const prop = store.getPropertyById(app.propId) || store.getProperties()[0];
    if (!prop) return renderEmptyState('S14', "物件が見つかりません", "掲載が終了したか、まだ物件が登録されていません。", "#s01", "物件一覧へ");

    return `
      <section class="screen-view s14-view" data-screen="S14">
        ${renderHeaderBanner("S14", "契約成立・物件引渡し記録", "オーナー・事業主", "外部での賃貸借契約締結と物件引渡しの完了をシステムに記録し、開店準備フェーズへ進めます。", [{ label: "申込詳細", hash: `#s10?id=${app.id}` }])}

        <div class="form-stepper-box">
          <div style="background-color:var(--bg-subtle); padding:1rem 1.25rem;  margin-bottom:1.5rem;">
            <div style="font-size:0.875rem; color:var(--text-muted); margin-bottom:0.25rem;">対象物件・出店者</div>
            <h3 style="font-size:1.15rem; font-weight:800; color:var(--text-main); margin-bottom:0.35rem;">${escapeHtml(prop.title)}</h3>
            <div style="font-size:0.88rem; color:var(--primary-800); font-weight:700;">
              出店屋号: ${escapeHtml(app.storeName)} （応募者: ${escapeHtml(app.applicantName)}）
            </div>
          </div>

          <form onsubmit="event.preventDefault(); MachirikuScreens.handleContractComplete('${app.id}');">
            <div style="display:grid; grid-template-columns:1fr 1fr; gap:1rem; margin-bottom:1rem;">
              <div class="form-group">
                <label for="field-31">賃貸借契約締結日 *必須</label>
                <input id="field-31" type="date" name="contractDate" class="form-control" required value="2026-10-20">
              </div>
              <div class="form-group">
                <label for="field-32">鍵・物件引渡し完了日 *必須</label>
                <input id="field-32" type="date" name="handoverDate" class="form-control" required value="2026-10-22">
              </div>
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:1rem; margin-bottom:1rem;">
              <div class="form-group">
                <label for="field-33">合意賃料 (月額/円) *必須</label>
                <input id="field-33" type="number" name="agreedRent" class="form-control" required value="${app.targetRent || prop.rent}">
              </div>
              <div class="form-group">
                <label for="field-34">契約種別・期間</label>
                <input id="field-34" type="text" name="contractTerm" class="form-control" value="普通借家契約 2年間（更新可）">
              </div>
            </div>

            <div class="form-group" style="margin-bottom:1.5rem;">
              <label for="field-35">合意特記事項・引渡し条件</label>
              <textarea id="field-35" class="form-control" rows="3">フリーレント1ヶ月適用。内装工事期間は2026年11月10日まで。店舗看板の設置承諾済。</textarea>
            </div>

            <div style="display:flex; justify-content:flex-end; gap:1rem;">
              <button type="button" class="btn-secondary" onclick="MachirikuApp.navigate('s10', { id: '${app.id}' })">戻る</button>
              <button type="submit" class="btn-primary" style="padding:0.75rem 2rem;">
                契約成立を確定し、出店計画公開へ進む 
              </button>
            </div>
          </form>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S15: 出店計画の作成・公開 (出店事業主)
  // =========================================================================
  function renderS15(params) {
    const state = store.getState();
    const targetPropId = params?.propId || (params?.appId ? store.getApplicationById(params.appId)?.propId : null) || store.getProperties()[0]?.id;
    const prop = store.getPropertyById(targetPropId);
    if (!prop) return renderEmptyState('S15', "物件が見つかりません", "掲載が終了したか、まだ物件が登録されていません。", "#s01", "物件一覧へ");
    const candidateProps = state.properties.filter(p => p.status === "preparing" || p.status === "contracted" || p.status === "opened");
    const activePropsList = candidateProps.length > 0 ? candidateProps : state.properties;

    return `
      <section class="screen-view s15-view" data-screen="S15">
        ${renderHeaderBanner("S15", "出店計画の作成・公開", "出店事業主", "オープン予定日、店名、内装イメージを住民に向けて発信します。公開すると住民のS05進捗画面に反映されます。", [{ label: "出店進捗タイムライン", hash: "#s05" }])}

        <div class="form-stepper-box">
          <form onsubmit="event.preventDefault(); MachirikuScreens.handlePublishPlanSubmit(this, '${prop.id}');">
            <div class="form-group" style="margin-bottom:1rem;">
              <label for="field-36">出店対象物件</label>
              <select id="field-36" name="propId" class="form-select" onchange="MachirikuApp.navigate('s15', { propId: this.value })">
                ${activePropsList.map(p => `
                  <option value="${encodeURIComponent(p.id)}" ${p.id === prop.id ? 'selected' : ''}>${escapeHtml(p.title)} （${escapeHtml(p.address)}）</option>
                `).join("")}
              </select>
            </div>
            <div class="form-group" style="margin-bottom:1rem;">
              <label for="field-37">店舗正式名称 *必須</label>
              <input id="field-37" type="text" name="storeName" class="form-control" required value="${escapeHtml(prop.storePlan?.storeName || '')}">
            </div>
            <div class="form-group" style="margin-bottom:1rem;">
              <label for="field-progress-status">現在の進捗状況（DB: progress_status） *必須</label>
              <select id="field-progress-status" name="progressStatus" class="form-select" required>
                <option value="PREPARING" ${(!prop.storePlan?.progressStatus || prop.storePlan?.progressStatus === 'PREPARING') ? 'selected' : ''}>開店準備中</option>
                <option value="CONSTRUCTION" ${prop.storePlan?.progressStatus === 'CONSTRUCTION' ? 'selected' : ''}>内装・設備工事中</option>
                <option value="DATE_FIXED" ${prop.storePlan?.progressStatus === 'DATE_FIXED' ? 'selected' : ''}>オープン日決定</option>
                <option value="OPENED" ${prop.storePlan?.progressStatus === 'OPENED' ? 'selected' : ''}>開店・オープン</option>
              </select>
            </div>
            <div style="display:grid; grid-template-columns:1fr 1fr; gap:1rem; margin-bottom:1rem;">
              <div class="form-group">
                <label for="field-38">グランドオープン予定日 *必須</label>
                <input id="field-38" type="text" name="openDate" class="form-control" required value="${prop.storePlan?.openDate || ''}">
              </div>
              <div class="form-group">
                <label for="field-39">出店予定業態</label>
                <input id="field-39" type="text" class="form-control" value="${prop.topDemands[0]?.label || '未集計'}" readonly>
              </div>
            </div>
            <div class="form-group" style="margin-bottom:1.5rem;">
              <label for="field-40">街の住民へのメッセージ・開店コンセプト *必須</label>
              <textarea id="field-40" name="message" class="form-control" rows="4" required placeholder="街の皆様への想いや、お店のこだわりをお書きください。">${prop.storePlan?.message || ''}</textarea>
            </div>
            <div style="display:flex; justify-content:flex-end; gap:1rem;">
              <button type="button" class="btn-secondary" onclick="MachirikuApp.navigate('s05')">キャンセル</button>
              <button type="submit" class="btn-primary" style="padding:0.75rem 2rem;">開店予定を公開する</button>
            </div>
          </form>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S16: ログイン (全員)
  // =========================================================================
  function renderS16(params = {}) {
    const errorHtml = params && params.errorMessage ? `
      <div style="background:#fee2e2; border:1px solid #f87171; color:#991b1b; padding:0.75rem 1rem; border-radius:var(--radius-md); font-size:0.875rem; margin-bottom:1.25rem;">
        ${escapeHtml(params.errorMessage)}
      </div>
    ` : '';
    const ctx = (typeof window !== 'undefined' && window.__MACHIRIKU_CONTEXT_PATH__ !== undefined)
      ? window.__MACHIRIKU_CONTEXT_PATH__
      : (typeof window !== 'undefined' && window.location && window.location.pathname.startsWith('/machiriku') ? '/machiriku' : '');
    const loginActionUrl = (ctx || '') + '/jp/machiriku/servlet/auth/Login.action';

    return `
      <section class="screen-view s16-view" data-screen="S16">
        <div class="form-stepper-box" style="max-width:540px;">
          <h1 style="font-size:1.35rem; font-weight:800; color:var(--text-main); margin:0 0 1.25rem 0; text-align:center;">ログイン</h1>
          ${errorHtml}
          <div style="margin-top:0.5rem; margin-bottom:1.5rem;">
            <a href="${loginActionUrl}" class="btn-line-login" style="text-decoration:none; display:flex; align-items:center; justify-content:center; gap:0.75rem;">
              <img src="assets/line_icon.svg" alt="LINE" class="line-icon-svg" style="width:24px; height:24px; flex-shrink:0;">
              <span>LINEでログイン</span>
            </a>
            <p style="font-size:0.8125rem; color:var(--text-sub); text-align:center; margin-top:0.75rem; line-height:1.5;">
              LINEの表示名・ユーザーIDと、取得できる場合はメールアドレスを取得し、会員情報の確認に利用します。
            </p>
          </div>

          <div style="text-align:center; margin-top:1.25rem; font-size:0.875rem;">
            アカウントをお持ちでない方は <a href="#s17" style="color:var(--primary-700); font-weight:700;">新規会員登録</a>
          </div>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S17: 会員登録 (LINEログイン仕様)
  // =========================================================================
  function renderS17() {
    const ctx = (typeof window !== 'undefined' && window.__MACHIRIKU_CONTEXT_PATH__ !== undefined)
      ? window.__MACHIRIKU_CONTEXT_PATH__
      : (typeof window !== 'undefined' && window.location && window.location.pathname.startsWith('/machiriku') ? '/machiriku' : '');
    const registerActionUrl = (ctx || '') + '/jp/machiriku/servlet/auth/Register.action';

    return `
      <section class="screen-view s17-view" data-screen="S17">
        <div class="form-stepper-box" style="max-width:540px;">
          <h1 style="font-size:1.35rem; font-weight:800; color:var(--text-main); margin:0 0 1.25rem 0; text-align:center;">種別別 会員登録</h1>
          <div class="form-group" style="margin-bottom:1.5rem;">
            <label style="font-size:0.9rem; font-weight:800;">利用区分を選択してください *必須</label>
            <div style="display:grid; grid-template-columns:repeat(3, 1fr); gap:0.75rem; margin-top:0.5rem;">
              <label class="role-select-card is-selected">
                <input type="radio" name="regRole" value="resident" checked onchange="this.closest('.form-group').querySelectorAll('.role-select-card').forEach(function(c){c.classList.toggle('is-selected', c.querySelector('input').checked)})">
                <div class="role-card-title" style="font-weight:700; margin-top:0.3rem;">地域住民</div>
              </label>
              <label class="role-select-card">
                <input type="radio" name="regRole" value="business" onchange="this.closest('.form-group').querySelectorAll('.role-select-card').forEach(function(c){c.classList.toggle('is-selected', c.querySelector('input').checked)})">
                <div class="role-card-title" style="font-weight:700; margin-top:0.3rem;">出店事業主</div>
              </label>
              <label class="role-select-card">
                <input type="radio" name="regRole" value="owner" onchange="this.closest('.form-group').querySelectorAll('.role-select-card').forEach(function(c){c.classList.toggle('is-selected', c.querySelector('input').checked)})">
                <div class="role-card-title" style="font-weight:700; margin-top:0.3rem;">物件オーナー</div>
              </label>
            </div>
          </div>

          <div style="margin-top:1.5rem;">
            <a href="${registerActionUrl}" class="btn-line-login" style="text-decoration:none; display:flex; align-items:center; justify-content:center; gap:0.75rem;">
              <img src="assets/line_icon.svg" alt="LINE" class="line-icon-svg" style="width:24px; height:24px; flex-shrink:0;">
              <span>LINEで登録・ログイン</span>
            </a>
            <p style="font-size:0.8125rem; color:var(--text-sub); text-align:center; margin-top:0.75rem; line-height:1.5;">
              LINEアカウントを使って簡単・安全に登録・ログインできます。<br>
              <span style="font-size:0.75rem; color:var(--text-muted);">※利用規約・プライバシーポリシーに同意の上、ログインへお進みください。</span>
            </p>
          </div>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S18: プロフィール・基本情報設定 (全員)
  // =========================================================================
  let s18GoogleMap = null;
  let s18Marker = null;
  let s18WorkGoogleMap = null;
  let s18WorkMarker = null;

  function renderS18LogoutSection(isOnboarding) {
    if (isOnboarding) {
      return `
        <div style="margin-top:1.5rem; text-align:center;">
          <button type="button" onclick="MachirikuScreens.handleLogout(true)" style="background:none; border:none; color:var(--text-sub); font-size:0.875rem; text-decoration:underline; cursor:pointer; padding:0.4rem 0.8rem;">
            登録を中断してログアウト
          </button>
        </div>
      `;
    }
    return `
      <div style="margin-top:2.5rem; padding-top:1.5rem; border-top:1px solid var(--border-color); text-align:center;">
        <button type="button" class="btn-outline" onclick="MachirikuScreens.handleLogout(false)" style="color:var(--danger-text, #c92a2a); border-color:var(--danger-border, #ffc9c9); background:var(--danger-bg-subtle, #fff5f5); width:100%; max-width:280px; min-height:44px; font-weight:700; font-size:0.95rem; border-radius:var(--radius-md); cursor:pointer; display:inline-flex; align-items:center; justify-content:center; gap:0.5rem; margin:0 auto; box-sizing:border-box;">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
            <polyline points="16 17 21 12 16 7"></polyline>
            <line x1="21" y1="12" x2="9" y2="12"></line>
          </svg>
          <span>ログアウト</span>
        </button>
      </div>
    `;
  }

  function renderS18Business(params, user, sessionData, displayName, email, activeRoleLabel, isOnboarding, emailNotify) {
    const bizCategories = [
      'カフェ・喫茶', '居酒屋・バー', 'ラーメン・麺類', 'レストラン・軽食',
      'テイクアウト・総菜', '小売・物販', '雑貨・書店', '美容室・理容',
      'ネイル・サロン', 'フィットネス', 'コワーキング', 'その他'
    ];
    const defaultPreferred = user.preferredIndustries || ['カフェ・喫茶'];
    const currentArea = user.preferredArea || '';

    const empCount = (user.employeeCount !== undefined && user.employeeCount !== null) ? user.employeeCount : 1;
    const mgmtExp = user.managementExperience || 'なし';
    const indExp = user.industryExperience || '1〜3年未満';
    const qualifications = user.qualifications || [];
    const qList = ['食品衛生責任者', '防火管理者', '調理師免許', '美容師・理容師免許', '宅地建物取引士', 'その他資格・許認可'];

    return `
      <section class="screen-view s18-view s18-business-view" data-screen="S18">
        ${renderHeaderBanner(
          "S18",
          isOnboarding ? "アカウントの初期設定" : "プロフィール・基本情報設定",
          isOnboarding ? activeRoleLabel : "共通",
          isOnboarding
            ? "LINEアカウントから情報を取得しました。事業者名や出店希望条件を入力してマチリクを始めましょう。"
            : "事業者情報、出店希望業種、希望エリア、保有資格などの基本情報を編集できます。"
        )}

        <div class="form-stepper-box" style="max-width: 680px;">
          ${isOnboarding ? `
            <div class="onboarding-welcome-card" style="background: var(--bg-subtle); border: 1.5px solid var(--border-color); border-radius: var(--radius-lg); padding: var(--space-3) var(--space-4); margin-bottom: var(--space-4);">
              <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:0.4rem;">
                <div style="display:flex; align-items:center; gap:0.6rem;">
                  <span style="display:inline-flex; align-items:center; justify-content:center; width:28px; height:28px; background:var(--line-green); color:var(--text-inverse); border-radius:var(--radius-full); flex-shrink:0;">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                  </span>
                  <strong style="font-size:1.02rem; color:var(--text-main);">LINEログインが完了しました</strong>
                </div>
                <span style="display:inline-flex; align-items:center; gap:0.35rem; padding:0.25rem 0.65rem; background:var(--line-green); color:var(--text-inverse); font-size:0.8125rem; font-weight:800; border-radius:var(--radius-full); letter-spacing:0.02em;">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                  <span>アカウント区分: ${escapeHtml(activeRoleLabel)}</span>
                </span>
              </div>
            </div>
          ` : ''}

          <form id="s18-biz-form" onsubmit="MachirikuScreens.handleS18BusinessProfileSubmit(event, this); return false;">
            <!-- 事業者名 / 屋号 -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label for="s18-biz-name" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
                事業者名 / 屋号 <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
              </label>
              <input id="s18-biz-name" name="businessName" type="text" class="form-control" value="${escapeHtml(user.businessName || user.brand || user.company || sessionData?.businessName || '')}" required placeholder="例: 宇都宮カフェ Lab">
              <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※店舗名、屋号、法人名などをご入力ください。</span>
            </div>

            <!-- 代表者名 -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label for="s18-rep-name" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
                代表者名 <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
              </label>
              <input id="s18-rep-name" name="representativeName" type="text" class="form-control" value="${escapeHtml(displayName)}" required placeholder="例: 佐藤 健一" oninput="const h = document.getElementById('header-user-name'); if(h && this.value.trim()) { h.textContent = this.value.trim(); if(window.MachirikuHeaderTicker) window.MachirikuHeaderTicker.update(); }">
              <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINEの表示名が初期設定されています。自由に編集できます。</span>
            </div>

            <!-- メールアドレス -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label for="s18-biz-email" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
                メールアドレス <span style="font-size:0.78rem; color:var(--text-sub);">(変更可能)</span>
              </label>
              <input id="s18-biz-email" name="email" type="email" class="form-control" value="${escapeHtml(email)}" placeholder="example@domain.com">
              <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINE認証で取得したメールアドレスが反映されています。</span>
            </div>

            <!-- 連絡先電話番号 -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label for="s18-biz-phone" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
                連絡先電話番号 <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
              </label>
              <input id="s18-biz-phone" name="phone" type="tel" class="form-control" value="${escapeHtml(user.phone || sessionData?.phone || '')}" required placeholder="例: 09012345678 (ハイフンなし)">
              <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※出店応募や物件オーナーとの連絡・確認用に使用されます。</span>
            </div>

            <!-- 出店希望業種・ジャンル (複数選択可) -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
                出店希望業種・ジャンル <span style="color:var(--danger-text); font-size:0.8rem;">*必須 (複数選択可)</span>
              </label>
              <div style="display:grid; grid-template-columns:repeat(auto-fill, minmax(130px, 1fr)); gap:0.4rem;">
                ${bizCategories.map((cat) => `
                  <label style="display:flex; align-items:center; gap:0.4rem; padding:0.5rem 0.65rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.84rem;">
                    <input type="checkbox" name="preferredIndustry" value="${cat}" ${defaultPreferred.includes(cat) ? 'checked' : ''} style="cursor:pointer; margin:0;">
                    <span>${cat}</span>
                  </label>
                `).join('')}
              </div>
            </div>

            <!-- 出店希望地域 (エリア - 地図なぞり範囲指定 / 選択) -->
            <div class="form-group" style="margin-bottom:1.5rem; background:var(--bg-subtle); padding:var(--space-4); border-radius:var(--radius-lg); border:1px solid var(--border-color);">
              <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.65rem; flex-wrap:wrap; gap:0.5rem;">
                <label for="s18-biz-area" style="font-weight:800; font-size:0.95rem; color:var(--text-main);">
                  出店希望エリア <span style="color:var(--danger-text); font-size:0.8rem;">*必須 (マップ上でなぞって指定可)</span>
                </label>
                <div style="display:flex; align-items:center; gap:0.35rem;">
                  <button type="button" id="s18-draw-btn-draw" class="btn-outline btn-sm is-active" onclick="MachirikuScreens.setBizMapMode('draw')" style="font-size:0.8rem; padding:0.3rem 0.65rem; border-radius:var(--radius-full); display:inline-flex; align-items:center; gap:0.3rem; background:var(--primary-700); color:var(--bg-card); font-weight:700; cursor:pointer;">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 19l7-7 3 3-7 7-3-3z"/><path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/><path d="M2 2l7.586 7.586"/></svg>
                    <span>なぞる</span>
                  </button>
                  <button type="button" id="s18-draw-btn-pan" class="btn-outline btn-sm" onclick="MachirikuScreens.setBizMapMode('pan')" style="font-size:0.8rem; padding:0.3rem 0.65rem; border-radius:var(--radius-full); display:inline-flex; align-items:center; gap:0.3rem; background:var(--bg-card); color:var(--text-main); cursor:pointer;">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 14 14"/></svg>
                    <span>地図移動</span>
                  </button>
                  <button type="button" class="btn-outline btn-sm" onclick="MachirikuScreens.clearBizDraw()" style="font-size:0.8rem; padding:0.3rem 0.65rem; border-radius:var(--radius-full); display:inline-flex; align-items:center; gap:0.25rem; background:var(--bg-card); cursor:pointer;">
                    <span>やり直す</span>
                  </button>
                </div>
              </div>

              <!-- 地図コンテナ -->
              <div id="s18-biz-map-wrap" style="position:relative; width:100%; height:270px; border-radius:var(--radius-md); border:1.5px solid var(--border-color); background-color:var(--bg-subtle); overflow:hidden; touch-action:none;">
                <div id="s18-biz-map-picker" class="s18-map-picker" style="width:100%; height:100%;"></div>
                <div id="s18-biz-draw-surface" style="position:absolute; top:0; left:0; width:100%; height:100%; z-index:5; cursor:crosshair; touch-action:none;"></div>
                <canvas id="s18-biz-draw-canvas" style="position:absolute; top:0; left:0; width:100%; height:100%; pointer-events:none; z-index:6;"></canvas>
                <div id="s18-biz-draw-hint" style="position:absolute; top:8px; left:8px; background:rgba(0,0,0,0.65); color:var(--text-inverse); font-size:0.75rem; font-weight:700; padding:0.25rem 0.6rem; border-radius:var(--radius-md); pointer-events:none; z-index:10;">
                  なぞるモード: 指やマウスで囲んで指定
                </div>
              </div>

              <div style="margin-top:0.75rem;">
                <label for="s18-biz-area" style="font-size:0.8rem; font-weight:700; color:var(--text-sub); display:block; margin-bottom:0.25rem;">
                  出店希望エリア名 (地図をなぞると自動入力・手動修正可):
                </label>
                <input id="s18-biz-area" name="preferredArea" type="text" class="form-control" value="${escapeHtml(currentArea)}" required placeholder="地図上を指やマウスで囲むと自動入力されます（手入力も可能）" style="width:100%;">
                <input id="s18-biz-area-points" name="preferredAreaPoints" type="hidden" value="">
                <div id="s18-biz-map-status" style="font-size:0.75rem; color:var(--primary-700); margin-top:0.35rem; font-weight:600;">
                  ${currentArea ? `✓ 選択中: ${escapeHtml(currentArea)}` : '※地図上をなぞるか、直接エリア名を入力してください'}
                </div>
              </div>
            </div>

            <!-- 従業員数 -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label for="s18-biz-employees" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
                従業員数 (予定含む) <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
              </label>
              <input id="s18-biz-employees" name="employeeCount" type="number" min="0" class="form-control" value="${empCount}" required placeholder="例: 1">
              <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※ご自身のみ・初出店の場合は「1」または「0」と入力してください。</span>
            </div>

            <!-- 店舗経営の経験 -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
                店舗経営の経験 <span style="color:var(--text-muted); font-size:0.8rem;">(任意)</span>
              </label>
              <div style="display:flex; flex-wrap:wrap; gap:0.5rem;">
                <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.85rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.875rem;">
                  <input type="radio" name="mgmtExperience" value="あり" ${mgmtExp === 'あり' ? 'checked' : ''} style="cursor:pointer;">
                  <span>あり (現在経営中 / 過去に経営経験あり)</span>
                </label>
                <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.85rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.875rem;">
                  <input type="radio" name="mgmtExperience" value="なし" ${mgmtExp === 'なし' ? 'checked' : ''} style="cursor:pointer;">
                  <span>なし (新規開業・初出店)</span>
                </label>
              </div>
            </div>

            <!-- 業界での勤務経験 -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
                業界での勤務経験 <span style="color:var(--text-muted); font-size:0.8rem;">(任意)</span>
              </label>
              <div style="display:flex; flex-wrap:wrap; gap:0.5rem;">
                ${['未経験', '1〜3年未満', '3〜5年未満', '5年以上'].map((exp) => `
                  <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.85rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.875rem;">
                    <input type="radio" name="industryExperience" value="${exp}" ${indExp === exp ? 'checked' : ''} style="cursor:pointer;">
                    <span>${exp}</span>
                  </label>
                `).join('')}
              </div>
            </div>

            <!-- 保有資格・許認可 -->
            <div class="form-group" style="margin-bottom:1.5rem;">
              <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
                保有資格・許認可 <span style="color:var(--text-muted); font-size:0.8rem;">(任意・複数選択可)</span>
              </label>
              <div style="display:flex; flex-wrap:wrap; gap:0.5rem;">
                ${qList.map((q) => `
                  <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.75rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.84rem;">
                    <input type="checkbox" name="qualification" value="${q}" ${qualifications.includes(q) ? 'checked' : ''} style="cursor:pointer; margin:0;">
                    <span>${q}</span>
                  </label>
                `).join('')}
              </div>
              <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.35rem;">※出店応募時に必要な資格情報を事前に登録しておくことで、スムーズに応募手続きを行えます。</span>
            </div>

            <!-- 新着店舗・オファー情報のメール受信設定 -->
            <label style="display:flex; align-items:center; gap:0.65rem; cursor:pointer; font-size:0.875rem; color:var(--text-main); font-weight:600; line-height:1.45; margin-top:1.5rem; padding:0.75rem 1rem; background:var(--bg-subtle); border-radius:var(--radius-md); border:1px solid var(--border-color); box-sizing:border-box; min-height:48px;">
              <input type="checkbox" id="s18-biz-email-notify" name="emailNotify" ${emailNotify ? 'checked' : ''} style="cursor:pointer; width:18px; height:18px; margin:0; flex-shrink:0;">
              <span>希望条件に合う新着空き店舗や物件オーナーからのオファー通知をメールで受け取る</span>
            </label>

            <!-- 送信ボタン -->
            <div style="margin-top:1.5rem;">
              <button type="submit" class="btn-primary" style="width:100%; min-height:48px; font-size:1.05rem; font-weight:800; display:flex; align-items:center; justify-content:center; gap:0.5rem; box-shadow:var(--shadow-floating); cursor:pointer;">
                <span>${isOnboarding ? '登録を完了してマチリクを始める' : 'プロフィールを更新する'}</span>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
              </button>
            </div>
          </form>
          ${renderS18LogoutSection(isOnboarding)}
        </div>
      </section>
    `;
  }

  function renderS18Owner(params, user, sessionData, displayName, email, activeRoleLabel, isOnboarding, emailNotify) {
    const ownerCategories = [
      '飲食', '小売', '美容・健康', '教育・仕事', '趣味・娯楽', '生活サービス', '医療・福祉'
    ];
    const defaultPreferred = isOnboarding ? [] : (user.preferredIndustries || []);
    const company = isOnboarding ? '' : (user.company || '');

    return `
      <section class="screen-view s18-view s18-owner-view" data-screen="S18">
        ${renderHeaderBanner(
          "S18",
          isOnboarding ? "アカウントの初期設定" : "プロフィール・基本情報設定",
          isOnboarding ? activeRoleLabel : "共通",
          isOnboarding
            ? "LINEアカウントから情報を取得しました。オーナー情報や誘致したい希望業種を入力してマチリクを始めましょう。"
            : "オーナー情報、屋号・管理名、誘致希望業種などの基本情報を編集できます。"
        )}

        <div class="form-stepper-box" style="max-width: 680px;">
          ${isOnboarding ? `
            <div class="onboarding-welcome-card" style="background: var(--bg-subtle); border: 1.5px solid var(--border-color); border-radius: var(--radius-lg); padding: var(--space-3) var(--space-4); margin-bottom: var(--space-4);">
              <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:0.4rem;">
                <div style="display:flex; align-items:center; gap:0.6rem;">
                  <span style="display:inline-flex; align-items:center; justify-content:center; width:28px; height:28px; background:var(--line-green); color:var(--text-inverse); border-radius:var(--radius-full); flex-shrink:0;">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                  </span>
                  <strong style="font-size:1.02rem; color:var(--text-main);">LINEログインが完了しました</strong>
                </div>
                <span style="display:inline-flex; align-items:center; gap:0.35rem; padding:0.25rem 0.65rem; background:var(--line-green); color:var(--text-inverse); font-size:0.8125rem; font-weight:800; border-radius:var(--radius-full); letter-spacing:0.02em;">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                  <span>アカウント区分: ${escapeHtml(activeRoleLabel)}</span>
                </span>
              </div>
            </div>
          ` : ''}

          <form id="s18-owner-form" onsubmit="MachirikuScreens.handleS18OwnerProfileSubmit(event, this); return false;">
            <!-- オーナー氏名 / 代表者名 -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label for="s18-owner-name" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
                オーナー氏名 / 代表者名 <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
              </label>
              <input id="s18-owner-name" name="representativeName" type="text" class="form-control" value="${escapeHtml(displayName)}" required placeholder="例: 佐藤 武雄" oninput="const h = document.getElementById('header-user-name'); if(h && this.value.trim()) { h.textContent = this.value.trim(); if(window.MachirikuHeaderTicker) window.MachirikuHeaderTicker.update(); }">
              <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINEの表示名が初期設定されています。本名や代表者名にご変更いただけます。</span>
            </div>

            <!-- 屋号・法人名 / 管理名 -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label for="s18-owner-company" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
                屋号・法人名 / 管理名 <span style="font-size:0.78rem; color:var(--text-sub);">(任意)</span>
              </label>
              <input id="s18-owner-company" name="company" type="text" class="form-control" value="${escapeHtml(user.company || company || '')}" placeholder="例: 佐藤不動産管理、宇都宮商事(株) など">
              <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※屋号や法人名、物件の管理名義などをお持ちの場合はご入力ください（空欄でも可）。</span>
            </div>

            <!-- 連絡先電話番号 -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label for="s18-owner-phone" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
                連絡先電話番号 <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
              </label>
              <input id="s18-owner-phone" name="phone" type="tel" class="form-control" value="${escapeHtml(user.phone || sessionData?.phone || '')}" required placeholder="例: 09012345678 (ハイフンなし)">
              <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※出店希望者との連絡、物件問い合わせや運営確認用に使用されます。</span>
            </div>

            <!-- メールアドレス -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label for="s18-owner-email" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
                メールアドレス <span style="font-size:0.78rem; color:var(--text-sub);">(変更可能)</span>
              </label>
              <input id="s18-owner-email" name="email" type="email" class="form-control" value="${escapeHtml(email)}" placeholder="example@domain.com">
              <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINE認証で取得したメールアドレスが反映されています。</span>
            </div>

            <!-- 貸したい業種・誘致希望ジャンル (複数選択可) -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
                貸したい業種・誘致希望ジャンル <span style="color:var(--danger-text); font-size:0.8rem;">*必須 (複数選択可)</span>
              </label>
              <div style="display:grid; grid-template-columns:repeat(auto-fill, minmax(130px, 1fr)); gap:0.4rem;">
                ${ownerCategories.map((cat) => `
                  <label style="display:flex; align-items:center; gap:0.4rem; padding:0.5rem 0.65rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.84rem;">
                    <input type="checkbox" name="preferredIndustry" value="${cat}" ${defaultPreferred.includes(cat) ? 'checked' : ''} style="cursor:pointer; margin:0;">
                    <span>${cat}</span>
                  </label>
                `).join('')}
              </div>
              <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.35rem;">※所有物件や空き区画に誘致したい希望ジャンルを選択してください（需要マッチングやオファー時に活用されます）。</span>
            </div>

            <!-- 出店応募通知のメール受信設定 -->
            <label style="display:flex; align-items:center; gap:0.65rem; cursor:pointer; font-size:0.875rem; color:var(--text-main); font-weight:600; line-height:1.45; margin-top:1.5rem; padding:0.75rem 1rem; background:var(--bg-subtle); border-radius:var(--radius-md); border:1px solid var(--border-color); box-sizing:border-box; min-height:48px;">
              <input type="checkbox" id="s18-owner-email-notify" name="emailNotify" ${emailNotify ? 'checked' : ''} style="cursor:pointer; width:18px; height:18px; margin:0; flex-shrink:0;">
              <span>所有物件への出店応募や事業主からの問い合わせ通知をメールで受け取る</span>
            </label>

            <!-- 送信ボタン -->
            <div style="margin-top:1.5rem;">
              <button type="submit" class="btn-primary" style="width:100%; min-height:48px; font-size:1.05rem; font-weight:800; display:flex; align-items:center; justify-content:center; gap:0.5rem; box-shadow:var(--shadow-floating); cursor:pointer;">
                <span>${isOnboarding ? '登録を完了してマチリクを始める' : 'プロフィールを更新する'}</span>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
              </button>
            </div>
          </form>
          ${renderS18LogoutSection(isOnboarding)}
        </div>
      </section>
    `;
  }

  // ==========================================
  // S18: 地域住民向け「欲しい店舗・施設ジャンル」タブ＆ピル選択状態・ヘルパー
  // ==========================================
  const RESIDENT_TAXONOMY_FALLBACK = [
    {
      id: "C01", name: "飲食",
      subcategories: [
        { id: "I0101", name: "カフェ・喫茶店" },
        { id: "I0102", name: "レストラン" },
        { id: "I0103", name: "ラーメン・麺類" },
        { id: "I0104", name: "定食・食堂" },
        { id: "I0105", name: "居酒屋・バー" },
        { id: "I0106", name: "弁当・テイクアウト" },
        { id: "I0107", name: "パン屋・ベーカリー" }
      ]
    },
    {
      id: "C02", name: "小売",
      subcategories: [
        { id: "I0201", name: "コンビニ・小型スーパー" },
        { id: "I0202", name: "食料品・生鮮店" },
        { id: "I0203", name: "雑貨・生活用品店" },
        { id: "I0204", name: "衣料品・セレクトショップ" },
        { id: "I0205", name: "書店・文具店" },
        { id: "I0206", name: "花屋・フラワーショップ" }
      ]
    },
    {
      id: "C03", name: "美容・健康",
      subcategories: [
        { id: "I0301", name: "美容室・ヘアサロン" },
        { id: "I0302", name: "理容室・バーバー" },
        { id: "I0303", name: "ネイル・エステサロン" },
        { id: "I0304", name: "ジム・フィットネススタジオ" }
      ]
    },
    {
      id: "C04", name: "教育・仕事",
      subcategories: [
        { id: "I0401", name: "学習塾・スクール" },
        { id: "I0402", name: "コワーキングスペース" }
      ]
    },
    {
      id: "C05", name: "趣味・娯楽",
      subcategories: [
        { id: "I0501", name: "ゲーム・ホビーショップ" },
        { id: "I0502", name: "娯楽施設" },
        { id: "I0503", name: "文化・体験教室" }
      ]
    },
    {
      id: "C06", name: "生活サービス",
      subcategories: [
        { id: "I0601", name: "クリーニング店" },
        { id: "I0602", name: "修理サービス" },
        { id: "I0603", name: "ペット関連店舗" }
      ]
    },
    {
      id: "C07", name: "医療・福祉",
      subcategories: [
        { id: "I0701", name: "クリニック" },
        { id: "I0702", name: "福祉・介護サービス" }
      ]
    }
  ];

  let residentActiveCatId = 'C01';
  let residentSelectedIndustries = new Set();

  function getResidentTaxonomy() {
    if (typeof window !== 'undefined' && window.MACHIRIKU_DATA && Array.isArray(window.MACHIRIKU_DATA.CATEGORIES)) {
      return window.MACHIRIKU_DATA.CATEGORIES.map(cat => ({
        id: cat.id,
        name: cat.name,
        subcategories: (cat.subcategories || []).map(s => ({
          id: s.id,
          name: s.name
        }))
      }));
    }
    return RESIDENT_TAXONOMY_FALLBACK;
  }

  function renderResidentCatTabsHtml() {
    const taxonomy = getResidentTaxonomy();
    return taxonomy.map(cat => {
      const isActive = (cat.id === residentActiveCatId);
      const count = cat.subcategories.filter(s => residentSelectedIndustries.has(s.id) || residentSelectedIndustries.has(s.name)).length;
      return `
        <button type="button" class="resident-cat-tab-btn ${isActive ? 'is-active' : ''}" onclick="MachirikuScreens.switchResidentCatTab('${cat.id}')">
          <span>${cat.name}</span>
          ${count > 0 ? `<span class="resident-cat-tab-badge">${count}</span>` : ''}
        </button>
      `;
    }).join('');
  }

  function renderResidentSubcatPillsHtml(catId) {
    const taxonomy = getResidentTaxonomy();
    const cat = taxonomy.find(c => c.id === catId) || taxonomy[0];
    const allSelected = cat.subcategories.length > 0 && cat.subcategories.every(s => residentSelectedIndustries.has(s.id) || residentSelectedIndustries.has(s.name));

    // 2枚目画像の一番最初: 「すべて選択」ボタン
    const selectAllBtnHtml = `
      <button type="button" class="resident-ind-pill resident-ind-pill-all ${allSelected ? 'is-selected' : ''}" onclick="MachirikuScreens.toggleAllResidentCatIndustries('${cat.id}')">
        <span class="resident-ind-pill-icon">${allSelected ? '✓' : '+'}</span>
        <span>${allSelected ? 'すべて選択中' : 'すべて選択'}</span>
      </button>
    `;

    const pillsHtml = cat.subcategories.map(sub => {
      const isSelected = residentSelectedIndustries.has(sub.id) || residentSelectedIndustries.has(sub.name);
      return `
        <button type="button" class="resident-ind-pill ${isSelected ? 'is-selected' : ''}" onclick="MachirikuScreens.toggleResidentIndustry('${sub.id}')">
          <span class="resident-ind-pill-icon">${isSelected ? '✓' : '+'}</span>
          <span>${sub.name}</span>
        </button>
      `;
    }).join('');

    return selectAllBtnHtml + pillsHtml;
  }

  function toggleAllResidentCatIndustries(catId) {
    const taxonomy = getResidentTaxonomy();
    const cat = taxonomy.find(c => c.id === catId) || taxonomy[0];
    const allSubs = cat.subcategories;
    const allSelected = allSubs.length > 0 && allSubs.every(s => residentSelectedIndustries.has(s.id) || residentSelectedIndustries.has(s.name));

    if (allSelected) {
      allSubs.forEach(s => {
        residentSelectedIndustries.delete(s.id);
        residentSelectedIndustries.delete(s.name);
      });
    } else {
      allSubs.forEach(s => {
        residentSelectedIndustries.add(s.id);
      });
    }
    syncResidentIndustryUI();
  }

  function renderResidentSelectedTagsHtml() {
    const taxonomy = getResidentTaxonomy();
    const allSubs = [];
    taxonomy.forEach(c => {
      c.subcategories.forEach(s => {
        if (residentSelectedIndustries.has(s.id) || residentSelectedIndustries.has(s.name)) {
          allSubs.push({ ...s, catName: c.name });
        }
      });
    });

    if (allSubs.length === 0) {
      return `<span style="font-size:0.75rem; color:var(--text-muted); padding:0.05rem 0.2rem;">※下のタブからジャンルを選んでください（複数選択可）</span>`;
    }

    const tagsHtml = allSubs.map(s => `
      <span class="resident-selected-tag">
        <span>${s.name}</span>
        <button type="button" class="resident-selected-tag-del" onclick="MachirikuScreens.removeResidentIndustry('${s.id}')" title="${s.name}を解除">×</button>
      </span>
    `).join('');

    return `
      <div style="display:flex; flex-wrap:wrap; gap:0.3rem 0.4rem; align-items:center; width:100%;">
        ${tagsHtml}
        <button type="button" class="resident-clear-all-btn" onclick="MachirikuScreens.clearAllResidentIndustries()">
          すべて解除
        </button>
      </div>
    `;
  }

  function syncResidentIndustryUI() {
    if (typeof document === 'undefined') return;
    const countEl = document.getElementById('s18-resident-ind-count');
    if (countEl) countEl.textContent = `選択中: ${residentSelectedIndustries.size}件`;

    const tagsEl = document.getElementById('s18-resident-selected-tags');
    if (tagsEl) tagsEl.innerHTML = renderResidentSelectedTagsHtml();

    const tabsEl = document.getElementById('s18-resident-cat-tabs');
    if (tabsEl) tabsEl.innerHTML = renderResidentCatTabsHtml();

    const panelEl = document.getElementById('s18-resident-subcat-panel');
    if (panelEl) panelEl.innerHTML = renderResidentSubcatPillsHtml(residentActiveCatId);

    const hiddenInput = document.getElementById('s18-preferred-industries');
    if (hiddenInput) {
      hiddenInput.value = JSON.stringify(Array.from(residentSelectedIndustries));
    }
  }

  function switchResidentCatTab(catId) {
    residentActiveCatId = catId;
    if (typeof document !== 'undefined') {
      const tabsEl = document.getElementById('s18-resident-cat-tabs');
      if (tabsEl) tabsEl.innerHTML = renderResidentCatTabsHtml();
      const panelEl = document.getElementById('s18-resident-subcat-panel');
      if (panelEl) panelEl.innerHTML = renderResidentSubcatPillsHtml(catId);
    }
  }

  function toggleResidentIndustry(subId) {
    if (residentSelectedIndustries.has(subId)) {
      residentSelectedIndustries.delete(subId);
    } else {
      residentSelectedIndustries.add(subId);
    }
    syncResidentIndustryUI();
  }

  function removeResidentIndustry(subId) {
    residentSelectedIndustries.delete(subId);
    const taxonomy = getResidentTaxonomy();
    for (const cat of taxonomy) {
      const match = cat.subcategories.find(s => s.id === subId);
      if (match) {
        residentSelectedIndustries.delete(match.name);
      }
    }
    syncResidentIndustryUI();
  }

  function clearAllResidentIndustries() {
    residentSelectedIndustries.clear();
    syncResidentIndustryUI();
  }

  function renderS18(params = {}) {
    s18GoogleMap = null;
    s18Marker = null;
    s18WorkGoogleMap = null;
    s18WorkMarker = null;
    const state = store.getState();
    const user = state.currentUser || {};

    let sessionData = null;
    try {
      if (typeof window !== 'undefined' && window.sessionStorage) {
        const raw = window.sessionStorage.getItem('machiriku_onboarding');
        if (raw) sessionData = JSON.parse(raw);
      }
    } catch (_) {}

    const isOnboarding = Boolean(
      (params && (params.onboarding === '1' || params.onboarding === true || params.onboarding === 'true')) ||
      (sessionData && sessionData.role)
    );

    const realUserDisplayName = (user.name && user.name !== '田中 一郎' && user.name !== 'LINEユーザー') ? user.name : (user.displayName || '');
    const displayName = params?.displayName || sessionData?.name || realUserDisplayName || user.name || 'LINEユーザー';
    const email = user.email || (params?.email !== undefined ? params.email : (sessionData?.email || ''));
    const ageGroup = user.ageGroup || sessionData?.ageGroup || '';
    const gender = user.gender || sessionData?.gender || '';
    const industry = user.industry || sessionData?.industry || '';
    const savedPref = user.preferredIndustries || sessionData?.preferredIndustries || [];
    residentSelectedIndustries = new Set(Array.isArray(savedPref) ? savedPref : []);
    if (!residentActiveCatId) {
      residentActiveCatId = 'C01';
    }
    const livingArea = user.livingArea || sessionData?.livingArea || '栃木県宇都宮市本町1-1';
    const livingLat = (user.livingCoords && user.livingCoords.lat) || (sessionData && sessionData.livingLat) || 36.5610;
    const livingLng = (user.livingCoords && user.livingCoords.lng) || (sessionData && sessionData.livingLng) || 139.8830;
    const workAddress = user.workAddress || sessionData?.workAddress || '';
    const workLat = (user.workCoords && user.workCoords.lat) || 36.5590;
    const workLng = (user.workCoords && user.workCoords.lng) || 139.8980;
    const emailNotify = (user.emailNotify !== false && sessionData?.emailNotify !== false);

    const ageOptions = ['未設定', '10代', '20代', '30代', '40代', '50代', '60代以上'];
    const genderOptions = ['回答しない', '男性', '女性', 'その他'];
    const industryOptions = [
      '飲食・カフェ',
      '小売・物販',
      '美容・サロン',
      '医療・介護・福祉',
      'IT・通信・オフィス',
      '建設・不動産',
      '教育・スクール',
      'サービス・エンタメ',
      '学生',
      '主婦・主夫',
      'その他'
    ];

    const activeRoleKey = String(user?.role || user?.userRole || state.currentRole || params?.role || sessionData?.role || 'resident').toLowerCase();
    const roleLabelMap = {
      resident: '地域住民',
      business: '出店事業主',
      owner: '物件オーナー',
      admin: '運営管理者'
    };
    const activeRoleLabel = roleLabelMap[activeRoleKey] || '地域住民';

    // 出店事業主向けの初期設定・プロフィール画面
    if (activeRoleKey === 'business') {
      return renderS18Business(params, user, sessionData, displayName, email, activeRoleLabel, isOnboarding, emailNotify);
    }

    // 物件オーナー向けの初期設定・プロフィール画面
    if (activeRoleKey === 'owner') {
      return renderS18Owner(params, user, sessionData, displayName, email, activeRoleLabel, isOnboarding, emailNotify);
    }

    return `
      <section class="screen-view s18-view" data-screen="S18">
        ${renderHeaderBanner(
          "S18",
          isOnboarding ? "アカウントの初期設定" : "プロフィール・基本情報設定",
          isOnboarding ? activeRoleLabel : "共通",
          isOnboarding
            ? "LINEアカウントから情報を取得しました。お住まいの地域など基本情報を入力してマチリクを始めましょう。"
            : "アカウント情報、居住地、お仕事・活動エリアなどの基本情報を編集できます。"
        )}

        <div class="form-stepper-box" style="max-width: 680px;">
          ${isOnboarding ? `
            <div class="onboarding-welcome-card" style="background: var(--bg-subtle); border: 1.5px solid var(--border-color); border-radius: var(--radius-lg); padding: var(--space-3) var(--space-4); margin-bottom: var(--space-4);">
              <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:0.4rem;">
                <div style="display:flex; align-items:center; gap:0.6rem;">
                  <span style="display:inline-flex; align-items:center; justify-content:center; width:28px; height:28px; background:var(--line-green); color:var(--text-inverse); border-radius:var(--radius-full); flex-shrink:0;">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                  </span>
                  <strong style="font-size:1.02rem; color:var(--text-main);">LINEログインが完了しました</strong>
                </div>
                <span style="display:inline-flex; align-items:center; gap:0.35rem; padding:0.25rem 0.65rem; background:var(--line-green); color:var(--text-inverse); font-size:0.8125rem; font-weight:800; border-radius:var(--radius-full); letter-spacing:0.02em;">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                  <span>アカウント区分: ${escapeHtml(activeRoleLabel)}</span>
                </span>
              </div>
            </div>
          ` : ''}

          <form id="s18-profile-form" onsubmit="MachirikuScreens.handleS18ProfileSubmit(event, this); return false;">
            <!-- ニックネーム (表示名) -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label for="s18-name" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
                ニックネーム (表示名) <span style="color:var(--danger-text); font-size:0.8rem;">*必須</span>
              </label>
              <input id="s18-name" name="name" type="text" class="form-control" value="${escapeHtml(displayName)}" required placeholder="例: 山田 太郎" oninput="const h = document.getElementById('header-user-name'); if(h && this.value.trim()) { h.textContent = this.value.trim(); if(window.MachirikuHeaderTicker) window.MachirikuHeaderTicker.update(); }">
              <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINEの表示名が初期設定されています。自由に編集できます。</span>
            </div>

            <!-- メールアドレス -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label for="s18-email" style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.35rem;">
                メールアドレス <span style="font-size:0.78rem; color:var(--text-sub);">(変更可能)</span>
              </label>
              <input id="s18-email" name="email" type="email" class="form-control" value="${escapeHtml(email)}" placeholder="example@domain.com">
              <span style="font-size:0.75rem; color:var(--text-sub); display:block; margin-top:0.25rem;">※LINE認証で取得したメールアドレスが反映されています。</span>
            </div>

            <!-- 年齢 (任意・ラジオボタン) -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
                年齢 <span style="color:var(--text-muted); font-size:0.8rem;">(任意)</span>
              </label>
              <div class="setup-age-grid" style="display:grid; grid-template-columns:repeat(7, minmax(0, 1fr)); gap:0.35rem; width:100%; box-sizing:border-box;">
                ${ageOptions.map(opt => `
                  <label style="display:flex; align-items:center; justify-content:center; gap:0.25rem; padding:0.45rem 0.15rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.8125rem; white-space:nowrap; text-align:center; box-sizing:border-box;">
                    <input type="radio" name="ageGroup" value="${opt}" ${(ageGroup === opt || (!ageGroup && opt === '未設定')) ? 'checked' : ''} style="cursor:pointer; margin:0; flex-shrink:0;">
                    <span>${opt}</span>
                  </label>
                `).join('')}
              </div>
            </div>

            <!-- 性別 (任意・ラジオボタン) -->
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label style="font-weight:700; font-size:0.9rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
                性別 <span style="color:var(--text-muted); font-size:0.8rem;">(任意)</span>
              </label>
              <div style="display:flex; flex-wrap:wrap; gap:0.5rem;">
                ${genderOptions.map(opt => `
                  <label style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.45rem 0.85rem; border:1px solid var(--border-color); border-radius:var(--radius-md); background:var(--bg-card); cursor:pointer; font-size:0.875rem;">
                    <input type="radio" name="gender" value="${opt}" ${(gender === opt || (!gender && opt === '回答しない')) ? 'checked' : ''} style="cursor:pointer;">
                    <span>${opt}</span>
                  </label>
                `).join('')}
              </div>
            </div>

            <!-- 近くに欲しい店舗・施設ジャンル (任意・複数選択) -->
            <div class="form-group resident-industry-picker">
              <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.45rem; flex-wrap:wrap; gap:0.4rem;">
                <label style="font-weight:800; font-size:0.92rem; color:var(--text-main); display:flex; align-items:center; gap:0.35rem;">
                  <span>近くに欲しい店舗・施設ジャンル</span>
                  <span style="color:var(--text-muted); font-size:0.78rem; font-weight:normal;">(任意・複数選択可)</span>
                </label>
                <span id="s18-resident-ind-count" style="font-size:0.75rem; font-weight:700; color:var(--primary-700); background:var(--primary-50); border:1px solid var(--primary-100); padding:0.15rem 0.55rem; border-radius:var(--radius-full);">
                  選択中: ${residentSelectedIndustries.size}件
                </span>
              </div>

              <!-- 選択中のピル一覧（タグエリア） -->
              <div id="s18-resident-selected-tags" class="resident-selected-tags-box">
                ${renderResidentSelectedTagsHtml()}
              </div>

              <!-- 大分類タブバー -->
              <div style="margin-bottom:0.45rem;">
                <div class="resident-cat-tabs-bar" id="s18-resident-cat-tabs">
                  ${renderResidentCatTabsHtml()}
                </div>
              </div>

              <!-- 小分類ピル（チップ）選択エリア -->
              <div id="s18-resident-subcat-panel" class="resident-subcat-grid">
                ${renderResidentSubcatPillsHtml(residentActiveCatId)}
              </div>

              <!-- フォーム送信用の隠しフィールド -->
              <input type="hidden" id="s18-preferred-industries" name="preferredIndustries" value="${escapeHtml(JSON.stringify(Array.from(residentSelectedIndustries)))}">
            </div>

            <!-- 居住地の住所 (マップ上で選択・必須) -->
            <div class="form-group" style="margin-bottom:1.5rem; background:var(--bg-subtle); padding:var(--space-4); border-radius:var(--radius-lg); border:1px solid var(--border-color);">
              <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.35rem; flex-wrap:wrap; gap:0.5rem;">
                <label for="s18-living-address" style="font-weight:800; font-size:0.95rem; color:var(--text-main);">
                  居住地の住所 <span style="color:var(--danger-text); font-size:0.8rem;">*必須 (マップ上で選択)</span>
                </label>
                <button type="button" class="btn-outline btn-sm" onclick="MachirikuScreens.handleS18CurrentLocation()" style="font-size:0.8rem; padding:0.3rem 0.75rem; border-radius:var(--radius-full); display:inline-flex; align-items:center; gap:0.35rem; background:var(--bg-card); cursor:pointer;">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/><line x1="12" y1="2" x2="12" y2="6"/><line x1="12" y1="18" x2="12" y2="22"/><line x1="2" y1="12" x2="6" y2="12"/><line x1="18" y1="12" x2="22" y2="12"/></svg>
                  <span>現在地から自動設定</span>
                </button>
              </div>
              <p style="font-size:0.8rem; color:var(--text-sub); margin-bottom:0.6rem; line-height:1.4;">
                地図上をクリックするかピンをドラッグして、お住まいの居住地を設定してください。
              </p>

              <!-- 地図コンテナ -->
              <div id="s18-map-picker" class="s18-map-picker" style="width:100%; height:260px; border-radius:var(--radius-md); border:1.5px solid var(--border-color); background-color:var(--bg-subtle); position:relative; overflow:hidden;"></div>

              <div style="margin-top:0.75rem;">
                <label for="s18-living-address" style="font-size:0.8rem; font-weight:700; color:var(--text-sub); display:block; margin-bottom:0.25rem;">
                  選択中の居住地住所:
                </label>
                <input id="s18-living-address" name="livingAddress" type="text" class="form-control" value="${escapeHtml(livingArea)}" required placeholder="地図をクリックすると自動で住所が設定されます">
                <input id="s18-living-lat" name="livingLat" type="hidden" value="${livingLat}">
                <input id="s18-living-lng" name="livingLng" type="hidden" value="${livingLng}">
                <div id="s18-map-status" style="font-size:0.75rem; color:var(--primary-700); margin-top:0.35rem; font-weight:600;">
                  ✓ 選択中: (${Number(livingLat).toFixed(4)}, ${Number(livingLng).toFixed(4)})
                </div>
              </div>
            </div>

            <!-- 職場や学校などの住所 (任意) -->
            <div class="form-group" style="margin-bottom:1.75rem;">
              <!-- 登録前・折りたたみ時のブロックスイッチ (画像①) -->
              <button type="button" id="s18-work-trigger-card" class="work-register-card-trigger" onclick="MachirikuScreens.toggleS18WorkMap(true)" style="${workAddress ? 'display:none;' : 'display:flex;'} width:100%; justify-content:space-between; align-items:center; padding:1.1rem 1.25rem; background:var(--bg-card); border:1.5px solid var(--border-color); border-radius:var(--radius-md); cursor:pointer; text-align:left; box-sizing:border-box;">
                <span id="s18-work-trigger-label" style="font-size:0.95rem; font-weight:700; color:var(--text-main);">${workAddress ? `職場・学校: ${escapeHtml(workAddress)}` : '職場や学校を登録する'}</span>
                <span id="s18-work-trigger-icon" style="font-size:1.25rem; color:var(--text-sub); line-height:1; font-weight:700;">${workAddress ? '変更' : '＋'}</span>
              </button>

              <!-- クリック時に展開されるブロック (画像②) -->
              <div id="s18-work-map-area" style="${workAddress ? 'display:block;' : 'display:none;'} background:var(--bg-subtle); padding:var(--space-4); border-radius:var(--radius-lg); border:1px solid var(--border-color);">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.4rem; flex-wrap:wrap; gap:0.5rem;">
                  <label for="s18-work-address" style="font-weight:800; font-size:0.95rem; color:var(--text-main); margin:0;">
                    職場や学校の住所 <span style="color:var(--text-muted); font-size:0.8rem;">(任意・マップ上で選択)</span>
                  </label>
                  <div style="display:flex; align-items:center; gap:0.5rem;">
                    <button type="button" class="btn-outline btn-sm" onclick="MachirikuScreens.handleS18WorkCurrentLocation()" style="font-size:0.8rem; padding:0.3rem 0.75rem; border-radius:var(--radius-full); display:inline-flex; align-items:center; gap:0.35rem; background:var(--bg-card); cursor:pointer;">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/><line x1="12" y1="2" x2="12" y2="6"/><line x1="12" y1="18" x2="12" y2="22"/><line x1="2" y1="12" x2="6" y2="12"/><line x1="18" y1="12" x2="22" y2="12"/></svg>
                      <span>現在地から自動設定</span>
                    </button>
                    <button type="button" class="btn-outline btn-sm" onclick="MachirikuScreens.toggleS18WorkMap(false)" style="font-size:0.8rem; padding:0.3rem 0.65rem; border-radius:var(--radius-full); display:inline-flex; align-items:center; gap:0.25rem; background:var(--bg-card); cursor:pointer;">
                      <span>✕ 閉じる</span>
                    </button>
                  </div>
                </div>
                <p style="font-size:0.8rem; color:var(--text-sub); margin-bottom:0.6rem; line-height:1.4;">
                  地図上をクリックするかピンをドラッグして、勤務先や学校を設定してください。
                </p>

                <!-- 地図コンテナ -->
                <div id="s18-work-map-picker" class="s18-map-picker" style="width:100%; height:260px; border-radius:var(--radius-md); border:1.5px solid var(--border-color); background-color:var(--bg-subtle); position:relative; overflow:hidden;"></div>

                <div style="margin-top:0.75rem;">
                  <label for="s18-work-address" style="font-size:0.8rem; font-weight:700; color:var(--text-sub); display:block; margin-bottom:0.25rem;">
                    選択中の職場・学校住所:
                  </label>
                  <input id="s18-work-address" name="workAddress" type="text" class="form-control" value="${escapeHtml(workAddress)}" placeholder="地図をクリックすると自動で住所が設定されます">
                  <input id="s18-work-lat" name="workLat" type="hidden" value="${workLat}">
                  <input id="s18-work-lng" name="workLng" type="hidden" value="${workLng}">
                  <div id="s18-work-map-status" style="font-size:0.75rem; color:var(--primary-700); margin-top:0.35rem; font-weight:600;">
                    ${workAddress ? `✓ 選択中: (${Number(workLat).toFixed(4)}, ${Number(workLng).toFixed(4)})` : '※地図をクリックしてピンを配置してください'}
                  </div>
                </div>
              </div>
            </div>

            <!-- 新着店舗・新着クーポン情報のメール受信設定 -->
            <label style="display:flex; align-items:center; gap:0.65rem; cursor:pointer; font-size:0.875rem; color:var(--text-main); font-weight:600; line-height:1.45; margin-top:1.5rem; padding:0.75rem 1rem; background:var(--bg-subtle); border-radius:var(--radius-md); border:1px solid var(--border-color); box-sizing:border-box; min-height:48px;">
              <input type="checkbox" id="s18-email-notify" name="emailNotify" ${emailNotify ? 'checked' : ''} style="cursor:pointer; width:18px; height:18px; margin:0; flex-shrink:0;">
              <span>新着店舗や新着クーポン情報をメールで受け取る</span>
            </label>

            <!-- 送信ボタン -->
            <div style="margin-top:1.5rem;">
              <button type="submit" class="btn-primary" style="width:100%; min-height:48px; font-size:1.05rem; font-weight:800; display:flex; align-items:center; justify-content:center; gap:0.5rem; box-shadow:var(--shadow-floating); cursor:pointer;" onclick="MachirikuApp.showToast('プロフィールを更新しました')">
                <span>${isOnboarding ? '登録を完了してマチリクを始める' : 'プロフィールを更新する'}</span>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
              </button>
            </div>
          </form>
          ${renderS18LogoutSection(isOnboarding)}
        </div>
      </section>
    `;
  }

  function initS18MapPicker() {
    if (typeof document === 'undefined') return;
    const workArea = document.getElementById('s18-work-map-area');
    if (workArea && workArea.style.display !== 'none') {
      initS18WorkMapPicker();
    }
    const container = document.getElementById('s18-map-picker');
    if (!container) return;

    const latInput = document.getElementById('s18-living-lat');
    const lngInput = document.getElementById('s18-living-lng');
    const addrInput = document.getElementById('s18-living-address');
    const statusEl = document.getElementById('s18-map-status');

    let currentLat = latInput ? parseFloat(latInput.value) : 36.5610;
    let currentLng = lngInput ? parseFloat(lngInput.value) : 139.8830;
    if (isNaN(currentLat) || isNaN(currentLng)) {
      currentLat = 36.5610;
      currentLng = 139.8830;
    }

    function onLocationSelected(lat, lng) {
      if (latInput) latInput.value = lat;
      if (lngInput) lngInput.value = lng;
      if (statusEl) {
        statusEl.textContent = `✓ 選択中: (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
      }
      reverseGeocodeWithGoogle(lat, lng, (res) => {
        const resolved = res.formattedAddress || res.name || (res.city ? `栃木県${res.city}` : '栃木県宇都宮市');
        if (addrInput) {
          addrInput.value = resolved;
          addrInput.classList.add('is-flash-updated');
          setTimeout(() => addrInput.classList.remove('is-flash-updated'), 1000);
        }
        if (statusEl) {
          statusEl.textContent = `✓ 選択中: (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
        }
      });
    }

    // Google Maps API 利用可能な場合
    if (typeof google !== 'undefined' && google.maps && google.maps.Map) {
      const initialPos = { lat: currentLat, lng: currentLng };
      s18GoogleMap = new google.maps.Map(container, {
        center: initialPos,
        zoom: 14,
        mapTypeControl: false,
        streetViewControl: false,
        fullscreenControl: false,
        zoomControl: false,
        gestureHandling: 'greedy',
        scrollwheel: true
      });

      s18Marker = new google.maps.Marker({
        position: initialPos,
        map: s18GoogleMap,
        draggable: true,
        title: "居住地ピン"
      });

      google.maps.event.addListener(s18GoogleMap, 'click', (e) => {
        const lat = e.latLng.lat();
        const lng = e.latLng.lng();
        s18Marker.setPosition(e.latLng);
        onLocationSelected(lat, lng);
      });

      google.maps.event.addListener(s18Marker, 'dragend', (e) => {
        const lat = e.latLng.lat();
        const lng = e.latLng.lng();
        onLocationSelected(lat, lng);
      });
      return;
    }

    // Google Maps API 未読み込み・テスト実行時のフォールバックUI
    container.innerHTML = `
      <div style="width:100%; height:100%; display:flex; flex-direction:column; align-items:center; justify-content:center; background:var(--bg-subtle); padding:1rem; text-align:center;">
        <div style="margin-bottom:0.25rem;"><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="10" r="3"/><path d="M12 2a8 8 0 0 0-8 8c0 5.25 8 12 8 12s8-6.75 8-12a8 8 0 0 0-8-8z"/></svg></div>
        <div style="font-weight:700; font-size:0.875rem; color:var(--primary-900); margin-bottom:0.25rem;">宇都宮居住地マップ（テスト選択）</div>
        <p style="font-size:0.75rem; color:var(--text-sub); margin-bottom:0.5rem;">クリックして居住地を選択してください</p>
        <div style="display:flex; gap:0.4rem; flex-wrap:wrap; justify-content:center;">
          <button type="button" class="btn-outline btn-sm" style="font-size:0.75rem; background:var(--bg-card);" onclick="MachirikuScreens.setS18Coords(36.5610, 139.8830, '栃木県宇都宮市本町1-1')">本町周辺</button>
          <button type="button" class="btn-outline btn-sm" style="font-size:0.75rem; background:var(--bg-card);" onclick="MachirikuScreens.setS18Coords(36.5590, 139.8980, '栃木県宇都宮市駅前通り3丁目')">宇都宮駅前</button>
          <button type="button" class="btn-outline btn-sm" style="font-size:0.75rem; background:var(--bg-card);" onclick="MachirikuScreens.setS18Coords(36.5660, 139.8700, '栃木県宇都宮市松原2丁目')">松原周辺</button>
        </div>
      </div>
    `;
  }

  function handleS18CurrentLocation() {
    if (typeof navigator !== 'undefined' && navigator.geolocation) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('現在地を取得しています...');
      }
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const lat = pos.coords.latitude;
          const lng = pos.coords.longitude;
          setS18Coords(lat, lng);
        },
        (err) => {
          setS18Coords(36.5590, 139.8980, '栃木県宇都宮市駅前通り3丁目');
          if (typeof MachirikuApp !== 'undefined') {
            MachirikuApp.showToast('位置情報取得不可のため、宇都宮駅前を設定しました');
          }
        },
        { timeout: 7000 }
      );
    } else {
      setS18Coords(36.5610, 139.8830, '栃木県宇都宮市本町1-1');
    }
  }

  function setS18Coords(lat, lng, manualAddr = null) {
    if (typeof document === 'undefined') return;
    const latInput = document.getElementById('s18-living-lat');
    const lngInput = document.getElementById('s18-living-lng');
    const addrInput = document.getElementById('s18-living-address');
    const statusEl = document.getElementById('s18-map-status');

    if (latInput) latInput.value = lat;
    if (lngInput) lngInput.value = lng;

    if (s18GoogleMap && typeof google !== 'undefined' && google.maps) {
      const newPos = new google.maps.LatLng(lat, lng);
      s18GoogleMap.panTo(newPos);
      if (s18Marker) s18Marker.setPosition(newPos);
    }

    if (manualAddr && addrInput) {
      addrInput.value = manualAddr;
      if (statusEl) statusEl.textContent = `✓ 選択中: (${Number(lat).toFixed(4)}, ${Number(lng).toFixed(4)})`;
      return;
    }

    if (statusEl) statusEl.textContent = `✓ 選択中: (${Number(lat).toFixed(4)}, ${Number(lng).toFixed(4)})`;
    reverseGeocodeWithGoogle(lat, lng, (res) => {
      const resolved = res.formattedAddress || res.name || (res.city ? `栃木県${res.city}` : '栃木県宇都宮市');
      if (addrInput) addrInput.value = resolved;
      if (statusEl) statusEl.textContent = `✓ 選択中: (${Number(lat).toFixed(4)}, ${Number(lng).toFixed(4)})`;
    });
  }

  function toggleS18WorkMap(forceOpen) {
    if (typeof document === 'undefined') return;
    const triggerCard = document.getElementById('s18-work-trigger-card');
    const area = document.getElementById('s18-work-map-area');
    if (!area) return;

    const isHidden = (area.style.display === 'none' || area.style.display === '');
    const shouldOpen = (forceOpen !== undefined) ? !!forceOpen : isHidden;

    if (shouldOpen) {
      if (triggerCard) triggerCard.style.display = 'none';
      area.style.display = 'block';
      initS18WorkMapPicker();
      if (s18WorkGoogleMap && typeof google !== 'undefined' && google.maps && google.maps.event) {
        setTimeout(() => {
          google.maps.event.trigger(s18WorkGoogleMap, 'resize');
          const latInput = document.getElementById('s18-work-lat');
          const lngInput = document.getElementById('s18-work-lng');
          const lat = latInput ? parseFloat(latInput.value) : 36.5590;
          const lng = lngInput ? parseFloat(lngInput.value) : 139.8980;
          s18WorkGoogleMap.setCenter({ lat: isNaN(lat) ? 36.5590 : lat, lng: isNaN(lng) ? 139.8980 : lng });
        }, 100);
      }
    } else {
      area.style.display = 'none';
      if (triggerCard) {
        triggerCard.style.display = 'flex';
        const workAddrInput = document.getElementById('s18-work-address');
        const triggerLabel = document.getElementById('s18-work-trigger-label');
        const triggerIcon = document.getElementById('s18-work-trigger-icon');
        const val = workAddrInput ? workAddrInput.value.trim() : '';
        if (triggerLabel) {
          triggerLabel.textContent = val ? `職場・学校: ${val}` : '職場や学校を登録する';
        }
        if (triggerIcon) {
          triggerIcon.textContent = val ? '変更' : '＋';
        }
      }
    }
  }

  function initS18WorkMapPicker() {
    if (typeof document === 'undefined') return;
    const container = document.getElementById('s18-work-map-picker');
    if (!container) return;

    const latInput = document.getElementById('s18-work-lat');
    const lngInput = document.getElementById('s18-work-lng');
    const addrInput = document.getElementById('s18-work-address');
    const statusEl = document.getElementById('s18-work-map-status');

    let currentLat = latInput ? parseFloat(latInput.value) : 36.5590;
    let currentLng = lngInput ? parseFloat(lngInput.value) : 139.8980;
    if (isNaN(currentLat) || isNaN(currentLng)) {
      currentLat = 36.5590;
      currentLng = 139.8980;
    }

    function onWorkLocationSelected(lat, lng) {
      if (latInput) latInput.value = lat;
      if (lngInput) lngInput.value = lng;
      if (statusEl) {
        statusEl.textContent = `✓ 選択中: (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
      }
      reverseGeocodeWithGoogle(lat, lng, (res) => {
        const resolved = res.formattedAddress || res.name || (res.city ? `栃木県${res.city}` : '栃木県宇都宮市');
        if (addrInput) {
          addrInput.value = resolved;
          addrInput.classList.add('is-flash-updated');
          setTimeout(() => addrInput.classList.remove('is-flash-updated'), 1000);
        }
        if (statusEl) {
          statusEl.textContent = `✓ 選択中: (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
        }
      });
    }

    if (typeof google !== 'undefined' && google.maps && google.maps.Map) {
      const initialPos = { lat: currentLat, lng: currentLng };
      if (s18WorkGoogleMap && s18WorkMarker) {
        google.maps.event.trigger(s18WorkGoogleMap, 'resize');
        s18WorkGoogleMap.setCenter(initialPos);
        s18WorkMarker.setPosition(initialPos);
        return;
      }
      s18WorkGoogleMap = new google.maps.Map(container, {
        center: initialPos,
        zoom: 14,
        mapTypeControl: false,
        streetViewControl: false,
        fullscreenControl: false,
        zoomControl: false,
        gestureHandling: 'greedy',
        scrollwheel: true
      });

      s18WorkMarker = new google.maps.Marker({
        position: initialPos,
        map: s18WorkGoogleMap,
        draggable: true,
        title: "職場・学校ピン"
      });

      google.maps.event.addListener(s18WorkGoogleMap, 'click', (e) => {
        const lat = e.latLng.lat();
        const lng = e.latLng.lng();
        s18WorkMarker.setPosition(e.latLng);
        onWorkLocationSelected(lat, lng);
      });

      google.maps.event.addListener(s18WorkMarker, 'dragend', (e) => {
        const lat = e.latLng.lat();
        const lng = e.latLng.lng();
        onWorkLocationSelected(lat, lng);
      });

      setTimeout(() => {
        if (s18WorkGoogleMap && typeof google !== 'undefined' && google.maps && google.maps.event) {
          google.maps.event.trigger(s18WorkGoogleMap, 'resize');
          s18WorkGoogleMap.setCenter(initialPos);
        }
      }, 100);
      return;
    }

    // Google Maps 未初期化時のフォールバックUI
    container.innerHTML = `
      <div style="width:100%; height:100%; display:flex; flex-direction:column; align-items:center; justify-content:center; background:var(--bg-subtle); padding:1rem; text-align:center;">
        <div style="margin-bottom:0.25rem;"><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="2" width="16" height="20" rx="2" ry="2"/><line x1="9" y1="22" x2="9" y2="18"/><line x1="15" y1="22" x2="15" y2="18"/></svg></div>
        <div style="font-weight:700; font-size:0.875rem; color:var(--primary-900); margin-bottom:0.25rem;">職場・学校エリア（テスト選択）</div>
        <p style="font-size:0.75rem; color:var(--text-sub); margin-bottom:0.5rem;">クリックして勤務・通学地を選択してください</p>
        <div style="display:flex; gap:0.4rem; flex-wrap:wrap; justify-content:center;">
          <button type="button" class="btn-outline btn-sm" style="font-size:0.75rem; background:var(--bg-card);" onclick="MachirikuScreens.setS18WorkCoords(36.5590, 139.8980, '栃木県宇都宮市駅前通り3丁目')">宇都宮駅前</button>
          <button type="button" class="btn-outline btn-sm" style="font-size:0.75rem; background:var(--bg-card);" onclick="MachirikuScreens.setS18WorkCoords(36.5650, 139.9800, '栃木県宇都宮市ゆいの杜4丁目')">清原・ゆいの杜</button>
          <button type="button" class="btn-outline btn-sm" style="font-size:0.75rem; background:var(--bg-card);" onclick="MachirikuScreens.setS18WorkCoords(36.5530, 139.8800, '栃木県宇都宮市西原1丁目')">西原・南部</button>
        </div>
      </div>
    `;
  }

  function handleS18WorkCurrentLocation() {
    if (typeof navigator !== 'undefined' && navigator.geolocation) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('現在地を取得しています...');
      }
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setS18WorkCoords(pos.coords.latitude, pos.coords.longitude);
        },
        () => {
          setS18WorkCoords(36.5590, 139.8980, '栃木県宇都宮市駅前通り3丁目');
          if (typeof MachirikuApp !== 'undefined') {
            MachirikuApp.showToast('位置情報取得不可のため、宇都宮駅前を設定しました');
          }
        },
        { timeout: 7000 }
      );
    } else {
      setS18WorkCoords(36.5590, 139.8980, '栃木県宇都宮市駅前通り3丁目');
    }
  }

  function setS18WorkCoords(lat, lng, manualAddr = null) {
    if (typeof document === 'undefined') return;
    const latInput = document.getElementById('s18-work-lat');
    const lngInput = document.getElementById('s18-work-lng');
    const addrInput = document.getElementById('s18-work-address');
    const statusEl = document.getElementById('s18-work-map-status');

    if (latInput) latInput.value = lat;
    if (lngInput) lngInput.value = lng;

    if (s18WorkGoogleMap && typeof google !== 'undefined' && google.maps) {
      const newPos = new google.maps.LatLng(lat, lng);
      s18WorkGoogleMap.panTo(newPos);
      if (s18WorkMarker) s18WorkMarker.setPosition(newPos);
    }

    if (manualAddr && addrInput) {
      addrInput.value = manualAddr;
      if (statusEl) statusEl.textContent = `✓ 選択中: (${Number(lat).toFixed(4)}, ${Number(lng).toFixed(4)})`;
      return;
    }

    if (statusEl) statusEl.textContent = `✓ 選択中: (${Number(lat).toFixed(4)}, ${Number(lng).toFixed(4)})`;
    reverseGeocodeWithGoogle(lat, lng, (res) => {
      const resolved = res.formattedAddress || res.name || (res.city ? `栃木県${res.city}` : '栃木県宇都宮市');
      if (addrInput) addrInput.value = resolved;
      if (statusEl) statusEl.textContent = `✓ 選択中: (${Number(lat).toFixed(4)}, ${Number(lng).toFixed(4)})`;
    });
  }

  let s18BizMap = null;
  let s18BizOverlay = null;
  let s18BizPolygon = null;
  let s18BizPoints = [];
  let s18BizMode = 'draw';
  let s18BizIsDrawing = false;

  function showBizMapFallback(container) {
    if (!container) return;
    container.innerHTML = `
      <div style="width:100%; height:100%; display:flex; flex-direction:column; align-items:center; justify-content:center; background:var(--bg-subtle); padding:1rem; text-align:center;">
        <div style="margin-bottom:0.25rem;"><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="10" r="3"/><path d="M12 2a8 8 0 0 0-8 8c0 5.25 8 12 8 12s8-6.75 8-12a8 8 0 0 0-8-8z"/></svg></div>
        <div style="font-weight:700; font-size:0.875rem; color:var(--primary-900); margin-bottom:0.25rem;">出店希望エリアマップ</div>
        <p style="font-size:0.75rem; color:var(--text-sub); margin-bottom:0.5rem;">※テスト環境：下の入力欄に希望の出店エリアを直接入力してください</p>
      </div>
    `;
  }

  function initS18BizAreaMap() {
    if (typeof document === 'undefined') return;
    const container = document.getElementById('s18-biz-map-picker');
    const wrap = document.getElementById('s18-biz-map-wrap');
    if (!container || !wrap) return;

    function mountMap() {
      if (!document.body.contains(container)) return;
      s18BizPoints = [];
      s18BizIsDrawing = false;
      s18BizMode = 'draw';

      container.innerHTML = '';

      const centerPos = { lat: 36.5610, lng: 139.8830 };
      s18BizMap = new google.maps.Map(container, {
        center: centerPos,
        zoom: 14,
        mapTypeControl: false,
        streetViewControl: false,
        fullscreenControl: false,
        zoomControl: false,
        gestureHandling: 'none',
        draggable: false,
        scrollwheel: false,
        disableDoubleClickZoom: true
      });

      s18BizOverlay = new google.maps.OverlayView();
      s18BizOverlay.draw = function() {};
      s18BizOverlay.setMap(s18BizMap);

      const pointsInput = document.getElementById('s18-biz-area-points');
      if (pointsInput && pointsInput.value) {
        try {
          const parsed = JSON.parse(pointsInput.value);
          if (Array.isArray(parsed) && parsed.length >= 3) {
            s18BizPoints = parsed.map(pt => new google.maps.LatLng(pt.lat, pt.lng));
            s18BizPolygon = new google.maps.Polygon({
              paths: s18BizPoints,
              strokeColor: 'rgb(45, 106, 79)',
              strokeOpacity: 0.9,
              strokeWeight: 3,
              fillColor: 'rgb(216, 243, 220)',
              fillOpacity: 0.35,
              map: s18BizMap
            });
          }
        } catch (_) {}
      }

      setupBizDrawingListeners(wrap);

      setTimeout(() => {
        if (s18BizMap && typeof google !== 'undefined' && google.maps && google.maps.event) {
          google.maps.event.trigger(s18BizMap, 'resize');
        }
      }, 200);
    }

    if (typeof google !== 'undefined' && google.maps && google.maps.Map) {
      mountMap();
      return;
    }

    if (typeof window !== 'undefined' && typeof setInterval === 'function') {
      let retries = 0;
      const timer = setInterval(() => {
        retries++;
        if (typeof google !== 'undefined' && google.maps && google.maps.Map) {
          clearInterval(timer);
          mountMap();
        }
        if (retries > 60) {
          clearInterval(timer);
          showBizMapFallback(container);
        }
      }, 100);
      return;
    }

    showBizMapFallback(container);
  }

  function setupBizDrawingListeners(wrap) {
    let surface = document.getElementById('s18-biz-draw-surface');
    if (!surface) {
      surface = document.createElement('div');
      surface.id = 's18-biz-draw-surface';
      surface.style.position = 'absolute';
      surface.style.top = '0';
      surface.style.left = '0';
      surface.style.width = '100%';
      surface.style.height = '100%';
      surface.style.zIndex = '5';
      surface.style.cursor = 'crosshair';
      surface.style.touchAction = 'none';
      wrap.appendChild(surface);
    }

    let canvas = document.getElementById('s18-biz-draw-canvas');
    if (!canvas) {
      canvas = document.createElement('canvas');
      canvas.id = 's18-biz-draw-canvas';
      canvas.style.position = 'absolute';
      canvas.style.top = '0';
      canvas.style.left = '0';
      canvas.style.width = '100%';
      canvas.style.height = '100%';
      canvas.style.pointerEvents = 'none';
      canvas.style.zIndex = '6';
      wrap.appendChild(canvas);
    }
    const rect = wrap.getBoundingClientRect();
    canvas.width = rect.width || wrap.clientWidth || 320;
    canvas.height = rect.height || wrap.clientHeight || 270;
    const ctx = canvas.getContext ? canvas.getContext('2d') : null;

    let pixelPoints = [];

    surface.onpointerdown = (e) => {
      if (s18BizMode !== 'draw') return;
      if (surface.setPointerCapture) {
        try { surface.setPointerCapture(e.pointerId); } catch (_) {}
      }
      s18BizIsDrawing = true;
      pixelPoints = [];
      const bRect = wrap.getBoundingClientRect();
      const x = e.clientX - bRect.left;
      const y = e.clientY - bRect.top;
      pixelPoints.push({ x, y });

      if (ctx) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.beginPath();
        ctx.moveTo(x, y);
        ctx.strokeStyle = 'rgb(45, 106, 79)';
        ctx.lineWidth = 3;
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';
      }
    };

    surface.onpointermove = (e) => {
      if (!s18BizIsDrawing || s18BizMode !== 'draw') return;
      const bRect = wrap.getBoundingClientRect();
      const x = e.clientX - bRect.left;
      const y = e.clientY - bRect.top;
      pixelPoints.push({ x, y });

      if (ctx) {
        ctx.lineTo(x, y);
        ctx.stroke();
      }
    };

    const finishDraw = (e) => {
      if (!s18BizIsDrawing || s18BizMode !== 'draw') return;
      s18BizIsDrawing = false;
      if (surface.releasePointerCapture) {
        try { surface.releasePointerCapture(e.pointerId); } catch (_) {}
      }

      if (ctx) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
      }

      if (pixelPoints.length < 3) return;

      if (!s18BizOverlay || !s18BizOverlay.getProjection) return;
      const proj = s18BizOverlay.getProjection();
      if (!proj) return;

      const sampled = [];
      const step = Math.max(1, Math.floor(pixelPoints.length / 60));
      for (let i = 0; i < pixelPoints.length; i += step) {
        sampled.push(pixelPoints[i]);
      }
      if (sampled[sampled.length - 1] !== pixelPoints[pixelPoints.length - 1]) {
        sampled.push(pixelPoints[pixelPoints.length - 1]);
      }

      const latLngs = [];
      let sumLat = 0;
      let sumLng = 0;
      for (let i = 0; i < sampled.length; i++) {
        const pt = proj.fromContainerPixelToLatLng(new google.maps.Point(sampled[i].x, sampled[i].y));
        if (pt) {
          latLngs.push(pt);
          sumLat += pt.lat();
          sumLng += pt.lng();
        }
      }

      if (latLngs.length < 3) return;

      if (s18BizPolygon) {
        s18BizPolygon.setMap(null);
      }

      s18BizPolygon = new google.maps.Polygon({
        paths: latLngs,
        strokeColor: 'rgb(45, 106, 79)',
        strokeOpacity: 0.9,
        strokeWeight: 3,
        fillColor: 'rgb(216, 243, 220)',
        fillOpacity: 0.35,
        map: s18BizMap
      });

      const centerLat = sumLat / latLngs.length;
      const centerLng = sumLng / latLngs.length;

      const rawPoints = latLngs.map(ll => ({ lat: Number(ll.lat().toFixed(6)), lng: Number(ll.lng().toFixed(6)) }));
      const pointsInput = document.getElementById('s18-biz-area-points');
      if (pointsInput) pointsInput.value = JSON.stringify(rawPoints);

      const areaInput = document.getElementById('s18-biz-area');
      const statusEl = document.getElementById('s18-biz-map-status');

      reverseGeocodeWithGoogle(centerLat, centerLng, (res) => {
        const areaName = res.formattedAddress || res.name || (res.city ? `栃木県${res.city}` : '栃木県宇都宮市');
        const cleanName = `${areaName} 周辺 (なぞり範囲指定)`;
        if (areaInput) {
          areaInput.value = cleanName;
          areaInput.classList.add('is-flash-updated');
          setTimeout(() => areaInput.classList.remove('is-flash-updated'), 1000);
        }
        if (statusEl) {
          statusEl.textContent = `✓ なぞり範囲指定完了 (${rawPoints.length}点) : ${cleanName}`;
        }
        if (typeof MachirikuApp !== 'undefined') {
          MachirikuApp.showToast('出店希望エリアの範囲を指定しました');
        }
      });
    };

    surface.onpointerup = finishDraw;
    surface.onpointercancel = finishDraw;
  }

  function setBizMapMode(mode) {
    s18BizMode = mode;
    const btnDraw = document.getElementById('s18-draw-btn-draw');
    const btnPan = document.getElementById('s18-draw-btn-pan');
    const surface = document.getElementById('s18-biz-draw-surface');
    const hint = document.getElementById('s18-biz-draw-hint');

    if (mode === 'draw') {
      if (btnDraw) {
        btnDraw.classList.add('is-active');
        btnDraw.style.background = 'var(--primary-700)';
        btnDraw.style.color = 'var(--bg-card)';
        btnDraw.style.fontWeight = '700';
      }
      if (btnPan) {
        btnPan.classList.remove('is-active');
        btnPan.style.background = 'var(--bg-card)';
        btnPan.style.color = 'var(--text-main)';
        btnPan.style.fontWeight = 'normal';
      }
      if (surface) {
        surface.style.pointerEvents = 'auto';
        surface.style.display = 'block';
      }
      if (hint) {
        hint.textContent = 'なぞるモード: 指やマウスで囲んで指定';
      }
      if (s18BizMap) {
        s18BizMap.setOptions({
          draggable: false,
          gestureHandling: 'none',
          scrollwheel: false
        });
      }
    } else {
      if (btnPan) {
        btnPan.classList.add('is-active');
        btnPan.style.background = 'var(--primary-700)';
        btnPan.style.color = 'var(--bg-card)';
        btnPan.style.fontWeight = '700';
      }
      if (btnDraw) {
        btnDraw.classList.remove('is-active');
        btnDraw.style.background = 'var(--bg-card)';
        btnDraw.style.color = 'var(--text-main)';
        btnDraw.style.fontWeight = 'normal';
      }
      if (surface) {
        surface.style.pointerEvents = 'none';
        surface.style.display = 'none';
      }
      if (hint) {
        hint.textContent = '地図移動モード: ドラッグして移動・拡大';
      }
      if (s18BizMap) {
        s18BizMap.setOptions({
          draggable: true,
          gestureHandling: 'greedy',
          scrollwheel: true
        });
      }
    }
  }

  function clearBizDraw() {
    if (s18BizPolygon) {
      s18BizPolygon.setMap(null);
      s18BizPolygon = null;
    }
    s18BizPoints = [];
    const pointsInput = document.getElementById('s18-biz-area-points');
    if (pointsInput) pointsInput.value = '';
    const areaInput = document.getElementById('s18-biz-area') || document.getElementById('s18-biz-area-name');
    if (areaInput) areaInput.value = '';
    const statusEl = document.getElementById('s18-biz-map-status');
    if (statusEl) statusEl.textContent = '範囲指定がリセットされました。地図をなぞるか主要エリアから選択してください。';
    const canvas = document.getElementById('s18-biz-draw-canvas');
    if (canvas && canvas.getContext) {
      const ctx = canvas.getContext('2d');
      if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
    if (typeof MachirikuApp !== 'undefined') {
      MachirikuApp.showToast('指定範囲をクリアしました');
    }
  }

  function setBizAreaFromSelect(areaName) {
    if (!areaName) return;
    const areaInput = document.getElementById('s18-biz-area');
    if (areaInput) areaInput.value = areaName;
    const statusEl = document.getElementById('s18-biz-map-status');
    if (statusEl) statusEl.textContent = `✓ 選択中: ${areaName}`;

    const areaCoords = {
      '東武宇都宮駅・オリオン通り周辺': { lat: 36.5590, lng: 139.8820, delta: 0.0035 },
      'JR宇都宮駅西口・大通り周辺': { lat: 36.5600, lng: 139.8930, delta: 0.0035 },
      '宇都宮駅東口・LRT沿線エリア': { lat: 36.5580, lng: 139.9040, delta: 0.0040 },
      '競輪場通り・八幡台周辺': { lat: 36.5750, lng: 139.8880, delta: 0.0040 },
      '鶴田・西川田エリア': { lat: 36.5450, lng: 139.8550, delta: 0.0050 },
      '雀宮・インターパーク周辺': { lat: 36.5050, lng: 139.9100, delta: 0.0060 }
    };

    const target = areaCoords[areaName] || { lat: 36.5610, lng: 139.8830, delta: 0.0035 };

    if (s18BizMap && typeof google !== 'undefined' && google.maps) {
      s18BizMap.panTo({ lat: target.lat, lng: target.lng });
      s18BizMap.setZoom(15);

      if (s18BizPolygon) {
        s18BizPolygon.setMap(null);
      }
      const polyPts = [];
      const numPts = 8;
      const d = target.delta;
      for (let i = 0; i < numPts; i++) {
        const angle = (i * 2 * Math.PI) / numPts;
        const pLat = target.lat + Math.sin(angle) * d * 0.7;
        const pLng = target.lng + Math.cos(angle) * d;
        polyPts.push(new google.maps.LatLng(pLat, pLng));
      }
      s18BizPolygon = new google.maps.Polygon({
        paths: polyPts,
        strokeColor: 'rgb(45, 106, 79)',
        strokeOpacity: 0.9,
        strokeWeight: 3,
        fillColor: 'rgb(216, 243, 220)',
        fillOpacity: 0.35,
        map: s18BizMap
      });
      const pointsInput = document.getElementById('s18-biz-area-points');
      if (pointsInput) {
        pointsInput.value = JSON.stringify(polyPts.map(p => ({ lat: Number(p.lat().toFixed(6)), lng: Number(p.lng().toFixed(6)) })));
      }
    }
  }

  function handleS18ProfileSubmit(event, form) {
    if (event && event.preventDefault) event.preventDefault();

    const nameInput = (typeof document !== 'undefined' ? document.getElementById('s18-name') : null) || form?.name;
    const emailInput = (typeof document !== 'undefined' ? document.getElementById('s18-email') : null) || form?.email;
    const ageChecked = (typeof document !== 'undefined' ? document.querySelector('input[name="ageGroup"]:checked') : null);
    const genderChecked = (typeof document !== 'undefined' ? document.querySelector('input[name="gender"]:checked') : null);
    const ageVal = ageChecked ? (ageChecked.value === '未設定' ? '' : ageChecked.value) : (form?.ageGroup?.value || '');
    const genderVal = genderChecked ? (genderChecked.value === '回答しない' ? '' : genderChecked.value) : (form?.gender?.value || '');
    
    // 近くに欲しい店舗・施設ジャンル（複数選択）の取得
    const prefIndInput = (typeof document !== 'undefined' ? document.getElementById('s18-preferred-industries') : null) || form?.preferredIndustries;
    let preferredIndustries = [];
    if (prefIndInput && prefIndInput.value) {
      try {
        preferredIndustries = JSON.parse(prefIndInput.value);
      } catch (_) {
        preferredIndustries = String(prefIndInput.value).split(',').map(s => s.trim()).filter(Boolean);
      }
    } else if (residentSelectedIndustries && residentSelectedIndustries.size > 0) {
      preferredIndustries = Array.from(residentSelectedIndustries);
    }

    const taxonomy = getResidentTaxonomy();
    const prefNames = preferredIndustries.map(idOrName => {
      for (const cat of taxonomy) {
        const found = cat.subcategories.find(s => s.id === idOrName);
        if (found) return found.name;
      }
      return idOrName;
    });

    const industryInput = (typeof document !== 'undefined' ? document.getElementById('s18-industry') : null) || form?.industry;
    const legacyIndustry = industryInput ? industryInput.value : '';
    const addrInput = (typeof document !== 'undefined' ? document.getElementById('s18-living-address') : null) || form?.livingAddress;
    const latInput = (typeof document !== 'undefined' ? document.getElementById('s18-living-lat') : null) || form?.livingLat;
    const lngInput = (typeof document !== 'undefined' ? document.getElementById('s18-living-lng') : null) || form?.livingLng;
    const workInput = (typeof document !== 'undefined' ? document.getElementById('s18-work-address') : null) || form?.workAddress;
    const workLatInput = (typeof document !== 'undefined' ? document.getElementById('s18-work-lat') : null) || form?.workLat;
    const workLngInput = (typeof document !== 'undefined' ? document.getElementById('s18-work-lng') : null) || form?.workLng;
    const emailNotifyInput = (typeof document !== 'undefined' ? document.getElementById('s18-email-notify') : null);
    const emailNotify = emailNotifyInput ? emailNotifyInput.checked : true;

    const name = (nameInput ? nameInput.value : '').trim();
    const livingAddress = (addrInput ? addrInput.value : '').trim();

    if (!name) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('ニックネームを入力してください');
      }
      if (nameInput && nameInput.focus) nameInput.focus();
      return;
    }

    if (!livingAddress) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('居住地を地図上でクリックして指定してください（必須）');
      }
      if (addrInput && addrInput.focus) addrInput.focus();
      return;
    }

    const lat = latInput ? parseFloat(latInput.value) : 36.5610;
    const lng = lngInput ? parseFloat(lngInput.value) : 139.8830;

    const profileData = {
      name: name,
      email: emailInput ? emailInput.value.trim() : '',
      emailNotify: emailNotify,
      ageGroup: ageVal,
      gender: genderVal,
      preferredIndustries: preferredIndustries,
      preferredIndustryNames: prefNames,
      industry: prefNames.length > 0 ? prefNames.join(', ') : legacyIndustry,
      livingArea: livingAddress,
      livingCoords: { lat: isNaN(lat) ? 36.5610 : lat, lng: isNaN(lng) ? 139.8830 : lng },
      workAddress: workInput ? workInput.value.trim() : '',
      workCoords: (workLatInput && workLngInput && !isNaN(parseFloat(workLatInput.value)) && !isNaN(parseFloat(workLngInput.value))) ? { lat: parseFloat(workLatInput.value), lng: parseFloat(workLngInput.value) } : null,
      isOnboarded: true
    };

    if (store && store.updateUserProfile) {
      store.updateUserProfile(profileData);
    }

    try {
      if (typeof window !== 'undefined' && window.sessionStorage) {
        window.sessionStorage.removeItem('machiriku_onboarding');
        if (name) {
          window.sessionStorage.setItem('machiriku_user_display_name', name);
        }
      }
    } catch (_) {}

    const headerNameEl = document.getElementById('header-user-name');
    if (headerNameEl && name) {
      headerNameEl.textContent = name;
    }

    if (typeof MachirikuApp !== 'undefined') {
      MachirikuApp.showToast('基本情報を登録しました！マチリクへようこそ');
      MachirikuApp.navigate('s01');
    }
  }

  function handleS18BusinessProfileSubmit(event, form) {
    if (event && event.preventDefault) event.preventDefault();

    const bizNameInput = (typeof document !== 'undefined' ? document.getElementById('s18-biz-name') : null) || form?.businessName;
    const repNameInput = (typeof document !== 'undefined' ? document.getElementById('s18-rep-name') : null) || form?.representativeName;
    const emailInput = (typeof document !== 'undefined' ? document.getElementById('s18-biz-email') : null) || form?.email;
    const phoneInput = (typeof document !== 'undefined' ? document.getElementById('s18-biz-phone') : null) || form?.phone;
    const areaInput = (typeof document !== 'undefined' ? document.getElementById('s18-biz-area') : null) || form?.preferredArea;
    const empInput = (typeof document !== 'undefined' ? document.getElementById('s18-biz-employees') : null) || form?.employeeCount;
    const mgmtChecked = (typeof document !== 'undefined' ? document.querySelector('input[name="mgmtExperience"]:checked') : null);
    const indChecked = (typeof document !== 'undefined' ? document.querySelector('input[name="industryExperience"]:checked') : null);
    const emailNotifyInput = (typeof document !== 'undefined' ? document.getElementById('s18-biz-email-notify') : null);

    const businessName = (bizNameInput ? bizNameInput.value : '').trim();
    const repName = (repNameInput ? repNameInput.value : '').trim();
    const phone = (phoneInput ? phoneInput.value : '').trim();

    if (!businessName) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('事業者名・屋号を入力してください');
      }
      if (bizNameInput && bizNameInput.focus) bizNameInput.focus();
      return;
    }

    if (!repName) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('代表者名を入力してください');
      }
      if (repNameInput && repNameInput.focus) repNameInput.focus();
      return;
    }

    if (!phone) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('連絡先電話番号を入力してください');
      }
      if (phoneInput && phoneInput.focus) phoneInput.focus();
      return;
    }
    const bizDigitsOnly = phone.replace(/[\s()（）ー−‐-]/g, '');
    if (!/^\d+$/.test(bizDigitsOnly)) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('電話番号は半角数字で入力してください（英字や記号は使用できません）');
      }
      if (phoneInput && phoneInput.focus) phoneInput.focus();
      return;
    }
    if (bizDigitsOnly.length < 9 || bizDigitsOnly.length > 11) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('電話番号の桁数を確認してください（例: 09012345678）');
      }
      if (phoneInput && phoneInput.focus) phoneInput.focus();
      return;
    }

    const industries = [];
    if (typeof document !== 'undefined') {
      document.querySelectorAll('input[name="preferredIndustry"]:checked').forEach(el => {
        industries.push(el.value);
      });
    }

    const qualifications = [];
    if (typeof document !== 'undefined') {
      document.querySelectorAll('input[name="qualification"]:checked').forEach(el => {
        qualifications.push(el.value);
      });
    }

    const profileData = {
      role: 'business',
      name: repName,
      businessName: businessName,
      representativeName: repName,
      email: emailInput ? emailInput.value.trim() : '',
      phone: phone,
      preferredArea: areaInput ? areaInput.value : '',
      preferredAreaPoints: (typeof document !== 'undefined' ? document.getElementById('s18-biz-area-points')?.value : '') || '',
      employeeCount: empInput ? parseInt(empInput.value, 10) || 1 : 1,
      preferredIndustries: industries,
      managementExperience: mgmtChecked ? mgmtChecked.value : 'なし',
      industryExperience: indChecked ? indChecked.value : '未経験',
      qualifications: qualifications,
      emailNotify: emailNotifyInput ? emailNotifyInput.checked : true,
      isOnboarded: true
    };

    if (store && store.updateUserProfile) {
      store.updateUserProfile(profileData);
    }

    try {
      if (typeof window !== 'undefined' && window.sessionStorage) {
        window.sessionStorage.removeItem('machiriku_onboarding');
      }
    } catch (_) {}

    if (typeof MachirikuApp !== 'undefined') {
      MachirikuApp.showToast('出店事業主の初期設定が完了しました！マチリクへようこそ');
      MachirikuApp.navigate('s01');
    }
  }

  function handleS18OwnerProfileSubmit(event, form) {
    if (event && event.preventDefault) event.preventDefault();

    const repNameInput = (typeof document !== 'undefined' ? document.getElementById('s18-owner-name') : null) || form?.representativeName;
    const companyInput = (typeof document !== 'undefined' ? document.getElementById('s18-owner-company') : null) || form?.company;
    const phoneInput = (typeof document !== 'undefined' ? document.getElementById('s18-owner-phone') : null) || form?.phone;
    const emailInput = (typeof document !== 'undefined' ? document.getElementById('s18-owner-email') : null) || form?.email;
    const emailNotifyInput = (typeof document !== 'undefined' ? document.getElementById('s18-owner-email-notify') : null);

    const repName = (repNameInput ? repNameInput.value : '').trim();
    const phone = (phoneInput ? phoneInput.value : '').trim();
    const companyVal = (companyInput ? companyInput.value : '').trim();

    if (!repName) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('オーナー氏名・代表者名を入力してください');
      }
      if (repNameInput && repNameInput.focus) repNameInput.focus();
      return;
    }

    if (!phone) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('連絡先電話番号を入力してください');
      }
      if (phoneInput && phoneInput.focus) phoneInput.focus();
      return;
    }
    const bizDigitsOnly = phone.replace(/[\s()（）ー−‐-]/g, '');
    if (!/^\d+$/.test(bizDigitsOnly)) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('電話番号は半角数字で入力してください（英字や記号は使用できません）');
      }
      if (phoneInput && phoneInput.focus) phoneInput.focus();
      return;
    }
    if (bizDigitsOnly.length < 9 || bizDigitsOnly.length > 11) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('電話番号の桁数を確認してください（例: 09012345678）');
      }
      if (phoneInput && phoneInput.focus) phoneInput.focus();
      return;
    }

    const industries = [];
    if (typeof document !== 'undefined') {
      document.querySelectorAll('#s18-owner-form input[name="preferredIndustry"]:checked').forEach(el => {
        industries.push(el.value);
      });
    }

    if (industries.length === 0) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('誘致希望業種を少なくとも1つ選択してください');
      }
      return;
    }

    const profileData = {
      role: 'owner',
      name: repName,
      representativeName: repName,
      company: companyVal,
      phone: phone,
      email: emailInput ? emailInput.value.trim() : '',
      preferredIndustries: industries,
      emailNotify: emailNotifyInput ? emailNotifyInput.checked : true,
      isOnboarded: true
    };

    if (store && store.updateUserProfile) {
      store.updateUserProfile(profileData);
    }
    if (store && store.setRole) {
      store.setRole('owner');
    }

    try {
      if (typeof window !== 'undefined' && window.sessionStorage) {
        window.sessionStorage.removeItem('machiriku_onboarding');
      }
    } catch (_) {}

    if (typeof MachirikuApp !== 'undefined') {
      MachirikuApp.showToast('物件オーナーの初期設定が完了しました！マチリクへようこそ');
      MachirikuApp.navigate('s11');
    }
  }

  // =========================================================================
  // S19: 通知・オファー条件設定 (全員)
  // =========================================================================
  function renderS19() {
    return `
      <section class="screen-view s19-view" data-screen="S19">
        ${renderHeaderBanner("S19", "通知・オファー受信条件設定", "共通", "新着空き店舗、投票した物件の進捗、クーポンの受信条件を設定します。")}

        <div class="form-stepper-box">
          <h3 style="font-size:1rem; font-weight:800; margin-bottom:1rem;">通知受信チャネル</h3>
          <div style="display:flex; flex-direction:column; gap:0.75rem; margin-bottom:1.5rem;">
            <label style="display:flex; align-items:center; gap:0.5rem; cursor:pointer;">
              <input type="checkbox" checked> メール通知を受信する
            </label>
            <label style="display:flex; align-items:center; gap:0.5rem; cursor:pointer;">
              <input type="checkbox" checked> LINE連携・プッシュ通知を受信する
            </label>
            <label style="display:flex; align-items:center; gap:0.5rem; cursor:pointer;">
              <input type="checkbox"> SMSによる重要なお知らせを受信する
            </label>
          </div>

          <h3 style="font-size:1rem; font-weight:800; margin-bottom:1rem;">希望物件アラート条件</h3>
          <div class="form-group" style="margin-bottom:1rem;">
            <label for="field-52">通知希望エリア</label>
            <select id="field-52" class="form-select">
              <option>オリオン通り・江野町周辺のみ</option>
              <option selected>宇都宮市中心市街地 全域</option>
            </select>
          </div>
          <button type="button" class="btn-primary" style="width:100%;" onclick="MachirikuApp.showToast('通知設定を保存しました')">
            設定を保存する
          </button>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S20: 全33画面一覧・仕様対応表 (設計レビュー専用)
  // =========================================================================
  function renderS20() {
    const screens = data.SCREENS_SPEC;
    const state = store.getState();

    return `
      <section class="screen-view s20-view" data-screen="S20">
        ${renderHeaderBanner("S20", "全33画面一覧・仕様対応マトリクス", "設計レビュー", "企画書・画面設計書・テーブル設計書と完全一致する全33画面の対応一覧です。各ボタンから任意の画面へ直接ジャンプして動作確認できます。")}

        <div class="data-table-wrapper">
          <table class="shadcn-table screen-spec-table">
            <thead>
              <tr>
                <th style="width: 70px;">画面ID</th>
                <th style="width: 200px;">画面名称</th>
                <th style="width: 130px;">対象ロール</th>
                <th>主要仕様・参照設計書</th>
                <th style="width: 140px; text-align: center;">画面へ移動</th>
              </tr>
            </thead>
            <tbody>
              ${screens.map(s => {
                const isCurrent = (typeof window.MachirikuApp !== 'undefined' && window.MachirikuApp.getCurrentScreen) ? window.MachirikuApp.getCurrentScreen() === s.id : false;
                return `
                  <tr class="screen-spec-row ${isCurrent ? 'is-current' : ''}">
                    <td><strong>${s.num}</strong></td>
                    <td><strong>${s.name}</strong></td>
                    <td><span class="badge-status status-voting">${s.roleLabel}</span></td>
                    <td style="font-size:0.875rem; color:var(--text-sub);">
                      ${s.desc}<br>
                      <small style="color:var(--text-muted);">（設計書参照: ${s.refExcel}）</small>
                    </td>
                    <td style="text-align: center;">
                      <button type="button" class="btn-primary" style="font-size:0.875rem; padding:0.3rem 0.65rem;" onclick="MachirikuApp.navigate('${s.id}')">
                        開く 
                      </button>
                    </td>
                  </tr>
                `;
              }).join("")}
            </tbody>
          </table>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S21: 開店支援サービス (出店事業主)
  // =========================================================================
  function renderS21() {
    return `
      <section class="screen-view s21-view" data-screen="S21">
        ${renderHeaderBanner("S21", "開店支援サービス・提携サポート", "出店事業主", "内装施工、厨房機器リース、宇都宮市創業補助金申請など、スムーズな開店をサポートする専門サービス一覧です。")}

        <div style="display:grid; grid-template-columns:repeat(auto-fill, minmax(320px, 1fr)); gap:1.25rem;">
          <div class="detail-card">
            <h3 style="font-size:1.1rem; font-weight:800; margin-bottom:0.5rem;">店舗内装設計・リノベーション</h3>
            <p style="font-size:0.875rem; color:var(--text-sub); margin-bottom:1rem;">
              宇都宮市内の商業店舗実績豊富な設計・工務店ネットワーク。概算見積もり無料。
            </p>
            <button type="button" class="btn-secondary" onclick="MachirikuApp.showToast('内装相談フォームを開きました')">相談を申し込む</button>
          </div>

          <div class="detail-card">
            <h3 style="font-size:1.1rem; font-weight:800; margin-bottom:0.5rem;">厨房機器・什器リース</h3>
            <p style="font-size:0.875rem; color:var(--text-sub); margin-bottom:1rem;">
              初期投資を抑える厨房機器サブスクリプション。製氷機、冷凍冷蔵庫、フライヤー完備。
            </p>
            <button type="button" class="btn-secondary" onclick="MachirikuApp.showToast('厨房カタログを開きました')">カタログを見る</button>
          </div>

          <div class="detail-card">
            <h3 style="font-size:1.1rem; font-weight:800; margin-bottom:0.5rem;">宇都宮市中心市街地創業補助金</h3>
            <p style="font-size:0.875rem; color:var(--text-sub); margin-bottom:1rem;">
              空き店舗活用で最大150万円の改装費補助。申請書類作成を専門税理士がサポート。
            </p>
            <button type="button" class="btn-primary" onclick="MachirikuApp.showToast('補助金の相談を申し込みました')">補助金について相談する</button>
          </div>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S22: 来店アンケート管理 (出店事業主)
  // =========================================================================
  function renderS22() {
    const state = store.getState();
    const surveys = state.surveys || [];
    if (!surveys.length) return renderEmptyState('S22', 'アンケートはまだありません', '店舗・物件を選んで、確認用のアンケートを作成できます。', '#s23', 'アンケートをつくる');

    return `
      <section class="screen-view s22-view" data-screen="S22">
        ${renderHeaderBanner("S22", "来店アンケート管理", "出店事業主", "店舗で回答してもらうアンケートの作成、公開、回答数、集計を管理します。")}

        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:1.5rem;">
          <div style="font-weight:700;">管理アンケート数: <strong>${surveys.length}</strong> 件</div>
          <button type="button" class="btn-primary" onclick="MachirikuApp.navigate('s23')">＋ 新しいアンケートを作成</button>
        </div>

        <div style="display:flex; flex-direction:column; gap:1.25rem;">
          ${surveys.map(s => `
            <div class="detail-card" style="display:flex; justify-content:space-between; align-items:center;">
              <div>
                <div style="display:flex; gap:0.5rem; margin-bottom:0.25rem;">
                  <span class="badge-status status-opened">${s.statusLabel}</span>
                  <span style="font-size:0.875rem; color:var(--text-muted);">作成日: ${s.createdAt}</span>
                </div>
                <h3 style="font-size:1.1rem; font-weight:800;">${s.title}</h3>
                <div style="font-size:0.875rem; color:var(--text-sub); margin-top:0.25rem;">
                  対象店舗: <strong>${s.storeName}</strong> / 設問数: ${s.questions.length}問
                </div>
                <div style="font-size:0.88rem; color:var(--primary-700); font-weight:700; margin-top:0.35rem;">
                  平均満足度: ${s.ratingAvg} / 5.0 （回答累計: ${s.responseCount}件）
                </div>
              </div>
              <div style="display:flex; gap:0.6rem;">
                <button type="button" class="btn-secondary" onclick="MachirikuApp.navigate('s25', { id: '${s.id}' })">QRコード表示</button>
                <button type="button" class="btn-primary" onclick="MachirikuApp.navigate('s27', { id: '${s.id}' })">集計結果を見る</button>
              </div>
            </div>
          `).join("")}
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S23: 来店アンケート作成 (formrun 仕様)
  // =========================================================================
  function renderS23() {
    const state = store.getState();
    const candidateProps = state.properties.filter(p => p.status === "opened" || p.status === "preparing");
    const activeStores = candidateProps.length > 0 ? candidateProps : state.properties;

    return `
      <section class="screen-view s23-view" data-screen="S23">
        ${renderHeaderBanner("S23", "来店アンケート作成", "出店事業主", "お客様の声を集めるアンケートを作成します。総合満足度(固定必須)に加え、独自の質問を追加できます。", [{ label: "アンケート管理", hash: "#s22" }])}

        <div class="form-stepper-box">
          <form onsubmit="event.preventDefault(); MachirikuScreens.handleCreateSurveySubmit(this);">
            <div class="form-group" style="margin-bottom:1rem;">
              <label for="field-53">対象店舗・物件</label>
              <select id="field-53" name="storeSelect" class="form-select">
                ${activeStores.map(p => `<option value="${p.id}">${escapeHtml(p.title)}</option>`).join("")}
              </select>
            </div>
            <div class="form-group" style="margin-bottom:1rem;">
              <label for="field-54">アンケートタイトル *必須</label>
              <input id="field-54" type="text" name="title" class="form-control" required value="【新作】ご来店アンケート">
            </div>
            <div class="form-group" style="margin-bottom:1.25rem;">
              <label for="field-55">説明文・お礼クーポンの案内</label>
              <textarea id="field-55" name="description" class="form-control" rows="2">今後のお店づくりの参考に、ご感想をお聞かせください。</textarea>
            </div>

            <!-- Q1: 固定必須 5段階満足度 -->
            <div style="background-color:var(--bg-subtle);   padding:1rem; margin-bottom:1rem;">
              <span class="badge-status status-voting">Q1 (固定・必須)</span>
              <div style="font-weight:700; margin-top:0.4rem;">本日の店舗の総合満足度をお聞かせください（1〜5）</div>
            </div>

            <!-- Q2: 独自質問 -->
            <div style="background-color:var(--bg-card);   padding:1rem; margin-bottom:1.5rem;">
              <span class="badge-status status-reviewing">Q2 (選択式)</span>
              <div class="form-group" style="margin-top:0.4rem;">
                <label for="field-56">質問文</label>
                <input id="field-56" type="text" name="q2Text" class="form-control" value="本日ご注文いただいたメインメニューは何ですか？">
              </div>
            </div>

            <div style="display:flex; justify-content:flex-end; gap:1rem;">
              <button type="button" class="btn-secondary" onclick="MachirikuApp.navigate('s22')">戻る</button>
              <button type="submit" class="btn-primary" style="padding:0.75rem 2rem;">確認画面へ進む </button>
            </div>
          </form>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S24: アンケート公開前確認 (出店事業主)
  // =========================================================================
  function renderS24(params) {
    const surveyId = params?.id || store.getState().surveys[0]?.id || "srv-001";
    const survey = store.getSurveyById(surveyId) || store.getState().surveys[0];
    if (!survey) return renderEmptyState('S24', "アンケートが見つかりません", "アンケート一覧から、対象のお店を選び直してください。", "#s22", "アンケート一覧へ");

    return `
      <section class="screen-view s24-view" data-screen="S24">
        ${renderHeaderBanner("S24", "アンケート公開前確認・プレビュー", "出店事業主", "お客様が実際にスマホで回答する画面のプレビューを確認し、公開を確定します。", [{ label: "アンケート作成", hash: "#s23" }])}

        <div class="form-stepper-box">
          <div style="border:2px solid var(--primary-600);  padding:1.5rem; margin-bottom:1.5rem; background-color:var(--bg-card);">
            <div style="text-align:center; font-weight:800; font-size:1.15rem; margin-bottom:0.5rem; color:var(--primary-800);">
              スマホ回答プレビュー画面: 『${escapeHtml(survey.title)}』
            </div>
            <div style="font-size:0.875rem; color:var(--text-sub); text-align:center; margin-bottom:1rem;">
              店舗: <strong>${escapeHtml(survey.storeName)}</strong> / ${escapeHtml(survey.description)}
            </div>
            <div style="background-color:var(--bg-subtle); padding:1rem;  font-size:0.88rem; margin-bottom:0.75rem; ">
              <strong>Q1.</strong> 本日の店舗の総合満足度をお聞かせください 
            </div>
            ${survey.questions.filter(q => q.id !== 'q1').map((q, idx) => `
              <div style="background-color:var(--bg-subtle); padding:1rem;  font-size:0.88rem; margin-bottom:0.5rem; ">
                <strong>Q${idx + 2}.</strong> ${escapeHtml(q.text)} (${q.type === 'choice' ? '選択式' : '自由記述'})
              </div>
            `).join("")}
          </div>

          <div style="display:flex; justify-content:flex-end; gap:1rem;">
            <button type="button" class="btn-secondary" onclick="MachirikuApp.navigate('s23')">修正する</button>
            <button type="button" class="btn-primary" style="padding:0.75rem 2rem;" onclick="MachirikuScreens.handlePublishSurveyConfirm('${survey.id}')">
              公開してQRコードを発行する 
            </button>
          </div>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S25: 店内掲示用QRコード (卓上POP A4/スタンド印刷用レイアウト)
  // =========================================================================
  function renderS25(params) {
    const surveyId = params?.id || store.getState().surveys[0]?.id || "srv-001";
    const state = store.getState();
    const survey = state.surveys.find(s => s.id === surveyId) || state.surveys[0];
    if (!survey) return renderEmptyState('S25', "アンケートが見つかりません", "アンケート一覧から、対象のお店を選び直してください。", "#s22", "アンケート一覧へ");

    return `
      <section class="screen-view s25-view" data-screen="S25">
        ${renderHeaderBanner("S25", "店内掲示用 QRコード卓上POP", "出店事業主", "店内のテーブルやレジ横に設置できる印刷用POPです。スマホで読み取るとアンケート画面が開きます。", [{ label: "アンケート管理", hash: "#s22" }])}

        <div style="text-align:center; margin-bottom:1.5rem;" class="btn-print-hide">
          <button type="button" class="btn-primary" onclick="window.print()">この卓上POPを印刷する (Ctrl+P)</button>
          <button type="button" class="btn-secondary" style="margin-left:0.5rem;" onclick="MachirikuApp.navigate('s26', { id: '${survey.id}' })">
            住民として回答画面を開く 
          </button>
        </div>

        <!-- 卓上POP A4 / スタンド印刷用レイアウト (印刷CSS対応) -->
        <div class="tabletop-pop-preview" id="tabletop-pop-preview">
          <div class="pop-store-title">${escapeHtml(survey.storeName)}</div>
          <div class="pop-catchcopy">ご来店の感想をお聞かせください</div>
          
          <div class="pop-qr-wrapper">
            <div id="survey-qr-container"></div>
          </div>

          <div style="font-size:0.875rem; color:var(--text-sub); margin-bottom:1rem;">
            スマホの標準カメラで上記QRコードを読み取ってください
          </div>

          <div class="pop-incentive-box">
            ${state.coupons.find(c => c.id === survey.rewardCouponId)?.discountValue ? `回答のお礼: ${escapeHtml(state.coupons.find(c => c.id === survey.rewardCouponId).discountValue)}` : 'ご協力ありがとうございます。'}
          </div>

          <div class="pop-fold-line"></div>
          <div style="font-size:0.875rem; color:var(--text-muted);">
            マチリク店舗支援システム / 宇都宮中心市街地活性化
          </div>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S26: 来店後アンケート回答 (地域住民)
  // =========================================================================
  function renderS26(params) {
    const surveyId = params?.id || "srv-001";
    const state = store.getState();
    const survey = state.surveys.find(s => s.id === surveyId) || state.surveys[0];
    if (!survey) return renderEmptyState('S26', "アンケートが見つかりません", "アンケート一覧から、対象のお店を選び直してください。", "#s22", "アンケート一覧へ");
    const isAnswered = state.currentUser.answeredSurveys?.includes(survey.id);

    return `
      <section class="screen-view s26-view" data-screen="S26">
        ${renderHeaderBanner("S26", "来店アンケート回答", "地域住民", "ご来店いただきありがとうございます。率直なご感想をお聞かせください。回答完了でクーポンを獲得できます。")}

        <div class="form-stepper-box" style="max-width:600px;">
          <div style="text-align:center; margin-bottom:1.5rem;">
            <h2 style="font-size:1.25rem; font-weight:800; color:var(--primary-800);">${escapeHtml(survey.storeName)}</h2>
            <p style="font-size:0.875rem; color:var(--text-sub); margin-top:0.35rem;">${escapeHtml(survey.description)}</p>
          </div>

          ${isAnswered ? `
            <div style="background-color:var(--primary-50);  padding:1.5rem;  text-align:center; margin-bottom:1.5rem;">
              
              <h3 style="font-size:1.1rem; font-weight:800; color:var(--primary-700); margin-bottom:0.4rem;">ご回答ありがとうございました。</h3>
              <p style="font-size:0.875rem; color:var(--primary-700); margin-bottom:1rem;">
                ${survey.rewardCouponId && state.coupons.some(c => c.id === survey.rewardCouponId) ? 'お礼のクーポンを、受け取ったクーポンから確認できます。' : 'ご感想はお店の運営に役立てます。'}
              </p>
              ${state.coupons.some(c => c.id === survey.rewardCouponId) ? '<a class="btn-primary" href="#s30">クーポンを確認する</a>' : '<a class="btn-primary" href="#s01">物件一覧へ</a>'}
            </div>
          ` : `
            <form onsubmit="event.preventDefault(); MachirikuScreens.handleSurveyResponseSubmit('${survey.id}', this);">
              <!-- Q1: 総合満足度 (スターレーティング) -->
              <div style="background-color:var(--bg-card);   padding:1.25rem; margin-bottom:1.25rem;">
                <label style="font-size:0.95rem; font-weight:800; color:var(--text-main); display:block; margin-bottom:0.5rem;">
                  Q1. 本日の総合満足度をお聞かせください *必須
                </label>
                <div style="display:flex; justify-content:center; gap:0.5rem; font-size:2rem; cursor:pointer;" id="star-rating-box">
                  <span onclick="MachirikuScreens.setRating(1)"></span>
                  <span onclick="MachirikuScreens.setRating(2)"></span>
                  <span onclick="MachirikuScreens.setRating(3)"></span>
                  <span onclick="MachirikuScreens.setRating(4)"></span>
                  <span onclick="MachirikuScreens.setRating(5)" style="color:var(--warning-text);"></span>
                </div>
                <input type="hidden" name="rating" id="survey-rating-val" value="5">
              </div>

              <!-- Q2: メニュー選択 -->
              <div style="background-color:var(--bg-card);   padding:1.25rem; margin-bottom:1.25rem;">
                <label style="font-size:0.95rem; font-weight:800; color:var(--text-main); display:block; margin-bottom:0.5rem;" for="field-57">
                  Q2. 本日ご注文いただいたメニュー
                </label>
                <select id="field-57" name="q2" class="form-select">
                  <option value="宇都宮ネギとマルゲリータピッツァ">宇都宮ネギとマルゲリータピッツァ</option>
                  <option value="4種チーズと栃乙女蜂蜜のクアトロ">4種チーズと栃乙女蜂蜜のクアトロ</option>
                  <option value="日光クラフト生ビール">日光クラフト生ビール</option>
                  <option value="ランチセット">ランチセット</option>
                </select>
              </div>

              <!-- Q3: 自由コメント -->
              <div style="background-color:var(--bg-card);   padding:1.25rem; margin-bottom:1.5rem;">
                <label style="font-size:0.95rem; font-weight:800; color:var(--text-main); display:block; margin-bottom:0.5rem;" for="field-58">
                  Q3. ご感想・スタッフへのメッセージ（任意）
                </label>
                <textarea id="field-58" name="comment" class="form-control" rows="3" placeholder="ピッツァの生地がもちもちでとても美味しかったです。"></textarea>
              </div>

              <button type="submit" class="btn-primary" style="width:100%; padding:0.85rem; font-size:1.05rem;">
                回答を送信する
              </button>
            </form>
          `}
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S27: 来店アンケート集計 (shadcn/ui Blocks 仕様)
  // =========================================================================
  function renderS27(params) {
    const surveyId = params?.id || "srv-001";
    const state = store.getState();
    const survey = state.surveys.find(s => s.id === surveyId) || state.surveys[0];
    if (!survey) return renderEmptyState('S27', "アンケートが見つかりません", "アンケート一覧から、対象のお店を選び直してください。", "#s22", "アンケート一覧へ");
    const responses = survey.responses || [];
    const coupon = state.coupons.find(c => c.id === survey.rewardCouponId);
    const issued = coupon?.issuedCount || 0;
    const used = coupon?.usedCount || 0;
    const usageRate = issued > 0 ? `${Math.round(used / issued * 1000) / 10}%` : '—';

    return `
      <section class="screen-view s27-view" data-screen="S27">
        ${renderHeaderBanner("S27", "来店アンケート集計ダッシュボード", "出店事業主", "お客様からの回答結果・満足度評価・生の声のクロス集計ダッシュボードです。", [{ label: "アンケート管理", hash: "#s22" }])}

        <div class="shadcn-dashboard">
          <div class="metrics-kpi-grid metrics-three">
            <div class="metric-card">
              <div class="metric-card-top"><span>回答総数</span><span></span></div>
              <div class="metric-card-val">${survey.responseCount} <span style="font-size:1rem;">件</span></div>
              <div class="metric-card-footer">${escapeHtml(survey.storeName)}</div>
            </div>
            <div class="metric-card">
              <div class="metric-card-top"><span>平均満足度</span><span></span></div>
              <div class="metric-card-val">${survey.responseCount > 0 ? survey.ratingAvg : '—'} <small style="font-size:1rem; color:var(--text-muted);">/ 5.0</small></div>
              <div class="metric-card-footer">${survey.responseCount > 0 ? '回答者による5段階評価' : '回答が集まると表示されます'}</div>
            </div>
            <div class="metric-card">
              <div class="metric-card-top"><span>クーポン利用率</span><span></span></div>
              <div class="metric-card-val">${usageRate}</div>
              <div class="metric-card-footer">${used}件利用 / ${issued}件配布</div>
            </div>
          </div>

          <!-- 回答者からの生の声・フィードバック一覧 -->
          <div class="detail-card">
            <h3 class="detail-card-title">最新の来店者フィードバック一覧</h3>
            <div style="display:flex; flex-direction:column; gap:0.75rem;">
              ${responses.length ? responses.map(r => `
                <div class="quote-bubble">
                  <div style="display:flex; justify-content:space-between; margin-bottom:0.35rem;">
                    <span class="response-score">満足度 ${r.rating} / 5</span>
                    <span style="font-size:0.875rem; color:var(--text-muted);">${r.age} ${r.gender} - ${r.date}</span>
                  </div>
                  <div style="font-size:0.88rem; color:var(--text-main); font-weight:600; margin-bottom:0.25rem;">
                    注文: ${r.q2}
                  </div>
                  ${r.comment ? `<p style="font-size:0.875rem; color:var(--text-sub);">“${r.comment}”</p>` : ''}
                </div>
              `).join("") : '<p class="sample-note">まだ回答がありません。店内にQRコードを掲示して、感想を集めましょう。</p>'}
            </div>
          </div>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S28: クーポン作成・配布管理 (STORES 仕様)
  // =========================================================================
  function renderS28() {
    const state = store.getState();
    const coupons = state.coupons || [];
    if (!coupons.length) return renderEmptyState('S28', 'クーポンはまだありません', '対象物件と特典の内容を選んで、確認用のクーポンを作成できます。', '#s29', 'クーポンをつくる');

    return `
      <section class="screen-view s28-view" data-screen="S28">
        ${renderHeaderBanner("S28", "クーポン作成・配布管理", "出店事業主", "STORESスタイルのクーポン管理です。割引種別、対象者、有効期限、発行上限数を設定・追跡します。")}

        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:1.5rem;">
          <div style="font-weight:700;">発行中クーポン: <strong>${coupons.length}</strong> 件</div>
          <button type="button" class="btn-primary" onclick="MachirikuApp.navigate('s29')">＋ 新規クーポンを作成する</button>
        </div>

        <div class="data-table-wrapper">
          <table class="shadcn-table">
            <thead>
              <tr>
                <th>クーポン名</th>
                <th>割引内容</th>
                <th>配布対象</th>
                <th>発行数 / 上限</th>
                <th>利用数</th>
                <th>有効期限</th>
                <th>状態</th>
              </tr>
            </thead>
            <tbody>
              ${coupons.map(c => `
                <tr>
                  <td><strong>${c.title}</strong><br><small style="color:var(--text-muted);">${c.storeName}</small></td>
                  <td><span class="badge-status status-voting">${c.discountValue}</span></td>
                  <td style="font-size:0.875rem;">${c.targetLabel}</td>
                  <td><strong>${c.issuedCount}</strong> / ${c.maxLimit} 枚</td>
                  <td><strong style="color:var(--accent-price);">${c.usedCount}</strong> 枚</td>
                  <td style="font-size:0.875rem; color:var(--text-muted);">〜 ${c.expireDate}</td>
                  <td><span class="badge-status status-opened">配布中</span></td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S29: クーポン設定 (STORES 仕様)
  // =========================================================================
  function renderS29() {
    const properties = store.getProperties();
    return `
      <section class="screen-view s29-view" data-screen="S29">
        ${renderHeaderBanner("S29", "新規クーポン条件設定", "出店事業主", "クーポンの割引種別、対象顧客、上限数を設定します。", [{ label: "クーポン管理", hash: "#s28" }])}

        <div class="form-stepper-box">
          <form onsubmit="event.preventDefault(); MachirikuScreens.handleCreateCouponSubmit(this);">
            <div class="form-group" style="margin-bottom:1rem;">
              <label for="coupon-property">対象物件</label>
              <select id="coupon-property" name="propId" class="form-select" required>
                ${properties.map(p => `<option value="${p.id}">${escapeHtml(p.title)}</option>`).join('')}
              </select>
            </div>
            <div class="form-group" style="margin-bottom:1rem;">
              <label for="field-59">クーポンタイトル *必須</label>
              <input id="field-59" type="text" name="title" class="form-control" required placeholder="例: 【オープン感謝】全品10% OFFクーポン">
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:1rem; margin-bottom:1rem;">
              <div class="form-group">
                <label for="field-60">クーポン種別 *必須</label>
                <select id="field-60" name="type" class="form-select">
                  <option value="percent">割引率（% OFF）</option>
                  <option value="amount">割引額（円引き）</option>
                  <option value="service">無料サービス</option>
                </select>
              </div>
              <div class="form-group">
                <label for="field-61">割引・サービス内容 *必須</label>
                <input id="field-61" type="text" name="discountValue" class="form-control" required placeholder="例: 10% OFF または ドリンク1杯無料">
              </div>
            </div>

            <div class="form-group" style="margin-bottom:1rem;">
              <label for="field-62">配布対象条件 *必須</label>
              <select id="field-62" name="targetType" class="form-select">
                <option value="all_voters">この空き店舗に投票した住民全員</option>
                <option value="survey_respondent">来店後アンケートに回答したお客様</option>
                <option value="all_citizens">マチリク登録住民全員</option>
              </select>
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:1rem; margin-bottom:1rem;">
              <div class="form-group">
                <label for="field-63">発行上限枚数</label>
                <input id="field-63" type="number" name="maxLimit" class="form-control" value="100">
              </div>
              <div class="form-group">
                <label for="field-64">有効期限</label>
                <input id="field-64" type="date" name="expireDate" class="form-control" value="2026-12-31">
              </div>
            </div>

            <div class="form-group" style="margin-bottom:1.5rem;">
              <label for="field-65">利用条件</label>
              <input id="field-65" type="text" name="condition" class="form-control" value="お会計1,500円以上でご利用可能">
            </div>

            <div style="display:flex; justify-content:flex-end; gap:1rem;">
              <button type="button" class="btn-secondary" onclick="MachirikuApp.navigate('s28')">戻る</button>
              <button type="submit" class="btn-primary" style="padding:0.75rem 2rem;">クーポンを発行・配布開始する</button>
            </div>
          </form>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S30: 受け取ったクーポン一覧 (まちのコイン × STORES 仕様)
  // =========================================================================
  function renderS30() {
    const state = store.getState();
    const myCoupons = state.coupons.filter(c => state.currentUser.receivedCoupons?.includes(c.id)) || [];

    return `
      <section class="screen-view s30-view" data-screen="S30">
        ${renderHeaderBanner("S30", "受け取ったクーポン・ウォレット", "地域住民", "投票やお礼アンケートで獲得したクーポン一覧です。店頭でお会計時に提示して利用できます。")}

        ${myCoupons.length === 0 ? `
          <div class="detail-card" style="text-align:center; padding:3rem 1.5rem;">
            
            <h3 style="font-size:1.15rem; font-weight:800; margin-bottom:0.5rem;">受け取ったクーポンはありません</h3>
            <p style="font-size:0.88rem; color:var(--text-muted); margin-bottom:1.5rem;">
              気になる空き店舗に投票するか、店舗の来店アンケートに答えると限定クーポンが届きます。
            </p>
            <a href="#s01" class="btn-primary">空き店舗に投票しに行く</a>
          </div>
        ` : `
          <div class="coupons-grid">
            ${myCoupons.map(c => `
              <div class="coupon-ticket-card">
                <div class="coupon-ticket-left">
                  <div style="font-size:0.875rem; font-weight:700;">マチリク限定</div>
                  <div class="coupon-discount-val">${c.discountValue}</div>
                  <div style="font-size:0.875rem;">${c.typeLabel}</div>
                </div>
                <div class="coupon-ticket-notch"></div>
                <div class="coupon-ticket-right">
                  <div>
                    <h4 class="coupon-title"><a href="#s31?id=${c.id}">${c.title}</a></h4>
                    <div class="coupon-store-name">${c.storeName}</div>
                    <div style="font-size:0.875rem; color:var(--text-muted);">条件: ${c.condition}</div>
                  </div>
                  <div style="display:flex; justify-content:space-between; align-items:center; margin-top:0.75rem;">
                    <span style="font-size:0.875rem; color:var(--text-muted);">期限: 〜${c.expireDate}</span>
                    <a class="btn-secondary" href="#s31?id=${c.id}">${c.isUsedByCurrentUser ? '利用済みの内容' : '店頭で使う'}</a>
                  </div>
                </div>
              </div>
            `).join("")}
          </div>
        `}
      </section>
    `;
  }

  // =========================================================================
  // S31: クーポン提示・利用 (STORES 仕様)
  // =========================================================================
  function renderS31(params) {
    const couponId = params?.id || "cpn-001";
    const state = store.getState();
    const coupon = state.coupons.find(c => c.id === couponId) || state.coupons[0];
    if (!coupon) return renderEmptyState('S31', "クーポンが見つかりません", "受け取ったクーポンの一覧を確認してください。", "#s30", "クーポン一覧へ");
    const isUsed = coupon.isUsedByCurrentUser;

    return `
      <section class="screen-view s31-view" data-screen="S31">
        ${renderHeaderBanner("S31", "クーポン提示・店頭消込", "地域住民", "お会計時に店員様にお見せください。消込確定ボタンでご利用済みになります。", [{ label: "クーポン一覧", hash: "#s30" }])}

        <div class="redemption-panel">
          <div class="used-stamp ${isUsed ? 'is-stamped' : ''}" id="used-stamp-mark">利用済</div>

          <div style="font-size:0.875rem; color:var(--text-muted); margin-bottom:0.25rem;">${coupon.storeName}</div>
          <h2 style="font-size:1.35rem; font-weight:900; color:var(--primary-800); margin-bottom:0.75rem;">${coupon.title}</h2>
          
          <div style="background-color:var(--bg-subtle); padding:1rem;  margin-bottom:1.5rem;">
            <div style="font-size:2rem; font-weight:900; color:var(--accent-price);">${coupon.discountValue}</div>
            <div style="font-size:0.875rem; color:var(--text-sub); margin-top:0.25rem;">条件: ${coupon.condition}</div>
          </div>

          <!-- クーポンバーコード表示 -->
          <div style="margin-bottom:1.5rem;">
            <div style="background-color:var(--bg-card); border:1px solid var(--border-strong); padding:0.75rem; display:inline-block; border-radius:var(--radius-sm);">
              <div style="font-size:0.875rem; color:var(--text-sub);">クーポン番号</div>
              <div style="font-size:0.875rem; color:var(--text-muted); font-family:monospace; margin-top:0.25rem;">${coupon.code}</div>
            </div>
          </div>

          ${isUsed ? `
            <div style="color:var(--accent-price); font-weight:800; font-size:1rem; margin-top:1rem;">
              このクーポンはご利用済みです（${coupon.redeemedAt || '利用完了'}）
            </div>
          ` : `
            <div style="margin-top:1rem;">
              <p style="font-size:0.875rem; color:var(--text-muted); margin-bottom:0.75rem;">
                注意: 必ずお会計時に店員様の前で押してください
              </p>
              <button type="button" class="btn-primary" style="width:100%; padding:0.85rem; font-size:1rem; background-color:var(--accent-price);" onclick="MachirikuScreens.handleRedeemCoupon('${coupon.id}')">
                店員様が確認して利用を確定する
              </button>
            </div>
          `}
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S32: 事業主の実績・顧客評価 (さかさま不動産 仕様)
  // =========================================================================
  function renderS32() {
    const state = store.getState();
    const biz = state.users.business;
    const reviews = businessReviews(biz);

    return `
      <section class="screen-view s32-view" data-screen="S32">
        ${renderHeaderBanner("S32", "事業主の実績・顧客評価プロフィール", "オーナー・事業主", "さかさま不動産スタイルの透明性プロフィールです。過去の店舗経験、保有資格、来店客からの評価を公開します。")}

        <div style="display:grid; grid-template-columns:1fr 2fr; gap:1.75rem;">
          <div class="detail-card" style="text-align:center;">
            
            <h2 style="font-size:1.25rem; font-weight:800;">${biz.name}</h2>
            <div style="font-size:0.875rem; color:var(--text-muted); margin-bottom:1rem;">${biz.company}</div>
            
            <div style="background-color:var(--primary-50);  padding:1rem;  margin-bottom:1rem;">
              <div style="font-size:0.875rem; color:var(--primary-700); font-weight:700;">顧客満足度レーティング</div>
              <div style="font-size:2rem; font-weight:900; color:var(--primary-700); line-height:1.2;">${reviews.average}</div>
              <div style="font-size:0.875rem; color:var(--primary-700);">来店アンケート ${reviews.count}件</div>
            </div>

            <div style="font-size:0.875rem; color:var(--text-sub); text-align:left;">
              <div><strong>実務経験:</strong> ${biz.experienceYears}</div>
            </div>
          </div>

          <div>
            <div class="story-card-box">
              <h3 style="font-size:1.1rem; font-weight:800; margin-bottom:0.75rem;">■ 出店者の想い・ビジョン</h3>
              <div class="story-content-body">
                ${biz.story}
              </div>
            </div>

            <div class="detail-card">
              <h3 style="font-size:1.1rem; font-weight:800; margin-bottom:0.75rem;">■ 運営実績店舗</h3>
              <div>
                ${state.properties.filter(p => p.storePlan && state.applications.some(a => a.propId === p.id && a.applicantId === biz.id && a.status === 'contracted')).map(p => `<p><a class="secondary-action" href="#s05?id=${p.id}">${escapeHtml(p.storePlan.storeName)} / ${escapeHtml(p.title)}</a></p>`).join('') || '<p class="sample-note">公開された出店計画はまだありません。</p>'}
              </div>
            </div>
          </div>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // S33: 物件PR広告 (物件オーナー)
  // =========================================================================
  function renderS33(params) {
    const propId = params?.id || store.getProperties()[0]?.id;
    const prop = store.getPropertyById(propId);
    if (!prop) return renderEmptyState('S33', "物件が見つかりません", "掲載が終了したか、まだ物件が登録されていません。", "#s01", "物件一覧へ");

    return `
      <section class="screen-view s33-view" data-screen="S33">
        ${renderHeaderBanner("S33", "物件PR注目枠 広告掲載申込", "物件オーナー", "検索結果の最上部PR注目枠に物件を優先表示する広告オプションです。出店希望者の目に留まりやすくなります。", [{ label: "物件管理", hash: "#s11" }])}

        <div class="form-stepper-box">
          <div style="margin-bottom:1.5rem;">
            <h3 style="font-size:1.1rem; font-weight:800; margin-bottom:0.35rem;">対象物件: ${escapeHtml(prop.title)}</h3>
            <div style="font-size:0.875rem; color:var(--text-muted);">現在PR掲載状態: ${prop.isPr ? ' 掲載中' : '未掲載'}</div>
          </div>

          <div style="display:grid; grid-template-columns:1fr 1fr; gap:1rem; margin-bottom:1.5rem;">
            <div style="border:2px solid var(--primary-600);  padding:1.25rem; background-color:var(--primary-50);">
              <div style="font-weight:800; font-size:1.05rem; color:var(--primary-800);">スタンダードPRプラン</div>
              <div style="font-size:1.5rem; font-weight:900; color:var(--accent-price); margin:0.4rem 0;">10,000円 <small style="font-size:0.875rem; color:var(--text-sub);">/月</small></div>
              <ul style="font-size:0.875rem; color:var(--text-sub); margin-left:1rem; line-height:1.6;">
                <li>検索結果トップに「PR」バッジ付きで固定表示</li>
                <li>地図上のピンにゴールドPRリボンを表示</li>
                <li>新着出店希望者への推薦メールに掲載</li>
              </ul>
            </div>

            <div style="  padding:1.25rem;">
              <div style="font-weight:800; font-size:1.05rem;">スポット2週間集中プラン</div>
              <div style="font-size:1.5rem; font-weight:900; color:var(--accent-price); margin:0.4rem 0;">6,000円 <small style="font-size:0.875rem; color:var(--text-sub);">/2週間</small></div>
              <ul style="font-size:0.875rem; color:var(--text-sub); margin-left:1rem; line-height:1.6;">
                <li>退去直後の空き期間を最短にする短期集中掲載</li>
                <li>需要スコア上位の注目枠として露出</li>
              </ul>
            </div>
          </div>

          <button type="button" class="btn-primary" style="width:100%; padding:0.85rem;" onclick="MachirikuScreens.handleApplyPr('${prop.id}')">
             スタンダードPRプランで掲載を申し込む
          </button>
        </div>
      </section>
    `;
  }

  // =========================================================================
  // イベントハンドラー実装群
  // =========================================================================

  function handleS01Search(form) {
    const fd = new FormData(form);
    store.setFilters({
      keyword: fd.get("keyword") || "",
      category: store.getState().filters.category
    });
  }

  function resetSearch() {
    store.setFilters({
      keyword: '',
      pref: 'all',
      cities: [],
      category: 'all',
      categories: [],
      rentMin: 0,
      rentMax: 500000,
      includeMgmtFee: false,
      noDeposit: false,
      sort: 'score_desc'
    }, { resetForm: true });
    areaModalState.isOpen = false;
    areaModalState.pendingCities = [];
    rentPopoverOpen = false;
    rentPopoverState.pendingMin = 0;
    rentPopoverState.pendingMax = 500000;
    rentPopoverState.includeMgmtFee = false;
    rentPopoverState.noDeposit = false;
    catPopoverOpen = false;
    catPopoverState.pendingCategories = [];
  }

  // エリアモーダル操作
  function openAreaModal() {
    const state = store.getState();
    const pref = (state.filters.pref && state.filters.pref !== 'all') ? state.filters.pref : '栃木';
    areaModalState.selectedPref = pref;
    const currentCities = (state.filters.cities && state.filters.cities.length > 0)
      ? state.filters.cities
      : (pref === '栃木' ? ['宇都宮市'] : []);
    areaModalState.pendingCities = [...currentCities];
    areaModalState.step = (state.filters.pref === 'all' && (!state.filters.cities || state.filters.cities.length === 0)) ? 'pref' : 'city';
    areaModalState.isOpen = true;
    rentPopoverOpen = false;
    catPopoverOpen = false;
    store.notify('ui');
  }

  function closeAreaModal() {
    areaModalState.isOpen = false;
    store.notify('ui');
  }

  function setAreaModalStep(step, pref) {
    areaModalState.step = step;
    if (pref) {
      areaModalState.selectedPref = pref;
      areaModalState.pendingCities = [];
    }
    store.notify('ui');
  }

  function handleAreaCityCheck(city, isChecked) {
    if (isChecked) {
      if (!areaModalState.pendingCities.includes(city)) {
        areaModalState.pendingCities.push(city);
      }
    } else {
      areaModalState.pendingCities = areaModalState.pendingCities.filter(c => c !== city);
    }
    store.notify('ui');
  }

  function handleAreaCitySoloSelect(city) {
    areaModalState.pendingCities = [city];
    areaModalState.isOpen = false;
    store.setFilters({
      pref: areaModalState.selectedPref,
      cities: [city]
    });
    if (typeof MachirikuApp !== 'undefined') MachirikuApp.showToast(city + " を適用しました");
  }

  function clearAreaSelection() {
    areaModalState.pendingCities = [];
    store.setFilters({
      pref: 'all',
      cities: []
    });
    store.notify('ui');
  }

  function applyAreaSelection() {
    areaModalState.isOpen = false;
    store.setFilters({
      pref: areaModalState.selectedPref,
      cities: [...areaModalState.pendingCities]
    });
    if (typeof MachirikuApp !== 'undefined') MachirikuApp.showToast("エリア条件を適用しました");
  }

  function filterByPref(prefName) {
    store.setFilters({ pref: prefName });
  }

  function scrollToMap() {
    const mapPane = document.getElementById('portal-map-pane');
    if (mapPane) {
      mapPane.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }

  const MUNICIPALITY_CENTERS = [
    // 栃木県内全14市
    { pref: '栃木', city: '日光市', lat: 36.7558, lng: 139.5990 },
    { pref: '栃木', city: '宇都宮市', lat: 36.5590, lng: 139.8830 },
    { pref: '栃木', city: '小山市', lat: 36.3150, lng: 139.8000 },
    { pref: '栃木', city: '栃木市', lat: 36.3820, lng: 139.7340 },
    { pref: '栃木', city: '足利市', lat: 36.3330, lng: 139.4520 },
    { pref: '栃木', city: '佐野市', lat: 36.3150, lng: 139.5790 },
    { pref: '栃木', city: '鹿沼市', lat: 36.5670, lng: 139.7450 },
    { pref: '栃木', city: '那須塩原市', lat: 36.9620, lng: 139.9830 },
    { pref: '栃木', city: '大田原市', lat: 36.8710, lng: 140.0150 },
    { pref: '栃木', city: 'さくら市', lat: 36.6850, lng: 139.9650 },
    { pref: '栃木', city: '那須烏山市', lat: 36.6560, lng: 140.1500 },
    { pref: '栃木', city: '下野市', lat: 36.3860, lng: 139.8530 },
    { pref: '栃木', city: '真岡市', lat: 36.4390, lng: 140.0130 },
    { pref: '栃木', city: '矢板市', lat: 36.8060, lng: 139.9240 },
    // 東京都
    { pref: '東京', city: '千代田区', lat: 35.6812, lng: 139.7671 },
    { pref: '東京', city: '新宿区', lat: 35.6938, lng: 139.7034 },
    { pref: '東京', city: '渋谷区', lat: 35.6620, lng: 139.7038 },
    { pref: '東京', city: '中央区', lat: 35.6707, lng: 139.7719 },
    { pref: '東京', city: '港区', lat: 35.6586, lng: 139.7514 },
    // 埼玉・群馬・茨城
    { pref: '埼玉', city: 'さいたま市', lat: 35.8617, lng: 139.6455 },
    { pref: '群馬', city: '前橋市', lat: 36.3912, lng: 139.0608 },
    { pref: '群馬', city: '高崎市', lat: 36.3225, lng: 139.0127 },
    { pref: '茨城', city: '水戸市', lat: 36.3659, lng: 140.4712 },
    { pref: '茨城', city: 'つくば市', lat: 36.0835, lng: 140.0764 }
  ];

  function resolveLocationFromCoords(lat, lng) {
    let nearest = null;
    let minDist = Infinity;
    for (const item of MUNICIPALITY_CENTERS) {
      const d = Math.hypot(item.lat - lat, item.lng - lng);
      if (d < minDist) {
        minDist = d;
        nearest = item;
      }
    }
    const approxKm = minDist * 111;
    if (nearest && approxKm <= 50) {
      const prefSuffix = (nearest.pref === '東京' ? '都' : (nearest.pref === '大阪' || nearest.pref === '京都' ? '府' : (nearest.pref === '北海道' ? '' : '県')));
      return {
        pref: nearest.pref,
        city: nearest.city,
        name: `${nearest.pref}${prefSuffix}${nearest.city}`
      };
    }
    return { pref: 'all', city: null, name: '現在地周辺' };
  }

  // Google Maps 逆ジオコーディング（Geocoder）を実行
  function reverseGeocodeWithGoogle(lat, lng, callback) {
    if (typeof google !== 'undefined' && google.maps && google.maps.Geocoder) {
      const geocoder = new google.maps.Geocoder();
      geocoder.geocode({ location: { lat, lng } }, (results, status) => {
        if (status === 'OK' && results && results[0]) {
          let pref = '';
          let city = '';
          for (const comp of results[0].address_components) {
            if (comp.types.includes('administrative_area_level_1')) {
              pref = comp.long_name.replace(/(都|道|府|県)$/, '');
            }
            if (comp.types.includes('locality')) {
              city = comp.long_name;
            } else if (!city && (comp.types.includes('sublocality_level_1') || comp.types.includes('administrative_area_level_2'))) {
              city = comp.long_name;
            }
          }
          if (pref || city) {
            const prefSuffix = (pref === '東京' ? '都' : (pref === '大阪' || pref === '京都' ? '府' : (pref === '北海道' ? '' : '県')));
            const fullName = `${pref}${prefSuffix}${city || ''}`;
            const formatted = results[0].formatted_address ? results[0].formatted_address.replace(/^日本、\s*/, '') : fullName;
            callback({
              pref: pref || '栃木',
              city: city || null,
              name: fullName,
              formattedAddress: formatted
            });
            return;
          }
        }
        // Geocoder がエラー（API未有効化や権限など）または該当なしの場合はフォールバック
        const fb = resolveLocationFromCoords(lat, lng);
        callback({
          ...fb,
          formattedAddress: fb.name
        });
      });
      return;
    }

    // Google Maps API 未初期化時のフォールバック
    const fb = resolveLocationFromCoords(lat, lng);
    callback({
      ...fb,
      formattedAddress: fb.name
    });
  }

  function handleCurrentLocationSearch() {
    if (typeof navigator !== 'undefined' && navigator.geolocation) {
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('現在地を取得中...');
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
          const lat = position.coords.latitude;
          const lng = position.coords.longitude;

          // Google Maps 逆ジオコーディングで市区町村を取得
          reverseGeocodeWithGoogle(lat, lng, (loc) => {
            const pref = loc.pref || 'all';
            const cities = loc.city ? [loc.city] : [];

            areaModalState.selectedPref = pref;
            areaModalState.pendingCities = [...cities];
            areaModalState.step = cities.length > 0 ? 'city' : 'pref';
            areaModalState.isOpen = false;

            store.setFilters({
              pref: pref,
              cities: [...cities]
            });

            // 地図を現在地へ移動
            if (window.MachirikuMap && typeof window.MachirikuMap.panToLocation === 'function') {
              window.MachirikuMap.panToLocation(lat, lng, 14);
            }

            if (typeof MachirikuApp !== 'undefined') {
              const count = store.getFilteredProperties().length;
              const displayName = loc.name || (pref !== 'all' ? `${pref} ${cities.join(', ')}` : '現在地周辺');
              if (count > 0) {
                MachirikuApp.showToast(`現在地（${displayName}）の物件を表示中（${count}件）`);
              } else {
                MachirikuApp.showToast(`現在地（${displayName}）を検索しました（該当物件0件）`);
              }
            }
          });
        },
        () => {
          areaModalState.selectedPref = '栃木';
          areaModalState.pendingCities = ['宇都宮市'];
          areaModalState.step = 'city';
          areaModalState.isOpen = false;
          store.setFilters({ pref: '栃木', cities: ['宇都宮市'] });
          if (typeof MachirikuApp !== 'undefined') {
            MachirikuApp.showToast('現在地を取得できませんでした。対象エリア（栃木県宇都宮市）を表示します。');
          }
        },
        { enableHighAccuracy: true, timeout: 5000, maximumAge: 0 }
      );
    } else {
      areaModalState.selectedPref = '栃木';
      areaModalState.pendingCities = ['宇都宮市'];
      areaModalState.step = 'city';
      areaModalState.isOpen = false;
      store.setFilters({ pref: '栃木', cities: ['宇都宮市'] });
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('位置情報サービスに対応していません。栃木県宇都宮市を表示します。');
      }
    }
  }

  // 賃料ポップオーバー操作
  function toggleRentPopover() {
    rentPopoverOpen = !rentPopoverOpen;
    if (rentPopoverOpen) {
      catPopoverOpen = false;
      const state = store.getState();
      rentPopoverState.pendingMin = state.filters.rentMin || 0;
      rentPopoverState.pendingMax = state.filters.rentMax || 500000;
      rentPopoverState.includeMgmtFee = Boolean(state.filters.includeMgmtFee);
      rentPopoverState.noDeposit = Boolean(state.filters.noDeposit);
    }
    store.notify('ui');
  }

  function closeRentPopover() {
    rentPopoverOpen = false;
    store.notify('ui');
  }

  function handleRentSelectChange(type, val) {
    const num = Number(val);
    if (type === 'min') {
      rentPopoverState.pendingMin = num;
      if (rentPopoverState.pendingMin > rentPopoverState.pendingMax) {
        rentPopoverState.pendingMax = rentPopoverState.pendingMin;
      }
    } else {
      rentPopoverState.pendingMax = num;
      if (rentPopoverState.pendingMax < rentPopoverState.pendingMin) {
        rentPopoverState.pendingMin = rentPopoverState.pendingMax;
      }
    }

    const minMan = Math.round((rentPopoverState.pendingMin || 0) / 10000);
    const maxMan = rentPopoverState.pendingMax >= 50000000 ? 50 : Math.min(50, Math.round(rentPopoverState.pendingMax / 10000));

    // スライダー位置を同期
    const minInput = document.getElementById('rent-slider-min');
    const maxInput = document.getElementById('rent-slider-max');
    if (minInput) minInput.value = minMan;
    if (maxInput) maxInput.value = maxMan;

    // トラック強調位置
    const highlight = document.getElementById('range-slider-highlight');
    if (highlight) {
      const left = (minMan / 50) * 100;
      const width = ((maxMan - minMan) / 50) * 100;
      highlight.style.left = `${left}%`;
      highlight.style.width = `${width}%`;
    }

    // 他方のセレクトも連動同期
    syncRentSelectValue('rent-sel-min', rentPopoverState.pendingMin, false);
    syncRentSelectValue('rent-sel-max', rentPopoverState.pendingMax, true);

    // ヒストグラムの強調更新（棒の高さは維持）
    updateRentHistogramHighlight(minMan, maxMan);

    // 該当物件数ライブプレビュー更新
    updateRentLivePreviewCount();
  }

  function handleRentOptionChange(opt, isChecked) {
    if (opt === 'mgmt') rentPopoverState.includeMgmtFee = Boolean(isChecked);
    if (opt === 'deposit') rentPopoverState.noDeposit = Boolean(isChecked);

    // 母集団とヒストグラムを再計算
    const baseProps = getRentHistogramBaseProperties();
    const { bins, maxCount, totalProperties } = computeRentHistogram(baseProps, rentPopoverState.includeMgmtFee);
    const minMan = Math.round((rentPopoverState.pendingMin || 0) / 10000);
    const maxMan = rentPopoverState.pendingMax >= 50000000 ? 50 : Math.min(50, Math.round(rentPopoverState.pendingMax / 10000));

    const histContainer = document.getElementById('rent-histogram-bars');
    if (histContainer) {
      histContainer.innerHTML = renderRentHistogramHtml(bins, maxCount, minMan, maxMan);
    }
    const summaryEl = document.getElementById('rent-hist-summary-text');
    if (summaryEl) {
      summaryEl.innerHTML = `賃料分布 (母集団 <strong>${totalProperties}件</strong>)`;
    }

    updateRentLivePreviewCount();
  }

  function resetRentFilters() {
    rentPopoverState.pendingMin = 0;
    rentPopoverState.pendingMax = 50000000;
    rentPopoverState.includeMgmtFee = false;
    rentPopoverState.noDeposit = false;
    store.setFilters({
      rentMin: 0,
      rentMax: 50000000,
      includeMgmtFee: false,
      noDeposit: false
    });
    rentPopoverOpen = false;
    if (typeof MachirikuApp !== 'undefined') MachirikuApp.showToast("賃料条件をリセットしました");
  }

  function applyRentFilters() {
    rentPopoverOpen = false;
    store.setFilters({
      rentMin: rentPopoverState.pendingMin,
      rentMax: rentPopoverState.pendingMax,
      includeMgmtFee: rentPopoverState.includeMgmtFee,
      noDeposit: rentPopoverState.noDeposit
    });
    if (typeof MachirikuApp !== 'undefined') MachirikuApp.showToast("賃料条件を適用しました");
  }

  // 業種ポップオーバー操作
  function toggleCatPopover() {
    catPopoverOpen = !catPopoverOpen;
    if (catPopoverOpen) {
      rentPopoverOpen = false;
      const state = store.getState();
      catPopoverState.pendingCategories = (state.filters.categories && state.filters.categories.length)
        ? [...state.filters.categories]
        : (state.filters.category !== 'all' ? [state.filters.category] : []);
    }
    store.notify('ui');
  }

  function closeCatPopover() {
    catPopoverOpen = false;
    store.notify('ui');
  }

  function toggleDemandAccordion(propId, btn) {
    if (typeof document === 'undefined') return;
    const subranks = document.getElementById(`demand-subranks-${propId}`);
    if (!subranks) return;
    const isHidden = (subranks.style.display === 'none' || subranks.hidden);
    subranks.style.display = isHidden ? 'flex' : 'none';
    subranks.hidden = !isHidden;
    if (btn) {
      btn.classList.toggle('is-open', isHidden);
      btn.setAttribute('aria-expanded', String(isHidden));
      btn.title = isHidden ? '2位・3位を折りたたむ' : '2位・3位の需要を表示';
    }
  }

  function handleCatCheckboxChange(catId, isChecked) {
    if (isChecked) {
      if (!catPopoverState.pendingCategories.includes(catId)) {
        catPopoverState.pendingCategories.push(catId);
      }
    } else {
      catPopoverState.pendingCategories = catPopoverState.pendingCategories.filter(id => id !== catId);
    }
    store.notify('ui');
  }

  function toggleAllCategories() {
    if (catPopoverState.pendingCategories.length === data.CATEGORIES.length) {
      catPopoverState.pendingCategories = [];
    } else {
      catPopoverState.pendingCategories = data.CATEGORIES.map(c => c.id);
    }
    store.notify('ui');
  }

  function applyCatFilters() {
    catPopoverOpen = false;
    const cats = [...catPopoverState.pendingCategories];
    store.setFilters({
      categories: cats,
      category: cats.length === 1 ? cats[0] : (cats.length === 0 ? 'all' : cats[0])
    });
    if (typeof MachirikuApp !== 'undefined') MachirikuApp.showToast("業種条件を適用しました");
  }

  // ドキュメントクリック / ESCキーでのポップオーバー・モーダルクローズ
  if (typeof document !== 'undefined' && document.addEventListener) {
    document.addEventListener('click', (e) => {
      let changed = false;
      if (rentPopoverOpen) {
        const anchor = document.getElementById('rent-popover-anchor');
        if (anchor && !anchor.contains(e.target)) {
          rentPopoverOpen = false;
          changed = true;
        }
      }
      if (catPopoverOpen) {
        const anchor = document.getElementById('cat-popover-anchor');
        if (anchor && !anchor.contains(e.target)) {
          catPopoverOpen = false;
          changed = true;
        }
      }
      if (changed) store.notify('ui');
    });

    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        if (areaModalState.isOpen) {
          closeAreaModal();
        } else if (rentPopoverOpen || catPopoverOpen) {
          rentPopoverOpen = false;
          catPopoverOpen = false;
          store.notify('ui');
        }
      }
    });
  }

  function handleRentSliderInput(type, val) {
    const minInput = document.getElementById('rent-slider-min');
    const maxInput = document.getElementById('rent-slider-max');
    if (!minInput || !maxInput) return;

    let minVal = parseInt(minInput.value, 10) || 0;
    let maxVal = parseInt(maxInput.value, 10) || 50;

    // 下限が上限を超えないよう制御
    if (type === 'min' && minVal > maxVal) {
      minVal = maxVal;
      minInput.value = minVal;
    } else if (type === 'max' && maxVal < minVal) {
      maxVal = minVal;
      maxInput.value = maxVal;
    }

    // pending 状態の更新（適用ボタンを押すまでは検索の確定反映は行わない）
    rentPopoverState.pendingMin = minVal * 10000;
    rentPopoverState.pendingMax = maxVal >= 50 ? 50000000 : maxVal * 10000;

    // スライダートラック強調表示
    const highlight = document.getElementById('range-slider-highlight');
    if (highlight) {
      const left = (minVal / 50) * 100;
      const width = ((maxVal - minVal) / 50) * 100;
      highlight.style.left = `${left}%`;
      highlight.style.width = `${width}%`;
    }

    // ドロップダウン選択肢との双方向同期
    syncRentSelectValue('rent-sel-min', rentPopoverState.pendingMin, false);
    syncRentSelectValue('rent-sel-max', rentPopoverState.pendingMax, true);

    // ヒストグラムの選択範囲強調（棒の高さは維持し、in-rangeクラスのみ更新）
    updateRentHistogramHighlight(minVal, maxVal);

    // 該当物件数のライブプレビュー更新
    updateRentLivePreviewCount();
  }

  // セレクトボックスの値同期ヘルパー
  function syncRentSelectValue(selectId, yenVal, isMax) {
    const sel = document.getElementById(selectId);
    if (!sel) return;
    const val = Number(yenVal);
    let found = false;
    for (let i = 0; i < sel.options.length; i++) {
      if (Number(sel.options[i].value) === val) {
        sel.selectedIndex = i;
        found = true;
        break;
      }
    }
    if (!found) {
      const man = Math.round(val / 10000);
      const label = isMax && val >= 50000000 ? '上限なし' : (val === 0 ? (isMax ? '上限なし' : '下限なし') : `${man}万円`);
      let newOpt;
      if (typeof Option !== 'undefined') {
        newOpt = new Option(label, val, true, true);
      } else if (typeof document !== 'undefined' && document.createElement) {
        newOpt = document.createElement('option');
        newOpt.value = String(val);
        newOpt.textContent = label;
        newOpt.selected = true;
      } else {
        newOpt = { value: String(val), text: label, selected: true };
      }
      sel.add(newOpt, 0);
      sel.selectedIndex = 0;
    }
  }

  // ヒストグラムの選択範囲強調更新（高さは一切変えずクラスのみ更新）
  function updateRentHistogramHighlight(minMan, maxMan) {
    const histContainer = document.getElementById('rent-histogram-bars');
    if (!histContainer) return;
    const bars = histContainer.querySelectorAll('.hist-bar');
    bars.forEach(bar => {
      const binMin = Number(bar.dataset.min);
      const binMax = Number(bar.dataset.max);
      const inRange = binMax > minMan && binMin < maxMan;
      bar.classList.toggle('in-range', inRange);
    });
  }

  // 該当物件数のリアルタイム更新（要件3：ライブプレビュー）
  function updateRentLivePreviewCount() {
    const prospectiveCount = store.getFilteredProperties({
      rentMin: rentPopoverState.pendingMin,
      rentMax: rentPopoverState.pendingMax,
      includeMgmtFee: rentPopoverState.includeMgmtFee,
      noDeposit: rentPopoverState.noDeposit
    }).length;

    const previewEl = document.getElementById('rent-live-preview');
    if (previewEl) {
      if (prospectiveCount > 0) {
        previewEl.className = 'rent-live-preview';
        previewEl.innerHTML = `この条件に該当する物件: <strong class="preview-count">${prospectiveCount}件</strong>`;
      } else {
        previewEl.className = 'rent-live-preview is-zero';
        previewEl.innerHTML = `<span class="preview-zero-text">該当する物件: <strong>0件</strong> <small class="zero-hint">（賃料の範囲を広げてください）</small></span>`;
      }
    }

    const applyBtn = document.getElementById('btn-rent-apply');
    if (applyBtn) {
      applyBtn.textContent = `適用する (${prospectiveCount}件)`;
    }
  }

  // 棒グラフホバー時の情報表示
  function handleHistBarHover(el, label, count) {
    const tipEl = document.getElementById('rent-hist-hover-tip');
    const summaryEl = document.getElementById('rent-hist-summary-text');
    if (tipEl && count > 0) {
      tipEl.textContent = `${label}: ${count}件`;
      tipEl.style.display = 'inline-block';
      if (summaryEl) summaryEl.style.display = 'none';
    }
  }

  function handleHistBarLeave() {
    const tipEl = document.getElementById('rent-hist-hover-tip');
    const summaryEl = document.getElementById('rent-hist-summary-text');
    if (tipEl) tipEl.style.display = 'none';
    if (summaryEl) summaryEl.style.display = 'inline-block';
  }

  // 棒グラフクリック時：その賃料帯にスライダーを合わせる
  function handleHistBarClick(binMin, binMax) {
    const minInput = document.getElementById('rent-slider-min');
    const maxInput = document.getElementById('rent-slider-max');
    if (!minInput || !maxInput) return;
    minInput.value = binMin;
    maxInput.value = Math.min(50, binMax);
    handleRentSliderInput('min', binMin);
    handleRentSliderInput('max', Math.min(50, binMax));
  }

  function handleRentBoxChange(type, textVal) {
    let num = parseInt(String(textVal).replace(/[^\d]/g, ''), 10);
    const currentMin = Math.round((store.getState().filters.rentMin || 0) / 10000);
    const currentMax = Math.round((store.getState().filters.rentMax || 500000) / 10000);
    if (isNaN(num)) {
      num = type === 'min' ? 0 : 50;
    }
    num = Math.max(0, Math.min(50, num));

    if (type === 'min') {
      if (num > currentMax) num = currentMax;
      store.setFilters({ rentMin: num * 10000 });
    } else {
      if (num < currentMin) num = currentMin;
      store.setFilters({ rentMax: num >= 50 ? 50000000 : num * 10000 });
    }
  }

  function resetRentSlider() {
    store.setFilters({ rentMin: 0, rentMax: 50000000 });
    const minSlider = document.getElementById('rent-slider-min');
    const maxSlider = document.getElementById('rent-slider-max');
    if (minSlider) minSlider.value = '0';
    if (maxSlider) maxSlider.value = '50';
  }

  function filterByCat(catId) {
    store.setFilters({ category: catId });
  }

  function handleSortChange(sortVal) {
    store.setFilters({ sort: sortVal });
  }

  function handleToggleFollow(propId) {
    store.toggleFollowProperty(propId);
    const isNowFollowed = store.getState().currentUser.followedProps?.includes(propId);
    MachirikuApp.showToast(isNowFollowed ? '物件をフォローしました' : 'フォローを解除しました');
  }

  function validateRanks(form) {
    const r1 = form.rank1.value;
    const r2 = form.rank2.value;
    const r3 = form.rank3.value;

    if (r1 && r2 && r1 === r2) {
      alert("第1希望と第2希望に同じ業種は選択できません。別の業種をお選びください。");
      form.rank2.value = "";
    }
    if ((r1 && r3 && r1 === r3) || (r2 && r3 && r2 === r3)) {
      alert("第3希望に重複する業種は選択できません。別の業種をお選びください。");
      form.rank3.value = "";
    }
  }

  function handleVoteSubmit(propId, form) {
    const ranks = [form.rank1.value, form.rank2.value, form.rank3.value].filter(Boolean);
    const surveyData = {
      frequency: form.frequency ? form.frequency.value : '',
      timing: form.timing ? form.timing.value : '',
      comment: form.comment ? form.comment.value : ''
    };

    const res = store.submitVote(propId, ranks, surveyData);
    if (res.success) {
      MachirikuApp.showToast('投票内容を受け付けました');
      MachirikuApp.navigate('s02', { id: propId });
    }
  }

  function handleApplySubmit(propId, form) {
    const prop = store.getPropertyById(propId);
    if (!prop) { MachirikuApp.showToast('対象の情報が見つかりません。一覧から選び直してください。'); return; }
    const appData = {
      propId: propId,
      propTitle: prop.title,
      storeName: form.storeName.value,
      genreId: form.genreId.value,
      targetRent: form.targetRent?.value || prop.rent,
      targetOpenDate: form.targetOpenDate?.value || form.requestedStartOn?.value || '',
      contractKind: form.contractKind?.value || 'MONTHLY',
      requestedStartOn: form.requestedStartOn?.value || '',
      requestedEndOn: form.requestedEndOn?.value || '',
      viewingRequestedAt: form.viewingRequestedAt?.value || '',
      message: form.message?.value || form.concept?.value || '',
      concept: form.message?.value || form.concept?.value || ''
    };

    store.submitApplication(appData);
    MachirikuApp.showToast("出店エントリーを送信しました。 オーナーからの連絡をお待ちください");
    MachirikuApp.navigate('s09');
  }

  function handleAppAction(appId, status) {
    store.updateApplicationStatus(appId, status);
    MachirikuApp.showToast("ステータスを更新しました");
    MachirikuApp.navigate('s10', { id: appId });
  }

  function handlePropRegisterSubmit(form, editId) {
    const areaTsubo = Number(form.areaTsubo?.value) || 20;
    const facs = [];
    if (form.facGas?.checked) facs.push("都市ガス");
    if (form.facWater?.checked) facs.push("給排水設備");
    if (form.facGrease?.checked) facs.push("グリストラップ");
    if (form.facDuct?.checked) facs.push("換気ダクト");
    if (form.facPower?.checked) facs.push("動力電力");
    if (form.facSign?.checked) facs.push("看板スペース");
    if (form.facPark?.checked) facs.push("駐車場あり");
    if (form.facToilet?.checked) facs.push("専用トイレ");

    const propData = {
      title: form.title.value,
      address: form.address.value,
      access: form.access ? form.access.value : "東武宇都宮駅 徒歩5分",
      rent: Number(form.rent.value),
      managementFee: Number(form.managementFee?.value) || 0,
      deposit: form.deposit?.value || "敷金2ヶ月 / 礼金1ヶ月",
      securityDeposit: form.securityDeposit?.value || "30万円",
      areaTsubo: areaTsubo,
      areaSqm: Math.round(areaTsubo * 3.305785 * 10) / 10,
      floor: form.floor?.value || "1階路面",
      structure: form.structure?.value || "鉄骨造",
      builtYear: form.builtYear?.value || "1995年築",
      condition: form.condition?.value || "業種相談可",
      facilities: facs.length > 0 ? facs : ["都市ガス", "給排水設備"],
      statusLabel: form.statusLabel?.value || "募集中",
      description: form.description?.value || ""
    };

    if (editId) {
      store.updateProperty(editId, propData);
      MachirikuApp.showToast("物件情報を更新しました。");
    } else {
      store.registerProperty(propData);
      MachirikuApp.showToast("物件を登録し、掲載を開始しました。");
    }
    MachirikuApp.navigate('s11');
  }

  function handleDeleteProp(propId) {
    if (confirm("本当にこの物件を削除しますか？ ")) {
      store.deleteProperty(propId);
      MachirikuApp.showToast("物件を削除しました");
      MachirikuApp.navigate('s11');
    }
  }

  function handleSendOffer() {
    MachirikuApp.showToast("事業主様へオファーメッセージを送信しました。");
    MachirikuApp.navigate('s11');
  }

  function handleContractComplete(appId) {
    if (appId) {
      store.updateApplicationStatus(appId, "contracted");
      const app = store.getApplicationById(appId);
    if (!app) { MachirikuApp.showToast('対象の情報が見つかりません。一覧から選び直してください。'); return; }
      MachirikuApp.showToast("契約成立と引渡しを記録しました。");
      MachirikuApp.navigate('s15', { appId: appId, propId: app ? app.propId : undefined });
    } else {
      MachirikuApp.showToast("契約成立と引渡しを記録しました。");
      MachirikuApp.navigate('s15');
    }
  }

  function handlePublishPlanSubmit(form, propId) {
    const targetPropId = propId || form.propId?.value || store.getProperties()[0]?.id;
    const progressStatus = form.progressStatus?.value || 'PREPARING';
    store.publishStorePlan({
      propId: targetPropId,
      storeName: form.storeName.value,
      progressStatus: progressStatus,
      openDate: form.openDate.value,
      message: form.message.value,
      isOpenNow: progressStatus === 'OPENED'
    });
    MachirikuApp.showToast("出店計画を公開しました。 住民の進捗画面に反映されました");
    MachirikuApp.navigate('s05');
  }

  const LINE_AUTH_CONFIG = {
    channelId: '2012162622',
    localCallbackUrl: 'http://localhost:8000/machiriku/callback.html'
  };

  function handleLogout(isOnboarding) {
    const msg = isOnboarding ? '登録を中断してログアウトしますか？' : 'ログアウトしますか？';
    if (!confirm(msg)) {
      return;
    }
    try {
      if (typeof window !== 'undefined') {
        if (window.sessionStorage) {
          window.sessionStorage.removeItem('machiriku_onboarding');
          window.sessionStorage.removeItem('machiriku_user_display_name');
        }
        if (window.localStorage) {
          const keysToRemove = [];
          for (let i = 0; i < window.localStorage.length; i++) {
            const k = window.localStorage.key(i);
            if (k && k.startsWith('machiriku')) {
              keysToRemove.push(k);
            }
          }
          keysToRemove.forEach(k => window.localStorage.removeItem(k));
        }
        if (window.MachirikuStore && typeof window.MachirikuStore.resetState === 'function') {
          window.MachirikuStore.resetState();
        }
      }
    } catch (_) {}

    const ctx = (typeof window !== 'undefined' && window.__MACHIRIKU_CONTEXT_PATH__ !== undefined)
      ? window.__MACHIRIKU_CONTEXT_PATH__
      : (window.location.pathname.startsWith('/machiriku') ? '/machiriku' : '');

    if (ctx || window.location.pathname.includes('/machiriku')) {
      window.location.href = (ctx || '/machiriku') + '/jp/machiriku/servlet/auth/Logout.action';
    } else {
      if (typeof MachirikuStore !== 'undefined' && typeof MachirikuStore.setRole === 'function') {
        MachirikuStore.setRole('resident');
      }
      if (typeof MachirikuApp !== 'undefined') {
        MachirikuApp.showToast('ログアウトしました');
        MachirikuApp.navigate('s01');
      } else {
        window.location.href = 'index.html';
      }
    }
  }

  function handleCreateSurveySubmit(form) {
    const prop = store.getPropertyById(form.storeSelect?.value);
    if (!prop) { MachirikuApp.showToast('対象物件を選んでください。'); return; }
    const srv = store.createSurvey({
      propId: prop.id,
      storeName: prop.storePlan?.storeName || prop.title,
      title: form.title.value,
      description: form.description.value,
      rewardCouponId: null,
      questions: [
        { id: "q1", type: "rating", text: "本日の店舗の総合満足度をお聞かせください", required: true },
        { id: "q2", type: "choice", text: form.q2Text.value, required: true }
      ]
    });
    MachirikuApp.showToast("アンケートを作成しました。 確認画面を開きます");
    MachirikuApp.navigate('s24', { id: srv.id });
  }

  function handlePublishSurveyConfirm(surveyId) {
    MachirikuApp.showToast("アンケートを公開し、店内掲示用QRコードを発行しました。");
    MachirikuApp.navigate('s25', { id: surveyId });
  }

  function setRating(val) {
    const input = document.getElementById("survey-rating-val");
    if (input) input.value = val;
    const box = document.getElementById("star-rating-box");
    if (box) {
      const stars = box.querySelectorAll("span");
      stars.forEach((s, idx) => {
        s.style.color = (idx < val) ? "var(--warning-text)" : "var(--border-strong)";
      });
    }
  }

  function handleSurveyResponseSubmit(surveyId, form) {
    const res = store.submitSurveyResponse(surveyId, {
      rating: form.rating.value,
      q2: form.q2.value,
      comment: form.comment.value
    });

    if (res.success) {
      MachirikuApp.showToast(res.rewardCouponId ? '回答ありがとうございました。クーポンを受け取りました' : '回答ありがとうございました。');
      MachirikuApp.navigate(res.rewardCouponId ? 's30' : 's26', res.rewardCouponId ? {} : { id: surveyId });
    }
  }

  function handleCreateCouponSubmit(form) {
    const targetVal = form.targetType ? form.targetType.value : "all_voters";
    const label = targetVal === 'all_voters' 
      ? 'この空き店舗に投票した住民' 
      : (targetVal === 'all_citizens' ? 'マチリク登録住民全員' : 'アンケート回答者');

    const prop = store.getPropertyById(form.propId?.value || store.getProperties()[0]?.id);
    if (!prop) { MachirikuApp.showToast('対象物件を選んでください。'); return; }
    store.createCoupon({
      propId: prop.id,
      storeName: prop.storePlan?.storeName || prop.title,
      title: form.title.value,
      type: form.type.value,
      discountValue: form.discountValue.value,
      targetType: targetVal,
      targetLabel: label,
      maxLimit: form.maxLimit.value,
      expireDate: form.expireDate.value,
      condition: form.condition.value
    });

    MachirikuApp.showToast("新しいクーポンを発行・配布開始しました。");
    MachirikuApp.navigate('s28');
  }

  function handleRedeemCoupon(couponId) {
    const res = store.redeemCoupon(couponId);
    if (res.success) {
      const stamp = document.getElementById("used-stamp-mark");
      if (stamp) stamp.classList.add("is-stamped");
      MachirikuApp.showToast("クーポンを利用しました。 ご利用ありがとうございました");
      setTimeout(() => {
        MachirikuApp.navigate('s30');
      }, 1500);
    } else {
      alert(res.message);
    }
  }

  function handleApplyPr(propId) {
    store.applyPrAdvertisement(propId, "スタンダードPRプラン");
    MachirikuApp.showToast("PR広告の掲載を開始しました。 検索トップに表示されます");
    MachirikuApp.navigate('s01');
  }

  function markAllNotifsRead() {
    const state = store.getState();
    state.notifications.forEach(n => n.isRead = true);
    store.notify();
    MachirikuApp.showToast("すべての通知を既読にしました");
  }

  // S02 物件写真ギャラリー & カルーセル操作
  function selectGalleryImage(index) {
    if (!currentGallery.items || !currentGallery.items[index]) return;
    currentGallery.index = index;
    updateGalleryView(true);
  }

  function navGallery(delta) {
    if (!currentGallery.items || !currentGallery.items.length) return;
    const len = currentGallery.items.length;
    currentGallery.index = (currentGallery.index + delta + len) % len;
    updateGalleryView(true);
  }

  function scrollThumbs(direction) {
    const track = document.getElementById('gallery-thumbs-track');
    if (track) {
      track.scrollBy({ left: direction * 280, behavior: 'smooth' });
    }
  }

  function updateGalleryView(withTransition = true) {
    const item = currentGallery.items[currentGallery.index];
    if (!item) return;

    // スライダートラックをアニメーション移動
    const sliderTrack = document.getElementById('gallery-slider-track');
    if (sliderTrack) {
      sliderTrack.style.transition = withTransition ? 'transform 0.4s cubic-bezier(0.16, 1, 0.3, 1)' : 'none';
      sliderTrack.style.transform = `translateX(-${currentGallery.index * 100}%)`;
    }

    const idxEl = document.getElementById('gallery-current-index');
    if (idxEl) idxEl.textContent = String(currentGallery.index + 1);
    const captionEl = document.getElementById('gallery-caption-text');
    if (captionEl) captionEl.textContent = item.label;
    const kindEl = document.getElementById('gallery-kind-label');
    if (kindEl) kindEl.textContent = getKindLabel(item.kind);

    const track = document.getElementById('gallery-thumbs-track');
    if (track) {
      const btns = track.querySelectorAll('.gallery-thumb-btn');
      btns.forEach((btn, idx) => {
        if (idx === currentGallery.index) {
          btn.classList.add('is-selected');
          if (btn.scrollIntoView) {
            btn.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
          }
        } else {
          btn.classList.remove('is-selected');
        }
      });
    }
  }

  // スライダーのポインタードラッグ＆スワイプ＆キーボード対応
  function initGallerySlider() {
    const view = document.getElementById('gallery-main-view');
    const track = document.getElementById('gallery-slider-track');
    if (!view || !track) return;
    if (view.dataset.sliderInit === 'true') return;
    view.dataset.sliderInit = 'true';

    let isDragging = false;
    let startX = 0;
    let currentDeltaX = 0;
    let viewWidth = 0;

    function onPointerDown(e) {
      if (e.target.closest('.gallery-nav-btn, .btn-gallery-all-overlay')) return;
      if (e.pointerType === 'mouse' && e.button !== 0) return;

      isDragging = true;
      startX = e.clientX;
      currentDeltaX = 0;
      viewWidth = view.offsetWidth || 1;
      track.style.transition = 'none';
      view.style.cursor = 'grabbing';
      if (view.setPointerCapture) {
        try { view.setPointerCapture(e.pointerId); } catch (_) {}
      }
    }

    function onPointerMove(e) {
      if (!isDragging) return;
      currentDeltaX = e.clientX - startX;
      const basePercent = -currentGallery.index * 100;
      const dragPercent = (currentDeltaX / viewWidth) * 100;
      track.style.transform = `translateX(${basePercent + dragPercent}%)`;
    }

    function onPointerUp(e) {
      if (!isDragging) return;
      isDragging = false;
      view.style.cursor = '';
      if (view.releasePointerCapture) {
        try { view.releasePointerCapture(e.pointerId); } catch (_) {}
      }

      const threshold = Math.min(60, viewWidth * 0.15);
      if (currentDeltaX < -threshold) {
        navGallery(1);
      } else if (currentDeltaX > threshold) {
        navGallery(-1);
      } else {
        updateGalleryView(true);
      }
      currentDeltaX = 0;
    }

    view.addEventListener('pointerdown', onPointerDown);
    view.addEventListener('pointermove', onPointerMove);
    view.addEventListener('pointerup', onPointerUp);
    view.addEventListener('pointercancel', onPointerUp);

    function onKeyDown(e) {
      if (!document.getElementById('gallery-main-view')) {
        window.removeEventListener('keydown', onKeyDown);
        return;
      }
      if (['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement?.tagName)) return;
      if (document.getElementById('photos-modal') || document.getElementById('lightbox-modal')) return;

      if (e.key === 'ArrowLeft') {
        e.preventDefault();
        navGallery(-1);
      } else if (e.key === 'ArrowRight') {
        e.preventDefault();
        navGallery(1);
      }
    }
    window.addEventListener('keydown', onKeyDown);
  }

  let modalGalleryItems = [];
  let currentLightboxIdx = 0;

  function openAllPhotosModal(propId) {
    const prop = store.getPropertyById(propId);
    if (!prop) return;
    const items = (prop.gallery && prop.gallery.length) ? prop.gallery : currentGallery.items;
    modalGalleryItems = items;
    closeAllPhotosModal();

    const modalHtml = `
      <div class="photos-modal-overlay" id="photos-modal" onclick="if(event.target === this) MachirikuScreens.closeAllPhotosModal()">
        <div class="photos-modal-box">
          <div class="photos-modal-header">
            <h3 class="photos-modal-title">${escapeHtml(prop.title)} の写真 (${items.length}枚)</h3>
            <button type="button" class="photos-modal-close-btn" onclick="MachirikuScreens.closeAllPhotosModal()" aria-label="閉じる">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
            </button>
          </div>
          <div class="photos-modal-grid" id="photos-modal-grid">
            ${items.map((item, idx) => `
              <div class="photo-tile-item" onclick="MachirikuScreens.openLightbox(${idx})" title="クリックして拡大表示">
                <img src="${escapeHtml(item.url)}" alt="${escapeHtml(item.label || prop.title)}" loading="lazy">
                <div class="photo-tile-hover-overlay">
                  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
                </div>
              </div>
            `).join('')}
          </div>
        </div>
      </div>
    `;
    if (document.body && document.body.insertAdjacentHTML) {
      document.body.insertAdjacentHTML('beforeend', modalHtml);
    }
  }

  function closeAllPhotosModal() {
    const modal = document.getElementById('photos-modal');
    if (modal) modal.remove();
    closeLightbox();
  }

  let lightboxKeyHandler = null;

  function openLightbox(idx) {
    if (!modalGalleryItems || !modalGalleryItems[idx]) return;
    currentLightboxIdx = idx;
    closeLightbox();

    const item = modalGalleryItems[idx];
    const total = modalGalleryItems.length;

    const lightboxHtml = `
      <div class="lightbox-overlay" id="lightbox-modal" onclick="if(event.target === this) MachirikuScreens.closeLightbox()">
        <button type="button" class="lightbox-close-btn" onclick="MachirikuScreens.closeLightbox()" aria-label="拡大表示を閉じる">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
        </button>
        ${total > 1 ? `
          <button type="button" class="lightbox-nav-btn prev" onclick="event.stopPropagation(); MachirikuScreens.navLightbox(-1)" aria-label="前の写真">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
          </button>
          <button type="button" class="lightbox-nav-btn next" onclick="event.stopPropagation(); MachirikuScreens.navLightbox(1)" aria-label="次の写真">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
          </button>
        ` : ''}
        <div class="lightbox-content-box" onclick="event.stopPropagation()">
          <img id="lightbox-img" src="${escapeHtml(item.url)}" alt="${escapeHtml(item.label || '')}" class="lightbox-image">
          <div class="lightbox-counter">${idx + 1} / ${total}</div>
        </div>
      </div>
    `;
    if (document.body && document.body.insertAdjacentHTML) {
      document.body.insertAdjacentHTML('beforeend', lightboxHtml);
    }

    if (typeof window !== 'undefined') {
      if (lightboxKeyHandler) window.removeEventListener('keydown', lightboxKeyHandler);
      lightboxKeyHandler = (e) => {
        if (e.key === 'Escape') MachirikuScreens.closeLightbox();
        else if (e.key === 'ArrowLeft') MachirikuScreens.navLightbox(-1);
        else if (e.key === 'ArrowRight') MachirikuScreens.navLightbox(1);
      };
      window.addEventListener('keydown', lightboxKeyHandler);
    }
  }

  function closeLightbox() {
    const lb = document.getElementById('lightbox-modal');
    if (lb) lb.remove();
    if (typeof window !== 'undefined' && lightboxKeyHandler) {
      window.removeEventListener('keydown', lightboxKeyHandler);
      lightboxKeyHandler = null;
    }
  }

  function navLightbox(delta) {
    if (!modalGalleryItems.length) return;
    const nextIdx = (currentLightboxIdx + delta + modalGalleryItems.length) % modalGalleryItems.length;
    currentLightboxIdx = nextIdx;

    const item = modalGalleryItems[nextIdx];
    const total = modalGalleryItems.length;

    const img = document.getElementById('lightbox-img');
    const counter = document.querySelector('#lightbox-modal .lightbox-counter');
    if (!img) {
      openLightbox(nextIdx);
      return;
    }

    // DOMを破棄せず、画像とカウンターのみをスムーズにインプレース更新（裏画面の露出を完全防止）
    img.style.opacity = '0.5';
    img.src = item.url;
    img.alt = item.label || '';
    img.onload = () => { img.style.opacity = '1'; };
    setTimeout(() => { if (img) img.style.opacity = '1'; }, 80);

    if (counter) {
      counter.textContent = `${nextIdx + 1} / ${total}`;
    }
  }

  function filterPhotosModal(kind) {}
  function selectGalleryFromModal(index) {
    selectGalleryImage(index);
    closeAllPhotosModal();
  }

  // QRコード描画ヘルパー (qrcode.js を使用)
  function renderQrCode(containerId, text) {
    const container = document.getElementById(containerId);
    if (!container) return;
    if (!window.qrcode) {
      container.textContent = 'QRコードを表示できません。ページを再読み込みしてください。';
      return;
    }
    try {
      const qr = window.qrcode(0, 'M');
      qr.addData(text);
      qr.make();
      container.innerHTML = qr.createImgTag(4, 8);
    } catch (e) {
      container.textContent = 'QRコードを表示できません。ページを再読み込みしてください。';
      console.warn("QR Code render error:", e);
    }
  }

  let s02GoogleMap = null;
  function initS02MiniMap() {
    if (typeof document === 'undefined') return;
    const container = document.getElementById('s02-location-map');
    if (!container) return;

    const lat = parseFloat(container.getAttribute('data-lat'));
    const lng = parseFloat(container.getAttribute('data-lng'));
    const title = container.getAttribute('data-title') || '対象物件';
    if (isNaN(lat) || isNaN(lng)) return;

    function renderNativeMap() {
      const pos = { lat, lng };
      s02GoogleMap = new google.maps.Map(container, {
        center: pos,
        zoom: 16,
        mapTypeControl: false,
        streetViewControl: false,
        fullscreenControl: false,
        zoomControl: false,
        gestureHandling: 'greedy',
        scrollwheel: true
      });
      new google.maps.Marker({
        position: pos,
        map: s02GoogleMap,
        title: title
      });
    }

    if (typeof google !== 'undefined' && google.maps && google.maps.Map) {
      renderNativeMap();
    } else {
      // Fallback iframe or wait for script load
      const q = encodeURIComponent(`${lat},${lng}`);
      container.innerHTML = `<iframe width="100%" height="100%" style="border:0;" loading="lazy" allowfullscreen referrerpolicy="no-referrer-when-downgrade" src="https://maps.google.com/maps?q=${q}&z=16&output=embed"></iframe>`;
      
      let checks = 0;
      const interval = setInterval(() => {
        checks++;
        if (typeof google !== 'undefined' && google.maps && google.maps.Map) {
          clearInterval(interval);
          renderNativeMap();
        } else if (checks > 10) {
          clearInterval(interval);
        }
      }, 500);
    }
  }

  return {
    renderEmptyState, resetSearch, handlePropertyImageError,
    renderS01, renderS02, renderS03, renderS04, renderS05, renderS06, renderS07,
    renderS08, renderS09, renderS10, renderS11, renderS12, renderS13, renderS14,
    renderS15, renderS16, renderS17, renderS18, renderS19, renderS20, renderS21,
    renderS22, renderS23, renderS24, renderS25, renderS26, renderS27, renderS28,
    renderS29, renderS30, renderS31, renderS32, renderS33,
    handleS01Search,
    filterByCat,
    filterByPref,
    scrollToMap,
    handleCurrentLocationSearch,
    openAreaModal,
    closeAreaModal,
    setAreaModalStep,
    handleAreaCityCheck,
    handleAreaCitySoloSelect,
    clearAreaSelection,
    applyAreaSelection,
    toggleRentPopover,
    closeRentPopover,
    handleRentSelectChange,
    handleRentOptionChange,
    resetRentFilters,
    applyRentFilters,
    toggleCatPopover,
    toggleDemandAccordion,
    closeCatPopover,
    handleCatCheckboxChange,
    toggleAllCategories,
    applyCatFilters,
    handleRentSliderInput,
    handleRentBoxChange,
    resetRentSlider,
    handleHistBarHover,
    handleHistBarLeave,
    handleHistBarClick,
    handleSortChange,
    handleToggleFollow,
    validateRanks,
    handleVoteSubmit,
    handleApplySubmit,
    handleAppAction,
    handlePropRegisterSubmit,
    handleDeleteProp,
    handleSendOffer,
    handleContractComplete,
    handlePublishPlanSubmit,
    handleLogout,
    initS02MiniMap,
    initS18MapPicker,
    handleS18CurrentLocation,
    setS18Coords,
    toggleS18WorkMap,
    initS18WorkMapPicker,
    handleS18WorkCurrentLocation,
    setS18WorkCoords,
    handleS18ProfileSubmit,
    handleS18BusinessProfileSubmit,
    handleS18OwnerProfileSubmit,
    initS18BizAreaMap,
    setBizMapMode,
    clearBizDraw,
    setBizAreaFromSelect,
    handleCreateSurveySubmit,
    handlePublishSurveyConfirm,
    setRating,
    handleSurveyResponseSubmit,
    handleCreateCouponSubmit,
    handleRedeemCoupon,
    handleApplyPr,
    markAllNotifsRead,
    renderQrCode,
    initGallerySlider,
    selectGalleryImage,
    navGallery,
    scrollThumbs,
    openAllPhotosModal,
    closeAllPhotosModal,
    openLightbox,
    closeLightbox,
    navLightbox,
    filterPhotosModal,
    selectGalleryFromModal,
    switchResidentCatTab,
    toggleResidentIndustry,
    toggleAllResidentCatIndustries,
    removeResidentIndustry,
    clearAllResidentIndustries
  };
})();
