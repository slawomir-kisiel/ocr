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
                "im-grayscale",
                "im-normalize",
                "im-auto-threshold",
                "im-adaptive-threshold",
                "im-deskew",
                "im-background-correct",
                "im-median",
                "im-morphology",
                "im-remove-table-frames"
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
    void grayscaleConvertsColorPixelsToEqualRgbChannels() {
        var processor = new GrayscaleImageProcessor();
        var input = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        input.setRGB(0, 0, new Color(200, 40, 20).getRGB());
        input.setRGB(1, 0, new Color(10, 150, 70).getRGB());

        var output = processor.process(
            new ImageProcessingRequest(new ImageMagickProcessingImage(input), ExtensionParameters.empty()),
            noopContext()
        );

        assertGray(output.asBufferedImage().getRGB(0, 0));
        assertGray(output.asBufferedImage().getRGB(1, 0));
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

    @Test
    void removeTableFramesCoversDetectedTableLinesAndEmitsTrace() {
        var processor = new RemoveTableFramesImageProcessor();
        var input = tableImage();
        var trace = new java.util.ArrayList<Map<String, Object>>();

        var output = processor.process(
            new ImageProcessingRequest(new ImageMagickProcessingImage(input), ExtensionParameters.of(Map.of(
                "adaptiveWindow", 15,
                "adaptiveOffset", 5,
                "lineGapTolerance", 4,
                "lineMergeTolerance", 2,
                "minLineCoverage", 0.45d,
                "minLineLengthRatio", 0.20d,
                "frameThickness", 3,
                "sampleRadius", 4
            ))),
            () -> (event, attributes) -> trace.add(attributes)
        );

        assertThat(luminance(output.asBufferedImage().getRGB(8, 10))).isGreaterThan(220);
        assertThat(luminance(output.asBufferedImage().getRGB(40, 30))).isGreaterThan(220);
        assertThat(trace).hasSize(1);
        assertThat(trace.getFirst())
            .containsEntry("processorId", "im-remove-table-frames")
            .containsEntry("tablesDetected", 1)
            .containsEntry("tableCellsDetected", 4);
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

    private static BufferedImage tableImage() {
        var image = new BufferedImage(80, 60, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            var rows = new int[] {10, 30, 50};
            var columns = new int[] {8, 40, 72};
            for (int y : rows) {
                for (int x = columns[0]; x <= columns[2]; x++) {
                    if (x != 24 && x != 25 && x != 56 && x != 57) {
                        image.setRGB(x, y, Color.BLACK.getRGB());
                    }
                }
            }
            for (int x : columns) {
                for (int y = rows[0]; y <= rows[2]; y++) {
                    if (y != 20 && y != 21 && y != 40 && y != 41) {
                        image.setRGB(x, y, Color.BLACK.getRGB());
                    }
                }
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static int luminance(int argb) {
        var red = (argb >>> 16) & 0xff;
        var green = (argb >>> 8) & 0xff;
        var blue = argb & 0xff;
        return (int) Math.round(0.2126d * red + 0.7152d * green + 0.0722d * blue);
    }

    private static void assertGray(int argb) {
        var red = (argb >>> 16) & 0xff;
        var green = (argb >>> 8) & 0xff;
        var blue = argb & 0xff;
        assertThat(red).isEqualTo(green).isEqualTo(blue);
    }
}
