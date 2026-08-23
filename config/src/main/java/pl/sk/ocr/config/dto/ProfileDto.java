package pl.sk.ocr.config.dto;

public record ProfileDto(
    String schemaVersion,
    String id,
    String version,
    String displayName,
    String description,
    ProfileCategoriesDto categories,
    ProfilePreprocessingDto preprocessing,
    DirectoriesDto directories,
    ProcessingDto processing,
    OcrSettingsDto ocr,
    TraceDto trace,
    ProfileOutputDto output
) {
}
