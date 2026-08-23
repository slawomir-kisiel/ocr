package pl.sk.ocr.extensions.imagemagick;

import java.awt.image.BufferedImage;
import java.util.List;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.imagemagick.ocr.preprocess.MorphologyOperation;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.*;

public final class MorphologyImageProcessor extends AbstractImageMagickProcessor {
    public MorphologyImageProcessor() {
        super(processor(
            "im-morphology",
            "ImageMagick morphology",
            "Applies binary morphology operation.",
            enumParameter("operation", "Operation", "Morphology operation.", false,
                List.of("ERODE", "DILATE", "OPEN", "CLOSE"), "OPEN")
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.morphology(input,
            enumValue(parameters, "operation", MorphologyOperation.class, MorphologyOperation.OPEN));
    }
}
