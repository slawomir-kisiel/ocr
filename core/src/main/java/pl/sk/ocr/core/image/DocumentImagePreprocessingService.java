package pl.sk.ocr.core.image;

import java.util.List;
import pl.sk.ocr.config.runtime.ExtensionRef;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.extension.api.Extension;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.ExtensionRegistry;
import pl.sk.ocr.extension.api.image.ImageProcessingRequest;
import pl.sk.ocr.extension.api.image.ImageProcessor;
import pl.sk.ocr.extension.api.image.ProcessingImage;
import pl.sk.ocr.extension.api.trace.TraceSink;

public final class DocumentImagePreprocessingService {
    private final ExtensionRegistry extensionRegistry;

    public DocumentImagePreprocessingService(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    public ProcessingImage prepare(PageNumber page, ProcessingImage renderedPage, List<ExtensionRef> imageProcessors) {
        if (renderedPage == null) {
            throw new IllegalArgumentException("DOCUMENT_IMAGE_PREPROCESSING_FAILED: rendered page is required");
        }
        var current = renderedPage;
        var steps = imageProcessors == null ? List.<ExtensionRef>of() : imageProcessors;
        for (var processorRef : steps) {
            try {
                current = imageProcessor(processorRef).process(
                    new ImageProcessingRequest(current, ExtensionParameters.of(processorRef.parameters())),
                    () -> TraceSink.NOOP
                );
            } catch (RuntimeException e) {
                var message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                throw new IllegalArgumentException("DOCUMENT_IMAGE_PREPROCESSING_FAILED page=" + page.value()
                    + " processor=" + processorRef.id().value() + ": " + message, e);
            }
        }
        return current;
    }

    private ImageProcessor imageProcessor(ExtensionRef ref) {
        Extension extension = extensionRegistry.find(ref.id())
            .orElseThrow(() -> new IllegalArgumentException("DOCUMENT_IMAGE_PROCESSOR_NOT_FOUND: " + ref.id().value()));
        if (extension instanceof ImageProcessor processor) {
            return processor;
        }
        throw new IllegalArgumentException("DOCUMENT_IMAGE_PROCESSOR_INVALID_TYPE: " + ref.id().value());
    }
}
