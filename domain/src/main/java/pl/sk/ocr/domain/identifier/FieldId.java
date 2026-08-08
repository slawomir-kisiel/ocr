package pl.sk.ocr.domain.identifier;

import pl.sk.ocr.domain.Validation;

public record FieldId(String value) {
    public FieldId {
        value = Validation.requireText(value, "field id");
    }
}
