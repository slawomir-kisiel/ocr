package pl.sk.ocr.extension.api.matcher;

import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.extension.api.ExtensionParameters;

public record MatchRequest(String expected, String actual, ExtensionParameters parameters) {
    public MatchRequest {
        expected = Validation.requireText(expected, "expected");
        actual = actual == null ? "" : actual;
        parameters = parameters == null ? ExtensionParameters.empty() : parameters;
    }
}
