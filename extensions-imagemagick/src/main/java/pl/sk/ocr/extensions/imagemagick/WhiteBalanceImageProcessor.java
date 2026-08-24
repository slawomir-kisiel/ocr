package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class WhiteBalanceImageProcessor extends AbstractImageMagickProcessor {
    public WhiteBalanceImageProcessor() {
        super(processor(
            "im-white-balance",
            "ImageMagick white balance",
            "Applies gray-world white balance to reduce color casts."
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.grayWorldWhiteBalance(input);
    }
}
