package pl.sk.ocr.extensions.imagemagick;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import pl.imagemagick.ocr.preprocess.HistogramOptions;
import pl.imagemagick.ocr.preprocess.ImageDeskew;
import pl.imagemagick.ocr.preprocess.ImagePreprocessOptions;
import pl.imagemagick.ocr.preprocess.MorphologyOperation;
import pl.imagemagick.ocr.preprocess.OcrImagePreprocessor;
import pl.imagemagick.ocr.preprocess.PipelineProfile;
import pl.imagemagick.ocr.preprocess.PreferredOrientation;
import pl.imagemagick.ocr.preprocess.PreprocessResult;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.*;

public final class ProfileImageProcessor extends AbstractImageMagickProcessor {
    private PreprocessResult lastResult;

    public ProfileImageProcessor() {
        super(processor(
            "im-profile",
            "ImageMagick OCR profile",
            "Runs a high-level OCR preprocessing profile.",
            enumParameter("profile", "Profile", "Preprocessing profile.", true,
                List.of("CUSTOM", "GOOD_SCAN", "NOISY_SCAN", "PHONE_PHOTO", "COLOR_BACKGROUND"), "GOOD_SCAN"),
            integerParameter("maxWidth", "Max width", "Maximum output width. 0 disables the limit.", false, 0, 20000, ImagePreprocessOptions.DEFAULT_MAX_WIDTH),
            integerParameter("maxHeight", "Max height", "Maximum output height. 0 disables the limit.", false, 0, 20000, ImagePreprocessOptions.DEFAULT_MAX_HEIGHT),
            enumParameter("orientation", "Orientation", "Preferred orientation.", false, List.of("ANY", "PORTRAIT", "LANDSCAPE"), "ANY"),
            booleanParameter("autoRotate", "Auto rotate", "Rotate by 90 degrees when preferred orientation does not match.", false, false),
            booleanParameter("normalize", "Normalize", "Enable contrast normalization.", false, false),
            booleanParameter("adaptiveThreshold", "Adaptive threshold", "Enable adaptive threshold.", false, false),
            booleanParameter("deskew", "Deskew", "Enable text deskew.", false, false),
            booleanParameter("trim", "Trim", "Trim background margins.", false, false)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        var profile = enumValue(parameters, "profile", PipelineProfile.class, PipelineProfile.GOOD_SCAN);
        var base = ImagePreprocessOptions.profile(profile);
        var options = base.withOverrides(
            integer(parameters, "maxWidth", base.maxWidth()),
            integer(parameters, "maxHeight", base.maxHeight()),
            enumValue(parameters, "orientation", PreferredOrientation.class, base.preferredOrientation()),
            bool(parameters, "autoRotate", base.autoRotateToPreferredOrientation()),
            ImagePreprocessOptions.DEFAULT_ORIENTATION_RATIO_TOLERANCE,
            base.histogramOptions(),
            false,
            bool(parameters, "normalize", base.normalize()),
            null,
            null,
            null,
            false,
            bool(parameters, "adaptiveThreshold", base.adaptiveThreshold()),
            base.adaptiveWindow(),
            base.adaptiveOffset(),
            null,
            bool(parameters, "deskew", base.deskew()),
            ImageDeskew.DEFAULT_THRESHOLD,
            base.deskewAutoCrop(),
            bool(parameters, "trim", base.trim()),
            base.trimTolerance()
        );
        lastResult = new OcrImagePreprocessor().process(input, options);
        return lastResult.image();
    }

    @Override
    Function<BufferedImage, Map<String, Object>> extraTrace() {
        return ignored -> {
            if (lastResult == null) {
                return Map.of();
            }
            var attributes = new HashMap<String, Object>();
            attributes.put("scaleApplied", lastResult.scaleApplied());
            attributes.put("orientationRotated", lastResult.orientationRotated());
            if (lastResult.deskewAngle() != null) {
                attributes.put("deskewAngle", lastResult.deskewAngle());
            }
            if (lastResult.cropBounds() != null) {
                attributes.put("cropBounds", lastResult.cropBounds());
            }
            if (lastResult.normalizeBlackPoint() != null) {
                attributes.put("normalizeBlackPoint", lastResult.normalizeBlackPoint());
            }
            if (lastResult.normalizeWhitePoint() != null) {
                attributes.put("normalizeWhitePoint", lastResult.normalizeWhitePoint());
            }
            return attributes;
        };
    }
}
