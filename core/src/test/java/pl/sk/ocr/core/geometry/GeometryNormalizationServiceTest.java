package pl.sk.ocr.core.geometry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.runtime.AnchorDefinition;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.config.runtime.ExtensionRef;
import pl.sk.ocr.config.runtime.GeometryConfiguration;
import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.config.runtime.SinglePageSelection;
import pl.sk.ocr.domain.config.ConfigurationVersion;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.identifier.AnchorId;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.identifier.ExtensionId;

class GeometryNormalizationServiceTest {

    @Test
    void translatesOnlyWhenSingleTextAnchorIsDetected() {
        var anchorId = new AnchorId("title");
        var category = category(List.of(anchor(anchorId, "text", new Region(10, 10, 20, 20), true)), List.of(anchorId));

        var result = new GeometryNormalizationService().normalize(
            category,
            List.of(new ReferenceFeature(anchorId, new Region(20, 30, 40, 60), 1.0))
        );

        assertThat(result.status()).isEqualTo(GeometryStatus.NORMALIZED);
        assertThat(result.transform().scale().x()).isEqualTo(1.0);
        assertThat(result.transform().scale().y()).isEqualTo(1.0);
        assertThat(result.transform().map(new Region(100, 100, 20, 20))).isEqualTo(new Region(110, 120, 20, 20));
        assertThat(result.usedControlPoints()).hasSize(1);
    }

    @Test
    void scalesFromTwoTextAnchorTopLeftPoints() {
        var first = new AnchorId("first");
        var second = new AnchorId("second");
        var category = category(
            List.of(
                anchor(first, "text", new Region(10, 10, 20, 20), true),
                anchor(second, "text", new Region(110, 210, 20, 20), true)
            ),
            List.of(first, second)
        );

        var result = new GeometryNormalizationService().normalize(
            category,
            List.of(
                new ReferenceFeature(first, new Region(20, 30, 40, 60), 1.0),
                new ReferenceFeature(second, new Region(220, 430, 40, 60), 1.0)
            )
        );

        assertThat(result.status()).isEqualTo(GeometryStatus.NORMALIZED);
        assertThat(result.transform().scale().x()).isEqualTo(2.0);
        assertThat(result.transform().scale().y()).isEqualTo(2.0);
        assertThat(result.transform().translateX()).isEqualTo(0.0);
        assertThat(result.transform().translateY()).isEqualTo(10.0);
        assertThat(result.usedAnchors()).containsExactly(first, second);
    }

    @Test
    void usesQrBottomRightAsSecondControlPoint() {
        var anchorId = new AnchorId("qr");
        var category = category(List.of(anchor(anchorId, "qr", new Region(10, 20, 100, 50), true)), List.of(anchorId));

        var result = new GeometryNormalizationService().normalize(
            category,
            List.of(new ReferenceFeature(anchorId, new Region(20, 40, 200, 100), 1.0))
        );

        assertThat(result.status()).isEqualTo(GeometryStatus.NORMALIZED);
        assertThat(result.transform().scale().x()).isEqualTo(2.0);
        assertThat(result.transform().scale().y()).isEqualTo(2.0);
        assertThat(result.transform().translateX()).isEqualTo(0.0);
        assertThat(result.transform().translateY()).isEqualTo(0.0);
        assertThat(result.usedControlPoints())
            .extracting(GeometryNormalizationResult.ControlPoint::point)
            .containsExactly("TOP_LEFT", "BOTTOM_RIGHT");
    }

    @Test
    void selectsControlPointPairWithLargestReferenceDistance() {
        var near = new AnchorId("near");
        var middle = new AnchorId("middle");
        var far = new AnchorId("far");
        var category = category(
            List.of(
                anchor(near, "text", new Region(0, 0, 20, 20), true),
                anchor(middle, "text", new Region(20, 20, 20, 20), true),
                anchor(far, "text", new Region(200, 100, 20, 20), true)
            ),
            List.of(near, middle, far)
        );

        var result = new GeometryNormalizationService().normalize(
            category,
            List.of(
                new ReferenceFeature(near, new Region(10, 10, 20, 20), 1.0),
                new ReferenceFeature(middle, new Region(50, 50, 20, 20), 1.0),
                new ReferenceFeature(far, new Region(410, 210, 20, 20), 1.0)
            )
        );

        assertThat(result.usedAnchors()).containsExactly(near, far);
        assertThat(result.usedControlPoints())
            .extracting(GeometryNormalizationResult.ControlPoint::anchorId)
            .containsExactly(near, far);
    }

    @Test
    void failsWhenRequiredGeometryAnchorIsMissing() {
        var anchorId = new AnchorId("missing");
        var category = category(List.of(anchor(anchorId, "text", new Region(10, 10, 20, 20), true)), List.of(anchorId));

        var result = new GeometryNormalizationService().normalize(category, List.of());

        assertThat(result.status()).isEqualTo(GeometryStatus.FAILED);
        assertThat(result.transform()).isEqualTo(pl.sk.ocr.domain.geometry.Transform.IDENTITY);
    }

    private CategoryRuntimeConfiguration category(List<AnchorDefinition> anchors, List<AnchorId> geometryAnchors) {
        return new CategoryRuntimeConfiguration(
            new CategoryId("invoice-a"),
            new ConfigurationVersion("1.0"),
            "Invoice",
            new SinglePageSelection(1),
            OcrSettings.defaults(),
            new GeometryConfiguration(100, 100, "ANCHORS", geometryAnchors),
            List.of(),
            anchors,
            List.of()
        );
    }

    private AnchorDefinition anchor(AnchorId id, String detectorId, Region bounds, boolean required) {
        return new AnchorDefinition(
            id,
            1,
            new ExtensionRef(new ExtensionId(detectorId), Map.of()),
            null,
            null,
            required,
            bounds,
            null
        );
    }
}
