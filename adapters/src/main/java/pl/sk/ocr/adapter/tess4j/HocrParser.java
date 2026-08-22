package pl.sk.ocr.adapter.tess4j;

import java.util.List;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.ocr.BoundingBox;
import pl.sk.ocr.domain.ocr.Confidence;
import pl.sk.ocr.domain.ocr.OcrArea;
import pl.sk.ocr.domain.ocr.OcrLine;
import pl.sk.ocr.domain.ocr.OcrParagraph;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.ocr.OcrWord;

final class HocrParser {
    private static final Pattern BBOX = Pattern.compile("\\bbbox\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\b");
    private static final Pattern WORD_CONFIDENCE = Pattern.compile("\\bx_wconf\\s+(\\d+(?:\\.\\d+)?)\\b");

    OcrText parse(String hocr) {
        var document = Jsoup.parse(hocr == null ? "" : hocr);
        var areas = document.select(".ocr_carea").stream()
            .map(this::parseArea)
            .filter(area -> !area.paragraphs().isEmpty())
            .toList();
        var text = areas.stream()
            .flatMap(area -> area.paragraphs().stream())
            .flatMap(paragraph -> paragraph.lines().stream())
            .map(this::lineText)
            .filter(value -> !value.isBlank())
            .reduce((left, right) -> left + System.lineSeparator() + right)
            .orElse("");
        return new OcrText(text, hocr, areas);
    }

    private OcrArea parseArea(Element element) {
        var paragraphs = element.select("> .ocr_par").stream()
            .map(this::parseParagraph)
            .filter(paragraph -> !paragraph.lines().isEmpty())
            .toList();
        return new OcrArea(parseBoundingBox(element), paragraphs);
    }

    private OcrParagraph parseParagraph(Element element) {
        var lines = element.select("> .ocr_line, > .ocrx_line").stream()
            .map(this::parseLine)
            .filter(line -> !line.words().isEmpty())
            .toList();
        return new OcrParagraph(parseBoundingBox(element), lines);
    }

    private OcrLine parseLine(Element element) {
        var words = element.select("> .ocrx_word").stream()
            .map(this::parseWord)
            .filter(word -> !word.text().isBlank())
            .toList();
        return new OcrLine(parseBoundingBox(element), words);
    }

    private OcrWord parseWord(Element element) {
        return new OcrWord(element.text(), parseBoundingBox(element), parseConfidence(element));
    }

    private String lineText(OcrLine line) {
        return line.words().stream()
            .map(OcrWord::text)
            .reduce((left, right) -> left + " " + right)
            .orElse("");
    }

    private BoundingBox parseBoundingBox(Element element) {
        var matcher = BBOX.matcher(element.attr("title"));
        if (!matcher.find()) {
            return new BoundingBox(new Region(0, 0, 0, 0));
        }
        var x0 = Double.parseDouble(matcher.group(1));
        var y0 = Double.parseDouble(matcher.group(2));
        var x1 = Double.parseDouble(matcher.group(3));
        var y1 = Double.parseDouble(matcher.group(4));
        return new BoundingBox(new Region(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0)));
    }

    private Confidence parseConfidence(Element element) {
        var matcher = WORD_CONFIDENCE.matcher(element.attr("title"));
        if (!matcher.find()) {
            return new Confidence(1.0);
        }
        return new Confidence(Math.min(1.0, Math.max(0.0, Double.parseDouble(matcher.group(1)) / 100.0)));
    }
}
