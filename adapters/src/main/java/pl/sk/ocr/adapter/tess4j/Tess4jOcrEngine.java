package pl.sk.ocr.adapter.tess4j;

import java.util.List;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import pl.sk.ocr.core.ocr.OcrEngine;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public final class Tess4jOcrEngine implements OcrEngine {
    @Override
    public OcrText recognize(ProcessingImage image, OcrOptions options) {
        var tesseract = createTesseract(options);
        try {
            return new OcrText(tesseract.doOCR(image.asBufferedImage()), List.of());
        } catch (TesseractException e) {
            throw new IllegalStateException("OCR failed", e);
        }
    }

    private ITesseract createTesseract(OcrOptions options) {
        var tesseract = new Tesseract();
        if (options.language() != null && !options.language().isBlank()) {
            tesseract.setLanguage(options.language());
        }
        if (options.datapath() != null && !options.datapath().isBlank()) {
            tesseract.setDatapath(options.datapath());
        }
        return tesseract;
    }
}
