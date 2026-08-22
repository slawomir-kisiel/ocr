package pl.sk.ocr.configurator.app;

import java.util.List;
import java.util.Map;
import pl.sk.ocr.config.dto.CategoryDto;
import pl.sk.ocr.config.dto.ExtensionRefDto;
import pl.sk.ocr.config.dto.FieldDto;
import pl.sk.ocr.config.dto.OcrSettingsDto;
import pl.sk.ocr.config.dto.RegionDto;
import pl.sk.ocr.config.runtime.ExtensionRef;
import pl.sk.ocr.config.runtime.FieldDefinition;
import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.core.processing.FieldProcessingService;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.geometry.Transform;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.domain.identifier.FieldId;
import pl.sk.ocr.domain.issue.ProcessingStage;
import pl.sk.ocr.domain.result.StageResult;
import pl.sk.ocr.domain.trace.ProcessingTrace;
import pl.sk.ocr.domain.trace.TraceEntry;
import pl.sk.ocr.domain.trace.TraceImageRef;
import pl.sk.ocr.domain.trace.TraceMode;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public final class PreviewFieldUseCase {
    private final FieldProcessingService fieldProcessingService;

    public PreviewFieldUseCase(FieldProcessingService fieldProcessingService) {
        this.fieldProcessingService = fieldProcessingService;
    }

    public FieldPreviewResult preview(CategoryDto category, FieldDto field, ProcessingImage pageImage) {
        return preview(category, field, pageImage, new InMemoryTraceImageStore());
    }

    public FieldPreviewResult preview(CategoryDto category, FieldDto field, ProcessingImage pageImage, TraceImageStore traceImageStore) {
        if (category == null) {
            throw new IllegalArgumentException("category is required");
        }
        if (field == null) {
            throw new IllegalArgumentException("field is required");
        }
        if (pageImage == null) {
            throw new IllegalArgumentException("page image is required");
        }
        if (traceImageStore == null) {
            throw new IllegalArgumentException("trace image store is required");
        }
        traceImageStore.clear();
        var fieldDefinition = fieldDefinition(category, field);
        var imageRefs = traceImages(traceImageStore, fieldDefinition, pageImage);
        var result = fieldProcessingService.extract(fieldDefinition, pageImage, Transform.IDENTITY);
        var trace = new ProcessingTrace(
            TraceMode.FULL,
            List.of(new StageResult(ProcessingStage.FIELD_OCR, result.status(), result.issues())),
            List.of(new TraceEntry(
                ProcessingStage.FIELD_OCR,
                "Field preview completed",
                Map.of(
                    "fieldId", result.fieldId().value(),
                    "status", result.status().name(),
                    "value", result.value() == null ? "" : result.value()
                ),
                imageRefs
            ))
        );
        return new FieldPreviewResult(result, trace);
    }

    private List<TraceImageRef> traceImages(TraceImageStore traceImageStore, FieldDefinition field, ProcessingImage pageImage) {
        var refs = new java.util.ArrayList<TraceImageRef>();
        refs.add(traceImageStore.put("Preview page image", pageImage));
        try {
            refs.add(traceImageStore.put("Field crop input", crop(pageImage, field.region())));
        } catch (RuntimeException ignored) {
            // FieldProcessingService will report invalid regions through FieldResult; trace image capture stays best-effort.
        }
        return List.copyOf(refs);
    }

    private ProcessingImage crop(ProcessingImage image, Region region) {
        if (image instanceof BufferedProcessingImage buffered) {
            return buffered.crop(region);
        }
        return new BufferedProcessingImage(image.asBufferedImage()).crop(region);
    }

    private FieldDefinition fieldDefinition(CategoryDto category, FieldDto field) {
        var categoryOcr = ocr(category.ocr(), OcrSettings.defaults());
        var output = field.output();
        return new FieldDefinition(
            new FieldId(required(field.id(), "field id")),
            field.displayName(),
            field.page() == null ? 1 : field.page(),
            region(field.region()),
            field.required() == null || field.required(),
            ocr(field.ocr(), categoryOcr),
            output != null && Boolean.TRUE.equals(output.exported()),
            output == null ? null : output.columnName(),
            extensions(field.imageProcessors()),
            extensions(field.transformers()),
            extensions(field.validators())
        );
    }

    private OcrSettings ocr(OcrSettingsDto dto, OcrSettings defaults) {
        if (dto == null) {
            return defaults;
        }
        return new OcrSettings(dto.language() == null ? defaults.language() : dto.language(), dto.datapath() == null ? defaults.datapath() : dto.datapath());
    }

    private Region region(RegionDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("field region is required");
        }
        return new Region(dto.x(), dto.y(), dto.width(), dto.height());
    }

    private List<ExtensionRef> extensions(List<ExtensionRefDto> refs) {
        return refs == null ? List.of() : refs.stream().map(this::extension).toList();
    }

    private ExtensionRef extension(ExtensionRefDto dto) {
        if (dto == null || dto.id() == null || dto.id().isBlank()) {
            throw new IllegalArgumentException("extension id is required");
        }
        return new ExtensionRef(new ExtensionId(dto.id().trim()), dto.parameters());
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
