package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.decimalParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class ClaheImageProcessor extends AbstractImageMagickProcessor {
    public ClaheImageProcessor() {
        super(processor(
            "im-clahe",
            "ImageMagick CLAHE",
            "Applies local histogram equalization with contrast limiting.",
            integerParameter("tileSize", "Tile size", "CLAHE tile size in pixels.", false, 2, null, 64),
            decimalParameter("clipLimit", "Clip limit", "Maximum local histogram amplification.", false, 0.01d, 100.0d, 2.0d)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.clahe(input,
            integer(parameters, "tileSize", 64),
            decimal(parameters, "clipLimit", 2.0d));
    }
}
