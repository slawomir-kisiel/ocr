package pl.sk.ocr.extension.api.detector;

import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public record DetectionRequest(ProcessingImage image, String text, ExtensionParameters parameters) {
    public DetectionRequest {
        parameters = parameters == null ? ExtensionParameters.empty() : parameters;
    }
}
