package pl.sk.ocr.extensions.imagemagick;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import pl.imagemagick.ocr.preprocess.ImageDeskew;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.*;

public final class DeskewImageProcessor extends AbstractImageMagickProcessor {
    private ImageDeskew.DeskewResult lastResult;

    public DeskewImageProcessor() {
        super(processor(
            "im-deskew",
            "ImageMagick deskew",
            "Detects and corrects text skew.",
            integerParameter("threshold", "Threshold", "Text pixel threshold.", false, 0, 255, 180),
            booleanParameter("autoCrop", "Auto crop", "Crop background after rotation.", false, true)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        lastResult = ImageMagickLikeOps.deskewWithDiagnostics(input,
            integer(parameters, "threshold", 180), bool(parameters, "autoCrop", true));
        return lastResult.image();
    }

    @Override
    Function<BufferedImage, Map<String, Object>> extraTrace() {
        return ignored -> {
            var attributes = new HashMap<String, Object>();
            if (lastResult != null) {
                attributes.put("angle", lastResult.angle());
                if (lastResult.cropBounds() != null) {
                    attributes.put("cropBounds", lastResult.cropBounds());
                }
            }
            return attributes;
        };
    }
}
