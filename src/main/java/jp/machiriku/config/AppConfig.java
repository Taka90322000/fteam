package jp.machiriku.config;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.github.cdimascio.dotenv.Dotenv;

/** Server-side settings. The project .env stays outside webapp/resources and the WAR. */
public final class AppConfig {
    private static volatile AppConfig current;
    private final Map<String, String> fileValues;
    private final Function<String, String> environment;

    private AppConfig(Map<String, String> fileValues, Function<String, String> environment) {
        this.fileValues = Map.copyOf(fileValues);
        this.environment = environment;
    }

    public static String get(String key) {
        return current().value(key);
    }

    private static AppConfig current() {
        AppConfig result = current;
        if (result == null) {
            synchronized (AppConfig.class) {
                result = current;
                if (result == null) {
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
        // A present but blank file value deliberately disables a stale OS setting.
        return fileValues.containsKey(key) ? fileValues.get(key) : environment.apply(key);
    }

    static AppConfig load(Path file, Function<String, String> environment) {
        Map<String, String> values = new HashMap<>();
        if (file != null && Files.exists(file)) {
            try {
                if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
                    throw new IllegalStateException();
                }
                Dotenv dotenv = Dotenv.configure().directory(file.toAbsolutePath().getParent().toString())
                        .filename(file.getFileName().toString()).load();
                // dotenv.get() favors OS variables; use file-only entries for local-file priority.
                for (var entry : dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)) {
                    values.put(entry.getKey(), entry.getValue());
                }
            } catch (RuntimeException e) {
                // Parser exceptions may contain an entire line with a password. Never retain them.
                throw new IllegalStateException("Cannot read .env. Check UTF-8 encoding and KEY=VALUE syntax.");
            }
        }
        return new AppConfig(values, environment);
    }

    static Path locateFile(Path classLocation, Path workingDirectory, String explicitFile) {
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
        // Exploded Tomcat: <project>/target/machiriku/WEB-INF/classes -> <project>/.env.
        Path project = projectRoot(classLocation);
        if (project == null) { project = projectRoot(workingDirectory); }
        return project == null ? null : project.resolve(".env");
    }

    private static Path projectRoot(Path start) {
        if (start == null) { return null; }
        for (Path directory = start.toAbsolutePath().normalize(); directory != null; directory = directory.getParent()) {
            if (Files.isRegularFile(directory.resolve("pom.xml"))
                    && Files.isDirectory(directory.resolve("src/main/webapp"))) { return directory; }
        }
        return null;
    }

    private static Path classLocation() {
        try {
            var source = AppConfig.class.getProtectionDomain().getCodeSource();
            return source == null ? null : Path.of(source.getLocation().toURI());
        } catch (URISyntaxException | RuntimeException e) {
            return null;
        }
    }
}
