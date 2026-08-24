package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class StripMetadataImageProcessor extends AbstractImageMagickProcessor {
    public StripMetadataImageProcessor() {
        super(processor("im-strip-metadata", "ImageMagick strip metadata", "Returns an image copy without metadata."));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.stripMetadata(input);
    }
}
