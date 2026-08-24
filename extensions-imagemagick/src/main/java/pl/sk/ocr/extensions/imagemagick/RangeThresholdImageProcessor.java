package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.Color;
import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class RangeThresholdImageProcessor extends AbstractImageMagickProcessor {
    public RangeThresholdImageProcessor() {
        super(processor(
            "im-range-threshold",
            "ImageMagick range threshold",
            "Binarizes image by mapping grayscale values inside a range to black and others to white.",
            integerParameter("low", "Low", "Lowest grayscale value included in the black range.", false, 0, 255, 0),
            integerParameter("high", "High", "Highest grayscale value included in the black range.", false, 0, 255, 160)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.rangeThreshold(input,
            integer(parameters, "low", 0),
            integer(parameters, "high", 160),
            Color.BLACK,
            Color.WHITE);
    }
}
