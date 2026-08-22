package pl.sk.ocr.domain.ocr;

import java.util.List;
import pl.sk.ocr.domain.Validation;

public record OcrArea(BoundingBox boundingBox, List<OcrParagraph> paragraphs) {
    public OcrArea {
        boundingBox = Validation.requireNonNull(boundingBox, "area bounding box");
        paragraphs = List.copyOf(Validation.requireNoNulls(paragraphs == null ? List.of() : paragraphs, "paragraphs"));
    }

    static List<OcrArea> fromWords(List<OcrWord> words) {
        var normalized = List.copyOf(Validation.requireNoNulls(words == null ? List.of() : words, "words"));
        if (normalized.isEmpty()) {
            return List.of();
        }
        var line = new OcrLine(BoundingBoxes.unionWords(normalized), normalized);
        var paragraph = new OcrParagraph(line.boundingBox(), List.of(line));
        return List.of(new OcrArea(paragraph.boundingBox(), List.of(paragraph)));
    }
}
