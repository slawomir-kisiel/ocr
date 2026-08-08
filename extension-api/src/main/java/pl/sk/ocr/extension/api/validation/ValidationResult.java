package pl.sk.ocr.extension.api.validation;

import java.util.List;
import pl.sk.ocr.domain.Validation;

public record ValidationResult(ValidationStatus status, List<String> messages) {
    public ValidationResult {
        status = Validation.requireNonNull(status, "status");
        messages = List.copyOf(Validation.requireNoNulls(messages == null ? List.of() : messages, "messages"));
    }
}
