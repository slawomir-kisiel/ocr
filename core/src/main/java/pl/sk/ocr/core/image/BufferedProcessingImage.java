package pl.sk.ocr.core.image;

import java.awt.image.BufferedImage;
import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public record BufferedProcessingImage(BufferedImage asBufferedImage) implements ProcessingImage {
    public BufferedProcessingImage {
        asBufferedImage = Validation.requireNonNull(asBufferedImage, "image");
    }

    @Override
    public int width() {
        return asBufferedImage.getWidth();
    }

    @Override
    public int height() {
        return asBufferedImage.getHeight();
    }

    public BufferedProcessingImage crop(Region region) {
        if (region.x() >= width() || region.y() >= height()
            || region.x() + region.width() <= 0 || region.y() + region.height() <= 0) {
            throw new IllegalArgumentException("region is outside image bounds");
        }
        var x = clamp((int) Math.round(region.x()), 0, width() - 1);
        var y = clamp((int) Math.round(region.y()), 0, height() - 1);
        var maxWidth = width() - x;
        var maxHeight = height() - y;
        var cropWidth = clamp((int) Math.round(region.width()), 1, maxWidth);
        var cropHeight = clamp((int) Math.round(region.height()), 1, maxHeight);
        return new BufferedProcessingImage(asBufferedImage.getSubimage(x, y, cropWidth, cropHeight));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
