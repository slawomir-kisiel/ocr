package pl.sk.ocr.domain.identifier;

import pl.sk.ocr.domain.Validation;

public record CategoryId(String value) {
    public CategoryId {
        value = Validation.requireText(value, "category id");
    }
}
