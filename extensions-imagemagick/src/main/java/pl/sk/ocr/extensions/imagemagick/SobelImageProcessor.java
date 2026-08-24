package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class SobelImageProcessor extends AbstractImageMagickProcessor {
    public SobelImageProcessor() {
        super(processor(
            "im-sobel",
            "ImageMagick Sobel",
            "Detects edges using the Sobel operator. Use threshold 0 to keep grayscale magnitude.",
            integerParameter("threshold", "Threshold", "Edge threshold. 0 keeps grayscale magnitude.", false, 0, 255, 60)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.sobel(input, integer(parameters, "threshold", 60));
    }
}
