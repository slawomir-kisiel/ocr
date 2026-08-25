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
    List<String> referenceAnchorIds,
    List<ExtensionRefDto> imageProcessors,
    List<ExtensionRefDto> transformers,
    List<ExtensionRefDto> validators
) {
    public FieldDto(
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
        this(id, displayName, page, region, required, ocr, output, List.of(), imageProcessors, transformers, validators);
    }

    public FieldDto {
        referenceAnchorIds = referenceAnchorIds == null ? List.of() : List.copyOf(referenceAnchorIds);
        imageProcessors = imageProcessors == null ? List.of() : List.copyOf(imageProcessors);
        transformers = transformers == null ? List.of() : List.copyOf(transformers);
        validators = validators == null ? List.of() : List.copyOf(validators);
    }
}
