package pl.sk.ocr.configurator.app;

import pl.sk.ocr.adapter.pdfbox.PdfBoxDocumentReader;
import pl.sk.ocr.adapter.tess4j.Tess4jOcrEngine;
import pl.sk.ocr.config.JsonConfigurationMapper;
import pl.sk.ocr.configurator.async.ExecutorBackgroundExecutor;
import pl.sk.ocr.configurator.validation.DraftValidationService;
import pl.sk.ocr.core.document.DocumentReader;
import pl.sk.ocr.core.ocr.OcrEngine;
import pl.sk.ocr.core.processing.FieldProcessingService;
import pl.sk.ocr.extension.api.ServiceLoaderExtensionRegistryFactory;

public record ConfiguratorServices(
    JsonConfigurationMapper mapper,
    DocumentReader documentReader,
    OcrEngine ocrEngine,
    pl.sk.ocr.extension.api.ExtensionRegistry extensionRegistry,
    ExecutorBackgroundExecutor backgroundExecutor,
    DraftValidationService validationService,
    PreviewFieldUseCase previewField
) {
    public static ConfiguratorServices production() {
        var registry = ServiceLoaderExtensionRegistryFactory.load(ConfiguratorServices.class.getClassLoader());
        var ocrEngine = new Tess4jOcrEngine();
        return new ConfiguratorServices(
            new JsonConfigurationMapper(),
            new PdfBoxDocumentReader(),
            ocrEngine,
            registry,
            new ExecutorBackgroundExecutor(),
            new DraftValidationService(registry),
            new PreviewFieldUseCase(new FieldProcessingService(ocrEngine, registry))
        );
    }
}
