package pl.sk.ocr.core.identification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.runtime.ExtensionRef;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.config.runtime.GeometryConfiguration;
import pl.sk.ocr.config.runtime.IdentificationCondition;
import pl.sk.ocr.config.runtime.IdentificationGroup;
import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.config.runtime.SinglePageSelection;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.domain.config.ConfigurationVersion;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.ocr.BoundingBox;
import pl.sk.ocr.domain.ocr.Confidence;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.ocr.OcrWord;
import pl.sk.ocr.extension.api.DefaultExtensionRegistry;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.detector.DetectionRequest;
import pl.sk.ocr.extension.api.detector.DetectionResult;
import pl.sk.ocr.extension.api.detector.DetectionStatus;
import pl.sk.ocr.extension.api.detector.Detector;
import pl.sk.ocr.extension.api.detector.DetectorContext;

class CategoryIdentificationServiceTest {

    @Test
    void appliesAndInsideGroupAndOrBetweenGroups() {
        var service = new CategoryIdentificationService();
        var category = category("invoice-a", List.of(
            new IdentificationGroup(List.of(
                new IdentificationCondition(1, "INVOICE", null, new ExtensionRef(new ExtensionId("text"), Map.of()), null),
                new IdentificationCondition(1, "TOTAL", null, new ExtensionRef(new ExtensionId("text"), Map.of()), null)
            )),
            new IdentificationGroup(List.of(
                new IdentificationCondition(1, "FORM", null, new ExtensionRef(new ExtensionId("text"), Map.of()), null)
            ))
        ));

        var result = service.identify(List.of(category), new OcrText("FORM ABC", List.of()));

        assertThat(result.status()).isEqualTo(IdentificationStatus.MATCHED);
        assertThat(result.category().id()).isEqualTo(new CategoryId("invoice-a"));
    }

    @Test
    void matchesQrConditionUsingDetectorPayload() {
        var service = new CategoryIdentificationService(new DefaultExtensionRegistry(List.of(new TestQrDetector())));
        var category = category("voucher", List.of(new IdentificationGroup(List.of(
            new IdentificationCondition(1, "VOUCHER", null, new ExtensionRef(new ExtensionId("qr"), Map.of()), null)
        ))));

        var result = service.identify(List.of(category), new OcrText("", List.of()),
            new BufferedProcessingImage(new java.awt.image.BufferedImage(20, 20, java.awt.image.BufferedImage.TYPE_INT_RGB)));

        assertThat(result.status()).isEqualTo(IdentificationStatus.MATCHED);
        assertThat(result.category().id()).isEqualTo(new CategoryId("voucher"));
    }

    private static CategoryRuntimeConfiguration category(String id, List<IdentificationGroup> groups) {
        return new CategoryRuntimeConfiguration(
            new CategoryId(id),
            new ConfigurationVersion("1.0"),
            id,
            new SinglePageSelection(1),
            OcrSettings.defaults(),
            new GeometryConfiguration(100, 100, "NONE", List.of()),
            groups,
            List.of(),
            List.of()
        );
    }

    private static final class TestQrDetector implements Detector {
        @Override
        public DetectionResult detect(DetectionRequest request, DetectorContext context) {
            return new DetectionResult(DetectionStatus.DETECTED,
                new OcrText("VOUCHER-1", List.of(new OcrWord("VOUCHER-1", new BoundingBox(new Region(1, 1, 10, 10)), new Confidence(1.0)))),
                "QR detected");
        }

        @Override
        public ExtensionDescriptor descriptor() {
            return new ExtensionDescriptor(new ExtensionId("qr"), ExtensionType.DETECTOR, "QR", "QR", "1.0", List.of());
        }
    }
}
