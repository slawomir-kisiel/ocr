package pl.sk.ocr.extension.api.detector;

import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.domain.geometry.Region;

public record DetectedGeometry(Region region, double score) {
    public DetectedGeometry {
        region = Validation.requireNonNull(region, "region");
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score must be between 0.0 and 1.0");
        }
    }
}
