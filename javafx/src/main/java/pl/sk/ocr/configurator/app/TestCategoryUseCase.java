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
import pl.sk.ocr.config.runtime.ProfileRuntimeConfiguration;
import pl.sk.ocr.config.runtime.RuntimeConfiguration;
import pl.sk.ocr.core.document.DocumentReader;
import pl.sk.ocr.core.document.RenderOptions;
import pl.sk.ocr.core.document.RenderedDocument;
import pl.sk.ocr.core.ocr.OcrEngine;
import pl.sk.ocr.core.processing.DocumentProcessor;
import pl.sk.ocr.domain.config.ConfigurationVersion;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.domain.result.DocumentResult;
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
        if (category == null) {
            throw new IllegalArgumentException("category is required");
        }
        if (documentPath == null) {
            throw new IllegalArgumentException("reference document is required");
        }
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("rendered document pages are required");
        }
        var processor = new DocumentProcessor(inMemoryReader(pages), ocrEngine, extensionRegistry);
        return processor.process(documentPath, runtime(category));
    }

    private DocumentReader inMemoryReader(Map<PageNumber, ProcessingImage> pages) {
        var rendered = new RenderedDocument(Map.copyOf(pages));
        return (source, options) -> rendered;
    }

    private RuntimeConfiguration runtime(CategoryDto category) {
        var runtimeCategory = categoryMapper.map(category);
        var categoryId = new CategoryId(category.id());
        var base = Path.of(".");
        var profile = new ProfileRuntimeConfiguration(
            "javafx-preview",
            new ConfigurationVersion(category.version() == null ? "1.0" : category.version()),
            base,
            CategoriesMode.EXPLICIT,
            List.of(categoryId),
            new DirectoriesConfiguration(base, base, base),
            new ProcessingConfiguration(1, 1),
            OcrSettings.defaults(),
            TraceMode.FULL,
            new CsvOutputConfiguration(base.resolve("preview.csv"), StandardCharsets.UTF_8, ";", "\"", true, true)
        );
        return new RuntimeConfiguration(profile, List.of(runtimeCategory));
    }
}
