package pl.sk.ocr.extensions.standard.validation;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;
import static pl.sk.ocr.extensions.standard.TextSupport.digits;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.validation.ValidationRequest;
import pl.sk.ocr.extension.api.validation.ValidationResult;
import pl.sk.ocr.extension.api.validation.ValidationContext;

public final class PeselValidator extends AbstractValidator {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("pesel", ExtensionType.VALIDATOR, "PESEL", "Validates Polish PESEL checksum.");
    }

    @Override
    public ValidationResult validate(ValidationRequest request, ValidationContext context) {
        var value = digits(request.value());
        if (value.length() != 11) {
            return invalid("PESEL must contain 11 digits.");
        }
        var weights = new int[] {1, 3, 7, 9, 1, 3, 7, 9, 1, 3};
        var sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Character.digit(value.charAt(i), 10) * weights[i];
        }
        var check = (10 - sum % 10) % 10;
        return check == Character.digit(value.charAt(10), 10) ? valid() : invalid("Invalid PESEL checksum.");
    }
}

