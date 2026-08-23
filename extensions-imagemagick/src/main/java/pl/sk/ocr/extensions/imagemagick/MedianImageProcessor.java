package pl.sk.ocr.extensions.imagemagick;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.*;

public final class MedianImageProcessor extends AbstractImageMagickProcessor {
    public MedianImageProcessor() {
        super(processor(
            "im-median",
            "ImageMagick median",
            "Applies a 3x3 median filter to reduce isolated pixel noise."
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.median3x3(input);
    }
}
