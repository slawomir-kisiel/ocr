package pl.sk.ocr.domain.identifier;

import pl.sk.ocr.domain.Validation;

public record AnchorId(String value) {
    public AnchorId {
        value = Validation.requireText(value, "anchor id");
    }
}
