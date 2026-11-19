package jp.machiriku.config;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** 一時ファイルだけを使い、設定値の優先順位・探索場所・安全なエラー処理を検証する。 */
public class AppConfigTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    // .envをOS環境変数より優先し、明示的な空欄を古いOS設定で補わないことを確認する。
    public void fileValuesOverrideEnvironmentAndPresentBlankRemainsBlank() throws Exception {
        Path file = write(".env", "DOTENV_TEST_PASSWORD=file-secret\nDOTENV_TEST_BLANK=\n");
        Map<String, String> environment = Map.of("DOTENV_TEST_PASSWORD", "stale-os-secret",
                "DOTENV_TEST_BLANK", "stale-os-value", "DOTENV_TEST_FALLBACK", "os-only");
        AppConfig config = AppConfig.load(file, environment::get);
        assertEquals("file-secret", config.value("DOTENV_TEST_PASSWORD"));
        assertEquals("", config.value("DOTENV_TEST_BLANK"));
        assertEquals("os-only", config.value("DOTENV_TEST_FALLBACK"));
        assertNull(config.value("DOTENV_TEST_MISSING"));
    }

    @Test
    // 引用符付きの秘密値、JDBC URLの記号、日本語をそのまま読み込めることを確認する。
    public void quotedPasswordAndJdbcQueryPreserveSpecialCharacters() throws Exception {
        Path file = write(".env", "# 設定ファイル\n\n"
                + "DOTENV_TEST_PASSWORD=\"secret#with=$characters\"\n"
                + "DOTENV_TEST_URL=\"jdbc:postgresql://localhost:5432/example?sslmode=require&applicationName=machiriku\"\n"
                + "DOTENV_TEST_NAME=\"マチリク 利用者\"\n");
        AppConfig config = AppConfig.load(file, key -> null);
        assertEquals("secret#with=$characters", config.value("DOTENV_TEST_PASSWORD"));
        assertEquals("jdbc:postgresql://localhost:5432/example?sslmode=require&applicationName=machiriku",
                config.value("DOTENV_TEST_URL"));
        assertEquals("マチリク 利用者", config.value("DOTENV_TEST_NAME"));
    }

    @Test
    // Windowsパスのバックスラッシュが、別のエスケープ文字へ変換されないことを確認する。
    public void windowsBackslashesAreNotParsedAsPropertiesEscapes() throws Exception {
        Path file = write(".env", "DOTENV_TEST_PATH=C:\\fteam\\machiriku\\local\n");
        assertEquals("C:\\fteam\\machiriku\\local", AppConfig.load(file, key -> null).value("DOTENV_TEST_PATH"));
    }

    @Test
    // 自動探索した.envがない場合には、従来のOS環境変数を使えることを確認する。
    public void missingAutomaticFileFallsBackToEnvironment() throws Exception {
        Path missing = temporary.getRoot().toPath().resolve("absent.env");
        assertEquals("existing-os-value", AppConfig.load(missing, key -> "existing-os-value").value("DOTENV_TEST_KEY"));
        assertEquals("existing-os-value", AppConfig.load(null, key -> "existing-os-value").value("DOTENV_TEST_KEY"));
    }

    @Test
    // 設定ファイルの構文エラーでも、秘密値や元の解析例外を外へ出さないことを確認する。
    public void malformedFileErrorDoesNotLeakSettingLineOrRetainParserCause() throws Exception {
        String secret = "do-not-print-this-secret";
        Path file = write(".env", "PASSWORD " + secret + " INVALID LINE\n");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> AppConfig.load(file, key -> null));
        assertFalse(error.toString().contains(secret));
        assertFalse(error.getMessage().contains("PASSWORD"));
        assertNull(error.getCause());
    }

    @Test
    // ファイルではなくディレクトリーを設定先に指定した場合のエラーを確認する。
    public void directoryCannotBeLoadedAsAnEnvironmentFile() throws Exception {
        Path directory = temporary.newFolder("directory.env").toPath();
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> AppConfig.load(directory, key -> null));
        assertNull(error.getCause());
    }

    @Test
    // Tomcatの作業フォルダーが別でも、クラス配置先からプロジェクトの.envを探せることを確認する。
    public void locatesProjectFileFromExplodedTomcatClassesDespiteUnrelatedWorkingDirectory() throws Exception {
        Path root = project("application");
        Path classes = Files.createDirectories(root.resolve("target/machiriku/WEB-INF/classes"));
        Path tomcatBin = temporary.newFolder("external-tomcat", "bin").toPath();
        Path selected = AppConfig.locateFile(classes, tomcatBin, null);
        assertEquals(root.resolve(".env"), selected);
        assertFalse(selected.startsWith(root.resolve("src/main/webapp")));
        assertFalse(selected.startsWith(root.resolve("src/main/resources")));
    }

    @Test
    // 複数のプロジェクトが候補になった場合、読み込んだクラス側の設定を優先することを確認する。
    public void classSourceProjectHasPriorityOverWorkingDirectoryProject() throws Exception {
        Path root = project("source-project");
        Path classes = Files.createDirectories(root.resolve("target/classes"));
        Path other = project("working-project");
        assertEquals(root.resolve(".env"), AppConfig.locateFile(classes, other, ""));
    }

    @Test
    // 最も近いプロジェクトを選び、必要な場合だけ作業フォルダーからも探索することを確認する。
    public void findsNearestProjectAndFallsBackToWorkingDirectoryWhenNeeded() throws Exception {
        Path root = project("parent-project");
        Path module = root.resolve("module");
        Files.createDirectories(module.resolve("src/main/webapp"));
        Files.writeString(module.resolve("pom.xml"), "<project/>", StandardCharsets.UTF_8);
        Path classes = Files.createDirectories(module.resolve("target/classes"));
        assertEquals(module.resolve(".env"), AppConfig.locateFile(classes, root, null));
        assertEquals(root.resolve(".env"), AppConfig.locateFile(null, root.resolve("src/main"), null));
    }

    @Test
    // プロジェクトと無関係なフォルダーの.envを誤って読み込まないことを確認する。
    public void unrelatedDirectoryDoesNotAccidentallyLoadItsEnvironmentFile() throws Exception {
        Path unrelated = temporary.newFolder("unrelated").toPath();
        Files.writeString(unrelated.resolve(".env"), "DOTENV_TEST_SECRET=unrelated\n", StandardCharsets.UTF_8);
        assertNull(AppConfig.locateFile(unrelated, unrelated, null));
    }

    @Test
    // 明示指定した外部の設定ファイルが、自動探索より優先されることを確認する。
    public void explicitExternalFileOverridesAutomaticProjectDiscovery() throws Exception {
        Path root = project("application");
        Path explicit = write("external-settings.env", "DOTENV_TEST_KEY=configured\n");
        assertEquals(explicit, AppConfig.locateFile(root, root, explicit.toString()));
        assertEquals("configured", AppConfig.load(explicit, key -> null).value("DOTENV_TEST_KEY"));
    }

    @Test
    // 明示指定のファイルがない場合、パスを漏らさず設定エラーにすることを確認する。
    public void explicitlyConfiguredMissingFileFailsWithoutExposingItsPath() throws Exception {
        Path missing = temporary.getRoot().toPath().resolve("secret-location-missing.env");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> AppConfig.locateFile(null, null, missing.toString()));
        assertFalse(error.toString().contains("secret-location"));
        assertNull(error.getCause());
    }

    @Test
    // 明示指定がディレクトリーの場合、別の設定へ黙って切り替えないことを確認する。
    public void explicitlyConfiguredDirectoryFailsInsteadOfFallingBack() throws Exception {
        Path root = project("application");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> AppConfig.locateFile(root, root, root.toString()));
        assertNull(error.getCause());
    }

    @Test
    // BOM付きUTF-8に対する解析ライブラリの挙動を確認し、未対応でも秘密値が漏れないことを調べる。
    public void probesUtf8BomWithoutAssumingParserSupport() throws Exception {
        Path file = write("bom.env", "\uFEFFDOTENV_TEST_BOM=bom-value\n");
        try {
            AppConfig config = AppConfig.load(file, key -> null);
            assertEquals("bom-value", config.value("DOTENV_TEST_BOM"));
            assertNull(config.value("\uFEFFDOTENV_TEST_BOM"));
            System.out.println("dotenv-java UTF-8 BOM probe: accepted and normalized");
        } catch (IllegalStateException error) {
            assertNull(error.getCause());
            assertFalse(error.toString().contains("bom-value"));
            System.out.println("dotenv-java UTF-8 BOM probe: rejected with a sanitized error");
        }
    }

    @Test
    // 単一引用符の扱いを確認し、設定値をシェルの式として実行しないことを調べる。
    public void probesSingleQuoteSupportWithoutTreatingQuotesAsShellExpressions() throws Exception {
        Path file = write("single-quotes.env", "DOTENV_TEST_SINGLE='literal#dollar$value'\n");
        try {
            String value = AppConfig.load(file, key -> null).value("DOTENV_TEST_SINGLE");
            if ("literal#dollar$value".equals(value)) {
                System.out.println("dotenv-java single-quote probe: supported, contents preserved literally");
            } else {
                assertNotNull(value);
                System.out.println("dotenv-java single-quote probe: quotes have different semantics; use double quotes in examples");
            }
        } catch (IllegalStateException error) {
            assertNull(error.getCause());
            System.out.println("dotenv-java single-quote probe: rejected with a sanitized error; use double quotes in examples");
        }
    }

    private Path write(String name, String contents) throws IOException {
        Path file = temporary.getRoot().toPath().resolve(name);
        Files.writeString(file, contents, StandardCharsets.UTF_8);
        return file.toAbsolutePath().normalize();
    }

    private Path project(String name) throws IOException {
        Path root = temporary.newFolder(name).toPath().toAbsolutePath().normalize();
        Files.createDirectories(root.resolve("src/main/webapp"));
        Files.writeString(root.resolve("pom.xml"), "<project/>", StandardCharsets.UTF_8);
        return root;
    }
}
