package pl.sk.ocr.core.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.runtime.ExtensionRef;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.extension.api.DefaultExtensionRegistry;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;
import pl.sk.ocr.extension.api.image.ImageProcessingRequest;
import pl.sk.ocr.extension.api.image.ImageProcessor;
import pl.sk.ocr.extension.api.image.ProcessingImage;

class DocumentImagePreprocessingServiceTest {
    @Test
    void recordsStepTraceWithInputOutputAndExtensionEvents() {
        var source = image(10, 10);
        var firstOutput = image(20, 10);
        var secondOutput = image(20, 30);
        var service = new DocumentImagePreprocessingService(new DefaultExtensionRegistry(List.of(
            new StubProcessor("first", firstOutput),
            new StubProcessor("second", secondOutput)
        )));

        var result = service.prepareWithTrace(new PageNumber(1), source, List.of(
            new ExtensionRef(new ExtensionId("first"), Map.of("alpha", 1)),
            new ExtensionRef(new ExtensionId("second"), Map.of("beta", 2))
        ));

        assertThat(result.image()).isSameAs(secondOutput);
        assertThat(result.steps()).hasSize(2);
        assertThat(result.steps().get(0))
            .satisfies(step -> {
                assertThat(step.order()).isEqualTo(1);
                assertThat(step.processorId()).isEqualTo("first");
                assertThat(step.input()).isSameAs(source);
                assertThat(step.output()).isSameAs(firstOutput);
                assertThat(step.events()).singleElement()
                    .satisfies(event -> {
                        assertThat(event.event()).isEqualTo("processor-called");
                        assertThat(event.attributes()).containsEntry("id", "first");
                    });
            });
        assertThat(result.steps().get(1).input()).isSameAs(firstOutput);
        assertThat(result.steps().get(1).output()).isSameAs(secondOutput);
    }

    private static ProcessingImage image(int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return new BufferedProcessingImage(image);
    }

    private record StubProcessor(String id, ProcessingImage output) implements ImageProcessor {
        @Override
        public ProcessingImage process(ImageProcessingRequest request, ImageProcessingContext context) {
            context.trace().add("processor-called", Map.of("id", id, "inputWidth", request.image().width()));
            return output;
        }

        @Override
        public ExtensionDescriptor descriptor() {
            return new ExtensionDescriptor(new ExtensionId(id), ExtensionType.IMAGE_PROCESSOR, id, "", "1.0", List.of());
        }
    }
}
