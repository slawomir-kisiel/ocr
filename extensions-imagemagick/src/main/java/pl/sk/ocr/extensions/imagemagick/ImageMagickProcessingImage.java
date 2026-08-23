package pl.sk.ocr.extensions.imagemagick;

import java.awt.image.BufferedImage;
import pl.sk.ocr.extension.api.image.ProcessingImage;

final class ImageMagickProcessingImage implements ProcessingImage {
    private final BufferedImage image;

    ImageMagickProcessingImage(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image is required");
        }
        this.image = image;
    }

    @Override
    public int width() {
        return image.getWidth();
    }

    @Override
    public int height() {
        return image.getHeight();
    }

    @Override
    public BufferedImage asBufferedImage() {
        return image;
    }
}
