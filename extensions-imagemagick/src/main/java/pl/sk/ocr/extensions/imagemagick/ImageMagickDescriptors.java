package pl.sk.ocr.extensions.imagemagick;

import java.util.List;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionParameterDescriptor;
import pl.sk.ocr.extension.api.ExtensionParameterType;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.ParameterConstraints;

final class ImageMagickDescriptors {
    static final String VERSION = "1.0";

    private ImageMagickDescriptors() {
    }

    static ExtensionDescriptor processor(String id, String displayName, String description,
                                         ExtensionParameterDescriptor... parameters) {
        return new ExtensionDescriptor(new ExtensionId(id), ExtensionType.IMAGE_PROCESSOR, displayName, description,
            VERSION, List.of(parameters));
    }

    static ExtensionParameterDescriptor integerParameter(String name, String displayName, String description,
                                                         boolean required, Integer min, Integer max, Integer defaultValue) {
        return new ExtensionParameterDescriptor(name, displayName, description, ExtensionParameterType.INTEGER, required,
            new ParameterConstraints(min, max, null, List.of()), defaultValue);
    }

    static ExtensionParameterDescriptor decimalParameter(String name, String displayName, String description,
                                                         boolean required, Double min, Double max, Double defaultValue) {
        return new ExtensionParameterDescriptor(name, displayName, description, ExtensionParameterType.DECIMAL, required,
            new ParameterConstraints(min, max, null, List.of()), defaultValue);
    }

    static ExtensionParameterDescriptor booleanParameter(String name, String displayName, String description,
                                                         boolean required, Boolean defaultValue) {
        return new ExtensionParameterDescriptor(name, displayName, description, ExtensionParameterType.BOOLEAN, required,
            null, defaultValue);
    }

    static ExtensionParameterDescriptor enumParameter(String name, String displayName, String description,
                                                      boolean required, List<String> allowedValues, String defaultValue) {
        return new ExtensionParameterDescriptor(name, displayName, description, ExtensionParameterType.ENUM, required,
            new ParameterConstraints(null, null, null, allowedValues), defaultValue);
    }
}
