# 地域住民の投票API(マチリク)

投票まわりのAPIは3つです。URLの前半は共通で、`/machiriku/jp/machiriku/servlet/vote/` の後ろが違います。

| API | URL | 使える人 | 用途 |
|---|---|---|---|
| 投票画面の初期表示 | `VoteContext.action` | 住民 | 物件の概要・投票できるか・生活拠点と距離・自分の現在の投票 |
| 業種マスタ | `Industries.action` | 誰でも | 希望業種の選択肢(大分類と小分類) |
| 投票・取消・自分の投票の取得 | `Vote.action` | 住民 | 物件への投票(下記) |
| 投票履歴 | `VoteHistory.action` | 住民 | 自分の投票の一覧(取消済みを含む) |
| 需要の集計 | `Demand.action` | 誰でも(コメントは会員だけ) | 物件別・エリア別の希望業種ランキング |
| 投票数のまとめ取得 | `VoteCounts.action` | 誰でも | 物件一覧に出す投票数と一番人気の業種 |

**投票画面(R01)の流れ**: ①`Industries.action` で選択肢を作る → ②`VoteContext.action?propertyId=N` で物件・拠点・現在の投票を表示 →
③「投票する」で `Vote.action` に POST(`locationId` に選んだ拠点)→ ④取消は `Vote.action` に `withdraw=1`。

以下、まず `Vote.action` の説明です。

URL: `/machiriku/jp/machiriku/servlet/vote/Vote.action`(GET・POST。既存の `*.action` 規約どおり)

ログイン済みの「住民(RESIDENT・ACTIVE)」だけが使えます。応答はすべて JSON(UTF-8)です。
成功: `{"success":true, ...}` / 失敗: `{"success":false,"code":"...","message":"..."}`

## 自分の投票を取得 (GET)
`Vote.action?propertyId=12` → `{"success":true,"vote":null}` または `{"success":true,"vote":{...}}`
(取消済みの投票は `null`)

## 投票・再投票 (POST、ヘッダ `X-Requested-With: XMLHttpRequest` 必須)
パラメータ(application/x-www-form-urlencoded):
- `propertyId` 物件ID(数値)
- `rank1` 第1希望(必須) / `rank2` / `rank3` … `industry_code`(例 `I0101`)または industry_type_id。空欄は未選択。途中を飛ばすのは不可
- `locationId` 距離の基準にする生活拠点(`VoteContext.action` の `locations[].locationId`)。**省略すると、座標のある拠点のうち物件に一番近いもの**。本人の拠点で、座標が登録されている必要がある
- `frequency` 利用頻度(30文字まで) / `comment` コメント(1000文字まで)
- `timing` は DB に列がないため**保存しません**(無視)

応答: `{"success":true,"updated":false,"vote":{voteId,propertyId,withdrawn,distanceKm,distanceScaleKm,frequency,comment,updatedAt,choices:[{rank,industryCode,industryName,rankWeight}],score}}`
`updated` は再投票なら true。距離は住民の生活拠点のうち物件に最も近いもの。

## 取消 (POST)
`propertyId=12&withdraw=1`(同じヘッダ必須) → `{"success":true,"withdrawn":true}`

## エラー
| code | HTTP | 意味 |
|---|---|---|
| UNAUTHENTICATED | 401 | 未ログイン |
| SETUP_REQUIRED / FORBIDDEN / NOT_RESIDENT | 403 | 登録未完了・停止中・住民以外 |
| CSRF | 403 | POST にヘッダがない |
| INVALID_INPUT / BAD_CHOICES / BAD_LOCATION | 400 | 入力不正・希望業種の指定が不正・選んだ拠点が本人のものでない |
| PROPERTY_NOT_FOUND | 404 | 物件がない(削除済み・運営が非表示の物件も同じ) |
| PROPERTY_CLOSED / PROPERTY_NO_LOCATION / NO_LOCATION | 409 | 募集中でない・物件に座標がない・住民の拠点に座標がない(選んだ拠点に座標がない場合も) |
| METHOD_NOT_ALLOWED | 405 | GET/POST 以外 |
| SERVER_ERROR | 500 | DBエラー(詳細は返さない) |

## フロント側の呼び出し例(ブラウザコンソールでも可)
```js
const body = new URLSearchParams({propertyId: 12, rank1: 'I0101', rank2: 'I0201', frequency: '週1回程度', comment: ''});
fetch('/machiriku/jp/machiriku/servlet/vote/Vote.action', {method: 'POST', headers: {'X-Requested-With': 'XMLHttpRequest'}, body}).then(r => r.json()).then(console.log);
```

---

# 投票履歴API `VoteHistory.action`(GETのみ)

ログイン済みの住民(RESIDENT・ACTIVE)が、**自分の**投票を新しい順に取得します。取消済みの投票も残ります。
エラーは `Vote.action` と同じ形式です(401・403・405・500、パラメーター不正は 400 INVALID_INPUT)。

| パラメーター | 既定 | 内容 |
|---|---|---|
| `status` | `all` | `active`(有効)・`withdrawn`(取消済み)・`all` |
| `limit` | 20 | 1〜100 |
| `offset` | 0 | 何件目から取得するか |

```json
{
  "success": true, "total": 3, "limit": 20, "offset": 0, "hasMore": false,
  "votes": [{
    "voteId": 9, "propertyId": 5, "withdrawn": false, "withdrawnAt": null,
    "distanceKm": 0.400, "distanceScaleKm": 1.500, "frequency": "週1回程度", "comment": "…",
    "choices": [{"rank":1, "industryCode":"I0102", "industryName":"レストラン", "rankWeight":1.00}],
    "score": 1.263, "createdAt": "…", "updatedAt": "…",
    "property": {"propertyId":5, "name":"…", "prefecture":"栃木県", "municipality":"宇都宮市",
                 "addressLine":"…", "listingStatus":"OPEN", "deleted": false}
  }]
}
```
- 物件が削除されても履歴には残り、`property.deleted` が `true` になります(名前は表示できます)。
- 再投票・取消は、物件の画面から `Vote.action` で行います。

# 需要の集計API `Demand.action`(GETのみ)

住民の投票から、**希望業種のランキング**を集計して返します。有効な(取消されていない)投票だけが対象で、
スコアは `rank_weight / (1 + distance_km / distance_scale_km)` を業種ごとに合計したものです(定義書「設計補足」と同じ)。
集計用のテーブルはなく、呼ぶたびに計算します。削除済み・運営が非表示にした物件は含みません。

## 物件別 `Demand.action?propertyId=12`
| 任意パラメーター | 既定 | 内容 |
|---|---|---|
| `limit` | 10 | 業種ランキングの件数(1〜50) |
| `commentLimit` | 10 | 要望コメントの件数(0〜30)。**ログイン中の会員にだけ**返す |

```json
{
  "success": true, "scope": "PROPERTY",
  "property": {"propertyId":12, "name":"…", "prefecture":"栃木県", "municipality":"宇都宮市"},
  "propertyCount": 1, "voteCount": 2, "voterCount": 2, "averageDistanceKm": 5.000, "totalScore": 2.400,
  "industries": [{"rank":1, "industryTypeId":10, "industryCode":"I0102", "industryName":"…",
                  "score":1.300, "sharePercent":54.2, "residentCount":2,
                  "firstChoiceCount":1, "secondChoiceCount":1, "thirdChoiceCount":0}],
  "categories": [{"rank":1, "categoryCode":"C01", "categoryName":"飲食", "score":2.400, "sharePercent":100.0, "residentCount":2}],
  "frequencies": [{"frequency":"週1回程度", "count":1}],
  "comments": [{"comment":"…", "frequency":"週1回程度", "votedAt":"…"}],
  "commentsVisible": true, "generatedAt": "…"
}
```
- `sharePercent` は全業種のスコア合計に対する割合(%)です。`limit` で件数を絞っても、割合は全体に対する値のままです。
- `categories`(大分類)の `residentCount` は、同じ住民が同じ大分類の2業種を選んでも **1人** と数えます(定義書どおり)。
- コメントに、投稿した人の名前・会員IDは含まれません。ログインしていないと `comments` は空で、`commentsVisible` が `false` です。
- 物件がない・削除済み・非表示のときは 404 `PROPERTY_NOT_FOUND`。

## エリア別 `Demand.action?municipality=宇都宮市&prefecture=栃木県`
`municipality`・`prefecture` はどちらも省略できます(両方省略すると、掲載中の全物件が対象)。
`limit` に加えて、`topProperties`(需要の高い物件の件数。既定5、0〜20)を指定できます。

応答は物件別とほぼ同じ形で、`scope` が `AREA`、`property` の代わりに `area`、`comments` の代わりに次が付きます。
```json
"topProperties": [{"rank":1, "propertyId":1, "name":"…", "municipality":"宇都宮市", "voteCount":2, "score":2.400, "topIndustryName":"カフェ"}]
```
- 対象は、掲載中(OPEN)で、削除・非表示でない物件です。
- `voterCount` は、複数の物件に投票した住民も **1人** と数えます。
- 投票が1件もないときは、`voteCount` が 0、`industries` が空配列、`averageDistanceKm` が `null` です。

## 画面からの呼び出し例
```js
fetch('/machiriku/jp/machiriku/servlet/vote/Demand.action?propertyId=12').then(r => r.json()).then(console.log);
fetch('/machiriku/jp/machiriku/servlet/vote/Demand.action?municipality=' + encodeURIComponent('宇都宮市')).then(r => r.json()).then(console.log);
fetch('/machiriku/jp/machiriku/servlet/vote/VoteHistory.action?status=active').then(r => r.json()).then(console.log);
```

## 画面側でのつなぎ込みについて
今の画面(`property/detail.jsp`、`property/analytics.jsp`、`js/data.js`)は、固定のダミーの数字で表示しています。
ここのAPIに差し替えるときの対応は、おおよそ次のとおりです。
- `industries[].industryName`・`score`・`sharePercent` → 「希望業種ランキング」
- `voteCount` → 「投票数」 / `categories` → 大分類の内訳 / `comments` → 「住民の生の声」
- `topProperties` → エリアの需要分析

---

# 投票画面の初期表示 `VoteContext.action?propertyId=N`(GETのみ)

ログイン済みの住民が、投票画面(R01)を開くときに**1回**呼びます。
```json
{
  "success": true,
  "property": {"propertyId":7, "name":"…", "prefecture":"栃木県", "municipality":"宇都宮市", "addressLine":"…", "listingStatus":"OPEN", "distanceScaleKm":10.000},
  "votable": true, "reason": null, "reasonMessage": null, "voteCount": 2,
  "locations": [
    {"locationId":1, "kind":"HOME", "label":"自宅", "prefecture":"…", "municipality":"…", "addressLine":"…", "hasCoordinates":true, "distanceKm":2.769, "nearest":false},
    {"locationId":4, "kind":"WORK", "label":"勤務先", "hasCoordinates":true, "distanceKm":0.014, "nearest":true},
    {"locationId":5, "kind":"SCHOOL", "label":"学校", "hasCoordinates":false, "distanceKm":null, "nearest":false}
  ],
  "defaultLocationId": 4,
  "vote": null
}
```
- `votable` が `false` のときは、`reason`(`PROPERTY_CLOSED`・`PROPERTY_NO_LOCATION`・`NO_LOCATION`)と `reasonMessage` を画面に出し、投票ボタンを無効にします。
- `locations` は自宅・勤務先・学校の順です。**`hasCoordinates` が `false` の拠点は基準に選べません**(送ると `NO_LOCATION`)。
- `defaultLocationId` は、`locationId` を送らなかったときに採用される拠点(物件に一番近いもの)です。ラジオボタンの初期選択に使えます。
- `vote` は自分の現在の投票(`Vote.action` の GET と同じ形)。未投票・取消済みなら `null` です。
- 物件がない・削除済み・非表示のときは 404 `PROPERTY_NOT_FOUND`。

# 業種マスタ `Industries.action`(GETのみ・ログイン不要)
```json
{"success": true, "categories": [
  {"categoryId":1, "categoryCode":"C01", "categoryName":"飲食",
   "industries":[{"industryTypeId":1, "industryCode":"I0101", "industryName":"カフェ・喫茶店"}, …]}, …]}
```
- 有効な大分類・小分類だけを、管理画面で決めた表示順に返します。運営が業種を追加・停止すると、そのまま反映されます。
- 投票では、`industryCode`(または `industryTypeId`)を `Vote.action` の `rank1`〜`rank3` に送ります。画面に固定で書いた業種IDではなく、**こちらの値を使ってください**。

# 投票数のまとめ取得 `VoteCounts.action?propertyIds=1,2,3`(GETのみ・ログイン不要)
物件一覧の「投票数」「一番求められている業種」用です(物件ごとに `Demand.action` を呼ぶより速い)。1〜50件まで。
```json
{"success": true, "properties": [
  {"propertyId":1, "voteCount":2, "score":2.400, "topIndustry":{"industryCode":"I0102", "industryName":"レストラン"}},
  {"propertyId":2, "voteCount":0, "score":0.000, "topIndustry":null}]}
```
- 削除済み・運営が非表示・存在しない物件は、結果に含まれません。投票がない物件は `voteCount` が 0、`topIndustry` が `null` です。
- 取消した投票は数えません。結果は物件IDの昇順です。
