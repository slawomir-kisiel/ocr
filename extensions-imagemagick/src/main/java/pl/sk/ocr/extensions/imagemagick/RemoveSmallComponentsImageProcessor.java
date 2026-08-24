package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class RemoveSmallComponentsImageProcessor extends AbstractImageMagickProcessor {
    public RemoveSmallComponentsImageProcessor() {
        super(processor("im-remove-small-components", "ImageMagick remove small components", "Removes small dark connected components.",
            integerParameter("minArea", "Min area", "Dark components smaller than this area are removed.", false, 1, null, 6)));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.removeSmallComponents(input, integer(parameters, "minArea", 6));
    }
}
