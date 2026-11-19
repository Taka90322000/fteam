package jp.machiriku.profile;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

import jp.machiriku.config.AppConfig;

/** 切り抜き済みのアイコンをPNGに再エンコードし、公開ディレクトリの外に保存する。 */
public final class ProfileIconStore {
    /** 画面で切り抜いて送る正方形の一辺のピクセル数。 */
    public static final int PIXELS = 512;

    /** 透過を含むアイコンを受け付ける圧縮済みファイルの上限。 */
    public static final int MAX_BYTES = 1280 * 1024;

    /** DBに保存するアプリ内の画像参照先の接頭辞。 */
    public static final String URL_PREFIX = "/profile-icons/";

    /** サーバーが発行する参照先の形式。元画像のファイル名は使わない。 */
    private static final Pattern REFERENCE = Pattern.compile(URL_PREFIX
            + "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png");

    /** 再配備の影響を受けない画像保存用ディレクトリ。 */
    private final Path directory;

    /**
     * 画像の保存先を指定する。
     * @param directory Tomcatの公開先とは別の永続ディレクトリ
     */
    public ProfileIconStore(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    /**
     * 明示された保存先を優先し、未指定のときはTomcat配下のデータ用ディレクトリを使う。
     * @return アプリ共通設定を使用する画像ストア
     */
    public static ProfileIconStore configured() {
        // .envまたは環境変数で指定された永続ボリュームのパス。
        String setting = AppConfig.get("MACHIRIKU_ICON_DIRECTORY");
        if (setting != null && !setting.isBlank()) {
            return new ProfileIconStore(Path.of(setting.trim()));
        }

        // Tomcat起動ディレクトリに依存せず、再配備で置き換えるWebアプリの外に保存する。
        String catalinaBase = System.getProperty("catalina.base");
        Path dataDirectory = catalinaBase == null || catalinaBase.isBlank()
                ? Path.of(System.getProperty("user.home"), ".machiriku")
                : Path.of(catalinaBase, "machiriku-data");
        return new ProfileIconStore(dataDirectory.resolve("profile-icons"));
    }

    /**
     * DBに保存された参照先が、このストアのファイルを示すか判定する。
     * @param reference アプリ内の画像参照先または旧形式のURL
     * @return サーバー発行の形式と一致する場合はtrue
     */
    public static boolean manages(String reference) {
        return reference != null && REFERENCE.matcher(reference).matches();
    }

    /**
     * 切り抜き済みの画像を検証し、会員用ディレクトリに新しいPNGを保存する。
     * @param userId 認証済みの会員ID
     * @param contents アップロードされたファイルの入力ストリーム
     * @param size multipartに含まれるファイルのバイト数
     * @return DBに記録する新しいアイコン参照先
     * @throws IllegalArgumentException ファイルが上限内の512ピクセル四方のPNGでない場合
     * @throws IOException 入力の読み取りやディスクへの書き込みに失敗した場合
     */
    public String save(int userId, InputStream contents, long size) throws IOException {
        // 本文の読み取りにも上限を設け、サイズ申告にかかわらずメモリ使用量を抑える。
        if (size <= 0 || size > MAX_BYTES) {
            throw new IllegalArgumentException("アイコン画像の容量が上限を超えているか、ファイルが空です。");
        }
        byte[] bytes = contents.readNBytes(MAX_BYTES + 1);
        if (bytes.length != size) {
            throw new IllegalArgumentException("アイコン画像を最後まで読み取れませんでした。");
        }

        // 拡張子やContent-Typeに依存せずPNGとして読み、画像の付加情報を取り除く。
        BufferedImage image = readPng(bytes);
        // 保存中のファイルと過去のアイコンを区別できるよう、差し替えごとに新しい名前を発行する。
        String reference = URL_PREFIX + UUID.randomUUID() + ".png";
        Path destination = resolve(userId, reference);
        Files.createDirectories(destination.getParent());
        // 書き込みが完了するまで、配信用URLからアクセスできる名前に移動しない。
        Path temporary = Files.createTempFile(destination.getParent(), "icon-", ".tmp");
        try {
            if (!ImageIO.write(image, "png", temporary.toFile())) {
                throw new IOException("PNG encoder is not available.");
            }
            Files.move(temporary, destination);
        } catch (IOException | RuntimeException error) {
            // 途中まで書き込まれたファイルが残らないようにする。
            try { Files.deleteIfExists(temporary); }
            catch (IOException cleanupError) { error.addSuppressed(cleanupError); }
            throw error;
        }
        return reference;
    }

    /**
     * 指定された会員の画像を配信用に開く。
     * @param userId 認証済みの会員ID
     * @param reference アプリ内の画像参照先
     * @return 呼び出し元で閉じるPNGの入力ストリーム
     * @throws IOException 参照先が無効か、画像を開けない場合
     */
    public InputStream open(int userId, String reference) throws IOException {
        // 他のファイル形式や外部ファイルへの参照を配信しない。
        if (!manages(reference)) { throw new NoSuchFileException("Profile icon"); }
        return Files.newInputStream(resolve(userId, reference), LinkOption.NOFOLLOW_LINKS);
    }

    /**
     * プロフィールから差し替えられた、自分の会員用ディレクトリ内の画像を削除する。
     * @param userId アイコンを保存した会員ID
     * @param reference 削除対象の参照先。旧形式のURLは処理対象外
     * @throws IOException ファイルの削除に失敗した場合
     */
    public void delete(int userId, String reference) throws IOException {
        if (manages(reference)) { Files.deleteIfExists(resolve(userId, reference)); }
    }

    /**
     * 検証済みの参照先を会員用ディレクトリ内のパスへ変換する。
     * @param userId 認証済みの正の会員ID
     * @param reference サーバーが発行する参照先
     * @return ファイルシステム上の保存先
     */
    private Path resolve(int userId, String reference) {
        return directory.resolve(Integer.toString(userId)).resolve(reference.substring(URL_PREFIX.length()));
    }

    /**
     * PNGのヘッダーから画像サイズを確認し、読み込んだ画素を標準的なRGBA形式に変換する。
     * @param bytes 容量を確認済みの画像データ
     * @return メタデータを含まない512ピクセル四方の画素
     * @throws IllegalArgumentException 切り抜き済みの有効なPNGでない場合
     */
    private static BufferedImage readPng(byte[] bytes) {
        // PNGのデコーダーを指定し、ヘッダー検証が済むまで画素を展開しない。
        ImageReader reader = ImageIO.getImageReadersByFormatName("png").next();
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            reader.setInput(input, true, true);
            if (reader.getWidth(0) != PIXELS || reader.getHeight(0) != PIXELS) {
                throw new IllegalArgumentException("アイコン画像は512×512ピクセルに切り抜いてください。");
            }

            // 透明部分を保持しつつ、サーバー側で画素だけを取り出す。
            BufferedImage normalized = new BufferedImage(PIXELS, PIXELS, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = normalized.createGraphics();
            try { graphics.drawImage(reader.read(0), 0, 0, null); }
            finally { graphics.dispose(); }
            return normalized;
        } catch (IOException | IndexOutOfBoundsException error) {
            throw new IllegalArgumentException("アイコン画像を読み込めませんでした。画像を選び直してください。", error);
        } finally {
            reader.dispose();
        }
    }
}
