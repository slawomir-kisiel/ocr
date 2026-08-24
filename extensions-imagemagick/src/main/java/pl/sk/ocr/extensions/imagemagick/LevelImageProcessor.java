package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.decimalParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class LevelImageProcessor extends AbstractImageMagickProcessor {
    public LevelImageProcessor() {
        super(processor(
            "im-level",
            "ImageMagick level",
            "Maps selected black and white points to the full tonal range.",
            integerParameter("blackPoint", "Black point", "Input level mapped to black.", false, 0, 255, 0),
            integerParameter("whitePoint", "White point", "Input level mapped to white.", false, 0, 255, 255),
            decimalParameter("gamma", "Gamma", "Midtone gamma correction.", false, 0.01d, 10.0d, 1.0d)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.level(input,
            integer(parameters, "blackPoint", 0),
            integer(parameters, "whitePoint", 255),
            decimal(parameters, "gamma", 1.0d));
    }
}
