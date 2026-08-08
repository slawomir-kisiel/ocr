package pl.sk.ocr.domain.identifier;

import java.util.regex.Pattern;
import pl.sk.ocr.domain.Validation;

public record ExtensionId(String value) {
    private static final Pattern FORMAT = Pattern.compile("[a-z0-9][a-z0-9-]*");

    public ExtensionId {
        value = Validation.requireText(value, "extension id");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("extension id must match [a-z0-9][a-z0-9-]*");
        }
    }
}
