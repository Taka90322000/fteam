package jp.machiriku.property;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

import jp.machiriku.bean.PropertyImageInput;
import jp.machiriku.config.AppConfig;
import jp.machiriku.property.PropertyForms.PropertyPhotoDraft;

/**
 * 物件画像を検証・再エンコードし、Web公開ディレクトリの外へ安全に永続化するストア。
 * 元のファイル名やメタデータをそのまま使わず、サーバー発行のUUID名で保存する。
 */
public final class PropertyImageStore {
    /** 1枚あたりのデコード後バイト数上限（2MB）。 */
    public static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    /** 許容する画像の最大幅・最大高さ（ピクセル）。 */
    public static final int MAX_DIMENSION_PX = 4096;

    /** DBの storage_key に記録する参照先の接頭辞。 */
    public static final String URL_PREFIX = "/property-images/";

    /** サーバーが発行する storage_key の形式。 */
    private static final Pattern STORAGE_KEY_PATTERN = Pattern.compile(
            "^/property-images/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg$");

    /** 受け付けるData URLの形式（JPEG / PNG / GIF）。 */
    private static final Pattern DATA_URL_PATTERN = Pattern.compile(
            "^data:image/(jpeg|jpg|png|gif);base64,([A-Za-z0-9+/=\\r\\n]+)$",
            Pattern.CASE_INSENSITIVE);

    /** 再配備の影響を受けない物件画像保存用ディレクトリ。 */
    private final Path directory;

    /**
     * 画像保存先ディレクトリを指定してストアを生成する。
     *
     * @param directory Tomcatの公開先とは別の永続ディレクトリ
     */
    public PropertyImageStore(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    /**
     * 環境設定またはTomcatのデータディレクトリを使用する標準ストアを返す。
     *
     * @return アプリ共通設定を使用する物件画像ストア
     */
    public static PropertyImageStore configured() {
        // .envまたは環境変数で永続ボリュームが指定されていれば優先する。
        String setting = AppConfig.get("MACHIRIKU_PROPERTY_IMAGE_DIRECTORY");
        if (setting != null && !setting.isBlank()) {
            return new PropertyImageStore(Path.of(setting.trim()));
        }

        // 未指定時は再配備で消えないデータ用ディレクトリ配下へ保存する。
        String catalinaBase = System.getProperty("catalina.base");
        Path dataDirectory = catalinaBase == null || catalinaBase.isBlank()
                ? Path.of(System.getProperty("user.home"), ".machiriku")
                : Path.of(catalinaBase, "machiriku-data");
        return new PropertyImageStore(dataDirectory.resolve("property-images"));
    }

    /**
     * 指定された storage_key がこのストアの管理対象形式かを判定する。
     *
     * @param storageKey 判定対象の参照キー
     * @return サーバー発行形式に一致する場合はtrue
     */
    public static boolean manages(String storageKey) {
        return storageKey != null && STORAGE_KEY_PATTERN.matcher(storageKey).matches();
    }

    /**
     * 物件写真の下書き一覧を検証・再エンコードしてディスクへ保存し、DB登録用リストを返す。
     * 途中で1枚でも失敗した場合は、それまでに書き込んだファイルをすべて削除して例外を送出する。
     *
     * @param ownerUserId 認証済みのオーナー会員ID
     * @param drafts 検証済みの写真下書き一覧
     * @return 表示順（1始まり）付きのDB登録用画像一覧
     * @throws IllegalArgumentException 画像形式・容量・寸法が不正な場合
     * @throws IOException ディスクへの書き込みに失敗した場合
     */
    public List<PropertyImageInput> saveAll(int ownerUserId, List<PropertyPhotoDraft> drafts) throws IOException {
        if (drafts == null || drafts.isEmpty()) {
            return List.of();
        }
        if (drafts.size() > PropertyForms.MAX_PHOTO_COUNT) {
            throw new IllegalArgumentException("登録できる物件写真は最大" + PropertyForms.MAX_PHOTO_COUNT + "枚までです。");
        }

        // 保存成功した画像情報と、途中失敗時のロールバック用キー一覧を管理する。
        List<PropertyImageInput> saved = new ArrayList<>();
        List<String> writtenKeys = new ArrayList<>();
        try {
            for (int index = 0; index < drafts.size(); index++) {
                PropertyPhotoDraft draft = drafts.get(index);
                String storageKey = saveOne(ownerUserId, draft.dataUrl());
                writtenKeys.add(storageKey);
                saved.add(new PropertyImageInput(storageKey, index + 1, draft.altText(), draft.imageKind()));
            }
            return List.copyOf(saved);
        } catch (IOException | RuntimeException error) {
            // 途中まで保存されたファイルが残らないようにすべて片付ける。
            deleteAllQuietly(ownerUserId, writtenKeys, error);
            throw error;
        }
    }

    /**
     * 保存済みファイルを指定キーに従って削除する（DBロールバック時のクリーンアップ用）。
     *
     * @param ownerUserId 画像を保存したオーナー会員ID
     * @param storageKeys 削除対象の storage_key 一覧
     * @throws IOException ファイル削除に失敗した場合
     */
    public void deleteAll(int ownerUserId, List<String> storageKeys) throws IOException {
        if (storageKeys == null || storageKeys.isEmpty()) {
            return;
        }
        for (String key : storageKeys) {
            if (manages(key)) {
                Files.deleteIfExists(resolve(ownerUserId, key));
            }
        }
    }

    /**
     * 1枚のData URL画像を検証・再エンコードして原子的に保存する。
     *
     * @param ownerUserId 認証済みのオーナー会員ID
     * @param dataUrl 検証対象のData URL
     * @return 発行した storage_key
     * @throws IllegalArgumentException 画像形式・容量・寸法が不正な場合
     * @throws IOException ファイル書き込みに失敗した場合
     */
    private String saveOne(int ownerUserId, String dataUrl) throws IOException {
        byte[] rawBytes = decodeDataUrl(dataUrl);
        BufferedImage normalized = readAndNormalizeImage(rawBytes);

        // 元のファイル名は一切使わず、衝突しないUUID名で保存する。
        String storageKey = URL_PREFIX + UUID.randomUUID() + ".jpg";
        Path destination = resolve(ownerUserId, storageKey);
        Files.createDirectories(destination.getParent());

        // 一時ファイルへ完全に書き終えてから最終パスへ移動する。
        Path temporary = Files.createTempFile(destination.getParent(), "prop-", ".tmp");
        try {
            if (!ImageIO.write(normalized, "jpg", temporary.toFile())) {
                throw new IOException("JPEG encoder is not available.");
            }
            Files.move(temporary, destination);
            return storageKey;
        } catch (IOException | RuntimeException error) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupError) {
                error.addSuppressed(cleanupError);
            }
            throw error;
        }
    }

    /**
     * Data URLの形式とサイズを確認し、画像バイト列へデコードする。
     *
     * @param dataUrl Data URL文字列
     * @return デコードされた画像バイト列
     * @throws IllegalArgumentException Data URL形式不正または容量超過の場合
     */
    private static byte[] decodeDataUrl(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new IllegalArgumentException("写真データが空です。画像を選び直してください。");
        }
        Matcher matcher = DATA_URL_PATTERN.matcher(dataUrl.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("物件写真はJPEG・PNG・GIF形式の画像を選択してください。");
        }
        String base64Payload = matcher.group(2);
        // Base64長から事前に上限超過を弾き、過度なメモリ確保を防ぐ。
        if (base64Payload.length() > ((long) MAX_IMAGE_BYTES * 4L / 3L) + 16L) {
            throw new IllegalArgumentException("物件写真1枚あたりの容量が上限（2MB）を超えています。");
        }
        byte[] decoded;
        try {
            decoded = Base64.getMimeDecoder().decode(base64Payload);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("写真データの読み込みに失敗しました。画像を選び直してください。", e);
        }
        if (decoded.length == 0 || decoded.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("物件写真1枚あたりの容量が上限（2MB）を超えているか、ファイルが空です。");
        }
        return decoded;
    }

    /**
     * 画像ヘッダーで寸法上限を確認した上でデコードし、メタデータを含まないRGB画像へ正規化する。
     *
     * @param bytes 検証済みの画像バイト列
     * @return メタデータを除去したRGBの BufferedImage
     * @throws IllegalArgumentException 有効な画像でないか、寸法上限を超える場合
     */
    private static BufferedImage readAndNormalizeImage(byte[] bytes) {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("物件写真を読み込めませんでした。JPEG・PNG・GIF形式の画像を選び直してください。");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION_PX || height > MAX_DIMENSION_PX) {
                    throw new IllegalArgumentException("物件写真の解像度が上限（4096×4096）を超えています。");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new IllegalArgumentException("物件写真を読み込めませんでした。画像を選び直してください。");
                }
                // 透過PNG等も安全にJPEG出力できるよう白背景のRGB画像へ描き直す。
                BufferedImage normalized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = normalized.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, width, height);
                    graphics.drawImage(decoded, 0, 0, null);
                } finally {
                    graphics.dispose();
                }
                return normalized;
            } finally {
                reader.dispose();
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("物件写真を読み込めませんでした。画像を選び直してください。", e);
        }
    }

    /**
     * 検証済みの storage_key をオーナー別ディレクトリ配下の実パスへ変換する。
     *
     * @param ownerUserId オーナー会員ID
     * @param storageKey 検証済み storage_key
     * @return 保存先のファイルパス
     */
    private Path resolve(int ownerUserId, String storageKey) {
        return directory.resolve(Integer.toString(ownerUserId)).resolve(storageKey.substring(URL_PREFIX.length()));
    }

    /**
     * 例外発生時に書き込み済みファイルを安全に削除し、削除失敗は元の例外へ抑制例外として記録する。
     *
     * @param ownerUserId オーナー会員ID
     * @param storageKeys 削除対象のキー一覧
     * @param original 元の例外
     */
    private void deleteAllQuietly(int ownerUserId, List<String> storageKeys, Throwable original) {
        try {
            deleteAll(ownerUserId, storageKeys);
        } catch (IOException cleanupError) {
            original.addSuppressed(cleanupError);
        }
    }
}
