package pl.sk.ocr.core.geometry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import pl.sk.ocr.config.runtime.AnchorDefinition;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.domain.geometry.Scale;
import pl.sk.ocr.domain.geometry.Transform;
import pl.sk.ocr.domain.identifier.AnchorId;

public final class GeometryNormalizationService {
    private static final double QR_BARCODE_MIN_SIZE = 16.0;
    private static final double AXIS_EPSILON = 1.0;

    public GeometryNormalizationResult normalize(CategoryRuntimeConfiguration category, List<ReferenceFeature> detectedFeatures) {
        var geometry = category.geometry();
        if (geometry == null || geometry.anchors().isEmpty() || "NONE".equals(geometry.strategy())) {
            return new GeometryNormalizationResult(GeometryStatus.DEGRADED, Transform.IDENTITY, List.of());
        }
        var controlPoints = controlPoints(category, detectedFeatures);
        var missingRequired = category.anchors().stream()
            .filter(AnchorDefinition::required)
            .anyMatch(anchor -> geometry.anchors().contains(anchor.id()));
        if (controlPoints.isEmpty()) {
            return new GeometryNormalizationResult(
                missingRequired ? GeometryStatus.FAILED : GeometryStatus.DEGRADED,
                Transform.IDENTITY,
                List.of()
            );
        }
        if ("ANCHOR_TRANSLATION".equals(geometry.strategy())) {
            return translationFrom(controlPoints);
        }
        if (controlPoints.size() == 1) {
            var point = controlPoints.getFirst();
            var transform = new Transform(
                Scale.IDENTITY,
                point.detectedX() - point.referenceX(),
                point.detectedY() - point.referenceY()
            );
            return new GeometryNormalizationResult(
                GeometryStatus.NORMALIZED,
                transform,
                List.of(point.anchorId()),
                List.of(point),
                null
            );
        }
        var pair = widestPair(controlPoints);
        var first = pair.first();
        var second = pair.second();
        var referenceDx = second.referenceX() - first.referenceX();
        var referenceDy = second.referenceY() - first.referenceY();
        var detectedDx = second.detectedX() - first.detectedX();
        var detectedDy = second.detectedY() - first.detectedY();
        var scale = new Scale(
            Math.abs(referenceDx) < AXIS_EPSILON ? 1.0 : detectedDx / referenceDx,
            Math.abs(referenceDy) < AXIS_EPSILON ? 1.0 : detectedDy / referenceDy
        );
        var transform = new Transform(
            scale,
            first.detectedX() - first.referenceX() * scale.x(),
            first.detectedY() - first.referenceY() * scale.y()
        );
        return new GeometryNormalizationResult(
            GeometryStatus.NORMALIZED,
            transform,
            usedAnchors(first, second),
            List.of(first, second),
            pair.referenceDistance()
        );
    }

    private GeometryNormalizationResult translationFrom(List<GeometryNormalizationResult.ControlPoint> controlPoints) {
        var translateX = controlPoints.stream()
            .mapToDouble(point -> point.detectedX() - point.referenceX())
            .average()
            .orElse(0.0);
        var translateY = controlPoints.stream()
            .mapToDouble(point -> point.detectedY() - point.referenceY())
            .average()
            .orElse(0.0);
        return new GeometryNormalizationResult(
            GeometryStatus.NORMALIZED,
            new Transform(Scale.IDENTITY, translateX, translateY),
            usedAnchors(controlPoints),
            controlPoints,
            null
        );
    }

    private List<GeometryNormalizationResult.ControlPoint> controlPoints(
        CategoryRuntimeConfiguration category,
        List<ReferenceFeature> detectedFeatures
    ) {
        var points = new ArrayList<GeometryNormalizationResult.ControlPoint>();
        for (var anchorId : category.geometry().anchors()) {
            var anchor = category.anchors().stream()
                .filter(candidate -> candidate.id().equals(anchorId))
                .findFirst();
            var detected = detectedFeatures.stream()
                .filter(feature -> feature.anchorId().equals(anchorId))
                .findFirst();
            if (anchor.isEmpty() || detected.isEmpty() || anchor.get().bounds() == null || detected.get().bounds() == null) {
                continue;
            }
            addTopLeft(points, anchor.get(), detected.get());
            if (isQrOrBarcode(anchor.get()) && hasStableSize(anchor.get(), detected.get())) {
                addBottomRight(points, anchor.get(), detected.get());
            }
        }
        return points;
    }

    private void addTopLeft(List<GeometryNormalizationResult.ControlPoint> points, AnchorDefinition anchor, ReferenceFeature detected) {
        var reference = anchor.bounds().topLeft();
        var actual = detected.bounds().topLeft();
        points.add(new GeometryNormalizationResult.ControlPoint(
            anchor.id(),
            "TOP_LEFT",
            reference.x(),
            reference.y(),
            actual.x(),
            actual.y()
        ));
    }

    private void addBottomRight(List<GeometryNormalizationResult.ControlPoint> points, AnchorDefinition anchor, ReferenceFeature detected) {
        var reference = anchor.bounds().bottomRight();
        var actual = detected.bounds().bottomRight();
        points.add(new GeometryNormalizationResult.ControlPoint(
            anchor.id(),
            "BOTTOM_RIGHT",
            reference.x(),
            reference.y(),
            actual.x(),
            actual.y()
        ));
    }

    private boolean isQrOrBarcode(AnchorDefinition anchor) {
        if (anchor.detector() == null || anchor.detector().id() == null) {
            return false;
        }
        var id = anchor.detector().id().value();
        return "qr".equals(id) || "barcode".equals(id);
    }

    private boolean hasStableSize(AnchorDefinition anchor, ReferenceFeature detected) {
        return anchor.bounds().width() >= QR_BARCODE_MIN_SIZE
            && anchor.bounds().height() >= QR_BARCODE_MIN_SIZE
            && detected.bounds().width() >= QR_BARCODE_MIN_SIZE
            && detected.bounds().height() >= QR_BARCODE_MIN_SIZE;
    }

    private ControlPointPair widestPair(List<GeometryNormalizationResult.ControlPoint> points) {
        ControlPointPair widest = null;
        for (int first = 0; first < points.size(); first++) {
            for (int second = first + 1; second < points.size(); second++) {
                var candidate = new ControlPointPair(points.get(first), points.get(second));
                if (widest == null || candidate.referenceDistance() > widest.referenceDistance()) {
                    widest = candidate;
                }
            }
        }
        return widest;
    }

    private List<AnchorId> usedAnchors(GeometryNormalizationResult.ControlPoint first, GeometryNormalizationResult.ControlPoint second) {
        var ids = new LinkedHashSet<AnchorId>();
        ids.add(first.anchorId());
        ids.add(second.anchorId());
        return List.copyOf(ids);
    }

    private List<AnchorId> usedAnchors(List<GeometryNormalizationResult.ControlPoint> controlPoints) {
        var ids = new LinkedHashSet<AnchorId>();
        controlPoints.stream().map(GeometryNormalizationResult.ControlPoint::anchorId).forEach(ids::add);
        return List.copyOf(ids);
    }

    private record ControlPointPair(
        GeometryNormalizationResult.ControlPoint first,
        GeometryNormalizationResult.ControlPoint second
    ) {
        double referenceDistance() {
            return Math.hypot(second.referenceX() - first.referenceX(), second.referenceY() - first.referenceY());
        }
    }
}
