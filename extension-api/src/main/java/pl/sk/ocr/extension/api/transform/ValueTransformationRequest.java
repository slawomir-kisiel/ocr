package pl.sk.ocr.extension.api.transform;

import pl.sk.ocr.extension.api.ExtensionParameters;

public record ValueTransformationRequest(String value, ExtensionParameters parameters) {
    public ValueTransformationRequest {
        value = value == null ? "" : value;
        parameters = parameters == null ? ExtensionParameters.empty() : parameters;
    }
}
