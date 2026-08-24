package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class ShaveImageProcessor extends AbstractImageMagickProcessor {
    public ShaveImageProcessor() {
        super(processor("im-shave", "ImageMagick shave", "Removes margins from image edges.",
            integerParameter("left", "Left", "Left margin to remove.", false, 0, null, 0),
            integerParameter("top", "Top", "Top margin to remove.", false, 0, null, 0),
            integerParameter("right", "Right", "Right margin to remove.", false, 0, null, 0),
            integerParameter("bottom", "Bottom", "Bottom margin to remove.", false, 0, null, 0)));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.shave(input,
            integer(parameters, "left", 0), integer(parameters, "top", 0),
            integer(parameters, "right", 0), integer(parameters, "bottom", 0));
    }
}
