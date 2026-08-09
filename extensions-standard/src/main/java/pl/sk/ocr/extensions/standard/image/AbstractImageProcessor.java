package pl.sk.ocr.extensions.standard.image;

import java.awt.image.BufferedImage;
import pl.sk.ocr.extension.api.image.ImageProcessor;
import pl.sk.ocr.extension.api.image.ProcessingImage;

abstract class AbstractImageProcessor implements ImageProcessor {
    ProcessingImage copyOf(ProcessingImage image) {
        var source = image.asBufferedImage();
        var copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return new BufferedImageProcessingImage(copy);
    }
}

