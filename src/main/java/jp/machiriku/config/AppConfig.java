package jp.machiriku.config;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.github.cdimascio.dotenv.Dotenv;

/** サーバー共通設定。.envは公開用フォルダーやWARに含めず、プロジェクト直下などに置く。 */
public final class AppConfig {
    // 初回に読み込んだ設定を、複数のリクエストから安全に共有する。
    private static volatile AppConfig current;
    private final Map<String, String> fileValues;
    private final Function<String, String> environment;

    private AppConfig(Map<String, String> fileValues, Function<String, String> environment) {
        this.fileValues = Map.copyOf(fileValues);
        this.environment = environment;
    }

    /** 指定項目の設定を取得する。.envに項目がある場合は環境変数より優先する。 */
    public static String get(String key) {
        return current().value(key);
    }

    /** 初回アクセス時だけ設定ファイルを探して読み込み、以後は同じ設定を使う。 */
    private static AppConfig current() {
        AppConfig result = current;
        if (result == null) {
            synchronized (AppConfig.class) {
                result = current;
                if (result == null) {
                    // ファイルの明示指定はJVMプロパティを優先し、次に環境変数を確認する。
                    String explicit = System.getProperty("machiriku.env.file");
                    if (explicit == null || explicit.isBlank()) { explicit = System.getenv("MACHIRIKU_ENV_FILE"); }
                    Path file = locateFile(classLocation(), Path.of(System.getProperty("user.dir")), explicit);
                    result = load(file, System::getenv);
                    current = result;
                }
            }
        }
        return result;
    }

    String value(String key) {
        // .envに空値が明記されている場合も優先し、OSに残る古い設定へ戻さない。
        return fileValues.containsKey(key) ? fileValues.get(key) : environment.apply(key);
    }

    /** ファイルに明記された値だけを取り込み、存在しない項目の環境変数参照は後で行う。 */
    static AppConfig load(Path file, Function<String, String> environment) {
        Map<String, String> values = new HashMap<>();
        if (file != null && Files.exists(file)) {
            try {
                if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
                    throw new IllegalStateException();
                }
                Dotenv dotenv = Dotenv.configure().directory(file.toAbsolutePath().getParent().toString())
                        .filename(file.getFileName().toString()).load();
                // dotenv.get()はOS環境変数を優先するため、ファイル内の項目だけを取得して優先順位を保つ。
                for (var entry : dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)) {
                    values.put(entry.getKey(), entry.getValue());
                }
            } catch (RuntimeException e) {
                // 解析例外にはパスワードを含む行が載る可能性があるため、元の例外を保持しない。
                throw new IllegalStateException("Cannot read .env. Check UTF-8 encoding and KEY=VALUE syntax.");
            }
        }
        return new AppConfig(values, environment);
    }

    /** 明示されたファイルを優先し、指定がなければプロジェクト直下の.envを探す。 */
    static Path locateFile(Path classLocation, Path workingDirectory, String explicitFile) {
        // 明示指定の誤りを別ファイルで補わず、設定ミスとして通知する。
        if (explicitFile != null && !explicitFile.isBlank()) {
            try {
                Path explicit = Path.of(explicitFile).toAbsolutePath().normalize();
                if (!Files.isRegularFile(explicit) || !Files.isReadable(explicit)) {
                    throw new IllegalStateException();
                }
                return explicit;
            } catch (RuntimeException e) {
                throw new IllegalStateException("Configured .env file is missing or unreadable.");
            }
        }
        // Tomcatの展開先から親をたどり、例：<project>/target/machiriku/WEB-INF/classesから.envを探す。
        Path project = projectRoot(classLocation);
        if (project == null) { project = projectRoot(workingDirectory); }
        return project == null ? null : project.resolve(".env");
    }

    /** pom.xmlとWeb公開フォルダーを目印に、Mavenプロジェクトのルートを特定する。 */
    private static Path projectRoot(Path start) {
        if (start == null) { return null; }
        for (Path directory = start.toAbsolutePath().normalize(); directory != null; directory = directory.getParent()) {
            if (Files.isRegularFile(directory.resolve("pom.xml"))
                    && Files.isDirectory(directory.resolve("src/main/webapp"))) { return directory; }
        }
        return null;
    }

    // Tomcatの作業ディレクトリーに依存しないよう、読み込まれたクラスの場所も探索の起点にする。
    private static Path classLocation() {
        try {
            var source = AppConfig.class.getProtectionDomain().getCodeSource();
            return source == null ? null : Path.of(source.getLocation().toURI());
        } catch (URISyntaxException | RuntimeException e) {
            return null;
        }
    }
}
