package pl.sk.ocr.configurator.app;

import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.core.ocr.OcrEngine;
import pl.sk.ocr.core.ocr.OcrOptions;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public final class RunPageOcrUseCase {
    private final OcrEngine ocrEngine;

    public RunPageOcrUseCase(OcrEngine ocrEngine) {
        this.ocrEngine = ocrEngine;
    }

    public OcrText run(ProcessingImage image, OcrSettings settings) {
        return ocrEngine.recognize(image, new OcrOptions(settings.language(), settings.datapath()));
    }
}
