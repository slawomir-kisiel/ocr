package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.decimalParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class GammaImageProcessor extends AbstractImageMagickProcessor {
    public GammaImageProcessor() {
        super(processor(
            "im-gamma",
            "ImageMagick gamma",
            "Applies gamma correction to image midtones.",
            decimalParameter("gamma", "Gamma", "Gamma correction value.", false, 0.01d, 10.0d, 1.0d)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.gamma(input, decimal(parameters, "gamma", 1.0d));
    }
}
