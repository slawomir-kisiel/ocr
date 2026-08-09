package pl.sk.ocr.extensions.standard.validation;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;
import static pl.sk.ocr.extensions.standard.TextSupport.digits;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.validation.ValidationContext;
import pl.sk.ocr.extension.api.validation.ValidationRequest;
import pl.sk.ocr.extension.api.validation.ValidationResult;

public final class NipValidator extends AbstractValidator {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("nip", ExtensionType.VALIDATOR, "NIP", "Validates Polish NIP checksum.");
    }

    @Override
    public ValidationResult validate(ValidationRequest request, ValidationContext context) {
        var value = digits(request.value());
        if (value.length() != 10) {
            return invalid("NIP must contain 10 digits.");
        }
        var weights = new int[] {6, 5, 7, 2, 3, 4, 5, 6, 7};
        var sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Character.digit(value.charAt(i), 10) * weights[i];
        }
        var check = sum % 11;
        return check != 10 && check == Character.digit(value.charAt(9), 10) ? valid() : invalid("Invalid NIP checksum.");
    }
}

