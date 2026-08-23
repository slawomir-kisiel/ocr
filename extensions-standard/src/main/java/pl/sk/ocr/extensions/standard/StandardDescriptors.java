package pl.sk.ocr.extensions.standard;

import java.util.List;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionParameterDescriptor;
import pl.sk.ocr.extension.api.ExtensionParameterType;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.ParameterConstraints;

public final class StandardDescriptors {
    private static final String VERSION = "1.0";

    private StandardDescriptors() {
    }

    public static ExtensionDescriptor extensionDescriptor(String id, ExtensionType type, String displayName, String description,
                                                          ExtensionParameterDescriptor... parameters) {
        return new ExtensionDescriptor(new ExtensionId(id), type, displayName, description, VERSION, List.of(parameters));
    }

    public static ExtensionParameterDescriptor stringParameter(String name, String displayName, String description,
                                                              boolean required, String defaultValue) {
        return new ExtensionParameterDescriptor(name, displayName, description, ExtensionParameterType.STRING, required,
            null, defaultValue);
    }

    public static ExtensionParameterDescriptor integerParameter(String name, String displayName, String description,
                                                               boolean required, Integer min, Integer max, Integer defaultValue) {
        return new ExtensionParameterDescriptor(name, displayName, description, ExtensionParameterType.INTEGER, required,
            new ParameterConstraints(min, max, null, List.of()), defaultValue);
    }

    public static ExtensionParameterDescriptor decimalParameter(String name, String displayName, String description,
                                                               boolean required, Double min, Double max, Double defaultValue) {
        return new ExtensionParameterDescriptor(name, displayName, description, ExtensionParameterType.DECIMAL, required,
            new ParameterConstraints(min, max, null, List.of()), defaultValue);
    }

    public static ExtensionParameterDescriptor regexParameter(String name, String displayName, String description,
                                                             boolean required, String defaultValue) {
        return new ExtensionParameterDescriptor(name, displayName, description, ExtensionParameterType.REGEX, required,
            null, defaultValue);
    }

    public static ExtensionParameterDescriptor booleanParameter(String name, String displayName, String description,
                                                               boolean required, Boolean defaultValue) {
        return new ExtensionParameterDescriptor(name, displayName, description, ExtensionParameterType.BOOLEAN, required,
            null, defaultValue);
    }
}

