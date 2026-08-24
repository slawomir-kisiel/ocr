package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.booleanParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.decimalParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class RotateImageProcessor extends AbstractImageMagickProcessor {
    public RotateImageProcessor() {
        super(processor("im-rotate", "ImageMagick rotate", "Rotates image by the given angle.",
            decimalParameter("degrees", "Degrees", "Rotation angle in degrees.", false, -360.0d, 360.0d, 0.0d),
            booleanParameter("expandCanvas", "Expand canvas", "Expand canvas to fit rotated image.", false, true)));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.rotate(input, decimal(parameters, "degrees", 0.0d), bool(parameters, "expandCanvas", true));
    }
}
