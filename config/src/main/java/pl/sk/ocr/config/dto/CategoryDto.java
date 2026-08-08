package pl.sk.ocr.config.dto;

import java.util.List;

public record CategoryDto(
    String schemaVersion,
    String id,
    String version,
    String displayName,
    String description,
    PageSelectionDto pages,
    OcrSettingsDto ocr,
    IdentificationDto identification,
    GeometryDto geometry,
    List<AnchorDto> anchors,
    List<FieldDto> fields
) {
}
