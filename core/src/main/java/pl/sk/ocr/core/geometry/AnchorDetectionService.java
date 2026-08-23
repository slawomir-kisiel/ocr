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
import pl.sk.ocr.extension.api.trace.TraceSink;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.ocr.OcrWord;

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
        var detected = detectByExtension(anchor, pageImage);
        if (detected.isPresent()) {
            return detected;
        }
        var expectedText = anchor.detector() == null ? null : anchor.detector().parameters().get("text");
        if (!(expectedText instanceof String text) || text.isBlank()) {
            return Optional.empty();
        }
        return findTextBounds(pageOcr.words(), text, anchor.searchRegion())
            .map(bounds -> new ReferenceFeature(anchor.id(), bounds, 1.0));
    }

    private Optional<ReferenceFeature> detectByExtension(AnchorDefinition anchor, ProcessingImage pageImage) {
        if (pageImage == null || anchor.detector() == null) {
            return Optional.empty();
        }
        var extension = extensionRegistry.find(anchor.detector().id());
        if (extension.isEmpty() || !(extension.get() instanceof Detector detector)) {
            return Optional.empty();
        }
        try {
            var image = detectorImage(pageImage, anchor.searchRegion());
            var result = detector.detect(new DetectionRequest(image, null,
                ExtensionParameters.of(anchor.detector().parameters())), () -> TraceSink.NOOP);
            if (result.status() != DetectionStatus.DETECTED || result.geometries().isEmpty()) {
                return Optional.empty();
            }
            var geometry = result.geometries().getFirst();
            return Optional.of(new ReferenceFeature(anchor.id(), translate(geometry.region(), anchor.searchRegion()), geometry.score()));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
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

    private Optional<Region> findTextBounds(List<OcrWord> words, String expectedText, Region searchRegion) {
        var expected = expectedText.toLowerCase(java.util.Locale.ROOT);
        return words.stream()
            .filter(word -> word.text().toLowerCase(java.util.Locale.ROOT).contains(expected))
            .filter(word -> searchRegion == null || searchRegion.contains(word.boundingBox().region().topLeft()))
            .map(word -> word.boundingBox().region())
            .findFirst();
    }
}
