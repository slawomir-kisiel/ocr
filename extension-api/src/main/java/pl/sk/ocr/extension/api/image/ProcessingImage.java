package pl.sk.ocr.extension.api.image;

import java.awt.image.BufferedImage;

public interface ProcessingImage {
    int width();

    int height();

    BufferedImage asBufferedImage();
}
