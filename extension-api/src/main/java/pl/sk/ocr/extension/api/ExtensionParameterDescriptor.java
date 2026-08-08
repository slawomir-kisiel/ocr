package pl.sk.ocr.extension.api;

import pl.sk.ocr.domain.Validation;

public record ExtensionParameterDescriptor(
    String name,
    String displayName,
    String description,
    ExtensionParameterType type,
    boolean required,
    ParameterConstraints constraints,
    Object defaultValue
) {
    public ExtensionParameterDescriptor {
        name = Validation.requireText(name, "parameter name");
        displayName = Validation.requireText(displayName, "parameter display name");
        description = description == null ? "" : description;
        type = Validation.requireNonNull(type, "parameter type");
    }
}
