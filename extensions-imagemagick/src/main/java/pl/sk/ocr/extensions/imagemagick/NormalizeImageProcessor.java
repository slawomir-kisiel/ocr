package pl.sk.ocr.extensions.imagemagick;

import java.awt.image.BufferedImage;
import java.util.List;
import pl.imagemagick.ocr.preprocess.HistogramOptions;
import pl.imagemagick.ocr.preprocess.HistogramRegionMode;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.*;

public final class NormalizeImageProcessor extends AbstractImageMagickProcessor {
    public NormalizeImageProcessor() {
        super(processor(
            "im-normalize",
            "ImageMagick normalize",
            "Normalizes tonal range using configurable histogram region.",
            enumParameter("histogramRegion", "Histogram region", "Histogram analysis region.", false,
                List.of("FULL", "CENTER_PERCENT", "AUTO_STABLE_CENTER"), "FULL"),
            integerParameter("centerPercent", "Center percent", "Central region percent.", false, 1, 100, 70),
            integerParameter("autoStart", "Auto start", "Initial center percent for AUTO_STABLE_CENTER.", false, 1, 100, 50),
            integerParameter("autoStep", "Auto step", "Region growth step for AUTO_STABLE_CENTER.", false, 1, 100, 10),
            integerParameter("autoMax", "Auto max", "Maximum center percent for AUTO_STABLE_CENTER.", false, 1, 100, 100),
            integerParameter("medianJump", "Median jump", "Maximum accepted median jump.", false, 0, 255, 25),
            decimalParameter("blackRatioJump", "Black ratio jump", "Maximum accepted black ratio jump.", false, 0.0, 1.0, 0.05),
            integerParameter("blackThreshold", "Black threshold", "Black pixel threshold for histogram diagnostics.", false, 0, 255, 32)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        return ImageMagickLikeOps.normalize(input, histogramOptions(parameters));
    }

    private HistogramOptions histogramOptions(ExtensionParameters parameters) {
        var mode = enumValue(parameters, "histogramRegion", HistogramRegionMode.class, HistogramRegionMode.FULL);
        return new HistogramOptions(
            mode,
            integer(parameters, "centerPercent", 70),
            integer(parameters, "autoStart", 50),
            integer(parameters, "autoStep", 10),
            integer(parameters, "autoMax", 100),
            integer(parameters, "medianJump", 25),
            decimal(parameters, "blackRatioJump", 0.05),
            integer(parameters, "blackThreshold", 32)
        );
    }
}
