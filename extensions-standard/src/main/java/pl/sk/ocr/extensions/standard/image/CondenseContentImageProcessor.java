package pl.sk.ocr.extensions.standard.image;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;
import pl.sk.ocr.extension.api.image.ImageProcessingRequest;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public final class CondenseContentImageProcessor extends AbstractImageProcessor {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("condense-content", ExtensionType.IMAGE_PROCESSOR, "Condense Content", "Standard placeholder for condensing sparse content before OCR.");
    }

    @Override
    public ProcessingImage process(ImageProcessingRequest request, ImageProcessingContext context) {
        return copyOf(request.image());
    }
}

