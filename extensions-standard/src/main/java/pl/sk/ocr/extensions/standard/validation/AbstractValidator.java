package pl.sk.ocr.extensions.standard.validation;

import java.util.List;
import pl.sk.ocr.extension.api.validation.ValidationResult;
import pl.sk.ocr.extension.api.validation.ValidationStatus;

abstract class AbstractValidator implements pl.sk.ocr.extension.api.validation.Validator {
    ValidationResult valid() {
        return new ValidationResult(ValidationStatus.VALID, List.of());
    }

    ValidationResult invalid(String message) {
        return new ValidationResult(ValidationStatus.INVALID, List.of(message));
    }
}

