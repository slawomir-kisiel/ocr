package pl.sk.ocr.core.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.ocr.BoundingBox;
import pl.sk.ocr.domain.ocr.OcrArea;
import pl.sk.ocr.domain.ocr.OcrLine;
import pl.sk.ocr.domain.ocr.OcrParagraph;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.ocr.OcrWord;

public final class OcrTextRegionExtractor {

    public ExtractionResult extract(OcrText source, Region region) {
        if (source == null || region == null || source.areas().isEmpty()) {
            return new ExtractionResult(new OcrText("", "", List.of()), 0, 0);
        }
        var selectedAreas = new ArrayList<OcrArea>();
        for (var area : source.areas()) {
            var paragraphs = new ArrayList<OcrParagraph>();
            for (var paragraph : area.paragraphs()) {
                var lines = new ArrayList<OcrLine>();
                for (var line : paragraph.lines()) {
                    var words = line.words().stream()
                        .filter(word -> contains(region, word))
                        .toList();
                    if (!words.isEmpty()) {
                        lines.add(new OcrLine(union(words), words));
                    }
                }
                if (!lines.isEmpty()) {
                    paragraphs.add(new OcrParagraph(unionLines(lines), lines));
                }
            }
            if (!paragraphs.isEmpty()) {
                selectedAreas.add(new OcrArea(unionParagraphs(paragraphs), paragraphs));
            }
        }
        var text = selectedAreas.stream()
            .flatMap(area -> area.paragraphs().stream())
            .flatMap(paragraph -> paragraph.lines().stream())
            .map(line -> line.words().stream()
                .map(OcrWord::text)
                .collect(Collectors.joining(" ")))
            .collect(Collectors.joining(System.lineSeparator()));
        var extracted = new OcrText(text, source.hocr(), selectedAreas);
        return new ExtractionResult(extracted, extracted.words().size(), extracted.lines().size());
    }

    private boolean contains(Region region, OcrWord word) {
        var wordRegion = word.boundingBox().region();
        return region.contains(wordRegion.topLeft()) && region.contains(wordRegion.bottomRight());
    }

    private BoundingBox unionLines(List<OcrLine> lines) {
        return unionRegions(lines.stream().map(line -> line.boundingBox().region()).toList());
    }

    private BoundingBox unionParagraphs(List<OcrParagraph> paragraphs) {
        return unionRegions(paragraphs.stream().map(paragraph -> paragraph.boundingBox().region()).toList());
    }

    private BoundingBox union(List<OcrWord> words) {
        return unionRegions(words.stream().map(word -> word.boundingBox().region()).toList());
    }

    private BoundingBox unionRegions(List<Region> regions) {
        if (regions.isEmpty()) {
            return new BoundingBox(new Region(0, 0, 0, 0));
        }
        var minX = Double.POSITIVE_INFINITY;
        var minY = Double.POSITIVE_INFINITY;
        var maxX = Double.NEGATIVE_INFINITY;
        var maxY = Double.NEGATIVE_INFINITY;
        for (var region : regions) {
            minX = Math.min(minX, region.x());
            minY = Math.min(minY, region.y());
            maxX = Math.max(maxX, region.x() + region.width());
            maxY = Math.max(maxY, region.y() + region.height());
        }
        return new BoundingBox(new Region(minX, minY, maxX - minX, maxY - minY));
    }

    public record ExtractionResult(OcrText ocrText, int selectedWords, int selectedLines) {
    }
}
