package pl.sk.ocr.extension.api.image;

import pl.sk.ocr.extension.api.Extension;

public interface ImageProcessor extends Extension {
    ProcessingImage process(ImageProcessingRequest request, ImageProcessingContext context);
}
