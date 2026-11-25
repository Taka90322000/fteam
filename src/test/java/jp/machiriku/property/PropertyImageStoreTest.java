package jp.machiriku.property;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Test;

import jp.machiriku.bean.PropertyImageInput;
import jp.machiriku.property.PropertyForms.PropertyPhotoDraft;

/**
 * PropertyImageStoreの画像検証・再エンコード保存・途中失敗時ロールバック削除を検証するユニットテスト。
 */
public class PropertyImageStoreTest {

    /**
     * 有効なPNG/JPEGのData URLが再エンコードされてオーナー別ディレクトリへ保存され、削除も可能であることを検証する。
     *
     * @throws Exception テスト用一時ディレクトリや画像生成に失敗した場合
     */
    @Test
    public void savesValidPhotosAsJpegAndDeletesCleanly() throws Exception {
        Path root = Files.createTempDirectory("machiriku-prop-img-test-");
        try {
            PropertyImageStore store = new PropertyImageStore(root);
            List<PropertyPhotoDraft> drafts = List.of(
                    new PropertyPhotoDraft("EXTERIOR", "店舗外観", createDataUrl("png", 64, 48)),
                    new PropertyPhotoDraft("INTERIOR", "客席スペース", createDataUrl("jpg", 80, 60)));

            List<PropertyImageInput> saved = store.saveAll(42, drafts);
            assertEquals(2, saved.size());
            assertEquals(1, saved.get(0).displayOrder());
            assertEquals(2, saved.get(1).displayOrder());
            assertEquals("EXTERIOR", saved.get(0).imageKind());
            assertEquals("店舗外観", saved.get(0).altText());
            assertTrue(PropertyImageStore.manages(saved.get(0).storageKey()));
            assertTrue(PropertyImageStore.manages(saved.get(1).storageKey()));

            // 実際にファイルが書き込まれ、有効なJPEGとして読み込めることを確認する。
            for (PropertyImageInput img : saved) {
                Path file = root.resolve("42").resolve(img.storageKey().substring(PropertyImageStore.URL_PREFIX.length()));
                assertTrue(Files.isRegularFile(file));
                assertNotNull(ImageIO.read(file.toFile()));
            }

            // 削除後はファイルが存在しないことを確認する。
            store.deleteAll(42, saved.stream().map(PropertyImageInput::storageKey).toList());
            for (PropertyImageInput img : saved) {
                Path file = root.resolve("42").resolve(img.storageKey().substring(PropertyImageStore.URL_PREFIX.length()));
                assertFalse(Files.exists(file));
            }
        } finally {
            deleteRecursively(root);
        }
    }

    /**
     * 2枚目の画像が不正な場合に例外を送出し、1枚目の保存済みファイルも残さず削除することを検証する。
     *
     * @throws Exception テスト用一時ディレクトリ操作に失敗した場合
     */
    @Test
    public void cleansUpPreviouslyWrittenFilesWhenLaterImageFails() throws Exception {
        Path root = Files.createTempDirectory("machiriku-prop-img-rollback-");
        try {
            PropertyImageStore store = new PropertyImageStore(root);
            List<PropertyPhotoDraft> drafts = List.of(
                    new PropertyPhotoDraft("EXTERIOR", "有効な写真", createDataUrl("png", 32, 32)),
                    new PropertyPhotoDraft("INTERIOR", "壊れた写真", "data:image/jpeg;base64,bm90LWFuLWltYWdl"));

            assertThrows(IllegalArgumentException.class, () -> store.saveAll(99, drafts));

            Path ownerDir = root.resolve("99");
            if (Files.exists(ownerDir)) {
                try (var stream = Files.list(ownerDir)) {
                    assertEquals(0L, stream.count());
                }
            }
        } finally {
            deleteRecursively(root);
        }
    }

    /**
     * テスト用の小さな単色画像をData URL文字列として生成する。
     *
     * @param format 画像形式（png または jpg）
     * @param width 幅（ピクセル）
     * @param height 高さ（ピクセル）
     * @return Data URL文字列
     * @throws Exception 画像エンコードに失敗した場合
     */
    private static String createDataUrl(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, out));
        String mime = "jpg".equals(format) ? "image/jpeg" : "image/png";
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /**
     * テスト終了後に一時ディレクトリを再帰削除する。
     *
     * @param path 削除対象パス
     * @throws Exception ファイル削除に失敗した場合
     */
    private static void deleteRecursively(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            for (Path target : stream.sorted((a, b) -> b.compareTo(a)).toList()) {
                Files.deleteIfExists(target);
            }
        }
    }
}
