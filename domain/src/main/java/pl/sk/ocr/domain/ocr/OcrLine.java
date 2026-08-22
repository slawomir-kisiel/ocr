package pl.sk.ocr.domain.ocr;

import java.util.List;
import pl.sk.ocr.domain.Validation;

public record OcrLine(BoundingBox boundingBox, List<OcrWord> words) {
    public OcrLine {
        boundingBox = Validation.requireNonNull(boundingBox, "line bounding box");
        words = List.copyOf(Validation.requireNoNulls(words == null ? List.of() : words, "words"));
    }
}
