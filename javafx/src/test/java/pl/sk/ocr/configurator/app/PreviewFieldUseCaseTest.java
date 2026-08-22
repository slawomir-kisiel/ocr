package pl.sk.ocr.configurator.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.dto.CategoryDto;
import pl.sk.ocr.config.dto.ExtensionRefDto;
import pl.sk.ocr.config.dto.FieldDto;
import pl.sk.ocr.config.dto.OutputDto;
import pl.sk.ocr.config.dto.PageSelectionDto;
import pl.sk.ocr.config.dto.RegionDto;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.core.processing.FieldProcessingService;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.result.ProcessingStatus;
import pl.sk.ocr.domain.trace.TraceMode;
import pl.sk.ocr.extension.api.DefaultExtensionRegistry;
import pl.sk.ocr.extension.api.image.ProcessingImage;
import pl.sk.ocr.extensions.standard.StandardExtensionProvider;

class PreviewFieldUseCaseTest {

    @Test
    void previewsFieldThroughCoreFieldPipelineAndReturnsFullTrace() {
        var registry = new DefaultExtensionRegistry(new StandardExtensionProvider().extensions());
        var useCase = new PreviewFieldUseCase(new FieldProcessingService(this::ocr, registry));
        var traceImages = new InMemoryTraceImageStore();
        var field = new FieldDto("amount", "Amount", 1, new RegionDto(0, 0, 20, 20), true, null,
            new OutputDto(true, "amount"),
            List.of(),
            List.of(new ExtensionRefDto("trim", Map.of()), new ExtensionRefDto("remove-whitespace", Map.of())),
            List.of());

        var result = useCase.preview(category(field), field, image(), traceImages);

        assertThat(result.fieldResult().fieldId().value()).isEqualTo("amount");
        assertThat(result.fieldResult().value()).isEqualTo("123");
        assertThat(result.fieldResult().status()).isEqualTo(ProcessingStatus.SUCCESS);
        assertThat(result.trace().mode()).isEqualTo(TraceMode.FULL);
        assertThat(result.trace().entries()).isNotEmpty();
        assertThat(result.trace().entries().get(0).images()).hasSize(2);
        assertThat(result.trace().entries().get(0).images())
            .allSatisfy(ref -> assertThat(traceImages.get(ref)).isPresent());
    }

    @Test
    void newPreviewClearsPreviousTraceImages() {
        var registry = new DefaultExtensionRegistry(new StandardExtensionProvider().extensions());
        var useCase = new PreviewFieldUseCase(new FieldProcessingService(this::ocr, registry));
        var traceImages = new InMemoryTraceImageStore();
        var field = new FieldDto("amount", "Amount", 1, new RegionDto(0, 0, 20, 20), true, null,
            new OutputDto(true, "amount"), List.of(), List.of(), List.of());

        var first = useCase.preview(category(field), field, image(), traceImages);
        var firstRef = first.trace().entries().get(0).images().get(0);
        var second = useCase.preview(category(field), field, image(), traceImages);

        assertThat(traceImages.get(firstRef)).isEmpty();
        assertThat(second.trace().entries().get(0).images())
            .allSatisfy(ref -> assertThat(traceImages.get(ref)).isPresent());
    }

    private OcrText ocr(ProcessingImage image, OcrOptions options) {
        return new OcrText(" 1 2 3 ", List.of());
    }

    private static BufferedProcessingImage image() {
        return new BufferedProcessingImage(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB));
    }

    private static CategoryDto category(FieldDto field) {
        return new CategoryDto("1.0", "invoice", "1.0", "Invoice", "",
            new PageSelectionDto("SINGLE", 1, null, null, null),
            null,
            null,
            null,
            List.of(),
            List.of(field));
    }
}
