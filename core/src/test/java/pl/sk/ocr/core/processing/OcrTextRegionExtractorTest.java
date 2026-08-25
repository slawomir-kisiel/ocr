package pl.sk.ocr.core.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.ocr.BoundingBox;
import pl.sk.ocr.domain.ocr.Confidence;
import pl.sk.ocr.domain.ocr.OcrArea;
import pl.sk.ocr.domain.ocr.OcrLine;
import pl.sk.ocr.domain.ocr.OcrParagraph;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.ocr.OcrWord;

class OcrTextRegionExtractorTest {

    private final OcrTextRegionExtractor extractor = new OcrTextRegionExtractor();

    @Test
    void rebuildsOcrStructureWithWordsFullyContainedInRegion() {
        var source = ocr(
            line(word("left", 0, 0, 5, 5), word("inside", 10, 0, 8, 5)),
            line(word("next", 10, 10, 8, 5), word("right", 40, 10, 8, 5))
        );

        var result = extractor.extract(source, new Region(8, 0, 20, 20));

        assertThat(result.selectedWords()).isEqualTo(2);
        assertThat(result.selectedLines()).isEqualTo(2);
        assertThat(result.ocrText().value()).isEqualTo("inside" + System.lineSeparator() + "next");
        assertThat(result.ocrText().areas()).hasSize(1);
        assertThat(result.ocrText().paragraphs()).hasSize(1);
        assertThat(result.ocrText().lines()).hasSize(2);
        assertThat(result.ocrText().words()).extracting(OcrWord::text).containsExactly("inside", "next");
    }

    @Test
    void returnsEmptyOcrWhenNoWordsMatchRegion() {
        var source = ocr(line(word("outside", 0, 0, 5, 5)));

        var result = extractor.extract(source, new Region(20, 20, 10, 10));

        assertThat(result.selectedWords()).isZero();
        assertThat(result.selectedLines()).isZero();
        assertThat(result.ocrText().value()).isEmpty();
        assertThat(result.ocrText().areas()).isEmpty();
    }

    private static OcrText ocr(OcrLine... lines) {
        var paragraph = new OcrParagraph(new BoundingBox(new Region(0, 0, 50, 20)), List.of(lines));
        var area = new OcrArea(paragraph.boundingBox(), List.of(paragraph));
        return new OcrText("source", "<html></html>", List.of(area));
    }

    private static OcrLine line(OcrWord... words) {
        return new OcrLine(new BoundingBox(new Region(0, 0, 50, 10)), List.of(words));
    }

    private static OcrWord word(String text, double x, double y, double width, double height) {
        return new OcrWord(text, new BoundingBox(new Region(x, y, width, height)), new Confidence(0.9));
    }
}
