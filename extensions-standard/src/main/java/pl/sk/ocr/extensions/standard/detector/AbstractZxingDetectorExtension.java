package pl.sk.ocr.extensions.standard.detector;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.util.List;
import java.util.Map;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.extension.api.detector.DetectedGeometry;
import pl.sk.ocr.extension.api.detector.DetectionRequest;
import pl.sk.ocr.extension.api.detector.DetectionResult;
import pl.sk.ocr.extension.api.detector.DetectionStatus;
import pl.sk.ocr.extension.api.detector.DetectorContext;

abstract class AbstractZxingDetectorExtension extends AbstractDetectorExtension {
    private final List<BarcodeFormat> formats;

    AbstractZxingDetectorExtension(List<BarcodeFormat> formats) {
        this.formats = List.copyOf(formats);
    }

    @Override
    public DetectionResult detect(DetectionRequest request, DetectorContext context) {
        var source = new BufferedImageLuminanceSource(request.image().asBufferedImage());
        var bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            var result = new MultiFormatReader().decode(bitmap, Map.of(DecodeHintType.POSSIBLE_FORMATS, formats));
            return new DetectionResult(
                DetectionStatus.DETECTED,
                List.of(new DetectedGeometry(bounds(result.getResultPoints(), request.image().width(), request.image().height()), 1.0)),
                result.getText()
            );
        } catch (NotFoundException e) {
            return notDetected("Code was not detected.");
        } catch (RuntimeException e) {
            return new DetectionResult(DetectionStatus.FAILED, List.of(),
                e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private Region bounds(ResultPoint[] points, int imageWidth, int imageHeight) {
        if (points == null || points.length == 0) {
            return new Region(0, 0, imageWidth, imageHeight);
        }
        var minX = Double.MAX_VALUE;
        var minY = Double.MAX_VALUE;
        var maxX = 0.0;
        var maxY = 0.0;
        for (ResultPoint point : points) {
            minX = Math.min(minX, point.getX());
            minY = Math.min(minY, point.getY());
            maxX = Math.max(maxX, point.getX());
            maxY = Math.max(maxY, point.getY());
        }
        return new Region(minX, minY, Math.max(1.0, maxX - minX), Math.max(1.0, maxY - minY));
    }
}
