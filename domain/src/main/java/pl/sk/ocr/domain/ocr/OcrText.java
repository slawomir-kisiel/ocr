package pl.sk.ocr.domain.ocr;

import java.util.List;
import pl.sk.ocr.domain.Validation;

public record OcrText(String value, List<OcrWord> words) {
    public OcrText {
        value = value == null ? "" : value;
        words = List.copyOf(Validation.requireNoNulls(words == null ? List.of() : words, "words"));
    }
}
