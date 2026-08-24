package pl.sk.ocr.core.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.sk.ocr.config.runtime.*;
import pl.sk.ocr.core.document.RenderedDocument;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.core.processing.DocumentProcessor;
import pl.sk.ocr.domain.config.ConfigurationVersion;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.identifier.BatchId;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.identifier.FieldId;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.result.ProcessingStatus;
import pl.sk.ocr.domain.trace.TraceMode;
import pl.sk.ocr.extension.api.image.ProcessingImage;

class BatchDispatcherTest {
    @TempDir
    Path tempDir;

    @Test
    void processesFilesConcurrentlyWritesCsvAndMovesSources() throws Exception {
        var input = Files.createDirectory(tempDir.resolve("input"));
        var success = Files.createDirectory(tempDir.resolve("success"));
        var error = Files.createDirectory(tempDir.resolve("error"));
        for (int i = 0; i < 60; i++) {
            Files.writeString(input.resolve("doc-%03d.pdf".formatted(i)), "PDF");
        }
        Files.writeString(input.resolve("bad-000.pdf"), "BROKEN");
        var output = tempDir.resolve("out").resolve("result.csv");
        var summary = tempDir.resolve("out").resolve("summary.json");
        var configuration = configuration(input, success, error, output);
        var processor = new DocumentProcessor(new FakeReader(), new FakeOcrEngine());
        var dispatcher = new BatchDispatcher(processor);

        var result = dispatcher.run(new BatchId("batch-1"), new BatchOptions(configuration, 4, null, summary));

        assertThat(result.documents()).hasSize(61);
        assertThat(result.status()).isEqualTo(ProcessingStatus.FAILED);
        assertThat(Files.list(input)).isEmpty();
        assertThat(Files.list(success)).hasSize(60);
        assertThat(Files.list(error)).hasSize(1);
        assertThat(Files.readAllLines(output)).hasSize(62);
        assertThat(Files.readString(output)).contains("doc-000.pdf").contains("bad-000.pdf").contains("DOCUMENT_PROCESSING_FAILED");
        assertThat(Files.readString(summary)).contains("\"totalDocuments\" : 61");
    }

    private static RuntimeConfiguration configuration(Path input, Path success, Path error, Path output) {
        var category = new CategoryRuntimeConfiguration(
            new CategoryId("generic"),
            new ConfigurationVersion("1.0"),
            "Generic",
            new SinglePageSelection(1),
            OcrSettings.defaults(),
            new GeometryConfiguration(100, 100, "NONE", List.of()),
            List.of(new IdentificationGroup(List.of(new IdentificationCondition(1, "DOC", null,
                new pl.sk.ocr.config.runtime.ExtensionRef(new pl.sk.ocr.domain.identifier.ExtensionId("text"), java.util.Map.of()), null)))),
            List.of(),
            List.of(new FieldDefinition(
                new FieldId("number"),
                "Number",
                1,
                new Region(0, 0, 10, 10),
                true,
                OcrSettings.defaults(),
                true,
                "number",
                List.of(),
                List.of(),
                List.of()
            ))
        );
        var profile = new ProfileRuntimeConfiguration(
            "default",
            new ConfigurationVersion("1.0"),
            Path.of("."),
            List.of(),
            CategoriesMode.EXPLICIT,
            List.of(new CategoryId("generic")),
            ProfilePreprocessingConfiguration.empty(),
            new DirectoriesConfiguration(input, success, error),
            new ProcessingConfiguration(4, 16),
            OcrSettings.defaults(),
            TraceMode.OFF,
            new CsvOutputConfiguration(output, java.nio.charset.StandardCharsets.UTF_8, ";", "\"", true, true)
        );
        return new RuntimeConfiguration(profile, List.of(category));
    }

    private static final class FakeReader implements pl.sk.ocr.core.document.DocumentReader {
        @Override
        public RenderedDocument read(Path source, pl.sk.ocr.core.document.RenderOptions options) {
            if (source.getFileName().toString().startsWith("bad")) {
                throw new IllegalStateException("broken input");
            }
            return new RenderedDocument(Map.of(new PageNumber(1), new BufferedProcessingImage(testImage())));
        }
    }

    private static final class FakeOcrEngine implements pl.sk.ocr.core.ocr.OcrEngine {
        @Override
        public OcrText recognize(ProcessingImage image, OcrOptions options) {
            if (image.width() == 100) {
                return new OcrText("DOC", List.of());
            }
            return new OcrText("42", List.of());
        }
    }

    private static BufferedImage testImage() {
        var image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 100, 100);
        graphics.dispose();
        return image;
    }
}

