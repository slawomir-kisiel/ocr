package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.decimalParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class PerspectiveImageProcessor extends AbstractImageMagickProcessor {
    public PerspectiveImageProcessor() {
        super(processor("im-perspective", "ImageMagick perspective", "Warps a quadrilateral region to a rectangular output.",
            decimalParameter("x1", "X1", "Top-left source X. Negative means image left.", false, null, null, -1.0d),
            decimalParameter("y1", "Y1", "Top-left source Y. Negative means image top.", false, null, null, -1.0d),
            decimalParameter("x2", "X2", "Top-right source X. Negative means image right.", false, null, null, -1.0d),
            decimalParameter("y2", "Y2", "Top-right source Y. Negative means image top.", false, null, null, -1.0d),
            decimalParameter("x3", "X3", "Bottom-right source X. Negative means image right.", false, null, null, -1.0d),
            decimalParameter("y3", "Y3", "Bottom-right source Y. Negative means image bottom.", false, null, null, -1.0d),
            decimalParameter("x4", "X4", "Bottom-left source X. Negative means image left.", false, null, null, -1.0d),
            decimalParameter("y4", "Y4", "Bottom-left source Y. Negative means image bottom.", false, null, null, -1.0d),
            integerParameter("width", "Width", "Output width. 0 keeps input width.", false, 0, null, 0),
            integerParameter("height", "Height", "Output height. 0 keeps input height.", false, 0, null, 0)));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        var right = input.getWidth() - 1.0d;
        var bottom = input.getHeight() - 1.0d;
        var quad = new double[] {
            coordinate(parameters, "x1", 0.0d), coordinate(parameters, "y1", 0.0d),
            coordinate(parameters, "x2", right), coordinate(parameters, "y2", 0.0d),
            coordinate(parameters, "x3", right), coordinate(parameters, "y3", bottom),
            coordinate(parameters, "x4", 0.0d), coordinate(parameters, "y4", bottom)
        };
        var width = integer(parameters, "width", 0);
        var height = integer(parameters, "height", 0);
        return ImageMagickLikeOps.perspective(input, quad,
            width <= 0 ? input.getWidth() : width,
            height <= 0 ? input.getHeight() : height);
    }

    private double coordinate(ExtensionParameters parameters, String name, double defaultValue) {
        var value = decimal(parameters, name, -1.0d);
        return value < 0.0d ? defaultValue : value;
    }
}
