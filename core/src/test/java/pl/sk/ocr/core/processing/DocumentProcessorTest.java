package pl.sk.ocr.core.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.runtime.*;
import pl.sk.ocr.core.document.RenderOptions;
import pl.sk.ocr.core.document.RenderedDocument;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.domain.config.ConfigurationVersion;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.identifier.AnchorId;
import pl.sk.ocr.domain.identifier.FieldId;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.domain.ocr.BoundingBox;
import pl.sk.ocr.domain.ocr.Confidence;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.ocr.OcrWord;
import pl.sk.ocr.domain.result.ProcessingStatus;
import pl.sk.ocr.domain.trace.TraceMode;
import pl.sk.ocr.extension.api.image.ProcessingImage;

class DocumentProcessorTest {

    @Test
    void processesSimpleDocumentThroughWalkingSkeleton() {
        var image = testImage();
        var reader = (pl.sk.ocr.core.document.DocumentReader) (source, options) ->
            new RenderedDocument(Map.of(new PageNumber(1), new BufferedProcessingImage(image)));
        var ocr = new FakeOcrEngine();
        var processor = new DocumentProcessor(reader, ocr);

        var result = processor.process(Path.of("simple-document.pdf"), configuration());

        assertThat(result.status()).isEqualTo(ProcessingStatus.SUCCESS);
        assertThat(result.categoryId()).isEqualTo(new CategoryId("invoice-a"));
        assertThat(result.fields()).singleElement()
            .satisfies(field -> {
                assertThat(field.fieldId()).isEqualTo(new FieldId("document-number"));
                assertThat(field.value()).isEqualTo("FV-123");
            });
    }

    @Test
    void returnsAmbiguousWhenMultipleCategoriesMatch() {
        var reader = (pl.sk.ocr.core.document.DocumentReader) (source, options) ->
            new RenderedDocument(Map.of(new PageNumber(1), new BufferedProcessingImage(testImage())));
        var processor = new DocumentProcessor(reader, new FakeOcrEngine());
        var base = configuration();
        var second = new CategoryRuntimeConfiguration(
            new CategoryId("invoice-b"),
            new ConfigurationVersion("1.0"),
            "Invoice B",
            new SinglePageSelection(1),
            OcrSettings.defaults(),
            new GeometryConfiguration(100, 100, "NONE", List.of()),
            base.categories().getFirst().identificationGroups(),
            List.of(),
            base.categories().getFirst().fields()
        );

        var result = processor.process(Path.of("ambiguous.pdf"), new RuntimeConfiguration(base.profile(), List.of(base.categories().getFirst(), second)));

        assertThat(result.status()).isEqualTo(ProcessingStatus.FAILED);
        assertThat(result.issues()).singleElement()
            .satisfies(issue -> assertThat(issue.code().value()).isEqualTo("CATEGORY_AMBIGUOUS"));
    }

    @Test
    void classifyOnlyStopsAfterCategoryIdentification() {
        var reader = (pl.sk.ocr.core.document.DocumentReader) (source, options) ->
            new RenderedDocument(Map.of(new PageNumber(1), new BufferedProcessingImage(testImage())));
        var processor = new DocumentProcessor(reader, new FakeOcrEngine());

        var result = processor.process(Path.of("classify.pdf"), configuration(ProcessingMode.CLASSIFY_ONLY));

        assertThat(result.status()).isEqualTo(ProcessingStatus.SUCCESS);
        assertThat(result.categoryId()).isEqualTo(new CategoryId("invoice-a"));
        assertThat(result.fields()).isEmpty();
    }

    private static RuntimeConfiguration configuration() {
        return configuration(ProcessingMode.FULL);
    }

    private static RuntimeConfiguration configuration(ProcessingMode mode) {
        var field = new FieldDefinition(
            new FieldId("document-number"),
            "Document number",
            1,
            new Region(0, 0, 20, 20),
            true,
            OcrSettings.defaults(),
            true,
            "document_number",
            List.of(),
            List.of(new ExtensionRef(new pl.sk.ocr.domain.identifier.ExtensionId("trim"), Map.of())),
            List.of()
        );
        var category = new CategoryRuntimeConfiguration(
            new CategoryId("invoice-a"),
            new ConfigurationVersion("1.0"),
            "Invoice A",
            new SinglePageSelection(1),
            OcrSettings.defaults(),
            new GeometryConfiguration(100, 100, "SINGLE_REFERENCE", List.of(new AnchorId("title"))),
            List.of(new IdentificationGroup(List.of(new IdentificationCondition(1, "INVOICE", null,
                new ExtensionRef(new pl.sk.ocr.domain.identifier.ExtensionId("text"), Map.of()), null)))),
            List.of(new AnchorDefinition(
                new AnchorId("title"),
                1,
                new ExtensionRef(new pl.sk.ocr.domain.identifier.ExtensionId("text"), Map.of()),
                "INVOICE",
                null,
                true,
                new Region(10, 10, 20, 20),
                null
            )),
            List.of(field)
        );
        var profile = new ProfileRuntimeConfiguration(
            "default",
            new ConfigurationVersion("1.0"),
            Path.of("."),
            List.of(),
            CategoriesMode.EXPLICIT,
            List.of(new CategoryId("invoice-a")),
            ProfilePreprocessingConfiguration.empty(),
            new DirectoriesConfiguration(Path.of("input"), Path.of("success"), Path.of("error")),
            new ProcessingConfiguration(1, 4, mode),
            OcrSettings.defaults(),
            TraceMode.OFF,
            new CsvOutputConfiguration(Path.of("result.csv"), java.nio.charset.StandardCharsets.UTF_8, ";", "\"", true, false)
        );
        return new RuntimeConfiguration(profile, List.of(category));
    }

    private static BufferedImage testImage() {
        var image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 100, 100);
        graphics.dispose();
        return image;
    }

    private static final class FakeOcrEngine implements pl.sk.ocr.core.ocr.OcrEngine {
        @Override
        public OcrText recognize(ProcessingImage image, OcrOptions options) {
            if (image.width() == 100) {
                return new OcrText("INVOICE", List.of(new OcrWord(
                    "INVOICE",
                    new BoundingBox(new Region(20, 20, 40, 40)),
                    new Confidence(1.0)
                )));
            }
            return new OcrText(" FV-123 ", List.of());
        }
    }
}

