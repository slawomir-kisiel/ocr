package pl.sk.ocr.core.processing;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.config.runtime.FieldDefinition;
import pl.sk.ocr.config.runtime.IdentificationCondition;
import pl.sk.ocr.config.runtime.ProcessingMode;
import pl.sk.ocr.config.runtime.RuntimeConfiguration;
import pl.sk.ocr.core.document.DocumentReader;
import pl.sk.ocr.core.document.RenderOptions;
import pl.sk.ocr.core.geometry.AnchorDetectionService;
import pl.sk.ocr.core.geometry.GeometryNormalizationService;
import pl.sk.ocr.core.geometry.GeometryNormalizationResult;
import pl.sk.ocr.core.geometry.GeometryStatus;
import pl.sk.ocr.core.geometry.ReferenceFeature;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.core.image.DocumentImagePreprocessingService;
import pl.sk.ocr.core.identification.CategoryIdentificationService;
import pl.sk.ocr.core.identification.IdentificationStatus;
import pl.sk.ocr.core.ocr.OcrEngine;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.geometry.Transform;
import pl.sk.ocr.domain.identifier.AnchorId;
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
import pl.sk.ocr.extension.api.detector.DetectionRequest;
import pl.sk.ocr.extension.api.detector.DetectionStatus;
import pl.sk.ocr.extension.api.detector.Detector;
import pl.sk.ocr.extension.api.image.ProcessingImage;
import pl.sk.ocr.extension.api.matcher.MatchRequest;
import pl.sk.ocr.extension.api.matcher.Matcher;
import pl.sk.ocr.extension.api.trace.TraceSink;

public final class DocumentProcessor {
    private final DocumentReader documentReader;
    private final OcrEngine ocrEngine;
    private final CategoryIdentificationService identificationService;
    private final AnchorDetectionService anchorDetectionService;
    private final GeometryNormalizationService geometryNormalizationService;
    private final DocumentImagePreprocessingService documentImagePreprocessingService;
    private final FieldProcessingService fieldProcessingService;
    private final ExtensionRegistry extensionRegistry;

    public DocumentProcessor(DocumentReader documentReader, OcrEngine ocrEngine) {
        this(documentReader, ocrEngine, new DefaultExtensionRegistry(List.of()));
    }

    public DocumentProcessor(DocumentReader documentReader, OcrEngine ocrEngine, ExtensionRegistry extensionRegistry) {
        this.documentReader = documentReader;
        this.ocrEngine = ocrEngine;
        this.identificationService = new CategoryIdentificationService(extensionRegistry);
        this.anchorDetectionService = new AnchorDetectionService(extensionRegistry);
        this.geometryNormalizationService = new GeometryNormalizationService();
        this.documentImagePreprocessingService = new DocumentImagePreprocessingService(extensionRegistry);
        this.fieldProcessingService = new FieldProcessingService(ocrEngine, extensionRegistry);
        this.extensionRegistry = extensionRegistry;
    }

    public DocumentResult process(Path source, RuntimeConfiguration configuration) {
        var documentId = new DocumentId(source.getFileName().toString());
        try {
            var rendered = documentReader.read(source, RenderOptions.defaults());
            var firstPageNumber = new PageNumber(1);
            var firstPage = preparePage(firstPageNumber, rendered.requirePage(firstPageNumber), configuration);
            var pageOcr = ocrEngine.recognize(firstPage, options(configuration.profile().ocr()));
            var traceEntries = identificationTrace(configuration.categories(), pageOcr, firstPage);
            var identification = identificationService.identify(configuration.categories(), pageOcr, firstPage);
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
            if (configuration.profile().processing().mode() == ProcessingMode.CLASSIFY_ONLY) {
                return DocumentResult.from(documentId, category.id(), List.of(), List.of(),
                    trace(configuration.profile().traceMode(), traceEntries, List.of()));
            }
            var referenceFeatures = anchorDetectionService.detect(category, pageOcr, firstPage);
            var geometry = geometryNormalizationService.normalize(category, referenceFeatures);
            traceEntries.addAll(anchorTrace(category, referenceFeatures, geometry, pageOcr));
            traceEntries.add(geometryTrace(category, geometry, referenceFeatures));
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
            var fields = extractFields(category, firstPage, geometry.transform(), referenceFeatures, pageOcr, configuration.profile().ocr(), traceEntries);
            return DocumentResult.from(documentId, category.id(), fields, List.of(),
                trace(configuration.profile().traceMode(), traceEntries, List.of()));
        } catch (DocumentImagePreprocessingException e) {
            return DocumentResult.from(documentId, null, List.of(), List.of(ProcessingIssue.error(
                new IssueCode("DOCUMENT_IMAGE_PREPROCESSING_FAILED"),
                ErrorScope.DOCUMENT,
                ProcessingStage.PAGE_PREPARATION,
                e.getCause() == null || e.getCause().getMessage() == null ? e.getMessage() : e.getCause().getMessage()
            )), ProcessingTrace.off());
        } catch (RuntimeException e) {
            return DocumentResult.from(documentId, null, List.of(), List.of(ProcessingIssue.error(
                new IssueCode("DOCUMENT_PROCESSING_FAILED"),
                ErrorScope.DOCUMENT,
                ProcessingStage.DOCUMENT_LOADING,
                e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()
            )), ProcessingTrace.off());
        }
    }

    private ProcessingImage preparePage(PageNumber page, ProcessingImage renderedPage, RuntimeConfiguration configuration) {
        try {
            return documentImagePreprocessingService.prepare(page, renderedPage,
                configuration.profile().preprocessing().imageProcessors());
        } catch (RuntimeException e) {
            throw new DocumentImagePreprocessingException(e);
        }
    }

    private ProcessingTrace trace(TraceMode mode, List<TraceEntry> entries, List<ProcessingIssue> issues) {
        if (mode == TraceMode.OFF) {
            return ProcessingTrace.off();
        }
        var status = issues == null || issues.isEmpty() ? ProcessingStatus.SUCCESS : ProcessingStatus.FAILED;
        return new ProcessingTrace(mode, traceStages(entries, status, issues), entries);
    }

    private List<StageResult> traceStages(List<TraceEntry> entries, ProcessingStatus status, List<ProcessingIssue> issues) {
        var stages = new ArrayList<StageResult>();
        stages.add(new StageResult(ProcessingStage.CATEGORY_IDENTIFICATION, status, issues));
        if (entries.stream().anyMatch(entry -> entry.stage() == ProcessingStage.ANCHOR_DETECTION)) {
            stages.add(new StageResult(ProcessingStage.ANCHOR_DETECTION, status, List.of()));
        }
        if (entries.stream().anyMatch(entry -> entry.stage() == ProcessingStage.GEOMETRY_RESOLUTION)) {
            stages.add(new StageResult(ProcessingStage.GEOMETRY_RESOLUTION, status, issues == null ? List.of() : issues.stream()
                .filter(issue -> issue.stage() == ProcessingStage.GEOMETRY_RESOLUTION)
                .toList()));
        }
        return stages;
    }

    private List<TraceEntry> anchorTrace(CategoryRuntimeConfiguration category, List<ReferenceFeature> referenceFeatures,
                                         GeometryNormalizationResult geometry, pl.sk.ocr.domain.ocr.OcrText pageOcr) {
        var geometryAnchorIds = category.geometry() == null ? List.<pl.sk.ocr.domain.identifier.AnchorId>of() : category.geometry().anchors();
        var usedAnchorIds = geometry.usedAnchors();
        return geometryAnchorIds.stream()
            .map(anchorId -> {
                var anchor = category.anchors().stream()
                    .filter(candidate -> candidate.id().equals(anchorId))
                    .findFirst();
                var feature = referenceFeatures.stream()
                    .filter(candidate -> candidate.anchorId().equals(anchorId))
                    .findFirst();
                var attributes = new java.util.LinkedHashMap<String, Object>();
                attributes.put("categoryId", category.id().value());
                attributes.put("anchorId", anchorId.value());
                attributes.put("matched", feature.isPresent());
                attributes.put("used", usedAnchorIds.contains(anchorId));
                anchor.ifPresent(definition -> {
                    attributes.put("required", definition.required());
                    attributes.put("detectorId", definition.detector() == null ? "" : definition.detector().id().value());
                    attributes.put("matcherId", definition.matcher() == null ? "" : definition.matcher().id().value());
                    attributes.put("expectedText", definition.expectedText());
                    putRegion(attributes, "reference", definition.bounds());
                    putRegion(attributes, "search", definition.searchRegion());
                    attributes.put("ocrTextInSearchRegion", detectorText(pageOcr, definition.searchRegion()));
                });
                feature.ifPresent(detected -> {
                    attributes.put("confidence", detected.confidence());
                    putRegion(attributes, "detected", detected.bounds());
                });
                return new TraceEntry(
                    ProcessingStage.ANCHOR_DETECTION,
                    feature.isPresent() ? "Anchor detected: " + anchorId.value() : "Anchor missing: " + anchorId.value(),
                    attributes,
                    List.of()
                );
            })
            .toList();
    }

    private TraceEntry geometryTrace(CategoryRuntimeConfiguration category, GeometryNormalizationResult geometry,
                                     List<ReferenceFeature> referenceFeatures) {
        var attributes = new java.util.LinkedHashMap<String, Object>();
        attributes.put("categoryId", category.id().value());
        attributes.put("status", geometry.status().name());
        attributes.put("usedAnchors", geometry.usedAnchors().stream().map(anchor -> anchor.value()).toList());
        attributes.put("scaleX", geometry.transform().scale().x());
        attributes.put("scaleY", geometry.transform().scale().y());
        attributes.put("translateX", geometry.transform().translateX());
        attributes.put("translateY", geometry.transform().translateY());
        attributes.put("affineA", geometry.transform().affineA());
        attributes.put("affineB", geometry.transform().affineB());
        attributes.put("affineC", geometry.transform().affineC());
        attributes.put("affineD", geometry.transform().affineD());
        attributes.put("controlPointCount", geometry.usedControlPoints().size());
        if (geometry.selectedPairDistance() != null) {
            attributes.put("selectedPairDistance", geometry.selectedPairDistance());
        }
        attributes.put("usedControlPoints", geometry.usedControlPoints().stream()
            .map(point -> Map.of(
                "anchorId", point.anchorId().value(),
                "point", point.point(),
                "referenceX", point.referenceX(),
                "referenceY", point.referenceY(),
                "detectedX", point.detectedX(),
                "detectedY", point.detectedY()
            ))
            .toList());
        attributes.put("detectedAnchors", referenceFeatures.stream()
            .map(feature -> Map.of(
                "anchorId", feature.anchorId().value(),
                "confidence", feature.confidence(),
                "bounds", regionMap(feature.bounds())
            ))
            .toList());
        return new TraceEntry(
            ProcessingStage.GEOMETRY_RESOLUTION,
            "Geometry " + geometry.status(),
            attributes,
            List.of()
        );
    }

    private List<TraceEntry> identificationTrace(List<CategoryRuntimeConfiguration> categories, pl.sk.ocr.domain.ocr.OcrText pageOcr,
                                                 ProcessingImage pageImage) {
        var entries = new ArrayList<TraceEntry>();
        for (var category : categories) {
            var groups = category.identificationGroups();
            for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
                var conditions = groups.get(groupIndex).conditions();
                for (int conditionIndex = 0; conditionIndex < conditions.size(); conditionIndex++) {
                    var condition = conditions.get(conditionIndex);
                    var attributes = new java.util.LinkedHashMap<String, Object>();
                    var haystack = detectorPayload(condition, pageOcr, pageImage);
                    var normalizedHaystack = normalize(haystack);
                    var normalizedExpected = normalize(condition.expectedText());
                    var matches = conditionMatches(condition, haystack, normalizedHaystack, normalizedExpected);
                    attributes.put("categoryId", category.id().value());
                    attributes.put("group", groupIndex + 1);
                    attributes.put("condition", conditionIndex + 1);
                    attributes.put("configuredPage", condition.page());
                    attributes.put("detectorId", condition.detector() == null ? "" : condition.detector().id().value());
                    attributes.put("expectedText", condition.expectedText());
                    attributes.put("normalizedExpected", normalizedExpected);
                    attributes.put("matcherId", condition.matcher() == null ? "" : condition.matcher().id().value());
                    attributes.put("matcherStatus", matcherStatus(condition));
                    attributes.put("searchRegion", regionText(condition.searchRegion()));
                    putRegion(attributes, "search", condition.searchRegion());
                    attributes.put("ocrWordsTotal", pageOcr.words().size());
                    attributes.put("ocrTextInRegion", haystack);
                    attributes.put("detectorPayload", haystack);
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
        return normalizedExpected.isBlank() ? !haystack.isBlank() : normalizedHaystack.contains(normalizedExpected);
    }

    private String detectorPayload(IdentificationCondition condition, pl.sk.ocr.domain.ocr.OcrText pageOcr, ProcessingImage pageImage) {
        if (pageImage == null || condition.detector() == null) {
            return "";
        }
        var extension = extensionRegistry.find(condition.detector().id());
        if (extension.isEmpty() || !(extension.get() instanceof Detector detector)) {
            return "";
        }
        try {
            var result = detector.detect(new DetectionRequest(detectorImage(pageImage, condition.searchRegion()), detectorText(pageOcr, condition.searchRegion()),
                ExtensionParameters.of(condition.detector().parameters())), () -> TraceSink.NOOP);
            if (result.status() != DetectionStatus.DETECTED) {
                return "";
            }
            return result.text().value().isBlank() ? result.text().textFromWords() : result.text().value();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private String detectorText(pl.sk.ocr.domain.ocr.OcrText pageOcr, Region searchRegion) {
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

    private void putRegion(Map<String, Object> attributes, String prefix, Region region) {
        if (region == null) {
            return;
        }
        attributes.put(prefix + "X", region.x());
        attributes.put(prefix + "Y", region.y());
        attributes.put(prefix + "Width", region.width());
        attributes.put(prefix + "Height", region.height());
    }

    private Map<String, Object> regionMap(Region region) {
        if (region == null) {
            return Map.of();
        }
        return Map.of(
            "x", region.x(),
            "y", region.y(),
            "width", region.width(),
            "height", region.height()
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private List<FieldResult> extractFields(CategoryRuntimeConfiguration category, ProcessingImage pageImage, Transform transform,
                                            List<ReferenceFeature> referenceFeatures, pl.sk.ocr.domain.ocr.OcrText pageOcr,
                                            pl.sk.ocr.config.runtime.OcrSettings pageOcrSettings, List<TraceEntry> traceEntries) {
        return category.fields().stream()
            .map(field -> {
                var resolution = resolveFieldRegion(category, field, transform, referenceFeatures);
                traceEntries.add(fieldRegionTrace(category, field, resolution));
                var resolvedField = field.withResolvedRegion(resolution.resolvedRegion());
                var decision = fieldProcessingService.decideOcrMode(resolvedField, pageOcr, pageOcrSettings);
                var selected = decision.mode() == FieldProcessingService.FieldOcrMode.PAGE_OCR_REUSE
                    ? new OcrTextRegionExtractor().extract(pageOcr, resolution.resolvedRegion())
                    : null;
                if (selected != null && selected.selectedWords() == 0) {
                    decision = new FieldProcessingService.FieldOcrDecision(
                        FieldProcessingService.FieldOcrMode.FIELD_OCR,
                        "PAGE_OCR_NO_WORDS_IN_REGION"
                    );
                }
                traceEntries.add(fieldOcrTrace(category, resolvedField, resolution.resolvedRegion(), decision, selected));
                return fieldProcessingService.extract(resolvedField, pageImage, Transform.IDENTITY, pageOcr, pageOcrSettings);
            })
            .toList();
    }

    private FieldRegionResolution resolveFieldRegion(CategoryRuntimeConfiguration category, FieldDefinition field, Transform transform,
                                                     List<ReferenceFeature> referenceFeatures) {
        var globalRegion = transform.map(field.region());
        var referenceAnchorIds = field.referenceAnchorIds();
        if (referenceAnchorIds == null || referenceAnchorIds.isEmpty()) {
            return new FieldRegionResolution("GLOBAL", globalRegion, null, List.of(), globalRegion, null, null, null);
        }
        var attempts = new ArrayList<FieldReferenceAnchorAttempt>();
        for (var anchorId : referenceAnchorIds) {
            var anchor = category.anchors().stream()
                .filter(candidate -> candidate.id().equals(anchorId))
                .findFirst();
            var detected = referenceFeatures.stream()
                .filter(candidate -> candidate.anchorId().equals(anchorId))
                .findFirst();
            var matched = anchor.isPresent() && detected.isPresent() && anchor.get().bounds() != null;
            attempts.add(new FieldReferenceAnchorAttempt(anchorId, matched));
            if (matched) {
                var reference = anchor.get().bounds();
                var resolved = resolveRelativeToAnchor(field.region(), reference, detected.get().bounds(), transform);
                return new FieldRegionResolution("REFERENCE_ANCHOR", resolved, anchorId, attempts,
                    globalRegion, reference, detected.get().bounds(), null);
            }
        }
        return new FieldRegionResolution("DEGRADED", globalRegion, null, attempts, globalRegion, null, null,
            "No configured reference anchor was matched");
    }

    private Region resolveRelativeToAnchor(Region fieldRegion, Region referenceAnchor, Region detectedAnchor, Transform transform) {
        var topLeft = mapLocal(fieldRegion.x(), fieldRegion.y(), referenceAnchor, detectedAnchor, transform);
        var topRight = mapLocal(fieldRegion.x() + fieldRegion.width(), fieldRegion.y(), referenceAnchor, detectedAnchor, transform);
        var bottomLeft = mapLocal(fieldRegion.x(), fieldRegion.y() + fieldRegion.height(), referenceAnchor, detectedAnchor, transform);
        var bottomRight = mapLocal(fieldRegion.x() + fieldRegion.width(), fieldRegion.y() + fieldRegion.height(), referenceAnchor, detectedAnchor,
            transform);
        var minX = Math.min(Math.min(topLeft.x(), topRight.x()), Math.min(bottomLeft.x(), bottomRight.x()));
        var minY = Math.min(Math.min(topLeft.y(), topRight.y()), Math.min(bottomLeft.y(), bottomRight.y()));
        var maxX = Math.max(Math.max(topLeft.x(), topRight.x()), Math.max(bottomLeft.x(), bottomRight.x()));
        var maxY = Math.max(Math.max(topLeft.y(), topRight.y()), Math.max(bottomLeft.y(), bottomRight.y()));
        return new Region(minX, minY, maxX - minX, maxY - minY);
    }

    private pl.sk.ocr.domain.geometry.Point mapLocal(double sourceX, double sourceY, Region referenceAnchor, Region detectedAnchor,
                                                     Transform transform) {
        var localX = sourceX - referenceAnchor.x();
        var localY = sourceY - referenceAnchor.y();
        return new pl.sk.ocr.domain.geometry.Point(
            detectedAnchor.x() + transform.affineA() * localX + transform.affineB() * localY,
            detectedAnchor.y() + transform.affineC() * localX + transform.affineD() * localY
        );
    }

    private TraceEntry fieldRegionTrace(CategoryRuntimeConfiguration category, FieldDefinition field, FieldRegionResolution resolution) {
        var attributes = new java.util.LinkedHashMap<String, Object>();
        attributes.put("categoryId", category.id().value());
        attributes.put("fieldId", field.id().value());
        attributes.put("status", resolution.status());
        attributes.put("referenceAnchorIds", field.referenceAnchorIds().stream().map(AnchorId::value).toList());
        attributes.put("selectedReferenceAnchorId", resolution.selectedAnchorId() == null ? "" : resolution.selectedAnchorId().value());
        attributes.put("referenceAnchorAttempts", resolution.attempts().stream()
            .map(attempt -> Map.of("anchorId", attempt.anchorId().value(), "matched", attempt.matched()))
            .toList());
        putRegion(attributes, "configured", field.region());
        putRegion(attributes, "global", resolution.globalRegion());
        putRegion(attributes, "resolved", resolution.resolvedRegion());
        putRegion(attributes, "selectedReference", resolution.selectedReferenceRegion());
        putRegion(attributes, "selectedDetected", resolution.selectedDetectedRegion());
        if (resolution.fallbackReason() != null) {
            attributes.put("fallbackReason", resolution.fallbackReason());
        }
        return new TraceEntry(
            ProcessingStage.FIELD_REGION_RESOLUTION,
            "Field region " + resolution.status() + ": " + field.id().value(),
            attributes,
            List.of()
        );
    }

    private TraceEntry fieldOcrTrace(CategoryRuntimeConfiguration category, FieldDefinition field, Region resolvedRegion,
                                     FieldProcessingService.FieldOcrDecision decision,
                                     OcrTextRegionExtractor.ExtractionResult selected) {
        var attributes = new java.util.LinkedHashMap<String, Object>();
        attributes.put("categoryId", category.id().value());
        attributes.put("fieldId", field.id().value());
        attributes.put("fieldOcrMode", decision.mode().name());
        attributes.put("fallbackReason", decision.fallbackReason());
        attributes.put("selectedWords", selected == null ? 0 : selected.selectedWords());
        attributes.put("selectedLines", selected == null ? 0 : selected.selectedLines());
        if (selected != null) {
            attributes.put("rawOcr", selected.ocrText().value());
            attributes.put("rawOcrHocr", selected.ocrText().hocr());
        }
        putRegion(attributes, "resolved", resolvedRegion);
        return new TraceEntry(
            ProcessingStage.FIELD_OCR,
            "Field OCR " + decision.mode().name() + ": " + field.id().value(),
            attributes,
            List.of()
        );
    }

    private record FieldRegionResolution(String status, Region resolvedRegion, AnchorId selectedAnchorId,
                                         List<FieldReferenceAnchorAttempt> attempts, Region globalRegion,
                                         Region selectedReferenceRegion, Region selectedDetectedRegion, String fallbackReason) {
    }

    private record FieldReferenceAnchorAttempt(AnchorId anchorId, boolean matched) {
    }

    private OcrOptions options(pl.sk.ocr.config.runtime.OcrSettings settings) {
        return new OcrOptions(settings.language(), settings.datapath());
    }

    private static final class DocumentImagePreprocessingException extends RuntimeException {
        private DocumentImagePreprocessingException(Throwable cause) {
            super(cause);
        }
    }

}
