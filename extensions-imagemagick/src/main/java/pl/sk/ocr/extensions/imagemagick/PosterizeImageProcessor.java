package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class PosterizeImageProcessor extends AbstractImageMagickProcessor {
    public PosterizeImageProcessor() {
        super(processor("im-posterize", "ImageMagick posterize", "Reduces the number of tonal levels.",
            integerParameter("levels", "Levels", "Number of posterization levels.", false, 2, 256, 4)));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.posterize(input, integer(parameters, "levels", 4));
    }
}
