package pl.sk.ocr.extensions.imagemagick;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.*;

public final class AdaptiveThresholdImageProcessor extends AbstractImageMagickProcessor {
    public AdaptiveThresholdImageProcessor() {
        super(processor(
            "im-adaptive-threshold",
            "ImageMagick adaptive threshold",
            "Binarizes image with a local adaptive threshold.",
            integerParameter("window", "Window", "Odd local window size, minimum 3.", false, 3, 501, 31),
            integerParameter("offset", "Offset", "Threshold offset from local mean.", false, -255, 255, 8)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.adaptiveThreshold(input, integer(parameters, "window", 31), integer(parameters, "offset", 8));
    }
}
