package pl.sk.ocr.core.geometry;

import java.util.List;
import pl.sk.ocr.domain.geometry.Transform;
import pl.sk.ocr.domain.identifier.AnchorId;

public record GeometryNormalizationResult(GeometryStatus status, Transform transform, List<AnchorId> usedAnchors) {
    public GeometryNormalizationResult {
        usedAnchors = List.copyOf(usedAnchors == null ? List.of() : usedAnchors);
    }
}
