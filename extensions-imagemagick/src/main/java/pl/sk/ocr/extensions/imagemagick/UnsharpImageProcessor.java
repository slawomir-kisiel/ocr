package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.decimalParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class UnsharpImageProcessor extends AbstractImageMagickProcessor {
    public UnsharpImageProcessor() {
        super(processor(
            "im-unsharp",
            "ImageMagick unsharp",
            "Sharpens by subtracting a blurred version of the image.",
            integerParameter("radius", "Radius", "Blur radius.", false, 1, null, 2),
            decimalParameter("sigma", "Sigma", "Gaussian blur sigma.", false, 0.01d, null, 1.2d),
            decimalParameter("amount", "Amount", "Sharpening amount.", false, 0.0d, 10.0d, 0.7d),
            integerParameter("threshold", "Threshold", "Minimum channel difference to sharpen.", false, 0, 255, 4)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.unsharp(input,
            integer(parameters, "radius", 2),
            decimal(parameters, "sigma", 1.2d),
            decimal(parameters, "amount", 0.7d),
            integer(parameters, "threshold", 4));
    }
}
