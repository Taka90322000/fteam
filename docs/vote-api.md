# 地域住民の投票API(マチリク)

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
| INVALID_INPUT / BAD_CHOICES | 400 | 入力不正・希望業種の指定が不正 |
| PROPERTY_NOT_FOUND | 404 | 物件がない |
| PROPERTY_CLOSED / PROPERTY_NO_LOCATION / NO_LOCATION | 409 | 募集中でない・物件に座標がない・住民の拠点に座標がない |
| METHOD_NOT_ALLOWED | 405 | GET/POST 以外 |
| SERVER_ERROR | 500 | DBエラー(詳細は返さない) |

## フロント側の呼び出し例(ブラウザコンソールでも可)
```js
const body = new URLSearchParams({propertyId: 12, rank1: 'I0101', rank2: 'I0201', frequency: '週1回程度', comment: ''});
fetch('/machiriku/jp/machiriku/servlet/vote/Vote.action', {method: 'POST', headers: {'X-Requested-With': 'XMLHttpRequest'}, body}).then(r => r.json()).then(console.log);
```
