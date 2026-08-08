package pl.sk.ocr.config;

import pl.sk.ocr.domain.Validation;

public record ConfigurationProblem(String code, String path, String message) {
    public ConfigurationProblem {
        code = Validation.requireText(code, "code");
        path = path == null ? "$" : path;
        message = Validation.requireText(message, "message");
    }
}
