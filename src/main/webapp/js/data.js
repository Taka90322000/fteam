/**
 * マチリク (Machiriku) - マスターデータ & 初期状態
 * 宇都宮市 空き店舗マッチング・地域需要プラットフォーム
 * 準拠: 企画書、画面設計書(33画面)、テーブル設計書(7大分類・27小分類、33テーブル)
 */
window.MACHIRIKU_DATA = (function() {
  // 7大分類・27小分類マスタ (マチリク_テーブル設計書3.xlsx 準拠)
  const CATEGORIES = [
    {
      id: "C01", code: "C01", name: "飲食", order: 1, color: "#e07a5f",
      subcategories: [
        { id: "I0101", code: "I0101", name: "カフェ・喫茶店", icon: "" },
        { id: "I0102", code: "I0102", name: "レストラン", icon: "" },
        { id: "I0103", code: "I0103", name: "ラーメン・麺類", icon: "" },
        { id: "I0104", code: "I0104", name: "定食・食堂", icon: "" },
        { id: "I0105", code: "I0105", name: "居酒屋・バー", icon: "" },
        { id: "I0106", code: "I0106", name: "弁当・テイクアウト", icon: "" },
        { id: "I0107", code: "I0107", name: "パン屋・ベーカリー", icon: "" }
      ]
    },
    {
      id: "C02", code: "C02", name: "小売", order: 2, color: "#3d5a80",
      subcategories: [
        { id: "I0201", code: "I0201", name: "コンビニ・小型スーパー", icon: "" },
        { id: "I0202", code: "I0202", name: "食料品・生鮮店", icon: "" },
        { id: "I0203", code: "I0203", name: "雑貨・生活用品店", icon: "" },
        { id: "I0204", code: "I0204", name: "衣料品・セレクトショップ", icon: "" },
        { id: "I0205", code: "I0205", name: "書店・文具店", icon: "" },
        { id: "I0206", code: "I0206", name: "花屋・フラワーショップ", icon: "" }
      ]
    },
    {
      id: "C03", code: "C03", name: "美容・健康", order: 3, color: "#81b29a",
      subcategories: [
        { id: "I0301", code: "I0301", name: "美容室・ヘアサロン", icon: "" },
        { id: "I0302", code: "I0302", name: "理容室・バーバー", icon: "" },
        { id: "I0303", code: "I0303", name: "ネイル・エステサロン", icon: "" },
        { id: "I0304", code: "I0304", name: "ジム・フィットネススタジオ", icon: "" }
      ]
    },
    {
      id: "C04", code: "C04", name: "教育・仕事", order: 4, color: "#2a9d8f",
      subcategories: [
        { id: "I0401", code: "I0401", name: "学習塾・スクール", icon: "" },
        { id: "I0402", code: "I0402", name: "コワーキングスペース", icon: "" }
      ]
    },
    {
      id: "C05", code: "C05", name: "趣味・娯楽", order: 5, color: "#f4a261",
      subcategories: [
        { id: "I0501", code: "I0501", name: "ゲーム・ホビーショップ", icon: "" },
        { id: "I0502", code: "I0502", name: "娯楽施設", icon: "" },
        { id: "I0503", code: "I0503", name: "文化・体験教室", icon: "" }
      ]
    },
    {
      id: "C06", code: "C06", name: "生活サービス", order: 6, color: "#6d597a",
      subcategories: [
        { id: "I0601", code: "I0601", name: "クリーニング店", icon: "" },
        { id: "I0602", code: "I0602", name: "修理サービス", icon: "" },
        { id: "I0603", code: "I0603", name: "ペット関連店舗", icon: "" }
      ]
    },
    {
      id: "C07", code: "C07", name: "医療・福祉", order: 7, color: "#457b9d",
      subcategories: [
        { id: "I0701", code: "I0701", name: "クリニック", icon: "" },
        { id: "I0702", code: "I0702", name: "福祉・介護サービス", icon: "" }
      ]
    }
  ];

  // 全小分類のフラットマップ
  const SUBCATS = {};
  CATEGORIES.forEach(cat => {
    cat.subcategories.forEach(sub => {
      SUBCATS[sub.id] = { ...sub, categoryId: cat.id, categoryName: cat.name, color: cat.color };
    });
  });

  // Virtual property catalog. Galleries: tools/update-generated-galleries.cjs
  const DATASET_VERSION = "machiriku-v20261111";
  const PROPERTIES = [
  {
    "id": "prop-orion-01",
    "code": "PROP-ORION-001",
    "source": {
      "site": "マチリク専任AI査定物件",
      "url": "https://machiriku.city.utsunomiya.local/properties/prop-orion-01",
      "listingId": "prop-orion-01",
      "displayListingNumber": "PROP-ORION-001",
      "retrievedAt": "2026-10-26T10:00:00+09:00",
      "listingUpdatedAt": "2026-10-26",
      "listingUpdatedAtRaw": "2026/10/26"
    },
    "title": "オリオン通りサウスビル 1階/101",
    "buildingName": "オリオン通りサウスビル",
    "unitNumber": "101",
    "propertyType": "賃貸店舗",
    "subtitle": "路面貸店舗 / 1階/4階建 / 52.8㎡",
    "sourceArea": "宇都宮市江野町",
    "address": "栃木県宇都宮市江野町5-12",
    "access": "東武宇都宮駅 徒歩3分",
    "rent": 198000,
    "rentRaw": "19.8万円",
    "rentTaxStatus": "税込",
    "managementFee": 15000,
    "deposit": "2ヶ月",
    "keyMoney": "1ヶ月",
    "securityDeposit": "なし",
    "otherFees": [
      {
        "name": "商店街共益費",
        "amountRaw": "月額5,000円"
      },
      {
        "name": "仲介手数料",
        "amountRaw": "賃料1ヶ月分＋税"
      }
    ],
    "areaTsubo": 15.97,
    "areaSqm": 52.8,
    "areaTsuboBasis": "calculated (52.8 sqm / 3.305785)",
    "floor": "1階/4階建",
    "structure": "RC(鉄筋コンクリート造)",
    "builtYear": "2008年3月",
    "handover": "相談（即時可）",
    "listingStatusRaw": "空き店舗（居抜き）",
    "condition": "元カフェ・ベーカリー居抜き（厨房設備残置・譲渡無償）",
    "permittedUses": [
      "カフェ・喫茶店",
      "ベーカリー",
      "スイーツ・テイクアウト",
      "軽飲食",
      "物販"
    ],
    "facilities": [
      "アーケード商店街直結（雨天集客力高）",
      "前面ガラス張りファサード",
      "木目調エントランス",
      "動力あり",
      "グリストラップ完備",
      "専用水洗トイレ",
      "都市ガス・給排水",
      "即引渡し可"
    ],
    "description": "宇都宮市の中心商業地「オリオン通り商店街」のアーケード内に位置する好立地路面店舗です。全天候型の歩行者専用アーケードに面しており、平日・休日を問わず安定した人通りがあります。前面大判ガラスサッシと落ち着いた木目調エントランスで視認性に優れ、店内はモダンなウッド＆スチール調。カウンター・給排水・排気ダクトが整っており、カフェ、ベーカリー、スイーツショップ等に最適です。",
    "broker": {
      "name": "マチリク宇都宮中心街エリアデスク",
      "listingReference": "MCH-ORION-2026-01"
    },
    "status": "listed",
    "statusLabel": "募集中",
    "isPr": false,
    "ownerId": "owner_utsunomiya_01",
    "ownerName": "オリオン商業開発株式会社",
    "location": {
      "lat": 36.559312,
      "lng": 139.882105,
      "accuracy": "pinpoint",
      "sourceUrl": "https://machiriku.city.utsunomiya.local/properties/prop-orion-01"
    },
    "mapCoords": {
      "x": 410,
      "y": 285,
      "lat": 36.559312,
      "lng": 139.882105,
      "isApproximate": false
    },
    "isOutOfMap": false,
    "coverImage": "assets/properties/prop-orion-01/01_exterior.jpg",
    "imageCaption": "外観 / オリオン通りアーケード沿い1階路面",
    "gallery": [
      {
        "index": 0,
        "kind": "exterior",
        "label": "外観（オリオン通りアーケード沿い1階路面）",
        "url": "assets/properties/prop-orion-01/01_exterior.jpg",
        "localPath": "assets/properties/prop-orion-01/01_exterior.jpg"
      },
      {
        "index": 1,
        "kind": "interior",
        "label": "内装（客席・木目カウンターとシックな黒スチール調）",
        "url": "assets/properties/prop-orion-01/02_interior.jpg",
        "localPath": "assets/properties/prop-orion-01/02_interior.jpg"
      }
    ],
    "votesCount": 342,
    "demandScore": 342,
    "topDemands": [
      {
        "subcatId": "I0101",
        "label": "カフェ・喫茶店",
        "votes": 148,
        "score": 148
      },
      {
        "subcatId": "I0107",
        "label": "パン屋・ベーカリー",
        "votes": 112,
        "score": 112
      },
      {
        "subcatId": "I0106",
        "label": "弁当・テイクアウト",
        "votes": 82,
        "score": 82
      }
    ],
    "sourceImages": [],
    "missingFields": [],
    "fieldEvidence": {},
    "sourceNotes": [],
    "residentVoices": [
      {
        "author": "30代・女性（地域住民）",
        "text": "オリオン通りを毎朝通勤で通るので、朝7時台からテイクアウトできる焼きたてパンとコーヒーのお店があったら絶対通います！",
        "date": "2026-10-24"
      },
      {
        "author": "40代・女性（地域住民）",
        "text": "買い物の合間にゆっくり座れる落ち着いたカフェが欲しいです。ベビーカーでも入りやすい段差なしの路面店なら最高です。",
        "date": "2026-10-22"
      },
      {
        "author": "20代（地域住民）",
        "text": "放課後や休日に友達と立ち寄れる映えるスイーツやカフェがあると嬉しい。夜も少し遅くまで営業してほしい！",
        "date": "2026-10-20"
      }
    ]
  },
  {
    "id": "prop-odori-02",
    "code": "PROP-ODORI-002",
    "source": {
      "site": "マチリク専任AI査定物件",
      "url": "https://machiriku.city.utsunomiya.local/properties/prop-odori-02",
      "listingId": "prop-odori-02",
      "displayListingNumber": "PROP-ODORI-002",
      "retrievedAt": "2026-10-26T10:00:00+09:00",
      "listingUpdatedAt": "2026-10-26",
      "listingUpdatedAtRaw": "2026/10/26"
    },
    "title": "宇都宮大通りフロントタワー 1階/102",
    "buildingName": "宇都宮大通りフロントタワー",
    "unitNumber": "102",
    "propertyType": "賃貸店舗事務所",
    "subtitle": "大通り角地店舗 / 1階/8階建 / 66.2㎡",
    "sourceArea": "宇都宮市本町",
    "address": "栃木県宇都宮市本町3-8",
    "access": "東武宇都宮駅 徒歩8分",
    "rent": 285000,
    "rentRaw": "28.5万円",
    "rentTaxStatus": "税込",
    "managementFee": 22000,
    "deposit": "3ヶ月",
    "keyMoney": "1ヶ月",
    "securityDeposit": "なし",
    "otherFees": [
      {
        "name": "共用部看板使用料",
        "amountRaw": "月額10,000円"
      },
      {
        "name": "仲介手数料",
        "amountRaw": "賃料1ヶ月分＋税"
      }
    ],
    "areaTsubo": 20.03,
    "areaSqm": 66.2,
    "areaTsuboBasis": "calculated (66.2 sqm / 3.305785)",
    "floor": "1階/8階建",
    "structure": "SRC(鉄骨鉄筋コンクリート造)",
    "builtYear": "2015年9月",
    "handover": "即時",
    "listingStatusRaw": "空室（元調剤薬局・物販居抜き）",
    "condition": "元調剤薬局居抜き（受付カウンター、待合、バックヤード残置）",
    "permittedUses": [
      "クリニック",
      "調剤薬局・ドラッグストア",
      "金融・保険窓口",
      "物販",
      "ショールーム"
    ],
    "facilities": [
      "宇都宮大通り交差点角地（圧倒的視認性）",
      "全自動ドア・完全バリアフリー設計",
      "車椅子対応トイレ",
      "大容量個別空調2系統",
      "バックヤード・調剤室ブース",
      "ビル共用ゴミ置場",
      "機械警備システム"
    ],
    "description": "宇都宮市の目抜き通り「大通り」と本町交差点の角地に堂々と佇む高層タワービルの1階路面区画です。バス停目の前で通行量・交通量ともに宇都宮随一。大開口のガラスウォールにより交差点の全方向から店舗内が視認できます。元調剤薬局のため、清潔な待合空間と調剤ブース、給排水、バリアフリートイレが整備済み。内科・歯科・皮膚科クリニックや処方薬局、大手ブランドショールームに最適です。",
    "broker": {
      "name": "大通りビジネスプロパティ株式会社",
      "listingReference": "MCH-ODORI-2026-02"
    },
    "status": "listed",
    "statusLabel": "募集中",
    "isPr": false,
    "ownerId": "owner_utsunomiya_02",
    "ownerName": "宇都宮本町都市開発",
    "location": {
      "lat": 36.562104,
      "lng": 139.884518,
      "accuracy": "pinpoint",
      "sourceUrl": "https://machiriku.city.utsunomiya.local/properties/prop-odori-02"
    },
    "mapCoords": {
      "x": 430,
      "y": 230,
      "lat": 36.562104,
      "lng": 139.884518,
      "isApproximate": false
    },
    "isOutOfMap": false,
    "coverImage": "assets/properties/prop-odori-02/01_exterior.jpg",
    "imageCaption": "外観 / 宇都宮大通り・本町交差点角地ビル1階",
    "gallery": [
      {
        "index": 0,
        "kind": "exterior",
        "label": "外観 / 宇都宮大通り・本町交差点角地ビル1階",
        "url": "assets/properties/prop-odori-02/01_exterior.jpg",
        "localPath": "assets/properties/prop-odori-02/01_exterior.jpg"
      },
      {
        "kind": "interior",
        "label": "内装（受付カウンター・待合・調剤ブース）",
        "url": "assets/properties/prop-odori-02/02_interior.jpg",
        "localPath": "assets/properties/prop-odori-02/02_interior.jpg",
        "index": 1
      }
    ],
    "votesCount": 289,
    "demandScore": 289,
    "topDemands": [
      {
        "subcatId": "I0701",
        "label": "クリニック",
        "votes": 120,
        "score": 120
      },
      {
        "subcatId": "I0201",
        "label": "コンビニ・小型スーパー",
        "votes": 98,
        "score": 98
      },
      {
        "subcatId": "I0601",
        "label": "生活サービス",
        "votes": 71,
        "score": 71
      }
    ],
    "sourceImages": [],
    "missingFields": [],
    "fieldEvidence": {},
    "sourceNotes": [],
    "residentVoices": [
      {
        "author": "60代以上・女性（地域住民）",
        "text": "大通り沿いに気軽に相談できる専門クリニックやドラッグストアがあると、高齢者でも通いやすくて非常に助かります。",
        "date": "2026-10-23"
      },
      {
        "author": "40代・男性（地域住民）",
        "text": "昼休みや仕事帰りにサッと立ち寄れる調剤薬局や健康相談スポットができると重宝します。",
        "date": "2026-10-21"
      }
    ],
    "galleryVersion": "2026-10-26-interiors-v1"
  },
  {
    "id": "prop-tobu-03",
    "code": "PROP-TOBU-003",
    "source": {
      "site": "マチリク専任AI査定物件",
      "url": "https://machiriku.city.utsunomiya.local/properties/prop-tobu-03",
      "listingId": "prop-tobu-03",
      "displayListingNumber": "PROP-TOBU-003",
      "retrievedAt": "2026-10-26T10:00:00+09:00",
      "listingUpdatedAt": "2026-10-26",
      "listingUpdatedAtRaw": "2026/10/26"
    },
    "title": "東武宇都宮駅前ダイニングテラス 2階/201",
    "buildingName": "東武宇都宮駅前ダイニングテラス",
    "unitNumber": "201",
    "propertyType": "賃貸店舗",
    "subtitle": "飲食専用フロア / 2階/3階建 / 59.4㎡",
    "sourceArea": "宇都宮市池上町",
    "address": "栃木県宇都宮市池上町4-2",
    "access": "東武宇都宮駅 徒歩2分",
    "rent": 176000,
    "rentRaw": "17.6万円",
    "rentTaxStatus": "税込",
    "managementFee": 18000,
    "deposit": "2ヶ月",
    "keyMoney": "1ヶ月",
    "securityDeposit": "なし",
    "otherFees": [
      {
        "name": "ダクト清掃積立金",
        "amountRaw": "月額4,000円"
      },
      {
        "name": "仲介手数料",
        "amountRaw": "賃料1ヶ月分＋税"
      }
    ],
    "areaTsubo": 17.97,
    "areaSqm": 59.4,
    "areaTsuboBasis": "calculated (59.4 sqm / 3.305785)",
    "floor": "2階/3階建",
    "structure": "鉄骨造",
    "builtYear": "2012年7月",
    "handover": "即時",
    "listingStatusRaw": "空き店舗（元イタリアンバル居抜き）",
    "condition": "元イタリアンバル居抜き（厨房設備・カウンター・エアコン完備）",
    "permittedUses": [
      "居酒屋・バー",
      "レストラン",
      "バル・ビストロ",
      "カフェ・軽食"
    ],
    "facilities": [
      "東武宇都宮駅西口徒歩2分の抜群の集客立地",
      "道路に面した専用独立直通階段",
      "開放的なオープンテラス・バルコニー席",
      "重飲食対応大型排気ダクト・グリストラップ",
      "専用水洗トイレ",
      "都市ガス・業務用給湯器",
      "深夜営業相談可"
    ],
    "description": "東武宇都宮駅西口から徒歩2分、飲食店が軒を連ねる池上町エリアの飲食専門ビル2階区画です。1階から直接上がれる独立専用階段を備えており、隠れ家感と視認性を両立。元人気イタリアンバルの居抜きで、ダークウッド調のバーカウンター、厨房給排水、ダクト、テラス席がそのまま残置されています。初期投資を大幅に抑えて本格的なバルやダイニングバーを開業可能です。",
    "broker": {
      "name": "宇都宮駅前テナントエージェント",
      "listingReference": "MCH-TOBU-2026-03"
    },
    "status": "listed",
    "statusLabel": "募集中",
    "isPr": false,
    "ownerId": "owner_utsunomiya_03",
    "ownerName": "池上リアルエステート",
    "location": {
      "lat": 36.557821,
      "lng": 139.880432,
      "accuracy": "pinpoint",
      "sourceUrl": "https://machiriku.city.utsunomiya.local/properties/prop-tobu-03"
    },
    "mapCoords": {
      "x": 370,
      "y": 295,
      "lat": 36.557821,
      "lng": 139.880432,
      "isApproximate": false
    },
    "isOutOfMap": false,
    "coverImage": "assets/properties/prop-tobu-03/01_exterior.jpg",
    "imageCaption": "外観 / 東武駅前通り・2階専用バルコニーと直通階段",
    "gallery": [
      {
        "index": 0,
        "kind": "exterior",
        "label": "外観 / 東武駅前通り・2階専用バルコニーと直通階段",
        "url": "assets/properties/prop-tobu-03/01_exterior.jpg",
        "localPath": "assets/properties/prop-tobu-03/01_exterior.jpg"
      },
      {
        "kind": "interior",
        "label": "内装（ダークウッドのバーカウンター・客席）",
        "url": "assets/properties/prop-tobu-03/02_interior.jpg",
        "localPath": "assets/properties/prop-tobu-03/02_interior.jpg",
        "index": 1
      },
      {
        "kind": "interior",
        "label": "内装（窓際の客席・カウンター別アングル）",
        "url": "assets/properties/prop-tobu-03/03_interior.jpg",
        "localPath": "assets/properties/prop-tobu-03/03_interior.jpg",
        "index": 2
      }
    ],
    "votesCount": 315,
    "demandScore": 315,
    "topDemands": [
      {
        "subcatId": "I0105",
        "label": "居酒屋・バー",
        "votes": 156,
        "score": 156
      },
      {
        "subcatId": "I0102",
        "label": "レストラン",
        "votes": 104,
        "score": 104
      },
      {
        "subcatId": "I0101",
        "label": "カフェ・喫茶店",
        "votes": 55,
        "score": 55
      }
    ],
    "sourceImages": [],
    "missingFields": [],
    "fieldEvidence": {},
    "sourceNotes": [],
    "residentVoices": [
      {
        "author": "30代・男性（地域住民）",
        "text": "仕事帰りにふらっと一人でも立ち寄れる、ワインやクラフトビールが美味しいバルを熱望しています！",
        "date": "2026-10-25"
      },
      {
        "author": "20代・女性（地域住民）",
        "text": "女子会やデートに使える雰囲気が良くて美味しい料理が出るビストロができると嬉しいです。",
        "date": "2026-10-22"
      }
    ],
    "galleryVersion": "2026-10-26-interiors-v1"
  },
  {
    "id": "prop-union-04",
    "code": "PROP-UNION-004",
    "source": {
      "site": "マチリク専任AI査定物件",
      "url": "https://machiriku.city.utsunomiya.local/properties/prop-union-04",
      "listingId": "prop-union-04",
      "displayListingNumber": "PROP-UNION-004",
      "retrievedAt": "2026-10-26T10:00:00+09:00",
      "listingUpdatedAt": "2026-10-26",
      "listingUpdatedAtRaw": "2026/10/26"
    },
    "title": "ユニオンストリートスクエア 1階/101",
    "buildingName": "ユニオンストリートスクエア",
    "unitNumber": "101",
    "propertyType": "賃貸店舗",
    "subtitle": "路面セレクトショップ / 1階/3階建 / 41.2㎡",
    "sourceArea": "宇都宮市伝馬町",
    "address": "栃木県宇都宮市伝馬町2-18",
    "access": "東武宇都宮駅 徒歩5分",
    "rent": 143000,
    "rentRaw": "14.3万円",
    "rentTaxStatus": "税込",
    "managementFee": 10000,
    "deposit": "2ヶ月",
    "keyMoney": "1ヶ月",
    "securityDeposit": "なし",
    "otherFees": [
      {
        "name": "ユニオン通り商店会費",
        "amountRaw": "月額3,000円"
      },
      {
        "name": "仲介手数料",
        "amountRaw": "賃料1ヶ月分＋税"
      }
    ],
    "areaTsubo": 12.46,
    "areaSqm": 41.2,
    "areaTsuboBasis": "calculated (41.2 sqm / 3.305785)",
    "floor": "1階/3階建",
    "structure": "RC(鉄筋コンクリート造)",
    "builtYear": "2016年11月",
    "handover": "即時",
    "listingStatusRaw": "空き店舗（元セレクトショップ居抜き）",
    "condition": "元アパレル・雑貨店（コンクリート打放し・アイアン什器・配線ダクト）",
    "permittedUses": [
      "衣料品・セレクトショップ",
      "雑貨・生活用品店",
      "書店・ギャラリー",
      "古着・ヴィンテージ",
      "コーヒースタンド"
    ],
    "facilities": [
      "宇都宮カルチャー発信地「ユニオン通り」路面店",
      "コンクリート打ち放し×黒アイアンの洗練デザイン",
      "天井高3.2mの開放感とライティングレール",
      "モルタル仕上げフロア",
      "試着室スペース・バックヤード",
      "専用水洗トイレ",
      "前面歩道平坦・自動ドア"
    ],
    "description": "若者カルチャーやこだわり派が集まる宇都宮屈指のストリート「ユニオン通り」の好立地路面店舗です。コンクリート打ち放しの外壁とブラックアイアンフレームのガラスファサードが際立つモダン建築。天井高が約3.2mあり、スポットライト配線レールやフィッティングスペース、什器が美しく配置できます。アパレル、ヴィンテージ、ライフスタイル雑貨、デザインスタジオ等に最適な空間です。",
    "broker": {
      "name": "ユニオンクリエイティブ不動産",
      "listingReference": "MCH-UNION-2026-04"
    },
    "status": "listed",
    "statusLabel": "募集中",
    "isPr": false,
    "ownerId": "owner_utsunomiya_04",
    "ownerName": "ユニオンアセットマネジメント",
    "location": {
      "lat": 36.558542,
      "lng": 139.876521,
      "accuracy": "pinpoint",
      "sourceUrl": "https://machiriku.city.utsunomiya.local/properties/prop-union-04"
    },
    "mapCoords": {
      "x": 320,
      "y": 280,
      "lat": 36.558542,
      "lng": 139.876521,
      "isApproximate": false
    },
    "isOutOfMap": false,
    "coverImage": "assets/properties/prop-union-04/01_exterior.jpg",
    "imageCaption": "外観 / ユニオン通り・コンクリート打放し路面店",
    "gallery": [
      {
        "index": 0,
        "kind": "exterior",
        "label": "外観 / ユニオン通り・コンクリート打放し路面店",
        "url": "assets/properties/prop-union-04/01_exterior.jpg",
        "localPath": "assets/properties/prop-union-04/01_exterior.jpg"
      },
      {
        "kind": "interior",
        "label": "内装（モルタル床・黒アイアンラック・照明レール）",
        "url": "assets/properties/prop-union-04/02_interior.jpg",
        "localPath": "assets/properties/prop-union-04/02_interior.jpg",
        "index": 1
      }
    ],
    "votesCount": 264,
    "demandScore": 264,
    "topDemands": [
      {
        "subcatId": "I0204",
        "label": "衣料品・セレクトショップ",
        "votes": 138,
        "score": 138
      },
      {
        "subcatId": "I0203",
        "label": "雑貨・生活用品店",
        "votes": 86,
        "score": 86
      },
      {
        "subcatId": "I0101",
        "label": "カフェ・喫茶店",
        "votes": 40,
        "score": 40
      }
    ],
    "sourceImages": [],
    "missingFields": [],
    "fieldEvidence": {},
    "sourceNotes": [],
    "residentVoices": [
      {
        "author": "20代・男性（地域住民）",
        "text": "ユニオン通りらしい、エッジの効いた国内デザイナーズや上質な古着を扱うセレクトショップが入ってほしいです！",
        "date": "2026-10-24"
      },
      {
        "author": "30代・女性（地域住民）",
        "text": "器や日用品を丁寧にセレクトした暮らしの道具店があると、散歩ついでに立ち寄りたい。",
        "date": "2026-10-21"
      }
    ],
    "galleryVersion": "2026-10-26-interiors-v1"
  },
  {
    "id": "prop-lrt-05",
    "code": "PROP-LRT-005",
    "source": {
      "site": "マチリク専任AI査定物件",
      "url": "https://machiriku.city.utsunomiya.local/properties/prop-lrt-05",
      "listingId": "prop-lrt-05",
      "displayListingNumber": "PROP-LRT-005",
      "retrievedAt": "2026-10-26T10:00:00+09:00",
      "listingUpdatedAt": "2026-10-26",
      "listingUpdatedAtRaw": "2026/10/26"
    },
    "title": "東宿郷LRTステーションフロント 2階/202",
    "buildingName": "東宿郷LRTステーションフロント",
    "unitNumber": "202",
    "propertyType": "賃貸店舗事務所",
    "subtitle": "LRT駅前オフィス / 2階/6階建 / 74.5㎡",
    "sourceArea": "宇都宮市東宿郷",
    "address": "栃木県宇都宮市東宿郷3丁目2-5",
    "access": "東宿郷駅 徒歩2分",
    "rent": 230000,
    "rentRaw": "23.0万円",
    "rentTaxStatus": "税込",
    "managementFee": 25000,
    "deposit": "3ヶ月",
    "keyMoney": "1ヶ月",
    "securityDeposit": "なし",
    "otherFees": [
      {
        "name": "共益費・清掃費",
        "amountRaw": "管理費に含む"
      },
      {
        "name": "仲介手数料",
        "amountRaw": "賃料1ヶ月分＋税"
      }
    ],
    "areaTsubo": 22.54,
    "areaSqm": 74.5,
    "areaTsuboBasis": "calculated (74.5 sqm / 3.305785)",
    "floor": "2階/6階建",
    "structure": "S造(鉄骨造)",
    "builtYear": "2021年4月",
    "handover": "相談（2026年11月可）",
    "listingStatusRaw": "空室（築浅オフィス・コワーキング）",
    "condition": "築浅美室（OAフロア、個別空調、男女別トイレ、EV完備）",
    "permittedUses": [
      "コワーキングスペース",
      "IT・デザインオフィス",
      "士業事務所",
      "資格スクール",
      "サテライト拠点"
    ],
    "facilities": [
      "次世代型路面電車「LRT東宿郷駅」徒歩2分の超好アクセス",
      "築浅ガラスカーテンウォール外観・高い企業イメージ",
      "OAフロア（床下配線）完備",
      "男女別共用トイレ・給湯室",
      "エレベーター2基完備",
      "機械警備・非接触ICカードキー",
      "光ファイバー引き込み済み"
    ],
    "description": "宇都宮駅東口の再開発とLRT開通により急速に発展する東宿郷エリアの築浅ハイグレードオフィスビル2階です。LRT東宿郷停留所から徒歩2分、JR宇都宮駅東口からも徒歩7分と交通利便性は抜群。2面採光の明るいガラスカーテンウォールで視界が開け、OAフロアや個別空調、男女別共用部を完備。芳賀工業団地方面へのアクセスも良く、企業のサテライトオフィスやコワーキングスペースに最適です。",
    "broker": {
      "name": "東宿郷プロパティマネジメント",
      "listingReference": "MCH-LRT-2026-05"
    },
    "status": "listed",
    "statusLabel": "募集中",
    "isPr": false,
    "ownerId": "owner_utsunomiya_05",
    "ownerName": "東宿郷開発パートナーズ",
    "location": {
      "lat": 36.558234,
      "lng": 139.905612,
      "accuracy": "pinpoint",
      "sourceUrl": "https://machiriku.city.utsunomiya.local/properties/prop-lrt-05"
    },
    "mapCoords": {
      "x": 680,
      "y": 280,
      "lat": 36.558234,
      "lng": 139.905612,
      "isApproximate": false
    },
    "isOutOfMap": false,
    "coverImage": "assets/properties/prop-lrt-05/01_exterior.jpg",
    "imageCaption": "外観 / LRT東宿郷駅前・近代的なガラスカーテンウォールビル",
    "gallery": [
      {
        "index": 0,
        "kind": "exterior",
        "label": "外観 / LRT東宿郷駅前・近代的なガラスカーテンウォールビル",
        "url": "assets/properties/prop-lrt-05/01_exterior.jpg",
        "localPath": "assets/properties/prop-lrt-05/01_exterior.jpg"
      },
      {
        "kind": "interior",
        "label": "内装（執務エリア・ガラス会議室・LRT通り）",
        "url": "assets/properties/prop-lrt-05/02_interior.jpg",
        "localPath": "assets/properties/prop-lrt-05/02_interior.jpg",
        "index": 1
      }
    ],
    "votesCount": 198,
    "demandScore": 198,
    "topDemands": [
      {
        "subcatId": "I0402",
        "label": "コワーキングスペース",
        "votes": 115,
        "score": 115
      },
      {
        "subcatId": "I0401",
        "label": "学習塾・スクール",
        "votes": 52,
        "score": 52
      },
      {
        "subcatId": "I0101",
        "label": "カフェ・喫茶店",
        "votes": 31,
        "score": 31
      }
    ],
    "sourceImages": [],
    "missingFields": [],
    "fieldEvidence": {},
    "sourceNotes": [],
    "residentVoices": [
      {
        "author": "30代・男性（地域住民）",
        "text": "LRT通勤沿線でドロップイン利用できる電源・Wi-Fi完備のコワーキングスペースが欲しいです。",
        "date": "2026-10-23"
      },
      {
        "author": "40代・男性（地域住民）",
        "text": "新幹線とLRTの両方に近いこの立地で、打ち合わせもできる洗練されたシェアオフィスを希望します。",
        "date": "2026-10-19"
      }
    ],
    "galleryVersion": "2026-10-26-interiors-v1"
  },
  {
    "id": "prop-kamagawa-06",
    "code": "PROP-KAMAGAWA-006",
    "source": {
      "site": "マチリク専任AI査定物件",
      "url": "https://machiriku.city.utsunomiya.local/properties/prop-kamagawa-06",
      "listingId": "prop-kamagawa-06",
      "displayListingNumber": "PROP-KAMAGAWA-006",
      "retrievedAt": "2026-10-26T10:00:00+09:00",
      "listingUpdatedAt": "2026-10-26",
      "listingUpdatedAtRaw": "2026/10/26"
    },
    "title": "釜川プロムナードビル 1階/101",
    "buildingName": "釜川プロムナードビル",
    "unitNumber": "101",
    "propertyType": "賃貸店舗",
    "subtitle": "親水遊歩道テラス店舗 / 1階/3階建 / 46.5㎡",
    "sourceArea": "宇都宮市中央",
    "address": "栃木県宇都宮市中央5丁目1-14",
    "access": "東武宇都宮駅 徒歩7分",
    "rent": 158000,
    "rentRaw": "15.8万円",
    "rentTaxStatus": "税込",
    "managementFee": 12000,
    "deposit": "2ヶ月",
    "keyMoney": "1ヶ月",
    "securityDeposit": "なし",
    "otherFees": [
      {
        "name": "テラス使用承諾費",
        "amountRaw": "賃料に含む"
      },
      {
        "name": "仲介手数料",
        "amountRaw": "賃料1ヶ月分＋税"
      }
    ],
    "areaTsubo": 14.07,
    "areaSqm": 46.5,
    "areaTsuboBasis": "calculated (46.5 sqm / 3.305785)",
    "floor": "1階/3階建",
    "structure": "RC(鉄筋コンクリート造)",
    "builtYear": "2010年5月",
    "handover": "相談",
    "listingStatusRaw": "空き店舗（元フレンチビストロ居抜き）",
    "condition": "元ビストロ・カフェ居抜き（テラス席、タイル厨房カウンター、ダクト）",
    "permittedUses": [
      "レストラン",
      "カフェ・喫茶店",
      "ワインバー・ビストロ",
      "ベーカリー",
      "スイーツ"
    ],
    "facilities": [
      "釜川沿いのせせらぎ遊歩道直結（宇都宮のオアシス空間）",
      "ウッドデッキ調の専用親水テラススペース",
      "赤レンガタイル調の温もりある外観",
      "オープンキッチン・グリストラップ・給排水",
      "店舗専用トイレ",
      "個別空調",
      "ガス・水道引き込み済み"
    ],
    "description": "宇都宮市民の憩いの場である「釜川プロムナード」に面した希少な1階リバーサイド店舗です。川沿いのシダレヤナギの緑と水辺のせせらぎを眺めながら食事ができる専用テラスが特徴。元人気フレンチビストロの居抜きで、オープンキッチンカウンターと客席の動線が機能的。春の桜や新緑の季節には絶景のロケーションとなり、テラスカフェやワインビストロに抜群の人気を誇ります。",
    "broker": {
      "name": "釜川リバーサイドエージェント",
      "listingReference": "MCH-KAMA-2026-06"
    },
    "status": "listed",
    "statusLabel": "募集中",
    "isPr": false,
    "ownerId": "owner_utsunomiya_06",
    "ownerName": "釜川水辺都市開発",
    "location": {
      "lat": 36.557218,
      "lng": 139.886241,
      "accuracy": "pinpoint",
      "sourceUrl": "https://machiriku.city.utsunomiya.local/properties/prop-kamagawa-06"
    },
    "mapCoords": {
      "x": 480,
      "y": 310,
      "lat": 36.557218,
      "lng": 139.886241,
      "isApproximate": false
    },
    "isOutOfMap": false,
    "coverImage": "assets/properties/prop-kamagawa-06/01_exterior.jpg",
    "imageCaption": "外観 / 釜川遊歩道直結・親水テラス付き路面店",
    "gallery": [
      {
        "index": 0,
        "kind": "exterior",
        "label": "外観 / 釜川遊歩道直結・親水テラス付き路面店",
        "url": "assets/properties/prop-kamagawa-06/01_exterior.jpg",
        "localPath": "assets/properties/prop-kamagawa-06/01_exterior.jpg"
      },
      {
        "kind": "interior",
        "label": "内装（釜川沿いの客席・テラスドア）",
        "url": "assets/properties/prop-kamagawa-06/02_interior.jpg",
        "localPath": "assets/properties/prop-kamagawa-06/02_interior.jpg",
        "index": 1
      },
      {
        "kind": "facility",
        "label": "設備（オープンキッチン・シンク・コンロ）",
        "url": "assets/properties/prop-kamagawa-06/03_facility.jpg",
        "localPath": "assets/properties/prop-kamagawa-06/03_facility.jpg",
        "index": 2
      }
    ],
    "votesCount": 278,
    "demandScore": 278,
    "topDemands": [
      {
        "subcatId": "I0101",
        "label": "カフェ・喫茶店",
        "votes": 132,
        "score": 132
      },
      {
        "subcatId": "I0102",
        "label": "レストラン",
        "votes": 95,
        "score": 95
      },
      {
        "subcatId": "I0105",
        "label": "居酒屋・バー",
        "votes": 51,
        "score": 51
      }
    ],
    "sourceImages": [],
    "missingFields": [],
    "fieldEvidence": {},
    "sourceNotes": [],
    "residentVoices": [
      {
        "author": "50代・女性（地域住民）",
        "text": "釜川の景色を見ながら美味しいランチやお茶を楽しめるテラスカフェが復活してくれたら嬉しいです。",
        "date": "2026-10-24"
      },
      {
        "author": "30代・男性（地域住民）",
        "text": "夜にふらっと寄ってナチュールワインと小皿料理が楽しめる大人のビストロを期待しています。",
        "date": "2026-10-22"
      }
    ],
    "galleryVersion": "2026-10-26-interiors-v1"
  },
  {
    "id": "prop-bamba-07",
    "code": "PROP-BAMBA-007",
    "source": {
      "site": "マチリク専任AI査定物件",
      "url": "https://machiriku.city.utsunomiya.local/properties/prop-bamba-07",
      "listingId": "prop-bamba-07",
      "displayListingNumber": "PROP-BAMBA-007",
      "retrievedAt": "2026-10-26T10:00:00+09:00",
      "listingUpdatedAt": "2026-10-26",
      "listingUpdatedAtRaw": "2026/10/26"
    },
    "title": "バンバスクエア 1階/103",
    "buildingName": "バンバスクエア",
    "unitNumber": "103",
    "propertyType": "賃貸店舗",
    "subtitle": "門前町和モダン店舗 / 1階/3階建 / 49.6㎡",
    "sourceArea": "宇都宮市馬場通り",
    "address": "栃木県宇都宮市馬場通り1丁目3-6",
    "access": "東武宇都宮駅 徒歩6分",
    "rent": 165000,
    "rentRaw": "16.5万円",
    "rentTaxStatus": "税込",
    "managementFee": 10000,
    "deposit": "2ヶ月",
    "keyMoney": "1ヶ月",
    "securityDeposit": "なし",
    "otherFees": [
      {
        "name": "門前通り振興会費",
        "amountRaw": "月額2,500円"
      },
      {
        "name": "仲介手数料",
        "amountRaw": "賃料1ヶ月分＋税"
      }
    ],
    "areaTsubo": 15,
    "areaSqm": 49.6,
    "areaTsuboBasis": "calculated (49.6 sqm / 3.305785)",
    "floor": "1階/3階建",
    "structure": "鉄骨造",
    "builtYear": "2014年8月",
    "handover": "相談（即時可）",
    "listingStatusRaw": "空き店舗（元和菓子甘味処居抜き）",
    "condition": "元和カフェ・甘味処居抜き（木製格子戸、小上がり畳、茶房カウンター）",
    "permittedUses": [
      "和菓子・甘味処",
      "日本茶カフェ",
      "定食・和食",
      "工芸品・お土産",
      "着物・和小物"
    ],
    "facilities": [
      "宇都宮の総鎮守「二荒山神社」門前町・バンバ通りの石畳沿い",
      "落ち着きのある無垢材格子戸ファサード・暖簾掛け",
      "小上がり畳スペースと無垢一枚板風カウンター",
      "茶房用給排水・ミニ厨房・給湯器",
      "和モダン手洗い洗面・専用トイレ",
      "都市ガス・個別エアコン",
      "参拝客・観光客の自然な流入動線"
    ],
    "description": "宇都宮の歴史の中心「二荒山神社」の門前町・バンバ通りに面した和モダンの貸店舗です。石畳の歩道に調和する美しい木製格子戸と暖簾掛けフックが特徴。元和菓子・甘味処の居抜きで、白木とダークブラウンの無垢材カウンター、畳敷きの小上がり席、和紙シェードの照明が整備されており、日本茶カフェ、和菓子・甘味処、うどん・蕎麦店、伝統工芸品店等にそのまま活用できます。",
    "broker": {
      "name": "宇都宮門前町不動産",
      "listingReference": "MCH-BAMBA-2026-07"
    },
    "status": "listed",
    "statusLabel": "募集中",
    "isPr": false,
    "ownerId": "owner_utsunomiya_07",
    "ownerName": "馬場通り資産管理合同会社",
    "location": {
      "lat": 36.561532,
      "lng": 139.885814,
      "accuracy": "pinpoint",
      "sourceUrl": "https://machiriku.city.utsunomiya.local/properties/prop-bamba-07"
    },
    "mapCoords": {
      "x": 440,
      "y": 220,
      "lat": 36.561532,
      "lng": 139.885814,
      "isApproximate": false
    },
    "isOutOfMap": false,
    "coverImage": "assets/properties/prop-bamba-07/01_exterior.jpg",
    "imageCaption": "外観 / 二荒山神社前バンバ通り・和モダン木製格子戸",
    "gallery": [
      {
        "index": 0,
        "kind": "exterior",
        "label": "外観 / 二荒山神社前バンバ通り・和モダン木製格子戸",
        "url": "assets/properties/prop-bamba-07/01_exterior.jpg",
        "localPath": "assets/properties/prop-bamba-07/01_exterior.jpg"
      },
      {
        "kind": "interior",
        "label": "内装（木製格子・白木カウンター・畳小上がり）",
        "url": "assets/properties/prop-bamba-07/02_interior.jpg",
        "localPath": "assets/properties/prop-bamba-07/02_interior.jpg",
        "index": 1
      }
    ],
    "votesCount": 295,
    "demandScore": 295,
    "topDemands": [
      {
        "subcatId": "I0101",
        "label": "カフェ・喫茶店",
        "votes": 142,
        "score": 142
      },
      {
        "subcatId": "I0107",
        "label": "パン屋・ベーカリー",
        "votes": 88,
        "score": 88
      },
      {
        "subcatId": "I0503",
        "label": "文化・体験教室",
        "votes": 65,
        "score": 65
      }
    ],
    "sourceImages": [],
    "missingFields": [],
    "fieldEvidence": {},
    "sourceNotes": [],
    "residentVoices": [
      {
        "author": "50代・女性（地域住民）",
        "text": "お参りの帰りにお抹茶や季節の和菓子を楽しめる上品な甘味処があると本当に嬉しいです。",
        "date": "2026-10-25"
      },
      {
        "author": "30代・男性（地域住民）",
        "text": "宇都宮の伝統や食文化を発信するような、こだわりの和カフェやテイクアウト店を期待しています。",
        "date": "2026-10-20"
      }
    ],
    "galleryVersion": "2026-10-26-interiors-v1"
  },
  {
    "id": "prop-stone-08",
    "code": "PROP-STONE-008",
    "source": {
      "site": "マチリク専任AI査定物件",
      "url": "https://machiriku.city.utsunomiya.local/properties/prop-stone-08",
      "listingId": "prop-stone-08",
      "displayListingNumber": "PROP-STONE-008",
      "retrievedAt": "2026-10-26T10:00:00+09:00",
      "listingUpdatedAt": "2026-10-26",
      "listingUpdatedAtRaw": "2026/10/26"
    },
    "title": "南宇都宮大谷石スタジオ・蔵 1階",
    "buildingName": "南宇都宮大谷石蔵スタジオ",
    "unitNumber": "1階",
    "propertyType": "賃貸店舗事務所",
    "subtitle": "大谷石蔵リノベ物件 / 1階/1階建 / 57.2㎡",
    "sourceArea": "宇都宮市吉野",
    "address": "栃木県宇都宮市吉野2丁目6-15",
    "access": "南宇都宮駅 徒歩3分",
    "rent": 135000,
    "rentRaw": "13.5万円",
    "rentTaxStatus": "税込",
    "managementFee": 8000,
    "deposit": "2ヶ月",
    "keyMoney": "1ヶ月",
    "securityDeposit": "なし",
    "otherFees": [
      {
        "name": "敷地内駐車場1台",
        "amountRaw": "月額6,000円"
      },
      {
        "name": "仲介手数料",
        "amountRaw": "賃料1ヶ月分＋税"
      }
    ],
    "areaTsubo": 17.3,
    "areaSqm": 57.2,
    "areaTsuboBasis": "calculated (57.2 sqm / 3.305785)",
    "floor": "1階/1階建",
    "structure": "大谷石造・木造平屋",
    "builtYear": "1952年築（2020年耐震・設備リノベーション済）",
    "handover": "即時",
    "listingStatusRaw": "空室（大谷石蔵リノベスタジオ）",
    "condition": "大谷石現し内装、モルタル土間床、天井高4.0m、耐震補強済",
    "permittedUses": [
      "クリエイティブスタジオ",
      "アートギャラリー",
      "工房・クラフト体験教室",
      "アンティーク・雑貨",
      "カフェ・アトリエ"
    ],
    "facilities": [
      "宇都宮の文化遺産「大谷石造り」の歴史的蔵建築",
      "2020年耐震改修・水回り設備完全刷新済み",
      "天井高約4.0mの木造トラス大空間",
      "作品展示用スポットライトライティングレール",
      "新品水洗トイレ・給排水シンク完備",
      "エアコン完備（大谷石の高い蓄熱・調湿効果あり）",
      "敷地内専用駐車場あり"
    ],
    "description": "南宇都宮駅前の大谷石蔵群エリアに位置する、本物の大谷石造りの蔵をリノベーションした唯一無二の貸テナントです。大谷石特有の温もりあるテクスチャーと高い調湿性・遮音性を備え、4m超の木造トラス天井が圧倒的な存在感を放ちます。2020年に耐震補強と水回り設備の全面更新を完了。陶芸・木工などのクラフト工房、現代アートギャラリー、写真スタジオ、隠れ家カフェに最適です。",
    "broker": {
      "name": "大谷石ヘリテージ不動産",
      "listingReference": "MCH-STONE-2026-08"
    },
    "status": "listed",
    "statusLabel": "募集中",
    "isPr": false,
    "ownerId": "owner_utsunomiya_08",
    "ownerName": "吉野大谷石保存会",
    "location": {
      "lat": 36.541245,
      "lng": 139.872314,
      "accuracy": "pinpoint",
      "sourceUrl": "https://machiriku.city.utsunomiya.local/properties/prop-stone-08"
    },
    "mapCoords": {
      "x": null,
      "y": null,
      "lat": 36.541245,
      "lng": 139.872314,
      "isApproximate": true
    },
    "isOutOfMap": true,
    "coverImage": "assets/properties/prop-stone-08/01_exterior.jpg",
    "imageCaption": "外観 / 南宇都宮・歴史ある大谷石造りの蔵",
    "gallery": [
      {
        "index": 0,
        "kind": "exterior",
        "label": "外観 / 南宇都宮・歴史ある大谷石造りの蔵",
        "url": "assets/properties/prop-stone-08/01_exterior.jpg",
        "localPath": "assets/properties/prop-stone-08/01_exterior.jpg"
      },
      {
        "kind": "interior",
        "label": "内装（大谷石壁・木造トラス・展示スペース）",
        "url": "assets/properties/prop-stone-08/02_interior.jpg",
        "localPath": "assets/properties/prop-stone-08/02_interior.jpg",
        "index": 1
      },
      {
        "kind": "interior",
        "label": "内装（入口側からの採光・展示スペース別アングル）",
        "url": "assets/properties/prop-stone-08/03_interior.jpg",
        "localPath": "assets/properties/prop-stone-08/03_interior.jpg",
        "index": 2
      },
      {
        "kind": "facility",
        "label": "設備（作業台・収納棚・展示用スポットライト）",
        "url": "assets/properties/prop-stone-08/04_facility.jpg",
        "localPath": "assets/properties/prop-stone-08/04_facility.jpg",
        "index": 3
      }
    ],
    "votesCount": 185,
    "demandScore": 185,
    "topDemands": [
      {
        "subcatId": "I0503",
        "label": "文化・体験教室",
        "votes": 92,
        "score": 92
      },
      {
        "subcatId": "I0203",
        "label": "雑貨・生活用品店",
        "votes": 56,
        "score": 56
      },
      {
        "subcatId": "I0101",
        "label": "カフェ・喫茶店",
        "votes": 37,
        "score": 37
      }
    ],
    "sourceImages": [],
    "missingFields": [],
    "fieldEvidence": {},
    "sourceNotes": [],
    "residentVoices": [
      {
        "author": "30代・女性（地域住民）",
        "text": "大谷石の蔵の空間は本当に特別。ワークショップや展示ができるギャラリースタジオができると文化の発信地になります！",
        "date": "2026-10-23"
      },
      {
        "author": "40代・男性（地域住民）",
        "text": "南宇都宮の石蔵文化を守りつつ、休日に行きたくなるような体験工房やカフェに期待しています。",
        "date": "2026-10-18"
      }
    ],
    "galleryVersion": "2026-10-26-interiors-v1"
  },
  {
    "id": "prop-tomatsuri-09",
    "code": "PROP-TOMATSURI-009",
    "source": {
      "site": "マチリク専任AI査定物件",
      "url": "https://machiriku.city.utsunomiya.local/properties/prop-tomatsuri-09",
      "listingId": "prop-tomatsuri-09",
      "displayListingNumber": "PROP-TOMATSURI-009",
      "retrievedAt": "2026-10-26T10:00:00+09:00",
      "listingUpdatedAt": "2026-10-26",
      "listingUpdatedAtRaw": "2026/10/26"
    },
    "title": "戸祭競輪場通りサニープレイス 1階/102",
    "buildingName": "戸祭競輪場通りサニープレイス",
    "unitNumber": "102",
    "propertyType": "賃貸店舗",
    "subtitle": "ロードサイドサロン / 1階/2階建 / 78.0㎡",
    "sourceArea": "宇都宮市中戸祭町",
    "address": "栃木県宇都宮市中戸祭町825-4",
    "access": "東武宇都宮駅 徒歩28分",
    "rent": 210000,
    "rentRaw": "21.0万円",
    "rentTaxStatus": "税込",
    "managementFee": 15000,
    "deposit": "2ヶ月",
    "keyMoney": "1ヶ月",
    "securityDeposit": "なし",
    "otherFees": [
      {
        "name": "専用駐車場3台分",
        "amountRaw": "賃料に含む"
      },
      {
        "name": "仲介手数料",
        "amountRaw": "賃料1ヶ月分＋税"
      }
    ],
    "areaTsubo": 23.6,
    "areaSqm": 78,
    "areaTsuboBasis": "calculated (78.0 sqm / 3.305785)",
    "floor": "1階/2階建",
    "structure": "鉄骨造",
    "builtYear": "2017年3月",
    "handover": "相談（即時可）",
    "listingStatusRaw": "空き店舗（元美容室ヘアサロン居抜き）",
    "condition": "元ヘアサロン居抜き（セット面4席配線、シャンプー配管2台分、ボイラー残置）",
    "permittedUses": [
      "美容室・ヘアサロン",
      "理容室・バーバー",
      "ネイル・エステサロン",
      "ジム・フィットネススタジオ",
      "整体・整骨院"
    ],
    "facilities": [
      "宇都宮市北部の主要幹線道路「競輪場通り」沿い好立地",
      "店舗前面に専用無料駐車場3台分完備（車社会対応）",
      "白を基調とした明るく開放的なガラス張りファサード",
      "大型ガス給湯器・サロン用給排水配管残置",
      "セット面用床下配線・スポット照明",
      "スタッフルーム・洗濯機パン・専用トイレ",
      "個別天井カセット型エアコン2基"
    ],
    "description": "宇都宮市の人気住宅エリア・戸祭地区を通る幹線「競輪場通り」沿いのロードサイド店舗です。店舗正面に専用駐車場3台分を備えており、マイカー来客に完全対応。白を基調とした爽やかなファサードと大判ガラス面で車通りからの視認性も抜群です。元人気ヘアサロンの居抜きで、セット面配線、シャンプー台配管、大型給湯ボイラー、スタッフルームが残置されており、サロン開業コストを劇的に圧縮できます。",
    "broker": {
      "name": "栃木ロードサイドテナントセンター",
      "listingReference": "MCH-TOMAT-2026-09"
    },
    "status": "listed",
    "statusLabel": "募集中",
    "isPr": false,
    "ownerId": "owner_utsunomiya_09",
    "ownerName": "戸祭エステート開発",
    "location": {
      "lat": 36.578412,
      "lng": 139.878523,
      "accuracy": "pinpoint",
      "sourceUrl": "https://machiriku.city.utsunomiya.local/properties/prop-tomatsuri-09"
    },
    "mapCoords": {
      "x": null,
      "y": null,
      "lat": 36.578412,
      "lng": 139.878523,
      "isApproximate": true
    },
    "isOutOfMap": true,
    "coverImage": "assets/properties/prop-tomatsuri-09/01_exterior.jpg",
    "imageCaption": "外観 / 競輪場通りロードサイド・白基調店舗と専用駐車場",
    "gallery": [
      {
        "index": 0,
        "kind": "exterior",
        "label": "外観 / 競輪場通りロードサイド・白基調店舗と専用駐車場",
        "url": "assets/properties/prop-tomatsuri-09/01_exterior.jpg",
        "localPath": "assets/properties/prop-tomatsuri-09/01_exterior.jpg"
      },
      {
        "kind": "interior",
        "label": "内装（セット面・ミラー・シャンプーブース）",
        "url": "assets/properties/prop-tomatsuri-09/02_interior.jpg",
        "localPath": "assets/properties/prop-tomatsuri-09/02_interior.jpg",
        "index": 1
      }
    ],
    "votesCount": 210,
    "demandScore": 210,
    "topDemands": [
      {
        "subcatId": "I0301",
        "label": "美容室・ヘアサロン",
        "votes": 124,
        "score": 124
      },
      {
        "subcatId": "I0304",
        "label": "ジム・フィットネススタジオ",
        "votes": 54,
        "score": 54
      },
      {
        "subcatId": "I0303",
        "label": "ネイル・エステサロン",
        "votes": 32,
        "score": 32
      }
    ],
    "sourceImages": [],
    "missingFields": [],
    "fieldEvidence": {},
    "sourceNotes": [],
    "residentVoices": [
      {
        "author": "30代・女性（地域住民）",
        "text": "駐車場があって通いやすい美容室やネイルサロンが欲しいです。平日の昼間にサッと行けるお店を希望！",
        "date": "2026-10-24"
      },
      {
        "author": "40代・男性（地域住民）",
        "text": "仕事帰りや週末に寄れるパーソナルジムや整体院ができると健康のために通いたいです。",
        "date": "2026-10-20"
      }
    ],
    "galleryVersion": "2026-10-26-interiors-v1"
  },
  {
    "id": "prop-miyukigahara-10",
    "code": "PROP-MIYUKIGAHARA-010",
    "source": {
      "site": "マチリク専任AI査定物件",
      "url": "https://machiriku.city.utsunomiya.local/properties/prop-miyukigahara-10",
      "listingId": "prop-miyukigahara-10",
      "displayListingNumber": "PROP-MIYUKIGAHARA-010",
      "retrievedAt": "2026-10-26T10:00:00+09:00",
      "listingUpdatedAt": "2026-10-26",
      "listingUpdatedAtRaw": "2026/10/26"
    },
    "title": "御幸ケ原コミュニティプラザ 1階/101",
    "buildingName": "御幸ケ原コミュニティプラザ",
    "unitNumber": "101",
    "propertyType": "賃貸店舗事務所",
    "subtitle": "郊外タウン複合店舗 / 1階/2階建 / 80.5㎡",
    "sourceArea": "宇都宮市御幸ケ原町",
    "address": "栃木県宇都宮市御幸ケ原町142-8",
    "access": "岡本駅 徒歩18分",
    "rent": 182000,
    "rentRaw": "18.2万円",
    "rentTaxStatus": "税込",
    "managementFee": 18000,
    "deposit": "2ヶ月",
    "keyMoney": "1ヶ月",
    "securityDeposit": "なし",
    "otherFees": [
      {
        "name": "共用駐車場10台分利用権",
        "amountRaw": "共益費に含む"
      },
      {
        "name": "仲介手数料",
        "amountRaw": "賃料1ヶ月分＋税"
      }
    ],
    "areaTsubo": 24.35,
    "areaSqm": 80.5,
    "areaTsuboBasis": "calculated (80.5 sqm / 3.305785)",
    "floor": "1階/2階建",
    "structure": "RC(鉄筋コンクリート造)",
    "builtYear": "2013年10月",
    "handover": "即時",
    "listingStatusRaw": "空室（元学習塾・スクール居抜き）",
    "condition": "元個別指導学習塾居抜き（個別ブース・ホワイトボード・教卓・OA配線完備）",
    "permittedUses": [
      "学習塾・スクール",
      "文化・体験教室",
      "小児・一般クリニック",
      "デイサービス・福祉",
      "プログラミング教室"
    ],
    "facilities": [
      "御幸ケ原の大型ファミリー住宅地中心部・生活道路沿い",
      "敷地内共用大型駐車場10台以上完備・駐輪場あり",
      "個別学習指導ブース・机・椅子・ホワイトボード残置",
      "共用男女別トイレ・洗面所",
      "個別空調エアコン2系統・LED照明",
      "バリアフリー対応エントランス",
      "即時開業可能"
    ],
    "description": "宇都宮市東部の成熟したファミリーベッドタウン「御幸ケ原町」の複合コミュニティビル1階テナントです。前面に10台以上駐車可能な共用駐車場を備えており、近隣小中学校からのアクセスも良好。元大手個別指導学習塾の居抜きで、個別ブースやホワイトボード、面談コーナー、事務スペースがそのまま活用できます。学習塾、英語・プログラミングスクール、小児科クリニック、福祉施設等に最適です。",
    "broker": {
      "name": "御幸都市開発エステート",
      "listingReference": "MCH-MIYUKI-2026-10"
    },
    "status": "listed",
    "statusLabel": "募集中",
    "isPr": false,
    "ownerId": "owner_utsunomiya_10",
    "ownerName": "御幸ケ原タウンマネジメント",
    "location": {
      "lat": 36.586231,
      "lng": 139.919542,
      "accuracy": "pinpoint",
      "sourceUrl": "https://machiriku.city.utsunomiya.local/properties/prop-miyukigahara-10"
    },
    "mapCoords": {
      "x": null,
      "y": null,
      "lat": 36.586231,
      "lng": 139.919542,
      "isApproximate": true
    },
    "isOutOfMap": true,
    "coverImage": "assets/properties/prop-miyukigahara-10/01_exterior.jpg",
    "imageCaption": "外観 / 御幸ケ原タウンセンター・1階共用駐車場付き",
    "gallery": [
      {
        "index": 0,
        "kind": "exterior",
        "label": "外観 / 御幸ケ原タウンセンター・1階共用駐車場付き",
        "url": "assets/properties/prop-miyukigahara-10/01_exterior.jpg",
        "localPath": "assets/properties/prop-miyukigahara-10/01_exterior.jpg"
      },
      {
        "kind": "interior",
        "label": "内装（個別学習ブース・ホワイトボード）",
        "url": "assets/properties/prop-miyukigahara-10/02_interior.jpg",
        "localPath": "assets/properties/prop-miyukigahara-10/02_interior.jpg",
        "index": 1
      },
      {
        "kind": "interior",
        "label": "内装（教材棚から見た教室・窓側の採光）",
        "url": "assets/properties/prop-miyukigahara-10/03_interior.jpg",
        "localPath": "assets/properties/prop-miyukigahara-10/03_interior.jpg",
        "index": 2
      }
    ],
    "votesCount": 176,
    "demandScore": 176,
    "topDemands": [
      {
        "subcatId": "I0401",
        "label": "学習塾・スクール",
        "votes": 108,
        "score": 108
      },
      {
        "subcatId": "I0701",
        "label": "クリニック",
        "votes": 42,
        "score": 42
      },
      {
        "subcatId": "I0503",
        "label": "文化・体験教室",
        "votes": 26,
        "score": 26
      }
    ],
    "sourceImages": [],
    "missingFields": [],
    "fieldEvidence": {},
    "sourceNotes": [],
    "residentVoices": [
      {
        "author": "40代・女性（地域住民）",
        "text": "子供が安全に通えるプログラミングや英語のスクールが徒歩圏内にできたらぜひ通わせたいです。",
        "date": "2026-10-22"
      },
      {
        "author": "30代・男性（地域住民）",
        "text": "駐車場が広くて送り迎えがしやすい学習塾や習い事教室がこの地域には必要です。",
        "date": "2026-10-18"
      }
    ],
    "galleryVersion": "2026-10-26-interiors-v1"
  }
];

  const USERS = {
    resident: {
      id: "usr-res-01",
      name: "田中 一郎",
      role: "resident",
      roleName: "地域住民",
      email: "tanaka.ichiro@example.com",
      phone: "090-1234-5678",
      livingArea: "宇都宮市江野町",
      livingDistanceKm: 0.4,
      followedProps: [],
      votedProps: {},
      receivedCoupons: [],
      answeredSurveys: []
    },
    business: {
      id: "usr-biz-01",
      name: "鈴木 健太",
      role: "business",
      roleName: "出店事業主",
      brand: "日光焙煎珈琲スタンド / 釜川クラフトピッツァ",
      company: "合同会社日光珈琲舎",
      email: "suzuki.kenta@nikko-coffee.jp",
      phone: "",
      experienceYears: "飲食業界歴12年（うち店舗統括マネージャー5年）",
      qualifications: [
        { name: "調理師免許", number: "第12458号 (栃木県知事)", verified: true },
        { name: "食品衛生責任者", number: "東食第8823号", verified: true },
        { name: "防火管理者（甲種）", number: "宇消第1044号", verified: true }
      ],
      story: "栃木県日光市出身。都内のスペシャルティコーヒー専門店やイタリアンバルで12年修行を積み、地元栃木の魅力を食と空間で発信したいとUターンしました。単なる飲食店にとどまらず、地域の生産者と街の人々が日常的に交差する『サードプレイス』を創出することを目指しています。",
      avgSatisfaction: 0,
      reviewCount: 0,
      activeSurveys: [],
      activeCoupons: []
    },
    owner: {
      id: "usr-owner-01",
      name: "佐藤 武雄",
      role: "owner",
      roleName: "物件オーナー",
      email: "sato.takeo@sato-fudosan.co.jp",
      phone: "",
      company: "佐藤不動産管理（個人オーナー）",
      ownedPropIds: [],
      activePrPropIds: []
    },
    admin: {
      id: "usr-admin-01",
      name: "マチリク事務局管理者",
      role: "admin",
      roleName: "運営管理者",
      email: "admin@machiriku.utsunomiya.jp"
    }
  };

  // HOME'S supplies listing facts, not applications, reviews, owners or coupons.
  const INITIAL_APPLICATIONS = [];
  const INITIAL_SURVEYS = [];
  const INITIAL_COUPONS = [];

  // 全33画面仕様定義（レビュー画面S20およびルーター制御用）
  const SCREENS_SPEC = [
    { id: "s01", num: "S01", name: "物件検索・地図・需要", role: "all", roleLabel: "全員", desc: "空き店舗を一覧・地図から探す。需要ランキング表示とアットホーム風カード・Mapbox風地図連携", refExcel: "Row 4 (S01 / home)" },
    { id: "s02", num: "S02", name: "物件詳細", role: "all", roleLabel: "全員", desc: "物件詳細情報、設備、Decidim風住民需要分析、投票・出店応募への導線", refExcel: "Row 5 (S02 / property/detail)" },
    { id: "s03", num: "S03", name: "順位投票・任意アンケート", role: "resident", roleLabel: "地域住民", desc: "第1〜第3希望の順位投票（1.0/0.6/0.3pt係数・距離減衰）、利用意向アンケート", refExcel: "Row 9 (R01 投票)" },
    { id: "s04", num: "S04", name: "フォロー物件一覧", role: "resident", roleLabel: "地域住民", desc: "クリップした物件の状況・出店進捗ステータス確認", refExcel: "企画書 A85-89" },
    { id: "s05", num: "S05", name: "出店計画・進捗閲覧", role: "all", roleLabel: "全員", desc: "決定した事業主のストーリー、開店までのオープンタイムライン・進捗公開", refExcel: "企画書 A49-50 / Decidim" },
    { id: "s06", num: "S06", name: "通知一覧", role: "all", roleLabel: "全員", desc: "投票物件の進捗通知、クーポン獲得通知、応募審査状況の案内", refExcel: "企画書 A31/A87" },
    { id: "s07", num: "S07", name: "物件・地域の需要分析", role: "business_owner", roleLabel: "事業主・オーナー", desc: "shadcn/ui風メトリクス・7大分類別集計グラフ・住民生の声データ", refExcel: "Row 13/15 (B03/O02 需要分析)" },
    { id: "s08", num: "S08", name: "出店申込・資格確認", role: "business", roleLabel: "出店事業主", desc: "想い・ストーリー、事業計画、調理師・食品衛生等の資格確認ステップフォーム", refExcel: "Row 11 (B01 出店応募)" },
    { id: "s09", num: "S09", name: "出店申込一覧", role: "owner_business", roleLabel: "オーナー・事業主", desc: "応募事業主の一覧テーブル・審査進捗ステータス管理", refExcel: "Row 12 (B02 応募状況確認)" },
    { id: "s10", num: "S10", name: "申込詳細・可否判断", role: "owner_business", roleLabel: "オーナー・事業主", desc: "応募者の想い・計画・実績評価確認、面談・承認・合意アクション", refExcel: "Row 16 (O03 出店希望確認)" },
    { id: "s11", num: "S11", name: "登録物件管理", role: "owner", roleLabel: "物件オーナー", desc: "所有物件のステータス管理、応募状況、需要サマリー、PR枠申込導線", refExcel: "Row 14 (O01 登録物件管理)" },
    { id: "s12", num: "S12", name: "物件登録・編集", role: "owner", roleLabel: "物件オーナー", desc: "賃料、面積、設備チェック、写真アップロード、掲載申請フォーム", refExcel: "Row 17/19 (O04/O06 物件登録・編集)" },
    { id: "s13", num: "S13", name: "事業主へのオファー", role: "owner", roleLabel: "物件オーナー", desc: "オーナーから希望業種の事業主へ直接出店を打診するメッセージ送信", refExcel: "企画書 A32-35" },
    { id: "s14", num: "S14", name: "契約成立・物件引渡し", role: "owner_business", roleLabel: "オーナー・事業主", desc: "賃貸借契約締結記録、引渡し完了、開店準備フェーズへの移行", refExcel: "企画書 A30/D35-38" },
    { id: "s15", num: "S15", name: "出店計画の作成・公開", role: "business", roleLabel: "出店事業主", desc: "オープン予定日、店舗名、内装写真、住民への開店メッセージ作成・公開", refExcel: "企画書 A49-50" },
    { id: "s16", num: "S16", name: "ログイン", role: "all", roleLabel: "全員", desc: "利用者認証（ワンクリックでのデモロール切り替えログイン機能付き）", refExcel: "Row 6 (S03 / auth/login)" },
    { id: "s17", num: "S17", name: "種別別の会員登録", role: "all", roleLabel: "全員", desc: "住民・事業主・オーナー別の登録フォーム、formrun風ステップUI", refExcel: "Row 7 (S04 / auth/register)" },
    { id: "s18", num: "S18", name: "プロフィール・経験・資格編集", role: "all", roleLabel: "全員", desc: "事業主の修行経験年数・保有資格証、住民の生活エリア設定", refExcel: "企画書 A18-21/A46" },
    { id: "s19", num: "S19", name: "通知・オファー条件設定", role: "all", roleLabel: "全員", desc: "新着物件通知、投票・応募通知、希望エリア・賃料帯のアラート設定", refExcel: "企画書 A31/A69" },
    { id: "s20", num: "S20", name: "画面一覧・仕様対応表", role: "all", roleLabel: "設計レビュー", desc: "全33画面のマッピング一覧、1クリック画面ジャンプ、仕様準拠表", refExcel: "Row 3 (画面一覧表)" },
    { id: "s21", num: "S21", name: "開店支援サービス", role: "business", roleLabel: "出店事業主", desc: "内装設計施工、厨房機器リース、宇都宮市創業補助金等の提携サポート", refExcel: "企画書 A51-68/A178" },
    { id: "s22", num: "S22", name: "来店アンケート管理", role: "business", roleLabel: "出店事業主", desc: "店舗アンケートの公開状態管理、回答数・平均スコア推移", refExcel: "Row 27 (B04 来店アンケート管理)" },
    { id: "s23", num: "S23", name: "来店アンケート作成", role: "business", roleLabel: "出店事業主", desc: "総合満足度(固定必須)＋独自設問作成、テンプレート選択、設問ガイド", refExcel: "Row 28 (B05 来店アンケート作成)" },
    { id: "s24", num: "S24", name: "アンケート公開前確認", role: "business", roleLabel: "出店事業主", desc: "スマホ実画面プレビュー、受付期間・お礼クーポンの設定と確認", refExcel: "企画書 A51-59" },
    { id: "s25", num: "S25", name: "店内掲示用QR", role: "business", roleLabel: "出店事業主", desc: "印刷対応卓上POPレイアウト、高精細QRコード生成、住民回答への直接リンク", refExcel: "Row 29 (B06 店内掲示用QR)" },
    { id: "s26", num: "S26", name: "来店後アンケート回答", role: "resident", roleLabel: "地域住民", desc: "スマホ最適化の回答フォーム、スター評価、回答完了でクーポン付与", refExcel: "Row 30 (R03 来店後アンケート回答)" },
    { id: "s27", num: "S27", name: "来店アンケート集計", role: "business", roleLabel: "出店事業主", desc: "shadcn/ui風集計ダッシュボード、満足度分布、年代別クロス集計、生の声一覧", refExcel: "Row 31 (B07 来店アンケート集計)" },
    { id: "s28", num: "S28", name: "クーポン作成・配布管理", role: "business", roleLabel: "出店事業主", desc: "STORES風クーポン管理テーブル、発行数・利用数・上限コントロール", refExcel: "Row 32 (B08 クーポン作成・配布管理)" },
    { id: "s29", num: "S29", name: "クーポン設定", role: "business", roleLabel: "出店事業主", desc: "種別(割引/値引/サービス)、対象(投票者/アンケート回答者)、上限枚数設定", refExcel: "企画書 A63-68" },
    { id: "s30", num: "S30", name: "受け取ったクーポン", role: "resident", roleLabel: "地域住民", desc: "まちのコイン風ウォレット、獲得済みチケット一覧、利用期限バッジ", refExcel: "Row 33 (R04 受け取ったクーポン)" },
    { id: "s31", num: "S31", name: "クーポン提示・利用", role: "resident", roleLabel: "地域住民", desc: "店頭提示UI、店員消込スライダー、USEDスタンプアニメーション演出", refExcel: "Row 34 (R05 クーポン提示・利用)" },
    { id: "s32", num: "S32", name: "事業主の実績・顧客評価", role: "owner_business", roleLabel: "オーナー・事業主", desc: "さかさま不動産風プロフィール、来店客平均満足度(4.4)、経歴・クチコミ公開", refExcel: "企画書 A46-48/A54" },
    { id: "s33", num: "S33", name: "物件PR広告", role: "owner", roleLabel: "物件オーナー", desc: "オーナー向け収益プラン、検索トップ注目枠への掲載申込・シミュレーション", refExcel: "企画書 A170-176" }
  ];

  return {
    DATASET_VERSION,
    CATEGORIES,
    SUBCATS,
    PROPERTIES,
    USERS,
    INITIAL_APPLICATIONS,
    INITIAL_SURVEYS,
    INITIAL_COUPONS,
    SCREENS_SPEC
  };
})();
