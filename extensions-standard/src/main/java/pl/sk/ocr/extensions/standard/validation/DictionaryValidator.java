package pl.sk.ocr.extensions.standard.validation;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;
import static pl.sk.ocr.extensions.standard.StandardDescriptors.stringParameter;
import static pl.sk.ocr.extensions.standard.TextSupport.normalize;

import java.util.Arrays;
import java.util.stream.Collectors;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.validation.ValidationContext;
import pl.sk.ocr.extension.api.validation.ValidationRequest;
import pl.sk.ocr.extension.api.validation.ValidationResult;

public final class DictionaryValidator extends AbstractValidator {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("dictionary", ExtensionType.VALIDATOR, "Dictionary", "Validates value against comma-separated allowed values.",
            stringParameter("values", "Values", "Comma-separated allowed values.", true, ""));
    }

    @Override
    public ValidationResult validate(ValidationRequest request, ValidationContext context) {
        var values = request.parameters().getString("values").orElse("");
        var allowed = Arrays.stream(values.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .map(pl.sk.ocr.extensions.standard.TextSupport::normalize)
            .collect(Collectors.toSet());
        if (allowed.isEmpty()) {
            return invalid("Dictionary values are required.");
        }
        return allowed.contains(normalize(request.value())) ? valid() : invalid("Value is not in dictionary.");
    }
}

