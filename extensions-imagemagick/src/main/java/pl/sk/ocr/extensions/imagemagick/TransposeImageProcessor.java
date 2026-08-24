package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class TransposeImageProcessor extends AbstractImageMagickProcessor {
    public TransposeImageProcessor() {
        super(processor("im-transpose", "ImageMagick transpose", "Transposes image across the main diagonal."));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.transpose(input);
    }
}
