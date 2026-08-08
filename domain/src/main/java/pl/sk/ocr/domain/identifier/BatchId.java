package pl.sk.ocr.domain.identifier;

import pl.sk.ocr.domain.Validation;

public record BatchId(String value) {
    public BatchId {
        value = Validation.requireText(value, "batch id");
    }
}
