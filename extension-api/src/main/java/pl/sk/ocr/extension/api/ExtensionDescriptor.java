package pl.sk.ocr.extension.api;

import java.util.List;
import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.domain.identifier.ExtensionId;

public record ExtensionDescriptor(
    ExtensionId id,
    ExtensionType type,
    String displayName,
    String description,
    String version,
    List<ExtensionParameterDescriptor> parameters
) {
    public ExtensionDescriptor {
        id = Validation.requireNonNull(id, "extension id");
        type = Validation.requireNonNull(type, "extension type");
        displayName = Validation.requireText(displayName, "display name");
        description = description == null ? "" : description;
        version = Validation.requireText(version, "version");
        parameters = List.copyOf(Validation.requireNoNulls(parameters == null ? List.of() : parameters, "parameters"));
    }
}
