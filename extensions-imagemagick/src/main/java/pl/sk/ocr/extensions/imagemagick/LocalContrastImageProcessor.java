package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.decimalParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class LocalContrastImageProcessor extends AbstractImageMagickProcessor {
    public LocalContrastImageProcessor() {
        super(processor(
            "im-local-contrast",
            "ImageMagick local contrast",
            "Enhances local contrast using neighborhood statistics.",
            integerParameter("radius", "Radius", "Neighborhood radius.", false, 1, null, 5),
            decimalParameter("amount", "Amount", "Local contrast strength.", false, 0.0d, 10.0d, 1.0d)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.localContrast(input,
            integer(parameters, "radius", 5),
            decimal(parameters, "amount", 1.0d));
    }
}
