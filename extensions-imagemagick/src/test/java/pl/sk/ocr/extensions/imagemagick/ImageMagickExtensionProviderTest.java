package pl.sk.ocr.extensions.imagemagick;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.ServiceLoaderExtensionRegistryFactory;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;
import pl.sk.ocr.extension.api.image.ImageProcessingRequest;
import pl.sk.ocr.extension.api.image.ImageProcessor;

class ImageMagickExtensionProviderTest {
    @Test
    void serviceLoaderExposesImageMagickProcessors() {
        var registry = ServiceLoaderExtensionRegistryFactory.load(getClass().getClassLoader());

        assertThat(registry.extensions())
            .extracting(extension -> extension.descriptor().id().value())
            .contains(
                "im-profile",
                "im-normalize",
                "im-auto-threshold",
                "im-adaptive-threshold",
                "im-deskew",
                "im-background-correct",
                "im-median",
                "im-morphology"
            );
    }

    @Test
    void autoThresholdProcessesBufferedImage() {
        var processor = new AutoThresholdImageProcessor();
        var input = testImage();

        var output = processor.process(
            new ImageProcessingRequest(new ImageMagickProcessingImage(input), ExtensionParameters.of(Map.of("method", "OTSU"))),
            noopContext()
        );

        assertThat(output.width()).isEqualTo(input.getWidth());
        assertThat(output.height()).isEqualTo(input.getHeight());
        assertThat(output.asBufferedImage()).isNotSameAs(input);
    }

    @Test
    void profileProcessorEmitsTrace() {
        var processor = new ProfileImageProcessor();
        var input = testImage();
        var trace = new java.util.ArrayList<Map<String, Object>>();

        processor.process(
            new ImageProcessingRequest(new ImageMagickProcessingImage(input), ExtensionParameters.of(Map.of("profile", "GOOD_SCAN"))),
            () -> (event, attributes) -> trace.add(attributes)
        );

        assertThat(trace).hasSize(1);
        assertThat(trace.getFirst())
            .containsEntry("processorId", "im-profile")
            .containsKeys("inputWidth", "inputHeight", "outputWidth", "outputHeight", "scaleApplied");
    }

    private static ImageProcessingContext noopContext() {
        return () -> pl.sk.ocr.extension.api.trace.TraceSink.NOOP;
    }

    private static BufferedImage testImage() {
        var image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.fillRect(5, 6, 30, 8);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
