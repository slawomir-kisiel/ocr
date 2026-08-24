package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.Color;
import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class ExtentImageProcessor extends AbstractImageMagickProcessor {
    public ExtentImageProcessor() {
        super(processor("im-extent", "ImageMagick extent", "Places image on a canvas. Width or height 0 keeps current size.",
            integerParameter("width", "Width", "Output canvas width. 0 keeps input width.", false, 0, null, 0),
            integerParameter("height", "Height", "Output canvas height. 0 keeps input height.", false, 0, null, 0),
            integerParameter("red", "Red", "Background red channel.", false, 0, 255, 255),
            integerParameter("green", "Green", "Background green channel.", false, 0, 255, 255),
            integerParameter("blue", "Blue", "Background blue channel.", false, 0, 255, 255)));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        var width = integer(parameters, "width", 0);
        var height = integer(parameters, "height", 0);
        return ImageMagickLikeOps.extent(input,
            width <= 0 ? input.getWidth() : width,
            height <= 0 ? input.getHeight() : height,
            new Color(integer(parameters, "red", 255), integer(parameters, "green", 255), integer(parameters, "blue", 255)));
    }
}
