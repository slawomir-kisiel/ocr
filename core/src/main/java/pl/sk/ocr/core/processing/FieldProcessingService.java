package pl.sk.ocr.core.processing;

import java.util.ArrayList;
import java.util.List;
import pl.sk.ocr.config.runtime.ExtensionRef;
import pl.sk.ocr.config.runtime.FieldDefinition;
import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.core.ocr.OcrEngine;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.geometry.Transform;
import pl.sk.ocr.domain.ocr.OcrText;
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
import pl.sk.ocr.extension.api.detector.DetectionRequest;
import pl.sk.ocr.extension.api.detector.DetectionStatus;
import pl.sk.ocr.extension.api.detector.Detector;
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
    private final OcrTextRegionExtractor pageOcrExtractor = new OcrTextRegionExtractor();

    public FieldProcessingService(OcrEngine ocrEngine, ExtensionRegistry extensionRegistry) {
        this.ocrEngine = ocrEngine;
        this.extensionRegistry = extensionRegistry;
    }

    public String recognizeRaw(FieldDefinition field, ProcessingImage pageImage, Transform transform) {
        return preview(field, pageImage, transform).ocrText().value();
    }

    public FieldProcessingPreview preview(FieldDefinition field, ProcessingImage pageImage, Transform transform) {
        return preview(field, pageImage, transform, null, null);
    }

    public FieldProcessingPreview preview(FieldDefinition field, ProcessingImage pageImage, Transform transform, OcrText pageOcr,
                                          OcrSettings pageOcrSettings) {
        var steps = new ArrayList<ImageTraceStep>();
        var resolvedRegion = transform.map(field.region());
        var currentImage = crop(pageImage, resolvedRegion);
        steps.add(new ImageTraceStep("Field input", currentImage));
        var decision = decideOcrMode(field, pageOcr, pageOcrSettings);
        if (decision.mode() == FieldOcrMode.PAGE_OCR_REUSE) {
            var extraction = pageOcrExtractor.extract(pageOcr, resolvedRegion);
            if (extraction.selectedWords() > 0) {
                return new FieldProcessingPreview(List.copyOf(steps), extraction.ocrText(), decision, extraction.selectedWords(), extraction.selectedLines());
            }
            decision = new FieldOcrDecision(FieldOcrMode.FIELD_OCR, "PAGE_OCR_NO_WORDS_IN_REGION");
        }
        var processorIndex = 1;
        for (ExtensionRef processorRef : field.imageProcessors()) {
            currentImage = imageProcessor(processorRef).process(
                new ImageProcessingRequest(currentImage, parameters(processorRef)),
                () -> TraceSink.NOOP
            );
            steps.add(new ImageTraceStep("After " + processorRef.id().value(), currentImage));
            processorIndex++;
        }
        steps.add(new ImageTraceStep("OCR input", currentImage));
        return new FieldProcessingPreview(List.copyOf(steps), recognize(currentImage, field), decision, 0, 0);
    }

    public FieldResult extract(FieldDefinition field, ProcessingImage pageImage, Transform transform) {
        return extract(field, pageImage, transform, null, null);
    }

    public FieldResult extract(FieldDefinition field, ProcessingImage pageImage, Transform transform, OcrText pageOcr, OcrSettings pageOcrSettings) {
        var issues = new ArrayList<ProcessingIssue>();
        ProcessingImage currentImage;
        Region resolvedRegion;
        try {
            resolvedRegion = transform.map(field.region());
            currentImage = crop(pageImage, resolvedRegion);
        } catch (RuntimeException e) {
            return failed(field, issues, "FIELD_REGION_OUT_OF_BOUNDS", ProcessingStage.FIELD_REGION_RESOLUTION, e);
        }

        var decision = decideOcrMode(field, pageOcr, pageOcrSettings);
        String value;
        if (decision.mode() == FieldOcrMode.PAGE_OCR_REUSE) {
            try {
                var extraction = pageOcrExtractor.extract(pageOcr, resolvedRegion);
                if (extraction.selectedWords() > 0) {
                    value = extraction.ocrText().value();
                } else {
                    decision = new FieldOcrDecision(FieldOcrMode.FIELD_OCR, "PAGE_OCR_NO_WORDS_IN_REGION");
                    value = null;
                }
            } catch (RuntimeException e) {
                return failed(field, issues, "FIELD_OCR_FAILED", ProcessingStage.FIELD_OCR, e, resolvedRegion);
            }
        } else {
            value = null;
        }
        if (decision.mode() == FieldOcrMode.FIELD_OCR) {
        for (ExtensionRef processorRef : field.imageProcessors()) {
            try {
                currentImage = imageProcessor(processorRef).process(
                    new ImageProcessingRequest(currentImage, parameters(processorRef)),
                    () -> TraceSink.NOOP
                );
            } catch (RuntimeException e) {
                return failed(field, issues, "FIELD_IMAGE_PROCESSING_FAILED", ProcessingStage.IMAGE_PROCESSING, e, resolvedRegion);
            }
        }

        try {
            value = recognize(currentImage, field).value();
        } catch (RuntimeException e) {
            return failed(field, issues, "FIELD_OCR_FAILED", ProcessingStage.FIELD_OCR, e, resolvedRegion);
        }
        }

        for (ExtensionRef transformerRef : field.transformers()) {
            try {
                value = transform(value, transformerRef);
            } catch (RuntimeException e) {
                return failed(field, issues, "FIELD_VALUE_TRANSFORMATION_FAILED", ProcessingStage.VALUE_TRANSFORMATION, e, resolvedRegion);
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
        return new FieldResult(field.id(), value, status, issues, resolvedRegion);
    }

    public FieldOcrDecision decideOcrMode(FieldDefinition field, OcrText pageOcr, OcrSettings pageOcrSettings) {
        if (pageOcr == null) {
            return new FieldOcrDecision(FieldOcrMode.FIELD_OCR, "PAGE_OCR_MISSING");
        }
        if (pageOcr.words().isEmpty()) {
            return new FieldOcrDecision(FieldOcrMode.FIELD_OCR, "PAGE_OCR_EMPTY");
        }
        if (field.imageProcessors() != null && !field.imageProcessors().isEmpty()) {
            return new FieldOcrDecision(FieldOcrMode.FIELD_OCR, "FIELD_HAS_IMAGE_PROCESSORS");
        }
        if (!usesDefaultOcrDetector(field)) {
            return new FieldOcrDecision(FieldOcrMode.FIELD_OCR, "FIELD_USES_NON_OCR_DETECTOR");
        }
        if (!sameOcrSettings(field.ocr(), pageOcrSettings)) {
            return new FieldOcrDecision(FieldOcrMode.FIELD_OCR, "FIELD_OVERRIDES_OCR_SETTINGS");
        }
        return new FieldOcrDecision(FieldOcrMode.PAGE_OCR_REUSE, "");
    }

    private ProcessingImage crop(ProcessingImage image, FieldDefinition field, Transform transform) {
        return crop(image, transform.map(field.region()));
    }

    private ProcessingImage crop(ProcessingImage image, Region resolvedRegion) {
        if (image instanceof BufferedProcessingImage buffered) {
            return buffered.crop(resolvedRegion);
        }
        return new BufferedProcessingImage(image.asBufferedImage()).crop(resolvedRegion);
    }

    private ImageProcessor imageProcessor(ExtensionRef ref) {
        return require(ref, ImageProcessor.class, "IMAGE_PROCESSING_FAILED");
    }

    private OcrText recognize(ProcessingImage image, FieldDefinition field) {
        var detector = field.ocr() == null ? null : field.ocr().detector();
        if (detector == null || detector.id() == null
            || detector.id().value() == null
            || detector.id().value().isBlank()
            || "ocr".equals(detector.id().value())
            || "text".equals(detector.id().value())) {
            return ocrEngine.recognize(image, options(field.ocr()));
        }
        var extension = require(detector, Detector.class, "FIELD_DETECTION_FAILED");
        var result = extension.detect(new DetectionRequest(image, "", parameters(detector)), () -> TraceSink.NOOP);
        if (result.status() != DetectionStatus.DETECTED) {
            return new OcrText("", List.of());
        }
        var text = result.text();
        return text.value().isBlank() && !text.textFromWords().isBlank()
            ? new OcrText(text.textFromWords(), text.hocr(), text.areas())
            : text;
    }

    private boolean usesDefaultOcrDetector(FieldDefinition field) {
        var detector = field.ocr() == null ? null : field.ocr().detector();
        if (detector == null || detector.id() == null || detector.id().value() == null || detector.id().value().isBlank()) {
            return true;
        }
        var id = detector.id().value();
        return "ocr".equals(id) || "text".equals(id);
    }

    private boolean sameOcrSettings(OcrSettings fieldSettings, OcrSettings pageSettings) {
        var field = fieldSettings == null ? OcrSettings.defaults() : fieldSettings;
        var page = pageSettings == null ? OcrSettings.defaults() : pageSettings;
        return java.util.Objects.equals(field.language(), page.language())
            && java.util.Objects.equals(field.datapath(), page.datapath());
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
        return failed(field, issues, code, stage, e, null);
    }

    private FieldResult failed(FieldDefinition field, List<ProcessingIssue> issues, String code, ProcessingStage stage, RuntimeException e,
                               Region resolvedRegion) {
        issues.add(issue(code, field.required() ? Severity.ERROR : Severity.WARNING, stage, message(e)));
        return new FieldResult(field.id(), null, field.required() ? ProcessingStatus.FAILED : ProcessingStatus.WARNING, issues, resolvedRegion);
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

    public record ImageTraceStep(String label, ProcessingImage image) {
    }

    public enum FieldOcrMode {
        PAGE_OCR_REUSE,
        FIELD_OCR
    }

    public record FieldOcrDecision(FieldOcrMode mode, String fallbackReason) {
    }

    public record FieldProcessingPreview(List<ImageTraceStep> images, OcrText ocrText, FieldOcrDecision decision, int selectedWords,
                                         int selectedLines) {
        public FieldProcessingPreview {
            images = List.copyOf(images == null ? List.of() : images);
            decision = decision == null ? new FieldOcrDecision(FieldOcrMode.FIELD_OCR, "") : decision;
        }

        public FieldProcessingPreview(List<ImageTraceStep> images, OcrText ocrText) {
            this(images, ocrText, new FieldOcrDecision(FieldOcrMode.FIELD_OCR, ""), 0, 0);
        }
    }
}
