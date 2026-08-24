package pl.sk.ocr.extension.api.detector;

import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.domain.ocr.OcrText;

public record DetectionResult(DetectionStatus status, OcrText text, String message) {
    public DetectionResult {
        status = Validation.requireNonNull(status, "status");
        text = text == null ? new OcrText("", java.util.List.of()) : text;
        message = message == null ? "" : message;
    }
}
