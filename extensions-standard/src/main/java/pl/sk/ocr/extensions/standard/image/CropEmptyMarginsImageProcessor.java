package pl.sk.ocr.extensions.standard.image;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;
import static pl.sk.ocr.extensions.standard.StandardDescriptors.integerParameter;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;
import pl.sk.ocr.extension.api.image.ImageProcessingRequest;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public final class CropEmptyMarginsImageProcessor extends AbstractImageProcessor {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("crop-empty-margins", ExtensionType.IMAGE_PROCESSOR, "Crop Empty Margins",
            "Crops bright empty margins around document content.",
            integerParameter("backgroundTolerance", "Background tolerance",
                "Pixels with brightness at or above this value are treated as background.", false, 0, 255, 245),
            integerParameter("contentThreshold", "Content threshold",
                "Minimum content pixels required to keep a row or column.", false, 1, null, 10),
            integerParameter("padding", "Padding",
                "Pixels kept around detected content.", false, 0, null, 10));
    }

    @Override
    public ProcessingImage process(ImageProcessingRequest request, ImageProcessingContext context) {
        var source = request.image().asBufferedImage();
        var tolerance = clamp(intParameter(request.parameters(), "backgroundTolerance", 245), 0, 255);
        var threshold = Math.max(1, intParameter(request.parameters(), "contentThreshold", 10));
        var padding = Math.max(0, intParameter(request.parameters(), "padding", 10));

        var minY = firstContentRow(source, tolerance, threshold);
        if (minY < 0) {
            return copyOf(request.image());
        }
        var maxY = lastContentRow(source, tolerance, threshold);
        var minX = firstContentColumn(source, tolerance, threshold);
        var maxX = lastContentColumn(source, tolerance, threshold);
        if (minX < 0 || maxX < minX || maxY < minY) {
            return copyOf(request.image());
        }

        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(source.getWidth() - 1, maxX + padding);
        maxY = Math.min(source.getHeight() - 1, maxY + padding);

        if (minX == 0 && minY == 0 && maxX == source.getWidth() - 1 && maxY == source.getHeight() - 1) {
            return copyOf(request.image());
        }
        return new BufferedImageProcessingImage(source.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1));
    }

    private int firstContentRow(java.awt.image.BufferedImage image, int tolerance, int threshold) {
        for (int y = 0; y < image.getHeight(); y++) {
            if (contentPixelsInRow(image, y, tolerance) >= threshold) {
                return y;
            }
        }
        return -1;
    }

    private int lastContentRow(java.awt.image.BufferedImage image, int tolerance, int threshold) {
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            if (contentPixelsInRow(image, y, tolerance) >= threshold) {
                return y;
            }
        }
        return -1;
    }

    private int firstContentColumn(java.awt.image.BufferedImage image, int tolerance, int threshold) {
        for (int x = 0; x < image.getWidth(); x++) {
            if (contentPixelsInColumn(image, x, tolerance) >= threshold) {
                return x;
            }
        }
        return -1;
    }

    private int lastContentColumn(java.awt.image.BufferedImage image, int tolerance, int threshold) {
        for (int x = image.getWidth() - 1; x >= 0; x--) {
            if (contentPixelsInColumn(image, x, tolerance) >= threshold) {
                return x;
            }
        }
        return -1;
    }

    private int contentPixelsInRow(java.awt.image.BufferedImage image, int y, int tolerance) {
        var count = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            if (isContent(image.getRGB(x, y), tolerance)) {
                count++;
            }
        }
        return count;
    }

    private int contentPixelsInColumn(java.awt.image.BufferedImage image, int x, int tolerance) {
        var count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            if (isContent(image.getRGB(x, y), tolerance)) {
                count++;
            }
        }
        return count;
    }

    private boolean isContent(int rgb, int tolerance) {
        var alpha = (rgb >>> 24) & 0xff;
        if (alpha == 0) {
            return false;
        }
        var red = (rgb >>> 16) & 0xff;
        var green = (rgb >>> 8) & 0xff;
        var blue = rgb & 0xff;
        var brightness = (int) Math.round(0.299 * red + 0.587 * green + 0.114 * blue);
        return brightness < tolerance;
    }

    private int intParameter(ExtensionParameters parameters, String name, int fallback) {
        return parameters.get(name)
            .filter(value -> !value.toString().isBlank())
            .map(value -> value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString()))
            .orElse(fallback);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
