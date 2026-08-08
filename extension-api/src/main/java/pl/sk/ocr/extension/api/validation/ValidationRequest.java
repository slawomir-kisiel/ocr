package pl.sk.ocr.extension.api.validation;

import pl.sk.ocr.extension.api.ExtensionParameters;

public record ValidationRequest(String value, ExtensionParameters parameters) {
    public ValidationRequest {
        value = value == null ? "" : value;
        parameters = parameters == null ? ExtensionParameters.empty() : parameters;
    }
}
