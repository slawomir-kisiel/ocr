package pl.sk.ocr.configurator.app;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import pl.sk.ocr.config.CategoryRuntimeMapper;
import pl.sk.ocr.config.dto.CategoryDto;
import pl.sk.ocr.config.runtime.CategoriesMode;
import pl.sk.ocr.config.runtime.CsvOutputConfiguration;
import pl.sk.ocr.config.runtime.DirectoriesConfiguration;
import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.config.runtime.ProcessingConfiguration;
import pl.sk.ocr.config.runtime.ProfilePreprocessingConfiguration;
import pl.sk.ocr.config.runtime.ProfileRuntimeConfiguration;
import pl.sk.ocr.config.runtime.RuntimeConfiguration;
import pl.sk.ocr.core.document.DocumentReader;
import pl.sk.ocr.core.document.RenderOptions;
import pl.sk.ocr.core.document.RenderedDocument;
import pl.sk.ocr.core.ocr.OcrEngine;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.core.processing.DocumentProcessor;
import pl.sk.ocr.domain.config.ConfigurationVersion;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.domain.issue.ProcessingStage;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.trace.ProcessingTrace;
import pl.sk.ocr.domain.trace.TraceEntry;
import pl.sk.ocr.domain.trace.TraceMode;
import pl.sk.ocr.extension.api.ExtensionRegistry;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public final class TestCategoryUseCase {
    private final OcrEngine ocrEngine;
    private final ExtensionRegistry extensionRegistry;
    private final CategoryRuntimeMapper categoryMapper;

    public TestCategoryUseCase(OcrEngine ocrEngine, ExtensionRegistry extensionRegistry, CategoryRuntimeMapper categoryMapper) {
        this.ocrEngine = ocrEngine;
        this.extensionRegistry = extensionRegistry;
        this.categoryMapper = categoryMapper;
    }

    public DocumentResult test(CategoryDto category, Path documentPath, Map<PageNumber, ProcessingImage> pages) {
        return test(category, documentPath, pages, new InMemoryTraceImageStore());
    }

    public DocumentResult test(CategoryDto category, Path documentPath, Map<PageNumber, ProcessingImage> pages, TraceImageStore traceImageStore) {
        return test(category, documentPath, pages, traceImageStore, OcrSettings.defaults());
    }

    public DocumentResult test(CategoryDto category, Path documentPath, Map<PageNumber, ProcessingImage> pages,
                               TraceImageStore traceImageStore, OcrSettings defaultOcrSettings) {
        return test(category, documentPath, pages, traceImageStore, defaultOcrSettings, true);
    }

    public DocumentResult test(CategoryDto category, Path documentPath, Map<PageNumber, ProcessingImage> pages,
                               TraceImageStore traceImageStore, OcrSettings defaultOcrSettings, boolean clearTraceImageStore) {
        if (category == null) {
            throw new IllegalArgumentException("category is required");
        }
        if (documentPath == null) {
            throw new IllegalArgumentException("reference document is required");
        }
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("rendered document pages are required");
        }
        if (traceImageStore == null) {
            throw new IllegalArgumentException("trace image store is required");
        }
        if (clearTraceImageStore) {
            traceImageStore.clear();
        }
        var runtime = runtime(category, defaultOcrSettings == null ? OcrSettings.defaults() : defaultOcrSettings);
        var processor = new DocumentProcessor(inMemoryReader(pages), ocrEngine, extensionRegistry);
        var result = processor.process(documentPath, runtime);
        var categorizationDiagnostics = categorizationDiagnostics(pages, runtime.profile().ocr(), traceImageStore);
        return enrichTrace(result, categorizationDiagnostics);
    }

    private TraceEntry categorizationDiagnostics(Map<PageNumber, ProcessingImage> pages, OcrSettings ocr, TraceImageStore traceImageStore) {
        var page = pages.get(new PageNumber(1));
        if (page == null) {
            return null;
        }
        var ref = traceImageStore.put("Categorization OCR input page 1", page);
        var ocrText = recognize(page, ocr);
        return new TraceEntry(
            ProcessingStage.CATEGORY_IDENTIFICATION,
            "Categorization OCR input and raw OCR",
            Map.of(
                "page", 1,
                "rawOcr", ocrText.value(),
                "rawOcrHocr", ocrText.hocr()
            ),
            List.of(ref)
        );
    }

    private OcrText recognize(ProcessingImage page, OcrSettings ocr) {
        try {
            return ocrEngine.recognize(page, new OcrOptions(ocr.language(), ocr.datapath()));
        } catch (RuntimeException | Error e) {
            System.err.println("Category test diagnostic OCR failed; continuing with regular category test.");
            e.printStackTrace(System.err);
            return new OcrText("", List.of());
        }
    }

    private DocumentResult enrichTrace(DocumentResult result, TraceEntry categorizationDiagnostics) {
        if (categorizationDiagnostics == null) {
            return result;
        }
        var trace = result.trace() == null ? ProcessingTrace.off() : result.trace();
        var entries = new java.util.ArrayList<TraceEntry>();
        entries.add(categorizationDiagnostics);
        entries.addAll(trace.entries());
        var enrichedTrace = new ProcessingTrace(trace.mode() == TraceMode.OFF ? TraceMode.FULL : trace.mode(), trace.stages(), entries);
        return new DocumentResult(result.documentId(), result.categoryId(), result.status(), result.fields(), result.issues(), enrichedTrace);
    }

    private DocumentReader inMemoryReader(Map<PageNumber, ProcessingImage> pages) {
        var rendered = new RenderedDocument(Map.copyOf(pages));
        return (source, options) -> rendered;
    }

    private RuntimeConfiguration runtime(CategoryDto category, OcrSettings ocrSettings) {
        var runtimeCategory = categoryMapper.map(category);
        var categoryId = new CategoryId(category.id());
        var base = Path.of(".");
        var profile = new ProfileRuntimeConfiguration(
            "javafx-preview",
            new ConfigurationVersion(category.version() == null ? "1.0" : category.version()),
            base,
            List.of(),
            CategoriesMode.EXPLICIT,
            List.of(categoryId),
            ProfilePreprocessingConfiguration.empty(),
            new DirectoriesConfiguration(base, base, base),
            new ProcessingConfiguration(1, 1),
            ocrSettings,
            TraceMode.FULL,
            new CsvOutputConfiguration(base.resolve("preview.csv"), StandardCharsets.UTF_8, ";", "\"", true, true)
        );
        return new RuntimeConfiguration(profile, List.of(runtimeCategory));
    }
}
