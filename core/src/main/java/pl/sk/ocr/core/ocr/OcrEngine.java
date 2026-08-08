package pl.sk.ocr.core.ocr;

import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public interface OcrEngine {
    OcrText recognize(ProcessingImage image, OcrOptions options);
}
