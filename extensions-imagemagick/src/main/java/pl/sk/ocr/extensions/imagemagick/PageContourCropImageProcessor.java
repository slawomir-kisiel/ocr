package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class PageContourCropImageProcessor extends AbstractImageMagickProcessor {
    public PageContourCropImageProcessor() {
        super(processor("im-page-contour-crop", "ImageMagick page contour crop", "Crops image to detected page contour.",
            integerParameter("edgeThreshold", "Edge threshold", "Sobel edge threshold.", false, 0, 255, 60),
            integerParameter("margin", "Margin", "Margin kept around detected contour.", false, 0, null, 10)));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.cropToPageContour(input,
            integer(parameters, "edgeThreshold", 60),
            integer(parameters, "margin", 10));
    }
}
