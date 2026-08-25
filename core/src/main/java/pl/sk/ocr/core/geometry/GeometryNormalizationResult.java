package pl.sk.ocr.core.geometry;

import java.util.List;
import pl.sk.ocr.domain.geometry.Transform;
import pl.sk.ocr.domain.identifier.AnchorId;

public record GeometryNormalizationResult(
    GeometryStatus status,
    Transform transform,
    List<AnchorId> usedAnchors,
    List<ControlPoint> usedControlPoints,
    Double selectedPairDistance
) {
    public GeometryNormalizationResult(GeometryStatus status, Transform transform, List<AnchorId> usedAnchors) {
        this(status, transform, usedAnchors, List.of(), null);
    }

    public GeometryNormalizationResult {
        usedAnchors = List.copyOf(usedAnchors == null ? List.of() : usedAnchors);
        usedControlPoints = List.copyOf(usedControlPoints == null ? List.of() : usedControlPoints);
    }

    public record ControlPoint(
        AnchorId anchorId,
        String point,
        double referenceX,
        double referenceY,
        double detectedX,
        double detectedY
    ) {
    }
}
