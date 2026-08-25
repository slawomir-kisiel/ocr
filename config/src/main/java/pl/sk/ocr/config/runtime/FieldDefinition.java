package pl.sk.ocr.config.runtime;

import java.util.List;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.identifier.AnchorId;
import pl.sk.ocr.domain.identifier.FieldId;

public record FieldDefinition(
    FieldId id,
    String displayName,
    int page,
    Region region,
    boolean required,
    OcrSettings ocr,
    boolean exported,
    String columnName,
    List<AnchorId> referenceAnchorIds,
    List<ExtensionRef> imageProcessors,
    List<ExtensionRef> transformers,
    List<ExtensionRef> validators
) {
    public FieldDefinition(
        FieldId id,
        String displayName,
        int page,
        Region region,
        boolean required,
        OcrSettings ocr,
        boolean exported,
        String columnName,
        List<ExtensionRef> imageProcessors,
        List<ExtensionRef> transformers,
        List<ExtensionRef> validators
    ) {
        this(id, displayName, page, region, required, ocr, exported, columnName, List.of(), imageProcessors, transformers, validators);
    }

    public FieldDefinition {
        referenceAnchorIds = List.copyOf(referenceAnchorIds == null ? List.of() : referenceAnchorIds);
        imageProcessors = List.copyOf(imageProcessors == null ? List.of() : imageProcessors);
        transformers = List.copyOf(transformers == null ? List.of() : transformers);
        validators = List.copyOf(validators == null ? List.of() : validators);
    }

    public FieldDefinition withResolvedRegion(Region resolvedRegion) {
        return new FieldDefinition(id, displayName, page, resolvedRegion, required, ocr, exported, columnName,
            referenceAnchorIds, imageProcessors, transformers, validators);
    }
}
