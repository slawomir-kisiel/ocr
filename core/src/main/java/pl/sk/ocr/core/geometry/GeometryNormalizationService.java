package pl.sk.ocr.core.geometry;

import java.util.List;
import pl.sk.ocr.config.runtime.AnchorDefinition;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.domain.geometry.Scale;
import pl.sk.ocr.domain.geometry.Transform;

public final class GeometryNormalizationService {
    public GeometryNormalizationResult normalize(CategoryRuntimeConfiguration category, List<ReferenceFeature> detectedFeatures) {
        var geometry = category.geometry();
        if (geometry == null || geometry.anchors().isEmpty() || "NONE".equals(geometry.strategy())) {
            return new GeometryNormalizationResult(GeometryStatus.DEGRADED, Transform.IDENTITY, List.of());
        }
        for (var anchorId : geometry.anchors()) {
            var anchor = category.anchors().stream().filter(candidate -> candidate.id().equals(anchorId)).findFirst();
            var detected = detectedFeatures.stream().filter(feature -> feature.anchorId().equals(anchorId)).findFirst();
            if (anchor.isPresent() && anchor.get().bounds() != null && detected.isPresent()) {
                return transformFrom(anchor.get(), detected.get());
            }
        }
        var missingRequired = category.anchors().stream()
            .filter(AnchorDefinition::required)
            .anyMatch(anchor -> geometry.anchors().contains(anchor.id()));
        return new GeometryNormalizationResult(missingRequired ? GeometryStatus.FAILED : GeometryStatus.DEGRADED, Transform.IDENTITY, List.of());
    }

    private GeometryNormalizationResult transformFrom(AnchorDefinition anchor, ReferenceFeature detected) {
        var reference = anchor.bounds();
        var actual = detected.bounds();
        var scale = new Scale(actual.width() / reference.width(), actual.height() / reference.height());
        var translateX = actual.x() - reference.x() * scale.x();
        var translateY = actual.y() - reference.y() * scale.y();
        return new GeometryNormalizationResult(GeometryStatus.NORMALIZED, new Transform(scale, translateX, translateY), List.of(anchor.id()));
    }
}
