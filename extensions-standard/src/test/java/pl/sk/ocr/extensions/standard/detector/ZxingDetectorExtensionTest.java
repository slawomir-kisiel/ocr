package pl.sk.ocr.extensions.standard.detector;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.detector.DetectionRequest;
import pl.sk.ocr.extension.api.detector.DetectionStatus;
import pl.sk.ocr.extension.api.image.ProcessingImage;
import pl.sk.ocr.extension.api.trace.TraceSink;

class ZxingDetectorExtensionTest {
    @Test
    void qrDetectorReadsQrPayload() throws Exception {
        var detector = new QrDetectorExtension();

        var result = detector.detect(new DetectionRequest(image(BarcodeFormat.QR_CODE, "QR-123"), null,
            ExtensionParameters.empty()), () -> TraceSink.NOOP);

        assertThat(result.status()).isEqualTo(DetectionStatus.DETECTED);
        assertThat(result.message()).isEqualTo("QR-123");
        assertThat(result.geometries()).isNotEmpty();
    }

    @Test
    void barcodeDetectorReadsCode128Payload() throws Exception {
        var detector = new BarcodeDetectorExtension();

        var result = detector.detect(new DetectionRequest(image(BarcodeFormat.CODE_128, "ABC123"), null,
            ExtensionParameters.empty()), () -> TraceSink.NOOP);

        assertThat(result.status()).isEqualTo(DetectionStatus.DETECTED);
        assertThat(result.message()).isEqualTo("ABC123");
        assertThat(result.geometries()).isNotEmpty();
    }

    private ProcessingImage image(BarcodeFormat format, String payload) throws Exception {
        var matrix = new MultiFormatWriter().encode(payload, format, 180, 180);
        return new TestImage(MatrixToImageWriter.toBufferedImage(matrix));
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
