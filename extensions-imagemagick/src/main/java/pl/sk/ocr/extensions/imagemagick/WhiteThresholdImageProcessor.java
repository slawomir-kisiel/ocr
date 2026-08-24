package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class WhiteThresholdImageProcessor extends AbstractImageMagickProcessor {
    public WhiteThresholdImageProcessor() {
        super(processor(
            "im-white-threshold",
            "ImageMagick white threshold",
            "Forces pixels at or above threshold to white and leaves other pixels unchanged.",
            integerParameter("threshold", "Threshold", "Pixels at or above this grayscale value become white.", false, 0, 255, 230)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.whiteThreshold(input, integer(parameters, "threshold", 230));
    }
}
