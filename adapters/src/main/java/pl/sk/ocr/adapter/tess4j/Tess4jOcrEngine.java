package pl.sk.ocr.adapter.tess4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import pl.sk.ocr.core.ocr.OcrEngine;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public final class Tess4jOcrEngine implements OcrEngine {
    static final String WINDOWS_DEFAULT_DATAPATH = "C:\\Program Files\\Tesseract-OCR\\tessdata";
    private final HocrParser hocrParser = new HocrParser();

    @Override
    public OcrText recognize(ProcessingImage image, OcrOptions options) {
        validateLanguageData(options);
        var tesseract = createTesseract(options);
        try {
            return recognizeHocr(tesseract, image);
        } catch (TesseractException e) {
            throw new IllegalStateException("OCR failed", e);
        } catch (IOException e) {
            throw new IllegalStateException("OCR failed while reading HOCR output", e);
        } catch (RuntimeException | Error e) {
            throw new IllegalStateException("OCR failed in native Tesseract layer: " + message(e), e);
        }
    }

    private OcrText recognizeHocr(ITesseract tesseract, ProcessingImage image) throws TesseractException, IOException {
        var outputDir = Files.createTempDirectory("sk-ocr-hocr-");
        var outputbase = outputDir.resolve("page").toString();
        try {
            tesseract.createDocumentsWithResults(
                image.asBufferedImage(),
                "page",
                outputbase,
                List.of(ITesseract.RenderedFormat.HOCR),
                TessPageIteratorLevel.RIL_WORD);
            var hocrPath = outputDir.resolve("page.hocr");
            return hocrParser.parse(Files.readString(hocrPath));
        } finally {
            deleteRecursively(outputDir);
        }
    }

    private ITesseract createTesseract(OcrOptions options) {
        var tesseract = new Tesseract();
        if (options.language() != null && !options.language().isBlank()) {
            tesseract.setLanguage(options.language());
        }
        var datapath = effectiveDatapath(options);
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

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            var sorted = paths.sorted((left, right) -> right.compareTo(left)).toList();
            for (var current : sorted) {
                Files.deleteIfExists(current);
            }
        }
    }
}
