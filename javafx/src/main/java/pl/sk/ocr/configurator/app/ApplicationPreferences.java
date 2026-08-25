package pl.sk.ocr.configurator.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;
import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.core.document.RenderOptions;

public final class ApplicationPreferences {
    public enum DirectoryKey {
        OPEN_PROFILE("openProfileDirectory"),
        SAVE_PROFILE("saveProfileDirectory"),
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

    public enum RecentKey {
        PROFILE("recentProfiles"),
        CONFIGURATION("recentConfigurations"),
        DOCUMENT("recentDocuments");

        private final String key;

        RecentKey(String key) {
            this.key = key;
        }
    }

    public record Settings(String tesseractDatapath, String defaultOcrLanguage, int defaultPdfDpi,
                           String lastOpenedDirectory, int cacheLimit) {
    }

    public record WindowState(double x, double y, double width, double height, boolean maximized) {
    }

    private static final String TESSERACT_DATAPATH = "tesseractDatapath";
    private static final String DEFAULT_OCR_LANGUAGE = "defaultOcrLanguage";
    private static final String DEFAULT_PDF_DPI = "defaultPdfDpi";
    private static final String CACHE_LIMIT = "cacheLimit";
    private static final String WINDOW_X = "windowX";
    private static final String WINDOW_Y = "windowY";
    private static final String WINDOW_WIDTH = "windowWidth";
    private static final String WINDOW_HEIGHT = "windowHeight";
    private static final String WINDOW_MAXIMIZED = "windowMaximized";
    private static final String LEFT_PANEL_TAB = "leftPanelTab";
    private static final String RIGHT_PANEL_TAB = "rightPanelTab";
    private static final String LAST_PROFILE = "lastProfile";
    private static final int RECENT_LIMIT = 10;
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

    public Optional<WindowState> windowState() {
        var width = preferences.getDouble(WINDOW_WIDTH, -1);
        var height = preferences.getDouble(WINDOW_HEIGHT, -1);
        if (width <= 0 || height <= 0) {
            return Optional.empty();
        }
        return Optional.of(new WindowState(
            preferences.getDouble(WINDOW_X, Double.NaN),
            preferences.getDouble(WINDOW_Y, Double.NaN),
            width,
            height,
            preferences.getBoolean(WINDOW_MAXIMIZED, false)
        ));
    }

    public void saveWindowState(WindowState state) {
        if (state == null || state.width() <= 0 || state.height() <= 0) {
            return;
        }
        if (Double.isFinite(state.x())) {
            preferences.putDouble(WINDOW_X, state.x());
        }
        if (Double.isFinite(state.y())) {
            preferences.putDouble(WINDOW_Y, state.y());
        }
        preferences.putDouble(WINDOW_WIDTH, state.width());
        preferences.putDouble(WINDOW_HEIGHT, state.height());
        preferences.putBoolean(WINDOW_MAXIMIZED, state.maximized());
    }

    public Optional<String> leftPanelTab() {
        return Optional.ofNullable(blankToNull(preferences.get(LEFT_PANEL_TAB, "")));
    }

    public void saveLeftPanelTab(String tab) {
        putNullable(LEFT_PANEL_TAB, tab);
    }

    public Optional<String> rightPanelTab() {
        return Optional.ofNullable(blankToNull(preferences.get(RIGHT_PANEL_TAB, "")));
    }

    public void saveRightPanelTab(String tab) {
        putNullable(RIGHT_PANEL_TAB, tab);
    }

    public Optional<Path> lastProfile() {
        var value = blankToNull(preferences.get(LAST_PROFILE, ""));
        if (value == null) {
            return Optional.empty();
        }
        var path = Path.of(value);
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }

    public void saveLastProfile(Path profile) {
        if (profile == null) {
            preferences.remove(LAST_PROFILE);
            return;
        }
        preferences.put(LAST_PROFILE, profile.toAbsolutePath().normalize().toString());
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

    public List<Path> recentFiles(RecentKey key) {
        var value = preferences.get(key.key, "");
        if (value.isBlank()) {
            return List.of();
        }
        return value.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .map(Path::of)
            .distinct()
            .toList();
    }

    public void rememberRecentFile(RecentKey key, Path file) {
        if (file == null) {
            return;
        }
        var normalized = file.toAbsolutePath().normalize();
        var files = new ArrayList<Path>();
        files.add(normalized);
        for (var recent : recentFiles(key)) {
            var candidate = recent.toAbsolutePath().normalize();
            if (!candidate.equals(normalized)) {
                files.add(candidate);
            }
            if (files.size() >= RECENT_LIMIT) {
                break;
            }
        }
        preferences.put(key.key, files.stream().map(Path::toString).collect(java.util.stream.Collectors.joining("\n")));
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
