package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class KuwaharaImageProcessor extends AbstractImageMagickProcessor {
    public KuwaharaImageProcessor() {
        super(processor(
            "im-kuwahara",
            "ImageMagick Kuwahara",
            "Smooths low-variance regions while preserving stronger boundaries.",
            integerParameter("radius", "Radius", "Filter radius.", false, 1, null, 2)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.kuwahara(input, integer(parameters, "radius", 2));
    }
}
