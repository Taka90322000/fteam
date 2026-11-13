# 内装・設備画像の生成と配備

2026-10-26。内蔵のimage_genを1枚ずつ順番に呼び出し、物件2〜10に合計14枚を新規生成しました。各物件の既存外観を参照し、追加アングル・設備は同じ物件の生成済み内装も参照しています。画像を確認後、トリミングやレタッチをせずJPEGへ形式変換し、各専用フォルダーに保存しました。すべて1448×1086pxです。

既存外観10枚、物件1の内装は保持しました。実在物件・ストック写真の流用、他物件への画像使い回し、生成失敗時の代替写真はありません。生成は14回すべて成功しています。

| 物件ID | 今回の追加ファイル | 外観込み合計 |
|---|---|---:|
| prop-orion-01 | 追加なし | 2 |
| prop-odori-02 | 02_interior.jpg（受付・待合） | 2 |
| prop-tobu-03 | 02_interior.jpg（バー・客席）、03_interior.jpg（別アングル） | 3 |
| prop-union-04 | 02_interior.jpg（展示・販売フロア） | 2 |
| prop-lrt-05 | 02_interior.jpg（執務・会議スペース） | 2 |
| prop-kamagawa-06 | 02_interior.jpg（客席・テラス）、03_facility.jpg（厨房） | 3 |
| prop-bamba-07 | 02_interior.jpg（和カフェ・畳席） | 2 |
| prop-stone-08 | 02_interior.jpg（全景）、03_interior.jpg（別アングル）、04_facility.jpg（作業台・照明） | 4 |
| prop-tomatsuri-09 | 02_interior.jpg（セット面・シャンプーブース） | 2 |
| prop-miyukigahara-10 | 02_interior.jpg（学習ブース）、03_interior.jpg（教材棚側） | 3 |

保存先はこのファイルと同じディレクトリ内の各物件IDフォルダーです。

## プロンプト・登録情報

最終プロンプト、参照外観・内装、生成方式、元出力、配備先、画像種別とラベルは`../../tools/generated-property-images.json`に記録しています。`js/data.js`のgalleryにはindex、kind、label、url、localPathを登録しました。

再登録と一致確認はワークスペースのルートで実行します。

```powershell
node machiriku/tools/update-generated-galleries.cjs
node machiriku/tools/update-generated-galleries.cjs --check
```

登録処理は存在する生成画像だけを対象にします。他物件の写真を流用する旧`tools/build-full-galleries.cjs`は使用しません。現在の物件は仮想データのため、HOME'S再取込も実行しません。

保存済みプレビューにはgalleryVersionで写真の更新を反映します。物件情報、投票・需要、応募、フォロー、利用者追加物件は保持します。

## 検証

- 14枚すべてJPEGとしてデコード可能、1448×1086pxであることを確認。
- 有効な全ギャラリー25枚の参照先存在、物件ID別フォルダー、index、url/localPath一致、ファイル重複なしを確認。
- 新規14枚が旧HOME'S画像と同じファイルでないこと、既存外観10枚のSHA256が作業前と一致することを確認。
- 外観だけの保存済みギャラリーが更新され、投票・フォロー・物件の編集値・利用者追加物件を保持するテストに合格。
- verify.cjs、ui_inspector.cjs、test_checker.cjs、生成マニフェスト一致確認にすべて合格。33画面、4ロールのHTML生成を含む。

生成出力は画像として確認しました。サイトのブラウザー描画・実機での目視確認は、既存のローカルファイル表示制限により未実施です。
