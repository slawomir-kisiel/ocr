package pl.sk.ocr.domain.ocr;

import java.util.Map;
import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.domain.identifier.PageNumber;

public record OcrResult(Map<PageNumber, OcrText> pages) {
    public OcrResult {
        pages = Map.copyOf(Validation.requireNonNull(pages, "pages"));
    }
}
