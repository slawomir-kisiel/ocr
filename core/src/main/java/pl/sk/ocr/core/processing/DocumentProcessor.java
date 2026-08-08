package pl.sk.ocr.core.processing;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.config.runtime.FieldDefinition;
import pl.sk.ocr.config.runtime.RuntimeConfiguration;
import pl.sk.ocr.core.document.DocumentReader;
import pl.sk.ocr.core.document.RenderOptions;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.core.ocr.OcrEngine;
import pl.sk.ocr.core.ocr.OcrOptions;
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

    public DocumentProcessor(DocumentReader documentReader, OcrEngine ocrEngine) {
        this.documentReader = documentReader;
        this.ocrEngine = ocrEngine;
    }

    public DocumentResult process(Path source, RuntimeConfiguration configuration) {
        var documentId = new DocumentId(source.getFileName().toString());
        try {
            var rendered = documentReader.read(source, RenderOptions.defaults());
            var firstPage = rendered.requirePage(new PageNumber(1));
            var pageOcr = ocrEngine.recognize(firstPage, options(configuration.profile().ocr()));
            var category = identify(configuration.categories(), pageOcr.value());
            if (category == null) {
                return DocumentResult.from(documentId, null, List.of(), List.of(ProcessingIssue.error(
                    new IssueCode("CATEGORY_NOT_IDENTIFIED"),
                    ErrorScope.CATEGORY,
                    ProcessingStage.CATEGORY_IDENTIFICATION,
                    "No category matched document"
                )), ProcessingTrace.off());
            }
            var fields = extractFields(category, rendered.pages().get(new PageNumber(1)));
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

    private CategoryRuntimeConfiguration identify(List<CategoryRuntimeConfiguration> categories, String text) {
        var normalized = normalize(text);
        for (CategoryRuntimeConfiguration category : categories) {
            var matched = category.identificationGroups().stream()
                .anyMatch(group -> group.conditions().stream()
                    .allMatch(condition -> normalized.contains(normalize(condition.expectedText()))));
            if (matched) {
                return category;
            }
        }
        return null;
    }

    private List<FieldResult> extractFields(CategoryRuntimeConfiguration category, ProcessingImage pageImage) {
        var results = new ArrayList<FieldResult>();
        for (FieldDefinition field : category.fields()) {
            var crop = crop(pageImage, field);
            var rawText = ocrEngine.recognize(crop, options(field.ocr())).value();
            results.add(new FieldResult(field.id(), applyMinimalTransforms(rawText, field), ProcessingStatus.SUCCESS, List.of()));
        }
        return results;
    }

    private ProcessingImage crop(ProcessingImage image, FieldDefinition field) {
        if (image instanceof BufferedProcessingImage buffered) {
            return buffered.crop(field.region());
        }
        return new BufferedProcessingImage(image.asBufferedImage()).crop(field.region());
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

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT).trim();
    }
}
