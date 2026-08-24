package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class CropImageProcessor extends AbstractImageMagickProcessor {
    public CropImageProcessor() {
        super(processor("im-crop", "ImageMagick crop", "Crops image to a rectangle. Width or height 0 means remaining image size.",
            integerParameter("x", "X", "Crop origin X.", false, 0, null, 0),
            integerParameter("y", "Y", "Crop origin Y.", false, 0, null, 0),
            integerParameter("width", "Width", "Crop width. 0 means image width minus X.", false, 0, null, 0),
            integerParameter("height", "Height", "Crop height. 0 means image height minus Y.", false, 0, null, 0)));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        var x = Math.min(Math.max(0, integer(parameters, "x", 0)), input.getWidth() - 1);
        var y = Math.min(Math.max(0, integer(parameters, "y", 0)), input.getHeight() - 1);
        var width = integer(parameters, "width", 0);
        var height = integer(parameters, "height", 0);
        width = width <= 0 ? input.getWidth() - x : Math.min(width, input.getWidth() - x);
        height = height <= 0 ? input.getHeight() - y : Math.min(height, input.getHeight() - y);
        return ImageMagickLikeOps.crop(input, x, y, width, height);
    }
}
