package pl.sk.ocr.extensions.imagemagick;

import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.decimalParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.integerParameter;
import static pl.sk.ocr.extensions.imagemagick.ImageMagickDescriptors.processor;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import pl.imagemagick.ocr.preprocess.ImageMagickLikeOps;
import pl.imagemagick.ocr.preprocess.TableDetectionOptions;
import pl.imagemagick.ocr.preprocess.TableDetectionResult;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;

public final class RemoveTableFramesImageProcessor extends AbstractImageMagickProcessor {
    private TableDetectionResult lastDetection;

    public RemoveTableFramesImageProcessor() {
        super(processor(
            "im-remove-table-frames",
            "ImageMagick remove table frames",
            "Detects line-based tables and covers their frame lines with surrounding background color.",
            integerParameter("frameThickness", "Frame thickness",
                "Line thickness to cover around each detected table row or column line.", false, 1, null, 3),
            integerParameter("sampleRadius", "Sample radius",
                "Neighborhood radius used to estimate replacement background color.", false, 1, null, 4),
            integerParameter("adaptiveWindow", "Adaptive window",
                "Odd adaptive threshold window used by table detection.", false, 3, null, 41),
            integerParameter("adaptiveOffset", "Adaptive offset",
                "Adaptive threshold offset used by table detection.", false, null, null, 10),
            integerParameter("lineGapTolerance", "Line gap tolerance",
                "Maximum gap inside a broken table line.", false, 0, null, 12),
            integerParameter("lineMergeTolerance", "Line merge tolerance",
                "Maximum distance for merging nearby table lines.", false, 0, null, 5),
            decimalParameter("minLineCoverage", "Min line coverage",
                "Minimum black-pixel coverage for a broken table line.", false, 0.01d, 1.0d, 0.55d),
            decimalParameter("minLineLengthRatio", "Min line length ratio",
                "Minimum table line length relative to image dimension.", false, 0.01d, 1.0d, 0.15d),
            integerParameter("minRows", "Min rows",
                "Minimum detected row count.", false, 1, null, 2),
            integerParameter("minColumns", "Min columns",
                "Minimum detected column count.", false, 1, null, 2)
        ));
    }

    @Override
    BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context) {
        var detectionOptions = detectionOptions(parameters);
        lastDetection = ImageMagickLikeOps.detectTables(input, detectionOptions);
        return ImageMagickLikeOps.removeTableFrames(input, detectionOptions,
            Math.max(1, integer(parameters, "frameThickness", 3)),
            Math.max(1, integer(parameters, "sampleRadius", 4)));
    }

    @Override
    Function<BufferedImage, Map<String, Object>> extraTrace() {
        return ignored -> {
            if (lastDetection == null) {
                return Map.of();
            }
            var attributes = new HashMap<String, Object>();
            attributes.put("tablesDetected", lastDetection.tables().size());
            attributes.put("tableCellsDetected", lastDetection.tables().stream().mapToInt(table -> table.cells().size()).sum());
            attributes.put("tableRowsDetected", lastDetection.tables().stream().mapToInt(table -> table.rows().size()).sum());
            attributes.put("tableColumnsDetected", lastDetection.tables().stream().mapToInt(table -> table.columns().size()).sum());
            return attributes;
        };
    }

    private TableDetectionOptions detectionOptions(ExtensionParameters parameters) {
        var adaptiveWindow = Math.max(3, integer(parameters, "adaptiveWindow", 41));
        if (adaptiveWindow % 2 == 0) {
            adaptiveWindow++;
        }
        return new TableDetectionOptions(
            adaptiveWindow,
            integer(parameters, "adaptiveOffset", 10),
            Math.max(0, integer(parameters, "lineGapTolerance", 12)),
            Math.max(0, integer(parameters, "lineMergeTolerance", 5)),
            clamp(decimal(parameters, "minLineCoverage", 0.55d), 0.01d, 1.0d),
            clamp(decimal(parameters, "minLineLengthRatio", 0.15d), 0.01d, 1.0d),
            Math.max(1, integer(parameters, "minRows", 2)),
            Math.max(1, integer(parameters, "minColumns", 2))
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
