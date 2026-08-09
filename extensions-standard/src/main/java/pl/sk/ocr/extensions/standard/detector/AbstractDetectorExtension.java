package pl.sk.ocr.extensions.standard.detector;

import java.util.List;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.extension.api.detector.DetectedGeometry;
import pl.sk.ocr.extension.api.detector.DetectionResult;
import pl.sk.ocr.extension.api.detector.DetectionStatus;

abstract class AbstractDetectorExtension implements pl.sk.ocr.extension.api.detector.Detector {
    DetectionResult notDetected(String message) {
        return new DetectionResult(DetectionStatus.NOT_DETECTED, List.of(), message);
    }

    DetectionResult detectedWholeImage(pl.sk.ocr.extension.api.image.ProcessingImage image, String message) {
        return new DetectionResult(DetectionStatus.DETECTED,
            List.of(new DetectedGeometry(new Region(0, 0, image.width(), image.height()), 1.0)),
            message);
    }
}

