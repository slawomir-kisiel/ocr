package pl.sk.ocr.core.processing;

import java.util.ArrayList;
import java.util.List;
import pl.sk.ocr.config.runtime.ExtensionRef;
import pl.sk.ocr.config.runtime.FieldDefinition;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.core.ocr.OcrEngine;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.domain.geometry.Transform;
import pl.sk.ocr.domain.issue.ErrorScope;
import pl.sk.ocr.domain.issue.IssueCode;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.issue.ProcessingStage;
import pl.sk.ocr.domain.issue.Severity;
import pl.sk.ocr.domain.result.FieldResult;
import pl.sk.ocr.domain.result.ProcessingStatus;
import pl.sk.ocr.extension.api.Extension;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.ExtensionRegistry;
import pl.sk.ocr.extension.api.image.ImageProcessingRequest;
import pl.sk.ocr.extension.api.image.ImageProcessor;
import pl.sk.ocr.extension.api.image.ProcessingImage;
import pl.sk.ocr.extension.api.trace.TraceSink;
import pl.sk.ocr.extension.api.transform.ValueTransformationRequest;
import pl.sk.ocr.extension.api.transform.ValueTransformer;
import pl.sk.ocr.extension.api.validation.ValidationRequest;
import pl.sk.ocr.extension.api.validation.ValidationStatus;
import pl.sk.ocr.extension.api.validation.Validator;

public final class FieldProcessingService {
    private final OcrEngine ocrEngine;
    private final ExtensionRegistry extensionRegistry;

    public FieldProcessingService(OcrEngine ocrEngine, ExtensionRegistry extensionRegistry) {
        this.ocrEngine = ocrEngine;
        this.extensionRegistry = extensionRegistry;
    }

    public FieldResult extract(FieldDefinition field, ProcessingImage pageImage, Transform transform) {
        var issues = new ArrayList<ProcessingIssue>();
        ProcessingImage currentImage;
        try {
            currentImage = crop(pageImage, field, transform);
        } catch (RuntimeException e) {
            return failed(field, issues, "FIELD_REGION_INVALID", ProcessingStage.FIELD_REGION_RESOLUTION, e);
        }

        for (ExtensionRef processorRef : field.imageProcessors()) {
            try {
                currentImage = imageProcessor(processorRef).process(
                    new ImageProcessingRequest(currentImage, parameters(processorRef)),
                    () -> TraceSink.NOOP
                );
            } catch (RuntimeException e) {
                return failed(field, issues, "IMAGE_PROCESSING_FAILED", ProcessingStage.IMAGE_PROCESSING, e);
            }
        }

        String value;
        try {
            value = ocrEngine.recognize(currentImage, options(field.ocr())).value();
        } catch (RuntimeException e) {
            return failed(field, issues, "FIELD_OCR_FAILED", ProcessingStage.FIELD_OCR, e);
        }

        for (ExtensionRef transformerRef : field.transformers()) {
            try {
                value = transform(value, transformerRef);
            } catch (RuntimeException e) {
                return failed(field, issues, "VALUE_TRANSFORMATION_FAILED", ProcessingStage.VALUE_TRANSFORMATION, e);
            }
        }

        for (ExtensionRef validatorRef : field.validators()) {
            try {
                var result = validator(validatorRef).validate(
                    new ValidationRequest(value, parameters(validatorRef)),
                    () -> TraceSink.NOOP
                );
                if (result.status() != ValidationStatus.VALID) {
                    issues.add(issue(
                        "FIELD_VALIDATION_FAILED",
                        result.status() == ValidationStatus.WARNING ? Severity.WARNING : Severity.ERROR,
                        ProcessingStage.FIELD_VALIDATION,
                        String.join("; ", result.messages())
                    ));
                }
            } catch (RuntimeException e) {
                issues.add(issue("FIELD_VALIDATION_FAILED", Severity.ERROR, ProcessingStage.FIELD_VALIDATION, message(e)));
            }
        }

        var status = ProcessingStatus.aggregate(issues.stream()
            .map(issue -> issue.severity() == Severity.WARNING ? ProcessingStatus.WARNING : ProcessingStatus.FAILED)
            .toList());
        return new FieldResult(field.id(), value, status, issues);
    }

    private ProcessingImage crop(ProcessingImage image, FieldDefinition field, Transform transform) {
        var resolvedRegion = transform.map(field.region());
        if (image instanceof BufferedProcessingImage buffered) {
            return buffered.crop(resolvedRegion);
        }
        return new BufferedProcessingImage(image.asBufferedImage()).crop(resolvedRegion);
    }

    private ImageProcessor imageProcessor(ExtensionRef ref) {
        return require(ref, ImageProcessor.class, "IMAGE_PROCESSING_FAILED");
    }

    private Validator validator(ExtensionRef ref) {
        return require(ref, Validator.class, "FIELD_VALIDATION_FAILED");
    }

    private String transform(String value, ExtensionRef ref) {
        // Backward-compatible built-in used by early fixtures before standard extensions are implemented.
        if ("trim".equals(ref.id().value())) {
            return value == null ? "" : value.trim();
        }
        var extension = extensionRegistry.find(ref.id()).orElseThrow();
        if (extension instanceof ValueTransformer transformer) {
            return transformer.transform(new ValueTransformationRequest(value, parameters(ref)));
        }
        throw new IllegalArgumentException("Extension is not a value transformer: " + ref.id().value());
    }

    private <T extends Extension> T require(ExtensionRef ref, Class<T> type, String code) {
        var extension = extensionRegistry.find(ref.id())
            .orElseThrow(() -> new IllegalArgumentException(code + ": missing extension " + ref.id().value()));
        if (!type.isInstance(extension)) {
            throw new IllegalArgumentException("Extension has invalid type: " + ref.id().value());
        }
        return type.cast(extension);
    }

    private ExtensionParameters parameters(ExtensionRef ref) {
        return ExtensionParameters.of(ref.parameters());
    }

    private OcrOptions options(pl.sk.ocr.config.runtime.OcrSettings settings) {
        return new OcrOptions(settings.language(), settings.datapath());
    }

    private FieldResult failed(FieldDefinition field, List<ProcessingIssue> issues, String code, ProcessingStage stage, RuntimeException e) {
        issues.add(issue(code, field.required() ? Severity.ERROR : Severity.WARNING, stage, message(e)));
        return new FieldResult(field.id(), null, field.required() ? ProcessingStatus.FAILED : ProcessingStatus.WARNING, issues);
    }

    private ProcessingIssue issue(String code, Severity severity, ProcessingStage stage, String message) {
        return new ProcessingIssue(
            new IssueCode(code),
            severity,
            ErrorScope.FIELD,
            stage,
            message == null || message.isBlank() ? code : message,
            null,
            null,
            null,
            null,
            null,
            java.util.Map.of()
        );
    }

    private String message(RuntimeException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
