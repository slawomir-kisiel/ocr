package pl.sk.ocr.domain.ocr;

import java.util.List;
import pl.sk.ocr.domain.Validation;

public record OcrParagraph(BoundingBox boundingBox, List<OcrLine> lines) {
    public OcrParagraph {
        boundingBox = Validation.requireNonNull(boundingBox, "paragraph bounding box");
        lines = List.copyOf(Validation.requireNoNulls(lines == null ? List.of() : lines, "lines"));
    }
}
