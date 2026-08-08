package pl.sk.ocr.extension.api.image;

import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.extension.api.ExtensionParameters;

public record ImageProcessingRequest(ProcessingImage image, ExtensionParameters parameters) {
    public ImageProcessingRequest {
        image = Validation.requireNonNull(image, "image");
        parameters = parameters == null ? ExtensionParameters.empty() : parameters;
    }
}
