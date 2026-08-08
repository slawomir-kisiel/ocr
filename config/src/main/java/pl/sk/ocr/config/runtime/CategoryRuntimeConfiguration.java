package pl.sk.ocr.config.runtime;

import java.util.List;
import pl.sk.ocr.domain.config.ConfigurationVersion;
import pl.sk.ocr.domain.identifier.CategoryId;

public record CategoryRuntimeConfiguration(
    CategoryId id,
    ConfigurationVersion version,
    String displayName,
    PageSelection pages,
    OcrSettings ocr,
    GeometryConfiguration geometry,
    List<IdentificationGroup> identificationGroups,
    List<AnchorDefinition> anchors,
    List<FieldDefinition> fields
) {
    public CategoryRuntimeConfiguration {
        identificationGroups = List.copyOf(identificationGroups == null ? List.of() : identificationGroups);
        anchors = List.copyOf(anchors == null ? List.of() : anchors);
        fields = List.copyOf(fields == null ? List.of() : fields);
    }
}
