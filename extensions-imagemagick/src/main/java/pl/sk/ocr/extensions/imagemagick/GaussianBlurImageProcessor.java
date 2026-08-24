package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.decimalParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class GaussianBlurImageProcessor extends AbstractImageMagickProcessor {
    public GaussianBlurImageProcessor() {
        super(processor(
            "im-gaussian-blur",
            "ImageMagick gaussian blur",
            "Applies Gaussian blur.",
            integerParameter("radius", "Radius", "Blur radius.", false, 1, null, 2),
            decimalParameter("sigma", "Sigma", "Gaussian sigma.", false, 0.01d, null, 1.2d)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.gaussianBlur(input,
            integer(parameters, "radius", 2),
            decimal(parameters, "sigma", 1.2d));
    }
}
