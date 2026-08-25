package pl.sk.ocr.core.image;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        return prepareWithTrace(page, renderedPage, imageProcessors).image();
    }

    public DocumentImagePreprocessingResult prepareWithTrace(PageNumber page, ProcessingImage renderedPage, List<ExtensionRef> imageProcessors) {
        if (renderedPage == null) {
            throw new IllegalArgumentException("DOCUMENT_IMAGE_PREPROCESSING_FAILED: rendered page is required");
        }
        var current = renderedPage;
        var steps = imageProcessors == null ? List.<ExtensionRef>of() : imageProcessors;
        var traces = new ArrayList<DocumentImagePreprocessingResult.StepTrace>();
        for (var processorRef : steps) {
            try {
                var input = snapshot(current);
                var events = new ArrayList<DocumentImagePreprocessingResult.EventTrace>();
                current = imageProcessor(processorRef).process(
                    new ImageProcessingRequest(current, ExtensionParameters.of(processorRef.parameters())),
                    () -> traceSink(events)
                );
                traces.add(new DocumentImagePreprocessingResult.StepTrace(
                    traces.size() + 1,
                    processorRef.id().value(),
                    input,
                    current,
                    List.copyOf(events)
                ));
            } catch (RuntimeException e) {
                var message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                throw new IllegalArgumentException("DOCUMENT_IMAGE_PREPROCESSING_FAILED page=" + page.value()
                    + " processor=" + processorRef.id().value() + ": " + message, e);
            }
        }
        return new DocumentImagePreprocessingResult(page, current, traces);
    }

    private ProcessingImage snapshot(ProcessingImage image) {
        var source = image.asBufferedImage();
        var copy = new java.awt.image.BufferedImage(source.getWidth(), source.getHeight(), source.getType() == 0
            ? java.awt.image.BufferedImage.TYPE_INT_ARGB
            : source.getType());
        var graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return new BufferedProcessingImage(copy);
    }

    private TraceSink traceSink(List<DocumentImagePreprocessingResult.EventTrace> events) {
        return (event, attributes) -> events.add(new DocumentImagePreprocessingResult.EventTrace(
            event,
            attributes == null ? Map.of() : Map.copyOf(attributes)
        ));
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
