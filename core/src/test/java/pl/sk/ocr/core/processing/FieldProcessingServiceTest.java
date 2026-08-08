package pl.sk.ocr.core.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.runtime.ExtensionRef;
import pl.sk.ocr.config.runtime.FieldDefinition;
import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.geometry.Transform;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.domain.identifier.FieldId;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.result.ProcessingStatus;
import pl.sk.ocr.extension.api.DefaultExtensionRegistry;
import pl.sk.ocr.extension.api.Extension;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;
import pl.sk.ocr.extension.api.image.ImageProcessingRequest;
import pl.sk.ocr.extension.api.image.ImageProcessor;
import pl.sk.ocr.extension.api.image.ProcessingImage;
import pl.sk.ocr.extension.api.transform.ValueTransformationRequest;
import pl.sk.ocr.extension.api.transform.ValueTransformer;
import pl.sk.ocr.extension.api.validation.ValidationContext;
import pl.sk.ocr.extension.api.validation.ValidationRequest;
import pl.sk.ocr.extension.api.validation.ValidationResult;
import pl.sk.ocr.extension.api.validation.ValidationStatus;
import pl.sk.ocr.extension.api.validation.Validator;

class FieldProcessingServiceTest {

    @Test
    void runsImageProcessorsOcrTransformersAndValidatorsInOrder() {
        var service = new FieldProcessingService(
            (image, options) -> new OcrText(" raw value ", List.of()),
            new DefaultExtensionRegistry(List.of(new PassThroughProcessor(), new UppercaseTransformer(), new RequiredValidator()))
        );

        var result = service.extract(field(
            List.of(new ExtensionRef(new ExtensionId("pass-through"), Map.of())),
            List.of(new ExtensionRef(new ExtensionId("uppercase"), Map.of())),
            List.of(new ExtensionRef(new ExtensionId("required"), Map.of()))
        ), image(), Transform.IDENTITY);

        assertThat(result.status()).isEqualTo(ProcessingStatus.SUCCESS);
        assertThat(result.value()).isEqualTo(" RAW VALUE ");
    }

    @Test
    void returnsValidationFailureWhenValidatorRejectsValue() {
        var service = new FieldProcessingService(
            (image, options) -> new OcrText("", List.of()),
            new DefaultExtensionRegistry(List.of(new RequiredValidator()))
        );

        var result = service.extract(field(List.of(), List.of(), List.of(new ExtensionRef(new ExtensionId("required"), Map.of()))), image(), Transform.IDENTITY);

        assertThat(result.status()).isEqualTo(ProcessingStatus.FAILED);
        assertThat(result.issues()).singleElement()
            .satisfies(issue -> assertThat(issue.code().value()).isEqualTo("FIELD_VALIDATION_FAILED"));
    }

    @Test
    void returnsFieldFailureWhenRegionIsOutsideImage() {
        var service = new FieldProcessingService(
            (image, options) -> new OcrText("unused", List.of()),
            new DefaultExtensionRegistry(List.of())
        );
        var field = new FieldDefinition(
            new FieldId("outside"),
            "Outside",
            1,
            new Region(500, 500, 10, 10),
            true,
            OcrSettings.defaults(),
            true,
            "outside",
            List.of(),
            List.of(),
            List.of()
        );

        var result = service.extract(field, image(), Transform.IDENTITY);

        assertThat(result.status()).isEqualTo(ProcessingStatus.FAILED);
        assertThat(result.issues()).singleElement()
            .satisfies(issue -> assertThat(issue.code().value()).isEqualTo("FIELD_REGION_INVALID"));
    }

    private static FieldDefinition field(List<ExtensionRef> processors, List<ExtensionRef> transformers, List<ExtensionRef> validators) {
        return new FieldDefinition(
            new FieldId("field"),
            "Field",
            1,
            new Region(0, 0, 20, 20),
            true,
            OcrSettings.defaults(),
            true,
            "field",
            processors,
            transformers,
            validators
        );
    }

    private static ProcessingImage image() {
        var image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 50, 50);
        graphics.dispose();
        return new BufferedProcessingImage(image);
    }

    private interface DescribedExtension extends Extension {
        String id();

        @Override
        default ExtensionDescriptor descriptor() {
            return new ExtensionDescriptor(new ExtensionId(id()), ExtensionType.VALUE_TRANSFORMER, id(), "", "1.0", List.of());
        }
    }

    private static final class PassThroughProcessor implements ImageProcessor, DescribedExtension {
        @Override
        public ProcessingImage process(ImageProcessingRequest request, ImageProcessingContext context) {
            return request.image();
        }

        @Override
        public String id() {
            return "pass-through";
        }
    }

    private static final class UppercaseTransformer implements ValueTransformer, DescribedExtension {
        @Override
        public String transform(ValueTransformationRequest request) {
            return request.value().toUpperCase(java.util.Locale.ROOT);
        }

        @Override
        public String id() {
            return "uppercase";
        }
    }

    private static final class RequiredValidator implements Validator, DescribedExtension {
        @Override
        public ValidationResult validate(ValidationRequest request, ValidationContext context) {
            return request.value().isBlank()
                ? new ValidationResult(ValidationStatus.INVALID, List.of("required"))
                : new ValidationResult(ValidationStatus.VALID, List.of());
        }

        @Override
        public String id() {
            return "required";
        }
    }
}
