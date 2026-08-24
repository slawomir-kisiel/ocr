package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class BoxBlurImageProcessor extends AbstractImageMagickProcessor {
    public BoxBlurImageProcessor() {
        super(processor(
            "im-box-blur",
            "ImageMagick box blur",
            "Applies a simple neighborhood average blur.",
            integerParameter("radius", "Radius", "Blur radius.", false, 1, null, 2)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.boxBlur(input, integer(parameters, "radius", 2));
    }
}
