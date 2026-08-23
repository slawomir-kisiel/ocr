package pl.sk.ocr.core.processing;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.config.runtime.IdentificationCondition;
import pl.sk.ocr.config.runtime.RuntimeConfiguration;
import pl.sk.ocr.core.document.DocumentReader;
import pl.sk.ocr.core.document.RenderOptions;
import pl.sk.ocr.core.geometry.AnchorDetectionService;
import pl.sk.ocr.core.geometry.GeometryNormalizationService;
import pl.sk.ocr.core.geometry.GeometryStatus;
import pl.sk.ocr.core.identification.CategoryIdentificationService;
import pl.sk.ocr.core.identification.IdentificationStatus;
import pl.sk.ocr.core.ocr.OcrEngine;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.geometry.Transform;
import pl.sk.ocr.domain.identifier.DocumentId;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.domain.issue.ErrorScope;
import pl.sk.ocr.domain.issue.IssueCode;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.issue.ProcessingStage;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.result.FieldResult;
import pl.sk.ocr.domain.result.ProcessingStatus;
import pl.sk.ocr.domain.result.StageResult;
import pl.sk.ocr.domain.trace.TraceEntry;
import pl.sk.ocr.domain.trace.TraceMode;
import pl.sk.ocr.domain.trace.ProcessingTrace;
import pl.sk.ocr.extension.api.DefaultExtensionRegistry;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.ExtensionRegistry;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.image.ProcessingImage;
import pl.sk.ocr.extension.api.matcher.MatchRequest;
import pl.sk.ocr.extension.api.matcher.Matcher;

public final class DocumentProcessor {
    private final DocumentReader documentReader;
    private final OcrEngine ocrEngine;
    private final CategoryIdentificationService identificationService;
    private final AnchorDetectionService anchorDetectionService;
    private final GeometryNormalizationService geometryNormalizationService;
    private final FieldProcessingService fieldProcessingService;
    private final ExtensionRegistry extensionRegistry;

    public DocumentProcessor(DocumentReader documentReader, OcrEngine ocrEngine) {
        this(documentReader, ocrEngine, new DefaultExtensionRegistry(List.of()));
    }

    public DocumentProcessor(DocumentReader documentReader, OcrEngine ocrEngine, ExtensionRegistry extensionRegistry) {
        this.documentReader = documentReader;
        this.ocrEngine = ocrEngine;
        this.identificationService = new CategoryIdentificationService(extensionRegistry);
        this.anchorDetectionService = new AnchorDetectionService();
        this.geometryNormalizationService = new GeometryNormalizationService();
        this.fieldProcessingService = new FieldProcessingService(ocrEngine, extensionRegistry);
        this.extensionRegistry = extensionRegistry;
    }

    public DocumentResult process(Path source, RuntimeConfiguration configuration) {
        var documentId = new DocumentId(source.getFileName().toString());
        try {
            var rendered = documentReader.read(source, RenderOptions.defaults());
            var firstPage = rendered.requirePage(new PageNumber(1));
            var pageOcr = ocrEngine.recognize(firstPage, options(configuration.profile().ocr()));
            var traceEntries = identificationTrace(configuration.categories(), pageOcr);
            var identification = identificationService.identify(configuration.categories(), pageOcr);
            if (identification.status() == IdentificationStatus.NOT_FOUND) {
                var issue = ProcessingIssue.error(
                    new IssueCode("CATEGORY_NOT_IDENTIFIED"),
                    ErrorScope.CATEGORY,
                    ProcessingStage.CATEGORY_IDENTIFICATION,
                    "No category matched document"
                );
                return DocumentResult.from(documentId, null, List.of(), List.of(issue),
                    trace(configuration.profile().traceMode(), traceEntries, List.of(issue)));
            }
            if (identification.status() == IdentificationStatus.AMBIGUOUS) {
                var issue = ProcessingIssue.error(
                    new IssueCode("CATEGORY_AMBIGUOUS"),
                    ErrorScope.CATEGORY,
                    ProcessingStage.CATEGORY_IDENTIFICATION,
                    "Multiple categories matched document"
                );
                return DocumentResult.from(documentId, null, List.of(), List.of(issue),
                    trace(configuration.profile().traceMode(), traceEntries, List.of(issue)));
            }
            var category = identification.category();
            var referenceFeatures = anchorDetectionService.detect(category, pageOcr);
            var geometry = geometryNormalizationService.normalize(category, referenceFeatures);
            if (geometry.status() == GeometryStatus.FAILED) {
                var issue = ProcessingIssue.error(
                    new IssueCode("GEOMETRY_RESOLUTION_FAILED"),
                    ErrorScope.GEOMETRY,
                    ProcessingStage.GEOMETRY_RESOLUTION,
                    "Required anchors were not detected"
                );
                return DocumentResult.from(documentId, category.id(), List.of(), List.of(issue),
                    trace(configuration.profile().traceMode(), traceEntries, List.of(issue)));
            }
            var fields = extractFields(category, rendered.pages().get(new PageNumber(1)), geometry.transform());
            return DocumentResult.from(documentId, category.id(), fields, List.of(),
                trace(configuration.profile().traceMode(), traceEntries, List.of()));
        } catch (RuntimeException e) {
            return DocumentResult.from(documentId, null, List.of(), List.of(ProcessingIssue.error(
                new IssueCode("DOCUMENT_PROCESSING_FAILED"),
                ErrorScope.DOCUMENT,
                ProcessingStage.DOCUMENT_LOADING,
                e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()
            )), ProcessingTrace.off());
        }
    }

    private ProcessingTrace trace(TraceMode mode, List<TraceEntry> entries, List<ProcessingIssue> issues) {
        if (mode == TraceMode.OFF) {
            return ProcessingTrace.off();
        }
        var status = issues == null || issues.isEmpty() ? ProcessingStatus.SUCCESS : ProcessingStatus.FAILED;
        return new ProcessingTrace(mode, List.of(new StageResult(ProcessingStage.CATEGORY_IDENTIFICATION, status, issues)), entries);
    }

    private List<TraceEntry> identificationTrace(List<CategoryRuntimeConfiguration> categories, pl.sk.ocr.domain.ocr.OcrText pageOcr) {
        var entries = new ArrayList<TraceEntry>();
        for (var category : categories) {
            var groups = category.identificationGroups();
            for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
                var conditions = groups.get(groupIndex).conditions();
                for (int conditionIndex = 0; conditionIndex < conditions.size(); conditionIndex++) {
                    var condition = conditions.get(conditionIndex);
                    var attributes = new java.util.LinkedHashMap<String, Object>();
                    var haystack = condition.searchRegion() == null ? pageOcr.value() : wordsInRegion(pageOcr.words(), condition.searchRegion());
                    var normalizedHaystack = normalize(haystack);
                    var normalizedExpected = normalize(condition.expectedText());
                    var matches = conditionMatches(condition, haystack, normalizedHaystack, normalizedExpected);
                    attributes.put("categoryId", category.id().value());
                    attributes.put("group", groupIndex + 1);
                    attributes.put("condition", conditionIndex + 1);
                    attributes.put("type", condition.type());
                    attributes.put("configuredPage", condition.page());
                    attributes.put("expectedText", condition.expectedText());
                    attributes.put("normalizedExpected", normalizedExpected);
                    attributes.put("matcherId", condition.matcher() == null ? "" : condition.matcher().id().value());
                    attributes.put("matcherStatus", matcherStatus(condition));
                    attributes.put("searchRegion", regionText(condition.searchRegion()));
                    attributes.put("ocrWordsTotal", pageOcr.words().size());
                    attributes.put("ocrTextInRegion", haystack);
                    attributes.put("normalizedOcrTextInRegion", normalizedHaystack);
                    attributes.put("matched", matches);
                    entries.add(new TraceEntry(
                        ProcessingStage.CATEGORY_IDENTIFICATION,
                        "Identification condition " + (matches ? "matched" : "did not match"),
                        attributes,
                        List.of()
                    ));
                }
            }
        }
        return entries;
    }

    private boolean conditionMatches(IdentificationCondition condition, String haystack, String normalizedHaystack, String normalizedExpected) {
        if (condition.matcher() != null) {
            var extension = extensionRegistry.find(condition.matcher().id());
            if (extension.isPresent() && extension.get() instanceof Matcher matcher) {
                return matcher.match(new MatchRequest(condition.expectedText(), haystack,
                    ExtensionParameters.of(condition.matcher().parameters()))).matched();
            }
            return false;
        }
        return switch (condition.type()) {
            case "TEXT" -> normalizedHaystack.contains(normalizedExpected);
            case "TEXT_FUZZY" -> normalizedHaystack.contains(normalizedExpected) || similarity(normalizedHaystack, normalizedExpected) >= 0.80;
            default -> false;
        };
    }

    private String matcherStatus(IdentificationCondition condition) {
        if (condition.matcher() == null || condition.matcher().id() == null) {
            return "not configured";
        }
        try {
            var extension = extensionRegistry.find(new ExtensionId(condition.matcher().id().value()));
            if (extension.isEmpty()) {
                return "unknown extension";
            }
            var type = extension.get().descriptor().type();
            return type == ExtensionType.MATCHER ? "OK" : "invalid type: " + type;
        } catch (RuntimeException e) {
            return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
    }

    private String wordsInRegion(List<pl.sk.ocr.domain.ocr.OcrWord> words, Region region) {
        return words.stream()
            .filter(word -> region.contains(word.boundingBox().region().topLeft()) || region.contains(word.boundingBox().region().bottomRight()))
            .map(pl.sk.ocr.domain.ocr.OcrWord::text)
            .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right);
    }

    private String regionText(Region region) {
        if (region == null) {
            return "whole page";
        }
        return "x=" + region.x() + ", y=" + region.y() + ", width=" + region.width() + ", height=" + region.height();
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

    private List<FieldResult> extractFields(CategoryRuntimeConfiguration category, ProcessingImage pageImage, Transform transform) {
        return category.fields().stream()
            .map(field -> fieldProcessingService.extract(field, pageImage, transform))
            .toList();
    }

    private OcrOptions options(pl.sk.ocr.config.runtime.OcrSettings settings) {
        return new OcrOptions(settings.language(), settings.datapath());
    }

}
