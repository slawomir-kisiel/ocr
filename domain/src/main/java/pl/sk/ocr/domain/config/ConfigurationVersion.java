package pl.sk.ocr.domain.config;

import pl.sk.ocr.domain.Validation;

public record ConfigurationVersion(String value) {
    public ConfigurationVersion {
        value = Validation.requireText(value, "configuration version");
    }
}
