package pl.sk.ocr.core.geometry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.runtime.AnchorDefinition;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.config.runtime.ExtensionRef;
import pl.sk.ocr.config.runtime.GeometryConfiguration;
import pl.sk.ocr.config.runtime.IdentificationGroup;
import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.config.runtime.SinglePageSelection;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.domain.config.ConfigurationVersion;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.identifier.AnchorId;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.domain.ocr.BoundingBox;
import pl.sk.ocr.domain.ocr.Confidence;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.ocr.OcrWord;
import pl.sk.ocr.extension.api.DefaultExtensionRegistry;

class AnchorDetectionServiceTest {

    @Test
    void textAnchorReturnsMatchedWordBoundsInsideSearchRegion() {
        var service = new AnchorDetectionService(new DefaultExtensionRegistry(List.of()));
        var anchorId = new AnchorId("total-anchor");
        var category = new CategoryRuntimeConfiguration(
            new CategoryId("invoice"),
            new ConfigurationVersion("1.0"),
            "Invoice",
            new SinglePageSelection(1),
            OcrSettings.defaults(),
            new GeometryConfiguration(1000, 1000, "ANCHOR", List.of(anchorId)),
            List.of(new IdentificationGroup(List.of())),
            List.of(new AnchorDefinition(anchorId, 1, new ExtensionRef(new ExtensionId("text"), Map.of()), "TOTAL", null, true,
                new Region(20, 20, 40, 10), new Region(100, 100, 300, 200))),
            List.of()
        );
        var pageOcr = new OcrText("TOTAL", List.of(
            new OcrWord("TOTAL", new BoundingBox(new Region(150, 180, 45, 12)), new Confidence(0.91))
        ));
        var pageImage = new BufferedProcessingImage(new java.awt.image.BufferedImage(800, 800, java.awt.image.BufferedImage.TYPE_INT_RGB));

        var features = service.detect(category, pageOcr, pageImage);

        assertThat(features).hasSize(1);
        assertThat(features.getFirst().bounds()).isEqualTo(new Region(150, 180, 45, 12));
    }

    @Test
    void textAnchorReturnsBoundsForSmallestMatchingWordSequence() {
        var service = new AnchorDetectionService(new DefaultExtensionRegistry(List.of()));
        var anchorId = new AnchorId("invoice-number-anchor");
        var category = new CategoryRuntimeConfiguration(
            new CategoryId("invoice"),
            new ConfigurationVersion("1.0"),
            "Invoice",
            new SinglePageSelection(1),
            OcrSettings.defaults(),
            new GeometryConfiguration(1000, 1000, "ANCHOR", List.of(anchorId)),
            List.of(new IdentificationGroup(List.of())),
            List.of(new AnchorDefinition(anchorId, 1, new ExtensionRef(new ExtensionId("text"), Map.of()), "INVOICE NUMBER", null, true,
                new Region(20, 20, 120, 10), new Region(100, 100, 300, 200))),
            List.of()
        );
        var pageOcr = new OcrText("THE INVOICE NUMBER 123", List.of(
            new OcrWord("THE", new BoundingBox(new Region(110, 180, 25, 12)), new Confidence(0.80)),
            new OcrWord("INVOICE", new BoundingBox(new Region(150, 180, 60, 12)), new Confidence(0.90)),
            new OcrWord("NUMBER", new BoundingBox(new Region(215, 180, 55, 12)), new Confidence(0.86)),
            new OcrWord("123", new BoundingBox(new Region(275, 180, 30, 12)), new Confidence(0.82))
        ));
        var pageImage = new BufferedProcessingImage(new java.awt.image.BufferedImage(800, 800, java.awt.image.BufferedImage.TYPE_INT_RGB));

        var features = service.detect(category, pageOcr, pageImage);

        assertThat(features).hasSize(1);
        assertThat(features.getFirst().bounds()).isEqualTo(new Region(150, 180, 120, 12));
        assertThat(features.getFirst().confidence()).isEqualTo(0.88);
    }
}
