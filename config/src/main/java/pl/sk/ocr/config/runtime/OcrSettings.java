package pl.sk.ocr.config.runtime;

public record OcrSettings(String language, String datapath) {
    public static OcrSettings defaults() {
        return new OcrSettings("pol", null);
    }
}
