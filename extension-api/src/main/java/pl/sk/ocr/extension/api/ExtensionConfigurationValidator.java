package pl.sk.ocr.extension.api;

import java.util.List;

public interface ExtensionConfigurationValidator {
    List<ExtensionConfigurationProblem> validate(ExtensionDescriptor descriptor, ExtensionParameters parameters);
}
