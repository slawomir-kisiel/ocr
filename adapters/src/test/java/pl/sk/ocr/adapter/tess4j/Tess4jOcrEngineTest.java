package pl.sk.ocr.adapter.tess4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.extension.api.image.ProcessingImage;

class Tess4jOcrEngineTest {
    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void usesWindowsDefaultDatapathWhenNoDatapathIsConfigured() {
        var originalOsName = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows 11");

            var datapath = new Tess4jOcrEngine().effectiveDatapath(new OcrOptions("pol", null));

            assertThat(datapath).isEqualTo(Tess4jOcrEngine.WINDOWS_DEFAULT_DATAPATH);
        } finally {
            restoreOsName(originalOsName);
        }
    }

    @Test
    void configuredDatapathOverridesWindowsDefault() {
        var originalOsName = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows 11");

            var datapath = new Tess4jOcrEngine().effectiveDatapath(new OcrOptions("pol", "D:\\tessdata"));

            assertThat(datapath).isEqualTo("D:\\tessdata");
        } finally {
            restoreOsName(originalOsName);
        }
    }

    @Test
    void failsBeforeNativeOcrWhenLanguageDataIsMissing() {
        var engine = new Tess4jOcrEngine();
        var image = new ProcessingImage() {
            @Override
            public BufferedImage asBufferedImage() {
                return new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            }

            @Override
            public int width() {
                return 10;
            }

            @Override
            public int height() {
                return 10;
            }
        };

        assertThatThrownBy(() -> engine.recognize(image, new OcrOptions("pol", tempDir.toString())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Missing Tesseract language data")
            .hasMessageContaining("pol.traineddata");
    }

    private void restoreOsName(String originalOsName) {
        if (originalOsName == null) {
            System.clearProperty("os.name");
        } else {
            System.setProperty("os.name", originalOsName);
        }
    }
}
