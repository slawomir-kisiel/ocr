package pl.sk.ocr.extensions.standard.validation;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;
import static pl.sk.ocr.extensions.standard.StandardDescriptors.regexParameter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.validation.ValidationContext;
import pl.sk.ocr.extension.api.validation.ValidationRequest;
import pl.sk.ocr.extension.api.validation.ValidationResult;

public final class RegexValidator extends AbstractValidator {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("regex-validator", ExtensionType.VALIDATOR, "Regex", "Validates value against a Java regular expression.",
            regexParameter("pattern", "Pattern", "Java regular expression.", true, ""));
    }

    @Override
    public ValidationResult validate(ValidationRequest request, ValidationContext context) {
        var pattern = request.parameters().getString("pattern").orElse("");
        if (pattern.isBlank()) {
            return invalid("Pattern is required.");
        }
        try {
            return Pattern.compile(pattern).matcher(request.value()).matches() ? valid() : invalid("Value does not match pattern.");
        } catch (PatternSyntaxException e) {
            return invalid("Invalid regex pattern: " + e.getMessage());
        }
    }
}

