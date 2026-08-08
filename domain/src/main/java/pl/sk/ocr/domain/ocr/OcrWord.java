package pl.sk.ocr.domain.ocr;

import pl.sk.ocr.domain.Validation;

public record OcrWord(String text, BoundingBox boundingBox, Confidence confidence) {
    public OcrWord {
        text = Validation.requireText(text, "word text");
        boundingBox = Validation.requireNonNull(boundingBox, "bounding box");
        confidence = Validation.requireNonNull(confidence, "confidence");
    }
}
