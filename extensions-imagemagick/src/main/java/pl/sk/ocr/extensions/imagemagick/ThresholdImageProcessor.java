package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class ThresholdImageProcessor extends AbstractImageMagickProcessor {
    public ThresholdImageProcessor() {
        super(processor(
            "im-threshold",
            "ImageMagick threshold",
            "Binarizes image using a fixed grayscale threshold.",
            integerParameter("threshold", "Threshold", "Pixels below this grayscale value become black.", false, 0, 255, 180)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.threshold(input, integer(parameters, "threshold", 180));
    }
}
