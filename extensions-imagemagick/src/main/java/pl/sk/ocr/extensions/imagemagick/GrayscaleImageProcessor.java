package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class GrayscaleImageProcessor extends AbstractImageMagickProcessor {
    public GrayscaleImageProcessor() {
        super(processor(
            "im-grayscale",
            "ImageMagick grayscale",
            "Converts a color scan to grayscale."
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.grayscale(input);
    }
}
