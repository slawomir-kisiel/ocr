package pl.sk.ocr.extension.api;

import pl.sk.ocr.domain.Validation;

public class ExtensionException extends RuntimeException {
    private final String code;

    public ExtensionException(String code, String message) {
        super(Validation.requireText(message, "message"));
        this.code = Validation.requireText(code, "code");
    }

    public String code() {
        return code;
    }
}
