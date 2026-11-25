package jp.machiriku.bean;

/**
 * 保存済み物件画像のメタ情報を保持する不変データクラス。
 * property_images テーブルの1行に対応する。
 *
 * @param storageKey サーバー側で発行した画像保存先キー（例：/property-images/uuid.jpg）
 * @param displayOrder 表示順（1以上の連番）
 * @param altText 写真の説明文・代替テキスト（任意、最大200文字）
 * @param imageKind 写真種別（EXTERIOR / INTERIOR / FLOORPLAN / FACILITY / SURROUNDING / OTHER）
 */
public record PropertyImageInput(
        String storageKey,
        int displayOrder,
        String altText,
        String imageKind) {
}
