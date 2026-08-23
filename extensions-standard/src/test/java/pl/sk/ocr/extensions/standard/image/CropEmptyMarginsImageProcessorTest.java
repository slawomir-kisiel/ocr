package pl.sk.ocr.extensions.standard.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingRequest;
import pl.sk.ocr.extension.api.image.ProcessingImage;
import pl.sk.ocr.extension.api.trace.TraceSink;

class CropEmptyMarginsImageProcessorTest {
    @Test
    void cropsBrightMarginsAroundContent() {
        var processor = new CropEmptyMarginsImageProcessor();
        var image = whiteImage(100, 80);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(30, 20, 40, 30);
        } finally {
            graphics.dispose();
        }

        var result = processor.process(new ImageProcessingRequest(new TestImage(image),
            ExtensionParameters.of(Map.of("backgroundTolerance", 245, "contentThreshold", 3, "padding", 5))),
            () -> TraceSink.NOOP);

        assertThat(result.width()).isEqualTo(50);
        assertThat(result.height()).isEqualTo(40);
    }

    @Test
    void returnsCopyWhenNoContentIsFound() {
        var processor = new CropEmptyMarginsImageProcessor();
        var image = whiteImage(40, 30);

        var result = processor.process(new ImageProcessingRequest(new TestImage(image), ExtensionParameters.empty()),
            () -> TraceSink.NOOP);

        assertThat(result.width()).isEqualTo(40);
        assertThat(result.height()).isEqualTo(30);
        assertThat(result.asBufferedImage()).isNotSameAs(image);
    }

    @Test
    void ignoresSingleNoisePixelWhenThresholdIsHigher() {
        var processor = new CropEmptyMarginsImageProcessor();
        var image = whiteImage(100, 80);
        image.setRGB(1, 1, Color.BLACK.getRGB());
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(30, 20, 40, 30);
        } finally {
            graphics.dispose();
        }

        var result = processor.process(new ImageProcessingRequest(new TestImage(image),
            ExtensionParameters.of(Map.of("contentThreshold", 3, "padding", 0))),
            () -> TraceSink.NOOP);

        assertThat(result.width()).isEqualTo(40);
        assertThat(result.height()).isEqualTo(30);
    }

    private BufferedImage whiteImage(int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private record TestImage(BufferedImage asBufferedImage) implements ProcessingImage {
        @Override
        public int width() {
            return asBufferedImage.getWidth();
        }

        @Override
        public int height() {
            return asBufferedImage.getHeight();
        }
    }
}
