package pl.sk.ocr.core.geometry;

import java.util.List;
import java.util.Optional;
import pl.sk.ocr.config.runtime.AnchorDefinition;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.extension.api.DefaultExtensionRegistry;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.ExtensionRegistry;
import pl.sk.ocr.extension.api.detector.DetectionRequest;
import pl.sk.ocr.extension.api.detector.DetectionStatus;
import pl.sk.ocr.extension.api.detector.Detector;
import pl.sk.ocr.extension.api.image.ProcessingImage;
import pl.sk.ocr.extension.api.matcher.MatchRequest;
import pl.sk.ocr.extension.api.matcher.Matcher;
import pl.sk.ocr.extension.api.trace.TraceSink;
import pl.sk.ocr.domain.ocr.OcrText;

public final class AnchorDetectionService {
    private final ExtensionRegistry extensionRegistry;

    public AnchorDetectionService() {
        this(new DefaultExtensionRegistry(List.of()));
    }

    public AnchorDetectionService(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    public List<ReferenceFeature> detect(CategoryRuntimeConfiguration category, OcrText pageOcr) {
        return detect(category, pageOcr, null);
    }

    public List<ReferenceFeature> detect(CategoryRuntimeConfiguration category, OcrText pageOcr, ProcessingImage pageImage) {
        return category.anchors().stream()
            .map(anchor -> detect(anchor, pageOcr, pageImage))
            .flatMap(Optional::stream)
            .toList();
    }

    private Optional<ReferenceFeature> detect(AnchorDefinition anchor, OcrText pageOcr, ProcessingImage pageImage) {
        return detectByExtension(anchor, pageOcr, pageImage);
    }

    private Optional<ReferenceFeature> detectByExtension(AnchorDefinition anchor, OcrText pageOcr, ProcessingImage pageImage) {
        if (pageImage == null || anchor.detector() == null) {
            return Optional.empty();
        }
        if ("text".equals(anchor.detector().id().value())) {
            return matchingRegion(anchor, ocrInRegion(pageOcr, anchor.searchRegion()))
                .map(match -> new ReferenceFeature(anchor.id(), match.region(), match.score()));
        }
        var extension = extensionRegistry.find(anchor.detector().id());
        if (extension.isEmpty() || !(extension.get() instanceof Detector detector)) {
            return Optional.empty();
        }
        try {
            var image = detectorImage(pageImage, anchor.searchRegion());
            var result = detector.detect(new DetectionRequest(image, detectorText(pageOcr, anchor.searchRegion()),
                ExtensionParameters.of(anchor.detector().parameters())), () -> TraceSink.NOOP);
            if (result.status() != DetectionStatus.DETECTED || result.text().words().isEmpty()) {
                return Optional.empty();
            }
            return matchingRegion(anchor, result.text())
                .map(match -> new ReferenceFeature(anchor.id(), translate(match.region(), anchor.searchRegion()), match.score()));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private String detectorText(OcrText pageOcr, Region searchRegion) {
        if (pageOcr == null) {
            return "";
        }
        return searchRegion == null ? pageOcr.value() : pageOcr.words().stream()
            .filter(word -> searchRegion.contains(word.boundingBox().region().topLeft())
                || searchRegion.contains(word.boundingBox().region().bottomRight()))
            .map(pl.sk.ocr.domain.ocr.OcrWord::text)
            .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right);
    }

    private OcrText ocrInRegion(OcrText pageOcr, Region searchRegion) {
        if (pageOcr == null) {
            return new OcrText("", List.of());
        }
        if (searchRegion == null) {
            return pageOcr;
        }
        var words = pageOcr.words().stream()
            .filter(word -> searchRegion.contains(word.boundingBox().region().topLeft())
                || searchRegion.contains(word.boundingBox().region().bottomRight()))
            .toList();
        return new OcrText(words.stream()
            .map(pl.sk.ocr.domain.ocr.OcrWord::text)
            .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right), words);
    }

    private Optional<AnchorMatch> matchingRegion(AnchorDefinition anchor, OcrText text) {
        var expected = anchor.expectedText();
        if (expected == null || expected.isBlank()) {
            return text.words().stream()
                .findFirst()
                .map(word -> new AnchorMatch(word.boundingBox().region(), word.confidence().value()));
        }
        var words = text.words();
        for (int length = 1; length <= words.size(); length++) {
            for (int start = 0; start + length <= words.size(); start++) {
                var candidate = words.subList(start, start + length);
                if (matches(anchor, expected, phrase(candidate))) {
                    return Optional.of(new AnchorMatch(bounds(candidate), confidence(candidate)));
                }
            }
        }
        return Optional.empty();
    }

    private boolean matches(AnchorDefinition anchor, String expected, String actual) {
        if (anchor.matcher() != null) {
            var extension = extensionRegistry.find(anchor.matcher().id());
            if (extension.isEmpty() || !(extension.get() instanceof Matcher matcher)) {
                return false;
            }
            return matcher.match(new MatchRequest(expected, actual, ExtensionParameters.of(anchor.matcher().parameters()))).matched();
        }
        return normalize(actual).contains(normalize(expected));
    }

    private ProcessingImage detectorImage(ProcessingImage pageImage, Region searchRegion) {
        if (searchRegion == null) {
            return pageImage;
        }
        return new BufferedProcessingImage(pageImage.asBufferedImage()).crop(searchRegion);
    }

    private Region translate(Region region, Region searchRegion) {
        if (searchRegion == null) {
            return region;
        }
        return new Region(region.x() + searchRegion.x(), region.y() + searchRegion.y(), region.width(), region.height());
    }

    private String phrase(List<pl.sk.ocr.domain.ocr.OcrWord> words) {
        return words.stream()
            .map(pl.sk.ocr.domain.ocr.OcrWord::text)
            .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right);
    }

    private Region bounds(List<pl.sk.ocr.domain.ocr.OcrWord> words) {
        var first = words.getFirst().boundingBox().region();
        var minX = first.x();
        var minY = first.y();
        var maxX = first.x() + first.width();
        var maxY = first.y() + first.height();
        for (var word : words) {
            var region = word.boundingBox().region();
            minX = Math.min(minX, region.x());
            minY = Math.min(minY, region.y());
            maxX = Math.max(maxX, region.x() + region.width());
            maxY = Math.max(maxY, region.y() + region.height());
        }
        return new Region(minX, minY, maxX - minX, maxY - minY);
    }

    private double confidence(List<pl.sk.ocr.domain.ocr.OcrWord> words) {
        return words.stream()
            .mapToDouble(word -> word.confidence().value())
            .average()
            .orElse(0.0);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private record AnchorMatch(Region region, double score) {
    }
}
