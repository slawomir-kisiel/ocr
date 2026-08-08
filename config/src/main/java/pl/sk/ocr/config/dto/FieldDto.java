package pl.sk.ocr.config.dto;

import java.util.List;

public record FieldDto(
    String id,
    String displayName,
    Integer page,
    RegionDto region,
    Boolean required,
    OcrSettingsDto ocr,
    OutputDto output,
    List<ExtensionRefDto> imageProcessors,
    List<ExtensionRefDto> transformers,
    List<ExtensionRefDto> validators
) {
}
