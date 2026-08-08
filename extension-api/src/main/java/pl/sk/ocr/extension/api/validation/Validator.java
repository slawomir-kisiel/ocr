package pl.sk.ocr.extension.api.validation;

import pl.sk.ocr.extension.api.Extension;

public interface Validator extends Extension {
    ValidationResult validate(ValidationRequest request, ValidationContext context);
}
