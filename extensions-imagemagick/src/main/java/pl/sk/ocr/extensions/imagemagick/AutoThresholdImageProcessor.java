package pl.sk.ocr.extensions.imagemagick;

import java.awt.image.BufferedImage;
import java.util.List;
import pl.imagemagick.ocr.preprocess.AutoThresholdMethod;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.*;

public final class AutoThresholdImageProcessor extends AbstractImageMagickProcessor {
    public AutoThresholdImageProcessor() {
        super(processor(
            "im-auto-threshold",
            "ImageMagick auto threshold",
            "Binarizes image using an automatic threshold method.",
            enumParameter("method", "Method", "Threshold selection method.", false, List.of("OTSU", "TRIANGLE", "KAPUR"), "OTSU")
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.autoThreshold(input, enumValue(parameters, "method", AutoThresholdMethod.class, AutoThresholdMethod.OTSU));
    }
}
