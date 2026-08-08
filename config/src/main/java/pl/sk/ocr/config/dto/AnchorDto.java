package pl.sk.ocr.config.dto;

public record AnchorDto(
    String id,
    Integer page,
    ExtensionRefDto detector,
    Boolean required,
    ReferenceFeatureDto referenceFeature,
    RegionDto searchRegion
) {
}
