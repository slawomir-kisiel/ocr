package pl.sk.ocr.extension.api;

import java.util.List;
import java.util.regex.Pattern;
import pl.sk.ocr.domain.Validation;

public record ParameterConstraints(Number min, Number max, Pattern pattern, List<String> allowedValues) {
    public ParameterConstraints {
        allowedValues = List.copyOf(Validation.requireNoNulls(allowedValues == null ? List.of() : allowedValues, "allowed values"));
    }
}
