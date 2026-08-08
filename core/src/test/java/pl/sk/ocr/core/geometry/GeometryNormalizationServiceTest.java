package pl.sk.ocr.core.geometry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.runtime.AnchorDefinition;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.config.runtime.GeometryConfiguration;
import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.config.runtime.SinglePageSelection;
import pl.sk.ocr.domain.config.ConfigurationVersion;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.identifier.AnchorId;
import pl.sk.ocr.domain.identifier.CategoryId;

class GeometryNormalizationServiceTest {

    @Test
    void computesScaleAndTranslationFromReferenceAnchor() {
        var anchorId = new AnchorId("title");
        var category = new CategoryRuntimeConfiguration(
            new CategoryId("invoice-a"),
            new ConfigurationVersion("1.0"),
            "Invoice",
            new SinglePageSelection(1),
            OcrSettings.defaults(),
            new GeometryConfiguration(100, 100, "SINGLE_REFERENCE", List.of(anchorId)),
            List.of(),
            List.of(new AnchorDefinition(anchorId, 1, null, true, new Region(10, 10, 20, 20), null)),
            List.of()
        );

        var result = new GeometryNormalizationService().normalize(
            category,
            List.of(new ReferenceFeature(anchorId, new Region(20, 30, 40, 60), 1.0))
        );

        assertThat(result.status()).isEqualTo(GeometryStatus.NORMALIZED);
        assertThat(result.transform().map(new Region(10, 10, 20, 20))).isEqualTo(new Region(20, 30, 40, 60));
    }
}
