package jp.machiriku.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;

import javax.imageio.ImageIO;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** アップロードされたアイコンの形式検証と、会員単位の保存・配信を確認する。 */
public class ProfileIconStoreTest {
    /** アプリの実データに触れないよう、各テストに一時ディレクトリを用意する。 */
    @Rule public TemporaryFolder directory = new TemporaryFolder();

    /**
     * 有効なPNGが元の見た目を保ち、保存先の会員だけが開けることを確認する。
     * @throws IOException テスト画像や一時ファイルを読み書きできない場合
     */
    @Test
    public void savesAndServesPngForTheOwningMember() throws IOException {
        // 透明度を持つ512ピクセル四方の画像で、保存後の色と透過を確認する。
        ProfileIconStore store = new ProfileIconStore(directory.getRoot().toPath());
        byte[] original = image("png", ProfileIconStore.PIXELS);
        String reference = store.save(42, new ByteArrayInputStream(original), original.length);
        assertTrue(ProfileIconStore.manages(reference));
        try (InputStream contents = store.open(42, reference)) {
            BufferedImage stored = ImageIO.read(contents);
            assertEquals(512, stored.getWidth());
            assertEquals(512, stored.getHeight());
            assertEquals(0xff336699, stored.getRGB(20, 30));
            assertEquals(0, stored.getRGB(0, 0) >>> 24);
        }
        assertThrows(NoSuchFileException.class, () -> store.open(99, reference));
        assertFalse(ProfileIconStore.manages("/profile-icons/another-file.png"));
        store.delete(42, reference);
        assertThrows(NoSuchFileException.class, () -> store.open(42, reference));
    }

    /**
     * 切り抜きと異なる寸法や容量、PNG以外のデータの保存を拒否する。
     * @throws IOException テスト画像を用意できない場合
     */
    @Test
    public void rejectsWrongFormatDimensionsAndSizeBeforeWriting() throws IOException {
        // エラーが出た段階で、配信用ディレクトリやファイルが残っていないことも確認する。
        ProfileIconStore store = new ProfileIconStore(directory.getRoot().toPath());
        byte[] jpeg = image("jpeg", ProfileIconStore.PIXELS);
        byte[] small = image("png", 128);
        assertThrows(IllegalArgumentException.class, () -> store.save(42, new ByteArrayInputStream(jpeg), jpeg.length));
        assertThrows(IllegalArgumentException.class, () -> store.save(42, new ByteArrayInputStream(small), small.length));
        assertThrows(IllegalArgumentException.class, () -> store.save(42, InputStream.nullInputStream(), ProfileIconStore.MAX_BYTES + 1));
        assertEquals(0, directory.getRoot().list().length);
    }

    /**
     * 指定されたエンコード形式のテスト画像を作る。
     * @param format ImageIOで書き出す画像形式
     * @param size 幅と高さ共通のピクセル数
     * @return エンコード後の画像データ
     * @throws IOException 画像をエンコードできない場合
     */
    private static byte[] image(String format, int size) throws IOException {
        // PNGの透明部分とJPEGの不透明な色空間を切り替える。
        BufferedImage image = new BufferedImage(size, size, "png".equals(format) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        image.setRGB(20, 30, 0xff336699);
        ByteArrayOutputStream contents = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, contents));
        return contents.toByteArray();
    }
}
