package pl.sk.ocr.domain.ocr;

import java.util.List;
import java.util.stream.Collectors;
import pl.sk.ocr.domain.Validation;

public record OcrText(String value, String hocr, List<OcrArea> areas) {
    public OcrText {
        value = value == null ? "" : value;
        hocr = hocr == null ? "" : hocr;
        areas = List.copyOf(Validation.requireNoNulls(areas == null ? List.of() : areas, "areas"));
    }

    public OcrText(String value, List<OcrWord> words) {
        this(value, "", OcrArea.fromWords(words));
    }

    public List<OcrParagraph> paragraphs() {
        return areas.stream()
            .flatMap(area -> area.paragraphs().stream())
            .toList();
    }

    public List<OcrLine> lines() {
        return paragraphs().stream()
            .flatMap(paragraph -> paragraph.lines().stream())
            .toList();
    }

    public List<OcrWord> words() {
        return lines().stream()
            .flatMap(line -> line.words().stream())
            .toList();
    }

    public String textFromWords() {
        return lines().stream()
            .map(line -> line.words().stream()
                .map(OcrWord::text)
                .collect(Collectors.joining(" ")))
            .collect(Collectors.joining(System.lineSeparator()));
    }
}
