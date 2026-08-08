package pl.sk.ocr.extension.api;

import pl.sk.ocr.domain.Validation;

public record ExtensionConfigurationProblem(String parameterName, String message) {
    public ExtensionConfigurationProblem {
        parameterName = Validation.requireText(parameterName, "parameter name");
        message = Validation.requireText(message, "message");
    }
}
