package pl.sk.ocr.core.processing;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.config.runtime.FieldDefinition;
import pl.sk.ocr.config.runtime.RuntimeConfiguration;
import pl.sk.ocr.core.document.DocumentReader;
import pl.sk.ocr.core.document.RenderOptions;
import pl.sk.ocr.core.geometry.AnchorDetectionService;
import pl.sk.ocr.core.geometry.GeometryNormalizationService;
import pl.sk.ocr.core.geometry.GeometryStatus;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.core.identification.CategoryIdentificationService;
import pl.sk.ocr.core.identification.IdentificationStatus;
import pl.sk.ocr.core.ocr.OcrEngine;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.domain.geometry.Transform;
import pl.sk.ocr.domain.identifier.DocumentId;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.domain.issue.ErrorScope;
import pl.sk.ocr.domain.issue.IssueCode;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.issue.ProcessingStage;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.result.FieldResult;
import pl.sk.ocr.domain.result.ProcessingStatus;
import pl.sk.ocr.domain.trace.ProcessingTrace;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public final class DocumentProcessor {
    private final DocumentReader documentReader;
    private final OcrEngine ocrEngine;
    private final CategoryIdentificationService identificationService;
    private final AnchorDetectionService anchorDetectionService;
    private final GeometryNormalizationService geometryNormalizationService;

    public DocumentProcessor(DocumentReader documentReader, OcrEngine ocrEngine) {
        this.documentReader = documentReader;
        this.ocrEngine = ocrEngine;
        this.identificationService = new CategoryIdentificationService();
        this.anchorDetectionService = new AnchorDetectionService();
        this.geometryNormalizationService = new GeometryNormalizationService();
    }

    public DocumentResult process(Path source, RuntimeConfiguration configuration) {
        var documentId = new DocumentId(source.getFileName().toString());
        try {
            var rendered = documentReader.read(source, RenderOptions.defaults());
            var firstPage = rendered.requirePage(new PageNumber(1));
            var pageOcr = ocrEngine.recognize(firstPage, options(configuration.profile().ocr()));
            var identification = identificationService.identify(configuration.categories(), pageOcr);
            if (identification.status() == IdentificationStatus.NOT_FOUND) {
                return DocumentResult.from(documentId, null, List.of(), List.of(ProcessingIssue.error(
                    new IssueCode("CATEGORY_NOT_IDENTIFIED"),
                    ErrorScope.CATEGORY,
                    ProcessingStage.CATEGORY_IDENTIFICATION,
                    "No category matched document"
                )), ProcessingTrace.off());
            }
            if (identification.status() == IdentificationStatus.AMBIGUOUS) {
                return DocumentResult.from(documentId, null, List.of(), List.of(ProcessingIssue.error(
                    new IssueCode("CATEGORY_AMBIGUOUS"),
                    ErrorScope.CATEGORY,
                    ProcessingStage.CATEGORY_IDENTIFICATION,
                    "Multiple categories matched document"
                )), ProcessingTrace.off());
            }
            var category = identification.category();
            var referenceFeatures = anchorDetectionService.detect(category, pageOcr);
            var geometry = geometryNormalizationService.normalize(category, referenceFeatures);
            if (geometry.status() == GeometryStatus.FAILED) {
                return DocumentResult.from(documentId, category.id(), List.of(), List.of(ProcessingIssue.error(
                    new IssueCode("GEOMETRY_NORMALIZATION_FAILED"),
                    ErrorScope.GEOMETRY,
                    ProcessingStage.GEOMETRY_RESOLUTION,
                    "Required anchors were not detected"
                )), ProcessingTrace.off());
            }
            var fields = extractFields(category, rendered.pages().get(new PageNumber(1)), geometry.transform());
            return DocumentResult.from(documentId, category.id(), fields, List.of(), ProcessingTrace.off());
        } catch (RuntimeException e) {
            return DocumentResult.from(documentId, null, List.of(), List.of(ProcessingIssue.error(
                new IssueCode("DOCUMENT_PROCESSING_FAILED"),
                ErrorScope.DOCUMENT,
                ProcessingStage.DOCUMENT_LOADING,
                e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()
            )), ProcessingTrace.off());
        }
    }

    private List<FieldResult> extractFields(CategoryRuntimeConfiguration category, ProcessingImage pageImage, Transform transform) {
        var results = new ArrayList<FieldResult>();
        for (FieldDefinition field : category.fields()) {
            var crop = crop(pageImage, field, transform);
            var rawText = ocrEngine.recognize(crop, options(field.ocr())).value();
            results.add(new FieldResult(field.id(), applyMinimalTransforms(rawText, field), ProcessingStatus.SUCCESS, List.of()));
        }
        return results;
    }

    private ProcessingImage crop(ProcessingImage image, FieldDefinition field, Transform transform) {
        var resolvedRegion = transform.map(field.region());
        if (image instanceof BufferedProcessingImage buffered) {
            return buffered.crop(resolvedRegion);
        }
        return new BufferedProcessingImage(image.asBufferedImage()).crop(resolvedRegion);
    }

    private String applyMinimalTransforms(String value, FieldDefinition field) {
        var result = value == null ? "" : value;
        for (var transformer : field.transformers()) {
            if ("trim".equals(transformer.id().value())) {
                result = result.trim();
            }
        }
        return result;
    }

    private OcrOptions options(pl.sk.ocr.config.runtime.OcrSettings settings) {
        return new OcrOptions(settings.language(), settings.datapath());
    }

}
