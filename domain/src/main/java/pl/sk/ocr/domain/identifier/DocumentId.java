package pl.sk.ocr.domain.identifier;

import pl.sk.ocr.domain.Validation;

public record DocumentId(String value) {
    public DocumentId {
        value = Validation.requireText(value, "document id");
    }
}
