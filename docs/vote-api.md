# 投票機能(地域住民の物件投票)

地域住民が、空き物件に「ここにこんなお店が欲しい」と投票する機能。
班の他の機能(ログイン・プロフィールなど)と同じく、**MVC(Action → JSP)** で作っている。
Actionは JSON を返さず、処理したあとに JSP へ forward する(保存後は redirect)。

ファイル名は `vote-api.md` のままだが、内容は MVC 構成の説明。

## 1. 構成(MVC)

| 役割 | 置き場所 | 内容 |
|---|---|---|
| Controller | `servlet/vote/*Action.java` | 本人確認・入力の読み取り・DAOの呼び出し・遷移先の決定 |
| Model | `bean/`(入れ物)、`dao/`(DB処理) | 投票の保存、需要の集計など。画面のことは知らない |
| View | `WEB-INF/views/vote/*.jsp` | Actionが渡した値を表示するだけ。DBには触れない |

```
ブラウザ ──GET──▶ VoteForm.action ──▶ DAOで物件・拠点・業種・現在の投票を取得
                         │
                         └─ request属性に入れて forward ──▶ vote_form.jsp(投票画面)
ブラウザ ──POST─▶ Vote.action ───▶ 検査して保存(VoteDao)
                         ├─ 成功 ─ redirect ─▶ VoteDone.action ─▶ vote_done.jsp(完了画面)
                         └─ 入力エラー ─ forward ─▶ vote_form.jsp(入力値とエラー文つきで再表示)
```

保存後は redirect するので、完了画面を更新(リロード)しても二重に投票されない。

## 2. URL一覧

すべて `/machiriku/jp/machiriku/servlet/vote/〇〇.action`(`FrontController` が `*.action` を `servlet.vote.〇〇Action` に振り分ける)。

| URL | 方法 | ログイン | 表示するJSP | 役割 |
|---|---|---|---|---|
| `VoteForm.action?propertyId=N` | GET | 地域住民 | `vote_form.jsp` | 投票画面(入力フォーム) |
| `Vote.action` | POST | 地域住民 | (保存後は `VoteDone.action` へ redirect / 入力エラー時は `vote_form.jsp`) | 投票の保存・再投票・取り下げ |
| `VoteDone.action?propertyId=N` | GET | 地域住民 | `vote_done.jsp` | 投票完了画面(保存済みの内容を表示) |
| `VoteHistory.action` | GET | 地域住民 | `vote_history.jsp` | 自分の投票履歴・取り下げ |
| `Demand.action` | GET | 不要 | `demand.jsp` | 住民の投票から集計した需要(物件別・エリア別) |

「地域住民」とは、ログイン済みで、初期設定が済んだ、有効な地域住民の会員のこと。

### 本人確認の結果(全Action共通)

| 状態 | 動き |
|---|---|
| ログインしていない | `Login.action` へ redirect |
| 初期設定が未完了(PENDING) | 初期設定画面(`ResidentSetup.action` など)へ redirect |
| 退会済み、または地域住民ではない(事業者・オーナー) | 403 |

### パラメーター

**`VoteForm.action`**: `propertyId`(必須、1以上の整数)。不正なら 400、物件がなければ 404。

**`Vote.action`**(POST。`application/x-www-form-urlencoded`)

| 名前 | 内容 |
|---|---|
| `csrfToken` | 必須。画面に埋め込まれた値(フォームの hidden)。違うと 403 |
| `propertyId` | 必須 |
| `rank1` `rank2` `rank3` | 希望する業種の業種コード(例: `I0101`)。`rank1` は必須。順位の飛ばし・重複は不可 |
| `frequency` | 利用頻度の想定(任意) |
| `comment` | 事業主への要望(任意、1000字まで) |
| `locationId` | 投票の基準にする自分の生活拠点(省略すると最寄りの拠点)。座標がある自分の拠点だけ |
| `withdraw=1` | 付けると、保存ではなく取り下げ(この場合 `rank*` は不要) |

**`VoteHistory.action`**: `status`(`all`(既定)/`active`/`withdrawn`)、`page`(1から。1ページ10件)、`withdrawn=1`(取り下げ直後の通知表示用)。

**`Demand.action`**(物件指定かエリア指定。どちらも省略するとエリア全体)

| 名前 | 内容 |
|---|---|
| `propertyId` | 物件1件の需要 |
| `prefecture` `municipality` | エリアの需要(掲載中の物件が対象) |
| `limit` | 業種ランキングの件数(既定10、最大50) |
| `commentLimit` | 物件別のコメント件数(既定10、最大30。会員にだけ表示) |
| `topProperties` | エリア別の「需要の高い物件」件数(既定5、最大20) |

## 3. 入力エラーのときの動き

| 場面 | 動き |
|---|---|
| `Vote.action` の入力が不正(業種の選び方、拠点、文字数など) | 400 で `vote_form.jsp` を**再表示**。画面上部にエラー文、入力欄には入力済みの値が残る |
| 募集が終了・座標なしなどで投票できない | 409 で `vote_form.jsp` を再表示 |
| 物件がない・削除済み・管理者が非表示 | 404 |
| `csrfToken` が違う | 403 |
| `Demand.action` のパラメーターが不正 | 400 で `demand.jsp` を表示(集計なし、エラー文あり) |
| DBエラー | 500(詳細は画面に出さず、ログにだけ残す) |

## 4. 業務ルール

- 順位の重み: 1位 = 1.00、2位 = 0.60、3位 = 0.30。第1希望は必須。
- スコア = `順位の重み ÷ (1 + 距離km ÷ 物件の距離基準km)`。距離は、選んだ生活拠点(省略時は最寄り)から物件まで。
- 1人1物件につき1票(`UNIQUE(property_id, resident_user_id)`)。再投票は同じ行を更新する。取り下げ済みの投票があれば、その行を使い回して再投票できる。
- 需要の集計の対象は、取り下げていない投票だけ。業種ジャンル別の「希望人数」は、同じ人を重ねて数えない。
- 管理者が非表示にした物件、削除済みの物件には、投票できない・集計に出さない。
- 「オープン希望時期」は、DBに保存する列がないので、画面にも項目を出していない。必要ならDBの変更(班での相談)が要る。

## 5. 画面への導線(班の他の画面から)

既存の `property/vote.jsp`(JSで動くダミーの画面)や、物件詳細の「投票する」ボタンは、**班の人のファイル**なので変更していない。
つなぐときは、リンクの先を次にする。

- 投票する: `/machiriku/jp/machiriku/servlet/vote/VoteForm.action?propertyId=物件ID`
- 物件の需要を見る: `/machiriku/jp/machiriku/servlet/vote/Demand.action?propertyId=物件ID`
- 自分の投票履歴: `/machiriku/jp/machiriku/servlet/vote/VoteHistory.action`

## 6. 変更の履歴

- JSONを返すAPI(`Industries` / `VoteContext` / `VoteCounts` と、各Actionの JSON 応答)は廃止し、画面表示(Action → JSP)に置き換えた。
  業種の一覧・物件の情報・投票人数などは、Actionが集めてJSPに渡す。
- 画面(JSP)の場所は `servlet/vote/VoteViews.java` の4つの定数に集めた。Actionは直接パスを書かず、この定数を使う。
- 画面でELを使えるように、`VoteContext`・`IndustryMaster`・`VoteHistoryPage`・`DemandReport`(とその中の入れ物)に `getXxx()` を足した。
  `ViewContractTest` が、下の「8」に書いた `${...}` で値を読めることを確かめている。

## 7. テスト

- Action: `PageTestHarness`(偽のリクエスト・レスポンス)で、forward 先・redirect 先・ステータス・request属性を確認する。DBは使わない。
- DAO: `MACHIRIKU_RUN_DB_TESTS=true` を付けたときだけ実DBで動く(一時テーブルを使い、本物のデータには触れない)。
- 画面との約束: `ViewContractTest` が、各Actionを動かして渡された値を `${a.b[0].c}` の形でたどり、読めることを確認する。
  beanに項目を足したのに `getXxx()` を足し忘れると、このテストが失敗する。
- 画面: `WEB-INF/views/vote/*.jsp` は、Tomcatで `VoteForm.action?propertyId=N` などを開いて確認する。

## 8. 画面を作る人へ(フロント担当向け)

投票機能の画面(JSP)は、フロント担当が自由に作ってよい。Actionが決めているのは、次の3つだけ。

1. JSPの**場所** … `servlet/vote/VoteViews.java`(下の8-1)
2. Actionが渡す**値の名前**(request属性)と、そこから読める項目(8-2)
3. フォームで送る**項目名**(8-3)

これを守れば、画面の見た目・HTML・CSSは何でもよく、バックエンドは直さなくても動く。

### 8-1. JSPの場所を決める

JSPは `WEB-INF` の下に置く(アドレスを直接開けず、必ずActionを通る)。置き場所・ファイル名を変えるときは、`VoteViews.java` の定数だけを書き換える。

| 定数 | 今の値 | 使う場面 |
|---|---|---|
| `VoteViews.FORM` | `/WEB-INF/views/vote/vote_form.jsp` | 投票画面。入力エラー時の再表示にも使う |
| `VoteViews.DONE` | `/WEB-INF/views/vote/vote_done.jsp` | 投票完了画面 |
| `VoteViews.HISTORY` | `/WEB-INF/views/vote/vote_history.jsp` | 自分の投票履歴 |
| `VoteViews.DEMAND` | `/WEB-INF/views/vote/demand.jsp` | 需要の集計(物件別・エリア別) |

### 8-2. 画面ごとの「渡される値」(request属性)

JSPでは `${名前}` で読める。値が無いことがあるものは「なし可」と書いた(`<c:if test="${not empty ...}">` で確認する)。
値は、Actionが読み込む前に整えてあるので、JSPからDBに触る必要はない。

**投票画面(`FORM`)** … `VoteForm.action?propertyId=N`、または `Vote.action` の入力エラー時

| 名前 | 中身 | 読み方の例 |
|---|---|---|
| `voteContext` | 物件・投票できるか・生活拠点 | `${voteContext.property.name}` `${voteContext.property.prefecture}` `${voteContext.property.municipality}` `${voteContext.property.addressLine}` `${voteContext.voteCount}`(今の投票人数) |
| | 投票できるか | `${voteContext.votable}`(false のときは投票欄を出さず `${voteContext.reasonMessage}` を表示。`${voteContext.reason}` は `PROPERTY_CLOSED` / `PROPERTY_NO_LOCATION` / `NO_LOCATION`) |
| | 生活拠点の候補(`locations`) | `<c:forEach var="loc" items="${voteContext.locations}">` で `${loc.locationId}` `${loc.kind}`(HOME / WORK / SCHOOL) `${loc.addressLine}` `${loc.distanceKm}` `${loc.hasCoordinates}` `${loc.nearest}`。`hasCoordinates` が false の拠点は距離がなく、選べない |
| | 既定の拠点 | `${voteContext.defaultLocationId}`(なし可。最寄りの拠点) |
| `industries` | 業種の選択肢 | `<c:forEach var="cat" items="${industries.categories}">` → `${cat.categoryName}` → `<c:forEach var="ind" items="${cat.industries}">` → `${ind.industryCode}` `${ind.industryName}` |
| `currentVote` | 自分の今の投票(なし可。未投票・取り下げ済みは空) | `${currentVote.choices[0].industryName}` `${currentVote.visitFrequency}` `${currentVote.requestComment}` `${currentVote.score}` |
| `formValues` | フォームの初期値(再表示のときは入力済みの値) | `${formValues.rank1}` `${formValues.rank2}` `${formValues.rank3}` `${formValues.frequency}` `${formValues.comment}` `${formValues.locationId}` |
| `errorMessage` | エラー文(なし可) | `${errorMessage}` |
| `csrfToken` | フォームに必ず入れる値 | `${csrfToken}` |

**投票完了画面(`DONE`)** … `VoteDone.action?propertyId=N`(`Vote.action` が保存後にredirectする。リロードしても二重投票にならない)

| 名前 | 読み方の例 |
|---|---|
| `vote` | `${vote.choices[0].industryName}`(`choices` は順位順) `${vote.visitFrequency}` `${vote.requestComment}` `${vote.score}` |
| `voteContext` | `${voteContext.property.name}` `${voteContext.voteCount}` |
| `updated` | `${updated}` … true なら「投票を更新しました」、false なら「投票しました」 |

**投票履歴(`HISTORY`)** … `VoteHistory.action?status=all&page=1`

| 名前 | 読み方の例 |
|---|---|
| `history` | `${history.total}`(全件数) `<c:forEach var="item" items="${history.items}">`(1ページ10件) |
| 1件ごと(`item`) | `${item.propertyName}` `${item.prefecture}` `${item.municipality}` `${item.addressLine}` `${item.listingStatus}` `${item.propertyDeleted}`(true なら物件は削除済み。詳細へのリンクは出さない) `${item.createdAt}` |
| | 投票の中身: `${item.vote.propertyId}` `${item.vote.withdrawn}`(true は取り下げ済み) `${item.vote.updatedAt}` `${item.vote.score}` `${item.vote.visitFrequency}` `${item.vote.requestComment}` `${item.vote.choices[0].industryName}` |
| `status` | `${status}` … `all` / `active` / `withdrawn`(今の絞り込み。リンクの強調表示に使う) |
| `pageNumber` `totalPages` | `${pageNumber}` / `${totalPages}`(ページ送りに使う。次は `VoteHistory.action?status=${status}&page=${pageNumber + 1}`) |
| `withdrawnNotice` | `${withdrawnNotice}` … true のとき「取り下げました」を表示 |
| `csrfToken` | 取り下げボタンのフォームに入れる |

**需要の集計(`DEMAND`)** … `Demand.action?propertyId=N`(物件別)、`Demand.action?prefecture=栃木県&municipality=宇都宮市`(エリア別)。ログイン不要

| 名前 | 読み方の例 |
|---|---|
| `report` | `${report.scope}`(`PROPERTY` / `AREA`) `${report.propertyName}` `${report.voteCount}`(有効な投票数) `${report.voterCount}`(投票した人数) `${report.averageDistanceKm}` `${report.totalScore}` |
| 業種ランキング | `<c:forEach var="ind" items="${report.industries}">` → `${ind.rank}` `${ind.industryName}` `${ind.score}` `${ind.sharePercent}` `${ind.residentCount}` `${ind.firstChoiceCount}` `${ind.secondChoiceCount}` `${ind.thirdChoiceCount}` |
| 大分類 | `${report.categories}` → `${cat.categoryName}` `${cat.residentCount}` `${cat.sharePercent}` |
| 来店頻度 | `${report.frequencies}` → `${f.frequency}` `${f.count}` |
| 要望コメント | `${report.comments}` → `${c.comment}` `${c.frequency}` `${c.votedAt}`(**ログイン中の会員にだけ入る**。`${commentsVisible}` が false なら「ログインすると見られます」など) |
| 需要の高い物件(エリア別のみ) | `${report.topProperties}` → `${p.rank}` `${p.propertyId}` `${p.propertyName}` `${p.municipality}` `${p.voteCount}` `${p.topIndustryName}` |
| `prefecture` `municipality` | 検索欄の初期値(空文字なら未指定) |
| `errorMessage` | パラメーターが不正なとき(400)だけ入る。そのとき `report` は無い |

### 8-3. フォームで送る項目

どのフォームも、`method="post"`、送り先は `${pageContext.request.contextPath}/jp/machiriku/servlet/vote/Vote.action`、**`csrfToken`(hidden)が必須**(無いと 403)。

```jsp
<form method="post" action="${pageContext.request.contextPath}/jp/machiriku/servlet/vote/Vote.action">
  <input type="hidden" name="csrfToken" value="${csrfToken}">
  <input type="hidden" name="propertyId" value="${voteContext.property.propertyId}">
  <%-- 第1〜第3希望: 値は業種コード(${ind.industryCode})。rank1 だけ必須 --%>
  <select name="rank1"> ... <option value="${ind.industryCode}" ${formValues.rank1 == ind.industryCode ? 'selected' : ''}>${ind.industryName}</option> ... </select>
  <select name="rank2"> ... </select>
  <select name="rank3"> ... </select>
  <input name="frequency" value="${formValues.frequency}">      <%-- 30文字まで。選択肢にするなら「週1回程度」など自由に --%>
  <textarea name="comment">${formValues.comment}</textarea>      <%-- 1000文字まで(<c:out> で必ずエスケープ) --%>
  <input type="radio" name="locationId" value="${loc.locationId}"> <%-- 省略すると最寄りの拠点 --%>
  <button type="submit">投票する</button>
</form>
```

| 操作 | 送る項目 | 結果 |
|---|---|---|
| 投票・再投票 | `csrfToken` `propertyId` `rank1`(必須) `rank2` `rank3` `frequency` `comment` `locationId` | 成功 → `VoteDone.action` へredirect。入力エラー → `FORM` を再表示(400/409) |
| 取り下げ | `csrfToken` `propertyId` `withdraw=1`(`rank*` は不要) | 成功 → `VoteHistory.action?withdrawn=1` へredirect |

- 希望の選び方の誤り(順位の飛ばし・同じ業種を2回など)は、`errorMessage` に文が入って `FORM` が再表示される。`formValues` に入力済みの値が入るので、`${formValues.○○}` を入力欄の初期値にしておけば、入力が消えない。
- `comment` などユーザーが入力した文字は、`${...}` のまま書くとHTMLとして解釈されてしまう。`<c:out value="${...}"/>` か `fn:escapeXml` で表示する(班の `Html.escape` でもよい)。

### 8-4. 班の他の画面からのリンク

「5. 画面への導線」のとおり。リンク先のURLは変わらない。画面のファイル名を変えても、リンクは変えなくてよい(Action経由のため)。
