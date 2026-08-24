package pl.sk.ocr.extensions.standard.detector;

import java.util.List;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.ocr.BoundingBox;
import pl.sk.ocr.domain.ocr.Confidence;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.ocr.OcrWord;
import pl.sk.ocr.extension.api.detector.DetectionResult;
import pl.sk.ocr.extension.api.detector.DetectionStatus;

abstract class AbstractDetectorExtension implements pl.sk.ocr.extension.api.detector.Detector {
    DetectionResult notDetected(String message) {
        return new DetectionResult(DetectionStatus.NOT_DETECTED, new OcrText("", List.of()), message);
    }

    DetectionResult detectedWholeImage(pl.sk.ocr.extension.api.image.ProcessingImage image, String message) {
        return detectedText(message, new Region(0, 0, image.width(), image.height()), 1.0, message);
    }

    DetectionResult detectedText(String text, Region region, double score, String message) {
        return new DetectionResult(DetectionStatus.DETECTED,
            new OcrText(text, List.of(new OcrWord(text, new BoundingBox(region), new Confidence(score)))),
            message);
    }

    DetectionResult failed(String message) {
        return new DetectionResult(DetectionStatus.FAILED, new OcrText("", List.of()), message);
    }
}

