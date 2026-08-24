package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.decimalParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class BilateralImageProcessor extends AbstractImageMagickProcessor {
    public BilateralImageProcessor() {
        super(processor(
            "im-bilateral",
            "ImageMagick bilateral",
            "Reduces noise while preserving text edges.",
            integerParameter("radius", "Radius", "Neighborhood radius.", false, 1, null, 2),
            decimalParameter("spatialSigma", "Spatial sigma", "Spatial distance sigma.", false, 0.01d, null, 2.0d),
            decimalParameter("rangeSigma", "Range sigma", "Color distance sigma.", false, 0.01d, null, 30.0d)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.bilateral(input,
            integer(parameters, "radius", 2),
            decimal(parameters, "spatialSigma", 2.0d),
            decimal(parameters, "rangeSigma", 30.0d));
    }
}
