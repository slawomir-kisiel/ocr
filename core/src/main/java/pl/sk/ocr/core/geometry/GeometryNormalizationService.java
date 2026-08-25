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
    private static final double ROBUST_RESIDUAL_THRESHOLD = 5.0;

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
        var strategy = strategy(geometry.strategy());
        if ("ANCHOR_TRANSLATION".equals(strategy)) {
            return translationFrom(controlPoints);
        }
        if ("AFFINE".equals(strategy)) {
            return affineFrom(controlPoints).orElseGet(() -> scaleTranslateFrom(controlPoints));
        }
        if ("ROBUST_AFFINE".equals(strategy)) {
            return robustAffineFrom(controlPoints).orElseGet(() -> affineFrom(controlPoints).orElseGet(() -> scaleTranslateFrom(controlPoints)));
        }
        return scaleTranslateFrom(controlPoints);
    }

    private String strategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return "NONE";
        }
        if ("ANCHORS".equals(strategy)) {
            return "TWO_POINT_SCALE_TRANSLATE";
        }
        return strategy;
    }

    private GeometryNormalizationResult scaleTranslateFrom(List<GeometryNormalizationResult.ControlPoint> controlPoints) {
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

    private java.util.Optional<GeometryNormalizationResult> affineFrom(List<GeometryNormalizationResult.ControlPoint> controlPoints) {
        if (controlPoints.size() < 3) {
            return java.util.Optional.empty();
        }
        var coefficients = affineCoefficients(controlPoints);
        if (coefficients.isEmpty()) {
            return java.util.Optional.empty();
        }
        var transform = transform(coefficients.get());
        return java.util.Optional.of(new GeometryNormalizationResult(
            GeometryStatus.NORMALIZED,
            transform,
            usedAnchors(controlPoints),
            controlPoints,
            null
        ));
    }

    private java.util.Optional<GeometryNormalizationResult> robustAffineFrom(List<GeometryNormalizationResult.ControlPoint> controlPoints) {
        if (controlPoints.size() < 4) {
            return affineFrom(controlPoints);
        }
        RobustCandidate best = null;
        for (int first = 0; first < controlPoints.size(); first++) {
            for (int second = first + 1; second < controlPoints.size(); second++) {
                for (int third = second + 1; third < controlPoints.size(); third++) {
                    var seed = List.of(controlPoints.get(first), controlPoints.get(second), controlPoints.get(third));
                    var coefficients = affineCoefficients(seed);
                    if (coefficients.isEmpty()) {
                        continue;
                    }
                    var candidate = robustCandidate(transform(coefficients.get()), coefficients.get(), controlPoints);
                    if (best == null || candidate.betterThan(best)) {
                        best = candidate;
                    }
                }
            }
        }
        if (best == null || best.inliers().size() < 3) {
            return java.util.Optional.empty();
        }
        if (best.inliers().size() == controlPoints.size()) {
            return affineFrom(controlPoints);
        }
        return affineFrom(best.inliers());
    }

    private RobustCandidate robustCandidate(Transform transform, AffineCoefficients coefficients,
                                            List<GeometryNormalizationResult.ControlPoint> controlPoints) {
        var inliers = controlPoints.stream()
            .filter(point -> residual(transform, point) <= ROBUST_RESIDUAL_THRESHOLD)
            .toList();
        var error = inliers.stream()
            .mapToDouble(point -> residual(transform, point))
            .average()
            .orElse(Double.MAX_VALUE);
        var totalError = controlPoints.stream()
            .mapToDouble(point -> residual(transform, point))
            .average()
            .orElse(Double.MAX_VALUE);
        var distortion = Math.abs(coefficients.a() - 1.0)
            + Math.abs(coefficients.b())
            + Math.abs(coefficients.c())
            + Math.abs(coefficients.d() - 1.0);
        return new RobustCandidate(inliers, error, totalError, distortion);
    }

    private java.util.Optional<AffineCoefficients> affineCoefficients(List<GeometryNormalizationResult.ControlPoint> points) {
        var matrix = new double[3][3];
        var targetX = new double[3];
        var targetY = new double[3];
        for (var point : points) {
            var x = point.referenceX();
            var y = point.referenceY();
            var row = new double[] {x, y, 1.0};
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    matrix[i][j] += row[i] * row[j];
                }
                targetX[i] += row[i] * point.detectedX();
                targetY[i] += row[i] * point.detectedY();
            }
        }
        var xCoefficients = solve3(matrix, targetX);
        var yCoefficients = solve3(matrix, targetY);
        if (xCoefficients == null || yCoefficients == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new AffineCoefficients(
            xCoefficients[0],
            xCoefficients[1],
            yCoefficients[0],
            yCoefficients[1],
            xCoefficients[2],
            yCoefficients[2]
        ));
    }

    private Transform transform(AffineCoefficients coefficients) {
        var scaleX = Math.max(AXIS_EPSILON, Math.hypot(coefficients.a(), coefficients.c()));
        var scaleY = Math.max(AXIS_EPSILON, Math.hypot(coefficients.b(), coefficients.d()));
        return new Transform(
            new Scale(scaleX, scaleY),
            coefficients.tx(),
            coefficients.ty(),
            coefficients.a(),
            coefficients.b(),
            coefficients.c(),
            coefficients.d()
        );
    }

    private double residual(Transform transform, GeometryNormalizationResult.ControlPoint point) {
        var mapped = transform.map(new pl.sk.ocr.domain.geometry.Point(point.referenceX(), point.referenceY()));
        return Math.hypot(mapped.x() - point.detectedX(), mapped.y() - point.detectedY());
    }

    private double[] solve3(double[][] sourceMatrix, double[] sourceVector) {
        var matrix = new double[3][3];
        var vector = new double[3];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(sourceMatrix[i], 0, matrix[i], 0, 3);
            vector[i] = sourceVector[i];
        }
        for (int pivot = 0; pivot < 3; pivot++) {
            var best = pivot;
            for (int row = pivot + 1; row < 3; row++) {
                if (Math.abs(matrix[row][pivot]) > Math.abs(matrix[best][pivot])) {
                    best = row;
                }
            }
            if (Math.abs(matrix[best][pivot]) < 0.000001) {
                return null;
            }
            if (best != pivot) {
                var tmpRow = matrix[pivot];
                matrix[pivot] = matrix[best];
                matrix[best] = tmpRow;
                var tmpValue = vector[pivot];
                vector[pivot] = vector[best];
                vector[best] = tmpValue;
            }
            var divisor = matrix[pivot][pivot];
            for (int column = pivot; column < 3; column++) {
                matrix[pivot][column] /= divisor;
            }
            vector[pivot] /= divisor;
            for (int row = 0; row < 3; row++) {
                if (row == pivot) {
                    continue;
                }
                var factor = matrix[row][pivot];
                for (int column = pivot; column < 3; column++) {
                    matrix[row][column] -= factor * matrix[pivot][column];
                }
                vector[row] -= factor * vector[pivot];
            }
        }
        return vector;
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

    private record AffineCoefficients(double a, double b, double c, double d, double tx, double ty) {
    }

    private record RobustCandidate(List<GeometryNormalizationResult.ControlPoint> inliers, double meanResidual, double totalMeanResidual,
                                   double distortion) {
        boolean betterThan(RobustCandidate other) {
            if (inliers.size() != other.inliers().size()) {
                return inliers.size() > other.inliers().size();
            }
            if (Math.abs(distortion - other.distortion()) > 0.000001) {
                return distortion < other.distortion();
            }
            if (Math.abs(totalMeanResidual - other.totalMeanResidual()) > 0.000001) {
                return totalMeanResidual < other.totalMeanResidual();
            }
            return meanResidual < other.meanResidual();
        }
    }
}
