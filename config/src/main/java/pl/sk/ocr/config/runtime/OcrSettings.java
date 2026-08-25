package pl.sk.ocr.config.runtime;

public record OcrSettings(String language, String datapath, ExtensionRef detector) {
    public OcrSettings(String language, String datapath) {
        this(language, datapath, null);
    }

    public static OcrSettings defaults() {
        return new OcrSettings("pol", null, null);
    }
}
