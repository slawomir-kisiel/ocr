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
        return switch (condition.type()) {
            case "TEXT" -> containsText(condition, pageOcr, false);
            case "TEXT_FUZZY" -> containsText(condition, pageOcr, true);
            case "QR", "BARCODE" -> detectorMatches(condition, pageImage);
            default -> false;
        };
    }

    private boolean detectorMatches(IdentificationCondition condition, ProcessingImage pageImage) {
        if (pageImage == null || condition.detector() == null) {
            return false;
        }
        try {
            var extension = extensionRegistry.find(condition.detector().id());
            if (extension.isEmpty() || !(extension.get() instanceof Detector detector)) {
                return false;
            }
            var detection = detector.detect(new DetectionRequest(detectorImage(pageImage, condition.searchRegion()), condition.expectedText(),
                ExtensionParameters.of(condition.detector().parameters())), () -> TraceSink.NOOP);
            if (detection.status() != DetectionStatus.DETECTED) {
                return false;
            }
            var payload = detection.message();
            if (condition.expectedText() == null || condition.expectedText().isBlank()) {
                return true;
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
        } catch (RuntimeException e) {
            return false;
        }
    }

    private ProcessingImage detectorImage(ProcessingImage pageImage, Region searchRegion) {
        if (searchRegion == null) {
            return pageImage;
        }
        return new BufferedProcessingImage(pageImage.asBufferedImage()).crop(searchRegion);
    }

    private boolean containsText(IdentificationCondition condition, OcrText ocr, boolean fuzzy) {
        var expected = condition.expectedText();
        var haystack = condition.searchRegion() == null ? ocr.value() : wordsInRegion(ocr.words(), condition.searchRegion());
        if (condition.matcher() != null) {
            var extension = extensionRegistry.find(condition.matcher().id());
            if (extension.isEmpty() || !(extension.get() instanceof Matcher matcher)) {
                return false;
            }
            return matcher.match(new MatchRequest(expected, haystack, ExtensionParameters.of(condition.matcher().parameters()))).matched();
        }
        var normalizedHaystack = normalize(haystack);
        var normalizedExpected = normalize(expected);
        if (normalizedHaystack.contains(normalizedExpected)) {
            return true;
        }
        return fuzzy && similarity(normalizedHaystack, normalizedExpected) >= 0.80;
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

    private static double similarity(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        var distance = levenshtein(a, b);
        return 1.0 - ((double) distance / Math.max(a.length(), b.length()));
    }

    private static int levenshtein(String a, String b) {
        var dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                var cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
