package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.decimalParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class HsvThresholdImageProcessor extends AbstractImageMagickProcessor {
    public HsvThresholdImageProcessor() {
        super(processor(
            "im-hsv-threshold",
            "ImageMagick HSV threshold",
            "Creates a binary mask for pixels matching hue, saturation and value constraints.",
            decimalParameter("minHue", "Min hue", "Minimum normalized hue.", false, 0.0d, 1.0d, 0.55d),
            decimalParameter("maxHue", "Max hue", "Maximum normalized hue.", false, 0.0d, 1.0d, 0.75d),
            decimalParameter("minSaturation", "Min saturation", "Minimum normalized saturation.", false, 0.0d, 1.0d, 0.2d),
            decimalParameter("minValue", "Min value", "Minimum normalized value.", false, 0.0d, 1.0d, 0.2d)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.hsvThreshold(input,
            (float) decimal(parameters, "minHue", 0.55d),
            (float) decimal(parameters, "maxHue", 0.75d),
            (float) decimal(parameters, "minSaturation", 0.2d),
            (float) decimal(parameters, "minValue", 0.2d));
    }
}
