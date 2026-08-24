package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class FlipVerticalImageProcessor extends AbstractImageMagickProcessor {
    public FlipVerticalImageProcessor() {
        super(processor("im-flip-vertical", "ImageMagick flip vertical", "Flips image vertically."));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.flipVertical(input);
    }
}
