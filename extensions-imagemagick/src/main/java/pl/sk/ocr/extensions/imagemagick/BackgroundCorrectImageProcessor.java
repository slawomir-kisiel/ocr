package pl.sk.ocr.extensions.imagemagick;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.*;

public final class BackgroundCorrectImageProcessor extends AbstractImageMagickProcessor {
    public BackgroundCorrectImageProcessor() {
        super(processor(
            "im-background-correct",
            "ImageMagick background correct",
            "Corrects uneven background by subtracting blurred background estimate.",
            integerParameter("blurRadius", "Blur radius", "Background estimation blur radius.", false, 1, 200, 25)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.correctBackground(input, integer(parameters, "blurRadius", 25));
    }
}
