package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class BlackThresholdImageProcessor extends AbstractImageMagickProcessor {
    public BlackThresholdImageProcessor() {
        super(processor(
            "im-black-threshold",
            "ImageMagick black threshold",
            "Forces pixels at or below threshold to black and leaves other pixels unchanged.",
            integerParameter("threshold", "Threshold", "Pixels at or below this grayscale value become black.", false, 0, 255, 45)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.blackThreshold(input, integer(parameters, "threshold", 45));
    }
}
