package pl.sk.ocr.config.dto;

public record OcrSettingsDto(String language, String datapath, ExtensionRefDto detector) {
    public OcrSettingsDto(String language, String datapath) {
        this(language, datapath, null);
    }
}
