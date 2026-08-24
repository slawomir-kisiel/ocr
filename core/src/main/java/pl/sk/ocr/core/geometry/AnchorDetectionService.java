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
        var extension = extensionRegistry.find(anchor.detector().id());
        if (extension.isEmpty() || !(extension.get() instanceof Detector detector)) {
            if ("text".equals(anchor.detector().id().value())) {
                return matchingRegion(anchor, pageOcr)
                    .map(match -> new ReferenceFeature(anchor.id(), match.region(), match.score()));
            }
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

    private Optional<AnchorMatch> matchingRegion(AnchorDefinition anchor, OcrText text) {
        var expected = anchor.expectedText();
        if (expected == null || expected.isBlank()) {
            return text.words().stream()
                .findFirst()
                .map(word -> new AnchorMatch(word.boundingBox().region(), word.confidence().value()));
        }
        return text.words().stream()
            .filter(word -> matches(anchor, expected, word.text()))
            .findFirst()
            .map(word -> new AnchorMatch(word.boundingBox().region(), word.confidence().value()));
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

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private record AnchorMatch(Region region, double score) {
    }
}
