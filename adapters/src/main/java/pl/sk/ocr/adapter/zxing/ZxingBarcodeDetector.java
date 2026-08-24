package pl.sk.ocr.adapter.zxing;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.util.List;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.domain.ocr.BoundingBox;
import pl.sk.ocr.domain.ocr.Confidence;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.ocr.OcrWord;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.detector.DetectionRequest;
import pl.sk.ocr.extension.api.detector.DetectionResult;
import pl.sk.ocr.extension.api.detector.DetectionStatus;
import pl.sk.ocr.extension.api.detector.Detector;
import pl.sk.ocr.extension.api.detector.DetectorContext;

public final class ZxingBarcodeDetector implements Detector {
    private static final ExtensionDescriptor DESCRIPTOR = new ExtensionDescriptor(
        new ExtensionId("barcode"),
        ExtensionType.DETECTOR,
        "Barcode",
        "Detects QR and barcodes using ZXing",
        "1.0",
        List.of()
    );

    @Override
    public DetectionResult detect(DetectionRequest request, DetectorContext context) {
        var source = new BufferedImageLuminanceSource(request.image().asBufferedImage());
        var bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            var result = new MultiFormatReader().decode(bitmap);
            var region = bounds(result.getResultPoints());
            return new DetectionResult(
                DetectionStatus.DETECTED,
                new OcrText(result.getText(), List.of(new OcrWord(result.getText(), new BoundingBox(region), new Confidence(1.0)))),
                "Code detected"
            );
        } catch (NotFoundException e) {
            return new DetectionResult(DetectionStatus.NOT_DETECTED, new OcrText("", List.of()), "");
        }
    }

    @Override
    public ExtensionDescriptor descriptor() {
        return DESCRIPTOR;
    }

    private Region bounds(ResultPoint[] points) {
        if (points == null || points.length == 0) {
            return new Region(0, 0, 0, 0);
        }
        var minX = Float.MAX_VALUE;
        var minY = Float.MAX_VALUE;
        var maxX = Float.MIN_VALUE;
        var maxY = Float.MIN_VALUE;
        for (ResultPoint point : points) {
            minX = Math.min(minX, point.getX());
            minY = Math.min(minY, point.getY());
            maxX = Math.max(maxX, point.getX());
            maxY = Math.max(maxY, point.getY());
        }
        return new Region(minX, minY, Math.max(1.0, maxX - minX), Math.max(1.0, maxY - minY));
    }
}
