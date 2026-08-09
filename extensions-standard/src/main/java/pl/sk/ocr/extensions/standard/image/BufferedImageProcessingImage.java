package pl.sk.ocr.extensions.standard.image;

import java.awt.image.BufferedImage;
import pl.sk.ocr.extension.api.image.ProcessingImage;

final class BufferedImageProcessingImage implements ProcessingImage {
    private final BufferedImage image;

    BufferedImageProcessingImage(BufferedImage image) {
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

