package pl.sk.ocr.extensions.standard.image;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;
import pl.sk.ocr.extension.api.image.ImageProcessingRequest;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public final class CropEmptyMarginsImageProcessor extends AbstractImageProcessor {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("crop-empty-margins", ExtensionType.IMAGE_PROCESSOR, "Crop Empty Margins", "Standard placeholder for cropping empty margins.");
    }

    @Override
    public ProcessingImage process(ImageProcessingRequest request, ImageProcessingContext context) {
        return copyOf(request.image());
    }
}

