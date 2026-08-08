package pl.sk.ocr.domain.trace;

import pl.sk.ocr.domain.Validation;

public record TraceImageRef(String id, String label) {
    public TraceImageRef {
        id = Validation.requireText(id, "trace image id");
        label = Validation.requireText(label, "trace image label");
    }
}
