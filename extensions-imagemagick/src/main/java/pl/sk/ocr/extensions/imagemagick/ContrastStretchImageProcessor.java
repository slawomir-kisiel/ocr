package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.decimalParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class ContrastStretchImageProcessor extends AbstractImageMagickProcessor {
    public ContrastStretchImageProcessor() {
        super(processor(
            "im-contrast-stretch",
            "ImageMagick contrast stretch",
            "Stretches tonal range by clipping black and white histogram fractions.",
            decimalParameter("blackFraction", "Black fraction", "Fraction of darkest pixels clipped to black.", false, 0.0d, 1.0d, 0.01d),
            decimalParameter("whiteFraction", "White fraction", "Fraction of brightest pixels clipped to white.", false, 0.0d, 1.0d, 0.01d)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.contrastStretch(input,
            decimal(parameters, "blackFraction", 0.01d),
            decimal(parameters, "whiteFraction", 0.01d));
    }
}
