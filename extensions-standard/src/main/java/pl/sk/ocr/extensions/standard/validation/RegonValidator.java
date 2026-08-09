package pl.sk.ocr.extensions.standard.validation;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;
import static pl.sk.ocr.extensions.standard.TextSupport.digits;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.validation.ValidationContext;
import pl.sk.ocr.extension.api.validation.ValidationRequest;
import pl.sk.ocr.extension.api.validation.ValidationResult;

public final class RegonValidator extends AbstractValidator {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("regon", ExtensionType.VALIDATOR, "REGON", "Validates Polish REGON checksum for 9 or 14 digits.");
    }

    @Override
    public ValidationResult validate(ValidationRequest request, ValidationContext context) {
        var value = digits(request.value());
        if (value.length() == 9) {
            return validChecksum(value, new int[] {8, 9, 2, 3, 4, 5, 6, 7}) ? valid() : invalid("Invalid REGON checksum.");
        }
        if (value.length() == 14) {
            return validChecksum(value, new int[] {2, 4, 8, 5, 0, 9, 7, 3, 6, 1, 2, 4, 8}) ? valid() : invalid("Invalid REGON checksum.");
        }
        return invalid("REGON must contain 9 or 14 digits.");
    }

    private boolean validChecksum(String value, int[] weights) {
        var sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Character.digit(value.charAt(i), 10) * weights[i];
        }
        var check = sum % 11;
        if (check == 10) {
            check = 0;
        }
        return check == Character.digit(value.charAt(weights.length), 10);
    }
}

