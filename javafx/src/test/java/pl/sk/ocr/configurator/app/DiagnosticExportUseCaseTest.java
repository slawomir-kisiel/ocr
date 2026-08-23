package pl.sk.ocr.configurator.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.sk.ocr.configurator.result.CategoryReferenceDocumentTestResult;
import pl.sk.ocr.domain.identifier.DocumentId;
import pl.sk.ocr.domain.issue.ProcessingStage;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.result.ProcessingStatus;
import pl.sk.ocr.domain.result.StageResult;
import pl.sk.ocr.domain.trace.ProcessingTrace;
import pl.sk.ocr.domain.trace.TraceEntry;
import pl.sk.ocr.domain.trace.TraceMode;
import pl.sk.ocr.extension.api.image.ProcessingImage;

class DiagnosticExportUseCaseTest {
    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void exportsTraceImageAndMetadataWithDeterministicNames() throws Exception {
        var store = new InMemoryTraceImageStore();
        var ref = store.put("crop", image());
        var trace = new ProcessingTrace(
            TraceMode.FULL,
            List.of(new StageResult(ProcessingStage.FIELD_OCR, ProcessingStatus.SUCCESS, List.of())),
            List.of(new TraceEntry(ProcessingStage.FIELD_OCR, "ocr",
                Map.of("step", 1, "rawOcr", "Voucher", "rawOcrHocr", "<html><body>Voucher</body></html>"),
                List.of(ref)))
        );
        var useCase = new DiagnosticExportUseCase();

        var images = useCase.exportAllImages(tempDir, trace, store);
        var metadata = useCase.exportMetadata(tempDir, trace);
        var bundle = useCase.exportBundle(tempDir.resolve("diagnostics.zip"), trace, store);

        assertThat(images.files()).extracting(path -> path.getFileName().toString())
            .containsExactly("001_FIELD_OCR_001.png");
        assertThat(metadata.files()).extracting(path -> path.getFileName().toString())
            .containsExactly("trace-metadata.json");
        assertThat(bundle.files()).extracting(path -> path.getFileName().toString())
            .containsExactly("diagnostics.zip");
        assertThat(Files.readString(tempDir.resolve("trace-metadata.json")))
            .contains("\"stage\" : \"FIELD_OCR\"");
        try (var zip = new ZipFile(tempDir.resolve("diagnostics.zip").toFile())) {
            assertThat(zip.getEntry("trace-metadata.json")).isNotNull();
            assertThat(zip.getEntry("001_FIELD_OCR_001.png")).isNotNull();
            assertThat(zip.getEntry("artifacts/001_FIELD_OCR_raw-ocr.hocr")).isNotNull();
            assertThat(zip.getEntry("artifacts/002_FIELD_OCR_raw-ocr.txt")).isNotNull();
        }
    }

    @Test
    void exportsCategorizationOcrArtifacts() throws Exception {
        var store = new InMemoryTraceImageStore();
        var ref = store.put("categorization", image());
        var trace = new ProcessingTrace(
            TraceMode.FULL,
            List.of(new StageResult(ProcessingStage.CATEGORY_IDENTIFICATION, ProcessingStatus.SUCCESS, List.of())),
            List.of(new TraceEntry(ProcessingStage.CATEGORY_IDENTIFICATION, "categorization",
                Map.of("rawOcr", "Voucher", "rawOcrHocr", "<html><body>Voucher</body></html>"),
                List.of(ref)))
        );
        var useCase = new DiagnosticExportUseCase();

        useCase.exportBundle(tempDir.resolve("category-diagnostics.zip"), trace, store);

        try (var zip = new ZipFile(tempDir.resolve("category-diagnostics.zip").toFile())) {
            assertThat(zip.getEntry("001_CATEGORY_IDENTIFICATION_001.png")).isNotNull();
            assertThat(zip.getEntry("artifacts/001_CATEGORY_IDENTIFICATION_raw-ocr.hocr")).isNotNull();
            assertThat(zip.getEntry("artifacts/002_CATEGORY_IDENTIFICATION_raw-ocr.txt")).isNotNull();
        }
    }

    @Test
    void exportsReferenceDocumentDiagnosticsBundle() throws Exception {
        var store = new InMemoryTraceImageStore();
        var ref = store.put("preprocessing input", image());
        var trace = new ProcessingTrace(
            TraceMode.FULL,
            List.of(new StageResult(ProcessingStage.PAGE_PREPARATION, ProcessingStatus.SUCCESS, List.of())),
            List.of(new TraceEntry(ProcessingStage.PAGE_PREPARATION, "preprocessing",
                Map.of("rawOcr", "Voucher", "rawOcrHocr", "<html><body>Voucher</body></html>"),
                List.of(ref)))
        );
        var document = new DocumentResult(new DocumentId("dark.pdf"), null, ProcessingStatus.FAILED, List.of(), List.of(), trace);
        var result = new CategoryReferenceDocumentTestResult("dark-skewed", "samples/dark.pdf", tempDir.resolve("dark.pdf"), document, store);
        var useCase = new DiagnosticExportUseCase();

        useCase.exportReferenceDocumentBundle(tempDir.resolve("reference-diagnostics.zip"), List.of(result));

        try (var zip = new ZipFile(tempDir.resolve("reference-diagnostics.zip").toFile())) {
            assertThat(zip.getEntry("metadata.json")).isNotNull();
            assertThat(zip.getEntry("documents/dark-skewed/trace.json")).isNotNull();
            assertThat(zip.getEntry("documents/dark-skewed/images/001_PAGE_PREPARATION_001.png")).isNotNull();
            assertThat(zip.getEntry("documents/dark-skewed/artifacts/001_PAGE_PREPARATION_raw-ocr.hocr")).isNotNull();
            assertThat(zip.getEntry("documents/dark-skewed/artifacts/002_PAGE_PREPARATION_raw-ocr.txt")).isNotNull();
        }
    }

    @Test
    void exportsTraceMetadataWithRuntimeObjectsInAttributes() throws Exception {
        var store = new InMemoryTraceImageStore();
        var ref = store.put("preprocessing output", image());
        var trace = new ProcessingTrace(
            TraceMode.FULL,
            List.of(new StageResult(ProcessingStage.PAGE_PREPARATION, ProcessingStatus.SUCCESS, List.of())),
            List.of(new TraceEntry(ProcessingStage.PAGE_PREPARATION, "preprocessing",
                Map.of(
                    "events", List.of(Map.of(
                        "event", "crop",
                        "attributes", Map.of("cropBounds", new Rectangle(1, 2, 3, 4))
                    )),
                    "image", image()
                ),
                List.of(ref)))
        );
        var document = new DocumentResult(new DocumentId("cropped.pdf"), null, ProcessingStatus.SUCCESS, List.of(), List.of(), trace);
        var result = new CategoryReferenceDocumentTestResult("cropped", "samples/cropped.pdf", tempDir.resolve("cropped.pdf"), document, store);
        var useCase = new DiagnosticExportUseCase();

        useCase.exportReferenceDocumentBundle(tempDir.resolve("runtime-attributes.zip"), List.of(result));

        try (var zip = new ZipFile(tempDir.resolve("runtime-attributes.zip").toFile())) {
            var traceJson = new String(zip.getInputStream(zip.getEntry("documents/cropped/trace.json")).readAllBytes());
            assertThat(traceJson)
                .contains("\"cropBounds\"")
                .contains("\"x\" : 1")
                .contains("\"width\" : 3")
                .contains("\"type\"");
        }
    }

    private ProcessingImage image() {
        return new ProcessingImage() {
            private final BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

            @Override
            public int width() {
                return image.getWidth();
            }

            @Override
            public int height() {
                return image.getHeight();
            }

            @Override
            public BufferedImage asBufferedImage() {
                return image;
            }
        };
    }
}
