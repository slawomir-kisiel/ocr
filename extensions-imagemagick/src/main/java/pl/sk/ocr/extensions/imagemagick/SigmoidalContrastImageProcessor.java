package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.booleanParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.decimalParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class SigmoidalContrastImageProcessor extends AbstractImageMagickProcessor {
    public SigmoidalContrastImageProcessor() {
        super(processor(
            "im-sigmoidal-contrast",
            "ImageMagick sigmoidal contrast",
            "Adjusts contrast around a normalized midpoint.",
            decimalParameter("contrast", "Contrast", "Sigmoidal contrast strength.", false, 0.0d, 50.0d, 8.0d),
            decimalParameter("midpoint", "Midpoint", "Normalized midpoint from 0.0 to 1.0.", false, 0.0d, 1.0d, 0.5d),
            booleanParameter("sharpen", "Sharpen", "Increase contrast instead of softening it.", false, true)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.sigmoidalContrast(input,
            decimal(parameters, "contrast", 8.0d),
            decimal(parameters, "midpoint", 0.5d),
            bool(parameters, "sharpen", true));
    }
}
