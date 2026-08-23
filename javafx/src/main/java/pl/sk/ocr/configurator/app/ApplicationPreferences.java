package pl.sk.ocr.configurator.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;
import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.core.document.RenderOptions;

public final class ApplicationPreferences {
    public enum DirectoryKey {
        OPEN_CONFIGURATION("openConfigurationDirectory"),
        SAVE_CONFIGURATION("saveConfigurationDirectory"),
        OPEN_DOCUMENT("openDocumentDirectory"),
        EXPORT_DOCUMENT("exportDocumentDirectory"),
        LAST_OPENED("lastOpenedDirectory");

        private final String key;

        DirectoryKey(String key) {
            this.key = key;
        }
    }

    public record Settings(String tesseractDatapath, String defaultOcrLanguage, int defaultPdfDpi,
                           String lastOpenedDirectory, int cacheLimit) {
    }

    private static final String TESSERACT_DATAPATH = "tesseractDatapath";
    private static final String DEFAULT_OCR_LANGUAGE = "defaultOcrLanguage";
    private static final String DEFAULT_PDF_DPI = "defaultPdfDpi";
    private static final String CACHE_LIMIT = "cacheLimit";
    private static final String DEFAULT_LANGUAGE = "pol";
    private static final int DEFAULT_DPI = 300;
    private static final int DEFAULT_CACHE_LIMIT = 25;

    private final Preferences preferences;

    public ApplicationPreferences() {
        this(Preferences.userNodeForPackage(ApplicationPreferences.class));
    }

    ApplicationPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public Settings settings() {
        return new Settings(
            blankToNull(preferences.get(TESSERACT_DATAPATH, defaultDatapath())),
            preferences.get(DEFAULT_OCR_LANGUAGE, DEFAULT_LANGUAGE),
            preferences.getInt(DEFAULT_PDF_DPI, DEFAULT_DPI),
            blankToNull(preferences.get(DirectoryKey.LAST_OPENED.key, "")),
            preferences.getInt(CACHE_LIMIT, DEFAULT_CACHE_LIMIT)
        );
    }

    public void save(Settings settings) {
        putNullable(TESSERACT_DATAPATH, settings.tesseractDatapath());
        preferences.put(DEFAULT_OCR_LANGUAGE, blankToDefault(settings.defaultOcrLanguage(), DEFAULT_LANGUAGE));
        preferences.putInt(DEFAULT_PDF_DPI, Math.max(1, settings.defaultPdfDpi()));
        putDirectory(DirectoryKey.LAST_OPENED, blankToNull(settings.lastOpenedDirectory()));
        preferences.putInt(CACHE_LIMIT, Math.max(1, settings.cacheLimit()));
    }

    public OcrSettings ocrSettings() {
        var settings = settings();
        return new OcrSettings(settings.defaultOcrLanguage(), settings.tesseractDatapath());
    }

    public RenderOptions renderOptions() {
        return new RenderOptions(settings().defaultPdfDpi());
    }

    public Optional<Path> directory(DirectoryKey key) {
        var value = blankToNull(preferences.get(key.key, ""));
        if (value == null) {
            return Optional.empty();
        }
        var path = Path.of(value);
        return Files.isDirectory(path) ? Optional.of(path) : Optional.empty();
    }

    public void rememberFile(DirectoryKey key, Path file) {
        if (file == null) {
            return;
        }
        var directory = Files.isDirectory(file) ? file : file.getParent();
        putDirectory(key, directory == null ? null : directory.toString());
        putDirectory(DirectoryKey.LAST_OPENED, directory == null ? null : directory.toString());
    }

    private void putDirectory(DirectoryKey key, String value) {
        putNullable(key.key, value);
    }

    private void putNullable(String key, String value) {
        var normalized = blankToNull(value);
        if (normalized == null) {
            preferences.remove(key);
        } else {
            preferences.put(key, normalized);
        }
    }

    private static String blankToDefault(String value, String defaultValue) {
        var normalized = blankToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String defaultDatapath() {
        if (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) {
            return "C:\\Program Files\\Tesseract-OCR\\tessdata";
        }
        return null;
    }
}
