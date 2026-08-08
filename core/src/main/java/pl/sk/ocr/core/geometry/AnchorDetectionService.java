package pl.sk.ocr.core.geometry;

import java.util.List;
import java.util.Optional;
import pl.sk.ocr.config.runtime.AnchorDefinition;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.ocr.OcrWord;

public final class AnchorDetectionService {
    public List<ReferenceFeature> detect(CategoryRuntimeConfiguration category, OcrText pageOcr) {
        return category.anchors().stream()
            .map(anchor -> detect(anchor, pageOcr))
            .flatMap(Optional::stream)
            .toList();
    }

    private Optional<ReferenceFeature> detect(AnchorDefinition anchor, OcrText pageOcr) {
        var expectedText = anchor.detector() == null ? null : anchor.detector().parameters().get("text");
        if (!(expectedText instanceof String text) || text.isBlank()) {
            return Optional.empty();
        }
        return findTextBounds(pageOcr.words(), text, anchor.searchRegion())
            .map(bounds -> new ReferenceFeature(anchor.id(), bounds, 1.0));
    }

    private Optional<Region> findTextBounds(List<OcrWord> words, String expectedText, Region searchRegion) {
        var expected = expectedText.toLowerCase(java.util.Locale.ROOT);
        return words.stream()
            .filter(word -> word.text().toLowerCase(java.util.Locale.ROOT).contains(expected))
            .filter(word -> searchRegion == null || searchRegion.contains(word.boundingBox().region().topLeft()))
            .map(word -> word.boundingBox().region())
            .findFirst();
    }
}
