package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class TrimImageProcessor extends AbstractImageMagickProcessor {
    public TrimImageProcessor() {
        super(processor("im-trim", "ImageMagick trim", "Trims background margins.",
            integerParameter("tolerance", "Tolerance", "Background color distance tolerance.", false, 0, 255, 12)));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.trim(input, integer(parameters, "tolerance", 12));
    }
}
