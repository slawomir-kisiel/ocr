package pl.sk.ocr.extension.api.detector;

import java.util.List;
import pl.sk.ocr.domain.Validation;

public record DetectionResult(DetectionStatus status, List<DetectedGeometry> geometries, String message) {
    public DetectionResult {
        status = Validation.requireNonNull(status, "status");
        geometries = List.copyOf(Validation.requireNoNulls(geometries == null ? List.of() : geometries, "geometries"));
        message = message == null ? "" : message;
    }
}
