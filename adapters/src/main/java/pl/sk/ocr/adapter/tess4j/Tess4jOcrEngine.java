package pl.sk.ocr.adapter.tess4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import pl.sk.ocr.core.ocr.OcrEngine;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public final class Tess4jOcrEngine implements OcrEngine {
    static final String WINDOWS_DEFAULT_DATAPATH = "C:\\Program Files\\Tesseract-OCR\\tessdata";

    @Override
    public OcrText recognize(ProcessingImage image, OcrOptions options) {
        validateLanguageData(options);
        var tesseract = createTesseract(options);
        try {
            return new OcrText(tesseract.doOCR(image.asBufferedImage()), List.of());
        } catch (TesseractException e) {
            throw new IllegalStateException("OCR failed", e);
        } catch (RuntimeException | Error e) {
            throw new IllegalStateException("OCR failed in native Tesseract layer: " + message(e), e);
        }
    }

    private ITesseract createTesseract(OcrOptions options) {
        var tesseract = new Tesseract();
        System.out.println("Using language: " + options.language());
        if (options.language() != null && !options.language().isBlank()) {
            tesseract.setLanguage(options.language());
        }
        var datapath = effectiveDatapath(options);
        System.out.println("Using Tesseract datapath: " + datapath);
        if (datapath != null && !datapath.isBlank()) {
            tesseract.setDatapath(datapath);
        }
        return tesseract;
    }

    String effectiveDatapath(OcrOptions options) {
        if (options.datapath() != null && !options.datapath().isBlank()) {
            return options.datapath();
        }
        return isWindows() ? WINDOWS_DEFAULT_DATAPATH : null;
    }

    private void validateLanguageData(OcrOptions options) {
        var datapath = effectiveDatapath(options);
        var language = options.language();
        if (datapath == null || datapath.isBlank() || language == null || language.isBlank()) {
            return;
        }
        for (var part : language.split("\\+")) {
            var normalized = part.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            var trainedData = Path.of(datapath, normalized + ".traineddata");
            if (!Files.isRegularFile(trainedData)) {
                throw new IllegalStateException("Missing Tesseract language data: " + trainedData);
            }
        }
    }

    private String message(Throwable e) {
        return e.getMessage() == null || e.getMessage().isBlank() ? e.getClass().getSimpleName() : e.getMessage();
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }
}
