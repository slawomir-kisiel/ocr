package pl.sk.ocr.core.identification;

import java.util.List;
import java.util.Locale;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.config.runtime.IdentificationCondition;
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
import pl.sk.ocr.domain.ocr.OcrWord;

public final class CategoryIdentificationService {
    private final ExtensionRegistry extensionRegistry;

    public CategoryIdentificationService() {
        this(new DefaultExtensionRegistry(List.of()));
    }

    public CategoryIdentificationService(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    public IdentificationResult identify(List<CategoryRuntimeConfiguration> categories, OcrText pageOcr) {
        return identify(categories, pageOcr, null);
    }

    public IdentificationResult identify(List<CategoryRuntimeConfiguration> categories, OcrText pageOcr, ProcessingImage pageImage) {
        var matches = categories.stream()
            .filter(category -> matches(category, pageOcr, pageImage))
            .toList();
        if (matches.isEmpty()) {
            return new IdentificationResult(IdentificationStatus.NOT_FOUND, null, matches);
        }
        if (matches.size() > 1) {
            return new IdentificationResult(IdentificationStatus.AMBIGUOUS, null, matches);
        }
        return new IdentificationResult(IdentificationStatus.MATCHED, matches.getFirst(), matches);
    }

    private boolean matches(CategoryRuntimeConfiguration category, OcrText pageOcr, ProcessingImage pageImage) {
        return category.identificationGroups().stream()
            .anyMatch(group -> group.conditions().stream().allMatch(condition -> matches(condition, pageOcr, pageImage)));
    }

    private boolean matches(IdentificationCondition condition, OcrText pageOcr, ProcessingImage pageImage) {
        return detectorMatches(condition, pageOcr, pageImage);
    }

    private boolean detectorMatches(IdentificationCondition condition, OcrText pageOcr, ProcessingImage pageImage) {
        if (condition.detector() == null) {
            return false;
        }
        if (pageImage == null && "text".equals(condition.detector().id().value())) {
            return payloadMatches(condition, detectorText(pageOcr, condition.searchRegion()));
        }
        if (pageImage == null) {
            return false;
        }
        try {
            var extension = extensionRegistry.find(condition.detector().id());
            if (extension.isEmpty() || !(extension.get() instanceof Detector detector)) {
                if ("text".equals(condition.detector().id().value())) {
                    return payloadMatches(condition, detectorText(pageOcr, condition.searchRegion()));
                }
                return false;
            }
            var detection = detector.detect(new DetectionRequest(detectorImage(pageImage, condition.searchRegion()), detectorText(pageOcr, condition.searchRegion()),
                ExtensionParameters.of(condition.detector().parameters())), () -> TraceSink.NOOP);
            if (detection.status() != DetectionStatus.DETECTED) {
                return false;
            }
            var payload = detection.text().value().isBlank() ? detection.text().textFromWords() : detection.text().value();
            if (condition.expectedText() == null || condition.expectedText().isBlank()) {
                return true;
            }
            return payloadMatches(condition, payload);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean payloadMatches(IdentificationCondition condition, String payload) {
        if (condition.expectedText() == null || condition.expectedText().isBlank()) {
            return payload != null && !payload.isBlank();
        }
        if (condition.matcher() != null) {
            var matcherExtension = extensionRegistry.find(condition.matcher().id());
            if (matcherExtension.isEmpty() || !(matcherExtension.get() instanceof Matcher matcher)) {
                return false;
            }
            return matcher.match(new MatchRequest(condition.expectedText(), payload,
                ExtensionParameters.of(condition.matcher().parameters()))).matched();
        }
        return normalize(payload).contains(normalize(condition.expectedText()));
    }

    private String detectorText(OcrText pageOcr, Region searchRegion) {
        if (pageOcr == null) {
            return "";
        }
        return searchRegion == null ? pageOcr.value() : wordsInRegion(pageOcr.words(), searchRegion);
    }

    private ProcessingImage detectorImage(ProcessingImage pageImage, Region searchRegion) {
        if (searchRegion == null) {
            return pageImage;
        }
        return new BufferedProcessingImage(pageImage.asBufferedImage()).crop(searchRegion);
    }

    private String wordsInRegion(List<OcrWord> words, Region region) {
        return words.stream()
            .filter(word -> region.contains(word.boundingBox().region().topLeft()) || region.contains(word.boundingBox().region().bottomRight()))
            .map(OcrWord::text)
            .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
